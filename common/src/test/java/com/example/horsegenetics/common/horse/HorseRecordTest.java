package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
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

    private static HorseRecord raw(UUID id, Sex sex, String first, String last, String code) {
        return new HorseRecord(id, sex, first, last, Optional.empty(), code, GENOME.epigenomeCode(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, 0.0, 0.0, Optional.empty());
    }

    @Test
    void founderHasNoParentsGenerationZeroNoAttribution() {
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "Swift", "Aspen", GENOME);
        assertFalse(r.hasKnownParents());
        assertEquals(0, r.generation());
        assertTrue(r.tamedBy().isEmpty());
        assertTrue(r.bredBy().isEmpty());
        assertTrue(r.attribution().isEmpty());
    }

    @Test
    void bredCarriesParentsAndGeneration() {
        HorseRecord r = HorseRecord.bred(ID, Sex.FEMALE, "Bold", "Canyon", GENOME, DAM, SIRE, 2);
        assertEquals(Optional.of(DAM), r.motherId());
        assertEquals(Optional.of(SIRE), r.fatherId());
        assertEquals(2, r.generation());
    }

    @Test
    void displayNamePrefersBarnNameThenFirstLast() {
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "Swift", "Aspen", GENOME);
        assertEquals("Swift Aspen", r.displayName());
        assertEquals("Barn", r.withBarnName(Optional.of("Barn")).displayName());
        assertEquals("Swift", r.withNames("Swift", "").displayName());
        assertEquals("Aspen", r.withNames("", "Aspen").displayName());
    }

    @Test
    void barnNameIsTrimmedToSixteenCharsAndBlankBecomesEmpty() {
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "a", "b", GENOME);
        assertEquals(Optional.of("0123456789ABCDEF"), r.withBarnName(Optional.of("0123456789ABCDEF__extra")).barnName());
        assertTrue(r.withBarnName(Optional.of("   ")).barnName().isEmpty());
    }

    @Test
    void attributionIsBreederThenTamer() {
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "a", "b", GENOME);
        assertEquals(Optional.of("TamerJoe"), r.withTamedBy("TamerJoe").attribution());
        assertEquals(Optional.of("BreederAmy"), r.withTamedBy("TamerJoe").withBredBy("BreederAmy").attribution());
    }

    @Test
    void hasNameIsFalseOnlyWhenEverythingIsBlank() {
        assertFalse(raw(ID, Sex.MALE, "", "", "EeAa").hasName());
        assertTrue(raw(ID, Sex.MALE, "First", "", "EeAa").hasName());
        assertTrue(raw(ID, Sex.MALE, "", "Last", "EeAa").hasName());
        assertTrue(raw(ID, Sex.MALE, "", "", "EeAa").withBarnName(Optional.of("Barn")).hasName());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> raw(null, Sex.MALE, "a", "b", "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, null, "a", "b", "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, Sex.MALE, null, "b", "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, Sex.MALE, "a", null, "EeAa"));
        assertThrows(NullPointerException.class, () -> raw(ID, Sex.MALE, "a", "b", null));
    }

    @Test
    void negativeGenerationClamped() {
        assertEquals(0, new HorseRecord(ID, Sex.MALE, "a", "b", null, "EeAa", "",
                null, null, null, null, -9, 0.0, 0.0, null).generation());
    }

    @Test
    void statsRoundUpAndClampAndDriveHasStats() {
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "a", "b", GENOME);
        assertEquals(0.0, r.speed());
        assertEquals(0.0, r.health());
        assertFalse(r.hasStats());

        HorseRecord s = r.withStats(-1.0, 25.0);
        assertEquals(0.0, s.speed());
        assertEquals(25.0, s.health());
        assertFalse(s.hasStats());

        HorseRecord t = r.withStats(0.2401, 21.2);
        assertEquals(0.241, t.speed(), 1e-9);  // ceil to 3 decimals
        assertEquals(22.0, t.health(), 1e-9);  // ceil to whole
        assertTrue(t.hasStats());
    }

    @Test
    void withersPreserveStatsAndParentStats() {
        ParentStats ps = ParentStats.of(0.2, 0.3, 20.0, 26.0);
        HorseRecord r = HorseRecord.founder(ID, Sex.MALE, "Swift", "Aspen", GENOME)
                .withStats(0.3, 27.0).withParentStats(ps);
        assertEquals(0.3, r.withNames("New", "Name").speed());
        assertEquals(27.0, r.withBarnName(Optional.of("Barn")).health());
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
