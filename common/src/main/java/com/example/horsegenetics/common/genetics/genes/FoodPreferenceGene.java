package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Food preference</b> ({@code horsegenetics.food_preference}) - every horse
 * with this gene has one food it would rather have than anything else.
 *
 * <h2>It beats the diet, on purpose</h2>
 * The favourite is drawn independently of {@link DietGene}, so a meat-eating
 * horse can have an inexplicable weakness for something it is otherwise supposed
 * to refuse - and <b>the preference wins</b>. It will accept it, every time.
 *
 * <p>That is the most interesting thing the locus does and it is not an
 * accident. The alternative - the diet refuses, so the preference silently never
 * fires - would mean a horse could carry a gene that does literally nothing,
 * which is the failure mode the roadmap already tracks. The consequence is a
 * requirement on the feeding code rather than on this file: it must ask
 * <i>this</i> locus before it asks the diet. Two genes writing one answer is
 * exactly the silent-drift shape the contracts list exists for.
 *
 * <h2>Dominant, which makes it a breed gene</h2>
 * One copy is enough, so a heterozygote passes the favourite to half its foals
 * and a line converges quickly. A breed is temperament and husbandry far more
 * than it is statistics, and "this line will do anything for apples" is the kind
 * of invisible detail that makes a breed feel like one - one line in a breed
 * file that changes how the animal is kept.
 */
public final class FoodPreferenceGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.food_preference";
    public static final int PRIORITY = 177;

    /** Bond points for feeding the favourite, and how long the buff lasts. */
    public static final int BOND_AMOUNT = 4;
    public static final int BUFF_TICKS = 1200;

    /** The buff itself. Modest - the bond is the real reward. */
    public static final String BUFF_EFFECT = "minecraft:speed";

    /** Share of wild founders carrying one copy. Dominant, so a carrier expresses. */
    public static final double WILD_PERCENT = 18.0;

    /** One favourite: its allele and the item it names. */
    public record Favourite(Allele allele, String item, String label) {
    }

    private final List<Favourite> favourites = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final Expression[] singles;
    private final Expression wild = Expression.wildType(
            "The horse has no opinion about food beyond what its diet allows.");

    private final FounderTable founders;

    public FoodPreferenceGene() {
        fav("App", "minecraft:apple", "apples");
        fav("Car", "minecraft:carrot", "carrots");
        fav("Sug", "minecraft:sugar", "sugar");
        fav("Mel", "minecraft:melon_slice", "melon");
        fav("Bre", "minecraft:bread", "bread");
        fav("Cke", "minecraft:cake", "cake");
        fav("Bee", "minecraft:beetroot", "beetroot");
        fav("Ptt", "minecraft:baked_potato", "baked potatoes");
        fav("Cod", "minecraft:cooked_cod", "cooked cod");
        fav("Bef", "minecraft:cooked_beef", "steak");
        fav("Chr", "minecraft:sweet_berries", "sweet berries");
        fav("Coc", "minecraft:cocoa_beans", "cocoa beans");

        n = new Allele(KEY, favourites.size(), "n", "Wild-type (n)");
        List<Allele> all = new ArrayList<>();
        for (Favourite f : favourites) {
            all.add(f.allele());
        }
        all.add(n);
        alleles = List.copyOf(all);

        List<Expression> out = new ArrayList<>();
        out.add(wild);
        singles = new Expression[alleles.size()];
        for (Favourite f : favourites) {
            Expression e = Expression.wildType("loves-" + f.allele().token(),
                    "Loves " + f.label(),
                    "One copy is enough. Fed " + f.label() + ", this horse gets a short burst of "
                            + "speed and a great deal more bond than the food deserves - and it "
                            + "will accept them even if its diet says otherwise, which is the "
                            + "point of the locus rather than an oversight.");
            singles[f.allele().order()] = e;
            out.add(e);
        }
        expressions = List.copyOf(out);

        FounderTable.Builder b = FounderTable.builder();
        double each = WILD_PERCENT / favourites.size();
        for (Favourite f : favourites) {
            b.weight(f.allele(), n, each);
        }
        founders = b.weight(n, n, 100.0 - WILD_PERCENT).build();
    }

    private void fav(String token, String item, String label) {
        favourites.add(new Favourite(
                new Allele(KEY, favourites.size(), token, "Loves " + label + " (" + token + ")"),
                item, label));
    }

    /**
     * The item this horse would rather have than anything, or {@code null}.
     *
     * <p><b>This is the method the feeding code calls</b>, and it must be asked
     * before the diet is - see the class note.
     */
    public String favouriteOf(AllelePair pair) {
        int a = pair.first().order();
        return a == n.order() ? null : favourites.get(a).item();
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Food preference"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        return a == n.order() ? wild : singles[a];
    }

    /**
     * No standing effect. What this locus does happens on an interaction the
     * feeding code owns, so the ability list is empty and
     * {@link #favouriteOf} is the whole interface.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        return List.of();
    }
}
