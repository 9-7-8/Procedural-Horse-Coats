package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.trait.BreedStatTargets;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One horse breed: a constrained slice of the gene pool plus the metadata the
 * spawn and breeding systems need. A wild <b>herd</b> of a breed is rolled by
 * {@link BreedFounder} from these fields; a lone wild horse, a {@code /summon}
 * or a plain spawn-egg horse is {@link Breeds#FERAL_MIXED} instead and rolls the
 * ordinary unconstrained founder.
 *
 * <h2>Almost every breed is a JSON file</h2>
 * This type is the <b>parsed</b> form. The breeds the mod ships live in
 * {@code common/src/main/resources/horsegenetics/breeds/}, one file each, and a
 * player may drop more into {@code .minecraft/phc/breeds/} after the jar
 * is built - see {@code breed/spec/BreedSpecLoader} and
 * {@code wiki/breed-designer/}. Writing a breed in Java is still supported
 * ({@link Builder}) but is reserved for one that needs behaviour a data file
 * cannot express; a breed that is a set of genes, biomes, bands and numbers
 * belongs in a file, where somebody who does not build the mod can edit it.
 *
 * <h2>What a breed pins, and what it leaves alone</h2>
 * <ul>
 *   <li><b>Coat genes</b> it does not name are left <b>wild</b> - breeds are
 *       visually unified, so a Friesian is jet black because nothing switches
 *       any pattern or dilution on.</li>
 *   <li><b>Disorder genes</b> it does not name are <b>clear</b>: a breed only
 *       ever carries the disorders its breed sheet lists, in its pools.</li>
 *   <li><b>The four magical body-stat genes</b> are driven by
 *       {@link #statTargets()}: an axis with a {@link TargetBand} makes every
 *       founder carry that gene's pushing allele, and the gene lands the horse
 *       inside the band. Speed, jump and health are always homozygous; size is
 *       heterozygous for a founder that lands between 0.7x and 1.3x and
 *       homozygous outside it (see {@link BreedStatCurve#heterozygousSize}). An
 *       axis with no band is left wild.</li>
 *   <li><b>Any other gene's epigenetic numbers</b> may be pinned by
 *       {@link #bands()} - "deeply black", not merely "black". See
 *       {@link BreedBands}.</li>
 *   <li><b>Magical genes</b> it does not name are <b>wild</b>. There is no
 *       stray magic: only {@link Breeds#FERAL_MIXED} rolls random magic.</li>
 * </ul>
 *
 * <p>Real-world traits this mod cannot model yet - breed-specific mane shapes,
 * lower-leg feathering, head profiles, the metallic coat sheen - are recorded
 * in {@link #notes()} and surfaced on {@code wiki/breeds.html}. The list used
 * to name the leopard complex, tiger eye and HYPP; all three have since
 * shipped, which is exactly why the examples belong here and the inventory
 * belongs on the wiki page.
 */
public record Breed(
        String id,
        String name,
        boolean magical,
        List<String> biomes,
        double spawnWeight,
        Set<BreedSource> sources,
        Map<String, List<Combo>> genePools,
        StatScores scores,
        BreedBands bands,
        List<String> notes,
        Optional<PriceRange> price,
        String description,
        SpawnTime spawnTime) {

    /** One weighted allele combination in a breed's pool for a gene, as tokens. */
    public record Combo(String a, String b, double weight) {}

    /** A closed {@code lo..hi}; a single-number score is a zero-width one. */
    public record Range(double lo, double hi) {

        public Range {
            if (hi < lo) {
                double t = lo;
                lo = hi;
                hi = t;
            }
        }

        public static Range of(double one) {
            return new Range(one, one);
        }

        public boolean isPoint() {
            return lo == hi;
        }
    }

    /**
     * The breed sheet's <b>declared</b> numbers - three 1-10 scores and a size
     * range in multiples of the baseline horse - kept as written rather than only as the
     * {@link TargetBand}s {@link BreedStatCurve} turns them into.
     *
     * <p>They are kept because they are what a person edits. A band is
     * {@code [1.90, 2.10]}; the thing the breed sheet, the wiki page and the
     * breed designer all say is "speed 9". Storing the resolved band alone made
     * a breed file unreadable and a round trip through the designer lossy, so
     * the declaration is what is stored and the band is derived on demand
     * ({@link Breed#statTargets()}).
     */
    public record StatScores(Optional<Range> speed, Optional<Range> jump,
                             Optional<Range> health, Optional<Range> size) {

        public static final StatScores NONE =
                new StatScores(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public StatScores {
            speed = speed == null ? Optional.empty() : speed;
            jump = jump == null ? Optional.empty() : jump;
            health = health == null ? Optional.empty() : health;
            size = size == null ? Optional.empty() : size;
        }

        public boolean isEmpty() {
            return speed.isEmpty() && jump.isEmpty() && health.isEmpty() && size.isEmpty();
        }
    }

    /**
     * What one horse of this breed fetches, in emeralds: an inclusive range,
     * rolled per horse rather than fixed, so two of a breeder's Fjords are not
     * the same price to the copper. This is also what a signed transfer paper
     * for one costs at the cowboy's.
     *
     * <p>A breed that names no range is priced at the default - see
     * {@code HorsePrices}, which owns that number, because how much a horse
     * costs in emeralds is an economy decision and not a fact about the breed.
     * What a breed <i>does</i> get to say is "mine are dear", and that is what
     * this is for.
     */
    public record PriceRange(int min, int max) {
        public PriceRange {
            min = Math.max(1, min);
            max = Math.max(min, max);
        }
    }

    public Breed {
        biomes = List.copyOf(biomes);
        sources = sources == null ? BreedSource.ALL : Set.copyOf(sources);
        // Ordered copies, not Map.copyOf / Set.copyOf: those are deliberately
        // unordered, and these are written back out to a checked-in file by
        // BreedSpecWriter. An unordered copy makes that file's diff depend on
        // the JVM's hash seed, which is a regenerated-artefact trap of exactly
        // the kind CLAUDE.md's "regenerate what you invalidate" table exists for.
        genePools = ordered(genePools);
        scores = scores == null ? StatScores.NONE : scores;
        bands = bands == null ? BreedBands.NONE : bands;
        notes = List.copyOf(notes);
        price = price == null ? Optional.empty() : price;
        description = description == null ? "" : description;
        spawnTime = spawnTime == null ? SpawnTime.ANY : spawnTime;
    }

    private static Map<String, List<Combo>> ordered(Map<String, List<Combo>> pools) {
        Map<String, List<Combo>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Combo>> e : pools.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public boolean constrains(String geneKey) {
        return genePools.containsKey(geneKey);
    }

    /** May this breed turn up from {@code source}? */
    public boolean allows(BreedSource source) {
        return sources.contains(source);
    }

    /** Is there a breed spawn egg for this breed? */
    public boolean hasSpawnEgg() {
        return allows(BreedSource.SPAWN_EGG);
    }

    /**
     * The per-axis bands the founder roll pins, derived from {@link #scores()}.
     *
     * <p>Cheap enough to derive on each call - four comparisons and at most four
     * {@link BreedStatCurve} evaluations - and deriving is what keeps the
     * declared score and the resolved band from being able to disagree.
     */
    public BreedStatTargets statTargets() {
        if (scores.isEmpty()) {
            return BreedStatTargets.NONE;
        }
        BreedStatTargets.Builder b = BreedStatTargets.builder();
        scores.speed().ifPresent(r -> b.band(StatAxis.SPEED, BreedStatCurve.bandFor(StatAxis.SPEED, r.lo(), r.hi())));
        scores.jump().ifPresent(r -> b.band(StatAxis.JUMP, BreedStatCurve.bandFor(StatAxis.JUMP, r.lo(), r.hi())));
        scores.health().ifPresent(r -> b.band(StatAxis.HEALTH, BreedStatCurve.bandFor(StatAxis.HEALTH, r.lo(), r.hi())));
        scores.size().ifPresent(r -> b.band(StatAxis.SCALE, BreedStatCurve.sizeBand(r.lo(), r.hi())));
        return b.build();
    }

    /**
     * The breed's founder table for a gene it constrains. Tokens resolve against
     * the live registry.
     *
     * <p>A breed file's weights are <b>relative</b> - three pairs at 10 each is
     * a third apiece, which is how the breed designer writes them - while a
     * {@link FounderTable} takes percentages and complains about any other
     * total. So they are scaled to 100 here, once, rather than letting every
     * founder roll log the same "does not sum to 100" error.
     */
    public FounderTable founderTable(String geneKey) {
        Gene gene = Genes.byKey(geneKey);
        List<Combo> pool = genePools.get(geneKey);
        double total = 0.0;
        for (Combo c : pool) {
            total += c.weight();
        }
        double scale = total > 0.0 ? 100.0 / total : 1.0;
        FounderTable.Builder b = FounderTable.builder();
        for (Combo c : pool) {
            b.weight(alleleOf(gene, c.a()), alleleOf(gene, c.b()), c.weight() * scale);
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
        private boolean magical = false;
        private final List<String> biomes = new ArrayList<>();
        private double spawnWeight = Commonness.MODERATE.weight;
        private final Set<BreedSource> sources = EnumSet.noneOf(BreedSource.class);
        /** Whether {@link #sources} was named at all - an empty list is a real answer. */
        private boolean sourcesNamed = false;
        private final Map<String, List<Combo>> pools = new LinkedHashMap<>();
        private Optional<Range> speed = Optional.empty();
        private Optional<Range> jump = Optional.empty();
        private Optional<Range> health = Optional.empty();
        private Optional<Range> size = Optional.empty();
        private final BreedBands.Builder bands = BreedBands.builder();
        private final List<String> notes = new ArrayList<>();
        private Optional<PriceRange> price = Optional.empty();
        private String description = "";
        private SpawnTime spawnTime = SpawnTime.ANY;

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

        public Builder spawnWeight(double w) {
            this.spawnWeight = w;
            return this;
        }

        /** Mark the breed magical rather than natural - a wiki/label distinction, not a mechanic. */
        public Builder magical() {
            this.magical = true;
            return this;
        }

        /**
         * Where this breed may come from. <b>Calling this at all</b> is the
         * answer, so {@code sources()} with no arguments means "nowhere" -
         * which is what {@link Breeds#FERAL_MIXED} is. A breed that never calls
         * it is allowed every source.
         */
        public Builder sources(BreedSource... which) {
            sourcesNamed = true;
            for (BreedSource s : which) {
                sources.add(s);
            }
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

        /** Pin one of a gene's epigenetic numbers to a band on every founder. */
        public Builder band(String geneKey, String valueName, double lo, double hi) {
            bands.band(geneKey, valueName, lo, hi);
            return this;
        }

        /** Lock one of a gene's noise seeds to a single value on every founder. */
        public Builder seed(String geneKey, String valueName, long seed) {
            bands.seed(geneKey, valueName, seed);
            return this;
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
                    .gene("horsegenetics.agouti", "a", "a", 25)
                    .shadeAny();
        }

        public Builder agoutiBayBias() {
            return gene("horsegenetics.agouti", "A", "A", 55)
                    .gene("horsegenetics.agouti", "A", "a", 38)
                    .gene("horsegenetics.agouti", "a", "a", 7)
                    .shadeAny();
        }

        public Builder agoutiBlack() {
            return fixed("horsegenetics.agouti", "a").shadeAny();
        }

        /**
         * The <b>shade</b> locus at its wild spread. Every one of the three
         * agouti helpers calls this, and that is not a convenience: shade is a
         * modifier a coat gene reads, so {@link BreedFounder} forces it wild on
         * any breed that does not name it - and a world where every breed horse
         * is shade-neutral and only feral mixed ones vary would be a bug nobody
         * would think to look for. A breed with a real shade preference (a
         * registry that culls bright bays, say) overrides it by naming the
         * locus again afterwards.
         *
         * <p>Even {@code agoutiBlack()} takes it: a black horse carries and
         * transmits shade like any other, and a breed that could never pass one
         * on would be the same bug one generation later.
         */
        public Builder shadeAny() {
            return gene("horsegenetics.shade", "ShL", "ShL", 9)
                    .gene("horsegenetics.shade", "ShL", "Sh", 27)
                    .gene("horsegenetics.shade", "ShL", "ShD", 15)
                    .gene("horsegenetics.shade", "Sh", "Sh", 20)
                    .gene("horsegenetics.shade", "Sh", "ShD", 22)
                    .gene("horsegenetics.shade", "ShD", "ShD", 7);
        }

        // --- stat scores ---

        public Builder speed(double score) {
            return speed(score, score);
        }

        public Builder speed(double lo, double hi) {
            this.speed = Optional.of(new Range(lo, hi));
            return this;
        }

        public Builder jump(double score) {
            this.jump = Optional.of(Range.of(score));
            return this;
        }

        public Builder health(double score) {
            this.health = Optional.of(Range.of(score));
            return this;
        }

        /** The breed's size range, in multiples of the baseline horse. Sets the body-scale band. */
        public Builder size(double lo, double hi) {
            this.size = Optional.of(new Range(lo, hi));
            return this;
        }

        /** A sentence or two for the Breeds tab - what a player reads about the breed in game. */
        public Builder description(String text) {
            this.description = text == null ? "" : text;
            return this;
        }

        /** When its wild herds may be founded - see {@link SpawnTime}. */
        public Builder spawnTime(SpawnTime time) {
            this.spawnTime = time == null ? SpawnTime.ANY : time;
            return this;
        }

        public Builder note(String s) {
            notes.add(s);
            return this;
        }

        /**
         * What a breeder asks for one, in emeralds, inclusive. Leave it unset
         * and {@code HorsePrices} uses its default - which is what almost every
         * breed should do. Set it for the handful whose reputation is the point.
         */
        public Builder price(int minEmeralds, int maxEmeralds) {
            this.price = Optional.of(new PriceRange(minEmeralds, maxEmeralds));
            return this;
        }

        public Breed build() {
            return new Breed(id, name, magical, biomes, spawnWeight,
                    sourcesNamed ? sources : BreedSource.ALL, pools,
                    new StatScores(speed, jump, health, size), bands.build(),
                    notes, price,
                    description, spawnTime);
        }
    }
}
