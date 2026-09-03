package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.Genes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drop-in path: a folder of JSON files becomes registered genes. The
 * behaviour that matters most here is what happens when one file is <b>bad</b> -
 * it must cost you that gene and nothing else, because the alternative is a
 * player losing their whole collection (or the game refusing to boot) over a
 * missing comma.
 */
class GeneSpecLoaderTest {

    @AfterEach
    void unregister() {
        Genes.clearLoaded();
    }

    @Test
    void everyShippedExampleParses() {
        // The creator offers these from its menu and the parity fixtures bake
        // them, so a broken one has to fail the build, not the tool.
        for (String file : List.of("silver.json", "dun.json", "tobiano.json", "aurora.json")) {
            GeneSpec spec = GeneSpecParser.parse(GeneSpecParserTest.example(file), file);
            assertTrue(spec.key().startsWith("example."), file + " should use the example namespace");
            assertFalse(spec.expressions().isEmpty(), file + " declares no outcomes");
            boolean paints = spec.expressions().stream().anyMatch(e -> !e.layers().isEmpty());
            assertTrue(paints, file + " does nothing to the coat");
        }
    }

    @Test
    void loadsAFolderInFilenameOrder(@TempDir Path dir) throws IOException {
        write(dir, "b-second.json", gene("example.second", 20));
        write(dir, "a-first.json", gene("example.first", 10));

        GeneSpecLoader.Result result = GeneSpecLoader.fromDirectory(dir);

        assertTrue(result.ok(), result.errors().toString());
        assertEquals(List.of("example.first", "example.second"),
                result.specs().stream().map(GeneSpec::key).toList());
    }

    @Test
    void aMissingFolderIsNotAnError() {
        GeneSpecLoader.Result result = GeneSpecLoader.fromDirectory(Path.of("no", "such", "folder"));
        assertTrue(result.ok());
        assertTrue(result.specs().isEmpty());
    }

    @Test
    void oneBadFileDoesNotCostYouTheGoodOnes(@TempDir Path dir) throws IOException {
        write(dir, "good.json", gene("example.good", 10));
        write(dir, "broken.json", "{ \"key\": \"example.broken\", oops }");
        write(dir, "wrong.json", "{ \"key\": \"example.wrong\", \"alleles\": [], \"layers\": [] }");

        List<String> errors = GeneSpecLoader.register(GeneSpecLoader.fromDirectory(dir));

        assertEquals(2, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("broken.json"), errors.get(0));
        assertEquals(1, Genes.loaded().size());
        assertEquals("example.good", Genes.loaded().get(0).key());
    }

    @Test
    void aDuplicateKeyIsReportedRatherThanThrown(@TempDir Path dir) throws IOException {
        write(dir, "one.json", gene("example.same", 10));
        write(dir, "two.json", gene("example.same", 20));

        List<String> errors = GeneSpecLoader.register(GeneSpecLoader.fromDirectory(dir));

        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("already registered"), errors.get(0));
        assertEquals(1, Genes.loaded().size());
    }

    private static void write(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static String gene(String key, int priority) {
        return """
                { "format": 2, "key": "%s", "priority": %d,
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "founders": { "A/A": 1, "A/a": 9, "a/a": 90 },
                  "expressions": [
                    { "id": "v", "when": [ "A/A", "A/a" ],
                      "layers": [ { "masks": [ { "type": "ALL" } ],
                                    "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                    { "id": "wild", "wildType": true } ] }
                """.formatted(key, priority);
    }
}
