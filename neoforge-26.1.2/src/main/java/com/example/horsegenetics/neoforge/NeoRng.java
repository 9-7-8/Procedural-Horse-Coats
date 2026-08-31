package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.Rng;
import net.minecraft.util.RandomSource;

/**
 * The entire cost of using Minecraft's world-seeded randomness from the
 * common module: one two-line adapter class. This is the pattern to repeat
 * for every other Minecraft-specific dependency common needs bridged.
 */
public record NeoRng(RandomSource source) implements Rng {

    @Override
    public float nextFloat() {
        return source.nextFloat();
    }

    @Override
    public boolean nextBoolean() {
        return source.nextBoolean();
    }

    @Override
    public int nextInt(int bound) {
        return source.nextInt(bound);
    }
}
