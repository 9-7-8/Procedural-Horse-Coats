/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CartBannerFlagModel extends BannerFlagModel {
    public CartBannerFlagModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(@NotNull Float sway) {
        super.setupAnim(sway);
        this.flag.xRot = (0.01F * Mth.cos(Mth.TWO_PI * sway)) * Mth.PI;
    }
}
