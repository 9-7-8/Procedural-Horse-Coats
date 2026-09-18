/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.entity;

import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostilionEntity extends DummyLivingEntity {
    public PostilionEntity(EntityType<? extends @NotNull LivingEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.getCoachman() == null) {
                this.discard();
            }
        }
    }

    @Nullable
    private LivingEntity getCoachman() {
        final Entity mount = this.getVehicle();
        if (mount != null) {
            return CartWorld.get(this.level()).getDrawn(mount)
                    .map(AbstractDrawnEntity::getControllingPassenger).orElse(null);
        }
        return null;
    }

}
