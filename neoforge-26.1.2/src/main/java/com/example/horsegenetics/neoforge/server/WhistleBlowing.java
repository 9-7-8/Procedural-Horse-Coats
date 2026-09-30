package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.WhistleSelection;
import com.example.horsegenetics.common.care.WhistleSelection.Candidate;
import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.EnderWhistleItem;
import com.example.horsegenetics.neoforge.item.WhistleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <b>One key blows every whistle you are carrying</b> that is not degenerate to
 * another - the server half, which is all of it that matters.
 *
 * <h2>The client is not asked which</h2>
 *
 * <p>{@code BlowWhistlesPayload} carries nothing at all. The scan, the
 * selection and the cooldowns are all here, on the sender's real inventory,
 * because an inventory index arriving from a client is a claim about a
 * container rather than a fact about one. The worst a tampered client can do is
 * press its own key.
 *
 * <h2>The rule is in common/, the world is here</h2>
 *
 * <p>{@link WhistleSelection} decides which whistles are worth blowing -
 * largest area whistle only, every distinct binding, nothing twice - and it is
 * pure, so it has a unit test. This class does what needs a world: finding the
 * stacks, honouring cooldowns, calling the items, and writing one line of chat.
 *
 * <h2>Two ways to call one horse</h2>
 *
 * <p>A player can hold an echo whistle <em>and</em> an ender whistle bound to a
 * horse standing twenty blocks away. Both would call it. So the area whistle is
 * blown first and reports which horses it dealt with, and an ender whistle
 * bound to one of those is skipped - otherwise that horse is teleported twice
 * and chimes twice, which reads as the key being broken.
 *
 * <p><b>Cooldowns are the items' own.</b> Every whistle is blown by calling the
 * same method the right-click calls, so the key cannot be a way round a
 * cooldown, and a whistle already cooling down is skipped rather than reset.
 */
public final class WhistleBlowing {

    private WhistleBlowing() {
    }

    /** The key was pressed. Everything follows from the sender's own pack. */
    public static void blowAll(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        List<Candidate> carried = new ArrayList<>();
        Inventory inventory = player.getInventory();
        // getContainerSize() spans the main inventory, the hotbar and the
        // equipment slots, which is where the offhand lives. Armour slots come
        // with it and cannot hold a whistle, so they cost a null check each.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)) {
                continue;
            }
            if (stack.getItem() instanceof WhistleItem area) {
                carried.add(new Candidate(stacks.size(), area.radius(), null));
                stacks.add(stack);
            } else if (stack.getItem() instanceof EnderWhistleItem) {
                BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
                carried.add(new Candidate(stacks.size(), 0,
                        bound == null ? null : bound.id().toString()));
                stacks.add(stack);
            }
        }

        List<Candidate> picked = WhistleSelection.pick(carried);
        if (picked.isEmpty()) {
            player.sendSystemMessage(Component.literal(carried.isEmpty()
                    ? "You have no whistles to blow."
                    : "Your whistles are still catching their breath."));
            return;
        }

        int called = 0;
        Set<UUID> handled = new HashSet<>();
        List<String> answered = new ArrayList<>();
        for (Candidate pick : picked) {
            ItemStack stack = stacks.get(pick.slot());
            if (pick.isArea() && stack.getItem() instanceof WhistleItem area) {
                WhistleItem.Blown blown = area.blow(player, stack);
                called += blown.total();
                handled.addAll(blown.handled());
                continue;
            }
            BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
            // Already brought in by the area whistle a moment ago - calling it
            // again would teleport it a second time and chime for it twice.
            if (bound == null || handled.contains(bound.id())) {
                continue;
            }
            if (EnderWhistleItem.blow(player, stack) != null) {
                answered.add(bound.name() == null || bound.name().isBlank()
                        ? "your bound horse" : bound.name());
            }
        }

        player.sendSystemMessage(Component.literal(line(called, answered)));
    }

    /**
     * One line for the whole press, however many whistles it took. Built here
     * rather than left to each item so the player gets a sentence instead of a
     * column of them.
     *
     * <p>The ender whistle's own half is phrased as <em>answered</em> rather
     * than counted with the rest, because its horse may still be a chunk load
     * away when this line is written - {@code EnderWhistleCalls} follows up with
     * its own message when a far horse actually arrives, or does not.
     */
    private static String line(int called, List<String> answered) {
        String near = called == 0
                ? "No tamed horses of yours nearby"
                : "Whistled " + called + " horse" + (called == 1 ? "" : "s") + " to you";
        if (answered.isEmpty()) {
            return near + ".";
        }
        return near + ", and " + joined(answered) + " answered from far away.";
    }

    /** "Biscuit", "Biscuit and Juniper", "Biscuit, Juniper and Marrow". */
    private static String joined(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1))
                + " and " + names.get(names.size() - 1);
    }
}
