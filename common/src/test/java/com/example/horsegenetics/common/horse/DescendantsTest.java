package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pedigree read <b>downward</b> - what the horse information screen's
 * Offspring tab draws. The two properties worth pinning are that generations
 * come back grouped and in order, and that a line folding back on itself does
 * not loop: horses in this mod can be bred to their own descendants, and a
 * naive walk would list a horse twice or never stop.
 */
class DescendantsTest {

    private static final Genome GENOME = Genome.of(Genotype.wildType(), new SeededRng(3L));

    private static UUID id(int n) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", n));
    }

    private static HorseRecord horse(int n, Integer dam, Integer sire) {
        return new HorseRecord(id(n), "H" + n, "Test", Optional.empty(),
                GENOME.genotypeCode(), GENOME.epigenomeCode(), Optional.empty(),
                dam == null ? Optional.empty() : Optional.of(id(dam)),
                sire == null ? Optional.empty() : Optional.of(id(sire)),
                Optional.empty(), Optional.empty(), 0, Optional.empty());
    }

    private static List<String> names(List<HorseRecord> records) {
        return records.stream().map(HorseRecord::firstName).sorted().toList();
    }

    @Test
    void generationsComeBackGroupedAndInOrder() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        db.record(horse(1, null, null));   // the root mare
        db.record(horse(2, null, null));   // an unrelated sire
        db.record(horse(3, 1, 2));         // foal
        db.record(horse(4, 1, 2));         // foal
        db.record(horse(5, 3, 2));         // grandfoal
        db.record(horse(6, 5, 2));         // great-grandfoal

        List<List<HorseRecord>> generations = db.descendantsOf(id(1), 5);
        assertEquals(3, generations.size());
        assertEquals(List.of("H3", "H4"), names(generations.get(0)));
        assertEquals(List.of("H5"), names(generations.get(1)));
        assertEquals(List.of("H6"), names(generations.get(2)));
    }

    @Test
    void depthStopsTheWalk() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        db.record(horse(1, null, null));
        db.record(horse(2, 1, null));
        db.record(horse(3, 2, null));
        assertEquals(1, db.descendantsOf(id(1), 1).size());
        assertTrue(db.descendantsOf(id(1), 0).isEmpty());
    }

    /** A mare bred back to her own grandson - each horse once, at its first rung. */
    @Test
    void aLineThatFoldsBackDoesNotLoop() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        db.record(horse(1, null, null));
        db.record(horse(2, 1, null));      // foal
        db.record(horse(3, 2, null));      // grandfoal
        db.record(horse(4, 1, 3));         // the mare again, by her grandson

        List<List<HorseRecord>> generations = db.descendantsOf(id(1), 5);
        assertEquals(List.of("H2", "H4"), names(generations.get(0)));
        assertEquals(List.of("H3"), names(generations.get(1)));
        assertEquals(2, generations.size());
    }

    @Test
    void aHorseWithNoFoalsHasNoGenerations() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        db.record(horse(1, null, null));
        assertTrue(db.descendantsOf(id(1), 3).isEmpty());
    }
}
