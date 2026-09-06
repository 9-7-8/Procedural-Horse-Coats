package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The Horse Browser telling the server which gene is selected in the list, so
 * the Crafting tab's "book &rarr; gene paper" craft knows which paper to make.
 * An empty string clears the selection.
 */
public record SelectBrowserGenePayload(String geneKey) implements CustomPacketPayload {

    public static final Type<SelectBrowserGenePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "select_browser_gene"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectBrowserGenePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectBrowserGenePayload::geneKey,
                    SelectBrowserGenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
