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
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ReaperCartEntity extends AbstractDrawnEntity {

    private static final EntityDataAccessor<@NotNull Boolean> FOLDED = SynchedEntityData.defineId(ReaperCartEntity.class, EntityDataSerializers.BOOLEAN);

    public ReaperCartEntity(EntityType<? extends @NotNull Entity> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected double getSpacing() {
        return 1.3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FOLDED, true);
    }

    public boolean isFolded() {
        return this.entityData.get(FOLDED);
    }

    @Override
    public void tick() {
        super.tick();
        final Entity coachman = this.getControllingPassenger();
        final Entity pulling = this.getPulling();
        boolean folded = !(pulling != null && coachman != null);
        if (folded != this.entityData.get(FOLDED)) {
            playSound(SoundEvents.WOODEN_TRAPDOOR_CLOSE);
        }
        this.entityData.set(FOLDED, folded);
        // Only spawn the driving Postilion onto a real mount (horse/donkey/mule/camel), never onto a
        // player pulling the cart on foot — that produced a bad passenger state that crashed the game.
        if (pulling instanceof net.minecraft.world.entity.Mob && coachman != null && pulling.getControllingPassenger() == null) {
            final PostilionEntity postilion = HorseCarts.POSTILION_ENTITY.create(this.level(), EntitySpawnReason.SPAWN_ITEM_USE);
            if (postilion != null) {
                postilion.snapTo(pulling.getX(), pulling.getY(), pulling.getZ(), coachman.getYRot(), coachman.getXRot());
                if (postilion.startRiding(pulling)) {
                    this.level().addFreshEntity(postilion);
                } else {
                    postilion.discard();
                }
            }
        }
    }

    public float getPassengersRidingOffsetY(EntityDimensions entityDimensions, float f) {
        //18/16
        return (entityDimensions.height() - 3f / 16f) * f;
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity entity, @NotNull EntityDimensions entityDimensions, float f) {
        final Vec3 forward = this.getLookAngle().scale(-0.45);
        return new Vec3(forward.x, getPassengersRidingOffsetY(entityDimensions, f) + forward.y, forward.z);
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (this.hasPassenger(passenger)) {
            passenger.setYBodyRot(this.getYRot());
            final float f2 = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
            final float f1 = Mth.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.setYRot(passenger.getYRot() + (f1 - f2));
            passenger.setYHeadRot(passenger.getYRot());
        }
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand, @NotNull Vec3 vec) {
        if (isLocked()) return InteractionResult.FAIL;
        if (!this.level().isClientSide()) {
            if (player.isSecondaryUseActive()) {
                player.sendOverlayMessage(Component.translatable("message.horsegenetics.use_reaper"));
            } else if (!player.isSecondaryUseActive() && this.pulling != null && this.pulling != player) {
                if (player.startRiding(this)) {
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void pulledTick() {
        super.pulledTick();
        if (this.getPulling() == null) {
            return;
        }
        if (!this.level().isClientSide()) {
            Optional<Entity> pulling = CartWorld.get(this.level()).getCurrentlyPulling(this);
            if (pulling.isPresent() && this.getFirstPassenger() instanceof ServerPlayer pl) {
                if (this.xo != this.getX() || this.zo != this.getZ()) {
                    this.harvest(pl);
                }
            }
        }
    }

    private void harvest(ServerPlayer player) {
        for (int i = 0; i <= 12; i += 2) {
            float f = 1.1f + ((float) i / 10f);
            final double x = this.getX() + Mth.sin((float) Math.toRadians(this.getYRot() + 90)) * f;
            final double z = this.getZ() - Mth.cos((float) Math.toRadians(this.getYRot() + 90)) * f;
            final BlockPos blockPos = new BlockPos((int) Math.round(x - 0.5), (int) Math.round(this.getY() - 0.75D), (int) Math.round(z - 0.5));
            BlockPos pos = blockPos.above();
            BlockState state = level().getBlockState(pos);
            if (state.is(HorseCarts.REAPER_HARVESTABLE)) {
                if (level().removeBlock(pos, false)) {
                    level().destroyBlock(pos, false);
                    if (!state.requiresCorrectToolForDrops()) {
                        Block.dropResources(state, level(), pos, level().getBlockEntity(pos), player, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    @Override
    public Item getCartItem() {
        return HorseCarts.item(CartKind.REAPER, getWoodType());
    }

    @Override
    public CartsConfig.CartConfig getConfig() {
        return CartsConfig.get().of(CartKind.REAPER);
    }
}