package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The regression guard for the "chestnut / bay horse renders as the plain white
 * template" bug: two different coats must never encode to the same texture id.
 * The old encoder lower-cased the key, which threw away the entire dominance
 * signal ({@code E} vs {@code e}, {@code W} vs {@code w}, ...).
 */
class CoatTextureIdTest {

    /** Every genotype expressible with the registered genes - all 3^9 of them. */
    private static List<Genotype> allGenotypes() {
        List<List<AllelePair>> perGene = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            List<Allele> as = g.alleles();
            Allele hi = as.get(0);
            Allele lo = as.get(as.size() - 1);
            perGene.add(List.of(new AllelePair(hi, hi), new AllelePair(hi, lo), new AllelePair(lo, lo)));
        }
        List<List<AllelePair>> combos = new ArrayList<>();
        combos.add(new ArrayList<>());
        for (List<AllelePair> options : perGene) {
            List<List<AllelePair>> next = new ArrayList<>();
            for (List<AllelePair> prefix : combos) {
                for (AllelePair p : options) {
                    List<AllelePair> extended = new ArrayList<>(prefix);
                    extended.add(p);
                    next.add(extended);
                }
            }
            combos = next;
        }
        List<Genotype> out = new ArrayList<>(combos.size());
        for (List<AllelePair> pairs : combos) {
            out.add(Genotype.of(pairs));
        }
        return out;
    }

    @Test
    void everyGenotypeGetsItsOwnTextureId() {
        Map<String, String> byId = new HashMap<>();
        int n = 0;
        for (Genotype g : allGenotypes()) {
            for (long seed : new long[]{0L, 7L, -1L}) {
                for (String age : new String[]{":adult", ":foal"}) {
                    String key = new CoatData(g, Epigenome.fromSeed(seed)).textureKey() + age;
                    String id = CoatTextureId.encode(key);
                    String clash = byId.put(id, key);
                    if (clash != null && !clash.equals(key)) {
                        throw new AssertionError("texture id collision on '" + id + "': " + clash + " vs " + key);
                    }
                    n++;
                }
            }
        }
        assertEquals(3 * 3 * 3 * 3 * 3 * 3 * 3 * 3 * 3, allGenotypes().size());
        assertTrue(n > 100_000, "expected every genotype x seed x age combination, got " + n);
    }

    @Test
    void encodedIdsUseOnlyLegalResourcePathCharacters() {
        for (Genotype g : allGenotypes()) {
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
                "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-Cr/N-N/N:adult",
                "e/e-a/a-W/W-T/t-Ch/c-Spl/spl-G/g-N/N-prl/N@18446744073709551615:foal",
                "",
                "..__@:/-")) {
            assertEquals(key, CoatTextureId.decode(CoatTextureId.encode(key)));
        }
    }

    @Test
    void dominanceSurvivesTheEncoding() {
        // the exact pair the old lower-casing encoder merged
        String white = Genotype.parse("E/E-A/A-W/W-t/t-c/c-spl/spl-g/g-N/N-N/N").toCode();
        String chestnut = Genotype.parse("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N").toCode();
        assertEquals(white.toLowerCase(java.util.Locale.ROOT), chestnut.toLowerCase(java.util.Locale.ROOT));
        org.junit.jupiter.api.Assertions.assertNotEquals(
                CoatTextureId.encode(white), CoatTextureId.encode(chestnut));
    }
}
