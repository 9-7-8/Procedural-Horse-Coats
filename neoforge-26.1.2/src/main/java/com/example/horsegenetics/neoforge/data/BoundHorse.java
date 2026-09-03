package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Payload of the {@code horsegenetics:bound_horse} data component: which horse
 * an item is tied to, by {@link UUID}, plus a display {@code name} for tooltips
 * and the sign face. Carried by a <b>bound stall sign</b>
 * ({@code bound_stall_sign}); a later "bound ticket" can reuse it.
 */
public record BoundHorse(UUID id, String name) {

    public static final Codec<BoundHorse> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BoundHorse::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(BoundHorse::name)
    ).apply(i, BoundHorse::new));

    public static final StreamCodec<ByteBuf, BoundHorse> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BoundHorse::id,
            ByteBufCodecs.STRING_UTF8, BoundHorse::name,
            BoundHorse::new);
}
