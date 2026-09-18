/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.network.serverbound;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import com.example.horsegenetics.neoforge.carts.entity.PostilionEntity;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record CoachmanMovePayload(float zza) implements CustomPacketPayload {

    public static final Type<@NotNull CoachmanMovePayload> TYPE = new CustomPacketPayload.Type<>(HorseCarts.resLoc("coachman_move"));
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull CoachmanMovePayload> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull CoachmanMovePayload decode(FriendlyByteBuf buf) {
            return new CoachmanMovePayload(buf.readFloat());
        }

        @Override
        public void encode(FriendlyByteBuf buf, CoachmanMovePayload msg) {
            buf.writeFloat(msg.zza());
        }
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoachmanMovePayload msg, ServerPlayer player) {
        ServerLevel level = player.level();
        Entity vehicle = player.getRootVehicle();
        if (vehicle != player && vehicle.getControllingPassenger() == player && vehicle instanceof AbstractDrawnEntity drawnEntity) {
            Entity pulling = CartWorld.get(level).getCurrentlyPulling(drawnEntity).orElse(null);
            if (pulling != null) {
                LivingEntity passenger = pulling.getControllingPassenger();
                if (passenger instanceof PostilionEntity postilion) {
                    postilion.setYRot(player.getYRot());
                    postilion.yRotO = postilion.getYRot();
                    postilion.setXRot(player.getXRot() * 0.5F);
                    postilion.zza = msg.zza();
                    postilion.xxa = 0.0F;
                }
            }
        }
    }
}
