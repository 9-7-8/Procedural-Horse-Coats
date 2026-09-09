package com.example.horsegenetics.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>An unmodifiable map that is safe to read in the browser.</b>
 *
 * <p>{@code Map.of()} and {@code Map.copyOf(...)} are not, when the result is
 * <b>empty</b>. TeaVM's immutable-map implementation hashes the key into a
 * table it has not sized, so the first {@code get} on an empty one traps with
 * <i>remainder by zero</i> - in the wasm only. On a JVM the same call returns
 * {@code null} and everything works, which is exactly what made this expensive
 * to find: <a href="../../../../../../../wiki/known-gaps.html">gap 107</a> was
 * a gene page whose preview window never appeared, with a green build, a baked
 * icon, and every value it consumed verified from Java.
 *
 * <p>The gene that found it was {@code invert}, the only gene in the registry
 * with an {@code ALL} mask - and an {@code ALL} mask takes no parameters, so
 * its {@code Params} map is empty and the painter's first lookup on it killed
 * the bake. Nothing about invert was wrong.
 *
 * <p>So: any map in {@code common/} that is <b>read by key</b> and may be empty
 * is built here instead. The copy is a {@link LinkedHashMap} - ordinary,
 * lazily-tabled, and correct empty on both targets - wrapped so callers still
 * cannot write to it. (This is also what {@code common/}'s Java 8 target wants;
 * see hard rule 2 in {@code CLAUDE.md}.)
 */
public final class CommonMaps {

    private CommonMaps() {}

    /** An unmodifiable, insertion-ordered copy - empty or not. */
    public static <K, V> Map<K, V> copyOf(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /** The empty map, as a value the wasm can read. */
    public static <K, V> Map<K, V> empty() {
        return Collections.unmodifiableMap(new LinkedHashMap<K, V>());
    }
}
