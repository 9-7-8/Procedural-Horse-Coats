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
 * <b>Row BB west: MUSIC, a social gene that answers for itself</b> (2026-10-02) - four horses in glass cells
 * round a jukebox, their bond read straight off the care attachment for one day of the care clock. PACK LEADER
 * stood in the east half until 2026-10-08, when its last check (the release of a dead leader's followers, #213)
 * passed on three launches running and the pen went; its four checks are on wiki/gene-pack-leader.html.
 *
 * <p>It starts itself at build, runs on {@link DebugYardHerd#after}, and end in
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
        ActionTrace.log("test yard", "row BB built (west: MUSIC) at game tick "
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
}
