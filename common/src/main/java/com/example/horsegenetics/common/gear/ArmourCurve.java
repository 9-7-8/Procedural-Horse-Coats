package com.example.horsegenetics.common.gear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>What a chestplate is worth on a horse</b> (#296): the curve vanilla's own
 * chestplate-to-horse-armour pairs draw, so a modded horse armour can protect in
 * proportion to that mod's chestplate instead of the flat ingot-or-gem guess.
 *
 * <p>The rungs are the caller's, read off the game's armour materials at run time
 * (the NeoForge side passes leather, copper, iron and diamond), never written
 * down here. Between two rungs the value is a straight line; below the first it
 * is in proportion to the first; above the last it carries on the last
 * segment's slope, up to the cap.
 *
 * <p><b>Why only the plain rungs</b> (a build-time call, recorded on
 * {@code wiki/item-saddle.html#armour-curve}): vanilla's pairs are not a function.
 * Gold (chest 5, horse 7) sits above iron on a horse and below it on a player,
 * and netherite (chest 8, horse 19) shares diamond's chestplate value with
 * nearly twice its horse value; our own chainmail (5, 4) shares gold's chest
 * value. Those are deliberate exceptions of one material each, not a trend, so a
 * modded chestplate is placed by the materials that are not exceptions, and
 * netherite's horse value is the cap no modded armour passes.
 */
public final class ArmourCurve {

    /** One vanilla material: its chestplate's armour and its horse armour's. */
    public record Rung(int chest, int body) {
    }

    private ArmourCurve() {
    }

    /**
     * The horse armour value for a chestplate worth {@code chest}, at least 1 and
     * at most {@code cap}.
     *
     * @param rungs at least one, chest values positive and all different
     * @throws IllegalArgumentException for a chest value that is not positive,
     *                                  or rungs that do not meet the above
     */
    public static int bodyFor(double chest, List<Rung> rungs, int cap) {
        if (!(chest > 0)) {
            throw new IllegalArgumentException("chest armour must be positive: " + chest);
        }
        List<Rung> sorted = new ArrayList<>(rungs);
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException("no rungs");
        }
        sorted.sort(Comparator.comparingInt(Rung::chest));
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).chest() <= 0 || (i > 0 && sorted.get(i).chest() == sorted.get(i - 1).chest())) {
                throw new IllegalArgumentException("rungs need positive, distinct chest values: " + sorted);
            }
        }
        Rung first = sorted.get(0);
        double body;
        if (chest <= first.chest()) {
            body = chest * first.body() / first.chest();
        } else {
            // The segment chest falls in, or the last one, carried on past its end.
            int i = 1;
            while (i < sorted.size() - 1 && chest > sorted.get(i).chest()) {
                i++;
            }
            if (sorted.size() == 1) {
                body = chest * first.body() / first.chest();
            } else {
                Rung lo = sorted.get(i - 1);
                Rung hi = sorted.get(i);
                body = lo.body() + (chest - lo.chest()) * (hi.body() - lo.body()) / (double) (hi.chest() - lo.chest());
            }
        }
        long rounded = Math.round(body);
        return (int) Math.max(1, Math.min(cap, rounded));
    }
}
