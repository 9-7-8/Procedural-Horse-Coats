package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.genes.PackLeaderGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row BB east: PACK AWAY</b> - a pack leader taken away alive, and what its cows do next (2026-10-08).
 *
 * <p>MUSIC stood in the west half from 2026-10-02 until 2026-10-08. Its five bond checks are on
 * wiki/gene-music-enjoyer.html; its last reading, that a finished record goes on paying, is there too and is the
 * owner's to rule on (#217); and its day-rollover check proved #216. PACK LEADER stood in the east half until the
 * same day (#213); its four checks are on wiki/gene-pack-leader.html.
 *
 * <p>The pen starts itself at build, runs on {@link DebugYardHerd#after}, and ends in
 * {@code CLOCKWORK PASS|FAIL|INCONCLUSIVE} lines through {@link DebugYardClockwork#verdict}. A clock left running
 * from an older yard stops itself on {@link #run}; every timed step goes through {@link #step}, which answers every
 * still-open check of its pen INCONCLUSIVE if the step throws; and a deadline after which whatever has not
 * answered says INCONCLUSIVE with what it saw.
 */
final class DebugYardSocial {

    private DebugYardSocial() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        try {
            packAway(level, gy, x0 + 25, z0, myRun);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BB east (PACK AWAY) failed to build", e);
        }
        ActionTrace.log("test yard", "row BB built (east: PACK AWAY) at game tick " + level.getGameTime());
    }

    // ------------------------------------------------------------------
    // shared
    // ------------------------------------------------------------------

    /** Answer a check once; a second answer for the same check is dropped. {@code pass == null} is INCONCLUSIVE. */
    private static void answer(Set<String> answered, String check, @Nullable Boolean pass, String detail) {
        if (!answered.add(check)) {
            return;
        }
        if (pass == null) {
            DebugYardClockwork.inconclusive(check, detail);
        } else {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }

    /**
     * One timed step: {@code ticks} from now, skipped if the yard has been rebuilt since, and if it throws, every
     * check of its pen that has not answered answers INCONCLUSIVE naming the exception - a broken step must not
     * leave a check PENDING all night.
     */
    private static void step(ServerLevel level, long ticks, int myRun, List<String> checks, Set<String> answered,
                             String pen, Runnable body) {
        DebugYardHerd.after(level, Math.max(1L, ticks), () -> {
            if (myRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a " + pen + " step failed", e);
                for (String c : checks) {
                    answer(answered, c, null, "a timed step threw " + e);
                }
            }
        });
    }

    private static @Nullable Horse findHorse(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static @Nullable Mob findMob(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Mob m && m.isAlive() ? m : null;
    }

    /** The centre of a block's floor. */
    private static Vec3 feet(BlockPos at) {
        return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ==================================================================
    // EAST - PACK AWAY
    // ==================================================================

    /*
     * PACK AWAY (wiki/gene-pack-leader.html, Verification tab, NOT played). The open check it answers:
     *
     *   "Nothing is left behind when the leader is taken away rather than killed. A leader that dies releases
     *    its followers at once (verified). One that is put in a stasis chamber, sold, or sent to another
     *    dimension is released by the same code, and no pen has read it. Pass: no mob keeps walking toward
     *    where the horse was."
     *
     * WHAT THE CODE DOES, READ 2026-10-08. GeneAbilityHandler.followHorse gives each cow in radius 16 a path to
     * the leader's block on the leader's 40-tick beat (at most MAX_TARGETS, and none to a cow within FOLLOW_STOP,
     * 3). GeneAbilityHandler.onEntityLeave calls releaseFollowers for a horse whose removal reason is DISCARDED
     * or CHANGED_DIMENSION, which stops the navigation of every mob in the radius whose path ends within 3 of
     * where the horse stood. A stasis chamber takes a horse with Entity.discard (HorseStasisHandler), so DISCARDED
     * is that case; CHANGED_DIMENSION is what Entity.teleport to another level sets on the old entity.
     *
     * THE PEN: the plot PACK LEADER stood in, ten persistent cows at its east end. Twice over, with a fresh
     * Cow/Cow leader each time:
     *   beats 0-5  the leader stands at the west end and steps a block a beat, so the cows come to it;
     *   beat 6     one tick before the beat it is put 13 blocks east, so every cow is outside FOLLOW_STOP and
     *              the beat hands out up to six fresh paths to a block it has not stood on before;
     *   then       the cows walking to that block are counted, and the leader is taken away - the first by
     *              Entity.discard, the second by Entity.teleport into the overworld (the arrival is discarded).
     * A cow counts as walking to the leader when its navigation is in progress and its target is exactly the
     * leader's block (PathNavigation.moveTo(Entity) records target.blockPosition()).
     *
     * WHY IT STILL STANDS (2026-10-08). Both checks passed on the launch of 14:47 and are recorded on the page.
     * But that launch's second walk read SEVEN cows walking to the leader's block on the jump beat, and the aura
     * touches at most six a beat. The seventh (cow 4, 4.5 blocks off) had been walking to that same block for
     * the first leader. Either its own stroll ended on that one block, about one chance in a hundred, or a cow
     * the release stopped took the walk up again later than this pen looked. The node counts, the per-tick
     * count over ten beats and the "before the jump" count below were added to tell those apart and have not
     * run yet. That is the open question on the page now, and this pen goes when it is answered.
     *
     * PASS / FAIL, per leader: PASS - at least one cow was walking to the leader at the removal, and none is one
     * tick later, one beat later or two beats later. FAIL - any still is, with how far it had to go.
     * RECORDED, NOT JUDGED: the ticks of the ten beats after the removal on which any cow was walking to that
     * block, and the beats BEFORE the jump on which one already was (in the second walk the jump block is where
     * the first leader was taken from). A cow's own stroll ends on one given block about once in four hundred,
     * so over four hundred ticks and ten cows a single hit is chance; several, or the same cows, is not.
     * INCONCLUSIVE - no cow was walking to it at the removal (nothing to release), fewer than seven cows alive,
     * the leader missing, or the teleport returning nothing. About 1,800 ticks from build; deadline 4,000.
     */

    private static final String PACK = "PACK AWAY";
    private static final String P_DISCARD = PACK + " - a Cow/Cow leader that is discarded (a stasis chamber takes"
            + " a horse this way) leaves no cow walking to where it stood";
    private static final String P_DIMENSION = PACK + " - a Cow/Cow leader sent to another dimension leaves no cow"
            + " walking to where it stood";
    private static final List<String> P_CHECKS = List.of(P_DISCARD, P_DIMENSION);

    private static final long BEAT = PackLeaderGene.INTERVAL_TICKS;
    private static final int COWS = 10;
    private static final long P_DEADLINE = 4_000L;
    /** The beat the leader is put far from its cows on, and removed after. */
    private static final int JUMP_BEAT = 6;

    /** Leader steps, relative to the pen's (x0, z0). Never the same block twice in one walk. */
    private static final int[][] STEPS = {{3, 4}, {3, 5}, {3, 6}, {3, 7}, {3, 8}, {2, 8}, {16, 6}};

    /** The cows' home blocks, relative to the pen's (x0, z0). */
    private static final int[][] HOMES = {{14, 4}, {14, 6}, {14, 8}, {15, 5}, {15, 7}, {16, 4}, {16, 8}, {17, 4},
            {17, 6}, {17, 8}};

    private static final class Pack {
        final int run;
        final Set<String> answered = new HashSet<>();
        final List<UUID> cows = new ArrayList<>();
        int gy;
        int x0;
        int z0;
        UUID leader;
        final StringBuilder beats = new StringBuilder();
        /** Beats before the jump on which a cow was already walking to the jump block. */
        int early;

        Pack(int run) {
            this.run = run;
        }
    }

    private static void packAway(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : P_CHECKS) {
            DebugYardClockwork.expect(c);
        }
        Pack p = new Pack(myRun);
        p.gy = gy;
        p.x0 = x0;
        p.z0 = z0;
        int x1 = x0 + 19;
        int z1 = z0 + 12;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        YardPens.register(gy, x0, x1, z0, z1, PACK);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("PACK AWAY", "10 cows, a Cow/Cow", "leader discarded,", "then one sent away"));
        for (int i = 0; i < HOMES.length; i++) {
            Entity e = DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + HOMES[i][0] + 0.5,
                    z0 + HOMES[i][1] + 0.5);
            if (e instanceof Mob cow) {
                cow.setCustomName(Component.literal(PACK + " cow " + (i + 1)));
                p.cows.add(cow.getUUID());
            }
        }
        step(level, 60, myRun, P_CHECKS, p.answered, PACK, () -> startLeader(level, p, P_DISCARD));
        step(level, P_DEADLINE, myRun, P_CHECKS, p.answered, PACK, () -> {
            for (String c : P_CHECKS) {
                answer(p.answered, c, null, "deadline: beats [" + p.beats + "]");
            }
        });
    }

    private static List<Mob> myCows(ServerLevel level, Pack p) {
        List<Mob> out = new ArrayList<>();
        for (UUID id : p.cows) {
            Mob m = findMob(level, id);
            if (m != null) {
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Is this cow walking a path to {@code target} right now? PathNavigation.getTargetPos is the block a path was
     * last created for and isInProgress is "a path that is not done"; GeneAbilityHandler.releaseFollowers reads
     * the same two.
     */
    private static boolean pathingTo(Mob m, BlockPos target) {
        return m.getNavigation().isInProgress() && target.equals(m.getNavigation().getTargetPos());
    }

    private static String walkingTo(ServerLevel level, Pack p, BlockPos at) {
        int n = 0;
        StringBuilder who = new StringBuilder();
        for (Mob m : myCows(level, p)) {
            if (pathingTo(m, at)) {
                n++;
                // "node 0 of 9" is a path made this tick; a higher node is one the cow has been walking.
                net.minecraft.world.level.pathfinder.Path path = m.getNavigation().getPath();
                who.append(who.length() == 0 ? "" : ", ").append(m.getName().getString()).append(' ')
                        .append(f1(Math.sqrt(m.distanceToSqr(feet(at))))).append(" off")
                        .append(path == null ? "" : ", node " + path.getNextNodeIndex() + " of "
                                + path.getNodeCount());
            }
        }
        return n + (n == 0 ? "" : " (" + who + ")");
    }

    private static BlockPos jumpBlock(Pack p) {
        return new BlockPos(p.x0 + STEPS[JUMP_BEAT][0], p.gy + 1, p.z0 + STEPS[JUMP_BEAT][1]);
    }

    private static void stepTo(Pack p, Horse h, int index) {
        int[] s = STEPS[index];
        h.teleportTo(p.x0 + s[0] + 0.5, p.gy + 1, p.z0 + s[1] + 0.5);
        h.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
    }

    /** Cows home, then a fresh leader at the west end, sampled on its own beat. */
    private static void startLeader(ServerLevel level, Pack p, String check) {
        for (int i = 0; i < p.cows.size() && i < HOMES.length; i++) {
            Mob m = findMob(level, p.cows.get(i));
            if (m != null) {
                m.getNavigation().stop();
                m.teleportTo(p.x0 + HOMES[i][0] + 0.5, p.gy + 1, p.z0 + HOMES[i][1] + 0.5);
            }
        }
        p.beats.setLength(0);
        p.early = 0;
        step(level, 60, p.run, P_CHECKS, p.answered, PACK, () -> {
            Horse lead = DebugYardUnattended.horse(level, p.gy, p.x0 + STEPS[0][0] + 0.5, p.z0 + STEPS[0][1] + 0.5,
                    Sex.FEMALE, PackLeaderGene.KEY + "=Cow/Cow", true, PACK + " LEADER Cow/Cow");
            if (lead == null) {
                answer(p.answered, check, null, "the Cow/Cow leader could not be spawned");
                next(level, p, check);
                return;
            }
            p.leader = lead.getUUID();
            long phase = lead.getUUID().getLeastSignificantBits() & 0x7FFFFFFFL;
            // The first beat at least 40 ticks out, so the horse has joined and its abilities have resolved.
            long from = level.getGameTime() + 40;
            long beat0 = from + Math.floorMod(-(from + phase), BEAT);
            step(level, beat0 - level.getGameTime(), p.run, P_CHECKS, p.answered, PACK,
                    () -> leaderBeat(level, p, check, 0));
        });
    }

    private static void leaderBeat(ServerLevel level, Pack p, String check, int k) {
        Horse lead = findHorse(level, p.leader);
        if (lead == null) {
            answer(p.answered, check, null, "the leader is gone at beat " + k + ", before it was taken away; beats ["
                    + p.beats + "]");
            next(level, p, check);
            return;
        }
        List<Mob> mine = myCows(level, p);
        if (mine.size() < 7) {
            answer(p.answered, check, null, "only " + mine.size() + " of " + COWS + " cows alive at beat " + k);
            lead.discard();
            next(level, p, check);
            return;
        }
        BlockPos at = lead.blockPosition();
        String walking = walkingTo(level, p, at);
        p.beats.append(p.beats.length() == 0 ? "" : "; ").append('b').append(k).append(" @").append(at.getX() - p.x0)
                .append(',').append(at.getZ() - p.z0).append(' ').append(walking);
        if (k < JUMP_BEAT) {
            // Nothing has stood on the jump block yet in this walk. A cow walking to it now is walking to where
            // the LAST leader was taken from - the first launch read seven cows on a beat capped at six.
            String early = walkingTo(level, p, jumpBlock(p));
            if (!early.equals("0")) {
                p.beats.append(" [to the jump block before the jump: ").append(early).append(']');
                p.early++;
            }
            int nextBeat = k + 1;
            step(level, BEAT - 1, p.run, P_CHECKS, p.answered, PACK, () -> {
                Horse again = findHorse(level, p.leader);
                if (again != null) {
                    stepTo(p, again, nextBeat);
                }
                step(level, 1, p.run, P_CHECKS, p.answered, PACK, () -> leaderBeat(level, p, check, nextBeat));
            });
            return;
        }
        // Taken away right after the beat's sample: every re-pathed cow is mid-walk to this block.
        boolean dimension = check.equals(P_DIMENSION);
        String how;
        if (dimension) {
            ServerLevel over = level.getServer().overworld();
            // Entity.teleport as TicketHandler.arrive calls it. The old entity leaves this level with
            // RemovalReason.CHANGED_DIMENSION; the copy that arrives is scenery nobody wants in the overworld.
            Entity moved = lead.teleport(new TeleportTransition(over, new Vec3(0.5, 300.0, 0.5), Vec3.ZERO,
                    lead.getYRot(), lead.getXRot(), Set.of(), TeleportTransition.DO_NOTHING));
            if (moved == null) {
                answer(p.answered, check, null, "Entity.teleport to the overworld returned nothing; beats ["
                        + p.beats + "]");
                lead.discard();
                next(level, p, check);
                return;
            }
            how = "sent to the overworld (the old entity's removal reason " + lead.getRemovalReason() + ")";
            moved.discard();
        } else {
            lead.discard();
            how = "discarded (removal reason " + lead.getRemovalReason() + ")";
        }
        String atRemoval = walking;
        String[] later = new String[3];
        // Every tick for ten beats as well: a cow that takes the walk up again between two readings is the
        // thing this pen exists to catch, and three readings can step over it.
        int[] resumed = new int[2];     // ticks on which any cow walked there, and the most at once
        for (int t = 1; t <= 10 * BEAT; t++) {
            step(level, t, p.run, P_CHECKS, p.answered, PACK, () -> {
                int n = 0;
                for (Mob m : myCows(level, p)) {
                    if (pathingTo(m, at)) {
                        n++;
                    }
                }
                if (n > 0) {
                    resumed[0]++;
                    resumed[1] = Math.max(resumed[1], n);
                }
            });
        }
        step(level, 1, p.run, P_CHECKS, p.answered, PACK, () -> later[0] = walkingTo(level, p, at));
        step(level, BEAT, p.run, P_CHECKS, p.answered, PACK, () -> later[1] = walkingTo(level, p, at));
        step(level, 2 * BEAT, p.run, P_CHECKS, p.answered, PACK, () -> later[2] = walkingTo(level, p, at));
        step(level, 10 * BEAT + 1, p.run, P_CHECKS, p.answered, PACK, () -> {
            String detail = "leader " + how + " on block " + (at.getX() - p.x0) + "," + (at.getZ() - p.z0) + " with "
                    + atRemoval + " cow(s) walking to it; still walking there one tick later " + later[0]
                    + ", one beat later " + later[1] + ", two beats later " + later[2] + "; over the ten beats after"
                    + " it a cow was walking there on " + resumed[0] + " tick(s) of " + 10 * BEAT + " (most at once "
                    + resumed[1] + "); beats before the jump with a cow already walking to the jump block "
                    + p.early + " | beats [" + p.beats + "]";
            if (findHorse(level, p.leader) != null) {
                answer(p.answered, check, null, "the leader is still in the yard - nothing was released | " + detail);
            } else if (atRemoval.startsWith("0")) {
                answer(p.answered, check, null, "no cow was walking to the leader when it was taken, so there was"
                        + " nothing to release | " + detail);
            } else {
                boolean none = "0".equals(later[0]) && "0".equals(later[1]) && "0".equals(later[2]);
                answer(p.answered, check, none, detail);
            }
            next(level, p, check);
        });
    }

    /** The discarded leader is followed by the one sent to another dimension, with the same cows. */
    private static void next(ServerLevel level, Pack p, String done) {
        if (done.equals(P_DISCARD)) {
            step(level, 40, p.run, P_CHECKS, p.answered, PACK, () -> startLeader(level, p, P_DIMENSION));
        }
    }
}
