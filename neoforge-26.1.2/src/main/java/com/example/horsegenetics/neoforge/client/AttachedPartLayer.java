package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.parts.SaddleZone;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
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
 * Draws the horse's <b>grown parts</b> - a unicorn horn, a rack of antlers - as
 * meshes hung on bones the horse model already animates.
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
     * Where the <b>left</b> antler roots on the adult skull; the right is the same
     * with {@code x} negated. Vanilla's {@code left_ear} spans {@code x 0.55..2.55,
     * y -13..-10, z 4..5}, so this is just inside the ear and a unit forward of it,
     * half a unit into the top of the skull - the pedicle. A first placement from the
     * boxes, not a tuned one.
     */
    private static final float[] ADULT_CROWN = {1.5f, -10.5f, 3.0f};

    /**
     * The same on the foal. Never drawn - a foal wears no antlers - and here because
     * the switch below must answer for every anchor on both models.
     */
    private static final float[] FOAL_CROWN = {1.5f, -3.45f, 0.8f};

    /**
     * Where the <b>left</b> dragon horn roots on the adult skull; the right is the
     * same with {@code x} negated. The head box ends at {@code x 3} and {@code z 5},
     * and vanilla's {@code left_ear} spans {@code x 0.55..2.55, y -13..-10, z 4..5} -
     * so {@code x 2.6} is just outside the ear, {@code z 4.5} half a unit inside the
     * back of the skull, and {@code y -9.5} a unit and a half below its top: the side
     * of the poll, behind the ear and clear of the {@link #ADULT_CROWN} antler root
     * two units forward. A first placement from the boxes, not a tuned one.
     */
    private static final float[] ADULT_NAPE = {2.6f, -9.5f, 4.5f};

    /** The same on the foal. Never drawn - a foal grows no dragon horns. */
    private static final float[] FOAL_NAPE = {2.6f, -2.95f, 1.8f};

    /**
     * Where a row along the back roots on the <b>adult</b> body, in body-local model
     * units - the first anchor off the head. Read from the 26.1.2 sources, not seen:
     * the {@code body} box is {@code (-5,-8,-17)} sized {@code (10,10,22)} on a pivot of
     * {@code (0,11,5)} (and {@code HdHorseModel.createHdBodyMesh} copies it exactly), so
     * its top is {@code y -8} and it runs {@code z -17..5}. The neck's back edge and the
     * mane meet that top at about {@code z -10..-12} with the head at rest, so
     * {@code z -11} is the withers; {@code y -7.6} is under half a unit inside the top,
     * so a raked spine shows no gap at its root. The row runs back along {@code +z}
     * from here ({@code DorsalSpineGenerator.ROW_LENGTH}).
     */
    private static final float[] ADULT_SPINE = {0f, -7.6f, -11.0f};

    /**
     * The same on the foal. Never drawn - a foal wears no body part
     * ({@code PartKind.showsOnFoal}) - and the adult's value rather than a throw,
     * because the switch below must answer for every anchor on both models.
     */
    private static final float[] FOAL_SPINE = ADULT_SPINE;

    /**
     * Where the <b>left</b> flank's plates root on the adult body, in body-local units;
     * the right is the same with {@code x} negated. Read from the 26.1.2 sources, not
     * seen: the {@code body} box runs {@code x -5..5}, {@code y -8..2}, {@code z -17..5},
     * so {@code x 5} is the side wall, {@code y -7.2} under a unit below the line of the
     * back (clear of a dorsal row's roots), and {@code z -6} halfway along. The plate mesh
     * puts its shoulder cluster 7.5 units forward of here ({@code z -13.5}) and its hip
     * cluster 9.2 back ({@code z 3.2}), so both stand clear of the saddle's
     * {@code z -9.5..0.5} ({@link #SADDLE_FRONT}). Where a flap or barding meets them is
     * the treatment's open look, not engineered ahead of being seen.
     */
    private static final float[] ADULT_FLANK = {5.0f, -7.2f, -6.0f};

    /** The same on the foal. Never drawn - a foal wears no body part - as {@link #FOAL_SPINE}. */
    private static final float[] FOAL_FLANK = ADULT_FLANK;

    /**
     * Where a narwhal horn roots on the <b>adult</b> muzzle, in head-local units. Read
     * from the 26.1.2 sources, not seen: {@code upper_mouth} is a {@code PartPose.ZERO}
     * child of {@code head_parts}, as {@code head} is, so the head bone's frame is the
     * muzzle's too, and vanilla animates no jaw ({@code AbstractEquineModel.setupAnim}
     * moves only {@code head_parts}) - the horn cannot be left behind by a mouth that
     * moves. The muzzle box is {@code x -2..2, y -11..-6, z -7..-2}, front {@code -z};
     * so {@code y -8.5} is the middle of its front face and {@code z -6.5} half a unit
     * inside it, enough that the horn shows no gap at its root.
     */
    private static final float[] ADULT_SNOUT = {0f, -8.5f, -6.5f};

    /** The same on the foal. Never drawn - a foal grows no narwhal horn - as {@link #FOAL_SPINE}. */
    private static final float[] FOAL_SNOUT = ADULT_SNOUT;

    /**
     * Where the <b>left</b> boar tusk roots on the adult muzzle, in head-local units;
     * the right is the same with {@code x} negated. Read from the 26.1.2 sources, not
     * seen: the muzzle box is {@code x -2..2, y -11..-6, z -7..-2} with {@code -y} the
     * top of the nose and {@code -z} the front, and the model has no separate lower jaw
     * (see {@link #ADULT_SNOUT}: nothing animates one), so the "corner of the jaw" is a
     * place on that one box. {@code x 1.8} is just inside the side wall, {@code y -7.2}
     * a unit above the chin where a mouth line would be, and {@code z -4.6} the middle
     * of the muzzle's length. The tusk leaves sideways and curls up the side of the nose
     * ({@code TuskGenerator}). A first placement from the boxes, not a tuned one.
     */
    private static final float[] ADULT_JAW = {1.8f, -7.2f, -4.6f};

    /** The same on the foal. Never drawn - a foal grows no tusks - as {@link #FOAL_SPINE}. */
    private static final float[] FOAL_JAW = ADULT_JAW;

    /**
     * Where the <b>left</b> sabre fang roots on the adult muzzle, in head-local units;
     * the right is the same with {@code x} negated. Read from the same box, not seen:
     * {@code z -6.9} is just inside the front face, so the front half of the blade
     * stands proud of it and shows from its root down; {@code x 1.55} is inside the
     * face's corner, and {@code y -7.6} the mouth line, a unit and a half above the chin
     * the fang hangs past. The mesh turns itself over to hang ({@code SabreGenerator}),
     * so this anchor needs no turn of its own. A first placement, not a tuned one.
     */
    private static final float[] ADULT_LIP = {1.55f, -7.6f, -6.9f};

    /** The same on the foal. Never drawn - a foal grows no fangs - as {@link #FOAL_SPINE}. */
    private static final float[] FOAL_LIP = ADULT_LIP;

    /**
     * How far a part at {@link PartAnchor#SNOUT} is turned before it is drawn, about x.
     * Every part mesh grows up its own {@code -y}; a quarter-turn about {@code +x} takes
     * {@code -y} to {@code -z}, forward along the head's own axis - which, with the neck's
     * rest pitch, points a little below level. The per-horse tilt is applied after this,
     * so a negative tilt lifts the point.
     */
    private static final float SNOUT_PITCH = (float) (Math.PI / 2);

    /**
     * The saddle, along the body, in body-local units. Vanilla's
     * {@code EquineSaddleModel.createSaddleLayer} hangs it on {@code body} as a box
     * {@code (-5,-8,-9)} sized {@code (10,9,9)} inflated by {@code 0.5}, so it covers
     * {@code z -9.5..0.5}. Read from the 26.1.2 sources, not seen. The saddle pad
     * ({@code HorseTackSlot.SADDLE_PAD}) is a slot nothing draws yet, so it cannot
     * reach past this today.
     */
    private static final float SADDLE_FRONT = -9.5f;
    /** @see #SADDLE_FRONT */
    private static final float SADDLE_BACK = 0.5f;
    /**
     * How far either side of the saddle a spine's root still counts as under it: a
     * spine is a unit or two thick and rakes back, so one rooted just clear of the
     * pommel would still stand in the rider's lap. A first value, not a tuned one.
     */
    private static final float SADDLE_MARGIN = 1.0f;

    /** The saddle zone along a {@code SPINE} part, in the anchor's own units - what {@code PartMeshes} asks. */
    static final float SADDLE_ZONE_FROM = SADDLE_FRONT - SADDLE_MARGIN - ADULT_SPINE[2];
    /** @see #SADDLE_ZONE_FROM */
    static final float SADDLE_ZONE_TO = SADDLE_BACK + SADDLE_MARGIN - ADULT_SPINE[2];

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
        // A saddle that is DRAWN, or a rider: a phantom saddle is present and not
        // drawn, and its rider still sits on the back. isRidden is vanilla's own
        // (AbstractHorseRenderer: entity.isVehicle()).
        boolean saddleDrawn = state.saddle != null && !state.saddle.isEmpty()
                && !state.saddle.has(ModDataComponents.PHANTOM_SADDLE.get());
        boolean underSaddle = SaddleZone.covers(saddleDrawn, state.isRidden);
        for (AttachedPart part : genetic.parts) {
            // A foal grows no antlers, ram's horns or dragon horns - they come with
            // maturity (PartKind.showsOnFoal) - but wears its half-size horn.
            if (state.isBaby && !part.kind().showsOnFoal()) {
                continue;
            }
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
            // A row along the back scales each element about its own root instead -
            // across a row, this scale would stretch it lengthwise too. That goes
            // through the slice, applied in setupAnim like the count.
            boolean perElement = part.kind().scalesPerElement();
            if (!perElement) {
                poseStack.scale(part.girth(), part.stretch(), part.girth());
            } else if (part.span() != 1f) {
                // How much of the back a sail covers: the row drawn shorter along its own
                // length, z, from the withers. Spacing and membrane panels shorten with it.
                poseStack.scale(1f, 1f, part.span());
            }

            PartModel model = PartMeshes.get(part.shape());
            float shown = part.shown();
            int hidden = underSaddle && part.kind().saddleZoned() ? model.saddleGroups(part.span()) : 0;
            float along = perElement ? part.stretch() : 1f;
            float across = perElement ? part.girth() : 1f;
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
            //
            // A see-through part is two: its kind's see-through regions blended, the
            // rest solid - so the expensive blended pass draws the least it can. A
            // crystal antler's bone shafts and solid points; a sail's membrane and solid
            // spines. Which regions is the kind's data (PartKind.translucentRegions); the
            // base colour goes see-through and the tip colour stays solid, so a two-tone
            // sail is a membrane of one colour on spines of the other. At opacity 0 - a
            // bone sail, bare rays - the blended pass is skipped altogether.
            boolean glow = part.emissive() && genetic.drawPartGlow;
            int glowRegions = part.kind().glowRegions();
            //
            // Every slice carries the saddle zone and the per-element scale, which are
            // nothing (0, 1, 1) for every part that is not a row along the back.
            if (part.translucent()) {
                int see = part.kind().translucentRegions();
                if (part.opacity() > 0f) {
                    submitSlice(submitNodeCollector, model,
                            PartModel.Slice.regions(see, shown).perGroup(hidden, along, across), poseStack,
                            genetic, lightCoords,
                            RiderFade.fade(part.baseTint(), alpha * part.opacity()), true);
                }
                submitSlice(submitNodeCollector, model,
                        PartModel.Slice.regions(PartSheet.SOLID & ~see, shown).perGroup(hidden, along, across),
                        poseStack, genetic, lightCoords, RiderFade.fade(part.tipTint(), alpha),
                        genetic.isFading());
            } else if (!part.twoTone()) {
                submitSlice(submitNodeCollector, model,
                        PartModel.Slice.regions(PartSheet.SOLID, shown).perGroup(hidden, along, across),
                        poseStack, genetic, lightCoords, RiderFade.fade(part.baseTint(), alpha),
                        genetic.isFading());
            } else {
                int n = model.segmentCount();
                for (int i = 0; i < n; i++) {
                    int tint = RiderFade.fade(part.tintAt(model.along(i)), alpha);
                    submitSlice(submitNodeCollector, model,
                            PartModel.Slice.segment(i, shown).perGroup(hidden, along, across), poseStack,
                            genetic, lightCoords, tint, genetic.isFading());
                    if (glow) {
                        submitGlow(submitNodeCollector, model,
                                new PartModel.Slice(i, shown, glowRegions).perGroup(hidden, along, across),
                                poseStack, genetic, tint);
                    }
                }
            }
            if (glow && !part.twoTone()) {
                submitGlow(submitNodeCollector, model,
                        PartModel.Slice.regions(glowRegions, shown).perGroup(hidden, along, across),
                        poseStack, genetic, RiderFade.fade(part.tipTint(), alpha));
            }
            // Leaves are their own colour and their own pass, over boxes every antler
            // mesh already carries and only a blooming horse ever draws.
            if (part.blooms()) {
                submitSlice(submitNodeCollector, model,
                        PartModel.Slice.regions(PartSheet.bit(PartSheet.BLOOM), shown)
                                .perGroup(hidden, along, across),
                        poseStack, genetic, lightCoords, RiderFade.fade(part.bloomTint(), alpha),
                        genetic.isFading());
            }
            poseStack.popPose();
        }
    }

    /**
     * One slice of a part - the whole of it, one segment, or some of its regions -
     * in one colour.
     *
     * @param blended draw it through the blended pipeline: a fading horse, or a
     *                crystal antler's shafts
     */
    private static void submitSlice(SubmitNodeCollector collector, PartModel model,
                                    PartModel.Slice slice, PoseStack poseStack,
                                    GeneticHorseRenderState genetic, int lightCoords, int tint,
                                    boolean blended) {
        collector.order(1).submitModel(
                model,
                slice,
                poseStack,
                blended ? RenderTypes.entityTranslucent(SHEET) : RenderTypes.entityCutout(SHEET),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                tint,
                null,
                genetic.outlineColor,
                null);
    }

    /**
     * The glow pass: the same slice again at full brightness - the way
     * EmissiveCoatLayer redraws a glowing mane - so only a glowing part pays for it,
     * it glows in its own colour whatever that is, and a player who cannot afford it
     * can turn the pass off and keep the part.
     */
    private static void submitGlow(SubmitNodeCollector collector, PartModel model,
                                   PartModel.Slice slice, PoseStack poseStack,
                                   GeneticHorseRenderState genetic, int tint) {
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
            case CROWN_RIGHT, CROWN_LEFT -> {
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                float[] base = baby ? FOAL_CROWN : ADULT_CROWN;
                float side = anchor == PartAnchor.CROWN_LEFT ? 1f : -1f;
                yield new float[] {side * base[0], base[1], base[2]};
            }
            case NAPE_RIGHT, NAPE_LEFT -> {
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                float[] base = baby ? FOAL_NAPE : ADULT_NAPE;
                float side = anchor == PartAnchor.NAPE_LEFT ? 1f : -1f;
                yield new float[] {side * base[0], base[1], base[2]};
            }
            case SPINE -> {
                // The first anchor off the head: body is the root's child, and its own
                // pose is the rear (body.xRot in AbstractEquineModel.setupAnim). Never
                // a child added to body in createHdBodyMesh - that moves the mesh the
                // HD coat UVs are baked against.
                root.getChild("body").translateAndRotate(poseStack);
                yield baby ? FOAL_SPINE : ADULT_SPINE;
            }
            case BODY_RIGHT, BODY_LEFT -> {
                // The body bone again, so the plates rear with it as the spines do.
                root.getChild("body").translateAndRotate(poseStack);
                float[] base = baby ? FOAL_FLANK : ADULT_FLANK;
                float side = anchor == PartAnchor.BODY_LEFT ? 1f : -1f;
                yield new float[] {side * base[0], base[1], base[2]};
            }
            case SNOUT -> {
                // The head bone, so a narwhal horn grazes and tosses with the head; the
                // muzzle is the same frame (see ADULT_SNOUT).
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                yield baby ? FOAL_SNOUT : ADULT_SNOUT;
            }
            case JAW_RIGHT, JAW_LEFT -> {
                // The head bone again: the muzzle shares its frame and no jaw animates.
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                float[] base = baby ? FOAL_JAW : ADULT_JAW;
                float side = anchor == PartAnchor.JAW_LEFT ? 1f : -1f;
                yield new float[] {side * base[0], base[1], base[2]};
            }
            case LIP_RIGHT, LIP_LEFT -> {
                ModelPart headParts = root.getChild("head_parts");
                headParts.translateAndRotate(poseStack);
                headParts.getChild("head").translateAndRotate(poseStack);
                float[] base = baby ? FOAL_LIP : ADULT_LIP;
                float side = anchor == PartAnchor.LIP_LEFT ? 1f : -1f;
                yield new float[] {side * base[0], base[1], base[2]};
            }
        };

        poseStack.translate(at[0] * UNIT, at[1] * UNIT, at[2] * UNIT);
        // After the translate, so the root moves and only the part turns: forward, not up.
        if (anchor == PartAnchor.SNOUT) {
            poseStack.mulPose(Axis.XP.rotation(SNOUT_PITCH));
        }
        if (baby) {
            poseStack.scale(FOAL_SCALE, FOAL_SCALE, FOAL_SCALE);
        }
    }

}
