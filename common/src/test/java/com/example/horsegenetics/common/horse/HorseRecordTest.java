package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorseRecordTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DAM = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SIRE = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    /** A genome standing in for "whatever this horse carries" - these tests are about the record. */
    private static final Genome GENOME = Genome.of(Genotype.wildType(), new SeededRng(7L));

    private static HorseRecord raw(UUID id, String first, String last, String code) {
        return new HorseRecord(id, first, last, Optional.empty(), code, GENOME.epigenomeCode(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, Optional.empty());
    }

    /**
     * Sex is not a field - it is read back off the sex locus in the genetic
     * code, so a record can never disagree with the genome it carries.
     */
    @Test
    void sexIsDerivedFromTheGenome() {
        assertEquals(Sex.FEMALE, HorseRecord.founder(ID, "M", "One", GENOME.withSex(Sex.FEMALE)).sex());
        assertEquals(Sex.MALE, HorseRecord.founder(ID, "S", "Two", GENOME.withSex(Sex.MALE)).sex());
        // ...and it follows the genome through a withGenome() swap.
        HorseRecord mare = HorseRecord.founder(ID, "M", "One", GENOME.withSex(Sex.FEMALE));
        assertEquals(Sex.MALE, mare.withGenome(GENOME.withSex(Sex.MALE)).sex());
    }

    /** The blank sentinel has no sex segment at all, and reads as a mare. */
    @Test
    void unassignedReadsAsAMare() {
        assertEquals(Sex.FEMALE, HorseRecord.unassigned(ID).sex());
        assertEquals(Sex.FEMALE, raw(ID, "a", "b", "").sex());
    }

    @Test
    void founderHasNoParentsGenerationZeroNoAttribution() {
        HorseRecord r = HorseRecord.founder(ID, "Swift", "Aspen", GENOME);
        assertFalse(r.hasKnownParents());
        assertEquals(0, r.generation());
        assertTrue(r.tamedBy().isEmpty());
        assertTrue(r.bredBy().isEmpty());
        assertTrue(r.attribution().isEmpty());
    }

    @Test
    void bredCarriesParentsAndGeneration() {
        HorseRecord r = HorseRecord.bred(ID, "Bold", "Canyon", GENOME, DAM, SIRE, 2);
        assertEquals(Optional.of(DAM), r.motherId());
        assertEquals(Optional.of(SIRE), r.fatherId());
        assertEquals(2, r.generation());
    }

    @Test
    void displayNamePrefersBarnNameThenFirstLast() {
        HorseRecord r = HorseRecord.founder(ID, "Swift", "Aspen", GENOME);
        assertEquals("Swift Aspen", r.displayName());
        assertEquals("Barn", r.withBarnName(Optional.of("Barn")).displayName());
        assertEquals("Swift", r.withNames("Swift", "").displayName());
        assertEquals("Aspen", r.withNames("", "Aspen").displayName());
    }

    @Test
    void barnNameIsTrimmedToSixteenCharsAndBlankBecomesEmpty() {
        HorseRecord r = HorseRecord.founder(ID, "a", "b", GENOME);
        assertEquals(Optional.of("0123456789ABCDEF"), r.withBarnName(Optional.of("0123456789ABCDEF__extra")).barnName());
        assertTrue(r.withBarnName(Optional.of("   ")).barnName().isEmpty());
    }

    @Test
    void attributionIsBreederThenTamer() {
        HorseRecord r = HorseRecord.founder(ID, "a", "b", GENOME);
        assertEquals(Optional.of("TamerJoe"), r.withTamedBy("TamerJoe").attribution());
        assertEquals(Optional.of("BreederAmy"), r.withTamedBy("TamerJoe").withBredBy("BreederAmy").attribution());
    }

    @Test
    void hasNameIsFalseOnlyWhenEverythingIsBlank() {
        assertFalse(raw(ID, "", "", "EeAa").hasName());
        assertTrue(raw(ID, "First", "", "EeAa").hasName());
        assertTrue(raw(ID, "", "Last", "EeAa").hasName());
        assertTrue(raw(ID, "", "", "EeAa").withBarnName(Optional.of("Barn")).hasName());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> raw(null, "a", "b", "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, null, "b", "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, "a", null, "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, "a", "b", null));
    }

    @Test
    void negativeGenerationClamped() {
        assertEquals(0, new HorseRecord(ID, "a", "b", null, "EeAa", "",
                null, null, null, null, null, -9, null).generation());
    }

    /**
     * There are no stat fields to store any more: a record's speed and health
     * are resolved out of the genetic code it already carries, so the two can
     * never disagree.
     */
    @Test
    void statsAreDerivedFromTheGeneticCode() {
        HorseRecord r = HorseRecord.founder(ID, "a", "b", GENOME);
        Traits t = r.traits();
        assertEquals(HorseTraits.resolve(r.genotype()).speed(), t.speed(), 1e-12);
        assertEquals(HorseTraits.resolve(r.genotype()).health(), t.health(), 1e-12);
        // ...and a rename cannot move them, because they were never copied.
        assertEquals(t.speed(), r.withNames("New", "Name").traits().speed(), 1e-12);
    }

    @Test
    void withersPreserveParentStats() {
        ParentStats ps = ParentStats.of(0.2, 0.3, 20.0, 26.0);
        HorseRecord r = HorseRecord.founder(ID, "Swift", "Aspen", GENOME).withParentStats(ps);
        assertEquals(Optional.of(ps), r.withNames("New", "Name").parentStats());
        assertEquals(Optional.of(ps), r.withBarnName(Optional.of("Barn")).parentStats());
        assertEquals(Optional.of(ps), r.withTamedBy("x").parentStats());
        assertEquals(Optional.of(ps), r.withBredBy("y").parentStats());
    }

    @Test
    void parentStatsRanksAgainstBothParents() {
        ParentStats ps = ParentStats.of(0.20, 0.30, 20.0, 26.0);
        assertEquals(1, ps.rankSpeed(0.31));   // above both
        assertEquals(0, ps.rankSpeed(0.25));   // between
        assertEquals(-1, ps.rankSpeed(0.19));  // below both
        assertEquals(1, ps.rankHealth(27.0));
        assertEquals(-1, ps.rankHealth(19.0));
    }
}
