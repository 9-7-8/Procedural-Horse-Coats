package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HurtNotice;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>The two questions both chat notices ask of a horse</b>: what to call it,
 * and what hurt it. Shared so that the line about a burning horse and the line
 * about the horse that burned to death cannot drift apart.
 *
 * @see HorseHurtNoticeHandler
 * @see HorseDeathNoticeHandler
 */
final class HorseNotices {

    private HorseNotices() {
    }

    /**
     * What to call it: the name on the tag, then the name this mod gave it, then
     * whatever vanilla calls a donkey.
     */
    static String name(AbstractHorse horse) {
        if (horse.getCustomName() != null) {
            return horse.getCustomName().getString();
        }
        if (horse instanceof Horse h && HorseRecords.hasRealRecord(h)) {
            return HorseRecords.of(h).displayName();
        }
        return horse.getName().getString();
    }

    /**
     * Vanilla's damage type, refined by the one thing it cannot say: whether the
     * fire burning this horse is the sun.
     *
     * <p>{@code SunSensitivityHandler} burns a sun-sensitive horse with vanilla's
     * on-fire damage, so the damage type alone cannot tell a dhampir caught in
     * the open from a horse standing in a campfire. A sun-burnt horse is not
     * actually alight, which is the discriminator used here.
     */
    static HurtNotice.Cause causeOf(AbstractHorse horse, String msgId) {
        HurtNotice.Cause cause = HurtNotice.of(msgId);
        if (cause == HurtNotice.Cause.FIRE && !horse.isOnFire()
                && horse instanceof Horse h && SunSensitivityHandler.isSensitive(h)) {
            return HurtNotice.Cause.SUNLIGHT;
        }
        return cause;
    }
}
