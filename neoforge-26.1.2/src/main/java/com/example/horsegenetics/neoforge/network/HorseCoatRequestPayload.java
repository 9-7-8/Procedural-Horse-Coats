package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Client -&gt; server: "send me the coats of these horses, I am about to draw
 * them."
 *
 * <h2>Why the roster does not just carry them</h2>
 * A coat is the genotype <b>and the epigenome</b>, and an epigenome code runs to
 * something like eight thousand characters
 * ({@code EpigenomeSizeTest.reportsTheCodeSize} prints the real number). Putting
 * one on every {@link HorseRosterPayload} entry would multiply that packet by
 * an order of magnitude to draw a row of sixteen-pixel horses, most of which
 * are scrolled off screen and will never be looked at.
 *
 * <p>So the browser asks for the rows it is <b>actually drawing</b>, once each,
 * and caches what comes back for the session
 * ({@code client/ClientHorseCoats}). A player who never scrolls pays for one
 * screenful; a player who scrolls the whole stable pays what the roster would
 * have cost anyway, spread out and only because they looked.
 *
 * <p>The reply is a {@link HorseCoatBatchPayload}, and it carries only the
 * epigenome: the client already has every genotype from the roster.
 */
public record HorseCoatRequestPayload(List<UUID> ids) implements CustomPacketPayload {

    /**
     * A ceiling on one request, comfortably more than a screenful of rows. It
     * is also the anti-abuse limit: this payload makes the server read records
     * and write strings, so a forged packet asking for ten thousand should not
     * be able to.
     */
    public static final int MAX_IDS = 64;

    public static final Type<HorseCoatRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_coat_request"));

    public static final StreamCodec<ByteBuf, HorseCoatRequestPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_IDS)), HorseCoatRequestPayload::ids,
            HorseCoatRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
