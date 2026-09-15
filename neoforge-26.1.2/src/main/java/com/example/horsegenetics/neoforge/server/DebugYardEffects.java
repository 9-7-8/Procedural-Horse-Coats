package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 *       <td>SWIM SPEED - the attribute; BREATH - three horses under a lid</td></tr>
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
            swim(level, gy, east, mouthZ + ROW_AC);
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
     * Swim speed is a {@code water_movement_efficiency} multiplier. The suspicion (gap 121's research, 2026-09-15): no
     * code sets that attribute's base, and a total multiplier on a base of 0 is still 0, so the gene may do nothing.
     */
    private static void swim(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AC_D, "SWIM SPEED", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("SWIM SPEED", "Otr, Stne and a", "plain horse: the", "water attribute"));
        String[][] spec = {
                {"horsegenetics.magic_swim_speed=Otr/Otr", "SWIM OTTER"},
                {"horsegenetics.magic_swim_speed=Otr/n", "SWIM OTTER ONE"},
                {"horsegenetics.magic_swim_speed=Stne/Stne", "SWIM STONE"},
                {PLAIN, "SWIM PLAIN"}};
        List<Horse> horses = new ArrayList<>();
        for (int i = 0; i < spec.length; i++) {
            Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5 + (i % 2) * 4, z0 + 3.5 + (i / 2) * 5,
                    Sex.FEMALE, spec[i][0], true, spec[i][1]);
            if (h != null) {
                horses.add(h);
            }
        }
        DebugWorldWatch.watchAttribute("SWIM SPEED ATTR", DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AC_D),
                Attributes.WATER_MOVEMENT_EFFICIENCY);
        DebugYardHerd.after(level, 100, () -> {
            StringBuilder sb = new StringBuilder();
            for (Horse h : horses) {
                AttributeInstance ai = h.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
                sb.append(sb.length() == 0 ? "" : "; ").append(h.getCustomName() == null ? "?" : h.getCustomName().getString())
                        .append(ai == null ? " has no such attribute" : String.format(" base %.3f value %.3f, %d modifier(s)",
                                ai.getBaseValue(), ai.getValue(), ai.getModifiers().size()));
            }
            ActionTrace.log("test yard", "SWIM SPEED: " + sb + " - expect OTTER above PLAIN and STONE below it;"
                    + " every value 0.000 with modifiers present means the gene multiplies nothing (FAIL)");
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
        int[] cellX = {x0 + 1, x0 + 4, x0 + 7};
        for (int cx : cellX) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = gy - 3; y <= gy - 1; y++) {
                        level.setBlock(new BlockPos(cx + dx, y, z0 + 4 + dz), Blocks.WATER.defaultBlockState(), 3);
                    }
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
        drown(level, horses, names, new long[]{-1, -1, -1}, new int[]{300, 300, 300}, start, 1);
    }

    private static void drown(ServerLevel level, Horse[] horses, String[] names, long[] firstHurt, int[] minAir,
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
                sb.append(sb.length() == 0 ? "" : "; ").append(names[i]).append(h.isAlive()
                        ? String.format(" air %d hp %.0f%s", h.getAirSupply(), h.getHealth(), h.isUnderWater() ? "" : " HEAD OUT")
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
            drown(level, horses, names, firstHurt, minAir, start, n + 1);
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
