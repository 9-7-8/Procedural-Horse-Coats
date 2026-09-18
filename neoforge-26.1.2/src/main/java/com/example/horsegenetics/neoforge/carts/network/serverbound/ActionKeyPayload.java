/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.network.serverbound;

import it.unimi.dsi.fastutil.Pair;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public record ActionKeyPayload() implements CustomPacketPayload {

    public static final Type<@NotNull ActionKeyPayload> TYPE = new CustomPacketPayload.Type<>(HorseCarts.resLoc("action_key"));
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull ActionKeyPayload> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ActionKeyPayload decode(FriendlyByteBuf object) {
            return new ActionKeyPayload();
        }

        @Override
        public void encode(FriendlyByteBuf object, ActionKeyPayload object2) {}
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ServerPlayer player) {
        final Entity pulling;
        final Level level = player.level();
        if (player.getVehicle() == null) {
            pulling = player;
        } else {
            pulling = player.getVehicle();
        }
        var drawn = CartWorld.getServer(HorseCarts.server, level.dimension()).getDrawn(pulling);
        drawn.map(c -> Pair.of(c, (Entity) null))
                .or(() -> level.getEntitiesOfClass(AbstractDrawnEntity.class, pulling.getBoundingBox().inflate(2.0d), entity -> entity != pulling).stream()
                        .min(Comparator.comparing(pulling::distanceTo))
                        .map(c -> Pair.of(c, pulling))
                ).filter(p -> p.key().getConfig().adventureModeInteract.get()
                        || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE)
                .ifPresent(p -> p.key().setPulling(p.value()));
    }

}
