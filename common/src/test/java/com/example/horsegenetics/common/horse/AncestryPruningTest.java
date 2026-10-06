package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ancestry store forgets a departing horse only when nothing could ask for it again (#200): nobody's horse,
 * nobody's parent, and never a stallion who has covered - his foal may be born after he is gone, and since #202
 * the pregnancy carries only his id.
 */
class AncestryPruningTest {

    private static final Genome GENOME = Genome.of(Genotype.wildType(), new SeededRng(5L));

    private static UUID id(int n) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", n));
    }

    private static HorseRecord horse(int n, Integer dam, Integer sire) {
        return new HorseRecord(id(n), "H" + n, "Test", Optional.empty(),
                GENOME.genotypeCode(), GENOME.epigenomeCode(), Optional.empty(),
                dam == null ? Optional.empty() : Optional.of(id(dam)),
                sire == null ? Optional.empty() : Optional.of(id(sire)),
                Optional.empty(), Optional.empty(), 0, Optional.empty(), false, Optional.empty());
    }

    @Test
    void aChildlessWildHorseIsForgotten() {
        HorseRecord wild = horse(1, null, null);
        assertTrue(AncestryPruning.mayForget(wild, false, List.of(wild, horse(2, null, null))));
    }

    @Test
    void aParentIsKeptOnEitherSide() {
        HorseRecord dam = horse(1, null, null);
        HorseRecord sire = horse(2, null, null);
        List<HorseRecord> all = List.of(dam, sire, horse(3, 1, 2));
        assertFalse(AncestryPruning.mayForget(dam, false, all));
        assertFalse(AncestryPruning.mayForget(sire, false, all));
    }

    @Test
    void anyoneWhoseHorseItWasIsKept() {
        HorseRecord base = horse(1, null, null);
        List<HorseRecord> all = List.of(base);
        assertFalse(AncestryPruning.mayForget(base.withOwner(id(99)), false, all));
        assertFalse(AncestryPruning.mayForget(base.withTamedBy("alex"), false, all));
        assertFalse(AncestryPruning.mayForget(base.withBredBy("alex"), false, all));
    }

    @Test
    void aStallionWhoHasCoveredIsKept() {
        HorseRecord stallion = horse(1, null, null);
        assertFalse(AncestryPruning.mayForget(stallion, true, List.of(stallion)));
    }

    @Test
    void beingSomeonesFoalIsNoProtection() {
        // The edge that matters points from child to parent: a foal with nobody's name on it can go.
        HorseRecord foal = horse(3, 1, 2);
        assertTrue(AncestryPruning.mayForget(foal, false, List.of(horse(1, null, null), horse(2, null, null), foal)));
    }
}
