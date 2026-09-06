package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -&gt; client: the player right-clicked a horse with a name tag - open
 * the rename window for it. The current names are read client-side from
 * {@code ClientHorseRecordCache}.
 */
public record OpenHorseRenamePayload(int entityId) implements CustomPacketPayload {

    public static final Type<OpenHorseRenamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "open_horse_rename"));

    public static final StreamCodec<ByteBuf, OpenHorseRenamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenHorseRenamePayload::entityId,
            OpenHorseRenamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
