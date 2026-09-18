/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class CartsModelLayers {
    public static final ModelLayerLocation ANIMAL_CART = main("animal_cart");
    public static final ModelLayerLocation PLOW = main("plow");
    public static final ModelLayerLocation SUPPLY_CART = main("supply_cart");
    public static final ModelLayerLocation HAND_CART = main("hand_cart");
    public static final ModelLayerLocation SEED_DRILL = main("seed_drill");
    public static final ModelLayerLocation REAPER = main("reaper");
    public static final ModelLayerLocation WAGON = main("wagon");
    public static final ModelLayerLocation WAGON_ROOF = main("wagon_roof");
    public static final ModelLayerLocation WAGON_CHEST = main("wagon_chest");

    @SuppressWarnings("ConfusingMainMethod")
    private static ModelLayerLocation main(String name) {
        return layer(name, "main");
    }

    @SuppressWarnings("SameParameterValue")
    private static ModelLayerLocation layer(String name, String layer) {
        return new ModelLayerLocation(HorseCarts.resLoc(name), layer);
    }
}
