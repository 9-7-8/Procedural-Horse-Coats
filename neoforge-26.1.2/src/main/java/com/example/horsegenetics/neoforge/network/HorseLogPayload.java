package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.log.HorseEvent;
import com.example.horsegenetics.common.log.HorseEventLog;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseEventCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server -&gt; client: <b>one player's horse history</b>, newest first, for the
 * browser's Log tab.
 *
 * <p>The whole log rather than a page or a delta. That is a deliberate choice
 * and it rests on the log already being bounded server-side: at
 * {@link HorseEventLog#MAX_PER_OWNER} rows of a few dozen bytes each, the worst
 * case is tens of kilobytes, which is less than the roster this screen already
 * sends. Paging would buy nothing except two more states to get wrong - a
 * half-loaded list and a stale page - and the filter would then have to be
 * server-side to be correct, for a list the client can filter in a frame.
 *
 * <p>Sent on request when the tab is opened and its Refresh button is pressed,
 * and <b>pushed unasked whenever a row is written for an online player</b>, so
 * a log left open updates as the day goes on rather than lying until somebody
 * clicks.
 */
public record HorseLogPayload(List<HorseEvent> events) implements CustomPacketPayload {

    public static final Type<HorseLogPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_log"));

    public static final StreamCodec<ByteBuf, HorseLogPayload> STREAM_CODEC = StreamCodec.composite(
            HorseEventCodecs.STREAM_CODEC.apply(
                    ByteBufCodecs.list(HorseEventLog.MAX_PER_OWNER)),
            HorseLogPayload::events,
            HorseLogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
