package com.example.horsegenetics.neoforge.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

/**
 * Finds the enclosed air volume behind a stall sign.
 *
 * <p>A stall sign is placed on the <b>outside</b> face of a wall block; the
 * seed for the search is the block on the <b>opposite</b> side of that wall (its
 * interior face). From there this flood-fills through air, staying within
 * <b>one layer up or down</b> of the seed (so a stall is at most three blocks
 * tall), and succeeds only if the fill closes before it reaches
 * {@link #MAX_BLOCKS} cells - i.e. the area is actually walled in. An open-topped
 * or open-sided area just runs away and fails.
 */
public final class StallDetector {

    /** Above this the fill is treated as "not enclosed" and the sign is rejected. */
    public static final int MAX_BLOCKS = 512;

    /** The axis-aligned block span of a detected stall, plus its cell count. */
    public record Result(BlockPos min, BlockPos max, int blockCount) {

        public int sizeX() {
            return max.getX() - min.getX() + 1;
        }

        public int sizeY() {
            return max.getY() - min.getY() + 1;
        }

        public int sizeZ() {
            return max.getZ() - min.getZ() + 1;
        }
    }

    private StallDetector() {
    }

    public static Optional<Result> detect(LevelReader level, BlockPos seed) {
        if (!isOpen(level, seed)) {
            return Optional.empty();
        }
        int seedY = seed.getY();

        Set<BlockPos> filled = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        filled.add(seed.immutable());
        frontier.add(seed.immutable());

        int minX = seed.getX();
        int minY = seed.getY();
        int minZ = seed.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        while (!frontier.isEmpty()) {
            BlockPos p = frontier.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (Math.abs(n.getY() - seedY) > 1) {
                    continue; // this layer, or one up / down
                }
                if (filled.contains(n) || !isOpen(level, n)) {
                    continue;
                }
                if (filled.size() >= MAX_BLOCKS) {
                    return Optional.empty(); // ran away - not enclosed
                }
                BlockPos immut = n.immutable();
                filled.add(immut);
                frontier.add(immut);
                minX = Math.min(minX, n.getX());
                minY = Math.min(minY, n.getY());
                minZ = Math.min(minZ, n.getZ());
                maxX = Math.max(maxX, n.getX());
                maxY = Math.max(maxY, n.getY());
                maxZ = Math.max(maxZ, n.getZ());
            }
        }

        return Optional.of(new Result(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                filled.size()));
    }

    /** A cell the fill can pass through: inside the world and empty of blocks. */
    private static boolean isOpen(LevelReader level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getBlockState(pos).isAir();
    }
}
