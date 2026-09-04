package com.example.horsegenetics.common.genetics;

import java.util.List;

/**
 * A single heritable gene: its alleles, what each combination of them does to
 * the horse, and how common each combination is in the wild.
 *
 * <h2>Alleles and combinations</h2>
 * A gene has <b>any number</b> of alleles. A horse carries two of them, and
 * order does not matter, so a gene with {@code n} alleles has
 * {@code n(n+1)/2} combinations - three for two alleles, six for three, 465 for
 * a thirty-allele {@code KIT}. <b>Every combination produces some result</b>,
 * and the gene says which by declaring its distinct results as
 * {@link Expression}s and mapping any pair to one of them with
 * {@link #expressionOf}.
 *
 * <p>There is deliberately <b>no dominance property</b> anywhere in this
 * interface. "Dominant" and "recessive" are shorthand for which combinations
 * happen to share a result, they only describe a two-allele locus, and they
 * cannot express codominance without a third word. The combination table says
 * all of it directly and says it for any number of alleles - see
 * {@link Expression}.
 *
 * <h2>The coat, in three phases</h2>
 * Every pixel starts at "maximal red + maximal black pigment, no magical
 * colour" - a plain black horse - and then:
 * <ol>
 *   <li><b>natural (melanin) phase</b> - each gene's expression, for every
 *       <b>natural</b> gene ({@link #isNatural()}, the default) in
 *       {@link Genes#naturalOrder()}, gets an {@link Expression#restrict} turn
 *       to push the pigment field <i>down</i>. Downward only: a natural gene
 *       can take red or black away, never add colour.</li>
 *   <li><b>resolve</b> - the surviving {@code (red, black)} pair is looked up
 *       in the red/black gradient and becomes an RGB colour (so
 *       champagne-on-bay differs from champagne-on-black, and anything under a
 *       fully-restricted white is invisible).</li>
 *   <li><b>magical (RGB) phase</b> - each <b>magical</b> gene's expression
 *       ({@code isNatural() == false}) gets an {@link Expression#tint} turn to
 *       add or remove signed red / green / blue on top of that resolved colour.
 *       The accumulator is an uncapped {@code int} per channel and is only
 *       capped to 0-255 at conversion, so a gene can commit hard enough that
 *       nothing else can pull the horse back.</li>
 * </ol>
 *
 * <p><b>A gene is natural or magical, never both</b> - declared, not inferred.
 * A gene that wants to do both registers as two genes. Natural is reserved for
 * genes that exist in real life (see {@code wiki/philosophy.html}).
 *
 * <p><b>Every paint function is pure.</b> An expression is handed read-only
 * views of the state so far and <i>returns</i> its contribution; it never
 * mutates shared scratch space. Two genes handed the same inputs always return
 * the same outputs, and each is testable on its own against a synthetic coat.
 *
 * <p>(This interface lives in {@code genetics} but {@link Expression}
 * references {@code coat.pattern} - the two packages form an intentional cycle
 * so "the coat function lives on the gene" stays literally true.)
 */
public interface Gene {

    /** {@code <modauthor>.<gene>}, e.g. {@code "horsegenetics.agouti"}. */
    String key();

    /** Display name for the gene dictionary, the wiki and tooltips. */
    default String name() {
        return key();
    }

    /**
     * <b>Gene priority</b> - a fixed constant of the gene (never data on a
     * horse, never varies between horses) that decides <b>processing order</b>.
     * Every gene has to answer; there is no default.
     *
     * <p>{@link Genes} derives all three orderings ({@link Genes#codeOrder()},
     * {@link Genes#naturalOrder()}, {@link Genes#magicalOrder()}) by sorting on
     * {@code (priority, key())} - a lower number is processed earlier, and
     * <b>ties break alphabetically by {@link #key()}</b>, so two mods that both
     * pick the same number still produce one fixed, reproducible order. The
     * same order rolls a founder's genes, so a gene's
     * {@link #founderTable founder distribution} can only ever look at genes
     * with a <i>lower</i> priority than its own.
     *
     * <p><b>This is not {@link AlleleEpigenetics#priority()}.</b> That one is
     * per allele copy and only selects <i>which copy's seed expresses</i>; it
     * cannot move a gene in the processing queue and never will.
     *
     * <p><b>Bands, by convention:</b> {@code 0}-{@code 99} is the natural band,
     * {@code 100} and up is the magical band. The band is only a convention -
     * registering a natural gene at {@code >= 100} (or a magical one below it)
     * logs a warning and carries on; it is the {@link #isNatural() phase}, not
     * the number, that decides which of the two coat passes a gene runs in.
     *
     * <p><b>Within the natural band, low numbers are for genes that set pigment
     * absolutely, higher numbers for dilutions</b> - agouti
     * ({@code BayCoat} sets its points absolutely) must run before
     * {@code PigmentField.dilute} does anything, or a bay's mane will not
     * dilute. See {@code wiki/roadmap.html} §2.
     */
    int priority();

    /**
     * All alleles this gene defines. The list order is arbitrary as far as the
     * model is concerned - it is <b>not</b> a dominance ranking - but it must
     * agree with each {@link Allele#order()}, because {@link AllelePair} uses it
     * to put a pair in a canonical, stable slot order.
     */
    List<Allele> alleles();

    /**
     * The allele a horse is assumed to carry when a stored genotype code has no
     * segment for this gene - the population's baseline, and the one a
     * {@code n/n} display collapses to. It is a <b>parsing default</b>, not a
     * statement about expression; whether a given combination does anything is
     * {@link Expression#wildType()}.
     */
    Allele defaultAllele();

    /**
     * A <b>natural</b> gene only restricts red / black pigment in phase 1; a
     * <b>magical</b> gene ({@code false}) only adds signed RGB in phase 3, after
     * the pigment field has been resolved. Never both.
     */
    default boolean isNatural() {
        return true;
    }

    // --- the combination table -------------------------------------------

    /**
     * Every distinct outcome this gene can produce, including its wild type(s).
     * The gene dictionary, the wiki page generator and the creator's preview
     * list read this; the coat pipeline does not. Every value
     * {@link #expressionOf} can return must appear here.
     */
    List<Expression> expressions();

    /**
     * <b>The function.</b> Which outcome does this combination of alleles
     * produce? Reads the pair and nothing else, so it is the answer the gene
     * dictionary, the wiki and the gallery's "which pairs look alike" reduction
     * all use.
     *
     * <p>Several pairs may - and usually do - return the same
     * {@link Expression}: that is exactly what a two-allele "dominant" gene is.
     */
    Expression expressionOf(AllelePair pair);

    /**
     * <b>Can a horse actually carry this combination?</b> True for every
     * combination of almost every gene - a locus normally lets any two of its
     * alleles pair up. The exception is a gene whose alleles are not
     * interchangeable: the sex locus has no {@code Y/Y} horse, because a foal
     * always takes an {@code X} from its dam.
     *
     * <p>A combination that cannot occur is left out of
     * {@link GenotypeCatalog#allPairsOf} - so it gets no gallery pen and is not
     * counted in {@link GenotypeCatalog#totalGenotypes()} - and an author
     * should leave it out of the {@link #founderTable founder table} too.
     * {@link #expressionOf} still has to answer for it: parsing is tolerant, so
     * a hand-written code string can name one, and returning the nearest
     * sensible outcome beats throwing.
     */
    default boolean canOccur(AllelePair pair) {
        return true;
    }

    /**
     * The same question <b>in the context of the whole genotype</b>, for the
     * handful of genes whose result depends on another gene: agouti paints
     * black points, so it does nothing at all on a chestnut horse.
     *
     * <p>Defaults to {@link #expressionOf}. An override may only return one of
     * this gene's declared {@link #expressions()} - typically a wild type, to
     * say "suppressed here". This is what the coat pipeline calls.
     *
     * <p>Sound because the genotype is complete by the time a coat is built;
     * the founder <i>roll</i>, which is not, uses {@link FounderContext}
     * instead.
     */
    default Expression expressionIn(AllelePair pair, Genotype genotype) {
        return expressionOf(pair);
    }

    /**
     * <b>Does this gene ever paint anything?</b> Derived: false only when
     * <i>every</i> combination it can produce is a
     * {@link Expression#wildType() wild type}, i.e. the gene is heritable but
     * has no coat function at all. The sex locus is the first such gene.
     *
     * <p>A gene like that is excluded from a horse's texture key
     * ({@code CoatData.textureKey()}), so two horses that differ only in it
     * share one baked texture - the same reasoning that already keeps invisible
     * epigenetics out of the key.
     */
    default boolean affectsCoat() {
        for (Expression e : expressions()) {
            if (!e.wildType()) {
                return true;
            }
        }
        return false;
    }

    // --- founder population ----------------------------------------------

    /**
     * How common each combination of this gene's alleles is among <b>founder</b>
     * horses - wild spawns, not foals. See {@link FounderTable}; one
     * {@link com.example.horsegenetics.common.Rng#nextFloat()} is drawn from it
     * per founder.
     *
     * <p>{@code context} carries the part of the genotype already rolled, for a
     * gene whose frequency depends on it. Most genes ignore it and return a
     * constant field.
     */
    FounderTable founderTable(FounderContext context);

    // --- derived conveniences ---------------------------------------------

    /** Resolve one code token to an allele of this gene. */
    default Allele fromToken(String token) {
        for (Allele a : alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new IllegalArgumentException("gene " + key() + " has no allele '" + token + "'");
    }

    /** The outcome under {@code id}, or {@code null} - for the wiki and the spec loader. */
    default Expression expression(String id) {
        for (Expression e : expressions()) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /** Does {@code pair} change the coat at all, in the context of the whole {@code genotype}? */
    default boolean isVisible(AllelePair pair, Genotype genotype) {
        return !expressionIn(pair, genotype).wildType();
    }

    /**
     * Is this gene's contribution byte-for-byte identical on every horse with
     * this {@code pair} / {@code genotype}? A {@code false} anywhere forces
     * per-horse texture generation.
     */
    default boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return expressionIn(pair, genotype).deterministic();
    }
}
