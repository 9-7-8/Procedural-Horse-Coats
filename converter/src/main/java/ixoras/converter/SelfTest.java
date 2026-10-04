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
 * <p>Passed 2026-10-04 (javac into a scratch folder, run in a temp folder, nothing in the repo touched). Covers gzip,
 * zlib, raw, LZ4-framed and external (.mcc) chunks, a text file, a gzip .dat, an old-named folder, translation keys,
 * a dir pack and a zip pack, the original left byte-identical and a no-op second run. It does NOT cover real 26.1.2
 * worlds, oversize chunks or the world's real file kinds.
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
            fails += check(log, "translation key rewritten, longer word untouched",
                    Files.readString(out.resolve("advancements/a.json")).contains("item.ixoras_horses.whistle")
                            && Files.readString(out.resolve("advancements/a.json")).contains("xhorsegenetics:no"));
            fails += packs(log, tmp);
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
                "{\"id\":\"horsegenetics:husbandry/x\",\"key\":\"item.horsegenetics.whistle\",\"word\":\"xhorsegenetics:no\"}");
        // a region with three chunks, in three compression types
        byte[] c1 = zlib(root("", "a", "horsegenetics:one"));
        byte[] c2 = gzip(root("", "a", "plain, no tokens"));
        byte[] c3 = root("", "a", "horsegenetics:three");
        byte[] c4 = lz4Raw(root("", "a", "horsegenetics:four-lz4"));   // type 4, raw-method blocks
        byte[] c5 = zlib(root("", "a", "horsegenetics:five-external")); // lives in c.5.0.mcc, type 2 | 128
        Files.write(world.resolve("region/c.5.0.mcc"), c5);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bo);
        int[] loc = new int[1024];
        byte[][] body = {c1, c2, c3, c4, new byte[0]};
        int[] comp = {2, 1, 3, 4, 2 | 0x80};
        int[] slot = {0, 1, 2, 3, 5};
        int sector = 2;
        byte[] chunks = new byte[0];
        ByteArrayOutputStream cb = new ByteArrayOutputStream();
        for (int i = 0; i < 5; i++) {
            int total = 5 + body[i].length;
            int sectors = (total + 4095) / 4096;
            DataOutputStream cd = new DataOutputStream(cb);
            cd.writeInt(body[i].length + 1);
            cd.writeByte(comp[i]);
            cd.write(body[i]);
            cd.write(new byte[sectors * 4096 - total]);
            loc[slot[i]] = (sector << 8) | sectors;
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

    /** lz4-java block-stream framing with RAW-method blocks (token 0x10), then the empty end block. */
    private static byte[] lz4Raw(byte[] raw) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] magic = "LZ4Block".getBytes(StandardCharsets.US_ASCII);
        bo.write(magic);
        bo.write(0x10 | 9);
        le(bo, raw.length);
        le(bo, raw.length);
        le(bo, 0);
        bo.write(raw);
        bo.write(magic);
        bo.write(0x10 | 9);
        le(bo, 0);
        le(bo, 0);
        le(bo, 0);
        return bo.toByteArray();
    }

    private static void le(ByteArrayOutputStream bo, int v) {
        bo.write(v);
        bo.write(v >>> 8);
        bo.write(v >>> 16);
        bo.write(v >>> 24);
    }

    private static int packs(PrintStream log, Path tmp) throws IOException {
        Path pack = tmp.resolve("MyPack");
        Files.createDirectories(pack.resolve("assets/horsegenetics/lang"));
        Files.writeString(pack.resolve("assets/horsegenetics/lang/en_us.json"), "{\"item.horsegenetics.whistle\":\"Whistle\"}");
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"x\"}}");
        Path zip = tmp.resolve("Other.zip");
        try (java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            z.putNextEntry(new java.util.zip.ZipEntry("data/horsegenetics/tags/x.json"));
            z.write("{\"values\":[\"horsegenetics:a\"]}".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        new PackConverter("horsegenetics", "ixoras_horses", log).convertAll(java.util.List.of(pack, zip), true, false,
                new java.util.Scanner(""));
        int f = 0;
        f += check(log, "dir pack converted beside original", Files.exists(tmp.resolve("MyPack (converted)/assets/ixoras_horses/lang/en_us.json"))
                && Files.readString(tmp.resolve("MyPack (converted)/assets/ixoras_horses/lang/en_us.json")).contains("item.ixoras_horses.whistle"));
        f += check(log, "original pack untouched", Files.exists(pack.resolve("assets/horsegenetics/lang/en_us.json")));
        f += check(log, "zip pack converted beside original", Files.exists(tmp.resolve("Other (converted).zip")));
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(tmp.resolve("Other (converted).zip").toFile())) {
            java.util.zip.ZipEntry e = zf.getEntry("data/ixoras_horses/tags/x.json");
            f += check(log, "zip entry renamed and rewritten", e != null
                    && new String(zf.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8).contains("ixoras_horses:a"));
        }
        return f;
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
