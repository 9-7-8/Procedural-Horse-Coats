package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.pack.PackBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the <b>chests a horse is carrying</b>, one on each flank - whatever
 * was hung there, as itself.
 *
 * <p>Nothing here knows what a chest looks like. The worn stack is resolved to
 * its ordinary item model in {@code GeneticHorseRenderer.extractRenderState}
 * and submitted here, so a barrel is a barrel, a dyed shulker box is that
 * colour, and another mod's crate draws the way its own item does without this
 * mod having heard of it. That is the rendering half of "anything you can
 * place down and store items in".
 *
 * <p>It hangs on the <b>body bone</b>, so it rears with the horse, and sits at
 * {@link PackBox}'s centre - the same three numbers the click test uses, so
 * what a player sees is what they click. A block is a cube and a pannier is
 * not, so it is drawn at half size and squeezed flat against the flank
 * ({@link PackBox#DRAWN_THICK}); vanilla does the same to a donkey's chest by
 * modelling it three pixels deep.
 *
 * <p><b>Not seen in a running game</b> - and the facing is the part to look
 * at: a chest's latch should point away from the horse on both sides.
 */
public class PackLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /** The body bone's origin in the model: {@code PartPose.offset(0, 11, 5)}, in blocks. */
    private static final float BODY_Y = 11f / 16f;
    private static final float BODY_Z = 5f / 16f;
    /** The model's ground: twenty-four units below its origin, with y running down. */
    private static final float GROUND = 24f / 16f;

    public PackLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!(state instanceof GeneticHorseRenderState genetic) || state.isInvisible || state.isBaby) {
            return;
        }
        draw(poseStack, collector, lightCoords, state, genetic.packLeft, PackBox.Side.LEFT);
        draw(poseStack, collector, lightCoords, state, genetic.packRight, PackBox.Side.RIGHT);
    }

    private void draw(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                      HorseRenderState state, ItemStackRenderState chest, PackBox.Side side) {
        if (chest.isEmpty()) {
            return;     // nothing hung on that flank
        }
        poseStack.pushPose();
        ModelPart root = this.getParentModel().root();
        root.translateAndRotate(poseStack);
        root.getChild("body").translateAndRotate(poseStack);
        // Model space: +x is the horse's left, +y is DOWN, -z is its head.
        poseStack.translate(
                (float) (side.sign() * PackBox.CENTRE_OUT),
                GROUND - (float) PackBox.CENTRE_UP - BODY_Y,
                -(float) PackBox.CENTRE_FORWARD - BODY_Z);
        // Stand the block the right way up (a half turn about x: +y up, +z to
        // the head), then turn its front - +z on an unrotated block item, where
        // a chest's latch is - to face away from the horse.
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (side.sign() * 90.0)));
        poseStack.scale((float) PackBox.DRAWN_SIZE, (float) PackBox.DRAWN_SIZE, (float) PackBox.DRAWN_THICK);
        chest.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
