package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * <b>A whistled horse that is near enough walks to you instead of appearing.</b>
 *
 * <p>Every area whistle used to teleport, always, at any distance inside its
 * radius - so blowing a basic whistle at a horse ten blocks away in an open
 * field made it vanish and reappear at your elbow, which reads as a bug even
 * though it is the feature working. A horse close enough to simply come now
 * comes; everything further away teleports exactly as before.
 *
 * <p>The <b>ender whistle is deliberately untouched</b>: its whole point is
 * "from anywhere", and a walk cannot cross a dimension.
 *
 * <h2>Decided once, at blow time</h2>
 *
 * <p>{@link #tryWalk} asks the horse's own navigator for a <b>fresh</b> path to
 * the player and walks only if that path exists, actually arrives, and is no
 * longer than {@link #WALK_PATH_BLOCKS} steps. Nothing about the decision is
 * re-made later: a horse either set off walking or it teleported, and there is
 * no third state to reason about.
 *
 * <p><b>The route is measured, not the distance.</b> A horse fifteen blocks away
 * on the far side of a fence can be eighty blocks of walking, and a whistle that
 * sent it on that journey would be worse than the teleport it replaced. Because
 * a route is never shorter than the straight line between its ends, capping the
 * route alone also caps the distance.
 *
 * <p><b>Not {@code BondFollowGoal}'s {@code ReachCheck}.</b> That one caches its
 * answer for two seconds, which is right for a goal that asks every tick and
 * wrong for a whistle: a call is a single event and deserves a fresh answer. It
 * also only answers yes/no, and this needs the length.
 *
 * <h2>The state machine lives in the sweep, not the goal</h2>
 *
 * <p>{@link WhistleCallGoal} is deliberately dumb - it walks toward whoever the
 * marker names and does nothing else. Every way a call can end is decided in
 * {@link #onServerTick} instead, for one reason: <b>a goal that never starts
 * cannot end itself.</b> If something at priority 0 holds MOVE, or the horse is
 * in danger, the goal simply never runs, and a state machine living inside it
 * would leave the horse neither walking nor teleported - the "the whistle
 * ignored my horse" report, with nothing in the log.
 *
 * <p>So a call ends in exactly one of four ways, all of them here:
 * <ul>
 *   <li><b>Arrived</b> - within {@link #ARRIVED_SQR}. Done.</li>
 *   <li><b>Gave up</b> - the deadline passed, or it made no headway for
 *       {@link #STUCK_TICKS}. It is <b>teleported</b>, exactly as it would have
 *       been before this existed. That fallback is the guarantee that a walked
 *       call is never worse than the old behaviour, and it is also what covers
 *       the goal never having started.</li>
 *   <li><b>In danger</b> - on fire, freezing or hurt. The call is dropped and it
 *       is <b>not</b> teleported: the horse is in trouble, not lost, and yanking
 *       a burning animal to your feet is not help.</li>
 *   <li><b>Invalidated</b> - the player left, died or changed level; the horse
 *       died, was mounted or was leashed. Dropped quietly.</li>
 * </ul>
 *
 * <p><b>In memory only</b>, keyed by horse UUID, and never saved. A call lasts a
 * few seconds; a half-finished one restored from disk would be a bug, and a
 * reload mid-walk simply ends it with the horse standing where it got to.
 */
@EventBusSubscriber
public final class WhistleCalls {

    private WhistleCalls() {
    }

    /**
     * <b>How long a walking route may be, in steps.</b> Settled with the owner at
     * 16 - the basic whistle's whole reach, so that tier never teleports a horse
     * it could have walked, and the golden and echo whistles teleport only the
     * far ones.
     *
     * <p>Steps, not blocks: a path node is one block step, and a diagonal step
     * covers about 1.4 blocks. So this is an approximation of a distance, which
     * is why the tooltip says "about".
     */
    public static final int WALK_PATH_BLOCKS = 16;

    /** Ten seconds. Long enough for sixteen blocks at a trot, with room to spare. */
    private static final int DEADLINE_TICKS = 200;

    /** Two seconds of getting no closer, and the horse is stuck rather than slow. */
    private static final int STUCK_TICKS = 40;

    /** Close enough. The same three blocks the teleport path calls "already here". */
    private static final double ARRIVED_SQR = 9.0;

    /** Below this much closer, a horse has not really made headway. */
    private static final double PROGRESS_SQR = 0.5;

    /** One live call. Mutable, because the sweep tracks headway on it. */
    static final class Call {
        final AbstractHorse horse;
        final ServerPlayer player;
        final long deadline;
        long lastProgressTick;
        double bestDistSq;

        Call(AbstractHorse horse, ServerPlayer player, long now) {
            this.horse = horse;
            this.player = player;
            this.deadline = now + DEADLINE_TICKS;
            this.lastProgressTick = now;
            this.bestDistSq = horse.distanceToSqr(player);
        }
    }

    private static final Map<UUID, Call> ACTIVE = new HashMap<>();

    // ------------------------------------------------------------------
    // The decision, once, at blow time
    // ------------------------------------------------------------------

    /**
     * Start {@code horse} walking to {@code player} if it can get there on its
     * own feet within the cap.
     *
     * @return whether it is walking - false means the caller should teleport it
     *         exactly as it always did
     */
    public static boolean tryWalk(ServerPlayer player, AbstractHorse horse) {
        if (horse.isLeashed() || horse.isVehicle() || inDanger(horse)) {
            return false;
        }
        Path path = horse.getNavigation().createPath(player, 0);
        // canReach(): a path that exists but stops short is a fence, and vanilla
        // will happily hand one back. BondFollowGoal treats it the same way.
        if (path == null || !path.canReach()) {
            return false;
        }
        // The first node is where the horse already stands, so the steps it has
        // to take is one fewer than the node count.
        if (path.getNodeCount() - 1 > WALK_PATH_BLOCKS) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        long now = server == null ? 0L : server.getTickCount();
        HorseLeads.untieFor(horse, player);
        ACTIVE.put(horse.getUUID(), new Call(horse, player, now));
        ActionTrace.log("whistle", "walking " + horse.getUUID() + " " + (path.getNodeCount() - 1) + " steps");
        return true;
    }

    /** The player this horse was called by, or null if it is not under a call. */
    static @Nullable ServerPlayer calledBy(AbstractHorse horse) {
        Call call = ACTIVE.get(horse.getUUID());
        return call == null ? null : call.player;
    }

    /**
     * <b>Coarser than {@code PanicGoal}'s own trigger, on purpose.</b> That one
     * asks whether the last damage source was of a panic-causing type; this errs
     * toward dropping the call, because the cost of dropping one wrongly is a
     * player blowing the whistle again, and the cost of holding one wrongly is a
     * burning horse walking calmly toward them.
     */
    static boolean inDanger(AbstractHorse horse) {
        return horse.isOnFire() || horse.isFreezing() || horse.hurtTime > 0;
    }

    // ------------------------------------------------------------------
    // Every way a call can end
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = event.getServer().getTickCount();
        for (Iterator<Map.Entry<UUID, Call>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Call call = it.next().getValue();
            AbstractHorse horse = call.horse;
            ServerPlayer player = call.player;

            if (!horse.isAlive() || horse.isRemoved() || horse.isVehicle() || horse.isLeashed()
                    || !player.isAlive() || player.isRemoved() || player.level() != horse.level()) {
                it.remove();
                horse.getNavigation().stop();
                continue;
            }
            if (inDanger(horse)) {
                it.remove();
                horse.getNavigation().stop();
                ActionTrace.log("whistle", "call dropped, " + horse.getUUID() + " is in trouble");
                continue;
            }

            double distSq = horse.distanceToSqr(player);
            if (distSq < ARRIVED_SQR) {
                it.remove();
                horse.getNavigation().stop();
                continue;
            }
            if (distSq < call.bestDistSq - PROGRESS_SQR) {
                call.bestDistSq = distSq;
                call.lastProgressTick = now;
            }
            if (now > call.deadline || now - call.lastProgressTick > STUCK_TICKS) {
                it.remove();
                ActionTrace.log("whistle", "call gave up on " + horse.getUUID() + ", teleporting");
                teleportTo(player, horse, Math.floorMod(horse.getUUID().hashCode(), 9));
            }
        }
    }

    // ------------------------------------------------------------------
    // The teleport, shared with the whistle itself
    // ------------------------------------------------------------------

    /**
     * Put {@code horse} beside {@code player}, on the spread grid so a herd does
     * not stack. Lives here rather than on the item because a call that gives up
     * mid-walk has to land a horse exactly the way the item would have.
     */
    public static void teleportTo(ServerPlayer player, AbstractHorse horse, int slot) {
        ServerLevel level = player.level();
        horse.getNavigation().stop();
        BlockPos spot = spreadSpot(level, player, slot);
        horse.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), 0.0F);
        horse.setDeltaMovement(Vec3.ZERO);
        horse.fallDistance = 0.0;
    }

    /**
     * A landing spot near {@code player}: rings of a 3x3 grid fanning out, then
     * nudged up/down a few blocks to sit on solid ground rather than in it or
     * floating. Best effort - a whistle recall doesn't need to be perfect.
     */
    public static BlockPos spreadSpot(ServerLevel level, ServerPlayer player, int n) {
        int gx = (n % 3) - 1;
        int gz = ((n / 3) % 3) - 1;
        int ring = 2 + (n / 9) * 2;
        BlockPos p = player.blockPosition().offset(gx * ring, 0, gz * ring);
        for (int i = 0; i < 3 && level.getBlockState(p.below()).isAir(); i++) {
            p = p.below();
        }
        for (int i = 0; i < 4 && !level.getBlockState(p).isAir(); i++) {
            p = p.above();
        }
        return p;
    }
}
