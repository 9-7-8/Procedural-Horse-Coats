package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.trait.BreedStatTargets;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One horse breed: a constrained slice of the gene pool plus the metadata the
 * spawn and breeding systems need. A wild <b>herd</b> of a breed is rolled by
 * {@link BreedFounder} from these fields; a lone wild horse, a {@code /summon}
 * or a spawn-egg horse is {@link Breeds#UNKNOWN} instead and rolls the ordinary
 * unconstrained founder.
 *
 * <h2>What a breed pins, and what it leaves alone</h2>
 * <ul>
 *   <li><b>Coat genes</b> it does not name are left <b>wild</b> - breeds are
 *       visually unified, so a Friesian is jet black because nothing switches
 *       any pattern or dilution on.</li>
 *   <li><b>Disorder genes</b> it does not name keep their <b>global</b> founder
 *       rates, so any bred line can still turn up a carrier - unless the breed
 *       is {@link Builder#hardy() hardy}, which forces every implemented
 *       disorder locus to clear.</li>
 *   <li><b>The four magical body-stat genes</b> are driven by
 *       {@link #statTargets()}: an axis with a {@link TargetBand} makes every
 *       founder homozygous for that gene's pushing allele, and the gene lands
 *       the horse inside the band. An axis with no band is left wild.</li>
 *   <li><b>Other magical genes</b> appear via the geometric {@link #magicChance}
 *       draw, honouring {@link #magicWhitelist} / {@link #magicBlacklist}.</li>
 * </ul>
 *
 * <p>Real-world traits this mod cannot model yet - the leopard complex, tiger
 * eye, HYPP, breed-specific mane shapes, feathering, head profiles - are
 * recorded in {@link #notes()} and surfaced on {@code wiki/breeds.html}.
 */
public record Breed(
        String id,
        String name,
        List<String> biomes,
        double spawnWeight,
        Map<String, List<Combo>> genePools,
        BreedStatTargets statTargets,
        double magicChance,
        Set<String> magicWhitelist,
        Set<String> magicBlacklist,
        boolean hardy,
        List<String> notes) {

    /** One weighted allele combination in a breed's pool for a gene, as tokens. */
    public record Combo(String a, String b, double weight) {}

    public Breed {
        biomes = List.copyOf(biomes);
        genePools = Map.copyOf(genePools);
        magicWhitelist = Set.copyOf(magicWhitelist);
        magicBlacklist = Set.copyOf(magicBlacklist);
        notes = List.copyOf(notes);
    }

    public boolean constrains(String geneKey) {
        return genePools.containsKey(geneKey);
    }

    /** The breed's founder table for a gene it constrains. Tokens resolve against the live registry. */
    public FounderTable founderTable(String geneKey) {
        Gene gene = Genes.byKey(geneKey);
        FounderTable.Builder b = FounderTable.builder();
        for (Combo c : genePools.get(geneKey)) {
            b.weight(alleleOf(gene, c.a()), alleleOf(gene, c.b()), c.weight());
        }
        return b.build();
    }

    private static Allele alleleOf(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new IllegalArgumentException("breed allele " + token + " unknown on gene " + gene.key());
    }

    public static Builder of(String id, String name) {
        return new Builder(id, name);
    }

    // ------------------------------------------------------------------

    public static final class Builder {
        private final String id;
        private final String name;
        private final List<String> biomes = new ArrayList<>();
        private double spawnWeight = Commonness.MODERATE.weight;
        private final Map<String, List<Combo>> pools = new LinkedHashMap<>();
        private final BreedStatTargets.Builder targets = BreedStatTargets.builder();
        private double magicChance = 0.20;
        private final Set<String> whitelist = new LinkedHashSet<>();
        private final Set<String> blacklist = new LinkedHashSet<>();
        private boolean hardy = false;
        private final List<String> notes = new ArrayList<>();

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder biomes(String... ids) {
            for (String s : ids) {
                biomes.add(s);
            }
            return this;
        }

        public Builder commonness(Commonness c) {
            this.spawnWeight = c.weight;
            return this;
        }

        /** Add a weighted combo (as allele tokens) to a gene's pool. Repeatable. */
        public Builder gene(String geneKey, String a, String b, double weight) {
            pools.computeIfAbsent(geneKey, k -> new ArrayList<>()).add(new Combo(a, b, weight));
            return this;
        }

        /** A gene fixed homozygous for one allele. */
        public Builder fixed(String geneKey, String token) {
            return gene(geneKey, token, token, 100.0);
        }

        // --- extension / agouti convenience ---

        public Builder extensionAny() {
            return gene("horsegenetics.extension", "E", "E", 42)
                    .gene("horsegenetics.extension", "E", "e", 44)
                    .gene("horsegenetics.extension", "e", "e", 14);
        }

        public Builder extensionBlackBias() {
            return gene("horsegenetics.extension", "E", "E", 62)
                    .gene("horsegenetics.extension", "E", "e", 33)
                    .gene("horsegenetics.extension", "e", "e", 5);
        }

        public Builder extensionChestnut() {
            return fixed("horsegenetics.extension", "e");
        }

        public Builder agoutiAny() {
            return gene("horsegenetics.agouti", "A", "A", 30)
                    .gene("horsegenetics.agouti", "A", "a", 45)
                    .gene("horsegenetics.agouti", "a", "a", 25);
        }

        public Builder agoutiBayBias() {
            return gene("horsegenetics.agouti", "A", "A", 55)
                    .gene("horsegenetics.agouti", "A", "a", 38)
                    .gene("horsegenetics.agouti", "a", "a", 7);
        }

        public Builder agoutiBlack() {
            return fixed("horsegenetics.agouti", "a");
        }

        // --- stat targets ---

        public Builder speed(double score) {
            return speed(score, score);
        }

        public Builder speed(double lo, double hi) {
            targets.band(StatAxis.SPEED, BreedStatCurve.bandFor(StatAxis.SPEED, lo, hi));
            return this;
        }

        public Builder jump(double score) {
            targets.band(StatAxis.JUMP, BreedStatCurve.bandFor(StatAxis.JUMP, score, score));
            return this;
        }

        public Builder health(double score) {
            targets.band(StatAxis.HEALTH, BreedStatCurve.bandFor(StatAxis.HEALTH, score, score));
            return this;
        }

        /** The breed's height range, in hands. Sets the body-scale band. */
        public Builder height(double loHh, double hiHh) {
            targets.band(StatAxis.SCALE, BreedStatCurve.scaleBand(loHh, hiHh));
            return this;
        }

        public Builder magicChance(double p) {
            this.magicChance = p;
            return this;
        }

        public Builder magicWhitelist(String... geneKeys) {
            for (String s : geneKeys) {
                whitelist.add(s);
            }
            return this;
        }

        public Builder magicBlacklist(String... geneKeys) {
            for (String s : geneKeys) {
                blacklist.add(s);
            }
            return this;
        }

        public Builder hardy() {
            this.hardy = true;
            return this;
        }

        public Builder note(String s) {
            notes.add(s);
            return this;
        }

        public Breed build() {
            return new Breed(id, name, biomes, spawnWeight, pools, targets.build(),
                    magicChance, whitelist, blacklist, hardy, notes);
        }
    }
}
