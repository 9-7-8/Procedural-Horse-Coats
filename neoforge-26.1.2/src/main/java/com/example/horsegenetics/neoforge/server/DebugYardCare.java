package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.VerdantGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row AV: two long care pens that answer for themselves overnight</b> (2026-10-01). West, BOND DECAY - the
 * daily bond fade, read straight off the attachment on a clock. East, VERDANT FLOORS - four glass cells, one
 * verdant horse each, every floor block snapshotted and compared an hour later.
 *
 * <p>Both halves start themselves at build, run on {@link DebugYardHerd#after}, and end in
 * {@code CLOCKWORK PASS|FAIL|INCONCLUSIVE} lines through {@link DebugYardClockwork#verdict}. Nothing here needs
 * a person: the horses are tamed with no owner (so no proximity, riding or hand-feeding bond can reach them),
 * and every horse's hunger is written back to {@link Hunger#FULL} every five minutes so none of them grazes.
 */
final class DebugYardCare {

    private DebugYardCare() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    /** Five minutes of game time: the reading interval of both pens. */
    private static final long READ_EVERY = 6_000L;
    /** One hour of game time. */
    private static final long HOUR = 72_000L;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        try {
            bondDecay(level, gy, x0, z0, myRun);
            verdantFloors(level, gy, x0 + 25, z0, myRun);
            ActionTrace.log("test yard", "row AV built (west: BOND DECAY; east: VERDANT FLOORS) at game tick "
                    + level.getGameTime() + ", day " + level.getGameTime() / HorseCareAttachment.DAY_TICKS);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AV (care pens) failed to build", e);
        }
    }

    // ==================================================================
    // WEST - BOND DECAY
    // ==================================================================

    /*
     * BOND DECAY (wiki/horse-care.html, "Bond fades to 31", 2026-09-24, NOT played). The open checks it answers:
     *
     *   "/bond 100 a horse, note the number on its information screen, leave it in a loaded pen and sleep
     *    through several nights. Pass: the number is down by one per night."
     *   "Load an existing world with horses you bonded before this build and read their numbers immediately.
     *    Pass: unchanged. Fail: they are all at exactly 31, which is the unstamped-horse guard not working."
     *   "Unloaded time is charged. ... Pass: the number dropped by the days you were away, all at once on the
     *    first tick."
     *   "The floor holds, and nothing is topped up. Leave a /bond 35 horse alone for a fortnight, and a /bond 10
     *    foal beside it. Pass: 31 and 10. Fail: the foal is at 31."
     *
     * WHAT A DAY IS, READ FROM THE CODE. HorseCareHandler.onHorseTick computes today = level.getGameTime() /
     * HorseCareAttachment.DAY_TICKS (24,000) - the GAME time, the ever-increasing tick counter, NOT the day time
     * that the daylight cycle, /time and sleeping move. So the yard's "day" is twenty real minutes of server
     * ticks whatever the dimension's sky does, and three days is one hour: this pen is NOT at the mercy of the
     * debug dimension's clock (the yard clock, DebugYardHerd.after, reads the same getGameTime). The flip side,
     * for the page: sleeping through a night skips day time only, so the wiki's "sleep through several nights.
     * Pass: down by one per night" will read NO decay for a slept night - a night slept costs no bond, a night
     * waited out costs one. UNVERIFIED for 26.1.2 that sleep leaves getGameTime alone (vanilla behaviour as
     * long known; not read out of this jar). That is a correction for the page, not something this pen judges.
     *
     * THE ARITHMETIC. Each 30-tick scan: if dayStamp == 0 and today > 0, the horse is stamped today and charged
     * nothing (the unstamped guard); else if today > dayStamp, bond = Bond.decayed(bond, today - dayStamp,
     * behaviour.bond_decay_per_day, behaviour.bond_floor) and the stamp moves to today. Bond.decayed: nothing
     * at or below the floor moves; otherwise bond - days * perDay, but never under the floor. The pen predicts
     * that with its own few lines (expect(), below), not by calling Bond.decayed - a prediction must not share
     * the code it is checking. Both config values are read at runtime through ServerConfig at setup and named
     * in every detail line.
     *
     * NOTHING ELSE MAY MOVE THE NUMBER. Bond rises only through HorseCareHandler.awardBond: owner proximity,
     * riding, hand-feeding (all need horse.getOwner() to be a loaded Player), a favourite food, shearing, and a
     * gene's bond effect (MusicEnjoyerGene). The horses are spawned by DebugPenManager.spawnHorse(tamed = true),
     * which sets the tamed flag and NO owner, so the first three cannot happen; nothing feeds or shears them;
     * and their genotype is verdant n/n with every other gene at its default. The floor is stone, wall to wall,
     * and the pen's water cauldron is filled in: no grass, no water. Hunger is written to FULL every reading, so
     * the HungerFoodGoal never sends anyone looking for food (and a starving horse is not a variable either).
     *
     * THE FIVE MARES, each written straight through the attachment at setup (HorseCareAttachment.with):
     *   H1 - bond 100, stamped today: falls by exactly perDay per elapsed day.
     *   H2 - bond 90, dayStamp 0 (what the code calls "never stamped"): its first tick stamps it and charges
     *        nothing. A broken guard would charge it for the world's whole age - to the floor in any old world.
     *   H3 - bond 100, stamped five days ago: its first tick charges all five days at once (100 - 5 * perDay).
     *   H4 - bond floor + 2: reaches the floor and stops there, never under it.
     *   H5 - a foal, bond below the floor (10 at the default floor of 31): never topped up to the floor.
     *
     * TIMING. The setup waits for a game tick that is 3,000 ticks into a five-minute quarter, so the writes and
     * every reading after them sit 3,000 ticks clear of any day boundary - a horse's 30-tick scan lag cannot put
     * a reading on the wrong side of one. If the world is younger than six days (a stamp five days back must not
     * land on 0, which would read as "never stamped"), it waits until day 6. H2 and H3 are read 100 ticks after
     * the writes (the "first tick" checks); H1, H4 and H5 every five minutes, FAIL at the first reading that
     * disagrees and PASS once three whole days (one hour) have elapsed. A one-line reading of all five is logged
     * every hour. Deadline: seven hours of game time after the build, every check still open answers
     * INCONCLUSIVE with the readings.
     *
     * PASS / FAIL, per check: the reading equals the predicted number (and for H4 is never under the floor and
     * ends on it; for H5 never moves). A horse gone, or one whose stamp shows it never ticked, is INCONCLUSIVE.
     * behaviour.bond_decay_per_day = 0 makes every check INCONCLUSIVE at setup (decay is off; nothing to see).
     */

    private static final String BOND = "BOND DECAY";
    private static final String CHECK_UNSTAMPED = BOND + " - an unstamped horse (dayStamp 0) is stamped on its first"
            + " tick and charged nothing, not snapped to the floor";
    private static final String CHECK_UNLOADED = BOND + " - a horse stamped five days ago is charged all five days"
            + " at once on its first tick";
    private static final String CHECK_DAILY = BOND + " - bond falls by exactly behaviour.bond_decay_per_day per"
            + " elapsed day";
    private static final String CHECK_FLOOR = BOND + " - decay stops at behaviour.bond_floor and never goes under it";
    private static final String CHECK_FOAL = BOND + " - a foal below the floor is never topped up to it";

    private static final int BOND_DAYS = 3;
    private static final long BOND_DEADLINE = 7 * HOUR;
    /** How far back H3's stamp is written. */
    private static final int H3_BACK = 5;

    private static final class BondPen {
        final int run;
        final long built;
        final Map<String, UUID> horses = new LinkedHashMap<>();
        final Map<String, Integer> start = new LinkedHashMap<>();
        final Map<String, StringBuilder> history = new LinkedHashMap<>();
        final Set<String> answered = new HashSet<>();
        int perDay;
        int floor;
        long day0;
        long t0;
        long h3Stamp;
        int lowestH4 = Integer.MAX_VALUE;
        int readings;

        BondPen(int run, long built) {
            this.run = run;
            this.built = built;
        }
    }

    private static void bondDecay(ServerLevel level, int gy, int x0, int z0, int myRun) {
        int x1 = x0 + 19;
        int z1 = z0 + 12;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        // Stone wall to wall, over the cauldron fencedPlot sank at (x0 + 1, z0 + 1): no grass, no water.
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = x0 + 1; x < x1; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("BOND DECAY", "1/day to the floor", "unstamped, 5 days", "back, floor, foal"));
        YardPens.register(gy, x0, x1, z0, z1, BOND);

        for (String c : List.of(CHECK_UNSTAMPED, CHECK_UNLOADED, CHECK_DAILY, CHECK_FLOOR, CHECK_FOAL)) {
            DebugYardClockwork.expect(c);
        }

        BondPen p = new BondPen(myRun, level.getGameTime());
        String code = VerdantGene.KEY + "=n/n";
        double[][] at = {{x0 + 3.5, z0 + 3.5}, {x0 + 9.5, z0 + 3.5}, {x0 + 15.5, z0 + 3.5},
                {x0 + 5.5, z0 + 8.5}, {x0 + 12.5, z0 + 8.5}};
        String[] tags = {"H1", "H2", "H3", "H4", "H5"};
        String[] what = {"100 today", "90 unstamped", "100 five days back", "floor + 2", "foal under floor"};
        for (int i = 0; i < tags.length; i++) {
            Horse h = DebugYardUnattended.horse(level, gy, at[i][0], at[i][1], Sex.FEMALE, code, true,
                    BOND + " " + tags[i] + " (" + what[i] + ")");
            if (h == null) {
                continue;
            }
            if ("H5".equals(tags[i])) {
                h.setBaby(true);
            }
            p.horses.put(tags[i], h.getUUID());
            p.history.put(tags[i], new StringBuilder());
        }

        long now = level.getGameTime();
        // The first tick 3,000 into a five-minute quarter, at least two seconds out so the horses have joined.
        long first = now + 40;
        long delay = 40 + Math.floorMod(3_000L - first, READ_EVERY);
        DebugYardHerd.after(level, delay, () -> bondSetup(level, p));
        DebugYardHerd.after(level, BOND_DEADLINE, () -> bondDeadline(level, p));
    }

    private static long today(ServerLevel level) {
        return level.getGameTime() / HorseCareAttachment.DAY_TICKS;
    }

    private static @Nullable Horse find(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static void write(Horse h, int bond, long stamp) {
        HorseCareAttachment care = HorseCareAttachment.DEFAULT.with(bond, Optional.empty(), 0, stamp, 0L, 0L);
        h.setData(ModAttachments.HORSE_CARE.get(), care);
        HorseCareHandler.syncCare(h, care);
    }

    private static void feed(Horse h) {
        h.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
    }

    /** What the code's rule should leave, worked out here and not by calling Bond.decayed. */
    private static int expect(BondPen p, int start, long days) {
        if (days <= 0 || p.perDay <= 0 || start <= p.floor) {
            return start;
        }
        long left = (long) start - days * (long) p.perDay;
        return (int) Math.max(p.floor, left);
    }

    private static void answer(BondPen p, String check, @Nullable Boolean pass, String detail) {
        if (!p.answered.add(check)) {
            return;
        }
        if (pass == null) {
            DebugYardClockwork.inconclusive(check, detail);
        } else {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }

    private static String config(BondPen p) {
        return "config bond_decay_per_day " + p.perDay + ", bond_floor " + p.floor;
    }

    private static void bondSetup(ServerLevel level, BondPen p) {
        if (p.run != run) {
            return;
        }
        long now = level.getGameTime();
        long today = today(level);
        if (today < H3_BACK + 1) {
            // A stamp five days back must be a real day (>= 1): 0 is what the code reads as "never stamped".
            long wait = (H3_BACK + 1) * HorseCareAttachment.DAY_TICKS + 3_000L - now;
            ActionTrace.log("test yard", BOND + ": the world is on day " + today + "; waiting " + wait / 1200
                    + " min for day " + (H3_BACK + 1) + " so H3's stamp five days back is not day 0");
            DebugYardHerd.after(level, wait, () -> bondSetup(level, p));
            return;
        }
        p.perDay = ServerConfig.bondDecayPerDay();
        p.floor = ServerConfig.bondFloor();
        p.day0 = today;
        p.t0 = now;
        if (p.perDay <= 0) {
            String why = config(p) + ": decay is off in this world, so no check here can see it run";
            for (String c : List.of(CHECK_UNSTAMPED, CHECK_UNLOADED, CHECK_DAILY, CHECK_FLOOR, CHECK_FOAL)) {
                answer(p, c, null, why);
            }
            return;
        }
        int h4 = Math.min(HorseCareAttachment.MAX_BOND, p.floor + 2);
        int h5 = p.floor > 10 ? 10 : p.floor - 1;
        p.h3Stamp = today - H3_BACK;
        Object[][] plan = {{"H1", 100, today}, {"H2", 90, 0L}, {"H3", 100, p.h3Stamp}, {"H4", h4, today},
                {"H5", Math.max(0, h5), today}};
        StringBuilder wrote = new StringBuilder();
        for (Object[] row : plan) {
            String tag = (String) row[0];
            Horse h = find(level, p.horses.get(tag));
            if (h == null) {
                wrote.append(tag).append(" MISSING; ");
                continue;
            }
            int bond = (Integer) row[1];
            long stamp = (Long) row[2];
            write(h, bond, stamp);
            feed(h);
            p.start.put(tag, bond);
            wrote.append(tag).append(" bond ").append(bond).append(" stamp ").append(stamp).append("; ");
        }
        ActionTrace.log("test yard", BOND + ": setup at tick " + now + ", day " + today + " (" + config(p)
                + ") - wrote " + wrote + "H2/H3 read in 100 ticks, H1/H4/H5 every 5 min, verdict after "
                + BOND_DAYS + " days");
        if (h5 < 0) {
            answer(p, CHECK_FOAL, null, config(p) + ": a floor of " + p.floor + " leaves no bond below it to test");
        }
        DebugYardHerd.after(level, 100, () -> bondFirstTick(level, p));
        DebugYardHerd.after(level, READ_EVERY, () -> bondRead(level, p));
    }

    /** H2 and H3, 100 ticks after the write: three or more of their 30-tick scans have run. */
    private static void bondFirstTick(ServerLevel level, BondPen p) {
        if (p.run != run) {
            return;
        }
        long today = today(level);
        Horse h2 = find(level, p.horses.get("H2"));
        if (h2 == null || !p.start.containsKey("H2")) {
            answer(p, CHECK_UNSTAMPED, null, "H2 is missing - nothing to read");
        } else {
            HorseCareAttachment c = h2.getData(ModAttachments.HORSE_CARE.get());
            int broken = expect(p, 90, today);  // what charging from day 0 would leave
            String d = "H2 written bond 90 with dayStamp 0 at day " + p.day0 + "; 100 ticks later, day " + today
                    + ": bond " + c.bond() + ", dayStamp " + c.dayStamp() + " (expected 90, stamped " + today
                    + "; a broken guard charging the world's " + today + " days would leave " + broken + ") | "
                    + config(p);
            if (c.dayStamp() == 0L) {
                answer(p, CHECK_UNSTAMPED, null, d + " - its stamp is still 0, so its scan has not run");
            } else {
                answer(p, CHECK_UNSTAMPED, c.bond() == 90 && c.dayStamp() == today, d);
            }
        }
        Horse h3 = find(level, p.horses.get("H3"));
        if (h3 == null || !p.start.containsKey("H3")) {
            answer(p, CHECK_UNLOADED, null, "H3 is missing - nothing to read");
        } else {
            HorseCareAttachment c = h3.getData(ModAttachments.HORSE_CARE.get());
            long owed = today - p.h3Stamp;
            int want = expect(p, 100, owed);
            String d = "H3 written bond 100 stamped day " + p.h3Stamp + " at day " + p.day0 + "; 100 ticks later,"
                    + " day " + today + ": bond " + c.bond() + ", dayStamp " + c.dayStamp() + " (expected " + want
                    + " = 100 less " + owed + " day(s) x " + p.perDay + ", floor " + p.floor + ", stamped " + today
                    + ") | " + config(p);
            if (c.dayStamp() == p.h3Stamp) {
                answer(p, CHECK_UNLOADED, null, d + " - its stamp has not moved, so its scan has not run");
            } else {
                answer(p, CHECK_UNLOADED, c.bond() == want && c.dayStamp() == today, d);
            }
        }
    }

    private static void bondRead(ServerLevel level, BondPen p) {
        if (p.run != run || p.answered.size() >= 5) {
            return;
        }
        DebugYardHerd.after(level, READ_EVERY, () -> bondRead(level, p));  // first, so a throw cannot stop it
        p.readings++;
        long today = today(level);
        long days = today - p.day0;
        StringBuilder line = new StringBuilder();
        Map<String, Integer> now = new LinkedHashMap<>();
        for (Map.Entry<String, UUID> e : p.horses.entrySet()) {
            String tag = e.getKey();
            Horse h = find(level, e.getValue());
            if (h == null || !p.start.containsKey(tag)) {
                line.append(tag).append(" missing; ");
                continue;
            }
            feed(h);
            HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
            now.put(tag, c.bond());
            line.append(tag).append(' ').append(c.bond()).append(" (stamp ").append(c.dayStamp()).append(")");
            if ("H5".equals(tag)) {
                line.append(h.isBaby() ? " foal" : " grown");
            }
            line.append("; ");
            StringBuilder hist = p.history.get(tag);
            String last = hist.length() == 0 ? null : hist.substring(hist.lastIndexOf(" ") + 1);
            if (last == null || !last.endsWith("=" + c.bond())) {
                hist.append(hist.length() == 0 ? "" : " ").append("d").append(days).append('=').append(c.bond());
            }
        }
        String reading = "day " + today + " (+" + days + " since setup): " + line + config(p);

        // H1 - exactly perDay a day.
        if (!p.answered.contains(CHECK_DAILY)) {
            Integer b = now.get("H1");
            if (b == null) {
                answer(p, CHECK_DAILY, null, "H1 is missing at " + reading + " | history " + p.history.get("H1"));
            } else {
                int want = expect(p, 100, days);
                String d = "H1 from 100 at day " + p.day0 + ": " + b + " after " + days + " day(s), expected " + want
                        + " | history " + p.history.get("H1") + " | " + config(p);
                if (b != want) {
                    answer(p, CHECK_DAILY, false, d);
                } else if (days >= BOND_DAYS) {
                    answer(p, CHECK_DAILY, true, d + " - every reading on the line");
                }
            }
        }
        // H4 - stops at the floor.
        if (!p.answered.contains(CHECK_FLOOR)) {
            Integer b = now.get("H4");
            if (b == null) {
                answer(p, CHECK_FLOOR, null, "H4 is missing at " + reading + " | history " + p.history.get("H4"));
            } else {
                int start = p.start.get("H4");
                p.lowestH4 = Math.min(p.lowestH4, b);
                int want = expect(p, start, days);
                String d = "H4 from " + start + " at day " + p.day0 + ": " + b + " after " + days + " day(s), expected "
                        + want + ", lowest seen " + p.lowestH4 + " | history " + p.history.get("H4") + " | "
                        + config(p);
                if (b < p.floor || b != want) {
                    answer(p, CHECK_FLOOR, false, d + (b < p.floor ? " - UNDER the floor" : ""));
                } else if (days >= BOND_DAYS) {
                    // floor + 2 reaches the floor in at most two days at any perDay >= 1.
                    answer(p, CHECK_FLOOR, b == p.floor, d + (b == p.floor
                            ? " - reached the floor and held it" : " - never reached the floor"));
                }
            }
        }
        // H5 - never topped up.
        if (!p.answered.contains(CHECK_FOAL)) {
            Integer b = now.get("H5");
            if (b == null) {
                answer(p, CHECK_FOAL, null, "H5 is missing at " + reading + " | history " + p.history.get("H5"));
            } else {
                int start = p.start.get("H5");
                String d = "H5 (foal) from " + start + " at day " + p.day0 + ": " + b + " after " + days
                        + " day(s), expected " + start + " throughout | history " + p.history.get("H5") + " | "
                        + config(p);
                if (b != start) {
                    answer(p, CHECK_FOAL, false, d + (b == p.floor ? " - TOPPED UP to the floor" : ""));
                } else if (days >= BOND_DAYS) {
                    answer(p, CHECK_FOAL, true, d);
                }
            }
        }
        if (p.readings % (HOUR / READ_EVERY) == 0) {
            ActionTrace.log("test yard", BOND + " hourly reading | " + reading);
        }
        if (p.answered.size() >= 5) {
            ActionTrace.log("test yard", BOND + ": every check answered | " + reading);
        }
    }

    private static void bondDeadline(ServerLevel level, BondPen p) {
        if (p.run != run) {
            return;
        }
        String why = "no verdict in " + BOND_DEADLINE / HOUR + " hours of game time (setup "
                + (p.t0 == 0 ? "never ran" : "at day " + p.day0) + ", now day " + today(level) + ", " + p.readings
                + " readings) | H1 " + p.history.get("H1") + " | H4 " + p.history.get("H4") + " | H5 "
                + p.history.get("H5") + " | " + config(p);
        for (String c : List.of(CHECK_UNSTAMPED, CHECK_UNLOADED, CHECK_DAILY, CHECK_FLOOR, CHECK_FOAL)) {
            answer(p, c, null, why);
        }
    }

    // ==================================================================
    // EAST - VERDANT FLOORS
    // ==================================================================

    /*
     * VERDANT FLOORS (wiki/gene-verdant.html, Verification tab, "Still open (NOT played)"). The checks it answers:
     *
     *   "The carrier does nothing, and that is the control. A mush/moss horse is verdant-carrier ... Stand one on
     *    its own ground for hours. Pass: not one block converted."
     *   "A moss horse on a cobblestone floor - the one worth doing first. ... Stable a moss/moss horse on a
     *    cobbled floor for an hour and count the moss blocks."
     *   "Each cover eats only its own list. ... Pass: mush/mush takes podzol and rooted dirt; grass/grass takes
     *    dirt, coarse dirt and rooted dirt and leaves podzol alone; neither touches stone."
     *   "It is one block a beat and it stays a patch."
     *
     * WHAT THE CODE SAYS (read 2026-10-01). VerdantGene: tokens mush, moss, grass, n; a cover only for a
     * homozygote, any mixed pair inert; SPREAD_RADIUS 2, SPREAD_INTERVAL_TICKS 60, SPREAD_CHANCE 0.35.
     * GeneAbilityHandler.spread: on a beat ((gameTime + phase) mod 60 == 0) and a 0.35 roll, ONE random block
     * at offset x,z in -2..2 and y in -1..+1 of horse.blockPosition() is tried, and converted if
     * GeneAbilityHandler.convert lists it - so at most one conversion a beat, 1,200 beats in the hour. convert:
     *   mycelium: grass block, dirt, coarse dirt, podzol, rooted dirt -> mycelium
     *   moss:     stone, COBBLESTONE, andesite, gravel, dirt, coarse dirt, grass block -> moss block
     *   grass:    dirt, coarse dirt, rooted dirt -> grass block
     * So THE PAGE AND THE CODE DISAGREE on moss: the page's "nothing a player built with is eaten" cannot hold,
     * because cobblestone and stone are on the moss list. This pen tests what the CODE says - moss/moss DOES
     * convert cobblestone - and a PASS on that check is evidence for the page's warn box, not its claim.
     *
     * THE CELLS. Four glass cells (walls two high, glass lid at gy + 3 leaving the yard's light blocks in place
     * so the cells stay lit), one mare each:
     *   1 (NW) moss/moss  on cobblestone             3 (SW) mush/mush  west half podzol, east half stone
     *   2 (NE) mush/moss  west half dirt, east cobble 4 (SE) grass/grass west half podzol, east half dirt
     * Every wall's footing is STONE (a second "not on the list" block for mush and grass, and on moss's list).
     * The reach is two blocks: from the interior's edge it crosses the wall's footing and lands on a one-block
     * gap of STONE BRICKS between cells and round the outside (stone bricks are on no list). So no horse can
     * touch another cell's floor or anything outside this footprint - the north gap row keeps the aisle's grass
     * at z0 - 1 out of reach too. The snapshot covers the whole east footprint floor, gaps included, and any
     * change outside a cell is reported.
     *
     * NOTHING ELSE EATS THE FLOOR. HungerFoodGoal grazes below Hunger.GRAZE_BELOW (75) and turns grass AND moss
     * blocks to dirt; hunger drains about 50 a day (twenty minutes), so every horse is written back to FULL every
     * five minutes and never gets near 75. VANILLA does move one block here: a grass block spreads onto lit
     * dirt by random tick. So cell 4's floor count of dirt -> grass includes vanilla's spread from the horse's
     * first patch; the rate check therefore uses DebugWorldWatch.spreadsPlacedBy (the gene's own conversions,
     * counted at the moment it converts) for every cell, and the floor diff for cells 1-3, where nothing vanilla
     * spreads (mycelium spreads only onto dirt, and cell 3 has none; moss and podzol do not spread).
     *
     * PASS / FAIL. Snapshot every floor block at t0 (two seconds after build), compare at t0 + 60 min:
     *   carrier: cell 2 floor unchanged and 0 gene conversions; FAIL on any.
     *   no stone: no stone block in cells 3 or 4 changed (cell 3's stone half and both footings); FAIL on any.
     *   podzol: no podzol block in cell 4 changed; FAIL on any.
     *   grass greens dirt: at least one dirt -> grass block in cell 4; FAIL on none.
     *   moss eats cobble: at least one cobblestone -> moss block in cell 1; FAIL on none.
     *   rate cap: every horse's gene conversions <= 1,200 (one a beat), and cells 1-3's floor changes too.
     *     Honestly the cap cannot be exceeded by these small floors; the detail carries the observed rate
     *     against the expected one (1,200 beats x 0.35 x the share of tries that land on a listed block).
     * A horse missing, out of its cell, or not carrying the genotype it was built with is INCONCLUSIVE for the
     * checks it carries. Answers once, at sixty minutes.
     */

    private static final String VERDANT = "VERDANT FLOORS";
    private static final String CHECK_CARRIER = VERDANT + " - a mush/moss carrier converts nothing";
    private static final String CHECK_NO_STONE = VERDANT + " - mush/mush and grass/grass convert no stone";
    private static final String CHECK_PODZOL = VERDANT + " - grass/grass leaves podzol alone";
    private static final String CHECK_GREENS = VERDANT + " - grass/grass turns dirt to grass";
    private static final String CHECK_COBBLE = VERDANT + " - moss/moss converts cobblestone (it is on the code's"
            + " moss list)";
    private static final String CHECK_RATE = VERDANT + " - no horse converts more than one block a beat";

    private static final long VERDANT_RUN = HOUR;

    private record Cell(String name, String tokens, String cover, int x0, int x1, int z0, int z1, Block west,
                        Block east) {
        boolean holds(int x, int z) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }
    }

    private static final class VerdantRun {
        final int run;
        final int gy;
        final int fx0;
        final int fz0;
        final List<Cell> cells = new ArrayList<>();
        final Map<String, UUID> horse = new LinkedHashMap<>();
        final Map<String, Integer> placedAtStart = new LinkedHashMap<>();
        final Map<BlockPos, Block> before = new LinkedHashMap<>();
        final Map<String, String> trouble = new LinkedHashMap<>();
        long t0;

        VerdantRun(int run, int gy, int fx0, int fz0) {
            this.run = run;
            this.gy = gy;
            this.fx0 = fx0;
            this.fz0 = fz0;
        }
    }

    /** {@code ex0} is the east half's first column, x0 + 25. The footprint is ex0 .. ex0 + 19, z0 .. z0 + 12. */
    private static void verdantFloors(ServerLevel level, int gy, int ex0, int z0, int myRun) {
        for (String c : List.of(CHECK_CARRIER, CHECK_NO_STONE, CHECK_PODZOL, CHECK_GREENS, CHECK_COBBLE, CHECK_RATE)) {
            DebugYardClockwork.expect(c);
        }
        VerdantRun v = new VerdantRun(myRun, gy, ex0, z0);
        // Stone bricks over the whole footprint first: the gaps between cells and round them stay this.
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = ex0; x <= ex0 + 19; x++) {
            for (int z = z0; z <= z0 + 12; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, bricks);
            }
        }
        String k = VerdantGene.KEY + "=";
        v.cells.add(new Cell("cell 1 moss/moss", "moss/moss", "moss", ex0 + 1, ex0 + 9, z0 + 1, z0 + 5,
                Blocks.COBBLESTONE, Blocks.COBBLESTONE));
        v.cells.add(new Cell("cell 2 mush/moss", "mush/moss", "", ex0 + 11, ex0 + 18, z0 + 1, z0 + 5,
                Blocks.DIRT, Blocks.COBBLESTONE));
        v.cells.add(new Cell("cell 3 mush/mush", "mush/mush", "mycelium", ex0 + 1, ex0 + 9, z0 + 7, z0 + 11,
                Blocks.PODZOL, Blocks.STONE));
        v.cells.add(new Cell("cell 4 grass/grass", "grass/grass", "grass", ex0 + 11, ex0 + 18, z0 + 7, z0 + 11,
                Blocks.PODZOL, Blocks.DIRT));
        for (Cell c : v.cells) {
            verdantCell(level, gy, c);
            YardPens.register(gy, c.x0(), c.x1(), c.z0(), c.z1(), VERDANT + " " + c.name());
            Horse h = DebugYardUnattended.horse(level, gy, (c.x0() + c.x1() + 1) / 2.0, (c.z0() + c.z1() + 1) / 2.0,
                    Sex.FEMALE, k + c.tokens(), true, VERDANT + " " + c.tokens());
            if (h != null) {
                v.horse.put(c.name(), h.getUUID());
                feed(h);
            }
        }
        DebugPenManager.placeSign(level, new BlockPos(ex0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("VERDANT FLOORS", "moss|carrier", "mush|grass", "count after 1 hour"));
        DebugYardHerd.after(level, 40, () -> verdantStart(level, v));
    }

    /** Floor (halves split west / east by x, stone footing under the walls), glass walls, glass lid. */
    private static void verdantCell(ServerLevel level, int gy, Cell c) {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        int mid = (c.x0() + 1 + c.x1() - 1) / 2;   // last column of the west half of the interior
        for (int x = c.x0(); x <= c.x1(); x++) {
            for (int z = c.z0(); z <= c.z1(); z++) {
                boolean wall = x == c.x0() || x == c.x1() || z == c.z0() || z == c.z1();
                BlockState floor = wall ? stone : (x <= mid ? c.west() : c.east()).defaultBlockState();
                DebugPenManager.groundColumn(level, x, gy, z, floor);
                for (int y = gy + 1; y <= gy + 2; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), wall ? glass : air);
                }
                // The lid, but not over a lamp: the yard's light blocks at gy + 3 keep the cell lit.
                BlockPos lid = new BlockPos(x, gy + 3, z);
                if (!level.getBlockState(lid).is(Blocks.LIGHT)) {
                    DebugPenManager.fastSet(level, lid, glass);
                }
            }
        }
    }

    private static Set<String> checksOf(String cell) {
        return switch (cell) {
            case "cell 1 moss/moss" -> Set.of(CHECK_COBBLE, CHECK_RATE);
            case "cell 2 mush/moss" -> Set.of(CHECK_CARRIER, CHECK_RATE);
            case "cell 3 mush/mush" -> Set.of(CHECK_NO_STONE, CHECK_RATE);
            default -> Set.of(CHECK_NO_STONE, CHECK_PODZOL, CHECK_GREENS, CHECK_RATE);
        };
    }

    private static void verdantStart(ServerLevel level, VerdantRun v) {
        if (v.run != run) {
            return;
        }
        v.t0 = level.getGameTime();
        StringBuilder who = new StringBuilder();
        for (Cell c : v.cells) {
            Horse h = find(level, v.horse.get(c.name()));
            String bad = horseTrouble(h, c);
            if (bad != null) {
                v.trouble.put(c.name(), bad + " at t0");
            }
            if (h != null) {
                v.placedAtStart.put(c.name(), DebugWorldWatch.spreadsPlacedBy(h.getUUID()));
            }
            who.append(c.name()).append(bad == null ? " ok" : " " + bad).append("; ");
        }
        for (int x = v.fx0; x <= v.fx0 + 19; x++) {
            for (int z = v.fz0; z <= v.fz0 + 12; z++) {
                BlockPos at = new BlockPos(x, v.gy, z);
                v.before.put(at, level.getBlockState(at).getBlock());
            }
        }
        ActionTrace.log("test yard", VERDANT + ": t0 at tick " + v.t0 + ", " + v.before.size()
                + " floor blocks snapshotted; " + who + "verdict in 60 min");
        verdantTick(level, v, 1);
    }

    /** Every five minutes: keep them fed, note any horse that left its cell; at sixty, judge. */
    private static void verdantTick(ServerLevel level, VerdantRun v, int step) {
        DebugYardHerd.after(level, READ_EVERY, () -> {
            if (v.run != run) {
                return;
            }
            for (Cell c : v.cells) {
                Horse h = find(level, v.horse.get(c.name()));
                if (h != null) {
                    feed(h);
                }
                String bad = horseTrouble(h, c);
                if (bad != null && !v.trouble.containsKey(c.name())) {
                    v.trouble.put(c.name(), bad + " at " + step * 5 + " min");
                }
            }
            if (step * READ_EVERY >= VERDANT_RUN) {
                verdantJudge(level, v);
            } else {
                verdantTick(level, v, step + 1);
            }
        });
    }

    /** Why this cell's horse cannot be judged, or null. */
    private static @Nullable String horseTrouble(@Nullable Horse h, Cell c) {
        if (h == null) {
            return "horse missing";
        }
        if (!c.holds(h.getBlockX(), h.getBlockZ())) {
            return "horse out of its cell at " + h.blockPosition().toShortString();
        }
        if (HorseRecords.hasRealRecord(h)) {
            AllelePair pair = HorseRecords.of(h).genotype().pair(VerdantGene.KEY);
            String cover = pair == null ? "" : Genes.VERDANT.coverOf(pair);
            if (pair == null || !cover.equals(c.cover())) {
                return "horse carries " + (pair == null ? "no verdant pair" : pair.toTokens()) + ", cover '" + cover
                        + "', not the cell's '" + c.cover() + "'";
            }
        }
        return null;
    }

    private static String id(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).getPath();
    }

    private static void verdantJudge(ServerLevel level, VerdantRun v) {
        long elapsed = level.getGameTime() - v.t0;
        long beats = elapsed / VerdantGene.SPREAD_INTERVAL_TICKS;
        // from->to counts per cell, and the changes outside every cell.
        Map<String, Map<String, Integer>> diff = new LinkedHashMap<>();
        Map<String, Integer> outside = new LinkedHashMap<>();
        for (Cell c : v.cells) {
            diff.put(c.name(), new LinkedHashMap<>());
        }
        for (Map.Entry<BlockPos, Block> e : v.before.entrySet()) {
            Block now = level.getBlockState(e.getKey()).getBlock();
            if (now == e.getValue()) {
                continue;
            }
            String change = id(e.getValue()) + "->" + id(now);
            Map<String, Integer> into = outside;
            for (Cell c : v.cells) {
                if (c.holds(e.getKey().getX(), e.getKey().getZ())) {
                    into = diff.get(c.name());
                }
            }
            into.merge(change, 1, Integer::sum);
        }
        Map<String, Integer> placed = new LinkedHashMap<>();
        StringBuilder all = new StringBuilder();
        for (Cell c : v.cells) {
            Horse h = find(level, v.horse.get(c.name()));
            int n = h == null || !v.placedAtStart.containsKey(c.name()) ? -1
                    : DebugWorldWatch.spreadsPlacedBy(h.getUUID()) - v.placedAtStart.get(c.name());
            placed.put(c.name(), n);
            all.append(c.name()).append(": floor ").append(diff.get(c.name()).isEmpty() ? "unchanged"
                    : diff.get(c.name())).append(", gene conversions ").append(n < 0 ? "?" : n)
                    .append(v.trouble.containsKey(c.name()) ? " [" + v.trouble.get(c.name()) + "]" : "").append("; ");
        }
        all.append("outside the cells: ").append(outside.isEmpty() ? "unchanged" : outside)
                .append(" | ").append(elapsed / 1200).append(" min, ").append(beats).append(" beats");
        String summary = all.toString();
        ActionTrace.log("test yard", VERDANT + " at 60 min | " + summary);

        Set<String> blocked = new HashSet<>();
        for (Map.Entry<String, String> t : v.trouble.entrySet()) {
            blocked.addAll(checksOf(t.getKey()));
        }

        // Carrier.
        Map<String, Integer> c2 = diff.get("cell 2 mush/moss");
        int p2 = placed.get("cell 2 mush/moss");
        verdantAnswer(blocked, CHECK_CARRIER, c2.isEmpty() && p2 == 0,
                "mush/moss on dirt + cobblestone: floor " + (c2.isEmpty() ? "unchanged" : c2) + ", gene conversions "
                        + p2 + " (pass is none of either) | " + summary);

        // No stone, cells 3 and 4.
        int stone3 = count(diff.get("cell 3 mush/mush"), "stone->");
        int stone4 = count(diff.get("cell 4 grass/grass"), "stone->");
        int podzolMyc = count(diff.get("cell 3 mush/mush"), "podzol->mycelium");
        verdantAnswer(blocked, CHECK_NO_STONE, stone3 == 0 && stone4 == 0,
                "stone blocks changed: mush/mush " + stone3 + " (its stone half and footing), grass/grass " + stone4
                        + " (its footing); for the record mush/mush took " + podzolMyc + " podzol to mycelium"
                        + " (the page also expects that) | " + summary);

        // Podzol, cell 4.
        int podzol4 = count(diff.get("cell 4 grass/grass"), "podzol->");
        verdantAnswer(blocked, CHECK_PODZOL, podzol4 == 0,
                "podzol blocks changed under grass/grass: " + podzol4 + " | " + summary);

        // Dirt greened, cell 4.
        int greened = count(diff.get("cell 4 grass/grass"), "dirt->grass_block");
        int p4 = placed.get("cell 4 grass/grass");
        verdantAnswer(blocked, CHECK_GREENS, greened >= 1,
                "dirt -> grass block under grass/grass: " + greened + " on the floor (vanilla's own grass spread"
                        + " adds to this once there is a first patch), " + p4 + " by the gene itself | " + summary);

        // Cobblestone mossed, cell 1.
        int mossed = count(diff.get("cell 1 moss/moss"), "cobblestone->moss_block");
        int mossStone = count(diff.get("cell 1 moss/moss"), "stone->moss_block");
        verdantAnswer(blocked, CHECK_COBBLE, mossed >= 1,
                "cobblestone -> moss block under moss/moss: " + mossed + " (and " + mossStone + " stone footing"
                        + " blocks) - the code's moss list includes cobblestone and stone, so the page's claim that"
                        + " nothing a player builds with is eaten does not hold whatever this reads | " + summary);

        // Rate cap.
        boolean under = true;
        StringBuilder rate = new StringBuilder();
        for (Cell c : v.cells) {
            int n = placed.get(c.name());
            int floorChanges = 0;
            for (int x : diff.get(c.name()).values()) {
                floorChanges += x;
            }
            boolean vanillaSpreads = c.name().startsWith("cell 4");
            if (n > beats || (!vanillaSpreads && floorChanges > beats)) {
                under = false;
            }
            rate.append(c.name()).append(' ').append(n).append(" by the gene, ").append(floorChanges)
                    .append(" on the floor").append(vanillaSpreads ? " (vanilla grass spread included)" : "")
                    .append("; ");
        }
        boolean anyPlacedUnknown = placed.containsValue(-1);
        String rateDetail = String.format(Locale.ROOT, "%scap %d (one a beat over %d ticks); a horse that converts"
                        + " every try that lands would make about %.0f (beats x %.2f), and the 7x3 and 6x3 floors cap far"
                        + " lower - this cap cannot be exceeded here, the per-cell numbers are the reading | %s",
                rate, beats, elapsed, beats * VerdantGene.SPREAD_CHANCE, VerdantGene.SPREAD_CHANCE, summary);
        if (anyPlacedUnknown) {
            blocked.add(CHECK_RATE);
        }
        verdantAnswer(blocked, CHECK_RATE, under, rateDetail);
    }

    private static int count(Map<String, Integer> changes, String prefix) {
        int n = 0;
        for (Map.Entry<String, Integer> e : changes.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                n += e.getValue();
            }
        }
        return n;
    }

    private static void verdantAnswer(Set<String> blocked, String check, boolean pass, String detail) {
        if (blocked.contains(check)) {
            DebugYardClockwork.inconclusive(check, "a horse this check needs was missing, out of its cell or not"
                    + " the genotype it was built as (see [..] below) | would have read " + (pass ? "PASS" : "FAIL")
                    + ": " + detail);
        } else {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }
}
