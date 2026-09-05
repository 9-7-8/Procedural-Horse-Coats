package com.example.horsegenetics.common.genetics;

import java.util.Map;
import java.util.Optional;

/**
 * A <b>per-parent modifier on the breeding draw</b> - what a fed breeding carrot
 * (roadmap wiki &sect;14) does, expressed as data so {@code common/} owns it and
 * the translator just builds one.
 *
 * <p>The load-bearing rule (&sect;14, philosophy's determinism contract): a
 * carrot biases the <b>gamete</b> a parent contributes, it never rewrites the
 * parent's own genotype. So every field here changes only what
 * {@link Genome#breedWith(Genome, com.example.horsegenetics.common.Rng, GameteBias, GameteBias)}
 * draws <i>from</i> this parent for one foal:
 *
 * <ul>
 *   <li><b>{@code rerollEpigenetics}</b> - the <i>Unknown Epigenetic Splice</i>
 *       carrot: every allele copy this parent passes on gets a fresh epigenetic
 *       seed instead of the parent copy's own (a foal normally inherits the seed
 *       verbatim).</li>
 *   <li><b>{@code preferLowerOrder}</b> - <i>stabilizer</i> ({@code true}) and
 *       <i>magnifier</i> ({@code false}): when this parent's two copies for a
 *       gene differ, contribute the earlier-declared allele
 *       ({@link AllelePair#first()}, the variant / dominant one) or the
 *       later-declared one ({@link AllelePair#second()}, the baseline /
 *       recessive one) rather than the 50/50 coin. A no-op at a homozygous
 *       locus - which is exactly the "does nothing when the parent has no
 *       dominant allele" the carrot is documented to have.</li>
 *   <li><b>{@code substitutePairs}</b> - the <i>Unknown Gene Splice</i> carrot
 *       and every <i>magic gene carrot</i>: for the named gene, draw this
 *       parent's gamete from the substitute pair instead of its real one. Magic
 *       carrot for gene G &rarr; {@code {G: n/G}} (heterozygous) or
 *       {@code {G: G/G}} (homozygous); gene splice &rarr;
 *       {@code {G: <the pair the splice table rolled>}}. A substituted copy
 *       always gets a fresh epigenetic seed - there is no real parent copy
 *       behind it.</li>
 * </ul>
 *
 * <p>{@link #NONE} changes nothing, and a breed with {@code NONE} on both sides
 * is <b>byte-identical</b> to the plain {@link Genome#breedWith(Genome,
 * com.example.horsegenetics.common.Rng)} - no extra RNG draws - which is the
 * property the determinism golden test pins.
 */
public record GameteBias(boolean rerollEpigenetics,
                         Optional<Boolean> preferLowerOrder,
                         Map<String, AllelePair> substitutePairs) {

    public static final GameteBias NONE = new GameteBias(false, Optional.empty(), Map.of());

    public GameteBias {
        preferLowerOrder = preferLowerOrder == null ? Optional.empty() : preferLowerOrder;
        substitutePairs = substitutePairs == null ? Map.of() : Map.copyOf(substitutePairs);
    }

    public boolean isNone() {
        return !rerollEpigenetics && preferLowerOrder.isEmpty() && substitutePairs.isEmpty();
    }

    /** The pair this parent contributes a gamete from for {@code geneKey}. */
    public AllelePair pairFor(String geneKey, AllelePair real) {
        return substitutePairs.getOrDefault(geneKey, real);
    }

    public boolean substitutes(String geneKey) {
        return substitutePairs.containsKey(geneKey);
    }

    // --- builders for the common shapes -------------------------------

    public static GameteBias epigeneticSplice() {
        return new GameteBias(true, Optional.empty(), Map.of());
    }

    public static GameteBias stabilizer() {
        return new GameteBias(false, Optional.of(Boolean.TRUE), Map.of());
    }

    public static GameteBias magnifier() {
        return new GameteBias(false, Optional.of(Boolean.FALSE), Map.of());
    }

    public static GameteBias substituting(Map<String, AllelePair> pairs) {
        return new GameteBias(false, Optional.empty(), pairs);
    }

    /** Fold two biases (a combination carrot) into one. Later wins on the copy-pick; substitutes merge, later key wins. */
    public GameteBias merge(GameteBias other) {
        if (other.isNone()) {
            return this;
        }
        if (isNone()) {
            return other;
        }
        java.util.Map<String, AllelePair> subs = new java.util.LinkedHashMap<>(substitutePairs);
        subs.putAll(other.substitutePairs);
        Optional<Boolean> prefer = other.preferLowerOrder.isPresent() ? other.preferLowerOrder : preferLowerOrder;
        return new GameteBias(rerollEpigenetics || other.rerollEpigenetics, prefer, subs);
    }
}
