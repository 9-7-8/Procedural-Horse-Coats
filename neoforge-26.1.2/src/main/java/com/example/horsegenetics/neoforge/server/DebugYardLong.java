package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Conceivable;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.DryadGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Embryo;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_U;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_U_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_W;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Row O east, rows U and W</b> since 2026-09-30, when row S (DRYAD CARRIER), DRYAD DARK SMALL and then DRYAD FLOWER
 * went as answered, and the one dryad left moved into row O.
 *
 * <p><b>Rows S-U: pens that only answer after a whole day</b> (owner, 2026-09-14: "I'm
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
            // DRYAD OAK+BIRCH and RATIO BRINDLE passed on the morning of 2026-10-02 (the dryad at 145 min, brindle at 40
            // foals) and went; the dryad helpers below stay for the next dryad question.

            // ANSWERED AND GONE (2026-10-02): RATIO HYPP, LETHAL WHITE, SIZE, KIT W5 and MILK CLASH logged their own
            // PASS on the night of 2026-10-01 - with RATIO ACAN D5 - and were closed on their pages. What is left is the
            // three that stalled behind #23 (covers refused against a wall), started over with that fixed.

            // ROW W (2026-09-15, owner: every unattended test into the yard). Two genotypes a gene rules
            // out (gap 225): the doubled allele must be lost at conception, never born. A milk clash
            // whose every foal would be Watr/Lava, which MilkGene forbids. And a colour gene with knobs,
            // for inheritance of its epigenetic values. The conception log (gap 245) covers all four.
            ratio(level, gy, west + 9, mouthZ + ROW_W, "RATIO MITF SW3", "horsegenetics.mitf", "SW3/N", "SW3/N", false,
                    List.of("RATIO: MITF SW3", "SW3/N x SW3/N:", "SW3/SW3 impossible", "- lost, never born"),
                    "no SW3/SW3 foal ever; about 1 in 4 conceptions lost early, 2 SW3/N : 1 N/N born");
            // RATIO STARBURST passed the same morning (40 foals, 6 : 23 : 11, p 0.341) and went.
            ActionTrace.log("test yard", "all-day pens built (row W: RATIO MITF SW3)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: all-day rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Dryads
    // ------------------------------------------------------------------

    /** Stone across the whole yard for a dryad row, from four blocks before it to four after. */
    private static void stoneBand(ServerLevel level, int gy, int xFrom, int xTo, int z0, int depth) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = xFrom; x <= xTo; x++) {
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
    private static void lightFromTheFloor(ServerLevel level, int gy, int xFrom, int xTo, int z0, int depth) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();
        for (int x = xFrom; x <= xTo; x++) {
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
    // DRYAD OAK+BIRCH answers for itself
    // ------------------------------------------------------------------

    /*
     * WHAT THIS PEN IS STILL FOR (2026-09-30, owner: "Build in more pens that answer for themselves").
     * Its sign asks "and still no tree?", and that was answered long ago: trees grew in this pen on
     * 2026-09-14 (10 oak and 5 birch logs, then 30 birch_log), and run 10 today read 5x oak_log at 31
     * minutes. gene-dryad.html's Verification tab says not to re-test it. The one question on that tab
     * a designed pen CAN answer is "the interval is on the copy, and the two halves of a mixed horse
     * differ": DryadGene.abilitiesFor must time oak off copy(0) and birch off copy(1), each doubled. If
     * it read expressed() instead - the dominant copy, Oak's, for a heterozygote - both species would
     * plant on one beat and it would still look like a working gene.
     *
     * HOW IT TELLS, EXACTLY. GeneAbilityHandler.beat() fires a spread only on ticks where
     * (gameTime + phase) mod interval == 0, phase being the horse UUID's low bits. So a planting's tick
     * says which interval timed it. Every tick this polls DebugWorldWatch.spreadsPlacedBy for the pen's
     * horses; when one count rises, the one sapling that is new in the pen says the species, and the
     * tick is tested against the interval this pen predicts from the horse's own epigenome copies. A
     * founder's two copies are rolled independently between MIN_INTERVAL and MAX_INTERVAL, so the two
     * beats almost never coincide, and a planting timed by the wrong copy lands on the right beat by
     * chance about 3 times in 40,000 (the +-1 tick tolerance below). Two plantings of EACH species on
     * their own beat is therefore overwhelming, and it costs the pen hours rather than a day: at the
     * 2026-09-14 rate (7 plantings in about three hours) two of each is a two-to-four-hour wait.
     */

    /** Plantings of each species, every one on its own copy's beat, that make a PASS. */
    private static final int DRYAD_EACH = 2;
    /**
     * Eight yard hours. At the slowest copies (MAX_INTERVAL doubled, one try in three landing) a species
     * still expects about four landings from two horses in that time; nothing by then is a finding.
     */
    private static final long DRYAD_DEADLINE = 8L * 60 * 1200;
    /** Bumped by every build, so a poll left running for the yard before this one stops itself. */
    private static int dryadRun;

    private static final class DryadHorse {
        final UUID id;
        final String label;
        /** GeneAbilityHandler.beat()'s phase for this horse, copied rather than called - it is private. */
        final long phase;
        final long oakEvery;
        final long birchEvery;
        int placed;
        long lastOak = -1;
        long lastBirch = -1;

        DryadHorse(UUID id, String label, long oakEvery, long birchEvery, int placed) {
            this.id = id;
            this.label = label;
            this.phase = id.getLeastSignificantBits() & 0x7FFFFFFFL;
            this.oakEvery = oakEvery;
            this.birchEvery = birchEvery;
            this.placed = placed;
        }
    }

    private static final class DryadCheck {
        final int run;
        final long start;
        final int gy;
        final int x0;
        final int z0;
        final int x1;
        final int z1;
        final List<DryadHorse> horses = new ArrayList<>();
        Set<BlockPos> saplings = new HashSet<>();
        int oak;
        int birch;
        int untied;
        boolean judged;

        DryadCheck(int run, long start, int gy, int x0, int z0, int x1, int z1) {
            this.run = run;
            this.start = start;
            this.gy = gy;
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
        }
    }

    private static final String DRYAD = "DRYAD OAK+BIRCH";

    private static void dryadVerdict(ServerLevel level, int gy, int x0, int z0, int x1, int z1) {
        int run = ++dryadRun;
        // A second after the build, so the stocked horses are in the world to be found.
        DebugYardHerd.after(level, 20, () -> {
            if (run != dryadRun) {
                return;
            }
            DryadCheck c = new DryadCheck(run, level.getGameTime(), gy, x0, z0, x1, z1);
            Gene gene = Genes.byKeyOrNull(DryadGene.KEY);
            StringBuilder who = new StringBuilder();
            if (gene != null) {
                for (Horse h : level.getEntitiesOfClass(Horse.class, DebugTestYard.box(x0, gy, z0, x1, gy + 3, z1),
                        e -> e.isAlive() && HorseRecords.hasRealRecord(e))) {
                    HorseRecord r = HorseRecords.of(h);
                    AllelePair pair = r.genotype().pair(DryadGene.KEY);
                    if (pair == null || !"Oak/Brch".equals(pair.toTokens())) {
                        continue;
                    }
                    // Read off the copies directly, NOT through GeneEpigenetics.copy(slot) or expressed():
                    // the pen's prediction must not share the reading it is checking. Copy 0 rides on the
                    // pair's first allele, and Oak sorts before Brch (DryadGene's constructor note).
                    Epigenome.Copies copies = r.epigenome().copies(gene);
                    long oak = Math.round(Epigenome.readable(gene, copies.first()).get(DryadGene.INTERVAL))
                            * DryadGene.MIXED_SLOWDOWN;
                    long birch = Math.round(Epigenome.readable(gene, copies.second()).get(DryadGene.INTERVAL))
                            * DryadGene.MIXED_SLOWDOWN;
                    DryadHorse d = new DryadHorse(h.getUUID(), "mare " + (c.horses.size() + 1), oak, birch,
                            DebugWorldWatch.spreadsPlacedBy(h.getUUID()));
                    c.horses.add(d);
                    who.append(who.length() == 0 ? "" : "; ").append(d.label).append(" oak every ").append(oak)
                            .append(" ticks, birch every ").append(birch);
                }
            }
            if (c.horses.isEmpty()) {
                ActionTrace.log("test yard", DRYAD + " at build: no Oak/Brch horse found in the pen"
                        + " - INCONCLUSIVE (the pen was not stocked, so nothing can be timed)");
                return;
            }
            ActionTrace.log("test yard", DRYAD + ": timing " + c.horses.size() + " horse(s) - " + who
                    + " (each copy's interval x" + DryadGene.MIXED_SLOWDOWN + "); a PASS is " + DRYAD_EACH
                    + " plantings of each species, every one on its own copy's beat");
            c.saplings = saplingsIn(level, c);
            dryadPoll(level, c);
        });
    }

    private static void dryadPoll(ServerLevel level, DryadCheck c) {
        DebugYardHerd.after(level, 1, () -> {
            if (c.run != dryadRun || c.judged) {
                return;
            }
            dryadPoll(level, c);    // first, so a throw below cannot stop the clock
            long now = level.getGameTime();
            DryadHorse landed = null;
            int rises = 0;
            for (DryadHorse d : c.horses) {
                int placed = DebugWorldWatch.spreadsPlacedBy(d.id);
                if (placed > d.placed) {
                    rises += placed - d.placed;
                    landed = d;
                }
                d.placed = placed;  // also takes a watch restart (the count going down) as the new zero
            }
            if (rises > 0) {
                Set<BlockPos> standing = saplingsIn(level, c);
                List<BlockPos> fresh = new ArrayList<>();
                for (BlockPos p : standing) {
                    if (!c.saplings.contains(p)) {
                        fresh.add(p);
                    }
                }
                c.saplings = standing;
                if (rises == 1 && fresh.size() == 1) {
                    dryadLanding(level, c, landed, fresh.get(0), now);
                } else {
                    // Two horses in one tick, or a sapling back on a spot the last look already had.
                    // Rare, and skipping it only costs time.
                    c.untied++;
                    ActionTrace.log("test yard", DRYAD + ": a planting at tick " + now + " could not be tied to one"
                            + " horse and one new sapling (" + rises + " landing(s), " + fresh.size()
                            + " new sapling(s)) - not counted");
                }
            } else if (now % 200 == 0) {
                // Forget saplings that grew or were broken, so a replanted spot reads as new.
                c.saplings = saplingsIn(level, c);
            }
            if (!c.judged && now - c.start >= DRYAD_DEADLINE) {
                dryadDeadline(c);
            }
        });
    }

    /** Oak and birch saplings standing in and around the pen. Only its grass can hold one (the stone band). */
    private static Set<BlockPos> saplingsIn(ServerLevel level, DryadCheck c) {
        Set<BlockPos> out = new HashSet<>();
        for (int x = c.x0 - 4; x <= c.x1 + 4; x++) {
            for (int z = c.z0 - 4; z <= c.z1 + 4; z++) {
                for (int y = c.gy; y <= c.gy + 2; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.OAK_SAPLING) || s.is(Blocks.BIRCH_SAPLING)) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Whether {@code tick} is one of the ticks {@code GeneAbilityHandler.beat} fires on for this interval.
     *
     * <p>UNVERIFIED: that DebugYardHerd's clock (ServerTickEvent.Post) reads the same getGameTime() the
     * horse's own tick used earlier in that server tick. It should - the level's time moves at the start
     * of its tick - but one tick either way is accepted rather than bet a false FAIL on it.
     */
    private static boolean onBeat(long tick, long phase, long interval) {
        if (interval <= 1) {
            return true;
        }
        long r = Math.floorMod(tick + phase, interval);
        return r == 0 || r == 1 || r == interval - 1;
    }

    private static void dryadLanding(ServerLevel level, DryadCheck c, DryadHorse d, BlockPos at, long now) {
        boolean oak = level.getBlockState(at).is(Blocks.OAK_SAPLING);
        String species = oak ? "oak" : "birch";
        String other = oak ? "birch" : "oak";
        long own = oak ? d.oakEvery : d.birchEvery;
        long wrong = oak ? d.birchEvery : d.oakEvery;
        boolean onOwn = onBeat(now, d.phase, own);
        boolean onWrong = onBeat(now, d.phase, wrong);
        String what = species + " by " + d.label + " at tick " + now + " (" + at.toShortString() + ")";
        int minutes = (int) ((now - c.start) / 1200);
        if (onOwn && onWrong) {
            ActionTrace.log("test yard", DRYAD + ": " + what + " is on both copies' beats (" + own + " and " + wrong
                    + " ticks), which cannot tell them apart - not counted");
            return;
        }
        if (onOwn) {
            long last = oak ? d.lastOak : d.lastBirch;
            String since = last < 0 ? "its first " + species
                    : Math.round((double) (now - last) / own) + " beat(s) since its last " + species;
            if (oak) {
                c.oak++;
                d.lastOak = now;
            } else {
                c.birch++;
                d.lastBirch = now;
            }
            ActionTrace.log("test yard", DRYAD + ": " + what + " is on its " + species + " copy's beat (every " + own
                    + " ticks), " + since + "; its " + other + " copy's beat is every " + wrong + " | " + c.oak
                    + " oak, " + c.birch + " birch so far");
            if (c.oak >= DRYAD_EACH && c.birch >= DRYAD_EACH) {
                c.judged = true;
                StringBuilder beats = new StringBuilder();
                for (DryadHorse h : c.horses) {
                    beats.append(beats.length() == 0 ? "" : "; ").append(h.label).append(" oak every ")
                            .append(h.oakEvery).append(", birch every ").append(h.birchEvery);
                }
                ActionTrace.log("test yard", DRYAD + " at " + minutes + " min: " + c.oak + " oak and " + c.birch
                        + " birch plantings, every one on its own copy's beat (" + beats + " ticks)"
                        + " - PASS");
            }
            return;
        }
        c.judged = true;
        if (onWrong) {
            ActionTrace.log("test yard", DRYAD + " at " + minutes + " min: " + what + " - FAIL (it is on the " + other
                    + " copy's beat, every " + wrong + " ticks, not its own every " + own + ": one copy's interval"
                    + " is timing both species, which is what reading expressed() instead of copy(0) and copy(1)"
                    + " would do)");
        } else if (onBeat(now, d.phase, own / DryadGene.MIXED_SLOWDOWN)
                || onBeat(now, d.phase, wrong / DryadGene.MIXED_SLOWDOWN)) {
            ActionTrace.log("test yard", DRYAD + " at " + minutes + " min: " + what + " - FAIL (it is on half a"
                    + " copy's beat: the mixed pair's slowdown, DryadGene.MIXED_SLOWDOWN, is not being applied)");
        } else {
            ActionTrace.log("test yard", DRYAD + " at " + minutes + " min: " + what + " - INCONCLUSIVE (on none of"
                    + " the beats this pen models, whole or halved, own copy or other: GeneAbilityHandler.beat()"
                    + " or its phase has probably changed, so the pen's arithmetic is the suspect, not the gene)");
        }
    }

    private static void dryadDeadline(DryadCheck c) {
        c.judged = true;
        String at = DRYAD + " at " + DRYAD_DEADLINE / 1200 + " min: ";
        if (c.oak + c.birch == 0) {
            ActionTrace.log("test yard", at + "no planting tied to a horse - INCONCLUSIVE (" + c.untied
                    + " untied; spreadsPlacedBy only counts while the world watch runs - check the census ran)");
            return;
        }
        if (c.oak == 0 || c.birch == 0) {
            // One species never planting is the other half of the expressed() failure ("count one allele
            // twice and the other never"). How unlikely is none, given what the copies predict?
            double oakRate = 0;
            double birchRate = 0;
            for (DryadHorse h : c.horses) {
                oakRate += 1.0 / h.oakEvery;
                birchRate += 1.0 / h.birchEvery;
            }
            int seen = Math.max(c.oak, c.birch);
            double missingShare = (c.oak == 0 ? oakRate : birchRate) / (oakRate + birchRate);
            double p = Math.pow(1.0 - missingShare, seen);
            String missing = c.oak == 0 ? "oak" : "birch";
            String line = at + seen + " " + (c.oak == 0 ? "birch" : "oak") + " plantings and no " + missing
                    + String.format(java.util.Locale.ROOT, ", where the copies give %s %.0f%% of plantings"
                    + " (p %.4f of none)", missing, 100 * missingShare, p);
            ActionTrace.log("test yard", line + (p < 0.01
                    ? " - FAIL (one allele never plants: its copy is not being read)"
                    : " - INCONCLUSIVE (too few plantings to say the " + missing + " copy is silent)"));
            return;
        }
        ActionTrace.log("test yard", at + c.oak + " oak and " + c.birch + " birch plantings, all on their own"
                + " copy's beat - INCONCLUSIVE (a PASS needs " + DRYAD_EACH + " of each; the horses planted slowly)");
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

        // The verdict (2026-09-30). Set by ratio() once the tally exists, so its constructor stays as it was.
        Rule rule = Rule.NONE;
        /** The allele whose copies are counted - H, O, Big, W5, SW3, W; Brn for the sex-linked pen. */
        String mutant = "";
        /** HYPP and LETHAL WHITE: a homozygote alive when FOAL_KEEP ends is a FAIL. */
        boolean homozygoteDies;
        /** ACAN D5: every foal is an affected dwarf and none may die - a death of any class is a FAIL. */
        boolean allSurvive;
        long startTick;
        /** Born foals, and distinct conception draws, by copies of {@link #mutant}: [0, 1, 2]. */
        final int[] bornBy = new int[3];
        final int[] drawnBy = new int[3];
        /** The tokens each of those classes printed as, for the verdict line. */
        final String[] labelBy = new String[3];
        int draws;
        int conceptions;
        int colts;
        int mareMissing;
        /** A PASS or INCONCLUSIVE is logged once; a FAIL may still follow one, and says it overrides it. */
        boolean judged;
        boolean failed;
        /** Nothing more is judged at all - the world cannot answer this pen's question. */
        boolean closed;

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
        tally.startTick = level.getGameTime();
        // WHICH QUESTION EACH PEN ANSWERS. By name, since the names are what the signs, the log and the
        // Verification tabs share; a pen not listed here keeps its tally and gets no verdict.
        switch (name) {
            case "RATIO HYPP" -> judgeBy(tally, Rule.BORN_121, "H", true);
            case "RATIO LETHAL WHITE" -> judgeBy(tally, Rule.BORN_121, "O", true);
            case "RATIO SIZE" -> judgeBy(tally, Rule.BORN_121, "Big", false);
            case "RATIO STARBURST" -> judgeBy(tally, Rule.BORN_121, "W", false);
            case "RATIO BRINDLE" -> judgeBy(tally, Rule.SEX_LINKED, "Brn", false);
            case "RATIO KIT W5" -> judgeBy(tally, Rule.CONCEIVED_121, "W5", false);
            case "RATIO MITF SW3" -> judgeBy(tally, Rule.CONCEIVED_121, "SW3", false);
            case "RATIO MILK CLASH" -> judgeBy(tally, Rule.NO_FOAL, "", false);
            case "RATIO ACAN D5" -> {
                judgeBy(tally, Rule.BORN_121, "D5", false);
                tally.allSurvive = true;
            }
            default -> { }
        }
        // A world with lethals off carries a lethal embryo to term and lets a lethal foal live
        // (ServerConfig.lethalsActive), so the class these pens exist to see is never lost or killed.
        // That is the world's setup, not the gene: say so once and judge nothing else.
        boolean needsLethals = tally.homozygoteDies || tally.rule == Rule.CONCEIVED_121 || tally.rule == Rule.NO_FOAL;
        if (needsLethals && !ServerConfig.lethalsActive()) {
            inconclusive(tally, "at build", "lethals are off in this world's health mode, so the class this pen"
                    + " exists to see is carried like any other");
            tally.closed = true;
        }
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
        judgeFoal(t, record.sex(), tokens(pair), cls);
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
        judgeConception(t, result.pregnancy().get());
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
                    if (t.allSurvive) {
                        fail(t, "at " + t.born + " foals", "a " + cls + " foal died inside " + FOAL_KEEP / 20
                                + " s of birth; only D1/D1 is lethal, so every D5 dwarf must live");
                    }
                    it.remove();
                    t.done.add(e.getKey());
                } else if (now - e.getValue() >= FOAL_KEEP) {
                    if (t.homozygoteDies && copies(cls, t.mutant) == 2) {
                        fail(t, "at " + t.born + " foals", "a " + cls + " foal is still alive " + FOAL_KEEP / 20
                                + " seconds after birth; every one must die at birth");
                    }
                    h.discard();    // counted and outlived a birth lethal: out of the way of the cap
                    it.remove();
                    t.done.add(e.getKey());
                }
            }
            // The pair is kept fit (#215). Only the pen's locus is fixed and the rest of each genome is rolled,
            // so a mare can roll a disorder that costs her a point now and then; health does not come back by
            // itself and a natural cover needs 90%, which ended this pen at 13 foals on 2026-10-08.
            for (Horse adult : level.getEntitiesOfClass(Horse.class, box, h -> !h.isBaby() && h.isAlive()
                    && h.getHealth() < h.getMaxHealth())) {
                adult.setHealth(adult.getMaxHealth());
            }
            if (round % 60 == 59) {     // every ten minutes, whether or not anything changed
                ActionTrace.log("test yard", t.name + " tally | " + t.summary());
            }
            judgeScan(level, t, now);
            scan(level, t, box, round + 1);
        });
    }

    // ------------------------------------------------------------------
    // The ratio pens' verdicts
    // ------------------------------------------------------------------

    /*
     * EVERY RATIO PEN ANSWERS FOR ITSELF (owner, 2026-09-30: "Build in more pens that answer for
     * themselves"). The owner ruled the same day that an automatic PASS closes the pen's wiki check and
     * the pen is then deleted, so a PASS here has to be one nobody would second-guess:
     *
     * - A foal the gene forbids FAILs the moment it is seen: a W5/W5 or SW3/SW3 born, any milk-clash
     *   foal, a brindle colt (or a filly without her sire's Brn), an H/H or O/O still alive when
     *   FOAL_KEEP ends.
     * - A proportion is judged once, by chi-square against 1:2:1 at p < 0.01 (2 df: 9.21). Not 0.05:
     *   RATIO SIZE's embryos over five runs today read Big/n 31, n/n 16, Big/Big 6 - chi-square about
     *   5.3, p about 0.07. A fair locus reads like that one run in fourteen; at 0.05 the pens would
     *   FAIL a fair gene one run in twenty, and a false FAIL costs an evening of hunting.
     * - Forty per verdict: the smallest class then expects ten, twice the textbook floor of five for
     *   chi-square, and gap 245's failure - no homozygote in 35 - scores 10 from its empty class alone,
     *   so it cannot pass. A pen makes a foal every few minutes, so forty is a two-to-three hour run.
     * - WHICH COUNT. The born foals where every class is born (HYPP, LETHAL WHITE, SIZE, STARBURST,
     *   BRINDLE): the sign promises what is born. The conception draws for KIT and MITF, whose
     *   homozygote is lost before birth, so a born count can only ever read 2:1 and cannot see the draw.
     *   Identical twins are one draw and are counted once.
     */

    private enum Rule {
        NONE,
        /** 1:2:1 in born foals. */
        BORN_121,
        /** 1:2:1 in conception draws; the homozygote is never born. */
        CONCEIVED_121,
        /** Brn/Y sire x n/n dam: every filly Brn/n, every colt without Brn. */
        SEX_LINKED,
        /** Watr/Watr x Lava/Lava: every conception lost, no foal ever. */
        NO_FOAL
    }

    private static final int VERDICT_FOALS = 40;
    private static final int VERDICT_DRAWS = 40;
    /** The chi-square value at p = 0.01 with two degrees of freedom. */
    private static final double CHI2_2DF_P01 = 9.2103;
    /** Ten lost pregnancies and no foal; see {@link #judgeConception} for why it is read at the eleventh. */
    private static final int MILK_CONCEPTIONS = 10;
    /** P(fewer than ten colts in forty) is about 1 in 3000; fewer is a sex-ratio problem, not brindle's. */
    private static final int BRINDLE_MIN_COLTS = 10;
    /** Eight yard hours. Forty foals is two or three; a pen still short by eight has stalled. */
    private static final long RATIO_DEADLINE = 8L * 60 * 1200;
    /** Scans in a row the mare must be missing before it counts - a minute, so a chunk blink is not it. */
    private static final int MARE_GONE_SCANS = 6;
    /** The cause Conceivable gives a Watr/Lava embryo - the milk rule's, not a trait lethal like MET's. */
    private static final String MILK_CAUSE = Conceivable.ID_PREFIX + "milk";

    private static void judgeBy(Tally t, Rule rule, String mutant, boolean homozygoteDies) {
        t.rule = rule;
        t.mutant = mutant;
        t.homozygoteDies = homozygoteDies;
    }

    private static void pass(Tally t, String text) {
        if (t.judged || t.failed || t.closed) {
            return;
        }
        t.judged = true;
        ActionTrace.log("test yard", t.name + " " + text + " - PASS");
    }

    private static void inconclusive(Tally t, String at, String why) {
        if (t.judged || t.failed || t.closed) {
            return;
        }
        t.judged = true;
        ActionTrace.log("test yard", t.name + " " + at + " - INCONCLUSIVE (" + why + ")");
    }

    /** Once. A FAIL after a PASS still logs - a forbidden foal outranks a ratio - and says so. */
    private static void fail(Tally t, String at, String why) {
        if (t.failed || t.closed) {
            return;
        }
        boolean overrides = t.judged;
        t.failed = true;
        t.judged = true;
        ActionTrace.log("test yard", t.name + " " + at + " - FAIL (" + why
                + (overrides ? "; this overrides the verdict logged above" : "") + ")");
    }

    /** Copies of {@code mutant} in "A/B" or "colt A/B"; -1 when there is no readable pair. */
    private static int copies(@Nullable String tokens, String mutant) {
        if (tokens == null || mutant.isEmpty()) {
            return -1;
        }
        String[] parts = tokens.substring(tokens.lastIndexOf(' ') + 1).split("/");
        if (parts.length != 2) {
            return -1;
        }
        int n = 0;
        for (String p : parts) {
            if (p.equals(mutant)) {
                n++;
            }
        }
        return n;
    }

    private static void noteLabel(Tally t, int m, String tokens) {
        if (m >= 0 && t.labelBy[m] == null) {
            t.labelBy[m] = tokens;
        }
    }

    private static String label(Tally t, int m) {
        if (t.labelBy[m] != null) {
            return t.labelBy[m];
        }
        return m == 2 ? t.mutant + "/" + t.mutant : m == 1 ? t.mutant + "/+" : "+/+";
    }

    /** Chi-square of {@code by} (copies 0, 1, 2) against 1:2:1. */
    private static double chi121(int[] by) {
        double n = by[0] + by[1] + by[2];
        double quarter = n / 4.0;
        double half = n / 2.0;
        return (by[2] - quarter) * (by[2] - quarter) / quarter
                + (by[1] - half) * (by[1] - half) / half
                + (by[0] - quarter) * (by[0] - quarter) / quarter;
    }

    private static void ratioVerdict(Tally t, int[] by, String at, String extra) {
        double chi = chi121(by);
        // At two degrees of freedom the chi-square survival function is exactly exp(-x/2).
        double p = Math.exp(-chi / 2.0);
        String text = String.format(java.util.Locale.ROOT,
                "%s: %s %d, %s %d, %s %d against 1:2:1, chi-square %.2f (2 df, p %.3f; FAIL at p < 0.01)%s",
                at, label(t, 2), by[2], label(t, 1), by[1], label(t, 0), by[0], chi, p, extra);
        if (chi > CHI2_2DF_P01) {
            fail(t, text, "the split is off 1:2:1 at p < 0.01");
        } else {
            pass(t, text);
        }
    }

    /** A foal of this pen's mare, just counted. */
    private static void judgeFoal(Tally t, Sex sex, String tokens, String cls) {
        if (t.rule == Rule.NONE || t.closed) {
            return;
        }
        if (t.rule == Rule.NO_FOAL) {
            fail(t, "at foal " + t.born, "a " + cls + " foal was born; a Watr x Lava cross must lose every conception");
            return;
        }
        int m = copies(tokens, t.mutant);
        if (m < 0) {
            return;     // no pair at the locus: nothing to judge, and not the gene's doing
        }
        t.bornBy[m]++;
        if (t.rule != Rule.SEX_LINKED) {
            noteLabel(t, m, tokens);
        }
        switch (t.rule) {
            case CONCEIVED_121 -> {
                if (m == 2) {
                    fail(t, "at foal " + t.born, "a " + tokens + " foal was born; that genotype is impossible and"
                            + " must be lost at conception");
                }
            }
            case SEX_LINKED -> {
                boolean colt = sex != Sex.FEMALE;
                if (colt) {
                    t.colts++;
                    if (m > 0) {
                        fail(t, "at foal " + t.born, "a brindle colt, " + cls + ": a colt's one X is his n/n dam's");
                    }
                } else if (m != 1) {
                    fail(t, "at foal " + t.born, "a filly " + cls + ": every filly takes her sire's Brn X and her"
                            + " dam's n");
                }
                if (t.born >= VERDICT_FOALS) {
                    int fillies = t.born - t.colts;
                    if (t.colts >= BRINDLE_MIN_COLTS) {
                        pass(t, "at " + t.born + " foals: " + fillies + " fillies, every one Brn/n, and " + t.colts
                                + " colts, not one carrying Brn");
                    } else {
                        inconclusive(t, "at " + t.born + " foals", "only " + t.colts + " colts, fewer than "
                                + BRINDLE_MIN_COLTS + " - too few to show a brindle colt, and a sex ratio to look at");
                    }
                }
            }
            default -> { }  // BORN_121 is judged on the scan, once its homozygotes have had FOAL_KEEP to die
        }
    }

    /**
     * A conception by this pen's mare, embryos and all.
     *
     * <p>READ AT THE NEXT CONCEPTION, not at the fortieth or the tenth: a mare cannot conceive while
     * pregnant or just foaled ({@code Conception.Outcome.NOT_RECEPTIVE}), so at conception N+1 every
     * pregnancy before it has ended - lost, or born and already counted by {@link #count}. A PASS read
     * any earlier could be followed by the very foal it said would never come.
     */
    private static void judgeConception(Tally t, Pregnancy p) {
        if (t.rule == Rule.NONE || t.closed) {
            return;
        }
        if (t.rule == Rule.CONCEIVED_121 && t.draws >= VERDICT_DRAWS) {
            ratioVerdict(t, t.drawnBy, "at conception " + (t.conceptions + 1) + " (the " + t.draws
                    + " draws before it, every pregnancy ended)", String.format(java.util.Locale.ROOT,
                    "; born %s %d, %s %d, %s %d", label(t, 1), t.bornBy[1], label(t, 0), t.bornBy[0],
                    label(t, 2), t.bornBy[2]));
        }
        if (t.rule == Rule.NO_FOAL && t.conceptions >= MILK_CONCEPTIONS && t.born == 0) {
            pass(t, "at conception " + (t.conceptions + 1) + ": the " + t.conceptions + " pregnancies before it all"
                    + " ended with no foal, every embryo Watr/Lava and lost to the milk rule (" + MILK_CAUSE + ")");
        }
        t.conceptions++;
        List<Embryo> embryos = p.embryos();
        boolean identical = p.identicalTwins();
        for (int i = 0; i < embryos.size(); i++) {
            if (i > 0 && identical) {
                continue;   // one zygote that split: one draw
            }
            Embryo e = embryos.get(i);
            String tok = tokens(e.foal().genotype().pair(t.key));
            String at = "at conception " + t.conceptions;
            if (t.rule == Rule.CONCEIVED_121) {
                int m = copies(tok, t.mutant);
                if (m < 0) {
                    continue;
                }
                t.drawnBy[m]++;
                t.draws++;
                noteLabel(t, m, tok);
                if (m == 2 && !e.lostEarly()) {
                    fail(t, at, "a " + tok + " embryo was not marked to be lost; it will be carried to term");
                }
            } else if (t.rule == Rule.NO_FOAL) {
                Optional<Condition> cause = Conceivable.failure(e.foal().genotype());
                if (!(tok.contains("Watr") && tok.contains("Lava"))) {
                    fail(t, at, "an embryo drew " + tok + "; Watr/Watr x Lava/Lava can only make Watr/Lava");
                } else if (!e.lostEarly()) {
                    fail(t, at, "a Watr/Lava embryo was not marked to be lost; it will be carried to term");
                } else if (cause.isEmpty() || !MILK_CAUSE.equals(cause.get().id())) {
                    fail(t, at, "the Watr/Lava embryo is lost, but " + (cause.isEmpty()
                            ? "by no gene's canOccur - a trait lethal such as MET's did it"
                            : "to " + cause.get().id() + ", not " + MILK_CAUSE));
                }
            }
        }
    }

    /** Every ten seconds, from the pen's own scan. */
    private static void judgeScan(ServerLevel level, Tally t, long now) {
        if (t.rule == Rule.NONE || t.closed || t.judged) {
            return;
        }
        if (t.rule == Rule.BORN_121) {
            int n = t.bornBy[0] + t.bornBy[1] + t.bornBy[2];
            // A lethal pen waits while a homozygote is still inside its FOAL_KEEP: past it, alive, it FAILs.
            if (n >= VERDICT_FOALS && !(t.homozygoteDies && livingHomozygote(t))) {
                ratioVerdict(t, t.bornBy, "at " + n + " foals born", t.homozygoteDies
                        ? "; every " + label(t, 2) + " dead within " + FOAL_KEEP / 20 + " s of birth" : "");
                return;
            }
        }
        if (t.damEntity == null || !(level.getEntity(t.damEntity) instanceof Horse dam && dam.isAlive())) {
            if (++t.mareMissing >= MARE_GONE_SCANS) {
                inconclusive(t, "at " + t.born + " foals", "its mare is gone, so no more of its foals or"
                        + " conceptions can be counted");
            }
            return;
        }
        t.mareMissing = 0;
        if (now - t.startTick >= RATIO_DEADLINE) {
            String got = switch (t.rule) {
                case CONCEIVED_121 -> t.draws + " conception draws of " + VERDICT_DRAWS;
                case NO_FOAL -> t.conceptions + " conceptions of " + (MILK_CONCEPTIONS + 1);
                default -> t.born + " foals of " + VERDICT_FOALS;
            };
            inconclusive(t, "at " + RATIO_DEADLINE / 1200 + " min", "only " + got + " in eight hours; the pair"
                    + " bred too slowly to judge");
        }
    }

    private static boolean livingHomozygote(Tally t) {
        for (UUID id : t.living.keySet()) {
            if (copies(t.classOf.get(id), t.mutant) == 2) {
                return true;
            }
        }
        return false;
    }
}
