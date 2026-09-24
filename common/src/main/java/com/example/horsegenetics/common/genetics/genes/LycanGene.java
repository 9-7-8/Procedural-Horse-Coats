package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>LYCAN</b> ({@code horsegenetics.lycan}) - the werewolf locus. A horse with
 * two matching copies <b>stops being a horse at nightfall</b>: it becomes a real
 * wolf, or a real cat, or a real chicken, until the sun comes up.
 *
 * <h2>Two copies of the <i>same</i> allele, or nothing</h2>
 * Every allele here is recessive, and recessive to each other as well as to the
 * wild type. {@code Wlf/n} is a horse. {@code Wlf/Cat} is <b>also</b> a horse -
 * two shapes and it settles on neither, which is the outcome
 * {@link #MISMATCHED} names. Only a matched pair shifts.
 *
 * <p>That is the whole design of the locus and it is what makes it worth
 * breeding. A locus with thirty-seven alleles where any one of them showed would
 * be a slot machine; a locus where you must find the <i>same</i> allele twice is
 * a search with a target. Two wild shifters caught in different biomes are
 * probably different animals, and their foals are horses - the disappointment is
 * the game.
 *
 * <h2>What the allele set is</h2>
 * {@link MobRoster#ground()}: every vanilla mob that is both <b>non-hostile</b>
 * and <b>walks on land</b>, minus the horse family, because a horse that turns
 * into a horse at night is not a gene. That is a mechanical rule rather than a
 * taste, which matters: it is the reason the list contains a strider and a
 * wandering trader without anybody having to defend either. The two golems, the
 * villager, the copper golem and the mannequin are {@code MISC} and so are out;
 * the mob ids are strings because {@code common/} may not import Minecraft, and
 * the translator resolves them against the live registry - an id this game
 * version has never heard of simply never shifts.
 *
 * <h2>Nothing that swims, nothing that flies, nothing hostile</h2>
 * The other two per-mob loci - {@link PackLeaderGene}, {@link SpawnerGene} - are
 * happy to name a squid or a bee, because there the mob turns up <i>beside</i>
 * the horse. This one <b>replaces</b> the horse, and a body is not a costume: a
 * were-cod left a horse drowning in air on a hillside, and a were-bat flew off
 * over the treeline with somebody's mare inside it. So the roster's
 * {@link MobRoster.Habitat} tag is a hard gate here rather than a note, and it
 * is enforced twice - once by this constructor taking only
 * {@link MobRoster.Habitat#GROUND} entries, and again in the game module, where
 * the shift refuses a resolved entity type whose spawn category or navigation
 * says it swims, flies or hunts. The second check is the one that will still
 * hold when the roster is derived from the live registry and starts naming
 * mobs nobody here has read.
 *
 * <h2>What is not in this file</h2>
 * The <b>shift itself</b> - swapping the horse for a real animal at dusk and
 * back at dawn, banning riding, the walking cloud of particles and the "hit me
 * and I will follow you until sunrise" temper - is behaviour, and behaviour
 * lives in the game module: {@code neoforge/server/LycanthropyHandler} and its
 * goal. Same split as {@link SunSensitivityGene}, for the same reason - each of those
 * would be an {@code effects} verb with exactly one user.
 *
 * <p>This file owns the genetics and the one thing about a shifted animal that
 * is <b>heritable</b>: the colour of the cloud it kicks up as it walks, drawn on
 * the allele copy and inherited with it, so a line of were-cats can be bred
 * toward a particular colour of cat.
 *
 * <p>It paints nothing - every outcome is a {@link Expression#wildType() wild
 * type}, the locus is out of the texture key, and the whole wide allele set
 * costs the genotype gallery one entry.
 */
public final class LycanGene implements Gene {

    public static final String KEY = "horsegenetics.lycan";

    /** Just after rainbow dust (151); it paints nothing, so the number is only a code-order slot. */
    public static final int PRIORITY = 152;

    /**
     * How many founders in a hundred are born homozygous for <b>each</b> of the
     * forms. One of these per form is the whole wild share of the locus, so the
     * chance of meeting <i>a</i> shifter moves with the size of
     * {@link MobRoster#ground()} while the chance of meeting a were-wolf
     * specifically does not - which is the right way round. It is the one
     * animal you are hunting for, and it should not get rarer because the game
     * added a mob.
     *
     * <p>No founder is a carrier and none is mismatched, which is the rule the
     * particle locus settled: a combination that shows nothing is a combination
     * a wild horse is not caught in, because the alternative is a locus whose
     * alleles can only be found by accident. Every shifter you meet in the world
     * is one you watched shift.
     */
    public static final double WILD_HOMOZYGOUS_PERCENT = 0.03;

    /** One allele: the mob it turns into, and the prose the outcome is described with. */
    public record Form(Allele allele, String mob, String label, String article) {

        /** {@code "a wolf"} / {@code "an ocelot"} - the description reads better than a bare noun. */
        public String withArticle() {
            return article + " " + label.toLowerCase(Locale.ROOT);
        }
    }

    private final List<Form> forms = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;

    /** Indexed by allele order; {@code null} at the wild type's slot. */
    private final Expression[] shifted;

    private final Expression WILD = Expression.wildType(
            "An ordinary horse, at every hour of the night.");

    /**
     * One copy, and the horse is a horse. Deliberately its own named outcome
     * rather than a fold into {@link #WILD}: a carrier is the thing you have
     * when you are halfway to a shifter, and the gene dictionary should be able
     * to say so even though nothing about the horse looks different.
     */
    private final Expression CARRIER = Expression.wildType("lycan-carrier", "Lycanthropy carrier",
            "One copy of a shape and one wild type. The horse is a horse, all night, every "
                    + "night - but bred to another carrier of the same shape, a quarter of its "
                    + "foals will not be.");

    /**
     * Two shapes, and it settles on neither. The outcome that makes the locus a
     * search rather than a lottery.
     */
    public final Expression MISMATCHED = Expression.wildType("lycan-mismatched", "Two shapes, neither taken",
            "Two different shapes, one on each copy, and the horse stays a horse - a shift needs "
                    + "the same animal twice. Both are carried and both are passed on, so this "
                    + "horse is one parent of two different lines.");

    private final FounderTable founders;

    public LycanGene() {
        // The roster's order: alphabetical by mob id. Unlike the particle locus
        // there is no rank here to encode - nothing is dominant to anything - so
        // the order is only the genotype code's layout, and alphabetical is the
        // order that makes a missing mob obvious when the game adds one.
        for (MobRoster.Entry e : MobRoster.ground()) {
            form(e.token(), e.mob(), e.label());
        }

        n = new Allele(KEY, forms.size(), "n", "Wild-type (n)");

        List<Allele> all = new ArrayList<>(forms.size() + 1);
        for (Form f : forms) {
            all.add(f.allele());
        }
        all.add(n);
        alleles = List.copyOf(all);

        List<Expression> out = new ArrayList<>();
        out.add(WILD);
        out.add(CARRIER);
        out.add(MISMATCHED);
        shifted = new Expression[alleles.size()];
        for (Form f : forms) {
            Expression e = Expression.wildType("lycan-" + idOf(f), f.label(),
                    "At nightfall this horse becomes " + f.withArticle() + " - a real one, which "
                            + "can be done everything to that " + f.withArticle() + " can be done "
                            + "to, and nothing else. It cannot be ridden, it trails a faint cloud "
                            + "of coloured dust as it moves, and anything that strikes it is "
                            + "followed until sunrise. At dawn it is the same horse it was, with "
                            + "the same name and the same pedigree.");
            shifted[f.allele().order()] = e;
            out.add(e);
        }
        expressions = List.copyOf(out);

        FounderTable.Builder b = FounderTable.builder();
        for (Form f : forms) {
            b.weight(f.allele(), WILD_HOMOZYGOUS_PERCENT);
        }
        founders = b.weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT * forms.size()).build();
    }

    private void form(String token, String mob, String label) {
        Allele a = new Allele(KEY, forms.size(), token, label + " (" + token + ")");
        forms.add(new Form(a, mob, label, article(label)));
    }

    /**
     * {@code "an ocelot"}, {@code "a wolf"}. Read off the label rather than
     * carried beside it: every mob name in the roster takes the article its
     * first letter implies, and a hand-written column would only be one more
     * thing to forget when the registry starts supplying the names.
     */
    private static String article(String label) {
        return "AEIOU".indexOf(Character.toUpperCase(label.charAt(0))) >= 0 ? "an" : "a";
    }

    private static String idOf(Form f) {
        return f.allele().token().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------

    @Override public String key() { return KEY; }
    @Override public String name() { return "Lycanthropy"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.EPIC; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }


    /** Every form this gene defines, in declaration order - for the wiki and the tests. */
    public List<Form> forms() {
        return List.copyOf(forms);
    }

    /** The wild type. */
    public Allele wildTypeAllele() {
        return n;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (b == n.order()) {
            // n sorts last, so second == n means at least one copy is wild.
            return a == n.order() ? WILD : CARRIER;
        }
        return a == b ? shifted[a] : MISMATCHED;
    }

    /**
     * The animal this horse becomes at night, or {@code null} if it stays a
     * horse. The single question the NeoForge handler asks this gene.
     */
    public Form formOf(AllelePair pair) {
        if (pair == null) {
            return null;
        }
        int a = pair.first().order();
        return a == pair.second().order() && a != n.order() ? forms.get(a) : null;
    }

    /** Does this horse shift at all? */
    public boolean shifts(AllelePair pair) {
        return formOf(pair) != null;
    }

    /**
     * The colour of the cloud a shifted animal kicks up as it walks - this
     * gene's one epigenetic value, and the only thing about a shifter that two
     * horses carrying the same allele do not share.
     *
     * <p>Held to a bright saturation and value like every other rolled colour in
     * the mod: a faint cloud round a wolf at night is the whole visual, and one
     * that rolled a muddy olive would read as a bug rather than as variety.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.colour("cloud", 0.55, 1.00, 0.70, 1.00));
    }
}
