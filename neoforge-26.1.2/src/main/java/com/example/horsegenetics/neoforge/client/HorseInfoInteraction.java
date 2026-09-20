package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Use on a horse to read its genes.</b> A plain right-click with an empty
 * hand, on any horse at all, unless the player has turned that off - see
 * {@code ClientConfig.rightClickOpensInfo}, and the half of this class's note
 * below that was written when it was sneak-only.
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
 * <h2>Two rules, and a setting that chooses between them</h2>
 * <b>On</b> (the default): an empty-handed right-click on <i>any</i> horse
 * opens the screen. That is the whole of it - no sneaking, tamed or not.
 * <b>Off</b>: sneak and use, and only on a horse that is not tamed. That was
 * the original rule, and it stopped short of a tamed horse on purpose, because
 * sneak-and-use on one is how vanilla opens the saddle and armour slots and
 * taking it over would have cost the player the ability to tack up.
 *
 * <p>What made the first rule affordable is that <b>the screen now carries the
 * tack itself</b> - saddle and armour sit on its Overview tab
 * ({@code HorseTackSlot}) - so the vanilla inventory is no longer the only way
 * to a saddle, and the {@code i} button ({@link HorseScreenHooks}) is no longer
 * the only way to this screen. The one thing the click <i>did</i> own that the
 * screen has to give back is mounting: hence the Ride button, and hence it
 * being on the same tab.
 *
 * <p>Either way, <b>an empty hand only</b>. A name tag, a lead, food, a
 * research paper or a carrot all mean something specific on a horse, and none
 * of them should open a window instead.
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
        if (!event.getItemStack().isEmpty()) {
            return; // an empty hand only - a paper, a name tag or a carrot all mean something else
        }
        if (event.getEntity().isSecondaryUseActive()) {
            // Sneak and use is left exactly as it was, whatever the setting:
            // on a tamed horse it is vanilla's inventory, which has to stay
            // reachable, and on any other it is this screen.
            if (horse.isTamed()) {
                return;
            }
        } else if (!ClientConfig.rightClickOpensInfo()) {
            return; // plain clicks are not ours unless the player asked for that
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
