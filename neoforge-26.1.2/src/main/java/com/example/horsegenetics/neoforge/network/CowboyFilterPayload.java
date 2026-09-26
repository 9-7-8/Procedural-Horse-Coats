package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: filter the cowboy's offer list to the horses matching
 * {@code query} - a SQL WHERE clause, the same language every other horse
 * search box in the mod takes ({@code HorseQuery}). Blank clears it.
 *
 * <p><b>The filtering has to happen on the server</b>, which is the whole reason
 * this packet exists rather than the screen simply drawing fewer rows. A trade
 * is sent back as an <i>index</i> into the merchant's offer list, so a list the
 * client has quietly shortened makes row three mean one horse on screen and a
 * different one on the server - and the player buys the wrong animal, for
 * twelve to twenty-eight emeralds, with nothing anywhere reporting an error.
 * Filtering server-side and resending the offers keeps one list, so the indices
 * cannot disagree.
 */
public record CowboyFilterPayload(String query) implements CustomPacketPayload {

    /** Long enough for "horsegenetics.something" plus a word; a filter box is not an essay. */
    public static final int MAX_QUERY = 64;

    public static final Type<CowboyFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "cowboy_filter"));

    public static final StreamCodec<ByteBuf, CowboyFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_QUERY), CowboyFilterPayload::query,
            CowboyFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
