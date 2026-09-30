package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws the horse's <b>grown parts</b> - today a unicorn horn - as meshes hung on
 * bones the horse model already animates.
 *
 * <p>This is the mod's <b>first visual that is not a texture</b>, and the one
 * structural claim behind it is that a part needs no animation of its own: a mesh
 * parented to {@code head_parts > head} inherits the head bob, the graze and the
 * rear for free, exactly as the horse's own ears do. Everything here is that move
 * plus an offset.
 *
 * <h2>Every part goes through this one layer</h2>
 * Not one layer per part. A horn, a set of antlers, a crystal cluster and
 * (eventually) a horse shoe all reduce to the same thing by the time they arrive -
 * an {@link AttachedPart}: a shape, a transform, a colour - and the layer does not
 * know which gene or which item asked for it. That is what stops three features
 * growing three layers with three different opinions about draw order.
 *
 * <h2>The root transform is applied, and that is not cosmetic</h2>
 * {@code CutieMarkLayer} reaches straight for {@code root().getChild("body")} and
 * hand-tunes its offsets to compensate; this walks the chain from the <b>root</b>
 * down. The difference is that {@code HdHorseModel.createHdLayer} applies
 * {@code MeshTransformer.scaling(1.1F)}, and that transformer rewrites the
 * <i>root part's own pose</i> - a 1.1 scale and a -2.4016 y offset - so a layer
 * that skips the root works in a space 10% too small and two and a half units too
 * high. Walking from the root means the number never appears in this file, and it
 * means the same code is correct for the foal, whose layer carries no such
 * transform at all.
 *
 * <h2>The adult and the foal do not share an offset</h2>
 * They share the bone <i>names</i> and nothing else. The adult head box is
 * {@code y -11..-6, z -2..5}; the foal's is {@code y -3.95..0.05, z -6.71..2.30},
 * on a head part with its own offset. So {@link PartAnchor} names the place and
 * this class keeps one offset per model for it - which is the whole reason the
 * anchor is an enum in {@code common/} and not a bone name.
 *
 * <p><b>Not seen in a running game.</b> The offsets below are computed from the two
 * models' boxes and are a first placement, not a tuned one.
 */
public class AttachedPartLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /** The greyscale grain every part samples; colour is the per-horse tint. */
    private static final Identifier SHEET = Identifier.fromNamespaceAndPath(
            HorseGenetics.MOD_ID, "textures/entity/horse/part_sheet.png");

    /** Packed block+sky light for a part that glows in the dark. */
    private static final int FULL_BRIGHT = 0x00F000F0;

    /** Model units to blocks - what the pose stack expects, and what a box's own vertices use. */
    private static final float UNIT = 1f / 16f;

    /**
     * Where a horn roots on the <b>adult</b> skull, in head-local model units.
     *
     * <p>The head box spans {@code y -11..-6} and {@code z -2..5}, front being -z,
     * and the ears sit at {@code z 4..5}. So {@code y = -10.5} is half a unit inside
     * the top surface - enough that a leaning horn shows no gap at its base - and
     * {@code z = 1} is the flat of the skull between the eyes, three units forward of
     * the ears.
     */
    private static final float[] ADULT_FOREHEAD = {0f, -10.5f, 1.0f};

    /**
     * The same on the <b>foal</b>, whose head is a different box on a differently
     * offset part: {@code y -3.95..0.05}, {@code z -6.71..2.30}, ears at about
     * {@code z 1.1..2.9}.
     */
    private static final float[] FOAL_FOREHEAD = {0f, -3.45f, -1.0f};

    /**
     * How large a foal's horn is against the horn it will grow into.
     *
     * <p>A foal wears its own horn rather than nothing (owner's call) because a horn
     * that appears at maturity hides every placement bug until the horse grows up,
     * and because every other gene on this animal shows on a foal. It is not scaled
     * by the model difference, though: vanilla's foal is a big-headed thing whose
     * skull is nearly the adult's, so matching the head would give a newborn a
     * full-length tusk. Just over half reads as a horn that has started.
     *
     * <p><b>Settled at half, 2026-09-30 (owner).</b> The adult's root carries a 1.1
     * scale and the foal's none, so 0.55 here is exactly half the adult horn in
     * world size. On a foal's near-adult-sized head that reads as comically small,
     * and the owner looked at it and chose to keep it.
     */
    private static final float FOAL_SCALE = 0.55f;

    public AttachedPartLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        // The common case is a horse with no parts, which is almost every horse:
        // one type check and one isEmpty, on the first line, the way BraidLayer
        // returns on a horse wearing no braid.
        if (!(state instanceof GeneticHorseRenderState genetic) || state.isInvisible
                || genetic.parts.isEmpty() || !genetic.drawParts) {
            return;
        }
        float alpha = genetic.fadeAlpha;
        for (AttachedPart part : genetic.parts) {
            poseStack.pushPose();
            anchor(part.kind().anchor(), state.isBaby, poseStack);

            // THESE TWO LINES ARE IN THE ORDER THEY HAVE TO BE, and it reads
            // backwards. A pose stack right-multiplies, so the LAST call applies
            // to the geometry FIRST: the mesh is scaled in its own frame, and only
            // then leaned. That is what makes a non-uniform scale safe - stretch
            // runs along the horn's own axis, and girth across it.
            //
            // Swapping them does not swap the look, it shears it: the scale would
            // then be applied in the anchor's frame to an already-leaned horn, so a
            // long horn on a strong lean would come out as a squashed parallelogram.
            // Nothing would log, and a nub would look fine.
            poseStack.mulPose(Axis.XP.rotation(part.tilt()));
            poseStack.scale(part.girth(), part.stretch(), part.girth());

            PartModel model = PartMeshes.get(part.shape());
            // A grown part fades with the horse it grew on - a solid horn hanging in
            // the air over a see-through horse would read as a bug. RiderFade.fade
            // keeps the colour and moves only the alpha, and the render type has to
            // change with it: a cutout pipeline carries no blend function, so a
            // half-transparent tint through it draws a fully solid horn and then, at
            // a low enough alpha, no horn at all.
            //
            // A one-colour part is one submit. A two-tone one is one submit PER
            // SEGMENT, each drawing only its own box in the colour tintAt gives
            // that far along - see PartModel on why that has to be a Slice applied
            // in setupAnim and not a skipDraw set here. Two-tone horns are the rare
            // case (two different colour alleles on a horse that is already one in
            // four hundred), so the common horn still costs one draw.
            boolean glow = part.emissive() && genetic.drawPartGlow;
            if (!part.twoTone()) {
                submitSlice(submitNodeCollector, model, PartModel.Slice.ALL, poseStack, genetic,
                        lightCoords, RiderFade.fade(part.baseTint(), alpha), glow);
            } else {
                int n = model.segmentCount();
                for (int i = 0; i < n; i++) {
                    float along = n == 1 ? 0f : (float) i / (n - 1);
                    submitSlice(submitNodeCollector, model, new PartModel.Slice(i), poseStack,
                            genetic, lightCoords, RiderFade.fade(part.tintAt(along), alpha), glow);
                }
            }
            poseStack.popPose();
        }
    }

    /**
     * One slice of a part - the whole of it, or one segment - in one colour, plus
     * its glow pass when it glows.
     *
     * <p>The glow is a second submit of the same slice at full brightness - the way
     * EmissiveCoatLayer redraws a glowing mane - so only a glowing horn pays for it,
     * it glows in its own colour whatever that is, and a player who cannot afford
     * it can turn the pass off and keep the horn.
     */
    private static void submitSlice(SubmitNodeCollector collector, PartModel model,
                                    PartModel.Slice slice, PoseStack poseStack,
                                    GeneticHorseRenderState genetic, int lightCoords, int tint,
                                    boolean glow) {
        collector.order(1).submitModel(
                model,
                slice,
                poseStack,
                genetic.isFading() ? RenderTypes.entityTranslucent(SHEET)
                        : RenderTypes.entityCutout(SHEET),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                tint,
                null,
                genetic.outlineColor,
                null);
        if (glow) {
            collector.order(2).submitModel(
                    model,
                    slice,
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(SHEET, false),
                    FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    tint,
                    null,
                    genetic.outlineColor,
                    null);
        }
    }

    /**
     * Put the pose stack at {@code anchor}, in that bone's own space and in that
     * bone's own units.
     *
     * <p><b>The only place in the mod that knows a horse bone's name</b>, which is
     * the point of {@link PartAnchor} being an enum: retargeting an anchor is an
     * edit here and never an edit to a gene.
     *
     * <p>Each {@code translateAndRotate} contributes that part's animated offset,
     * rotation and scale, dividing its translation by 16 as it goes - so after the
     * chain a model unit is {@link #UNIT} of a block, exactly as it is for the
     * cubes of the model itself.
     */
    private void anchor(PartAnchor anchor, boolean baby, PoseStack poseStack) {
        ModelPart root = this.getParentModel().root();
        // The root first, for every anchor: it carries the 1.1 mesh transform on the
        // adult and nothing on the foal, and skipping it is how a part ends up 10%
        // small and two and a half units high.
        root.translateAndRotate(poseStack);

        // A switch EXPRESSION, so the compiler refuses a new PartAnchor with no case
        // here. A statement switch would silently draw the new part at the horse's
        // origin, which is the shape of bug this codebase keeps paying for: a case on
        // a value nothing handles, failing by drawing nothing anybody can trace.
        float[] at = switch (anchor) {
            case FOREHEAD -> {
                // head_parts carries the neck angle; head is its child.
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                yield baby ? FOAL_FOREHEAD : ADULT_FOREHEAD;
            }
        };

        poseStack.translate(at[0] * UNIT, at[1] * UNIT, at[2] * UNIT);
        if (baby) {
            poseStack.scale(FOAL_SCALE, FOAL_SCALE, FOAL_SCALE);
        }
    }

}
