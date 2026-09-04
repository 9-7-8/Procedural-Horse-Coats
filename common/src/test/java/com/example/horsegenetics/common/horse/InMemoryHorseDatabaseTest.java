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

class InMemoryHorseDatabaseTest {

    private static final Genome GENOME = Genome.of(Genotype.wildType(), new SeededRng(7L));

    private static UUID id(String tail) {
        return UUID.fromString("00000000-0000-0000-0000-0000000000" + tail);
    }

    private static HorseRecord founder(String tail) {
        return HorseRecord.founder(id(tail), "F" + tail, "Wind", GENOME);
    }

    private static HorseRecord bred(String tail, String dam, String sire) {
        return HorseRecord.bred(id(tail), "B" + tail, "Wind", GENOME, id(dam), id(sire), 1);
    }

    @Test
    void recordThenLookup() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        assertEquals(Optional.empty(), db.lookup(id("01")));
        HorseRecord r = founder("01");
        db.record(r);
        assertEquals(Optional.of(r), db.lookup(id("01")));
        assertEquals(1, db.size());
    }

    @Test
    void recordReplacesSameId() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase();
        db.record(founder("01"));
        db.record(founder("01").withNames("renamed", "Wind"));
        assertEquals("renamed", db.lookup(id("01")).orElseThrow().firstName());
        assertEquals(1, db.size());
    }

    @Test
    void ancestorsByGeneration() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase(List.of(
                founder("aa"), founder("ab"),        // grandparents (maternal)
                founder("ba"), founder("bb"),        // grandparents (paternal)
                bred("0a", "aa", "ab"),              // dam
                bred("0b", "ba", "bb"),              // sire
                bred("01", "0a", "0b")               // subject
        ));

        List<String> depth1 = names(db.ancestorsOf(id("01"), 1));
        assertEquals(List.of("B0a", "B0b"), depth1);

        List<String> depth2 = names(db.ancestorsOf(id("01"), 2));
        assertEquals(List.of("B0a", "B0b", "Faa", "Fab", "Fba", "Fbb"), depth2);

        // nothing beyond gen 2 exists; depth 3 is the same as depth 2
        assertEquals(depth2, names(db.ancestorsOf(id("01"), 3)));
    }

    @Test
    void depthZeroOrNegativeIsEmpty() {
        InMemoryHorseDatabase db = new InMemoryHorseDatabase(List.of(bred("01", "0a", "0b"), founder("0a"), founder("0b")));
        assertTrue(db.ancestorsOf(id("01"), 0).isEmpty());
        assertTrue(db.ancestorsOf(id("01"), -5).isEmpty());
    }

    @Test
    void inbreedingListsEachAncestorOnce() {
        // both parents share the same sire "0c"
        InMemoryHorseDatabase db = new InMemoryHorseDatabase(List.of(
                founder("0c"),
                bred("0a", "0c", "0c"),
                bred("0b", "0c", "0c"),
                bred("01", "0a", "0b")
        ));
        List<String> anc = names(db.ancestorsOf(id("01"), 3));
        assertEquals(1, anc.stream().filter(n -> n.equals("F0c")).count(), anc.toString());
        assertEquals(List.of("B0a", "B0b", "F0c"), anc);
    }

    @Test
    void unknownAncestorsAreSkipped() {
        // subject references a dam that was never recorded
        InMemoryHorseDatabase db = new InMemoryHorseDatabase(List.of(
                founder("0b"),
                bred("01", "0a", "0b")   // "0a" absent
        ));
        assertEquals(List.of("F0b"), names(db.ancestorsOf(id("01"), 2)));
    }

    private static List<String> names(List<HorseRecord> records) {
        return records.stream().map(HorseRecord::firstName).toList();
    }
}
