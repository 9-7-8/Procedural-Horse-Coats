package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Yes/no facts about a horse's genome, parsed once per genome rather than once per tick</b> (2026-10-02).
 *
 * <p>The stress run's profile put {@code LycanthropyHandler.onTick} at 12.9% of all server time and
 * {@code SunSensitivityHandler.onTick} at 7.4% with a thousand ordinary horses - neither gene carried by any of them.
 * Both parsed the full genetic code (and lycanthropy the full epigenome) of every horse on every check interval,
 * only to find the locus wild-type. These facts are what those checks need first; each is read off one parse, kept
 * against the code it came from, and re-derived only when the code changes - the pattern
 * {@code GeneAbilityHandler.resolve} uses for the ability list, with the same size cap.
 */
final class GenomeFacts {

    private GenomeFacts() {
    }

    /** What the checks need; {@code code} is the genetic code they were read from. */
    private record Facts(String code, boolean lycanCarrier, boolean sunSensitive) {
    }

    private static final Map<UUID, Facts> CACHE = new ConcurrentHashMap<>();

    /** Does this horse carry any lycanthropy allele at all? A wild-type horse can never shift. */
    static boolean lycanCarrier(Horse horse) {
        Facts f = of(horse);
        return f != null && f.lycanCarrier();
    }

    /** Does daylight burn this horse? */
    static boolean sunSensitive(Horse horse) {
        Facts f = of(horse);
        return f != null && f.sunSensitive();
    }

    /** Is this foal one that dies at birth? Keyed on both codes, since traits read the epigenome too. */
    static boolean lethalAtBirth(Horse horse, HorseRecord record) {
        String code = record.geneticCode();
        String epi = record.epigenomeCode();
        Lethal l = LETHAL.get(horse.getUUID());
        if (l != null && l.code().equals(code) && l.epigenome().equals(epi)) {
            return l.lethal();
        }
        boolean lethal;
        try {
            lethal = HorseRecords.traitsOf(record).viability()
                    == com.example.horsegenetics.common.trait.Viability.LETHAL_AT_BIRTH;
        } catch (RuntimeException bad) {
            lethal = false;
        }
        if (LETHAL.size() > 4096) {
            LETHAL.clear();
        }
        LETHAL.put(horse.getUUID(), new Lethal(code, epi, lethal));
        return lethal;
    }

    private record Lethal(String code, String epigenome, boolean lethal) {
    }

    private static final Map<UUID, Lethal> LETHAL = new ConcurrentHashMap<>();

    private static Facts of(Horse horse) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return null;
        }
        HorseRecord record = HorseRecords.of(horse);
        String code = record.geneticCode();
        Facts f = CACHE.get(horse.getUUID());
        if (f != null && f.code().equals(code)) {
            return f;
        }
        boolean lycan;
        boolean sun;
        try {
            Genotype gt = Genotype.parse(code);
            lycan = !wild(Genes.LYCAN, gt.pair(Genes.LYCAN));
            sun = Genes.SUN_SENSITIVITY.isSensitive(gt.pair(Genes.SUN_SENSITIVITY));
        } catch (RuntimeException bad) {
            lycan = false;
            sun = false;
        }
        if (CACHE.size() > 4096) {
            CACHE.clear();
        }
        f = new Facts(code, lycan, sun);
        CACHE.put(horse.getUUID(), f);
        return f;
    }

    private static boolean wild(Gene gene, AllelePair pair) {
        return pair == null || (pair.first() == gene.defaultAllele() && pair.second() == gene.defaultAllele());
    }
}
