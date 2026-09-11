package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.CommonLog;
import com.example.horsegenetics.common.breed.spec.BreedSpecLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The breed registry: the files the mod ships, any a player has dropped in, and
 * {@link #FERAL_MIXED}, the label a horse with no herd identity carries.
 *
 * <h2>Breeds are data now</h2>
 * This class used to <i>be</i> the breeds - 885 lines of builder calls, one
 * method per breed. They live in
 * {@code common/src/main/resources/horsegenetics/breeds/} instead, one JSON file
 * each, listed in {@code index.json}; {@code wiki/breed-designer/} writes them
 * and {@code wiki/breed-format.html} documents them. The point is that adding a
 * breed is now something a player can do to a built jar, by putting a file in
 * {@code .minecraft/phc/breeds/}, and that changing one is a two-line diff
 * somebody can read.
 *
 * <p>The per-gene pool <b>rates</b> in those files are still estimates, chosen
 * so a herd reads as that breed without being a monoculture, and the biome
 * assignments and stat scores still come from the owner's breed sheet.
 * {@code wiki/breeds.html} is the readable version.
 *
 * <h2>Java breeds still exist</h2>
 * {@link #registerJava} is the escape hatch, and {@link #FERAL_MIXED} is the
 * only user of it. It is for a breed that needs <i>behaviour</i> - a magical
 * line whose founders are built by code rather than drawn from a table. A breed
 * that is a set of genes, biomes, bands and numbers must be a file: a Java one
 * cannot be edited, cannot be shared, and cannot be opened in the designer.
 *
 * <h2>When the files are read</h2>
 * Lazily, on first access, because a breed file names genes and the gene
 * registry is not complete until the host has loaded its own drop-ins. In
 * NeoForge that ordering is explicit - {@code ModGeneSpecs.load()} then
 * {@code ModBreedSpecs.load()}, both from the mod constructor - and the laziness
 * is what makes the same class work in a test and in the browser without either
 * having to know to call an initialiser.
 */
public final class Breeds {

    /**
     * The <b>Feral Mixed</b> breed - the label a horse with no herd identity
     * carries: a lone wild spawn, a {@code /summon}, a spawn-egg horse. Its
     * founder is the ordinary unconstrained roll.
     *
     * <p>It is <b>not</b> a breed a player can breed toward. {@link
     * BreedLineage#combine} treats it as <b>absorbing</b>: anything crossed with
     * a Feral Mixed horse is plain {@link BreedLineage#MIXED}, Feral Mixed
     * included. That is what the rename bought - as "Unknown" it behaved like an
     * ordinary distinct breed, so a Friesian bred to a wild loner produced a
     * "Friesian x Unknown cross" and the model had to answer whether Unknown was
     * a breed with a stat band, a pool and a claim to purity. Absorbing it into
     * Mixed makes the question disappear rather than answering it.
     *
     * <p>It is also the one breed that is still Java, and the reason the escape
     * hatch is kept: it is not a slice of the gene pool, it is the absence of
     * one, and there is nothing for a file to say.
     */
    public static final Breed FERAL_MIXED = Breed.of("feral_mixed", "Feral Mixed")
            .sources()  // no source: never spawns as itself, never has an egg
            .note("Lone wild spawns, /summon and spawn-egg horses. Every gene rolled unconstrained - the pre-breeds behaviour.")
            .note("Absorbing: any cross involving a Feral Mixed horse produces a Mixed foal.")
            .build();

    private static final List<Breed> ALL = new ArrayList<>();
    private static final Map<String, Breed> BY_ID = new LinkedHashMap<>();
    private static boolean builtinsLoaded;

    private Breeds() {
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Register a breed written in Java. See the class note: this is for a breed
     * that needs behaviour, not for one that is a table of numbers.
     */
    public static synchronized void registerJava(Breed b) {
        register(b);
    }

    private static void register(Breed b) {
        if (BY_ID.put(b.id(), b) != null) {
            throw new IllegalStateException("duplicate breed id " + b.id());
        }
        ALL.add(b);
    }

    /**
     * Read the breed files shipped inside the jar. Called automatically the
     * first time anything asks for a breed; safe to call again (it is a no-op).
     */
    public static synchronized void loadBuiltins() {
        if (builtinsLoaded) {
            return;
        }
        builtinsLoaded = true;
        accept(BreedSpecLoader.fromClasspath());
    }

    /**
     * Read a drop-in folder - {@code .minecraft/phc/breeds/} in game.
     * Returns everything worth telling the player about, most serious first:
     * files that would not parse, then loci that were skipped because this
     * install has not got the gene.
     */
    public static synchronized List<String> loadFrom(Path directory) {
        loadBuiltins();
        BreedSpecLoader.Result result = BreedSpecLoader.fromDirectory(directory);
        List<String> messages = new ArrayList<>(result.errors());
        messages.addAll(result.warnings());
        messages.addAll(accept(result));
        return messages;
    }

    /** Register what a load found, reporting the ids that collided. */
    private static List<String> accept(BreedSpecLoader.Result result) {
        List<String> problems = new ArrayList<>();
        for (String error : result.errors()) {
            CommonLog.warn(error);
        }
        for (String warning : result.warnings()) {
            CommonLog.warn(warning);
        }
        for (Breed breed : result.breeds()) {
            if (BY_ID.containsKey(breed.id())) {
                String message = "breed \"" + breed.id() + "\" is already registered - the later file is ignored";
                CommonLog.warn(message);
                problems.add(message);
                continue;
            }
            register(breed);
        }
        return problems;
    }

    /**
     * Register a <b>bundle</b> - a JSON array of breed objects - and mark the
     * built-ins as loaded, whatever the classpath holds.
     *
     * <p>This is the browser's path in. A TeaVM build cannot read the resource
     * folder the game reads breed files off, so the page fetches one bundled
     * file and hands the text here; marking the built-ins loaded is what stops
     * a later lazy {@link #loadBuiltins} finding nothing on the classpath and
     * concluding there are no breeds.
     *
     * @return everything worth telling the caller about, most serious first
     */
    public static synchronized List<String> registerBundle(String json, String source) {
        builtinsLoaded = true;
        List<String> messages = new ArrayList<>();
        List<Breed> parsed;
        try {
            parsed = com.example.horsegenetics.common.breed.spec.BreedSpecParser
                    .parseAll(json, source, messages::add);
        } catch (RuntimeException e) {
            String message = String.valueOf(e.getMessage());
            CommonLog.warn(message);
            messages.add(message);
            return messages;
        }
        messages.addAll(accept(new BreedSpecLoader.Result(parsed, List.of(), List.of())));
        return messages;
    }

    /** Forget every loaded breed. Tests only - nothing in the game re-reads the folder. */
    public static synchronized void resetForTesting() {
        ALL.clear();
        BY_ID.clear();
        builtinsLoaded = false;
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    public static List<Breed> all() {
        loadBuiltins();
        return List.copyOf(ALL);
    }

    public static Breed get(String id) {
        loadBuiltins();
        return BY_ID.getOrDefault(id, FERAL_MIXED);
    }

    public static Breed getOrFeral(Optional<String> id) {
        return id.map(Breeds::get).orElse(FERAL_MIXED);
    }

    public static String displayName(String id) {
        if (id == null || id.isBlank() || id.equals("feral_mixed")) {
            return "Feral Mixed";
        }
        loadBuiltins();
        Breed b = BY_ID.get(id);
        if (b != null) {
            return b.name();
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /** Breeds that can head a wild herd in {@code biomeId} (a "minecraft:plains" style string). */
    public static List<Breed> forBiome(String biomeId) {
        return forBiome(biomeId, BreedSource.WILD);
    }

    /**
     * Breeds that belong in {@code biomeId} and are allowed to come from
     * {@code source}. The cowboy asks with {@link BreedSource#COWBOY} and the
     * herd roll with {@link BreedSource#WILD}, which is the whole reason the
     * source is a parameter: "the country round here produces Fjords" and "a
     * dealer round here can get you a Fjord" are different claims, and a breed
     * may make one without the other.
     */
    public static List<Breed> forBiome(String biomeId, BreedSource source) {
        List<Breed> out = new ArrayList<>();
        for (Breed b : all()) {
            if (b.allows(source) && b.biomes().contains(biomeId)) {
                out.add(b);
            }
        }
        return out;
    }

    /** Every breed allowed to come from {@code source}, in registration order. */
    public static List<Breed> from(BreedSource source) {
        List<Breed> out = new ArrayList<>();
        for (Breed b : all()) {
            if (b.allows(source)) {
                out.add(b);
            }
        }
        return out;
    }
}
