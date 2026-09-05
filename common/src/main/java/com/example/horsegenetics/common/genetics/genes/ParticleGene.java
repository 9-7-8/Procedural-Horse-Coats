package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>Particle</b> ({@code horsegenetics.particle}) - a <b>magical</b> gene, and
 * by a wide margin the largest locus in the mod: <b>forty variant alleles</b>
 * plus the wild type, 861 combinations, 87 outcomes. Roughly one wild horse in
 * thirteen trails <i>something</i> as it moves; any one particular thing is
 * about one horse in five hundred.
 *
 * <h2>Why one locus and not forty genes</h2>
 * The alternative was forty two-allele genes, and it is worth being explicit
 * about why that is wrong rather than merely large. Forty independent loci mean
 * a horse can carry all forty at once, so the interesting question - <i>which</i>
 * of these does this horse trail - stops having an answer, and every serious
 * breeding line converges on a horse emitting everything. One locus with forty
 * alleles says the opposite thing, and says it structurally: a horse has two
 * copies of the chromosome, so it shows <b>at most two</b> particles, ever, and
 * choosing which two is the whole game. It is the argument that folded dominant
 * white and sabino into {@link KitGene}, at forty times the scale.
 *
 * <h2>Dominance, and the pairs that hide something</h2>
 * Every allele carries a <b>rank</b>, and the alleles are declared in rank
 * order, so {@link AllelePair}'s canonical form puts the more dominant copy in
 * slot 0. Where two alleles are not codominant the lower rank is the one you
 * see; the other is carried silently and passed on. That is what makes the locus
 * breedable rather than merely wide - a horse trailing dust may be hiding a
 * soul, and only its foals will say so.
 *
 * <p><b>Codominance is by family.</b> Every allele belongs to at most one group
 * ({@link Variant#group()}), and two <i>different</i> alleles of the same group
 * both show at once, in two different colours from two different places on the
 * horse. The groups are the ones the particles themselves suggest - the flames
 * and the smokes are one family of eight, so any two of them stack; dusts,
 * swirls, sparks, portals, sculk, rain, and the cherry/heart/soul trio are the
 * rest. Twenty-nine of the forty alleles sit in a group, which yields <b>46
 * double outcomes</b> on top of the 40 single ones. None of them can be caught
 * wild in any useful number: a doubled horse is one somebody bred.
 *
 * <h2>Everything visible about it is epigenetic</h2>
 * The allele names a particle and nothing else. Its <b>colour</b>, its
 * <b>second colour</b>, <b>where on the horse</b> it comes from - head, spine,
 * all four hooves, the front pair, the back pair, the tail - <b>how much</b> of
 * it there is, and one spare number for whatever else the particle takes, are
 * all drawn from the epigenetic seed of the allele copy that carries it. So two
 * horses that are both {@code Rflm/n} are not the same horse, and a foal that
 * inherits the copy inherits the exact look. This is the gene
 * {@link EpigeneticAbilityContribution} was written for.
 *
 * <p>Because each copy of a codominant pair carries its own draw, the two halves
 * are genuinely independent: red flames off the front hooves and blue smoke off
 * the tail is one horse, and nobody wrote that combination down.
 *
 * <h2>It paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type} - the trick milk,
 * verdant and magic size already use. {@link #affectsCoat()} is therefore false,
 * the locus is out of the texture key, and {@code GenotypeCatalog} collapses all
 * 861 combinations to a single entry. Forty alleles for no catalogue growth at
 * all.
 */
public final class ParticleGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.particle";

    /** Between magic body size (140) and light (160). It paints nothing, so the number is a slot. */
    public static final int PRIORITY = 150;

    /**
     * How common <b>each</b> of the forty variants is among founders - the same
     * for all of them, because no particle is the ordinary one.
     *
     * <p>It looks tiny and is not. Forty alleles at this frequency put the
     * variant share of the population at {@code 40 x 0.001 = 4%}, so about
     * <b>7.8%</b> of wild horses trail something, while any <i>named</i>
     * particle is roughly one horse in five hundred. That is the split the locus
     * wants: meeting a particle horse is a good day, meeting the one you were
     * looking for is a find, and meeting a codominant double in the wild
     * essentially never happens.
     */
    public static final double WILD_ALLELE_FREQUENCY = 0.001;

    /** Per-copy density, drawn uniformly over {@code [1, MAX_COUNT]}. */
    public static final int MAX_COUNT = 4;

    /** Probability the trail fires on any given moving tick. */
    public static final double EMIT_CHANCE = 0.2;

    /**
     * The body sites a copy can emit from, in draw order. These are
     * {@code anchor} words on {@code AbilityType.EMITTER}; the translator picks
     * the actual point, so {@code hooves} really is all four rather than a spot
     * between them.
     */
    public static final List<String> SITES =
            List.of("head", "spine", "hooves", "front_hooves", "back_hooves", "tail");

    /**
     * One variant allele: the particle it names, the codominance family it
     * belongs to ({@code ""} = none, so it never stacks with anything), and the
     * prose the outcome descriptions are built from.
     */
    public record Variant(Allele allele, String particle, String group, String label, String prose) {

        /** Does this allele show alongside {@code other} rather than hiding it? */
        public boolean codominantWith(Variant other) {
            return this != other && !group.isEmpty() && group.equals(other.group());
        }
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;

    /** Indexed by allele order; {@code null} at the wild type's slot. */
    private final Expression[] singles;

    /** Keyed {@code firstOrder * 64 + secondOrder}, present only for codominant pairs. */
    private final Map<Integer, Expression> duals = new LinkedHashMap<>();

    private final Expression wild = Expression.wildType(
            "The horse leaves the air behind it exactly as it found it.");

    private final FounderTable founders;

    public ParticleGene() {
        // Declared in rank order - most dominant first - because AllelePair
        // canonicalizes on Allele.order(). That is what puts the allele a horse
        // actually shows in slot 0, and lets copy(0) be the copy that matters.
        variant("Dst",     "minecraft:dust",                            "dst",  "Dust",              "a drift of coloured dust");
        variant("Dst2",    "minecraft:dust_color_transition",           "dst",  "Fading dust",       "dust that fades from one colour into another");
        variant("Clrstr",  "minecraft:glow",                            "str",  "Glow motes",        "soft glowing motes");
        variant("Rain",    "minecraft:rain",                            "rain", "Rain splash",       "splashes of rain");
        variant("Smflm",   "minecraft:small_flame",                     "burn", "Small flame",       "small flames");
        variant("Rain2",   "minecraft:fishing",                         "rain", "Water wake",        "the bobbing wake of water");
        variant("Skl",     "minecraft:trial_omen",                      "",     "Trial omen",        "the pale drift of a trial omen");
        variant("Wtsmk",   "minecraft:white_smoke",                     "burn", "White smoke",       "white smoke");
        variant("Smk",     "minecraft:smoke",                           "burn", "Smoke",             "smoke");
        variant("Wtsmk2",  "minecraft:white_ash",                       "burn", "White ash",         "a fall of white ash");
        variant("Prtl2",   "minecraft:reverse_portal",                  "prtl", "Reverse portal",    "portal motes falling inward");
        variant("Bwswrl",  "minecraft:effect",                          "swrl", "Spell swirl",       "a lingering spell swirl");
        variant("Whtswrl", "minecraft:entity_effect",                   "swrl", "Ambient swirl",     "an ambient swirl clinging to the coat");
        variant("Lmstr",   "minecraft:totem_of_undying",                "",     "Totem sparks",      "green totem sparks");
        variant("Rflm",    "minecraft:flame",                           "burn", "Flame",             "flames");
        variant("Bflm",    "minecraft:soul_fire_flame",                 "burn", "Soul flame",        "blue soul flames");
        variant("Chrylf",  "minecraft:cherry_leaves",                   "life", "Cherry petals",     "falling cherry petals");
        variant("Dst3",    "minecraft:dust_plume",                      "dst",  "Dust plume",        "a grey plume of kicked-up dust");
        variant("Lava",    "minecraft:lava",                            "",     "Lava embers",       "spitting embers of lava");
        variant("Smbflm",  "minecraft:copper_fire_flame",               "burn", "Copper flame",      "green copper flames");
        variant("Raid",    "minecraft:raid_omen",                       "",     "Raid omen",         "the dark drift of a raid omen");
        variant("Sklk",    "minecraft:sculk_charge_pop",                "sklk", "Sculk charge",      "bursting sculk charges");
        variant("Sklk2",   "minecraft:vibration",                       "sklk", "Sculk vibration",   "sculk vibrations travelling home");
        variant("Sklk3",   "minecraft:sculk_soul",                      "sklk", "Sculk soul",        "rising sculk souls");
        variant("Shrk",    "minecraft:shriek",                          "",     "Shriek",            "a shrieker's mark");
        variant("Ornspk",  "minecraft:trial_spawner_detection",         "spk",  "Spawner sparks",    "orange spawner sparks");
        variant("Blspk",   "minecraft:trial_spawner_detection_ominous", "spk",  "Ominous sparks",    "blue ominous spawner sparks");
        variant("Snw",     "minecraft:snowflake",                       "",     "Snowflakes",        "drifting snowflakes");
        variant("Whtstr",  "minecraft:electric_spark",                  "str",  "Electric sparks",   "white electric sparks");
        variant("Prtl",    "minecraft:portal",                          "prtl", "Portal motes",      "portal motes drifting outward");
        variant("Ooze",    "minecraft:item_slime",                      "",     "Ooze",              "flecks of slime");
        variant("Note",    "minecraft:note",                            "",     "Notes",             "notes");
        variant("Snc",     "minecraft:sonic_boom",                      "",     "Sonic ring",        "a sonic ring");
        variant("Dstrn",   "minecraft:enchant",                         "",     "Enchanting glyphs", "drifting glyphs");
        variant("Hrt",     "minecraft:heart",                           "life", "Hearts",            "hearts");
        variant("Dig",     "minecraft:block",                           "",     "Sculk debris",      "sculk debris kicked up from the ground");
        variant("Grnstr",  "minecraft:happy_villager",                  "str",  "Growth motes",      "green growth motes");
        variant("Csmk",    "minecraft:campfire_cosy_smoke",             "burn", "Campfire smoke",    "a slow curl of campfire smoke");
        variant("Bkswrl",  "minecraft:instant_effect",                  "swrl", "Evoker swirl",      "a sharp, instant spell swirl");
        variant("Soul",    "minecraft:soul",                            "life", "Souls",             "rising souls");

        n = new Allele(KEY, variants.size(), "n", "Wild-type (n)");

        List<Allele> all = new ArrayList<>(variants.size() + 1);
        for (Variant v : variants) {
            all.add(v.allele());
        }
        all.add(n);
        alleles = List.copyOf(all);

        // 87 outcomes, generated. Hand-writing them would be 87 constants nobody
        // could keep in agreement with the table above.
        List<Expression> out = new ArrayList<>();
        out.add(wild);
        singles = new Expression[alleles.size()];
        for (Variant v : variants) {
            Expression e = Expression.wildType(idOf(v), v.label(),
                    "The horse trails " + v.prose() + ". The colour, where on its body it comes "
                            + "from and how much of it there is are written on the allele copy, so "
                            + "no two horses carrying " + v.allele().token() + " need look alike.");
            singles[v.allele().order()] = e;
            out.add(e);
        }
        for (Variant a : variants) {
            for (Variant b : variants) {
                if (a.allele().order() >= b.allele().order() || !a.codominantWith(b)) {
                    continue;
                }
                Expression e = Expression.wildType(idOf(a) + "-" + b.allele().token().toLowerCase(Locale.ROOT),
                        a.label() + " and " + b.label(),
                        "One copy of each, and both show at once: " + a.prose() + " and " + b.prose()
                                + ". The two are drawn independently, so they need share neither a "
                                + "colour nor a place on the horse.");
                duals.put(key(a.allele().order(), b.allele().order()), e);
                out.add(e);
            }
        }
        expressions = List.copyOf(out);
        founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);
    }

    private void variant(String token, String particle, String group, String label, String prose) {
        Allele a = new Allele(KEY, variants.size(), token, label + " (" + token + ")");
        variants.add(new Variant(a, particle, group, label, prose));
    }

    private static String idOf(Variant v) {
        return "p-" + v.allele().token().toLowerCase(Locale.ROOT);
    }

    private static int key(int a, int b) {
        return a * 64 + b;
    }

    /** Baseline last, and a {@link LinkedHashMap} - see {@link MilkGene}. */
    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        for (Variant v : variants) {
            p.put(v.allele(), WILD_ALLELE_FREQUENCY);
        }
        p.put(n, 1.0 - WILD_ALLELE_FREQUENCY * variants.size());
        return p;
    }

    // ------------------------------------------------------------------

    @Override public String key() { return KEY; }
    @Override public String name() { return "Particle"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** Every variant this gene defines, in rank order - for the wiki and the tests. */
    public List<Variant> variants() {
        return List.copyOf(variants);
    }

    /** The wild type. */
    public Allele wildTypeAllele() {
        return n;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return wild; // the wild type sorts last, so first == n means both are
        }
        if (b == n.order() || a == b) {
            return singles[a];
        }
        Expression both = duals.get(key(a, b));
        // Not codominant: the lower rank is the one that shows, and slot 0 is it.
        return both != null ? both : singles[a];
    }

    /**
     * The variant copies this combination actually shows, in slot order - empty,
     * one, or (for a codominant pair) two. This is what {@link #abilitiesFor}
     * walks, and what an info panel should read.
     */
    public List<Variant> shown(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return List.of();
        }
        Variant first = variants.get(a);
        if (b == n.order() || a == b) {
            return List.of(first);
        }
        Variant second = variants.get(b);
        return first.codominantWith(second) ? List.of(first, second) : List.of(first);
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, AlleleRandomness epigenetics) {
        List<Variant> shown = shown(pair);
        if (shown.isEmpty()) {
            return List.of();
        }
        List<GeneAbility> out = new ArrayList<>(shown.size());
        for (int slot = 0; slot < shown.size(); slot++) {
            // copy(slot), not expressed(): a codominant pair has two copies to
            // read, and the expressed one would be one of them twice.
            out.add(emitter(shown.get(slot), epigenetics.copy(slot)));
        }
        return List.copyOf(out);
    }

    /**
     * One copy's trail. <b>Every value is drawn, in this order, every time</b>,
     * whether or not the particle uses it - the draw order is the contract, so a
     * particle that starts or stops caring about its second colour must not
     * shift the numbers drawn after it.
     */
    private GeneAbility.Emitter emitter(Variant v, Rng rng) {
        int color = HairPattern.randomBrightColour(rng);
        int color2 = HairPattern.randomBrightColour(rng);
        String site = SITES.get(rng.nextInt(SITES.size()));
        int count = 1 + rng.nextInt(MAX_COUNT);
        double data = rng.nextFloat();
        return new GeneAbility.Emitter("particle", "trail", site, new GeneAbility.Trigger.OnMove(),
                color, color2, count, data, v.particle(), EMIT_CHANCE,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
