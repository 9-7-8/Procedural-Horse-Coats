package com.example.horsegenetics.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws a {@code glow} gene's emissive coat regions - the mask baked by
 * {@link GeneticCoatTextureFactory#resolve} - a second time over the
 * base coat at <b>full brightness</b>, so a Suntouched horse's gold mane keeps
 * glowing in the dark.
 *
 * <p>Structural twin of vanilla's {@code HorseMarkingLayer}: same
 * {@code RenderLayer<HorseRenderState, HorseModel>} shape, same
 * {@code submitNodeCollector.order(1).submitModel(...)} call - only the render
 * type ({@link RenderTypes#eyes} instead of {@code entityTranslucent}) and the
 * light coords (forced bright) differ. It is a no-op unless
 * {@link GeneticHorseRenderState#emissiveCoatId} was set this frame.
 */
public class EmissiveCoatLayer extends RenderLayer<HorseRenderState, HorseModel> {

    public EmissiveCoatLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!(state instanceof GeneticHorseRenderState geneticState) || geneticState.emissiveCoatId == null) {
            return;
        }
        if (state.isInvisible) {
            return;
        }
        // A glow fades with the horse wearing it. Left at full strength it
        // would be the most conspicuous thing on a see-through animal - a
        // solid mane hanging in the air - which is exactly the failure the
        // fade was specified to avoid.
        //
        // It can fade at all only because RenderPipelines.EYES carries a
        // BlendFunction.TRANSLUCENT on its colour target, unlike the cutout
        // pipelines the coat and the gear are drawn on; so here the tint alone
        // is enough and no render type has to be swapped. The ten-argument
        // overload is used rather than the eight, which hard-codes a tint of
        // -1 and has no way to say anything else.
        submitNodeCollector.order(1)
                .submitModel(
                        this.getParentModel(),
                        state,
                        poseStack,
                        RenderTypes.eyes(geneticState.emissiveCoatId),
                        LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        RiderFade.tint(geneticState.fadeAlpha),
                        null,
                        state.outlineColor,
                        null
                );
    }
}
