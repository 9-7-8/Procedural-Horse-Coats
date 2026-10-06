package com.example.horsegenetics.common.care;

/**
 * <b>Dormancy: a horse nobody is near ticks one tick in {@link #STRIDE}</b> (owner, 2026-10-06, lag audit #204).
 *
 * <p>With no player within {@code performance.dormancy_radius} blocks, a horse's whole tick - vanilla movement
 * and AI, and every one of this mod's tick handlers - runs only on the ticks where
 * {@code (tickCount + entityId) % STRIDE == 0}. The owner's call was <i>throttle, and catch up</i>: nothing a
 * distant horse does is lost, it is only coarser. That holds because of where the run ticks fall:
 * <ul>
 *   <li><b>Every scan still runs on time.</b> The mod's per-horse scans are gated on
 *       {@code (tickCount + entityId) % INTERVAL == 0}, every INTERVAL is a multiple of {@code STRIDE}, and
 *       {@code tickCount} still advances on a skipped tick - so every scan tick is a run tick. Hunger, healing,
 *       bond, heat, pregnancy, wild turnover and the herd life keep their rates with no change to their code.
 *       A new scan interval that is not a multiple of {@code STRIDE} would silently run less often; see
 *       {@link #alignedInterval}.</li>
 *   <li><b>Game-time beats are widened</b> to the span a run covers ({@link #beatWithin}), so a gene effect on a
 *       world-clock beat fires once per beat, not only when a run tick happens to land on it.</li>
 *   <li><b>Foal age</b> is vanilla's per-tick count; the game module steps it on the skipped ticks.</li>
 * </ul>
 * What does slow is what nobody is there to see: walking, grazing, the goals.
 */
public final class Dormancy {

    private Dormancy() {
    }

    /** One run in this many ticks while dormant. Divides every scan interval in the mod (20, 30, 40, 100, 200). */
    public static final int STRIDE = 10;

    /** The default radius, in blocks: eight chunks, near the edge of where a horse can be seen at all. */
    public static final int DEFAULT_RADIUS = 128;

    /** The largest radius a server may set; 0 turns dormancy off. */
    public static final int MAX_RADIUS = 1024;

    /** Is this one of the ticks a dormant horse runs? Also the ticks on which dormancy is decided. */
    public static boolean runTick(int tickCount, int entityId) {
        return Math.floorMod(tickCount + entityId, STRIDE) == 0;
    }

    /** Would a scan on this interval still run at its full rate under dormancy? */
    public static boolean alignedInterval(int interval) {
        return interval > 0 && interval % STRIDE == 0;
    }

    /**
     * Does a world-clock beat fall inside the ticks this run stands for? {@code span} is 1 for an awake horse -
     * exactly the old {@code (gameTime + phase) % interval == 0} - and {@link #STRIDE} for a dormant one, whose
     * run covers itself and the ticks skipped before it.
     */
    public static boolean beatWithin(long gameTime, long phase, int interval, int span) {
        if (interval <= 1) {
            return true;
        }
        return Math.floorMod(gameTime + phase, (long) interval) < Math.min(Math.max(1, span), interval);
    }
}
