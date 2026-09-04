package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.horse.Sex;

import java.util.List;

/**
 * <b>Sex</b> ({@code horsegenetics.sex}) - the sex chromosome pair, registered
 * as an ordinary locus.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code X/X}</td><td>{@code mare} - a wild type; changes nothing about the coat</td></tr>
 *   <tr><td>{@code X/Y}</td><td>{@code stallion} - likewise</td></tr>
 *   <tr><td>{@code Y/Y}</td><td><b>cannot occur</b> - see {@link #canOccur}</td></tr>
 * </table>
 *
 * <h2>Why sex is a gene</h2>
 * Sex used to be a field rolled onto the {@link com.example.horsegenetics.common.horse.HorseRecord}
 * by a coin flip, which made it a second source of truth beside the genotype and
 * meant a foal's sex was <i>invented</i> rather than <i>inherited</i>. As a
 * locus it costs nothing extra and buys three things:
 * <ul>
 *   <li><b>Inheritance falls out of the ordinary Mendelian draw.</b> A mare is
 *       {@code X/X} and can only pass an {@code X}; a stallion is {@code X/Y}
 *       and passes one or the other 50/50 - exactly the two
 *       {@link com.example.horsegenetics.common.Rng#nextBoolean()} per gene
 *       {@link com.example.horsegenetics.common.genetics.Genotype#breedWith}
 *       already draws. No special case anywhere.</li>
 *   <li><b>It is resolved before every other gene.</b> At {@link #priority()}
 *       {@value #PRIORITY} it is the first gene in
 *       {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()}, so
 *       a future X-linked or Y-linked gene can segregate against a sex that is
 *       already decided (see {@code wiki/roadmap.html} §5.3).</li>
 *   <li><b>Sex travels with the genome</b> - into the genotype code, the
 *       stallion seed jar's stored sample, and the custom spawner - instead of
 *       needing its own field in each of them.</li>
 * </ul>
 *
 * <h2>It paints nothing</h2>
 * Both outcomes are {@link Expression#wildType() wild types}: this is the first
 * gene in the model whose every combination changes nothing about the coat.
 * That is what keeps it free - the gallery collapses the whole locus into one
 * pen, and {@link Gene#affectsCoat()} is false, so a mare and a stallion of the
 * same colour still share one baked texture. Sexual dimorphism (a stallion's
 * crest, a heavier build) is not modelled; if it ever is, it belongs to a
 * separate gene reading this one, not here.
 *
 * <p>Founder population: an even 50/50 split of mares and stallions.
 */
public final class SexGene implements Gene {

    public static final String KEY = "horsegenetics.sex";

    /**
     * Ahead of every coat gene, so any later sex-linked locus can read a sex
     * that is already resolved. Deliberately {@code 1}, not {@code 0}: the
     * roadmap reserves the slot and leaves room below it.
     */
    public static final int PRIORITY = 1;

    public final Allele X = new Allele(KEY, 0, "X", "X chromosome");
    public final Allele Y = new Allele(KEY, 1, "Y", "Y chromosome");
    private final List<Allele> alleles = List.of(X, Y);

    private final Expression MARE = Expression.wildType("mare", "Mare",
            "Two X copies. The horse is a mare (a filly as a foal); its coat is not affected.");

    private final Expression STALLION = Expression.wildType("stallion", "Stallion",
            "One X and one Y. The horse is a stallion (a colt as a foal); its coat is not affected.");

    private final List<Expression> expressions = List.of(MARE, STALLION);

    /**
     * An even split. Mares and stallions are equally common; {@code Y/Y} never
     * occurs. Declared variant-first / baseline-last like every other gene's
     * table, so a high roll lands on {@code X/X} the way a high roll lands on
     * the plain horse everywhere else.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(X, Y, 50.0)
            .weight(X, X, 50.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Sex"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * {@code X}. A code string with no sex segment therefore reads as
     * {@code X/X} - a mare, which is what the blank
     * {@link com.example.horsegenetics.common.horse.HorseRecord#unassigned}
     * sentinel has always been.
     */
    @Override public Allele defaultAllele() { return X; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.homozygousFor(X) ? MARE : STALLION;
    }

    /**
     * {@code Y/Y} is not a horse. Nothing can produce one - a foal always takes
     * an {@code X} from its dam - so the catalogue and the founder table both
     * leave it out; it is only reachable by hand-writing the code, where it
     * reads as a stallion rather than throwing.
     */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !pair.homozygousFor(Y);
    }

    // --- the enum the rest of the code reads ------------------------------

    /** Which {@link Sex} this combination is: {@code X/X} a mare, anything else a stallion. */
    public Sex sexOf(AllelePair pair) {
        return pair.homozygousFor(X) ? Sex.FEMALE : Sex.MALE;
    }

    /** The combination a horse of {@code sex} carries: {@code X/X} or {@code X/Y}. */
    public AllelePair pairFor(Sex sex) {
        return sex == Sex.FEMALE ? new AllelePair(X, X) : new AllelePair(X, Y);
    }
}
