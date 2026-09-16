package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * <h2>One pair of tables per region</h2>
 *
 * <p>There is no single "people" table any more. Every
 * {@code com.example.horsegenetics.common.breed.Region} has its own pair, and a
 * trader is named from the region his horses came from: the premise is that
 * villagers were pulled out of the real world together with their herds, so the
 * man selling Fjords is called Halvorsen and the man selling Andalusians is
 * called Olivares. A name that does not match the string in the paddock throws
 * that away for nothing.
 *
 * <p>The tables live at {@code /horsegenetics/names/people/<region>-alpha.txt}
 * and {@code -beta.txt}. A region whose files are missing is a build error, not
 * something to paper over with a fallback - the regions are a closed enum and
 * the files are generated against it, so a miss means the two have drifted.
 *
 * <p>Unlike the horse tables these are <b>not</b> copied into the horse
 * designer: {@code web/build.gradle.kts} takes {@code names/*.txt}, which does
 * not descend into {@code people/}. That is deliberate - the designer names
 * horses, never people, and the old flat cowboy tables were being copied into
 * its asset bundle and never read.
 *
 * <p>Lives in {@code common} for the same reason horse naming does - nothing
 * about picking a word out of a list needs Minecraft, and the 1.12.2 backport
 * gets it for free.
 */
public final class PersonNameGenerator {

    /** One generator per region id, built on first use and dropped when drop-ins load. */
    private static final Map<String, PersonNameGenerator> BY_REGION = new HashMap<>();

    /** Extra names from a drop-in folder, by region id. Added to the shipped tables. */
    private static final Map<String, List<String>> EXTRA_ALPHA = new TreeMap<>();
    private static final Map<String, List<String>> EXTRA_BETA = new TreeMap<>();

    private final List<String> alpha;
    private final List<String> beta;

    /** Visible for tests; prefer {@link #forRegion(String)} in normal code. */
    public PersonNameGenerator(List<String> alpha, List<String> beta) {
        if (alpha.isEmpty() || beta.isEmpty()) {
            throw new IllegalArgumentException("name word lists must both be non-empty");
        }
        this.alpha = List.copyOf(alpha);
        this.beta = List.copyOf(beta);
    }

    /**
     * The name tables for one region, e.g. {@code "scandinavia"}. Cached: the
     * tables are small and immutable, and a cowboy founding is not a hot path,
     * but re-reading a classpath resource per villager would be silly.
     *
     * @throws IllegalStateException if that region has no tables on the classpath
     */
    public static synchronized PersonNameGenerator forRegion(String regionId) {
        PersonNameGenerator cached = BY_REGION.get(regionId);
        if (cached != null) {
            return cached;
        }
        List<String> alpha = new ArrayList<>(readNames(resourceFor(regionId, ALPHA)));
        List<String> beta = new ArrayList<>(readNames(resourceFor(regionId, BETA)));
        alpha.addAll(EXTRA_ALPHA.getOrDefault(regionId, List.of()));
        beta.addAll(EXTRA_BETA.getOrDefault(regionId, List.of()));
        PersonNameGenerator made = new PersonNameGenerator(alpha, beta);
        BY_REGION.put(regionId, made);
        return made;
    }

    /**
     * Read a drop-in folder of extra name tables - {@code .minecraft/phc/names/}
     * in game - and <b>add</b> what it holds to the shipped tables.
     *
     * <p>Files are named the way the shipped ones are,
     * {@code <region>-alpha.txt} and {@code <region>-beta.txt}, and the format is
     * the same: comma-separated, newlines counting as ordinary whitespace, so a
     * file may be one long line or one name per line.
     *
     * <p><b>It adds rather than replaces</b>, and that is the deliberate choice.
     * The shipped tables are a baseline worth keeping - a drop-in holding ten
     * names would otherwise make every trader in that region draw from ten - and
     * the thing a player actually wants here is to widen a region, not to curate
     * it. Removing a shipped name means editing the shipped file.
     *
     * @param directory     the folder to read; a missing one is not an error
     * @param knownRegions  region ids that exist, so a typo in a file name is
     *                      reported rather than silently doing nothing. This class
     *                      deliberately does not know what a region is, so the
     *                      caller supplies the list.
     * @return everything worth telling the player about, most serious first
     */
    public static synchronized List<String> loadFrom(Path directory, Set<String> knownRegions) {
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return problems;
        }
        Map<String, List<String>> foundAlpha = new TreeMap<>();
        Map<String, List<String>> foundBeta = new TreeMap<>();

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.txt")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                // The folder's own README, which the NeoForge side writes into it on first
                // launch. It is a .txt in a folder of .txt files, so it lands in this scan
                // and was reported as a malformed name table on every single boot. A
                // warning that is always there and never actionable teaches people to stop
                // reading the log. (The breeds drop-in dodges this by luck: its README is
                // .txt and it scans for .json.)
                if (name.equalsIgnoreCase("README.txt")) {
                    continue;
                }
                String stem = name.substring(0, name.length() - ".txt".length());
                int dash = stem.lastIndexOf('-');
                String half = dash < 0 ? "" : stem.substring(dash + 1);
                if (!ALPHA.equals(half) && !BETA.equals(half)) {
                    problems.add(name + " is ignored - a name file must end \"-" + ALPHA
                            + ".txt\" or \"-" + BETA + ".txt\"");
                    continue;
                }
                String regionId = stem.substring(0, dash);
                if (!knownRegions.contains(regionId)) {
                    problems.add(name + " is ignored - \"" + regionId + "\" is not a region");
                    continue;
                }
                List<String> names = readNames(file);
                if (names.isEmpty()) {
                    problems.add(name + " holds no names");
                    continue;
                }
                (ALPHA.equals(half) ? foundAlpha : foundBeta)
                        .computeIfAbsent(regionId, k -> new ArrayList<>()).addAll(names);
            }
        } catch (IOException e) {
            problems.add("could not read " + directory + ": " + e);
            return problems;
        }

        EXTRA_ALPHA.putAll(foundAlpha);
        EXTRA_BETA.putAll(foundBeta);
        // Anything already built was built without these.
        BY_REGION.clear();
        return problems;
    }

    /** How many extra names a drop-in added for a region, given half. */
    public static synchronized int extrasFor(String regionId, boolean given) {
        return (given ? EXTRA_ALPHA : EXTRA_BETA).getOrDefault(regionId, List.of()).size();
    }

    /** Forget every drop-in and every cached generator. Tests only. */
    public static synchronized void resetForTesting() {
        EXTRA_ALPHA.clear();
        EXTRA_BETA.clear();
        BY_REGION.clear();
    }

    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";

    private static String resourceFor(String regionId, String half) {
        return "/horsegenetics/names/people/" + regionId + "-" + half + ".txt";
    }

    /** The same comma-separated format as {@link #readNames(String)}, off disk. */
    private static List<String> readNames(Path file) throws IOException {
        return splitNames(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
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
                return splitNames(all.toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read name resource: " + resource, e);
        }
    }

    /** Comma-separated, blanks dropped, duplicates kept. */
    private static List<String> splitNames(String text) {
        List<String> names = new ArrayList<>();
        for (String part : text.split(",")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
