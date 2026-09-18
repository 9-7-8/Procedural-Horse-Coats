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
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.client.renderer.CartsModelLayers;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.CartBannerFlagModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.WagonChestModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.WagonModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.WagonRoofModel;
import com.example.horsegenetics.neoforge.carts.entity.WagonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class WagonRenderer extends DrawnRenderer<WagonEntity, WagonRenderState, WagonModel> {

    public WagonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WagonModel(
                renderManager.bakeLayer(CartsModelLayers.WAGON),
                new WagonRoofModel(renderManager.bakeLayer(CartsModelLayers.WAGON_ROOF)),
                new WagonChestModel(renderManager.bakeLayer(CartsModelLayers.WAGON_CHEST)),
                new BannerModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER)),
                new CartBannerFlagModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER_FLAG))
        ));
    }

    @Override
    public void extractRenderState(WagonEntity entity, WagonRenderState state, float delta) {
        super.extractRenderState(entity, state, delta);
        state.maxChestCount = entity.getMaxChestCount();
        state.chestCount = entity.getChestCount();
        state.hasRoof = entity.hasRoof();
        state.unfurled = entity.getUnfurled();
        state.roofTexture = entity.getRoofTexture();
    }

    @Override
    protected void submitContents(WagonRenderState state, PoseStack stack, SubmitNodeCollector collector) {
        stack.pushPose();
        this.model.getBody().translateAndRotate(stack);
        collector.submitModel(this.model.getChestModel(), state, stack, this.model.renderType(HorseCarts.resLoc("textures/entity/wagon_chest.png")), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        collector.submitModel(this.model.getRoofModel(), state, stack, this.model.renderType(state.roofTexture), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (state.bannerColor != null) {
            stack.pushPose();
            stack.translate(0.0D, -0.58D, 2.62D);
            this.submitBanner(state, stack, collector);
            stack.popPose();
        }
        stack.popPose();
    }

    @Override
    public @NotNull WagonRenderState createRenderState() {
        return new WagonRenderState();
    }

    @Override
    public @NotNull Identifier getTextureLocation(WagonRenderState state) {
        return HorseCarts.resLoc("textures/entity/" + state.woodType.id() + "_wagon.png");
    }
}