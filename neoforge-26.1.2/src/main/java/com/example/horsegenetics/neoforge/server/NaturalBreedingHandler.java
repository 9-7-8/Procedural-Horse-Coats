package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.example.horsegenetics.common.repro.NaturalCover;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Leave a stallion with mares and there will be foals.</b> Owner, 2026-09-13:
 * the Spontaneous Breeding gene was "now basically just base game behavior",
 * so it is gone and this is what every horse does.
 *
 * <p>This class only gathers facts. Every rule - once per heat, healthy enough
 * ({@link ReproRules#COVER_HEALTH}), not ridden or leashed, no geldings, three
 * covers a day, the local cap, cowboy stock exempt - is
 * {@link NaturalCover#decide}, which is unit-tested. The cover goes
 * through {@link ReproHandler#breed}, so carrot effects armed on either horse act
 * on it, and it is credited to the mare's owner. It runs off the entity tick, so it
 * only ever happens in loaded chunks. The heat-attraction goal
 * ({@code HerdGoals.HeatAttraction}) is what brings the two together.
 *
 * <h4>The owner hears why</h4>
 * Owner's ask: <i>"if a breeding attempt does not go through for any reason,
 * print the failure to the horse's owner in chat"</i>. {@link #tell} does that,
 * in {@link CoverNotice}'s words, for the refusals an owner can act on and for
 * how a cover itself went. Which refusals those are, and why the other two say
 * nothing at all, is {@link CoverNotice}'s own documentation. The switch is
 * {@code notices.owned_horse_breeding}. The {@link ActionTrace} lines below are
 * unchanged: they are the debug record, they are not owner-facing, and they
 * throttle on their own schedule.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class NaturalBreedingHandler {

    private NaturalBreedingHandler() {
    }

    /** Two seconds; a heat is at least a Minecraft day, so this is plenty. */
    static final int SCAN = 40;

    /**
     * Each mare's courtship in progress. Transient on purpose, like vanilla's
     * {@code BreedGoal.loveTime}: a restart mid-courtship costs three seconds.
     */
    private static final java.util.Map<java.util.UUID, NaturalCover.Courtship> COURTSHIPS = new java.util.HashMap<>();

    /** When each capped mare last said so. Transient; a restart just says it again. */
    private static final java.util.Map<java.util.UUID, Long> CROWDED_LOGGED = new java.util.HashMap<>();

    /** When each too-hurt mare last said so. Transient, like {@link #CROWDED_LOGGED}. */
    private static final java.util.Map<java.util.UUID, Long> UNFIT_LOGGED = new java.util.HashMap<>();

    /**
     * What each mare's owner was last told, and when. Transient like the rest of
     * these: a restart costs one repeated line at most.
     */
    private static final java.util.Map<java.util.UUID, Told> NOTICES = new java.util.HashMap<>();

    /** Above this many remembered mares, the long-quiet ones are dropped on the next line. */
    private static final int SWEEP_ABOVE = 512;

    /** One line to an owner, for the throttle in {@link CoverNotice#dueAgain}. */
    private record Told(CoverNotice.Reason reason, long at) {}

    /** Wild mares the cap held back since {@link #wildCappedSince}, summarised every ten minutes. */
    private static final java.util.Set<java.util.UUID> WILD_CAPPED = new java.util.HashSet<>();
    private static long wildCappedSince = -1L;
    private static final long WILD_CAP_SUMMARY_TICKS = 12_000L;

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse mare) || !mare.isAlive() || mare.isBaby()) {
            return;
        }
        if (!(mare.level() instanceof ServerLevel level)) {
            return;
        }
        if ((mare.tickCount + mare.getId()) % SCAN != 0 || !HorseRecords.hasRealRecord(mare)) {
            return;
        }
        HorseRecord mareRecord = HorseRecords.of(mare);
        if (mareRecord.sex() != Sex.FEMALE) {
            return;
        }
        long now = level.getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = ReproHandler.of(mare);
        if (!ReproRules.mayTryNaturally(r, now, t)) {
            return;     // the cheap refusal first: most mares, most of the time
        }

        List<Horse> near = level.getEntitiesOfClass(Horse.class,
                mare.getBoundingBox().inflate(ReproRules.NATURAL_REACH),
                h -> h != mare && h.isAlive() && HorseRecords.hasRealRecord(h)
                        && HorseRecords.of(h).sex() == Sex.MALE && YardPens.together(mare, h));
        if (near.isEmpty()) {
            return;
        }
        List<NaturalCover.Stallion> candidates = new ArrayList<>(near.size());
        for (Horse h : near) {
            candidates.add(new NaturalCover.Stallion(party(h), h.distanceToSqr(mare),
                    ReproHandler.of(h).coversOn(now, t.dayTicks())));
        }
        int crowd = level.getEntitiesOfClass(Horse.class,
                mare.getBoundingBox().inflate(ReproRules.NATURAL_CAP_RADIUS), h -> h != mare && h.isAlive() && YardPens.together(mare, h)).size();

        NaturalCover.Party mareParty = party(mare);
        NaturalCover.Decision decision = NaturalCover.decide(mareParty, r, now, t, candidates, crowd);
        switch (decision.verdict()) {
            case COVER -> {
                // VANILLA'S COURTSHIP FIRST (gap 230): the same stallion in reach for three
                // seconds, as BreedGoal waits out loveTime >= 60 before it breeds.
                Horse stallion = near.get(decision.stallion());
                NaturalCover.Courtship before = COURTSHIPS.get(mare.getUUID());
                NaturalCover.Courtship courtship = before == null
                        ? NaturalCover.Courtship.start(stallion.getUUID(), now)
                        : before.seen(stallion.getUUID(), now);
                if (courtship.complete(now)) {
                    COURTSHIPS.remove(mare.getUUID());
                    cover(level, mare, mareRecord, r, stallion, now);
                } else {
                    COURTSHIPS.put(mare.getUUID(), courtship);
                }
            }
            case CROWDED -> {
                // The owner hears it wherever she is standing; the log below is the
                // debug record, and splits penned from wild for its own reasons.
                tell(mare, CoverNotice.Reason.CROWDED, crowd, now);
                if (!YardPens.inPen(mare)) {
                    // A WILD MARE GETS ONE SUMMARY, NOT A LINE (2026-09-14). Wild bands live
                    // eight to sixteen blocks, so nearly every wild mare is capped every heat,
                    // and a line each a minute was 2,900 of 3,400 fertility lines in a
                    // morning - burying the pen that tests the cap. They are counted instead.
                    if (wildCappedSince < 0L) {
                        wildCappedSince = now;
                    } else if (now - wildCappedSince >= WILD_CAP_SUMMARY_TICKS) {
                        ActionTrace.log("fertility", "crowding cap held back " + WILD_CAPPED.size()
                                + " wild mares in the last 10 minutes (more than " + ReproRules.NATURAL_CAP
                                + " horses within " + (int) ReproRules.NATURAL_CAP_RADIUS + " blocks)");
                        WILD_CAPPED.clear();
                        wildCappedSince = now;
                    }
                    WILD_CAPPED.add(mare.getUUID());
                    break;
                }
                // Once a minute per mare: a capped paddock asks every two seconds, and
                // eight mares saying so each time buries the log the cap is read from.
                Long last = CROWDED_LOGGED.get(mare.getUUID());
                if (last == null || now - last >= 1_200L) {
                    CROWDED_LOGGED.put(mare.getUUID(), now);
                    ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " not covered: "
                            + crowd + " other horses within " + (int) ReproRules.NATURAL_CAP_RADIUS
                            + " blocks, the cap is " + ReproRules.NATURAL_CAP);
                }
            }
            case NOT_A_BREEDING_MARE -> {
                // HER OWNER HEARS ALL FOUR, unlike the log below: ridden and leashed are
                // the player's own doing, but a mare left saddled in a pen overnight is
                // not, and "why is she the only one not in foal" is the question this
                // whole notice exists to answer.
                CoverNotice.Reason why = null;
                if (!mareParty.healthyEnough()) {
                    why = CoverNotice.Reason.HURT;
                } else if (mareParty.ridden()) {
                    why = CoverNotice.Reason.RIDDEN;
                } else if (mareParty.leashed()) {
                    why = CoverNotice.Reason.LEASHED;
                } else if (mareParty.cowboyStock()) {
                    why = CoverNotice.Reason.COWBOY_STOCK;
                }
                if (why != null) {
                    tell(mare, why, crowd, now);
                }
                // ONLY HER HEALTH IS WORTH A LINE (gap 258). To get here she is already an
                // adult mare in heat, with a try left and a stallion in reach, so the rest
                // of the reasons - ridden, leashed, cowboy stock - are the player's own
                // doing and obvious. Being a point short of full health was neither: it
                // refused her silently, and a miscarriage costs half a heart.
                if (!mareParty.healthyEnough()) {
                    Long saidAt = UNFIT_LOGGED.get(mare.getUUID());
                    if (saidAt == null || now - saidAt >= 1_200L) {
                        UNFIT_LOGGED.put(mare.getUUID(), now);
                        ActionTrace.log("fertility", ActionTrace.describeShort(mare) + String.format(
                                " not covered: hurt, %.1f/%.1f health - a natural cover needs %.0f%%",
                                mare.getHealth(), mare.getMaxHealth(), ReproRules.COVER_HEALTH * 100.0));
                    }
                }
            }
            default -> {
                // no able stallion in reach, or not her heat - nothing worth a line
            }
        }
    }

    private static void cover(ServerLevel level, Horse mare, HorseRecord mareRecord, Reproduction r, Horse stallion,
                              long now) {
        // Her one try this heat is spent whatever the roll says.
        ReproHandler.set(mare, r.withNaturalTry(now));
        HorseRecord stallionRecord = HorseRecords.of(stallion);
        Rng rng = HorseRecords.rng(mare);
        Conception.Result result = ReproHandler.breed(mare, mareRecord,
                HorseBreedingHandler.genomeOf(mare, mareRecord, rng),
                HorseBreedingHandler.genomeOf(stallion, stallionRecord, rng),
                stallionRecord, stallion, List.of(), bredBy(mare, mareRecord), ownerPlayer(mare));
        level.broadcastEntityEvent(mare, (byte) 18);
        // A cover is once a heat, so this one is not throttled in practice - and the
        // owner who is told why a cover did not happen has to be told when one did,
        // or the silence reads as another failure.
        tell(mare, switch (result.outcome()) {
            case CONCEIVED -> CoverNotice.Reason.CONCEIVED;
            case DID_NOT_TAKE -> CoverNotice.Reason.DID_NOT_TAKE;
            case NOT_RECEPTIVE -> CoverNotice.Reason.NOT_RECEPTIVE;
        }, 0, now);
        // Null for a wild mare, which HorseProgress swallows - a cover in a wild
        // band is nobody's achievement.
        HorseProgress.complete(ownerPlayer(mare), ProgressTask.NATURAL_COVER);
        ActionTrace.log("fertility", "natural cover: " + ActionTrace.describeShort(mare) + " by "
                + ActionTrace.describeShort(stallion) + " - " + result.outcome()
                + String.format(" (chance %.2f)", result.chance()));
    }

    /**
     * <b>Tell the mare's owner what happened, if it is worth saying again.</b>
     * Silent for a wild mare, for an owner who is not in this world to read it,
     * and while {@code notices.owned_horse_breeding} is off. The words and the
     * quiet period are {@link CoverNotice}; this is the translation.
     */
    private static void tell(Horse mare, CoverNotice.Reason reason, int crowd, long now) {
        if (!ServerConfig.breedingNotices() || !(ownerPlayer(mare) instanceof ServerPlayer player)) {
            return;
        }
        Told last = NOTICES.get(mare.getUUID());
        if (!CoverNotice.dueAgain(last == null ? null : last.reason(), last == null ? 0L : last.at(), reason, now)) {
            return;
        }
        if (NOTICES.size() > SWEEP_ABOVE) {
            NOTICES.entrySet().removeIf(e -> now - e.getValue().at() > CoverNotice.QUIET_TICKS);
        }
        NOTICES.put(mare.getUUID(), new Told(reason, now));
        player.sendSystemMessage(Component.literal(CoverNotice.line(HorseNotices.name(mare), reason, crowd))
                .withStyle(reason.good() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    /** The facts {@link NaturalCover} asks about one horse. */
    static NaturalCover.Party party(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        // hasData first: getData would attach an empty brand to every horse it asked.
        boolean cowboy = horse.hasData(ModAttachments.COWBOY_BRAND.get())
                && horse.getData(ModAttachments.COWBOY_BRAND.get()).isBranded();
        return new NaturalCover.Party(!horse.isBaby(), record.sex() == Sex.FEMALE, record.gelded(),
                ReproRules.healthyEnoughToBreed(horse.getHealth(), horse.getMaxHealth()),
                horse.isVehicle(), horse.isLeashed(), cowboy);
    }

    /**
     * The mare's owner, for {@code bredBy} - blank for a wild mare. An offline
     * owner is resolved through {@link HorseOwnership#ownerName}.
     */
    private static String bredBy(Horse mare, HorseRecord record) {
        if (!mare.isTamed()) {
            return "";
        }
        return HorseOwnership.ownerName(mare).orElse(record.tamedBy().orElse(""));
    }

    private static @Nullable Player ownerPlayer(Horse horse) {
        return horse.getOwner() instanceof Player p ? p : null;
    }
}
