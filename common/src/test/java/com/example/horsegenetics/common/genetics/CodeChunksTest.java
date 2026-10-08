package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The split rule is checked against {@code DataOutput.writeUTF} itself - the
 * call NBT makes - rather than against the number written in {@link CodeChunks},
 * so a wrong constant there fails here (issue #211).
 */
class CodeChunksTest {

    private static String ascii(int length) {
        StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            s.append((char) ('a' + i % 26));
        }
        return s.toString();
    }

    private static void writeUtf(String s) throws IOException {
        new DataOutputStream(new ByteArrayOutputStream()).writeUTF(s);
    }

    @Test
    void theLimitIsWhereWriteUtfPutsIt() throws IOException {
        String atLimit = ascii(CodeChunks.NBT_STRING_MAX_BYTES);
        String over = ascii(CodeChunks.NBT_STRING_MAX_BYTES + 1);
        writeUtf(atLimit);
        assertThrows(UTFDataFormatException.class, () -> writeUtf(over));
        assertTrue(CodeChunks.fitsOneString(atLimit));
        assertFalse(CodeChunks.fitsOneString(over));
    }

    @Test
    void fitsCountsBytesNotCharacters() {
        // 30,000 three-byte characters: well under the limit as characters, over it as bytes.
        String wide = "\u20AC".repeat(30000);
        assertFalse(CodeChunks.fitsOneString(wide));
        assertThrows(UTFDataFormatException.class, () -> writeUtf(wide));
        assertTrue(CodeChunks.fitsOneString("\u20AC".repeat(21845)));
        // NUL is two bytes in modified UTF-8.
        assertEquals(2, CodeChunks.modifiedUtf8Bytes("\u0000"));
    }

    @Test
    void aLongCodeSplitsIntoWritablePiecesAndComesBackWhole() throws IOException {
        for (String code : new String[] {
                ascii(CodeChunks.NBT_STRING_MAX_BYTES + 1), ascii(200000),
                "\u20AC".repeat(70000), ascii(CodeChunks.CHUNK_CHARS), ascii(1), ""}) {
            List<String> chunks = CodeChunks.split(code);
            assertFalse(chunks.isEmpty());
            for (String chunk : chunks) {
                writeUtf(chunk);
                assertTrue(CodeChunks.fitsOneString(chunk));
            }
            assertEquals(code, CodeChunks.join(chunks));
        }
    }
}
