package com.example.horsegenetics.common.breed.spec;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes every registered {@link Breed} out as a file, plus the
 * {@code index.json} the classpath loader reads.
 *
 * <p>Its first job was a one-off: turning the 885-line {@code Breeds.java} into
 * 48 data files without 48 chances to mistype an allele weight. It is kept
 * because it stays useful - it is how a breed written in Java (the escape hatch
 * for one that needs real behaviour) is exported for somebody to edit, and it
 * is how the round-trip test gets its input.
 *
 * <p>{@code ./gradlew :common:bakeBreedFiles}
 */
public final class BreedFileTool {

    /** Spelled out rather than escaped, so a regex-driven edit of this file cannot eat it. */
    private static final String NL = String.valueOf((char) 10);

    private BreedFileTool() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "common/src/main/resources/horsegenetics/breeds");
        Path bundle = args.length > 1 ? Path.of(args[1]) : null;
        Files.createDirectories(dir);

        List<String> names = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        for (Breed breed : Breeds.all()) {
            if (breed.id().equals(Breeds.FERAL_MIXED.id())) {
                continue;   // not a breed anyone can spawn, breed toward or edit
            }
            String file = breed.id() + ".json";
            String body = BreedSpecWriter.write(breed);
            Files.writeString(dir.resolve(file), body, StandardCharsets.UTF_8);
            names.add(file);
            bodies.add(body.trim());
        }

        // The browser's copy: one array of the same objects. TeaVM cannot read
        // the folder above off a classpath, so the wiki tools fetch this and
        // hand the text to Breeds.registerBundle. It is the same content, so it
        // cannot say anything the files do not - which is the only arrangement
        // worth having two copies in.
        if (bundle != null) {
            Files.createDirectories(bundle.getParent());
            Files.writeString(bundle, "[" + NL + String.join("," + NL, bodies) + NL + "]" + NL,
                    StandardCharsets.UTF_8);
            System.out.println("wrote the browser bundle to " + bundle.toAbsolutePath());
        }

        StringBuilder index = new StringBuilder("[\n");
        for (int i = 0; i < names.size(); i++) {
            index.append("  \"").append(names.get(i)).append('"');
            index.append(i == names.size() - 1 ? "\n" : ",\n");
        }
        index.append("]\n");
        Files.writeString(dir.resolve("index.json"), index.toString(), StandardCharsets.UTF_8);

        System.out.println("wrote " + names.size() + " breed files to " + dir.toAbsolutePath());
    }
}
