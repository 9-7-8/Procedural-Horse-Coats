package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.Genes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds gene JSON files and turns them into registered {@link SpecGene}s - the
 * "upload it to the game" half of the gene creator.
 *
 * <p>Two sources, and a build uses both:
 * <ul>
 *   <li>{@link #fromClasspath} - genes shipped <i>inside</i> the mod, listed in
 *       {@code horsegenetics/genes/index.json}. A jar has no directory to walk,
 *       hence the index file.</li>
 *   <li>{@link #fromDirectory} - a real folder on disk, walked in filename
 *       order. This is the drop-in path: save the file the creator gave you into
 *       the game's genes folder and restart. No rebuild, no code.</li>
 * </ul>
 *
 * <p>A bad file is <b>not</b> fatal to the rest. Each one is parsed on its own
 * and its failure collected into {@link Result#errors()}, so one typo does not
 * cost you the other nine genes - the caller decides how loudly to complain.
 * A file that parses but collides with an existing key is an error the same way.
 */
public final class GeneSpecLoader {

    /** Where {@link #fromClasspath()} looks, and what a mod jar should ship. */
    public static final String CLASSPATH_INDEX = "/horsegenetics/genes/index.json";

    /** What {@link #fromDirectory} expects a gene file to be called. */
    public static final String EXTENSION = ".json";

    private GeneSpecLoader() {}

    /** What a load attempt found: the specs that parsed, and why the rest didn't. */
    public record Result(List<GeneSpec> specs, List<String> errors) {

        public static final Result EMPTY = new Result(List.of(), List.of());

        public Result merge(Result other) {
            List<GeneSpec> s = new ArrayList<>(specs);
            s.addAll(other.specs);
            List<String> e = new ArrayList<>(errors);
            e.addAll(other.errors);
            return new Result(List.copyOf(s), List.copyOf(e));
        }

        public boolean ok() {
            return errors.isEmpty();
        }
    }

    // ------------------------------------------------------------------

    /**
     * Load every {@code .json} directly inside {@code dir}, in filename order so
     * two machines with the same folder load the same genes in the same order.
     * A missing folder is not an error - most installs have none.
     */
    public static Result fromDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Result.EMPTY;
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(files::add);
        } catch (IOException e) {
            return new Result(List.of(), List.of("could not list " + dir + ": " + e.getMessage()));
        }

        List<GeneSpec> specs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Path file : files) {
            try {
                specs.add(GeneSpecParser.parse(Files.readString(file, StandardCharsets.UTF_8),
                        file.getFileName().toString()));
            } catch (IOException e) {
                errors.add("could not read " + file + ": " + e.getMessage());
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }
        return new Result(List.copyOf(specs), List.copyOf(errors));
    }

    /** Load the genes listed in {@link #CLASSPATH_INDEX}, if that index exists. */
    public static Result fromClasspath() {
        return fromClasspath(CLASSPATH_INDEX);
    }

    public static Result fromClasspath(String indexResource) {
        String index = readResource(indexResource);
        if (index == null) {
            return Result.EMPTY;
        }
        String base = indexResource.substring(0, indexResource.lastIndexOf('/') + 1);
        List<GeneSpec> specs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Object parsed;
        try {
            parsed = Json.parse(index);
        } catch (RuntimeException e) {
            return new Result(List.of(), List.of(indexResource + ": " + e.getMessage()));
        }
        if (!(parsed instanceof List<?> names)) {
            return new Result(List.of(), List.of(indexResource + ": expected an array of file names"));
        }
        for (Object name : names) {
            String resource = base + name;
            String text = readResource(resource);
            if (text == null) {
                errors.add("gene file listed in " + indexResource + " but missing: " + resource);
                continue;
            }
            try {
                specs.add(GeneSpecParser.parse(text, String.valueOf(name)));
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }
        return new Result(List.copyOf(specs), List.copyOf(errors));
    }

    // ------------------------------------------------------------------

    /**
     * Register every spec in {@code result}, returning the errors - the parse
     * failures it already carried plus any key collision found on the way in.
     */
    public static List<String> register(Result result) {
        List<String> errors = new ArrayList<>(result.errors());
        for (GeneSpec spec : result.specs()) {
            try {
                Genes.register(new SpecGene(spec));
            } catch (RuntimeException e) {
                errors.add("could not register " + spec.key() + ": " + e.getMessage());
            }
        }
        return List.copyOf(errors);
    }

    /** The whole job: built-in gene files, then the drop-in folder. */
    public static List<String> loadAndRegister(Path dropInDirectory) {
        return register(fromClasspath().merge(fromDirectory(dropInDirectory)));
    }

    private static String readResource(String resource) {
        try (InputStream in = GeneSpecLoader.class.getResourceAsStream(resource)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
