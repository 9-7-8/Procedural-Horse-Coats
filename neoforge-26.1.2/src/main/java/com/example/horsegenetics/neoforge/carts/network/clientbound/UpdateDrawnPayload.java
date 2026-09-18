/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.network.clientbound;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record UpdateDrawnPayload(int pullingId, int cartId) implements CustomPacketPayload {

    public static final Type<@NotNull UpdateDrawnPayload> TYPE = new CustomPacketPayload.Type<>(HorseCarts.resLoc("update_drawn"));
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull UpdateDrawnPayload> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull UpdateDrawnPayload decode(FriendlyByteBuf buf) {
            int pullingId = buf.readVarInt();
            int cartId = buf.readVarInt();
            return new UpdateDrawnPayload(pullingId, cartId);
        }

        @Override
        public void encode(FriendlyByteBuf buf, UpdateDrawnPayload payload) {
            buf.writeVarInt(payload.pullingId);
            buf.writeVarInt(payload.cartId);
        }
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateDrawnPayload msg, Level level) {
        final Entity e = level.getEntity(msg.cartId);
        if (e instanceof AbstractDrawnEntity drawn) {
            if (msg.pullingId < 0) {
                drawn.setPulling(null);
            } else {
                drawn.setPulling(level.getEntity(msg.pullingId));
            }
        }
    }
}
