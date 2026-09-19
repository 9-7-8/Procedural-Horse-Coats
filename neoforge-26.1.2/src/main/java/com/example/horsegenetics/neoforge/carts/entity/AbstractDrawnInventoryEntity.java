/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.entity;

import com.example.horsegenetics.neoforge.carts.util.CartInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class AbstractDrawnInventoryEntity extends AbstractDrawnEntity implements HasCustomInventoryScreen, ContainerEntity {

    private CartInventory itemStacks;
    private final int containerSize;
    @Nullable
    private ResourceKey<@NotNull LootTable> lootTable;
    private long lootTableSeed;

    public AbstractDrawnInventoryEntity(EntityType<? extends @NotNull Entity> entityTypeIn, Level worldIn, int containerSize) {
        super(entityTypeIn, worldIn);
        this.itemStacks = CartInventory.withSize(containerSize, ItemStack.EMPTY);
        this.containerSize = containerSize;
        this.itemStacks.setOnContentsChanged(this::onContentsChanged);
    }

    /**
     * <b>How full this cart is</b>, by what is in the slots rather than by how
     * many slots are touched: a slot holding one arrow out of sixty-four counts
     * as a sixty-fourth of a slot, not as a full one.
     *
     * <p>That distinction is the difference between "the cart is loaded" and
     * "the cart has been opened". Counting occupied slots would make a horse
     * labour under nine single torches, and would let a player carry fifty-four
     * stacks of gold blocks for the same price. It is the slower of the two
     * calculations and it is only asked once a second, while hitched - see
     * {@code AbstractDrawnEntity.refreshDraught}.
     */
    @Override
    protected double fillLevel() {
        return this.containerSize <= 0 ? 0.0 : this.filledSlots(this.containerSize) / this.containerSize;
    }

    /**
     * How many slots' worth of goods sit in the first {@code limit} slots, as a
     * fraction of a full stack each. Split out because the wagon measures itself
     * against the rows it has actually had chests fitted for, not against the
     * twelve it could hold.
     */
    protected double filledSlots(final int limit) {
        final int end = Math.min(limit, this.getItemStacks().size());
        double filled = 0.0;
        for (int i = 0; i < end; i++) {
            final ItemStack stack = this.getItemStacks().get(i);
            if (!stack.isEmpty()) {
                final int max = Math.max(1, stack.getMaxStackSize());
                filled += Math.min(1.0, stack.getCount() / (double) max);
            }
        }
        return filled;
    }

    public boolean stillValid(@NotNull Player player) {
        return this.isChestVehicleStillValid(player);
    }

    @Override
    public void onDestroyedAndDoDrops(DamageSource source) {
        if (!(this.level() instanceof ServerLevel)) return;
        this.chestVehicleDestroyed(source, (ServerLevel) this.level(), this);
    }

    public void remove(Entity.@NotNull RemovalReason removalReason) {
        if (!this.level().isClientSide() && removalReason.shouldDestroy()) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(removalReason);
    }

    protected abstract InteractionResult onInteractNotOpen(Player player, InteractionHand hand);

    protected boolean canInteractNotOpen() {
        return true;
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand, @NotNull Vec3 vec) {
        if (isLocked()) return InteractionResult.FAIL;
        if (canInteractNotOpen() && this.canAddPassenger(player) && !player.isSecondaryUseActive()) {
            return onInteractNotOpen(player, interactionHand);
        } else {
            InteractionResult interactionResult = this.interactWithContainerVehicle(player);
            if (interactionResult.consumesAction()) {
                this.gameEvent(GameEvent.CONTAINER_OPEN, player);
                if (this.level() instanceof ServerLevel serverLevel) {
                    PiglinAi.angerNearbyPiglins(serverLevel, player, true);
                }
            }

            return interactionResult;
        }
    }

    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (!player.level().isClientSide()) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            if (this.level() instanceof ServerLevel serverLevel) {
                PiglinAi.angerNearbyPiglins(serverLevel, player, true);
            }
        }
    }

    public void clearContent() {
        this.clearChestVehicleContent();
    }

    public int getContainerSize() {
        return containerSize;
    }

    public @NotNull ItemStack getItem(int i) {
        return this.getChestVehicleItem(i);
    }

    public @NotNull ItemStack removeItem(int i, int j) {
        return this.removeChestVehicleItem(i, j);
    }

    public @NotNull ItemStack removeItemNoUpdate(int i) {
        return this.removeChestVehicleItemNoUpdate(i);
    }

    public void setItem(int i, @NotNull ItemStack itemStack) {
        this.setChestVehicleItem(i, itemStack);
    }

    public @NotNull SlotAccess getSlot(int i) {
        return Objects.requireNonNull(this.getChestVehicleSlot(i));
    }

    public void setChanged() {
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (this.lootTable != null && player.isSpectator()) {
            return null;
        } else {
            this.unpackLootTable(inventory.player);
            return createMenuLootUnpacked(i, inventory, player);
        }
    }

    protected abstract AbstractContainerMenu createMenuLootUnpacked(int i, Inventory inventory, Player player);

    public @NotNull NonNullList<@NotNull ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    public void clearItemStacks() {
        this.itemStacks = CartInventory.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.itemStacks.setOnContentsChanged(this::onContentsChanged);
    }

    public void stopOpen(Player player) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(player));
    }

    protected void onContentsChanged(int slot) {}

    public void unpackLootTable(@Nullable Player player) {
        this.unpackChestVehicleLootTable(player);
    }

    @Nullable
    @Override
    public ResourceKey<@NotNull LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<@NotNull LootTable> resourceLocation) {
        this.lootTable = resourceLocation;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long l) {
        this.lootTableSeed = l;
    }


    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        saveInventory(output);
    }

    protected abstract void saveInventory(ValueOutput output);

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        readInventory(input);
    }

    protected abstract void readInventory(ValueInput input);

}
