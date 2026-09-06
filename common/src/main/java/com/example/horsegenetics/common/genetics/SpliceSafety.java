package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Severity;
import com.example.horsegenetics.common.trait.Traits;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Which loci the Unknown Gene Splice carrot is allowed to touch</b> - the
 * blacklist, and the reason it is computed rather than typed.
 *
 * <h2>The rule</h2>
 * The Unknown Gene Splice carrot rolls a <b>random</b> locus and a random
 * combination at it, and hands the result to a foal that has not been born yet.
 * A player feeding it has chosen a surprise; they have not chosen to kill the
 * foal. So the carrot may never roll a locus that can make a horse <b>worse
 * off</b>:
 * <ul>
 *   <li>nothing that kills - the four recessive foal lethals, {@code MET}'s
 *       lethal-at-conception, {@code EDNRB}'s overo lethal white;</li>
 *   <li>nothing that costs hearts - the dwarfisms, silver's MCOA, and
 *       {@code MSTN}, which trades health for speed and carries no
 *       {@code Condition} at all;</li>
 *   <li>nothing that is merely <i>impairing</i> either. "It only takes two
 *       hearts" is still a horse the player did not ask to damage.</li>
 * </ul>
 *
 * <p>It stops there, and deliberately. A slower horse is not a damaged one -
 * {@code HMGA2}'s pony allele and magic speed's {@code Sluggish} both come out
 * of the carrot, because a surprise you did not want is exactly what a random
 * splice is for. <b>Hearts are the line.</b>
 *
 * <p>The <b>Known</b> Gene Splice carrot is deliberately not filtered by any of
 * this. There the player names the gene, has researched it, and has read what
 * it does - splicing a horse into a lethal genotype on purpose is allowed, and
 * is the player's business. The blacklist is about the <i>random</i> draw only.
 *
 * <h2>Why it is derived and not a list of gene keys</h2>
 * A hand-written blacklist is wrong the day someone adds a gene and forgets it,
 * and wrong <i>silently</i> - the failure is a dead foal in someone's world, not
 * a build error. So the pool is worked out from what the genes actually do:
 * every combination the carrot could roll is resolved through
 * {@link HorseTraits} on an otherwise wild-type horse, and the locus is dropped
 * if any of them produces a condition worse than
 * {@link Severity#INFORMATIONAL} or leaves the horse short of the baseline
 * health. A new health gene is excluded the moment it is registered, by having
 * done its job.
 *
 * <p>Informational conditions are deliberately <b>kept</b>. Splash deafness and
 * the leopard complex's night blindness are named and shown and cost the horse
 * nothing, and excluding them would delete most of the white-pattern loci from
 * the carrot for no protection at all.
 *
 * <p>As of this writing the rule drops <b>13</b> of the 46 built-in loci: the
 * sex locus (a different rule - a carrot must not flip a foal's sex), the seven
 * recessive-disorder loci, {@code MET} and {@code MILK} (both embryonic
 * lethals), {@code EDNRB} (overo lethal white), {@code SILVER} (MCOA), and
 * {@code MSTN} and magic health (hearts). None of them is named anywhere in
 * this file.
 *
 * <p>{@link Gene#spliceable()} is the manual override on top, for a gene whose
 * harm this cannot see.
 *
 * @see Gene#spliceable()
 * @see CarrotEffect
 */
public final class SpliceSafety {

    private SpliceSafety() {}

    private static volatile List<Gene> pool;

    /** Dropped when a gene is registered, exactly like {@link GenotypeCatalog}'s layout. */
    static void invalidate() {
        pool = null;
    }

    /**
     * The loci the Unknown Gene Splice carrot may roll, in
     * {@link Genes#codeOrder()} order. Never empty in practice - the coat genes
     * alone fill it - but a caller should still cope with an empty list rather
     * than index blindly.
     */
    public static List<Gene> pool() {
        List<Gene> p = pool;
        if (p == null) {
            p = compute();
            pool = p;
        }
        return p;
    }

    /** Is this one locus safe for the random splice? Public so a test can name the reason. */
    public static boolean isSafe(Gene gene) {
        // The sex locus is excluded for a different reason entirely: a carrot
        // must not flip a foal's sex. See wiki/roadmap.html#sex-linked.
        if (gene.key().equals(Genes.SEX.key())) {
            return false;
        }
        if (!gene.spliceable()) {
            return false;
        }
        Traits baseline = HorseTraits.baseline();
        // EVERY combination, including the ones canOccur rules out. That is the
        // line between MET and KIT, and it is the whole subtlety here: both
        // declare canOccur false for a homozygote, but MET's met/met is a real
        // lethal genotype the model refuses to create, while KIT's W22/W22
        // simply never existed. MET says so by declaring a lethal Condition and
        // KIT declares nothing - so reading conditions over all combinations
        // drops MET (whose allele would silently poison a breeding line) and
        // keeps KIT, sabino and dominant white in the carrot.
        for (Allele a : gene.alleles()) {
            for (Allele b : gene.alleles()) {
                if (a.order() > b.order()) {
                    continue;
                }
                Traits t;
                try {
                    t = HorseTraits.resolve(Genotype.wildType().with(new AllelePair(a, b)));
                } catch (RuntimeException e) {
                    return false; // a locus we cannot even resolve is not one to gamble a foal on
                }
                for (Condition c : t.conditions()) {
                    // Informational conditions are kept on purpose - splash
                    // deafness and the leopard complex's night blindness are
                    // named and shown and cost the horse nothing, and dropping
                    // them would delete most of the white-pattern loci from the
                    // carrot in exchange for no protection at all.
                    if (c.severity() != Severity.INFORMATIONAL) {
                        return false;
                    }
                }
                // Hearts, and only hearts. A pony allele that makes a horse
                // slower, or a Sluggish copy, is a surprise and not an injury -
                // which is what the carrot is for. Fewer hearts is an injury.
                if (t.health() < baseline.health()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Gene> compute() {
        List<Gene> out = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (isSafe(g)) {
                out.add(g);
            }
        }
        return List.copyOf(out);
    }
}
