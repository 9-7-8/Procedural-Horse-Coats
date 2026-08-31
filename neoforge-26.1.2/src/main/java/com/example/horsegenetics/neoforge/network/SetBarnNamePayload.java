package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: set (or, when blank, clear) a horse's free-form barn name
 * from the inventory screen. The server clamps to
 * {@code HorseRecord.MAX_BARN_NAME} and checks the player is near the horse.
 */
public record SetBarnNamePayload(int entityId, String barnName) implements CustomPacketPayload {

    public static final Type<SetBarnNamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "set_barn_name"));

    public static final StreamCodec<ByteBuf, SetBarnNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetBarnNamePayload::entityId,
            ByteBufCodecs.stringUtf8(64), SetBarnNamePayload::barnName,
            SetBarnNamePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
