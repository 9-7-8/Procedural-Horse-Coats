package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The browser key asking the server to open the {@code HorseBrowserMenu}. Carries
 * nothing - the menu is a plain reference/crafting window, gated by nothing.
 */
public record OpenHorseBrowserPayload() implements CustomPacketPayload {

    public static final Type<OpenHorseBrowserPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "open_horse_browser"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHorseBrowserPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenHorseBrowserPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
