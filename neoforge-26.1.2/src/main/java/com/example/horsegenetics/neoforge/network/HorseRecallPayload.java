package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Client -&gt; server: <b>send this horse home</b> - the horse browser's
 * <i>Send home</i> button. Carries the horse's <b>record</b> UUID rather than an
 * entity id, because the rows worth pressing it on are mostly horses with no
 * entity on this client at all.
 *
 * <p>The id is the whole packet and it is not trusted: it names a horse, it does
 * not assert anything about it. {@code StallRecall.request} re-checks ownership,
 * death, whether the horse has a stall, whether the player has a pen and whether
 * they hold a ticket that reaches - all server-side, because a client that says
 * "send horse X home" is a client that could say it about anybody's horse.
 */
public record HorseRecallPayload(UUID horseId) implements CustomPacketPayload {

    public static final Type<HorseRecallPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_recall"));

    public static final StreamCodec<ByteBuf, HorseRecallPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, HorseRecallPayload::horseId,
            HorseRecallPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
