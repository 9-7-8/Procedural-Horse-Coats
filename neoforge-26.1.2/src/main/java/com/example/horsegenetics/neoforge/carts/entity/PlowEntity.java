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

import com.google.common.collect.ImmutableList;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.CartsConfig;
import com.example.horsegenetics.neoforge.carts.container.PlowMenu;
import com.example.horsegenetics.neoforge.carts.util.CartItemUtil;
import com.example.horsegenetics.neoforge.carts.util.ProxyItemUseContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PlowEntity extends AbstractDrawnInventoryEntity {
    private static final int SLOT_COUNT = 3;
    private static final double BLADEOFFSET = 1.7D;
    private static final EntityDataAccessor<@NotNull Boolean> PLOWING = SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ImmutableList<@NotNull EntityDataAccessor<@NotNull ItemStack>> TOOLS = ImmutableList.of(
            SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.ITEM_STACK));

    public PlowEntity(final EntityType<? extends @NotNull Entity> entityTypeIn, final Level worldIn) {
        super(entityTypeIn, worldIn, SLOT_COUNT);
    }

    @Override
    protected double getSpacing() {
        return 1.3;
    }

    @Override
    public CartsConfig.CartConfig getConfig() {
        return CartsConfig.get().of(CartKind.PLOW);
    }

    public boolean getPlowing() {
        return this.entityData.get(PLOWING);
    }

    @Override
    public void pulledTick() {
        super.pulledTick();
        if (this.getPulling() == null) {
            return;
        }
        if (!this.level().isClientSide()) {
            Optional<Player> playerOptional = getControllingPlayer();
            if (getPlowing() && playerOptional.isPresent()) {
                if (this.xo != this.getX() || this.zo != this.getZ()) {
                    this.plow((ServerPlayer) playerOptional.get());
                }
            }
        }
    }

    private void plow(final ServerPlayer player) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            final ItemStack stack = this.getStackInSlot(i);
            if (CartItemUtil.isTool(stack)) {
                final float offset = 38.0F - i * 38.0F;
                final double blockPosX = this.getX() + Mth.sin((float) Math.toRadians(this.getYRot() - offset)) * BLADEOFFSET;
                final double blockPosZ = this.getZ() - Mth.cos((float) Math.toRadians(this.getYRot() - offset)) * BLADEOFFSET;
                final BlockPos blockPos = new BlockPos((int) blockPosX, (int) Math.round(this.getY() - 0.75D), (int) blockPosZ);
                final boolean damageable = stack.isDamageableItem();
                final int count = stack.getCount();
                tryBreakBlock(stack, blockPos.above(), level(), player);
                InteractionResult result = stack.getItem().useOn(new ProxyItemUseContext(player, stack, new BlockHitResult(Vec3.ZERO, Direction.UP, blockPos, false)));
                if (damageable && stack.getCount() < count) {
                    this.playSound(SoundEvents.ITEM_BREAK.value(), 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
                    this.updateSlot(i);
                }
            }
        }
    }

    private void tryBreakBlock(ItemStack stack, BlockPos pos, Level level, Player player) {
        BlockState state = level.getBlockState(pos);
        TagKey<@NotNull Block> tag;
        switch (stack.getItem()) {
            case HoeItem ignored -> tag = HorseCarts.PLOW_BREAKABLE_HOE;
            case ShovelItem ignored -> tag = HorseCarts.PLOW_BREAKABLE_SHOVEL;
            case AxeItem ignored -> tag = HorseCarts.PLOW_BREAKABLE_AXE;
            default -> {
                return;
            }
        }
        if (state.isAir()) return;
        if (state.is(tag)) {
            if (level.removeBlock(pos, false)) {
                level.destroyBlock(pos, false);
                if (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)) {
                    Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, stack);
                }
            }
        }
    }

    @Override
    protected AbstractContainerMenu createMenuLootUnpacked(int i, Inventory inventory, Player player) {
        return new PlowMenu(i, inventory, this);
    }

    @Override
    protected void onContentsChanged(int slot) {
        updateSlot(slot);
    }

    public void updateSlot(final int slot) {
        if (!this.level().isClientSide()) {
            if (this.getItemStacks().get(slot).isEmpty()) {
                this.entityData.set(TOOLS.get(slot), ItemStack.EMPTY);
            } else {
                this.entityData.set(TOOLS.get(slot), this.getItemStacks().get(slot));
            }

        }
    }

    public ItemStack getStackInSlot(final int i) {
        return this.entityData.get(TOOLS.get(i));
    }

    @Override
    public Item getCartItem() {
        return HorseCarts.item(CartKind.PLOW, getWoodType());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLOWING, false);
        for (final EntityDataAccessor<@NotNull ItemStack> param : TOOLS) {
            builder.define(param, ItemStack.EMPTY);
        }
    }

    @Override
    protected InteractionResult onInteractNotOpen(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            this.entityData.set(PLOWING, !this.entityData.get(PLOWING));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Plowing", this.entityData.get(PLOWING));
    }

    @Override
    protected void saveInventory(ValueOutput output) {
        ContainerHelper.saveAllItems(output, this.getItemStacks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PLOWING, input.getBooleanOr("Plowing", false));
    }

    @Override
    protected void readInventory(ValueInput input) {
        ContainerHelper.loadAllItems(input, this.getItemStacks());
    }

}