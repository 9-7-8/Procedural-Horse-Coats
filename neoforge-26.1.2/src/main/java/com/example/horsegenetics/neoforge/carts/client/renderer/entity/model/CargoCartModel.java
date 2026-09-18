/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity.model;

import com.example.horsegenetics.neoforge.carts.client.renderer.entity.CargoCartRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;

public abstract class CargoCartModel<T extends CargoCartRenderState> extends CartModel<T> {

    protected final ModelPart flowerBasket;
    protected final ModelPart extraWheel;

    protected CargoCartModel(ModelPart root, BannerModel bannerModel, BannerFlagModel flagModel) {
        super(root, bannerModel, flagModel);
        this.extraWheel = root.getChild("body").getChild("extraWheel");
        this.flowerBasket = root.getChild("body").getChild("flowerBasket");
        this.flowerBasket.visible = false;
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);
        this.extraWheel.xRot = 0.9F;
        this.extraWheel.zRot = (float) Math.PI * 0.3F;
        this.extraWheel.visible = false;
        this.extraWheel.visible = state.extraWheel;
        this.flowerBasket.visible = state.flowerBasket;
    }
}
