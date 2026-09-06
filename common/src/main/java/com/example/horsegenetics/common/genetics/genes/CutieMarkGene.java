package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;
import java.util.Optional;

/**
 * <b>Cutie mark</b> ({@code horsegenetics.cutie_mark}) - a <b>magical</b>,
 * <b>recessive</b> gene. A horse with two copies wears a little emblem of one to
 * three items, stamped on <b>both flanks</b>, drawn on top of every other coat
 * gene - white patterns included.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Cutmrk/n}</td><td>{@code cutie-mark-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Cutmrk/Cutmrk}</td><td>{@code cutie-mark} - the emblem</td></tr>
 * </table>
 *
 * <h2>It paints nothing in the pipeline</h2>
 * Like <a href="#">particle</a>, all three outcomes are {@link Expression#wildType()
 * wild types} - the coat <i>texture</i> is unchanged, so two horses that differ
 * only here share one baked coat. The emblem is a separate client render layer
 * ({@code CutieMarkLayer}) that reads this gene straight off the genotype, the
 * same way the {@code glow} emissive layer does. That is also why the priority
 * ({@value #PRIORITY}) is only a code-order slot: the layer draws after
 * everything regardless.
 *
 * <h2>Everything about the emblem is epigenetic</h2>
 * {@link #markFor} draws, off the expressing copy's seed and in this fixed
 * order: the item <b>count</b> (1-3), whether three sit in a <b>triangle</b>
 * rather than a row, three normalised <b>picks</b> in {@code [0,1)} (always
 * three, drawn whether used or not - the draw order is the contract), a
 * per-emblem <b>scale</b> and a small <b>tilt</b>. The picks are resolved
 * against the game's list of flat items on the client; which item a pick lands
 * on therefore depends on the installed mods, and that is fine - the mod
 * already has 26.1.2-only content, and within one install a horse's mark is
 * deterministic and inherited with the allele.
 */
public final class CutieMarkGene implements Gene {

    public static final String KEY = "horsegenetics.cutie_mark";
    public static final int PRIORITY = 196; // magical band; only a code-order slot (see class doc)

    /** Per allele. Recessive, so roughly the square of this is homozygous. */
    public static final double WILD_CUTMRK_FREQUENCY = 0.06;

    public final Allele Cutmrk = new Allele(KEY, 0, "Cutmrk", "Cutie mark (Cutmrk)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Cutmrk, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("cutie-mark-carrier", "Cutie-mark carrier",
            "One copy, which shows nothing. Two carriers bred together are the only way a cutie "
                    + "mark appears (or, rarely, a wild horse born with two).");

    private final Expression CUTIE_MARK = Expression.wildType("cutie-mark", "Cutie mark",
            "A small emblem of one to three items on both flanks, drawn on top of every other coat "
                    + "gene. Which items, how many, whether they sit in a row or a triangle, and how "
                    + "big they are, are all epigenetic and inherited with the allele - so a mark you "
                    + "like breeds true.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, CUTIE_MARK);

    private final FounderTable founders = FounderTable.hardyWeinberg(Cutmrk, n, WILD_CUTMRK_FREQUENCY);

    /** The emblem to draw, resolved by the client against its flat-item list. */
    public record Mark(int count, boolean triangle, double[] picks, double scale, double tilt) {
        public Mark {
            picks = picks.clone();
        }
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Cutie mark"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Cutmrk)) {
            case 2 -> CUTIE_MARK;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    public boolean shows(AllelePair pair) {
        return pair.count(Cutmrk) == 2;
    }

    /**
     * The emblem for this horse, or empty if it is not {@code Cutmrk/Cutmrk}.
     * Deterministic and heritable: everything is drawn off the expressing
     * copy's epigenetic seed.
     */
    public Optional<Mark> markFor(Genotype genotype, Epigenome epigenome) {
        if (genotype.pair(this).count(Cutmrk) != 2) {
            return Optional.empty();
        }
        Rng r = AlleleRandomness.forGene(this, genotype, epigenome).expressed();
        int count = 1 + r.nextInt(3);
        boolean triangle = r.nextBoolean() && count == 3;
        double[] picks = {r.nextFloat(), r.nextFloat(), r.nextFloat()};
        double scale = 0.7 + r.nextFloat() * 0.6;
        double tilt = (r.nextFloat() - 0.5) * 0.7;
        return Optional.of(new Mark(count, triangle, picks, scale, tilt));
    }
}
