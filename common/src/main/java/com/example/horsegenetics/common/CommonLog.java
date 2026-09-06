package com.example.horsegenetics.common;

import java.util.function.Consumer;

/**
 * The one way {@code common/} says something went wrong.
 *
 * <p><b>Why this exists rather than {@code System.getLogger}.</b> That API is
 * Java 9+, and this module's whole reason for being is that it runs anywhere:
 * the stated long-term aim is a 1.12.2 backport, which is <b>Java 8</b>, and
 * the wiki's browser tools now compile this module to JavaScript with TeaVM,
 * whose class library does not have {@code java.lang.System.Logger} either.
 * A logging call is not worth costing the module either of those, so the
 * dependency is gone and the substitution is done here.
 *
 * <p>It is deliberately tiny. {@code common/} has exactly four warnings in it -
 * two about a gene declaring a priority outside its phase's band, two about
 * frequencies that do not sum to what they claim - and none of them is on a hot
 * path. Anything richer would be a logging framework, which is a dependency,
 * which is the thing this module does not have.
 *
 * <p>A host may take the messages over with {@link #setSink}: NeoForge can point
 * them at its own logger, a browser build at {@code console.warn}. The default
 * writes to {@code System.err}, which every one of those environments has.
 */
public final class CommonLog {

    private static volatile Consumer<String> sink = message -> System.err.println("[horsegenetics] " + message);

    private CommonLog() {
    }

    /**
     * Route warnings somewhere else - a mod loader's logger, the browser
     * console. Passing {@code null} restores the default {@code System.err}
     * sink.
     */
    public static void setSink(Consumer<String> newSink) {
        sink = newSink != null ? newSink
                : message -> System.err.println("[horsegenetics] " + message);
    }

    /**
     * Warn, with {@code {0}}-style placeholders filled from {@code args} - the
     * same call shape the {@code System.Logger} calls used, so the messages did
     * not have to be rewritten when this replaced it.
     */
    public static void warn(String pattern, Object... args) {
        sink.accept(format(pattern, args));
    }

    /** Substitute {@code {0}}, {@code {1}}, ... An index with no argument is left as written. */
    static String format(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            int close = c == '{' ? pattern.indexOf('}', i) : -1;
            int index = close > i ? parseIndex(pattern, i + 1, close) : -1;
            if (index < 0) {
                out.append(c);
                continue;
            }
            out.append(index < args.length ? String.valueOf(args[index]) : pattern.substring(i, close + 1));
            i = close;
        }
        return out.toString();
    }

    /** The digits between {@code from} and {@code end}, or -1 if that is not a plain number. */
    private static int parseIndex(String s, int from, int end) {
        if (from >= end) {
            return -1;
        }
        int value = 0;
        for (int i = from; i < end; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }
}
