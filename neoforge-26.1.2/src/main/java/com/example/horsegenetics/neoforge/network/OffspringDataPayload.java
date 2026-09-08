package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseRecordCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Server -&gt; client: a horse's descendants, <b>generation by generation</b> -
 * its foals, then their foals, and so on.
 *
 * <p>Full {@link HorseRecord}s, the way {@link FamilyTreeDataPayload} sends
 * them, and for the same reason: a record carries its <b>epigenome</b>, so the
 * screen can draw each descendant in the coat it really has rather than in a
 * plausible stand-in. That is expensive - an epigenome code is thousands of
 * characters - which is exactly why this payload is only ever sent in answer to
 * a button press, and why {@link #MAX_PER_GENERATION} and
 * {@link #MAX_GENERATIONS} are small. A horse with more foals than the cap
 * shows the first of them and the screen says so.
 *
 * <p>Each generation is one {@link Generation}, in order, so the shape the
 * screen draws is the shape that arrives; flattening the list and rebuilding it
 * on the client would be the same data with a step to get wrong.
 */
public record OffspringDataPayload(UUID rootId, List<Generation> generations)
        implements CustomPacketPayload {

    /** How far down the line to walk. Great-grandfoals and one more. */
    public static final int MAX_GENERATIONS = 5;

    /** Per generation, not in total - a popular stallion's foals go wide fast. */
    public static final int MAX_PER_GENERATION = 24;

    /**
     * One rung of the descent. {@code truncated} is set when the generation was
     * cut at the cap, so the screen can say "and more" instead of quietly
     * showing a partial family as if it were the whole one.
     */
    public record Generation(List<HorseRecord> horses, boolean truncated) {
    }

    public static final Type<OffspringDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "offspring_data"));

    private static final StreamCodec<ByteBuf, Generation> GENERATION_STREAM_CODEC = StreamCodec.composite(
            HorseRecordCodecs.LIST_STREAM_CODEC, Generation::horses,
            ByteBufCodecs.BOOL, Generation::truncated,
            Generation::new
    );

    public static final StreamCodec<ByteBuf, OffspringDataPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OffspringDataPayload::rootId,
            GENERATION_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_GENERATIONS)),
            OffspringDataPayload::generations,
            OffspringDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
