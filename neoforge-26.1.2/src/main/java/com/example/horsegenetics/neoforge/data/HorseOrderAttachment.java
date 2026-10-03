package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.care.HorseOrder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * <b>The command whistle's standing order on one horse</b> ({@code horsegenetics:horse_order}).
 *
 * @param order     what it was told; {@link HorseOrder#REJOIN_HERD} is "no order", the default
 * @param anchor    where an anchored order (Stay) holds it
 * @param dimension the dimension the order was given in, as its id. A horse found in
 *                  another one has changed dimension, and its order is cleared
 * @param orderedBy the player who gave it: Follow follows them, and a horse whose owner
 *                  is no longer them (sold, transferred) has its order cleared
 *
 * <p>Saved by the order's NAME (an unknown one loads as no order) - see {@link HorseOrder}.
 * Synced, so the client can show a horse's order and grey the wheel without asking.
 */
public record HorseOrderAttachment(HorseOrder order, Optional<BlockPos> anchor, String dimension,
                                   Optional<UUID> orderedBy) {

    public static final HorseOrderAttachment NONE =
            new HorseOrderAttachment(HorseOrder.REJOIN_HERD, Optional.empty(), "", Optional.empty());

    private static final Codec<HorseOrder> ORDER_CODEC =
            Codec.STRING.xmap(HorseOrder::byName, HorseOrder::name);

    public static final MapCodec<HorseOrderAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ORDER_CODEC.optionalFieldOf("order", HorseOrder.REJOIN_HERD).forGetter(HorseOrderAttachment::order),
            BlockPos.CODEC.optionalFieldOf("anchor").forGetter(HorseOrderAttachment::anchor),
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(HorseOrderAttachment::dimension),
            UUIDUtil.CODEC.optionalFieldOf("ordered_by").forGetter(HorseOrderAttachment::orderedBy)
    ).apply(i, HorseOrderAttachment::new));

    public static final StreamCodec<ByteBuf, HorseOrderAttachment> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(HorseOrder::byName, HorseOrder::name), HorseOrderAttachment::order,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HorseOrderAttachment::anchor,
            ByteBufCodecs.STRING_UTF8, HorseOrderAttachment::dimension,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), HorseOrderAttachment::orderedBy,
            HorseOrderAttachment::new);

    public boolean hasOrder() {
        return order != HorseOrder.REJOIN_HERD;
    }
}
