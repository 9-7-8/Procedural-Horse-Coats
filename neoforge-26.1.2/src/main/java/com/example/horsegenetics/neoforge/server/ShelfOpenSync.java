package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * <b>Tell the client what the shelf holds, the moment it opens.</b>
 *
 * <p>An Equine Research Shelf's contents are a set of gene keys on the block
 * entity, not items in slots, so none of it reaches the client through the
 * ordinary container sync - the menu has three slots and the list is not one of
 * them. Every <i>change</i> pushes a {@code ShelfSyncPayload} from the menu
 * itself; this is the one push that has no change behind it, and without it a
 * freshly opened shelf draws as empty until you touch something.
 *
 * <p>It is an event rather than a line in the menu's constructor because the
 * menu is built before the player is listening to it: {@code openMenu} creates
 * the menu, <i>then</i> sends the open packet. Sending from the constructor
 * would arrive first and be dropped.
 */
@EventBusSubscriber
public final class ShelfOpenSync {

    private ShelfOpenSync() {
    }

    @SubscribeEvent
    static void onOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof ResearchShelfMenu shelf) {
            shelf.syncOnOpen();
            // Opening one is proof enough of having built one, and it needs no
            // hook in the block-placement path to say so.
            HorseProgress.complete(event.getEntity(), ProgressTask.BUILD_SHELF);
        }
    }
}
