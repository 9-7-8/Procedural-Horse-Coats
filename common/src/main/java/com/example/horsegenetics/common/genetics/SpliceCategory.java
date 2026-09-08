package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Which random splice carrot a locus belongs to.</b> The Unknown Gene Splice
 * carrot rolls anything {@link SpliceSafety} allows; a <i>themed</i> carrot
 * rolls one slice of that, so a player who wants a surprise <b>colour</b> is not
 * handed a jumping stat.
 *
 * <h2>Five themes, and what is deliberately in none of them</h2>
 * <ul>
 *   <li>{@link #DILUTION} - the loci that change what colour the pigment
 *       <i>is</i>: cream, champagne, dun, flaxen, mushroom, grey, tiger eye.</li>
 *   <li>{@link #WHITE} - the loci that take pigment away in patches: the
 *       spotting genes, roan, and the leopard complex with its two pattern
 *       modifiers.</li>
 *   <li>{@link #MARKING} - patterned or shaded pigment that is not white and
 *       not a dilution: sooty, pangar&eacute;, primitive striping, brindle.
 *       This is also the <b>fallback</b> for a natural coat gene nobody has
 *       classified, which is the safe place for one to land.</li>
 *   <li>{@link #PERFORMANCE} - the natural loci that move speed, jump or
 *       height. <b>And only upward</b>: see {@link #pairsFor}.</li>
 *   <li>{@link #MAGICAL} - everything {@link Gene#isNatural()} says is not
 *       real, whatever it does.</li>
 * </ul>
 *
 * <p><b>Extension, agouti and shade are in none of them.</b> They are the base
 * coat rather than a marking on it, and a carrot sold as "markings" that turned
 * a black horse chestnut would be answering a question the player did not ask.
 * They stay available on the unthemed Unknown Gene Splice carrot, which promises
 * nothing about what it rolls.
 *
 * <p>Neither is anything {@link SpliceSafety} excludes: a themed pool is a
 * <i>subset</i> of the safe pool, always, so no carrot in this family can hand a
 * player a damaged foal. The lethal and heart-costing loci reach a horse through
 * the <i>Known</i> Gene Splice carrot, deliberately and by name.
 *
 * <h2>Why the coat three are a declared table and the other two are derived</h2>
 * {@link #MAGICAL} and {@link #PERFORMANCE} are properties of a gene the code
 * can read - {@code isNatural()}, and whether any combination beats the baseline
 * horse. Dilution / white / marking is a <b>taxonomy a person applies</b>: dun
 * dilutes the body <i>and</i> draws a dorsal stripe, roan removes pigment
 * without being a spotting pattern, brindle paints white streaks and is still a
 * striping gene. No measurement of the finished coat sorts those the way a
 * horse person would, and being "derived" would not make a wrong answer right.
 * So the three are a table below, written against the {@link Genes} constants
 * rather than key strings - a renamed or retired gene breaks the compile here
 * instead of quietly dropping out of a carrot.
 *
 * <p>A gene not in the table falls to {@link #MARKING} if it paints and to no
 * category at all if it does not, so a drop-in gene from the gene creator lands
 * somewhere harmless rather than nowhere.
 */
public enum SpliceCategory {

    /** No theme - the original Unknown Gene Splice carrot's whole safe pool. */
    ANY("any"),
    DILUTION("dilution"),
    WHITE("white"),
    MARKING("marking"),
    PERFORMANCE("performance"),
    MAGICAL("magical");

    private final String id;

    SpliceCategory(String id) {
        this.id = id;
    }

    /** The token that appears in a carrot's effect id. */
    public String id() {
        return id;
    }

    public static SpliceCategory byId(String id) {
        for (SpliceCategory c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // The declared half
    // ------------------------------------------------------------------

    /**
     * The base coat, which is in no themed carrot - see the class note. Written
     * as a method rather than a set so it reads the same way
     * {@code HorseInfoScreen} asks the same question.
     */
    private static boolean isBaseCoat(Gene gene) {
        return gene == Genes.EXTENSION || gene == Genes.AGOUTI || gene == Genes.SHADE;
    }

    private static boolean isDilution(Gene gene) {
        return gene == Genes.MATP          // cream and pearl
                || gene == Genes.CHAMPAGNE
                || gene == Genes.DUN
                || gene == Genes.SILVER
                || gene == Genes.FLAXEN
                || gene == Genes.MUSHROOM
                || gene == Genes.GREY      // depigmentation, but of the whole horse at once
                || gene == Genes.TIGER_EYE; // dilutes the iris rather than the coat, and is still a dilution
    }

    private static boolean isWhite(Gene gene) {
        return gene == Genes.KIT
                || gene == Genes.EDNRB
                || gene == Genes.MITF
                || gene == Genes.PAX3
                || gene == Genes.TOBIANO
                || gene == Genes.MANCHADO
                || gene == Genes.ROAN
                || gene == Genes.LEOPARD
                || gene == Genes.PATN1
                || gene == Genes.PATN2;
    }

    // ------------------------------------------------------------------
    // Classification
    // ------------------------------------------------------------------

    /**
     * Which theme this locus belongs to, or {@code null} for one that belongs to
     * none - the sex locus, the base coat, the diet locus, and anything
     * {@link SpliceSafety} has already excluded.
     */
    public static SpliceCategory of(Gene gene) {
        if (!SpliceSafety.isSafe(gene) || isBaseCoat(gene)) {
            return null;
        }
        if (!gene.isNatural()) {
            return MAGICAL;
        }
        if (isDilution(gene)) {
            return DILUTION;
        }
        if (isWhite(gene)) {
            return WHITE;
        }
        if (!improvingPairs(gene).isEmpty()) {
            return PERFORMANCE;
        }
        return gene.affectsCoat() || isCoatModifier(gene) ? MARKING : null;
    }

    /**
     * A gene that paints nothing itself but is read through one that does -
     * {@code PATN1} is the worked example, and it is already in the white table.
     * This catches the next one: it has a coat dependency and no body term, so
     * the coat is the only thing it can be about.
     */
    private static boolean isCoatModifier(Gene gene) {
        return !gene.coatDependsOn().isEmpty();
    }

    // ------------------------------------------------------------------
    // The pool, and the pairs
    // ------------------------------------------------------------------

    private static volatile Map<SpliceCategory, List<Gene>> pools;

    /** Dropped when a gene is registered, exactly like {@link SpliceSafety}'s pool. */
    static void invalidate() {
        pools = null;
    }

    /**
     * The loci a carrot of this theme may roll, in {@link Genes#codeOrder()}
     * order. {@link #ANY} is {@link SpliceSafety#pool()} itself. Never empty for
     * the five themes with any real registry, but a caller should still cope
     * with an empty list rather than index blindly.
     */
    public static List<Gene> pool(SpliceCategory category) {
        if (category == null || category == ANY) {
            return SpliceSafety.pool();
        }
        Map<SpliceCategory, List<Gene>> p = pools;
        if (p == null) {
            p = compute();
            pools = p;
        }
        List<Gene> out = p.get(category);
        return out == null ? List.of() : out;
    }

    private static Map<SpliceCategory, List<Gene>> compute() {
        Map<SpliceCategory, List<Gene>> out = new EnumMap<>(SpliceCategory.class);
        for (SpliceCategory c : values()) {
            out.put(c, new ArrayList<>());
        }
        for (Gene gene : Genes.codeOrder()) {
            SpliceCategory c = of(gene);
            if (c != null) {
                out.get(c).add(gene);
            }
        }
        Map<SpliceCategory, List<Gene>> frozen = new EnumMap<>(SpliceCategory.class);
        for (Map.Entry<SpliceCategory, List<Gene>> e : out.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return frozen;
    }

    /**
     * The combinations a carrot of this theme may draw at {@code gene}.
     *
     * <p>The same as every combination for four of the five. {@link #PERFORMANCE}
     * is the exception, and it is the reason this method exists at all: the
     * carrot is sold as a <b>positive</b> health splice, and
     * {@code HMGA2}'s pony allele and {@code LCORL}'s short copy are both
     * perfectly safe - {@link SpliceSafety} keeps them, because a slower horse
     * is not a damaged one - and both are the opposite of what this carrot
     * promises. So it draws only from the combinations that beat the baseline
     * horse on something and lose to it on nothing.
     *
     * <p>Empty means "no opinion, use the gene's own splice table", which is
     * what {@code CarrotEffect} does with it.
     */
    public static List<AllelePair> pairsFor(Gene gene, SpliceCategory category) {
        return category == PERFORMANCE ? improvingPairs(gene) : List.of();
    }

    /**
     * Combinations of {@code gene} that make the horse measurably better and
     * never worse: at least one of speed, max health and jump above the
     * baseline, and none of the three below it.
     *
     * <p>Size is deliberately not read either way. A draught horse is not a
     * better horse than a pony, so a locus that only moves height contributes
     * nothing here and drops out of the performance carrot entirely - which is
     * why {@code LCORL} and {@code HMGA2} are on it for their <i>speed</i>
     * effects and not for their inches.
     */
    private static List<AllelePair> improvingPairs(Gene gene) {
        Traits baseline = HorseTraits.baseline();
        List<AllelePair> out = new ArrayList<>();
        for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
            Traits t;
            try {
                t = HorseTraits.resolve(Genotype.wildType().with(pair));
            } catch (RuntimeException unresolvable) {
                continue;
            }
            boolean worse = t.speed() < baseline.speed()
                    || t.health() < baseline.health()
                    || t.jump() < baseline.jump();
            boolean better = t.speed() > baseline.speed()
                    || t.health() > baseline.health()
                    || t.jump() > baseline.jump();
            if (better && !worse) {
                out.add(pair);
            }
        }
        return List.copyOf(out);
    }
}
