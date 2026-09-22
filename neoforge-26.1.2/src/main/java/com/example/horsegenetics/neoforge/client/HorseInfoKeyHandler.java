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
 * <b>The inventory key, from the saddle, opens this mod's screen.</b> Ride a
 * horse, press <kbd>E</kbd>, and you get {@link HorseInfoScreen} rather than
 * vanilla's horse inventory.
 *
 * <p>It is the companion to {@link HorseInfoInteraction}: sneak-and-use is how
 * you read a horse you are <i>standing beside</i>, and there is no comfortable
 * way to perform it on the animal you are sitting on. <kbd>E</kbd> is the
 * gesture vanilla already spends on "the window belonging to what I am riding",
 * so the two together mean the screen is one key or one click away from
 * wherever you happen to be.
 *
 * <p><b>Nothing is lost by taking it.</b> Vanilla's screen holds a saddle slot,
 * a body-armour slot and a chest grid; a horse has no chest
 * ({@code AbstractHorse.getInventoryColumns()} is zero for one) and the other
 * two are {@code HorseTackSlot.SADDLE} and {@code BARDING} on the Gear tab. A
 * donkey or a mule <i>does</i> have a chest, and never reaches here: this mod
 * writes a record only for a {@link net.minecraft.world.entity.animal.equine.Horse},
 * and with no record this bails out without consuming the press, so vanilla
 * opens its own screen exactly as before. Same for a horse whose record has not
 * arrived yet.
 *
 * <h2>Why the key is taken here and not the screen swapped later</h2>
 * The obvious alternative - let vanilla open and replace the screen on
 * {@code ScreenEvent.Opening} - is wrong twice over. The vanilla horse screen is
 * a <i>container</i> screen opened by the server: by the time it is opening, the
 * server has a menu bound to the player, and swapping in a plain screen leaves
 * that menu open with nothing to close it. Taking the key before the request is
 * sent means the server is never asked. It also has to happen in
 * {@link ClientTickEvent.Pre}, which {@code Minecraft.tick} fires <i>before</i>
 * {@code handleKeybinds} - that is the method whose
 * {@code keyInventory.consumeClick()} loop would otherwise reach
 * {@code isServerControlledInventory()} and send
 * {@code ServerboundPlayerCommandPacket.OPEN_INVENTORY}. The clicks are drained
 * in a loop for the same reason vanilla drains them: two presses in one tick
 * would otherwise leave the second for vanilla, which would open its screen on
 * top of this one.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseInfoKeyHandler {

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // The same guard vanilla puts around handleKeybinds. A press made while
        // a screen is up never counts as a click anyway, but the key must not
        // be drained out from under a screen that is open.
        if (mc.player == null || mc.screen != null || mc.getOverlay() != null) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }
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
        ActionTrace.log("key E", "horse menu from the saddle");
        mc.setScreen(new HorseInfoScreen(record, horse, null));
    }

    private HorseInfoKeyHandler() {
    }
}
