package ixoras.converter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A synthetic world built in a temp folder, converted, and checked: zero old tokens afterwards, the original
 * byte-identical, folders renamed, a second run a no-op. Run it with {@code --self-test}. No game, no real
 * world, no network; it is the first thing to run once the converter is allowed to run at all.
 *
 * <p>WRITTEN, NEVER RUN. Covers gzip and zlib and raw chunks, a text file, a gzip .dat, an old-named folder, and
 * the LZ4 decoder against one hand-made block. It does NOT cover real 26.1.2 worlds.
 */
final class SelfTest {
    private SelfTest() {
    }

    static int run(PrintStream log) throws Exception {
        Path tmp = Files.createTempDirectory("ixoras-converter-selftest");
        try {
            int fails = 0;
            fails += lz4(log);
            Path world = tmp.resolve("World");
            build(world);
            int code = Main.convertWorld(world, "horsegenetics", "ixoras_horses", false, log);
            Path out = tmp.resolve("World (converted)");
            fails += check(log, "convert exits 0", code == 0);
            fails += check(log, "output exists", Files.isDirectory(out));
            fails += check(log, "folder renamed", Files.isDirectory(out.resolve("data/ixoras_horses")) && !Files.exists(out.resolve("data/horsegenetics")));
            WorldConverter again = new WorldConverter(out, "horsegenetics", "ixoras_horses", true, log);
            fails += check(log, "no old tokens in output", again.run(null) == 0 && again.errors.isEmpty());
            fails += check(log, "text file rewritten",
                    Files.readString(out.resolve("advancements/a.json")).contains("ixoras_horses:husbandry/x"));
            fails += check(log, "class-name-like text untouched",
                    Files.readString(out.resolve("advancements/a.json")).contains("com.example.horsegenetics.Thing"));
            Path world2 = out;
            int code2 = Main.convertWorld(world2, "horsegenetics", "ixoras_horses", false, log);
            fails += check(log, "second run is a no-op, exit 0", code2 == 0 && !Files.exists(tmp.resolve("World (converted) (converted)")));
            log.println(fails == 0 ? "SELF-TEST PASSED" : "SELF-TEST FAILED: " + fails);
            return fails == 0 ? 0 : 2;
        } finally {
            try (Stream<Path> s = Files.walk(tmp)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static int check(PrintStream log, String what, boolean ok) {
        log.println((ok ? "  ok    " : "  FAIL  ") + what);
        return ok ? 0 : 1;
    }

    private static int lz4(PrintStream log) throws IOException {
        // "aaaaaaaaaa" as one sequence: token 0x1A: 1 literal 'a', then match len 10-4=6... encoded by hand:
        // literal 'a', offset 1, match length 9 (token low nibble 5 => 5+4) => total 10 'a'.
        byte[] block = {0x15, 'a', 0x01, 0x00};
        byte[] dst = new byte[10];
        Lz4.blockDecompress(block, 0, block.length, dst);
        return check(log, "lz4 block decode", new String(dst, StandardCharsets.US_ASCII).equals("aaaaaaaaaa"));
    }

    private static void build(Path world) throws IOException {
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("data/horsegenetics"));
        Files.createDirectories(world.resolve("advancements"));
        Files.write(world.resolve("level.dat"), gzip(root("Data", "horsegenetics:horse_record", "horsegenetics.extension=E/e-horsegenetics.agouti=A/a")));
        Files.write(world.resolve("data/horsegenetics/stasis.dat"), gzip(root("data", "k", "horsegenetics:thing")));
        Files.writeString(world.resolve("advancements/a.json"),
                "{\"id\":\"horsegenetics:husbandry/x\",\"note\":\"com.example.horsegenetics.Thing\"}");
        // a region with three chunks, in three compression types
        byte[] c1 = zlib(root("", "a", "horsegenetics:one"));
        byte[] c2 = gzip(root("", "a", "plain, no tokens"));
        byte[] c3 = root("", "a", "horsegenetics:three");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bo);
        int[] loc = new int[1024];
        byte[][] body = {c1, c2, c3};
        int[] comp = {2, 1, 3};
        int sector = 2;
        byte[] chunks = new byte[0];
        ByteArrayOutputStream cb = new ByteArrayOutputStream();
        for (int i = 0; i < 3; i++) {
            int total = 5 + body[i].length;
            int sectors = (total + 4095) / 4096;
            DataOutputStream cd = new DataOutputStream(cb);
            cd.writeInt(body[i].length + 1);
            cd.writeByte(comp[i]);
            cd.write(body[i]);
            cd.write(new byte[sectors * 4096 - total]);
            loc[i] = (sector << 8) | sectors;
            sector += sectors;
        }
        for (int v : loc) {
            d.writeInt(v);
        }
        for (int i = 0; i < 1024; i++) {
            d.writeInt(0);
        }
        d.write(cb.toByteArray());
        Files.write(world.resolve("region/r.0.0.mca"), bo.toByteArray());
    }

    private static byte[] root(String name, String key, String value) throws IOException {
        Nbt.Compound c = new Nbt.Compound();
        c.put(key, value);
        Nbt.NList l = new Nbt.NList(Nbt.STRING);
        l.items.add(value);
        c.put("list", l);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (DataOutputStream d = new DataOutputStream(bo)) {
            Nbt.writeRoot(d, new Nbt.Root(name, c));
        }
        return bo.toByteArray();
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bo)) {
            g.write(raw);
        }
        return bo.toByteArray();
    }

    private static byte[] zlib(byte[] raw) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (DeflaterOutputStream g = new DeflaterOutputStream(bo)) {
            g.write(raw);
        }
        return bo.toByteArray();
    }
}
