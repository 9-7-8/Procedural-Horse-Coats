package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
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
 * <p>The mark is <b>modifiable by other genes</b>: {@code markFor} folds every
 * {@link com.example.horsegenetics.common.genetics.CutieMarkContribution} over
 * the base emblem before handing it back, which is how a light-locus horse
 * comes to wear a glowing one. Every field of {@code Mark} must be honoured
 * here - a field this layer ignores is a promise the game does not keep.
 *
 * <p>Placement is an approximation - a fixed offset out from the animated body
 * part, aimed at the upper hindquarter - and wants tuning against a live horse.
 */
public class CutieMarkLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /** Emblem sizing. Smaller than a held item, and it shrinks further as the icon count rises. */
    private static final float BASE_SCALE = 0.24f;
    /** Blocks between adjacent icon centres - they deliberately overlap a little. */
    private static final float SPACING = 0.085f;
    /** Blocks each successive icon steps toward the viewer, so the stack order is unambiguous. */
    private static final float DEPTH_STEP = 0.012f;
    /** Degrees each icon is fanned in-plane, for a "stacked stickers" read. */
    private static final float FAN_DEG = 5.0f;
    /** Packed block+sky light for a mark another gene has made emissive. */
    private static final int FULL_BRIGHT = 0x00F000F0;

    /**
     * The emblem's stacks, built once per mark rather than once per icon per
     * frame (#203). Keyed on the {@link CutieMarkGene.Mark} that
     * {@link com.example.horsegenetics.common.coat.CoatData#cutieMark()} memoises,
     * which is one instance per horse's CoatData: a record's equals/hashCode on
     * its {@code double[]} is the array's identity, so the lookup is cheap and two
     * horses never alias. Weak, so an entry goes when its CoatData does. Render
     * thread only.
     */
    private static final Map<CutieMarkGene.Mark, ItemStack[]> STACKS = new WeakHashMap<>();

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
        // THE ONE PIECE OF A HORSE THAT CANNOT FADE, so while the rest of the
        // horse is see-through this is not drawn at all.
        //
        // The emblems are items, submitted through ItemStackRenderState.submit
        // - and that method takes a light, an overlay and an outline colour
        // and nothing else. There is no tint parameter anywhere on it, and an
        // item picks its own render types out of the model it was baked from,
        // so neither half of what fades a coat is reachable from here. The
        // choice is therefore between a solid emblem on a ghost horse and no
        // emblem, and a sticker floating in the fog is plainly the worse of
        // the two.
        //
        // Cheap to revisit if the owner would rather see it: one line. Faded
        // items would mean bypassing ItemStackRenderState entirely, which is
        // a great deal more than this mark is worth.
        if (genetic.isFading()) {
            return;
        }
        Optional<CutieMarkGene.Mark> maybe = genetic.coatData.cutieMark();
        if (maybe.isEmpty()) {
            return;
        }
        CutieMarkGene.Mark mark = maybe.get();
        int count = Math.min(mark.count(), itemStates.length);
        // A mark another gene has lit (today: the light locus) is drawn
        // full-bright, the same way EmissiveCoatLayer draws a glowing mane.
        int light = mark.emissive() ? FULL_BRIGHT : lightCoords;

        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();
        Level level = mc.level;
        ItemStack[] stacks = STACKS.computeIfAbsent(mark, CutieMarkLayer::stacksFor);
        for (int i = 0; i < count; i++) {
            resolver.updateForTopItem(itemStates[i], stacks[i], ItemDisplayContext.FIXED, level, null, i);
        }

        ModelPart body = this.getParentModel().root().getChild("body");
        // One big icon for a single-item mark; a 3-icon mark packs down so the
        // whole fan still fits inside the haunch.
        float countScale = 1.0f / (1.0f + 0.28f * (count - 1));
        float scale = BASE_SCALE * (float) mark.scale() * countScale;

        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            body.translateAndRotate(poseStack);
            // The body cube is body-local x in [-5,5], y in [-8,2], z in [-17,5]
            // (front is -z). Anchor the emblem mid-haunch (z ~ 0), high on the
            // flank, a touch proud of the x ~ +/-5 side face - far enough
            // forward that a 3-icon fan cannot spill past the rump.
            poseStack.translate(side * 5.6f / 16f, -4.0f / 16f, 0.0f);
            poseStack.mulPose(Axis.YP.rotationDegrees(side > 0 ? -90f : 90f));
            // +PI: entity model space is drawn rotated 180 degrees about Z (the
            // classic "models are upside down"), so an item submitted in the
            // FIXED display context comes out inverted - the extra PI spins it
            // upright again in its own plane. Then the per-horse tilt.
            poseStack.mulPose(Axis.ZP.rotation((float) mark.tilt() + (float) Math.PI));

            for (int i = 0; i < count; i++) {
                float[] xy = layout(i, count, mark.triangle());
                poseStack.pushPose();
                // In-plane fan offset, plus a small step toward the viewer so
                // icon i+1 is unambiguously in front of icon i - no co-planar
                // z-fighting, which was the "weird clipping".
                poseStack.translate(xy[0] * SPACING, xy[1] * SPACING, i * DEPTH_STEP);
                poseStack.mulPose(Axis.ZP.rotationDegrees((i - (count - 1) / 2.0f) * FAN_DEG));
                poseStack.scale(scale, scale, scale);
                itemStates[i].submit(poseStack, submitNodeCollector, light,
                        OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    /** One stack per pick - all three, whatever the count, so the array is index-safe. */
    private static ItemStack[] stacksFor(CutieMarkGene.Mark mark) {
        double[] picks = mark.picks();
        ItemStack[] out = new ItemStack[picks.length];
        for (int i = 0; i < picks.length; i++) {
            out[i] = new ItemStack(FlatItemCatalog.pick(picks[i]));
        }
        return out;
    }

    /** Emblem-local offset (in "slots", scaled by {@link #SPACING}) for icon {@code i} of {@code n}. */
    private static float[] layout(int i, int n, boolean triangle) {
        if (n == 1) {
            return new float[]{0f, 0f};
        }
        if (n == 2) {
            return new float[]{(i - 0.5f) * 1.1f, 0f};
        }
        if (triangle) {
            return switch (i) {
                case 0 -> new float[]{-0.95f, -0.55f};
                case 1 -> new float[]{0.95f, -0.55f};
                default -> new float[]{0f, 0.75f};
            };
        }
        return new float[]{(i - (n - 1) / 2.0f) * 1.1f, 0f};
    }
}
