/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public class CartItemUtil {

    public static boolean isHumanoidArmor(ItemStack stack) {
        return isHumanoidArmor(stack.getItem());
    }

    public static boolean isHumanoidArmor(Item item) {
        if (item.components().has(DataComponents.EQUIPPABLE)) {
            Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
            assert equippable != null;
            return equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
        }
        return false;
    }

    public static boolean isTool(ItemStack itemStack) {
        return isTool(itemStack.getItem());
    }

    public static boolean isTool(Item item) {
        return item.components().has(DataComponents.TOOL);
    }

}
