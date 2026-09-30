package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartKind;

import java.util.List;

/**
 * <b>Horn dust</b> ({@code horsegenetics.horn_dust}) - a <b>magical recessive</b>
 * that makes a unicorn's horn shed a fine dust, the colour of the horn, which
 * drifts down past its face. On a horse with no horn it does nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Dst/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Dst/Dst}</td><td>{@code horn-dust} - the horn sheds dust</td></tr>
 * </table>
 *
 * <h2>The dust is the horn's colour because it asks the horn</h2>
 * The colours come from {@link GrownParts#of}, the same door the renderer uses, so
 * the dust matches whatever the horn is drawn as - its shade drift, a chaos copy's
 * colour, both colours of a two-tone horn - with nothing here that could disagree.
 * A two-tone horn's dust fades from its base colour to its tip colour as it falls.
 *
 * <p>That needs the whole horse's epigenome rather than this locus's own numbers,
 * which is what {@link GeneEpigenetics#epigenome()} is for; this gene carries no
 * numbers of its own.
 *
 * <h2>No wild carriers</h2>
 * As {@link HornGlowGene}: a founder is {@code Dst/Dst} or {@code n/n}.
 */
public final class HornDustGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.horn_dust";

    /** Magical band, beside {@link HornGlowGene}. */
    public static final int PRIORITY = 229;

    /** Founders born {@code Dst/Dst}, in percent - about one in twelve. */
    public static final double WILD_HOMOZYGOUS_PERCENT = 8.0;

    /**
     * The mod's own falling dust - {@code ModParticles.HORN_DUST_ID}. Vanilla's
     * coloured dust hangs in the air where it appears; this is the same particle
     * with gravity, so it falls.
     */
    public static final String PARTICLE = "horsegenetics:horn_dust";

    /** Where it comes from - the {@code GeneAbilityHandler} anchor on the horn. */
    public static final String ANCHOR = "horn";

    /** Chance per tick of shedding a mote. A steady fine fall, not a cloud. */
    public static final double EMIT_CHANCE = 0.35;

    public final Allele Dst = new Allele(KEY, 0, "Dst", "Horn dust (Dst)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Dst, n);

    private final Expression WILD = Expression.wildType("A horn, if the horse has one, that sheds nothing.");

    private final Expression CARRIER = Expression.wildType("horn-dust-carrier", "Horn dust carrier",
            "One copy, which shows nothing. Two carriers bred together are how a dusting horn "
                    + "appears in a line that lacks one.");

    private final Expression DUSTING = Expression.wildType("horn-dust", "Dusting horn",
            "A fine dust, the colour of the horn, falls from it all the time - a two-tone horn's "
                    + "dust fades from one of its colours to the other. Shows only on a horse with "
                    + "a horn; on any other horse this does nothing.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, DUSTING);

    private final FounderTable founders = FounderTable.builder()
            .weight(Dst, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Horn dust"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Dst)) {
            case 2 -> DUSTING;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Would this horse's horn shed dust, if it had one? */
    public boolean dusts(AllelePair pair) {
        return pair != null && pair.count(Dst) == 2;
    }

    /**
     * One emitter off the horn, in its colours - or nothing, for a horse without
     * two copies or without a horn.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics) {
        if (!dusts(pair)) {
            return List.of();
        }
        for (AttachedPart part : GrownParts.of(genotype, epigenetics.epigenome())) {
            if (part.kind() == PartKind.HORN) {
                return List.of(new GeneAbility.Emitter("particle", "point", ANCHOR,
                        new GeneAbility.Trigger.Continuous(),
                        part.baseTint() & 0xFFFFFF, part.tipTint() & 0xFFFFFF, 1, 0.0,
                        PARTICLE, EMIT_CHANCE, 0, GeneAbility.Condition.ALWAYS, 1));
            }
        }
        return List.of();
    }
}
