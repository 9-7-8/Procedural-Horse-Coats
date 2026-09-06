package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * &ldquo;View splice recipe&rdquo; from the Horse Browser: fill the Crafting
 * tab's 3x3 grid with whatever of a gene's Known Gene Splice carrot ingredients
 * the player actually has (the rest are shown as ghosts, client-side). It never
 * crafts.
 */
public record ViewSpliceRecipePayload(String geneKey) implements CustomPacketPayload {

    public static final Type<ViewSpliceRecipePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "view_splice_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ViewSpliceRecipePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ViewSpliceRecipePayload::geneKey,
                    ViewSpliceRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
