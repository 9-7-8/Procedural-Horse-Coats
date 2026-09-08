package com.example.horsegenetics.common.stable;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>What lives in a generated stable.</b> One of these per stable building -
 * how many horses, where they stand, what breed they are, and what their
 * genotypes are made to guarantee.
 *
 * <h2>Why the rules are data and not code</h2>
 * A stable is a building somebody drew plus a sentence about the horses in it.
 * The building is an NBT and the sentence is this record, so <b>adding one is
 * two files and no Java</b> - which is the whole point, and the reason this
 * lives in {@code common/} with a plain constructor rather than in the NeoForge
 * module behind a codec. The codec is a thin shell over it
 * ({@code data/StableDefinitions}); the rolling is here, where it can be tested
 * without a game.
 *
 * <h2>The guarantees, and the order they are applied in</h2>
 * {@link #roll} starts from an ordinary {@link BreedFounder} draw and then
 * <b>narrows</b> it. Order matters, and it is the order the constraints were
 * asked for:
 * <ol>
 *   <li><b>Breed</b> - one of {@link #breeds}, or the feral mix when empty.</li>
 *   <li><b>Magical policy</b> ({@link #magic}) - the blanket rule for every
 *       magical locus, applied before anything is added back. The four
 *       body-stat loci are exempt throughout: they are how a breed gets its
 *       speed and height, not a magic trait.</li>
 *   <li><b>Rare natural genes</b> - force {@link #rareNaturalGenes} loci of at
 *       least {@link #rareTier} into a state that <i>shows</i>.</li>
 *   <li><b>Homozygous magical traits</b> - {@link #minMagic} of them, plus a
 *       ladder of extras.</li>
 * </ol>
 * Steps 3 and 4 are last because they are the promises a player was made; a
 * breed pool that would have cleared them does not get to.
 *
 * <p><b>Nothing here can produce a horse that is worse off.</b> Every locus it
 * forces is drawn from the same safe pool the random splice carrots use
 * ({@code SpliceSafety}), so a stable cannot hand a player a lethal foal or one
 * short of hearts - which matters more here than it does for a carrot, because
 * a player did not choose to open this box at all.
 */
public record StableSpawn(
        String structure,
        String name,
        int minHorses,
        int maxHorses,
        int fieldHorses,
        List<String> breeds,
        Magic magic,
        int rareNaturalGenes,
        GeneRarity rareTier,
        int minMagic,
        int maxMagic,
        double extraMagicChance,
        double extraMagicFalloff) {

    /** What a stable's horses are allowed to carry at the magical loci. */
    public enum Magic {
        /** Every magical locus forced to its wild type. No glow, no particles, no cutie mark. */
        NONE,
        /**
         * Only the magical genes that <b>paint</b> are cleared. A horse may still
         * heal, glow or trail something; what it may not be is a colour no real
         * horse could be. This is the rule for a stable of show horses.
         */
        NO_COAT,
        /** The breed's own magic chance decides, as it would anywhere else. */
        ALLOWED
    }

    /**
     * The four magical loci that are <b>not</b> "magic" in the sense a stable
     * means it. They are how {@code BreedFounder} hits a breed's speed / health
     * / jump / height bands, so clearing them would quietly delete the breed's
     * character, and forcing one homozygous would be a stat roll dressed up as a
     * glowing horse. Mirrors {@code BreedFounder.BODY_STAT_KEYS}, deliberately -
     * the two lists are the same list for two different reasons, and neither
     * class should be the other's dependency.
     */
    private static final List<String> BODY_STAT_KEYS = List.of(
            "horsegenetics.body_size",
            "horsegenetics.magic_speed",
            "horsegenetics.magic_health",
            "horsegenetics.magic_jump");

    private static boolean isBodyStat(Gene gene) {
        return BODY_STAT_KEYS.contains(gene.key());
    }

    public StableSpawn {
        breeds = breeds == null ? List.of() : List.copyOf(breeds);
        magic = magic == null ? Magic.ALLOWED : magic;
        rareTier = rareTier == null ? GeneRarity.RARE : rareTier;
        minHorses = Math.max(0, minHorses);
        maxHorses = Math.max(minHorses, maxHorses);
        fieldHorses = Math.max(0, fieldHorses);
        rareNaturalGenes = Math.max(0, rareNaturalGenes);
        minMagic = Math.max(0, minMagic);
        maxMagic = Math.max(minMagic, maxMagic);
    }

    /** How many horses this stable holds this time. */
    public int rollCount(Rng rng) {
        return minHorses + (maxHorses > minHorses ? rng.nextInt(maxHorses - minHorses + 1) : 0);
    }

    /**
     * One horse. The sex is drawn here rather than forced, so a generated stable
     * is a stable and not a breeding pair - a player who wants to breed from one
     * may have to find a second.
     */
    public Genome roll(Rng rng) {
        Breed breed = breed(rng);
        Genome genome = BreedFounder.roll(breed, rng, rng.nextBoolean() ? Sex.MALE : Sex.FEMALE);
        Genotype g = genome.genotype();

        g = applyMagicPolicy(g);
        g = addRareNaturals(g, rng);
        g = addHomozygousMagic(g, rng);

        // Re-rolled rather than carried: the epigenome is aligned to the
        // genotype it was rolled for, and three passes of substitutions is not
        // that genotype any more.
        return Genome.of(g, rng);
    }

    /**
     * The breed this horse is, or the feral mix when the stable names none it
     * can use.
     *
     * <p>A named breed that has switched {@link BreedSource#STABLE} off is
     * skipped rather than honoured: a stable definition is written once, by
     * whoever built the building, and a breed's own file is the later and more
     * specific word on where it may turn up.
     */
    public Breed breed(Rng rng) {
        List<Breed> allowed = new ArrayList<>();
        for (String id : breeds) {
            Breed breed = Breeds.get(id);
            if (breed != Breeds.FERAL_MIXED && breed.allows(BreedSource.STABLE)) {
                allowed.add(breed);
            }
        }
        if (allowed.isEmpty()) {
            return Breeds.FERAL_MIXED;
        }
        return allowed.get(rng.nextInt(allowed.size()));
    }

    // ------------------------------------------------------------------
    // The three narrowing passes
    // ------------------------------------------------------------------

    private Genotype applyMagicPolicy(Genotype g) {
        if (magic == Magic.ALLOWED) {
            return g;
        }
        for (Gene gene : Genes.codeOrder()) {
            if (gene.isNatural() || isBodyStat(gene)) {
                continue;
            }
            if (magic == Magic.NONE || gene.affectsCoat()) {
                g = g.with(wildType(gene));
            }
        }
        return g;
    }

    /**
     * Force {@link #rareNaturalGenes} natural loci of at least {@link #rareTier}
     * into a state that actually <i>shows</i> - homozygous for a variant allele,
     * which is the only way to be sure for a recessive.
     */
    private Genotype addRareNaturals(Genotype g, Rng rng) {
        if (rareNaturalGenes == 0) {
            return g;
        }
        List<Gene> pool = rareNaturalPool();
        return forceHomozygousVariant(g, rng, pool, rareNaturalGenes);
    }

    /**
     * <b>A stable that names a magic count owns the magic entirely.</b> The
     * breed's own geometric draw has already put some magical loci on this
     * horse, and "at least one homozygous magic trait, then a one-in-ten chance
     * of a second" is a statement about the <i>horse</i>, not about what this
     * pass adds on top. So the non-body-stat magical loci are cleared first and
     * exactly the rolled number are put back. Without that, every horse in the
     * stable had whatever its breed felt like plus the guarantee, and the ladder
     * described nothing anybody could observe.
     */
    private Genotype addHomozygousMagic(Genotype g, Rng rng) {
        if (maxMagic == 0) {
            return g;
        }
        for (Gene gene : magicalPool()) {
            g = g.with(wildType(gene));
        }
        int wanted = minMagic;
        // A ladder: each extra is conditional on the one before it, so a tenth
        // get a second, a twentieth a third, and so on to the cap.
        double chance = extraMagicChance;
        while (wanted < maxMagic && chance > 0 && rng.nextFloat() < chance) {
            wanted++;
            chance *= extraMagicFalloff;
        }
        return forceHomozygousVariant(g, rng, magicalPool(), wanted);
    }

    /**
     * Set {@code count} distinct loci from {@code pool} to two copies of a
     * variant allele, skipping any the horse already shows something at.
     * Short of loci, it does what it can - a stable in a registry with three
     * magical genes should still generate.
     */
    private static Genotype forceHomozygousVariant(Genotype g, Rng rng, List<Gene> pool, int count) {
        if (count <= 0 || pool.isEmpty()) {
            return g;
        }
        List<Gene> shuffled = new ArrayList<>(pool);
        // Fisher-Yates off the shared Rng, so the whole stable is one
        // reproducible stream rather than depending on a set's iteration order.
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Gene tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        int placed = 0;
        for (Gene gene : shuffled) {
            if (placed >= count) {
                break;
            }
            AllelePair pair = homozygousVariant(gene);
            if (pair == null) {
                continue;
            }
            g = g.with(pair);
            placed++;
        }
        return g;
    }

    /**
     * Two copies of this gene's first-declared variant allele, or {@code null}
     * when the gene cannot carry them - the same "first-declared is the variant"
     * convention the Known Gene Splice carrot uses.
     */
    private static AllelePair homozygousVariant(Gene gene) {
        Allele variant = null;
        for (Allele allele : gene.alleles()) {
            if (!allele.equals(gene.defaultAllele()) && !gene.isPlaceholder(allele)) {
                variant = allele;
                break;
            }
        }
        if (variant == null) {
            return null;
        }
        AllelePair pair = new AllelePair(variant, variant);
        return gene.canOccur(pair) && gene.sexConsistent(pair) ? pair : null;
    }

    private static AllelePair wildType(Gene gene) {
        return new AllelePair(gene.defaultAllele(), gene.defaultAllele());
    }

    // ------------------------------------------------------------------
    // Pools
    // ------------------------------------------------------------------

    /**
     * Natural loci at or above {@link #rareTier} that a stable may force.
     * Filtered through the splice-safety pool, so "a rare horse" can never mean
     * "a horse with a lethal".
     */
    public List<Gene> rareNaturalPool() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : com.example.horsegenetics.common.genetics.SpliceSafety.pool()) {
            if (gene.isNatural() && gene.rarity().ordinal() >= rareTier.ordinal()) {
                out.add(gene);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Magical loci a stable may force or clear, same safety filter - and without
     * the four body-stat loci, which are the breed's business (see
     * {@link #BODY_STAT_KEYS}).
     */
    public static List<Gene> magicalPool() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : com.example.horsegenetics.common.genetics.SpliceSafety.pool()) {
            if (!gene.isNatural() && !isBodyStat(gene)) {
                out.add(gene);
            }
        }
        return List.copyOf(out);
    }
}
