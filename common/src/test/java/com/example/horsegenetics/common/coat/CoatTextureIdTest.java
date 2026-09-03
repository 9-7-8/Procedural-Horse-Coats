package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.Epigenome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The regression guard for the "chestnut / bay horse renders as the plain white
 * template" bug: two different coats must never encode to the same texture id.
 * The old encoder lower-cased the key, which threw away the entire dominance
 * signal ({@code E} vs {@code e}, {@code W} vs {@code w}, ...).
 *
 * <p>Once there were only a handful of genes this walked <i>every</i> genotype
 * ({@code 3^genes}). That product is now far too large to enumerate, so the
 * checks run over a large deterministic <b>sample</b> - a fixed pseudo-random
 * draw plus a systematic per-gene sweep (each gene set to each of its pair
 * states with the rest wild, which is exactly where a dominance-folding bug
 * would bite) plus the whole {@link GenotypeCatalog}. Injectivity is really
 * proved by {@link CoatTextureId#decode} recovering the key exactly; the sample
 * is belt-and-braces.
 */
class CoatTextureIdTest {

    private static final int RANDOM_SAMPLE = 20_000;

    /** Distinct genotypes: a seeded random draw + a per-gene sweep + the catalogue. */
    private static List<Genotype> sample() {
        List<Gene> genes = Genes.codeOrder();
        Set<String> seen = new LinkedHashSet<>();
        List<Genotype> out = new ArrayList<>();

        java.util.function.Consumer<Genotype> add = g -> {
            if (seen.add(g.toCode())) {
                out.add(g);
            }
        };

        add.accept(Genotype.wildType());

        // per-gene sweep: one gene off wild type at a time, every pair it has.
        for (Gene g : genes) {
            for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
                add.accept(Genotype.of(pair));
            }
        }

        Random rng = new Random(20260902L);
        while (out.size() < RANDOM_SAMPLE) {
            List<AllelePair> pairs = new ArrayList<>(genes.size());
            for (Gene g : genes) {
                List<Allele> as = g.alleles();
                Allele x = as.get(rng.nextInt(as.size()));
                Allele y = as.get(rng.nextInt(as.size()));
                pairs.add(new AllelePair(x, y));
            }
            add.accept(Genotype.of(pairs));
        }
        return out;
    }

    @Test
    void everySampledGenotypeGetsAReversibleTextureId() {
        long[] n = {0};
        for (Genotype g : sample()) {
            for (long seed : new long[]{0L, 7L, -1L}) {
                for (String age : new String[]{":adult", ":foal"}) {
                    String key = new CoatData(g, Epigenome.fromSeed(seed)).textureKey() + age;
                    assertEquals(key, CoatTextureId.decode(CoatTextureId.encode(key)),
                            "id is not reversible, so two coats could collide on it: " + key);
                    n[0]++;
                }
            }
        }
        assertTrue(n[0] > 100_000, "expected a large sample, got " + n[0]);
    }

    /** The same claim from the other side: no two distinct genotypes land on one id. */
    @Test
    void noTwoSampledGenotypesShareATextureId() {
        Set<String> ids = new java.util.HashSet<>();
        List<Genotype> sample = sample();
        for (Genotype g : sample) {
            String id = CoatTextureId.encode(new CoatData(g, Epigenome.fromSeed(0L)).textureKey() + ":adult");
            assertTrue(ids.add(id), "texture id collision on " + id);
        }
        assertEquals(sample.size(), ids.size());
    }

    @Test
    void encodedIdsUseOnlyLegalResourcePathCharacters() {
        for (Genotype g : sample()) {
            String id = CoatTextureId.encode(new CoatData(g, Epigenome.fromSeed(12345L)).textureKey() + ":adult");
            for (int i = 0; i < id.length(); i++) {
                char c = id.charAt(i);
                boolean legal = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
                assertTrue(legal, "illegal resource-path char '" + c + "' in " + id);
            }
        }
    }

    @Test
    void roundTrips() {
        for (String key : List.of(
                "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-Cr/N-N/N-n/n-n/n:adult",
                "e/e-a/a-W/W-T/t-Ch/c-Spl/spl-G/g-N/N-prl/N-n/n-n/n@18446744073709551615:foal",
                "",
                "..__@:/-")) {
            assertEquals(key, CoatTextureId.decode(CoatTextureId.encode(key)));
        }
    }

    @Test
    void dominanceSurvivesTheEncoding() {
        // the exact shape the old lower-casing encoder merged: E/E-A/A-W/W-... vs
        // e/e-a/a-w/w-..., which are identical once case is thrown away.
        String white = Genotype.of(
                new AllelePair(Genes.EXTENSION.E, Genes.EXTENSION.E),
                new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A),
                new AllelePair(Genes.WHITE.W, Genes.WHITE.W)).toCode();
        String chestnut = Genotype.of(
                new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e),
                new AllelePair(Genes.AGOUTI.a, Genes.AGOUTI.a),
                new AllelePair(Genes.WHITE.w, Genes.WHITE.w)).toCode();
        assertEquals(white.toLowerCase(java.util.Locale.ROOT), chestnut.toLowerCase(java.util.Locale.ROOT));
        org.junit.jupiter.api.Assertions.assertNotEquals(
                CoatTextureId.encode(white), CoatTextureId.encode(chestnut));
    }
}
