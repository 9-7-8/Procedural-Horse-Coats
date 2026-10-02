package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Undeath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>{@link Undeath#kindOf} for a live horse</b>, cached on its genetic code.
 *
 * <p>The callers are hot: the "undead" mob group is asked of every candidate in an
 * aura's scan, and a horse's voice every time it makes a sound. A record's
 * genotype is parsed from its code on every read, so the answer is kept per code
 * string - two horses with one code share it, and the map is simply cleared when
 * it grows past {@link #CAP}, which no real world reaches between restarts.
 */
public final class UndeadHorses {

    private static final int CAP = 4096;
    private static final Map<String, Undeath.Kind> BY_CODE = new ConcurrentHashMap<>();

    private UndeadHorses() {
    }

    /** Which undead {@code entity} is - {@link Undeath.Kind#NONE} for anything not a recorded {@link Horse}. */
    public static Undeath.Kind kindOf(LivingEntity entity) {
        if (!(entity instanceof Horse horse)) {
            return Undeath.Kind.NONE;
        }
        String code;
        try {
            code = HorseRecords.of(horse).geneticCode();
        } catch (RuntimeException e) {
            return Undeath.Kind.NONE;
        }
        if (code == null || code.isEmpty()) {
            return Undeath.Kind.NONE;
        }
        Undeath.Kind cached = BY_CODE.get(code);
        if (cached != null) {
            return cached;
        }
        Undeath.Kind kind;
        try {
            kind = Undeath.kindOf(Genotype.parse(code));
        } catch (RuntimeException e) {
            kind = Undeath.Kind.NONE;
        }
        if (BY_CODE.size() > CAP) {
            BY_CODE.clear();
        }
        BY_CODE.put(code, kind);
        return kind;
    }

    public static boolean isUndead(LivingEntity entity) {
        return kindOf(entity) != Undeath.Kind.NONE;
    }
}
