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
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.AnimalCartModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.CartBannerFlagModel;
import com.example.horsegenetics.neoforge.carts.entity.AnimalCartEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class AnimalCartRenderer extends DrawnRenderer<AnimalCartEntity, CartRenderState, AnimalCartModel> {

    public AnimalCartRenderer(final EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnimalCartModel(
                renderManager.bakeLayer(CartsModelLayers.ANIMAL_CART),
                new BannerModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER)),
                new CartBannerFlagModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER_FLAG)))
        );
        this.shadowRadius = 1.0F;
    }

    @Override
    public @NotNull CartRenderState createRenderState() {
        return new CartRenderState();
    }

    @Override
    protected void submitContents(CartRenderState state, PoseStack stack, SubmitNodeCollector submitNodeCollector) {
        if (state.bannerColor != null) {
            stack.pushPose();
            this.model.getBody().translateAndRotate(stack);
            stack.translate(0.0D, -0.6D, 1.56D);
            this.submitBanner(state, stack, submitNodeCollector);
            stack.popPose();
        }
    }

    @Override
    public Identifier getTextureLocation(CartRenderState state) {
        return HorseCarts.resLoc("textures/entity/" + state.woodType.id() + "_animal_cart.png");
    }
}
