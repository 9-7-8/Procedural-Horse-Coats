package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Draws the <b>cutie mark</b> - one to three flat item emblems on each flank,
 * on top of the finished coat and every other layer, so a white horse still
 * shows its mark.
 *
 * <p>No-op unless the horse is {@code Cutmrk/Cutmrk}. Everything about the
 * emblem (which items, how many, row vs triangle, size, tilt) comes from
 * {@link CutieMarkGene#markFor}, off the expressing allele copy's epigenetic
 * seed; the normalised item picks are resolved against
 * {@link FlatItemCatalog}.
 *
 * <p>Placement is an approximation - a fixed offset out from the animated body
 * part - and wants tuning against a live horse.
 */
public class CutieMarkLayer extends RenderLayer<HorseRenderState, HorseModel> {

    private static final float BASE_SCALE = 0.30f;
    private static final float SPACING = 0.16f;

    private final ItemStackRenderState[] itemStates = {
            new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()
    };

    public CutieMarkLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !(state instanceof GeneticHorseRenderState genetic)) {
            return;
        }
        Optional<CutieMarkGene.Mark> maybe = Genes.CUTIE_MARK.markFor(
                genetic.coatData.genotype(), genetic.coatData.epigenome());
        if (maybe.isEmpty()) {
            return;
        }
        CutieMarkGene.Mark mark = maybe.get();
        int count = Math.min(mark.count(), itemStates.length);

        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();
        Level level = mc.level;
        for (int i = 0; i < count; i++) {
            ItemStack stack = new ItemStack(FlatItemCatalog.pick(mark.picks()[i]));
            resolver.updateForTopItem(itemStates[i], stack, ItemDisplayContext.FIXED, level, null, i);
        }

        ModelPart body = this.getParentModel().root().getChild("body");
        float scale = BASE_SCALE * (float) mark.scale();

        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            body.translateAndRotate(poseStack);
            // Out to the flank surface (body cube is ~10 wide, ~10 tall in model
            // units), then face the emblem outward and give it the per-horse tilt.
            poseStack.translate(side * 5.7f / 16f, -3.0f / 16f, -6.0f / 16f);
            poseStack.mulPose(Axis.YP.rotationDegrees(side > 0 ? -90f : 90f));
            poseStack.mulPose(Axis.ZP.rotation((float) mark.tilt()));

            for (int i = 0; i < count; i++) {
                float[] xy = layout(i, count, mark.triangle());
                poseStack.pushPose();
                poseStack.translate(xy[0] * SPACING, xy[1] * SPACING, 0.0f);
                poseStack.scale(scale, scale, scale);
                itemStates[i].submit(poseStack, submitNodeCollector, lightCoords,
                        OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    /** Emblem-local offset (in "slots") for item {@code i} of {@code n}. */
    private static float[] layout(int i, int n, boolean triangle) {
        if (n == 1) {
            return new float[]{0f, 0f};
        }
        if (n == 2) {
            return new float[]{i == 0 ? -0.55f : 0.55f, 0f};
        }
        if (triangle) {
            return switch (i) {
                case 0 -> new float[]{-0.5f, -0.4f};
                case 1 -> new float[]{0.5f, -0.4f};
                default -> new float[]{0f, 0.55f};
            };
        }
        return new float[]{(i - 1) * 1.0f, 0f};
    }
}
