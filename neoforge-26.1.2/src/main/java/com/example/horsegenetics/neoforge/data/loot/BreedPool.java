package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.breed.Region;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Draw a random breed of a bounded commonness</b> - the one piece of logic
 * behind every "a random breed spawn egg" in a chest or on a villager's counter.
 *
 * <p>The twin of {@link GenePool}, and the same argument for existing: a breed a
 * player drops into {@code .minecraft/phc/breeds/} joins these pools by
 * being registered, not by anyone editing a list. The only membership test is
 * {@link BreedSource#SPAWN_EGG} - which is exactly the "each breed can opt out"
 * switch, expressed as the absence of a source rather than as a flag of its own.
 *
 * <p>Within the window, draws are weighted by the breed's own
 * {@link Breed#spawnWeight()}, so the egg you find in a chest is as likely to be
 * a Quarter Horse as a Quarter Horse is likely to be the herd you ride past.
 * That is the point of a rare breed's egg being worth something.
 */
public final class BreedPool {

    private BreedPool() {
    }

    /**
     * A breed whose commonness is between {@code rarest} and {@code commonest}
     * inclusive, weighted toward the commoner end, or {@code null} if the window
     * is empty.
     *
     * <p>The tiers are given as {@link Commonness} rather than as raw weights so
     * a datapack trade can say "a rare one" without knowing that rare is 1.5.
     * Note the enum runs commonest-first, so {@code commonest.ordinal()} is the
     * <i>low</i> bound.
     */
    public static @Nullable Breed draw(RandomSource rng, Commonness commonest, Commonness rarest) {
        return draw(rng, commonest, rarest, null);
    }

    /**
     * The same draw, narrowed to <b>one region's countries</b> - what the
     * horseman's breed egg trades use, so that the eggs on his counter come from
     * the same part of the world as the horses his
     * <a href="https://9-7-8.github.io/Procedural-Horse-Coats/wiki/breeds.html#regions">cowboy</a>
     * sells. A {@code null} region draws from everywhere, which is what a
     * dungeon chest does: a chest has no cowboy standing next to it.
     *
     * <p><b>An empty window is a real outcome here, not a bug.</b> Three of the
     * ten regions have no breed at all in the commonest tier band, so a horseman
     * there returns {@code null}, the trade produces an empty stack, and vanilla
     * drops the offer - he simply has no common breed egg to sell. That is the
     * owner's call, taken over widening the band or falling back to the world
     * pool: the alternative to a missing trade is a horseman selling a breed
     * from the other side of the world, which is the one case a player would
     * notice the rule being broken.
     */
    public static @Nullable Breed draw(RandomSource rng, Commonness commonest, Commonness rarest,
                                       @Nullable Region region) {
        int lo = Math.min(commonest.ordinal(), rarest.ordinal());
        int hi = Math.max(commonest.ordinal(), rarest.ordinal());

        List<Breed> pool = new ArrayList<>();
        double total = 0.0;
        for (Breed breed : Breeds.from(BreedSource.SPAWN_EGG)) {
            int tier = Commonness.forWeight(breed.spawnWeight()).ordinal();
            if (tier < lo || tier > hi) {
                continue;
            }
            if (region != null && !region.countries().contains(breed.country())) {
                continue;
            }
            pool.add(breed);
            total += breed.spawnWeight();
        }
        if (pool.isEmpty() || total <= 0.0) {
            return null;
        }
        double roll = rng.nextDouble() * total;
        for (Breed breed : pool) {
            roll -= breed.spawnWeight();
            if (roll < 0.0) {
                return breed;
            }
        }
        return pool.get(pool.size() - 1);
    }
}
