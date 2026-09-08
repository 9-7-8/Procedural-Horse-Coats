package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.Sex;

import java.util.ArrayList;
import java.util.List;

/**
 * The genetics behind the two <b>gene editors</b> - the custom horse spawn egg
 * in game and the horse designer in the browser.
 *
 * <p>Those two are one screen in two places (see the class note on either), and
 * everything here used to be written out twice in the same shape under the same
 * names. That worked while it was two short methods. It stopped working when
 * the Randomize button grew a menu: eleven modes, a lock per row and a switch
 * that excludes the invisible loci is not a thing to keep in step by reading
 * two files side by side. So the rules live here and the screens are left with
 * what they genuinely cannot share - their widgets.
 *
 * <p><b>Nothing here holds state.</b> Each method takes what it needs and hands
 * back alleles; which rows are locked, which are on the horse and what the
 * player last picked from the menu belong to the editor, because they are what
 * is on the screen rather than what is on the horse.
 */
public final class EditorRules {

    private EditorRules() {}

    /** Which loci <b>Add random</b> may draw from. */
    public enum AddScope {
        ANY("Add random - any gene", "Add random"),
        NATURAL("Add random natural", "Add natural"),
        MAGICAL("Add random magical", "Add magical");

        private final String label;
        private final String shortLabel;

        AddScope(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
        }

        /** What the menu calls it. */
        public String label() {
            return label;
        }

        /** What the button face calls it - see {@link RandomizeMode#shortLabel()}. */
        public String shortLabel() {
            return shortLabel;
        }

        public boolean covers(Gene gene) {
            return this == ANY || (this == NATURAL) == gene.isNatural();
        }
    }

    /**
     * The three loci an editor always shows as carried, however plain the
     * horse: <b>extension, agouti and shade</b>. Between them they decide
     * whether a horse is black, bay or chestnut and which bay it is, every horse
     * that has ever existed has an answer at all three, and a list that hides
     * them until you click implies a horse can be without them.
     *
     * <p>They are also the three that a player opening the editor is most likely
     * to want first, which is the smaller half of the argument.
     */
    public static boolean alwaysCarried(Gene gene) {
        return gene == Genes.EXTENSION || gene == Genes.AGOUTI || gene == Genes.SHADE;
    }

    /**
     * The pair a gene is added at: <b>a combination that actually shows</b>.
     *
     * <p>Candidates are tried in order of how obvious they are - each variant
     * allele homozygous, then two <i>different</i> variant alleles, then one
     * variant against the baseline - and the first that both
     * {@link Gene#canOccur} and is not a wild type wins. A pair the gene rules
     * out is skipped ({@code KIT}'s four nonviable {@code W} homozygotes,
     * {@code MET}'s {@code met/met}); if nothing in the list expresses, the
     * first carryable candidate is used anyway, and if there is no variant at
     * all the row stays on the baseline.
     *
     * <p>The wild-type test is what the two-different-alleles rung is for. Magic
     * sectoral heterochromia is the gene that needs it: every one of its
     * homozygotes is silent by design, so a "first variant homozygote" rule
     * would add the row and show nothing.
     */
    public static AllelePair variantPair(Gene gene) {
        Allele base = gene.defaultAllele();
        List<Allele> alleles = gene.alleles();
        List<AllelePair> candidates = new ArrayList<>();
        for (Allele a : alleles) {
            if (!a.equals(base)) {
                candidates.add(new AllelePair(a, a));
            }
        }
        for (int i = 0; i < alleles.size(); i++) {
            for (int j = i + 1; j < alleles.size(); j++) {
                Allele a = alleles.get(i);
                Allele b = alleles.get(j);
                if (!a.equals(base) && !b.equals(base)) {
                    candidates.add(new AllelePair(a, b));
                }
            }
        }
        for (Allele a : alleles) {
            if (!a.equals(base)) {
                candidates.add(new AllelePair(a, base));
            }
        }
        AllelePair carryable = null;
        for (AllelePair pair : candidates) {
            if (!gene.canOccur(pair)) {
                continue;
            }
            if (carryable == null) {
                carryable = pair;
            }
            if (!gene.expressionOf(pair).wildType()) {
                return pair;
            }
        }
        return carryable != null ? carryable : new AllelePair(base, base);
    }

    /**
     * A <b>uniform</b> genotype: every locus draws two of its own alleles with
     * equal weight, and a pair the gene refuses collapses to that gene's wild
     * type. See {@link RandomizeMode#TRUE_RANDOM} for why that is a snap-back
     * and not a re-draw.
     *
     * <p>The sex locus is skipped and stamped afterwards - the editor's Sex
     * button owns it, and a sex-linked locus needs it settled before it can be
     * filled in honestly. The wildly improbable is the point of this mode, but
     * a horse that cannot exist is not improbable, it is wrong.
     */
    public static Genotype trueRandom(Rng rng, Sex sex) {
        Genotype gt = Genotype.wildType().withSex(sex);
        for (Gene g : Genes.codeOrder()) {
            if (g == Genes.SEX) {
                continue;
            }
            List<Allele> alleles = g.alleles();
            AllelePair pair = new AllelePair(
                    alleles.get(rng.nextInt(alleles.size())),
                    alleles.get(rng.nextInt(alleles.size())));
            if (g.canOccur(pair)) {
                gt = gt.with(pair);
            }
        }
        return gt;
    }

    /**
     * Is this magical locus currently <i>showing</i> on the horse? What the
     * {@code +N magical} modes count, and deliberately not "is the row added" -
     * a magical gene sitting at a combination that does nothing has not been
     * picked in any sense a player would recognise.
     */
    public static boolean showingMagical(Genotype genotype, Gene gene) {
        if (gene.isNatural()) {
            return false;
        }
        Expression x = genotype.expressionOf(gene);
        return x != null && !x.wildType();
    }
}
