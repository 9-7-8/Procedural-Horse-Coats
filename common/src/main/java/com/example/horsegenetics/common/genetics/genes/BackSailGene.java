package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.SailGenerator;
import com.example.horsegenetics.common.parts.SailSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Back sail</b> ({@code horsegenetics.back_sail}) - a <b>magical, recessive</b>
 * gene that grows a sail along the back: a row of fin-ray spines with a see-through
 * membrane between them. The one big dramatic body part (owner, 2026-10-02: rare
 * recessive, two copies), on the unicorn horn's rule and the dorsal spines' row.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Sail/n}</td><td>{@code back-sail-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Sail/Sail}</td><td>{@code back-sail} - the sail</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part
 * comes with maturity. A saddled or ridden horse hides the spines under the saddle,
 * and the membrane in front of each of them, and keeps the rest ({@code SaddleZone}).
 * It does nothing but show: no stat, no ability, no hitbox. It paints nothing, so
 * every outcome is a wild type and neither coat golden reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code length} (the {@link SailSize} class and a stretch) is each spine's and its
 * membrane's height; {@code count} is how many spines show from the withers back, out
 * of {@link SailGenerator#MAX_SPINES}; {@code coverage} is how far back the row
 * reaches, a scale along it; {@code opacity} is how see-through the membrane is;
 * {@code form} (tall, low, scalloped) and {@code curve} (how strongly the profile
 * arches) are baked into the mesh. A horse with two copies reads the first-declared
 * one, as every part gene does. Its colour is another locus, {@link BackSailColourGene},
 * put on by {@code GrownParts.of}: the base colour is the membrane and the tip colour
 * the spines, and its bone version leaves the membrane off.
 */
public final class BackSailGene implements Gene {

    public static final String KEY = "horsegenetics.back_sail";

    /** Magical band, the first free run after the dorsal spines; only a code-order slot. */
    public static final int PRIORITY = 243;

    /**
     * Per allele - the unicorn's figure (body-parts treatment: "near the unicorn's allele
     * frequency"). Recessive, so about one wild horse in four hundred grows a sail and
     * one in ten carries one copy.
     */
    public static final double WILD_SAIL_FREQUENCY = 0.05;

    public final Allele Sail = new Allele(KEY, 0, "Sail", "Back sail (Sail)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Sail, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("back-sail-carrier", "Back sail carrier",
            "One copy, which shows nothing. Two carriers bred together are the reliable way a "
                    + "sail appears.");

    private final Expression SAIL = Expression.wildType("back-sail", "Back sail",
            "A sail along the back: a row of spines with a see-through membrane stretched "
                    + "between them. Whether it peaks over the middle of the back, runs low the "
                    + "whole length or sags in scallops between its spines, how tall it is, how "
                    + "strongly it arches, how far back it reaches, how many spines it has and how "
                    + "see-through the membrane is are all epigenetic and inherited with the allele. "
                    + "Its colour is a gene of its own. A saddle or a rider hides the part under the "
                    + "saddle. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, SAIL);

    private final FounderTable founders = FounderTable.hardyWeinberg(Sail, n, WILD_SAIL_FREQUENCY);

    /** Position on the {@link SailSize} ladder: the tallest spine's height. */
    public static final String LENGTH = "length";
    /** How many spines show, from the withers back; fractional grows the last one in. */
    public static final String COUNT = "count";
    /** How far back the row reaches, as a share of the full row. */
    public static final String COVERAGE = "coverage";
    /** How strongly the profile arches - bucketed into the mesh. */
    public static final String CURVE = "curve";
    /** How opaque the membrane is. */
    public static final String OPACITY = "opacity";
    /** Which of {@link SailGenerator}'s forms. */
    public static final String FORM = "form";

    /** The least see-through a membrane gets: still a membrane, never a solid fin. */
    public static final double MAX_OPACITY = 0.85;
    /** The most: the faintest skin that still shows where the sail is. */
    public static final double MIN_OPACITY = 0.20;

    @Override public String key() { return KEY; }
    @Override public String name() { return "Back sail"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Sail)) {
            case 2 -> SAIL;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Whether this combination grows the sail - two copies. */
    public boolean shows(AllelePair pair) {
        return pair.count(Sail) == 2;
    }

    /**
     * Height wild in the lower two-thirds of the ladder and bred toward towering, the
     * count from about half a row to a full one, coverage from two-thirds of the back to
     * all of it, and the membrane between about a third and two-thirds opaque. Each
     * clamped so a drifted line stays a sail: never under one spine or a third of the
     * row, never past the croup, and never a solid fin or an invisible one.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(LENGTH, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(COUNT, 6.0, SailGenerator.MAX_SPINES)
                        .clampedTo(1.0, SailGenerator.MAX_SPINES),
                EpiValue.uniform(COVERAGE, 0.65, 1.0).clampedTo(0.35, 1.0),
                EpiValue.uniform(CURVE, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(OPACITY, 0.35, 0.70).clampedTo(MIN_OPACITY, MAX_OPACITY),
                EpiValue.category(FORM, SailGenerator.FORMS));
    }

    /**
     * The sail this horse grows, white and dark - or empty unless it is
     * {@code Sail/Sail}. Ask {@code GrownParts.of} for the coloured sail.
     */
    public Optional<AttachedPart> sailFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return Optional.of(AttachedPart.backSail(epi.category(FORM), epi.get(CURVE), epi.get(LENGTH),
                epi.get(COUNT), epi.get(COVERAGE), epi.get(OPACITY)));
    }

    /** What to call the sail - "a tall sail". Empty for a horse that has none. */
    public Optional<SailSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(SailSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(LENGTH)));
    }
}
