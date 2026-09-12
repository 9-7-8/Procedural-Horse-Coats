package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>One variant allele, one behaviour, no coat.</b> The shape most of the
 * behaviour family turned out to share: a single named allele that either
 * expresses or does not, granting a fixed list of {@link GeneAbility} when it
 * does, and painting nothing either way.
 *
 * <p>Seventeen genes fit it, which is the argument for it existing. Writing each
 * of those as its own {@code implements Gene} would be seventeen copies of the
 * same founder table, the same two expressions and the same
 * {@code expressionOf}, and seventeen chances for one of them to get the
 * dominance backwards.
 *
 * <p>The locus half of that is now {@link TwoAlleleGene}, which
 * {@link AbstractNaturalGene} and {@link AbstractMagicalGene} share - this class
 * is where the shape was first noticed, and it kept its positional constructor
 * because seventeen call sites use it. {@link TwoAlleleGene#gene} is the
 * readable way to declare a new one.
 *
 * <h2>What a subclass decides</h2>
 * The token and label, whether it is {@link Dominance#DOMINANT dominant} or
 * {@link Dominance#RECESSIVE recessive}, how often it turns up wild and in what
 * form ({@link Founders}), the two sentences a player reads, and
 * {@link #abilitiesWhenExpressed} - which is handed that horse's epigenetic
 * values, so a gene with an epigenetic radius overrides {@link #epiSchema()}
 * and reads it there.
 *
 * <h2>Founders: the choice that is actually a design decision</h2>
 * {@link Founders#EXPRESSING} puts the expressing combination in the wild, so a
 * player can <i>catch</i> one. {@link Founders#CARRIERS_ONLY} puts only the
 * carrier there, so the gene exists in the population and is never seen until
 * somebody breeds two together.
 *
 * <p>Neither is the default because the choice says what the gene is
 * <i>for</i>. A flavour gene nobody would organise a breeding programme around
 * should be findable, or it may as well not exist; a gene worth real effort -
 * fireproofing, a spawn ward, a horse that cannot drown - is better as something
 * a breeder made than as something they tripped over. A carriers-only gene is
 * also the clearest argument the {@code gene database} has for existing, since
 * testing is the only way to hunt one.
 *
 * <h2>Paints nothing</h2>
 * Both outcomes are {@link Expression#wildType() wild types}, so the locus is
 * out of the texture key and the genotype gallery collapses it however the
 * alleles fall.
 */
public abstract class AbstractAbilityGene extends TwoAlleleGene
        implements EpigeneticAbilityContribution {

    private final Expression active;

    protected AbstractAbilityGene(String key, int priority, String displayName,
                                  String token, String label,
                                  Dominance dominance, Founders foundersPolicy, double wildPercent,
                                  String wildText, String activeName, String activeText) {
        super(setup(key, priority, displayName, token, label,
                dominance, foundersPolicy, wildPercent, wildText, activeName, activeText));
        // A wild type, not a painted outcome: this locus changes what the horse
        // does, never how it looks, so it stays out of the texture key.
        this.active = Expression.wildType(expressionId(), activeName, activeText);
    }

    private static Setup setup(String key, int priority, String displayName,
                               String token, String label,
                               Dominance dominance, Founders foundersPolicy, double wildPercent,
                               String wildText, String activeName, String activeText) {
        Setup s = gene(key, priority, displayName)
                .variant(token, label)
                .founders(foundersPolicy, wildPercent)
                .wild(wildText)
                .outcome("active", activeName, activeText);
        return dominance == Dominance.DOMINANT ? s.dominant() : s.recessive();
    }

    /** The id of the expressing outcome. Overridable for a gene that wants a better word than "active". */
    protected String expressionId() {
        return "active";
    }

    /**
     * What the horse does when the locus expresses. Called only when it does, so
     * an implementation never has to test for the wild type.
     *
     * @param epi this horse's values for this locus, from the copy that
     *            expresses - empty unless {@link #epiSchema()} is overridden
     */
    protected abstract List<GeneAbility> abilitiesWhenExpressed(EpiValues epi);

    @Override
    protected final Expression outcome() {
        return active;
    }

    @Override
    public final boolean isNatural() {
        return false;
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.EMPTY;
    }

    @Override
    public final List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                                GeneEpigenetics epigenetics) {
        if (!expresses(pair)) {
            return List.of();
        }
        // copy(0): the variant sorts first, so slot 0 is always a variant copy
        // on any combination that expresses at all.
        return abilitiesWhenExpressed(epigenetics.copy(0));
    }
}
