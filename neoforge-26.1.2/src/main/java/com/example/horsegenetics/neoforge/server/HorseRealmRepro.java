package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>A private, slower clock for a horse that has been in the realm.</b> This is
 * the whole of the realm breeding-rate control, and it is one number per horse.
 *
 * <h2>Why a clock and not a timer</h2>
 * Nothing in {@code common.repro} counts down. {@code Pregnancy} says so in its
 * own header - <i>"every time is an absolute game tick... nothing counts
 * down"</i> - and every state the model has is <i>derived</i> from an anchor and
 * the current game tick: where a mare is in her cycle, whether she is due,
 * whether she has already had her one try this heat, how many covers a stallion
 * has had today. So there is no timer to divide. There is only the reading of
 * "now" that every one of those derivations is handed.
 *
 * <p>That turns out to be the better lever anyway. Instead of scaling a dozen
 * stage lengths and hoping none was missed, each horse keeps an <b>offset</b>:
 * the number of game ticks that have passed which, as far as its reproduction is
 * concerned, did not. {@link #reproTime} is game time minus that offset, and
 * every repro read and write for that horse uses it. One number, one place, and
 * a horse that has never set foot in the realm has an offset of zero and behaves
 * exactly as it did before this class existed.
 *
 * <h2>What the offset buys, in order</h2>
 * <ul>
 *   <li><b>The rate.</b> At {@code realm.breeding_rate_percent} = 25, a quarter
 *       of each elapsed tick is credited and three quarters are deferred, so
 *       heat, the retry window, the stallion's day and gestation all take four
 *       times as long. The treatment's wording exactly: a rate, not a chance.</li>
 *   <li><b>Zero pauses it.</b> Nothing is credited, the horse's clock stands
 *       still, and an existing pregnancy stands still with it - which a
 *       conception-chance reading of the same config could not have done.</li>
 *   <li><b>A dormant realm advances nothing.</b> This is the part an absolute
 *       deadline cannot express on its own. If the realm is empty the horse is
 *       not ticking, so no time is credited <i>and</i> all of it is deferred:
 *       a mare left pregnant in an empty realm is exactly as pregnant when
 *       somebody comes back, however many days later. The test is the size of
 *       the gap since this horse was last seen - a scan that is late by more
 *       than a few of its own periods means the horse was not being simulated,
 *       not that it was being simulated slowly.</li>
 *   <li><b>It survives the trip home.</b> The offset is a property of the horse,
 *       not of where it is standing, so it stops growing when the horse leaves
 *       and is never taken away. A mare who spent a month of game time in a
 *       paused realm walks out with the pregnancy she walked in with and carries
 *       it to term at ordinary speed. Clearing the offset on exit would instead
 *       make her instantly, retroactively overdue.</li>
 * </ul>
 *
 * <h2>Where it lives</h2>
 * The horse's NeoForge persistent data, not {@code Reproduction}. The record is
 * a {@code common/} type shared with the browser tools and a backport target,
 * and a dimension's pacing is not a fact about equine biology. Persistent data
 * saves with the entity, which is all this needs.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseRealmRepro {

    /**
     * Matched to {@code ReproHandler.SCAN} on purpose: the deferral is credited
     * on the same cadence the reproduction pass reads the clock on, so a scan
     * never sees a clock that is a scan out of date.
     */
    private static final int SCAN = 40;

    /**
     * How many of this horse's own scan periods may pass before a gap is read as
     * "it was not being simulated" rather than "it was simulated slowly". Three,
     * because one missed scan is ordinary lag and a dormant realm is measured in
     * minutes or days, never in six seconds.
     */
    private static final int DORMANT_AFTER_SCANS = 3;

    private static final String OFFSET_KEY = "horsegenetics:repro_offset";
    private static final String SEEN_KEY = "horsegenetics:repro_seen";

    /**
     * The tick this horse's reproduction thinks it is. Hand this to anything in
     * {@code common.repro} instead of {@code level.getGameTime()}.
     */
    public static long reproTime(AbstractHorse horse) {
        return horse.level().getGameTime() - offset(horse);
    }

    /** Ticks this horse has lived through that its reproduction did not. */
    public static long offset(AbstractHorse horse) {
        return horse.getPersistentData().getLongOr(OFFSET_KEY, 0L);
    }

    /**
     * True when the realm is paused outright. Gestation and heat stop on their
     * own once the clock does, but a mare who is already in heat and has not yet
     * spent her one try could still be covered on the tick the pause began - so
     * the natural-cover scan asks this directly.
     */
    static boolean reproductionPaused(AbstractHorse horse) {
        return HorseRealm.isInRealm(horse) && ServerConfig.realmBreedingRatePercent() == 0;
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !horse.isAlive()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = horse.getPersistentData();
        long seen = data.getLongOr(SEEN_KEY, now);
        data.putLong(SEEN_KEY, now);

        // Outside the realm the stamp is still kept up to date - it is the
        // baseline the first scan after walking back in measures against - but
        // nothing is ever deferred.
        if (!HorseRealm.isRealm(level)) {
            return;
        }
        long gap = now - seen;
        if (gap <= 0L) {
            return;
        }
        int rate = ServerConfig.realmBreedingRatePercent();
        boolean dormant = gap > (long) SCAN * DORMANT_AFTER_SCANS;
        long credit = dormant ? 0L : Math.round(gap * (rate / 100.0));
        long defer = gap - credit;
        if (defer > 0L) {
            data.putLong(OFFSET_KEY, offset(horse) + defer);
        }
    }

    private HorseRealmRepro() {
    }
}
