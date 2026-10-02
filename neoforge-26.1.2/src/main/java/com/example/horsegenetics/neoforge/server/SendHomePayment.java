package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.SendHome;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The Send home button's price, against a real inventory.</b> The rules and
 * the wording are {@link SendHome}'s; this is the part that needs the game - the
 * registry lookup of the configured id, counting a pack, and taking from it.
 *
 * <p>Takes a {@link Player} rather than a {@code ServerPlayer} so the gametest
 * can drive it with a mock player's real inventory.
 */
public final class SendHomePayment {

    /** Ids already warned about, so a bad config line is one log line, not one per click. */
    private static final Set<String> WARNED = new HashSet<>();

    private SendHomePayment() {
    }

    /** The configured price, resolved: what to take, or nothing when the trip is free. */
    public record Charge(SendHome.Price price, @Nullable Item item) {

        public static final Charge FREE = new Charge(SendHome.Price.FREE, null);

        public boolean free() {
            return item == null || price.free();
        }

        public String itemName() {
            return item == null ? "" : new ItemStack(item).getHoverName().getString();
        }
    }

    /** The server config's price, resolved against the item registry. */
    public static Charge current() {
        return resolve(ServerConfig.sendHomePrice());
    }

    /**
     * An id that names no registered item is <b>free, with one warning</b> - the
     * treatment's rule, so a typo in a server config never crashes a world load
     * and never locks every player out of the button.
     */
    public static Charge resolve(SendHome.Price price) {
        if (price.free()) {
            return Charge.FREE;
        }
        Identifier id = Identifier.tryParse(price.itemId());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || new ItemStack(item).isEmpty()) {
            if (WARNED.add(price.itemId())) {
                HorseGenetics.LOGGER.warn("behaviour.send_home_payment_item \"{}\" is not a registered "
                        + "item; Send home is free until it is fixed", price.itemId());
            }
            return Charge.FREE;
        }
        return new Charge(price, item);
    }

    /** Does this player hold the whole price? Always true for a free trip or a creative player. */
    public static boolean canPay(Player player, Charge charge) {
        if (charge.free() || player.getAbilities().instabuild) {
            return true;
        }
        return held(player.getInventory(), charge.item()) >= charge.price().count();
    }

    /**
     * Take the price. Call only after {@link #canPay} said yes and the trip has
     * actually happened - a refusal must leave the pack as it was.
     */
    public static void take(Player player, Charge charge) {
        if (charge.free() || player.getAbilities().instabuild) {
            return;
        }
        Inventory inventory = player.getInventory();
        int owed = charge.price().count();
        for (int slot = 0; slot < inventory.getContainerSize() && owed > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(charge.item())) {
                int taken = Math.min(owed, stack.getCount());
                stack.shrink(taken);
                owed -= taken;
            }
        }
        inventory.setChanged();
    }

    private static int held(Inventory inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * {@link SendHome#alternativesLine} with the names read off the live items,
     * so a renamed ticket is renamed in the sentence too.
     */
    public static String alternatives() {
        List<String> names = List.of(
                new ItemStack(ModItems.BASIC_TICKET.get()).getHoverName().getString(),
                new ItemStack(ModItems.HOLDING_PEN_TICKET.get()).getHoverName().getString(),
                new ItemStack(ModItems.ENDER_WHISTLE.get()).getHoverName().getString());
        return SendHome.alternativesLine(names);
    }
}
