package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproState;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.common.repro.StallionDay;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.carts.CartWood;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.SupplyCartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * <b>Row BC: the cart's harness and the stallion's day</b> (2026-10-02). Two open checks that need a horse,
 * a cart or a paddock and a clock, and nobody at the keyboard: whether a hitched horse gets its speed back,
 * and whether a stallion left with six mares now covers past three in a day.
 *
 * <table>
 *   <tr><th>half</th><th>pens</th></tr>
 *   <tr><td>west, x0 .. x0+19</td><td>DRAUGHT: one horse and one supply cart in a brick pen (x0 .. x0+19)</td></tr>
 *   <tr><td>east, x0+25 .. x0+44</td><td>STALLION DAY: a stallion and six mares (x0+25 .. x0+37), air at
 *       x0+38, and FRESHER, two stallions and a mare in a glass box (x0+39 .. x0+44, z0+3 .. z0+8)</td></tr>
 * </table>
 *
 * <p>Every pen reads state, never the log, and answers each of its checks exactly once through
 * {@link DebugYardClockwork#verdict} / {@link DebugYardClockwork#inconclusive}. Every clock step is wrapped so a
 * throw answers the pen's open checks INCONCLUSIVE instead of silencing them, and a {@link #run} counter stops
 * the steps of an older build. Timings: DRAUGHT answers at about 45 seconds (deadline 5 minutes); STALLION DAY
 * usually inside its first two reproductive days (a debug day is one minute), deadline 90 minutes; FRESHER
 * usually inside the first minute, deadline 90 minutes.
 */
final class DebugYardHarness {

    private DebugYardHarness() {
    }

    /** Bumped by every build, so a clock left running for the yard before this one stops itself. */
    private static int run;
    /** Checks that have answered in this build - each is answered exactly once. */
    private static final Set<String> ANSWERED = new HashSet<>();

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int thisRun = ++run;
        ANSWERED.clear();
        try {
            west(level, gy, x0, z0, thisRun);
            east(level, gy, x0, z0, thisRun);
            ActionTrace.log("test yard", "row BC built (west: DRAUGHT; east: STALLION DAY, FRESHER)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BC (harness and stallion day) failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    private static void pass(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, true, detail);
        }
    }

    private static void fail(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, false, detail);
        }
    }

    private static void inconclusive(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.inconclusive(check, detail);
        }
    }

    private static boolean answered(String check) {
        return ANSWERED.contains(check);
    }

    /**
     * One clock step: skipped if a newer build has started, and if it throws, every check it serves that is still
     * open answers INCONCLUSIVE with the exception, rather than the pen going silent.
     */
    private static void step(int stepRun, List<String> checks, Runnable body) {
        if (stepRun != run) {
            return;
        }
        try {
            body.run();
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BC step threw", e);
            for (String c : checks) {
                inconclusive(c, "a clock step threw " + e + " - the pen could not finish");
            }
        }
    }

    private static @Nullable Horse find(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static @Nullable UUID idOf(@Nullable Horse h) {
        return h == null ? null : h.getUUID();
    }

    private static String minutes(long ticks) {
        return String.format(Locale.ROOT, "%.1f min", ticks / 1200.0);
    }

    private static String f4(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    /**
     * Hold a horse on its mark: back to the spot if it has strayed more than a fifth of a block. UNVERIFIED as a
     * way to pin a mob: snapTo and getNavigation().stop() are each used elsewhere in this repo, but never together
     * every few ticks on a horse with live goals; FRESHER's counts log the distances it actually saw.
     */
    private static void hold(Horse h, int gy, double x, double z) {
        double dx = h.getX() - x;
        double dz = h.getZ() - z;
        if (dx * dx + dz * dz > 0.04 || Math.abs(h.getY() - (gy + 1)) > 0.5) {
            h.getNavigation().stop();
            h.snapTo(x, gy + 1, z, h.getYRot(), 0.0F);
        }
    }

    // ------------------------------------------------------------------
    // WEST - DRAUGHT
    // ------------------------------------------------------------------

    /*
     * DRAUGHT (wiki/carts.html, Verification tab):
     *   "Detaching gives the speed back. The modifier is transient and removed on unhitch. A horse that stays slow
     *    after unhitching is a leak, and it compounds every re-attach."
     * and, from the same page's account of the load: "A loaded cart is slower ... refreshDraught ... runs from
     * pulledTick".
     *
     * WHAT THE CODE DOES, READ FROM carts/entity/AbstractDrawnEntity. setPulling(horse) calls applyDraught(horse)
     * directly: it removes the PULL_MODIFIER_ID modifier (HorseCarts.resLoc("pull")) from MOVEMENT_SPEED, computes
     * CartDraft.speedModifier(HorseDraft.pullOf(horse), attr.getBaseValue(), currentLoad()) and, if negative, adds
     * it back as a transient ADD_MULTIPLIED_TOTAL modifier. So the draught is on from the moment of the hitch -
     * it does NOT need the cart to be pulled along, and this pen does not drive the horse. setPulling(null)
     * removes the same id. refreshDraught runs inside pulledTick, which CartWorld.tick calls every server tick
     * for every hitched cart whether or not it moves (HorseCarts.onServerTick), and it re-applies the draught
     * once a second (cart tickCount % 20) when currentLoad() has moved. currentLoad() for a supply cart is
     * CartDraft.loaded(0.30, 0.50, fillLevel()), fillLevel being the slots' fullness over 54. A hitched horse's
     * MOVE goals are held off by PullCartGoal, so it stands still.
     *
     * THE HITCH IS NOT THE PLAYER'S PATH. A player hitches by riding the horse and pressing R
     * (ActionKeyPayload.handle -> setPulling); the hands are a FakePlayer, which cannot ride. So the pen calls
     * cart.setPulling(horse) and cart.setPulling(null) - the exact two calls that path ends in.
     *
     * THE RUN. One tamed horse (every gene default) and an oak supply cart. The horse's MOVEMENT_SPEED is read
     * (value, base, modifier ids) before anything happens: the baseline. Then five cycles, each: horse and cart
     * put back on their marks (the cart 2.5 blocks behind, the hitch's own spacing), the cart emptied; hitch;
     * 40 ticks; read (EMPTY); 54 full stacks of dirt into the cart; 60 ticks (three refreshes); read (LOADED);
     * unhitch; 40 ticks; read (AFTER). Every hitched read first checks the cart is still hitched to this horse.
     *
     * CHECKS, judged when the fifth cycle's AFTER is read (about 45 seconds after build):
     *   SLOWED  - PASS: every cycle's EMPTY value is below the baseline with the pull modifier on.
     *             FAIL: hitched (confirmed) and not slower, or no pull modifier. INCONCLUSIVE: the hitch was refused
     *             (getPulling() is not the horse straight after setPulling), or the cart came off by itself.
     *   LOADED  - PASS: every cycle's LOADED value is below its EMPTY value and currentLoad() rose. FAIL: it did not
     *             (including the load not moving - 54 full stacks are fillLevel 1.0, load 0.30 -> 0.45).
     *   RESTORE - PASS: after every unhitch the value equals the baseline (to 1e-9), the pull modifier is gone and
     *             the modifier id set equals the baseline's; and the five EMPTY values and the five LOADED values
     *             are each identical across cycles (no compounding). FAIL: any of those differ while nothing but
     *             the pull modifier can explain it. INCONCLUSIVE: a modifier other than the pull one came or went
     *             between baseline and the read (some other system touched the attribute; the detail names it).
     * Every detail carries the baseline, each cycle's three values, the loads and what CartDraft predicts.
     * Deadline: 5 minutes after build, anything unanswered is INCONCLUSIVE with the cycles read so far.
     */

    private static final String D_SLOW = "DRAUGHT SLOWED - a horse hitched to a supply cart is slower than its"
            + " baseline, with the pull modifier on, on each of five hitches";
    private static final String D_LOAD = "DRAUGHT LOADED - filling the hitched supply cart slows the horse further"
            + " (refreshDraught re-applies the load while hitched)";
    private static final String D_RESTORE = "DRAUGHT RESTORE - unhitching gives back exactly the baseline speed and"
            + " modifier set, five hitch/unhitch cycles, no leak and no compounding";
    private static final List<String> D_ALL = List.of(D_SLOW, D_LOAD, D_RESTORE);

    private static final int CYCLES = 5;
    private static final long D_DEADLINE = 6_000L;
    /** HorseCarts.resLoc("pull") is what AbstractDrawnEntity's private PULL_MODIFIER_ID is built from. */
    private static final Identifier PULL_ID = HorseCarts.resLoc("pull");

    /** One read of the horse's MOVEMENT_SPEED. */
    private record Speed(double value, double base, boolean pull, Set<String> ids) {

        static @Nullable Speed of(Horse h) {
            AttributeInstance a = h.getAttribute(Attributes.MOVEMENT_SPEED);
            if (a == null) {
                return null;
            }
            Set<String> ids = new TreeSet<>();
            for (AttributeModifier m : a.getModifiers()) {
                ids.add(m.id().toString());
            }
            return new Speed(a.getValue(), a.getBaseValue(), a.hasModifier(PULL_ID), ids);
        }

        /** The ids other than the pull modifier. */
        Set<String> others() {
            Set<String> o = new TreeSet<>(ids);
            o.remove(PULL_ID.toString());
            return o;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%.6f (base %.6f, pull %s, ids %s)", value, base,
                    pull ? "ON" : "off", ids);
        }
    }

    private static final class Cycle {
        final int n;
        boolean hitched;
        boolean emptyStillHitched;
        boolean loadedStillHitched;
        @Nullable Speed empty;
        @Nullable Speed loaded;
        @Nullable Speed after;
        double loadEmpty = Double.NaN;
        double loadFull = Double.NaN;

        Cycle(int n) {
            this.n = n;
        }

        String line() {
            return "cycle " + n + ": hitch " + (hitched ? "took" : "REFUSED") + "; EMPTY " + empty + " load "
                    + f4(loadEmpty) + (emptyStillHitched ? "" : " [NOT hitched at read]") + "; LOADED " + loaded
                    + " load " + f4(loadFull) + (loadedStillHitched ? "" : " [NOT hitched at read]") + "; AFTER "
                    + after;
        }
    }

    private static final class Draught {
        final int run;
        final int gy;
        final double hx;
        final double hz;
        final double cx;
        final double cz;
        @Nullable UUID horse;
        @Nullable UUID cart;
        @Nullable Speed baseline;
        double pull = Double.NaN;
        final List<Cycle> cycles = new ArrayList<>();

        Draught(int run, int gy, double hx, double hz, double cx, double cz) {
            this.run = run;
            this.gy = gy;
            this.hx = hx;
            this.hz = hz;
            this.cx = cx;
            this.cz = cz;
        }

        String readings() {
            StringBuilder sb = new StringBuilder("baseline " + baseline + ", pull score " + f4(pull));
            if (baseline != null && !Double.isNaN(pull)) {
                double mEmpty = CartDraft.speedModifier(pull, baseline.base(), CartKind.SUPPLY_CART.load());
                double mFull = CartDraft.speedModifier(pull, baseline.base(),
                        CartDraft.loaded(CartKind.SUPPLY_CART.load(), CartKind.SUPPLY_CART.cargoShare(), 1.0));
                sb.append(String.format(Locale.ROOT, "; CartDraft predicts EMPTY %.6f (modifier %.4f), LOADED %.6f"
                                + " (modifier %.4f)", baseline.value() * (1 + mEmpty), mEmpty,
                        baseline.value() * (1 + mFull), mFull));
            }
            for (Cycle c : cycles) {
                sb.append(" | ").append(c.line());
            }
            return sb.toString();
        }
    }

    private static void west(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        for (String c : D_ALL) {
            DebugYardClockwork.expect(c);
        }
        int z1 = z0 + 12;
        DebugTestYard.fencedPlot(level, gy, x0, x0 + 19, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DRAUGHT", "hitch: slower", "load: slower", "unhitch: same"));
        YardPens.register(gy, x0, x0 + 19, z0, z1, "BC DRAUGHT");

        // The horse faces south (yaw 0, +Z); the cart stands behind it to the north at the hitch's own spacing
        // (getSpacing 1.7 + half the horse's width, about 2.4, plus the 0.2 the target vector is offset by).
        double hx = x0 + 10.5;
        double hz = z0 + 8.5;
        Draught d = new Draught(thisRun, gy, hx, hz, hx, hz - 2.6);
        Horse horse = DebugYardClockwork.horse(level, gy, hx, hz, Sex.MALE, "", "DRAUGHT HORSE");
        d.horse = idOf(horse);

        SupplyCartEntity cart = HorseCarts.SUPPLY_CART_ENTITY.create(level, EntitySpawnReason.COMMAND);
        if (cart != null) {
            cart.setWoodType(CartWood.fallback());
            cart.snapTo(d.cx, gy + 1, d.cz);
            level.addFreshEntity(cart);
            d.cart = cart.getUUID();
        }

        DebugYardHerd.after(level, D_DEADLINE, () -> step(d.run, D_ALL, () -> {
            for (String c : D_ALL) {
                inconclusive(c, "deadline (5 min): the cycles never finished; " + d.readings());
            }
        }));
        DebugYardHerd.after(level, 40, () -> step(d.run, D_ALL, () -> {
            Horse h = find(level, d.horse);
            SupplyCartEntity c = cartOf(level, d);
            if (h == null || c == null) {
                for (String x : D_ALL) {
                    inconclusive(x, "at build: horse " + (h != null) + ", supply cart " + (c != null));
                }
                return;
            }
            d.baseline = Speed.of(h);
            d.pull = HorseDraft.pullOf(h);
            if (d.baseline == null) {
                for (String x : D_ALL) {
                    inconclusive(x, "the horse has no MOVEMENT_SPEED attribute instance");
                }
                return;
            }
            if (d.baseline.pull()) {
                for (String x : D_ALL) {
                    inconclusive(x, "the pull modifier is already on before the first hitch; " + d.readings());
                }
                return;
            }
            ActionTrace.log("test yard", "DRAUGHT: baseline " + d.baseline + ", pull " + f4(d.pull));
            cycle(level, d, 1);
        }));
    }

    private static @Nullable SupplyCartEntity cartOf(ServerLevel level, Draught d) {
        return d.cart != null && level.getEntity(d.cart) instanceof SupplyCartEntity c && c.isAlive() ? c : null;
    }

    private static void cycle(ServerLevel level, Draught d, int n) {
        Horse h = find(level, d.horse);
        SupplyCartEntity c = cartOf(level, d);
        if (h == null || c == null) {
            for (String x : D_ALL) {
                inconclusive(x, "cycle " + n + ": horse " + (h != null) + ", cart " + (c != null) + " - gone; "
                        + d.readings());
            }
            return;
        }
        Cycle cy = new Cycle(n);
        d.cycles.add(cy);
        // Back on the marks, the cart empty: every cycle starts from the same place.
        h.getNavigation().stop();
        h.snapTo(d.hx, d.gy + 1, d.hz, 0.0F, 0.0F);
        c.clearContent();
        c.snapTo(d.cx, d.gy + 1, d.cz);
        // UNVERIFIED: that a vanilla Horse passes AbstractDrawnEntity.canPull with the default empty pull_animals
        // list (it must be a PlayerRideable and not an ItemSteerable). The cart code assumes it, nothing in this
        // repo asserts it; a refusal is caught right here and answers INCONCLUSIVE.
        c.setPulling(h);
        cy.hitched = c.getPulling() == h;
        if (!cy.hitched) {
            for (String x : D_ALL) {
                inconclusive(x, "cycle " + n + ": setPulling(horse) was refused - getPulling() is "
                        + c.getPulling() + " (canPull: the config's pull_animals list, or PlayerRideable); "
                        + d.readings());
            }
            return;
        }
        DebugYardHerd.after(level, 40, () -> step(d.run, D_ALL, () -> {
            Horse h2 = find(level, d.horse);
            SupplyCartEntity c2 = cartOf(level, d);
            if (h2 == null || c2 == null) {
                return;     // the deadline answers it
            }
            cy.emptyStillHitched = c2.getPulling() == h2;
            cy.empty = Speed.of(h2);
            cy.loadEmpty = c2.currentLoad();
            // Fifty-four full stacks: fillLevel 1.0.
            for (int i = 0; i < c2.getContainerSize(); i++) {
                c2.setItem(i, new ItemStack(Items.DIRT, 64));
            }
            DebugYardHerd.after(level, 60, () -> step(d.run, D_ALL, () -> {
                Horse h3 = find(level, d.horse);
                SupplyCartEntity c3 = cartOf(level, d);
                if (h3 == null || c3 == null) {
                    return;
                }
                cy.loadedStillHitched = c3.getPulling() == h3;
                cy.loaded = Speed.of(h3);
                cy.loadFull = c3.currentLoad();
                c3.setPulling(null);
                DebugYardHerd.after(level, 40, () -> step(d.run, D_ALL, () -> {
                    Horse h4 = find(level, d.horse);
                    if (h4 == null) {
                        return;
                    }
                    cy.after = Speed.of(h4);
                    ActionTrace.log("test yard", "DRAUGHT: " + cy.line());
                    SupplyCartEntity c4 = cartOf(level, d);
                    if (c4 != null) {
                        c4.clearContent();
                    }
                    if (n < CYCLES) {
                        DebugYardHerd.after(level, 20, () -> step(d.run, D_ALL, () -> cycle(level, d, n + 1)));
                    } else {
                        draughtVerdicts(d);
                    }
                }));
            }));
        }));
    }

    private static void draughtVerdicts(Draught d) {
        Speed b = d.baseline;
        String all = d.readings();
        if (b == null) {
            return;
        }
        // Any read missing, or the cart off before a hitched read: the setup, not the claim.
        for (Cycle c : d.cycles) {
            if (c.empty == null || c.loaded == null || c.after == null) {
                for (String x : D_ALL) {
                    inconclusive(x, "cycle " + c.n + " is missing a read; " + all);
                }
                return;
            }
        }
        boolean offEarly = false;
        for (Cycle c : d.cycles) {
            offEarly |= !c.emptyStillHitched || !c.loadedStillHitched;
        }
        if (offEarly) {
            inconclusive(D_SLOW, "the cart came off the horse by itself before a hitched read (pulledTick's"
                    + " distance or wall test) - a hitched read is not one; " + all);
            inconclusive(D_LOAD, "the cart came off by itself before a hitched read; " + all);
        }

        // SLOWED
        boolean slowAll = true;
        StringBuilder slowWhy = new StringBuilder();
        for (Cycle c : d.cycles) {
            boolean slower = c.empty.value() < b.value() - 1e-9;
            if (!slower || !c.empty.pull()) {
                slowAll = false;
                slowWhy.append(" cycle ").append(c.n).append(": EMPTY ").append(f4(c.empty.value()))
                        .append(" vs baseline ").append(f4(b.value())).append(", pull ")
                        .append(c.empty.pull() ? "on" : "OFF").append(';');
            }
        }
        if (slowAll) {
            pass(D_SLOW, "every hitch slowed the horse: " + all);
        } else {
            fail(D_SLOW, "hitched (getPulling() was the horse at the read) and not slowed -" + slowWhy
                    + " applyDraught is not firing on the hitch; " + all);
        }

        // LOADED
        boolean loadAll = true;
        StringBuilder loadWhy = new StringBuilder();
        for (Cycle c : d.cycles) {
            boolean rose = c.loadFull > c.loadEmpty + 1e-9;
            boolean slower = c.loaded.value() < c.empty.value() - 1e-9;
            if (!rose || !slower) {
                loadAll = false;
                loadWhy.append(" cycle ").append(c.n).append(": load ").append(f4(c.loadEmpty)).append(" -> ")
                        .append(f4(c.loadFull)).append(", speed ").append(f4(c.empty.value())).append(" -> ")
                        .append(f4(c.loaded.value())).append(';');
            }
        }
        if (loadAll) {
            pass(D_LOAD, "every loaded read was slower than the empty one, load " + f4(d.cycles.get(0).loadEmpty)
                    + " -> " + f4(d.cycles.get(0).loadFull) + "; " + all);
        } else {
            fail(D_LOAD, "loading the hitched cart did not slow the horse further:" + loadWhy
                    + " (a load that did not move is currentLoad/fillLevel; a load that moved with the speed"
                    + " unchanged is refreshDraught); " + all);
        }

        // RESTORE
        Set<String> baseOthers = b.others();
        StringBuilder foreign = new StringBuilder();
        StringBuilder leak = new StringBuilder();
        double emptyMin = Double.MAX_VALUE;
        double emptyMax = -Double.MAX_VALUE;
        double loadedMin = Double.MAX_VALUE;
        double loadedMax = -Double.MAX_VALUE;
        for (Cycle c : d.cycles) {
            for (Speed s : new Speed[] {c.empty, c.loaded, c.after}) {
                if (!s.others().equals(baseOthers)) {
                    foreign.append(" cycle ").append(c.n).append(" ids ").append(s.others()).append(';');
                }
            }
            if (c.after.pull() || Math.abs(c.after.value() - b.value()) > 1e-9 || !c.after.ids().equals(b.ids())
                    || Math.abs(c.after.base() - b.base()) > 1e-12) {
                leak.append(" cycle ").append(c.n).append(": AFTER ").append(c.after).append(';');
            }
            emptyMin = Math.min(emptyMin, c.empty.value());
            emptyMax = Math.max(emptyMax, c.empty.value());
            loadedMin = Math.min(loadedMin, c.loaded.value());
            loadedMax = Math.max(loadedMax, c.loaded.value());
        }
        boolean compounding = emptyMax - emptyMin > 1e-9 || loadedMax - loadedMin > 1e-9;
        String spread = String.format(Locale.ROOT, "EMPTY spread %.2e, LOADED spread %.2e over %d cycles",
                emptyMax - emptyMin, loadedMax - loadedMin, d.cycles.size());
        if (leak.length() == 0 && !compounding) {
            pass(D_RESTORE, "every unhitch gave back exactly the baseline " + f4(b.value()) + " with the baseline's"
                    + " modifier ids " + b.ids() + "; " + spread + "; " + all);
        } else if (foreign.length() > 0) {
            inconclusive(D_RESTORE, "a modifier other than the pull one came or went between baseline and a read"
                    + " (baseline others " + baseOthers + ":" + foreign + ") - something else touched the attribute;"
                    + " leaks seen:" + (leak.length() == 0 ? " none" : leak) + "; " + spread + "; " + all);
        } else {
            fail(D_RESTORE, (leak.length() > 0 ? "after unhitching the speed is not the baseline:" + leak : "")
                    + (compounding ? " the hitched speed changed from cycle to cycle (compounding)" : "") + "; "
                    + spread + "; " + all);
        }
    }

    // ------------------------------------------------------------------
    // EAST - STALLION DAY and FRESHER
    // ------------------------------------------------------------------

    /*
     * STALLION DAY (wiki/fertility.html, Verification tab, "A stallion's day is not capped"):
     *   "One stallion, six mares. Pen one entire stallion with six tamed mares and keep the total under
     *    fertility.nearby_horse_cap (50 by default) or that will stop it first, which is a different bullet. Run a
     *    few in-game days with debug.tools on so a heat is a minute. Pass: natural cover: lines under fertility in
     *    the log naming more than three different mares inside one day, with (chance ...) visibly lower on the
     *    later ones. Fail: exactly three a day, which would mean something else is still capping him - check it
     *    is not the crowding cap by counting the pen."
     *
     * HOW HIS DAY IS COUNTED, READ FROM THE CODE. ReproHandler.tryConceive takes covers =
     * Reproduction.coversOn(reproTime(stallion), timing.dayTicks()) - his coverDay/covers pair, zeroed when
     * floorDiv(now, dayTicks) is not coverDay - hands Conception.attempt ServerConfig.stallionDay(covers), whose
     * factor() is TIRED_STALLION_FACTOR once covers >= fertility.free_covers_per_day, and then recordCover(him).
     * With debug.tools on, dayTicks is ServerConfig.DEBUG_REPRO_DAY_TICKS (1,200): the stallion's day is one
     * minute of the reproductive clock (HorseRealmRepro.reproTime, the game time outside the realm). The pen
     * reads timing.dayTicks() at run time and names it.
     *
     * THE PEN (one YardPens group, 7 horses, far below the crowding cap): a tamed stallion in the middle and six
     * tamed mares round him at about three blocks, held out of heat at build. The clock waits for the start of a
     * reproductive day (day position 60) and puts all six in heat with DebugYardFertility.inHeat, so a whole
     * heat-up lands inside one day; from then on a mare found in DIESTRUS is put back in heat. Foals (matched by
     * their record's motherId) are taken away two minutes after first seen, so the cap never binds.
     *
     * WHAT A LOOK READS (every 5 ticks). A cover is a mare's lastNaturalTry moving (NaturalBreedingHandler.cover
     * stamps it before the roll). New covers in a look are put in stamp order and numbered against the
     * stallion's own counter: "before" is his covers today going into that cover, carried from his record at the
     * previous look. After the look his record must show exactly those covers (coverDay = the day, covers = the
     * running count); fewer means a cover was not counted against his day - and an uncounted cover is one that
     * never makes his odds drop, a FAIL. For each cover the pen computes the chance the rules give - the mare's
     * stage chance at the stamp (ReproRules.baseChance on her record from the look before), her FertilityGene
     * mareFactor, his allele factor, and ServerConfig.stallionDay(before).factor() - beside what a fresh
     * stallion would have had, and logs both. The chance actually rolled is not kept in any state; it is in the
     * handler's own "[fertility] natural cover: ... (chance X)" line, which sits right beside each of these in the
     * log for the reader to match one for one.
     *
     * PASS: on some reproductive day more than the free number (and at least four) DIFFERENT mares were
     * covered, every cover on that day past the free number went in with his counter
     * at or past the free number (so ServerConfig.stallionDay(...).tired() and the computed chance is
     * TIRED_STALLION_FACTOR x the fresh one), and his counter agreed with the covers seen at every look so far.
     * FAIL: his counter is behind the covers seen (immediately); or at 90 minutes, covers on at least two days
     * and never more than exactly the free number of different mares in any one. INCONCLUSIVE: a horse gone,
     * free_covers_per_day of six or more (six mares cannot get past it), or at 90 minutes too few covers to tell.
     *
     * FRESHER (same page, same tab):
     *   "She takes the fresher stallion. Two entire stallions and a mare in heat, one stallion already past three
     *    covers today and standing nearer than the other. Pass: the natural cover: line names the further, rested
     *    stallion. Fail: it names the nearer tired one, which would mean the gathering in NaturalBreedingHandler
     *    is handing NaturalCover a stale coversToday rather than the decision being wrong - the decision itself
     *    is tested."
     *
     * THE PEN (its own YardPens group): a glass box, walls two high, no lid, 4x4 inside (x0+40..43,
     * z0+4..7). TIRED stallion at (0.75, 0.75) of the inside, the mare at (0.75, 2.25) - 1.5 blocks from him -
     * and FRESH stallion at (2.75, 3.25), 2.24 from her: both strictly inside NATURAL_REACH (3), the tired one
     * nearer. All three are held on their marks (snapped back if they stray a fifth of a block) every look,
     * because HeatAttraction and wandering would otherwise decide who is "in reach" instead of the rule.
     * TIRED is made tired through the attachment: his Reproduction gets withCover(now, dayTicks) until
     * coversOn(now) is the free number - re-done every look, because his day rolls over every minute.
     * THE DAY BOUNDARY. For the few ticks after a day turns, before the next look tops him up, he would read
     * fresh. So the mare is only put in heat early in a day (day position 60 onward, and only while the half heat
     * inHeat gives her ends 100 ticks before the day does), and taken out of heat (outOfHeat) if she is still
     * receptive in the last 100 ticks of a day - she can never be covered across a boundary.
     *
     * WHAT A LOOK READS (every 10 ticks): both stallions' Reproduction (coverDay, covers) against the last look's,
     * after the pen's own top-up of TIRED; the mare's lastNaturalTry; distances; and the handler's path test
     * (createPath(stallion, 1).canReach(), else the 0.1-inflated hitbox touch) to each stallion.
     * PASS: the mare's stamp moved and FRESH's record took the cover (TIRED's did not). FAIL: TIRED's record took
     * it while FRESH was in reach and passed the path test at the look before. INCONCLUSIVE: TIRED took it but
     * FRESH was out of reach or failed the path test (then the gathering filtered him, which is a layout fault,
     * not this question); neither stallion's record moved; a horse gone; or 90 minutes with no cover.
     */

    private static final String S_DAY = "STALLION DAY - one stallion with six mares in heat covers more than his"
            + " free number of different mares inside one reproductive day, counting every cover so his later odds"
            + " are TIRED_STALLION_FACTOR x fresh";
    private static final String S_FRESH = "STALLION DAY FRESHER - a mare in heat with a tired stallion nearer and a"
            + " fresh one further, both in reach, is covered by the fresh one";

    private static final long S_DEADLINE = 90L * 1200;
    private static final long FOAL_KEEP = 2400L;
    private static final int GONE_LOOKS = 60;

    /** The day position, on a horse's own reproductive clock. */
    private static long dayPos(Horse h, ReproTiming t) {
        return Math.floorMod(HorseRealmRepro.reproTime(h), t.dayTicks());
    }

    /** Ticks until the given position in the next (or this) reproductive day. */
    private static long ticksUntilPos(Horse h, ReproTiming t, long pos) {
        long now = dayPos(h, t);
        return Math.floorMod(pos - now, t.dayTicks());
    }

    private static boolean keepInHeat(Horse mare) {
        if (ReproHandler.stateOf(mare) == ReproState.DIESTRUS) {
            DebugYardFertility.inHeat(mare);
            return true;
        }
        return false;
    }

    private static void east(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        DebugYardClockwork.expect(S_DAY);
        DebugYardClockwork.expect(S_FRESH);
        stallionDay(level, gy, x0, z0, thisRun);
        fresher(level, gy, x0, z0, thisRun);
    }

    // ---- STALLION DAY ----

    private record Cover(UUID mare, String name, long stamp, long day, int before, double base, double chance,
                         double fresh, boolean tired) {

        String line() {
            return String.format(Locale.ROOT, "%s at %d (day %d) - his %s cover today, base %.2f, chance %.4f"
                            + " (fresh would be %.4f, ratio %.3f)%s", name, stamp, day, ordinal(before + 1), base,
                    chance, fresh, fresh > 0 ? chance / fresh : Double.NaN, tired ? " TIRED" : "");
        }
    }

    private static String ordinal(int n) {
        return n + (n % 100 >= 11 && n % 100 <= 13 ? "th"
                : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th");
    }

    private static final class Paddock {
        final int run;
        final AABB box;
        @Nullable UUID stud;
        final List<UUID> mares = new ArrayList<>();
        final Map<UUID, String> names = new LinkedHashMap<>();
        final Set<UUID> damRecords = new HashSet<>();
        final Map<UUID, Reproduction> mareSnap = new LinkedHashMap<>();
        long runDay = Long.MIN_VALUE;
        int runCount;
        int free;
        long start;
        int looks;
        int reheats;
        int missing;
        int counterBehind;
        int counterAhead;
        final Map<Long, LinkedHashSet<UUID>> distinctByDay = new TreeMap<>();
        final Map<Long, List<Cover>> coversByDay = new TreeMap<>();
        final Map<UUID, Long> living = new LinkedHashMap<>();
        final Set<UUID> seen = new HashSet<>();
        int foals;

        Paddock(int run, AABB box) {
            this.run = run;
            this.box = box;
        }

        int maxDistinct() {
            int m = 0;
            for (Set<UUID> s : distinctByDay.values()) {
                m = Math.max(m, s.size());
            }
            return m;
        }

        String counts() {
            StringBuilder sb = new StringBuilder();
            sb.append(looks).append(" looks, ").append(reheats).append(" re-heats, ").append(foals)
                    .append(" foals taken away; free covers ").append(free).append(", day ")
                    .append(ServerConfig.reproTiming().dayTicks()).append(" ticks; counter behind ")
                    .append(counterBehind).append(", ahead ").append(counterAhead).append("; distinct mares per day ");
            StringBuilder per = new StringBuilder();
            for (Map.Entry<Long, LinkedHashSet<UUID>> e : distinctByDay.entrySet()) {
                per.append(per.length() == 0 ? "" : ", ").append("day ").append(e.getKey()).append(": ")
                        .append(e.getValue().size()).append(" of ").append(coversByDay.get(e.getKey()).size())
                        .append(" covers");
            }
            sb.append(per.length() == 0 ? "(no covers)" : per);
            return sb.toString();
        }

        String dayLine(long day) {
            StringBuilder sb = new StringBuilder("day " + day + ":");
            for (Cover c : coversByDay.getOrDefault(day, List.of())) {
                sb.append(" [").append(c.line()).append(']');
            }
            return sb.toString();
        }
    }

    private static void stallionDay(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        int xa = x0 + 25;
        int xb = x0 + 37;
        int z1 = z0 + 12;
        DebugTestYard.fencedPlot(level, gy, xa, xb, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(xa + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("STALLION DAY", "1 stallion", "6 mares in heat", "> 3 in a day?"));
        YardPens.register(gy, xa, xb, z0, z1, "BC STALLION DAY");

        Paddock p = new Paddock(thisRun, DebugTestYard.box(xa, gy, z0, xb, gy + 4, z1));
        double cx = xa + 6.5;
        double cz = z0 + 6.5;
        Horse stud = DebugYardClockwork.horse(level, gy, cx, cz, Sex.MALE, "", "DAY STUD");
        p.stud = idOf(stud);
        double[][] spots = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {-3.5, 0}, {3.5, 0}};
        for (int i = 0; i < spots.length; i++) {
            Horse m = DebugYardClockwork.horse(level, gy, cx + spots[i][0], cz + spots[i][1], Sex.FEMALE, "",
                    "DAY MARE " + (i + 1));
            if (m != null) {
                p.mares.add(m.getUUID());
                DebugYardFertility.outOfHeat(m);
            }
        }

        DebugYardHerd.after(level, 40, () -> step(p.run, List.of(S_DAY), () -> {
            Horse s = find(level, p.stud);
            int present = 0;
            for (UUID id : p.mares) {
                if (find(level, id) != null) {
                    present++;
                }
            }
            if (s == null || present < 6) {
                inconclusive(S_DAY, "at build: the pen is not stocked - stallion " + (s != null) + ", " + present
                        + " of 6 mares");
                return;
            }
            p.free = ServerConfig.freeCoversPerDay();
            if (p.free >= 6) {
                inconclusive(S_DAY, "fertility.free_covers_per_day is " + p.free + ": six mares cannot take him past"
                        + " it, so the tired odds are never reached");
                return;
            }
            ReproTiming t = ServerConfig.reproTiming();
            long wait = ticksUntilPos(s, t, 60);
            ActionTrace.log("test yard", "STALLION DAY: day is " + t.dayTicks() + " ticks, free covers " + p.free
                    + "; mares go into heat in " + wait + " ticks, at the start of a day");
            DebugYardHerd.after(level, wait, () -> step(p.run, List.of(S_DAY), () -> paddockStart(level, p)));
        }));
        DebugYardHerd.after(level, S_DEADLINE + 2_400L, () -> step(p.run, List.of(S_DAY), () -> {
            if (!answered(S_DAY)) {
                paddockVerdict(p);
            }
        }));
    }

    private static void paddockStart(ServerLevel level, Paddock p) {
        Horse s = find(level, p.stud);
        if (s == null) {
            inconclusive(S_DAY, "the stallion is gone before the start");
            return;
        }
        p.start = level.getGameTime();
        Reproduction sr = ReproHandler.of(s);
        p.runDay = sr.coverDay();
        p.runCount = sr.covers();
        for (UUID id : p.mares) {
            Horse m = find(level, id);
            if (m != null) {
                DebugYardFertility.inHeat(m);
                p.mareSnap.put(id, ReproHandler.of(m));
                HorseRecord mr = HorseRecords.of(m);
                p.names.put(id, mr.displayName());
                p.damRecords.add(mr.id());
            }
        }
        ActionTrace.log("test yard", "STALLION DAY: start - stud " + HorseRecords.of(s).displayName()
                + ", mares " + p.names.values() + ", all in heat at day position " + dayPos(s,
                ServerConfig.reproTiming()));
        paddockLook(level, p);
    }

    private static void paddockLook(ServerLevel level, Paddock p) {
        DebugYardHerd.after(level, 5, () -> step(p.run, List.of(S_DAY), () -> {
            long now = level.getGameTime();
            if (answered(S_DAY)) {
                paddockFinish(level, p);
                return;
            }
            paddockLook(level, p);      // first, so a throw below cannot stop the clock
            p.looks++;
            ReproTiming t = ServerConfig.reproTiming();
            Horse s = find(level, p.stud);
            List<Horse> present = new ArrayList<>();
            for (UUID id : p.mares) {
                Horse m = find(level, id);
                if (m != null) {
                    present.add(m);
                }
            }
            if (s == null || present.size() < p.mares.size()) {
                if (++p.missing >= GONE_LOOKS) {
                    inconclusive(S_DAY, "at " + minutes(now - p.start) + ": the stallion or a mare is gone ("
                            + present.size() + " of " + p.mares.size() + " mares); " + p.counts());
                }
                return;
            }
            p.missing = 0;

            // New covers, in stamp order.
            List<Object[]> fresh = new ArrayList<>();
            for (Horse m : present) {
                Reproduction r = ReproHandler.of(m);
                Reproduction prev = p.mareSnap.get(m.getUUID());
                if (prev != null && r.lastNaturalTry() != prev.lastNaturalTry()
                        && r.lastNaturalTry() != Long.MAX_VALUE) {
                    fresh.add(new Object[] {m, prev, r.lastNaturalTry()});
                }
            }
            fresh.sort(Comparator.comparingLong(o -> (Long) o[2]));
            HorseRecord studRec = HorseRecords.of(s);
            double sireAllele = Genes.FERTILITY.alleleFactor((studRec.hasGenome() ? studRec.genome().genotype()
                    : studRec.genotype()).pair(Genes.FERTILITY));
            Set<Long> touched = new LinkedHashSet<>();
            for (Object[] o : fresh) {
                Horse m = (Horse) o[0];
                Reproduction prev = (Reproduction) o[1];
                long stamp = (Long) o[2];
                long day = Math.floorDiv(stamp, t.dayTicks());
                int before = day == p.runDay ? p.runCount : 0;
                p.runDay = day;
                p.runCount = before + 1;
                HorseRecord mr = HorseRecords.of(m);
                double base = ReproRules.baseChance(prev, stamp, t);
                double mf = mr.hasGenome() ? Genes.FERTILITY.mareFactor(mr.genome()) : Double.NaN;
                StallionDay sd = ServerConfig.stallionDay(before);
                double chance = ReproRules.conceptionChance(base, mf, sireAllele * sd.factor());
                double freshChance = ReproRules.conceptionChance(base, mf, sireAllele);
                Cover c = new Cover(m.getUUID(), p.names.getOrDefault(m.getUUID(), mr.displayName()), stamp, day,
                        before, base, chance, freshChance, sd.tired());
                p.coversByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(c);
                p.distinctByDay.computeIfAbsent(day, k -> new LinkedHashSet<>()).add(m.getUUID());
                touched.add(day);
                ActionTrace.log("test yard", "STALLION DAY: cover - " + c.line() + "; " + p.distinctByDay.get(day)
                        .size() + " different mares today");
            }

            // His own counter must show exactly what was seen.
            Reproduction sr = ReproHandler.of(s);
            if (!fresh.isEmpty()) {
                if (sr.coverDay() == p.runDay && sr.covers() < p.runCount || sr.coverDay() < p.runDay) {
                    p.counterBehind++;
                    fail(S_DAY, "at " + minutes(now - p.start) + ": the stallion's record shows coverDay "
                            + sr.coverDay() + ", covers " + sr.covers() + " but " + p.runCount + " cover(s) were seen"
                            + " on day " + p.runDay + " - a cover not counted against his day never halves his"
                            + " odds; " + p.dayLine(p.runDay) + "; " + p.counts());
                } else if (sr.coverDay() != p.runDay || sr.covers() != p.runCount) {
                    p.counterAhead++;
                    ActionTrace.log("test yard", "STALLION DAY: note - his record (day " + sr.coverDay() + ", "
                            + sr.covers() + ") is ahead of the covers seen (day " + p.runDay + ", " + p.runCount
                            + "); carrying on from his record");
                }
            }
            p.runDay = sr.coverDay();
            p.runCount = sr.covers();

            // Keep them in heat, then remember where each one stands for the next look.
            for (Horse m : present) {
                if (keepInHeat(m)) {
                    p.reheats++;
                }
                p.mareSnap.put(m.getUUID(), ReproHandler.of(m));
            }
            foals(level, p, now);

            for (long day : touched) {
                Set<UUID> distinct = p.distinctByDay.get(day);
                if (distinct.size() > p.free && distinct.size() >= 4 && p.counterBehind == 0) {
                    boolean allTired = true;
                    for (Cover c : p.coversByDay.get(day)) {
                        if (c.before() >= p.free && !c.tired()) {
                            allTired = false;
                        }
                    }
                    if (allTired) {
                        pass(S_DAY, "at " + minutes(now - p.start) + ": " + distinct.size() + " different mares"
                                + " covered on day " + day + " (free covers " + p.free + "), every cover past the"
                                + " free ones counted against him and on TIRED_STALLION_FACTOR "
                                + ReproRules.TIRED_STALLION_FACTOR + " x the fresh chance - match each against the"
                                + " handler's own natural cover: (chance X) line; " + p.dayLine(day) + "; "
                                + p.counts());
                    }
                }
            }
            if (!answered(S_DAY) && now - p.start >= S_DEADLINE) {
                paddockVerdict(p);
            }
        }));
    }

    private static void paddockVerdict(Paddock p) {
        int days = p.distinctByDay.size();
        int max = p.maxDistinct();
        StringBuilder all = new StringBuilder();
        for (long day : p.coversByDay.keySet()) {
            all.append(" ").append(p.dayLine(day)).append(';');
        }
        if (days >= 2 && max == p.free) {
            fail(S_DAY, "at 90 min: covers on " + days + " days and never more than exactly " + p.free
                    + " different mares in one - something still caps him (the pen holds 7 horses, far under the"
                    + " crowding cap " + ServerConfig.nearbyHorseCap() + ");" + all + " " + p.counts());
        } else {
            inconclusive(S_DAY, "at 90 min: no day reached more than " + p.free + " different mares (best "
                    + max + " over " + days + " days with covers), but not a steady cap at the free number"
                    + " either;" + all + " " + p.counts());
        }
    }

    private static void foals(ServerLevel level, Paddock p, long now) {
        for (Horse foal : level.getEntitiesOfClass(Horse.class, p.box.inflate(0.0, 2.0, 0.0),
                h -> h.isBaby() && h.isAlive() && HorseRecords.hasRealRecord(h))) {
            UUID id = foal.getUUID();
            if (p.seen.contains(id)) {
                continue;
            }
            UUID mother = HorseRecords.of(foal).motherId().orElse(null);
            if (mother == null || !p.damRecords.contains(mother)) {
                continue;
            }
            p.seen.add(id);
            p.foals++;
            p.living.put(id, now);
        }
        for (var it = p.living.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (now - e.getValue() >= FOAL_KEEP) {
                Horse h = find(level, e.getKey());
                if (h != null) {
                    h.discard();    // out of the way of the crowding cap
                }
                it.remove();
            }
        }
    }

    /** Answered: take the foals away and stop the paddock breeding all night. */
    private static void paddockFinish(ServerLevel level, Paddock p) {
        for (UUID id : p.living.keySet()) {
            Horse h = find(level, id);
            if (h != null) {
                h.discard();
            }
        }
        p.living.clear();
        for (UUID id : p.mares) {
            DebugYardFertility.noNaturalCovers(find(level, id));
        }
    }

    // ---- FRESHER ----

    private static final class Fresher {
        final int run;
        final int gy;
        @Nullable UUID mare;
        @Nullable UUID tired;
        @Nullable UUID fresh;
        final double[] mareSpot;
        final double[] tiredSpot;
        final double[] freshSpot;
        Reproduction tiredSnap;
        Reproduction freshSnap;
        long mareTry;
        long start;
        int looks;
        int heatLooks;
        int bothInReach;
        int freshPath;
        int freshTouch;
        int freshNeither;
        int topUps;
        int heats;
        int outs;
        int missing;
        boolean lastFreshOk;
        boolean lastFreshInReach;
        double lastTiredDist = -1;
        double lastFreshDist = -1;

        Fresher(int run, int gy, double[] mareSpot, double[] tiredSpot, double[] freshSpot) {
            this.run = run;
            this.gy = gy;
            this.mareSpot = mareSpot;
            this.tiredSpot = tiredSpot;
            this.freshSpot = freshSpot;
        }

        String counts() {
            return String.format(Locale.ROOT, "%d looks (%d with her able to be covered): both stallions in reach"
                            + " %d, fresh stallion path %d / touching only %d / NEITHER %d; last distances tired %.2f,"
                            + " fresh %.2f; tired topped up %d times; heats started %d, taken out %d", looks,
                    heatLooks, bothInReach, freshPath, freshTouch, freshNeither, lastTiredDist, lastFreshDist,
                    topUps, heats, outs);
        }
    }

    private static void fresher(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        int bx0 = x0 + 39;
        int bx1 = x0 + 44;
        int bz0 = z0 + 3;
        int bz1 = z0 + 8;
        // A glass box, walls two high, no lid (a lid would put out a yard lamp and darken the floor).
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = bx0; x <= bx1; x++) {
            for (int z = bz0; z <= bz1; z++) {
                boolean wall = x == bx0 || x == bx1 || z == bz0 || z == bz1;
                for (int y = gy + 1; y <= gy + 2; y++) {
                    level.setBlock(new BlockPos(x, y, z), wall ? glass : air, 3);
                }
            }
        }
        DebugPenManager.placeSign(level, new BlockPos(bx0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("FRESHER", "tired stud near,", "fresh one far:", "she takes fresh?"));
        YardPens.register(gy, bx0, bx1, bz0, bz1, "BC FRESHER");

        // The inside is x bx0+1 .. bx1 (exclusive), z bz0+1 .. bz1: 4 x 4.
        double ix = bx0 + 1;
        double iz = bz0 + 1;
        Fresher f = new Fresher(thisRun, gy, new double[] {ix + 0.75, iz + 2.25},
                new double[] {ix + 0.75, iz + 0.75}, new double[] {ix + 2.75, iz + 3.25});
        Horse tired = DebugYardClockwork.horse(level, gy, f.tiredSpot[0], f.tiredSpot[1], Sex.MALE, "",
                "TIRED STUD");
        Horse fresh = DebugYardClockwork.horse(level, gy, f.freshSpot[0], f.freshSpot[1], Sex.MALE, "",
                "FRESH STUD");
        Horse mare = DebugYardClockwork.horse(level, gy, f.mareSpot[0], f.mareSpot[1], Sex.FEMALE, "",
                "FRESHER MARE");
        f.tired = idOf(tired);
        f.fresh = idOf(fresh);
        f.mare = idOf(mare);
        if (mare != null) {
            DebugYardFertility.outOfHeat(mare);
        }

        DebugYardHerd.after(level, 40, () -> step(f.run, List.of(S_FRESH), () -> {
            Horse m = find(level, f.mare);
            Horse t = find(level, f.tired);
            Horse s = find(level, f.fresh);
            if (m == null || t == null || s == null) {
                inconclusive(S_FRESH, "at build: mare " + (m != null) + ", tired stallion " + (t != null)
                        + ", fresh stallion " + (s != null));
                return;
            }
            f.start = level.getGameTime();
            topUp(t, f);
            f.tiredSnap = ReproHandler.of(t);
            f.freshSnap = ReproHandler.of(s);
            f.mareTry = ReproHandler.of(m).lastNaturalTry();
            ReproTiming tm = ServerConfig.reproTiming();
            ActionTrace.log("test yard", String.format(Locale.ROOT, "STALLION DAY FRESHER: start - mare %.2f from"
                            + " TIRED (covers today %d of free %d) and %.2f from FRESH (covers today %d); day %d"
                            + " ticks", Math.sqrt(m.distanceToSqr(t)),
                    f.tiredSnap.coversOn(HorseRealmRepro.reproTime(t), tm.dayTicks()), ServerConfig.freeCoversPerDay(),
                    Math.sqrt(m.distanceToSqr(s)), f.freshSnap.coversOn(HorseRealmRepro.reproTime(s), tm.dayTicks()),
                    tm.dayTicks()));
            fresherLook(level, f);
        }));
        DebugYardHerd.after(level, S_DEADLINE + 2_400L, () -> step(f.run, List.of(S_FRESH), () ->
                inconclusive(S_FRESH, "at 90 min: never covered; " + f.counts())));
    }

    /** His covers today up to the free number, through the attachment. True if anything was written. */
    private static boolean topUp(Horse stallion, Fresher f) {
        ReproTiming t = ServerConfig.reproTiming();
        long now = HorseRealmRepro.reproTime(stallion);
        int free = ServerConfig.freeCoversPerDay();
        Reproduction r = ReproHandler.of(stallion);
        boolean wrote = false;
        while (r.coversOn(now, t.dayTicks()) < free) {
            r = r.withCover(now, t.dayTicks());
            wrote = true;
        }
        if (wrote) {
            ReproHandler.set(stallion, r);
            f.topUps++;
        }
        return wrote;
    }

    private static void fresherLook(ServerLevel level, Fresher f) {
        DebugYardHerd.after(level, 10, () -> step(f.run, List.of(S_FRESH), () -> {
            if (answered(S_FRESH)) {
                DebugYardFertility.noNaturalCovers(find(level, f.mare));
                return;
            }
            fresherLook(level, f);      // first, so a throw below cannot stop the clock
            long now = level.getGameTime();
            String at = "at " + minutes(now - f.start) + ": ";
            Horse m = find(level, f.mare);
            Horse t = find(level, f.tired);
            Horse s = find(level, f.fresh);
            if (m == null || t == null || s == null) {
                if (++f.missing >= GONE_LOOKS / 2) {
                    inconclusive(S_FRESH, at + "a horse is gone (mare " + (m != null) + ", tired " + (t != null)
                            + ", fresh " + (s != null) + "); " + f.counts());
                }
                return;
            }
            f.missing = 0;
            f.looks++;

            // 1. Did anybody cover her since the last look?
            Reproduction mr = ReproHandler.of(m);
            Reproduction tr = ReproHandler.of(t);
            Reproduction sr = ReproHandler.of(s);
            boolean tiredMoved = tr.coverDay() != f.tiredSnap.coverDay() || tr.covers() != f.tiredSnap.covers();
            boolean freshMoved = sr.coverDay() != f.freshSnap.coverDay() || sr.covers() != f.freshSnap.covers();
            if (mr.lastNaturalTry() != f.mareTry || tiredMoved || freshMoved) {
                String what = String.format(Locale.ROOT, "mare's lastNaturalTry %d -> %d; TIRED record (day %d,"
                                + " %d) -> (day %d, %d); FRESH record (day %d, %d) -> (day %d, %d); at the look before"
                                + " she was %.2f from TIRED and %.2f from FRESH, FRESH %s and %s the path test; %s",
                        f.mareTry, mr.lastNaturalTry(), f.tiredSnap.coverDay(), f.tiredSnap.covers(), tr.coverDay(),
                        tr.covers(), f.freshSnap.coverDay(), f.freshSnap.covers(), sr.coverDay(), sr.covers(),
                        f.lastTiredDist, f.lastFreshDist, f.lastFreshInReach ? "in reach" : "OUT of reach",
                        f.lastFreshOk ? "passing" : "FAILING", f.counts());
                if (freshMoved && !tiredMoved) {
                    pass(S_FRESH, at + "she was covered by the further, rested stallion; " + what);
                } else if (tiredMoved && !freshMoved) {
                    if (f.lastFreshInReach && f.lastFreshOk) {
                        fail(S_FRESH, at + "she was covered by the nearer, TIRED stallion while the fresh one was in"
                                + " reach and reachable - the gathering handed NaturalCover a stale coversToday, or"
                                + " the preference is wrong; " + what);
                    } else {
                        inconclusive(S_FRESH, at + "the tired stallion covered her, but the fresh one was out of"
                                + " reach or failed the path test, so he was never a candidate - the layout, not"
                                + " the choice; " + what);
                    }
                } else {
                    inconclusive(S_FRESH, at + "her stamp moved but the stallions' records do not say which one"
                            + " covered (both or neither moved); " + what);
                }
                return;
            }

            // 2. Hold everyone on their marks, keep TIRED tired, and run her heat inside the day.
            hold(m, f.gy, f.mareSpot[0], f.mareSpot[1]);
            hold(t, f.gy, f.tiredSpot[0], f.tiredSpot[1]);
            hold(s, f.gy, f.freshSpot[0], f.freshSpot[1]);
            topUp(t, f);
            ReproTiming tm = ServerConfig.reproTiming();
            long pos = dayPos(m, tm);
            long half = tm.estrusTicks() / 2;
            boolean fits = 60 + half <= tm.dayTicks() - 100;
            ReproState state = ReproHandler.stateOf(m);
            if (pos >= tm.dayTicks() - 100) {
                if (state.receptive()) {
                    DebugYardFertility.outOfHeat(m);
                    f.outs++;
                }
            } else if (state == ReproState.DIESTRUS && pos >= 60 && (!fits || pos + half <= tm.dayTicks() - 100)) {
                DebugYardFertility.inHeat(m);
                f.heats++;
            }
            f.tiredSnap = ReproHandler.of(t);
            f.freshSnap = ReproHandler.of(s);
            f.mareTry = ReproHandler.of(m).lastNaturalTry();

            // 3. What the handler would see now: both in its search box, and FRESH's path test.
            f.lastTiredDist = Math.sqrt(m.distanceToSqr(t));
            f.lastFreshDist = Math.sqrt(m.distanceToSqr(s));
            AABB reach = m.getBoundingBox().inflate(ReproRules.NATURAL_REACH);
            boolean tIn = reach.intersects(t.getBoundingBox()) && m.distanceToSqr(t) < 9.0;
            boolean sIn = reach.intersects(s.getBoundingBox()) && m.distanceToSqr(s) < 9.0;
            f.lastFreshInReach = sIn;
            if (ReproRules.mayTryNaturally(ReproHandler.of(m), HorseRealmRepro.reproTime(m), tm)) {
                f.heatLooks++;
                if (tIn && sIn) {
                    f.bothInReach++;
                }
                Path path = m.getNavigation().createPath(s, 1);
                if (path != null && path.canReach()) {
                    f.freshPath++;
                    f.lastFreshOk = true;
                } else if (m.getBoundingBox().inflate(0.1).intersects(s.getBoundingBox())) {
                    f.freshTouch++;
                    f.lastFreshOk = true;
                } else {
                    f.freshNeither++;
                    f.lastFreshOk = false;
                }
            }
        }));
    }
}
