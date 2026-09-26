package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.HorseRealm;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * <b>Is this client standing in the horse realm?</b> One key comparison a tick,
 * remembered, so that two things which both need the answer agree on it and
 * neither has to ask the level directly.
 *
 * <p>Those two are the browser's <i>Horse realm</i> tab, which is
 * {@linkplain #inRealm() hidden outside the field} (owner, 2026-09-26), and
 * {@link ClientRealmRoster}, whose thousands of cached rows are kept for exactly
 * as long as the player is in there and dropped on the way out.
 *
 * <h2>Why a watch and not a check</h2>
 * Both callers want the <b>edge</b>, not the state. The roster has to be thrown
 * away at the moment of leaving - a check made later, when the screen next
 * opens, would either serve a stale field or re-ask for one the player has no
 * way to see. And the tab has to stop being drawn at that moment too, including
 * while the screen is open, which nothing else would notice.
 *
 * <p>The dimension is only read while a level exists, so this costs nothing at
 * the title screen, and {@code HorseRealm.REALM_LEVEL} is a
 * {@code ResourceKey} comparison - a reference check in the common case.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientRealmWatch {

    private static boolean inRealm;

    private ClientRealmWatch() {
    }

    /** True while the player is standing in the horse realm. */
    public static boolean inRealm() {
        return inRealm;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean now = mc.player != null
                && mc.player.level().dimension().equals(HorseRealm.REALM_LEVEL);
        if (now == inRealm) {
            return;
        }
        inRealm = now;
        if (!now) {
            // Left the field. The roster is thousands of rows about horses the
            // player can no longer see or buy, and the tab that reads it has
            // just gone; holding it would only guarantee that the next visit
            // opens on a stale field.
            ClientRealmRoster.clear();
        }
    }

    /** Leaving the world entirely is leaving the realm, whatever the last tick said. */
    static void forget() {
        inRealm = false;
    }
}
