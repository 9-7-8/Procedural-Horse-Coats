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
 * every "a random carrot" and "a random paper" the horseman sells.
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
