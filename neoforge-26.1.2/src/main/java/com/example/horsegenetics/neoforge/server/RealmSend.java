package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * <b>An operator moves horses into the horse realm</b> - the one move behind
 * {@code /horserealm send} and {@code /horsesweep ... realm}.
 *
 * <p>An <b>owned</b> horse arrives as it left, the same as one led in through a
 * portal: still its owner's, tack and all, and from then on the realm's own rules
 * apply to it - which means that unless its owner is in the realm too,
 * {@link HorseRealmFeral} turns it out within a few seconds. The command says so.
 * A horse nobody owns - a <b>wild</b> one,
 * or <b>cowboy stock</b> - is released on the way, as {@link WildTurnover} hands a
 * wild horse off: brand cleared and out of its dealer's herd, then
 * {@link HorseRelease#makeWild} before the move (with nobody to hand tack to it
 * drops where the horse stood), so it lands as one of the field's own.
 *
 * <p>Horses are put down on a grid south of the arrival spot, two blocks apart and
 * eight to a row, so a sweep of a hundred does not stack a hundred horses in one
 * block. The grid starts two blocks east of the field's west edge, which keeps it
 * off the column of water pools at x 0-1.
 *
 * <p><b>Not verified in-game.</b> The move is {@link WildTurnover}'s (a portal
 * ticket, from outside an entity tick here - vanilla's {@code /tp} moves entities
 * between dimensions from the same place).
 */
final class RealmSend {

    private static final int PER_ROW = 8;
    private static final int SPACING = 2;

    private RealmSend() {
    }

    /** Does this horse belong to nobody - untamed, or a dealer's stock? */
    static boolean releasedOnArrival(Horse horse) {
        return !horse.isTamed() || TransferPaperHandler.isBranded(horse);
    }

    /**
     * The dealer whose brand this horse wears, if they are loaded. Read directly rather
     * than through {@link CowboyHandler#ownerOf}, which clears the brand of a horse whose
     * dealer it cannot find - right for a dead dealer, wrong for one in an unloaded chunk.
     */
    static @Nullable Cowboy dealerOf(Horse horse, ServerLevel level) {
        var brand = horse.getExistingDataOrNull(ModAttachments.COWBOY_BRAND.get());
        if (brand == null || brand.cowboy().isEmpty()) {
            return null;
        }
        return level.getEntity(brand.cowboy().get()) instanceof Cowboy c && c.isAlive() ? c : null;
    }

    /**
     * <b>A branded horse a player has paid for.</b> Bought stock stays untamed and
     * branded until its transfer paper is redeemed, so it looks exactly like stock for
     * sale - moving or removing it would take a horse somebody owns the papers to. When
     * the dealer is not loaded nobody can say, so it counts as spoken for.
     */
    static boolean spokenFor(Horse horse, ServerLevel level) {
        if (!TransferPaperHandler.isBranded(horse)) {
            return false;
        }
        Cowboy dealer = dealerOf(horse, level);
        return dealer == null || dealer.hasSold(horse.getUUID());
    }

    /**
     * Move {@code horse} to slot {@code slot} of the arrival grid.
     *
     * @return the horse that arrived (across worlds, a new entity), or null when it did not move
     */
    static @Nullable Horse send(ServerLevel from, ServerLevel realm, Horse horse, int slot) {
        if (releasedOnArrival(horse)) {
            if (TransferPaperHandler.isBranded(horse)) {
                Cowboy dealer = dealerOf(horse, from);
                if (dealer != null) {
                    dealer.removeFromHerd(horse.getUUID());
                }
                CowboyHandler.clearBrand(horse);
            }
            HorseRelease.makeWild(from, horse, null);
        } else if (horse.isLeashed()) {
            horse.dropLeash();
        }
        Entity arrived = horse.teleport(new TeleportTransition(realm, spot(slot), Vec3.ZERO,
                horse.getYRot(), horse.getXRot(), Set.of(), TeleportTransition.PLACE_PORTAL_TICKET));
        return arrived instanceof Horse h ? h : null;
    }

    /** Where slot {@code n} of the arrival grid is. */
    static Vec3 spot(int n) {
        Vec3 at = HorseRealm.arrivalSpot();
        int row = n / PER_ROW;
        int col = n % PER_ROW;
        double x = Math.max(2.5, at.x - (PER_ROW / 2) * SPACING + col * SPACING);
        return new Vec3(x, at.y, at.z + 1 + row * SPACING);
    }
}
