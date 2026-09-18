package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.ActionTrace;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.world.entity.animal.equine.Horse;
import com.example.horsegenetics.neoforge.network.RequestDebugPensPayload;
import com.example.horsegenetics.neoforge.network.RequestHighlightHorsesPayload;
import com.example.horsegenetics.neoforge.network.RequestStallHighlightPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * NOTE: verify "ClientTickEvent.Post" is the correct current event class/name
 * for 26.1.2 against NeoForged docs - client tick event naming has shifted
 * across versions before (ClientTickEvent used to be a single fireable event
 * rather than split Pre/Post). Same category of caveat as the render-state
 * generics elsewhere in this project.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugKeyHandler {

    /** Ticks left in which a second press of F means "get off" rather than "land". Half a second. */
    private static final int DOUBLE_PRESS_TICKS = 10;

    private static int dismountWindow;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (dismountWindow > 0) {
            dismountWindow--;
        }
        // Each binding is guarded on its own. It used to return early when
        // generateDebugPens was null, which in a production build is always -
        // so a binding that IS registered there (the horse highlight) would
        // never have been read at all.
        while (DebugKeyBindings.generateDebugPens != null
                && DebugKeyBindings.generateDebugPens.consumeClick()) {
            ActionTrace.log("key F6", "debug pens requested");
            ClientPacketDistributor.sendToServer(new RequestDebugPensPayload());
        }
        while (DebugKeyBindings.showStalls != null && DebugKeyBindings.showStalls.consumeClick()) {
            ActionTrace.log("key F7", "stall overlay requested");
            ClientPacketDistributor.sendToServer(new RequestStallHighlightPayload());
        }
        while (DebugKeyBindings.diveHorse != null && DebugKeyBindings.diveHorse.consumeClick()) {
            // F IS CONTEXTUAL: one key meaning "do the vertical thing this horse
            // can do". Owner's call. The two contexts cannot overlap - a horse
            // in water is swimming, not flying - so no second binding is needed
            // and a rider has one key to remember mid-gallop.
            //
            // The CLIENT decides which packet to send, because it is the side
            // that knows both halves: the horse's genes (from the record cache)
            // and whether it is standing in water. The server re-checks the
            // rider either way.
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            // TRUE FLIGHT ONLY - a glider keeps every vanilla control it had.
            // Sneak still dismounts it (ClientFlightInput only takes sneak away
            // from true flight), so F has no landing to offer and falls through
            // to diving, exactly as it does on any ordinary horse.
            boolean flying = minecraft.player != null
                    && minecraft.player.getVehicle() instanceof Horse horse
                    && HorseFlight.of(horse).mode() == HorseFlight.Mode.TRUE_FLIGHT
                    && HorseFlight.active(horse);
            if (!flying) {
                ClientPacketDistributor.sendToServer(
                        new com.example.horsegenetics.neoforge.network.ToggleDivePayload());
            } else if (dismountWindow > 0) {
                // SECOND PRESS: get off, wherever we are. The emergency exit.
                dismountWindow = 0;
                ClientFlightHandler.forget(minecraft.player.getUUID());
                ClientPacketDistributor.sendToServer(
                        new com.example.horsegenetics.neoforge.network.DismountPayload());
            } else {
                // FIRST PRESS: come down. Sneak cannot mean "get off" up here -
                // it is the descend key - so landing is what F means by default
                // and stepping off in mid-air has to be asked for twice.
                dismountWindow = DOUBLE_PRESS_TICKS;
                ClientFlightHandler.landLocal(minecraft.player.getUUID());
                ClientPacketDistributor.sendToServer(
                        new com.example.horsegenetics.neoforge.network.LandPayload());
            }
        }
        while (DebugKeyBindings.highlightHorses != null && DebugKeyBindings.highlightHorses.consumeClick()) {
            // Logged on the press rather than on the toggle's answer, so a key
            // that is read but produces nothing is distinguishable from a key
            // that was never read - which is exactly how the F8 toggle bug read
            // from the outside (2026-09-12).
            ActionTrace.log("key F8", "horse highlight toggle pressed");
            ClientPacketDistributor.sendToServer(new RequestHighlightHorsesPayload());
        }
    }

    private DebugKeyHandler() {
    }
}
