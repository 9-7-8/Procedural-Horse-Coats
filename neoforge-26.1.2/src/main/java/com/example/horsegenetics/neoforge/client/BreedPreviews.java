package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genome;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>One representative horse per breed</b>, for the icon at the top of a
 * breed's entry.
 *
 * <h2>Why one and not a fresh roll</h2>
 * The same argument as {@link GenePreviews}: this is a reference entry, and an
 * entry whose picture changes each time you open it cannot be used to recognise
 * anything. A player who reads "Fjord" here and then meets a dun horse with a
 * dorsal stripe in the wild has learned something only if the picture held
 * still. So the seed is derived from the breed's own id and never moves.
 *
 * <p>It is still a <b>real founder roll</b> through {@link BreedFounder} - the
 * same call the world uses to make a wild herd - so the horse in the box is one
 * the breed could actually produce, drawn by the live coat pipeline. It is one
 * draw out of many, which is exactly what a plate in a field guide is, and the
 * entry says so rather than implying every Fjord looks like this.
 *
 * <p>Through {@link BreedFounder#plate}, not {@code roll}: the plate carries no
 * magical gene the breed does not name. A wild founder's stray dose of magic
 * put galaxy coats and the like on breeds that have nothing to do with them.
 */
public final class BreedPreviews {

    private static final Map<String, CoatData> COATS = new HashMap<>();
    private static final Map<String, Genome> GENOMES = new HashMap<>();

    private BreedPreviews() {
    }

    /** Dropped with the rest of the per-world client state. */
    public static void clear() {
        COATS.clear();
        GENOMES.clear();
    }

    /** The coat of this breed's representative horse, or {@code null} if it would not roll. */
    public static CoatData coatOf(Breed breed) {
        CoatData cached = COATS.get(breed.id());
        if (cached != null) {
            return cached;
        }
        Genome genome = genomeOf(breed);
        if (genome == null) {
            return null;
        }
        try {
            CoatData coat = new CoatData(genome.genotype(), genome.epigenome());
            COATS.put(breed.id(), coat);
            return coat;
        } catch (RuntimeException notPaintable) {
            return null;
        }
    }

    /** The representative horse itself - the entry reads its stats off this. */
    public static Genome genomeOf(Breed breed) {
        Genome cached = GENOMES.get(breed.id());
        if (cached != null) {
            return cached;
        }
        try {
            // Seeded from the id, so the plate for a breed is the same plate on
            // every world, every session and every player's machine.
            Genome genome = BreedFounder.plate(breed, new SeededRng(seedFor(breed.id())));
            GENOMES.put(breed.id(), genome);
            return genome;
        } catch (RuntimeException cannotRoll) {
            return null;
        }
    }

    /** A stable hash of the id. Written out rather than {@code String.hashCode} so it cannot drift. */
    private static long seedFor(String id) {
        long h = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < id.length(); i++) {
            h = h * 31L + id.charAt(i);
        }
        return h;
    }
}
