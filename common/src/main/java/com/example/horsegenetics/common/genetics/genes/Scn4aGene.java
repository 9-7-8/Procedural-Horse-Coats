package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>SCN4A</b> ({@code horsegenetics.scn4a}) - <b>HYPP</b>, hyperkalaemic
 * periodic paralysis: a sodium-channel defect that leaves the muscle liable to
 * lock up. One copy is enough, and two are fatal.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code H/N}</td><td>affected - episodic paralysis, and a horse you can still ride</td></tr>
 *   <tr><td>{@code H/H}</td><td><b>lethal at birth</b></td></tr>
 * </table>
 *
 * <h2>The first locus that is both</h2>
 * Every disorder before this one was a single thing: a heart reduction, or a
 * lethal. This is a sub-lethal heart reducer <i>and</i> a homozygous lethal on
 * the same locus, which is why {@link DominantDisorderGene} exists - see that
 * class for what breaks when a disorder stops being recessive.
 *
 * <p>The consequence for a breeder is the point of the gene. An {@code H/N}
 * horse is <b>visibly</b> unwell and can still be a good animal on every other
 * axis, so it is genuinely tempting to breed from - and two of them throw a dead
 * foal one time in four while half the survivors are affected again. It is the
 * only locus in the mod where the mistake is one you make with your eyes open.
 *
 * <h2>Founders can be affected here</h2>
 * They have to be: a dominant with no silent carrier that never appeared in a
 * founder could never appear at all. {@value #WILD_AFFECTED_PERCENT}% of wild
 * horses are {@code H/N}, kept low precisely because every one of them is sick
 * rather than merely carrying something.
 *
 * <h2>The numbers are a rendering, not arithmetic</h2>
 * The reference table gives HYPP as roughly &ldquo;30% health, 60% speed and
 * jump&rdquo;, in a severity model this mod does not have
 * (<a href="../../../../../../../wiki/roadmap.html#decisions">roadmap &sect;3</a>
 * flags it and does not decide it). Read literally against a 22-health baseline
 * those percentages would make an <i>affected but living</i> horse worse than
 * several of the mod's outright lethals, which is plainly not what the table
 * means. So the figures here are calibrated against the disorders that already
 * ship - the worst survivable outcome in the mod, and comfortably short of a
 * lethal - and the reference is used for the <em>ordering</em>, not the
 * arithmetic.
 */
public final class Scn4aGene extends DominantDisorderGene {

    public static final String KEY = "horsegenetics.scn4a";
    public static final int PRIORITY = 93;

    /** Share of founders born {@code H/N} - and therefore born affected. */
    public static final double WILD_AFFECTED_PERCENT = 1.2;

    public static final Condition HYPP = Condition.impairing(
            "hypp", "HYPP (periodic paralysis)",
            "A sodium-channel defect. The muscle locks up in episodes the horse cannot "
                    + "control - it tires early, moves stiffly and jumps badly, and one copy "
                    + "is enough to cause it.");

    public static final Condition HYPP_LETHAL = Condition.lethalAtBirth(
            "hypp-lethal", "HYPP (two copies)",
            "Two copies of the defect. The foal cannot breathe reliably from the moment it "
                    + "is born and does not survive it.");

    public Scn4aGene() {
        super(KEY, "SCN4A (HYPP)", PRIORITY,
                "H", "HYPP (H)", "N", "Wild-type (N)",
                WILD_AFFECTED_PERCENT, HYPP, HYPP_LETHAL);
    }

    @Override
    protected void affectHeterozygote(TraitBuilder out) {
        out.addHealth(-6.0).addSpeed(-0.040).addJump(-0.18);
    }

    @Override
    protected void affectHomozygote(TraitBuilder out) {
        out.addHealth(-16.0).addSpeed(-0.070).addJump(-0.30);
    }
}
