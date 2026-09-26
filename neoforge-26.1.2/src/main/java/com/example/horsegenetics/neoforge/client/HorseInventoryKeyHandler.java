package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.server.ActionTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * <b>The inventory key, aimed at a horse, opens that horse's menu</b> (owner,
 * 2026-09-25). Look at a horse and press <kbd>E</kbd> and you get
 * {@link HorseInfoScreen} - the same window sneak-and-use opens, and the same
 * one {@link HorseInfoKeyHandler} gives you from the saddle. Look at anything
 * else, or at nothing, and the key opens your own inventory exactly as before.
 *
 * <p>This is the third route to one screen and they divide by where you are
 * standing: {@code HorseInfoInteraction} is a horse <i>within arm's reach</i>,
 * {@code HorseInfoKeyHandler} is the horse <i>underneath you</i>, and this is
 * the horse <i>you are looking at</i> - which reaches across a paddock, and
 * needs no hand free and no modifier held.
 *
 * <h2>Everything worth knowing is on {@link HorseInfoKeyHandler}</h2>
 * Why the key is taken in {@link ClientTickEvent.Pre} rather than the screen
 * swapped later, why the clicks are drained in a loop, and why taking the key
 * costs vanilla's horse inventory nothing (its saddle and barding are two slots
 * on the Gear tab, and a horse has no chest) are all written up there. This
 * class differs in exactly one line: which horse it is about.
 *
 * <p><b>The rider case wins.</b> If the player is on a horse, that handler runs
 * and this one must not also fire - otherwise looking at a second horse from the
 * saddle would open the wrong animal's window. It is ordered by the vehicle
 * check here rather than by event priority, because two subscribers draining the
 * same key on the same tick is a thing to make impossible rather than to order
 * carefully.
 *
 * <p>No packet and no menu: the screen is built from
 * {@link ClientHorseRecordCache}, which the client already has for every horse
 * it can see. A horse with no record yet - or a donkey or a mule, which never
 * get one - bails out <b>without consuming the press</b>, so vanilla opens the
 * player's inventory as if this class did not exist.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseInventoryKeyHandler {

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // The same guard vanilla puts around handleKeybinds, and the same one
        // HorseInfoKeyHandler uses: a press made while a screen is up is not a
        // click, but the key must not be drained out from under an open screen.
        if (mc.player == null || mc.screen != null || mc.getOverlay() != null) {
            return;
        }
        if (mc.player.isPassenger()) {
            return; // riding: HorseInfoKeyHandler owns the key
        }
        if (!(mc.crosshairPickEntity instanceof AbstractHorse horse)) {
            return;
        }
        // What the client already worked out for this frame: the entity under
        // the crosshair, within the player's reach. Null when they are looking
        // at a block or at nothing.
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null) {
            return; // a donkey, a mule, or a horse we have not been told about yet
        }
        boolean pressed = false;
        while (mc.options.keyInventory.consumeClick()) {
            pressed = true;
        }
        if (!pressed) {
            return;
        }
        ActionTrace.log("key E", "horse menu for the horse being looked at");
        mc.setScreen(new HorseInfoScreen(record, horse, null));
    }

    private HorseInventoryKeyHandler() {
    }
}
