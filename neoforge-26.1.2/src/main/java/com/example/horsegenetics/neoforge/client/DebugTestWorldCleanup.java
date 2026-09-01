package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppedEvent;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dev-only: the worlds made by the "Spawn Test Horse World" button
 * ({@link DebugTitleScreenButton}) are throwaway, so they are deleted for you
 * instead of piling up in {@code run/saves}.
 *
 * <p>Two sweeps, both over {@code saves/} looking only for directories named
 * exactly {@value #PREFIX} + digits - the names {@link #newDirectoryName()}
 * hands out, so a hand-made world is never a candidate:
 * <ul>
 *   <li>{@link ClientStoppedEvent} - the normal path. It fires from
 *       {@code Minecraft#destroy} <em>after</em> the disconnect has halted the
 *       integrated server and waited for it to finish saving, and after
 *       {@code close()}, so nothing still holds the save folder. It is the last
 *       hook before {@code System.exit}.</li>
 *   <li>{@link ClientStartedEvent} - the safety net. A crash, a taskkill or a
 *       failed delete (Windows can hold a handle a moment longer) leaves the
 *       folder behind; the next launch clears it before anything opens a
 *       world.</li>
 * </ul>
 *
 * <p>Inert in production - nothing there creates these worlds, and the gate
 * below means nothing there deletes one either.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugTestWorldCleanup {

    /** Directory-name prefix for a button-spawned world. */
    public static final String PREFIX = "test_horse_";

    /** Exactly what {@link #newDirectoryName()} produces - nothing else is deleted. */
    private static final Pattern TEST_WORLD = Pattern.compile(Pattern.quote(PREFIX) + "\\d+");

    private DebugTestWorldCleanup() {
    }

    /** A fresh, unique save-directory name for the title-screen button. */
    public static String newDirectoryName() {
        return PREFIX + System.currentTimeMillis();
    }

    @SubscribeEvent
    static void onClientStarted(ClientStartedEvent event) {
        deleteTestWorlds(event.getClient(), "left over from a previous run");
    }

    @SubscribeEvent
    static void onClientStopped(ClientStoppedEvent event) {
        deleteTestWorlds(event.getClient(), "on shutdown");
    }

    private static void deleteTestWorlds(Minecraft client, String why) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        Path saves = client.getLevelSource().getBaseDir();
        if (!Files.isDirectory(saves)) {
            return;
        }
        List<Path> worlds;
        try (Stream<Path> entries = Files.list(saves)) {
            worlds = entries
                    .filter(Files::isDirectory)
                    .filter(p -> TEST_WORLD.matcher(p.getFileName().toString()).matches())
                    .toList();
        } catch (IOException e) {
            HorseGenetics.LOGGER.warn("Could not scan {} for test horse worlds", saves, e);
            return;
        }
        for (Path world : worlds) {
            try {
                deleteRecursively(world);
                HorseGenetics.LOGGER.info("Deleted test horse world {} ({})", world.getFileName(), why);
            } catch (IOException e) {
                // Not fatal: the ClientStartedEvent sweep on the next launch retries.
                HorseGenetics.LOGGER.warn("Could not delete test horse world {}", world, e);
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
