package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * "Write research paper" from the Horse Browser's gene-database tab (roadmap
 * &sect;16.2): spend one book, get a {@code research_paper} for a gene the
 * player has discovered. The server re-checks the database entry and the book.
 */
public record WriteResearchPaperPayload(String geneKey) implements CustomPacketPayload {

    public static final Type<WriteResearchPaperPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "write_research_paper"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WriteResearchPaperPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, WriteResearchPaperPayload::geneKey,
                    WriteResearchPaperPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
