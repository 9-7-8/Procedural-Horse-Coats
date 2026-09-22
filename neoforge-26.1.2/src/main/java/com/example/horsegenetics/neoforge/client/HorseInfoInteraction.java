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
 * <b>Sneak and use on a horse to read its genes.</b> An empty hand, the sneak
 * key held, on any horse at all - wild, tamed, yours or a stranger's.
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
 * <h2>The gesture is vanilla's, and the plain click is left alone</h2>
 * This briefly took the <i>plain</i> right-click instead, behind a setting that
 * defaulted to on, and the owner rejected it: that click mounts, mounting is
 * muscle memory, and a window opening where a saddle was expected is wrong
 * every time. So the plain click is vanilla's again - it mounts - and the
 * screen is on the gesture vanilla already spends on "open this animal's
 * window".
 *
 * <p>That gesture was vanilla's horse inventory on a tamed horse, and this
 * takes it over - on the owner's call, and it costs nothing, because
 * <b>everything that screen holds for a horse is on this one</b>: saddle and
 * body armour are {@code HorseTackSlot.SADDLE} and {@code BARDING} on the Gear
 * tab, and a horse has no chest - {@code AbstractHorse.getInventoryColumns()}
 * is zero for one. A donkey or a mule, which does have
 * one, never reaches here at all: this mod writes a record only for a
 * {@link net.minecraft.world.entity.animal.equine.Horse}, and no record means
 * this bails out below and the click goes on to vanilla. The vanilla screen is
 * still one <kbd>E</kbd> away from the saddle, and its {@code i} button
 * ({@link HorseScreenHooks}) still comes back here.
 *
 * <p><b>An empty hand only.</b> A name tag, a lead, food, a research paper or a
 * carrot all mean something specific on a horse, and none of them should open a
 * window instead.
 *
 * <h2>Why client-side only</h2>
 * The screen needs nothing from the server: {@code ClientHorseRecordCache} is
 * filled for every horse the player tracks, owned or not
 * ({@code HorseGeneticsEventHandler.onStartTracking}), so the record is already
 * here. Cancelling on the client also stops the interaction packet ever being
 * sent, so the server never opens the vanilla inventory underneath this one.
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
        if (!event.getEntity().isSecondaryUseActive()) {
            return; // a plain click still mounts, exactly as vanilla does
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
