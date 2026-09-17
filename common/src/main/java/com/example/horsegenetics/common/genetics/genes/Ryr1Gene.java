package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>RYR1</b> ({@code horsegenetics.ryr1}) - <b>malignant hyperthermia</b>, and
 * the only disorder in this mod that costs an affected horse <i>nothing</i>.
 *
 * <p>Dominant, like {@link Scn4aGene HYPP} and {@link Gys1Gene PSSM1}, so there
 * is no silent carrier and a wild-caught horse really can have it - which is the
 * only way a dominant allele can exist in the world at all. See
 * {@link DominantDisorderGene} for why that break is deliberate.
 *
 * <h2>Why one copy is free</h2>
 * Every other disorder here is a standing condition: a dwarf is always small, a
 * HERDA horse always has fragile skin. Malignant hyperthermia is not like that.
 * It is <b>episodic</b> - the calcium channel misbehaves only when something
 * triggers it, classically general anaesthesia, and otherwise the horse is a
 * completely ordinary horse. There is no anaesthetic in this game and no stress
 * model to hang a trigger on, so modelling it as a permanent heart reduction
 * would be inventing a symptom the real condition does not have between
 * episodes.
 *
 * <p>So the heterozygote is {@link Condition#informational informational}: named
 * on the horse, shown in the info panel and on a paper, and free. That is the
 * honest reading, and it also makes this the clearest example in the mod of a
 * gene whose whole value is <i>knowing</i> - the American Azteca registry
 * screens breeding stock for it for exactly that reason, alongside the four this
 * breed already carried.
 *
 * <p>Two copies is a different matter. A homozygote has no normal channel at all
 * and is genuinely compromised, so that one is priced in hearts like everything
 * else - and, as with every dominant here, no founder is ever born with it.
 */
public final class Ryr1Gene extends DominantDisorderGene {

    public static final String KEY = "horsegenetics.ryr1";
    public static final int PRIORITY = 105;

    /**
     * Founders born with one copy - and therefore affected, because a dominant
     * has no carrier to hide in. Kept low: the real variant is concentrated in
     * a few Quarter Horse-derived lines rather than spread through the species.
     */
    public static final double WILD_AFFECTED_PERCENT = 1.0;

    public static final Condition MH = Condition.informational(
            "malignant-hyperthermia", "Malignant hyperthermia",
            "One copy of the RYR1 variant. The horse is completely normal to look at and to "
                    + "ride - the fault only shows under anaesthesia or extreme stress, which "
                    + "is why the condition is screened for rather than spotted.");

    public static final Condition MH_SEVERE = Condition.impairing(
            "malignant-hyperthermia-severe", "Malignant hyperthermia (two copies)",
            "Two copies of the RYR1 variant, and no normal calcium channel left. The horse "
                    + "overheats and stiffens under any real exertion, and has fewer hearts "
                    + "for it.");

    public Ryr1Gene() {
        super(KEY, "RYR1", PRIORITY,
                "MH", "Malignant hyperthermia (MH)", "N", "Wild-type (N)",
                WILD_AFFECTED_PERCENT, MH, MH_SEVERE);
    }

    /** <b>Nothing.</b> An untriggered MH horse is an ordinary horse - see the class note. */
    @Override
    protected void affectHeterozygote(TraitBuilder out) {
    }

    @Override
    protected void affectHomozygote(TraitBuilder out) {
        out.addHealth(-6.0).addSpeed(-0.03).addJump(-0.10);
    }
}
