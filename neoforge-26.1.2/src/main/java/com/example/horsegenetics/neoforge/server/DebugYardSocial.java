package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.genes.MusicEnjoyerGene;
import com.example.horsegenetics.common.genetics.genes.PackLeaderGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row BB: two social genes that answer for themselves</b> (2026-10-02). West, MUSIC - four horses in glass cells
 * round a jukebox, their bond read straight off the care attachment for one day of the care clock. East, PACK LEADER
 * - ten loose cows in a walled pen, first beside a mismatched horse and then beside a matched leader whose steps
 * this class drives, every cow's navigation target read on the leader's own beat.
 *
 * <p>Both halves start themselves at build, run on {@link DebugYardHerd#after}, and end in
 * {@code CLOCKWORK PASS|FAIL|INCONCLUSIVE} lines through {@link DebugYardClockwork#verdict}. A clock left running
 * from an older yard stops itself on {@link #run}; every timed step goes through {@link #step}, which answers every
 * still-open check of its pen INCONCLUSIVE if the step throws; and each half has a deadline after which whatever
 * has not answered says INCONCLUSIVE with what it saw.
 */
final class DebugYardSocial {

    private DebugYardSocial() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        try {
            music(level, gy, x0, z0, myRun);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BB west (MUSIC) failed to build", e);
        }
        try {
            pack(level, gy, x0 + 25, z0, myRun);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BB east (PACK LEADER) failed to build", e);
        }
        ActionTrace.log("test yard", "row BB built (west: MUSIC; east: PACK LEADER) at game tick "
                + level.getGameTime() + ", care day " + level.getGameTime() / HorseCareAttachment.DAY_TICKS);
    }

    // ------------------------------------------------------------------
    // shared
    // ------------------------------------------------------------------

    /** Answer a check once; a second answer for the same check is dropped. {@code pass == null} is INCONCLUSIVE. */
    private static void answer(Set<String> answered, String check, @Nullable Boolean pass, String detail) {
        if (!answered.add(check)) {
            return;
        }
        if (pass == null) {
            DebugYardClockwork.inconclusive(check, detail);
        } else {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }

    /**
     * One timed step: {@code ticks} from now, skipped if the yard has been rebuilt since, and if it throws, every
     * check of its pen that has not answered answers INCONCLUSIVE naming the exception - a broken step must not
     * leave a check PENDING all night.
     */
    private static void step(ServerLevel level, long ticks, int myRun, List<String> checks, Set<String> answered,
                             String pen, Runnable body) {
        DebugYardHerd.after(level, Math.max(1L, ticks), () -> {
            if (myRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a " + pen + " step failed", e);
                for (String c : checks) {
                    answer(answered, c, null, "a timed step threw " + e);
                }
            }
        });
    }

    private static @Nullable Horse findHorse(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static @Nullable Mob findMob(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Mob m && m.isAlive() ? m : null;
    }

    /** The centre of a block's floor. */
    private static Vec3 feet(BlockPos at) {
        return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ==================================================================
    // WEST - MUSIC
    // ==================================================================

    /*
     * MUSIC (wiki/gene-music-enjoyer.html, Verification tab, NOT played). The open checks it answers:
     *
     *   "Bond actually rises. Tame an Msc/Msc horse, stand it beside a jukebox with a record in it, and read its
     *    bond before and after ten minutes. Pass: hearts every MusicEnjoyerGene.INTERVAL_TICKS and a bond number
     *    that has climbed."
     *   "An untamed horse gains nothing. The same jukebox, an untamed expressing horse. Pass: no hearts at all."
     *   "A carrier gains nothing. An Msc/n horse beside the same jukebox. Pass: no hearts, no bond."
     *   "The daily cap holds. Leave a tamed expressing horse at a playing jukebox overnight. Pass: its bond stops
     *    at the day's ceiling rather than arriving at the top tier by morning."
     *   "Distance matters. The same horse twelve blocks from the jukebox. Pass: nothing."
     *   And RECORDED, NOT JUDGED: "Does a finished record still count? The flag tests whether the jukebox holds a
     *    record, not whether it is audible ... Pass: record which it is."
     *
     * WHAT THE CODE DOES, READ 2026-10-02. MusicEnjoyerGene (recessive, token Msc) grants one Bond ability:
     * AMOUNT 1 point every INTERVAL_TICKS (200), condition flag near_jukebox. GeneAbilityHandler.maybeBond returns
     * at once for an untamed horse, then gates on offCooldown(horse, key, "bond", 200) and calls
     * HorseCareHandler.awardBondFor(horse, 1) - the shared path, which no-ops for an untamed horse and caps the
     * day's gain at HorseCareAttachment.DAILY_CAP (15) through bondToday. The flag near_jukebox is a sampled block
     * search (re-sampled every 20 ticks) over the box +-JUKEBOX_RANGE (8) horizontally and +-3 vertically round the
     * horse's block, true for any minecraft:jukebox whose HAS_RECORD state is set. So it reads HAS_RECORD, not
     * "playing": a finished disc still in the box keeps the flag on - the owner's open question, which this pen
     * logs as a measured line and does not judge.
     *
     * THE FOUR MARES, each in a 4x4 glass cell (2x2 floor), the jukebox free-standing at (x0+3, gy+1, z0+6):
     *   A - Msc/Msc tamed, cell x0+4..7, z0+1..4: 2-4 blocks from the jukebox. Must rise, to the cap and no further.
     *   B - Msc/Msc UNTAMED, cell x0+4..7, z0+8..11: 2-4 blocks. Must not move.
     *   C - Msc/n tamed, cell x0+8..11, z0+4..7: 6-7 blocks (inside the 8). Must not move.
     *   D - Msc/Msc tamed, cell x0+15..18, z0+4..7: 13-14 blocks, outside the 8-block box. Must not move.
     * Mares, so no stallion business; tamed with no owner (DebugPenManager.spawnHorse(tamed = true)), so the
     * owner-proximity and riding bond in HorseCareHandler cannot reach them; nothing feeds or shears them, and
     * their hunger is written back to FULL every reading so none goes looking for food. That leaves the gene as
     * the only thing that can move a number.
     *
     * DECAY. Bond decay (behaviour.bond_decay_per_day, down to behaviour.bond_floor) is charged only when the
     * care day (getGameTime() / 24,000, as HorseCareHandler counts it) moves past the horse's dayStamp. Setup
     * writes every horse bond 40, bondToday 0, dayStamp = today, so NO decay can land inside the judged day. The
     * config is read and printed in every detail line anyway, and the decay at the one rollover this pen watches
     * (into the "finished record" day) is logged against the config's prediction.
     *
     * TIMING. Setup waits for a care day with at least 20,000 ticks left (it starts at once if the day is <= 4,000
     * ticks old, else at 200 ticks into the next day: up to ~17 minutes' wait). Then a reading every 200 ticks to
     * 23,600 ticks into that day - about 16-20 minutes. The disc (music_disc.cat, 185 s) is re-inserted whenever a
     * reading finds it finished, up to 18,000 ticks into the day, so the judged day is about the gene and not the
     * record; after that it is left to finish, and on the next care day the pen reads A for 6,000 more ticks with a
     * finished disc in the box and logs what it gained ("MUSIC FINISHED RECORD" line). Any interval of the judged
     * day that ended with the disc found finished is also totted up and logged.
     *
     * PASS / FAIL:
     *   RISES  - PASS: A's bond is above its start by the end of the day (detail: when the first point came, when
     *            the cap was reached, the change history). FAIL: A never moved.
     *   CAP    - PASS: A's bondToday reached 15 and never read above it, and A's bond never rose after the reading
     *            that first showed bondToday at 15. FAIL: bondToday > 15 at any reading, or bond rose after the
     *            cap. INCONCLUSIVE if A never reached the cap (that is RISES's answer, not a cap result).
     *   UNTAMED, CARRIER, DISTANCE - PASS: that horse's bond and bondToday never left 40 / 0 all day. FAIL: at
     *            the first reading where either moved.
     * A missing horse or a missing jukebox is INCONCLUSIVE. Deadline 50,000 ticks (~42 min) after build.
     */

    private static final String MUSIC = "MUSIC";
    private static final String M_RISES = MUSIC + " - a tamed Msc/Msc horse beside a jukebox with a record gains bond";
    private static final String M_UNTAMED = MUSIC + " - an untamed Msc/Msc horse beside the jukebox gains nothing";
    private static final String M_CARRIER = MUSIC + " - a tamed Msc/n carrier beside the jukebox gains nothing";
    private static final String M_DISTANCE = MUSIC + " - a tamed Msc/Msc horse 13 blocks from the jukebox gains"
            + " nothing";
    private static final String M_CAP = MUSIC + " - the gene's bond stops at the daily cap (bondToday never above"
            + " DAILY_CAP, no rise after it)";
    private static final List<String> M_CHECKS = List.of(M_RISES, M_UNTAMED, M_CARRIER, M_DISTANCE, M_CAP);

    private static final int START_BOND = 40;
    private static final long M_READ = MusicEnjoyerGene.INTERVAL_TICKS;   // 200: one reading per beat
    private static final long DAY = HorseCareAttachment.DAY_TICKS;
    /** Start at once only if this much of the day or less has gone. */
    private static final long M_LATEST_START = 4_000L;
    /** The judged day ends at the first reading this far in. */
    private static final long M_DAY_END = 23_600L;
    /** After this far into the judged day the disc is left to finish. cat is 185 s = 3,700 ticks. */
    private static final long M_STOP_RESTART = 18_000L;
    /** How long the "finished record" observation runs into the next day. */
    private static final long M_FINISHED_SPAN = 6_000L;
    private static final long M_DEADLINE = 50_000L;

    private static final String[] TAGS = {"A", "B", "C", "D"};

    private static final class Music {
        final int run;
        final Set<String> answered = new HashSet<>();
        final Map<String, UUID> horses = new LinkedHashMap<>();
        final Map<String, String> label = new LinkedHashMap<>();
        BlockPos jukebox;
        long day = -1;
        long setupAt;
        int perDay;
        int floor;
        int readings;
        int restarts;
        int lastA = -1;
        int maxToday;
        int capBond = -1;
        long capAt = -1;
        long firstGainAt = -1;
        final StringBuilder history = new StringBuilder();
        int finishedGainDayN;
        int finishedIntervalsDayN;
        // the next day: a finished disc left in the box
        int n1Base = -1;
        int n1Prev = -1;
        boolean n1PrevFinished;
        int n1Gain;
        int n1FinishedReadings;
        int n1PlayingReadings;
        int n1Readings;
        int lastDayNBond = -1;
        boolean judged;

        Music(int run) {
            this.run = run;
        }
    }

    private static void music(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : M_CHECKS) {
            DebugYardClockwork.expect(c);
        }
        Music m = new Music(myRun);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("MUSIC", "Msc/Msc tame, wild,", "Msc/n, Msc/Msc far", "bond for a day"));

        m.jukebox = new BlockPos(x0 + 3, gy + 1, z0 + 6);
        level.setBlock(m.jukebox, Blocks.JUKEBOX.defaultBlockState(), 3);

        String key = MusicEnjoyerGene.KEY;
        Object[][] plan = {
                {"A", x0 + 4, z0 + 1, key + "=Msc/Msc", true, "Msc/Msc tame, beside"},
                {"B", x0 + 4, z0 + 8, key + "=Msc/Msc", false, "Msc/Msc UNTAMED, beside"},
                {"C", x0 + 8, z0 + 4, key + "=Msc/n", true, "Msc/n tame, beside"},
                {"D", x0 + 15, z0 + 4, key + "=Msc/Msc", true, "Msc/Msc tame, 13 away"},
        };
        for (Object[] row : plan) {
            String tag = (String) row[0];
            int cx = (Integer) row[1];
            int cz = (Integer) row[2];
            DebugYardClockwork.cell(level, gy, cx, cx + 3, cz, cz + 3);
            String label = MUSIC + " " + tag + " (" + row[5] + ")";
            Horse h = DebugYardUnattended.horse(level, gy, cx + 2.0, cz + 2.0, Sex.FEMALE, (String) row[3],
                    (Boolean) row[4], label);
            if (h != null) {
                m.horses.put(tag, h.getUUID());
                m.label.put(tag, label);
            }
        }

        // A care day with at least 20,000 ticks left, so the cap is reached and held well inside one day.
        long first = level.getGameTime() + 40;
        long off = first % DAY;
        long delay = off <= M_LATEST_START ? 40 : 40 + (DAY - off) + 200;
        if (delay > 40) {
            ActionTrace.log("test yard", MUSIC + ": care day " + first / DAY + " is " + off + " ticks old; waiting "
                    + delay / 1200 + " min for the next day so the cap can be reached and held inside one day");
        }
        step(level, delay, myRun, M_CHECKS, m.answered, MUSIC, () -> musicSetup(level, m));
        step(level, M_DEADLINE, myRun, M_CHECKS, m.answered, MUSIC, () -> {
            for (String c : M_CHECKS) {
                answer(m.answered, c, null, "deadline: " + m.readings + " reading(s) on day " + m.day + ", A "
                        + m.lastA + " (start " + START_BOND + "), history " + m.history + " | " + musicConfig(m));
            }
        });
    }

    private static String musicConfig(Music m) {
        return "config bond_decay_per_day " + m.perDay + ", bond_floor " + m.floor + "; DAILY_CAP "
                + HorseCareAttachment.DAILY_CAP + ", beat " + MusicEnjoyerGene.INTERVAL_TICKS + " ticks";
    }

    private static @Nullable JukeboxBlockEntity jukebox(ServerLevel level, Music m) {
        return level.getBlockEntity(m.jukebox) instanceof JukeboxBlockEntity jb ? jb : null;
    }

    /**
     * Put a disc in and start it. UNVERIFIED: JukeboxBlockEntity.setTheItem is used nowhere else in this repo; read
     * off the 26.1.2 sources - it stores the stack, sets the block's HAS_RECORD state (notifyItemChangedInJukebox)
     * and calls JukeboxSongPlayer.play when the stack carries a jukebox song. Replacing a finished disc with a fresh
     * one restarts the song from 0. Items.MUSIC_DISC_CAT is likewise unused elsewhere here (present in the 26.1.2
     * Items source). Ticking: the song advances in JukeboxBlockEntity.tick, which a force-loaded chunk runs.
     */
    private static void insertDisc(JukeboxBlockEntity jb) {
        jb.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
    }

    /** UNVERIFIED in this repo (read off the 26.1.2 sources): isPlaying is "a song is set", cleared on finish. */
    private static boolean playing(@Nullable JukeboxBlockEntity jb) {
        return jb != null && jb.getSongPlayer().isPlaying();
    }

    private static boolean hasRecord(ServerLevel level, Music m) {
        BlockState s = level.getBlockState(m.jukebox);
        return s.is(Blocks.JUKEBOX) && s.getValue(JukeboxBlock.HAS_RECORD);
    }

    private static void writeCare(Horse h, int bond, long stamp) {
        HorseCareAttachment care = HorseCareAttachment.DEFAULT.with(bond, Optional.empty(), 0, stamp, 0L, 0L);
        h.setData(ModAttachments.HORSE_CARE.get(), care);
        HorseCareHandler.syncCare(h, care);
    }

    private static void musicSetup(ServerLevel level, Music m) {
        long now = level.getGameTime();
        m.day = now / DAY;
        m.setupAt = now;
        m.perDay = ServerConfig.bondDecayPerDay();
        m.floor = ServerConfig.bondFloor();
        JukeboxBlockEntity jb = jukebox(level, m);
        if (jb == null) {
            for (String c : M_CHECKS) {
                answer(m.answered, c, null, "no jukebox block entity at " + m.jukebox.toShortString()
                        + " - the record cannot be played");
            }
            return;
        }
        insertDisc(jb);
        StringBuilder wrote = new StringBuilder();
        for (String tag : TAGS) {
            Horse h = findHorse(level, m.horses.get(tag));
            if (h == null) {
                wrote.append(tag).append(" MISSING; ");
                continue;
            }
            writeCare(h, START_BOND, m.day);
            h.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
            wrote.append(tag).append(h.isTamed() ? " tame" : " untamed").append("; ");
        }
        m.lastA = START_BOND;
        ActionTrace.log("test yard", MUSIC + ": setup at tick " + now + " (care day " + m.day + ", " + now % DAY
                + " ticks in) - every horse written bond " + START_BOND + ", bondToday 0, stamped today; " + wrote
                + "disc in: HAS_RECORD " + hasRecord(level, m) + ", playing " + playing(jb) + " | " + musicConfig(m));
        if (!hasRecord(level, m)) {
            for (String c : M_CHECKS) {
                answer(m.answered, c, null, "a disc was put in the jukebox and its HAS_RECORD state is still false"
                        + " - the near_jukebox flag cannot be on, so the world is not in the state the test needs");
            }
            return;
        }
        step(level, M_READ, m.run, M_CHECKS, m.answered, MUSIC, () -> musicRead(level, m));
    }

    private static void musicRead(ServerLevel level, Music m) {
        long now = level.getGameTime();
        long today = now / DAY;
        long off = now % DAY;
        if (today == m.day) {
            boolean more = dayNReading(level, m, off);
            if (!more) {
                return;
            }
            if (off >= M_DAY_END) {
                judgeDayN(level, m);
                step(level, DAY - off + 200, m.run, M_CHECKS, m.answered, MUSIC, () -> musicRead(level, m));
            } else {
                step(level, M_READ, m.run, M_CHECKS, m.answered, MUSIC, () -> musicRead(level, m));
            }
        } else if (today == m.day + 1) {
            judgeDayN(level, m);   // a no-op unless a reading was skipped past the end of the day
            nextDayReading(level, m, off);
            if (off < M_FINISHED_SPAN) {
                step(level, M_READ, m.run, M_CHECKS, m.answered, MUSIC, () -> musicRead(level, m));
            } else {
                logFinished(level, m);
            }
        } else {
            for (String c : M_CHECKS) {
                answer(m.answered, c, null, "the clock jumped from care day " + m.day + " to " + today
                        + " between readings; A " + m.lastA + ", history " + m.history);
            }
        }
    }

    /** One reading inside the judged day. Returns false when the pen can go no further. */
    private static boolean dayNReading(ServerLevel level, Music m, long off) {
        m.readings++;
        if (!level.getBlockState(m.jukebox).is(Blocks.JUKEBOX)) {
            for (String c : M_CHECKS) {
                answer(m.answered, c, null, "the jukebox at " + m.jukebox.toShortString() + " is gone at reading "
                        + m.readings);
            }
            return false;
        }
        JukeboxBlockEntity jb = jukebox(level, m);
        boolean wasPlaying = playing(jb);
        boolean had = hasRecord(level, m);
        long sinceSetup = level.getGameTime() - m.setupAt;

        // A
        Horse a = findHorse(level, m.horses.get("A"));
        if (a == null) {
            answer(m.answered, M_RISES, null, "A is missing at reading " + m.readings + "; history " + m.history);
            answer(m.answered, M_CAP, null, "A is missing at reading " + m.readings + "; history " + m.history);
        } else {
            a.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
            HorseCareAttachment c = a.getData(ModAttachments.HORSE_CARE.get());
            int bond = c.bond();
            int got = bond - m.lastA;
            if (got != 0) {
                m.history.append(m.history.length() == 0 ? "" : " ").append('+').append(sinceSetup).append("t=")
                        .append(bond).append("(today ").append(c.bondToday()).append(')');
            }
            if (got > 0 && !wasPlaying) {
                m.finishedGainDayN += got;   // an interval that ended with the disc found finished
                m.finishedIntervalsDayN++;
            }
            if (m.firstGainAt < 0 && bond > START_BOND) {
                m.firstGainAt = sinceSetup;
            }
            m.maxToday = Math.max(m.maxToday, c.bondToday());
            if (c.bondToday() > HorseCareAttachment.DAILY_CAP) {
                answer(m.answered, M_CAP, false, "A's bondToday read " + c.bondToday() + " > DAILY_CAP "
                        + HorseCareAttachment.DAILY_CAP + " at +" + sinceSetup + " ticks, bond " + bond + " | history "
                        + m.history + " | " + musicConfig(m));
            } else if (m.capBond >= 0 && bond > m.capBond) {
                answer(m.answered, M_CAP, false, "A reached the cap (bondToday " + HorseCareAttachment.DAILY_CAP
                        + ") at bond " + m.capBond + " at +" + m.capAt + " ticks, then read " + bond + " at +"
                        + sinceSetup + " (bondToday " + c.bondToday() + ") | history " + m.history + " | "
                        + musicConfig(m));
            } else if (m.capBond < 0 && c.bondToday() >= HorseCareAttachment.DAILY_CAP) {
                m.capBond = bond;
                m.capAt = sinceSetup;
            }
            m.lastA = bond;
        }

        // B, C, D - nothing may move.
        String[][] controls = {{"B", M_UNTAMED}, {"C", M_CARRIER}, {"D", M_DISTANCE}};
        for (String[] ctl : controls) {
            String tag = ctl[0];
            String check = ctl[1];
            if (m.answered.contains(check)) {
                continue;
            }
            Horse h = findHorse(level, m.horses.get(tag));
            if (h == null) {
                answer(m.answered, check, null, tag + " is missing at reading " + m.readings);
                continue;
            }
            h.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
            HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
            if (c.bond() != START_BOND || c.bondToday() != 0) {
                answer(m.answered, check, false, m.label.get(tag) + " read bond " + c.bond() + ", bondToday "
                        + c.bondToday() + " at +" + sinceSetup + " ticks (reading " + m.readings + "), written "
                        + START_BOND + "/0 at setup, tamed " + h.isTamed() + "; A meanwhile " + m.lastA + " | "
                        + musicConfig(m));
            }
        }

        // Keep the music on through the judged day, so the test is about the gene and not the record.
        if (jb != null && (!wasPlaying || !had) && off < M_STOP_RESTART) {
            insertDisc(jb);
            m.restarts++;
        }
        return true;
    }

    private static void judgeDayN(ServerLevel level, Music m) {
        if (m.judged) {
            return;
        }
        m.judged = true;
        String common = "disc re-inserted " + m.restarts + " time(s); " + m.readings + " readings every "
                + M_READ + " ticks over care day " + m.day + " | " + musicConfig(m);
        int gain = m.lastA - START_BOND;
        if (m.lastA >= 0 && !m.answered.contains(M_RISES)) {
            answer(m.answered, M_RISES, gain > 0, "A " + START_BOND + " -> " + m.lastA + " (+" + gain + "); first"
                    + " point at +" + m.firstGainAt + " ticks, bondToday 15 at +" + m.capAt + " ticks"
                    + " (one point a beat would take " + HorseCareAttachment.DAILY_CAP * MusicEnjoyerGene.INTERVAL_TICKS
                    + "); history " + (m.history.length() == 0 ? "(never moved)" : m.history.toString())
                    + " | " + common);
        }
        if (!m.answered.contains(M_CAP)) {
            if (m.capBond < 0) {
                answer(m.answered, M_CAP, null, "A never reached the cap (gain +" + gain + ", highest bondToday "
                        + m.maxToday + ") - nothing for the cap to stop | history " + m.history + " | " + common);
            } else {
                answer(m.answered, M_CAP, m.lastA == m.capBond && m.maxToday <= HorseCareAttachment.DAILY_CAP,
                        "A hit bondToday " + HorseCareAttachment.DAILY_CAP + " at bond " + m.capBond + " at +"
                                + m.capAt + " ticks and read " + m.lastA + " at the end of the day; highest bondToday "
                                + m.maxToday + " | history " + m.history + " | " + common);
            }
        }
        String[][] controls = {{"B", M_UNTAMED}, {"C", M_CARRIER}, {"D", M_DISTANCE}};
        for (String[] ctl : controls) {
            if (!m.answered.contains(ctl[1])) {
                answer(m.answered, ctl[1], true, m.label.get(ctl[0]) + " held bond " + START_BOND
                        + ", bondToday 0 at every reading while A went " + START_BOND + " -> " + m.lastA + " | "
                        + common);
            }
        }
        m.lastDayNBond = m.lastA;
        ActionTrace.log("test yard", MUSIC + ": judged day " + m.day + " - in the " + m.finishedIntervalsDayN
                + " reading interval(s) that ended with the disc found finished (it was then re-inserted), A gained "
                + m.finishedGainDayN + " point(s). The disc is now left to finish; next day's readings follow.");
    }

    /** The day after: no re-inserting. A's bond while the jukebox holds a finished disc. */
    private static void nextDayReading(ServerLevel level, Music m, long off) {
        Horse a = findHorse(level, m.horses.get("A"));
        if (a == null) {
            return;
        }
        a.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
        HorseCareAttachment c = a.getData(ModAttachments.HORSE_CARE.get());
        JukeboxBlockEntity jb = jukebox(level, m);
        boolean finished = hasRecord(level, m) && !playing(jb);
        m.n1Readings++;
        if (m.n1Base < 0) {
            m.n1Base = c.bond();
            int last = m.lastDayNBond;
            int want = last <= m.floor || m.perDay <= 0 ? last : Math.max(m.floor, last - m.perDay);
            ActionTrace.log("test yard", MUSIC + ": care day " + (m.day + 1) + " began - A " + last + " -> "
                    + c.bond() + " across the rollover (decay at " + m.perDay + "/day to floor " + m.floor
                    + " predicts " + want + "; a gene point already this day would add to it), bondToday "
                    + c.bondToday() + ". Jukebox HAS_RECORD " + hasRecord(level, m) + ", playing " + playing(jb));
        } else if (finished && m.n1PrevFinished) {
            m.n1Gain += c.bond() - m.n1Prev;
        }
        if (finished) {
            m.n1FinishedReadings++;
        } else {
            m.n1PlayingReadings++;
        }
        m.n1Prev = c.bond();
        m.n1PrevFinished = finished;
    }

    private static void logFinished(ServerLevel level, Music m) {
        Horse a = findHorse(level, m.horses.get("A"));
        String end = a == null ? "A missing" : "A " + a.getData(ModAttachments.HORSE_CARE.get()).bond() + ", bondToday "
                + a.getData(ModAttachments.HORSE_CARE.get()).bondToday();
        ActionTrace.log("test yard", MUSIC + " FINISHED RECORD (the owner's open question - recorded, not judged):"
                + " over " + m.n1Readings + " readings in the first " + M_FINISHED_SPAN + " ticks of care day "
                + (m.day + 1) + ", the jukebox held a finished disc (HAS_RECORD true, not playing) at "
                + m.n1FinishedReadings + " and was playing at " + m.n1PlayingReadings + "; between consecutive"
                + " finished-disc readings A's bond rose " + m.n1Gain + " point(s) (from " + m.n1Base + "; " + end
                + "). " + (m.n1Gain > 0
                ? "A finished disc left in the jukebox KEEPS paying out, bounded only by the daily cap."
                : "A finished disc paid out nothing.")
                + " Judged day for comparison: " + m.finishedGainDayN + " point(s) in intervals that ended with the"
                + " disc finished.");
    }

    // ==================================================================
    // EAST - PACK LEADER
    // ==================================================================

    /*
     * PACK LEADER (wiki/gene-pack-leader.html, Verification tab, NOT played). The open checks it answers:
     *
     *   "A retinue forms. Spawn a matched pair for a mob you can put in a pen - cows are the case the cost pen
     *    already used - with several of them inside sixteen blocks, then ride or lead the horse away. Pass: they
     *    walk after it, and give up once it is well outside the radius."
     *   "A mismatched pair leads nothing. Two different following alleles, with both mobs in range. Pass: neither
     *    species follows, and the gene row reads the mismatched expression."
     *   "The cap bounds the pathfinding. Put more of the mob in range than PackLeaderGene.MAX_TARGETS. Pass: only
     *    that many trail the horse per beat and the rest carry on grazing."
     *   "Nothing is left behind. Kill or remove the horse while a retinue is following it. Pass: the followers go
     *    back to wandering at once, and no mob keeps walking toward where the horse was."
     *
     * WHAT THE CODE DOES, READ 2026-10-02. PackLeaderGene is a matched-pair locus over MobRoster.peaceful() (cow is
     * token Cow, sheep Shp); a matched pair grants MobAura("follow", "all", "minecraft:cow", RADIUS 16,
     * INTERVAL_TICKS 40, MAX_TARGETS 6). GeneAbilityHandler.mobAura runs on the horse's beat - (gameTime + (UUID
     * least-significant bits & 0x7FFFFFFF)) mod 40 == 0 - scans the 16-inflated box, skips anything that is not a
     * cow or is further than 16 (a sphere), stops after 6 touched, and for each touched cow calls followHorse:
     * within FOLLOW_STOP (3) it stops the cow's navigation and turns its head; otherwise
     * mob.getNavigation().moveTo(horse, 1.0). No goal is installed. So "following" is observable on the cow as
     * its navigation: PathNavigation.moveTo(Entity) builds a path to target.blockPosition() and records it as
     * getTargetPos(). A cow counts as RE-PATHED BY THIS BEAT when its navigation is in progress and its target is
     * exactly the leader's current block. The leader is moved one block every beat, one tick before the beat, to
     * a block it has not stood on before in the run, so a target left over from an earlier beat can never match -
     * the count is per beat, which is what the cap promises. (A cow's own random stroll landing on exactly the
     * leader's block is the one false positive; it is rare, and the mismatch check below allows for it.) The
     * beat is computed the way GeneAbilityHandler.beat does, and each sample runs in the server tick's Post
     * phase, after every entity has ticked - so right after the aura.
     *
     * THE PEN: a brick-walled plot x0+25..x0+44, z0..z0+12, registered as its own yard pen, ten persistent cows
     * at its east end (x0+39..42). The radius is 16 and the half is only 20 wide, so a second pen side by side
     * would sit inside the leader's radius and its cows would compete for the six slots: the mismatched horse
     * goes FIRST, in the same pen, with the same cows, walking the same steps, and is removed before the leader
     * arrives.
     *
     *   1. MISMATCH (Cow/Shp): spawned at build at (x0+28, z0+4); ten samples 40 ticks apart, starting ~100 ticks
     *      after build, the horse stepped one block before each. FAIL if any cow targets its current block at two
     *      consecutive samples, or three cows at once (a matched aura re-paths up to six every beat); single
     *      coincidences are listed. The horse's own pair is printed (the gene row's input).
     *   2. The cows are put back at their home blocks and the mismatched horse removed; 100 ticks later the
     *      LEADER (Cow/Cow) is spawned at the same spot. Beats 0-8: it shuffles round a 2x5 patch at the west end
     *      (cows 11-15 blocks off, all inside the radius). Beats 9-20: it walks east one block a beat to x0+40.
     *   3. At beat 20, right after the sample, the leader is killed (Entity.kill, as ModGameTests does), and the
     *      cows are read again one beat and two beats later.
     *
     * PASS / FAIL:
     *   FOLLOW   - PASS: at least one cow re-pathed to the leader on some beat; the ten cows' mean distance to it
     *              at beat 8 is below beat 0's; and the cows that were within 6 blocks of it at beat 8 are, at
     *              beat 20, closer to it on average than they would be had they stood still (their beat-8 spots
     *              against its beat-20 spot). FAIL otherwise, with every beat's numbers.
     *   CAP      - PASS: no beat re-pathed more than MAX_TARGETS (6) cows, with at least one beat that had more than
     *              6 cows inside the radius (otherwise INCONCLUSIVE - the cap was never asked to do anything).
     *   RELEASE  - PASS: two beats (80 ticks) after the kill, no cow's navigation is in progress toward the block
     *              the leader died on. FAIL: any is, with how far it still had to go.
     *   MISMATCH - as above.
     * Fewer than seven of the ten cows alive, or the leader gone before its time: INCONCLUSIVE. About 1,800
     * ticks (90 s) from build to the last verdict; deadline 6,000 ticks.
     */

    private static final String PACK = "PACK LEADER";
    private static final String P_FOLLOW = PACK + " - a Cow/Cow horse's cows re-path to it, close in, and walk"
            + " after it";
    private static final String P_CAP = PACK + " - no beat re-paths more than MAX_TARGETS cows";
    private static final String P_RELEASE = PACK + " - two beats after the leader dies no cow still paths to where"
            + " it was";
    private static final String P_MISMATCH = PACK + " - a mismatched Cow/Shp horse leads no cow";
    private static final List<String> P_CHECKS = List.of(P_FOLLOW, P_CAP, P_RELEASE, P_MISMATCH);

    private static final long BEAT = PackLeaderGene.INTERVAL_TICKS;
    private static final double RADIUS = PackLeaderGene.RADIUS;
    private static final int COWS = 10;
    private static final long P_DEADLINE = 6_000L;
    /** The last leader beat; the kill follows its sample. */
    private static final int LAST_BEAT = 20;
    private static final int GATHER_BEAT = 8;
    private static final int MISMATCH_SAMPLES = 10;

    /** Leader steps, relative to the pen's (x0, z0) - here x0 is the pen's west wall. Never the same block twice. */
    private static final int[][] STEPS = buildSteps();

    private static int[][] buildSteps() {
        List<int[]> s = new ArrayList<>();
        // beats 0-8: a 2x5 patch at the west end
        int[][] patch = {{3, 4}, {3, 5}, {3, 6}, {3, 7}, {3, 8}, {2, 8}, {2, 7}, {2, 6}, {2, 5}};
        for (int[] p : patch) {
            s.add(p);
        }
        // beats 9-20: east one block a beat along z0+6
        for (int x = 4; x <= 15; x++) {
            s.add(new int[]{x, 6});
        }
        return s.toArray(new int[0][]);
    }

    /** The cows' home blocks, relative to the pen's (x0, z0). */
    private static final int[][] HOMES = {{14, 4}, {14, 6}, {14, 8}, {15, 5}, {15, 7}, {16, 4}, {16, 6}, {16, 8},
            {17, 5}, {17, 7}};

    private static final class Pack {
        final int run;
        final Set<String> answered = new HashSet<>();
        final List<UUID> cows = new ArrayList<>();
        int gy;
        int x0;
        int z0;
        UUID mismatched;
        String mismatchedPair = "?";
        UUID leader;
        long phase;
        // mismatch
        final Map<UUID, Integer> misLast = new HashMap<>();
        final StringBuilder misLog = new StringBuilder();
        int misSample;
        int misMaxAtOnce;
        boolean misRepeat;
        // leader
        final StringBuilder beats = new StringBuilder();
        final Map<UUID, Vec3> pos0 = new HashMap<>();
        double mean0 = Double.NaN;
        double mean8 = Double.NaN;
        final Map<UUID, Vec3> group8 = new HashMap<>();
        int maxPathed;
        int maxInRadius;
        int beatsOverCapRadius;
        int beatsAnyFollow;
        BlockPos diedAt;

        Pack(int run) {
            this.run = run;
        }
    }

    private static void pack(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : P_CHECKS) {
            DebugYardClockwork.expect(c);
        }
        Pack p = new Pack(myRun);
        p.gy = gy;
        p.x0 = x0;
        p.z0 = z0;
        int x1 = x0 + 19;
        int z1 = z0 + 12;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        YardPens.register(gy, x0, x1, z0, z1, PACK);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("PACK LEADER", "10 cows: Cow/Shp", "first, then Cow/Cow", "walks, then dies"));

        for (int i = 0; i < HOMES.length; i++) {
            Entity e = DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + HOMES[i][0] + 0.5,
                    z0 + HOMES[i][1] + 0.5);
            if (e instanceof Mob cow) {
                cow.setCustomName(Component.literal(PACK + " cow " + (i + 1)));
                p.cows.add(cow.getUUID());
            }
        }
        Horse mis = DebugYardUnattended.horse(level, gy, x0 + STEPS[0][0] + 0.5, z0 + STEPS[0][1] + 0.5, Sex.FEMALE,
                PackLeaderGene.KEY + "=Cow/Shp", true, PACK + " MISMATCHED Cow/Shp");
        if (mis != null) {
            p.mismatched = mis.getUUID();
            AllelePair pair = HorseRecords.of(mis).genotype().pair(PackLeaderGene.KEY);
            if (pair != null) {
                p.mismatchedPair = pair.first().token() + "/" + pair.second().token();
            }
        }
        step(level, 100, myRun, P_CHECKS, p.answered, PACK, () -> mismatchSample(level, p));
        step(level, P_DEADLINE, myRun, P_CHECKS, p.answered, PACK, () -> {
            for (String c : P_CHECKS) {
                answer(p.answered, c, null, "deadline: mismatch samples " + p.misSample + " [" + p.misLog
                        + "]; leader beats [" + p.beats + "]");
            }
        });
    }

    private static List<Mob> allCowsNear(ServerLevel level, Entity at) {
        return level.getEntitiesOfClass(Mob.class, at.getBoundingBox().inflate(RADIUS + 1.0),
                m -> m.isAlive() && m.getType() == EntityType.COW);
    }

    private static List<Mob> myCows(ServerLevel level, Pack p) {
        List<Mob> out = new ArrayList<>();
        for (UUID id : p.cows) {
            Mob m = findMob(level, id);
            if (m != null) {
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Is this cow walking a path to {@code target} right now? UNVERIFIED in this repo: PathNavigation.isInProgress
     * and getTargetPos are used nowhere else here; read off the 26.1.2 sources (getTargetPos is the block a path
     * was last created for, set in createPath; isInProgress is "a path that is not done").
     */
    private static boolean pathingTo(Mob m, BlockPos target) {
        return m.getNavigation().isInProgress() && target.equals(m.getNavigation().getTargetPos());
    }

    private static double meanDistance(List<Mob> cows, Vec3 to) {
        if (cows.isEmpty()) {
            return Double.NaN;
        }
        double sum = 0;
        for (Mob m : cows) {
            sum += m.position().distanceTo(to);
        }
        return sum / cows.size();
    }

    /** Move a horse to a step's block, centred. Entity.teleportTo(x, y, z) as RealmBackup and GeneReactionHandler do. */
    private static void stepTo(Pack p, Horse h, int index) {
        int[] s = STEPS[index];
        h.teleportTo(p.x0 + s[0] + 0.5, p.gy + 1, p.z0 + s[1] + 0.5);
        h.setData(ModAttachments.HUNGER.get(), Hunger.FULL);
    }

    // ---- 1. the mismatched horse ----

    private static void mismatchSample(ServerLevel level, Pack p) {
        Horse h = findHorse(level, p.mismatched);
        if (h == null) {
            answer(p.answered, P_MISMATCH, null, "the mismatched horse is missing at sample " + p.misSample);
            startLeader(level, p);
            return;
        }
        if (myCows(level, p).size() < 7) {
            answer(p.answered, P_MISMATCH, null, "only " + myCows(level, p).size() + " of " + COWS + " cows alive");
            startLeader(level, p);
            return;
        }
        BlockPos at = h.blockPosition();
        int count = 0;
        int inRadius = 0;
        StringBuilder who = new StringBuilder();
        for (Mob m : allCowsNear(level, h)) {
            if (m.distanceToSqr(h) <= RADIUS * RADIUS) {
                inRadius++;
            }
            if (pathingTo(m, at)) {
                count++;
                Integer prev = p.misLast.get(m.getUUID());
                if (prev != null && prev == p.misSample - 1) {
                    p.misRepeat = true;
                }
                p.misLast.put(m.getUUID(), p.misSample);
                who.append(who.length() == 0 ? "" : ",").append(m.getName().getString());
            }
        }
        p.misMaxAtOnce = Math.max(p.misMaxAtOnce, count);
        p.misLog.append(p.misLog.length() == 0 ? "" : "; ").append('s').append(p.misSample).append(' ')
                .append(count).append('/').append(inRadius).append(" in radius, mean ")
                .append(f1(meanDistance(myCows(level, p), h.position())));
        if (count > 0) {
            p.misLog.append(" (").append(who).append(')');
        }
        p.misSample++;
        if (p.misSample < MISMATCH_SAMPLES) {
            // One block on, one tick before the next sample - the same walk the leader gets.
            step(level, BEAT - 1, p.run, P_CHECKS, p.answered, PACK, () -> {
                Horse again = findHorse(level, p.mismatched);
                if (again != null) {
                    stepTo(p, again, p.misSample);
                }
                step(level, 1, p.run, P_CHECKS, p.answered, PACK, () -> mismatchSample(level, p));
            });
            return;
        }
        boolean fail = p.misRepeat || p.misMaxAtOnce >= 3;
        answer(p.answered, P_MISMATCH, !fail, "horse pair " + p.mismatchedPair + "; " + MISMATCH_SAMPLES
                + " samples 40 ticks apart, the horse stepped a block before each; per sample cows pathing to its"
                + " block / cows in radius 16: " + p.misLog + " | most at once " + p.misMaxAtOnce + ", same cow two"
                + " samples running " + p.misRepeat + " (a working aura would re-path up to "
                + PackLeaderGene.MAX_TARGETS + " every beat)");
        h.discard();   // out of the leader's way; read and recorded
        startLeader(level, p);
    }

    // ---- 2. the leader ----

    private static void startLeader(ServerLevel level, Pack p) {
        for (int i = 0; i < p.cows.size() && i < HOMES.length; i++) {
            Mob m = findMob(level, p.cows.get(i));
            if (m != null) {
                m.getNavigation().stop();
                m.teleportTo(p.x0 + HOMES[i][0] + 0.5, p.gy + 1, p.z0 + HOMES[i][1] + 0.5);
            }
        }
        step(level, 100, p.run, P_CHECKS, p.answered, PACK, () -> {
            Horse lead = DebugYardUnattended.horse(level, p.gy, p.x0 + STEPS[0][0] + 0.5, p.z0 + STEPS[0][1] + 0.5,
                    Sex.FEMALE, PackLeaderGene.KEY + "=Cow/Cow", true, PACK + " LEADER Cow/Cow");
            if (lead == null) {
                for (String c : List.of(P_FOLLOW, P_CAP, P_RELEASE)) {
                    answer(p.answered, c, null, "the Cow/Cow leader could not be spawned");
                }
                return;
            }
            p.leader = lead.getUUID();
            p.phase = lead.getUUID().getLeastSignificantBits() & 0x7FFFFFFFL;
            // The first beat at least 40 ticks out, so the horse has joined and its abilities have resolved.
            long from = level.getGameTime() + 40;
            long beat0 = from + Math.floorMod(-(from + p.phase), BEAT);
            ActionTrace.log("test yard", PACK + ": leader " + lead.getName().getString() + " spawned at tick "
                    + level.getGameTime() + ", beat phase " + Math.floorMod(p.phase, BEAT) + ", first sampled beat "
                    + beat0 + "; cows home at x+14..17");
            step(level, beat0 - level.getGameTime(), p.run, P_CHECKS, p.answered, PACK, () -> leaderBeat(level, p, 0));
        });
    }

    private static void leaderBeat(ServerLevel level, Pack p, int k) {
        Horse lead = findHorse(level, p.leader);
        if (lead == null) {
            for (String c : List.of(P_FOLLOW, P_CAP, P_RELEASE)) {
                answer(p.answered, c, null, "the leader is gone at beat " + k + " (before the kill); beats ["
                        + p.beats + "]");
            }
            return;
        }
        List<Mob> mine = myCows(level, p);
        if (mine.size() < 7) {
            for (String c : List.of(P_FOLLOW, P_CAP, P_RELEASE)) {
                answer(p.answered, c, null, "only " + mine.size() + " of " + COWS + " cows alive at beat " + k
                        + "; beats [" + p.beats + "]");
            }
            return;
        }
        BlockPos at = lead.blockPosition();
        int pathed = 0;
        int inRadius = 0;
        int close = 0;
        for (Mob m : allCowsNear(level, lead)) {
            double d2 = m.distanceToSqr(lead);
            if (d2 <= RADIUS * RADIUS) {
                inRadius++;
            }
            if (d2 <= 3.5 * 3.5) {
                close++;
            }
            if (pathingTo(m, at)) {
                pathed++;
            }
        }
        double mean = meanDistance(mine, lead.position());
        p.maxPathed = Math.max(p.maxPathed, pathed);
        p.maxInRadius = Math.max(p.maxInRadius, inRadius);
        if (inRadius > PackLeaderGene.MAX_TARGETS) {
            p.beatsOverCapRadius++;
        }
        if (pathed > 0) {
            p.beatsAnyFollow++;
        }
        p.beats.append(p.beats.length() == 0 ? "" : "; ").append('b').append(k).append(" @").append(at.getX() - p.x0)
                .append(',').append(at.getZ() - p.z0).append(' ').append(pathed).append(" pathing/")
                .append(inRadius).append(" in radius/").append(close).append(" within 3.5, mean ").append(f1(mean));

        if (pathed > PackLeaderGene.MAX_TARGETS) {
            answer(p.answered, P_CAP, false, "beat " + k + " re-pathed " + pathed + " cows to the leader's block, cap "
                    + PackLeaderGene.MAX_TARGETS + " (" + inRadius + " in radius) | beats [" + p.beats + "]");
        }
        if (k == 0) {
            p.mean0 = mean;
            for (Mob m : mine) {
                p.pos0.put(m.getUUID(), m.position());
            }
        }
        if (k == GATHER_BEAT) {
            p.mean8 = mean;
            for (Mob m : mine) {
                if (m.position().distanceTo(lead.position()) <= 6.0) {
                    p.group8.put(m.getUUID(), m.position());
                }
            }
        }
        if (k < LAST_BEAT) {
            int next = k + 1;
            step(level, BEAT - 1, p.run, P_CHECKS, p.answered, PACK, () -> {
                Horse again = findHorse(level, p.leader);
                if (again != null) {
                    stepTo(p, again, next);
                }
                step(level, 1, p.run, P_CHECKS, p.answered, PACK, () -> leaderBeat(level, p, next));
            });
            return;
        }
        judgeFollow(level, p, lead, mine);
        // 3. the kill, right after the last beat's sample: every re-pathed cow is mid-walk to this block.
        p.diedAt = at;
        int before = 0;
        for (Mob m : allCowsNear(level, lead)) {
            if (pathingTo(m, at)) {
                before++;
            }
        }
        int pathingAtKill = before;
        lead.kill(level);
        step(level, BEAT, p.run, P_CHECKS, p.answered, PACK, () -> {
            String one = releaseCount(level, p);
            step(level, BEAT, p.run, P_CHECKS, p.answered, PACK, () -> judgeRelease(level, p, pathingAtKill, one));
        });
    }

    private static void judgeFollow(ServerLevel level, Pack p, Horse lead, List<Mob> mine) {
        double cfSum = 0;
        double nowSum = 0;
        int n = 0;
        for (Map.Entry<UUID, Vec3> e : p.group8.entrySet()) {
            Mob m = findMob(level, e.getKey());
            if (m == null) {
                continue;
            }
            cfSum += e.getValue().distanceTo(lead.position());
            nowSum += m.position().distanceTo(lead.position());
            n++;
        }
        double cf = n == 0 ? Double.NaN : cfSum / n;
        double got = n == 0 ? Double.NaN : nowSum / n;
        boolean some = p.beatsAnyFollow > 0;
        boolean closed = p.mean8 < p.mean0;
        boolean walked = n > 0 && got < cf;
        answer(p.answered, P_FOLLOW, some && closed && walked, "cows re-pathed to the leader on " + p.beatsAnyFollow
                + " of " + (LAST_BEAT + 1) + " beats (most in one beat " + p.maxPathed + "); mean distance of the "
                + mine.size() + " cows beat 0 " + f1(p.mean0) + " -> beat " + GATHER_BEAT + " " + f1(p.mean8)
                + (closed ? " (closed in)" : " (DID NOT close in)") + "; the " + n + " cow(s) within 6 at beat "
                + GATHER_BEAT + " are " + f1(got) + " from it at beat " + LAST_BEAT + " after it walked "
                + (STEPS[LAST_BEAT][0] - STEPS[GATHER_BEAT][0]) + " blocks east, against " + f1(cf)
                + " had they stood still" + (walked ? " (walked after it)" : " (DID NOT walk after it)")
                + " | beats [" + p.beats + "]");
        if (!p.answered.contains(P_CAP)) {
            if (p.beatsOverCapRadius == 0) {
                answer(p.answered, P_CAP, null, "no beat had more than " + PackLeaderGene.MAX_TARGETS
                        + " cows inside the radius (most " + p.maxInRadius + ") - the cap was never asked to act;"
                        + " most re-pathed in one beat " + p.maxPathed + " | beats [" + p.beats + "]");
            } else {
                answer(p.answered, P_CAP, true, "most cows re-pathed in one beat " + p.maxPathed + " (cap "
                        + PackLeaderGene.MAX_TARGETS + ") on " + p.beatsOverCapRadius + " beat(s) with more than "
                        + PackLeaderGene.MAX_TARGETS + " in radius (most " + p.maxInRadius + ") | beats [" + p.beats
                        + "]");
            }
        }
    }

    // ---- 3. after the kill ----

    private static String releaseCount(ServerLevel level, Pack p) {
        int n = 0;
        StringBuilder who = new StringBuilder();
        for (Mob m : myCows(level, p)) {
            if (pathingTo(m, p.diedAt)) {
                n++;
                who.append(who.length() == 0 ? "" : ", ").append(m.getName().getString()).append(' ')
                        .append(f1(Math.sqrt(m.distanceToSqr(feet(p.diedAt))))).append(" off");
            }
        }
        return n + (n == 0 ? "" : " (" + who + ")");
    }

    private static void judgeRelease(ServerLevel level, Pack p, int atKill, String oneBeat) {
        Horse lead = findHorse(level, p.leader);
        if (lead != null) {
            answer(p.answered, P_RELEASE, null, "the leader is still alive after kill() - nothing was released");
            return;
        }
        String two = releaseCount(level, p);
        boolean none = two.startsWith("0");
        List<Mob> mine = myCows(level, p);
        answer(p.answered, P_RELEASE, none, "leader killed on block " + (p.diedAt.getX() - p.x0) + ","
                + (p.diedAt.getZ() - p.z0) + " with " + atKill + " cow(s) pathing to it; still pathing there one beat"
                + " later " + oneBeat + ", two beats later " + two + "; the " + mine.size() + " cows' mean distance"
                + " to that block now " + f1(meanDistance(mine, feet(p.diedAt))));
    }
}
