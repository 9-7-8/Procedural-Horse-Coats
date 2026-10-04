package ixoras.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Converts a world folder into a NEW folder, "MyWorld" to "MyWorld (converted)", never touching the original
 * (hard promise: the original is verified byte-identical afterwards). Nothing is ever deleted.
 *
 * <p>What it rewrites: region/entities/poi files (any depth, so dimension folders are covered), gzip or raw NBT
 * files (.dat, .dat_old, .nbt), and text files (.json, .mcmeta, .txt, .toml, .snbt, .properties, .mcfunction):
 * every old-namespace token becomes the new one. Any path segment that equals the old namespace is renamed
 * (data/horsegenetics to data/ixoras_horses, dimensions/horsegenetics to dimensions/ixoras_horses). Everything
 * else is copied untouched. Running it on an already converted world finds nothing and writes nothing.
 *
 * <p>UNVERIFIED against 26.1.2 (the building session proves each on a copy of a real world): where SavedData
 * files live and what the dimension folders look like; whether the file kinds below are the whole list; the
 * region compression types in use. Anything unreadable fails the run loudly rather than being skipped.
 */
final class WorldConverter {
    static final String VERSION = "0.1 (self-test passed on synthetic worlds; never run on a real world)";

    private static final Set<String> TEXT_EXT = Set.of("json", "mcmeta", "txt", "toml", "snbt", "properties", "mcfunction");
    private static final Set<String> NBT_EXT = Set.of("dat", "dat_old", "nbt");

    final Path world;
    final String oldNs;
    final String newNs;
    final boolean dryRun;
    final PrintStream log;
    final Rewriter rewriter;

    /** Per-kind counts for the report. */
    final Map<String, Long> filesByKind = new TreeMap<>();
    final Map<String, Long> tokensByKind = new TreeMap<>();
    final List<String> renamedPaths = new ArrayList<>();
    final List<String> errors = new ArrayList<>();
    long regionChunks;
    long regionChunksChanged;

    WorldConverter(Path world, String oldNs, String newNs, boolean dryRun, PrintStream log) {
        this.world = world;
        this.oldNs = oldNs;
        this.newNs = newNs;
        this.dryRun = dryRun;
        this.log = log;
        this.rewriter = new Rewriter(oldNs, newNs);
    }

    static Path outputFor(Path world) {
        Path abs = world.toAbsolutePath().normalize();
        return abs.resolveSibling(abs.getFileName() + " (converted)");
    }

    /** Is this a Minecraft world folder? */
    static boolean looksLikeWorld(Path p) {
        return Files.isRegularFile(p.resolve("level.dat"));
    }

    /**
     * Runs the conversion (or, in dry-run mode, the same walk without writing). Returns the total number of tokens
     * found. Never throws for one bad file: it records the error and carries on, so the report names every problem.
     */
    long run(Path out) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(world)) {
            files = s.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList();
        }
        Set<String> mccHandled = new java.util.HashSet<>();
        List<Path> mccFiles = new ArrayList<>();
        for (Path f : files) {
            Path rel = world.relativize(f);
            String name = f.getFileName().toString();
            if (name.equals("session.lock")) {
                continue; // a lock, not data; the game makes its own
            }
            if (name.endsWith(".mcc")) {
                mccFiles.add(f);
                continue;
            }
            Path destRel = renameSegments(rel);
            Path dest = out == null ? null : out.resolve(destRel.toString());
            if (dest != null) {
                Files.createDirectories(dest.getParent());
            }
            long before = rewriter.count;
            String kind = kindOf(name);
            try {
                switch (kind) {
                    case "region":
                        RegionFile.Result r = RegionFile.convert(f, dest, rewriter);
                        regionChunks += r.chunks;
                        regionChunksChanged += r.changedChunks;
                        for (String m : r.mccUsed) {
                            mccHandled.add(f.resolveSibling(m).toString());
                        }
                        break;
                    case "nbt":
                        convertNbtFile(f, dest);
                        break;
                    case "text":
                        convertTextFile(f, dest);
                        break;
                    default:
                        if (dest != null) {
                            Files.copy(f, dest);
                        }
                }
            } catch (IOException | RuntimeException e) {
                errors.add(rel + ": " + e.getMessage());
                continue;
            }
            filesByKind.merge(kind, 1L, Long::sum);
            long found = rewriter.count - before;
            if (found > 0) {
                tokensByKind.merge(kind, found, Long::sum);
            }
            if (!destRel.equals(rel)) {
                renamedPaths.add(rel + "  ->  " + destRel);
            }
        }
        for (Path m : mccFiles) {
            if (!mccHandled.contains(m.toString())) {
                // Not claimed by any region file: copy as is and say so; do not guess.
                log.println("warning: orphan " + world.relativize(m) + " copied unchanged");
                if (out != null) {
                    Path dest = out.resolve(renameSegments(world.relativize(m)).toString());
                    Files.createDirectories(dest.getParent());
                    Files.copy(m, dest);
                }
            }
        }
        return rewriter.count;
    }

    private static String kindOf(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
        if (ext.equals("mca")) {
            return "region";
        }
        if (NBT_EXT.contains(ext)) {
            return "nbt";
        }
        if (TEXT_EXT.contains(ext)) {
            return "text";
        }
        return "other";
    }

    /** Every path segment that is exactly the old namespace becomes the new one. */
    Path renameSegments(Path rel) {
        Path out = null;
        for (Path seg : rel) {
            String s = seg.toString();
            if (s.equals(oldNs)) {
                s = newNs;
            }
            out = out == null ? Path.of(s) : out.resolve(s);
        }
        return out;
    }

    private void convertNbtFile(Path in, Path out) throws IOException {
        byte[] raw = Files.readAllBytes(in);
        boolean gz = raw.length > 2 && (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B;
        byte[] plain = gz ? new GZIPInputStream(new ByteArrayInputStream(raw)).readAllBytes() : raw;
        Nbt.Root root = Nbt.readRoot(new DataInputStream(new ByteArrayInputStream(plain)));
        long before = rewriter.count;
        Object rewritten = rewriter.rewriteTag(root.value);
        if (out == null) {
            return;
        }
        if (rewriter.count == before) {
            Files.write(out, raw); // untouched bytes stay untouched
            return;
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream(plain.length);
        try (DataOutputStream d = new DataOutputStream(gz ? new GZIPOutputStream(bo) : bo)) {
            Nbt.writeRoot(d, new Nbt.Root(root.name, (Nbt.Compound) rewritten));
        }
        Files.write(out, bo.toByteArray());
    }

    private void convertTextFile(Path in, Path out) throws IOException {
        byte[] raw = Files.readAllBytes(in);
        String text = new String(raw, StandardCharsets.UTF_8);
        String changed = rewriter.rewrite(text);
        if (out != null) {
            Files.write(out, changed.equals(text) ? raw : changed.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** SHA-256 of every file under a folder, keyed by relative path: proof the original was not touched. */
    static Map<String, String> fingerprint(Path root) throws IOException {
        Map<String, String> m = new TreeMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(Files::isRegularFile).toList();
        }
        for (Path f : files) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (var in = Files.newInputStream(f)) {
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        md.update(buf, 0, n);
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : md.digest()) {
                    sb.append(String.format("%02x", b));
                }
                m.put(root.relativize(f).toString(), sb.toString());
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }
        return m;
    }

    /** Free space the output needs: about the world's size, plus a tenth. */
    static long neededBytes(Path world) throws IOException {
        long total = 0;
        try (Stream<Path> s = Files.walk(world)) {
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) {
                total += Files.size(p);
            }
        }
        return total + total / 10;
    }

    String report(Path out) {
        StringBuilder sb = new StringBuilder();
        sb.append("ixoras world converter ").append(VERSION).append('\n');
        sb.append("world:   ").append(world).append('\n');
        sb.append("output:  ").append(out == null ? "(dry run: nothing written)" : out.toString()).append('\n');
        sb.append("rewrite: ").append(oldNs).append(" -> ").append(newNs).append('\n');
        sb.append("files by kind:  ").append(filesByKind).append('\n');
        sb.append("tokens by kind: ").append(tokensByKind).append('\n');
        sb.append("region chunks:  ").append(regionChunks).append(" read, ").append(regionChunksChanged).append(" rewritten\n");
        sb.append("total tokens rewritten: ").append(rewriter.count).append('\n');
        if (!renamedPaths.isEmpty()) {
            sb.append("renamed paths:\n");
            for (String r : renamedPaths) {
                sb.append("  ").append(r).append('\n');
            }
        }
        if (!errors.isEmpty()) {
            sb.append("ERRORS (").append(errors.size()).append("):\n");
            for (String e : errors) {
                sb.append("  ").append(e).append('\n');
            }
        }
        return sb.toString();
    }
}
