package ixoras.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Datapack and resource-pack conversion. A pack (a folder or a .zip) that uses the old namespace is listed, the
 * player is asked yes/no per pack, and a converted copy is written NEXT TO the original, never over it:
 * folders assets/horsegenetics and data/horsegenetics become assets/ixoras_horses and data/ixoras_horses, and the
 * text files inside (json, mcmeta, lang, txt, mcfunction, snbt, properties) have their tokens rewritten.
 * Images, sounds and every other file are copied byte for byte.
 */
final class PackConverter {
    private static final Set<String> TEXT_EXT = Set.of("json", "mcmeta", "txt", "mcfunction", "snbt", "properties", "lang", "toml");

    private final Rewriter rewriter;
    private final String oldNs;
    private final String newNs;
    private final PrintStream log;

    PackConverter(String oldNs, String newNs, PrintStream log) {
        this.oldNs = oldNs;
        this.newNs = newNs;
        this.rewriter = new Rewriter(oldNs, newNs);
        this.log = log;
    }

    /** Does the pack use the old namespace (a namespace folder or a token in a text file)? */
    boolean usesOld(Path pack) throws IOException {
        if (Files.isDirectory(pack)) {
            if (Files.isDirectory(pack.resolve("assets").resolve(oldNs)) || Files.isDirectory(pack.resolve("data").resolve(oldNs))) {
                return true;
            }
            try (Stream<Path> s = Files.walk(pack)) {
                for (Path f : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) {
                    if (isText(f.getFileName().toString()) && rewriter.has(new String(Files.readAllBytes(f), StandardCharsets.UTF_8))) {
                        return true;
                    }
                }
            }
            return false;
        }
        try (ZipFile z = new ZipFile(pack.toFile())) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements(); ) {
                ZipEntry en = e.nextElement();
                if (en.getName().startsWith("assets/" + oldNs + "/") || en.getName().startsWith("data/" + oldNs + "/")) {
                    return true;
                }
                if (!en.isDirectory() && isText(en.getName())) {
                    try (InputStream in = z.getInputStream(en)) {
                        if (rewriter.has(new String(in.readAllBytes(), StandardCharsets.UTF_8))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Asks per pack (or says yes to all with {@code yes}) and writes "<name> (converted)" beside each accepted pack. */
    void convertAll(List<Path> packs, boolean yes, boolean dryRun, Scanner in) throws IOException {
        for (Path pack : packs) {
            if (!Files.exists(pack)) {
                log.println("pack not found, skipped: " + pack);
                continue;
            }
            if (!usesOld(pack)) {
                log.println("pack does not use " + oldNs + ", left alone: " + pack.getFileName());
                continue;
            }
            boolean go = yes;
            if (!yes) {
                log.print("Convert pack \"" + pack.getFileName() + "\" (copy written beside it)? [y/N] ");
                go = in.hasNextLine() && in.nextLine().trim().toLowerCase().startsWith("y");
            }
            if (!go || dryRun) {
                log.println((dryRun ? "dry run: would convert " : "skipped ") + pack.getFileName());
                continue;
            }
            Path abs = pack.toAbsolutePath().normalize();
            String fn = abs.getFileName().toString();
            boolean zip = !Files.isDirectory(abs);
            String stem = zip && fn.toLowerCase().endsWith(".zip") ? fn.substring(0, fn.length() - 4) : fn;
            Path out = abs.resolveSibling(stem + " (converted)" + (zip ? ".zip" : ""));
            if (Files.exists(out)) {
                throw new IOException("refusing to overwrite " + out);
            }
            if (zip) {
                convertZip(abs, out);
            } else {
                convertDir(abs, out);
            }
            log.println("wrote " + out);
        }
    }

    private static boolean isText(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 && TEXT_EXT.contains(name.substring(dot + 1).toLowerCase());
    }

    private String renameEntry(String name) {
        String[] parts = name.split("/", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i == 1 && parts[i].equals(oldNs) && (parts[0].equals("assets") || parts[0].equals("data"))) {
                parts[i] = newNs;
            }
        }
        return String.join("/", parts);
    }

    private void convertZip(Path in, Path out) throws IOException {
        try (ZipFile z = new ZipFile(in.toFile());
             ZipOutputStream zo = new ZipOutputStream(Files.newOutputStream(out))) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements(); ) {
                ZipEntry en = e.nextElement();
                ZipEntry ne = new ZipEntry(renameEntry(en.getName()));
                ne.setTime(en.getTime());
                zo.putNextEntry(ne);
                if (!en.isDirectory()) {
                    try (InputStream is = z.getInputStream(en)) {
                        byte[] data = is.readAllBytes();
                        if (isText(en.getName())) {
                            data = rewriter.rewrite(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                        }
                        zo.write(data);
                    }
                }
                zo.closeEntry();
            }
        }
    }

    private void convertDir(Path in, Path out) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(in)) {
            files = s.filter(Files::isRegularFile).toList();
        }
        for (Path f : files) {
            String rel = in.relativize(f).toString().replace('\\', '/');
            Path dest = out.resolve(renameEntry(rel));
            Files.createDirectories(dest.getParent());
            if (isText(f.getFileName().toString())) {
                String t = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                Files.write(dest, rewriter.rewrite(t).getBytes(StandardCharsets.UTF_8));
            } else {
                Files.copy(f, dest);
            }
        }
    }
}
