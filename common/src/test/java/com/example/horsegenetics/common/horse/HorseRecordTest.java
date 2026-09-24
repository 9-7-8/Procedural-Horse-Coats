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

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-0000000000dd");

    /** A genome standing in for "whatever this horse carries" - these tests are about the record. */
    private static final Genome GENOME = Genome.of(Genotype.wildType(), new SeededRng(7L));

    private static HorseRecord raw(UUID id, String first, String last, String code) {
        return new HorseRecord(id, first, last, Optional.empty(), code, GENOME.epigenomeCode(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0, Optional.empty(),
                false, Optional.empty());
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
    void aGeldingIsNotEntireAndSaysSo() {
        HorseRecord colt = HorseRecord.founder(ID, "a", "b", GENOME.withSex(Sex.MALE));
        assertTrue(colt.entire());
        HorseRecord gelding = colt.withGelded(true);
        assertFalse(gelding.entire());
        assertEquals(Sex.MALE, gelding.sex(), "the genotype is untouched");
        assertEquals("Gelding", gelding.sexLabel(true));
        assertEquals("Gelded colt", gelding.sexLabel(false));
        assertTrue(gelding.withBarnName(Optional.of("Barn")).withTamedBy("x").withBredBy("y").gelded(),
                "every wither keeps it");
        assertFalse(HorseRecord.founder(ID, "a", "b", GENOME.withSex(Sex.FEMALE)).entire());
    }

    @Test
    void negativeGenerationClamped() {
        assertEquals(0, new HorseRecord(ID, "a", "b", null, "EeAa", "",
                null, null, null, null, null, -9, null, false, null).generation());
    }

    /**
     * Ownership is carried on the record because vanilla's own answer never
     * reaches the client - {@code AbstractHorse} synchronises its flags byte and
     * keeps the owner in an NBT-only field, so a client-side
     * {@code getOwnerReference()} is always null. The naming box on the horse
     * screen was invisible to everyone, its owner included, for exactly that
     * reason.
     */
    @Test
    void ownershipIsCarriedOnTheRecordAndIsViewerSpecific() {
        HorseRecord wild = raw(ID, "A", "B", "EeAa");
        assertTrue(wild.ownerId().isEmpty());
        assertFalse(wild.ownedBy(OWNER), "a horse nobody owns is nobody's");

        HorseRecord owned = wild.withOwner(OWNER);
        assertTrue(owned.ownedBy(OWNER));
        assertFalse(owned.ownedBy(STRANGER), "a stranger does not get the naming box");
        assertFalse(owned.ownedBy(null), "no viewer, no ownership");
    }

    /**
     * Ownership moves - a transfer paper, a cowboy sale, a horse going wild
     * again - so the mirror has to be able to change and to clear, not just fill
     * a blank the way {@code tamedBy} does.
     */
    @Test
    void ownershipCanBeReassignedAndCleared() {
        HorseRecord sold = raw(ID, "A", "B", "EeAa").withOwner(OWNER).withOwner(STRANGER);
        assertFalse(sold.ownedBy(OWNER), "the previous owner keeps nothing");
        assertTrue(sold.ownedBy(STRANGER));

        HorseRecord feral = sold.withOwner(null);
        assertTrue(feral.ownerId().isEmpty());
        assertFalse(feral.ownedBy(STRANGER));
    }

    /**
     * A sold horse stays in the family tree and is drawn greyed out, so the
     * screen needs to tell "somebody else owns this" from "nobody does". Every
     * founder and every wild ancestor is unowned, so answering the second with
     * the first would grey out most of a pedigree.
     */
    @Test
    void anotherPlayersHorseIsNotTheSameAsAnUnownedOne() {
        HorseRecord wild = raw(ID, "A", "B", "EeAa");
        assertFalse(wild.ownedByAnother(OWNER), "nobody owns it, so nobody else does either");

        HorseRecord mine = wild.withOwner(OWNER);
        assertFalse(mine.ownedByAnother(OWNER), "my own horse is not somebody else's");
        assertTrue(mine.ownedByAnother(STRANGER));
        assertFalse(mine.ownedByAnother(null), "no viewer, no answer");

        assertFalse(mine.withOwner(null).ownedByAnother(STRANGER), "gone wild is not gone to someone");
    }

    /**
     * "Sold" is the narrower claim: the horse is credited to the viewer and
     * owned by somebody else. A stallion borrowed for one covering is another
     * player's horse without ever having been the viewer's to sell.
     */
    @Test
    void soldOnMeansItWasYoursAndIsNot() {
        HorseRecord bred = raw(ID, "A", "B", "EeAa").withBredBy("Ixora");

        assertTrue(bred.withOwner(STRANGER).soldOnBy("Ixora", OWNER), "bred by me, owned by them");
        assertFalse(bred.withOwner(OWNER).soldOnBy("Ixora", OWNER), "still mine");
        assertFalse(bred.soldOnBy("Ixora", OWNER), "unowned is not sold");
        assertFalse(bred.withOwner(OWNER).soldOnBy("Someone", STRANGER),
                "their horse in my tree was never mine to sell");

        // attribution() prefers bredBy, and falls back to tamedBy.
        HorseRecord tamed = raw(ID, "A", "B", "EeAa").withTamedBy("Ixora").withOwner(STRANGER);
        assertTrue(tamed.soldOnBy("Ixora", OWNER), "a horse I tamed and sold on");
        assertFalse(tamed.soldOnBy(null, OWNER), "no viewer name, no claim");
    }

    /** Ownership rides along with every other edit rather than being dropped by it. */
    @Test
    void ownershipSurvivesAnUnrelatedEdit() {
        HorseRecord renamed = raw(ID, "A", "B", "EeAa")
                .withOwner(OWNER)
                .withBarnName(Optional.of("Pip"));
        assertTrue(renamed.ownedBy(OWNER));
        assertEquals(Optional.of("Pip"), renamed.barnName());
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
