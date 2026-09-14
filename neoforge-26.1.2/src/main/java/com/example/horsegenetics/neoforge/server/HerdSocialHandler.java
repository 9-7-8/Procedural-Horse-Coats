package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.BandType;
import com.example.horsegenetics.common.herd.BandRole;
import com.example.horsegenetics.common.herd.HerdRules;
import com.example.horsegenetics.common.herd.Relationship;
import com.example.horsegenetics.common.herd.SocialLedger;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseSocialAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseSocialSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>The social tick.</b> Every horse, every {@link #SCAN} ticks, staggered: it
 * notices who is near it and writes that into its relationships, and a wild band
 * member also keeps its band whole.
 *
 * <h2>What it records</h2>
 * <ul>
 *   <li><b>Familiarity</b> with every horse within {@link #NEAR} blocks.</li>
 *   <li><b>Grooming</b> between partners standing within {@link #GROOM_NEAR}: two
 *       adult mares, or a dam and her foal.</li>
 *   <li><b>Rivalry</b> between adult stallions within {@link #RIVAL_NEAR} who have
 *       reason to be rivals - a band stallion and an outside male, and, more
 *       gently, the members of one bachelor band testing each other.</li>
 * </ul>
 *
 * <h2>Keeping a band whole</h2>
 * A band used to <b>split into single-horse herds</b> when its lead died: each
 * member promoted itself after a grace period and nobody re-pointed at anyone.
 * Now a family band simply follows its <b>lead mare</b> while it has no stallion
 * (see {@link WildHerdGoal}) and stays one band until a stallion takes it over; a
 * bachelor band re-points everyone at its highest-ranking member; and a horse left
 * with nobody from its band nearby joins the band of the horse it knows best.
 *
 * <p>The band-changing events - dispersal, takeovers, transfers - are
 * {@link BandLife}. Tamed horses keep relationships and nothing else.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HerdSocialHandler {

    private HerdSocialHandler() {
    }

    /** Five seconds. Every rate is per day, so this only trades precision for tick time. */
    public static final int SCAN = 100;

    static final double NEAR = 12.0;
    static final double GROOM_NEAR = 4.0;
    static final double RIVAL_NEAR = 20.0;
    /** How far a band is assumed to spread when looking for its members. */
    static final double BAND_REACH = 48.0;
    /** How long a band's lead must be missing before the band acts on it. Two minutes. */
    static final long LOST_LEAD_GRACE = 2_400L;

    /** When each horse first found its band's lead missing. Transient: a restart just restarts the wait. */
    private static final Map<UUID, Long> LEAD_MISSING_SINCE = new HashMap<>();

    /** Family bands already reported as holding together without a stallion. Transient. */
    private static final java.util.Set<UUID> VACANT_LOGGED = new java.util.HashSet<>();

    /** Each band's lead mare, recomputed at most once per scan. */
    private record CachedMare(Optional<UUID> mare, long at) {
    }

    private static final Map<UUID, CachedMare> LEAD_MARES = new HashMap<>();

    // ------------------------------------------------------------------
    // The tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0 || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        long now = level.getGameTime();
        HorseSocialAttachment before = horse.getData(ModAttachments.HORSE_SOCIAL.get());
        HorseSocialAttachment social = ensureBorn(horse, before, now);

        List<Horse> nearby = level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(RIVAL_NEAR),
                h -> h != horse && h.isAlive() && HorseRecords.hasRealRecord(h));

        SocialLedger ledger = social.ledger();
        long elapsed = social.lastDecay() == 0L ? 0L : now - social.lastDecay();
        ledger = ledger.decayed(now, elapsed);
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        for (Horse other : nearby) {
            double d2 = horse.distanceToSqr(other);
            double rival = rivalWeight(horse, care, other);
            if (d2 > NEAR * NEAR && rival <= 0.0) {
                continue;
            }
            double groom = d2 <= GROOM_NEAR * GROOM_NEAR ? groomWeight(horse, social, other) : 0.0;
            ledger = ledger.together(other.getUUID(), now, SCAN, groom, rival);
        }
        social = social.withLedger(ledger, now);

        if (!social.equals(before)) {
            horse.setData(ModAttachments.HORSE_SOCIAL.get(), social);
        }

        if (!horse.isTamed() && care.inWildHerd()) {
            keepBandWhole(horse, care, level, now);
            BandLife.consider(horse, level, now);
        }
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        LEAD_MISSING_SINCE.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------
    // Age
    // ------------------------------------------------------------------

    /**
     * A horse the social tick has not seen before gets an age. A foal's is exact -
     * vanilla counts its growing-up time down - and an adult's is an estimate
     * between two and ten days, off its UUID so it is the same estimate every time.
     */
    static HorseSocialAttachment ensureBorn(Horse horse, HorseSocialAttachment social, long now) {
        if (social.bornKnown()) {
            return social;
        }
        long born;
        if (horse.isBaby()) {
            born = now - (HerdRules.GROW_UP_TICKS + horse.getAge());   // getAge() counts up from -24000
        } else {
            long days = 2L + Math.floorMod(horse.getUUID().getLeastSignificantBits(), 9L);
            born = now - HerdRules.GROW_UP_TICKS - days * HerdRules.DAY_TICKS;
        }
        HorseRecord record = HorseRecords.of(horse);
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        Optional<UUID> natal = horse.isBaby() && care.inWildHerd() ? care.herd() : Optional.empty();
        double disperse = HerdRules.disperseAfterDays(record.sex(),
                new com.example.horsegenetics.neoforge.NeoRng(horse.getRandom()));
        return social.withBirth(born, social.dam().isPresent() ? social.dam() : record.motherId(), natal, disperse);
    }

    // ------------------------------------------------------------------
    // Who counts as what to whom
    // ------------------------------------------------------------------

    private static double groomWeight(Horse horse, HorseSocialAttachment social, Horse other) {
        HorseSocialAttachment theirs = other.getData(ModAttachments.HORSE_SOCIAL.get());
        boolean damAndFoal = social.dam().map(other.getUUID()::equals).orElse(false)
                || theirs.dam().map(horse.getUUID()::equals).orElse(false);
        if (damAndFoal) {
            return 1.0;
        }
        boolean twoMares = !horse.isBaby() && !other.isBaby()
                && socialMare(horse) && socialMare(other);
        return twoMares ? 1.0 : 0.0;
    }

    /**
     * A mare - or a gelding, who keeps company the way mares do (owner,
     * 2026-09-13: "no stallion behaviour").
     */
    static boolean socialMare(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        return record.sex() == Sex.FEMALE || record.gelded();
    }

    private static double rivalWeight(Horse horse, HorseCareAttachment care, Horse other) {
        if (horse.isTamed() || other.isTamed() || horse.isBaby() || other.isBaby()) {
            return 0.0;
        }
        if (!HorseRecords.of(horse).entire() || !HorseRecords.of(other).entire()) {
            return 0.0;
        }
        HorseCareAttachment theirs = other.getData(ModAttachments.HORSE_CARE.get());
        if (care.herd().isPresent() && care.herd().equals(theirs.herd())) {
            // one bachelor band testing itself, gently; a family band has one stallion
            return isBachelor(care) ? 0.25 : 0.0;
        }
        boolean eitherHoldsMares = isBandStallion(horse, care) || isBandStallion(other, theirs);
        return eitherHoldsMares ? 1.0 : 0.3;
    }

    static boolean isBachelor(HorseCareAttachment care) {
        return BandType.BACHELOR.name().equals(care.herdBand().orElse(""));
    }

    static boolean isBandStallion(Horse horse, HorseCareAttachment care) {
        return care.inWildHerd() && !isBachelor(care) && care.herd().map(horse.getUUID()::equals).orElse(false);
    }

    // ------------------------------------------------------------------
    // The band
    // ------------------------------------------------------------------

    /** The loaded members of {@code herd} within {@link #BAND_REACH} of {@code around}, untamed. */
    static List<Horse> bandMembers(ServerLevel level, UUID herd, Entity around) {
        return level.getEntitiesOfClass(Horse.class, around.getBoundingBox().inflate(BAND_REACH),
                h -> h.isAlive() && !h.isTamed()
                        && h.getData(ModAttachments.HORSE_CARE.get()).herd().map(herd::equals).orElse(false));
    }

    /** The loaded lead horse of a band, or {@code null} if it is dead, gone or unloaded. */
    static Horse leadOf(ServerLevel level, UUID herd) {
        Entity e = level.getEntity(herd);
        return e instanceof Horse h && h.isAlive() && !h.isTamed() ? h : null;
    }

    /**
     * The mare {@code herd} follows, cached for a scan. The oldest adult mare among
     * the loaded members, her standing breaking a tie - see
     * {@link HerdRules#leadMare}.
     */
    static Optional<UUID> leadMare(ServerLevel level, UUID herd, Entity around) {
        long now = level.getGameTime();
        CachedMare cached = LEAD_MARES.get(herd);
        if (cached != null && now - cached.at() < SCAN) {
            return cached.mare();
        }
        List<Horse> members = bandMembers(level, herd, around);
        List<HerdRules.MareFacts> mares = new ArrayList<>();
        for (Horse m : members) {
            if (m.isBaby() || HorseRecords.of(m).sex() != Sex.FEMALE) {
                continue;
            }
            HorseSocialAttachment s = m.getData(ModAttachments.HORSE_SOCIAL.get());
            double standing = 0.0;
            for (Horse other : members) {
                if (other != m) {
                    standing += s.ledger().rankOver(other.getUUID());
                }
            }
            double age = s.bornKnown() ? HerdRules.ageDays(s.bornTick(), now) : 0.0;
            mares.add(new HerdRules.MareFacts(m.getUUID(), age, standing));
        }
        Optional<UUID> mare = HerdRules.leadMare(mares);
        LEAD_MARES.put(herd, new CachedMare(mare, now));
        return mare;
    }

    /**
     * Keep a band from falling apart when its lead is gone - see the class note.
     */
    private static void keepBandWhole(Horse horse, HorseCareAttachment care, ServerLevel level, long now) {
        UUID herd = care.herd().orElseThrow();
        if (herd.equals(horse.getUUID()) || leadOf(level, herd) != null) {
            LEAD_MISSING_SINCE.remove(horse.getUUID());
            return;
        }
        long since = LEAD_MISSING_SINCE.computeIfAbsent(horse.getUUID(), k -> now);
        if (now - since < LOST_LEAD_GRACE) {
            return;
        }
        LEAD_MISSING_SINCE.remove(horse.getUUID());

        List<Horse> members = bandMembers(level, herd, horse);
        if (members.size() > 1) {
            if (isBachelor(care)) {
                // A bachelor band without its lead re-forms round its top-ranked male.
                Horse top = topRanked(members);
                BandLife.repoint(members, top.getUUID(), care.herdBreed().orElse(""), BandType.BACHELOR);
                ActionTrace.log("herd", "bachelor band " + short8(herd) + " re-formed round "
                        + ActionTrace.describeShort(top));
            } else if (VACANT_LOGGED.add(herd)) {
                // The one line that says the old split bug is gone: nothing else
                // happens here, so without it a band holding together is silence.
                ActionTrace.log("herd", "band " + short8(herd) + " has lost its stallion and stays one band: "
                        + members.size() + " horses, following lead mare "
                        + leadMare(level, herd, horse).map(id -> nameOf(level, id)).orElse("(none loaded)"));
            }
            // A family band keeps its id and follows its lead mare until a stallion
            // takes it over - it is vacant, not gone.
            return;
        }

        // Nobody from its band is anywhere near: join the band of the horse it knows best.
        SocialLedger ledger = horse.getData(ModAttachments.HORSE_SOCIAL.get()).ledger();
        Horse best = null;
        double bestFamiliarity = 0.0;
        for (Horse other : level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(BAND_REACH / 1.5),
                h -> h != horse && h.isAlive() && !h.isTamed()
                        && h.getData(ModAttachments.HORSE_CARE.get()).inWildHerd())) {
            double f = ledger.with(other.getUUID()).map(Relationship::familiarity).orElse(0.0);
            if (f > bestFamiliarity) {
                bestFamiliarity = f;
                best = other;
            }
        }
        if (best != null && bestFamiliarity >= 0.2) {
            HorseCareAttachment theirs = best.getData(ModAttachments.HORSE_CARE.get());
            horse.setData(ModAttachments.HORSE_CARE.get(), care.withWildHerd(theirs.herd().orElseThrow(),
                    theirs.herdBreed().orElse(""), theirs.herdBand().orElse(BandType.TRADITIONAL.name())));
            ActionTrace.log("herd", ActionTrace.describeShort(horse) + " lost its band and joined "
                    + ActionTrace.describeShort(best) + "'s");
        }
    }

    static Horse topRanked(List<Horse> members) {
        Horse best = members.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Horse m : members) {
            if (m.isBaby() || !HorseRecords.of(m).entire()) {
                continue;
            }
            SocialLedger l = m.getData(ModAttachments.HORSE_SOCIAL.get()).ledger();
            double score = 0.0;
            for (Horse other : members) {
                if (other != m) {
                    score += l.rankOver(other.getUUID());
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // What the information screen shows
    // ------------------------------------------------------------------

    /** Sent while a player has a horse's information screen open - once a second, with the inspect lease. */
    public static void sendSummary(ServerPlayer player, int entityId) {
        if (!(player.level().getEntity(entityId) instanceof Horse horse) || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        ServerLevel level = player.level();
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
        SocialLedger ledger = social.ledger();

        boolean inBand = !horse.isTamed() && care.inWildHerd();
        UUID herd = care.herd().orElse(null);
        BandRole role = roleOf(level, horse);

        String standing = "";
        if (inBand) {
            List<Horse> members = bandMembers(level, herd, horse);
            int outranks = 0;
            int others = 0;
            for (Horse m : members) {
                if (m == horse) {
                    continue;
                }
                others++;
                if (ledger.rankOver(m.getUUID()) > 0.05) {
                    outranks++;
                }
            }
            standing = others == 0 ? "the only one of its band nearby"
                    : "outranks " + outranks + " of the " + others + " band-mates nearby";
        }

        List<String> companions = new ArrayList<>();
        for (Relationship r : ledger.companions(3)) {
            companions.add(nameOf(level, r.other())
                    + (r.grooming() >= HerdRules.GROOMING_PARTNER ? " (grooming partner)" : ""));
        }
        String rival = ledger.rival().map(r -> nameOf(level, r.other())).orElse("");

        PacketDistributor.sendToPlayer(player, new HorseSocialSyncPayload(entityId, role.label(),
                role.description(), standing, companions, rival, ReproHandler.breedingLine(horse)));
    }

    /** The role the information screen shows - also what the test yard's herd watch logs. */
    static BandRole roleOf(ServerLevel level, Horse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
        boolean male = HorseRecords.of(horse).entire();   // a gelding's role reads like a mare's
        boolean inBand = !horse.isTamed() && care.inWildHerd();
        UUID herd = care.herd().orElse(null);
        boolean leadMare = inBand && !isBachelor(care)
                && leadMare(level, herd, horse).map(horse.getUUID()::equals).orElse(false);
        boolean natal = social.natalHerd().isPresent() && social.natalHerd().equals(care.herd());
        return BandRole.of(horse.isTamed(), inBand, isBachelor(care),
                herd != null && herd.equals(horse.getUUID()), male, horse.isBaby(), leadMare, natal);
    }

    static String nameOf(ServerLevel level, UUID id) {
        Entity e = level.getEntity(id);
        if (e instanceof Horse h && HorseRecords.hasRealRecord(h)) {
            return HorseRecords.of(h).displayName();
        }
        return HorseAncestryData.get(level.getServer()).lookup(id)
                .map(HorseRecord::displayName).orElse("a horse it once knew");
    }

    static String short8(UUID id) {
        return id.toString().substring(0, 8);
    }
}
