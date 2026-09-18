/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.network.serverbound;

import net.neoforged.neoforge.network.PacketDistributor;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.network.clientbound.UpdateDrawnPayload;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public record RequestCartUpdatePayload(int cartId) implements CustomPacketPayload {

    public static final Type<@NotNull RequestCartUpdatePayload> TYPE = new CustomPacketPayload.Type<>(HorseCarts.resLoc("request_cart_update"));
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull RequestCartUpdatePayload> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull RequestCartUpdatePayload decode(FriendlyByteBuf buf) {
            return new RequestCartUpdatePayload(buf.readVarInt());
        }

        @Override
        public void encode(FriendlyByteBuf buf, RequestCartUpdatePayload payload) {
            buf.writeVarInt(payload.cartId());
        }
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestCartUpdatePayload msg, ServerPlayer player) {
        var level = player.level();
        var pulling = CartWorld.get(level).getPulling();

        pulling.keySet().intStream()
                .filter(pullId -> CartWorld.get(level).getDrawn(level.getEntity(pullId)).map(Entity::getId).orElse(-1) == msg.cartId)
                .findFirst().ifPresent(pullId -> PacketDistributor.sendToPlayer(player, new UpdateDrawnPayload(pullId, msg.cartId)));
    }
}
