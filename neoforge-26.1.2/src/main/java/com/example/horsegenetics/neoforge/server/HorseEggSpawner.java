package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Puts a horse in the world from an item - the one place the two spawn eggs and
 * the creative editor agree on how that is done.
 *
 * <p>The order matters and is easy to get wrong: the {@link HorseRecord} has to
 * be applied <b>before</b> the entity joins the level, or
 * {@code HorseGeneticsEventHandler} sees a horse with no record on its join
 * event and rolls a random genotype over the top of the one that was asked for.
 * That bug is invisible - you get a horse, just not the one you paid for - which
 * is exactly why the three callers share this rather than each remembering.
 */
public final class HorseEggSpawner {

    private HorseEggSpawner() {
    }

    /**
     * A <b>foundation horse</b> of {@code breed}: an ordinary breed founder,
     * rolled from the breed's own pool the same way a wild herd member is, and
     * labelled with that breed.
     *
     * <p>Foundation is the point of a breed spawn egg. It is not a horse
     * somebody bred - it has no pedigree and no drift behind it, it is the
     * breed at its standard - so what it is worth is that it is a clean place
     * to start a line from, not that it is a good horse.
     */
    public static @Nullable Horse spawnFounder(ServerLevel level, Vec3 pos, float yRot, Breed breed) {
        Horse horse = create(level, pos, yRot);
        if (horse == null) {
            return null;
        }
        HorseRecords.apply(horse, HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), breed));
        level.addFreshEntity(horse);
        return horse;
    }

    /**
     * The exact horse an item recorded - the preset egg the creative editor
     * writes. Genotype and epigenome both come off the item, so the horse that
     * appears is the horse that was on screen when the egg was made.
     */
    public static @Nullable Horse spawnPreset(ServerLevel level, Vec3 pos, float yRot,
                                              StoredGenome stored, boolean baby) {
        Genome genome = new Genome(
                Genotype.parse(stored.genotypeCode()),
                stored.epigenomeCode().isEmpty()
                        ? Epigenome.random(new NeoRng(level.getRandom()))
                        : Epigenome.parse(stored.epigenomeCode()));

        Horse horse = create(level, pos, yRot);
        if (horse == null) {
            return null;
        }
        if (baby) {
            horse.setBaby(true);
        }
        HorseRecord record = HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), genome);
        String breed = stored.breed();
        if (!breed.isEmpty() && !breed.equals(BreedLineage.FERAL_ID)) {
            record = record.withBreed(breed);
        }
        HorseRecords.apply(horse, record);
        level.addFreshEntity(horse);
        return horse;
    }

    /** The display name of a breed token, for a tooltip or a chat line. */
    public static String describe(String breedId) {
        return Breeds.displayName(breedId);
    }

    private static @Nullable Horse create(ServerLevel level, Vec3 pos, float yRot) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (horse == null) {
            return null;
        }
        horse.snapTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        // An egg is worth real money and may be the only one of its breed the
        // player will ever see; it must not wander off and de-spawn.
        horse.setPersistenceRequired();
        return horse;
    }
}
