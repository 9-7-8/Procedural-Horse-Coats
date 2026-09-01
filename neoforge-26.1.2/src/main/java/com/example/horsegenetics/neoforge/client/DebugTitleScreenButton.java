package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.DebugTestWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Dev-only: a "Spawn Test Horse World" button on the title screen. It creates a
 * fresh throwaway world in <b>Creative</b> with cheats on, and flags
 * {@code DebugTestWorldHandler} to fill the hotbar (hay block, golden carrot,
 * stick, clock, paper, lead) on login - everything needed to exercise the
 * portal / taming / aging / inspect / lead features.
 *
 * <p>The world is throwaway: {@link DebugTestWorldCleanup} names the save
 * directory and deletes it again when the client shuts down.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugTitleScreenButton {

    private DebugTitleScreenButton() {
    }

    @SubscribeEvent
    static void onTitleScreenInit(ScreenEvent.Init.Post event) {
        if (FMLEnvironment.isProduction() || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        Button button = Button.builder(
                        Component.literal("Spawn Test Horse World"),
                        b -> spawnTestHorseWorld())
                .bounds(4, 4, 168, 20)
                .build();
        event.addListener(button);
    }

    private static void spawnTestHorseWorld() {
        Minecraft mc = Minecraft.getInstance();
        String directory = DebugTestWorldCleanup.newDirectoryName();
        LevelSettings settings = new LevelSettings(
                "Test Horse World",
                GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true, // allow cheats
                WorldDataConfiguration.DEFAULT);

        DebugTestWorldHandler.pendingHotbarFill = true;
        mc.createWorldOpenFlows().createFreshLevel(
                directory,
                settings,
                WorldOptions.defaultWithRandomSeed(),
                WorldPresets::createNormalWorldDimensions,
                mc.screen);
    }
}
