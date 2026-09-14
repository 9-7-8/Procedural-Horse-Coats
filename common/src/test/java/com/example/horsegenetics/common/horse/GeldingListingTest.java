package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gap 229: a gelding in the horse browser is a gelding - not a stallion for the
 * filters, and labelled as what he is.
 */
class GeldingListingTest {

    private static HorseListing male(String name, boolean adult, boolean gelded) {
        return HorseListing.of(UUID.nameUUIDFromBytes(name.getBytes()), name, "Test", "", "Arabian", 0,
                Genotype.wildType().withSex(Sex.MALE), adult, true, 0, false, true, "overworld", "owner", "",
                false, gelded);
    }

    private static final HorseListing STALLION = male("Stud", true, false);
    private static final HorseListing GELDING = male("Rook", true, true);
    private static final HorseListing COLT = male("Kid", false, false);
    private static final HorseListing GELDED_COLT = male("Nub", false, true);
    private static final List<HorseListing> ALL = List.of(STALLION, GELDING, COLT, GELDED_COLT);

    @Test
    void labelsSayGelding() {
        assertEquals("Stallion", STALLION.sexLabel());
        assertEquals("Gelding", GELDING.sexLabel());
        assertEquals("Gelded colt", GELDED_COLT.sexLabel());
        assertTrue(STALLION.entire());
        assertFalse(GELDING.entire());
    }

    @Test
    void theStallionAndColtFlagsLeaveGeldingsOut() {
        assertEquals(List.of(STALLION), HorseQuery.filter(ALL, "stallion"));
        assertEquals(List.of(COLT), HorseQuery.filter(ALL, "colt"));
    }

    @Test
    void geldingIsAFlagAndASexWord() {
        assertEquals(List.of(GELDING, GELDED_COLT), HorseQuery.filter(ALL, "gelding"));
        assertTrue(HorseQuery.flags().contains("gelding"));
        assertEquals(List.of(GELDING), HorseQuery.filter(ALL, "sex:gelding"));
    }
}
