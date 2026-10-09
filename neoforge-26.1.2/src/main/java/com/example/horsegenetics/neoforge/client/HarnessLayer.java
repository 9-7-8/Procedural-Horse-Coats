package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.pack.HarnessTier;
import com.example.horsegenetics.common.pack.PackBox;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a <b>storage harness</b>: a leather strap over the rump with a second
 * running back along the spine, and on each flank a metal frame with a shelf
 * under it - the thing a chest hangs in. (Owner, 2026-10-08: "a strap over the
 * butt and a metal frame around where the chests fit into".)
 *
 * <p>It is the first piece of gear here drawn as <b>geometry</b> rather than as
 * a mask over the horse's own mesh ({@link BraidLayer}): a frame stands off the
 * flank, and a mask cannot stand off anything. Two small model parts, built
 * once from boxes in the body bone's own units, submitted on the body bone so
 * they rear with the horse - the leather in the dye's colour, the fittings in
 * their metal's. One flat white texture; the colour is the tint.
 *
 * <p>The frames are placed from {@link PackBox}, the same numbers the chest is
 * drawn and clicked at, so a chest sits in its frame by construction: the frame
 * is the chest's outline against the flank, half a pixel proud of it all round.
 *
 * <p><b>Seen in a photo shoot on 2026-10-08</b>; not on a moving horse.
 */
public class HarnessLayer extends RenderLayer<HorseRenderState, HorseModel> {

    /** Vanilla's undyed leather. */
    static final int UNDYED = 0xA06540;

    private static final Identifier SHEET =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "textures/entity/horse/harness.png");

    // The body bone: its box is x -5..5, y -8..2 (y runs DOWN), z -17..5, and
    // its origin is PartPose.offset(0, 11, 5) in a model whose ground is y 24.
    private static final float BACK = -8f;
    private static final float FLANK = 5f;
    /** The chest's centre in the body bone's units, from the numbers it is drawn at. */
    private static final float CHEST_Y = (float) (24.0 - PackBox.CENTRE_UP * 16.0 - 11.0);
    private static final float CHEST_Z = (float) (-PackBox.CENTRE_FORWARD * 16.0 - 5.0);
    private static final float CHEST_HALF = (float) (PackBox.DRAWN_SIZE * 8.0);
    private static final float CHEST_OUT = (float) (PackBox.DRAWN_THICK * 16.0);

    private final ModelPart leather;
    private final ModelPart fittings;

    public HarnessLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
        ModelPart root = LayerDefinition.create(mesh(), 16, 16).bakeRoot();
        this.leather = root.getChild("leather");
        this.fittings = root.getChild("fittings");
    }

    /** The fittings' colour for a tier - the mid tone of that metal's item sprite. */
    static int metal(HarnessTier tier) {
        return switch (tier) {
            case COPPER -> 0xFFC15A36;
            case IRON -> 0xFFC6C6C6;
            case GOLD -> 0xFFE9B115;
            case NETHERITE -> 0xFF4D4346;
        };
    }

    private static MeshDefinition mesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float proud = 0.35f;            // how far the leather stands off the hide
        float strap = 2f;               // a strap's width
        float top = BACK - proud;
        CubeListBuilder leather = CubeListBuilder.create().texOffs(0, 0)
                // over the rump, flank to flank, above the chests
                .addBox(-FLANK - proud, top, CHEST_Z - strap / 2f, (FLANK + proud) * 2f, proud, strap)
                // and down each side to the top of its frame
                .addBox(FLANK, top, CHEST_Z - strap / 2f, proud, CHEST_Y - CHEST_HALF - top, strap)
                .addBox(-FLANK - proud, top, CHEST_Z - strap / 2f, proud, CHEST_Y - CHEST_HALF - top, strap)
                // back along the spine to the tail
                .addBox(-strap / 4f, top, CHEST_Z + strap / 2f, strap / 2f, proud, 5f - (CHEST_Z + strap / 2f));
        root.addOrReplaceChild("leather", leather, PartPose.ZERO);

        float bar = 0.8f;               // a frame bar's thickness
        float rim = 0.5f;               // how far the frame shows past the chest
        float y0 = CHEST_Y - CHEST_HALF - rim;
        float y1 = CHEST_Y + CHEST_HALF + rim;
        float z0 = CHEST_Z - CHEST_HALF - rim;
        float z1 = CHEST_Z + CHEST_HALF + rim;
        float plate = 0.9f;             // how far the frame stands off the flank
        CubeListBuilder fittings = CubeListBuilder.create().texOffs(0, 0);
        for (float side : new float[] {1f, -1f}) {
            float x = side > 0 ? FLANK : -FLANK - plate;
            fittings
                    .addBox(x, y0, z0, plate, bar, z1 - z0)                 // top bar
                    .addBox(x, y0, z0, plate, y1 - y0, bar)                 // front bar
                    .addBox(x, y0, z1 - bar, plate, y1 - y0, bar)           // rear bar
                    // the shelf: the bottom bar, carried out under the chest
                    .addBox(side > 0 ? FLANK : -FLANK - CHEST_OUT - rim, y1 - bar, z0,
                            CHEST_OUT + rim, bar, z1 - z0);
        }
        root.addOrReplaceChild("fittings", fittings, PartPose.ZERO);
        return mesh;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       HorseRenderState state, float yRot, float xRot) {
        if (!(state instanceof GeneticHorseRenderState genetic) || state.isInvisible || state.isBaby
                || genetic.harnessLeather == 0) {
            return;     // no harness - see GeneticHorseRenderState
        }
        poseStack.pushPose();
        ModelPart root = this.getParentModel().root();
        root.translateAndRotate(poseStack);
        root.getChild("body").translateAndRotate(poseStack);
        float alpha = genetic.fadeAlpha;
        draw(poseStack, collector, lightCoords, state, this.leather, RiderFade.fade(genetic.harnessLeather, alpha));
        draw(poseStack, collector, lightCoords, state, this.fittings, RiderFade.fade(genetic.harnessMetal, alpha));
        poseStack.popPose();
    }

    private static void draw(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                             HorseRenderState state, ModelPart part, int colour) {
        // The eleven-argument form: ..., sheeted, hasFoil, tintedColor,
        // crumblingOverlay, outlineColor. The braid's lesson - a tint passed in
        // the outline's place draws nothing and logs nothing.
        collector.order(1).submitModelPart(part, poseStack, RenderTypes.entityTranslucent(SHEET),
                lightCoords, OverlayTexture.NO_OVERLAY, null, false, false, colour, null, state.outlineColor);
    }
}
