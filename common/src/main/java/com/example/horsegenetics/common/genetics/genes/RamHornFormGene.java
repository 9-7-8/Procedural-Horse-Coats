package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.parts.RamHornGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Ram horn form</b> ({@code horsegenetics.ram_horn_form}) - a <b>magical</b>
 * dominance series deciding the shape of a horned horse's ram horns. On a polled
 * horse it does nothing.
 *
 * <table>
 *   <tr><th>allele</th><th>shape</th></tr>
 *   <tr><td>{@code Fhn}</td><td>four-horned - an upright pair and a curled pair, like a Jacob sheep</td></tr>
 *   <tr><td>{@code Crl}</td><td>curl - the ram's spiral; the ordinary shape</td></tr>
 *   <tr><td>{@code Crk}</td><td>corkscrew - long, straight and twisted</td></tr>
 *   <tr><td>{@code Hzt}</td><td>horizontal twist - flat, wide and sideways, Khnum's old horns</td></tr>
 *   <tr><td>{@code Scr}</td><td>scurs - loose stubs</td></tr>
 * </table>
 * <p>Dominance runs down the table: {@code Fhn > Crl > Crk > Hzt > Scr}. Four-horned on
 * top because the real multi-horned trait is dominant; scurs at the bottom, so a
 * scurred line is something bred for. One shape on both sides, never codominant,
 * for the antler form locus's reason.
 *
 * <p>The allele order - {@code Crl, Crk, Fhn, Scr, Hzt} - is {@link RamHornGenerator}'s shape
 * order and is stored in every genotype: append, never reorder.
 */
public final class RamHornFormGene implements Gene {

    public static final String KEY = "horsegenetics.ram_horn_form";

    /** Magical band, after {@link RamHornsGene}. */
    public static final int PRIORITY = 237;

    public final Allele Crl = new Allele(KEY, RamHornGenerator.CURL, "Crl", "Curled (Crl)");
    public final Allele Crk = new Allele(KEY, RamHornGenerator.CORKSCREW, "Crk", "Corkscrew (Crk)");
    public final Allele Fhn = new Allele(KEY, RamHornGenerator.FOUR, "Fhn", "Four-horned (Fhn)");
    public final Allele Scr = new Allele(KEY, RamHornGenerator.SCURS, "Scr", "Scurs (Scr)");
    public final Allele Hzt = new Allele(KEY, RamHornGenerator.HORIZONTAL, "Hzt", "Horizontal twist (Hzt)");
    private final List<Allele> alleles = List.of(Crl, Crk, Fhn, Scr, Hzt);

    /** Most dominant first. */
    private final List<Allele> dominance = List.of(Fhn, Crl, Crk, Hzt, Scr);

    private final Expression CURLED = Expression.wildType("ram-horn-curl", "Curled horns",
            "Ram's horns, if the horse has any, curl back, down past the ear and forward again "
                    + "- the ordinary shape. Does nothing to a polled horse.");
    private final Expression CORKSCREW = Expression.wildType("ram-horn-corkscrew", "Corkscrew horns",
            "Long, straight, twisted horns rising up and out - a Racka's or a markhor's. Does "
                    + "nothing to a polled horse.");
    private final Expression FOUR = Expression.wildType("ram-horn-four", "Four horns",
            "Two horns a side: a short upright pair and a curled pair sweeping down, like a "
                    + "Jacob sheep. Does nothing to a polled horse.");
    private final Expression SCURS = Expression.wildType("ram-horn-scurs", "Scurs",
            "Loose stubs instead of horns - the half-horned state. Two copies needed. Does "
                    + "nothing to a polled horse.");

    private final Expression HORIZONTAL = Expression.wildType("ram-horn-horizontal", "Horizontal twist horns",
            "Flat, wide horns that leave the skull sideways in a shallow twist - the old "
                    + "Egyptian ram's, and Khnum's. Shows only when no four-horned, curled or "
                    + "corkscrew allele is beside it. Does nothing to a polled horse.");

    /** In allele order. */
    private final List<Expression> byShape = List.of(CURLED, CORKSCREW, FOUR, SCURS, HORIZONTAL);

    private final FounderTable founders;

    public RamHornFormGene() {
        Map<Allele, Double> frequencies = new LinkedHashMap<>();
        frequencies.put(Crk, 0.12);
        frequencies.put(Fhn, 0.08);
        frequencies.put(Scr, 0.10);
        frequencies.put(Hzt, 0.04);
        // The plain allele last: a maximal founder roll is the ordinary horse.
        frequencies.put(Crl, 0.66);
        founders = FounderTable.hardyWeinberg(frequencies, pair -> true);
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Ram horn form"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.UNCOMMON; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return Crl; }
    @Override public List<Expression> expressions() { return byShape; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return byShape.get(shapeOf(pair));
    }

    /** The {@link RamHornGenerator} shape this pair grows: its more dominant allele's. */
    public int shapeOf(AllelePair pair) {
        if (pair == null) {
            return RamHornGenerator.CURL;
        }
        for (Allele a : dominance) {
            if (pair.count(a) > 0) {
                return a.order();
            }
        }
        return RamHornGenerator.CURL;
    }
}
