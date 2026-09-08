package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>Particle</b> ({@code horsegenetics.particle}) - a <b>magical recessive</b>,
 * and by a wide margin the largest locus in the mod: <b>forty variant alleles</b>
 * plus the wild type, 861 combinations, 87 outcomes. Roughly one wild horse in
 * thirteen trails <i>something</i> as it moves; any one particular thing is
 * about one horse in six hundred.
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
 * <h2>One copy of {@code n} and the locus is silent</h2>
 * <b>The whole locus is recessive to its own wild type.</b> {@code Rflm/n} is a
 * horse that trails nothing whatsoever and is indistinguishable from
 * {@code n/n}; two copies of a variant are what it takes to see anything at all.
 * That is the owner's call and it is the one rule everything else here follows
 * from - a particle is a thing you <i>breed for</i>, not a thing a single lucky
 * allele hands you, and a locus this wide would otherwise put something on far
 * too many horses.
 *
 * <p><b>Rank only decides between two variants.</b> Every allele carries one,
 * and the alleles are declared in rank order, so {@link AllelePair}'s canonical
 * form puts the more dominant copy in slot 0. Where two <i>variant</i> alleles
 * meet and are not codominant the lower rank is the one you see; the other is
 * carried silently and passed on. That is what makes the locus breedable rather
 * than merely wide - a horse trailing dust may be hiding a soul, and only its
 * foals will say so. Rank never beats the wild type: {@code n} is not the
 * weakest allele here, it is an off switch.
 *
 * <p><b>Codominance is by family.</b> Every allele belongs to at most one group
 * ({@link Variant#group()}), and two <i>different</i> alleles of the same group
 * both show at once, in two different colours from two different places on the
 * horse. The groups are the ones the particles themselves suggest - the flames
 * and the smokes are one family of eight, so any two of them stack; dusts,
 * swirls, sparks, portals, sculk, rain, and the cherry/heart/soul trio are the
 * rest. Twenty-nine of the forty alleles sit in a group, which yields <b>46
 * double outcomes</b> on top of the 40 single ones, and every one of them is a
 * combination a wild horse can be caught in - see {@link #foundersTable()}.
 *
 * <h2>Everything visible about it is epigenetic</h2>
 * The allele names a particle and nothing else. Its <b>colour</b>, its
 * <b>second colour</b>, <b>where on the horse</b> it comes from - head, spine,
 * all four hooves, the front pair, the back pair, the tail - <b>how much</b> of
 * it there is, and one spare number for whatever else the particle takes, are
 * all drawn from the epigenetic seed of the allele copy that carries it. So two
 * horses that are both {@code Rflm/Rflm} are not the same horse, and a foal that
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
     * How many founders in a hundred carry <b>each one</b> of the forty
     * homozygous variants - the same for all of them, because no particle is the
     * ordinary one.
     *
     * <p>Forty of these is {@code 40 x 0.15 = 6%} of the wild population
     * trailing one thing, and any <i>named</i> particle is roughly one horse in
     * six hundred and sixty. Meeting a particle horse is a good day; meeting the
     * one you were looking for is a find.
     */
    public static final double WILD_HOMOZYGOUS_PERCENT = 0.15;

    /**
     * How many founders in a hundred carry <b>each one</b> of the forty-six
     * codominant pairs - the two-particle horses. Rarer per combination than a
     * single, so a double is still a thing you mostly breed, but no longer a
     * thing the wild population flatly cannot contain: a pair from one family is
     * a <i>valid</i> heterozygote and the founder table says so.
     */
    public static final double WILD_CODOMINANT_PERCENT = 0.04;

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
        founders = foundersTable();
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

    /**
     * <b>Every combination a wild horse can be caught in is one that shows.</b>
     * The table lists the forty homozygotes, the forty-six codominant pairs, and
     * nothing else - no {@code X/n} carrier, and no cross-family
     * {@code Dst/Bflm} whose dust is quietly sitting on a soul flame.
     *
     * <p>This is not the random-mating shape, and it is not meant to be. A
     * locus that only expresses when both copies agree makes the carrier
     * <i>invisible</i>, and a founder population full of invisible carriers is a
     * population where the whole locus is a lottery run in the dark: you cannot
     * see what you have, so you cannot choose what to pair. Putting the wild
     * horses on the expressing combinations puts the alleles where a breeder can
     * find them - what you catch is what you saw it do - and the carriers then
     * appear where they belong, one generation down, in the foals of a horse
     * bred to a plain one.
     *
     * <p>The cross-family heterozygotes are excluded for the same reason and not
     * because they are impossible: {@code Dst/Bflm} is a perfectly legal horse
     * and breeding will produce one. It is just not a horse the wild ever hands
     * you, because a founder carrying a soul flame nobody can see is the case
     * this table exists to remove.
     */
    private FounderTable foundersTable() {
        FounderTable.Builder b = FounderTable.builder();
        double emitting = 0.0;
        for (Variant v : variants) {
            b.weight(v.allele(), WILD_HOMOZYGOUS_PERCENT);
            emitting += WILD_HOMOZYGOUS_PERCENT;
        }
        for (Variant a : variants) {
            for (Variant c : variants) {
                if (a.allele().order() < c.allele().order() && a.codominantWith(c)) {
                    b.weight(a.allele(), c.allele(), WILD_CODOMINANT_PERCENT);
                    emitting += WILD_CODOMINANT_PERCENT;
                }
            }
        }
        return b.weight(n, 100.0 - emitting).build();
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
        if (b == n.order()) {
            // The wild type sorts last, so second == n means at least one copy
            // is wild - and one copy of n silences the whole locus.
            return wild;
        }
        if (a == b) {
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
        if (b == n.order()) {
            return List.of();   // a single wild-type copy silences the locus
        }
        Variant first = variants.get(a);
        if (a == b) {
            return List.of(first);
        }
        Variant second = variants.get(b);
        return first.codominantWith(second) ? List.of(first, second) : List.of(first);
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics) {
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
     * What one copy's trail looks like: two colours, where on the horse it comes
     * from, how much of it there is, and one spare number the particle type may
     * read.
     *
     * <p>The site and the count are <b>categories</b>: they are picks from a
     * list, not magnitudes, so drift never nudges them a step - it either leaves
     * them alone or, very rarely, re-picks one outright. A horse's particles do
     * not wander from its mane to its tail by accident.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.category("site", SITES.size()),
                        EpiValue.category("count", MAX_COUNT),
                        EpiValue.uniform("data", 0, 1))
                .and(EpiValue.colour("color", 0.60, 1.00, 0.62, 1.00))
                .and(EpiValue.colour("color2", 0.60, 1.00, 0.62, 1.00));
    }

    /**
     * One copy's trail, read off that copy's stored values. Every value is
     * stored whether or not this particular particle uses it, so a variant that
     * starts or stops caring about its second colour costs nothing and moves
     * nothing.
     */
    private GeneAbility.Emitter emitter(Variant v, EpiValues epi) {
        int color = epi.rgb("color");
        int color2 = epi.rgb("color2");
        String site = SITES.get(epi.category("site"));
        int count = 1 + epi.category("count");
        double data = epi.get("data");
        return new GeneAbility.Emitter("particle", "trail", site, new GeneAbility.Trigger.OnMove(),
                color, color2, count, data, v.particle(), EMIT_CHANCE, 0,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
