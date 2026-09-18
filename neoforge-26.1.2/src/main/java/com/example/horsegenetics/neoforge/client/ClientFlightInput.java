package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.ToggleFlightPayload;
import com.example.horsegenetics.neoforge.server.FlightToggle;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * <b>Double-tap jump to fly; sneak to come down; and sneak must stop meaning "get off".</b>
 *
 * <p>All three are done here, on {@code MovementInputUpdateEvent}, which NeoForge posts from
 * {@code LocalPlayer.aiStep} immediately after {@code input.tick()} and <b>before</b> vanilla's own double-tap
 * detector and the horse's jump-charge block. That ordering is the whole reason this needs no mixin: the event hands
 * over the live {@code ClientInput}, whose {@code keyPresses} field is public and mutable, so the rider's keys can be
 * rewritten before anything vanilla reads them.
 *
 * <h2>Why sneak has to be taken away</h2>
 * Dismount is decided on the SERVER, in {@code Player.rideTick}: {@code wantsToStopRiding()} is
 * {@code isShiftKeyDown()}, and that flag is set from the input packet. So a rider who sneaks to descend is a rider
 * who has just stepped off a horse a hundred blocks up. Stripping {@code shift} out of the packet before it is sent
 * means the server never learns the key was held, the dismount never fires, and this class keeps the real value for
 * the descent. Cancelling {@code EntityMountEvent} would also stop the dismount, but that event sits on
 * {@code removeVehicle}, which is also how death, ejection and teleport get a rider off a horse - too blunt a tool
 * for a key rebind.
 *
 * <p>Dismounting while flying is on F instead - see {@code DebugKeyHandler}.
 *
 * <h2>The double tap</h2>
 * Vanilla's own creative-flight detector is copied rather than invented, down to the seven-tick window
 * ({@code LocalPlayer.aiStep}, {@code jumpTriggerTime = 7}). Vanilla explicitly allows that toggle while riding a
 * jumpable vehicle, so this is with the grain. What it does NOT do is reuse the mechanism: that path flips the
 * player's own {@code abilities.flying}, which is a different thing entirely from asking a horse to fly.
 *
 * <p><b>UNVERIFIED at runtime.</b> Written against the 26.1.2 sources.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientFlightInput {

    private ClientFlightInput() {
    }

    /** Vanilla's window, in ticks, between the two taps. {@code LocalPlayer.jumpTriggerTime = 7}. */
    private static final int DOUBLE_TAP_TICKS = 7;

    private static int tapWindow;
    private static boolean jumpWasDown;

    /** The real sneak key, kept after it has been stripped out of what the server is told. */
    private static volatile boolean descending;

    /** Read by {@code HorseFlightRiddenMixin} to turn sneak into downward input. */
    public static boolean descending() {
        return descending;
    }

    @SubscribeEvent
    static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof Horse horse)) {
            reset();
            return;
        }
        if (horse.onGround()) {
            // Down safely. Forget the toggle and any landing on this side; the
            // server clears its own copies by the same test, so neither has to
            // tell the other.
            ClientFlightHandler.forget(player.getUUID());
        }
        // TRUE FLIGHT ONLY. A glider is not toggled on - it glides whenever it is
        // off the ground - so neither the double tap nor the sneak handling below
        // has anything to do with it, and an elytra is steered by look alone.
        if (HorseFlight.of(horse).mode() != HorseFlight.Mode.TRUE_FLIGHT) {
            reset();
            return;
        }

        Input keys = event.getInput().keyPresses;

        if (tapWindow > 0) {
            tapWindow--;
        }
        boolean jumpDown = keys.jump();
        boolean rising = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        if (rising) {
            if (tapWindow == 0) {
                tapWindow = DOUBLE_TAP_TICKS;
            } else {
                tapWindow = 0;
                ClientFlightHandler.toggleLocal(player.getUUID());
                ClientPacketDistributor.sendToServer(new ToggleFlightPayload());
            }
        }

        boolean flying = FlightToggle.wants(horse);
        descending = flying && keys.shift();
        if (flying && keys.shift()) {
            // Take sneak out of what the server will be told, so Player.rideTick
            // never sees isShiftKeyDown() and never dismounts the rider. The
            // real value is kept in `descending` for the mixin to read.
            event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                    keys.jump(), false, keys.sprint());
        }
    }

    private static void reset() {
        tapWindow = 0;
        jumpWasDown = false;
        descending = false;
    }
}
