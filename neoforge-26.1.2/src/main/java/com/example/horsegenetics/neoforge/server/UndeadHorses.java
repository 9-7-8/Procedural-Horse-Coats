package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Undeath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>{@link Undeath#kindOf} for a live horse</b>, read off {@link GenomeFacts}.
 *
 * <p>The callers are hot: the "undead" mob group is asked of every candidate in an
 * aura's scan, and a horse's voice every time it makes a sound. A record's
 * genotype is parsed from its code on every read, so the answer is kept per horse.
 * It was a map keyed on the genetic code string itself (#200): up to 4096 keys of
 * several KB each, a full string compare on every hit against a reloaded horse's
 * new String, and a wholesale clear at the cap. GenomeFacts keeps one entry per
 * loaded horse and drops it when the horse leaves.
 */
public final class UndeadHorses {

    private UndeadHorses() {
    }

    /** Which undead {@code entity} is - {@link Undeath.Kind#NONE} for anything not a recorded {@link Horse}. */
    public static Undeath.Kind kindOf(LivingEntity entity) {
        if (!(entity instanceof Horse horse)) {
            return Undeath.Kind.NONE;
        }
        try {
            return GenomeFacts.undeadKind(horse);
        } catch (RuntimeException e) {
            return Undeath.Kind.NONE;
        }
    }

    public static boolean isUndead(LivingEntity entity) {
        return kindOf(entity) != Undeath.Kind.NONE;
    }
}
