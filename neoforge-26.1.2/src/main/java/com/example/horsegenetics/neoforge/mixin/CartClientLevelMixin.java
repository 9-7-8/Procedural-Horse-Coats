/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.mixin;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import com.example.horsegenetics.neoforge.carts.network.serverbound.RequestCartUpdatePayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class CartClientLevelMixin {

    @Inject(method = "addEntity", at = @At("TAIL"))
    public void addEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof AbstractDrawnEntity d) {
            ClientPacketDistributor.sendToServer(new RequestCartUpdatePayload(d.getId()));
        }
    }

}
