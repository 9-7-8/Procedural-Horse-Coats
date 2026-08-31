package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import net.minecraft.client.renderer.entity.state.HorseRenderState;

/**
 * Adds our {@link CoatData} onto vanilla's per-frame horse render state
 * (populated in {@code GeneticHorseRenderer.extractRenderState}, read back in
 * {@code getTextureLocation}). The coat carries the full genotype + epigenetic
 * seed, which is everything {@code GeneticCoatTextureFactory} needs.
 */
public class GeneticHorseRenderState extends HorseRenderState {

    /** Defaults to a plain black horse so an un-extracted state never NPEs. */
    public CoatData coatData = CoatData.DEFAULT;
}
