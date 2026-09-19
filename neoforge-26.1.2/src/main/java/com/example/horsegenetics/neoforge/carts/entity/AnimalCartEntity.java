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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AnimalCartEntity extends AbstractDrawnEntity {
    public AnimalCartEntity(final EntityType<? extends @NotNull Entity> entityTypeIn, final Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public CartsConfig.CartConfig getConfig() {
        return CartsConfig.get().of(CartKind.ANIMAL_CART);
    }

    /**
     * <b>The animal cart's cargo is whoever is sitting in it.</b> It has no
     * inventory, so its two seats are its capacity: one cow is half a load and
     * two is a full one.
     *
     * <p>No attempt to weigh a pig against a sheep. The cart only takes animals
     * under a certain size in the first place ({@link #tick}), so the ones that
     * fit are all roughly a sheep, and inventing a per-species mass would be a
     * table nobody could predict from looking at the cart.
     */
    @Override
    protected double fillLevel() {
        return Math.min(1.0, this.getPassengers().size() / 2.0);
    }

    @Override
    public void tick() {
        super.tick();
        final Entity coachman = this.getControllingPassenger();
        final Entity pulling = this.getPulling();
        // Only spawn the driving Postilion onto a real mount (horse/donkey/mule/camel). If the cart
        // is being pulled by a player on foot, `pulling` is that player, and making a Postilion ride
        // the player produced a bad passenger state that crashed the game.
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
        if (isLocked()) return;
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            boolean bl = !this.level().isClientSide() && !(this.getControllingPassenger() instanceof Player);
            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    if (bl
                            && canAddPassenger(entity)
                            && !entity.isPassenger()
                            && entity.getBbWidth() < this.getBbWidth()
                            && entity.getBbWidth() * entity.getBbHeight() < 1.5
                            && entity instanceof LivingEntity
                            && !(entity instanceof AgeableWaterCreature)
                            && !(entity instanceof Player)) {
                        if(entity instanceof TamableAnimal tamable) tamable.setInSittingPose(true);
                        entity.startRiding(this);
                    }
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResult interact(final @NotNull Player player, final @NotNull InteractionHand hand, final @NotNull Vec3 vec) {
        if (isLocked()) return InteractionResult.FAIL;
        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                for (final Entity entity : this.getPassengers()) {
                    if (!(entity instanceof Player)) {
                        entity.stopRiding();
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        final InteractionResult bannerResult = this.useBanner(player, hand);
        if (bannerResult.consumesAction()) {
            return bannerResult;
        }
        if (this.getPulling() != player) {
            if (!this.canAddPassenger(player)) {
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide()) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void push(final Entity entityIn) {
        if (!entityIn.hasPassenger(this)) {
            if (!this.level().isClientSide() && this.getPulling() != entityIn && this.getControllingPassenger() == null && this.getPassengers().size() < 2 && !entityIn.isPassenger() && entityIn.getBbWidth() < this.getBbWidth() && entityIn instanceof LivingEntity
                    && !(entityIn instanceof AgeableWaterCreature) && !(entityIn instanceof Player)) {
                entityIn.startRiding(this);
            } else {
                super.push(entityIn);
            }
        }
    }

    @Override
    protected boolean canAddPassenger(final @NotNull Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    public float getPassengersRidingOffsetY(EntityDimensions entityDimensions, float f) {
        return (entityDimensions.height() - 8f / 16f) * f;
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity entity, @NotNull EntityDimensions entityDimensions, float f) {
        double f1 = -0.1d;
        if (this.getPassengers().size() > 1) {
            f1 = this.getPassengers().indexOf(entity) == 0 ? 0.2d : -0.6d;
            if (entity instanceof Animal) {
                f1 += 0.2d;
            }
        }
        final Vec3 forward = this.getLookAngle().scale(f1 + Mth.sin((float) Math.toRadians(this.getXRot())) * 0.7D);
        return new Vec3(forward.x, getPassengersRidingOffsetY(entityDimensions, f) + forward.y, forward.z);
    }

    @Override
    public void positionRider(final @NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (this.hasPassenger(passenger)) {
            passenger.setYBodyRot(this.getYRot());
            final float f2 = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
            final float f1 = Mth.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.setYRot(passenger.getYRot() + (f1 - f2));
            passenger.setYHeadRot(passenger.getYRot());
            if (passenger instanceof Animal && this.getPassengers().size() > 1) {
                final int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((Animal) passenger).yBodyRot + j);
                passenger.setYHeadRot(passenger.getYHeadRot() + j);
            }
        }
    }

    @Override
    public Item getCartItem() {
        return HorseCarts.item(CartKind.ANIMAL_CART, getWoodType());
    }
}