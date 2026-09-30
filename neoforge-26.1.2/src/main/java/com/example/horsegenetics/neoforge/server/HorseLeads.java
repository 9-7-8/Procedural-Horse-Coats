package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

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

    // ---------------------------------------------------------------------
    // The other half: the leads VANILLA drops
    // ---------------------------------------------------------------------

    /**
     * <b>Remember who tied this lead on</b>, called from
     * {@code mixin/LeashPlacerMixin} every time a horse's leash data is
     * replaced.
     *
     * <p>Three cases, and the two that are <i>not</i> "record the player" are
     * the ones worth stating:
     *
     * <ul>
     *   <li><b>A player is the holder</b> - somebody just tied a lead on, or is
     *       walking the horse. Record them. Tying that same horse to a fence
     *       afterwards does not come through here at all
     *       ({@code Leashable.setLeashedTo} only calls {@code setLeashData} when
     *       there was no leash data yet, and takes a {@code setLeashHolder}
     *       branch otherwise), which is exactly what makes a knot remember the
     *       player who walked the horse to it.</li>
     *   <li><b>A non-player holder on a fresh leash</b> - a dispenser or another
     *       mod tying a horse straight to a knot, with no player in it anywhere.
     *       Forget whoever was recorded before, or a lead somebody else tied on
     *       would come back to them.</li>
     *   <li><b>A null holder, or null leash data</b> - leave the record alone.
     *       Null data is what vanilla writes <i>just before</i> it drops the
     *       lead, and clearing here would wipe the answer a moment before it is
     *       asked for. A null holder inside real data is a leash being restored
     *       from a save, where the holder is resolved a tick later; clearing
     *       there would lose the placer of every fence-tied horse on every
     *       world load.</li>
     * </ul>
     */
    public static void rememberPlacer(Mob horse, Leashable.@Nullable LeashData data) {
        if (data == null || data.leashHolder == null) {
            return;
        }
        horse.setData(ModAttachments.LEASH_PLACER.get(),
                data.leashHolder instanceof Player player
                        ? Optional.of(player.getUUID())
                        : Optional.empty());
    }

    /** Who tied the lead currently (or most recently) on this horse. */
    public static Optional<UUID> placerOf(Mob horse) {
        return horse.getData(ModAttachments.LEASH_PLACER.get());
    }

    /**
     * <b>Vanilla is about to drop a lead on the ground; send it to the player
     * who tied it on instead.</b> Called from {@code mixin/LeashDropMixin},
     * which cancels the ground drop when this returns {@code true}.
     *
     * <p>It covers every way vanilla loses a lead, because it sits on the one
     * line all of them funnel through - a leash that snapped at range, a fence
     * knot broken by hand, a holder that stopped existing. The seam is
     * {@code Entity.spawnAtLocation(ServerLevel, ItemLike)} rather than
     * {@code Leashable.dropLeash}: the two are the <i>only</i> callers that pass
     * it {@code Items.LEAD} in the whole game, and {@code dropLeash} is a
     * {@code private static} method on an interface, which is not somewhere a
     * mixin can reach.
     *
     * <p><b>Refuses more often than it accepts, on purpose.</b> No record, the
     * switch off, or a placer who is not on the server right now all return
     * {@code false} and let vanilla drop the lead where it always did. That is
     * the decided rule and it is the safe direction: the failure this must never
     * have is a lead that goes nowhere, and every path that cannot deliver one
     * hands it straight back to vanilla. Nothing is ever held for later.
     *
     * <p>The record is cleared on success only. A placer left on a horse whose
     * lead is already gone is never read - nothing asks unless a lead is being
     * dropped - and the next person to tie one on overwrites it.
     */
    public static boolean divertLeadDrop(Mob horse) {
        if (!ServerConfig.leadsReturn()) {
            return false;
        }
        UUID placerId = placerOf(horse).orElse(null);
        if (placerId == null || !(horse.level() instanceof ServerLevel level)) {
            return false;
        }
        // The whole server's list, not this level's: the decided rule is that
        // the lead reaches the placer wherever they are, another dimension
        // included. Offline is the fallback case, not a failure.
        return giveRecordedLeadTo(horse, level.getServer().getPlayerList().getPlayer(placerId));
    }

    /**
     * The half of {@link #divertLeadDrop} after the lookup: hand {@code placer}
     * the lead, but only if they are the one this horse's lead is recorded
     * against.
     *
     * <p><b>Separate from the lookup so that it can be tested.</b> A gametest
     * server has no players on it, so nothing a test can construct is findable
     * in {@code getPlayerList()} - the one helper that would be,
     * {@code makeMockServerPlayerInLevel}, is {@code @Deprecated(forRemoval)}
     * and really joins the player list, which fires this mod's join payloads
     * down a channel that does not exist and takes the run with it. So
     * {@code vanilla_lead_comes_back} drives this method and the single line
     * above it - the player-list lookup - is left to the in-game check.
     *
     * <p>The UUID is re-compared rather than trusted. A caller that had the
     * wrong player would otherwise hand somebody a lead that was never theirs,
     * and the check costs nothing.
     */
    public static boolean giveRecordedLeadTo(Mob horse, @Nullable Player placer) {
        if (placer == null || !placerOf(horse).filter(placer.getUUID()::equals).isPresent()) {
            return false;
        }
        horse.setData(ModAttachments.LEASH_PLACER.get(), Optional.empty());
        giveOrDrop(placer, new ItemStack(Items.LEAD));
        return true;
    }
}
