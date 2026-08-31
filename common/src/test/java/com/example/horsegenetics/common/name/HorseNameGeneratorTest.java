package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorseNameGeneratorTest {

    @Test
    void joinsOneWordFromEachTableInOrder() {
        HorseNameGenerator gen = new HorseNameGenerator(
                List.of("Swift", "Midnight", "Son of the"),
                List.of("Aspen", "Canyon"));
        // first nextInt -> alpha index, second -> beta index
        assertEquals("Midnight Aspen", gen.generate(new FakeRng().ints(1, 0)));
        assertEquals("Son of the Canyon", gen.generate(new FakeRng().ints(2, 1)));
    }

    @Test
    void drawsAlphaThenBeta() {
        HorseNameGenerator gen = new HorseNameGenerator(List.of("A", "B"), List.of("X", "Y"));
        // exactly two draws, alpha-bound then beta-bound; FakeRng range-checks each
        assertEquals("B X", gen.generate(new FakeRng().ints(1, 0)));
    }

    @Test
    void rejectsEmptyTables() {
        assertThrows(IllegalArgumentException.class,
                () -> new HorseNameGenerator(List.of(), List.of("X")));
        assertThrows(IllegalArgumentException.class,
                () -> new HorseNameGenerator(List.of("A"), List.of()));
    }

    @Test
    void bundledResourcesLoadAndProduceATwoPartName() {
        HorseNameGenerator gen = HorseNameGenerator.fromResources();
        String name = gen.generate(new FakeRng().ints(0, 0));
        assertTrue(name.contains(" "), "expected '<alpha> <beta>', got: " + name);
        assertTrue(name.length() > 2, name);
    }
}
