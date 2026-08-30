package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;

/**
 * Overrides just enough of vanilla's HorseRenderer to plug our own texture
 * selection in. Everything else (model, animation, markings layer) is left
 * to vanilla - we're only replacing the base coat texture.
 *
 * CAVEAT: this assumes HorseRenderer's render-state generic is open enough
 * to swap in GeneticHorseRenderState via a covariant createRenderState()
 * override, the way the NeoForged porting primers show for custom entities.
 * If vanilla's HorseRenderer is hard-coded to HorseRenderState rather than
 * generic over it, this pattern won't compile as-is and you'd instead need
 * to fully re-implement HorseRenderer (copy vanilla's class and modify) or
 * use a client-side lookup keyed by entity id directly in getTextureLocation
 * without a custom render state at all. Confirm against the decompiled
 * 26.1.2 source before assuming this compiles unmodified.
 */
public class GeneticHorseRenderer extends HorseRenderer {

    public GeneticHorseRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GeneticHorseRenderState createRenderState() {
        return new GeneticHorseRenderState();
    }

    @Override
    public void extractRenderState(Horse horse, net.minecraft.client.renderer.entity.state.HorseRenderState renderState, float partialTick) {
        super.extractRenderState(horse, renderState, partialTick);
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            CoatData coatData = ClientCoatCache.get(horse.getId());
            if (coatData != null) {
                geneticState.coatData = coatData;
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.client.renderer.entity.state.HorseRenderState renderState) {
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            return switch (geneticState.coatData.phenotype()) {
                case BAY -> GeneticCoatTextureFactory.getOrCreate(geneticState.coatData);
                case BLACK -> ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_black.png");
                case CHESTNUT -> ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_chestnut.png");
            };
        }
        return super.getTextureLocation(renderState);
    }
}
