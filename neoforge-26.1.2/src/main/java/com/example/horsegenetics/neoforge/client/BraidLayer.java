package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a <b>rescuing braid</b> worked into a horse's mane or tail, in whatever
 * colour the braid has been dyed.
 *
 * <p>Structural twin of {@link EmissiveCoatLayer}, which is the shape every
 * overlay on this horse takes: one {@code submitModel} of the whole model
 * through a texture that is transparent everywhere it must not paint. What is
 * different here is that the texture is a <b>mask</b> rather than a picture -
 * flat white where the mane or the tail is - and the colour is passed as the
 * submit's own tint. One pair of files therefore covers every colour a player
 * can mix in a cauldron, rather than sixteen baked sheets.
 *
 * <p>The masks are baked from {@code HorseSkinGeometry}, not painted -
 * {@code HairMaskTool}, and {@code :common:bakeHairMasks} when the mesh moves.
 *
 * <p>Two submits rather than one, because a horse can wear a braid in each slot
 * and they can be different colours. Neither costs anything on a horse wearing
 * none: the state fields are zero and this returns on the first line.
 *
 * <p><b>Not seen in a running game.</b>
 */
public class BraidLayer extends RenderLayer<HorseRenderState, HorseModel> {

    private static final Identifier MANE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/braid_mane.png");
    private static final Identifier TAIL =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/braid_tail.png");

    public BraidLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!(state instanceof GeneticHorseRenderState genetic) || state.isInvisible) {
            return;
        }
        // A braid is worked into the hair, so it fades with the hair: a plait
        // left hanging at full colour over a see-through horse would read as a
        // bug rather than as a braid. RiderFade.fade keeps the dye and moves
        // only the alpha.
        float alpha = genetic.fadeAlpha;
        draw(poseStack, submitNodeCollector, lightCoords, state, MANE,
                RiderFade.fade(genetic.braidMane, alpha));
        draw(poseStack, submitNodeCollector, lightCoords, state, TAIL,
                RiderFade.fade(genetic.braidTail, alpha));
    }

    private void draw(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                      HorseRenderState state, Identifier mask, int colour) {
        if (colour == 0) {
            return;     // nothing worn in that slot - see GeneticHorseRenderState
        }
        // ARGUMENT ORDER, and it was wrong here until 2026-09-29. The ten-arg
        // submitModel is
        //   (model, state, pose, renderType, light, overlay,
        //    tintedColor, sprite, outlineColor, crumblingOverlay)
        // and this call had the last two ints the other way round: the braid's
        // dye was being passed as the OUTLINE colour and the tint was
        // state.outlineColor, which is 0 for any horse that is not glowing.
        // A tint of zero is transparent black, so no braid has ever been drawn
        // - and since EntityRenderState.appearsGlowing() is
        // "outlineColor != 0", a dyed braid was quietly asking for a glowing
        // outline instead. Both halves are silent: nothing logs, and an
        // undrawn overlay looks exactly like a horse that is not wearing one.
        collector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.entityTranslucent(mask),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                colour,
                null,
                state.outlineColor,
                null);
    }
}
