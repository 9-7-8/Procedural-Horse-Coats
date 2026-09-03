package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A horse's full genotype: one {@link AllelePair} per registered {@link Gene}
 * (see {@link Genes}). Built from {@link Allele} objects; round-trips through a
 * <b>code string</b> for persistence / sync / the pedigree record.
 *
 * <p><b>Code format:</b> one <b>gene-keyed</b> segment per gene in
 * {@link Genes#codeOrder()}, each {@code <geneKey>=<a>/<b>} - the full
 * {@link Gene#key()}, the two alleles joined by {@code /}, dominant first,
 * segments joined by {@code -}. Alleles are their {@link Allele#token()}.
 * Example:
 * {@code "horsegenetics.extension=E/e-horsegenetics.agouti=A/a-..."}.
 *
 * <p><b>Parsing is tolerant</b> (dev only, no saves): a registered gene with no
 * segment reads as its wild type, and a segment naming a gene that is not
 * registered is <b>dropped</b> - so adding or removing a gene is nothing more
 * than a coat regeneration. A bad <i>allele token</i> on a known gene is still
 * a hard error. There is no positional / legacy code handling.
 */
public final class Genotype {

    private static final String GENE_SEP = "-";
    private static final String ALLELE_SEP = "/";
    private static final String NAME_SEP = "=";

    private final Map<String, AllelePair> byGene;

    private Genotype(Map<String, AllelePair> byGene) {
        this.byGene = Collections.unmodifiableMap(byGene);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /** From explicit pairs; any gene not supplied is filled with its default allele. */
    public static Genotype of(List<AllelePair> pairs) {
        Map<String, AllelePair> supplied = new LinkedHashMap<>();
        for (AllelePair p : pairs) {
            supplied.put(p.geneKey(), p);
        }
        Map<String, AllelePair> full = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            AllelePair p = supplied.get(g.key());
            full.put(g.key(), p != null ? p : new AllelePair(g.defaultAllele(), g.defaultAllele()));
        }
        return new Genotype(full);
    }

    public static Genotype of(AllelePair... pairs) {
        return of(List.of(pairs));
    }

    /** All wild-type - the "unassigned" placeholder and a convenient test base. */
    public static Genotype wildType() {
        return of(List.of());
    }

    public static Genotype parse(String code) {
        Objects.requireNonNull(code, "code");
        Map<String, AllelePair> supplied = new LinkedHashMap<>();
        if (!code.isEmpty()) {
            for (String segment : code.split(GENE_SEP, -1)) {
                int eq = segment.indexOf(NAME_SEP);
                if (eq < 0) {
                    throw new IllegalArgumentException(
                            "genotype segment needs '<gene>=<a>/<b>', got: " + segment);
                }
                Gene g = Genes.byKeyOrNull(segment.substring(0, eq));
                if (g == null) {
                    continue; // a gene no longer registered - drop the segment
                }
                String[] tokens = segment.substring(eq + 1).split(ALLELE_SEP, -1);
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("segment for " + g.key()
                            + " needs two '/'-separated alleles, got: " + segment);
                }
                supplied.put(g.key(), new AllelePair(g.fromToken(tokens[0]), g.fromToken(tokens[1])));
            }
        }
        return of(List.copyOf(supplied.values()));
    }

    public String toCode() {
        StringBuilder sb = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            if (sb.length() > 0) {
                sb.append(GENE_SEP);
            }
            AllelePair p = byGene.get(g.key());
            sb.append(g.key()).append(NAME_SEP)
              .append(p.first().token()).append(ALLELE_SEP).append(p.second().token());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Random population / Mendelian breeding
    // ------------------------------------------------------------------

    /**
     * One founder / wild horse. Each gene in {@link Genes#codeOrder()} draws its
     * combination from its own {@link FounderTable} - <b>one
     * {@link Rng#nextFloat()} per gene</b> - and is handed a
     * {@link FounderContext} over the genes already rolled, so a gene's
     * frequency may depend on what the horse already is.
     */
    public static Genotype random(Rng rng) {
        Map<String, AllelePair> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            m.put(g.key(), g.founderTable(new FounderContext(m, g)).draw(rng));
        }
        return new Genotype(m);
    }

    /**
     * Mendelian: for each gene the child takes one allele from each parent,
     * drawn 50/50 within that parent's pair. Two {@link Rng#nextBoolean()}
     * draws per gene, genes in {@link Genes#codeOrder()}.
     */
    public Genotype breedWith(Genotype other, Rng rng) {
        Map<String, AllelePair> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            AllelePair mine = pair(g);
            AllelePair theirs = other.pair(g);
            Allele c1 = rng.nextBoolean() ? mine.first() : mine.second();
            Allele c2 = rng.nextBoolean() ? theirs.first() : theirs.second();
            m.put(g.key(), new AllelePair(c1, c2));
        }
        return new Genotype(m);
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    public AllelePair pair(Gene gene) {
        return byGene.get(gene.key());
    }

    public AllelePair pair(String geneKey) {
        return byGene.get(geneKey);
    }

    public Collection<AllelePair> pairs() {
        return byGene.values();
    }

    public boolean has(Allele allele) {
        AllelePair p = byGene.get(allele.geneKey());
        return p != null && p.has(allele);
    }

    // ------------------------------------------------------------------
    // Determinism
    // ------------------------------------------------------------------

    public boolean isDeterministic() {
        return !hasVisibleNonDeterministic();
    }

    public boolean hasVisibleNonDeterministic() {
        for (Gene g : Genes.codeOrder()) {
            AllelePair p = pair(g);
            if (g.isVisible(p, this) && !g.isDeterministic(p, this)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Coarse phenotype label (foal *_baby textures, family-tree fallback, UI)
    // ------------------------------------------------------------------

    /**
     * Does {@code gene} do anything on this horse - i.e. is the combination it
     * carries something other than a {@link Expression#wildType() wild type},
     * in the context of the rest of the genotype? The generic question that
     * used to be a hand-written {@code isChampagne()} / {@code isGrey()} /
     * {@code hasTest()} per gene.
     */
    public boolean shows(Gene gene) {
        return gene.isVisible(pair(gene), this);
    }

    /** The {@link Expression} {@code gene} produces on this horse. */
    public Expression expressionOf(Gene gene) {
        return gene.expressionIn(pair(gene), this);
    }

    /** Every gene doing something on this horse, in {@link Genes#codeOrder()}. */
    public List<Gene> visibleGenes() {
        List<Gene> out = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (shows(g)) {
                out.add(g);
            }
        }
        return List.copyOf(out);
    }

    public boolean isWhite() {
        return Genes.WHITE.isWhite(pair(Genes.WHITE));
    }

    public boolean hasBlackPigment() {
        return Genes.EXTENSION.producesBlack(pair(Genes.EXTENSION));
    }

    public boolean isAgouti() {
        return Genes.AGOUTI.isBay(pair(Genes.AGOUTI));
    }

    public CoatPhenotype phenotype() {
        if (isWhite()) {
            return CoatPhenotype.WHITE;
        }
        if (!hasBlackPigment()) {
            return CoatPhenotype.CHESTNUT;
        }
        return isAgouti() ? CoatPhenotype.BAY : CoatPhenotype.BLACK;
    }

    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        return o instanceof Genotype g && g.byGene.equals(byGene);
    }

    @Override
    public int hashCode() {
        return byGene.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Genotype[")
                .append(GeneCodeDisplay.shortForm(this)).append(" -> ").append(phenotype());
        for (Gene g : visibleGenes()) {
            if (g == Genes.EXTENSION || g == Genes.AGOUTI || g == Genes.WHITE) {
                continue; // already said by phenotype()
            }
            sb.append(" +").append(expressionOf(g).id());
        }
        return sb.append(']').toString();
    }
}
