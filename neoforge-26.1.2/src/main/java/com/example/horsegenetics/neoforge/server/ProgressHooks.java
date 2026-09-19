package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.HorseProgressData;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

/**
 * <b>The checklist hooks that had nowhere else to live.</b>
 *
 * <p>Most tasks are ticked from the handler that already owned the thing -
 * taming from the tame handler, foals from the breeding handler. These three
 * have no such handler: crafting a gene carrot happens in vanilla's crafting
 * code, trading with an equestrian happens in vanilla's merchant code, and a
 * player's checklist has to reach them on login before they have done anything
 * at all.
 */
@EventBusSubscriber
public final class ProgressHooks {

    private ProgressHooks() {
    }

    /**
     * Send the checklist on the way in. Without this a returning player's ticks
     * are all absent until the first task completes, which reads as having lost
     * them.
     */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel level
                && level.getServer() != null) {
            HorseProgressData.get(level.getServer()).sync(player);
        }
    }

    /**
     * The tasks that are about the crafting table rather than the horse.
     *
     * <p>Three of them used to live here - bundling hair, twisting a rope,
     * weaving a cloth. Those items are gone and so are their tasks; hair is now
     * taken raw by everything that wanted them.
     */
    @SubscribeEvent
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(ModItems.KNOWN_GENE_SPLICE_CARROT.get())) {
            HorseProgress.complete(event.getEntity(), ProgressTask.GENE_CARROT);
        } else if (event.getCrafting().is(ModItems.BLANK_TICKET.get())) {
            HorseProgress.complete(event.getEntity(), ProgressTask.CRAFT_BLANK_TICKET);
        }
    }

    /** Trading with any of the four <em>equestrians</em> - any other villager is not the task. */
    @SubscribeEvent
    static void onTrade(TradeWithVillagerEvent event) {
        if (event.getAbstractVillager() instanceof Villager villager
                && ModVillagerProfessions.isEquestrian(villager.getVillagerData().profession())) {
            HorseProgress.complete(event.getEntity(), ProgressTask.TRADE_EQUESTRIAN);
        }
    }
}
