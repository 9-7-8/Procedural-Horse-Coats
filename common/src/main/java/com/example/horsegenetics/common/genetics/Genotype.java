package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.Sex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    /**
     * A genotype code <b>read back from a save</b>, trimmed to what this build
     * can parse: a segment naming an allele the gene no longer has - or that is
     * malformed - is dropped, so that locus comes back at its default, exactly
     * as {@link #parse} already treats a segment naming a gene that no longer
     * exists. Returns {@code code} itself when nothing was dropped.
     *
     * <p>{@link #parse} stays strict, because a code somebody typed should say
     * what is wrong with it. This is for codes nobody can retype: a horse saved
     * by an older release, whose one retired allele (Cleave's {@code Clv}, retired
     * in 0.5.000) would otherwise throw from inside an entity tick and take the
     * world down every time that chunk loaded. Released jars have players on
     * them, and losing one rare marking is better than losing the world.
     *
     * @param dropped told each segment that was dropped, for the log
     */
    public static String readableStored(String code, java.util.function.Consumer<String> dropped) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        StringBuilder kept = new StringBuilder(code.length());
        boolean any = false;
        for (String segment : code.split(GENE_SEP, -1)) {
            if (readable(segment)) {
                if (kept.length() > 0) {
                    kept.append(GENE_SEP);
                }
                kept.append(segment);
            } else {
                any = true;
                dropped.accept(segment);
            }
        }
        return any ? kept.toString() : code;
    }

    private static boolean readable(String segment) {
        int eq = segment.indexOf(NAME_SEP);
        if (eq < 0) {
            return false;
        }
        Gene g = Genes.byKeyOrNull(segment.substring(0, eq));
        if (g == null) {
            return true; // parse drops it by itself
        }
        String[] tokens = segment.substring(eq + 1).split(ALLELE_SEP, -1);
        if (tokens.length != 2) {
            return false;
        }
        try {
            g.fromToken(tokens[0]);
            g.fromToken(tokens[1]);
            return true;
        } catch (IllegalArgumentException retired) {
            return false;
        }
    }

    /**
     * This genotype with its sex locus set to {@code sex} - the rest untouched.
     * The one legitimate way to <i>choose</i> a horse's sex, and it is a founder
     * operation: the horse dimension stocks each pen with one mare and one
     * stallion of the same colour, and the custom spawn egg lets the player
     * pick. A foal never goes through it - its sex is inherited like any other
     * gene.
     */
    public Genotype withSex(Sex sex) {
        return with(Genes.SEX.pairFor(sex));
    }

    /**
     * This genotype with one gene's combination replaced - the rest untouched.
     * A <b>founder</b> operation, like {@link #withSex}: it is how the horse
     * dimension forces a showcase horse to actually show something
     * ({@link ShowcaseGenotypes}) and how the custom spawn egg builds the
     * genome the player picked. A foal never goes through it.
     */
    public Genotype with(AllelePair pair) {
        Map<String, AllelePair> m = new LinkedHashMap<>(byGene);
        m.put(pair.geneKey(), pair);
        return new Genotype(m);
    }

    public String toCode() {
        return code(g -> true);
    }

    /** One {@code <geneKey>=<a>/<b>} segment per gene {@code include} accepts, {@code -}-joined. */
    private String code(java.util.function.Predicate<Gene> include) {
        StringBuilder sb = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            if (!include.test(g)) {
                continue;
            }
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
     * drawn 50/50 within that parent's pair. <b>Two {@link Rng#nextBoolean()}
     * draws per gene</b>, genes in {@link Genes#codeOrder()}.
     *
     * <h2>Sex-linked loci</h2>
     * A gene that declares {@link Inheritance#X_LINKED} or
     * {@link Inheritance#Y_LINKED} does not get the sire's copy by a coin flip -
     * it is <b>decided by the foal's own sex</b>, which the sex locus (priority
     * 1, so always first in {@code codeOrder()}) has already drawn by the time
     * any other gene is reached. On an {@code X}-linked locus the sire gives his
     * {@code X}-borne allele to a filly and his {@code Y} - i.e. nothing - to a
     * colt; a {@code Y}-linked locus is the mirror. The dam is unchanged in both
     * cases.
     *
     * <p><b>The second coin is still flipped and thrown away.</b> Two booleans
     * per gene is an invariant a lot of things lean on - the golden coats, the
     * gamete-bias equivalence test, every claim that adding a gene shifts the
     * stream by a known amount - and a locus that quietly consumed one would
     * make the stream depend on a foal's sex. The particle locus's fixed draw
     * order, applied one layer up.
     */
    public Genotype breedWith(Genotype other, Rng rng) {
        Map<String, AllelePair> m = new LinkedHashMap<>();
        Sex childSex = null;
        for (Gene g : Genes.codeOrder()) {
            AllelePair mine = pair(g);
            AllelePair theirs = other.pair(g);
            Allele c1 = rng.nextBoolean() ? mine.first() : mine.second();
            Allele c2 = rng.nextBoolean() ? theirs.first() : theirs.second();
            AllelePair drawn = g.inheritance().sexLinked() && childSex != null
                    ? sexLinkedPair(g, this, other, childSex, c1, c2)
                    : new AllelePair(c1, c2);
            m.put(g.key(), drawn);
            if (g == Genes.SEX) {
                childSex = Genes.SEX.sexOf(drawn);
            }
        }
        return new Genotype(m);
    }

    /**
     * One sex-linked locus for a foal of {@code childSex}. {@code aPick} and
     * {@code bPick} are the two coins already flipped - one inside each parent's
     * own pair - so this method spends no randomness of its own and the
     * two-booleans-per-gene invariant holds.
     *
     * <p>Which parent is the dam is read off the parents' own sex loci rather
     * than from the call site, because {@code breedWith} is otherwise symmetric
     * and nothing else in the model has ever needed to know. If both read as the
     * same sex - which the breeding handlers do not allow, but a hand-written
     * genotype could - {@code a} is treated as the dam, the same tolerance
     * {@link #parse} shows everywhere else.
     */
    private static AllelePair sexLinkedPair(Gene gene, Genotype a, Genotype b, Sex childSex,
                                            Allele aPick, Allele bPick) {
        boolean aIsDam = a.sex() == Sex.FEMALE || b.sex() != Sex.FEMALE;
        Genotype sire = aIsDam ? b : a;
        Allele damPick = aIsDam ? aPick : bPick;
        Allele placeholder = gene.hemizygousPlaceholder();
        List<Allele> sireReal = gene.realAlleles(sire.pair(gene));

        if (gene.inheritance() == Inheritance.X_LINKED) {
            // The dam is diploid here, so her half is the ordinary coin flip.
            // A filly gets the sire's one X-borne allele; a colt gets his Y,
            // which is to say she gets the locus and he does not.
            Allele fromSire = childSex == Sex.FEMALE && !sireReal.isEmpty()
                    ? sireReal.get(0)
                    : placeholder;
            return new AllelePair(damPick, fromSire);
        }

        // Y-linked: the dam contributes nothing at all, and a filly has no copy.
        Allele fromSire = childSex == Sex.FEMALE || sireReal.isEmpty()
                ? placeholder
                : sireReal.get(0);
        return new AllelePair(placeholder, fromSire);
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    /**
     * This horse's {@link Sex}, read off the sex locus - the single source of
     * truth. {@code X/X} is a mare, anything else a stallion.
     */
    public Sex sex() {
        return Genes.SEX.sexOf(pair(Genes.SEX));
    }

    /**
     * The sex a code string describes, <b>without parsing the rest of it</b> -
     * {@link com.example.horsegenetics.common.horse.HorseRecord#sex()} is asked
     * this on every GUI frame and on every horse's ability tick, and building
     * one {@link AllelePair} per registered gene to read one of them is waste.
     * Falls back to
     * the sex gene's default ({@code X/X}, a mare) when the code has no sex
     * segment, exactly as {@link #parse} would.
     */
    public static Sex sexOf(String code) {
        String prefix = Genes.SEX.key() + NAME_SEP;
        int at = code.indexOf(prefix);
        boolean atSegmentStart = at == 0 || (at > 0 && code.charAt(at - 1) == GENE_SEP.charAt(0));
        if (at >= 0 && atSegmentStart) {
            int from = at + prefix.length();
            int end = code.indexOf(GENE_SEP.charAt(0), from);
            String body = end < 0 ? code.substring(from) : code.substring(from, end);
            int slash = body.indexOf(ALLELE_SEP.charAt(0));
            if (slash >= 0) {
                return Genes.SEX.sexOf(new AllelePair(
                        Genes.SEX.fromToken(body.substring(0, slash)),
                        Genes.SEX.fromToken(body.substring(slash + 1))));
            }
        }
        return Genes.SEX.sexOf(new AllelePair(Genes.SEX.defaultAllele(), Genes.SEX.defaultAllele()));
    }

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

    /**
     * {@link #toCode()} restricted to the genes that can paint something
     * ({@link Gene#affectsCoat()}), <b>plus</b> any modifier gene a painting
     * gene declares it reads ({@link Gene#coatDependsOn()} - the leopard
     * complex's {@code PATN1} / {@code PATN2}). This is the part of the
     * genotype a texture depends on, and so the basis of
     * {@code CoatData.textureKey()}: two horses with the same {@code coatCode}
     * are painted identically (epigenetics aside), which is why a mare and a
     * stallion of the same colour share one baked texture rather than doubling
     * the cache.
     *
     * <p>Not a persistence format - it is lossy on purpose and nothing parses
     * it back.
     */
    public String coatCode() {
        Set<String> depended = new HashSet<>();
        for (Gene g : Genes.codeOrder()) {
            if (!g.affectsCoat() || g.coatDependsOn().isEmpty()) {
                continue;
            }
            // Only fold a modifier's alleles into the key when the gene that
            // reads them actually paints on *this* horse - a PATN carrier with
            // no LP looks like any other horse and must share its texture.
            if (g.expressionIn(byGene.get(g.key()), this).wildType()) {
                continue;
            }
            depended.addAll(g.coatDependsOn());
        }
        return code(g -> paintsOnThisHorse(g) || depended.contains(g.key()));
    }

    /**
     * Does {@code gene} paint anything <b>on this horse</b>? Not just "can this
     * gene ever paint" ({@link Gene#affectsCoat()}) but "does the combination
     * this horse carries do something", which is the question the texture key
     * actually wants.
     *
     * <p>It is the difference between a cache that is right and a cache that is
     * <i>six times bigger than it needs to be</i>. Flaxen is invisible on any
     * horse that makes black hair and about half the population carries some,
     * so keying every bay on its six flaxen combinations would have multiplied
     * the bay textures by six for six identical bakes; agouti already did the
     * same thing more mildly to chestnuts.
     *
     * <p><b>Sound because a wild type has no painter.</b> Every channel the
     * composer runs skips a wild-type combination first - the phase-1 loop, the
     * phase-3 loop, the overlay pass and the LUT swap all test it - with
     * exactly two exceptions, the eye-colour and eye-patch channels, which ask
     * every implementor unconditionally. {@code GeneCoatHookTest} pins that no
     * gene claims an eye while wild type, which is what keeps this safe; if one
     * ever does, that test goes red rather than two differently-eyed horses
     * quietly sharing a texture.
     */
    private boolean paintsOnThisHorse(Gene gene) {
        return gene.affectsCoat() && !gene.expressionIn(byGene.get(gene.key()), this).wildType();
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

    /**
     * Is this horse white all over - i.e. does {@code KIT} carry dominant
     * white, or {@code EDNRB} the homozygous lethal white? Both remove every
     * pigment everywhere and mask every other gene, which is the only thing
     * {@link CoatPhenotype#WHITE} means.
     */
    public boolean isWhite() {
        return Genes.KIT.isDominantWhite(pair(Genes.KIT))
                || Genes.EDNRB.isLethalWhite(pair(Genes.EDNRB));
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
            if (g == Genes.EXTENSION || g == Genes.AGOUTI) {
                continue; // already said by phenotype()
            }
            sb.append(" +").append(expressionOf(g).id());
        }
        return sb.append(']').toString();
    }
}
