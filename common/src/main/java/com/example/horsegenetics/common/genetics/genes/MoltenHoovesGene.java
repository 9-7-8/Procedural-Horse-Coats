package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Molten hooves</b> ({@code horsegenetics.molten_hooves}) - a trail of
 * burning hoofprints that fades behind the horse as it moves.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Mlt/Mlt}</td><td>{@code double-molten} - <b>both</b> copies' colours in the trail</td></tr>
 *   <tr><td>{@code Mlt/n}</td><td>{@code molten} - that copy's colour</td></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - nothing</td></tr>
 * </table>
 *
 * <h2>It sets nothing on fire</h2>
 * Worth saying in the source as well as on the page, because the name promises
 * otherwise and a later session will be tempted. The first specification placed
 * real fire blocks; on a <b>dominant</b> locus that burns down the stable, the
 * forest and the horse. The mechanic and the demotion to recessive are one
 * decision, not two - if fire comes back, so does the inheritance change.
 *
 * <h2>Dominant, which nothing else here is</h2>
 * The magical emission loci are otherwise recessive or rare, so they are a
 * reward for breeding and invisible until you get there. One of them ought not
 * to be: a dominant emission gene spreads through a herd on its own, so a
 * player who catches a single molten horse and crosses it into their line sees
 * the result immediately. That is a different pleasure from the long recessive
 * hunt and worth having once.
 *
 * <p>Because it is dominant, <b>the particle budget matters more here than on
 * any other emitter</b> - a populated stable can have a dozen of these all
 * emitting on the same moving tick. {@link #COUNT} is fixed rather than
 * epigenetic and {@link #EMIT_CHANCE} is below the particle locus's, both for
 * that reason. The knob for "not dramatic enough" is the colour, not the count.
 *
 * <h2>Where the colour lives</h2>
 * On the allele copy, not on the horse - the same arrangement
 * {@link ParticleGene} uses, so epigenetic drift on breeding works with no code
 * here. Two molten copies carry two colours and the trail draws from both,
 * which is why a heterozygote and a homozygote look different even though the
 * <i>behaviour</i> is identical and the gene is dominant.
 *
 * <p>The hue is unconstrained. {@code EpiValue.colour} bounds saturation and
 * value but rolls hue over the whole circle, and rather than add a hue-bounded
 * draw for one gene, a molten horse is allowed to burn blue. Nothing about the
 * locus needs it to be orange.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, so the locus is
 * out of the texture key.
 */
public final class MoltenHoovesGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.molten_hooves";

    /** Beside the other emission loci - particle at 150, rainbow dust after it. It paints nothing, so the number is a slot. */
    public static final int PRIORITY = 154;

    /**
     * The particle - the mod's own glowing print, lying flat and fading from the
     * copy's first colour to its second. No vanilla particle is coloured,
     * emissive <i>and</i> flat (dust takes a colour but is lit by the world and
     * faces the camera; flame glows but ignores colour), and the one thing this
     * locus stores per copy is a colour. The NeoForge module registers it
     * ({@code particle/ModParticles}) and lays it by stride rather than by chance
     * ({@code GeneAbilityHandler.layHoofprint}). It was
     * {@code minecraft:dust_color_transition} until the owner looked for prints
     * on the ground and found coloured puffs.
     */
    public static final String PARTICLE = "horsegenetics:hoofprint";

    /**
     * Particles per firing, for the ordinary emitter path. The print path lays
     * one print per stride and ignores it - kept so the budget guard in the test
     * still means something if the gene ever goes back to puffs.
     */
    public static final int COUNT = 2;

    /** Probability per moving tick, for the ordinary emitter path; the print path is by distance. */
    public static final double EMIT_CHANCE = 0.15;

    /** Share of wild founders showing a single copy. Both combinations express, so there are no invisible carriers here. */
    public static final double WILD_SINGLE_PERCENT = 3.0;

    /** Share showing two copies - rarer, and the only way to get two colours without breeding for it. */
    public static final double WILD_DOUBLE_PERCENT = 0.4;

    private final Allele mlt = new Allele(KEY, 0, "Mlt", "Molten (Mlt)");
    private final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(mlt, n);

    private final Expression wild = Expression.wildType(
            "The horse leaves the ground behind it exactly as it found it.");

    private final Expression molten = Expression.wildType("molten", "Molten hooves",
            "One molten copy, which is all it takes. Burning hoofprints trail from all four "
                    + "feet as the horse moves and fade out behind it, in the colour written on "
                    + "that copy. Nothing catches fire - not grass, not crops, not a wooden "
                    + "stable, not the horse.");

    private final Expression doubleMolten = Expression.wildType("double-molten", "Doubly molten",
            "Two molten copies. The horse behaves exactly as a single copy does - this locus is "
                    + "dominant and one copy is already all of the effect - but the trail is drawn "
                    + "from both copies' colours at once, so a doubled horse is the only one whose "
                    + "fire is two-toned.");

    private final List<Expression> expressions = List.of(wild, molten, doubleMolten);

    private final FounderTable founders = FounderTable.builder()
            .weight(mlt, mlt, WILD_DOUBLE_PERCENT)
            .weight(mlt, n, WILD_SINGLE_PERCENT)
            .weight(n, n, 100.0 - WILD_SINGLE_PERCENT - WILD_DOUBLE_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Molten hooves"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(mlt)) {
            case 2 -> doubleMolten;
            case 1 -> molten;
            default -> wild;
        };
    }

    /**
     * One emitter per molten copy. {@code AllelePair} canonicalizes on
     * {@link Allele#order()} and {@code Mlt} is order 0, so a heterozygote's
     * molten copy is always slot 0 and {@code copy(0)} is the one to read.
     *
     * <p>Two emitters rather than one blended colour: the translator already
     * fires several emitters on one horse for the codominant particle pairs, so
     * two trails is a shape it handles, and blending two colours into one would
     * make a doubled horse look like a differently-coloured single rather than
     * like two things at once.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        int copies = pair.count(mlt);
        if (copies == 0) {
            return List.of();
        }
        List<GeneAbility> out = new ArrayList<>(copies);
        for (int slot = 0; slot < copies; slot++) {
            out.add(emitter(epigenetics.copy(slot)));
        }
        return List.copyOf(out);
    }

    /**
     * The two colours one copy carries - the hoofprint's colour and what it
     * fades to as it dies. Stored per copy and drifted with it, so a line
     * breeds true to its own fire.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of()
                .and(EpiValue.colour("color", 0.60, 1.00, 0.62, 1.00))
                .and(EpiValue.colour("color2", 0.60, 1.00, 0.62, 1.00));
    }

    private GeneAbility.Emitter emitter(EpiValues epi) {
        return new GeneAbility.Emitter("particle", "trail", "hooves",
                new GeneAbility.Trigger.OnMove(),
                epi.rgb("color"), epi.rgb("color2"), COUNT, 0.0,
                PARTICLE, EMIT_CHANCE, 0,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
