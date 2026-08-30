package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import net.minecraft.client.renderer.entity.state.HorseRenderState;

/**
 * Adds our coat data onto vanilla's per-frame horse render state, following
 * the pattern NeoForged's own primers use for extending render states (see
 * the "extractRenderState" pattern - add a field, populate it in
 * extractRenderState, read it back in getTextureLocation/submit).
 *
 * NOTE: entity rendering has kept moving in the 1.21.6+ / 26.x line (e.g. the
 * render()/submit()+SubmitNodeCollector split that block entity renderers
 * picked up). Double check against the current NeoForged docs for 26.1.2
 * whether AbstractHorseRenderer still exposes a simple getTextureLocation()
 * override point or whether texture selection has moved into submit().
 */
public class GeneticHorseRenderState extends HorseRenderState {

    /** Defaults to solid chestnut so an un-extracted state never NPEs. */
    public CoatData coatData = CoatData.solid(CoatPhenotype.CHESTNUT);
}
