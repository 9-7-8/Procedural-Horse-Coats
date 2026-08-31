package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds a horse name by joining one word from an "alpha" table (descriptors:
 * "Swift", "Midnight", "Son of the", ...) with one from a "beta" table
 * (mostly place/nature names: "Aspen", "Canyon", ...). One word per line in
 * each file; blank lines are ignored.
 *
 * <p>This lives in {@code common} on purpose - naming is going to be applied
 * more broadly than the debug pens (breeding, name tags, etc.), and none of
 * it needs Minecraft. The word tables ship as classpath resources under
 * {@code /horsegenetics/names/}.
 */
public final class HorseNameGenerator {

    private static final String ALPHA_RESOURCE = "/horsegenetics/names/horse-names-alpha.txt";
    private static final String BETA_RESOURCE = "/horsegenetics/names/horse-names-beta.txt";

    private final List<String> alpha;
    private final List<String> beta;

    /** Visible for tests; prefer {@link #fromResources()} in normal code. */
    public HorseNameGenerator(List<String> alpha, List<String> beta) {
        if (alpha.isEmpty() || beta.isEmpty()) {
            throw new IllegalArgumentException("name word lists must both be non-empty");
        }
        this.alpha = List.copyOf(alpha);
        this.beta = List.copyOf(beta);
    }

    /** Loads the bundled word tables from the classpath. */
    public static HorseNameGenerator fromResources() {
        return new HorseNameGenerator(readWords(ALPHA_RESOURCE), readWords(BETA_RESOURCE));
    }

    /** A horse's two name halves: {@code first} from the alpha table, {@code last} from the beta table. */
    public record NameParts(String first, String last) {
        public String joined() {
            return (first + " " + last).strip();
        }
    }

    /** Pick one word from each table. */
    public NameParts generateParts(Rng rng) {
        return new NameParts(alpha.get(rng.nextInt(alpha.size())), beta.get(rng.nextInt(beta.size())));
    }

    /** e.g. {@code "Swift Aspen"}, {@code "Son of the Canyon"}. */
    public String generate(Rng rng) {
        return generateParts(rng).joined();
    }

    private static List<String> readWords(String resource) {
        try (InputStream in = HorseNameGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing name resource on classpath: " + resource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read name resource: " + resource, e);
        }
    }
}
