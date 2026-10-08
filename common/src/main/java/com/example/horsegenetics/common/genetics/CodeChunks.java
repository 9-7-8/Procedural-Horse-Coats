package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A genome code that is too long for one saved string is saved as several.</b>
 *
 * <p>Minecraft writes every NBT string with {@code DataOutput.writeUTF}, which
 * <b>throws</b> above {@link #NBT_STRING_MAX_BYTES} bytes rather than truncating
 * (issue #211). An epigenome code stores literal values for every varying gene
 * and grows with each gene that has an epigenetic schema, so it is headed past
 * that line; the number is the JDK's and cannot be raised.
 *
 * <p>The rule lives here, game-free, so any version's save layer can use it:
 * a code that {@link #fitsOneString fits} is written as the one string it
 * always was, and only a longer one is written as the {@link #split} list. A
 * reader takes either shape, so no existing save changes at all.
 */
public final class CodeChunks {

    /** {@code DataOutput.writeUTF}'s limit: the length prefix is an unsigned short. */
    public static final int NBT_STRING_MAX_BYTES = 65535;

    /**
     * Characters per chunk. Modified UTF-8 spends at most three bytes on a
     * {@code char}, so a chunk can never pass {@link #NBT_STRING_MAX_BYTES}
     * whatever it holds.
     */
    public static final int CHUNK_CHARS = 16384;

    private CodeChunks() {
    }

    /** Whether {@code writeUTF} will take this string whole. */
    public static boolean fitsOneString(String code) {
        // Every char costs at least one byte and at most three.
        if (code.length() > NBT_STRING_MAX_BYTES) {
            return false;
        }
        if (code.length() * 3L <= NBT_STRING_MAX_BYTES) {
            return true;
        }
        return modifiedUtf8Bytes(code) <= NBT_STRING_MAX_BYTES;
    }

    /** The code in order, in pieces that each fit one saved string. Never empty. */
    public static List<String> split(String code) {
        List<String> chunks = new ArrayList<>();
        for (int at = 0; at < code.length(); at += CHUNK_CHARS) {
            chunks.add(code.substring(at, Math.min(code.length(), at + CHUNK_CHARS)));
        }
        if (chunks.isEmpty()) {
            chunks.add("");
        }
        return chunks;
    }

    /** The inverse of {@link #split}. */
    public static String join(List<String> chunks) {
        StringBuilder code = new StringBuilder();
        for (String chunk : chunks) {
            code.append(chunk);
        }
        return code.toString();
    }

    /** The byte count {@code writeUTF} checks: NUL is two bytes, and there are no four-byte forms. */
    static long modifiedUtf8Bytes(String s) {
        long bytes = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                bytes += 1;
            } else if (c <= 0x07FF) {
                bytes += 2;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }
}
