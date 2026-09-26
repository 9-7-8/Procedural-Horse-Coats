package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: the player picked something for one of the tack slots on
 * the horse information screen. The server refuses the lot if the horse is not
 * the player's or is out of reach.
 *
 * <p>The slot travels as the {@code HorseTackSlot} name rather than its
 * ordinal, so a slot inserted into the middle of that enum cannot silently mean
 * a different one to a client of the same build that is half a reload behind.
 *
 * <p>{@code inventorySlot} is an index into the player's own inventory, or
 * {@link #TAKE_OFF} to empty the slot. It used to be the <b>hand</b>, implicitly
 * - one click swapped the slot with whatever you were holding - which meant the
 * screen could only ever equip one item and gave no way to find out which of
 * the things in your pack would have fitted. The picker names the item, so the
 * packet has to as well. The index is re-checked server-side against the same
 * {@code accepts} the picker filtered on, because an index is a claim about
 * another container and the client does not get to be believed about that.
 */
public record TackSlotPayload(int entityId, String slot, int inventorySlot) implements CustomPacketPayload {

    /** Empty the slot and give back what was in it. */
    public static final int TAKE_OFF = -1;

    public static final Type<TackSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "tack_slot"));

    public static final StreamCodec<ByteBuf, TackSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TackSlotPayload::entityId,
            ByteBufCodecs.stringUtf8(32), TackSlotPayload::slot,
            ByteBufCodecs.VAR_INT, TackSlotPayload::inventorySlot,
            TackSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
