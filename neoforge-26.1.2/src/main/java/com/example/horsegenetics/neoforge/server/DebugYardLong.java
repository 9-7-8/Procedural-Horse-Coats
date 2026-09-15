package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_S;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_S_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_T;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_T_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_U;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_U_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_W;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows S-U: pens that only answer after a whole day</b> (owner, 2026-09-14: "I'm
 * going to leave this up all day, so add more pens which benefit from being run for a
 * very long time", and "add more dryad testing pens").
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>S</td><td>(spruce and jungle deleted 2026-09-15)</td><td>(acacia deleted), DRYAD CARRIER (Oak/n, plants nothing)</td></tr>
 *   <tr><td>T</td><td>(oak deleted), DRYAD FLOWER</td><td>DRYAD OAK+BIRCH (half rate each), DRYAD DARK SMALL (widened 10x9)</td></tr>
 *   <tr><td>U</td><td>RATIO HYPP, RATIO LETHAL WHITE</td><td>RATIO BRINDLE, RATIO SIZE</td></tr>
 * </table>
 *
 * <h2>The dryad rows stand on stone</h2>
 * A dryad plants anywhere within four blocks of itself, and a fence is not a wall to a
 * radius: on the first night saplings turned up three blocks outside their pen, and the
 * watch box under-counted. So rows S and T are floored with stone from four blocks
 * before the pens to four after, pens keep a four-block stone gap, and only the inside of
 * each pen is grass. Nothing can take root outside a pen, so every sapling a pen's watch
 * counts is that pen's. Every planting is also a {@code [watch] dryad planted} line
 * naming the horse, which is the rate.
 *
 * <h2>The ratio pens breed all day and clear their own foals</h2>
 * A pair breeds on its own (natural covers; with {@code debug.tools} a heat and a
 * pregnancy each last a minute), so a pen makes a foal every few minutes. Left alone it
 * would fill to the cap of eight and stop. So a clock reads every foal as it appears -
 * its pair for the locus, and its sex where that matters - notes whether it dies, and
 * removes it two minutes after birth. Each event logs the running tally beside what it
 * should converge on, and a day is a few hundred foals: enough to settle a "one in
 * four" that nobody could count by hand.
 */
@EventBusSubscriber
final class DebugYardLong {

    private DebugYardLong() {
    }

    private static final int SCAN = 200;
    /** A foal is counted, watched for a lethal-at-birth death, then taken away. */
    private static final long FOAL_KEEP = 2400L;
    /** The ratio pens of the yard built last; {@link #build} starts it over. */
    private static final List<Tally> TALLIES = new ArrayList<>();

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Horse foal) {
            onJoin(level, foal);
        }
    }

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        TALLIES.clear();
        try {
            stoneBand(level, gy, cx, mouthZ + ROW_S, ROW_S_D);
            stoneBand(level, gy, cx, mouthZ + ROW_T, ROW_T_D);

            // DELETED 2026-09-15 (owner: "delete the dryad pens which have grown and haven't killed
            // anyone"): DRYAD SPRUCE, DRYAD JUNGLE, DRYAD ACACIA and DRYAD OAK. Overnight each grew
            // trees with its horses in it (spruce 18 logs, jungle 4, acacia 25, oak 18) and none took
            // inWall damage. Kept: the carrier control, flowers, oak+birch (no tree overnight), and
            // both dark oak pens - DRYAD DARK is where the branch killed a horse (gap 244).
            dryad(level, gy, east + 11, mouthZ + ROW_S, 6, ROW_S_D, "DRYAD CARRIER", "Oak/n", 2,
                    List.of("DRYAD CARRIER", "Oak/n: must plant", "NOTHING all day", "(the control)"),
                    Blocks.OAK_SAPLING, Blocks.OAK_LOG, Blocks.BIRCH_SAPLING);

            dryad(level, gy, west + 11, mouthZ + ROW_T, 6, ROW_T_D, "DRYAD FLOWER", "Flwr/Flwr", 2,
                    List.of("DRYAD FLOWER", "Flwr/Flwr: flowers", "on grass only", "(all day)"),
                    Blocks.DANDELION, Blocks.POPPY, Blocks.OAK_SAPLING);
            dryad(level, gy, east, mouthZ + ROW_T, 6, ROW_T_D, "DRYAD OAK+BIRCH", "Oak/Brch", 2,
                    List.of("DRYAD OAK+BIRCH", "both, each at half", "rate - and still", "no tree?"),
                    Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.OAK_LOG, Blocks.BIRCH_LOG);
            // WIDENED 2026-09-15 (owner). Dark and pale oak now hold back for a horse two blocks out
            // (gap 244), and at five by six the old pen's one horse was always inside that. Ten by
            // nine leaves room for it to stand clear while a 2x2 grows.
            dryad(level, gy, east + 8, mouthZ + ROW_T, 10, ROW_T_D, "DRYAD DARK SMALL", "Dark/Dark", 1,
                    List.of("DARK, ONE HORSE", "widened 10x9", "for the 2-block", "branch check"),
                    Blocks.DARK_OAK_SAPLING, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
            lightFromTheFloor(level, gy, cx, mouthZ + ROW_S, ROW_S_D);
            lightFromTheFloor(level, gy, cx, mouthZ + ROW_T, ROW_T_D);

            ratio(level, gy, west, mouthZ + ROW_U, "RATIO HYPP", "horsegenetics.scn4a", "H/N", "H/N", false,
                    List.of("RATIO: HYPP", "H/N x H/N all day:", "1 in 4 H/H, and", "every H/H dies"),
                    "about 1 in 4 H/H, 1 in 2 H/N; every H/H dies at birth");
            ratio(level, gy, west + 9, mouthZ + ROW_U, "RATIO LETHAL WHITE", "horsegenetics.ednrb", "O/N", "O/N", false,
                    List.of("RATIO: OVERO", "O/N x O/N all day:", "1 in 4 O/O, and", "every O/O dies"),
                    "about 1 in 4 O/O, 1 in 2 O/N; every O/O dies at birth");
            ratio(level, gy, east, mouthZ + ROW_U, "RATIO BRINDLE", "horsegenetics.brindle", "n/n", "Brn/Y", true,
                    List.of("RATIO: BRINDLE", "Brn stallion x n", "mare: every filly", "Brn/n, no colt Brn"),
                    "every filly Brn/n, every colt n/Y - a brindle colt is a FAIL");
            ratio(level, gy, east + 9, mouthZ + ROW_U, "RATIO SIZE", "horsegenetics.body_size", "Big/n", "Big/n", false,
                    List.of("RATIO: SIZE", "Big/n x Big/n all", "day: 1 Big/Big to", "2 Big/n to 1 n/n"),
                    "about 1 Big/Big : 2 Big/n : 1 n/n");

            // ROW W (2026-09-15, owner: every unattended test into the yard). Two genotypes a gene rules
            // out (gap 225): the doubled allele must be lost at conception, never born. A milk clash
            // whose every foal would be Watr/Lava, which MilkGene forbids. And a colour gene with knobs,
            // for inheritance of its epigenetic values. The conception log (gap 245) covers all four.
            ratio(level, gy, west, mouthZ + ROW_W, "RATIO KIT W5", "horsegenetics.kit", "W5/N", "W5/N", false,
                    List.of("RATIO: KIT W5", "W5/N x W5/N:", "W5/W5 is impossible", "- lost, never born"),
                    "no W5/W5 foal ever; about 1 in 4 conceptions lost early as nonviable, 2 W5/N : 1 N/N born");
            ratio(level, gy, west + 9, mouthZ + ROW_W, "RATIO MITF SW3", "horsegenetics.mitf", "SW3/N", "SW3/N", false,
                    List.of("RATIO: MITF SW3", "SW3/N x SW3/N:", "SW3/SW3 impossible", "- lost, never born"),
                    "no SW3/SW3 foal ever; about 1 in 4 conceptions lost early, 2 SW3/N : 1 N/N born");
            ratio(level, gy, east, mouthZ + ROW_W, "RATIO MILK CLASH", "horsegenetics.milk", "Watr/Watr", "Lava/Lava", false,
                    List.of("RATIO: MILK CLASH", "Watr x Lava: every", "foal Watr/Lava, and", "that cannot be born"),
                    "no foal ever: every conception lost early, with a cause that is not MET's");
            ratio(level, gy, east + 9, mouthZ + ROW_W, "RATIO STARBURST", "horsegenetics.starburst", "W/n", "W/n", false,
                    List.of("RATIO: STARBURST", "W/n x W/n all day:", "1 W/W : 2 W/n : 1 n/n", "(knob inheritance)"),
                    "about 1 W/W : 2 W/n : 1 n/n");
            ActionTrace.log("test yard", "all-day pens built (rows S-U: dryads, inheritance ratios; row W: impossible"
                    + " genotypes, milk clash, starburst)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: all-day rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Dryads
    // ------------------------------------------------------------------

    /** Stone across the whole yard for a dryad row, from four blocks before it to four after. */
    private static void stoneBand(ServerLevel level, int gy, int cx, int z0, int depth) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = cx - 24; x <= cx + 24; x++) {
            for (int z = z0 - 4; z <= z0 + depth + 4; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
    }

    /**
     * <b>Light a dryad row from the floor, not from the air</b> (2026-09-14). The yard's lamp
     * grid is invisible {@code minecraft:light} blocks three up and four apart, and a tree cannot
     * grow through one: vanilla's clearance check takes only air and {@code #replaceable_by_trees},
     * which light is not in (read out of the 26.1.2 client jar). Oak and jungle, which want a
     * one-block ring, grew anyway; spruce and acacia, which want two, tried hundreds of times in a
     * day and never did (gap 240). So over these rows the lamps come out and glowstone goes into
     * the stone floor between the pens: the same "no dark block for a zombie" the lamps were for
     * (gap 212), with nothing above a sapling.
     *
     * <p>UNVERIFIED: that the lamps were the whole cause. The census's failed-grow count says.
     */
    private static void lightFromTheFloor(ServerLevel level, int gy, int cx, int z0, int depth) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();
        for (int x = cx - 24; x <= cx + 24; x++) {
            for (int z = z0 - 4; z <= z0 + depth + 4; z++) {
                for (int y = gy + 1; y <= gy + 8; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (level.getBlockState(at).is(Blocks.LIGHT)) {
                        DebugPenManager.fastSet(level, at, air);
                    }
                }
                // Every third stone cell. Grass inside a pen is never more than a few blocks from
                // one, so it stays well above the light level a monster spawns at.
                if (Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0) {
                    BlockPos floor = new BlockPos(x, gy, z);
                    if (level.getBlockState(floor).is(Blocks.STONE)) {
                        DebugPenManager.fastSet(level, floor, glow);
                    }
                }
            }
        }
    }

    private static void dryad(ServerLevel level, int gy, int x0, int z0, int width, int depth, String name,
                              String tokens, int horses, List<String> sign, Block... watched) {
        int x1 = x0 + width;
        int z1 = z0 + depth;
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        for (int x = x0 + 1; x < x1; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, grass);
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        DebugTestYard.stock(level, gy, x0 + 2.5, (z0 + z1) / 2.0, "horsegenetics.dryad", name, horses, 0, tokens);
        YardPens.register(gy, x0, x1, z0, z1, name);
        // Four past the walls, which is as far as a planting can reach; tall enough for a tree.
        DebugWorldWatch.watch(name, DebugTestYard.box(x0 - 4, gy, z0 - 4, x1 + 4, gy + 10, z1 + 4), null, watched);
    }

    // ------------------------------------------------------------------
    // Ratios
    // ------------------------------------------------------------------

    private static final class Tally {
        final String name;
        final String key;
        final boolean bySex;
        final String expect;
        /** The pen's mare, as an entity; only her foals are counted (gap 239). */
        final @Nullable UUID damEntity;
        final Map<UUID, Long> living = new LinkedHashMap<>();
        final Map<UUID, String> classOf = new LinkedHashMap<>();
        final Set<UUID> done = new HashSet<>();
        final Map<String, int[]> counts = new LinkedHashMap<>();    // [born, died]
        int born;
        int died;

        Tally(String name, String key, boolean bySex, String expect, @Nullable UUID damEntity) {
            this.name = name;
            this.key = key;
            this.bySex = bySex;
            this.expect = expect;
            this.damEntity = damEntity;
        }

        String summary() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, int[]> e : counts.entrySet()) {
                int b = e.getValue()[0];
                sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(b)
                        .append(String.format(" (%.0f%%)", born == 0 ? 0.0 : 100.0 * b / born));
                if (e.getValue()[1] > 0) {
                    sb.append(", ").append(e.getValue()[1]).append(" died");
                }
            }
            return born + " foals: " + sb + " | expect " + expect;
        }
    }

    private static void ratio(ServerLevel level, int gy, int x0, int z0, String name, String key, String mare,
                              String stallion, boolean bySex, List<String> sign, String expect) {
        int x1 = x0 + 9;
        int z1 = z0 + ROW_U_D;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        Horse dam = spawn(level, gy, x0 + 3.0, z0 + 5.0, Sex.FEMALE, key + "=" + mare, name + " MARE");
        spawn(level, gy, x0 + 6.0, z0 + 5.0, Sex.MALE, key + "=" + stallion, name + " STUD");
        DebugYardFertility.inHeat(dam);
        YardPens.register(gy, x0, x1, z0, z1, name);
        DebugWorldWatch.watchBreeding(name, DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1));
        Tally tally = new Tally(name, key, bySex, expect, dam == null ? null : dam.getUUID());
        TALLIES.add(tally);
        scan(level, tally, DebugTestYard.box(x0, gy, z0, x1, gy + 4, z1), 0);
    }

    private static @Nullable Horse spawn(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         String label) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, true);
        DebugTestYard.label(h, label);
        return h;
    }

    /**
     * A foal entering the world, counted by whichever ratio pen's mare bore it.
     *
     * <p>COUNTED AT BIRTH, NOT AT THE SCAN (2026-09-14). The scan below only ever saw living
     * foals, once every ten seconds - and a lethal-at-birth foal is dead in three. The first
     * H/H in RATIO HYPP was born at 17:26:20 and dead at 17:26:23, and the tally went on
     * reading "H/N 3 (100%)": the class the pen exists to count could never appear in it.
     * Both {@code ReproHandler.foal} and vanilla breeding write the foal's record before
     * {@code addFreshEntity}, so it is readable here.
     */
    static void onJoin(ServerLevel level, Horse foal) {
        if (TALLIES.isEmpty() || !foal.isBaby() || !HorseRecords.hasRealRecord(foal)) {
            return;
        }
        long now = level.getGameTime();
        for (Tally t : TALLIES) {
            count(level, t, foal, now);
        }
    }

    private static void count(ServerLevel level, Tally t, Horse foal, long now) {
        UUID id = foal.getUUID();
        if (t.living.containsKey(id) || t.done.contains(id)) {
            return;
        }
        // ONLY THIS PEN'S FOALS (gap 239). The pens share a fence line and foals get across it:
        // a neighbour's foal counted here, then removed by its own pen's clock, read as "a N/N
        // foal died" nine times in a morning and skewed the shares with it. Matched on the
        // record's mother rather than on position, since position is what failed - and a pen
        // whose mare is gone counts nothing, since at birth there is no position to fall back on.
        UUID damId = t.damEntity != null && level.getEntity(t.damEntity) instanceof Horse d
                && HorseRecords.hasRealRecord(d) ? HorseRecords.of(d).id() : null;
        HorseRecord record = HorseRecords.of(foal);
        if (damId == null || record.motherId().filter(damId::equals).isEmpty()) {
            return;
        }
        AllelePair pair = record.genotype().pair(t.key);
        String cls = (t.bySex ? (record.sex() == Sex.FEMALE ? "filly " : "colt ") : "")
                + (pair == null ? "?" : pair.toTokens());
        t.living.put(id, now);
        t.classOf.put(id, cls);
        t.counts.computeIfAbsent(cls, k -> new int[2])[0]++;
        t.born++;
        ActionTrace.log("test yard", t.name + ": foal " + t.born + " is " + cls + " | " + t.summary());
    }

    /** Gap 245: each ratio pen's running tally of shadow draws, by class. */
    private static final Map<Tally, Map<String, int[]>> SHADOW = new java.util.IdentityHashMap<>();

    /**
     * <b>Gap 245's instrument</b> (owner, 2026-09-14: "just build the extra logging for lethal
     * pens"). The lethal pens threw far too many homozygous foals in one run and none in 35 the
     * next, while the same draw on the same generator is fair offline. So at every conception in
     * a ratio pen this logs what the draw actually saw and made: the game tick, the chance, the
     * dam's and sire's pairs <i>as passed to the draw</i>, the embryo, whether mare and stallion
     * share one random source, and a <b>shadow draw</b> - the same two genomes through the same
     * {@code breedWith}, from a generator nothing else touches. The shadow changes nothing in the
     * game; it only tallies. If the shadow reads 1:2:1 while the real embryos do not, the fault is
     * in the stream the mare's random hands the draw; if both skew, it is in the genomes.
     * All four ratio pens log, so brindle and size are controls.
     */
    static void onConception(Horse mare, @Nullable Horse sire,
                             com.example.horsegenetics.common.genetics.Genome mareGenome,
                             com.example.horsegenetics.common.genetics.Genome sireGenome,
                             com.example.horsegenetics.common.repro.Conception.Result result) {
        if (TALLIES.isEmpty() || result.pregnancy().isEmpty() || !(mare.level() instanceof ServerLevel level)) {
            return;
        }
        Tally t = null;
        for (Tally x : TALLIES) {
            if (mare.getUUID().equals(x.damEntity)) {
                t = x;
                break;
            }
        }
        if (t == null) {
            return;
        }
        StringBuilder embryos = new StringBuilder();
        for (com.example.horsegenetics.common.repro.Embryo e : result.pregnancy().get().embryos()) {
            embryos.append(embryos.length() == 0 ? "" : " + ").append(tokens(e.foal().genotype().pair(t.key)));
        }
        com.example.horsegenetics.common.genetics.Genome shadow =
                com.example.horsegenetics.common.genetics.GeneticCodeCombiner.combine(mareGenome, sireGenome,
                        new com.example.horsegenetics.common.SeededRng(level.getRandom().nextLong()),
                        com.example.horsegenetics.common.genetics.GameteBias.NONE,
                        com.example.horsegenetics.common.genetics.GameteBias.NONE);
        String shadowClass = tokens(shadow.genotype().pair(t.key));
        Map<String, int[]> tally = SHADOW.computeIfAbsent(t, k -> new LinkedHashMap<>());
        tally.computeIfAbsent(shadowClass, k -> new int[1])[0]++;
        int shadows = 0;
        for (int[] c : tally.values()) {
            shadows += c[0];
        }
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, int[]> e : tally.entrySet()) {
            summary.append(summary.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue()[0])
                    .append(String.format(java.util.Locale.ROOT, " (%.0f%%)", 100.0 * e.getValue()[0] / shadows));
        }
        String sireRandom = sire == null ? "no live sire"
                : String.format(java.util.Locale.ROOT, "sire random #%08x (%s)",
                        System.identityHashCode(sire.getRandom()),
                        sire.getRandom() == mare.getRandom() ? "SHARED with the mare" : "separate");
        ActionTrace.log("test yard", String.format(java.util.Locale.ROOT,
                "%s conception at tick %d: chance %.2f | dam %s, sire %s as drawn | embryo %s | shadow %s"
                        + " | mare random #%08x, %s | shadow tally %d: %s | expect the shadow near %s",
                t.name, level.getGameTime(), result.chance(), tokens(mareGenome.genotype().pair(t.key)),
                tokens(sireGenome.genotype().pair(t.key)), embryos, shadowClass,
                System.identityHashCode(mare.getRandom()), sireRandom, shadows, summary, t.expect));
    }

    private static String tokens(@Nullable AllelePair pair) {
        return pair == null ? "?" : pair.toTokens();
    }

    private static void scan(ServerLevel level, Tally t, AABB box, int round) {
        DebugYardHerd.after(level, SCAN, () -> {
            long now = level.getGameTime();
            // A fallback only: a foal whose record landed after it joined. Birth is onJoin's.
            for (Horse foal : level.getEntitiesOfClass(Horse.class, box, h -> h.isBaby() && h.isAlive()
                    && HorseRecords.hasRealRecord(h))) {
                count(level, t, foal, now);
            }
            for (var it = t.living.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                Horse h = level.getEntity(e.getKey()) instanceof Horse x ? x : null;
                String cls = t.classOf.get(e.getKey());
                if (h == null || !h.isAlive()) {
                    t.counts.computeIfAbsent(cls, k -> new int[2])[1]++;
                    t.died++;
                    ActionTrace.log("test yard", t.name + ": a " + cls + " foal died | " + t.summary());
                    it.remove();
                    t.done.add(e.getKey());
                } else if (now - e.getValue() >= FOAL_KEEP) {
                    h.discard();    // counted and outlived a birth lethal: out of the way of the cap
                    it.remove();
                    t.done.add(e.getKey());
                }
            }
            if (round % 60 == 59) {     // every ten minutes, whether or not anything changed
                ActionTrace.log("test yard", t.name + " tally | " + t.summary());
            }
            scan(level, t, box, round + 1);
        });
    }
}
