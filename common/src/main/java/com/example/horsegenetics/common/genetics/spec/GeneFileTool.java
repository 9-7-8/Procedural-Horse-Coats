package com.example.horsegenetics.common.genetics.spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the <b>gene bundle</b> - every file listed in
 * {@code horsegenetics/genes/index.json}, concatenated into one JSON array.
 *
 * <p>It exists for the same reason {@code BreedFileTool}'s second output does:
 * the game reads its genes off a classpath index, and the browser cannot. The
 * wiki's designer fetches one file instead, and because that file is generated
 * from the mod's own resources rather than maintained beside them, the only way
 * for the tool to show a gene the game has not got is to forget to re-bake.
 * {@code CLAUDE.md}'s regeneration table lists it for exactly that reason.
 *
 * <p>Dev tooling. Nothing in the mod calls it at runtime.
 */
public final class GeneFileTool {

    private GeneFileTool() {}

    /** {@code args[0]} is where to write the bundle. */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: GeneFileTool <bundle.json>");
            System.exit(2);
            return;
        }
        Path out = Path.of(args[0]);
        String bundle = bundle();
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, bundle, StandardCharsets.UTF_8);
        System.out.println("wrote " + out.toAbsolutePath());
    }

    /**
     * The bundle text: the index's files, in index order, wrapped in
     * {@code [ ... ]}.
     *
     * <p>The files go in <b>verbatim</b> rather than being parsed and
     * re-written. A round trip through {@link GeneSpec} would silently drop
     * anything the parser defaults, and the point of the bundle is that the
     * browser gets the bytes the game gets.
     */
    public static String bundle() {
        List<String> files = index();
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < files.size(); i++) {
            String text = read("/horsegenetics/genes/" + files.get(i));
            if (text == null) {
                throw new IllegalStateException("gene file listed in index.json but missing: " + files.get(i));
            }
            sb.append(text.trim());
            sb.append(i == files.size() - 1 ? "\n" : ",\n");
        }
        return sb.append("]\n").toString();
    }

    private static List<String> index() {
        String text = read(GeneSpecLoader.CLASSPATH_INDEX);
        if (text == null) {
            return List.of();
        }
        if (!(Json.parse(text) instanceof List<?> names)) {
            throw new IllegalStateException(GeneSpecLoader.CLASSPATH_INDEX + ": expected an array of file names");
        }
        List<String> out = new ArrayList<>();
        for (Object name : names) {
            out.add(String.valueOf(name));
        }
        return out;
    }

    private static String read(String resource) {
        try (InputStream in = GeneFileTool.class.getResourceAsStream(resource)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
