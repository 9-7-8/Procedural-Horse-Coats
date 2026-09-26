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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SupplyCartEntity extends AbstractCargoCart {

    public SupplyCartEntity(EntityType<? extends @NotNull Entity> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn, 54);
    }

    /**
     * How far past the cart's own body a drop is picked up from, in blocks.
     * Wide enough to sweep the swath the reaper cuts to one side, narrow enough
     * that the cart does not rob a chest you emptied onto the floor beside it.
     */
    private static final double GLEAN_RADIUS = 2.0D;

    /**
     * <b>Gathers what the reaper in front of it has just cut.</b>
     *
     * <p>The reaper has no inventory - everything it cuts falls on the ground
     * where it fell, so harvesting a field meant walking it twice. Hitch a
     * supply cart behind the reaper and the second walk is the cart's job.
     *
     * <p><b>Only behind a reaper</b>, deliberately. A cart that hoovered up
     * every dropped item wherever it went would be a different and much worse
     * item: you could not put a supply cart down next to your stuff, and every
     * death near one would feed it your inventory. The pickup is a property of
     * the <i>combination</i> - reaper in front, cart behind - and not of the
     * cart, so it is tested here on {@link #getPulling()} rather than granted to
     * the cart at any other time. It also means the cart directly behind the
     * reaper is the one that gleans; a third vehicle further down the train is
     * being pulled by the cart, not by the reaper, and does nothing.
     */
    @Override
    public void pulledTick() {
        super.pulledTick();
        if (!this.level().isClientSide() && this.getPulling() instanceof ReaperCartEntity) {
            this.glean();
        }
    }

    private void glean() {
        for (final ItemEntity drop : this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(GLEAN_RADIUS),
                item -> item.isAlive() && !item.hasPickUpDelay())) {
            final ItemStack left = this.stow(drop.getItem());
            if (left.isEmpty()) {
                drop.discard();
            } else if (left.getCount() != drop.getItem().getCount()) {
                drop.setItem(left);
            }
        }
    }

    /**
     * Push {@code stack} into the cart, topping up part-stacks before opening a
     * new slot, and hand back whatever would not fit. A full cart simply leaves
     * the drop lying there, which is the readable failure: you come back to a
     * cart that is full and a field that is not clear.
     */
    private ItemStack stow(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < this.getContainerSize() && !remaining.isEmpty(); slot++) {
            final ItemStack held = this.getItem(slot);
            if (held.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(held, remaining)) {
                final int room = Math.min(held.getMaxStackSize(), this.getMaxStackSize()) - held.getCount();
                final int moved = Math.min(room, remaining.getCount());
                if (moved > 0) {
                    held.grow(moved);
                    this.setItem(slot, held);
                    remaining.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < this.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (this.getItem(slot).isEmpty()) {
                this.setItem(slot, remaining);
                remaining = ItemStack.EMPTY;
            }
        }
        return remaining;
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