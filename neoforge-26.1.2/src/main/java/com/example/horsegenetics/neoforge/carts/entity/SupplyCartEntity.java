/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.entity;

import com.example.horsegenetics.common.cart.CartKind;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.CartsConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SupplyCartEntity extends AbstractCargoCart {

    public SupplyCartEntity(EntityType<? extends @NotNull Entity> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn, 54);
    }

    @Override
    public Item getCartItem() {
        return HorseCarts.item(CartKind.SUPPLY_CART, getWoodType());
    }

    @Override
    public CartsConfig.CartConfig getConfig() {
        return CartsConfig.get().of(CartKind.SUPPLY_CART);
    }

    public float getPassengersRidingOffsetY(EntityDimensions entityDimensions, float f) {
        return (entityDimensions.height() - 9f / 16f) * f;
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity entity, @NotNull EntityDimensions entityDimensions, float f) {
        final Vec3 forward = this.getLookAngle().scale(-0.68);
        return new Vec3(forward.x, getPassengersRidingOffsetY(entityDimensions, f) + forward.y, forward.z);
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (this.hasPassenger(passenger)) {
            passenger.setYBodyRot(this.getYRot() + 180.0F);
            final float f2 = Mth.wrapDegrees(passenger.getYRot() - this.getYRot() + 180.0F);
            final float f1 = Mth.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.setYRot(passenger.getYRot() + (f1 - f2));
            passenger.setYHeadRot(passenger.getYRot());
        }
    }

    @Override
    protected InteractionResult onInteractNotOpen(Player player, InteractionHand hand) {
        final InteractionResult bannerResult = this.useBanner(player, hand);
        if (bannerResult.consumesAction()) {
            return bannerResult;
        }
        if (this.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected AbstractContainerMenu createMenuLootUnpacked(int i, Inventory inventory, Player player) {
        return ChestMenu.sixRows(i, inventory, this);
    }
}