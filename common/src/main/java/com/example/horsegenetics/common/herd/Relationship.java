package com.example.horsegenetics.common.herd;

import java.util.Objects;
import java.util.UUID;

/**
 * <b>What one horse makes of another</b> - one side of a pairwise relationship.
 *
 * <p>Horse society is not a ladder with an alpha at the top; it is a set of
 * two-horse facts (see {@code wiki/horse-care.html#science-rank}). So each horse
 * keeps its own view of each horse it knows, and the two views are written
 * together whenever something happens between them.
 *
 * @param other        the horse this is about
 * @param familiarity  0..1 - how well it knows the other. Grows with time spent
 *                     near each other, fades apart. Familiar horses spar rather
 *                     than fight.
 * @param rank         -1..1 - positive means <i>the other yields to this horse</i>,
 *                     negative means this horse yields. Moved by sparring,
 *                     displacement and fights; never a global position.
 * @param grooming     0..1 - a grooming-partner bond. Grows only between
 *                     partners (mares, a dam and her foal) standing together, and
 *                     fades very slowly, so it outlasts a stallion.
 * @param rivalry      0..1 - between males. Builds from contact across bands and
 *                     from contests, and makes a real fight likelier.
 * @param lastTogether the game tick the two were last near each other
 */
public record Relationship(UUID other, double familiarity, double rank, double grooming,
                           double rivalry, long lastTogether) {

    public Relationship {
        Objects.requireNonNull(other, "other");
        familiarity = clamp01(familiarity);
        rank = Math.max(-1.0, Math.min(1.0, rank));
        grooming = clamp01(grooming);
        rivalry = clamp01(rivalry);
    }

    /** A horse just met. */
    public static Relationship stranger(UUID other, long now) {
        return new Relationship(other, 0.0, 0.0, 0.0, 0.0, now);
    }

    /**
     * How much this relationship is worth keeping when a ledger is full: the
     * sum of everything it records, so a strong rival is kept as surely as a
     * close friend and a passing acquaintance is the first to go.
     */
    public double importance() {
        return familiarity + grooming * 1.5 + rivalry + Math.abs(rank) * 0.5;
    }

    Relationship withFamiliarity(double f) {
        return new Relationship(other, f, rank, grooming, rivalry, lastTogether);
    }

    Relationship withRank(double r) {
        return new Relationship(other, familiarity, r, grooming, rivalry, lastTogether);
    }

    Relationship withGrooming(double g) {
        return new Relationship(other, familiarity, rank, g, rivalry, lastTogether);
    }

    Relationship withRivalry(double r) {
        return new Relationship(other, familiarity, rank, grooming, r, lastTogether);
    }

    Relationship seen(long now) {
        return new Relationship(other, familiarity, rank, grooming, rivalry, now);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(1.0, v);
    }
}
