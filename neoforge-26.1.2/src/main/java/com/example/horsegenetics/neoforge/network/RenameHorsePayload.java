package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: apply a new registered first / last name to a horse, from
 * the rename window. The server re-checks the player is near the horse and still
 * holding a name tag, that the two parts are not both blank, and then consumes
 * one name tag.
 */
public record RenameHorsePayload(int entityId, String firstName, String lastName) implements CustomPacketPayload {

    public static final Type<RenameHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "rename_horse"));

    public static final StreamCodec<ByteBuf, RenameHorsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RenameHorsePayload::entityId,
            ByteBufCodecs.stringUtf8(48), RenameHorsePayload::firstName,
            ByteBufCodecs.stringUtf8(48), RenameHorsePayload::lastName,
            RenameHorsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
