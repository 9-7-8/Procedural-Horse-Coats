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

    public static final String PROPERTY = "horsegenetics.photoShoot";

    /** The colour the debug test boot is drawn in while a shot asks for it. */
    private static final int BOOT_COLOUR = 0xFF2E6FD9;


    /**
     * The debug test boot ({@code WornPieces.TEST_BOOT_FORE} and {@code _HIND}): its
     * colour as opaque ARGB while the shot on stage has {@code boot} in its name, and 0
     * otherwise. Read by {@code gear.WornLayer}, which only has the boot rows at all when
     * the client was started for a photo shoot. Not an item: nothing a player does sets it.
     */
    public static volatile int testBoot;

    /**
     * The debug test block ({@code WornPieces.TEST_BLOCK}): one on each hoof while the
     * shot on stage has {@code block} in its name, and none otherwise. Read by
     * {@code GeneticHorseRenderer} into every horse's render state, so it is the whole of
     * who "wears" it. Not an item: nothing a player does sets it.
     */
    public static volatile java.util.List<com.example.horsegenetics.common.gear.WornPart> testBlocks =
            java.util.List.of();

    /** True while the shot on stage has {@code -walk} in its name: its horse is held mid-stride. */
    private static boolean walking;

    /** Ticks after the player appears before anything happens - the test world's own setup first. */
    private static final int START_DELAY = 400;
    /** Ticks between staging a horse and shooting it: entity sync, mesh bake, chunk render. */
    private static final int SETTLE = 80;

    private static int ticks;
    private static int shot = -1;
    private static int wait;
    private static boolean done;
    /** {@code gear.edges} as it was before the shoot, put back when it ends. */
    private static Boolean edgesBefore;

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
        if (walking) {
            stride(mc);
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
            testBoot = 0;
            testBlocks = java.util.List.of();
            walking = false;
            mc.options.hideGui = false;
            if (edgesBefore != null) {
                com.example.horsegenetics.neoforge.ClientConfig.GEAR_EDGES.set(edgesBefore);
            }
            server.execute(DebugPhotoShoot::finish);
            HorseGenetics.LOGGER.info("[photo] done - {} shots", DebugPhotoShoot.SHOTS.size());
            return;
        }
        stage(server, id, shot);
        wait = SETTLE;
    }

    /**
     * Hold every horse in view at one fixed point of its stride. A staged horse has no AI
     * and does not move, and the walk cycle is driven by distance covered, so a still of
     * it "walking" has to put the cycle there by hand: after the entity's own tick has
     * let the cycle decay, set it back to full speed, five steps in (legs near full swing).
     */
    private static void stride(Minecraft mc) {
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.animal.equine.Horse horse) {
                horse.walkAnimation.stop();
                for (int step = 0; step < 5; step++) {
                    horse.walkAnimation.update(1f, 1f, 1f);
                }
            }
        }
    }

    private static void stage(IntegratedServer server, java.util.UUID id, int i) {
        // A shot named "...-flat" is taken with worn gear's depth switched off, so a
        // lifted piece can be photographed beside its flat self in one launch. Set in
        // memory only: nothing is saved to client.toml.
        if (edgesBefore == null) {
            edgesBefore = com.example.horsegenetics.neoforge.ClientConfig.gearEdges();
        }
        String name = DebugPhotoShoot.SHOTS.get(i).name();
        com.example.horsegenetics.neoforge.ClientConfig.GEAR_EDGES.set(!name.endsWith("-flat"));
        testBoot = name.contains("boot") ? BOOT_COLOUR : 0;
        // Shoes are real gear now: a shot is shod by DebugPhotoShoot.shod, through the slots.
        testBlocks = name.contains("block")
                ? com.example.horsegenetics.common.gear.WornPart.testBlocks(BOOT_COLOUR)
                : java.util.List.of();
        walking = name.contains("-walk");
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                DebugPhotoShoot.stage(sp, i);
            }
        });
    }
}
