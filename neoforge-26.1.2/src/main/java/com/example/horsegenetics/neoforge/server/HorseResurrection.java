package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * <b>Putting a dead horse back on its feet.</b> The one place that happens, so
 * that {@link HorseResurrectCommand} placing a horse and a resurrection egg
 * being used produce the same animal.
 *
 * <p>Almost all of the work is {@link HorseStasisHandler}'s already: a
 * {@link StasisSnapshot} is a whole entity tag and restoring one is a solved
 * problem with three proven callers. What this class adds is everything that is
 * different about a snapshot taken from a horse that <b>died</b> rather than one
 * taken from a horse that was bottled, and all of it is the same idea said four
 * times - <i>whatever killed it must not still be on it.</i>
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseResurrection {

    private HorseResurrection() {
    }

    /**
     * Bring the horse a snapshot holds back to life at {@code pos}.
     *
     * @return the live horse, or {@code null} if the tag could not be read or
     *         the level refused it - in which case nothing has happened and the
     *         caller must not consume whatever it was spending
     */
    public static @Nullable Horse raise(ServerLevel level, Vec3 pos, float yRot, StasisSnapshot snapshot) {
        clearCorpse(level.getServer(), snapshot);
        Horse horse = HorseStasisHandler.restore(level, snapshot);
        if (horse == null) {
            return null;
        }
        revive(horse);
        Horse placed = HorseStasisHandler.place(level, horse, pos, yRot);
        if (placed == null) {
            return null;
        }
        // place() writes a "left stasis" sighting, which clears the dead mark's
        // neighbours but not the mark itself - HorseWhereabouts refuses to
        // un-kill a horse on a sighting, on purpose, so that a stale one cannot.
        // This is the one caller entitled to say otherwise.
        HorseWhereabouts.get(level.getServer())
                .seenAlive(placed.getUUID(), level.dimension(), placed.blockPosition());
        return placed;
    }

    /**
     * <b>Undo the death, not just the absence.</b>
     *
     * <p>A stasis snapshot is of a healthy horse somebody chose to bottle. This
     * one is of an animal in the instant it died, and the tag is honest about
     * that: zero health, on fire, out of air, and carrying whatever effect
     * emptied the health bar. Restored as-is it would stand up and die again
     * inside a second, which reads as the command not working.
     *
     * <p>Called before the horse is placed, so it never exists in the world in
     * the state it died in.
     */
    private static void revive(Horse horse) {
        // Gear. It is in the tag because the snapshot is taken before vanilla
        // drops it, and it is also on the ground where the horse fell - so
        // keeping it here would mint a second saddle. Cleared through the
        // entity's own slots rather than by deleting NBT keys, because the keys
        // move between versions and a rename would fail silently as a duplicate
        // rather than as a crash.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            horse.setItemSlot(slot, ItemStack.EMPTY);
        }
        // The chests on its flanks, for the same reason and more of it: they
        // and everything in them dropped where it fell (HorsePackHandler.onDrops),
        // and a horse that came back still carrying them would be a way to
        // copy two chests of anything.
        HorsePackHandler.strip(horse);
        // Whatever killed it.
        horse.removeAllEffects();
        horse.clearFire();
        horse.setAirSupply(horse.getMaxAirSupply());
        horse.setLastHurtByMob(null);
        horse.resetFallDistance();
        // And the health bar last, from the genotype rather than from the tag -
        // the resolved maximum for this horse's alleles, filled. The same call
        // a newborn foal gets, for the same reason: the number on the tag is
        // zero and the number the genes say is the truth.
        HorseRecords.applyTraitsToEntity(horse, HorseRecords.of(horse), true);
    }

    /**
     * <b>Get the body out of the way first.</b>
     *
     * <p>A horse does not leave the world the instant it dies - it falls over
     * for twenty ticks and is removed at the end of that, and for the whole of
     * that second the corpse still holds the UUID. So the most likely
     * resurrection there is, the one an operator types while the owner is still
     * shouting about it, is the one that lands inside the death animation and is
     * refused by {@code PersistentEntitySectionManager} with
     * <i>"UUID of added entity already exists"</i> - a warning in the log and a
     * null return here, reported to the operator as a horse that could not be
     * read. It looked like the snapshot was broken; nothing was broken but the
     * timing.
     *
     * <p>Discarding is safe because of what is being discarded: an entity that
     * is not {@link net.minecraft.world.entity.Entity#isAlive()}. Its loot has
     * already dropped - {@code die()} runs before any of this - so nothing is
     * destroyed but the animation, and the alternative is refusing to bring the
     * horse back until it has finished falling over. A living horse with this
     * id is a different matter entirely and is left alone for
     * {@link #alreadyAlive} to refuse.
     *
     * <p>Every level, not just this one: the body is wherever it died, and the
     * horse is being raised wherever the operator is.
     */
    private static void clearCorpse(MinecraftServer server, StasisSnapshot snapshot) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity body = level.getEntity(snapshot.horseId());
            if (body != null && !body.isAlive()) {
                body.discard();
            }
        }
    }

    /**
     * Is this horse <b>already standing somewhere</b>? Two entities sharing one
     * UUID is the failure the whole snapshot design exists to avoid, and a
     * resurrection egg is an item - which in creative is a keystroke away from
     * being two items.
     *
     * <p>Alive, specifically - a corpse still mid-fall is not this, and
     * {@link #clearCorpse} deals with it.
     */
    public static boolean alreadyAlive(MinecraftServer server, StasisSnapshot snapshot) {
        return HorseStasisHandler.alreadyLoose(server, snapshot);
    }
}
