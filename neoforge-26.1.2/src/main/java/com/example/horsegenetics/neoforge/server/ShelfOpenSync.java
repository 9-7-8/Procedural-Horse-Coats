package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * <b>Opening an Equine Research Shelf ticks "build a shelf".</b>
 *
 * <p>It also used to push the shelf's contents at the client, because they
 * were a set of gene keys rather than items in slots. They are slots now, and
 * the ordinary container sync carries them, so only the checklist tick is left.
 * Opening one is proof enough of having built one, and it needs no hook in the
 * block-placement path to say so.
 */
@EventBusSubscriber
public final class ShelfOpenSync {

    private ShelfOpenSync() {
    }

    @SubscribeEvent
    static void onOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof ResearchShelfMenu) {
            HorseProgress.complete(event.getEntity(), ProgressTask.BUILD_SHELF);
        }
    }
}
