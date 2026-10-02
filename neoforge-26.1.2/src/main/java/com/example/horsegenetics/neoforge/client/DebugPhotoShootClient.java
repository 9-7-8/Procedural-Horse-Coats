package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.server.DebugPhotoShoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * <b>Dev-only: walks {@link DebugPhotoShoot#SHOTS} and saves one screenshot each</b>
 * to {@code run/screenshots/photo-<name>.png}, with the GUI hidden. Off unless the
 * client was started with {@code -Dhorsegenetics.photoShoot=true}
 * ({@code gradlew :neoforge-26.1.2:runClientTestWorld -PphotoShoot}).
 *
 * <p>Singleplayer only: it reaches the integrated server directly rather than over a
 * payload, which is exactly why it can be a dev tool and nothing more. The server half
 * builds the stage and spawns each horse; this half waits for the chunk and the horse
 * to reach the client, then grabs the main render target - Minecraft's own F2 path.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugPhotoShootClient {

    static final String PROPERTY = "horsegenetics.photoShoot";

    /** Ticks after the player appears before anything happens - the test world's own setup first. */
    private static final int START_DELAY = 400;
    /** Ticks between staging a horse and shooting it: entity sync, mesh bake, chunk render. */
    private static final int SETTLE = 80;

    private static int ticks;
    private static int shot = -1;
    private static int wait;
    private static boolean done;

    private DebugPhotoShootClient() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (done || !Boolean.getBoolean(PROPERTY)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) {
            return;
        }
        ticks++;
        if (ticks < START_DELAY) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        if (shot < 0) {
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayer(id);
                if (sp != null) {
                    DebugPhotoShoot.buildStage(sp);
                }
            });
            shot = 0;
            stage(server, id, 0);
            wait = SETTLE * 2;
            return;
        }
        String name = "photo-" + DebugPhotoShoot.SHOTS.get(shot).name() + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
                msg -> HorseGenetics.LOGGER.info("[photo] saved {}", name));
        shot++;
        if (shot >= DebugPhotoShoot.SHOTS.size()) {
            done = true;
            mc.options.hideGui = false;
            server.execute(DebugPhotoShoot::finish);
            HorseGenetics.LOGGER.info("[photo] done - {} shots", DebugPhotoShoot.SHOTS.size());
            return;
        }
        stage(server, id, shot);
        wait = SETTLE;
    }

    private static void stage(IntegratedServer server, java.util.UUID id, int i) {
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                DebugPhotoShoot.stage(sp, i);
            }
        });
    }
}
