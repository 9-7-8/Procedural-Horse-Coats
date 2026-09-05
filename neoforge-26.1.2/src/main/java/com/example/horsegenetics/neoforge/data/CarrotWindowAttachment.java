package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * A live <b>breeding-carrot window</b> on a horse (roadmap wiki &sect;14). A
 * carrot fed to a parent puts a temporary window on it - the same shape as
 * vanilla breeding-mode love - carrying the carrot's effect tokens
 * ({@link CarrotEffect#id()}) and the game time it lapses. While the window is
 * live the effects <b>are</b> stored on the horse and are part of the breeding
 * draw's input; they are consumed when the pair breeds and gone when the window
 * lapses. There is no permanent stored carrot state.
 *
 * <p>Not {@code copyOnDeath} - a dead horse does not breed.
 */
public record CarrotWindowAttachment(List<String> effects, long expiresAt) {

    public static final CarrotWindowAttachment EMPTY = new CarrotWindowAttachment(List.of(), 0L);

    /** The same length as vanilla breeding-mode love (30 s). */
    public static final long WINDOW_TICKS = 30L * 20L;

    public CarrotWindowAttachment {
        effects = List.copyOf(effects);
    }

    public static final MapCodec<CarrotWindowAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("effects", List.of()).forGetter(CarrotWindowAttachment::effects),
            Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(CarrotWindowAttachment::expiresAt)
    ).apply(i, CarrotWindowAttachment::new));

    public static final Codec<CarrotWindowAttachment> CODEC = MAP_CODEC.codec();

    public boolean isActiveAt(long now) {
        return !effects.isEmpty() && now < expiresAt;
    }

    /** The parsed effects if the window is still open at {@code now}, else empty. */
    public List<CarrotEffect> activeEffects(long now) {
        return isActiveAt(now) ? CarrotEffect.parseList(effects) : List.of();
    }

    /** Add {@code more} tokens and (re)start the window from {@code now}. */
    public CarrotWindowAttachment plus(List<String> more, long now) {
        java.util.List<String> merged = new java.util.ArrayList<>(effects);
        merged.addAll(more);
        return new CarrotWindowAttachment(merged, now + WINDOW_TICKS);
    }
}
