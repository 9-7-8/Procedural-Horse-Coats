package com.example.horsegenetics.common.genetics.spec;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>The creator's parity check, run from where the goldens run.</b>
 *
 * <p>Two separate failures put this here, and it is worth keeping them apart
 * because they fail in opposite directions:
 *
 * <ul>
 *   <li><b>Gap 269 - an unrun check does not fail at all.</b>
 *       {@code check-parity.mjs} was red for a whole release: the creator's
 *       {@code schema.js} mirror never gained {@code flight_true},
 *       {@code flight_glide} or {@code cloud_walk}, so it offered a shorter list
 *       of traversal flags than the game accepts. The check was current and
 *       correct and would have failed immediately - nothing ran it but a person
 *       deciding to, and the session that added the flags did not.
 *   <li><b>Gap 13 - a stale snapshot fails silently green.</b>
 *       {@code expected.json} is a checked-in snapshot of what the Java spec
 *       engine produces, so parity against a stale one is green <i>by
 *       definition</i>: that is how a UV swap hid for a day while the creator
 *       drew every horse with its spine and belly patches exchanged.
 * </ul>
 *
 * <p>So this bakes the fixture afresh into a temporary file and compares, which
 * catches the stale snapshot, and only then runs the parity check itself, which
 * catches the drift. Neither half is worth much alone.
 *
 * <p>Node is not needed to compile or to run the rest of the suite, so the
 * second half skips rather than fails when it is missing - but the first half,
 * which is pure Java, always runs.
 */
class CreatorParityTest {

    /** Walk up from the working directory until the repo root is under us. */
    private static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("wiki/gene-creator"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("could not find the repo root from " + Path.of("").toAbsolutePath());
        }
        return p;
    }

    @Test
    @DisplayName("expected.json is what the Java engine produces today, not a record of the past")
    void theFixtureIsNotStale() throws Exception {
        Path root = repoRoot();
        Path committed = root.resolve("wiki/gene-creator/fixtures/expected.json");
        Assumptions.assumeTrue(Files.exists(committed), "no expected.json checked in yet");

        Path fresh = Files.createTempFile("expected-fresh", ".json");
        try {
            SpecFixtureTool.main(new String[]{fresh.toAbsolutePath().toString()});
            String now = Files.readString(fresh, StandardCharsets.UTF_8).replace("\r\n", "\n");
            String was = Files.readString(committed, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(was, now,
                    "wiki/gene-creator/fixtures/expected.json is stale - the spec engine produces something "
                            + "different today. Parity against it is green by definition until you run "
                            + ":common:bakeSpecFixtures and commit the result.");
        } finally {
            Files.deleteIfExists(fresh);
        }
    }

    @Test
    @DisplayName("the creator's JS twin still agrees with the Java engine")
    void theCreatorAgreesWithJava() throws Exception {
        Path root = repoRoot();
        Path script = root.resolve("wiki/gene-creator/tools/check-parity.mjs");
        Assumptions.assumeTrue(Files.exists(script), "check-parity.mjs is not in this tree");
        Assumptions.assumeTrue(nodeOnPath(), "node is not on PATH - parity was not checked in this run");

        ProcessBuilder pb = new ProcessBuilder(nodeCommand(), script.toAbsolutePath().toString());
        pb.directory(root.toFile());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!proc.waitFor(5, TimeUnit.MINUTES)) {
            proc.destroyForcibly();
            fail("check-parity.mjs did not finish within five minutes");
        }
        if (proc.exitValue() != 0) {
            fail("check-parity.mjs failed (exit " + proc.exitValue() + "):\n" + out.strip());
        }
    }

    private static String nodeCommand() {
        // Windows resolves "node" only with the extension when invoked directly.
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "node.exe" : "node";
    }

    private static boolean nodeOnPath() {
        try {
            Process p = new ProcessBuilder(nodeCommand(), "--version").redirectErrorStream(true).start();
            return p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
