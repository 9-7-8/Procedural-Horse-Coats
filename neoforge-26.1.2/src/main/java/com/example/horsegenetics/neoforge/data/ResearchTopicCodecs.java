package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Serialization for the {@link ResearchTopic} a {@code research_paper} carries -
 * in the integration layer for the same reason {@link TransferDeedCodecs} is:
 * the domain type stays free of DataFixerUpper.
 *
 * <h2>The one piece of back-compat in this repo</h2>
 * {@code horsegenetics:research_gene} used to be a plain {@code String} gene
 * key, and there are papers in the owner's chests, shelves and villager windows
 * holding one. The rest of the mod refuses legacy shapes outright (CLAUDE.md
 * hard rule 6) and this is the deliberate exception, asked for by name: a stored
 * <b>string</b> still decodes, through {@link ResearchTopic#wholeGene} - which
 * reads it as the homozygous variant pair, the strongest thing a whole-gene
 * paper could have meant.
 *
 * <p>The component keeps its old registry id for exactly that reason. Renaming
 * it to {@code research_topic} would be tidier and would drop the component off
 * every existing paper, leaving blanks.
 *
 * <p>Encoding always writes the map form, so a world loaded once never writes a
 * bare string again.
 */
public final class ResearchTopicCodecs {

    private static final Codec<ResearchTopic> RECORD = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("gene").forGetter(ResearchTopic::geneKey),
            Codec.STRING.optionalFieldOf("a", "").forGetter(ResearchTopic::alleleA),
            Codec.STRING.optionalFieldOf("b", "").forGetter(ResearchTopic::alleleB)
    ).apply(i, ResearchTopic::new));

    public static final Codec<ResearchTopic> CODEC =
            Codec.either(RECORD, Codec.STRING).xmap(
                    either -> either.map(topic -> topic, ResearchTopic::wholeGene),
                    Either::left);

    /**
     * Over the wire it is the flat {@link ResearchTopic#token()}, not the map -
     * the client only ever renders it, and a paper that reached a client at all
     * came from a server that had already decoded it.
     */
    public static final StreamCodec<ByteBuf, ResearchTopic> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(ResearchTopicCodecs::fromToken, ResearchTopic::token);

    private static ResearchTopic fromToken(String token) {
        ResearchTopic topic = ResearchTopic.parse(token);
        return topic == null ? new ResearchTopic("", "", "") : topic;
    }

    private ResearchTopicCodecs() {
    }
}
