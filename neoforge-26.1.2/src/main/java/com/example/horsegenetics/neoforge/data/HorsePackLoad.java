package com.example.horsegenetics.neoforge.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * <b>How much a horse is carrying, as the client is allowed to know it.</b>
 * Two counts and nothing else: the items in the near-side chest and the items
 * in the off-side one.
 *
 * <p>{@link HorsePacks} holds the contents themselves and is never synced. The
 * client still needs two facts about them - whether a chest is empty, because
 * a chest with things in it does not come off and the Dress window should not
 * pretend it will, and how many items there are, because the Gear tab says
 * what the load is costing. Both are these two numbers.
 *
 * <p><b>Synced and not saved.</b> It is derived: the server recounts when the
 * horse joins a level and whenever a chest changes
 * ({@code HorsePackHandler.refresh}), so there is no stored copy to disagree
 * with the chests.
 *
 * @param weightless whether the near/off chest is one whose contents are not
 *                   on the horse at all, packed as two bits - the client works
 *                   the load out from the same rule the server does
 */
public record HorsePackLoad(int left, int right, int weightless) {

    public static final HorsePackLoad NONE = new HorsePackLoad(0, 0, 0);

    private static final int LEFT_WEIGHTLESS = 1;
    private static final int RIGHT_WEIGHTLESS = 2;

    public static final StreamCodec<ByteBuf, HorsePackLoad> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HorsePackLoad::left,
            ByteBufCodecs.VAR_INT, HorsePackLoad::right,
            ByteBufCodecs.VAR_INT, HorsePackLoad::weightless,
            HorsePackLoad::new);

    public static HorsePackLoad of(int left, boolean leftWeightless, int right, boolean rightWeightless) {
        return new HorsePackLoad(left, right,
                (leftWeightless ? LEFT_WEIGHTLESS : 0) | (rightWeightless ? RIGHT_WEIGHTLESS : 0));
    }

    /** Items in the chest on that side, weightless or not. */
    public int in(boolean leftSide) {
        return leftSide ? left : right;
    }

    /** The items the horse actually bears: what {@code PackLoad} is asked about. */
    public long weighed() {
        long total = 0;
        if ((weightless & LEFT_WEIGHTLESS) == 0) {
            total += left;
        }
        if ((weightless & RIGHT_WEIGHTLESS) == 0) {
            total += right;
        }
        return total;
    }
}
