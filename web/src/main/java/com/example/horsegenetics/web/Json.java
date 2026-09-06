package com.example.horsegenetics.web;

/**
 * A minimal JSON writer, because the browser side of the designer needs
 * structured answers and {@code common/} deliberately has no dependencies to
 * borrow one from.
 *
 * <p>It is a writer only. {@code common/genetics/spec/Json} is the reader, and
 * the two never meet: nothing the page sends back is JSON, because every edit
 * crosses the boundary as an int.
 */
final class Json {

    private final StringBuilder out = new StringBuilder(1 << 14);
    private boolean fresh = true;

    Json obj() {
        comma();
        out.append('{');
        fresh = true;
        return this;
    }

    Json endObj() {
        out.append('}');
        fresh = false;
        return this;
    }

    Json arr() {
        comma();
        out.append('[');
        fresh = true;
        return this;
    }

    Json endArr() {
        out.append(']');
        fresh = false;
        return this;
    }

    Json key(String name) {
        comma();
        string(name);
        out.append(':');
        fresh = true;
        return this;
    }

    Json val(String s) {
        comma();
        if (s == null) {
            out.append("null");
        } else {
            string(s);
        }
        fresh = false;
        return this;
    }

    Json val(long v) {
        comma();
        out.append(v);
        fresh = false;
        return this;
    }

    Json val(boolean v) {
        comma();
        out.append(v ? "true" : "false");
        fresh = false;
        return this;
    }

    /** Doubles are rounded to four places - this feeds a readout, not a computation. */
    Json val(double v) {
        comma();
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            out.append("null");
        } else {
            long scaled = Math.round(v * 10000.0);
            out.append(scaled / 10000L).append('.');
            long frac = Math.abs(scaled % 10000L);
            if (frac < 1000) {
                out.append('0');
            }
            if (frac < 100) {
                out.append('0');
            }
            if (frac < 10) {
                out.append('0');
            }
            out.append(frac);
        }
        fresh = false;
        return this;
    }

    Json kv(String name, String v) {
        return key(name).val(v);
    }

    Json kv(String name, long v) {
        return key(name).val(v);
    }

    Json kv(String name, boolean v) {
        return key(name).val(v);
    }

    Json kv(String name, double v) {
        return key(name).val(v);
    }

    private void comma() {
        char last = out.length() == 0 ? '\0' : out.charAt(out.length() - 1);
        if (!fresh && last != '{' && last != '[' && last != ':') {
            out.append(',');
        }
        fresh = false;
    }

    private void string(String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c > 0x7E) {
                        out.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
