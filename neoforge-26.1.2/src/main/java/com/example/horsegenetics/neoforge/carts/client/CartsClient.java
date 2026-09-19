/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.client.renderer.CartsModelLayers;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.AnimalCartRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.PlowRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.PostilionRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.ReaperRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.SeedDrillRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.SupplyCartRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.WagonRenderer;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.AnimalCartModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.PlowModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.ReaperModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.SeedDrillModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.SupplyCartModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.WagonModel;
import com.example.horsegenetics.neoforge.carts.client.renderer.entity.model.WagonRoofModel;
import com.example.horsegenetics.neoforge.carts.client.screen.PlowScreen;
import com.example.horsegenetics.neoforge.carts.client.screen.SeedDrillScreen;
import com.example.horsegenetics.neoforge.carts.client.screen.WagonScreen;
import com.example.horsegenetics.neoforge.carts.item.CartItem;
import com.example.horsegenetics.neoforge.carts.network.clientbound.UpdateDrawnPayload;
import com.example.horsegenetics.neoforge.carts.network.serverbound.ActionKeyPayload;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The client half of the cart system: renderers, layers, screens, the action
 * key, and the cart tooltip.
 *
 * <h2>No {@code @Mod}, and no mod bus passed in</h2>
 * Upstream's client half was a second {@code @Mod(dist = Dist.CLIENT)} class
 * with a constructor. A jar may hold several {@code @Mod} classes only if each
 * is its own mod id, so that had to go - and rather than thread a mod-bus
 * reference through the common constructor behind a dist check (which risks
 * loading a client class during verification on a dedicated server), this uses
 * the same {@code @EventBusSubscriber} idiom the rest of this mod's client code
 * uses. The annotation is what keeps the class off a server entirely, which
 * matters here: the key mappings below are constructed at class load.
 *
 * <p>The two game-bus listeners are added in {@link #onClientSetup} rather than
 * by a second annotated class, since {@code @EventBusSubscriber} names one bus.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CartsClient {

    private static final KeyMapping.Category KEY_CATEGORY =
            new KeyMapping.Category(HorseCarts.resLoc("carts"));

    private static final KeyMapping actionKeyMapping = new KeyMapping(
            "key.horsegenetics.cart_action", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);

    private CartsClient() {
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(CartsClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(CartsClient::onClientTick);
    }

    @SubscribeEvent
    static void onRegisterClientPayloads(final RegisterClientPayloadHandlersEvent event) {
        // NeoForge runs payload handlers on the main thread, so no execute() hop.
        event.register(UpdateDrawnPayload.TYPE, (payload, ctx) -> {
            final var level = Minecraft.getInstance().level;
            if (level != null) {
                UpdateDrawnPayload.handle(payload, level);
            }
        });
    }

    @SubscribeEvent
    static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HorseCarts.SUPPLY_CART_ENTITY, SupplyCartRenderer::new);
        event.registerEntityRenderer(HorseCarts.ANIMAL_CART_ENTITY, AnimalCartRenderer::new);
        event.registerEntityRenderer(HorseCarts.PLOW_ENTITY, PlowRenderer::new);
        event.registerEntityRenderer(HorseCarts.SEED_DRILL_ENTITY, SeedDrillRenderer::new);
        event.registerEntityRenderer(HorseCarts.REAPER_ENTITY, ReaperRenderer::new);
        event.registerEntityRenderer(HorseCarts.WAGON_ENTITY, WagonRenderer::new);
        event.registerEntityRenderer(HorseCarts.POSTILION_ENTITY, PostilionRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CartsModelLayers.SUPPLY_CART, SupplyCartModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.ANIMAL_CART, AnimalCartModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.PLOW, PlowModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.SEED_DRILL, SeedDrillModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.REAPER, ReaperModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.WAGON, WagonModel::createLayer);
        event.registerLayerDefinition(CartsModelLayers.WAGON_ROOF, WagonRoofModel::createRoofLayer);
        event.registerLayerDefinition(CartsModelLayers.WAGON_CHEST, WagonModel::createChestLayer);
    }

    @SubscribeEvent
    static void onRegisterScreens(final RegisterMenuScreensEvent event) {
        event.register(HorseCarts.PLOW_MENU_TYPE, PlowScreen::new);
        event.register(HorseCarts.SEED_DRILL_MENU_TYPE, SeedDrillScreen::new);
        event.register(HorseCarts.WAGON_9x4_MENU_TYPE, WagonScreen::new);
        event.register(HorseCarts.WAGON_9x8_MENU_TYPE, WagonScreen::new);
        event.register(HorseCarts.WAGON_12x9_MENU_TYPE, WagonScreen::new);
    }

    @SubscribeEvent
    static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(actionKeyMapping);
    }

    /**
     * The cart tooltip. Two lines about the vehicle, and - the part that is
     * ours - <b>what the load costs</b>, so a player can compare a wagon against
     * a supply cart before building either.
     *
     * <p>Two numbers on a vehicle that carries anything: empty and full. One
     * number would be a lie on a supply cart, which is most of the time the
     * heavier half of that pair.
     */
    private static void onTooltip(final ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof CartItem cart)) {
            return;
        }
        final var lines = event.getToolTip();
        if (!Minecraft.getInstance().hasShiftDown()) {
            lines.add(Component.translatable("item.cart.press_shift_tooltip").withStyle(ChatFormatting.GRAY));
            return;
        }
        final String key = "item.horsegenetics." + cart.getCartType().id();
        lines.add(Component.translatable(key + ".tooltip1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(key + ".tooltip2").withStyle(ChatFormatting.GRAY));
        // An ordinary horse's retention on this load, as a percentage: the one
        // number that says "this is the heavy one" without a wiki page.
        final var kind = cart.getCartType();
        lines.add(Component.translatable("item.horsegenetics.cart.draught", retentionPercent(kind.load()))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (kind.cargoShare() > 0.0) {
            final double full = CartDraft.loaded(kind.load(), kind.cargoShare(), 1.0);
            lines.add(Component.translatable("item.horsegenetics.cart.cargo", retentionPercent(full))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** What an ordinary horse keeps on this load, rounded for a tooltip. */
    private static int retentionPercent(final double load) {
        return (int) Math.round(100.0 * CartDraft.retention(
                com.example.horsegenetics.common.trait.HorseTraits.BASE_PULL,
                com.example.horsegenetics.common.trait.HorseTraits.BASE_SPEED,
                load));
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft client = Minecraft.getInstance();
        while (actionKeyMapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ActionKeyPayload());
        }
        if (!client.isPaused() && client.level != null) {
            CartWorld.getClient().tick(client.level);
        }
    }
}
