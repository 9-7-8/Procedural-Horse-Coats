package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The procedural-coat horse model: vanilla's equine geometry, but with a
 * <b>128x128, fully non-mirrored</b> UV layout so the coat engine can paint
 * every body part - left vs right, and each of the four legs - independently.
 *
 * <p><b>How the 2x resolution works.</b> A baked {@link ModelPart.Cube} maps
 * each face to a texel rectangle whose size is the box dimension (in model
 * units) and whose origin is {@code texOffs}; those texel coords are then
 * divided by the "texture size" the layer was baked with. Vanilla bakes the
 * horse layer at 64x64, so a 4-unit-wide leg face samples 4/64 of the sheet.
 * We keep the exact same {@code texOffs} / box numbers as vanilla (so this
 * file reads as a near-copy of {@link AbstractEquineModel#createBodyMesh}) but
 * pass a per-cube {@code texScale} of {@link #HD} = 0.5 while baking the layer
 * at 128x128. {@code CubeDefinition.bake} computes the effective texture size
 * as {@code 128 * 0.5 = 64}, so every normalized UV comes out identical to
 * vanilla - which means our sheet is simply the vanilla sheet scaled 2x, and
 * every face now covers twice as many real texels. See
 * {@code GeneticCoatTextureFactory} / the asset in
 * {@code common/.../assets/horsegenetics/textures/entity/horse/horse_white.png}.
 *
 * <p><b>Non-mirrored legs / ears.</b> Vanilla points all four legs at
 * {@code texOffs(48, 21)} (left legs additionally {@code .mirror()}), and both
 * ears at {@code texOffs(19, 16)}. Here each gets its own patch of the sheet,
 * placed in what was empty space on the vanilla layout (all values below are
 * vanilla 64-space numbers; on the 128px sheet they land at 2x):
 * <pre>
 *   right_hind_leg  texOffs(48, 21)  (unchanged - vanilla's leg patch)
 *   right_front_leg texOffs(48,  0)
 *   left_hind_leg   texOffs(26,  0)
 *   left_front_leg  texOffs(26, 16)
 *   right_ear       texOffs(19, 16)  (unchanged - vanilla's ear patch)
 *   left_ear        texOffs(19,  0)
 * </pre>
 * A leg unwrap is 16x15 (32x30 on the HD sheet); an ear unwrap is 6x4
 * (12x8 HD). The texture generator seeds the three new leg patches and the
 * one new ear patch by copying vanilla's shared patch, so a stock white horse
 * looks unchanged until the coat engine starts writing per-part detail.
 *
 * <p>Extends {@link HorseModel} (not {@code AbstractEquineModel} directly) so
 * it drops into {@code AbstractHorseRenderer<Horse, HorseRenderState,
 * HorseModel>} with no generics juggling; the superclass constructor just
 * pulls the same child part names out of the baked root that vanilla uses,
 * and all of them are present below.
 */
public final class HdHorseModel extends HorseModel {

    /** Per-cube texture-coordinate scale - see the class javadoc. */
    private static final float HD = 0.5F;

    /** Sheet the {@link #createHdLayer() HD layer} is baked against. */
    public static final int TEX_SIZE = 128;

    public HdHorseModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createHdLayer() {
        return LayerDefinition.create(createHdBodyMesh(CubeDeformation.NONE), TEX_SIZE, TEX_SIZE);
    }

    /**
     * A structural copy of {@link AbstractEquineModel#createBodyMesh} - same
     * part names, offsets, rotations and box geometry - differing only in the
     * per-cube {@code texScale} argument ({@link #HD}) and the four leg / two
     * ear {@code texOffs} values (independent patches, no {@code .mirror()}).
     */
    public static MeshDefinition createHdBodyMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.05F), HD, HD),
            PartPose.offset(0.0F, 11.0F, 5.0F)
        );
        PartDefinition headParts = root.addOrReplaceChild(
            "head_parts",
            CubeListBuilder.create()
                .texOffs(0, 35)
                .addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F, CubeDeformation.NONE, HD, HD),
            PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
        );
        PartDefinition head = headParts.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, g, HD, HD),
            PartPose.ZERO
        );
        headParts.addOrReplaceChild(
            "mane",
            CubeListBuilder.create().texOffs(56, 36).addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, g, HD, HD),
            PartPose.ZERO
        );
        headParts.addOrReplaceChild(
            "upper_mouth",
            CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, g, HD, HD),
            PartPose.ZERO
        );

        // --- legs: one independent, non-mirrored patch each ---
        root.addOrReplaceChild(
            "left_hind_leg",
            CubeListBuilder.create().texOffs(26, 0).addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, g, HD, HD),
            PartPose.offset(4.0F, 14.0F, 7.0F)
        );
        root.addOrReplaceChild(
            "right_hind_leg",
            CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, g, HD, HD),
            PartPose.offset(-4.0F, 14.0F, 7.0F)
        );
        root.addOrReplaceChild(
            "left_front_leg",
            CubeListBuilder.create().texOffs(26, 16).addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, g, HD, HD),
            PartPose.offset(4.0F, 14.0F, -10.0F)
        );
        root.addOrReplaceChild(
            "right_front_leg",
            CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, g, HD, HD),
            PartPose.offset(-4.0F, 14.0F, -10.0F)
        );

        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(42, 36).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F, g, HD, HD),
            PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
        );

        // --- ears: independent patches (vanilla shares texOffs(19, 16)) ---
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(19, 0).addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F), HD, HD),
            PartPose.ZERO
        );
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(19, 16).addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F), HD, HD),
            PartPose.ZERO
        );

        return mesh;
    }
}
