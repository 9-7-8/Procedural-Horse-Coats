package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.parts.AntlerGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Antler form</b> ({@code horsegenetics.antler_form}) - a <b>magical</b>
 * dominance series that decides <i>how</i> an antlered horse's rack grows. On a
 * horse without antlers it does nothing at all.
 *
 * <table>
 *   <tr><th>allele</th><th>habit</th></tr>
 *   <tr><td>{@code Pal}</td><td>palmate - a broad palm with points round its edge (moose, fallow)</td></tr>
 *   <tr><td>{@code Crn}</td><td>crowned - a tall beam ending in a cup of tines (a royal head)</td></tr>
 *   <tr><td>{@code Brw}</td><td>brow-tined - swept back and over, a tine forward over the face (reindeer)</td></tr>
 *   <tr><td>{@code Frk}</td><td>forked - branched up the beam (red deer)</td></tr>
 *   <tr><td>{@code n}</td><td>spike - a plain spike, forking once when big (a yearling, a brocket)</td></tr>
 * </table>
 * <p>Dominance runs down that table: {@code Pal > Crn > Brw > Frk > n}. One habit is
 * drawn on <b>both</b> sides, so the series is plain dominance and never codominant
 * - antlers are symmetric (D3, owner), and a horse that drew palmate on one side and
 * forked on the other would be a bug, not a phenotype.
 *
 * <h2>Why a locus and not a number</h2>
 * A habit is a <i>kind</i> a breeder crosses in on purpose, and a 3:1 ratio from a
 * Pal/n cross is exactly the legible thing a locus is for. How many tines, how long,
 * how thick are quantities, so they are numbers on the antler allele instead. That
 * line is the treatment's: "would a player want a 3:1 ratio from this?"
 *
 * <h2>The allele order is the generator's habit order</h2>
 * {@code Frk, Brw, Pal, Crn, n} is both the order the alleles are declared in - and
 * so stored in every saved genotype - and {@link AntlerGenerator}'s habit indices.
 * Appending a habit is safe; reordering would rewrite every horse's rack, and
 * {@code Genotype.readableStored} drops a segment naming an allele the build does
 * not have, so removal is worse.
 */
public final class AntlerFormGene implements Gene {

    public static final String KEY = "horsegenetics.antler_form";

    /** Magical band, after {@link AntlersGene}. */
    public static final int PRIORITY = 231;

    public final Allele Frk = new Allele(KEY, AntlerGenerator.FORK, "Frk", "Forked (Frk)");
    public final Allele Brw = new Allele(KEY, AntlerGenerator.BROW, "Brw", "Brow-tined (Brw)");
    public final Allele Pal = new Allele(KEY, AntlerGenerator.PALMATE, "Pal", "Palmate (Pal)");
    public final Allele Crn = new Allele(KEY, AntlerGenerator.CROWN, "Crn", "Crowned (Crn)");
    public final Allele n = new Allele(KEY, AntlerGenerator.SPIKE, "n", "Spike (n)");
    private final List<Allele> alleles = List.of(Frk, Brw, Pal, Crn, n);

    /** Most dominant first. */
    private final List<Allele> dominance = List.of(Pal, Crn, Brw, Frk, n);

    private final Expression SPIKE = Expression.wildType("antler-spike", "Spike antlers",
            "Antlers, if the horse has any, are plain spikes - or a spike with a single short "
                    + "fork once they grow large. Does nothing to a horse without antlers.");
    private final Expression FORKED = Expression.wildType("antler-forked", "Forked antlers",
            "Antlers branch up the beam, the lower tines longest - a red deer's rack. Does "
                    + "nothing to a horse without antlers.");
    private final Expression BROW = Expression.wildType("antler-brow", "Brow-tined antlers",
            "Antlers sweep back and over in a long curve, with one tine thrown forward over "
                    + "the face - a reindeer's rack. Does nothing to a horse without antlers.");
    private final Expression PALMATE = Expression.wildType("antler-palmate", "Palmate antlers",
            "Antlers open out from a short beam into a broad palm with points round its edge - "
                    + "a moose's or a fallow buck's. Does nothing to a horse without antlers.");
    private final Expression CROWNED = Expression.wildType("antler-crowned", "Crowned antlers",
            "Antlers rise to a cup of tines splayed every way at the top - a royal head. "
                    + "Does nothing to a horse without antlers.");

    /** In allele order, so {@code byHabit.get(allele.order())} is that allele's look. */
    private final List<Expression> byHabit = List.of(FORKED, BROW, PALMATE, CROWNED, SPIKE);

    private final FounderTable founders;

    public AntlerFormGene() {
        Map<Allele, Double> frequencies = new LinkedHashMap<>();
        frequencies.put(Frk, 0.50);
        frequencies.put(Brw, 0.12);
        frequencies.put(Pal, 0.08);
        frequencies.put(Crn, 0.08);
        // The plain allele last: a maximal founder roll is the ordinary horse.
        frequencies.put(n, 0.22);
        founders = FounderTable.hardyWeinberg(frequencies, pair -> true);
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Antler form"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.UNCOMMON; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return byHabit; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return byHabit.get(habitOf(pair));
    }

    /** The {@link AntlerGenerator} habit this pair grows: its more dominant allele's. */
    public int habitOf(AllelePair pair) {
        if (pair == null) {
            return AntlerGenerator.SPIKE;
        }
        for (Allele a : dominance) {
            if (pair.count(a) > 0) {
                return a.order();
            }
        }
        return AntlerGenerator.SPIKE;
    }
}
