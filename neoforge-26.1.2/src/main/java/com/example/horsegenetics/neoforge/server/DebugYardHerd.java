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
import static com.example.horsegenetics.neoforge.server.DebugTestYard.HERD_LONG_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.HERD_ROW_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_U;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_V;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_W;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows U-X of the test yard: band life, with nobody at the controls</b>
 * (2026-09-14; owner: "make sure we have tests that work with me afk in game for the
 * new herd dynamics system").
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
 *   <tr><th>row</th><th>west (wild)</th><th>east</th></tr>
 *   <tr><td>U</td><td>LEAVING HOME - a filly due now, a colt due in 3 min</td>
 *       <td>HOST BAND (wild) - where the filly goes; two west mares may move in</td></tr>
 *   <tr><td>V</td><td>LEADERLESS - stallion killed at 1 min, bachelors arrive at 5</td>
 *       <td>SPARRING - three tamed stallions who know each other</td></tr>
 *   <tr><td>W</td><td>HERDING - one mare put 20+ blocks out every 4 min</td>
 *       <td>GROOMING (north) and DISPLACEMENT (south), tamed</td></tr>
 *   <tr><td>X</td><td>TAKEOVER - a band and three bachelors</td>
 *       <td>DAM DEFENCE - a zombie hurts a tamed foal every 3 min, five times</td></tr>
 * </table>
 *
 * <p>Rows are {@code DebugTestYard.HERD_GAP} apart because a wild horse's reach does
 * not stop at a fence. Mares have natural covers switched off here, so no heat pulls a
 * stallion off what he is being tested for.
 */
@EventBusSubscriber
final class DebugYardHerd {

    private DebugYardHerd() {
    }

    private static final int W = 18;
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

    private static void after(ServerLevel level, long ticks, Runnable run) {
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

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        cancel();
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            leavingHome(level, gy, west, east, mouthZ + ROW_U);
            leaderless(level, gy, west, mouthZ + ROW_V);
            sparring(level, gy, east, mouthZ + ROW_V);
            herding(level, gy, west, mouthZ + ROW_W);
            grooming(level, gy, east, mouthZ + ROW_W);
            displacement(level, gy, east, mouthZ + ROW_W + 20);
            takeover(level, gy, west, mouthZ + ROW_X);
            damDefence(level, gy, east, mouthZ + ROW_X);
            ActionTrace.log("test yard", "herd rows U-X built");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: herd rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row U - dispersal, and mare transfers
    // ------------------------------------------------------------------

    /**
     * The filly is due to leave at once and the colt three minutes later, <b>on
     * purpose</b>: a filly takes the nearest bachelor over the nearest band, and a
     * brother who left first is the nearest bachelor there is. The two west mares know
     * the host band's mares already (their side only), so either may move over.
     */
    private static void leavingHome(ServerLevel level, int gy, int west, int east, int z0) {
        pen(level, gy, west, z0, W, HERD_ROW_D, "LEAVING HOME",
                List.of("LEAVING HOME", "filly leaves now,", "colt in 3 min;", "mares may move E"));
        Horse sire = horse(level, gy, west + 4.5, z0 + 5, Sex.MALE, false, "NATAL STALLION", 9.0);
        Horse m1 = horse(level, gy, west + 8.5, z0 + 4, Sex.FEMALE, false, "MOVER MARE 1", 8.5);
        Horse m2 = horse(level, gy, west + 10.5, z0 + 7, Sex.FEMALE, false, "MOVER MARE 2", 8.0);
        Horse filly = horse(level, gy, west + 12.5, z0 + 5, Sex.FEMALE, false, "FILLY: LEAVES NOW", 0.0);
        Horse colt = horse(level, gy, west + 14.5, z0 + 9, Sex.MALE, false, "COLT: LEAVES AT 3M", 0.0);
        band(sire, BandType.TRADITIONAL, sire, m1, m2, filly, colt);
        if (sire != null) {
            Optional<UUID> natal = Optional.of(sire.getUUID());
            // Adult days lived = the day she is due to leave, so the first scan sends her.
            born(filly, 3.05, natal, 3.0);
            // ...and him three minutes short of his.
            born(colt, 1.0 - 3.0 * 1200.0 / HerdRules.DAY_TICKS, natal, 1.0);
        }

        pen(level, gy, east, z0, W, HERD_ROW_D, "HOST BAND",
                List.of("HOST BAND", "the filly joins", "this band; W mares", "may move here"));
        Horse host = horse(level, gy, east + 12.5, z0 + 5, Sex.MALE, false, "HOST STALLION", 9.0);
        Horse h1 = horse(level, gy, east + 4.5, z0 + 5, Sex.FEMALE, false, "HOST MARE 1", 8.5);
        Horse h2 = horse(level, gy, east + 6.5, z0 + 9, Sex.FEMALE, false, "HOST MARE 2", 8.0);
        band(host, BandType.TRADITIONAL, host, h1, h2);
        for (Horse mover : new Horse[]{m1, m2}) {
            for (Horse friend : new Horse[]{h1, h2}) {
                oneWay(mover, friend, 0.9, 0.0, 0.5);
            }
        }
    }

    // ------------------------------------------------------------------
    // Row V - a band that loses its stallion; sparring
    // ------------------------------------------------------------------

    private static void leaderless(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, HERD_ROW_D, "LEADERLESS",
                List.of("LEADERLESS BAND", "stallion dies 1m:", "mares stay ONE band", "bachelors at 5m"));
        Horse stallion = horse(level, gy, x0 + 9.5, z0 + 7, Sex.MALE, false, "DOOMED STALLION", 9.0);
        Horse lead = horse(level, gy, x0 + 4.5, z0 + 4, Sex.FEMALE, false, "ELDEST MARE", 9.5);
        Horse m2 = horse(level, gy, x0 + 6.5, z0 + 8, Sex.FEMALE, false, "BAND MARE 2", 7.0);
        Horse m3 = horse(level, gy, x0 + 12.5, z0 + 5, Sex.FEMALE, false, "BAND MARE 3", 6.0);
        band(stallion, BandType.TRADITIONAL, stallion, lead, m2, m3);
        if (stallion == null) {
            return;
        }
        UUID stallionId = stallion.getUUID();
        AABB inside = DebugTestYard.box(x0, gy, z0, x0 + W, gy + 3, z0 + HERD_ROW_D);

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
            Horse a = horse(level, gy, x0 + 15.5, z0 + 11, Sex.MALE, false, "BACHELOR A", 5.0);
            Horse b = horse(level, gy, x0 + 14.5, z0 + 12, Sex.MALE, false, "BACHELOR B", 4.0);
            band(a, BandType.BACHELOR, a, b);
            seed(a, b, 0.6, 0.0, 0.0);
            ActionTrace.log("test yard", "LEADERLESS: two bachelors put in with the vacant band. Expect, "
                    + "usually within the hour: '[trace] herd | BACHELOR ... took over band' with NO fight, "
                    + "then the other bachelor re-formed round himself, and later real challenges");
        });
    }

    private static void sparring(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, HERD_ROW_D, "SPARRING",
                List.of("SPARRING", "3 tame stallions:", "rear, shove, yield", "NO damage, ever"));
        Horse a = horse(level, gy, x0 + 4.5, z0 + 5, Sex.MALE, true, "SPAR A", 6.0);
        Horse b = horse(level, gy, x0 + 9.5, z0 + 8, Sex.MALE, true, "SPAR B", 5.0);
        Horse c = horse(level, gy, x0 + 13.5, z0 + 5, Sex.MALE, true, "SPAR C", 4.0);
        seed(a, b, 0.6, 0.0, 0.0);
        seed(a, c, 0.6, 0.0, 0.0);
        seed(b, c, 0.6, 0.0, 0.0);
    }

    // ------------------------------------------------------------------
    // Row W - herding; grooming; displacement
    // ------------------------------------------------------------------

    /**
     * A lead let go twenty blocks out, done by the clock. The stray is never the lead
     * mare, whom everyone follows - so she may well walk back on her own before the
     * stallion moves, and the log says which: a {@code went to herd} line, or none.
     */
    private static void herding(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, HERD_LONG_D, "HERDING",
                List.of("HERDING", "a mare is put far", "off every 4 min:", "who brings her?"));
        Horse stallion = horse(level, gy, x0 + 12.5, z0 + 6, Sex.MALE, false, "HERD STALLION", 9.0);
        Horse lead = horse(level, gy, x0 + 5.5, z0 + 4, Sex.FEMALE, false, "HERD LEAD MARE", 9.5);
        Horse m2 = horse(level, gy, x0 + 8.5, z0 + 7, Sex.FEMALE, false, "HERD MARE", 7.0);
        Horse stray = horse(level, gy, x0 + 10.5, z0 + 4, Sex.FEMALE, false, "STRAY MARE", 5.0);
        band(stallion, BandType.TRADITIONAL, stallion, lead, m2, stray);
        seed(lead, m2, 0.8, 0.0, 0.0);
        seed(lead, stray, 0.8, 0.0, 0.0);
        seed(m2, stray, 0.8, 0.0, 0.0);
        DebugYardGameplay.chest(level, gy, x0 + 4, z0 - 2, "HERDING", List.of(new ItemStack(Items.LEAD, 2)));
        if (stray != null) {
            strayLoop(level, gy, x0, z0, stray.getUUID(), 1200, 1);
        }
    }

    private static void strayLoop(ServerLevel level, int gy, int x0, int z0, UUID strayId, long in, int n) {
        after(level, in, () -> {
            if (!(level.getEntity(strayId) instanceof Horse stray) || !stray.isAlive()) {
                return;
            }
            stray.getNavigation().stop();
            stray.teleportTo(x0 + 9.5, gy + 1, z0 + HERD_LONG_D - 3.5);
            ActionTrace.log("test yard", "HERDING #" + n + ": put STRAY MARE "
                    + String.format("%.0f", distanceFromBand(level, stray)) + " blocks from her band");
            after(level, 1200, () -> {
                if (level.getEntity(strayId) instanceof Horse s && s.isAlive()) {
                    double d = distanceFromBand(level, s);
                    ActionTrace.log("test yard", "HERDING #" + n + ", a minute later: STRAY MARE is "
                            + String.format("%.0f", d) + " blocks from her band - "
                            + (d < 8.0 ? "back" : "still out"));
                }
            });
            strayLoop(level, gy, x0, z0, strayId, 4800, n + 1);
        });
    }

    private static double distanceFromBand(ServerLevel level, Horse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (care.herd().isEmpty()) {
            return -1.0;
        }
        double x = 0;
        double z = 0;
        int n = 0;
        for (Horse m : HerdSocialHandler.bandMembers(level, care.herd().get(), horse)) {
            if (m != horse) {
                x += m.getX();
                z += m.getZ();
                n++;
            }
        }
        return n == 0 ? -1.0 : horse.position().subtract(new Vec3(x / n, horse.getY(), z / n)).horizontalDistance();
    }

    /** A small pen, so the two mares and the gelding stand within grooming reach often. */
    private static void grooming(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, 9, "GROOMING",
                List.of("GROOMING", "mares + gelding pair", "up in ~30 min;", "stallion never"));
        horse(level, gy, x0 + 2.5, z0 + 3, Sex.FEMALE, true, "GROOM MARE A", 6.0);
        horse(level, gy, x0 + 6.5, z0 + 3, Sex.FEMALE, true, "GROOM MARE B", 5.0);
        Horse gelding = horse(level, gy, x0 + 2.5, z0 + 7, Sex.MALE, true, "GROOM GELDING", 5.0);
        horse(level, gy, x0 + 6.5, z0 + 7, Sex.MALE, true, "GROOM STALLION (never)", 5.0);
        if (gelding != null) {
            HorseRecords.apply(gelding, HorseRecords.of(gelding).withGelded(true));
        }
    }

    private static void displacement(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, 10, "DISPLACEMENT",
                List.of("DISPLACEMENT", "4 ranked mares, hay:", "a higher one moves", "a lower one off"));
        Horse[] mares = {
                horse(level, gy, x0 + 4.5, z0 + 3, Sex.FEMALE, true, "RANK 1 MARE", 9.0),
                horse(level, gy, x0 + 13.5, z0 + 3, Sex.FEMALE, true, "RANK 2 MARE", 8.0),
                horse(level, gy, x0 + 4.5, z0 + 7, Sex.FEMALE, true, "RANK 3 MARE", 7.0),
                horse(level, gy, x0 + 13.5, z0 + 7, Sex.FEMALE, true, "RANK 4 MARE", 6.0)};
        for (int i = 0; i < mares.length; i++) {
            for (int j = i + 1; j < mares.length; j++) {
                seed(mares[i], mares[j], 0.8, 0.6, 0.0);
            }
        }
        for (int x = x0 + 7; x <= x0 + 11; x += 2) {
            level.setBlock(new BlockPos(x, gy + 1, z0 + 5), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------
    // Row X - a takeover; a dam defending her foal
    // ------------------------------------------------------------------

    private static void takeover(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, HERD_ROW_D, "TAKEOVER",
                List.of("TAKEOVER", "band + 3 bachelors:", "fights stop at 40%", "NOBODY dies"));
        Horse stallion = horse(level, gy, x0 + 5.5, z0 + 4, Sex.MALE, false, "BAND STALLION", 9.0);
        Horse m1 = horse(level, gy, x0 + 3.5, z0 + 3, Sex.FEMALE, false, "TAKEOVER MARE 1", 9.5);
        Horse m2 = horse(level, gy, x0 + 7.5, z0 + 3, Sex.FEMALE, false, "TAKEOVER MARE 2", 8.0);
        Horse m3 = horse(level, gy, x0 + 5.5, z0 + 6, Sex.FEMALE, false, "TAKEOVER MARE 3", 7.0);
        band(stallion, BandType.TRADITIONAL, stallion, m1, m2, m3);
        Horse c1 = horse(level, gy, x0 + 13.5, z0 + 10, Sex.MALE, false, "CHALLENGER 1", 6.0);
        Horse c2 = horse(level, gy, x0 + 15.5, z0 + 11, Sex.MALE, false, "CHALLENGER 2", 5.0);
        Horse c3 = horse(level, gy, x0 + 11.5, z0 + 12, Sex.MALE, false, "CHALLENGER 3", 4.0);
        band(c1, BandType.BACHELOR, c1, c2, c3);
        seed(c1, c2, 0.6, 0.0, 0.0);
        seed(c1, c3, 0.6, 0.0, 0.0);
        seed(c2, c3, 0.6, 0.0, 0.0);
    }

    private static void damDefence(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, HERD_ROW_D, "DAM DEFENCE",
                List.of("DAM DEFENCE", "a zombie hurts the", "foal every 3 min:", "the dam kills it"));
        Horse dam = horse(level, gy, x0 + 6.5, z0 + 6, Sex.FEMALE, true, "DAM", 7.0);
        Horse foal = horse(level, gy, x0 + 9.5, z0 + 6, Sex.FEMALE, true, "FOAL", 0.0);
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
            zombie.setPos(foal.getX() + 1.5, foal.getY(), foal.getZ());
            level.addFreshEntity(zombie);
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String watchName,
                            List<String> sign) {
        DebugTestYard.fencedPlot(level, gy, x0, x0 + width, z0, z0 + depth);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        DebugWorldWatch.watchSocial(watchName, DebugTestYard.box(x0, gy, z0, x0 + width, gy + 1, z0 + depth));
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
