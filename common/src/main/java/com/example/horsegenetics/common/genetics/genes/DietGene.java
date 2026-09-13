package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.DietContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Diet</b> ({@code horsegenetics.diet}) - what the horse will eat, and what
 * it gets out of it.
 *
 * <p>The wild type {@code n}, which is an ordinary horse eating ordinary horse
 * feed, and one allele per <b>narrow</b> diet - blood among them, which is the
 * odd one out twice over (below). Every narrow
 * allele is recessive to {@code n} <i>and to every other diet allele</i> - it
 * takes <b>two identical copies</b> to express, so {@code Dlava/Ding} is a
 * horse that eats hay like any other and carries two surprises. That is the
 * same "homozygous, and the same allele" rule {@link LutGene} uses, for the
 * same reason: several mutually exclusive variants at one locus have no
 * sensible blend.
 *
 * <h2>The trade</h2>
 * A narrow diet is a real cost - almost nothing feeds the horse - paid for with
 * efficiency. The numbers are on {@link Diet} and grade by breadth: a horse
 * that eats anything gets four points an item, a horse that eats raw meat gets
 * a hay bale's worth, and a horse that eats one metal goes to full health off a
 * single bar however hurt it was. Which metal, or which gem, is a per-horse roll
 * off the expressing copy's epigenetic seed, so it is fixed for life and
 * inherited with the allele.
 *
 * <h2>Wild only, and never a carrier</h2>
 * <ul>
 *   <li><b>No named breed carries any of it</b> - {@link #feralOnly()} is true,
 *       so {@code BreedFounder} forces {@code n/n} on every breed that does not
 *       name the locus. A lava-eating Shire is not a breed trait, it is a
 *       curiosity of the unbred population.</li>
 *   <li><b>A founder is never a carrier</b> - except of blood. The founder table
 *       lists {@code n/n} and the <i>homozygotes</i>, so a feral horse that
 *       rolls into this gene has the diet outright; the heterozygote exists only
 *       as something you breed. Blood is the mirror: never outright in the
 *       wild, a few silent carriers - see {@link #BLOOD_CARRIER_PERCENT}.</li>
 *   <li><b>The random splice cannot hand one to a foal.</b> Its
 *       {@link #spliceTable()} is the mirror image - {@code n/n} and the twelve
 *       <i>carriers</i> - because an unasked-for horse that only eats gold is a
 *       trap, and a carrier is a gift. The named Known Gene Splice carrot is
 *       heterozygous too, so a player who wants the diet still has to breed for
 *       it.</li>
 * </ul>
 *
 * <h2>Early, so it can be overridden</h2>
 * Priority {@link #PRIORITY}, ahead of every gene that paints. Nothing about
 * the coat needs that; what needs it is {@link HorseDiet#resolve}, which keeps
 * the <b>last</b> claim in {@code codeOrder()}, so any later gene saying "this
 * horse cannot be fed at all" wins without a special case anywhere.
 *
 * <p><b>Declared natural, and that is arguable.</b> It paints in neither phase,
 * so the flag chooses nothing about the coat; it is declared natural because
 * dietary preference is a real heritable thing and because a magical gene would
 * be thrown into {@code BreedFounder}'s magic lottery, which is the opposite of
 * what this locus wants. The lava and ingot ends of it are frank fantasy, so if
 * the natural/magical split ever means more than which coat pass a gene paints
 * in, this is the first gene to revisit. See {@code wiki/gene-diet.html}.
 */
public final class DietGene implements Gene, DietContribution {

    public static final String KEY = "horsegenetics.diet";

    /** Ahead of everything that paints - see the class note. */
    public static final int PRIORITY = 5;

    /** The narrow diets, in declaration order, then the wild type. */
    private static final Diet[] NARROW = {
            Diet.ANYTHING, Diet.RAW_MEAT, Diet.FISH, Diet.RAW_VEGETABLES, Diet.WHEAT,
            Diet.HUMAN_FOOD, Diet.CAKE, Diet.POTION, Diet.LAVA, Diet.WATER,
            Diet.INGOT, Diet.GEM, Diet.BLOOD};

    private static final String[] TOKENS = {
            "Dany", "Dmeat", "Dfish", "Dveg", "Dwht",
            "Dhum", "Dcake", "Dpot", "Dlava", "Dwat",
            "Ding", "Dgem", "Dbld"};

    /** How many founders in a hundred have each narrow diet outright. */
    private static final double EACH_PERCENT = 1.0;

    /**
     * <b>Blood is the exception to "never a carrier".</b> A wild horse that
     * could only heal by biting would not have lived to be caught, so no
     * founder has it outright - but a few carry one silent copy, the way the
     * other magical survivors' loci do, and two of those bred together is how a
     * blood-drinker turns up outside the Dhampir breed. It is also the one diet
     * the random splice never hands over, carrier or not.
     */
    public static final double BLOOD_CARRIER_PERCENT = 3.0;

    private final Map<Diet, Allele> byDiet = new LinkedHashMap<>();
    private final Map<Diet, Expression> expressionByDiet = new LinkedHashMap<>();

    /** The population baseline and the parsing default. Declared last, so it sorts last. */
    public final Allele n;

    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final FounderTable founders;
    private final FounderTable splices;

    private final Expression WILD = Expression.wildType(
            "An ordinary horse's appetite - it eats what any horse eats, and heals the way any "
                    + "horse does. Nothing here intervenes.");

    public DietGene() {
        List<Allele> all = new ArrayList<>(NARROW.length + 1);
        List<Expression> outcomes = new ArrayList<>(NARROW.length + 1);
        outcomes.add(WILD);
        for (int i = 0; i < NARROW.length; i++) {
            Diet diet = NARROW[i];
            Allele allele = new Allele(KEY, i, TOKENS[i], diet.label() + " (" + TOKENS[i] + ")");
            byDiet.put(diet, allele);
            all.add(allele);
            Expression outcome = Expression.wildType(diet.id(), diet.label(), describe(diet));
            expressionByDiet.put(diet, outcome);
            outcomes.add(outcome);
        }
        this.n = new Allele(KEY, NARROW.length, "n", "Ordinary appetite (n)");
        all.add(n);
        this.alleles = List.copyOf(all);
        this.expressions = List.copyOf(outcomes);

        FounderTable.Builder wild = FounderTable.builder();
        FounderTable.Builder splice = FounderTable.builder();
        double wildSpent = 0.0;
        double spliceSpent = 0.0;
        for (Map.Entry<Diet, Allele> e : byDiet.entrySet()) {
            Allele a = e.getValue();
            if (e.getKey() == Diet.BLOOD) {
                wild.weight(a, n, BLOOD_CARRIER_PERCENT);   // a carrier only - see the constant
                wildSpent += BLOOD_CARRIER_PERCENT;
                continue;                                   // and never a splice
            }
            wild.weight(a, a, EACH_PERCENT);        // outright, never a carrier
            splice.weight(a, n, EACH_PERCENT);      // a carrier, never outright
            wildSpent += EACH_PERCENT;
            spliceSpent += EACH_PERCENT;
        }
        this.founders = wild.weight(n, n, 100.0 - wildSpent).build();
        this.splices = splice.weight(n, n, 100.0 - spliceSpent).build();
    }

    /**
     * One sentence per diet, built from {@link Diet} so the healing claim and
     * the number it is about cannot drift apart.
     */
    private static String describe(Diet diet) {
        if (diet == Diet.BLOOD) {
            return "Blood diet. The horse eats nothing a hand can offer and does not heal beside hay "
                    + "and water. Hurt, it hunts: it bites a living mob for half a heart and heals "
                    + "three, then leaves that animal alone for a day. It never bites another horse "
                    + "or anything undead, and it only goes for a player - or a tamed cat or dog - "
                    + "when there is nothing else within reach.";
        }
        StringBuilder sb = new StringBuilder("The horse will eat nothing but ")
                .append(diet.label().toLowerCase().replace(" only", ""))
                .append(", and refuses everything else. ");
        if (diet.healsFully()) {
            sb.append("One is enough: it goes to full health however hurt it was.");
        } else {
            sb.append("One restores ").append(trim(diet.healPoints() / 2.0)).append(" hearts.");
        }
        if (diet.variants() > 0) {
            sb.append(" Which one it wants is fixed for the horse's life and inherited with the allele.");
        }
        return sb.toString();
    }

    private static String trim(double hearts) {
        return hearts == Math.floor(hearts) ? String.valueOf((long) hearts) : String.valueOf(hearts);
    }

    /** The allele that gives {@code diet}, or {@code null} for a diet no allele produces. */
    public Allele alleleFor(Diet diet) {
        return byDiet.get(diet);
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Diet"; }
    @Override public int priority() { return PRIORITY; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }
    @Override public Optional<FounderTable> spliceTable() { return Optional.of(splices); }

    /** No breed carries this - see the class note. */
    @Override public boolean feralOnly() { return true; }


    @Override
    public Expression expressionOf(AllelePair pair) {
        Diet diet = dietOf(pair);
        return diet == null ? WILD : expressionByDiet.get(diet);
    }

    /**
     * The diet {@code pair} produces, or {@code null} for a wild type. Two
     * identical narrow copies and nothing else: a heterozygote of two
     * <i>different</i> narrow alleles is an ordinary horse carrying both.
     */
    private Diet dietOf(AllelePair pair) {
        if (!pair.homozygous() || pair.has(n)) {
            return null;
        }
        for (Map.Entry<Diet, Allele> e : byDiet.entrySet()) {
            if (pair.homozygousFor(e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    // --- DietContribution -------------------------------------------------

    @Override
    public Optional<HorseDiet> diet(AllelePair pair, Genotype genotype, GeneEpigenetics random) {
        Diet diet = dietOf(pair);
        if (diet == null) {
            return Optional.empty();
        }
        if (diet.variants() <= 0) {
            return Optional.of(HorseDiet.of(diet));
        }
        // One stored number, off the expressing copy: which metal, which gem. The
        // horse asks for the same one for life, and a foal that inherits the copy
        // inherits the craving. Kept as a position rather than an index because
        // how many variants there are depends on which diet the alleles gave it.
        int pick = (int) (random.expressed().get("variant") * diet.variants());
        return Optional.of(HorseDiet.of(diet, pick));
    }
    /**
     * Which variant of its diet this horse craves - which metal, which gem. One
     * number, and the only thing about a diet that is not fixed by the alleles.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform("variant", 0, 1));
    }

}
