package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.breed.spec.BreedSpecLoader;
import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The <b>shipped breed files</b> - the ones under
 * {@code main/resources/horsegenetics/breeds/} - are a generated, checked-in
 * artefact, and CLAUDE.md's rule about those is that they fail silently when
 * they go stale. This is the thing that makes them fail loudly instead.
 *
 * <p>Three ways they can rot, all of them invisible in the game until somebody
 * notices a breed has quietly stopped being itself:
 * <ul>
 *   <li>a gene is renamed or an allele token changes, and every file naming it
 *       loads with a warning and one locus fewer;</li>
 *   <li>a file is added to the folder and left out of {@code index.json}, so it
 *       loads on a development machine (which walks a real directory) and not
 *       out of a jar (which has only the index);</li>
 *   <li>{@code BreedSpecWriter} and {@code BreedSpecParser} drift apart, so the
 *       files can still be read but re-baking them produces a different folder.</li>
 * </ul>
 */
class BreedFilesTest {

    @Test
    void everyShippedFileLoadsWithNoErrorsAndNoWarnings() {
        BreedSpecLoader.Result result = BreedSpecLoader.fromClasspath();
        assertTrue(result.errors().isEmpty(), "errors: " + result.errors());
        // A warning means a file names a gene or an allele this build has not
        // got. For a third-party breed that is the designed behaviour; for one
        // the mod ships with itself, it is a rename nobody finished.
        assertTrue(result.warnings().isEmpty(), "warnings: " + result.warnings());
        assertFalse(result.breeds().isEmpty(), "the classpath index found nothing");
    }

    @Test
    void theIndexListsEveryBreedTheRegistryHolds() {
        BreedSpecLoader.Result result = BreedSpecLoader.fromClasspath();
        assertEquals(Breeds.all().size(), result.breeds().size(),
                "the registry and the index disagree - a Java breed, or a stale index.json");
    }

    /**
     * Every file is exactly what {@code BreedSpecWriter} would write for the
     * breed it parses to. That is what {@code ./gradlew :common:bakeBreedFiles}
     * produces, so a green run here means re-baking would change nothing - and a
     * red one names the file to look at rather than leaving a whole-folder diff.
     */
    @Test
    void everyShippedFileIsExactlyWhatABakeWouldWrite() {
        List<String> wrong = new ArrayList<>();
        for (Breed breed : Breeds.all()) {
            String onDisk = resource("/horsegenetics/breeds/" + breed.id() + ".json");
            if (onDisk == null) {
                wrong.add(breed.id() + ": no file on the classpath");
                continue;
            }
            if (!onDisk.equals(BreedSpecWriter.write(breed))) {
                wrong.add(breed.id() + ": on disk differs from a fresh bake");
            }
        }
        assertTrue(wrong.isEmpty(), wrong + " - run ./gradlew :common:bakeBreedFiles");
    }

    /**
     * The wiki tools read one bundled array instead of walking a classpath,
     * because a TeaVM build cannot walk a classpath. Two copies of anything is a
     * chance for them to disagree, and this one disagrees <i>silently</i>: the
     * breed designer would go on offering yesterday's Friesian and validating
     * against yesterday's rules with nothing on screen to say so.
     */
    @Test
    void theBrowserBundleHoldsTheSameBreedsAsTheFiles() {
        Path bundle = Path.of("..", "wiki", "horse-designer", "assets", "breeds.json");
        assertTrue(Files.exists(bundle),
                bundle.toAbsolutePath() + " is missing - run ./gradlew :common:bakeBreedFiles");

        List<String> problems = new ArrayList<>();
        List<Breed> bundled;
        try {
            bundled = BreedSpecParser.parseAll(
                    Files.readString(bundle, StandardCharsets.UTF_8), "breeds.json", problems::add);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertTrue(problems.isEmpty(), "bundle: " + problems);

        List<String> mine = new ArrayList<>();
        for (Breed b : Breeds.all()) {
            mine.add(b.id());
        }
        List<String> theirs = new ArrayList<>();
        for (Breed b : bundled) {
            theirs.add(b.id());
        }
        assertEquals(mine, theirs,
                "the browser bundle is stale - run ./gradlew :common:bakeBreedFiles");
        for (int i = 0; i < bundled.size(); i++) {
            assertEquals(BreedSpecWriter.write(Breeds.all().get(i)), BreedSpecWriter.write(bundled.get(i)),
                    "breed " + mine.get(i) + " differs between the files and the bundle");
        }
    }

    private static String resource(String path) {
        try (InputStream in = BreedFilesTest.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
