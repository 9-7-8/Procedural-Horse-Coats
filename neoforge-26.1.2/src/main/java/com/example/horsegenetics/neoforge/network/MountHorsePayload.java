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
 * <p>It was written when a plain right-click opened that screen instead of
 * mounting, as the way back to the saddle. The screen is on sneak-and-use now
 * ({@code HorseInfoInteraction}) and the plain click mounts again, so this is
 * a convenience rather than the only route: you read a wild horse, decide you
 * want it, and get on from where you are. It does exactly what the click does -
 * vanilla's {@code doPlayerRide}, bucking and all.
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
