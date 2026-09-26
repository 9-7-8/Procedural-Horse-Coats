package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * <b>A horse stops being owned.</b> The one place a tamed animal becomes a wild
 * one, and the whole of what that means.
 *
 * <h2>Why it is its own class</h2>
 * There are two ways it happens now and they have nothing else in common. A
 * <b>freedom stick</b> is a player standing next to a horse deciding, which can
 * hand the tack back and say something. <b>Walking out of the realm</b> is the
 * horse being noticed, later, with nobody there - no player to give a saddle to
 * and nobody to tell. If each wrote its own version, the second would be the one
 * that quietly forgot to clear the temper, or the persistence, or the wild-spawn
 * mark that is the only reason a released horse ever finds a band.
 *
 * <h2>What "wild" is, exactly</h2>
 * Seven things, and every one of them matters:
 * <ul>
 *   <li><b>Untamed, unowned, no temper</b> - vanilla's three, plus
 *       {@code HorseRecords.setOwner(null)} so the mod's own papers agree with
 *       the entity. A horse whose record still names an owner is one the browser
 *       lists as yours and the realm treats as nobody's.</li>
 *   <li><b>Healed.</b> It is being turned out to live here; starting it on two
 *       hearts in a dimension with no healing gear in it is a slow death by
 *       bookkeeping.</li>
 *   <li><b>Nothing on its back.</b> Passengers off, lead dropped, tack off - a
 *       wild horse fails {@code canUseSlot(SADDLE)}, so a saddle left on one is
 *       a saddle destroyed.</li>
 *   <li><b>Persistent.</b> An {@code Animal} does not despawn in vanilla, but
 *       <i>the horse you left here is still here</i> is the promise this whole
 *       dimension makes and it should not rest on that.</li>
 *   <li><b>Marked as a wild spawn.</b> {@code HerdManager} only ever looks at
 *       horses carrying that mark, so without it a released horse stands alone
 *       forever in a field of bands - see {@link HorseRealmHerds}.</li>
 * </ul>
 *
 * <p>What is deliberately <b>not</b> touched: genotype, epigenome, name, breed,
 * pedigree, generation and bond. A released horse keeps everything that makes it
 * that horse, which is what makes releasing one into a shared field a gift
 * rather than a deletion - whoever tames it next gets the animal, the papers and
 * the name.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseRelease {

    private HorseRelease() {
    }

    /**
     * Turn {@code horse} out. Assumes every refusal has already been made - it
     * asks nothing and cannot fail.
     *
     * @param player whoever is present to be handed the tack, or {@code null}
     *               when nobody is; with nobody there the tack is dropped where
     *               the horse stands rather than deleted
     */
    public static void makeWild(ServerLevel level, Horse horse, @Nullable Player player) {
        returnTack(level, horse, player);
        horse.ejectPassengers();
        if (horse.isLeashed()) {
            horse.dropLeash();
        }
        horse.setHealth(horse.getMaxHealth());
        horse.setTamed(false);
        horse.setOwner(null);
        horse.setTemper(0);
        HorseRecords.setOwner(horse, null);
        horse.setPersistenceRequired();
        horse.getPersistentData().putBoolean(BreedSpawnHandler.WILD_SPAWN_KEY, true);
        level.playSound(null, horse.blockPosition(), SoundEvents.HORSE_BREATHE,
                SoundSource.NEUTRAL, 0.8F, 1.1F);
    }

    /**
     * Take the saddle and body armour off and hand them back. Into the player's
     * inventory, or at their feet if it is full - and if there is no player,
     * onto the ground where the horse is standing, which is the only honest
     * answer: the gear is theirs and the realm does not destroy things.
     */
    private static void returnTack(ServerLevel level, Horse horse, @Nullable Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.SADDLE, EquipmentSlot.BODY}) {
            ItemStack worn = horse.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            horse.setItemSlot(slot, ItemStack.EMPTY);
            if (player != null) {
                if (!player.getInventory().add(worn.copy())) {
                    player.drop(worn.copy(), false);
                }
            } else {
                horse.spawnAtLocation(level, worn.copy());
            }
        }
    }
}
