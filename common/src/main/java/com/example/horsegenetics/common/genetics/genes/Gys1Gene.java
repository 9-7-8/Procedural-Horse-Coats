package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>GYS1</b> ({@code horsegenetics.gys1}) - <b>PSSM1</b>, type 1 polysaccharide
 * storage myopathy: the muscle stores sugar it cannot use, and ties up under
 * work.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code P/N}</td><td>affected, mildly - the commonest sick horse in the mod</td></tr>
 *   <tr><td>{@code P/P}</td><td>affected, worse - and still a horse that lives</td></tr>
 * </table>
 *
 * <h2>The one disorder you will actually meet</h2>
 * Everything else on the health layer is rare by construction, because the
 * recessives need two carriers to find each other. This one is dominant, so
 * every copy shows, and at {@value #WILD_AFFECTED_PERCENT}% of founders it is by
 * a wide margin the disorder a player is most likely to run into - which is also
 * true of the real thing across draughts, warmbloods, paints and appaloosas.
 *
 * <p>That frequency is the number this gene is really about, and it is set
 * deliberately low relative to the real population. A dominant disorder at a
 * realistic rate would mean a visible fraction of every paddock is unwell, which
 * stops reading as a disorder and starts reading as the baseline horse being
 * bad. Rare enough to notice, common enough to matter.
 *
 * <h2>Neither combination is lethal</h2>
 * Which makes it the mildest locus on the health layer and the only dominant one
 * that is safe to breed from if you accept what you are getting. {@code P/P} is
 * worse than {@code P/N} rather than fatal, so unlike
 * {@link Scn4aGene HYPP} there is no cliff - the cost of ignoring it is a line
 * that gets quietly worse, not a dead foal.
 *
 * <p>The magnitudes are calibrated against the disorders that already ship
 * rather than computed from the reference's percentages - see
 * {@link Scn4aGene} for why.
 */
public final class Gys1Gene extends DominantDisorderGene {

    public static final String KEY = "horsegenetics.gys1";
    public static final int PRIORITY = 94;

    /** Share of founders born {@code P/N} - and therefore born affected. */
    public static final double WILD_AFFECTED_PERCENT = 2.5;

    public static final Condition PSSM1 = Condition.impairing(
            "pssm1", "PSSM1 (tying-up)",
            "The muscle stores sugar it cannot burn. The horse ties up under work - it "
                    + "tires sooner than it should and does not move freely afterwards.");

    public static final Condition PSSM1_SEVERE = Condition.impairing(
            "pssm1-severe", "PSSM1 (two copies)",
            "Two copies. The same disorder, worse: episodes come on under less work and "
                    + "take more out of the horse. It still lives an ordinary life.");

    public Gys1Gene() {
        super(KEY, "GYS1 (PSSM1)", PRIORITY,
                "P", "PSSM1 (P)", "N", "Wild-type (N)",
                WILD_AFFECTED_PERCENT, PSSM1, PSSM1_SEVERE);
    }

    @Override
    protected void affectHeterozygote(TraitBuilder out) {
        out.addHealth(-3.0).addSpeed(-0.015).addJump(-0.06);
    }

    @Override
    protected void affectHomozygote(TraitBuilder out) {
        out.addHealth(-6.0).addSpeed(-0.030).addJump(-0.12);
    }
}
