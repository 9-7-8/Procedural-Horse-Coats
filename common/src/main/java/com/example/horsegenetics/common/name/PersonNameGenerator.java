package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a <b>person's</b> name - a given name from an "alpha" table joined to
 * a family name from a "beta" table. The cowboy who breeds and sells horses at
 * a village barn is named from these, and that name is what the game says when
 * it says who bred one of his horses.
 *
 * <p>Deliberately <b>not</b> {@link HorseNameGenerator}. Both join an alpha word
 * to a beta word and there the resemblance stops: a horse's tables are one word
 * per line and read as descriptors ("Swift", "Midnight"), a person's are
 * comma-separated lists of real given and family names. Merging them would mean
 * one class that has to be told which of two file formats it is looking at, for
 * the sake of sharing four lines of {@code get(rng.nextInt(size))}.
 *
 * <p>Lives in {@code common} for the same reason horse naming does - nothing
 * about picking a word out of a list needs Minecraft, and the 1.12.2 backport
 * gets it for free. The tables ship as classpath resources under
 * {@code /horsegenetics/names/}.
 */
public final class PersonNameGenerator {

    private static final String COWBOY_ALPHA_RESOURCE = "/horsegenetics/names/cowboy-names-alpha.txt";
    private static final String COWBOY_BETA_RESOURCE = "/horsegenetics/names/cowboy-names-beta.txt";

    private final List<String> alpha;
    private final List<String> beta;

    /** Visible for tests; prefer {@link #cowboys()} in normal code. */
    public PersonNameGenerator(List<String> alpha, List<String> beta) {
        if (alpha.isEmpty() || beta.isEmpty()) {
            throw new IllegalArgumentException("name word lists must both be non-empty");
        }
        this.alpha = List.copyOf(alpha);
        this.beta = List.copyOf(beta);
    }

    /** The bundled cowboy tables: frontier given names, frontier family names. */
    public static PersonNameGenerator cowboys() {
        return new PersonNameGenerator(readNames(COWBOY_ALPHA_RESOURCE), readNames(COWBOY_BETA_RESOURCE));
    }

    /** A person's two name halves. */
    public record PersonName(String first, String last) {
        public String joined() {
            return (first + " " + last).strip();
        }
    }

    public PersonName generateParts(Rng rng) {
        return new PersonName(alpha.get(rng.nextInt(alpha.size())), beta.get(rng.nextInt(beta.size())));
    }

    /** e.g. {@code "Jesse Calloway"}. */
    public String generate(Rng rng) {
        return generateParts(rng).joined();
    }

    /** How many distinct full names the two tables can produce. */
    public int combinations() {
        return alpha.size() * beta.size();
    }

    /**
     * Reads a <b>comma-separated</b> name table. Newlines are treated as
     * ordinary whitespace, so the file may be one long line or many; blank
     * entries are dropped and duplicates are kept (the tables have a few, and a
     * repeated name is just a slightly likelier one).
     */
    private static List<String> readNames(String resource) {
        try (InputStream in = PersonNameGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing name resource on classpath: " + resource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder all = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    all.append(line).append(' ');
                }
                List<String> names = new ArrayList<>();
                for (String part : all.toString().split(",")) {
                    String name = part.trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
                return names;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read name resource: " + resource, e);
        }
    }
}
