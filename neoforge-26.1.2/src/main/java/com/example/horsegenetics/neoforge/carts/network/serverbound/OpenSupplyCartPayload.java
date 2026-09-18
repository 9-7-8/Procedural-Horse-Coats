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
import com.example.horsegenetics.neoforge.carts.entity.SupplyCartEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record OpenSupplyCartPayload() implements CustomPacketPayload {

    public static final Type<@NotNull OpenSupplyCartPayload> TYPE = new CustomPacketPayload.Type<>(HorseCarts.resLoc("open_supply_cart"));
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull OpenSupplyCartPayload> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull OpenSupplyCartPayload decode(FriendlyByteBuf object) {
            return new OpenSupplyCartPayload();
        }

        @Override
        public void encode(FriendlyByteBuf object, OpenSupplyCartPayload object2) {
        }
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final Player player) {
        final Entity ridden = player.getVehicle();
        if (ridden instanceof SupplyCartEntity) {
            ((SupplyCartEntity) ridden).openCustomInventoryScreen(player);
        }
    }
}
