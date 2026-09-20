package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: the player clicked one of the tack slots on the horse
 * information screen. One click is a swap between that slot and the hand - the
 * server decides which way round, and refuses the lot if the horse is not the
 * player's or is out of reach.
 *
 * <p>The slot travels as the {@code HorseTackSlot} name rather than its
 * ordinal, so a slot inserted into the middle of that enum cannot silently mean
 * a different one to a client of the same build that is half a reload behind.
 */
public record TackSlotPayload(int entityId, String slot) implements CustomPacketPayload {

    public static final Type<TackSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "tack_slot"));

    public static final StreamCodec<ByteBuf, TackSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TackSlotPayload::entityId,
            ByteBufCodecs.stringUtf8(32), TackSlotPayload::slot,
            TackSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
