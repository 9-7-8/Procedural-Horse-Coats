package com.example.horsegenetics.common.genetics.epi;

import com.example.horsegenetics.common.SeededRng;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes one allele copy's {@link EpiValues} as <b>human-readable
 * text</b>, which is the whole reason this change was made: a stored epigenome
 * is meant to be something a player can open, read and edit, not a seed that
 * has to be replayed through a PRNG to mean anything.
 *
 * <pre>
 * delta:0.34215
 * cover:0.4113,shape:#8f3a21c4b70e1d55,site:2,coronet:[0.31|0.8|0.12|0.6]
 * </pre>
 *
 * <ul>
 *   <li>a plain number is a magnitude or a category index;</li>
 *   <li>{@code #} prefixes a noise seed, in unsigned hex;</li>
 *   <li>{@code [a|b|c|d]} is a per-leg value in {@code CoatRegions.LEGS} order.</li>
 * </ul>
 *
 * <h2>Why the gene separator is not a hyphen</h2>
 * The old positional format joined genes with {@code -}, which was safe when
 * every field was an unsigned integer or hex. Values are now signed - a
 * {@code Small} copy's delta is written {@code -0.31} - so a hyphen can no
 * longer double as a delimiter. Genes join with {@code ;} instead.
 *
 * <h2>Tolerant reads</h2>
 * A value the stored text does not mention is <b>rolled deterministically</b>
 * from the gene key and the value's name, so every horse in the world agrees on
 * it and a gene that gained a value mid-development does not make old horses
 * unreadable. Using the schema midpoint instead would have been simpler and
 * wrong: every horse would have received the identical noise seed, and every
 * splash in the world would have been the same shape.
 */
public final class EpiCodec {

    public static final char GENE_SEP = ';';
    public static final char COPY_SEP = '/';
    public static final char FIELD_SEP = ',';
    public static final char NAME_SEP = ':';
    public static final char SEED_MARK = '#';
    private static final char LEG_OPEN = '[';
    private static final char LEG_SEP = '|';
    private static final char LEG_CLOSE = ']';

    /**
     * Decimal places kept. Six is comfortably finer than the smallest drift step
     * any declared value can take ({@link EpiDrift#SCALE} of the narrowest design
     * span in the registry), so no value is frozen by rounding, and it keeps the
     * code short enough to paste.
     */
    private static final int DECIMALS = 6;
    private static final long POW10 = 1000000L;

    private EpiCodec() {
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    /** Append {@code values} as comma-separated {@code name:value} fields. */
    public static void write(StringBuilder sb, EpiValues values) {
        EpiSchema schema = values.schema();
        for (int i = 0; i < schema.size(); i++) {
            EpiValue v = schema.get(i);
            sb.append(FIELD_SEP).append(v.name()).append(NAME_SEP);
            if (v.kind() == EpiValue.Kind.SEED) {
                sb.append(SEED_MARK).append(Long.toUnsignedString(values.seed(v.name()), 16));
            } else if (v.arity() == 1) {
                sb.append(num(values.get(v.name())));
            } else {
                sb.append(LEG_OPEN);
                for (int leg = 0; leg < v.arity(); leg++) {
                    if (leg > 0) {
                        sb.append(LEG_SEP);
                    }
                    sb.append(num(values.get(v.name(), leg)));
                }
                sb.append(LEG_CLOSE);
            }
        }
    }

    /**
     * {@code v} rounded to exactly what {@link #num} can write.
     *
     * <p>Every stored value passes through this the moment it is created, so the
     * number a horse carries in memory is always <b>exactly</b> the number its
     * code says. That is what makes {@code parse(toCode())} an identity, and it
     * matters well beyond the round-trip test: a value that lost a digit on the
     * way to disk would give a horse a subtly different coat after a reload than
     * before it, and the two would fork the texture cache without ever looking
     * different enough for anyone to notice why.
     */
    public static double quantise(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0;
        }
        if (Math.abs(v) >= 1e12) {
            return (long) v;
        }
        return Math.round(v * POW10) / (double) POW10;
    }

    /**
     * A double, to {@value #DECIMALS} places with trailing zeros trimmed and no
     * exponent. Hand-rolled rather than {@code String.format} because
     * {@code common/} compiles to WebAssembly and to a Java 8 backport, and
     * because a locale-sensitive decimal separator would make a code string
     * unreadable on half the machines that opened it.
     */
    public static String num(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "0";
        }
        if (Math.abs(d) >= 1e12) {
            return Long.toString((long) d); // far past anything meaningful; no fraction to keep
        }
        long scaled = Math.round(d * POW10);
        if (scaled == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(16);
        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }
        sb.append(scaled / POW10);
        long frac = scaled % POW10;
        if (frac != 0) {
            String f = Long.toString(frac);
            int end = f.length();
            while (end > 0 && f.charAt(end - 1) == '0') {
                end--;
            }
            sb.append('.');
            for (int pad = f.length(); pad < DECIMALS; pad++) {
                sb.append('0');
            }
            sb.append(f, 0, end);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Parse comma-separated {@code name:value} fields into a lookup. Reserved
     * fields (the copy's priority) are returned alongside the value fields; the
     * caller pulls out what it owns.
     */
    public static Map<String, String> fields(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        if (text.isEmpty()) {
            return out;
        }
        int at = 0;
        while (at <= text.length()) {
            int end = text.indexOf(FIELD_SEP, at);
            if (end < 0) {
                end = text.length();
            }
            String field = text.substring(at, end);
            if (!field.isEmpty()) {
                int sep = field.indexOf(NAME_SEP);
                if (sep < 0) {
                    throw new IllegalArgumentException(
                            "epigenetic field needs '<name>:<value>', got: " + field);
                }
                out.put(field.substring(0, sep), field.substring(sep + 1));
            }
            at = end + 1;
        }
        return out;
    }

    /**
     * Build one copy's values from parsed {@code fields}. Anything the schema
     * declares but the text omits is rolled deterministically off
     * {@code namespace} (the gene key) and the value's name - see the class doc.
     */
    public static EpiValues read(EpiSchema schema, Map<String, String> fields, String namespace) {
        if (schema.isEmpty()) {
            return EpiValues.EMPTY;
        }
        double[] scalars = new double[schema.scalarWidth()];
        long[] seeds = new long[schema.size()];
        for (int i = 0; i < schema.size(); i++) {
            EpiValue v = schema.get(i);
            String text = fields.get(v.name());
            if (text == null || text.isEmpty()) {
                fill(schema, scalars, seeds, i, EpiRoll.founder(
                        EpiSchema.of(v), new SeededRng(hash(namespace, v.name()))));
                continue;
            }
            if (v.kind() == EpiValue.Kind.SEED) {
                seeds[i] = parseSeed(text);
            } else if (v.arity() == 1) {
                scalars[schema.scalarOffset(i)] = v.clamp(parseNum(text));
            } else {
                readLegs(v, text, scalars, schema.scalarOffset(i));
            }
        }
        return EpiValues.of(schema, scalars, seeds);
    }

    private static void readLegs(EpiValue v, String text, double[] scalars, int offset) {
        if (text.length() < 2 || text.charAt(0) != LEG_OPEN || text.charAt(text.length() - 1) != LEG_CLOSE) {
            // A per-leg value written as one number applies to every leg. Cheap
            // tolerance, and the obvious thing for a person editing by hand.
            double all = v.clamp(parseNum(text));
            for (int leg = 0; leg < v.arity(); leg++) {
                scalars[offset + leg] = all;
            }
            return;
        }
        String body = text.substring(1, text.length() - 1);
        int at = 0;
        for (int leg = 0; leg < v.arity(); leg++) {
            int end = body.indexOf(LEG_SEP, at);
            if (end < 0) {
                end = body.length();
            }
            scalars[offset + leg] = at <= body.length()
                    ? v.clamp(parseNum(body.substring(at, Math.max(at, end))))
                    : v.min();
            at = end + 1;
        }
    }

    /** Copy a single-value roll into the flat arrays at {@code index}. */
    private static void fill(EpiSchema schema, double[] scalars, long[] seeds, int index,
                             EpiValues rolled) {
        EpiValue v = schema.get(index);
        if (v.kind() == EpiValue.Kind.SEED) {
            seeds[index] = rolled.seed(v.name());
            return;
        }
        for (int leg = 0; leg < v.arity(); leg++) {
            scalars[schema.scalarOffset(index) + leg] = rolled.get(v.name(), leg);
        }
    }

    private static double parseNum(String s) {
        String t = s.trim();
        if (t.isEmpty()) {
            return 0;
        }
        // Double.valueOf, not parseDouble: this is the call the spec parser
        // already proves works under TeaVM.
        return Double.valueOf(t).doubleValue();
    }

    private static long parseSeed(String s) {
        String t = s.charAt(0) == SEED_MARK ? s.substring(1) : s;
        return parseUnsignedHex(t);
    }

    /**
     * Sixteen hex digits back to a {@code long}.
     *
     * <p>This is {@code Long.parseUnsignedLong(s, 16)} written out. That method
     * is not in TeaVM's class library and the wiki's horse designer compiles this
     * module to WebAssembly, so using it would mean the browser could bake a
     * horse but never read one back. It is also one fewer JDK method between
     * {@code common/} and the Java 8 backport.
     *
     * <p>The shift is what makes it <i>unsigned</i>: a seed uses the full 64-bit
     * range, so the top bit is data, not a sign.
     */
    public static long parseUnsignedHex(String s) {
        if (s.isEmpty() || s.length() > 16) {
            throw new IllegalArgumentException("an epigenetic seed is 1-16 hex digits, got: " + s);
        }
        long value = 0;
        for (int i = 0; i < s.length(); i++) {
            int digit = Character.digit(s.charAt(i), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("an epigenetic seed is not hex: " + s);
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    /** A stable seed for a missing value, so every horse in the world agrees on it. */
    private static long hash(String namespace, String name) {
        return ((long) namespace.hashCode() * 0x9E3779B97F4A7C15L) ^ name.hashCode();
    }
}
