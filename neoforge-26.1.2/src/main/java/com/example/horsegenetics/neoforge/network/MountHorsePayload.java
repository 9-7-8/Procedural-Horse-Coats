package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: get on this horse, from the <b>Ride</b> button on the horse
 * information screen.
 *
 * <p>It exists because of the config that makes a plain right-click open that
 * screen ({@code ClientConfig.rightClickOpensInfo}). With that on, the click
 * that used to put you in the saddle opens a window instead - and mounting is
 * also how a wild horse is tamed, so without a way back to it that setting
 * would quietly cost the player both riding and taming. The button is that way
 * back, and it does exactly what the right-click did: vanilla's
 * {@code doPlayerRide}, bucking and all.
 */
public record MountHorsePayload(int entityId) implements CustomPacketPayload {

    public static final Type<MountHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "mount_horse"));

    public static final StreamCodec<ByteBuf, MountHorsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MountHorsePayload::entityId,
            MountHorsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
