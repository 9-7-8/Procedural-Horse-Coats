package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Sneak and use on a horse you do not own to read its genes.</b>
 *
 * <p>Until this, the only way into {@link HorseInfoScreen} was the {@code i}
 * button on the vanilla horse inventory - and that inventory only opens on a
 * <b>tamed</b> horse. So every horse a player had not already tamed was opaque:
 * a wild founder, a generated stable's stock, and above all the
 * <a href="../server/CowboyHandler.html">cowboy's string</a>, where a player is
 * being asked to pay twelve to twenty-eight emeralds for an animal whose genes
 * are the entire product. Buying blind is not a purchase, it is a raffle, and
 * the arcane dealer does not work without this.
 *
 * <h2>Why not on a tamed horse too</h2>
 * The owner asked for "any horse", and this deliberately stops short of one:
 * sneak-and-use on a tamed horse is how vanilla opens the saddle and armour
 * slots, and taking that over would cost a player the ability to tack up. A
 * tamed horse already reaches the same screen in one more click, through the
 * {@code i} button that {@link HorseScreenHooks} puts on that inventory. So the
 * rule is <b>the horses whose inventory you cannot open anyway</b>, which is the
 * set the request was actually about.
 *
 * <h2>Why client-side only</h2>
 * The screen needs nothing from the server: {@code ClientHorseRecordCache} is
 * filled for every horse the player tracks, owned or not
 * ({@code HorseGeneticsEventHandler.onStartTracking}), so the record is already
 * here. Cancelling on the client also stops the interaction packet ever being
 * sent, so the server never sees a mount attempt to refuse.
 *
 * <p>{@link EventPriority#HIGHEST} is load-bearing: {@code TransferPaperHandler}
 * cancels every ordinary interaction on a branded horse at default priority, and
 * a branded horse - the cowboy's whole string - is the main thing this exists
 * to look at. Running first is what gets past it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseInfoInteraction {

    private HorseInfoInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractHorse horse)) {
            return;
        }
        if (!event.getEntity().isSecondaryUseActive() || !event.getItemStack().isEmpty()) {
            return; // an empty hand only - a paper, a name tag or a carrot all mean something else
        }
        if (horse.isTamed()) {
            return; // vanilla's inventory, and the i button on it - see the class note
        }
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null) {
            return; // no record yet; let the click do whatever it would have done
        }
        Minecraft.getInstance().setScreen(new HorseInfoScreen(record, horse, null));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
