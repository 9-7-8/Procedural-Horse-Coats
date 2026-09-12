package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Client-only: cancel the name-tag interaction on this side too, so vanilla
 * does not predict a mount while the server is opening the rename window.</b>
 *
 * <h2>Why this is a separate class</h2>
 * {@code server/HorseInteractionHandler} runs on both sides and already
 * cancels the name-tag interaction - but only when
 * {@code HorseRecords.hasRealRecord(horse)} says the horse is one of ours, and
 * that asks the <b>{@code HORSE_RECORD} attachment</b>. Attachments are not
 * synced: on the client the attachment is always the empty default, so the
 * check was always false there and the client never cancelled.
 *
 * <p>What the client did instead is what vanilla does when you right-click a
 * tamed horse holding an item, which is <b>ride it</b>. So the rename window
 * opened and the player was simultaneously put on the horse - which is the
 * whole of "name tags don't work". The stick and clock branches in that same
 * handler carry a comment saying to cancel on both sides for exactly this
 * reason; the name-tag branch above them never did.
 *
 * <p>The client knows perfectly well which horses are ours - it just keeps it
 * in {@link ClientHorseRecordCache} rather than in the attachment. It cannot be
 * asked from the shared handler, because loading a client class on a dedicated
 * server is a crash, so the question is asked here instead. The server still
 * re-checks and is still the authority on whether the window opens; this only
 * stops the client from doing something else in the meantime.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseNameTagClient {

    private HorseNameTagClient() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()
                || !(event.getTarget() instanceof Horse horse)
                || !event.getItemStack().is(Items.NAME_TAG)) {
            return;
        }
        // Only ours. A horse the client has no record for is left to vanilla,
        // so a name tag still works normally on anything this mod has not
        // claimed - and the server would not open the window for one either.
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null || !record.hasName()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
