package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drop-in folder, which is the part a player touches without a rebuild.
 *
 * <p>Every test resets the generator afterwards: the drop-in tables and the
 * built generators are static, so a test that loaded extras would otherwise
 * hand them to {@code RegionTest}.
 */
class PersonNameGeneratorTest {

    private static final Set<String> KNOWN = Set.of("scandinavia", "north_america");

    @AfterEach
    void forgetDropIns() {
        PersonNameGenerator.resetForTesting();
    }

    @Test
    void rejectsEmptyTables() {
        assertThrows(IllegalArgumentException.class,
                () -> new PersonNameGenerator(List.of(), List.of("X")));
        assertThrows(IllegalArgumentException.class,
                () -> new PersonNameGenerator(List.of("A"), List.of()));
    }

    @Test
    void aMissingFolderIsNotAnError(@TempDir Path dir) {
        assertEquals(List.of(), PersonNameGenerator.loadFrom(dir.resolve("nope"), KNOWN));
    }

    @Test
    void dropInNamesAreAddedToTheShippedOnes(@TempDir Path dir) throws IOException {
        int before = PersonNameGenerator.forRegion("scandinavia").combinations();
        PersonNameGenerator.resetForTesting();

        write(dir, "scandinavia-alpha.txt", "Torfinn, Sigvard");
        write(dir, "scandinavia-beta.txt", "Aasgard");

        assertEquals(List.of(), PersonNameGenerator.loadFrom(dir, KNOWN));
        assertEquals(2, PersonNameGenerator.extrasFor("scandinavia", true));
        assertEquals(1, PersonNameGenerator.extrasFor("scandinavia", false));

        // Added, not replaced - the shipped table is still in there.
        int after = PersonNameGenerator.forRegion("scandinavia").combinations();
        assertTrue(after > before,
                "expected the drop-in to widen the table, " + before + " -> " + after);
    }

    @Test
    void oneNamePerLineReadsTheSameAsOneLongLine(@TempDir Path dir) throws IOException {
        write(dir, "scandinavia-alpha.txt", "Torfinn,\nSigvard,\n\nHalle\n");
        write(dir, "scandinavia-beta.txt", "Aasgard");

        assertEquals(List.of(), PersonNameGenerator.loadFrom(dir, KNOWN));
        assertEquals(3, PersonNameGenerator.extrasFor("scandinavia", true));
    }

    /**
     * The folder ships with a README explaining the format, and it is a
     * {@code .txt} sitting in a folder of {@code .txt} files. Before this was
     * handled, every server boot logged
     * {@code [names] README.txt is ignored - a name file must end "-alpha.txt"...},
     * which is noise that never goes away and never means anything.
     */
    @Test
    void theReadmeInTheFolderIsNotMistakenForANameTable(@TempDir Path dir) throws IOException {
        write(dir, "README.txt", "Horse Genetics - drop-in trader names\nName a file scandinavia-alpha.txt");
        write(dir, "scandinavia-alpha.txt", "Torfinn");

        assertEquals(List.of(), PersonNameGenerator.loadFrom(dir, KNOWN),
                "the README must not be reported as a malformed name table");
        assertEquals(1, PersonNameGenerator.extrasFor("scandinavia", true));
    }

    @Test
    void aTypoInAFileNameIsReportedRatherThanIgnored(@TempDir Path dir) throws IOException {
        write(dir, "scandinavai-alpha.txt", "Torfinn");   // region misspelled
        write(dir, "scandinavia-given.txt", "Sigvard");   // half misspelled
        write(dir, "notes.txt", "nothing to see");        // no half at all

        List<String> problems = PersonNameGenerator.loadFrom(dir, KNOWN);
        assertEquals(3, problems.size(), "expected all three to be reported: " + problems);
        assertTrue(problems.toString().contains("scandinavai"), problems.toString());
        assertEquals(0, PersonNameGenerator.extrasFor("scandinavia", true));
    }

    @Test
    void drawsAlphaThenBeta() {
        PersonNameGenerator gen = new PersonNameGenerator(List.of("A", "B"), List.of("X", "Y"));
        // first nextInt -> alpha index, second -> beta index
        assertEquals("B X", gen.generate(new FakeRng().ints(1, 0)));
    }

    private static void write(Path dir, String name, String body) throws IOException {
        Files.writeString(dir.resolve(name), body, StandardCharsets.UTF_8);
    }
}
