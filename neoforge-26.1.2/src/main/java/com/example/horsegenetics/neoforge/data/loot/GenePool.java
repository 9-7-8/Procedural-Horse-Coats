package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Draw a random gene of a bounded rarity</b> - the one piece of logic behind
 * every "a random carrot" and "a random paper" the equestrians sell.
 *
 * <p>The roadmap's requirement was that a third-party gene joins these pools
 * automatically and that <b>legendary and mythic are never sold</b>. Both fall
 * out of asking {@link Genes} rather than keeping a list: a gene is in the pool
 * if it is registered, has a gene carrot, and its {@link GeneRarity} is inside
 * the tier window the trade asked for. Nothing has to be edited when a gene is
 * added, and the never-sold rule is a comparison rather than a blacklist that
 * can go stale.
 *
 * <p>Within the window, draws are weighted by {@link GeneRarity#lootWeight()},
 * the same weighting the chest-loot papers use - so the price ladder and the
 * find-it-in-the-wild ladder agree.
 */
public final class GenePool {

    private GenePool() {
    }

    /**
     * A gene between {@code min} and {@code max} rarity inclusive, weighted
     * toward the commoner end, or {@code null} if the window is empty.
     */
    public static @Nullable Gene draw(RandomSource rng, GeneRarity min, GeneRarity max) {
        return draw(rng, min, max, gene -> true);
    }

    /**
     * As above, but only genes the caller will actually be able to use.
     *
     * <p>The filter is applied <b>before</b> the weighted draw rather than after
     * it, which is the whole reason it exists: drawing first and rejecting after
     * would either return nothing (costing the villager a trade slot for no
     * reason) or need a retry loop that can spin. The homozygous carrot is the
     * caller this was added for - a gene whose homozygote cannot occur is not a
     * gene it can make a carrot out of.
     */
    public static @Nullable Gene draw(RandomSource rng, GeneRarity min, GeneRarity max,
                                      java.util.function.Predicate<Gene> usable) {
        List<Gene> pool = new ArrayList<>();
        int total = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (!gene.hasGeneCarrot()) {
                continue;
            }
            GeneRarity rarity = gene.rarity();
            if (rarity.ordinal() < min.ordinal() || rarity.ordinal() > max.ordinal()) {
                continue;
            }
            if (!usable.test(gene)) {
                continue;
            }
            pool.add(gene);
            total += rarity.lootWeight();
        }
        if (pool.isEmpty() || total <= 0) {
            return null;
        }
        int roll = rng.nextInt(total);
        for (Gene gene : pool) {
            roll -= gene.rarity().lootWeight();
            if (roll < 0) {
                return gene;
            }
        }
        return pool.get(pool.size() - 1);
    }
}
