package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Passification</b> ({@code horsegenetics.passification}) - the way in to a
 * horse that would otherwise kill you, or that would otherwise never let you
 * near it at all.
 *
 * <p>{@link AggressionGene} can produce a wild horse that attacks players on
 * sight, and {@link SkittishGene} one that flees every player on sight; either
 * way the horse cannot be approached, so it cannot be fed, so it cannot be
 * tamed, and the allele is a dead end wherever it lands. This locus is the
 * answer. It adds no aggression or fear of its own and it never changes a
 * horse's temperament; all it does is say <i>what you can offer a horse to
 * make it stop treating you as a threat</i>, whether that threat was one it
 * meant to fight or one it meant to run from.
 *
 * <h2>It overrides every other aggression effect</h2>
 * Passification is a <b>veto</b>, not a competing behaviour: while it holds, the
 * horse will not select the player as a target for any reason - not the
 * aggression locus, not a herd-mate's alarm, not a hit it took. That has to
 * outrank everything, so the check lives at the one chokepoint every aggression
 * path goes through rather than in a priority number here (priority is a
 * code-order slot for a gene that paints nothing, and could not enforce this).
 *
 * <p><b>Harmful auras still apply.</b> Passification is about <i>targeting</i>;
 * a horse that damages what stands near it goes on doing so, and a player who
 * walks into one has not been betrayed by this gene.
 *
 * <h2>Every allele is incomplete dominant, and they stack</h2>
 * <b>One copy expresses.</b> A horse carrying two different routes offers
 * <b>both</b>, independently - {@code Prm/FTm} can be permanently passified with
 * one offering or temporarily calmed with another, and is worth more than either
 * parent. That is the owner's call and it is the point of the locus: there
 * should be many ways to passify a horse, not a precedence ladder that wastes
 * half of them. It is why this reads {@link GeneEpigenetics#copy(int) both
 * copies} rather than the expressed one.
 *
 * <h2>The offering is written on the allele copy</h2>
 * Which item, how much of it, and - for the temporary routes - how long the calm
 * lasts and how long before it can be offered again, are all
 * {@linkplain #epiSchema() epigenetic}: rolled once for a founder, written on
 * the copy, inherited with it, and drifting slightly each generation. So two
 * {@code Prm/n} horses are not the same horse - one wants four apples and the
 * other wants a single golden carrot - and a line can be bred toward an offering
 * its owner can actually afford.
 *
 * <h2>Where the behaviour lives</h2>
 * Here: the alleles, the numbers, and {@link #routesOf} - a pure function from a
 * combination to what it offers. In the NeoForge module: the feeding, the
 * timers, and the veto. Same split as {@link LycanGene} and
 * {@link SunSensitivityGene}, and for the same reason - this would be an
 * {@code effects} verb with exactly one user.
 *
 * <p>Paints nothing; the locus is out of the texture key.
 */
public final class PassificationGene implements Gene {

    public static final String KEY = "horsegenetics.passification";

    /**
     * Last of the behaviour family. The number does not enforce the override -
     * see the class note - but a reader who sorts the registry should find this
     * after the loci it talks over.
     */
    public static final int PRIORITY = 198;

    // ------------------------------------------------------------------
    // The epigenetic offering
    // ------------------------------------------------------------------

    /** Which item the horse wants, as an index into {@link #OFFERINGS}. */
    public static final String ITEM = "offering";

    /** How many of it. */
    public static final String AMOUNT = "amount";

    /** How long a temporary calm lasts, in ticks. */
    public static final String DURATION = "duration";

    /** How long before it can be offered again, in ticks. */
    public static final String COOLDOWN = "cooldown";

    /**
     * What a horse may ask for. A closed list because the value stored is an
     * <b>index</b> into it: the epigenome holds numbers, so "the item" can only
     * ever be a position in a table the gene owns. Adding to the end is free;
     * re-ordering rewrites what every existing horse wants.
     */
    public static final List<String> OFFERINGS = List.of(
            "minecraft:apple",
            "minecraft:golden_apple",
            "minecraft:carrot",
            "minecraft:golden_carrot",
            "minecraft:sugar",
            "minecraft:wheat",
            "minecraft:hay_block",
            "minecraft:bread",
            "minecraft:melon_slice",
            "minecraft:sweet_berries",
            "minecraft:beetroot",
            "minecraft:pumpkin",
            "minecraft:nether_wart",
            "minecraft:cake");

    /** Fewest and most of the offering a horse may ask for. */
    public static final double MIN_AMOUNT = 1.0;
    public static final double MAX_AMOUNT = 6.0;

    /** How long a temporary calm may last, in ticks - one to ten minutes. */
    public static final double MIN_DURATION = 1_200.0;
    public static final double MAX_DURATION = 12_000.0;

    /** How long between temporary calms, in ticks - half a minute to five. */
    public static final double MIN_COOLDOWN = 600.0;
    public static final double MAX_COOLDOWN = 6_000.0;

    private static final EpiSchema SCHEMA = EpiSchema.of(
            EpiValue.category(ITEM, OFFERINGS.size()),
            // Biased low: most horses want one or two of something, and a horse
            // that wants six golden apples is a breeding problem, not a default.
            EpiValue.power(AMOUNT, MIN_AMOUNT, MAX_AMOUNT, 1.8),
            EpiValue.uniform(DURATION, MIN_DURATION, MAX_DURATION),
            EpiValue.uniform(COOLDOWN, MIN_COOLDOWN, MAX_COOLDOWN));

    // ------------------------------------------------------------------
    // The alleles
    // ------------------------------------------------------------------

    /** How long the calm lasts once it has been bought. */
    public enum Kind {
        /** Bought once, and the horse never targets that player again. */
        PERMANENT,
        /** Bought again and again, each time for a while. */
        TEMPORARY
    }

    /** What stage of the horse's life the route is open in. */
    public enum Window {
        /** Foal and adult alike. */
        ANY("at any age"),
        /** Only while it is a foal - miss the window and the adult is beyond reach. */
        FOAL("only while it is a foal"),
        /** Only once it is grown. */
        ADULT("only once it is grown");

        private final String phrase;

        Window(String phrase) {
            this.phrase = phrase;
        }

        public String phrase() {
            return phrase;
        }
    }

    /**
     * One way in: what it costs, how long it lasts, and when it is open. The
     * value {@link #routesOf} hands the translator.
     *
     * @param item          item id to feed
     * @param amount        how many of it
     * @param durationTicks how long the calm lasts; ignored for
     *                      {@link Kind#PERMANENT}
     * @param cooldownTicks how long before it may be bought again; ignored for
     *                      {@link Kind#PERMANENT}
     */
    public record Route(Kind kind, Window window, String item, int amount,
                        int durationTicks, int cooldownTicks) {
    }

    /** One variant allele of the locus. */
    private record Variant(Allele allele, Kind kind, Window window, String name) {
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final Expression[] byVariant;

    private final Expression wild = Expression.wildType(
            "No way in. If this horse is aggressive to players it stays that way, and the only "
                    + "thing to do with it is keep a fence between you.");

    /**
     * Two different routes, both open. Its own outcome rather than a fold into
     * one of them, because a compound heterozygote is the thing a breeder is
     * actually working toward here.
     */
    private final Expression both = Expression.wildType("passification-both", "Two ways in",
            "Two different passification alleles, and the horse offers BOTH - either route "
                    + "works, each with its own offering. Worth more than either parent, which "
                    + "is the point of the locus.");

    private final FounderTable founders;

    /**
     * Share of wild horses carrying each route as a single copy. Generous on
     * purpose: the locus is worthless unless the aggressive horses that need it
     * are also reasonably likely to have it.
     */
    public static final double WILD_CARRIER_EACH_PERCENT = 4.0;

    /** Share of wild horses homozygous for each route. */
    public static final double WILD_HOMOZYGOUS_EACH_PERCENT = 0.5;

    public PassificationGene() {
        variant(Kind.PERMANENT, Window.ANY, "Prm", "Permanently passified");
        variant(Kind.TEMPORARY, Window.ANY, "Tmp", "Temporarily passified");
        variant(Kind.PERMANENT, Window.FOAL, "FPr", "Permanently passified as a foal");
        variant(Kind.PERMANENT, Window.ADULT, "APr", "Permanently passified as an adult");
        variant(Kind.TEMPORARY, Window.FOAL, "FTm", "Temporarily passified as a foal");
        variant(Kind.TEMPORARY, Window.ADULT, "ATm", "Temporarily passified as an adult");

        List<Allele> built = new ArrayList<>();
        for (Variant v : variants) {
            built.add(v.allele());
        }
        this.n = new Allele(KEY, variants.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        List<Expression> all = new ArrayList<>();
        all.add(wild);
        this.byVariant = new Expression[alleles.size()];
        for (Variant v : variants) {
            Expression e = Expression.wildType(
                    "passification-" + v.allele().token().toLowerCase(java.util.Locale.ROOT),
                    v.name(),
                    "One copy is enough. Feed this horse what it asks for, "
                            + v.window().phrase() + ", and it "
                            + (v.kind() == Kind.PERMANENT
                                    ? "will never target you again."
                                    : "leaves you alone for a while - then wants asking again.")
                            + " What it wants, and how much, is written on the allele copy and "
                            + "inherited with it.");
            byVariant[v.allele().order()] = e;
            all.add(e);
        }
        all.add(both);
        this.expressions = List.copyOf(all);

        FounderTable.Builder table = FounderTable.builder();
        double claimed = 0.0;
        for (Variant v : variants) {
            table.weight(v.allele(), WILD_HOMOZYGOUS_EACH_PERCENT);
            table.weight(v.allele(), n, WILD_CARRIER_EACH_PERCENT);
            claimed += WILD_HOMOZYGOUS_EACH_PERCENT + WILD_CARRIER_EACH_PERCENT;
        }
        // The baseline last - a FounderTable invariant GenotypeTest asserts.
        this.founders = table.weight(n, n, 100.0 - claimed).build();
    }

    private void variant(Kind kind, Window window, String token, String name) {
        variants.add(new Variant(new Allele(KEY, variants.size(), token, name + " (" + token + ")"),
                kind, window, name));
    }

    // ------------------------------------------------------------------

    /**
     * <b>Every way into this horse.</b> One entry per expressing copy, so a
     * compound heterozygote returns two and a homozygote returns one - the
     * numbers on a horse's two copies of the same allele are not necessarily
     * equal, but offering the same route twice at two prices would be a bug
     * rather than a feature, so a matched pair reads the first copy only.
     *
     * <p>Empty for a horse with no passification allele at all, which is most of
     * them.
     */
    public List<Route> routesOf(AllelePair pair, GeneEpigenetics epigenetics) {
        List<Route> routes = new ArrayList<>(2);
        for (int slot = 0; slot < 2; slot++) {
            Allele allele = slot == 0 ? pair.first() : pair.second();
            if (allele.equals(n)) {
                continue;
            }
            if (slot == 1 && pair.first().equals(pair.second())) {
                continue; // a matched pair is one route, not two
            }
            Variant v = variants.get(allele.order());
            routes.add(routeOf(v, epigenetics.copy(slot)));
        }
        return List.copyOf(routes);
    }

    private Route routeOf(Variant v, EpiValues epi) {
        int index = (int) Math.floor(epi.get(ITEM));
        if (index < 0 || index >= OFFERINGS.size()) {
            index = 0;
        }
        return new Route(v.kind(), v.window(), OFFERINGS.get(index),
                Math.max(1, (int) Math.round(epi.get(AMOUNT))),
                (int) Math.round(epi.get(DURATION)),
                (int) Math.round(epi.get(COOLDOWN)));
    }

    /** Does this combination offer any way in at all? */
    public boolean expresses(AllelePair pair) {
        return !pair.homozygousFor(n);
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Passification"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }
    @Override public EpiSchema epiSchema() { return SCHEMA; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(n)) {
            return wild;
        }
        boolean first = !pair.first().equals(n);
        boolean second = !pair.second().equals(n);
        if (first && second && !pair.first().equals(pair.second())) {
            return both;
        }
        return byVariant[(first ? pair.first() : pair.second()).order()];
    }
}
