/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.mixin;

import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import com.example.horsegenetics.neoforge.carts.network.serverbound.CoachmanMovePayload;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class CartLocalPlayerMixin extends Player {

    public CartLocalPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getRootVehicle()Lnet/minecraft/world/entity/Entity;"))
    public void tick(CallbackInfo ci) {
        Entity entity = this.getRootVehicle();
        if (entity != this && entity.getControllingPassenger() == this && entity instanceof AbstractDrawnEntity drawnEntity) {
            CartWorld.getClient().getCurrentlyPulling(drawnEntity).ifPresent(pulling -> ClientPacketDistributor.sendToServer(new CoachmanMovePayload(this.zza)));
        }
    }

}