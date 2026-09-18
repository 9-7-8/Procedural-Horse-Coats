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
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.ReaperModel;
import com.example.horsegenetics.neoforge.carts.entity.ReaperCartEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class ReaperRenderer extends DrawnRenderer<ReaperCartEntity, ReaperRenderState, ReaperModel> {

    public ReaperRenderer(final EntityRendererProvider.Context renderManager) {
        super(renderManager, new ReaperModel(renderManager.bakeLayer(CartsModelLayers.REAPER),
                new BannerModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER)),
                new CartBannerFlagModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER_FLAG))));
        this.shadowRadius = 1.0F;
    }

    @Override
    public void extractRenderState(ReaperCartEntity entity, ReaperRenderState state, float delta) {
        super.extractRenderState(entity, state, delta);
        state.folded = entity.isFolded();
    }

    @Override
    public @NotNull ReaperRenderState createRenderState() {
        return new ReaperRenderState();
    }

    @Override
    public @NotNull Identifier getTextureLocation(ReaperRenderState state) {
        return HorseCarts.resLoc("textures/entity/" + state.woodType.id() + "_reaper.png");
    }

    @Override
    protected void submitContents(ReaperRenderState state, PoseStack stack, SubmitNodeCollector collector) {}
}