package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Dev-only: <b>right-click a horseman with a clock to promote him one tier.</b>
 *
 * <h2>Why</h2>
 * The horseman's interesting trades are at tiers 4 and 5 - the random gene
 * carrots and the research papers - and the honest route to them is several
 * hundred emeralds of trading, per horseman, per test world. That is not a test,
 * it is an afternoon. Five clicks with the clock that is already in the test
 * world's hotbar gets the same villager to Master, and the thing actually under
 * test (does the tier's {@code trade_set} parse, does a bought carrot name a
 * real gene) is reached in seconds.
 *
 * <p>The clock because it is already in the debug hotbar, has no vanilla
 * interaction with a villager to shadow, and reads as "skip ahead in time".
 * It is not consumed.
 *
 * <h2>Why it needs an access transformer</h2>
 * {@code Villager.increaseMerchantCareer} is private, and the public routes are
 * both dead ends: {@code updateTrades} is protected, and {@code getOffers} only
 * rebuilds a <i>null</i> offer list, which nothing outside the class can make it
 * be. So raising the level without it leaves a Master horseman still offering
 * Novice trades. One AT line buys the one call that does both halves properly,
 * and it is reached from nowhere but this class.
 *
 * <p>Cancels the interaction so the trade screen does not open on the same
 * click - otherwise you would be looking at the old offers at the moment they
 * are replaced. Click again to trade.
 */
@EventBusSubscriber
public final class DebugHorsemanLevelUp {

    private DebugHorsemanLevelUp() {
    }

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!DebugAnnounce.enabled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(Items.CLOCK) || !(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (!villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
            return;
        }

        // Cancelled on both sides, so the client does not open a trade screen
        // against offers the server is about to replace.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        int current = villager.getVillagerData().level();
        if (!VillagerData.canLevelUp(current)) {
            DebugAnnounce.say(level, "Horseman",
                    villager.getName().getString() + " is already tier " + current + " (Master)",
                    ChatFormatting.GRAY);
            return;
        }

        villager.increaseMerchantCareer(level);
        int now = villager.getVillagerData().level();
        // Keep the XP consistent with the tier, or the merchant screen's progress
        // bar reads as a Master who has never traded - which is exactly what he is,
        // but it looks like a bug rather than a shortcut.
        villager.setVillagerXp(VillagerData.getMinXpPerLevel(now));

        DebugAnnounce.say(level, "Horseman",
                villager.getName().getString() + " promoted to tier " + now,
                ChatFormatting.AQUA);
    }
}
