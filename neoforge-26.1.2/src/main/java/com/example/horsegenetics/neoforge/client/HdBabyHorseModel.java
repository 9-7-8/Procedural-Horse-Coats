package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The foal equivalent of {@link HdHorseModel}: vanilla {@code BabyHorseModel}
 * geometry, baked at 128x128 with a per-cube {@code texScale} of {@link #HD} =
 * 0.5, so every normalized UV stays identical to vanilla and
 * {@code horse_white_baby.png} (the vanilla 64px baby sheet scaled 2x) lines
 * up. Legs and ears are <b>not</b> re-`texOffs`'d here (the baby model already
 * points each leg at a distinct patch), so this is a straight 2x-resolution
 * pass - the coat overlay still gets per-leg control via
 * {@code HorseSkinGeometry.Skin.BABY}.
 */
public final class HdBabyHorseModel extends BabyHorseModel {

    private static final float HD = 0.5F;
    public static final int TEX_SIZE = 128;

    public HdBabyHorseModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createHdLayer() {
        return LayerDefinition.create(createHdBabyMesh(CubeDeformation.NONE), TEX_SIZE, TEX_SIZE);
    }

    private static MeshDefinition createHdBabyMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 13).addBox(-4.0F, -3.5F, -7.0F, 8.0F, 7.0F, 14.0F, g, HD, HD),
            PartPose.offset(0.0F, 12.5F, 0.0F));
        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(24, 34).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 8.0F, g, HD, HD),
            PartPose.offsetAndRotation(0.0F, -1.0F, 7.0F, -0.7418F, 0.0F, 0.0F));

        root.addOrReplaceChild(
            "left_hind_leg",
            CubeListBuilder.create().texOffs(12, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g, HD, HD),
            PartPose.offset(2.4F, 16.0F, 5.4F));
        root.addOrReplaceChild(
            "right_hind_leg",
            CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g, HD, HD),
            PartPose.offset(-2.4F, 16.0F, 5.4F));
        root.addOrReplaceChild(
            "left_front_leg",
            CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g, HD, HD),
            PartPose.offset(2.4F, 16.0F, -5.4F));
        root.addOrReplaceChild(
            "right_front_leg",
            CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g, HD, HD),
            PartPose.offset(-2.4F, 16.0F, -5.4F));

        PartDefinition neck = root.addOrReplaceChild(
            "head_parts",
            CubeListBuilder.create().texOffs(30, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 8.0F, 4.0F, g, HD, HD),
            PartPose.offsetAndRotation(0.0F, 10.0F, -6.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.9484F, -6.705F, 6.0F, 4.0F, 9.0F, g, HD, HD),
            PartPose.offset(0.0F, -6.0516F, -0.2951F));
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, -2.5F, -0.8F, 2.0F, 3.0F, 1.0F, g, HD, HD),
            PartPose.offsetAndRotation(2.0F, -4.2484F, 1.9451F, 0.0F, 0.0F, 0.2618F));
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 3.0F, 1.0F, g, HD, HD),
            PartPose.offsetAndRotation(-2.0F, -4.2484F, 1.645F, 0.0F, 0.0F, -0.2618F));

        return mesh;
    }
}
