/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CargoCartRenderState extends CartRenderState {
    public NonNullList<@NotNull ItemStack> cargo;
    public NonNullList<@NotNull ItemStackRenderState> cargoStates;
    public long rngSeed;
    public Level level;
    public boolean extraWheel;
    public boolean flowerBasket;
    public ArmorStandRenderState armorRenderState;
    public ArmorStandRenderState armorRenderState2;
}
