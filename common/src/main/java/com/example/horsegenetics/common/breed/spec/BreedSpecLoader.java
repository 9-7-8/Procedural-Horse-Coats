package com.example.horsegenetics.common.breed.spec;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.genetics.spec.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Finds breed JSON files and turns them into {@link Breed}s - the exact twin of
 * {@code GeneSpecLoader}, deliberately, because the two answer the same question
 * and a player who has learnt one folder should not have to learn a second set
 * of rules for the other.
 *
 * <p>Two sources, and a build uses both:
 * <ul>
 *   <li>{@link #fromClasspath} - the breeds shipped <i>inside</i> the mod,
 *       listed in {@code horsegenetics/breeds/index.json}. A jar has no
 *       directory to walk, hence the index file.</li>
 *   <li>{@link #fromDirectory} - a real folder on disk, walked in filename
 *       order. This is the drop-in path: save the file the breed designer gave
 *       you into {@code .minecraft/phc/breeds/} and restart. No rebuild,
 *       no code, and no need for the jar to have been built with your breed in
 *       it.</li>
 * </ul>
 *
 * <p>A bad file is <b>not</b> fatal to the rest. Each is parsed on its own and
 * its failure collected into {@link Result#errors()}; a file that parses but
 * refers to genes this install has not got contributes a
 * {@link Result#warnings()} instead and still yields a breed. The caller
 * decides how loudly to complain about either.
 */
public final class BreedSpecLoader {

    /** Where {@link #fromClasspath()} looks, and what a mod jar should ship. */
    public static final String CLASSPATH_INDEX = "/horsegenetics/breeds/index.json";

    /** What {@link #fromDirectory} expects a breed file to be called. */
    public static final String EXTENSION = ".json";

    private BreedSpecLoader() {
    }

    /** What a load attempt found: the breeds that parsed, what was skipped, and what failed outright. */
    public record Result(List<Breed> breeds, List<String> warnings, List<String> errors) {

        public static final Result EMPTY = new Result(List.of(), List.of(), List.of());

        public Result merge(Result other) {
            List<Breed> b = new ArrayList<>(breeds);
            b.addAll(other.breeds);
            List<String> w = new ArrayList<>(warnings);
            w.addAll(other.warnings);
            List<String> e = new ArrayList<>(errors);
            e.addAll(other.errors);
            return new Result(List.copyOf(b), List.copyOf(w), List.copyOf(e));
        }

        public boolean ok() {
            return errors.isEmpty();
        }
    }

    // ------------------------------------------------------------------

    /**
     * Load every {@code .json} directly inside {@code dir}, in filename order so
     * two machines with the same folder load the same breeds in the same order.
     * A missing folder is not an error - most installs have none.
     */
    public static Result fromDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Result.EMPTY;
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION))
                    .filter(p -> !p.getFileName().toString().equals("index.json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(files::add);
        } catch (IOException e) {
            return new Result(List.of(), List.of(), List.of("could not list " + dir + ": " + e.getMessage()));
        }

        List<Breed> breeds = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                breeds.add(BreedSpecParser.parse(
                        Files.readString(file, StandardCharsets.UTF_8), name, warnings::add));
            } catch (IOException e) {
                errors.add("could not read " + file + ": " + e.getMessage());
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }
        return new Result(List.copyOf(breeds), List.copyOf(warnings), List.copyOf(errors));
    }

    /** Load the breeds listed in {@link #CLASSPATH_INDEX}, if that index exists. */
    public static Result fromClasspath() {
        return fromClasspath(CLASSPATH_INDEX);
    }

    public static Result fromClasspath(String indexResource) {
        String index = readResource(indexResource);
        if (index == null) {
            return Result.EMPTY;
        }
        String base = indexResource.substring(0, indexResource.lastIndexOf('/') + 1);
        List<Breed> breeds = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Object parsed;
        try {
            parsed = Json.parse(index);
        } catch (RuntimeException e) {
            return new Result(List.of(), List.of(), List.of(indexResource + ": " + e.getMessage()));
        }
        if (!(parsed instanceof List<?> names)) {
            return new Result(List.of(), List.of(), List.of(indexResource + ": expected an array of file names"));
        }
        for (Object name : names) {
            String resource = base + name;
            String text = readResource(resource);
            if (text == null) {
                errors.add("breed file listed in " + indexResource + " but missing: " + resource);
                continue;
            }
            try {
                breeds.add(BreedSpecParser.parse(text, String.valueOf(name), warnings::add));
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
            }
        }
        return new Result(List.copyOf(breeds), List.copyOf(warnings), List.copyOf(errors));
    }

    private static String readResource(String resource) {
        try (InputStream in = BreedSpecLoader.class.getResourceAsStream(resource)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
