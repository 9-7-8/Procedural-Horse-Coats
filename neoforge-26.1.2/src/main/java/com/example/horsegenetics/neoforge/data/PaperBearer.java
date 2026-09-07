package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Payload of the {@code horsegenetics:paper_bearer} component: <b>who a blank
 * transfer paper is bound to</b>. Stamped on at the crafting bench, because
 * that is the only moment the game knows whose paper it is.
 *
 * <p>Binding is what stops a blank paper being a universal bill of sale. Only
 * the bound player can sign one, and only against a horse they currently own,
 * so a paper is always written by someone with the standing to write it. The
 * <i>signed</i> paper carries no binding at all - it is a bearer instrument,
 * and passing it to another player is the whole point.
 *
 * <p>The {@code name} is a display copy for the tooltip; the {@code id} is what
 * is actually checked.
 */
public record PaperBearer(UUID id, String name) {

    public static final Codec<PaperBearer> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(PaperBearer::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(PaperBearer::name)
    ).apply(i, PaperBearer::new));

    public static final StreamCodec<ByteBuf, PaperBearer> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PaperBearer::id,
            ByteBufCodecs.STRING_UTF8, PaperBearer::name,
            PaperBearer::new);
}
