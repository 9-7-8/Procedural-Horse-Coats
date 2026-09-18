/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CartInventory extends NonNullList<@NotNull ItemStack> {

    private Consumer<Integer> onContentsChanged;

    public static CartInventory create() {
        return new CartInventory(new ArrayList<>(), null);
    }

    public static CartInventory createWithCapacity(int i) {
        return new CartInventory(new ArrayList<>(i), null);
    }

    public static CartInventory withSize(int i, ItemStack object) {
        if (object == null) {
            object = ItemStack.EMPTY;
        }
        List<ItemStack> list = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            list.add(object);
        }
        return new CartInventory(list, object);
    }

    public CartInventory(List<ItemStack> list, @Nullable ItemStack object) {
        super(list, object);
    }

    public void setOnContentsChanged(Consumer<Integer> onContentsChanged) {
        this.onContentsChanged = onContentsChanged;
    }

    @Override
    public @NotNull ItemStack set(int i, ItemStack object) {
        var out = super.set(i, object);
        onContentsChanged.accept(i);
        return out;
    }

    @Override
    public void add(int i, ItemStack object) {
        super.add(i, object);
        onContentsChanged.accept(i);
    }

    @Override
    public ItemStack remove(int i) {
        var out = super.remove(i);
        onContentsChanged.accept(i);
        return out;
    }

    @Override
    public void clear() {
        int size = this.size();
        super.clear();
        for(int i = 0; i < size; ++i) {
            onContentsChanged.accept(i);
        }
    }
}
