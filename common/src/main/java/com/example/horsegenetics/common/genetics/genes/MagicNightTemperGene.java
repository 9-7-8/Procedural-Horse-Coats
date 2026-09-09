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
 * <b>Magic night temper</b> ({@code horsegenetics.magic_night_temper}) - what
 * the horse becomes after dark.
 *
 * <p>Eight variants in two families of four. One family <b>hunts</b> and the
 * other <b>runs</b>, and each names who it feels that about: players, passive
 * animals, hostile mobs, or everything. By day every one of them is an ordinary
 * horse.
 *
 * <table>
 *   <tr><th>combination</th><th>after dark</th></tr>
 *   <tr><td>{@code Agp/Agp}</td><td>goes for <b>players</b></td></tr>
 *   <tr><td>{@code Agc/Agc}</td><td>goes for <b>passive animals</b></td></tr>
 *   <tr><td>{@code Agh/Agh}</td><td>goes for <b>hostile mobs</b></td></tr>
 *   <tr><td>{@code Aga/Aga}</td><td>goes for <b>everything</b></td></tr>
 *   <tr><td>{@code Flp/Flp}</td><td>flees <b>players</b></td></tr>
 *   <tr><td>{@code Flc/Flc}</td><td>flees <b>passive animals</b></td></tr>
 *   <tr><td>{@code Flh/Flh}</td><td>flees <b>hostile mobs</b></td></tr>
 *   <tr><td>{@code Fla/Fla}</td><td>flees <b>everything</b></td></tr>
 *   <tr><td>anything else</td><td>wild type - an ordinary horse, day or night</td></tr>
 * </table>
 *
 * <h2>Every feral horse carries one, and none of them shows it</h2>
 * The founder table is <b>entirely heterozygous</b>: the eight variants split
 * {@value #WILD_EACH_PERCENT}% of the wild population between them and there is
 * no {@code n/n} row at all. So a caught horse always carries exactly one of
 * these and never expresses it, and the wild-type combination - the plainest
 * outcome the locus has - is the one that <i>cannot</i> be caught. It only
 * appears a generation down, in a quarter of the foals of two carriers.
 *
 * <p>That is unlike anything else in the registry, and it is the owner's design
 * rather than a consequence of one. The other loci with invisible carriers put
 * wild horses on the combinations that <i>show</i>, so that what you catch is
 * what you watched it do. Here nothing is meant to be watchable: the whole point
 * is that any horse in the world might be hiding one of eight night behaviours,
 * and the only way to find out which is to breed it and wait for dark.
 *
 * <h2>It talks over the watching locus</h2>
 * {@link MagicNightWatchGene} makes a horse follow and stare at a player, and
 * this gene <b>supersedes it</b> whenever it actually has something to act on -
 * see {@code neoforge/server/NightBehaviourHandler}. A horse that both stalks
 * you and flees from you is not two behaviours, it is a bug; so the temper wins
 * while a target is in range, and the watching resumes when the field is empty.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, so the locus is
 * out of the texture key and the genotype gallery collapses it to one entry.
 */
public final class MagicNightTemperGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_night_temper";
    public static final int PRIORITY = 136;

    /** How far it notices, in blocks. */
    public static final double RADIUS = 16.0;

    /** Ticks between scans. A second - often enough to feel alive, cheap enough for every horse. */
    public static final int INTERVAL_TICKS = 20;

    /** Most entities one scan may consider. Every radius effect here states a cap. */
    public static final int MAX_TARGETS = 8;

    /**
     * Share of the wild population each variant takes. Eight of them, and they
     * add to <b>one hundred</b> - there is no wild-type row. See the class note.
     */
    public static final double WILD_EACH_PERCENT = 100.0 / 8;

    /** One variant: its allele, what it does, and who it does it to. */
    private record Variant(String token, String label, String mood, String towards,
                           String name, String description) {
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant("Agp", "Night-hunter, riders (Agp)", "aggressive", "players",
                    "Hunts riders after dark",
                    "Two copies. From dusk the horse goes for any player it can find within "
                            + "about " + (int) RADIUS + " blocks, and at dawn it is an ordinary "
                            + "horse again with no memory of it. Tamed makes no difference; this "
                            + "is not temper, it is what the animal is at night."),
            new Variant("Agc", "Night-hunter, herds (Agc)", "aggressive", "passive",
                    "Hunts animals after dark",
                    "Two copies. After dark it goes for the passive animals around it - the "
                            + "cows, the sheep, the other horses. A stable full of these is a "
                            + "problem that only appears overnight."),
            new Variant("Agh", "Night-hunter, monsters (Agh)", "aggressive", "hostile",
                    "Hunts monsters after dark",
                    "Two copies. After dark it goes for the monsters instead - the one variant "
                            + "on this locus a person might actually want standing in their "
                            + "yard, and it will get itself killed doing it unless it was bred "
                            + "for the fight as well."),
            new Variant("Aga", "Night-hunter, everything (Aga)", "aggressive", "all",
                    "Hunts everything after dark",
                    "Two copies. Everything within reach after dark, of any kind, including its "
                            + "own owner. There is no version of this that is safe to keep "
                            + "indoors with anything else."),
            new Variant("Flp", "Night-shy, riders (Flp)", "flee", "players",
                    "Flees riders after dark",
                    "Two copies. After dark it will not let a player near it - it breaks away "
                            + "and keeps going. By day it is perfectly ordinary and can be "
                            + "caught, tamed and ridden, which is what makes this one genuinely "
                            + "difficult to own rather than merely inconvenient."),
            new Variant("Flc", "Night-shy, herds (Flc)", "flee", "passive",
                    "Flees animals after dark",
                    "Two copies. After dark it will not stay near other animals, and drifts out "
                            + "of any herd it was in by morning."),
            new Variant("Flh", "Night-shy, monsters (Flh)", "flee", "hostile",
                    "Flees monsters after dark",
                    "Two copies. It runs from monsters after dark rather than standing to be "
                            + "hit - the most sensible thing on this locus, and the only one "
                            + "that reliably keeps the horse alive."),
            new Variant("Fla", "Night-shy, everything (Fla)", "flee", "all",
                    "Flees everything after dark",
                    "Two copies. It runs from anything that moves, all night, every night. "
                            + "Whatever it was bred for, it will not be where it was left."));

    private final List<Allele> alleles;
    private final Allele n;
    private final List<Expression> expressions;
    private final Expression WILD;
    private final List<Expression> byVariant = new ArrayList<>();
    private final FounderTable founders;

    public MagicNightTemperGene() {
        List<Allele> built = new ArrayList<>();
        for (int i = 0; i < VARIANTS.size(); i++) {
            Variant v = VARIANTS.get(i);
            built.add(new Allele(KEY, i, v.token(), v.label()));
        }
        this.n = new Allele(KEY, VARIANTS.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        this.WILD = Expression.wildType(
                "An ordinary horse, day and night. This is every combination but a matched "
                        + "pair - including a horse carrying two DIFFERENT variants, which shows "
                        + "neither, because \"hunt everything\" and \"run from everything\" "
                        + "cannot both be what an animal does.");
        List<Expression> all = new ArrayList<>();
        all.add(WILD);
        for (Variant v : VARIANTS) {
            Expression e = Expression.wildType(v.mood() + "-" + v.towards(), v.name(), v.description());
            byVariant.add(e);
            all.add(e);
        }
        this.expressions = List.copyOf(all);

        FounderTable.Builder table = FounderTable.builder();
        for (Allele a : alleles) {
            if (!a.equals(n)) {
                table.weight(a, n, WILD_EACH_PERCENT);
            }
        }
        this.founders = table.build();
    }

    /** The variant allele this horse is homozygous for, or {@code -1}. */
    private int expressedIndex(AllelePair pair) {
        for (int i = 0; i < VARIANTS.size(); i++) {
            if (pair.homozygousFor(alleles.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic night temper"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int i = expressedIndex(pair);
        return i < 0 ? WILD : byVariant.get(i);
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        int i = expressedIndex(pair);
        if (i < 0) {
            return List.of();
        }
        Variant v = VARIANTS.get(i);
        return List.of(new GeneAbility.NightTemper(v.mood(), v.towards(), RADIUS,
                INTERVAL_TICKS, MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }
}
