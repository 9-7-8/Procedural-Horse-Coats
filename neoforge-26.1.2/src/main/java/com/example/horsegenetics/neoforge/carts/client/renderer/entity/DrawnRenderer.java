/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.example.horsegenetics.neoforge.mixin.CartModelPartMixin;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.CartModel;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public abstract class DrawnRenderer<T extends AbstractDrawnEntity, S extends CartRenderState, M extends CartModel<@NotNull S>> extends EntityRenderer<@NotNull T, @NotNull S> {
    protected M model;

    private final ModelPart pole;
    private final ModelPart bar;

    private final BannerFlagModel flagModel;

    private final SpriteGetter materials;

    protected DrawnRenderer(final EntityRendererProvider.Context renderManager, final M model) {
        super(renderManager);
        this.model = model;
        this.materials = renderManager.getSprites();

        this.flagModel = model.getFlagModel();
        BannerModel bannerModel = model.getBannerModel();
        this.pole = bannerModel.root().getChild("pole");
        this.bar = bannerModel.root().getChild("bar");
    }

    @Override
    protected @NotNull AABB getBoundingBoxForCulling(@NotNull T minecraft) {
        return super.getBoundingBoxForCulling(minecraft).inflate(0.5);
    }

    @Override
    public abstract @NotNull S createRenderState();

    @Override
    public void extractRenderState(T entity, S state, float delta) {
        super.extractRenderState(entity, state, delta);
        AbstractDrawnEntity.RenderInfo info = entity.getInfo(delta);
        state.pitch = info.getPitch();
        state.yaw = info.getYaw();
        state.wheelRotation0 = entity.getWheelRotation(0);
        state.wheelRotation1 = entity.getWheelRotation(1);
        state.wheelRotationInc0 = entity.getWheelRotationIncrement(0);
        state.wheelRotationInc1 = entity.getWheelRotationIncrement(1);
        state.timeSinceHit = entity.getTimeSinceHit();
        state.damage = entity.getDamageTaken();
        state.forward = entity.getForwardDirection();
        state.bannerColor = entity.getBannerColor();
        state.bannerPattern = entity.getBannerPattern();
        state.woodType = entity.getWoodType();
    }

    public abstract Identifier getTextureLocation(S state);

    @Override
    public void submit(S state, PoseStack stack, SubmitNodeCollector collector, @NotNull CameraRenderState cameraState) {
        stack.pushPose();
        this.setupRotation(state, stack);

        this.model.setupAnim(state);
        collector.submitModel(this.model, state, stack, this.model.renderType(this.getTextureLocation(state)), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        this.submitContents(state, stack, collector);

        stack.popPose();
    }

    protected abstract void submitContents(S state, final PoseStack stack, final SubmitNodeCollector submitNodeCollector);

    public void setupRotation(S state, final PoseStack stack) {
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        final float time = state.timeSinceHit - state.delta;
        if (time > 0.0F) {
            final double center = 1.2D;
            stack.translate(0.0D, center, 0.0D);
            final float damage = Math.max(state.damage - state.delta, 0.0F);
            final float angle = Mth.sin(time) * time * damage / 60.0F;
            stack.mulPose(Axis.ZP.rotationDegrees(angle * state.forward));
            stack.translate(0.0D, -center, 0.0D);
            stack.translate(0.0D, angle / 32.0F, 0.0D);
        }
        stack.scale(-1.0F, -1.0F, 1.0F);
    }

    protected void submitBanner(S state, final PoseStack stack, final SubmitNodeCollector submitNodeCollector) {
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(90.0F));
        final float scale = 2.0F / 3.0F;
        stack.scale(scale, scale, scale);
        var material = Sheets.BANNER_BASE;
        submitNodeCollector.submitModelPart(pole, stack, material.renderType(RenderTypes::entitySolid), state.lightCoords, OverlayTexture.NO_OVERLAY, materials.get(material));
        submitNodeCollector.submitModelPart(bar, stack, material.renderType(RenderTypes::entitySolid), state.lightCoords, OverlayTexture.NO_OVERLAY, materials.get(material));
        float k = ((float)Math.floorMod((int) ((state.x * 7 + state.y * 9 + state.z * 13) + state.ageInTicks), 100) + state.delta) / 100.0F;
        stack.translate(-4 / 16f, 18 / 16f, 1.5f / 16f);
        BannerRenderer.submitPatterns(materials, stack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, this.flagModel, k, true, state.bannerColor, state.bannerPattern, null);
        stack.popPose();
    }

    protected void attach(final ModelPart bone, final ModelPart attachment, final Consumer<PoseStack> function, final PoseStack stack) {
        stack.pushPose();
        bone.translateAndRotate(stack);
        if (bone == attachment) {
            function.accept(stack);
        } else {
            final Map<String, ModelPart> childModels;
            childModels = ((CartModelPartMixin) ((Object) bone)).getChildren();
            for (final ModelPart child : childModels.values()) {
                this.attach(child, attachment, function, stack);
            }
        }
        stack.popPose();
    }
}
