package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * <b>What happens to the lead when this mod moves a horse out from under it.</b>
 *
 * <p>Every way this mod teleports a horse has to untie it first - a leash across
 * a teleport is a leash to nowhere - and vanilla's {@link Mob#dropLeash()} does
 * that by spawning an {@code Items.LEAD} <i>at the horse's old position</i>. For
 * vanilla that is right: the lead broke where the mob was. For a whistle or a
 * ticket it is a small theft. The horse arrives beside you and the lead is left
 * wherever it was standing - a hundred blocks away for an echo whistle, and in
 * <b>another dimension</b> for an ender whistle or an interdimensional ticket,
 * which is the case where it is simply gone.
 *
 * <p>So: untie without the ground drop ({@link Mob#removeLeash()}), and hand the
 * lead to the player who caused the move, falling back to dropping it at their
 * feet when their inventory is full. From
 * <a href="../../../../../../../../wiki/rider-comfort.html#roadmap">rider comfort</a>,
 * whose source for it is Horse Tweaks; gated on
 * {@code behaviour.leads_return} like the rest of that list.
 *
 * <p><b>Not applied to the two portal paths.</b> {@code PortalEventHandler} and
 * {@code HorsePortalManager} untie a horse that walked into a portal itself,
 * with its holder standing right there, so the lead lands at the player's feet
 * already and vanilla's behaviour is the legible one. They are deliberately left
 * on {@code dropLeash()}.
 */
public final class HorseLeads {

    private HorseLeads() {
    }

    /**
     * Untie {@code horse} before a teleport, giving the lead to {@code player}.
     *
     * <p>Safe to call on an unleashed horse - it does nothing - so callers can
     * drop their own {@code isLeashed()} guard rather than keep two of them.
     *
     * @param player the player who caused the move, and who gets the lead back
     */
    public static void untieFor(Mob horse, Player player) {
        if (!horse.isLeashed()) {
            return;
        }
        if (player == null || !ServerConfig.leadsReturn()) {
            horse.dropLeash();      // vanilla: the lead falls where the horse was
            return;
        }
        horse.removeLeash();        // untie with no ground drop...
        giveOrDrop(player, new ItemStack(Items.LEAD));   // ...and hand it over
    }

    /** The same give-or-drop {@code HorseDietHandler} uses for a bucket remainder. */
    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
