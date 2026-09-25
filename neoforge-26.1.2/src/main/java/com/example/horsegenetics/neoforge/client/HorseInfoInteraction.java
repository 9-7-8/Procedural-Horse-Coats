package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * this bails out below and the click goes on to vanilla. <kbd>E</kbd> from the
 * saddle now opens this screen too ({@link HorseInfoKeyHandler}), so on a horse
 * the vanilla screen is not reached at all; on the other equines it still is,
 * and its {@code i} button ({@link HorseScreenHooks}) still comes back here.
 *
 * <p><b>An empty hand only.</b> A name tag, a lead, food, a research paper or a
 * carrot all mean something specific on a horse, and none of them should open a
 * window instead.
 *
 * <h2>Why the screen is client-side, and why the cancel is not</h2>
 * The screen needs nothing from the server: {@code ClientHorseRecordCache} is
 * filled for every horse the player tracks, owned or not
 * ({@code HorseGeneticsEventHandler.onStartTracking}), so the record is already
 * here.
 *
 * <p><b>This class used to claim that cancelling here also stopped the
 * interaction packet being sent. It does not.</b>
 * {@code MultiPlayerGameMode.interact} sends {@code ServerboundInteractPacket}
 * on its first line, <i>before</i> either entity event is fired, so the server
 * hears the click no matter what is decided here - and answered it by opening
 * the vanilla horse inventory over the top of this screen. Cancelling on this
 * side is still worth doing, because it stops the client predicting a mount and
 * flickering, but the half that actually keeps this screen up is
 * {@code HorseInteractionHandler.onInfoGestureSpecific} on the server. The two
 * must agree about which clicks they take; see that method.
 *
 * <p>{@link EventPriority#HIGHEST} is load-bearing: {@code TransferPaperHandler}
 * cancels every ordinary interaction on a branded horse at default priority, and
 * a branded horse - the cowboy's whole string - is the main thing this exists
 * to look at. Running first is what gets past it.
 *
 * <h2>Both entity events, not just one</h2>
 * A right-click aimed at a mob reaches <b>{@code EntityInteractSpecific} first
 * and {@code EntityInteract} second</b>, and {@code Minecraft.startUseItem}
 * only tries the second when the first did not consume the click. Handling
 * {@code EntityInteract} alone therefore never ran: the uncancelled
 * {@code EntityInteractSpecific} let the {@code INTERACT_AT} packet through,
 * the server answered it with {@code AbstractHorse.mobInteract}, and the
 * <b>vanilla horse inventory opened over the top of this screen</b> - the whole
 * takeover described above silently did nothing on every horse. See
 * {@link CustomHorseSpawnEggClient}, which documents the same ordering and is
 * where the four-handler shape comes from: {@code PlayerInteractEvent} itself
 * is not cancellable in this SDK, so the two events cannot share one handler.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseInfoInteraction {

    private HorseInfoInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (openInfo(event.getTarget(), event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (openInfo(event.getTarget(), event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Opens the screen and reports whether the click was claimed. */
    private static boolean openInfo(Entity target, Player player, ItemStack held) {
        if (!(target instanceof AbstractHorse horse)) {
            return false;
        }
        if (!held.isEmpty()) {
            return false; // an empty hand only - a paper, a name tag or a carrot all mean something else
        }
        if (!player.isSecondaryUseActive()) {
            return false; // a plain click still mounts, exactly as vanilla does
        }
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null) {
            return false; // no record yet; let the click do whatever it would have done
        }
        Minecraft.getInstance().setScreen(new HorseInfoScreen(record, horse, null));
        return true;
    }
}
