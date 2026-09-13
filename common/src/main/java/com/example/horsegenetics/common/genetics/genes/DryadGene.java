package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Dryad</b> ({@code horsegenetics.dryad}) - a <b>magical</b> gene that paints
 * nothing. Every so often the horse leaves something growing where it has been,
 * and <i>what</i> it leaves is written on the allele.
 *
 * <h2>The locus, in one table</h2>
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - the ground is left alone</td></tr>
 *   <tr><td>one variant and {@code n}</td><td>carrier; nothing happens</td></tr>
 *   <tr><td>two of the same variant</td><td>that one thing, at full rate</td></tr>
 *   <tr><td>two <i>different</i> variants</td><td><b>both</b>, each at half rate</td></tr>
 * </table>
 *
 * <h2>Incomplete dominance, and why the last row is half and not double</h2>
 * The owner's rule (2026-09-13): <i>"make everything incomplete dominant as long
 * as the other allele isn't wildtype"</i>. So a variant is still recessive to the
 * wild type - one copy does nothing, exactly as before - but two <b>different</b>
 * variants no longer cancel each other out the way {@link VerdantGene}'s do.
 * They both express.
 *
 * <p><b>Each at half rate, so the total is the same as a homozygote's.</b> That
 * is what makes this <i>incomplete</i> dominance rather than simple codominance,
 * and it is the difference between a design and an exploit: if a mismatch
 * planted two things at full speed, every breeder would keep their lines
 * deliberately mixed and a matched pair would be strictly worse than the thing
 * you get by accident. Halving makes the choice real - the homozygote
 * <b>specialises</b> (and is the only way to a reliable dark-oak grove), the
 * heterozygote <b>diversifies</b>. Neither is better; they are for different
 * things.
 *
 * <p>The arithmetic needs no special case, which is the tell that the model was
 * already shaped for this: the interval is epigenetic and lives on the
 * <i>copy</i>, so a heterozygote reads {@code copy(0)} and {@code copy(1)}
 * separately and doubles each. {@link GeneEpigenetics#copy(int)}'s own
 * documentation says it exists for "a codominant gene, where both copies
 * contribute at once".
 *
 * <h2>What it plants, and the rule every one of them obeys</h2>
 * Six trees, plus three things that are not trees at all. The gene names a
 * <b>cover</b> and the translator decides what that means - see
 * {@code wiki/making-a-gene.html}'s {@code spread} verb - and every placing
 * cover now has to answer {@code canSurvive} before it is written. That is the
 * whole of the owner's first request: <i>never plant any kind of sapling which
 * can't grow</i>. A mushroom in daylight and a flower on stone are the same bug
 * as the dark oak was, and one guard in the translator covers all three.
 *
 * <h2>The dark oak, which is why the alleles are named at all</h2>
 * Until 2026-09-13 this gene picked its species from {@code pos.hashCode()}, and
 * one of the six was dark oak - which vanilla will only grow from a
 * 2&nbsp;&times;&nbsp;2 block of saplings. A gene that plants one sapling at a
 * time, at random, every few thousand ticks, will essentially never make a
 * square: measured overnight, a dark oak planted at 01:28 was still a sapling
 * forty minutes later while its oak neighbour had been a tree since 00:01, and
 * one position logged ten failed growth attempts in three quarters of an hour.
 * A sixth of the gene's entire output was permanent litter.
 *
 * <p>Naming the species on the allele fixes it at the root, because a dark-oak
 * horse now plants <i>only</i> dark oak and its saplings accumulate in one
 * place. The translator then <b>clusters</b> them - a dark oak looks for a spot
 * beside one already standing before it falls back to anywhere - so the square
 * completes on its own within a few plantings. "Eventually" was the owner's
 * word for it and it is the right one: it is a slower reward for a rarer horse.
 *
 * <h2>Bone meal, which was refused once</h2>
 * This gene's javadoc used to carry the sentence: <i>"An earlier specification
 * had it bone-mealing the surrounding area. That makes a horse which auto-farms
 * every crop you own, which is an economy lever nobody asked for and very hard
 * to walk back once players have it."</i> That note did its job - it made the
 * reversal deliberate rather than quiet, which is exactly what it was written
 * for. The owner asked for the allele on 2026-09-13.
 *
 * <p><b>The objection is answered rather than overruled</b>: the bone-meal cover
 * refuses <b>crops</b>. It will hurry a sapling, grass, a flower, moss or a
 * mushroom, and it will not touch wheat, carrots, potatoes, beetroot, melon or
 * pumpkin stems. So the gene is a horse that makes a place greener, not a horse
 * that farms - which is the flavour the allele was wanted for and none of the
 * economy the old note was worried about.
 *
 * <h2>A gene you notice a week later</h2>
 * Almost everything in the mod expresses immediately or not at all. This one
 * expresses across sessions: you stable a horse, and later there are trees
 * there. The interval is bounded so breeding can hurry it without turning it
 * into a per-tick effect.
 */
public final class DryadGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.dryad";
    public static final int PRIORITY = 161;

    /** The one value a copy carries: how many ticks between plantings. */
    public static final String INTERVAL = "interval";

    /** A day is 24000 ticks. The range is bounded so breeding can hurry it, not transform it. */
    public static final double MIN_INTERVAL = 9000;
    public static final double MAX_INTERVAL = 32000;

    /** Blocks. Small - a sapling should appear where the horse was, not across the field. */
    public static final double RADIUS = 4.0;

    /**
     * How much a heterozygote's two behaviours are each slowed.
     *
     * <p>Two, so that two halves make a whole: a mixed horse plants as often as
     * a matched one and simply alternates what it leaves. See the class note -
     * this number is the entire difference between a design decision and a
     * reason to never breed a line true.
     */
    public static final int MIXED_SLOWDOWN = 2;

    /**
     * One allele: the token a genotype writes, the cover the translator is
     * handed, and the words a player reads.
     *
     * @param noun what a sentence calls the thing, lower case ("birch saplings")
     */
    private record Variant(Allele allele, String cover, String label, String noun, String blurb) {
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;

    private final Expression wild;
    private final Expression carrier;

    /** Indexed by allele order. */
    private final Expression[] matched;

    /** Indexed by the two allele orders, lower first. */
    private final Expression[][] mixed;

    private final List<Expression> expressions;
    private final FounderTable founders;

    public DryadGene() {
        // ORDER IS THE GENOTYPE'S SORT ORDER and it is load-bearing: AllelePair
        // canonicalizes by it, so copy(0) is always the earlier allele here.
        // Trees first, in vanilla's own order, then the three that are not trees.
        variant("Oak", "sapling_oak", "Oak", "oak saplings",
                "the common one, and the fastest to become a tree");
        variant("Brch", "sapling_birch", "Birch", "birch saplings",
                "tall and thin, and forgiving about where it stands");
        variant("Spru", "sapling_spruce", "Spruce", "spruce saplings",
                "dark and dense; a spruce horse makes a wood you cannot see through");
        variant("Jung", "sapling_jungle", "Jungle", "jungle saplings",
                "the tallest single sapling in the game, and the one that wants room");
        variant("Aca", "sapling_acacia", "Acacia", "acacia saplings",
                "grows crooked, so an acacia line never makes the same shape twice");
        variant("Dark", "sapling_dark_oak", "Dark oak", "dark oak saplings",
                "the only one that needs four of itself in a square - so it plants beside "
                        + "what it has already planted, and a grove takes a few days rather "
                        + "than one");
        variant("Mush", "mushroom", "Mushroom", "mushrooms",
                "red and brown, and only where they will survive: out of the light, or on "
                        + "mycelium and podzol where the light does not matter");
        variant("Flwr", "flower", "Flower", "flowers",
                "whatever the biome would have grown there anyway, one at a time");
        variant("Bone", "bonemeal", "Bone meal", "a hurried-along growing thing",
                "it fertilises instead of planting - saplings, grass, flowers, moss and "
                        + "mushrooms, and pointedly never a crop");

        this.n = new Allele(KEY, variants.size(), "n", "Wild-type (n)");
        List<Allele> all = new ArrayList<>();
        for (Variant v : variants) {
            all.add(v.allele());
        }
        all.add(n);
        this.alleles = List.copyOf(all);

        this.wild = Expression.wildType("The ground the horse walks over stays as it was.");
        this.carrier = Expression.wildType("dryad-carrier", "Dryad carrier",
                "One variant copy and one wild type. Nothing happens - a variant is still "
                        + "recessive to the wild type - and the horse passes it on. Two variants "
                        + "is a different matter, even when they are different variants.");

        List<Expression> out = new ArrayList<>();
        out.add(wild);
        out.add(carrier);
        this.matched = new Expression[alleles.size()];
        for (Variant v : variants) {
            Expression e = Expression.wildType("dryad-" + v.allele().token(),
                    v.label() + " dryad",
                    "Two matching copies. Every so often the horse leaves " + v.noun()
                            + " near where it is standing - " + v.blurb()
                            + ". How often is written on the allele copy, so breeding can hurry "
                            + "it. This is the only way to get one thing reliably.");
            matched[v.allele().order()] = e;
            out.add(e);
        }
        this.mixed = new Expression[alleles.size()][alleles.size()];
        for (int i = 0; i < variants.size(); i++) {
            for (int j = i + 1; j < variants.size(); j++) {
                Variant a = variants.get(i);
                Variant b = variants.get(j);
                Expression e = Expression.wildType(
                        "dryad-" + a.allele().token() + "-" + b.allele().token(),
                        a.label() + " and " + b.label().toLowerCase(),
                        "Two different variants, and <b>both</b> express - each at half its "
                                + "usual rate, so the horse leaves things as often as a matched "
                                + "pair does and alternates between " + a.noun() + " and "
                                + b.noun() + ". It will never make a dark oak grove and it will "
                                + "never make a monoculture either.");
                mixed[i][j] = e;
                out.add(e);
            }
        }
        this.expressions = List.copyOf(out);
        this.founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);
    }

    private void variant(String token, String cover, String label, String noun, String blurb) {
        variants.add(new Variant(new Allele(KEY, variants.size(), token, label + " (" + token + ")"),
                cover, label, noun, blurb));
    }

    /**
     * How common each variant is in the wild.
     *
     * <p><b>Deliberately low, and the reason is the combinatorics rather than
     * the flavour.</b> Nine variants means nine matched pairs and thirty-six
     * mixed ones, so at any given allele frequency a wild dryad is
     * overwhelmingly likely to be a <i>mixed</i> one - which is the right
     * texture (you find horses that leave a bit of everything, and a horse that
     * only plants dark oak is something somebody bred) but it also means the
     * expressing rate climbs fast. {@value #PER_VARIANT} each holds the total
     * near the one per cent this locus has always had.
     */
    private static final double PER_VARIANT = 0.012;

    /** Baseline last, and a {@link LinkedHashMap} - see {@link MilkGene#frequencies()}. */
    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        double total = 0.0;
        for (Variant v : variants) {
            p.put(v.allele(), PER_VARIANT);
            total += PER_VARIANT;
        }
        p.put(n, 1.0 - total);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dryad"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(INTERVAL, MIN_INTERVAL, MAX_INTERVAL));
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        // n sorts last, so first() being the wild type means both are.
        if (a == n.order()) {
            return wild;
        }
        if (b == n.order()) {
            return carrier;
        }
        return a == b ? matched[a] : mixed[a][b];
    }

    /**
     * What this combination leaves behind, for the wiki and the info surfaces:
     * one cover, two covers, or none.
     */
    public List<String> coversOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order() || b == n.order()) {
            return List.of();
        }
        return a == b ? List.of(variants.get(a).cover())
                : List.of(variants.get(a).cover(), variants.get(b).cover());
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order() || b == n.order()) {
            return List.of();   // wild type, or a carrier: a variant is recessive to n
        }
        if (a == b) {
            return List.of(spread(variants.get(a).cover(), interval(epigenetics.copy(0), 1)));
        }
        // Both, each halved - and each reading ITS OWN copy's interval, which is
        // why this needs copy(slot) rather than expressed(). Asking for the
        // expressed copy here would count one allele twice and the other never.
        return List.of(
                spread(variants.get(a).cover(), interval(epigenetics.copy(0), MIXED_SLOWDOWN)),
                spread(variants.get(b).cover(), interval(epigenetics.copy(1), MIXED_SLOWDOWN)));
    }

    private static int interval(EpiValues epi, int slowdown) {
        return (int) Math.round(epi.get(INTERVAL)) * slowdown;
    }

    private static GeneAbility spread(String cover, int intervalTicks) {
        return new GeneAbility.Spread(cover, RADIUS, 1.0, intervalTicks,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
