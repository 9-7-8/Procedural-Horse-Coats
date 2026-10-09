package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: <b>open the chest on this side of this horse.</b> Sent by
 * the pack buttons on the horse screens, which are the only way to a chest
 * while riding - on foot, clicking the chest itself does the same thing with
 * no packet of ours ({@code HorsePackHandler.onClickChest}).
 *
 * <p>The slot travels by name, for {@link TackSlotPayload}'s reason. The
 * server checks everything: that the slot is a storage slot with a chest in
 * it, that the horse is the player's, and that they are beside it.
 */
public record OpenHorsePackPayload(int entityId, String slot) implements CustomPacketPayload {

    public static final Type<OpenHorsePackPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "open_horse_pack"));

    public static final StreamCodec<ByteBuf, OpenHorsePackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenHorsePackPayload::entityId,
            ByteBufCodecs.stringUtf8(32), OpenHorsePackPayload::slot,
            OpenHorsePackPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
