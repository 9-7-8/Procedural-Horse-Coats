package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Carries no data - the rider asking their mount to go down, or to stop going
 * down. A toggle rather than a held key, which is why it is a packet at all.
 *
 * <p>It replaced {@code rider.isSprinting()}, which could never work: sprinting
 * is a state the client decides to <i>enter</i>, and {@code LocalPlayer} will
 * not enter it while the player is a passenger, so the server flag stayed false
 * however long the key was held (owner, 2026-09-13: "still no way to dive on
 * oceanborn"). Reading the raw sprint key off the input packet fixed that and
 * was then dropped in favour of this on the owner's call - "change it to f to
 * ask a horse to dive / stop floating" - because a horse that holds its depth
 * is much easier to ride than one you have to keep a finger on.
 */
public record ToggleDivePayload() implements CustomPacketPayload {

    public static final Type<ToggleDivePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "toggle_dive"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleDivePayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleDivePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
