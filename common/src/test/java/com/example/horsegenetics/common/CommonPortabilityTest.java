package com.example.horsegenetics.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three <b>portability rules</b> for {@code common/}, enforced instead of
 * merely written down.
 *
 * <p>All three are in {@code CLAUDE.md} as hard rules, and until now nothing
 * checked any of them. Each fails in a way the compiler cannot see: the module
 * compiles, the suite passes, the mod ships, and the damage shows up in a
 * <b>browser nobody on this end can open a console in</b> or in a backport that
 * does not exist yet. That is the worst shape of failure this codebase has, and
 * it has already cost a day once.
 *
 * <h2>Why a source scan rather than a bytecode or classpath check</h2>
 * Because two of the three rules are about <i>which API was written</i>, not
 * about what the JVM ended up linking. {@code common/} is compiled against a
 * modern JDK for the NeoForge target, so a Java 9+ call is perfectly legal here
 * and only breaks the TeaVM and (one day) Java 8 targets. A compiler that is
 * happy is exactly the situation these rules exist for.
 *
 * <p>Reading the source is crude and it is honest about being crude: it will
 * not see a rule broken through reflection or string concatenation, and it is
 * not trying to. It catches the thing that actually happens, which is somebody
 * typing {@code Map.of()} because it is the obvious way to write an empty map.
 */
class CommonPortabilityTest {

    /** The module's own sources. Gradle runs tests with {@code common/} as the working directory. */
    private static final Path SRC = Path.of("src", "main", "java");

    private record Line(Path file, int number, String text) {

        @Override
        public String toString() {
            return SRC.relativize(file).toString().replace('\\', '/') + ":" + number + "  " + text.trim();
        }
    }

    private static List<Line> lines() {
        List<Line> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> read = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < read.size(); i++) {
                    out.add(new Line(f, i + 1, read.get(i)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** Ignore comment lines: a rule is allowed to be *discussed* anywhere. */
    private static boolean isCode(Line line) {
        String t = line.text().strip();
        return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
    }

    @Test
    void theSourceTreeIsFound() {
        assertTrue(Files.isDirectory(SRC),
                "expected to scan " + SRC.toAbsolutePath() + " - has the working directory moved? "
                        + "This test is worthless if it silently scans nothing.");
        assertTrue(lines().size() > 1000, "scanned suspiciously few lines");
    }

    /**
     * <b>Hard rule 1: {@code common/} imports nothing from Minecraft or NeoForge.</b>
     *
     * <p>This is the rule the whole module split exists to keep, and the one
     * that makes a version port cheap. It is also the easiest to break by
     * accident, because an IDE will happily auto-import {@code ResourceLocation}
     * into a file that is one directory away from code where it is normal.
     */
    @Test
    void commonImportsNothingFromMinecraftOrNeoForge() {
        List<Line> bad = lines().stream()
                .filter(CommonPortabilityTest::isCode)
                .filter(l -> l.text().strip().startsWith("import "))
                .filter(l -> l.text().contains("net.minecraft")
                        || l.text().contains("net.neoforged")
                        || l.text().contains("com.mojang"))
                .toList();
        assertEquals(List.of(), bad,
                "common/ must not import Minecraft, NeoForge or Mojang types - that is what makes "
                        + "the module portable. Put it in the NeoForge module and hand common/ plain data.");
    }

    /**
     * <b>Hard rule 2, the half that has actually bitten: no empty immutable map.</b>
     *
     * <p>TeaVM's empty immutable map traps with {@code remainder by zero} on the
     * first {@code get} &mdash; it hashes the key into a table it never sized.
     * So {@code Map.of()} and {@code Map.copyOf(...)} are safe on a JVM, safe in
     * the game, and a live grenade in the wasm the wiki runs on.
     *
     * <p>It cost a day: {@code invert} is the only gene in the registry with an
     * {@code ALL} mask, an {@code ALL} mask takes no parameters, so its params
     * map was empty and the painter's very first lookup on it killed the bake
     * &mdash; with a green build, a correctly baked icon, and every value
     * verified from Java. {@link CommonMaps} is the replacement: a
     * {@code LinkedHashMap} behind an unmodifiable wrapper, correct empty on
     * both targets.
     *
     * <p>{@link CommonMaps} itself is exempt, because it is where the safe
     * construction lives.
     *
     * <h2>What this deliberately does <i>not</i> flag</h2>
     * A <b>non-empty</b> {@code Map.of(k, v, ...)} literal, of which
     * {@link com.example.horsegenetics.common.genetics.genes.LightGene} ships
     * one. The trap is the empty map specifically - a populated one gets a
     * sized table and its lookups work - and that gene paints, so the browser
     * exercises it on every preview and would have died on it the way the
     * {@code ALL} mask did. Empirically fine, therefore, rather than argued to
     * be fine.
     *
     * <p>{@code Map.copyOf(...)} <b>is</b> flagged even though it usually has
     * something to copy, because whether its source is empty is a runtime
     * question and this check is a source scan. That is the one place here where
     * being crude costs a little convenience, and it is the right trade: the
     * failure it prevents is invisible on this side of the build.
     */
    @Test
    void commonBuildsItsImmutableMapsThroughCommonMaps() {
        List<Line> bad = lines().stream()
                .filter(l -> !l.file().endsWith("CommonMaps.java"))
                .filter(CommonPortabilityTest::isCode)
                .filter(l -> l.text().contains("Map.of()") || l.text().contains("Map.copyOf("))
                .toList();
        assertEquals(List.of(), bad,
                "Map.of() / Map.copyOf() in common/ - TeaVM's EMPTY immutable map divides by zero "
                        + "on the first get, and it fails only in the browser. Use CommonMaps.");
    }

    /**
     * <b>Hard rule 2, the rest: the named Java 9+ APIs that were removed once
     * and must not come back.</b>
     *
     * <p>The list is short and specific on purpose. This is not an attempt to
     * police the whole Java 8 surface &mdash; that wants a real
     * {@code --release 8} build, which is roadmap &mdash; it is a tripwire on
     * the exact calls that were found and removed, so re-introducing one is
     * loud rather than silent.
     */
    @Test
    void commonAvoidsTheJavaNinePlusApisThatWereRemoved() {
        List<String> banned = List.of("System.getLogger", "Long.parseUnsignedLong");
        List<Line> bad = lines().stream()
                .filter(CommonPortabilityTest::isCode)
                .filter(l -> banned.stream().anyMatch(b -> l.text().contains(b)))
                .toList();
        assertEquals(List.of(), bad,
                "common/ has three targets - NeoForge, TeaVM and one day Java 8 - and these calls "
                        + "were removed for the last two. See CommonLog and Epigenome.parseUnsignedHex.");
    }
}
