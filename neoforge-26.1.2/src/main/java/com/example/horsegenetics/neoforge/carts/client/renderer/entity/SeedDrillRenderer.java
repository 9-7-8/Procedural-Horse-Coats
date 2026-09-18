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
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.client.renderer.CartsModelLayers;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.CartBannerFlagModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.SeedDrillModel;
import com.example.horsegenetics.neoforge.carts.entity.SeedDrillEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

public class SeedDrillRenderer extends DrawnRenderer<SeedDrillEntity, SeedDrillRenderState, SeedDrillModel> {

    private final Random random = new Random();
    private final ItemModelResolver itemRenderer;

    public SeedDrillRenderer(final EntityRendererProvider.Context renderManager) {
        super(renderManager, new SeedDrillModel(renderManager.bakeLayer(CartsModelLayers.SEED_DRILL),
                new BannerModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER)),
                new CartBannerFlagModel(renderManager.bakeLayer(ModelLayers.STANDING_BANNER_FLAG))));
        this.shadowRadius = 1.0F;
        this.itemRenderer = renderManager.getItemModelResolver();
    }

    @Override
    public @NotNull SeedDrillRenderState createRenderState() {
        return new SeedDrillRenderState();
    }

    @Override
    public void extractRenderState(SeedDrillEntity entity, SeedDrillRenderState state, float delta) {
        super.extractRenderState(entity, state, delta);
        state.level = entity.level();
        state.seeds = NonNullList.create();
        state.itemStates = NonNullList.create();
        for (int i = 0; i < entity.getItemStacks().size(); i++) {
            state.seeds.add(i, entity.getStackInSlot(i));
            ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
            this.itemRenderer.updateForNonLiving(itemStackRenderState, entity.getStackInSlot(i), ItemDisplayContext.FIXED, entity);
            state.itemStates.add(i, itemStackRenderState);
        }
    }

    @Override
    public @NotNull Identifier getTextureLocation(final SeedDrillRenderState state) {
        return HorseCarts.resLoc("textures/entity/" + state.woodType.id() + "_seed_drill.png");
    }

    @Override
    protected void submitContents(SeedDrillRenderState state, final PoseStack stack, SubmitNodeCollector collector) {
        for (int i = 0; i < state.seeds.size(); i++) {
            final ItemStack itemStack = state.seeds.get(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            int finalI = i;
            random.setSeed(32L * i + Objects.hashCode(BuiltInRegistries.ITEM.getKey(itemStack.getItem())));
            for (int j = 0; j < itemStack.getCount() / 4; j++) {
                int finalJ = j;
                this.attach(this.model.getBody(), this.model.getBody(), s -> {
                    float f = (float) finalI - 4.5f;
                    s.translate(f / 6.5 + 1f / 16,-2f/16 - (float) finalJ / 32 + (finalI % 4) * 0.001, random.nextFloat(3f / 8) - 3f / 16);
                    s.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    s.scale(0.6f, 0.6f, 0.6f);
                    s.mulPose(Axis.ZP.rotation(random.nextFloat(2 * Mth.PI)));

                    state.itemStates.get(finalI).submit(stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                }, stack);
            }
        }
    }
}