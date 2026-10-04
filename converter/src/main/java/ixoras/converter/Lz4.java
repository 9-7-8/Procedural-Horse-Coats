package ixoras.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Decoder for the LZ4 "block stream" framing (lz4-java's LZ4BlockOutputStream), which is what Minecraft's
 * region files use for compression type 4. The converter only ever READS this; it writes zlib (type 2),
 * which every Minecraft version reads.
 *
 * <p>UNVERIFIED against 26.1.2: that type 4 is still this framing. The checksum is not verified (the
 * converter's own re-read of the output is the check). Any unknown block method fails loudly.
 */
final class Lz4 {
    private static final byte[] MAGIC = {'L', 'Z', '4', 'B', 'l', 'o', 'c', 'k'};

    private Lz4() {
    }

    static byte[] decodeStream(byte[] src) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(src.length * 3);
        int p = 0;
        while (p < src.length) {
            if (p + 21 > src.length) {
                throw new IOException("truncated LZ4 block header");
            }
            for (int i = 0; i < 8; i++) {
                if (src[p + i] != MAGIC[i]) {
                    throw new IOException("bad LZ4 block magic");
                }
            }
            int token = src[p + 8] & 0xFF;
            int comp = le32(src, p + 9);
            int decomp = le32(src, p + 13);
            p += 21; // magic 8 + token 1 + compressed 4 + decompressed 4 + checksum 4
            if (comp < 0 || decomp < 0 || p + comp > src.length) {
                throw new IOException("bad LZ4 block lengths");
            }
            if (decomp == 0) {
                break; // the end-of-stream block
            }
            int method = token & 0xF0;
            if (method == 0x10) {
                out.write(src, p, comp);
            } else if (method == 0x20) {
                byte[] block = new byte[decomp];
                blockDecompress(src, p, p + comp, block);
                out.write(block, 0, decomp);
            } else {
                throw new IOException("unknown LZ4 block method 0x" + Integer.toHexString(method));
            }
            p += comp;
        }
        return out.toByteArray();
    }

    private static int le32(byte[] b, int o) {
        return (b[o] & 0xFF) | (b[o + 1] & 0xFF) << 8 | (b[o + 2] & 0xFF) << 16 | (b[o + 3] & 0xFF) << 24;
    }

    /** The standard LZ4 block format: sequences of (token, literals, offset, match). */
    static void blockDecompress(byte[] src, int s, int end, byte[] dst) throws IOException {
        int d = 0;
        while (s < end) {
            int token = src[s++] & 0xFF;
            int lit = token >>> 4;
            if (lit == 15) {
                int b;
                do {
                    b = src[s++] & 0xFF;
                    lit += b;
                } while (b == 255);
            }
            if (d + lit > dst.length || s + lit > end) {
                throw new IOException("LZ4 literal run overruns");
            }
            System.arraycopy(src, s, dst, d, lit);
            s += lit;
            d += lit;
            if (s >= end) {
                break; // the last sequence has literals only
            }
            int off = (src[s] & 0xFF) | (src[s + 1] & 0xFF) << 8;
            s += 2;
            int len = token & 15;
            if (len == 15) {
                int b;
                do {
                    b = src[s++] & 0xFF;
                    len += b;
                } while (b == 255);
            }
            len += 4;
            if (off == 0 || off > d || d + len > dst.length) {
                throw new IOException("LZ4 match is out of range");
            }
            for (int i = 0; i < len; i++) { // byte by byte: matches may overlap their own output
                dst[d] = dst[d - off];
                d++;
            }
        }
        if (d != dst.length) {
            throw new IOException("LZ4 block decoded to " + d + " bytes, expected " + dst.length);
        }
    }
}
