package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The breeding-carrot effects waiting on a horse</b> - the effect tokens
 * ({@link CarrotEffect#id()}) of every carrot it has been fed since it last
 * conceived (or sired a conception).
 *
 * <p>They do not expire. Owner, 2026-09-13: a carrot "should just apply
 * indefinitely until the next successful breeding". It used to open a
 * 30-second window and put the horse in love; that made sense while a carrot
 * was fed at the moment of breeding, and none once a mare has to be in heat.
 * So a carrot now only arms the horse, through heat or out of it and through a
 * breeding that did not take, and {@code ReproHandler.breed} uses them up when
 * one does. Filling a seed jar moves a stallion's into the jar.
 *
 * <p>Not {@code copyOnDeath} - a dead horse does not breed.
 */
public record ArmedCarrotsAttachment(List<String> effects) {

    public static final ArmedCarrotsAttachment EMPTY = new ArmedCarrotsAttachment(List.of());

    public ArmedCarrotsAttachment {
        effects = List.copyOf(effects);
    }

    public static final MapCodec<ArmedCarrotsAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("effects", List.of()).forGetter(ArmedCarrotsAttachment::effects)
    ).apply(i, ArmedCarrotsAttachment::new));

    /** One more carrot's worth. Several carrots stack, in the order they were fed. */
    public ArmedCarrotsAttachment plus(List<String> more) {
        List<String> merged = new ArrayList<>(effects);
        merged.addAll(more);
        return new ArmedCarrotsAttachment(merged);
    }
}
