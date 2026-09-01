package com.example.horsegenetics.common.coat;

/**
 * Encodes a {@link CoatData#textureKey()} into a token the game module can use
 * as a <b>resource path</b> - i.e. one built only from {@code [a-z0-9_.]}.
 *
 * <p>This exists because the obvious thing (lower-case the key and replace every
 * illegal character with {@code _}) is <b>catastrophically lossy here</b>: case
 * <i>is</i> the dominance encoding in a genotype code, so {@code E/e} and
 * {@code e/e}, {@code W/w} and {@code w/w}, {@code A/a} and {@code a/a} all
 * collapse onto the same token. Doing that folded all 19 683 genotypes onto 27
 * texture ids; every coat baked under an id overwrote (and closed) the previous
 * one, so a chestnut or a black horse would render whichever coat was baked
 * last - a plain white horse whenever that was a dominant-white {@code W_} one.
 *
 * <p>The encoding is therefore <b>injective</b>, and {@link #decode} proves it:
 * <ul>
 *   <li>{@code a-z} / {@code 0-9} - kept verbatim,</li>
 *   <li>{@code A-Z} - {@code '.'} then the lower-case letter,</li>
 *   <li>anything else - {@code '_'} then four lower-case hex digits.</li>
 * </ul>
 * {@code .} and {@code _} never appear un-escaped, so the two escapes can never
 * be confused with literal text.
 *
 * <p>Pure string maths - nothing here knows Minecraft exists. (The legal path
 * character set it targets is Minecraft's, which also allows {@code -} and
 * {@code /}; those are deliberately <i>not</i> emitted, so the token stays a
 * single path segment.)
 */
public final class CoatTextureId {

    private static final char UPPER_ESCAPE = '.';
    private static final char HEX_ESCAPE = '_';
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CoatTextureId() {}

    /** Encode any string into a {@code [a-z0-9_.]} token. Distinct inputs give distinct outputs. */
    public static String encode(String key) {
        StringBuilder sb = new StringBuilder(key.length() + 16);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(UPPER_ESCAPE).append((char) (c - 'A' + 'a'));
            } else {
                sb.append(HEX_ESCAPE)
                  .append(HEX[(c >> 12) & 0xF]).append(HEX[(c >> 8) & 0xF])
                  .append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
            }
        }
        return sb.toString();
    }

    /** Inverse of {@link #encode} - mostly so the round-trip can be asserted in a test. */
    public static String decode(String token) {
        StringBuilder sb = new StringBuilder(token.length());
        int i = 0;
        while (i < token.length()) {
            char c = token.charAt(i);
            if (c == UPPER_ESCAPE) {
                require(i + 1 < token.length(), token, i);
                char l = token.charAt(i + 1);
                require(l >= 'a' && l <= 'z', token, i);
                sb.append((char) (l - 'a' + 'A'));
                i += 2;
            } else if (c == HEX_ESCAPE) {
                require(i + 4 < token.length(), token, i);
                int v = 0;
                for (int k = 1; k <= 4; k++) {
                    int d = Character.digit(token.charAt(i + k), 16);
                    require(d >= 0, token, i);
                    v = (v << 4) | d;
                }
                sb.append((char) v);
                i += 5;
            } else {
                require((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'), token, i);
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static void require(boolean ok, String token, int at) {
        if (!ok) {
            throw new IllegalArgumentException("not a CoatTextureId token at index " + at + ": " + token);
        }
    }
}
