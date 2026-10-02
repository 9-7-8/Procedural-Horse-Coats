package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicFactorGene;
import com.example.horsegenetics.common.genetics.genes.FireproofGene;
import com.example.horsegenetics.common.genetics.genes.MagicWaterBreathingGene;
import com.example.horsegenetics.common.genetics.genes.OceanBornGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <b>Row BA: what fire and water do to a horse whose genes say otherwise</b> (2026-10-02). Two clockwork pens,
 * both of which start themselves at build, run on {@link DebugYardHerd#after} and answer every question they ask
 * with one {@code CLOCKWORK PASS/FAIL/INCONCLUSIVE} line - nobody has to be there.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>page</th></tr>
 *   <tr><td>west, x0 .. x0+19</td><td>FIREPROOF - four glass cells (two on netherrack fire, two for plain
 *       hits), three sealed lava lanes, two 4-deep lava pools</td><td>gene-fireproof</td></tr>
 *   <tr><td>east, x0+25 .. x0+44</td><td>BREATH - four lidded water tanks, each with a dry glass cell
 *       behind it to lift its horse into</td><td>gene-magic-water-breathing</td></tr>
 * </table>
 *
 * <h2>Every fluid is in a box</h2>
 * Each lava lane, lava pool and water tank is a stone floor, glass walls and a stone lid, with the fluid placed as
 * source blocks wholly inside - nothing flows, and nothing reaches the walkway or the next row.
 *
 * <h2>Why some horses here have no AI goals</h2>
 * Two of the questions are about physics a goal would hide. A vanilla {@code FloatGoal} jumps a mob up through
 * water <i>and lava</i> on its own, so a horse that bobs at a lava surface with its goals intact proves nothing about
 * the gene's buoyancy, and a horse in a water tank with them intact swims its eyes up against the lid (a horse's
 * eyes are 0.08 under the top of its box, a water source's surface 0.11 under the block above it, so a horse
 * pressed to a lid one block above the water <i>breathes</i>). Such horses have every goal removed
 * ({@link #stripGoals}) 40 ticks after build - after the mod's join-time goals have gone on - and then sink and hold
 * still. Nothing about air, damage or lava travel reads a goal.
 *
 * <p>Total: the west half answers by about +800 ticks, the east by about +2,000 at most; a deadline at
 * {@value #DEADLINE} ticks answers INCONCLUSIVE for anything still open.
 */
final class DebugYardTraits {

    private DebugYardTraits() {
    }

    // ------------------------------------------------------------------
    // Shared state
    // ------------------------------------------------------------------

    /** Bumped by every build, so a clock step left over from the yard before this one stops itself. */
    private static int run;
    /** Every check this row registered, and those it has answered - the deadline sweeps the difference. */
    private static final Set<String> MINE = new LinkedHashSet<>();
    private static final Set<String> ANSWERED = new HashSet<>();

    /** Every check answers by this, INCONCLUSIVE if its own clock never got there. Five minutes. */
    private static final long DEADLINE = 6000L;

    /** The entry point: the yard calls this once per build. */
    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        MINE.clear();
        ANSWERED.clear();
        try {
            buildFireproof(level, gy, x0, z0, myRun);
            ActionTrace.log("test yard", "row BA west built: " + FIRE_PEN + " (fire at +40, hits at +40/+80,"
                    + " lanes driven +60..+260, pools sampled +200..+800)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BA west (" + FIRE_PEN + ") failed to build", e);
        }
        try {
            buildBreath(level, gy, x0 + 25, z0, myRun);
            ActionTrace.log("test yard", "row BA east built: " + BREATH_PEN + " (dives start at +"
                    + DIVE_START + " ticks)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BA east (" + BREATH_PEN + ") failed to build", e);
        }
        DebugYardHerd.after(level, DEADLINE, () -> {
            if (myRun != run) {
                return;
            }
            for (String c : MINE) {
                unsure(c, "no answer " + DEADLINE / 20 + " s after build - the pen's own clock never reached it"
                        + " (look for a '[Debug] herd rows: a timed step failed' WARN, or a build WARN for row BA)");
            }
        });
    }

    private static void expect(String check) {
        MINE.add(check);
        DebugYardClockwork.expect(check);
    }

    private static void answer(String check, boolean pass, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }

    private static void unsure(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.inconclusive(check, detail);
        }
    }

    /** A clock step that stops itself if the yard was rebuilt, and answers INCONCLUSIVE if it throws. */
    private static void step(ServerLevel level, long ticks, int thisRun, List<String> checks, Runnable body) {
        DebugYardHerd.after(level, ticks, () -> {
            if (thisRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a row BA step threw", e);
                for (String c : checks) {
                    unsure(c, "the step threw " + e);
                }
            }
        });
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static boolean alive(@Nullable LivingEntity e) {
        return e != null && e.isAlive() && !e.isRemoved();
    }

    /**
     * Take every goal off a mob, so nothing but physics moves it. The list is copied first, the way
     * {@code HorseAggroHandler} swaps its panic goals, because {@code removeGoal} edits the set being read.
     */
    private static int stripGoals(Mob m) {
        List<WrappedGoal> all = new ArrayList<>(m.goalSelector.getAvailableGoals());
        for (WrappedGoal w : all) {
            m.goalSelector.removeGoal(w.getGoal());
        }
        m.getNavigation().stop();
        return all.size();
    }

    /**
     * Move a live mob. UNVERIFIED: {@code Entity.teleportTo(double, double, double)} is read off the 26.1.2 sources
     * (server side it is {@code snapTo} plus the passengers) but nothing else in this repo calls it on a mob.
     */
    private static void moveTo(Entity e, double x, double y, double z) {
        e.teleportTo(x, y, z);
        e.setDeltaMovement(Vec3.ZERO);
    }

    /** Fire resistance for ten minutes, so lava cannot kill a control before it has been measured. */
    private static void fireResist(LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12_000, 0, true, false, false));
    }

    // ==================================================================
    // WEST - FIREPROOF
    // ==================================================================

    /*
     * THE QUESTIONS (each quoted from wiki/gene-fireproof.html's Verification tab, 2026-10-02).
     *
     * "A carrier burns. Spawn a Frp/n horse from the custom horse spawn egg and stand it in fire, with you aboard.
     *   Pass: both of you take fire damage on the usual schedule. The immunity is a damage cancel keyed on the
     *   expressing horse, and a cancel that is too wide makes every horse in the world fireproof without saying so."
     *   THE HORSE HALF ONLY: a FakePlayer cannot ride (DebugYardClockwork's javadoc), so the rider half stays the
     *   owner's - the check name says "horse only" so a PASS cannot be read as closing both.
     *   Two 4x4 glass cells on netherrack, an Frp/n horse in one and an Frp/Frp horse in the other as the control
     *   that the fire is real AND that the cancel still works. Health is read at +40, the 2x2 floor of each cell
     *   is set alight (and relit every 20 ticks, in case this dimension does not count netherrack as
     *   infiniburn), and health is read again 100 ticks later. The fire is put out after the reading.
     *   PASS: the carrier lost at least 1 health (or died) and the Frp/Frp horse lost none. FAIL: the carrier lost
     *   nothing, or the homozygote lost any. INCONCLUSIVE: a horse missing at the first reading. ~7 s.
     *
     * "Only fire is cancelled. Hurt an expressing horse with a sword and with a fall. Pass: both land normally.
     *   The filter is the minecraft:is_fire damage-type tag, and an invulnerable horse looks like a generous gene
     *   rather than a bug."
     *   An Frp/Frp horse and an n/n control, each alone in a glass cell, are hit with
     *   hurtServer(generic, 4) at +40 and hurtServer(fall, 4) at +80 - generic standing in for the sword (a
     *   player attack needs a player; both are outside is_fire, which is the whole filter). Health is read
     *   either side of each hit, in the same tick.
     *   PASS: the Frp/Frp horse lost health to both, and the same amount as the control (within 0.01). FAIL: it lost
     *   none to either, or less than the control. INCONCLUSIVE: the control lost nothing (something in this world
     *   is cancelling all horse damage, so the Frp/Frp reading would mean nothing). ~4 s.
     *
     * "The mixin is per horse and not per world. Push an ordinary horse, and any other mob, into lava. Pass: they
     *   move at vanilla's crawl. LavaTravelMixin replaces a hardcoded constant for every entity that swims in lava,
     *   and it reading the attribute for all of them would be invisible until somebody noticed a fast zombie."
     *   Three sealed lava lanes, 2 deep and 13 long: an Frp/Frp horse, an n/n horse and a pig, all three given
     *   fire resistance (which does not touch lava travel) so the controls live to be measured. Goals stripped at
     *   +40; from +60, every tick for 200 ticks, each mob's navigation is stopped and its MoveControl aimed down
     *   its lane with a speed modifier of DRIVE / its own movement_speed - so every one of them feeds the same
     *   forward input (zza = DRIVE) into travelInLava, and only the lava constant differs. Vanilla's lava
     *   travel is moveRelative(0.02 x input) then halving, which settles at 2 x 0.02 x DRIVE = 0.01 blocks a
     *   tick: 2.0 blocks in 200 ticks. lava_movement is read off each mob too: the plain horse's must be
     *   exactly the attribute's default 0.02, and a pig has no such attribute (so the mixin returns vanilla's
     *   constant for it).
     *   PASS: plain lava_movement 0.02, the pig has none, plain and pig each moved <= 4.0 blocks, and the Frp/Frp
     *   horse moved at least three times as far as either. FAIL: the plain horse's attribute is not 0.02, the pig
     *   has the attribute, or the plain horse or the pig moved > 4.0. INCONCLUSIVE: a mob gone or out of the
     *   lava, or the Frp/Frp horse - the positive control - not three times faster (then the drive is not
     *   working and a slow plain horse proves nothing). ~13 s.
     *
     * "An unridden one floats too. The buoyancy is a gravity modifier on the horse, but it was built and confirmed
     *   under a rider. Drive a riderless expressing horse into a lava pool. Pass: it rises and bobs at the surface
     *   rather than sinking to the floor and staying there."
     *   Two sealed pools of lava 4 deep (surface at gy + 4 + 8/9, a source block's height), an Frp/Frp horse
     *   spawned on the floor of each. Pool A's horse keeps its goals: the horse a player would see. Pool B's has
     *   its goals stripped at +40 and is put back on the floor at +60, so FloatGoal's own jumping cannot be
     *   what lifts it - the second check is the gene's buoyancy alone. From +200 (time to rise four blocks at
     *   about 0.04 a tick) feet Y is sampled every 20 ticks for 30 s.
     *   PASS: every sample's feet within 1 block of the surface (surface - 1 .. surface + 1). FAIL: any sample
     *   lower (sunk) or higher (thrown out). INCONCLUSIVE: the horse dead or gone, or the pool's lava gone. ~40 s.
     */

    private static final String FIRE_PEN = "FIREPROOF";
    private static final String FRP = FireproofGene.KEY;
    private static final String BURN = FIRE_PEN + " - an Frp/n carrier standing in fire loses health, an Frp/Frp"
            + " beside it loses none (horse only, no rider)";
    private static final String ONLY_FIRE = FIRE_PEN + " - an Frp/Frp horse takes generic and fall damage in full,"
            + " the same as a plain horse";
    private static final String PER_HORSE = FIRE_PEN + " - lava speed is per horse: a plain horse (lava_movement"
            + " 0.02) and a pig crawl through lava, an Frp/Frp horse swims";
    private static final String FLOAT = FIRE_PEN + " - an unridden Frp/Frp horse stays within 1 block of the"
            + " surface of a 4-deep lava pool";
    private static final String FLOAT_BARE = FIRE_PEN + " - with its AI goals stripped, an unridden Frp/Frp horse's"
            + " own buoyancy lifts it off the pool floor and holds it at the surface";

    private static final long FIRE_AT = 40L;
    private static final long BURN_FOR = 100L;
    private static final long DRIVE_AT = 60L;
    private static final int DRIVE_TICKS = 200;
    /** The forward input every lane mob is given - a walk's worth, so a fast swimmer stays inside its lane longer. */
    private static final double DRIVE = 0.25;
    private static final double VANILLA_LAVA = ModAttributes.VANILLA_LAVA_SPEED;
    private static final double CRAWL_MAX = 4.0;
    private static final long FLOAT_FROM = 200L;
    private static final int FLOAT_SAMPLES = 30;

    /** One lava lane: the mob, where its lane runs, and what was read. */
    private static final class Lane {
        final String label;
        final Mob mob;
        final double z;
        double x0;
        double x60;
        Lane(String label, Mob mob, double z) {
            this.label = label;
            this.mob = mob;
            this.z = z;
        }
    }

    private static void buildFireproof(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : List.of(BURN, ONLY_FIRE, PER_HORSE, FLOAT, FLOAT_BARE)) {
            expect(c);
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("FIREPROOF", "fire, hits,", "lava lanes,", "lava pools"));

        // --- Row A: four 4x4 glass cells along z0 .. z0+3 ---
        List<BlockPos> fire = new ArrayList<>();
        Horse carrier = fireCell(level, gy, x0, z0, "Frp/n", fire);
        Horse homo = fireCell(level, gy, x0 + 5, z0, "Frp/Frp", fire);
        DebugYardClockwork.cell(level, gy, x0 + 10, x0 + 13, z0, z0 + 3);
        Horse hitFrp = DebugYardClockwork.horse(level, gy, x0 + 12.0, z0 + 2.0, Sex.MALE, FRP + "=Frp/Frp",
                FIRE_PEN + " hits Frp/Frp");
        DebugYardClockwork.cell(level, gy, x0 + 15, x0 + 18, z0, z0 + 3);
        Horse hitPlain = DebugYardClockwork.horse(level, gy, x0 + 17.0, z0 + 2.0, Sex.MALE, FRP + "=n/n",
                FIRE_PEN + " hits n/n control");
        scheduleBurn(level, myRun, carrier, homo, fire);
        scheduleHits(level, myRun, hitFrp, hitPlain);

        // --- Row B west: three lava lanes, x0 .. x0+14, z0+4 .. z0+12 ---
        lanes(level, gy, x0, z0);
        List<Lane> lanes = new ArrayList<>();
        Horse laneFrp = DebugYardClockwork.horse(level, gy, x0 + 2.5, z0 + 6.0, Sex.MALE, FRP + "=Frp/Frp",
                FIRE_PEN + " lane Frp/Frp");
        Horse lanePlain = DebugYardClockwork.horse(level, gy, x0 + 2.5, z0 + 9.0, Sex.MALE, FRP + "=n/n",
                FIRE_PEN + " lane n/n");
        Entity pigE = DebugYardUnattended.animal(level, EntityType.PIG, gy, x0 + 2.0, z0 + 11.5);
        Mob pig = pigE instanceof Mob m ? m : null;
        if (laneFrp != null) {
            fireResist(laneFrp);
            lanes.add(new Lane("Frp/Frp", laneFrp, z0 + 6.0));
        }
        if (lanePlain != null) {
            fireResist(lanePlain);
            lanes.add(new Lane("n/n", lanePlain, z0 + 9.0));
        }
        if (pig != null) {
            fireResist(pig);
            lanes.add(new Lane("pig", pig, z0 + 11.5));
        }
        scheduleLanes(level, myRun, x0, lanes, laneFrp, lanePlain, pig);

        // --- Row B east: two 4-deep lava pools, x0+15 .. x0+19 ---
        pool(level, gy, x0 + 15, z0 + 4);
        pool(level, gy, x0 + 15, z0 + 8);
        Horse floatA = DebugYardClockwork.horse(level, gy, x0 + 17.5, z0 + 6.5, Sex.MALE, FRP + "=Frp/Frp",
                FIRE_PEN + " pool, goals on");
        Horse floatB = DebugYardClockwork.horse(level, gy, x0 + 17.5, z0 + 10.5, Sex.MALE, FRP + "=Frp/Frp",
                FIRE_PEN + " pool, goals stripped");
        double surface = gy + 4 + 8.0 / 9.0;
        BlockPos lavaA = new BlockPos(x0 + 17, gy + 4, z0 + 6);
        BlockPos lavaB = new BlockPos(x0 + 17, gy + 4, z0 + 10);
        step(level, 40, myRun, List.of(FLOAT_BARE), () -> {
            if (alive(floatB)) {
                stripGoals(floatB);
            }
        });
        step(level, 60, myRun, List.of(FLOAT_BARE), () -> {
            if (alive(floatB)) {
                moveTo(floatB, x0 + 17.5, gy + 1, z0 + 10.5);   // back to the floor, with nothing to jump it up
            }
        });
        sampleFloat(level, myRun, FLOAT, floatA, lavaA, surface, new StringBuilder(), 0);
        sampleFloat(level, myRun, FLOAT_BARE, floatB, lavaB, surface, new StringBuilder(), 0);
    }

    /** A 4x4 glass cell with a netherrack floor under its 2x2 inside, one horse in it; the fire is lit later. */
    private static @Nullable Horse fireCell(ServerLevel level, int gy, int x, int z0, String pair,
                                            List<BlockPos> fire) {
        DebugYardClockwork.cell(level, gy, x, x + 3, z0, z0 + 3);
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = 1; dz <= 2; dz++) {
                level.setBlock(new BlockPos(x + dx, gy, z0 + dz), Blocks.NETHERRACK.defaultBlockState(), 3);
                fire.add(new BlockPos(x + dx, gy + 1, z0 + dz));
            }
        }
        return DebugYardClockwork.horse(level, gy, x + 2.0, z0 + 2.0, Sex.MALE, FRP + "=" + pair,
                FIRE_PEN + " in fire " + pair);
    }

    /**
     * Light (or relight) every fire cell. UNVERIFIED: nothing else in this repo places {@code Blocks.FIRE}; its
     * default state on a netherrack floor is what flint and steel would give, read off the 26.1.2 sources.
     */
    private static void light(ServerLevel level, List<BlockPos> fire) {
        for (BlockPos p : fire) {
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private static void scheduleBurn(ServerLevel level, int myRun, @Nullable Horse carrier, @Nullable Horse homo,
                                     List<BlockPos> fire) {
        float[] before = new float[2];
        step(level, FIRE_AT, myRun, List.of(BURN), () -> {
            if (!alive(carrier) || !alive(homo)) {
                unsure(BURN, "a horse is missing before the fire was lit: carrier " + alive(carrier)
                        + ", Frp/Frp " + alive(homo));
                return;
            }
            before[0] = carrier.getHealth();
            before[1] = homo.getHealth();
            light(level, fire);
        });
        for (long t = FIRE_AT + 20; t < FIRE_AT + BURN_FOR; t += 20) {
            step(level, t, myRun, List.of(BURN), () -> light(level, fire));
        }
        step(level, FIRE_AT + BURN_FOR, myRun, List.of(BURN), () -> {
            if (carrier == null || homo == null) {
                unsure(BURN, "a horse never spawned");
                return;
            }
            int lit = 0;
            for (BlockPos p : fire) {
                if (level.getBlockState(p).is(Blocks.FIRE)) {
                    lit++;
                }
            }
            float c = carrier.isAlive() ? carrier.getHealth() : 0.0F;
            float h = homo.isAlive() ? homo.getHealth() : 0.0F;
            double carrierLost = before[0] - c;
            double homoLost = before[1] - h;
            String detail = "over " + BURN_FOR + " ticks in fire (" + lit + "/" + fire.size()
                    + " fire blocks still lit at the reading): Frp/n " + f(before[0]) + " -> " + f(c)
                    + (carrier.isAlive() ? "" : " (DIED)") + ", lost " + f(carrierLost)
                    + "; Frp/Frp " + f(before[1]) + " -> " + f(h) + (homo.isAlive() ? "" : " (DIED)")
                    + ", lost " + f(homoLost) + " | the rider half of the page's check is not tested here";
            if (before[0] <= 0.0F) {
                return;     // the first step already answered INCONCLUSIVE
            }
            answer(BURN, carrierLost >= 1.0 && homoLost <= 0.001, detail);
            for (BlockPos p : fire) {
                if (level.getBlockState(p).is(Blocks.FIRE)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        });
    }

    private static void scheduleHits(ServerLevel level, int myRun, @Nullable Horse frp, @Nullable Horse plain) {
        double[] generic = new double[2];
        step(level, 40, myRun, List.of(ONLY_FIRE), () -> {
            if (!alive(frp) || !alive(plain)) {
                unsure(ONLY_FIRE, "a horse is missing before the first hit: Frp/Frp " + alive(frp) + ", n/n "
                        + alive(plain));
                return;
            }
            generic[0] = hit(level, frp, true);
            generic[1] = hit(level, plain, true);
        });
        step(level, 80, myRun, List.of(ONLY_FIRE), () -> {
            if (!alive(frp) || !alive(plain)) {
                unsure(ONLY_FIRE, "a horse is missing before the fall hit: Frp/Frp " + alive(frp) + ", n/n "
                        + alive(plain) + "; generic took Frp/Frp " + f(generic[0]) + ", n/n " + f(generic[1]));
                return;
            }
            double frpFall = hit(level, frp, false);
            double plainFall = hit(level, plain, false);
            String detail = "hurtServer 4.0 each - generic: Frp/Frp lost " + f(generic[0]) + ", n/n lost "
                    + f(generic[1]) + "; fall: Frp/Frp lost " + f(frpFall) + ", n/n lost " + f(plainFall)
                    + " (generic stands in for the page's sword: a player attack needs a player)";
            if (generic[1] <= 0.0 || plainFall <= 0.0) {
                unsure(ONLY_FIRE, "the n/n control took no damage - something is cancelling all horse damage here; "
                        + detail);
                return;
            }
            boolean ok = generic[0] > 0.0 && frpFall > 0.0
                    && Math.abs(generic[0] - generic[1]) <= 0.01 && Math.abs(frpFall - plainFall) <= 0.01;
            answer(ONLY_FIRE, ok, detail);
        });
    }

    /** Hit once for 4 and return the health lost, read either side of the call in the same tick. */
    private static double hit(ServerLevel level, Horse h, boolean generic) {
        float before = h.getHealth();
        h.hurtServer(level, generic ? level.damageSources().generic() : level.damageSources().fall(), 4.0F);
        return before - h.getHealth();
    }

    /**
     * Three lanes along x, walls at z0+4, z0+7, z0+10 and z0+12 and at x0 and x0+14: two 2-wide horse lanes and a
     * 1-wide pig lane, lava 2 deep on a stone floor, glass to gy+4 and a stone lid at gy+5.
     */
    private static void lanes(ServerLevel level, int gy, int x0, int z0) {
        Set<Integer> wallZ = Set.of(z0 + 4, z0 + 7, z0 + 10, z0 + 12);
        for (int x = x0; x <= x0 + 14; x++) {
            for (int z = z0 + 4; z <= z0 + 12; z++) {
                boolean wall = x == x0 || x == x0 + 14 || wallZ.contains(z);
                level.setBlock(new BlockPos(x, gy, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = gy + 1; y <= gy + 4; y++) {
                    BlockState s = wall ? Blocks.GLASS.defaultBlockState()
                            : y <= gy + 2 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), s, 3);
                }
                level.setBlock(new BlockPos(x, gy + 5, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    /** A 5x5 pool (3x3 inside) at {@code (x, z)}: stone floor, lava gy+1 .. gy+4, glass to gy+7, stone lid at gy+8. */
    private static void pool(ServerLevel level, int gy, int x, int z) {
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                boolean wall = dx == 0 || dx == 4 || dz == 0 || dz == 4;
                level.setBlock(new BlockPos(x + dx, gy, z + dz), Blocks.STONE.defaultBlockState(), 3);
                for (int y = gy + 1; y <= gy + 7; y++) {
                    BlockState s = wall ? Blocks.GLASS.defaultBlockState()
                            : y <= gy + 4 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(new BlockPos(x + dx, y, z + dz), s, 3);
                }
                level.setBlock(new BlockPos(x + dx, gy + 8, z + dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private static void scheduleLanes(ServerLevel level, int myRun, int x0, List<Lane> lanes, @Nullable Horse frp,
                                      @Nullable Horse plain, @Nullable Mob pig) {
        step(level, 40, myRun, List.of(PER_HORSE), () -> {
            for (Lane l : lanes) {
                if (alive(l.mob)) {
                    stripGoals(l.mob);
                }
            }
        });
        step(level, DRIVE_AT, myRun, List.of(PER_HORSE), () -> {
            if (!alive(frp) || !alive(plain) || !alive(pig)) {
                unsure(PER_HORSE, "a lane mob is missing at the start: Frp/Frp " + alive(frp) + ", n/n "
                        + alive(plain) + ", pig " + alive(pig));
                return;
            }
            for (Lane l : lanes) {
                l.x0 = l.mob.getX();
            }
            drive(level, myRun, x0, lanes, frp, plain, pig, 0);
        });
    }

    /** One tick of the drive: stop each mob's path and aim its move control down its lane at input DRIVE. */
    private static void drive(ServerLevel level, int myRun, int x0, List<Lane> lanes, Horse frp, Horse plain,
                              Mob pig, int tick) {
        if (tick == 60) {
            for (Lane l : lanes) {
                l.x60 = l.mob.getX();
            }
        }
        if (tick >= DRIVE_TICKS) {
            judgeLanes(lanes, frp, plain, pig);
            return;
        }
        for (Lane l : lanes) {
            if (!alive(l.mob)) {
                continue;
            }
            l.mob.getNavigation().stop();
            double speed = l.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            l.mob.getMoveControl().setWantedPosition(x0 + 100.0, l.mob.getY(), l.z,
                    speed > 1e-6 ? DRIVE / speed : 1.0);
        }
        step(level, 1, myRun, List.of(PER_HORSE), () -> drive(level, myRun, x0, lanes, frp, plain, pig, tick + 1));
    }

    private static void judgeLanes(List<Lane> lanes, Horse frp, Horse plain, Mob pig) {
        StringBuilder sb = new StringBuilder();
        for (Lane l : lanes) {
            sb.append(sb.length() == 0 ? "" : "; ").append(l.label).append(" moved ")
                    .append(f(l.mob.getX() - l.x0)).append(" in ").append(DRIVE_TICKS).append(" ticks (")
                    .append(f(l.x60 - l.x0)).append(" in the first 60), in lava ").append(l.mob.isInLava())
                    .append(", y ").append(f(l.mob.getY()));
        }
        AttributeInstance plainAttr = plain.getAttribute(ModAttributes.LAVA_MOVEMENT);
        AttributeInstance frpAttr = frp.getAttribute(ModAttributes.LAVA_MOVEMENT);
        AttributeInstance pigAttr = pig.getAttribute(ModAttributes.LAVA_MOVEMENT);
        double pv = plainAttr == null ? Double.NaN : plainAttr.getValue();
        double fv = frpAttr == null ? Double.NaN : frpAttr.getValue();
        double vanillaDisp = DRIVE_TICKS * 2 * VANILLA_LAVA * DRIVE;
        String detail = "lava_movement: n/n " + (plainAttr == null ? "(none)" : String.format(Locale.ROOT, "%.4f", pv))
                + ", Frp/Frp " + (frpAttr == null ? "(none)" : String.format(Locale.ROOT, "%.4f", fv))
                + ", pig " + (pigAttr == null ? "(no attribute - the mixin returns vanilla's constant)"
                : String.format(Locale.ROOT, "HAS IT: %.4f", pigAttr.getValue()))
                + " | forward input " + DRIVE + " each; vanilla predicts " + f(vanillaDisp) + " blocks, Frp/Frp's"
                + " attribute predicts " + f(DRIVE_TICKS * 2 * fv * DRIVE) + " (the lane caps it near 10.8) | " + sb;
        if (!alive(frp) || !alive(plain) || !alive(pig) || !plain.isInLava() || !pig.isInLava()) {
            unsure(PER_HORSE, "a lane mob died or left the lava mid-drive; " + detail);
            return;
        }
        double plainDisp = plain.getX() - lanes.stream().filter(l -> l.mob == plain).findFirst().map(l -> l.x0)
                .orElse(plain.getX());
        double pigDisp = pig.getX() - lanes.stream().filter(l -> l.mob == pig).findFirst().map(l -> l.x0)
                .orElse(pig.getX());
        double frpDisp = frp.getX() - lanes.stream().filter(l -> l.mob == frp).findFirst().map(l -> l.x0)
                .orElse(frp.getX());
        boolean plainVanilla = plainAttr != null && Math.abs(pv - VANILLA_LAVA) <= 1e-9;
        boolean crawl = Math.abs(plainDisp) <= CRAWL_MAX && Math.abs(pigDisp) <= CRAWL_MAX;
        if (!plainVanilla || pigAttr != null || !crawl) {
            answer(PER_HORSE, false, detail);
            return;
        }
        double floor = Math.max(0.3, Math.max(Math.abs(plainDisp), Math.abs(pigDisp)));
        if (frpDisp < 3.0 * floor) {
            unsure(PER_HORSE, "the Frp/Frp horse (the positive control) was not three times faster, so the drive"
                    + " may not be moving anything; " + detail);
            return;
        }
        answer(PER_HORSE, true, detail);
    }

    /** One float sample every 20 ticks from FLOAT_FROM, FLOAT_SAMPLES of them, then the verdict. */
    private static void sampleFloat(ServerLevel level, int myRun, String check, @Nullable Horse h, BlockPos lava,
                                    double surface, StringBuilder ys, int n) {
        long at = n == 0 ? FLOAT_FROM : 20L;
        step(level, at, myRun, List.of(check), () -> {
            if (!alive(h)) {
                unsure(check, "the horse is dead or gone after " + n + " sample(s); feet Y " + ys);
                return;
            }
            if (!level.getBlockState(lava).is(Blocks.LAVA)) {
                unsure(check, "the pool's lava is gone at " + lava.toShortString() + "; feet Y " + ys);
                return;
            }
            ys.append(n == 0 ? "" : ",").append(String.format(Locale.ROOT, "%.2f", h.getY() - surface));
            if (n + 1 < FLOAT_SAMPLES) {
                sampleFloat(level, myRun, check, h, lava, surface, ys, n + 1);
                return;
            }
            String[] parts = ys.toString().split(",");
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            for (String p : parts) {
                double v = Double.parseDouble(p);
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            String detail = FLOAT_SAMPLES + " samples over 30 s of feet Y minus the lava surface (" + f(surface)
                    + "; the floor is -3.89): min " + f(lo) + ", max " + f(hi) + " | " + ys;
            answer(check, lo >= -1.0 && hi <= 1.0, detail);
        });
    }

    // ==================================================================
    // EAST - BREATH
    // ==================================================================

    /*
     * THE QUESTIONS (each quoted from wiki/gene-magic-water-breathing.html's Verification tab, 2026-10-02).
     *
     * "The absolute flag wins. A horse that is both Ocn/Ocn (ocean-born, which refills its air every tick) and
     *   Shal/Shal. Pass: it never takes drowning damage however long it is held under."
     *   Held under for HOLD = 1200 ticks (four plain-horse drownings' worth), health and air read every tick.
     *   PASS: health never fell. FAIL: it fell at any tick (the detail gives the lowest air seen, so a drowning
     *   reads as air at -20 beside the drop). INCONCLUSIVE: the horse died of something else, or its eyes were
     *   under water for fewer than 90% of the ticks (the tank is not holding it down).
     *
     * "The debt does not survive surfacing. Hold a Gil/Gil horse under, let it up until its air is full, and hold
     *   it under again. Pass: the second dive lasts as long as the first. BREATH_DEBT is cleared when the horse
     *   surfaces and when it leaves the level, and a part-point of stale credit carried across dives is invisible
     *   until somebody times two of them."
     *   A dive is timed from the first tick its eyes are under water (Entity.isUnderWater, the very test
     *   GeneAbilityHandler.breathe reads) to the first tick its air is at or below 0. Then it is lifted into its
     *   dry cell, waits until its air is full and 20 ticks more, and is put back under.
     *   PASS: the two dives differ by at most 10 ticks. FAIL: by more. INCONCLUSIVE: a dive that never reached
     *   zero within DIVE_CAP, or never got its eyes under. ~1,000-1,700 ticks, the horse's own factor decides.
     *
     * "Gil/Shal breathes like a plain horse. Add one to the pen. Pass: it drowns with the plain horse rather than
     *   at either extreme - the sum is signed, and nothing checks for the pair."
     *   One dive each beside an n/n horse. The pair's factor is 1 + Gil's delta - Shal's, and each delta is a
     *   separate gaussian draw (mean 0.25, sd 0.14), so a correctly signed sum still lands up to ~100 ticks off
     *   a plain horse for an unlucky pair. So the Gil/Shal horse is re-rolled at build, up to 30 times, until its
     *   two copies' deltas (read off its own record) differ by at most 0.08 - at most 24 ticks of honest spread.
     *   Every horse's predicted factor is in the detail.
     *   PASS: the Gil/Shal horse's dive is within 40 ticks of the plain horse's. FAIL: further. INCONCLUSIVE:
     *   no balanced pair in 30 rolls, the plain horse's own dive outside 250-350 (vanilla's 300 - then the tank
     *   is not measuring breath), or either dive unfinished. ~350 ticks.
     *
     * The tanks: 4x4, a 2x2 of water three deep (gy+1 .. gy+3) on a stone floor, glass walls, and a stone lid at
     * gy+4, one block above the water. Every horse spawns in its own dry glass cell behind its tank (z0+5 .. z0+8)
     * so it starts with full air, has its goals stripped at DIVE_START (so it sinks rather than swims its eyes up
     * to the lid - see the class comment), and is then moved to its tank floor. Air is logged every 20 ticks of
     * each dive, run-length compressed ("299*60" is sixty readings of 299).
     */

    private static final String BREATH_PEN = "BREATH";
    private static final String BREATH = MagicWaterBreathingGene.KEY;
    private static final String ABSOLUTE = BREATH_PEN + " - an Ocn/Ocn Shal/Shal horse held under water 1200 ticks"
            + " never takes drowning damage";
    private static final String DEBT = BREATH_PEN + " - a Gil/Gil horse's second dive lasts as long as its first"
            + " (within 10 ticks) after surfacing to full air";
    private static final String BALANCED = BREATH_PEN + " - a Gil/Shal horse runs out of air within 40 ticks of a"
            + " plain n/n horse";

    private static final long DIVE_START = 40L;
    private static final int HOLD = 1200;
    private static final int DIVE_CAP = 1500;
    private static final int UNDER_GRACE = 60;
    private static final int REFILL_CAP = 400;
    private static final int MONITOR_CAP = 4800;
    private static final double BALANCE_GAP = 0.08;

    private enum Phase { DIVE, REFILL, DONE }

    /** One horse, its tank and dry cell, and everything read off it. */
    private static final class Diver {
        final String label;
        final Horse horse;
        final double tankX;
        final double tankZ;
        final double dryX;
        final double dryZ;
        final int floorY;
        final int divesWanted;
        final boolean hold;
        final double factor;
        Phase phase = Phase.DIVE;
        int phaseStart;
        int diveStart = -1;
        final List<int[]> dives = new ArrayList<>();
        final List<String> airLogs = new ArrayList<>();
        final List<Integer> air = new ArrayList<>();
        int minAir = Integer.MAX_VALUE;
        int underTicks;
        int healthDrops;
        float firstHealth;
        float lastHealth;
        @Nullable String problem;

        Diver(String label, Horse horse, int gy, double tankX, double tankZ, double dryX, double dryZ,
              int divesWanted, boolean hold) {
            this.label = label;
            this.horse = horse;
            this.floorY = gy + 1;
            this.tankX = tankX;
            this.tankZ = tankZ;
            this.dryX = dryX;
            this.dryZ = dryZ;
            this.divesWanted = divesWanted;
            this.hold = hold;
            this.factor = breathFactor(horse);
        }

        int length(int i) {
            int[] d = dives.get(i);
            return d[1] - d[0];
        }

        void dunk() {
            moveTo(horse, tankX, floorY, tankZ);
        }

        void lift() {
            moveTo(horse, dryX, floorY, dryZ);
        }

        String says() {
            StringBuilder sb = new StringBuilder(label).append(String.format(Locale.ROOT, " (factor x%.3f, ~%d ticks)",
                    factor, Double.isNaN(factor) ? -1 : Math.round(300 * factor)));
            for (int i = 0; i < dives.size(); i++) {
                sb.append(" dive ").append(i + 1).append(": ").append(length(i)).append(" ticks [")
                        .append(airLogs.get(i)).append(']');
            }
            if (hold) {
                sb.append(" held ").append(HOLD).append(", eyes under ").append(underTicks).append(", min air ")
                        .append(minAir).append(", health drops ").append(healthDrops).append(" (")
                        .append(f(firstHealth)).append(" -> ").append(f(lastHealth)).append(") [")
                        .append(airLogs.isEmpty() ? compress(air) : airLogs.get(0)).append(']');
            }
            if (problem != null) {
                sb.append(" PROBLEM: ").append(problem);
            }
            return sb.toString();
        }
    }

    private static void buildBreath(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : List.of(ABSOLUTE, DEBT, BALANCED)) {
            expect(c);
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("BREATH", "held under,", "timed dives", "row BA east"));
        String[] labels = {"Ocn/Ocn Shal/Shal", "Gil/Gil", "Gil/Shal", "n/n"};
        String[] codes = {
                OceanBornGene.KEY + "=Ocn/Ocn-" + BREATH + "=Shal/Shal",
                BREATH + "=Gil/Gil",
                BREATH + "=Gil/Shal",
                BREATH + "=n/n"};
        List<Diver> divers = new ArrayList<>();
        Diver[] byRole = new Diver[4];
        double[] gap = {Double.NaN};
        for (int i = 0; i < 4; i++) {
            int x = x0 + 5 * i;
            tank(level, gy, x, z0);
            DebugYardClockwork.cell(level, gy, x, x + 3, z0 + 5, z0 + 8);
            String label = BREATH_PEN + " " + labels[i];
            Horse h = i == 2
                    ? balanced(level, gy, x + 2.0, z0 + 7.0, codes[i], label, gap)
                    : DebugYardClockwork.horse(level, gy, x + 2.0, z0 + 7.0, Sex.MALE, codes[i], label);
            if (h == null) {
                continue;
            }
            Diver d = new Diver(labels[i], h, gy, x + 2.0, z0 + 2.0, x + 2.0, z0 + 7.0,
                    i == 1 ? 2 : 1, i == 0);
            divers.add(d);
            byRole[i] = d;
        }
        List<String> all = List.of(ABSOLUTE, DEBT, BALANCED);
        step(level, DIVE_START, myRun, all, () -> {
            for (Diver d : divers) {
                if (alive(d.horse)) {
                    stripGoals(d.horse);
                    d.firstHealth = d.horse.getHealth();
                    d.lastHealth = d.firstHealth;
                    d.dunk();
                }
            }
            monitor(level, myRun, divers, byRole, gap[0], 0);
        });
    }

    /** 4x4: stone floor, glass walls gy+1 .. gy+3, water inside, a stone lid over all of it at gy+4. */
    private static void tank(ServerLevel level, int gy, int x, int z) {
        for (int dx = 0; dx <= 3; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                boolean wall = dx == 0 || dx == 3 || dz == 0 || dz == 3;
                level.setBlock(new BlockPos(x + dx, gy, z + dz), Blocks.STONE.defaultBlockState(), 3);
                for (int y = gy + 1; y <= gy + 3; y++) {
                    level.setBlock(new BlockPos(x + dx, y, z + dz),
                            wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 3);
                }
                level.setBlock(new BlockPos(x + dx, gy + 4, z + dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    /**
     * A Gil/Shal horse whose two copies nearly cancel: spawned, read, and discarded until the gap between its deltas
     * is at most {@value #BALANCE_GAP}, up to 30 times. {@code gap[0]} gets the gap kept (NaN if unreadable).
     */
    private static @Nullable Horse balanced(ServerLevel level, int gy, double x, double z, String code, String label,
                                            double[] gap) {
        Horse best = null;
        double bestGap = Double.MAX_VALUE;
        for (int i = 0; i < 30; i++) {
            Horse h = DebugYardClockwork.horse(level, gy, x, z, Sex.MALE, code, label);
            if (h == null) {
                continue;
            }
            double g = Math.abs(breathFactor(h) - 1.0);
            if (Double.isNaN(g)) {
                g = Double.MAX_VALUE;
            }
            if (g < bestGap) {
                if (best != null) {
                    best.discard();
                }
                best = h;
                bestGap = g;
            } else {
                h.discard();
            }
            if (bestGap <= BALANCE_GAP) {
                break;
            }
        }
        gap[0] = bestGap == Double.MAX_VALUE ? Double.NaN : bestGap;
        return best;
    }

    /**
     * This horse's breath factor as its own record predicts it: 1 plus each copy's delta, Gil adding and Shal
     * subtracting - read off the copies directly, not through the ability list, so the prediction does not share
     * the code it checks. NaN if the record is not readable.
     */
    private static double breathFactor(Horse h) {
        Gene gene = Genes.byKeyOrNull(BREATH);
        if (gene == null || !HorseRecords.hasRealRecord(h)) {
            return Double.NaN;
        }
        var record = HorseRecords.of(h);
        AllelePair pair = record.genotype().pair(BREATH);
        if (pair == null) {
            return Double.NaN;
        }
        Epigenome.Copies copies = record.epigenome().copies(gene);
        return 1.0
                + signed(pair.first(), Epigenome.readable(gene, copies.first()).get(AbstractMagicFactorGene.DELTA))
                + signed(pair.second(), Epigenome.readable(gene, copies.second()).get(AbstractMagicFactorGene.DELTA));
    }

    private static double signed(Allele allele, double delta) {
        return switch (allele.token()) {
            case "Gil" -> delta;
            case "Shal" -> -delta;
            default -> 0.0;
        };
    }

    /** One tick of every diver's state machine; the verdicts when all are done or MONITOR_CAP passes. */
    private static void monitor(ServerLevel level, int myRun, List<Diver> divers, Diver[] byRole, double gap,
                                int t) {
        boolean open = false;
        for (Diver d : divers) {
            tick(d, t);
            open |= d.phase != Phase.DONE;
        }
        if (!open || t >= MONITOR_CAP) {
            judgeBreath(byRole, gap, t);
            return;
        }
        step(level, 1, myRun, List.of(ABSOLUTE, DEBT, BALANCED),
                () -> monitor(level, myRun, divers, byRole, gap, t + 1));
    }

    private static void tick(Diver d, int t) {
        if (d.phase == Phase.DONE) {
            return;
        }
        Horse h = d.horse;
        if (!alive(h)) {
            d.problem = "died or was removed at monitor tick " + t + " (last health " + f(d.lastHealth) + ")";
            d.phase = Phase.DONE;
            return;
        }
        float hp = h.getHealth();
        if (hp < d.lastHealth - 1e-4F) {
            d.healthDrops++;
        }
        d.lastHealth = hp;
        int air = h.getAirSupply();
        switch (d.phase) {
            case DIVE -> {
                if (d.diveStart < 0) {
                    if (h.isUnderWater()) {
                        d.diveStart = t;
                        d.air.clear();
                    } else if (t - d.phaseStart > UNDER_GRACE) {
                        d.problem = "eyes never under water within " + UNDER_GRACE + " ticks of dive "
                                + (d.dives.size() + 1) + " (air " + air + ", y " + f(h.getY()) + ")";
                        d.lift();
                        d.phase = Phase.DONE;
                        return;
                    } else {
                        return;
                    }
                }
                int el = t - d.diveStart;
                if (h.isUnderWater()) {
                    d.underTicks++;
                }
                d.minAir = Math.min(d.minAir, air);
                if (el % 20 == 0) {
                    d.air.add(air);
                }
                if (d.hold) {
                    if (el >= HOLD) {
                        d.airLogs.add(compress(d.air));
                        d.lift();
                        d.phase = Phase.DONE;
                    }
                    return;
                }
                if (air <= 0) {
                    d.dives.add(new int[] {d.diveStart, t});
                    d.airLogs.add("start " + (d.air.isEmpty() ? "?" : d.air.get(0)) + ": " + compress(d.air)
                            + ", zero at +" + el);
                    d.lift();
                    if (d.dives.size() < d.divesWanted) {
                        d.phase = Phase.REFILL;
                        d.phaseStart = t;
                    } else {
                        d.phase = Phase.DONE;
                    }
                } else if (el > DIVE_CAP) {
                    d.problem = "still had air " + air + " after " + DIVE_CAP + " ticks of dive " + (d.dives.size() + 1)
                            + " [" + compress(d.air) + "]";
                    d.lift();
                    d.phase = Phase.DONE;
                }
            }
            case REFILL -> {
                if (air >= h.getMaxAirSupply() && t - d.phaseStart >= 20) {
                    d.dunk();
                    d.phase = Phase.DIVE;
                    d.phaseStart = t;
                    d.diveStart = -1;
                } else if (t - d.phaseStart > REFILL_CAP) {
                    d.problem = "air only back to " + air + "/" + h.getMaxAirSupply() + " after " + REFILL_CAP
                            + " ticks out of the water";
                    d.phase = Phase.DONE;
                }
            }
            case DONE -> {
            }
        }
    }

    /** Run-length compress a list of air readings: "300,280,260" or "299*60". */
    private static String compress(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < values.size()) {
            int v = values.get(i);
            int n = 1;
            while (i + n < values.size() && values.get(i + n) == v) {
                n++;
            }
            sb.append(sb.length() == 0 ? "" : ",").append(v);
            if (n > 1) {
                sb.append('*').append(n);
            }
            i += n;
        }
        return sb.toString();
    }

    private static void judgeBreath(Diver[] byRole, double gap, int t) {
        String ended = " (monitor ended at tick " + t + ")";
        // ABSOLUTE
        Diver ocn = byRole[0];
        if (ocn == null) {
            unsure(ABSOLUTE, "the Ocn/Ocn Shal/Shal horse never spawned");
        } else if (ocn.problem != null || ocn.phase != Phase.DONE) {
            unsure(ABSOLUTE, ocn.says() + ended);
        } else if (ocn.underTicks < HOLD * 9 / 10) {
            unsure(ABSOLUTE, "eyes under water for only " + ocn.underTicks + " of " + HOLD + " ticks - the tank is not"
                    + " holding it down; " + ocn.says());
        } else {
            answer(ABSOLUTE, ocn.healthDrops == 0, ocn.says());
        }
        // DEBT
        Diver gil = byRole[1];
        if (gil == null) {
            unsure(DEBT, "the Gil/Gil horse never spawned");
        } else if (gil.problem != null || gil.dives.size() < 2) {
            unsure(DEBT, gil.says() + ended);
        } else {
            int diff = Math.abs(gil.length(0) - gil.length(1));
            answer(DEBT, diff <= 10, "dives differ by " + diff + " ticks; " + gil.says());
        }
        // BALANCED
        Diver gs = byRole[2];
        Diver plain = byRole[3];
        if (gs == null || plain == null) {
            unsure(BALANCED, "a horse never spawned: Gil/Shal " + (gs != null) + ", n/n " + (plain != null));
            return;
        }
        String both = gs.says() + " | " + plain.says() + " | copies' delta gap " + (Double.isNaN(gap) ? "unreadable"
                : String.format(Locale.ROOT, "%.3f", gap)) + " (re-rolled to <= " + BALANCE_GAP + ")";
        if (gs.problem != null || plain.problem != null || gs.dives.isEmpty() || plain.dives.isEmpty()) {
            unsure(BALANCED, both + ended);
            return;
        }
        if (Double.isNaN(gap) || gap > BALANCE_GAP) {
            unsure(BALANCED, "no Gil/Shal roll in 30 had its copies within " + BALANCE_GAP + "; " + both);
            return;
        }
        int p = plain.length(0);
        if (p < 250 || p > 350) {
            unsure(BALANCED, "the plain horse's own dive was " + p + " ticks, not vanilla's ~300 - the tank is not"
                    + " measuring breath; " + both);
            return;
        }
        int diff = gs.length(0) - p;
        answer(BALANCED, Math.abs(diff) <= 40, "Gil/Shal " + gs.length(0) + " vs n/n " + p + " ticks (" + (diff >= 0
                ? "+" : "") + diff + "); " + both);
    }
}
