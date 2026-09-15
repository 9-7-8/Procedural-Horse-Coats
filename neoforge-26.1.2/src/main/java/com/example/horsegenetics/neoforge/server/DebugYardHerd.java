package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.BandType;
import com.example.horsegenetics.common.herd.HerdRules;
import com.example.horsegenetics.common.herd.Relationship;
import com.example.horsegenetics.common.herd.SocialLedger;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseSocialAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Q;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Q_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_R;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_R_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows Q-R of the test yard: band life, with nobody at the controls</b>
 * (2026-09-14; owner: "make sure we have tests that work with me afk in game for the
 * new herd dynamics system", then "condense down the testing area").
 *
 * <p>Every herd event is a roll against a per-day rate ({@link HerdRules}), and a
 * Minecraft day is twenty minutes, so the pens do two things a wild band in the
 * overworld cannot: they <b>set up the state an event needs</b> - a grown colt already
 * due to leave, mares who already know the band next door, stallions who already know
 * each other - and they <b>act on a clock</b> where the test needs a hand: the
 * stallion is killed, bachelors arrive, a mare is walked away, a zombie hurts a foal.
 * Each pen is watched with {@link DebugWorldWatch#watchSocial}, and everything the
 * clocks do is a {@code [trace] test yard} line beside the {@code [trace] herd} lines.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>Q</td><td>LEAVING HOME and HOST BAND (wild, one group)</td>
 *       <td>LEADERLESS (wild), GROOMING (tamed)</td></tr>
 *   <tr><td>R</td><td>TAKEOVER (wild), DAM DEFENCE (tamed)</td>
 *       <td>MET NATURAL (fertility); DISPLACEMENT deleted 2026-09-15, verified</td></tr>
 * </table>
 *
 * <p><b>Confirmed and deleted, 2026-09-14</b>: SPARRING and HERDING, read off the
 * first quarter hour's log (spars with no damage and a settled order; a stallion
 * fetching the stray from sixteen blocks).
 *
 * <p><b>The pens share walls</b>: each is its own {@link YardPens} pen, so no band
 * decision reaches over one. LEAVING HOME and HOST BAND are one group on purpose - a
 * filly needs a band next door to leave <i>for</i>. Mares have natural covers switched
 * off here, so no heat pulls a stallion off what he is being tested for.
 */
@EventBusSubscriber
final class DebugYardHerd {

    private DebugYardHerd() {
    }

    private static final String CODE = "horsegenetics.fertility=n/n";
    private static final String BREED = "feral_mixed";

    // ------------------------------------------------------------------
    // The clock
    // ------------------------------------------------------------------

    private record Task(long due, Runnable run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static @Nullable ServerLevel taskLevel;

    /** Forget every pending step - the yard was torn down, or the server is stopping. */
    static void cancel() {
        TASKS.clear();
        taskLevel = null;
    }

    /** Run {@code run} {@code ticks} game ticks from now. The breeding rows use it too. */
    static void after(ServerLevel level, long ticks, Runnable run) {
        taskLevel = level;
        TASKS.add(new Task(level.getGameTime() + ticks, run));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = taskLevel;
        if (level == null || TASKS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.due() <= now) {
                due.add(t);
                return true;
            }
            return false;
        });
        for (Task t : due) {
            try {
                t.run().run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] herd rows: a timed step failed", e);
            }
        }
    }

    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        cancel();
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    /**
     * Pending steps are cleared by {@code DebugWorldWatch.stop}, which every yard build
     * runs first - not here, because the breeding rows are built before these and
     * schedule steps of their own.
     */
    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN;
        try {
            leavingHome(level, gy, west, mouthZ + ROW_Q);
            leaderless(level, gy, east, mouthZ + ROW_Q);
            grooming(level, gy, east + 11, mouthZ + ROW_Q);
            takeover(level, gy, west, mouthZ + ROW_R);
            damDefence(level, gy, west + 12, mouthZ + ROW_R);
            ActionTrace.log("test yard", "herd pens built (rows Q-R)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: herd rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row R - dispersal and mare transfers; a band that loses its stallion; sparring
    // ------------------------------------------------------------------

    /**
     * The filly is due to leave at once and the colt three minutes later, <b>on
     * purpose</b>: a filly takes the nearest bachelor over the nearest band, and a
     * brother who left first is the nearest bachelor there is (gap 234). The two mover
     * mares know the host band's mares already (their side only), so either may move
     * over. Two pens, one {@link YardPens} group.
     */
    private static void leavingHome(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 10, ROW_Q_D, "LEAVING HOME", "LEAVING HOME",
                List.of("LEAVING HOME", "filly leaves now,", "colt in 3 min;", "mares may move E"));
        Horse sire = horse(level, gy, x0 + 2.5, z0 + 3, Sex.MALE, false, "NATAL STALLION", 9.0);
        Horse m1 = horse(level, gy, x0 + 5.5, z0 + 3, Sex.FEMALE, false, "MOVER MARE 1", 8.5);
        Horse m2 = horse(level, gy, x0 + 8.0, z0 + 3, Sex.FEMALE, false, "MOVER MARE 2", 8.0);
        Horse filly = horse(level, gy, x0 + 3.5, z0 + 6.5, Sex.FEMALE, false, "FILLY: LEAVES NOW", 0.0);
        Horse colt = horse(level, gy, x0 + 7.0, z0 + 6.5, Sex.MALE, false, "COLT: LEAVES AT 3M", 0.0);
        band(sire, BandType.TRADITIONAL, sire, m1, m2, filly, colt);
        if (sire != null) {
            Optional<UUID> natal = Optional.of(sire.getUUID());
            // Adult days lived = the day she is due to leave, so the first scan sends her.
            born(filly, 3.05, natal, 3.0);
            // ...and him three minutes short of his.
            born(colt, 1.0 - 3.0 * 1200.0 / HerdRules.DAY_TICKS, natal, 1.0);
        }

        int hx = x0 + 10;
        pen(level, gy, hx, z0, 9, ROW_Q_D, "HOST BAND", "LEAVING HOME",
                List.of("HOST BAND", "the filly joins", "this band; W mares", "may move here"));
        Horse host = horse(level, gy, hx + 6.0, z0 + 4.5, Sex.MALE, false, "HOST STALLION", 9.0);
        Horse h1 = horse(level, gy, hx + 2.5, z0 + 3, Sex.FEMALE, false, "HOST MARE 1", 8.5);
        Horse h2 = horse(level, gy, hx + 2.5, z0 + 6.5, Sex.FEMALE, false, "HOST MARE 2", 8.0);
        band(host, BandType.TRADITIONAL, host, h1, h2);
        for (Horse mover : new Horse[]{m1, m2}) {
            for (Horse friend : new Horse[]{h1, h2}) {
                oneWay(mover, friend, 0.9, 0.0, 0.5);
            }
        }
    }

    private static void leaderless(ServerLevel level, int gy, int x0, int z0) {
        int w = 11;
        pen(level, gy, x0, z0, w, ROW_Q_D, "LEADERLESS", "LEADERLESS",
                List.of("LEADERLESS BAND", "stallion dies 1m:", "mares stay ONE band", "bachelors at 5m"));
        Horse stallion = horse(level, gy, x0 + 5.5, z0 + 4.5, Sex.MALE, false, "DOOMED STALLION", 9.0);
        Horse lead = horse(level, gy, x0 + 2.5, z0 + 2.5, Sex.FEMALE, false, "ELDEST MARE", 9.5);
        Horse m2 = horse(level, gy, x0 + 2.5, z0 + 6.5, Sex.FEMALE, false, "BAND MARE 2", 7.0);
        Horse m3 = horse(level, gy, x0 + 8.5, z0 + 2.5, Sex.FEMALE, false, "BAND MARE 3", 6.0);
        band(stallion, BandType.TRADITIONAL, stallion, lead, m2, m3);
        if (stallion == null) {
            return;
        }
        UUID stallionId = stallion.getUUID();
        AABB inside = DebugTestYard.box(x0, gy, z0, x0 + w, gy + 3, z0 + ROW_Q_D);

        after(level, 1200, () -> {
            if (level.getEntity(stallionId) instanceof Horse s && s.isAlive()) {
                s.kill(level);
                ActionTrace.log("test yard", "LEADERLESS: killed DOOMED STALLION. Expect in about two minutes: "
                        + "'[trace] herd | band ... has lost its stallion and stays one band'");
            }
        });
        // The grace is two minutes, then up to one scan per member to notice it.
        after(level, 1200 + HerdSocialHandler.LOST_LEAD_GRACE + 600, () -> {
            Set<String> herds = new LinkedHashSet<>();
            int wild = 0;
            for (Horse h : level.getEntitiesOfClass(Horse.class, inside, Horse::isAlive)) {
                HorseCareAttachment care = h.getData(ModAttachments.HORSE_CARE.get());
                if (!h.isTamed() && care.inWildHerd()) {
                    wild++;
                    herds.add(HerdSocialHandler.short8(care.herd().orElseThrow()));
                }
            }
            ActionTrace.log("test yard", "LEADERLESS check, 3.5 min after the kill: " + wild + " mares in "
                    + herds.size() + " band(s) " + herds + " - "
                    + (wild > 1 && herds.size() == 1 ? "PASS, still one band" : "FAIL, the band split"));
        });
        UUID anchor = lead != null ? lead.getUUID() : stallionId;
        after(level, 6000, () -> {
            if (lead != null && level.getEntity(anchor) == null) {
                return;     // the yard is gone
            }
            Horse a = horse(level, gy, x0 + 8.5, z0 + 6.5, Sex.MALE, false, "BACHELOR A", 5.0);
            Horse b = horse(level, gy, x0 + 6.0, z0 + 7.0, Sex.MALE, false, "BACHELOR B", 4.0);
            band(a, BandType.BACHELOR, a, b);
            seed(a, b, 0.6, 0.0, 0.0);
            ActionTrace.log("test yard", "LEADERLESS: two bachelors put in with the vacant band. Expect, "
                    + "usually within the hour: '[trace] herd | BACHELOR ... took over band' with NO fight, "
                    + "then the other bachelor re-formed round himself, and later real challenges");
        });
    }

    // ------------------------------------------------------------------
    // Row S - a takeover; a dam defending her foal; displacement
    // ------------------------------------------------------------------

    private static void takeover(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 12, ROW_R_D, "TAKEOVER", "TAKEOVER",
                List.of("TAKEOVER", "band + 3 bachelors:", "fights stop at 40%", "NOBODY dies"));
        Horse stallion = horse(level, gy, x0 + 4.0, z0 + 3.5, Sex.MALE, false, "BAND STALLION", 9.0);
        Horse m1 = horse(level, gy, x0 + 2.0, z0 + 2.0, Sex.FEMALE, false, "TAKEOVER MARE 1", 9.5);
        Horse m2 = horse(level, gy, x0 + 6.0, z0 + 2.0, Sex.FEMALE, false, "TAKEOVER MARE 2", 8.0);
        Horse m3 = horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.FEMALE, false, "TAKEOVER MARE 3", 7.0);
        band(stallion, BandType.TRADITIONAL, stallion, m1, m2, m3);
        Horse c1 = horse(level, gy, x0 + 8.5, z0 + 8.5, Sex.MALE, false, "CHALLENGER 1", 6.0);
        Horse c2 = horse(level, gy, x0 + 10.0, z0 + 10.0, Sex.MALE, false, "CHALLENGER 2", 5.0);
        Horse c3 = horse(level, gy, x0 + 6.5, z0 + 10.0, Sex.MALE, false, "CHALLENGER 3", 4.0);
        band(c1, BandType.BACHELOR, c1, c2, c3);
        seed(c1, c2, 0.6, 0.0, 0.0);
        seed(c1, c3, 0.6, 0.0, 0.0);
        seed(c2, c3, 0.6, 0.0, 0.0);
    }

    private static void damDefence(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, 8, "DAM DEFENCE", "DAM DEFENCE",
                List.of("DAM DEFENCE", "a zombie hurts the", "foal every 3 min:", "the dam kills it"));
        Horse dam = horse(level, gy, x0 + 2.5, z0 + 3, Sex.FEMALE, true, "DAM", 7.0);
        Horse foal = horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.FEMALE, true, "FOAL", 0.0);
        if (dam == null || foal == null) {
            return;
        }
        foal.setAge(-24000);
        HorseSocialAttachment s = foal.getData(ModAttachments.HORSE_SOCIAL.get());
        foal.setData(ModAttachments.HORSE_SOCIAL.get(), s.withBirth(level.getGameTime(),
                Optional.of(dam.getUUID()), Optional.empty(), 1.0));
        poke(level, dam.getUUID(), foal.getUUID(), 600, 1);
    }

    private static void poke(ServerLevel level, UUID damId, UUID foalId, long in, int n) {
        after(level, in, () -> {
            if (!(level.getEntity(foalId) instanceof Horse foal) || !foal.isAlive() || !foal.isBaby()) {
                return;
            }
            Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            if (zombie == null) {
                return;
            }
            // A helmet, or the yard's open sky burns it before anything else can.
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            zombie.setPersistenceRequired();
            zombie.setPos(foal.getX(), foal.getY(), foal.getZ() + 1.2);
            level.addFreshEntity(zombie);
            // WEAK ENOUGH TO LOSE. The first run, 2026-09-14: the dam went for a
            // full-health zombie four seconds after it hurt her foal, took five hits of
            // 3.0 and died with the zombie untouched - the arithmetic that killed the
            // guardian twice. A plain horse cannot win that fight, so the pen was
            // testing the fight, not whether she starts it. Two kicks' worth now.
            zombie.setHealth(4.0F);
            foal.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
            ActionTrace.log("test yard", "DAM DEFENCE #" + n + " of 5: a zombie hurt FOAL. Expect '[trace] herd | "
                    + "... went for zombie' within a second and '[watch] creature died | minecraft:zombie' soon after");
            UUID zombieId = zombie.getUUID();
            after(level, 400, () -> {
                boolean alive = level.getEntity(zombieId) instanceof Zombie z && z.isAlive();
                String target = level.getEntity(damId) instanceof Horse d && d.getTarget() != null
                        ? d.getTarget().getType().builtInRegistryHolder().key().identifier().getPath()
                        + (d.getTarget().isAlive() ? "" : " (dead)") : "nothing";
                ActionTrace.log("test yard", "DAM DEFENCE #" + n + ", 20 s later: zombie "
                        + (alive ? "STILL ALIVE" : "dead") + ", DAM targeting " + target);
            });
            if (n < 5) {
                poke(level, damId, foalId, 3600, n + 1);
            }
        });
    }

    // DISPLACEMENT (four ranked mares at hay, with its hay restock) was deleted on 2026-09-15 as
    // verified: overnight all 19 "displaced ... at food or water" lines put the higher rank first.

    // ------------------------------------------------------------------
    // Row Q east - grooming
    // ------------------------------------------------------------------

    /** A small pen, so the two mares and the gelding stand within grooming reach often. */
    private static void grooming(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, 7, "GROOMING", "GROOMING",
                List.of("GROOMING", "mares + gelding pair", "up in ~30 min;", "stallion never"));
        horse(level, gy, x0 + 2.0, z0 + 2.0, Sex.FEMALE, true, "GROOM MARE A", 6.0);
        horse(level, gy, x0 + 5.0, z0 + 2.0, Sex.FEMALE, true, "GROOM MARE B", 5.0);
        Horse gelding = horse(level, gy, x0 + 2.0, z0 + 5.0, Sex.MALE, true, "GROOM GELDING", 5.0);
        horse(level, gy, x0 + 5.0, z0 + 5.0, Sex.MALE, true, "GROOM STALLION (never)", 5.0);
        if (gelding != null) {
            HorseRecords.apply(gelding, HorseRecords.of(gelding).withGelded(true));
            DebugTestYard.label(gelding, "GROOM GELDING");     // applying a record clears the label
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String watchName,
                            String group, List<String> sign) {
        DebugTestYard.fencedPlot(level, gy, x0, x0 + width, z0, z0 + depth);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        DebugWorldWatch.watchSocial(watchName, DebugTestYard.box(x0, gy, z0, x0 + width, gy + 1, z0 + depth));
        YardPens.register(gy, x0, x0 + width, z0, z0 + depth, group);
    }

    /** A named horse, {@code adultDays} past growing up, with natural covers off if she is a mare. */
    private static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, boolean tamed,
                                         String name, double adultDays) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, CODE, tamed);
        DebugTestYard.label(h, name);
        if (h != null) {
            if (sex == Sex.FEMALE) {
                DebugYardFertility.noNaturalCovers(h);
            }
            born(h, adultDays, Optional.empty(), 0.0);
        }
        return h;
    }

    /**
     * Set a horse's age, and the band it was born into. Written before its first
     * social scan, so {@code HerdSocialHandler.ensureBorn} keeps it rather than guessing.
     */
    private static void born(@Nullable Horse h, double adultDays, Optional<UUID> natal, double disperseAfter) {
        if (h == null) {
            return;
        }
        long now = h.level().getGameTime();
        long bornTick = now - HerdRules.GROW_UP_TICKS - Math.round(adultDays * HerdRules.DAY_TICKS);
        HorseSocialAttachment s = h.getData(ModAttachments.HORSE_SOCIAL.get());
        h.setData(ModAttachments.HORSE_SOCIAL.get(), s.withBirth(bornTick, s.dam(), natal, disperseAfter));
    }

    private static void band(@Nullable Horse lead, BandType type, Horse... members) {
        if (lead == null) {
            return;
        }
        for (Horse m : members) {
            if (m != null) {
                m.setData(ModAttachments.HORSE_CARE.get(), m.getData(ModAttachments.HORSE_CARE.get())
                        .withWildHerd(lead.getUUID(), BREED, type.name()));
            }
        }
    }

    /** Both sides of a relationship; {@code rank} is how far {@code b} yields to {@code a}. */
    private static void seed(@Nullable Horse a, @Nullable Horse b, double familiarity, double rank, double grooming) {
        oneWay(a, b, familiarity, rank, grooming);
        oneWay(b, a, familiarity, -rank, grooming);
    }

    private static void oneWay(@Nullable Horse a, @Nullable Horse b, double familiarity, double rank, double grooming) {
        if (a == null || b == null) {
            return;
        }
        HorseSocialAttachment s = a.getData(ModAttachments.HORSE_SOCIAL.get());
        List<Relationship> list = new ArrayList<>();
        for (Relationship r : s.relationships()) {
            if (!r.other().equals(b.getUUID())) {
                list.add(r);
            }
        }
        list.add(new Relationship(b.getUUID(), familiarity, rank, grooming, 0.0, a.level().getGameTime()));
        a.setData(ModAttachments.HORSE_SOCIAL.get(), s.withLedger(SocialLedger.of(list), s.lastDecay()));
    }
}
