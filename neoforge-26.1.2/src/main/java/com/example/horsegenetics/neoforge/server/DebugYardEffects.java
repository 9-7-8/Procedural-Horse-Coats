package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AB;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AB_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AC;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AC_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AD;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AD_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows AB-AD: the five effect genes nobody has watched</b> - mob aura, swim speed, water breathing, on death and
 * item drop (known-gaps gap 121: "have never run in a game"). None of their handlers writes a log line, so every pen
 * here logs its own readings and, where the answer is a yes or a no, PASS or FAIL.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>AB</td><td>WARD - Wrd/Wrd and three husks</td><td>WARD CONTROL - a plain horse and three husks</td></tr>
 *   <tr><td>AC</td><td>BAIT, BAIT CONTROL - does a husk come for the horse</td>
 *       <td>SWIM SPEED - laps of a channel; BREATH - three horses under a lid</td></tr>
 *   <tr><td>AD</td><td>DEATH LAVA, DEATH WATER - on a built floor</td>
 *       <td>DEATH BOOM - Xpl + Dia beside a witness; DEATH ITEMS - Egg and Swd</td></tr>
 * </table>
 */
final class DebugYardEffects {

    private DebugYardEffects() {
    }

    private static final String PLAIN = "horsegenetics.scn4a=N/N";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            aura(level, gy, west, mouthZ + ROW_AB, "WARD", "horsegenetics.magic_mob_aura=Wrd/Wrd");
            aura(level, gy, east, mouthZ + ROW_AB, "WARD CONTROL", PLAIN);
            baitPen(level, gy, west, mouthZ + ROW_AC, "BAIT", "horsegenetics.magic_mob_aura=Bai/Bai");
            baitPen(level, gy, west + 9, mouthZ + ROW_AC, "BAIT CONTROL", PLAIN);
            swimLaps(level, gy, east, mouthZ + ROW_AC);
            breath(level, gy, east + 9, mouthZ + ROW_AC);
            deathFluid(level, gy, west, mouthZ + ROW_AD, "DEATH LAVA", "Lav/Lav", Blocks.LAVA);
            deathFluid(level, gy, west + 9, mouthZ + ROW_AD, "DEATH WATER", "Wat/Wat", Blocks.WATER);
            deathBoom(level, gy, east, mouthZ + ROW_AD);
            deathItems(level, gy, east + 9, mouthZ + ROW_AD);
            ActionTrace.log("test yard", "effect pens built (rows AB-AD: ward, bait, swim speed, water breathing,"
                    + " on death, item drop)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AB-AD failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row AB: mob aura, ward
    // ------------------------------------------------------------------

    /** Ward pushes every hostile within ten blocks away; the control pen is where husks wander on their own. */
    private static void aura(ServerLevel level, int gy, int x0, int z0, String name, String code) {
        DebugYardUnattended.pen(level, gy, x0, z0, 18, ROW_AB_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, code.equals(PLAIN) ? "a plain horse and" : "a Wrd/Wrd horse and",
                        "three husks: how", "far do they keep?"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 6.5, Sex.MALE, code, true, name);
        List<Entity> husks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Entity e = DebugYardUnattended.animal(level, EntityType.HUSK, gy, x0 + 7.5 + i, z0 + 3.5 + i * 3);
            if (e != null) {
                husks.add(e);
            }
        }
        distances(level, name, h, husks, 1);
    }

    private static void distances(ServerLevel level, String name, @Nullable Horse h, List<Entity> husks, int n) {
        DebugYardHerd.after(level, n <= 12 ? 100 : 2_400, () -> {
            if (h == null || !h.isAlive()) {
                ActionTrace.log("test yard", name + ": the horse is gone - no more readings");
                return;
            }
            StringBuilder sb = new StringBuilder();
            double sum = 0.0;
            int alive = 0;
            for (Entity m : husks) {
                if (!m.isAlive()) {
                    continue;
                }
                double d = Math.sqrt(m.distanceToSqr(h));
                sb.append(String.format("%.1f ", d));
                sum += d;
                alive++;
            }
            ActionTrace.log("test yard", name + " reading " + n + ": husks at " + sb
                    + String.format("blocks (mean %.1f, %d alive)", alive == 0 ? 0.0 : sum / alive, alive)
                    + (n == 12 ? " - a minute in: WARD expects every husk past 10 blocks and a mean well above"
                    + " WARD CONTROL's" : ""));
            if (n < 40) {
                distances(level, name, h, husks, n + 1);
            }
        });
    }

    // ------------------------------------------------------------------
    // Row AC: bait, swim speed, water breathing
    // ------------------------------------------------------------------

    /** Bait makes a hostile that can see the horse target it; a husk ignores a plain horse. */
    private static void baitPen(ServerLevel level, int gy, int x0, int z0, String name, String code) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AC_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "a husk six blocks", "off: does it come", "for the horse?"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 3.5, Sex.MALE, code, true, name);
        Entity husk = DebugYardUnattended.animal(level, EntityType.HUSK, gy, x0 + 4.5, z0 + 9.5);
        baitWatch(level, name, h, husk, 1, false);
    }

    private static void baitWatch(ServerLevel level, String name, @Nullable Horse h, @Nullable Entity husk, int n,
                                  boolean everTargeted) {
        DebugYardHerd.after(level, 100, () -> {
            boolean targeting = husk instanceof Mob m && m.isAlive() && h != null && m.getTarget() == h;
            boolean ever = everTargeted || targeting;
            String hp = h == null ? "gone" : h.isAlive()
                    ? String.format("%.0f/%.0f", h.getHealth(), h.getMaxHealth()) : "DEAD";
            double d = h == null || husk == null ? -1.0 : Math.sqrt(husk.distanceToSqr(h));
            boolean last = n >= 12 || h == null || !h.isAlive();
            String verdict = "";
            if (last) {
                boolean want = name.equals("BAIT");
                verdict = " - after " + n * 5 + " s the husk " + (ever ? "DID" : "never") + " go for the horse; "
                        + (want ? "expect it did" : "expect it never did") + ": " + (ever == want ? "PASS" : "FAIL");
            }
            ActionTrace.log("test yard", name + " reading " + n + ": husk " + (targeting ? "TARGETING the horse"
                    : "not targeting it") + String.format(", %.1f blocks", d) + ", horse hp " + hp + verdict);
            if (!last) {
                baitWatch(level, name, h, husk, n + 1, ever);
            }
        });
    }

    /**
     * Gap 249: magic swim speed scales the horse's own push through water ({@link SwimScaling}). The first pen read the
     * {@code water_movement_efficiency} attribute the gene used to multiply and found it 0.000 on every genotype; this
     * one measures swimming itself. Three horses in their own walled channel, two wide and two deep - Otter, plain and
     * Stone - are sent to the far end and back by their navigation, and the pen logs how far each swam in two seconds.
     */
    private static void swimLaps(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AC_D, "SWIM SPEED", Blocks.STONE.defaultBlockState(),
                List.of("SWIM SPEED", "Otr, plain, Stne", "swim laps: blocks", "in two seconds"));
        for (int x = x0 + 1; x <= x0 + 8; x++) {
            boolean channel = (x - x0 - 1) % 3 != 2;       // channels at x0+1..2, x0+4..5, x0+7..8; stone between
            for (int z = z0 + 1; z < z0 + ROW_AC_D; z++) {
                for (int y = gy - 2; y <= gy + 2; y++) {
                    BlockState st = y == gy - 2 || !channel ? Blocks.STONE.defaultBlockState()
                            : y <= gy ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), st, 3);
                }
            }
        }
        String[] codes = {"horsegenetics.magic_swim_speed=Otr/Otr", PLAIN, "horsegenetics.magic_swim_speed=Stne/Stne"};
        String[] names = {"SWIM OTTER", "SWIM PLAIN", "SWIM STONE"};
        double[] xs = {x0 + 2.0, x0 + 5.0, x0 + 8.0};
        Horse[] horses = new Horse[3];
        for (int i = 0; i < 3; i++) {
            horses[i] = DebugPenManager.spawnHorse(level, gy, xs[i], z0 + 1.5, Sex.FEMALE, codes[i], true);
            DebugTestYard.label(horses[i], names[i]);
        }
        lap(level, horses, names, xs, z0, 1, new double[3]);
    }

    /**
     * Steer every horse at the far end through its move control, re-aimed every tick for {@code ticks} ticks. The first
     * version gave each horse a navigation path, and the plain and Stone horses bobbed in place (0.15 blocks in two
     * seconds, every lap): a floating horse does not follow a ground path. The move control is what turns a wanted
     * position into the horse's own forward push, which is exactly the push {@link SwimScaling} scales.
     */
    private static void drive(ServerLevel level, Horse[] horses, double[] xs, double targetZ, int ticks) {
        if (ticks <= 0) {
            return;
        }
        for (int i = 0; i < horses.length; i++) {
            Horse h = horses[i];
            if (h != null && h.isAlive()) {
                h.getMoveControl().setWantedPosition(xs[i], h.getY(), targetZ, 1.0);
            }
        }
        DebugYardHerd.after(level, 1, () -> drive(level, horses, xs, targetZ, ticks - 1));
    }

    private static void lap(ServerLevel level, Horse[] horses, String[] names, double[] xs, int z0, int n, double[] total) {
        DebugYardHerd.after(level, n == 1 ? 200 : 60, () -> {
            double targetZ = n % 2 == 1 ? z0 + ROW_AC_D - 1.5 : z0 + 1.5;
            double[] start = new double[horses.length];
            for (int i = 0; i < horses.length; i++) {
                Horse h = horses[i];
                if (h != null && h.isAlive()) {
                    start[i] = h.getZ();
                }
            }
            drive(level, horses, xs, targetZ, 100);
            DebugYardHerd.after(level, 101, () -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < horses.length; i++) {
                    Horse h = horses[i];
                    if (h == null || !h.isAlive()) {
                        sb.append(sb.length() == 0 ? "" : "; ").append(names[i]).append(" gone");
                        continue;
                    }
                    double d = Math.abs(h.getZ() - start[i]);
                    total[i] += d;
                    // Where it is and what it is doing, because the first two runs had PLAIN and STONE at exactly 0.15
                    // blocks every lap, which is not swimming.
                    sb.append(sb.length() == 0 ? "" : "; ").append(names[i]).append(String.format(
                            " %.2f (y %.2f, %s%s%s, moving %.3f/tick)", d, h.getY(), h.isInWater() ? "in water" : "NOT IN WATER",
                            h.onGround() ? ", on ground" : "", h.isImmobile() ? ", IMMOBILE" : "",
                            h.getDeltaMovement().horizontalDistance()));
                }
                String verdict = "";
                if (n == 8) {
                    // Clear margins, and a plain horse that really moved: the 10:09 run "passed" on 1.2 against 1.2. A
                    // horse swimming under its own AI is slow - vanilla's push in water is tiny - so "moved" is half a
                    // block over the eight five-second laps, not a sprint.
                    boolean swam = total[1] > 0.5;
                    boolean order = total[0] > total[1] * 1.15 && total[1] > total[2] * 1.15;
                    verdict = String.format(" | totals over 8 laps: OTTER %.2f, PLAIN %.2f, STONE %.2f - expect OTTER"
                            + " clearly above PLAIN clearly above STONE, with PLAIN over half a block: %s", total[0], total[1],
                            total[2], !swam ? "NO RESULT (the plain horse did not swim)" : order ? "PASS" : "FAIL");
                }
                ActionTrace.log("test yard", "SWIM SPEED lap " + n + " (blocks in 5 s): " + sb + verdict);
                if (n < 8) {
                    lap(level, horses, names, xs, z0, n + 1, total);
                }
            });
        });
    }

    /**
     * Water breathing stretches or shortens a horse's air while its head is under. Three horses are held under a stone
     * lid, each in its own 2x2 cell, and the pen logs their air and the tick each first takes drowning damage. The
     * suspicion: a {@code Shal} horse's extra drain can step past -20, the one value vanilla drowns at, so it never
     * drowns at all.
     */
    private static void breath(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AC_D, "BREATH", Blocks.STONE.defaultBlockState(),
                List.of("BREATH", "Gil, Shal, plain:", "held under a lid,", "who drowns first"));
        for (int x = x0 + 1; x <= x0 + 8; x++) {
            for (int z = z0 + 3; z <= z0 + 6; z++) {
                for (int y = gy - 4; y <= gy; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        // THE LID IS A WATERLOGGED TOP SLAB, NOT STONE (08:47 run). Under a solid lid the top water block's surface sits
        // 0.11 below it, and a horse floating up against the lid has its eye 0.08 below the lid - above the water, so
        // all three read HEAD OUT at full air for a minute. Fluid above fluid fills the block below to the brim, and the
        // slab's open lower half is water too, so the horse's head stays under while the slab still stops it rising.
        BlockState lid = Blocks.STONE_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        int[] cellX = {x0 + 1, x0 + 4, x0 + 7};
        for (int cx : cellX) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = gy - 3; y <= gy - 1; y++) {
                        level.setBlock(new BlockPos(cx + dx, y, z0 + 4 + dz), Blocks.WATER.defaultBlockState(), 3);
                    }
                    level.setBlock(new BlockPos(cx + dx, gy, z0 + 4 + dz), lid, 3);
                }
            }
        }
        String[] codes = {"horsegenetics.magic_water_breathing=Gil/Gil", "horsegenetics.magic_water_breathing=Shal/Shal", PLAIN};
        String[] names = {"BREATH GILLS", "BREATH SHALLOW", "BREATH PLAIN"};
        Horse[] horses = new Horse[3];
        for (int i = 0; i < 3; i++) {
            horses[i] = DebugPenManager.spawnHorse(level, gy - 3, cellX[i] + 1.0, z0 + 5.0, Sex.FEMALE, codes[i], true);
            DebugTestYard.label(horses[i], names[i]);
        }
        long start = level.getGameTime();
        drown(level, gy, horses, names, new long[]{-1, -1, -1}, new int[]{300, 300, 300}, start, 1);
    }

    private static void drown(ServerLevel level, int gy, Horse[] horses, String[] names, long[] firstHurt, int[] minAir,
                              long start, int n) {
        DebugYardHerd.after(level, 20, () -> {
            long t = level.getGameTime() - start;
            boolean anyAlive = false;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < horses.length; i++) {
                Horse h = horses[i];
                if (h == null) {
                    continue;
                }
                if (h.isAlive()) {
                    anyAlive = true;
                    minAir[i] = Math.min(minAir[i], h.getAirSupply());
                    if (firstHurt[i] < 0 && h.getHealth() < h.getMaxHealth()) {
                        firstHurt[i] = t;
                    }
                }
                // Where it is and what its eyes are in (the 08:40 run read HEAD OUT for all three, all minute).
                BlockPos eye = BlockPos.containing(h.getX(), h.getEyeY(), h.getZ());
                sb.append(sb.length() == 0 ? "" : "; ").append(names[i]).append(h.isAlive()
                        ? String.format(" air %d hp %.0f%s (feet y %+.2f, eye y %+.2f in %s)", h.getAirSupply(),
                        h.getHealth(), h.isUnderWater() ? "" : " HEAD OUT", h.getY() - gy, h.getEyeY() - gy,
                        blockId(level.getBlockState(eye)))
                        : " DEAD");
            }
            boolean done = !anyAlive || t >= 1_600;
            if (n % 5 == 0 || done) {
                ActionTrace.log("test yard", "BREATH at tick " + t + ": " + sb);
            }
            if (done) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < horses.length; i++) {
                    s.append(s.length() == 0 ? "" : ", ").append(names[i]).append(" first hurt at ")
                            .append(firstHurt[i] < 0 ? "NEVER" : String.valueOf(firstHurt[i])).append(" (lowest air ")
                            .append(minAir[i]).append(')');
                }
                boolean order = firstHurt[1] >= 0 && firstHurt[2] >= 0 && firstHurt[0] >= 0
                        && firstHurt[1] < firstHurt[2] && firstHurt[2] < firstHurt[0];
                ActionTrace.log("test yard", "BREATH summary: " + s + " - expect SHALLOW before PLAIN before GILLS: "
                        + (order ? "PASS" : "FAIL") + "; a horse whose air fell below -20 and was never hurt never drowns");
                return;
            }
            drown(level, gy, horses, names, firstHurt, minAir, start, n + 1);
        });
    }

    // ------------------------------------------------------------------
    // Row AD: on death, item drop
    // ------------------------------------------------------------------

    /** Lava or water where the horse fell, on a floor somebody built - and the floor must survive it. */
    private static void deathFluid(ServerLevel level, int gy, int x0, int z0, String name, String allele, Block fluid) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AD_D, name, Blocks.STONE.defaultBlockState(),
                List.of(name, allele + " horse on", "a built floor: the", "fluid, floor kept"), fluid);
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.MALE,
                "horsegenetics.magic_on_death=" + allele, true, name);
        DebugYardHerd.after(level, 200, () -> {
            if (h == null || !h.isAlive()) {
                ActionTrace.log("test yard", name + ": the horse was gone before its death - nothing to test");
                return;
            }
            BlockPos at = h.blockPosition();
            ActionTrace.log("test yard", name + ": killing the horse at " + at.toShortString());
            h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            DebugYardHerd.after(level, 40, () -> {
                BlockState st = level.getBlockState(at);
                boolean placed = st.is(fluid);
                boolean floorKept = level.getBlockState(at.below()).is(Blocks.STONE);
                AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AD_D);
                ActionTrace.log("test yard", name + " at 2 s: the block where it fell is " + blockId(st)
                        + ", the floor under it " + (floorKept ? "still stone" : "GONE (" + blockId(level.getBlockState(at.below())) + ")")
                        + ", items " + items(level, box) + " - " + (placed && floorKept ? "PASS" : "FAIL"));
                // Clear the fluid so the pen does not flow into its neighbours for the rest of the run.
                for (int x = x0 + 1; x < x0 + 9; x++) {
                    for (int z = z0 + 1; z < z0 + ROW_AD_D; z++) {
                        for (int y = gy + 1; y <= gy + 2; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!level.getBlockState(p).getFluidState().isEmpty()) {
                                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            });
        });
    }

    /** A horse that is volatile and diamond-bearing: a creeper's blast, then 2 to 5 diamonds instead of leather. */
    private static void deathBoom(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AD_D, "DEATH BOOM", Blocks.STONE.defaultBlockState(),
                List.of("DEATH BOOM", "Xpl + Dia horse", "and a witness: a", "blast, diamonds"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 4.5, Sex.MALE,
                "horsegenetics.magic_on_death=Xpl/Xpl-horsegenetics.magic_item_drop=Dia/Dia", true, "DEATH BOOM");
        Horse witness = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 8.0, Sex.FEMALE, PLAIN, true, "BOOM WITNESS");
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AD_D);
        DebugYardHerd.after(level, 260, () -> {
            if (h == null || !h.isAlive()) {
                ActionTrace.log("test yard", "DEATH BOOM: the horse was gone before its death - nothing to test");
                return;
            }
            float before = witness == null ? -1.0F : witness.getHealth();
            double apart = witness == null ? -1.0 : Math.sqrt(witness.distanceToSqr(h));
            ActionTrace.log("test yard", "DEATH BOOM: killing the horse at " + h.blockPosition().toShortString()
                    + String.format(", the witness %.1f blocks away", apart));
            h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            DebugYardHerd.after(level, 40, () -> {
                Map<String, Integer> found = itemCounts(level, box);
                int diamonds = found.getOrDefault("minecraft:diamond", 0);
                int leather = found.getOrDefault("minecraft:leather", 0);
                String w = witness == null ? "none" : witness.isAlive()
                        ? String.format("%.1f -> %.1f", before, witness.getHealth()) : "DEAD";
                ActionTrace.log("test yard", "DEATH BOOM at 2 s: items " + found + ", witness hp " + w
                        + " - 2 to 5 diamonds and no leather: " + (diamonds >= 2 && diamonds <= 5 && leather == 0 ? "PASS" : "FAIL")
                        + "; also expect a '[watch] EXPLOSION' line");
                DebugYardHerd.after(level, 100, () -> {
                    // Put back what the blast took, so the pen still holds its witness.
                    for (int x = x0 + 1; x < x0 + 9; x++) {
                        for (int z = z0 + 1; z < z0 + ROW_AD_D; z++) {
                            DebugPenManager.groundColumn(level, x, gy, z, Blocks.STONE.defaultBlockState());
                        }
                    }
                    DebugTestYard.fencedPlot(level, gy, x0, x0 + 9, z0, z0 + ROW_AD_D);
                    ActionTrace.log("test yard", "DEATH BOOM: floor and walls rebuilt after the blast");
                });
            });
        });
    }

    /** Egg drops a preset spawn egg of itself, Swd an enchanted iron sword - each instead of leather. */
    private static void deathItems(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AD_D, "DEATH ITEMS", Blocks.STONE.defaultBlockState(),
                List.of("DEATH ITEMS", "Egg and Swd horses:", "an egg, a sword,", "never leather"));
        Horse egg = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.MALE,
                "horsegenetics.magic_item_drop=Egg/Egg", true, "DEATH EGG");
        Horse sword = DebugYardUnattended.horse(level, gy, x0 + 6.5, z0 + 5.5, Sex.MALE,
                "horsegenetics.magic_item_drop=Swd/Swd", true, "DEATH SWORD");
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AD_D);
        DebugYardHerd.after(level, 300, () -> {
            for (Horse h : new Horse[]{egg, sword}) {
                if (h != null && h.isAlive()) {
                    h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
                }
            }
            DebugYardHerd.after(level, 40, () -> {
                int leather = 0;
                int swords = 0;
                int enchanted = 0;
                int eggs = 0;
                StringBuilder all = new StringBuilder();
                for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box.inflate(1.0, 3.0, 1.0))) {
                    ItemStack st = ie.getItem();
                    String id = st.getItem().builtInRegistryHolder().key().identifier().toString();
                    all.append(all.length() == 0 ? "" : ", ").append(st.getCount()).append("x ").append(id)
                            .append(st.isEnchanted() ? " (enchanted)" : "");
                    if (id.equals("minecraft:leather")) {
                        leather += st.getCount();
                    } else if (id.equals("minecraft:iron_sword")) {
                        swords++;
                        if (st.isEnchanted()) {
                            enchanted++;
                        }
                    } else if (id.contains("egg")) {
                        eggs++;
                    }
                }
                boolean pass = leather == 0 && eggs == 1 && swords == 1 && enchanted == 1;
                ActionTrace.log("test yard", "DEATH ITEMS at 2 s: " + all + " - one egg, one enchanted iron sword,"
                        + " no leather: " + (pass ? "PASS" : "FAIL") + " (the egg's clone is a tester's check)");
            });
        });
    }

    private static Map<String, Integer> itemCounts(ServerLevel level, AABB box) {
        Map<String, Integer> out = new TreeMap<>();
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box.inflate(1.0, 3.0, 1.0))) {
            out.merge(ie.getItem().getItem().builtInRegistryHolder().key().identifier().toString(),
                    ie.getItem().getCount(), Integer::sum);
        }
        return out;
    }

    private static String items(ServerLevel level, AABB box) {
        Map<String, Integer> m = itemCounts(level, box);
        return m.isEmpty() ? "none" : m.toString();
    }

    private static String blockId(BlockState st) {
        return st.getBlock().builtInRegistryHolder().key().identifier().toString();
    }
}
