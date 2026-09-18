/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity;

import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.CartBannerFlagModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.SupplyCartEntity;
import com.example.horsegenetics.neoforge.carts.client.renderer.CartsModelLayers;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.SupplyCartModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class SupplyCartRenderer extends CargoCartRenderer<SupplyCartEntity, SupplyCartModel> {

    public SupplyCartRenderer(final EntityRendererProvider.Context ctx) {
        super(ctx, new SupplyCartModel(ctx.bakeLayer(CartsModelLayers.SUPPLY_CART),
                new BannerModel(ctx.bakeLayer(ModelLayers.STANDING_BANNER)),
                new CartBannerFlagModel(ctx.bakeLayer(ModelLayers.STANDING_BANNER_FLAG))));
    }

    @Override
    protected double getFlowerOffsetZ() {
        return -3.0D / 16.0;
    }

    @Override
    protected Vec3 getWheelOffset() {
        return new Vec3(1.18D, 0.1D, -0.15D);
    }

    @Override
    protected Vec3 getPaintingOffset(int i, int n, int count) {
        return new Vec3(0.0D, 0.5 * (n - (count - 1) * 0.1D) / count, -1.0D / 16.0D * i);
    }

    @Override
    protected float getPaintingAngleFactor() {
        return 1;
    }

    @Override
    protected double getSuppliesOffsetX(int x) {
        return (x - 0.5D) * 11.0D / 16.0D;
    }

    @Override
    protected double getSuppliesOffsetZ(int z) {
        return (z * 11.0D - 9.0D) / 16.0D;
    }

    @Override
    protected float getBlockSize() {
        return 0.65F;
    }

    @Override
    protected float getArmorSize() {
        return 1f;
    }

    @Override
    protected double getShieldOffsetY() {
        return 1.2;
    }

    @Override
    protected float getItemSize() {
        return 0.7f;
    }

    @Override
    public @NotNull Identifier getTextureLocation(CargoCartRenderState state) {
        return HorseCarts.resLoc("textures/entity/" + state.woodType.id() + "_supply_cart.png");
    }
}
