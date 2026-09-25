package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>What one research paper is about: a gene and one allele pair.</b>
 *
 * <p>A paper used to name a whole gene, and that was the wrong unit. A breeder
 * with a horse in front of them is asking "what does {@code A/a} do here?" or
 * "what happens if I put these two together?" - not "what is agouti in
 * general". A pair is also what the {@code Known Gene Splice} carrot has always
 * wanted: {@link CarrotEffect.KnownGeneSplice} names <i>both</i> alleles, so a
 * whole-gene paper could only ever build the pair convention picked for it -
 * the first-declared allele, which on the forty-allele particle locus meant no
 * carrot could ever hand over a soul flame.
 *
 * <h2>The pair normalises</h2>
 * {@code A/a} and {@code a/A} are one topic, because {@link AllelePair} puts its
 * two slots in the gene's declaration order on construction. That is done here
 * too, whenever the gene is registered and knows both tokens, so
 * {@link #token()} is the single canonical key for a pair state and two papers
 * for the same combination stack.
 *
 * <h2>Strings, not {@link Allele} objects</h2>
 * A paper can outlive the gene it names - a datapack gene removed, a world
 * opened without the mod that added it - and a paper for a retired gene must be
 * inert, not an exception thrown inside an inventory. So the fields are the raw
 * tokens and every resolution ({@link #gene()}, {@link #pair()}) answers
 * {@code null} rather than throwing.
 */
public record ResearchTopic(String geneKey, String alleleA, String alleleB) {

    /** The separator inside {@link #token()}. Gene keys contain dots, never bars. */
    private static final String SEP = "|";

    public ResearchTopic {
        geneKey = geneKey == null ? "" : geneKey;
        alleleA = alleleA == null ? "" : alleleA;
        alleleB = alleleB == null ? "" : alleleB;
        // Normalise while we can. A pair this build cannot resolve keeps the
        // order it arrived in, so its token still round-trips unchanged.
        Gene gene = Genes.byKeyOrNull(geneKey);
        Allele a = alleleOrNull(gene, alleleA);
        Allele b = alleleOrNull(gene, alleleB);
        if (a != null && b != null) {
            AllelePair canonical = new AllelePair(a, b);
            alleleA = canonical.first().token();
            alleleB = canonical.second().token();
        }
    }

    // ------------------------------------------------------------------
    // Building one
    // ------------------------------------------------------------------

    /** The topic a horse's actual combination at {@code gene} teaches. */
    public static ResearchTopic of(Gene gene, AllelePair pair) {
        return new ResearchTopic(gene.key(), pair.first().token(), pair.second().token());
    }

    /**
     * <b>What a paper that named a whole gene turns into:</b> two copies of that
     * gene's variant allele - the homozygous, non-wild pair (owner's call, so a
     * paper found before this change still says something a breeder can act on,
     * and says the strongest thing it could have meant).
     *
     * <p>Falls back to the carrier pair {@code variant/wild} on a locus whose
     * homozygote cannot occur - a KIT lethal white, {@code met/met}. A paper
     * naming an impossible pair would craft an inert carrot, which is worse than
     * a slightly weaker one.
     */
    public static ResearchTopic wholeGene(String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        return gene == null
                ? new ResearchTopic(geneKey, "", "")
                : firstViable(gene, true);
    }

    /**
     * The pair to quote for a gene when nobody is holding a paper - the browser's
     * recipe ghost, and a gene's own wiki card. The gene's own
     * {@code geneCarrotHomozygous()} call, since that is the convention the
     * carrot has always used; it is an <b>example</b>, not a claim about every
     * carrot for the locus.
     */
    public static ResearchTopic defaultFor(Gene gene) {
        return firstViable(gene, gene.geneCarrotHomozygous());
    }

    /**
     * The variant pair this gene would rather name, backing off to one it allows.
     *
     * <p>Both callers need the same ladder and for the same reason: a pair the
     * locus forbids is not a weaker answer, it is <b>no</b> answer - a carrot
     * built from it is silently inert ({@code CarrotEffect.fold} drops it), and a
     * wiki card quoting it is telling the player something untrue. So try the
     * preferred shape, then the other one, then a sex-linked locus's hemizygous
     * reading.
     */
    private static ResearchTopic firstViable(Gene gene, boolean preferHomozygous) {
        Allele variant = variantOf(gene);
        if (variant == null) {
            return new ResearchTopic(gene.key(), "", "");
        }
        AllelePair doubled = new AllelePair(variant, variant);
        AllelePair carrier = new AllelePair(variant, gene.defaultAllele());
        List<AllelePair> ladder = preferHomozygous
                ? List.of(doubled, carrier)
                : List.of(carrier, doubled);
        for (AllelePair candidate : ladder) {
            if (gene.canOccur(candidate) && gene.sexConsistent(candidate)) {
                return of(gene, candidate);
            }
        }
        if (gene.inheritance().sexLinked()) {
            AllelePair hemizygous = new AllelePair(variant, gene.hemizygousPlaceholder());
            if (gene.canOccur(hemizygous) && gene.sexConsistent(hemizygous)) {
                return of(gene, hemizygous);
            }
        }
        // Nothing this gene allows pairs its own variant allele with anything.
        // Return the preferred shape rather than throwing: an inert paper is a
        // gene-authoring bug to find, not a reason to break an inventory.
        return of(gene, ladder.get(0));
    }

    /**
     * <b>The pairs a paper found in a chest or bought from the supplier may
     * name:</b> for each of the gene's variant alleles, the carrier pair
     * {@code variant/wild} and the true-breeding pair {@code variant/variant}.
     *
     * <p>Deliberately not every combination the locus allows (owner's call). A
     * forty-allele locus has eight hundred pairs and one of them being in this
     * chest is not a find, it is noise; eighty is a collection. The compound
     * pairs {@code X/Y} still exist and still make a carrot - a
     * <b>horse</b> hands you one, which is the route that is supposed to be
     * interesting.
     *
     * <p>Empty for a gene with no variant allele at all, which is a gene a paper
     * has nothing to say about.
     */
    public static List<ResearchTopic> lootPool(Gene gene) {
        return pool(gene, false);
    }

    /**
     * The true-breeding half of {@link #lootPool} - {@code variant/variant}
     * only. What the supplier's top tier sells, and the {@code homozygous} flag
     * on the {@code set_random_gene} loot function.
     */
    public static List<ResearchTopic> breedsTruePool(Gene gene) {
        return pool(gene, true);
    }

    private static List<ResearchTopic> pool(Gene gene, boolean homozygousOnly) {
        List<ResearchTopic> out = new ArrayList<>();
        for (Allele variant : gene.alleles()) {
            if (variant.equals(gene.defaultAllele()) || gene.isPlaceholder(variant)) {
                continue;
            }
            List<AllelePair> shapes = homozygousOnly
                    ? List.of(new AllelePair(variant, variant))
                    : List.of(new AllelePair(variant, variant),
                            new AllelePair(variant, gene.defaultAllele()));
            for (AllelePair pair : shapes) {
                if (gene.canOccur(pair) && gene.sexConsistent(pair)) {
                    out.add(of(gene, pair));
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * The gene's first allele that is neither the wild type nor a sex-linked
     * locus's reserved placeholder - "the variant", by the same convention the
     * carrot has always used, but asked rather than assumed to be index zero.
     */
    private static Allele variantOf(Gene gene) {
        if (gene == null) {
            return null;
        }
        for (Allele a : gene.alleles()) {
            if (!a.equals(gene.defaultAllele()) && !gene.isPlaceholder(a)) {
                return a;
            }
        }
        return gene.alleles().isEmpty() ? null : gene.alleles().get(0);
    }

    // ------------------------------------------------------------------
    // Serialisation - one flat string, so a component, a menu sync and a
    // block entity's saved pick are all just a String.
    // ------------------------------------------------------------------

    /** {@code <geneKey>|<a>|<b>} - the canonical key for one pair state. */
    public String token() {
        return geneKey + SEP + alleleA + SEP + alleleB;
    }

    /**
     * Read a {@link #token()} back. A string with no separators is read as a
     * bare gene key and migrated through {@link #wholeGene}, which is what makes
     * a paper written before this change still work; anything else unparseable
     * comes back {@code null}.
     */
    public static ResearchTopic parse(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String[] parts = token.split("\\" + SEP, 3);
        if (parts.length == 1) {
            return wholeGene(parts[0]);
        }
        if (parts.length != 3) {
            return null;
        }
        return new ResearchTopic(parts[0], parts[1], parts[2]);
    }

    // ------------------------------------------------------------------
    // Reading one
    // ------------------------------------------------------------------

    /** The gene, or {@code null} if this build has no such gene. */
    public Gene gene() {
        return Genes.byKeyOrNull(geneKey);
    }

    /** The combination, or {@code null} if either token is not one of this gene's alleles. */
    public AllelePair pair() {
        Gene gene = gene();
        Allele a = alleleOrNull(gene, alleleA);
        Allele b = alleleOrNull(gene, alleleB);
        return a == null || b == null ? null : new AllelePair(a, b);
    }

    /** Is there a gene here, with a pair it agrees a horse could carry? */
    public boolean isResolved() {
        Gene gene = gene();
        AllelePair pair = pair();
        return gene != null && pair != null && gene.canOccur(pair);
    }

    /** The carrot this paper crafts, or {@code null} if it names nothing this build has. */
    public CarrotEffect.KnownGeneSplice splice() {
        return isResolved() ? new CarrotEffect.KnownGeneSplice(geneKey, alleleA, alleleB) : null;
    }

    public boolean homozygous() {
        return alleleA.equals(alleleB);
    }

    /**
     * {@code homozygous} / {@code heterozygous} / {@code hemizygous} - the last
     * one for a stallion at an {@code X}-linked locus, where the second slot is
     * the reserved placeholder rather than a real allele.
     */
    public String zygosity() {
        AllelePair pair = pair();
        Gene gene = gene();
        if (pair != null && gene != null && gene.realAlleles(pair).size() < 2) {
            return "hemizygous";
        }
        return homozygous() ? "homozygous" : "heterozygous";
    }

    /** {@code A/a} - the pair alone, as it appears in a genotype code segment. */
    public String pairLabel() {
        AllelePair pair = pair();
        return pair == null ? alleleA + "/" + alleleB : pair.toTokens();
    }

    /** The gene's display name, or its raw key if this build has no such gene. */
    public String geneName() {
        Gene gene = gene();
        return gene == null ? geneKey : gene.name();
    }

    /** {@code Agouti: A/a} - what a paper, a shelf row and a tooltip all call this. */
    public String label() {
        return geneName() + ": " + pairLabel();
    }

    private static Allele alleleOrNull(Gene gene, String token) {
        if (gene == null || token.isEmpty()) {
            return null;
        }
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        return null;
    }
}
