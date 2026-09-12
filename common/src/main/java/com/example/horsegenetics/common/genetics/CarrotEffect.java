package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One thing a fed <b>breeding carrot</b> (roadmap wiki &sect;14) does to the
 * gamete a parent contributes. A carrot item / a live carrot window carries a
 * <i>list</i> of these (multiple carrots crafted together, &sect;14.3);
 * {@link #fold} turns the list into the single {@link GameteBias} the breeding
 * draw takes.
 *
 * <p>Pure {@code common/} - the translator only builds these and serialises
 * them; nothing here touches a game.
 */
public sealed interface CarrotEffect {

    /** A stable token for serialisation and tooltips. */
    String id();

    /**
     * The <b>Unknown Epigenetic Splice</b> carrot - re-roll the epigenetic seed
     * of every copy this parent passes on. The alleles are untouched.
     */
    record EpigeneticSplice() implements CarrotEffect {
        @Override public String id() { return "epigenetic_splice"; }
    }

    /** <i>Stabilizer</i> - contribute the dominant (earlier-declared) copy at every heterozygous locus. */
    record Stabilizer() implements CarrotEffect {
        @Override public String id() { return "stabilizer"; }
    }

    /** <i>Magnifier</i> - contribute the recessive (later-declared) copy at every heterozygous locus. */
    record Magnifier() implements CarrotEffect {
        @Override public String id() { return "magnifier"; }
    }

    /**
     * An <b>Unknown Gene Splice</b> carrot - pick one random gene and draw this
     * parent's gamete for it from that gene's splice distribution
     * ({@link Gene#spliceTable()}, or a uniform draw over its viable pairs if it
     * declares none). The gene and the pair are rolled at breeding time off the
     * foal's own deterministic RNG.
     *
     * <p>{@code category} narrows <i>which</i> loci it can land on -
     * {@link SpliceCategory#ANY} is the original carrot and rolls the whole safe
     * pool; the five themed carrots roll one slice of it, so a player who wanted
     * a surprise colour is not handed a jumping stat. A themed pool is always a
     * subset of the safe pool, so no carrot in this family can damage a foal.
     */
    record GeneSplice(SpliceCategory category) implements CarrotEffect {

        public GeneSplice {
            category = category == null ? SpliceCategory.ANY : category;
        }

        /** The unthemed carrot, and the token it has always written. */
        public GeneSplice() {
            this(SpliceCategory.ANY);
        }

        @Override public String id() {
            return category == SpliceCategory.ANY ? "gene_splice" : "gene_splice:" + category.id();
        }
    }

    /**
     * A <i>Known Gene Splice carrot</i> - treat this parent as carrying exactly
     * {@code alleleA}/{@code alleleB} for that one gene's gamete. Normal
     * Mendelian rules apply from there, so the foal draws one of the two.
     *
     * <h2>It names the alleles, not just the gene</h2>
     * This used to be a {@code boolean homozygous} beside the gene key, and the
     * pair was built by taking {@code gene.alleles().get(0)} - the
     * <b>first-declared</b> allele - as "the variant". That is fine for a
     * two-allele locus and silently useless for anything wider: the
     * <a href="https://example.invalid">particle</a> locus has forty alleles and
     * only ever spliced the first one, so no carrot could ever hand over a soul
     * flame, and KIT could only ever hand over one of its thirteen.
     *
     * <p>Naming both alleles also makes "heterozygous or homozygous" fall out
     * rather than being a flag: {@code n}/{@code X} is the old het,
     * {@code X}/{@code X} the old hom, and {@code X}/{@code Y} is a thing the
     * old shape could not say at all.
     */
    record KnownGeneSplice(String geneKey, String alleleA, String alleleB) implements CarrotEffect {
        @Override public String id() { return "known:" + geneKey + ":" + alleleA + ":" + alleleB; }
    }

    // ------------------------------------------------------------------
    // Serialisation - a flat string token, so the item component and the live
    // carrot window are just a List<String>. No polymorphic codec needed.
    // ------------------------------------------------------------------

    static java.util.Optional<CarrotEffect> parse(String token) {
        if (token == null) {
            return java.util.Optional.empty();
        }
        switch (token) {
            case "epigenetic_splice": return java.util.Optional.of(new EpigeneticSplice());
            case "stabilizer": return java.util.Optional.of(new Stabilizer());
            case "magnifier": return java.util.Optional.of(new Magnifier());
            case "gene_splice": return java.util.Optional.of(new GeneSplice());
            default:
                if (token.startsWith("gene_splice:")) {
                    SpliceCategory category = SpliceCategory.byId(token.substring("gene_splice:".length()));
                    return category == null
                            ? java.util.Optional.empty()
                            : java.util.Optional.of(new GeneSplice(category));
                }
                if (token.startsWith("known:")) {
                    String[] p = token.split(":", 4);
                    if (p.length == 4) {
                        return java.util.Optional.of(new KnownGeneSplice(p[1], p[2], p[3]));
                    }
                }
                return java.util.Optional.empty();
        }
    }

    /** Parse a token list, silently dropping anything unrecognised (a retired gene, a bad edit). */
    static List<CarrotEffect> parseList(List<String> tokens) {
        List<CarrotEffect> out = new ArrayList<>();
        for (String t : tokens) {
            parse(t).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    static List<String> tokens(List<CarrotEffect> effects) {
        List<String> out = new ArrayList<>(effects.size());
        for (CarrotEffect e : effects) {
            out.add(e.id());
        }
        return List.copyOf(out);
    }

    /** True if this list contains both a stabilizer and a magnifier, or two gene carrots that disagree. */
    static boolean isContradictory(List<CarrotEffect> effects) {
        boolean stab = false;
        boolean magn = false;
        Map<String, String> magic = new LinkedHashMap<>();
        for (CarrotEffect e : effects) {
            if (e instanceof Stabilizer) {
                stab = true;
            } else if (e instanceof Magnifier) {
                magn = true;
            } else if (e instanceof KnownGeneSplice mg) {
                String pair = mg.alleleA() + "/" + mg.alleleB();
                String prev = magic.putIfAbsent(mg.geneKey(), pair);
                if (prev != null && !prev.equals(pair)) {
                    return true;
                }
            }
        }
        return stab && magn;
    }

    // ------------------------------------------------------------------

    /**
     * Collapse a carrot's effect list into one {@link GameteBias} for this
     * parent. {@code parentGenotype} is read only to size a uniform gene-splice
     * draw; {@code rng} is the foal's breeding RNG, so the result is
     * deterministic per foal. An empty list yields {@link GameteBias#NONE}.
     */
    static GameteBias fold(List<CarrotEffect> effects, Genotype parentGenotype, Rng rng) {
        if (effects.isEmpty()) {
            return GameteBias.NONE;
        }
        boolean reroll = false;
        Boolean prefer = null; // true = stabilizer, false = magnifier
        Map<String, AllelePair> subs = new LinkedHashMap<>();

        for (CarrotEffect e : effects) {
            if (e instanceof EpigeneticSplice) {
                reroll = true;
            } else if (e instanceof Stabilizer) {
                prefer = Boolean.TRUE;
            } else if (e instanceof Magnifier) {
                prefer = Boolean.FALSE;
            } else if (e instanceof GeneSplice gs) {
                Gene g = randomSpliceGene(gs.category(), rng);
                if (g != null) {
                    subs.put(g.key(), splicePair(g, gs.category(), rng));
                }
            } else if (e instanceof KnownGeneSplice mg) {
                Gene g = Genes.byKeyOrNull(mg.geneKey());
                AllelePair named = g == null ? null : magicPair(g, mg.alleleA(), mg.alleleB());
                if (named != null && g.hasGeneCarrot()) {
                    subs.put(g.key(), named);
                }
            }
        }

        return new GameteBias(reroll,
                prefer == null ? java.util.Optional.empty() : java.util.Optional.of(prefer),
                subs);
    }

    /**
     * The locus the Unknown Gene Splice carrot lands on - drawn from
     * {@link SpliceSafety#pool()}, which is every gene except the sex locus
     * (a carrot must not flip a foal's sex) and every gene that could make the
     * foal <b>worse off</b>: lethal, impairing, or simply short of hearts.
     *
     * <p>A player feeding a random-splice carrot has chosen a surprise, not a
     * dead foal. The deliberate route into a lethal genotype is the
     * <i>Known</i> Gene Splice carrot, where the gene is named and researched
     * first - that one is not filtered.
     *
     * <p>Returns {@code null} if nothing is safe, which cannot happen with any
     * real registry but must not throw if it does.
     */
    private static Gene randomSpliceGene(SpliceCategory category, Rng rng) {
        List<Gene> pool = SpliceCategory.pool(category);
        return pool.isEmpty() ? null : pool.get(rng.nextInt(pool.size()));
    }

    /**
     * The combination the carrot lands on. A theme may narrow the draw as well
     * as the pool - the performance carrot rolls only the combinations that make
     * a horse better, because "positive health splice" is what it says on the
     * item - and when it has no opinion the gene's own splice table decides, as
     * it always did.
     */
    private static AllelePair splicePair(Gene gene, SpliceCategory category, Rng rng) {
        List<AllelePair> narrowed = SpliceCategory.pairsFor(gene, category);
        if (!narrowed.isEmpty()) {
            return narrowed.get(rng.nextInt(narrowed.size()));
        }
        return gene.spliceTable()
                .map(t -> t.draw(rng))
                .orElseGet(() -> {
                    List<AllelePair> pairs = GenotypeCatalog.allPairsOf(gene);
                    return pairs.get(rng.nextInt(pairs.size()));
                });
    }

    /**
     * The pair a carrot names, or {@code null} if it does not name one this gene
     * has. A token the registry does not know is <b>dropped</b> rather than
     * guessed at: a carrot naming a retired allele should do nothing, not splice
     * whatever happens to sit at that index now.
     *
     * <p>A pair the gene says cannot occur is dropped for the same reason -
     * {@code canOccur} is where a locus states its own impossible combinations
     * (a sex-linked pair, a lethal the founder table excludes), and a carrot is
     * not a way round it.
     */
    private static AllelePair magicPair(Gene gene, String tokenA, String tokenB) {
        // Looked up rather than asked for: Gene.fromToken THROWS on a token the
        // gene does not have, and this runs inside breeding - a carrot naming a
        // retired allele must be inert, not take the foal down with it.
        Allele a = alleleOrNull(gene, tokenA);
        Allele b = alleleOrNull(gene, tokenB);
        if (a == null || b == null) {
            return null;
        }
        AllelePair pair = new AllelePair(a, b);
        return gene.canOccur(pair) ? pair : null;
    }

    private static Allele alleleOrNull(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        return null;
    }

    /**
     * The pair a carrot made for {@code gene} alone should name, while a
     * research paper still documents a whole gene rather than one allele. The
     * first-declared allele is the variant by convention, and
     * {@link Gene#geneCarrotHomozygous()} decides whether the other copy is the
     * same again or the wild type.
     */
    public static KnownGeneSplice defaultSpliceFor(Gene gene) {
        String variant = gene.alleles().get(0).token();
        String other = gene.geneCarrotHomozygous()
                ? variant
                : gene.defaultAllele().token();
        return new KnownGeneSplice(gene.key(), variant, other);
    }
}
