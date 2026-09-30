package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: open the <b>Dress</b> window on this horse, from the Gear
 * tab of the information screen.
 *
 * <p>It has to be a request. A container menu exists only if the server opens
 * it, and the information screen is put up by the client on its own - so the
 * button asks, the server checks the same three things {@code handleTackSlot}
 * does (a horse, the player's, within eight blocks) and opens
 * {@code HorseGearMenu} or says nothing. There is no refusal to report: the
 * button is only offered when all three already hold on the client.
 */
public record OpenHorseGearPayload(int entityId) implements CustomPacketPayload {

    public static final Type<OpenHorseGearPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "open_horse_gear"));

    public static final StreamCodec<ByteBuf, OpenHorseGearPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenHorseGearPayload::entityId,
            OpenHorseGearPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
