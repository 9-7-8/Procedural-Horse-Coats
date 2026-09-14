package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>In the test yard, a horse only notices the horses in its own pen</b> (owner,
 * 2026-09-14: condense the testing area, pens isolated, yard only).
 *
 * <p>Every breeding and band check counts neighbours by distance, straight through
 * walls - the eight-within-sixteen cap, a bachelor's 24-block challenge, a mare's
 * 32-block transfer, a youngster's 64-block search for somewhere to go. The yard's
 * breeding and herd rows used to be spread hundreds of blocks deep so no pen reached
 * another. Now each registered pen is its own world for those checks, and the pens
 * pack wall to wall.
 *
 * <p>Pens sharing a <b>group</b> see each other: a dispersal test needs a band next
 * door to disperse <i>to</i>. A horse in no registered pen (the walkway, rows A-N, any
 * other dimension) sees every horse outside the registered pens, exactly as before.
 * Outside the horse dimension this answers {@code true} always, so no world's
 * gameplay changes.
 *
 * <p><b>Not a wall for vanilla.</b> Golden-carrot breeding is vanilla's
 * {@code BreedGoal}, which this does not reach: two horses in love a wall apart can
 * still breed, so pens that are fed carrots keep an air gap between them.
 */
final class YardPens {

    private YardPens() {
    }

    private record Pen(AABB inside, String group) {
    }

    private static final List<Pen> PENS = new ArrayList<>();

    static void clear() {
        PENS.clear();
    }

    /** A pen whose brick walls stand at {@code x0}, {@code x1}, {@code z0} and {@code z1}. */
    static void register(int gy, int x0, int x1, int z0, int z1, String group) {
        PENS.add(new Pen(new AABB(x0 + 1, gy, z0 + 1, x1, gy + 6, z1), group));
    }

    private static @Nullable String groupOf(Entity e) {
        for (Pen p : PENS) {
            if (p.inside().contains(e.position())) {
                return p.group();
            }
        }
        return null;
    }

    /** Is {@code e} inside one of the yard's registered pens? */
    static boolean inPen(Entity e) {
        return !PENS.isEmpty() && e.level().dimension().equals(DebugPenManager.DEBUG_LEVEL) && groupOf(e) != null;
    }

    /** May {@code a} take {@code b} into account at all? */
    static boolean together(Entity a, Entity b) {
        if (PENS.isEmpty() || !a.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            return true;
        }
        return Objects.equals(groupOf(a), groupOf(b));
    }
}
