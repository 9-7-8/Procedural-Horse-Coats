package com.example.horsegenetics.common.genetics.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader, written by hand because {@code common/} takes no
 * dependencies - not Gson, not Jackson, and certainly not Mojang's
 * {@code Codec}s (see the module rule in {@code CLAUDE.md}).
 *
 * <p>It parses the whole of JSON except the things a gene file has no use for:
 * there is no streaming mode and no lenient trailing-comma handling. Values come
 * back as the obvious Java types - {@link Map} (insertion-ordered),
 * {@link List}, {@link String}, {@link Double}, {@link Boolean}, {@code null} -
 * and {@link GeneSpecParser} does the typing.
 *
 * <p>Errors carry a line and column, because the reader on the other end of a
 * bad gene file is a person editing JSON by hand or debugging what the gene
 * creator emitted.
 */
public final class Json {

    private final String src;
    private int i;

    private Json(String src) {
        this.src = src;
    }

    /** Parse a whole document. Returns a Map / List / String / Double / Boolean / null. */
    public static Object parse(String text) {
        Json p = new Json(text);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (p.i < p.src.length()) {
            throw p.error("trailing content after the top-level value");
        }
        return value;
    }

    // ------------------------------------------------------------------

    private Object readValue() {
        if (i >= src.length()) {
            throw error("unexpected end of input");
        }
        char c = src.charAt(i);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readKeyword("true", Boolean.TRUE);
            case 'f' -> readKeyword("false", Boolean.FALSE);
            case 'n' -> readKeyword("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> out = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            i++;
            return out;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("object keys must be quoted strings");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            out.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return out;
            }
            if (c != ',') {
                throw error("expected ',' or '}' in object, got '" + c + "'");
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> out = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            i++;
            return out;
        }
        while (true) {
            skipWhitespace();
            out.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return out;
            }
            if (c != ',') {
                throw error("expected ',' or ']' in array, got '" + c + "'");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (i >= src.length()) {
                throw error("unterminated string");
            }
            char c = src.charAt(i++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i >= src.length()) {
                throw error("unterminated escape");
            }
            char e = src.charAt(i++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (i + 4 > src.length()) {
                        throw error("truncated \\u escape");
                    }
                    sb.append((char) Integer.parseInt(src.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> throw error("unknown escape '\\" + e + "'");
            }
        }
    }

    private Double readNumber() {
        int start = i;
        if (peek() == '-' || peek() == '+') {
            i++;
        }
        while (i < src.length() && (Character.isDigit(src.charAt(i)) || "+-.eE".indexOf(src.charAt(i)) >= 0)) {
            i++;
        }
        String text = src.substring(start, i);
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            throw error("not a number: '" + text + "'");
        }
    }

    private Object readKeyword(String word, Object value) {
        if (!src.startsWith(word, i)) {
            throw error("expected '" + word + "'");
        }
        i += word.length();
        return value;
    }

    // ------------------------------------------------------------------

    private void skipWhitespace() {
        while (i < src.length() && Character.isWhitespace(src.charAt(i))) {
            i++;
        }
    }

    private char peek() {
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private char next() {
        if (i >= src.length()) {
            throw error("unexpected end of input");
        }
        return src.charAt(i++);
    }

    private void expect(char c) {
        if (next() != c) {
            i--;
            throw error("expected '" + c + "'");
        }
    }

    private IllegalArgumentException error(String message) {
        int line = 1;
        int col = 1;
        for (int k = 0; k < Math.min(i, src.length()); k++) {
            if (src.charAt(k) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new IllegalArgumentException("JSON error at line " + line + ", column " + col + ": " + message);
    }
}
