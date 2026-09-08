package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.GeneCategory;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBreakdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>What two horses could produce, locus by locus.</b> A Punnett square per
 * gene, for the browser's Breeding preview tab.
 *
 * <h2>What it deliberately is not</h2>
 * It does <b>not</b> enumerate foal genotypes. The product over every gene is
 * astronomically large and would answer a question nobody asked; and it does
 * not draw a foal, because a predicted coat is a promise the draw does not
 * make. What it answers is the one thing a person actually wants before
 * committing two horses to a pairing: <i>can this pair throw that allele, and
 * how often</i>. Every number here is a per-locus probability, and the loci are
 * independent of each other exactly as the segregation is.
 *
 * <h2>The odds are the real ones</h2>
 * Each parent contributes one of its two copies with equal probability - the
 * same coin {@link Genotype#breedWith} flips - so a carrier x carrier locus
 * reads one in four, and reads it because the model says so rather than because
 * a table was typed in. Sex-linked loci are folded through the foal's own sex
 * draw ({@link #SEX_SHARE} each way), which is why an {@code X}-linked
 * recessive comes out at different odds for a colt and a filly.
 *
 * <h2>Grouping</h2>
 * The mod has no polygenic <i>inheritance</i> - every real cluster is modelled
 * as one atomic gene (see {@code wiki/philosophy.html}) - but it does have
 * several genes pulling on one visible trait, and those are the ones a beginner
 * needs to see together. {@link #groups} finds them without a hand-kept table:
 * the body axes come out of {@link TraitBreakdown}, so every gene that moves
 * speed lands under Speed; and on the coat side {@link Gene#coatDependsOn()}
 * nests a silent modifier under the gene that reads it, so the leopard complex
 * and its two {@code PATN} modifiers arrive as one block.
 */
public final class BreedingPreview {

    /** A foal is a colt or a filly with equal probability - the sex locus's own draw. */
    public static final double SEX_SHARE = 0.5;

    /** One combination the foal could land on at one locus. */
    public record Outcome(AllelePair pair, double chance) implements Comparable<Outcome> {

        public Expression expression() {
            return pair.expression();
        }

        /** Does this combination do anything, or is it the population baseline? */
        public boolean expressing() {
            return !expression().wildType();
        }

        @Override
        public int compareTo(Outcome other) {
            int byChance = Double.compare(other.chance, chance);
            return byChance != 0 ? byChance : pair.toTokens().compareTo(other.pair.toTokens());
        }
    }

    /** One locus: what each parent holds, and everything the foal could get. */
    public record Locus(Gene gene, AllelePair dam, AllelePair sire, List<Outcome> outcomes) {

        public Locus {
            outcomes = List.copyOf(outcomes);
        }

        /** True when every outcome is the same pair - nothing to decide here. */
        public boolean settled() {
            return outcomes.size() <= 1;
        }

        /** True when some outcome does something neither parent's own pair does. */
        public boolean canSurprise() {
            for (Outcome o : outcomes) {
                if (o.expressing() && !o.pair().equals(dam) && !o.pair().equals(sire)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** A named block of loci that pull on one trait. */
    public record Group(String label, String note, List<Locus> loci) {

        public Group {
            loci = List.copyOf(loci);
        }
    }

    private BreedingPreview() {
    }

    // ------------------------------------------------------------------
    // One locus
    // ------------------------------------------------------------------

    /**
     * Every combination this pairing can produce at {@code gene}, with its
     * probability, commonest first.
     *
     * <p>The parents arrive as whole genotypes rather than as pairs because a
     * sex-linked locus needs to know which parent is which and what the other
     * one is holding - the same information {@code Genotype.sexLinkedPair}
     * reads.
     */
    public static Locus locus(Gene gene, Genotype dam, Genotype sire) {
        Map<AllelePair, Double> weights = new LinkedHashMap<>();
        if (gene.inheritance().sexLinked()) {
            accumulateSexLinked(gene, dam, sire, weights);
        } else {
            for (Allele fromDam : copiesOf(dam.pair(gene))) {
                for (Allele fromSire : copiesOf(sire.pair(gene))) {
                    add(weights, new AllelePair(fromDam, fromSire), 0.25);
                }
            }
        }
        List<Outcome> outcomes = new ArrayList<>();
        for (Map.Entry<AllelePair, Double> e : weights.entrySet()) {
            outcomes.add(new Outcome(e.getKey(), e.getValue()));
        }
        outcomes.sort(null);
        return new Locus(gene, dam.pair(gene), sire.pair(gene), outcomes);
    }

    /**
     * A sex-linked locus, folded over the foal's own sex draw. Mirrors
     * {@code Genotype.sexLinkedPair} exactly: on the {@code X} a filly takes her
     * dam's coin and her sire's single copy while a colt takes his dam's coin
     * and the reserved slot; on the {@code Y} the dam contributes nothing at
     * all and a filly has no copy of the locus.
     */
    private static void accumulateSexLinked(Gene gene, Genotype dam, Genotype sire,
                                            Map<AllelePair, Double> weights) {
        Allele placeholder = gene.hemizygousPlaceholder();
        List<Allele> sireReal = gene.realAlleles(sire.pair(gene));
        Allele fromSire = sireReal.isEmpty() ? placeholder : sireReal.get(0);

        if (gene.inheritance() == Inheritance.X_LINKED) {
            for (Allele fromDam : copiesOf(dam.pair(gene))) {
                add(weights, new AllelePair(fromDam, fromSire), SEX_SHARE * 0.5);    // filly
                add(weights, new AllelePair(fromDam, placeholder), SEX_SHARE * 0.5); // colt
            }
            return;
        }
        add(weights, new AllelePair(placeholder, placeholder), SEX_SHARE); // filly
        add(weights, new AllelePair(placeholder, fromSire), SEX_SHARE);    // colt
    }

    /**
     * The two copies a parent can pass, as two entries even when they are the
     * same allele - so every branch is worth the same quarter and the weights
     * add up without a special case for a homozygote.
     */
    private static List<Allele> copiesOf(AllelePair pair) {
        return List.of(pair.first(), pair.second());
    }

    private static void add(Map<AllelePair, Double> weights, AllelePair pair, double share) {
        weights.merge(pair, share, Double::sum);
    }

    // ------------------------------------------------------------------
    // Grouping
    // ------------------------------------------------------------------

    /**
     * Every locus worth showing, in blocks. A locus whose outcome is already
     * settled - both parents homozygous for the same allele - is dropped unless
     * {@code includeSettled}, because a wall of one-outcome rows is exactly what
     * makes this screen unreadable for the person it is for.
     *
     * <p>Which horse is the dam is <b>not</b> trusted from the caller at the
     * sex-linked loci; see {@link #isDam}.
     */
    public static List<Group> groups(Genotype dam, Genotype sire, boolean includeSettled) {
        Map<String, Locus> byKey = new LinkedHashMap<>();
        for (Gene gene : Genes.codeOrder()) {
            Locus l = locus(gene, dam, sire);
            if (includeSettled || !l.settled()) {
                byKey.put(gene.key(), l);
            }
        }

        List<Group> groups = new ArrayList<>();
        Set<String> taken = new LinkedHashSet<>();

        for (StatAxis axis : StatAxis.values()) {
            List<Locus> loci = new ArrayList<>();
            for (Locus l : byKey.values()) {
                if (touchesAxis(l, axis)) {
                    loci.add(l);
                }
            }
            if (!loci.isEmpty()) {
                for (Locus l : loci) {
                    taken.add(l.gene().key());
                }
                groups.add(new Group(axisLabel(axis), axisNote(axis), loci));
            }
        }

        List<Locus> disorders = new ArrayList<>();
        for (Locus l : byKey.values()) {
            if (!taken.contains(l.gene().key()) && GeneCategory.of(l.gene()) == GeneCategory.BODY) {
                disorders.add(l);
            }
        }
        if (!disorders.isEmpty()) {
            for (Locus l : disorders) {
                taken.add(l.gene().key());
            }
            groups.add(new Group("Disorders",
                    "Mostly recessive: two carriers are the only way to see one.",
                    disorders));
        }

        // Coat. A silent modifier is nested directly under the gene that reads
        // it, so the leopard complex arrives as one block rather than as three
        // scattered rows. coatDependsOn() is the model's own declaration of that
        // link, so nothing is hard-coded here.
        Set<String> modifiers = new LinkedHashSet<>();
        for (Locus l : byKey.values()) {
            modifiers.addAll(l.gene().coatDependsOn());
        }
        List<Locus> coat = new ArrayList<>();
        for (Locus l : byKey.values()) {
            if (taken.contains(l.gene().key()) || modifiers.contains(l.gene().key())
                    || GeneCategory.of(l.gene()) != GeneCategory.COAT) {
                continue;
            }
            coat.add(l);
            taken.add(l.gene().key());
            for (String dependency : l.gene().coatDependsOn()) {
                Locus modifier = byKey.get(dependency);
                if (modifier != null && taken.add(dependency)) {
                    coat.add(modifier);
                }
            }
        }
        // A modifier whose reader is settled (and so absent above) still belongs here.
        for (Locus l : byKey.values()) {
            if (GeneCategory.of(l.gene()) == GeneCategory.COAT && taken.add(l.gene().key())) {
                coat.add(l);
            }
        }
        if (!coat.isEmpty()) {
            groups.add(new Group("Coat",
                    "An indented gene only means anything on a horse that expresses the one above it.",
                    coat));
        }

        List<Locus> other = new ArrayList<>();
        for (Locus l : byKey.values()) {
            if (!taken.contains(l.gene().key())) {
                other.add(l);
            }
        }
        if (!other.isEmpty()) {
            groups.add(new Group("Abilities, diet and eyes",
                    "Genes that reach the horse through something other than its coat or its body.",
                    other));
        }
        return List.copyOf(groups);
    }

    /** Is this locus nested under another one - a silent modifier someone else reads? */
    public static boolean isModifier(Gene gene) {
        for (Gene other : Genes.codeOrder()) {
            if (other.coatDependsOn().contains(gene.key())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does this locus move {@code axis} for <i>any</i> combination it could
     * produce? Asked of the outcomes rather than of the gene, so a locus that
     * happens to be silent in this particular pairing does not clutter the
     * block - and asked through {@link TraitBreakdown}, so it is the gene's own
     * arithmetic that answers.
     */
    private static boolean touchesAxis(Locus locus, StatAxis axis) {
        if (GeneCategory.of(locus.gene()) != GeneCategory.BODY) {
            return false;
        }
        for (Outcome o : locus.outcomes()) {
            Genotype probe = Genotype.wildType().with(o.pair());
            for (TraitBreakdown.Term term : TraitBreakdown.of(probe, null, true)) {
                if (term.gene() == locus.gene() && term.touches(axis)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String axisLabel(StatAxis axis) {
        return switch (axis) {
            case SPEED -> "Speed";
            case HEALTH -> "Max health";
            case JUMP -> "Jump strength";
            case SCALE -> "Size";
        };
    }

    private static String axisNote(StatAxis axis) {
        return switch (axis) {
            case SPEED -> "Several loci add into one number - a fast horse is fast at all of them.";
            case HEALTH -> "Several loci add into one number, and a disorder can take it back.";
            case JUMP -> "Several loci add into one number.";
            case SCALE -> "Height loci add; dwarfism multiplies, so it shrinks a draught horse too.";
        };
    }

    /**
     * Which of the two is the dam. Read off the sex locus rather than trusted
     * from the caller: segregation is symmetric at every autosomal locus, so
     * getting it backwards is invisible until a sex-linked one, where it is
     * simply wrong.
     */
    public static boolean isDam(Genotype genotype) {
        return genotype.sex() == Sex.FEMALE;
    }
}
