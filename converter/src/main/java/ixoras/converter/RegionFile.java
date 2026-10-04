package ixoras.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Converts one Anvil region file (.mca), chunk by chunk, so a gigabyte world is never held in memory.
 *
 * <p>Format as read (UNVERIFIED against 26.1.2; the converter FAILS LOUDLY on anything it does not know
 * and never skips a chunk): an 8192-byte header (1024 four-byte locations: offset in 4096-byte sectors in
 * the top 3 bytes, sector count in the last; then 1024 four-byte timestamps), then chunks, each a 4-byte
 * length, a 1-byte compression type (1 gzip, 2 zlib, 3 none, 4 LZ4; add 128 when the payload lives in a
 * sibling "c.X.Z.mcc" file) and the data.
 *
 * <p>A chunk with no old-namespace token is copied byte for byte, keeping its compression. A chunk that
 * changes is rewritten as zlib (type 2). The output file is rebuilt with chunks laid end to end.
 */
final class RegionFile {
    private static final int SECTOR = 4096;
    /** A chunk of 255 sectors or more is stored externally (.mcc), as vanilla does. */
    private static final int MAX_INLINE = 255 * SECTOR - 5;

    private RegionFile() {
    }

    /** Result of converting one region file. */
    static final class Result {
        int chunks;
        int changedChunks;
        final java.util.Set<String> mccUsed = new java.util.HashSet<>();
    }

    /**
     * @param in      the source region file
     * @param out     the destination, or null for a scan that writes nothing
     * @param rewriter the rewrite rule; its count rises by the tokens found
     */
    static Result convert(Path in, Path out, Rewriter rewriter) throws IOException {
        Result res = new Result();
        long size = Files.size(in);
        if (size < 2 * SECTOR) {
            if (out != null) {
                Files.copy(in, out);   // an empty or header-less region: nothing in it to rewrite
            }
            return res;
        }
        int[] rc = regionCoords(in.getFileName().toString());
        try (RandomAccessFile src = new RandomAccessFile(in.toFile(), "r");
             RandomAccessFile dst = out == null ? null : new RandomAccessFile(out.toFile(), "rw")) {
            byte[] header = new byte[2 * SECTOR];
            src.readFully(header);
            DataInputStream hin = new DataInputStream(new ByteArrayInputStream(header));
            int[] loc = new int[1024];
            int[] stamp = new int[1024];
            for (int i = 0; i < 1024; i++) {
                loc[i] = hin.readInt();
            }
            for (int i = 0; i < 1024; i++) {
                stamp[i] = hin.readInt();
            }
            int[] newLoc = new int[1024];
            long nextSector = 2;
            if (dst != null) {
                dst.setLength(0);
                dst.write(new byte[2 * SECTOR]); // header placeholder, written at the end
            }
            for (int i = 0; i < 1024; i++) {
                if (loc[i] == 0) {
                    continue;
                }
                long offset = (long) (loc[i] >>> 8) * SECTOR;
                int sectors = loc[i] & 0xFF;
                if (offset < 2L * SECTOR || offset + 5 > size) {
                    throw new IOException(in + ": chunk " + i + " points outside the file");
                }
                src.seek(offset);
                int length = src.readInt();
                int comp = src.readUnsignedByte();
                if (length < 1 || offset + 4 + length > size && (comp & 0x80) == 0) {
                    throw new IOException(in + ": chunk " + i + " has a bad length " + length);
                }
                boolean external = (comp & 0x80) != 0;
                int type = comp & 0x7F;
                byte[] payload;
                Path mcc;
                if (external) {
                    int cx = rc[0] * 32 + (i & 31);
                    int cz = rc[1] * 32 + (i >> 5);
                    mcc = in.resolveSibling("c." + cx + "." + cz + ".mcc");
                    if (!Files.exists(mcc)) {
                        throw new IOException(in + ": chunk " + i + " is external but " + mcc.getFileName() + " is missing");
                    }
                    payload = Files.readAllBytes(mcc);
                    res.mccUsed.add(mcc.getFileName().toString());
                } else {
                    mcc = null;
                    payload = new byte[length - 1];
                    src.readFully(payload);
                }
                res.chunks++;
                long before = rewriter.count;
                byte[] plain = decompress(type, payload, in + " chunk " + i);
                Nbt.Root root = Nbt.readRoot(new DataInputStream(new ByteArrayInputStream(plain)));
                Object rewritten = rewriter.rewriteTag(root.value);
                boolean changed = rewriter.count != before;
                String mccName = "c." + (rc[0] * 32 + (i & 31)) + "." + (rc[1] * 32 + (i >> 5)) + ".mcc";
                if (!changed) {
                    if (dst != null) {
                        nextSector = writeChunk(dst, newLoc, i, nextSector, comp, external ? new byte[0] : payload,
                                external ? out.resolveSibling(mccName) : null, external ? payload : null);
                    }
                    continue;
                }
                res.changedChunks++;
                if (dst == null) {
                    continue;
                }
                ByteArrayOutputStream bo = new ByteArrayOutputStream(plain.length);
                try (DataOutputStream dout = new DataOutputStream(new DeflaterOutputStream(bo))) {
                    Nbt.writeRoot(dout, new Nbt.Root(root.name, (Nbt.Compound) rewritten));
                }
                byte[] z = bo.toByteArray();
                boolean extNow = z.length >= MAX_INLINE;
                nextSector = writeChunk(dst, newLoc, i, nextSector, extNow ? (2 | 0x80) : 2,
                        extNow ? new byte[0] : z, extNow ? out.resolveSibling(mccName) : null, extNow ? z : null);
            }
            if (dst != null) {
                ByteArrayOutputStream hb = new ByteArrayOutputStream(2 * SECTOR);
                DataOutputStream hd = new DataOutputStream(hb);
                for (int v : newLoc) {
                    hd.writeInt(v);
                }
                for (int v : stamp) {
                    hd.writeInt(v);
                }
                dst.seek(0);
                dst.write(hb.toByteArray());
            }
        }
        return res;
    }

    /**
     * Appends a chunk at the next free sector. {@code inline} is the bytes stored in the region file itself
     * (empty for an external chunk, whose payload {@code mccBytes} is written to {@code mccPath}).
     */
    private static long writeChunk(RandomAccessFile dst, int[] newLoc, int index, long nextSector, int comp,
                                   byte[] inline, Path mccPath, byte[] mccBytes) throws IOException {
        if (mccPath != null) {
            Files.write(mccPath, mccBytes);
        }
        int total = 4 + 1 + inline.length;
        int sectors = (total + SECTOR - 1) / SECTOR;
        if (sectors > 255) {
            throw new IOException("chunk " + index + " needs " + sectors + " sectors even after external storage");
        }
        dst.seek(nextSector * SECTOR);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(sectors * SECTOR);
        DataOutputStream d = new DataOutputStream(bo);
        d.writeInt(inline.length + 1);
        d.writeByte(comp);
        d.write(inline);
        d.write(new byte[sectors * SECTOR - total]);
        dst.write(bo.toByteArray());
        newLoc[index] = (int) (nextSector << 8) | sectors;
        return nextSector + sectors;
    }

    static byte[] decompress(int type, byte[] data, String where) throws IOException {
        switch (type) {
            case 1:
                return new GZIPInputStream(new ByteArrayInputStream(data)).readAllBytes();
            case 2:
                return new InflaterInputStream(new ByteArrayInputStream(data)).readAllBytes();
            case 3:
                return data;
            case 4:
                return Lz4.decodeStream(data);
            default:
                throw new IOException(where + ": unknown chunk compression type " + type + " (refusing to skip it)");
        }
    }

    private static int[] regionCoords(String fileName) throws IOException {
        // r.<x>.<z>.mca
        String[] p = fileName.split("\\.");
        if (p.length != 4 || !p[0].equals("r")) {
            throw new IOException("not a region file name: " + fileName);
        }
        try {
            return new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        } catch (NumberFormatException e) {
            throw new IOException("not a region file name: " + fileName);
        }
    }
}
