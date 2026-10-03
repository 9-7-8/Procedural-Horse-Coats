package com.example.horsegenetics.common.wild;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.BreedSpawnSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>Thin ground refills.</b> The rules of the wild-horse top-up, with no
 * Minecraft in them.
 *
 * <p>The world is cut into fixed square <b>cells</b> of {@code wild.topup_cell_chunks}
 * chunks a side (8, so 128 blocks). A biome is unbounded and only the chunks
 * near players are loaded, so "fewer than five horses in that biome" is made
 * concrete as "fewer than {@code wild.topup_minimum} wild horses in this cell".
 * The minimum is a trigger, not a ceiling: one roll spawns a whole natural-sized
 * pack and may leave the cell well above it (owner: "that's just the bare
 * minimum per cell").
 *
 * <h2>One top-up, two triggers</h2>
 * Each cell, once looked at, holds a {@link CellStamp}: the game day it was
 * looked at and the {@linkplain #signature ecology signature} it was looked at
 * under. A cell is rolled when a player is near it and its stamp is not
 * {@linkplain #current current}. That one rule is both triggers the owner asked
 * for:
 * <ul>
 *   <li><b>Every morning</b>: a new game day makes every stamp a day old, so each
 *       cell rolls again the first time a player is near it that day.</li>
 *   <li><b>A changed breed list</b>: a biome newly given to a breed changes the
 *       signature, so every cell rolls once on the next walk - and then, being
 *       stamped, is a no-op until the morning. Running it twice is running it
 *       once, which is the idempotence the request asked for.</li>
 * </ul>
 * A stamp that is not today's is worth exactly as much as no stamp, so the host
 * keeps only today's ({@link #keep}) - which is also what bounds the ledger: it
 * holds the cells players have been near since the day turned, and no more.
 */
public final class TopUpPlan {

    /** {@code wild.topup_minimum}'s default; 0 switches the top-up off. */
    public static final int DEFAULT_MINIMUM = 5;
    public static final int MAX_MINIMUM = 64;

    /** {@code wild.topup_cell_chunks}'s default, and its bounds. */
    public static final int DEFAULT_CELL_CHUNKS = 8;
    public static final int MIN_CELL_CHUNKS = 2;
    public static final int MAX_CELL_CHUNKS = 32;

    /** {@code wild.topup_player_radius}'s default, and its bounds, in blocks. */
    public static final int DEFAULT_PLAYER_RADIUS = 96;
    public static final int MIN_PLAYER_RADIUS = 32;
    public static final int MAX_PLAYER_RADIUS = 256;

    /**
     * No horse of a top-up is placed nearer a player than this - vanilla's own
     * floor for a natural spawn ({@code NaturalSpawner.MIN_SPAWN_DISTANCE}, 24).
     */
    public static final double MIN_PLAYER_DISTANCE = 24.0;

    /** A cell's settings, as the host read them from config. Built only from there. */
    public record Settings(int minimum, int cellChunks, int playerRadius) {
        public Settings {
            if (minimum < 0 || minimum > MAX_MINIMUM) {
                throw new IllegalArgumentException("minimum must be 0-" + MAX_MINIMUM + ": " + minimum);
            }
            if (cellChunks < MIN_CELL_CHUNKS || cellChunks > MAX_CELL_CHUNKS) {
                throw new IllegalArgumentException("cell chunks must be " + MIN_CELL_CHUNKS + "-" + MAX_CELL_CHUNKS
                        + ": " + cellChunks);
            }
        }

        public static final Settings DEFAULT = new Settings(DEFAULT_MINIMUM, DEFAULT_CELL_CHUNKS, DEFAULT_PLAYER_RADIUS);

        /** {@code wild.topup_minimum} 0 is the off switch. */
        public boolean off() {
            return minimum == 0;
        }

        /** The edge of a cell in blocks. */
        public int cellBlocks() {
            return cellChunks * 16;
        }
    }

    /** What a cell's stamp says: the day it was looked at, under which signature. */
    public record CellStamp(long day, long signature) {
    }

    /** What the host does with a cell a player is near. */
    public enum Decision {
        /** Already looked at today, under today's settings. */
        CURRENT,
        /** Looked at now: enough wild horses already. Stamp it. */
        FULL,
        /** Looked at now: too few. Spawn a herd, and stamp it whether or not one fitted. */
        ROLL
    }

    private TopUpPlan() {
    }

    // ------------------------------------------------------------------
    // Cells
    // ------------------------------------------------------------------

    /** The cell index along one axis that block coordinate {@code block} falls in. Floors, so -1 is cell -1. */
    public static int cellOf(int block, int cellChunks) {
        return Math.floorDiv(block, cellChunks * 16);
    }

    /** One long naming a cell, x in the high half and z in the low. */
    public static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    public static int cellX(long key) {
        return (int) (key >> 32);
    }

    public static int cellZ(long key) {
        return (int) key;
    }

    /** The block a cell starts at, along one axis. */
    public static int cellMinBlock(int cell, int cellChunks) {
        return cell * cellChunks * 16;
    }

    /**
     * Every cell any part of which lies within {@code radius} blocks
     * (horizontally) of the point {@code (x, z)} - the cells a player there is
     * "near".
     */
    public static List<Long> cellsNear(double x, double z, int radius, int cellChunks) {
        int size = cellChunks * 16;
        int minX = Math.floorDiv((int) Math.floor(x - radius), size);
        int maxX = Math.floorDiv((int) Math.floor(x + radius), size);
        int minZ = Math.floorDiv((int) Math.floor(z - radius), size);
        int maxZ = Math.floorDiv((int) Math.floor(z + radius), size);
        List<Long> out = new ArrayList<>();
        double r2 = (double) radius * radius;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                double nearX = clamp(x, cx * (double) size, (cx + 1) * (double) size);
                double nearZ = clamp(z, cz * (double) size, (cz + 1) * (double) size);
                double dx = nearX - x;
                double dz = nearZ - z;
                if (dx * dx + dz * dz <= r2) {
                    out.add(cellKey(cx, cz));
                }
            }
        }
        return out;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------
    // Days and stamps
    // ------------------------------------------------------------------

    /** The game day a game time falls on. Game time, so neither /time set nor sleeping moves it. */
    public static long dayOf(long gameTime) {
        return Math.floorDiv(gameTime, WildLifetime.DAY_TICKS);
    }

    /** Has this cell already been looked at today, under these settings? A missing stamp never is. */
    public static boolean current(CellStamp stamp, long today, long signature) {
        return stamp != null && stamp.day() == today && stamp.signature() == signature;
    }

    /** Should the ledger keep this stamp? Only today's do anything; an older one is a missing one. */
    public static boolean keep(CellStamp stamp, long today) {
        return stamp.day() == today;
    }

    /**
     * The whole rule for one cell a player is near.
     *
     * @param stamp     the cell's stamp, or {@code null} if it has none
     * @param wildCount the untamed wild horses standing in the cell's loaded chunks now
     */
    public static Decision decide(CellStamp stamp, long today, long signature, int wildCount, Settings settings) {
        if (current(stamp, today, signature)) {
            return Decision.CURRENT;
        }
        return wildCount < settings.minimum() ? Decision.ROLL : Decision.FULL;
    }

    /** How many horses a rolled herd has: the biome's own natural pack count, {@code min..max} inclusive. */
    public static int packSize(int min, int max, Rng rng) {
        int lo = Math.max(1, min);
        int hi = Math.max(lo, max);
        return lo + rng.nextInt(hi - lo + 1);
    }

    // ------------------------------------------------------------------
    // The ecology signature
    // ------------------------------------------------------------------

    /**
     * <b>A hash of everything that decides where a wild horse may be</b>: each
     * wild breed's id, biomes, spawn weight, hour and floors; Feral Mixed's
     * settings; and the top-up's own cell size and minimum. Nothing else - a
     * breed's description, genes or stats can change without a single cell
     * re-rolling.
     *
     * <p>Breeds are read at startup, so the host computes this once per server
     * start. Order-independent: breeds are sorted by id and each breed's biomes
     * by name, so a file reordered changes nothing.
     */
    public static long signature(List<Breed> breeds, BreedSpawnSettings.Feral feral, Settings settings) {
        List<Breed> wild = new ArrayList<>();
        for (Breed b : breeds) {
            if (b.allows(BreedSource.WILD)) {
                wild.add(b);
            }
        }
        wild.sort(Comparator.comparing(Breed::id));
        StringBuilder s = new StringBuilder();
        for (Breed b : wild) {
            List<String> biomes = new ArrayList<>(b.biomes());
            biomes.sort(null);
            s.append(b.id()).append('|').append(biomes).append('|').append(b.spawnWeight())
                    .append('|').append(b.spawnTime().name()).append('|').append(b.spawnGround()).append('\n');
        }
        List<String> feralBiomes = new ArrayList<>(feral.biomes());
        feralBiomes.sort(null);
        s.append("feral|").append(feral.enabled()).append('|').append(feralBiomes).append('|').append(feral.herdWeight())
                .append('\n');
        s.append("cells|").append(settings.cellChunks()).append('|').append(settings.minimum());
        return fnv64(s.toString());
    }

    /** FNV-1a, 64 bit: the same answer on every JVM and in the browser build. */
    static long fnv64(String text) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) {
            h ^= text.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    // ------------------------------------------------------------------
    // Debug wording
    // ------------------------------------------------------------------

    /** The debug line for a cell that was rolled. */
    public static String rolledLine(long cell, String biome, int counted, int spawned, String where) {
        String at = "cell " + cellX(cell) + "," + cellZ(cell);
        if (spawned == 0) {
            return at + " (" + biome + ") had " + counted + " wild horses; no spot for a herd fitted, so it waits"
                    + " for tomorrow";
        }
        return at + " (" + biome + ") had " + counted + " wild horses; a herd of " + spawned + " arrived at " + where;
    }
}
