package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HealthContribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stock of a dealer who sells magic rather than a breed - what goes on one
 * of their horses, and what stops two of them being the same animal.
 *
 * <p>Every horse gets <b>one showing gene from each</b> of
 * {@link #REQUIRED_FAMILIES}, plus a {@link #OPTIONAL_FAMILY} on
 * {@link #OPTIONAL_CHANCE}. Showing, not carried: the point of the man is that a
 * player can see what they are buying, so nothing here is ever a hidden carrier.
 * Everything else on the horse stays wild type, which is what keeps eleven
 * magical genes legible rather than a smear.
 *
 * <p><b>The pools are generated, never listed.</b> A family's pool is
 * {@link Genes#magicalOrder()} filtered by {@link GeneFamily#of}, so a gene
 * added by a data pack or another mod is in his string the moment it registers,
 * with no edit here. The only hard-coded thing in this class is which
 * <i>families</i> a horse must cover, and a family is an enum constant rather
 * than a gene.
 *
 * <p>The reason he exists is coverage. Magical genes are rare on purpose - most
 * sit near a 0.1% founder weight, so a player can play for a very long time
 * without meeting one, and the wild population is not a realistic way to collect
 * them. His herd is: a dozen-odd showing genes per horse, no two the same combo,
 * restocked slowly and rotated. That makes "go and buy the locus you are
 * missing" a thing a player can actually do, which is what §3 of the philosophy
 * asks for and what nothing before this delivered.
 *
 * @see MagicalVariant the wild-herd version - one gene, one pair, a whole herd
 */
public final class ArcaneStock {

    /**
     * One showing gene from each of these on every horse. Listed in the order
     * the owner asked for them, which is for reading; the order they are
     * actually <b>drawn</b> in is computed per horse by {@link #drawOrder}.
     */
    public static final List<GeneFamily> REQUIRED_FAMILIES = List.of(
            GeneFamily.MAGIC_CORE,
            GeneFamily.MAGIC_YIELD,
            GeneFamily.MAGIC_BEHAVIOUR,
            GeneFamily.MAGIC_EMISSION,
            GeneFamily.MAGIC_BODY,
            GeneFamily.MAGIC_GROUND,
            GeneFamily.MAGIC_FIELDS,
            GeneFamily.MAGIC_SPOTS,
            GeneFamily.MAGIC_SPECKLE,
            GeneFamily.MAGIC_LINES,
            GeneFamily.MAGIC_HAIR);

    /** The one family a horse only sometimes carries. */
    public static final GeneFamily OPTIONAL_FAMILY = GeneFamily.MAGIC_MODIFIERS;

    /** How often {@link #OPTIONAL_FAMILY} lands. */
    public static final float OPTIONAL_CHANCE = 0.25F;

    /**
     * <b>The genes that make a noise</b>, which this dealer does not stock.
     * (Owner, 2026-09-18, after standing in a paddock with five of them.)
     *
     * <p>His string is up to ten horses in one pen, all of them carrying eleven
     * magical loci. A gene that is a pleasant surprise on one horse is a paddock
     * full of cat noises when every animal in earshot has one, and the player
     * cannot get away from it while they are shopping. Nothing else about these
     * genes is wrong - they are all still reachable by breeding, and still turn
     * up in wild magical herds.
     *
     * <p>Named one by one rather than detected, because there is nothing on a
     * gene that says "this is audible": the four {@code Sound}-verb genes are
     * found through their ability list, and {@code ender_echo}'s noise is a side
     * effect of the teleport rather than a declared sound at all. <b>A
     * third-party noisy gene will not be caught by this</b>, and the honest fix
     * for that is a flag on the gene rather than a longer list here.
     *
     * <p>Cadence, for anyone minded to let one back in: {@code meowing} is the
     * worst (a cat every fifteen seconds, unconditional), {@code singer} plays
     * the opening of a music disc every minute, and {@code base_alarm} is
     * dominant and shouts whenever anything hostile is near. {@code echolocate}
     * is silent until the horse is tamed and ridden, and {@code ender_echo} only
     * fires when the horse is hurt - those two are the mildest of the five and
     * the first to reconsider.
     *
     * <p>Not listed, because they make no sound themselves: {@code spawner},
     * {@code pack_leader} and the bait expression of {@code magic_mob_aura} are
     * quiet genes that gather noisy <i>mobs</i>. That is a different complaint,
     * and a different fix if it turns out to be one.
     */
    public static final Set<String> NOISY = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "horsegenetics.meowing",
            "horsegenetics.singer",
            "horsegenetics.base_alarm",
            "horsegenetics.echolocate",
            "horsegenetics.ender_echo")));

    private ArcaneStock() {
    }

    /**
     * Whether this dealer may put {@code gene} on a horse.
     *
     * <p>The same four exclusions {@link MagicalVariant} makes, for the same
     * reasons, minus the breed sheet - his horses have no breed to consult:
     * <ul>
     *   <li>{@link BreedFounder#BODY_STAT_KEYS} are drawn from the stat bands,
     *       so forcing a pair here would fight {@code stampStatTargets}.</li>
     *   <li>{@code feralOnly} is by definition not a thing a dealer stocks.</li>
     *   <li>A sex-linked pair is not legal on both sexes, and the sex locus is
     *       settled after this - see {@code enforceSexLinkage}.</li>
     *   <li>A {@link HealthContribution} gene is a disorder, not a feature, and
     *       selling one deliberately is a different product.</li>
     * </ul>
     */
    public static boolean eligible(Gene gene) {
        if (gene == null || gene.isNatural()) {
            return false;
        }
        if (BreedFounder.BODY_STAT_KEYS.contains(gene.key())
                || NOISY.contains(gene.key())
                || gene.feralOnly()
                || gene.inheritance().sexLinked()
                || gene instanceof HealthContribution) {
            return false;
        }
        return !MagicalVariant.showingPairs(gene).isEmpty();
    }

    /**
     * Every showing pair this dealer may sell in {@code family}, in registry
     * order - so the pool is the same list on the server, in a test and in the
     * browser, which is {@code §2} of the philosophy applied to a shop.
     */
    public static List<AllelePair> pool(GeneFamily family) {
        List<AllelePair> out = new ArrayList<>();
        for (Gene gene : Genes.magicalOrder()) {
            if (GeneFamily.of(gene) == family && eligible(gene)) {
                out.addAll(MagicalVariant.showingPairs(gene));
            }
        }
        return out;
    }

    /**
     * The identity of one allele combination, for the no-two-alike rule:
     * {@code horsegenetics.galaxy=Gxy/Gxy}.
     *
     * <p>Keyed on the <b>pair</b> and not the gene, which is the owner's call:
     * two horses may both show galaxy if one is homozygous and the other is not,
     * because those are two different animals to breed from and a player
     * collecting loci wants both.
     */
    public static String token(AllelePair pair) {
        return pair.geneKey() + "=" + pair.toTokens();
    }

    /**
     * Every showing combination a finished horse is using, as {@link #token}s.
     *
     * <p>Read off the genotype rather than remembered from the roll, so a stray
     * magical gene that came out of the base founder draw counts against the
     * herd's uniqueness too. That also makes the rule self-healing: a retired
     * horse takes its combos with it and the next one may have them.
     */
    public static Set<String> showingTokens(Genotype genotype) {
        Set<String> out = new LinkedHashSet<>();
        if (genotype == null) {
            return out;
        }
        for (Gene gene : Genes.magicalOrder()) {
            if (!eligible(gene)) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (pair != null && MagicalVariant.showingPairs(gene).contains(pair)) {
                out.add(token(pair));
            }
        }
        return out;
    }

    /**
     * One horse's worth of forced pairs: a showing combination from each
     * {@link #REQUIRED_FAMILIES}, none of them in {@code taken}, plus an
     * {@link #OPTIONAL_FAMILY} pair on {@link #OPTIONAL_CHANCE}.
     *
     * <p><b>{@code taken} is read and written.</b> The caller passes the combos
     * the rest of the herd is already using and gets them back with this
     * horse's added, which is what keeps the herd distinct without any of this
     * having to know what a herd is.
     *
     * <p>A family with nothing unused left is <b>skipped, not retried</b>: the
     * shipped registry has room to spare (the tightest family holds comfortably
     * more pairs than the biggest herd), but a pack that strips a family down to
     * two genes should give a dealer with one gene missing rather than a server
     * that hangs looking for an eleventh unique hair pair.
     */
    public static List<AllelePair> rollHorse(Rng rng, Set<String> taken) {
        List<AllelePair> out = new ArrayList<>();
        for (GeneFamily family : drawOrder(taken)) {
            takeOne(family, rng, taken, out);
        }
        if (rng.nextFloat() < OPTIONAL_CHANCE) {
            takeOne(OPTIONAL_FAMILY, rng, taken, out);
        }
        return out;
    }

    /**
     * {@link #REQUIRED_FAMILIES} with the <b>scarcest first</b>: fewest unused
     * pairs left, ties broken by declaration order so the result is a pure
     * function of {@code taken}.
     *
     * <p>Order matters because the families are wildly different sizes - the
     * shipped registry has one family with three times another's stock - and a
     * cramped family drawn last is the one that runs dry and gets skipped.
     * Drawing it first costs nothing and means the horse that misses out is the
     * one from the family with plenty.
     *
     * <p>Computed per horse rather than written down, which is the same reason
     * the pools are: someone drops in a folder of mane genes and this re-sorts
     * itself. A hand-written order would have been a list to maintain, and the
     * point of the whole class is not maintaining lists of genes.
     */
    public static List<GeneFamily> drawOrder(Set<String> taken) {
        List<GeneFamily> order = new ArrayList<>(REQUIRED_FAMILIES);
        final Map<GeneFamily, Integer> free = new HashMap<>();
        for (GeneFamily family : order) {
            free.put(family, freeCount(family, taken));
        }
        order.sort(new Comparator<GeneFamily>() {
            @Override
            public int compare(GeneFamily a, GeneFamily b) {
                int byFree = free.get(a).compareTo(free.get(b));
                return byFree != 0 ? byFree
                        : Integer.compare(REQUIRED_FAMILIES.indexOf(a), REQUIRED_FAMILIES.indexOf(b));
            }
        });
        return order;
    }

    private static int freeCount(GeneFamily family, Set<String> taken) {
        int n = 0;
        for (AllelePair pair : pool(family)) {
            if (!taken.contains(token(pair))) {
                n++;
            }
        }
        return n;
    }

    private static void takeOne(GeneFamily family, Rng rng, Set<String> taken, List<AllelePair> out) {
        List<AllelePair> free = new ArrayList<>();
        for (AllelePair pair : pool(family)) {
            if (!taken.contains(token(pair))) {
                free.add(pair);
            }
        }
        if (free.isEmpty()) {
            return;
        }
        AllelePair picked = free.get(rng.nextInt(free.size()));
        taken.add(token(picked));
        out.add(picked);
    }
}
