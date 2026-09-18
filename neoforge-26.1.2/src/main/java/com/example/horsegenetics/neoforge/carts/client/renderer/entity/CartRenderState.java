/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import com.example.horsegenetics.neoforge.carts.CartWood;

public class CartRenderState extends EntityRenderState {
    public float pitch;
    public float yaw;
    public double wheelRotation0;
    public double wheelRotationInc0;
    public double wheelRotation1;
    public double wheelRotationInc1;
    public float timeSinceHit;
    public float delta;
    public float damage;
    public int forward;
    public DyeColor bannerColor;
    public BannerPatternLayers bannerPattern;
    public CartWood woodType;
}
