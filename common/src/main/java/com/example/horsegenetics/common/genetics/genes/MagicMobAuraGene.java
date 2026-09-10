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

import java.util.List;

/**
 * <b>Magic mob aura</b> ({@code horsegenetics.magic_mob_aura}) - how everything
 * else in the world feels about the horse.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Wrd/Wrd}</td><td>{@code ward} - mobs will not come within {@value #WARD_RADIUS} blocks</td></tr>
 *   <tr><td>{@code Bai/Bai}</td><td>{@code bait} - hostile mobs within {@value #BAIT_RADIUS} blocks pick it over anything else</td></tr>
 *   <tr><td>{@code Wrd/Bai}</td><td>wild type - the two cancel outright</td></tr>
 *   <tr><td>anything with an {@code n}</td><td>wild type</td></tr>
 * </table>
 *
 * <h2>The compound heterozygote is the interesting row</h2>
 * {@code Wrd/Bai} produces <b>nothing</b>, and not as a shrug. The two alleles
 * are answers to one question - does a mob want to be near this horse - and a
 * horse carrying one of each is asking for both at once. Rather than pick a
 * winner or let a ward and a bait fight every tick in the translator, the locus
 * says the pair is silent. It is the same shape as
 * {@link MagicOnDeathGene}'s mixed pairs, and it means a breeder who crosses
 * their ward line with their bait line gets an ordinary-looking horse that
 * carries both and throws both.
 *
 * <h2>What each one is for</h2>
 * A ward is the horse you leave a stable with overnight. A bait is the horse
 * you take somewhere on purpose - it pulls aggro off its rider and off
 * everything else nearby, which is either a shield or a very short-lived horse
 * depending on how much {@link MagicHealthGene health} you bred into it. That
 * pairing is deliberate: the bait allele is the one that makes the other
 * magical loci worth stacking.
 *
 * <h2>Both take two copies</h2>
 * Recessive to the wild type and to each other. The founder table lists the two
 * expressing homozygotes and the plain horse, following the standing rule for a
 * magical locus whose carrier shows nothing: a wild population made of silent
 * carriers is a locus run in the dark, so what a player catches is what they
 * watched it do.
 */
public final class MagicMobAuraGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_mob_aura";
    public static final int PRIORITY = 135;

    /** How far a ward pushes mobs back, in blocks. */
    public static final double WARD_RADIUS = 10.0;

    /** How far a bait reaches, in blocks. Shorter than a ward: it has to be walked into. */
    public static final double BAIT_RADIUS = 16.0;

    /** Ticks between beats. A second - often enough to hold a line, cheap enough to run on every horse. */
    public static final int INTERVAL_TICKS = 20;

    /** Most entities one beat may touch. Every radius effect here is required to cap this. */
    public static final int MAX_TARGETS = 12;

    public static final double WILD_WARD_PERCENT = 0.18;
    public static final double WILD_BAIT_PERCENT = 0.12;

    public final Allele Wrd = new Allele(KEY, 0, "Wrd", "Warding (Wrd)");
    public final Allele Bai = new Allele(KEY, 1, "Bai", "Baiting (Bai)");
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Wrd, Bai, n);

    private final Expression WILD = Expression.wildType(
            "Mobs treat the horse the way they treat any horse. This covers every combination "
                    + "with a wild-type copy AND the pair of one warding copy with one baiting "
                    + "copy: those two are opposite answers to the same question, so together "
                    + "they say nothing at all.");

    private final Expression WARD = Expression.wildType("ward", "Warding",
            "Two warding copies. Mobs will not approach within about " + (int) WARD_RADIUS
                    + " blocks - they turn away at the edge rather than being hurt or teleported. "
                    + "A horse worth keeping a stable around.");

    private final Expression BAIT = Expression.wildType("bait", "Baiting",
            "Two baiting copies. Any hostile mob within about " + (int) BAIT_RADIUS
                    + " blocks that can see the horse picks it as its target over anything else "
                    + "in range, including its rider. Whether that is a shield or a very short "
                    + "career depends entirely on what else the horse was bred with.");

    private final List<Expression> expressions = List.of(WILD, WARD, BAIT);

    /** The two expressing homozygotes and the plain horse - no invisible carriers in the wild. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Wrd, Wrd, WILD_WARD_PERCENT)
            .weight(Bai, Bai, WILD_BAIT_PERCENT)
            .weight(n, n, 100.0 - WILD_WARD_PERCENT - WILD_BAIT_PERCENT)
            .build();

    private final List<GeneAbility> ward = one("repel", WARD_RADIUS);
    private final List<GeneAbility> bait = one("attract", BAIT_RADIUS);

    private static List<GeneAbility> one(String mode, double radius) {
        // "hostile" and no specific mob: this locus has always been about monsters,
        // and the group parameter simply makes that explicit rather than implied.
        return List.of(new GeneAbility.MobAura(mode, "hostile", "", radius, INTERVAL_TICKS,
                MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic mob aura"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(Wrd)) {
            return WARD;
        }
        return pair.homozygousFor(Bai) ? BAIT : WILD;
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        if (pair.homozygousFor(Wrd)) {
            return ward;
        }
        return pair.homozygousFor(Bai) ? bait : List.of();
    }
}
