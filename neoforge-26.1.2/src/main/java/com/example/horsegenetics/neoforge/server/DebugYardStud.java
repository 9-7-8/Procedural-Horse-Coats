package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproState;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row AU: natural breeding, judged by the pens themselves</b> (2026-10-01). Three open checks that need
 * nothing but a stallion, mares and time, so they left the "needs a person" pile: a gelding passed over, the
 * names a herd of foals gets, and whether the cover path test lets a pair breed through a gate, round a corner
 * and pressed together in a stall.
 *
 * <table>
 *   <tr><th>half</th><th>pens</th></tr>
 *   <tr><td>west, x0 .. x0+19</td><td>GELDING (x0 .. x0+8), two-block air gap, FOAL NAMES (x0+11 .. x0+19)</td></tr>
 *   <tr><td>east, x0+25 .. x0+44</td><td>COVER REACH: GATE (x0+25 .. x0+31), CORNER (x0+32 .. x0+41),
 *       STALL (x0+42 .. x0+44)</td></tr>
 * </table>
 *
 * <h2>How a pen here makes a pair breed with nobody there</h2>
 * Exactly as {@code DebugYardLong}'s ratio pens do: horses stocked tame, each pen registered with
 * {@link YardPens#register} (a horse notices only the horses of its own pen, so two pens a wall apart never
 * cover each other), and the mares put in heat with {@link DebugYardFertility#inHeat}.
 * {@code HerdGoals.HeatAttraction} walks an idle mare in heat to the nearest <i>entire</i> stallion within 16
 * blocks and stops them 2.5 apart; {@link NaturalBreedingHandler} then covers her on its two-second scan once the
 * same stallion has stood within {@link ReproRules#NATURAL_REACH} of her for three seconds, and only if
 * {@code canMeet} finds a walkable path to within a block of him or the two hitboxes already touch. No golden
 * carrots anywhere in this row: vanilla's carrot breeding ignores {@code YardPens}.
 *
 * <p><b>Every pen reads state, never the log.</b> A pregnancy is {@code ReproHandler.of(mare).pregnant()}; a
 * cover is her {@code lastNaturalTry} moving ({@code NaturalBreedingHandler.cover} stamps it before the roll,
 * so it moves on a cover that does not take too); a foal is a baby whose record's {@code motherId} is one of
 * the pen's mares. The handler's own lines - {@code [fertility] natural cover: ...} from {@code cover()}, and
 * {@code ... not covered: no walkable path to ...} for a yard mare whose path test failed - are still in the log
 * beside these verdicts for the morning reader.
 *
 * <p><b>Mares are kept in heat.</b> Each look, a pen mare found in {@link ReproState#DIESTRUS} is put back in
 * heat. A pregnant, just-foaled or foal-heat mare is left to her own clock. With {@code debug.tools} on, a heat
 * and a pregnancy are each about a minute, so a mare can be covered every few minutes.
 *
 * <p>Timings: GELDING answers at 60 minutes (or FAILs the moment a gelding-pen mare is pregnant); FOAL NAMES
 * at its eighth foal, typically within the first hour, and INCONCLUSIVE at three hours; each COVER REACH pen
 * PASSes at its mare's first pregnancy, usually within minutes, and is judged otherwise at 30 minutes.
 */
final class DebugYardStud {

    private DebugYardStud() {
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
            ActionTrace.log("test yard", "row AU built (west: GELDING, FOAL NAMES; east: COVER REACH gate,"
                    + " corner, stall)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AU (natural breeding) failed to build", e);
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

    /** A brick-walled pen with the yard's usual closed north gate and water, its sign, and its own group. */
    private static void plot(ServerLevel level, int gy, int xa, int xb, int za, int zb, String group,
                             int signX, List<String> sign) {
        DebugTestYard.fencedPlot(level, gy, xa, xb, za, zb);
        DebugPenManager.placeSign(level, new BlockPos(signX, gy + 1, za - 1), Direction.NORTH, sign);
        YardPens.register(gy, xa, xb, za, zb, group);
    }

    private static @Nullable Horse find(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static @Nullable UUID idOf(@Nullable Horse h) {
        return h == null ? null : h.getUUID();
    }

    /** Back in heat if she has dropped out of it; a pregnant or just-foaled mare is left alone. True if re-heated. */
    private static boolean keepInHeat(Horse mare) {
        if (ReproHandler.stateOf(mare) == ReproState.DIESTRUS) {
            DebugYardFertility.inHeat(mare);
            return true;
        }
        return false;
    }

    /** {@link ReproRules#mayTryNaturally} on her own clock: the handler's first gate, the cheap one. */
    private static boolean mayTry(Horse mare) {
        return ReproRules.mayTryNaturally(ReproHandler.of(mare), HorseRealmRepro.reproTime(mare),
                ServerConfig.reproTiming());
    }

    private static String minutes(long ticks) {
        return String.format(Locale.ROOT, "%.1f min", ticks / 1200.0);
    }

    // ------------------------------------------------------------------
    // WEST - GELDING and FOAL NAMES
    // ------------------------------------------------------------------

    /*
     * GELDING (wiki/item-vet-kit.html, Verification tab, "A gelding and a mare in heat"):
     *   "Pen a gelding with a mare in heat: she is never covered. The SEED JAR pen does not ask this. Golden
     *    carrots on the pair still give a foal, which is correct and is the thing most likely to be misread as
     *    the gelding not having taken - check the cover, not the carrots."
     *
     * A stallion and two mares, tamed; the stallion is tamed TO THE HANDS (a FakePlayer UUID is the one kind of
     * owner the clock can be) and gelded the real way, a sneak-use of the vet's kit through Player.interactOn,
     * which is what VetKitHandler hears: male, not already gelded, tamed, and owned by the user. The record's
     * gelded flag is read straight after; if it did not take, the check is INCONCLUSIVE - the setup failed and
     * there is no gelding to test. Both mares are held OUT of heat until then, so the stallion cannot cover one
     * in the second before he is gelded. Then the mares go into heat and are kept there for an hour; no carrots.
     *
     * PASS at 60 minutes: no pregnancy and no foal in the gelding pen, the control pen (FOAL NAMES, same setup
     * with an entire stallion) got at least one mare pregnant, and at least one look saw a gelding-pen mare who
     * could still be covered this heat standing within NATURAL_REACH of the gelding - so the gate was asked and
     * said no. FAIL the moment a gelding-pen mare is pregnant or has a foal. INCONCLUSIVE: the geld did not take,
     * a horse went missing, the control never got a mare pregnant (the pens cannot breed at all, so a quiet
     * gelding proves nothing), or the gelding and a receptive mare were never within reach of each other.
     *
     * One hour. Nothing draws a mare to a gelding (HeatAttraction looks for an entire horse), so the "in reach"
     * count is the pen's random wandering in a 7x11 floor; a look is every five seconds.
     *
     * FOAL NAMES (wiki/breeding.html, Verification tab, "Foal names, and one policy per player"):
     *   "The default, read off a herd rather than a pair. Breed one pair three or four times over. Every filly
     *    should carry her dam's last name and every colt his sire's, and no two foals should share a full name -
     *    that second half is the whole point of the change, and it is what the old schedule failed at only once a
     *    herd existed."
     *
     * WHICH POLICY APPLIES HERE. HorseBreedingHandler.populateFoal names a foal with HorseNames.foal under
     * namingPolicyFor(dam, breeder): the dam's owner's policy, else the breeder's, from HorseNamingData.policyFor.
     * These horses are setTamed(true) by DebugPenManager.spawnHorse with no owner at all, so
     * HorseOwnership.ownerId(dam) is null; a natural cover's bredBy is the mare's owner name or her record's
     * tamedBy, both empty, so bornFromPregnancy finds no breeder player either; and policyFor(null) is
     * NamingPolicy.DEFAULT - LAST half inherited, BY_SEX: a filly takes her dam's last name, a colt his sire's,
     * and the first name is rolled from the alpha table. That is exactly the default the wiki check asks about,
     * and no client's naming.* setting can reach it (even a horse owned by the Hands would get DEFAULT: their
     * UUID never sends a NamingPolicyPayload).
     *
     * An entire stallion and two mares; the control for GELDING too. Every foal of these two mares (matched by
     * its record's motherId, as DebugYardLong.count does) is read once - its sex, its full name, its dam's and
     * sire's names - and taken away two minutes after it was first seen, so the pen never nears the crowding cap.
     * PASS at the eighth foal if every filly's last name is her dam's, every colt's is the sire's, and the eight
     * full names are all different. FAIL at the first foal with the wrong last name, or the first full name that
     * repeats an earlier foal's. Note for the reader of a duplicate: the rolled first name is one word of several
     * hundred, so two same-surname siblings collide by pure chance about once in several hundred pairs - across
     * eight foals that is a percent or two, which the detail says. INCONCLUSIVE at three hours with the count, or
     * if a mare or the stallion is gone, or if a foal's father is not this pen's stallion.
     */

    private static final String GELDING = "GELDING - a gelding penned for an hour with two mares kept in heat"
            + " never covers either";
    private static final String NAMES = "FOAL NAMES - every filly carries her dam's last name, every colt his"
            + " sire's, and no two of eight foals share a full name";

    /** A look every five seconds - a pregnancy lasts about a minute, so none slips between two. */
    private static final long WEST_LOOK = 100L;
    private static final long GELDING_HOURS = 60L * 1200;
    private static final long NAMES_DEADLINE = 3L * 60 * 1200;
    private static final int NAMES_FOALS = 8;
    /** A foal is read, then taken away this long after it was first seen (DebugYardLong.FOAL_KEEP). */
    private static final long FOAL_KEEP = 2400L;
    /** Looks in a row a horse must be missing before it counts as gone - thirty seconds, not a chunk blink. */
    private static final int GONE_LOOKS = 6;

    private static final class West {
        final int run;
        final int gy;
        final AABB box;
        @Nullable UUID gelding;
        final List<UUID> geldMares = new ArrayList<>();
        @Nullable UUID stud;
        final List<UUID> controlMares = new ArrayList<>();
        /** Record ids, which is what a foal's motherId holds. */
        final Map<UUID, String> damLast = new LinkedHashMap<>();
        final Map<UUID, String> damName = new LinkedHashMap<>();
        final Set<UUID> geldDams = new HashSet<>();
        @Nullable UUID studRecord;
        String sireLast = "";
        String sireName = "";
        long start;
        final Map<UUID, Boolean> wasPregnant = new LinkedHashMap<>();
        int geldPregnancies;
        int controlPregnancies;
        int geldFoals;
        int inReachLooks;
        int receptiveLooks;
        int reheats;
        int looks;
        int geldMissing;
        int controlMissing;
        // foal names
        final Map<UUID, Long> living = new LinkedHashMap<>();
        final Set<UUID> seen = new HashSet<>();
        final List<String> fullNames = new ArrayList<>();
        final StringBuilder foalLog = new StringBuilder();
        int fillies;
        int colts;

        West(int run, int gy, AABB box) {
            this.run = run;
            this.gy = gy;
            this.box = box;
        }
    }

    private static void west(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        int z1 = z0 + 12;
        DebugYardClockwork.expect(GELDING);
        DebugYardClockwork.expect(NAMES);

        // GELDING: walls x0 .. x0+8. FOAL NAMES: walls x0+11 .. x0+19. Air between, x0+9 and x0+10.
        plot(level, gy, x0, x0 + 8, z0, z1, "AU GELDING", x0 + 1,
                List.of("GELDING", "2 mares in heat", "an hour: never", "covered?"));
        plot(level, gy, x0 + 11, x0 + 19, z0, z1, "AU FOAL NAMES", x0 + 12,
                List.of("FOAL NAMES", "filly: dam's last", "colt: sire's", "no repeats"));

        // An empty code is every gene at its default: the plainest horse, nothing lethal, ordinary fertility.
        String plain = "";
        Horse gelding = DebugYardClockwork.horse(level, gy, x0 + 4.5, z0 + 6.5, Sex.MALE, plain, "GELDING");
        Horse gm1 = DebugYardClockwork.horse(level, gy, x0 + 2.5, z0 + 3.5, Sex.FEMALE, plain, "GELDING MARE 1");
        Horse gm2 = DebugYardClockwork.horse(level, gy, x0 + 6.5, z0 + 9.5, Sex.FEMALE, plain, "GELDING MARE 2");
        Horse stud = DebugYardClockwork.horse(level, gy, x0 + 15.5, z0 + 6.5, Sex.MALE, plain, "NAMES STUD");
        Horse cm1 = DebugYardClockwork.horse(level, gy, x0 + 13.5, z0 + 3.5, Sex.FEMALE, plain, "NAMES MARE 1");
        Horse cm2 = DebugYardClockwork.horse(level, gy, x0 + 17.5, z0 + 9.5, Sex.FEMALE, plain, "NAMES MARE 2");

        West w = new West(thisRun, gy, DebugTestYard.box(x0, gy, z0, x0 + 19, gy + 4, z1));
        w.gelding = idOf(gelding);
        w.stud = idOf(stud);
        for (Horse m : new Horse[] {gm1, gm2}) {
            if (m != null) {
                w.geldMares.add(m.getUUID());
                DebugYardFertility.outOfHeat(m);
            }
        }
        for (Horse m : new Horse[] {cm1, cm2}) {
            if (m != null) {
                w.controlMares.add(m.getUUID());
                DebugYardFertility.outOfHeat(m);
            }
        }
        if (gelding != null) {
            // Tamed to the hands, the one owner a FakePlayer can be - VetKitHandler.geld wants the user to own him.
            // (The same two calls the SEED JAR pen made before it was closed, git d2fdb272^.)
            gelding.setTamed(true);
            gelding.setOwner(new DebugYardClockwork.Hands(level));
        }

        // Two seconds on: geld him, confirm the record, then start both pens' clocks together.
        DebugYardHerd.after(level, 40, () -> {
            if (w.run != run) {
                return;
            }
            westStart(level, w);
        });
    }

    private static void westStart(ServerLevel level, West w) {
        Horse gelding = find(level, w.gelding);
        Horse stud = find(level, w.stud);
        List<Horse> gMares = new ArrayList<>();
        for (UUID id : w.geldMares) {
            Horse m = find(level, id);
            if (m != null) {
                gMares.add(m);
            }
        }
        List<Horse> cMares = new ArrayList<>();
        for (UUID id : w.controlMares) {
            Horse m = find(level, id);
            if (m != null) {
                cMares.add(m);
            }
        }
        boolean controlOk = stud != null && cMares.size() == 2;
        if (!controlOk) {
            inconclusive(NAMES, "at build: the pen is not stocked - stallion " + (stud != null) + ", "
                    + cMares.size() + " of 2 mares found");
        }

        // THE GELD, the real way: a sneak-use of the vet's kit by the hands that own him.
        if (gelding == null || gMares.size() != 2) {
            inconclusive(GELDING, "at build: the pen is not stocked - gelding " + (gelding != null) + ", "
                    + gMares.size() + " of 2 mares found");
        } else {
            DebugYardClockwork.Hands kit = DebugYardClockwork.hands(level, gelding,
                    new ItemStack(ModItems.VET_KIT.get()), false);
            // isSecondaryUseActive() is what VetKitHandler asks; for a player that is the sneak key.
            kit.setShiftKeyDown(true);
            DebugYardClockwork.use(kit, gelding);
            HorseRecord r = HorseRecords.of(gelding);
            boolean gelded = r.gelded();
            ActionTrace.log("test yard", "GELDING: the vet's kit used sneaking on " + ActionTrace.describeShort(gelding)
                    + " - record gelded " + gelded + ", entire " + r.entire() + "; kit " + kit.said());
            if (!gelded) {
                inconclusive(GELDING, "the geld did not take: record gelded false after the sneak-use of the kit"
                        + " (tamed " + gelding.isTamed() + ", owned by the hands "
                        + HorseOwnership.isOwner(gelding, kit.getUUID()) + "); kit " + kit.said());
            }
            for (Horse m : gMares) {
                w.geldDams.add(HorseRecords.of(m).id());
                if (ReproHandler.of(m).pregnant()) {
                    inconclusive(GELDING, ActionTrace.describeShort(m) + " was already pregnant when the clock"
                            + " started, before the gelding could have covered her");
                }
            }
        }
        if (controlOk) {
            HorseRecord sr = HorseRecords.of(stud);
            w.studRecord = sr.id();
            w.sireLast = sr.lastName();
            w.sireName = sr.displayName();
            for (Horse m : cMares) {
                HorseRecord mr = HorseRecords.of(m);
                w.damLast.put(mr.id(), mr.lastName());
                w.damName.put(mr.id(), mr.displayName());
                if (mr.lastName().equals(sr.lastName())) {
                    ActionTrace.log("test yard", "FOAL NAMES: note - dam " + mr.displayName() + " and sire "
                            + sr.displayName() + " share the last name " + mr.lastName()
                            + ", so her foals cannot show which parent a surname came from");
                }
            }
            ActionTrace.log("test yard", "FOAL NAMES: sire " + sr.displayName() + ", dams " + w.damName.values()
                    + "; naming policy is NamingPolicy.DEFAULT (no owner, no breeder): filly takes dam's last,"
                    + " colt sire's, first rolled");
        }
        if (answered(GELDING) && answered(NAMES)) {
            return;
        }
        w.start = level.getGameTime();
        for (UUID id : w.geldMares) {
            Horse m = find(level, id);
            if (m != null) {
                DebugYardFertility.inHeat(m);
            }
        }
        for (UUID id : w.controlMares) {
            Horse m = find(level, id);
            if (m != null) {
                DebugYardFertility.inHeat(m);
            }
        }
        westLook(level, w);
    }

    private static void westLook(ServerLevel level, West w) {
        DebugYardHerd.after(level, WEST_LOOK, () -> {
            if (w.run != run) {
                return;
            }
            if (answered(GELDING) && answered(NAMES)) {
                westFinish(level, w);
                return;
            }
            westLook(level, w);     // first, so a throw below cannot stop the clock
            long now = level.getGameTime();
            w.looks++;

            // The mares: kept in heat, and every new pregnancy counted.
            int geldPresent = 0;
            for (UUID id : w.geldMares) {
                Horse m = find(level, id);
                if (m != null) {
                    geldPresent++;
                    mareLook(w, m, true);
                }
            }
            int controlPresent = 0;
            for (UUID id : w.controlMares) {
                Horse m = find(level, id);
                if (m != null) {
                    controlPresent++;
                    mareLook(w, m, false);
                }
            }

            // Was the gate asked? A gelding-pen mare who could still be covered this heat, within reach of him.
            Horse gelding = find(level, w.gelding);
            if (gelding != null) {
                double reach2 = ReproRules.NATURAL_REACH * ReproRules.NATURAL_REACH;
                for (UUID id : w.geldMares) {
                    Horse m = find(level, id);
                    if (m != null && ReproHandler.receptive(m)) {
                        w.receptiveLooks++;
                        if (mayTry(m) && m.distanceToSqr(gelding) < reach2) {
                            w.inReachLooks++;
                        }
                    }
                }
            }
            w.geldMissing = gelding == null || geldPresent < 2 ? w.geldMissing + 1 : 0;
            w.controlMissing = find(level, w.stud) == null || controlPresent < 2 ? w.controlMissing + 1 : 0;

            foals(level, w, now);

            if (w.geldMissing >= GONE_LOOKS) {
                inconclusive(GELDING, "at " + minutes(now - w.start) + ": the gelding or a mare of his pen is gone ("
                        + geldPresent + " of 2 mares); " + geldingCounts(w));
            }
            if (w.controlMissing >= GONE_LOOKS) {
                inconclusive(NAMES, "at " + minutes(now - w.start) + ": the stallion or a mare is gone; "
                        + namesCounts(w));
                if (w.controlPregnancies == 0) {
                    inconclusive(GELDING, "at " + minutes(now - w.start) + ": the control pen lost a horse before"
                            + " any pregnancy; " + geldingCounts(w));
                }
            }
            if (!answered(GELDING) && now - w.start >= GELDING_HOURS) {
                geldingVerdict(w, now);
            }
            if (!answered(NAMES) && now - w.start >= NAMES_DEADLINE) {
                inconclusive(NAMES, "at " + minutes(now - w.start) + ": only " + w.seen.size() + " of "
                        + NAMES_FOALS + " foals; " + namesCounts(w));
            }
        });
    }

    private static void mareLook(West w, Horse m, boolean geldingPen) {
        if (keepInHeat(m)) {
            w.reheats++;
        }
        boolean pregnant = ReproHandler.of(m).pregnant();
        boolean was = Boolean.TRUE.equals(w.wasPregnant.put(m.getUUID(), pregnant));
        if (pregnant && !was) {
            if (geldingPen) {
                w.geldPregnancies++;
                fail(GELDING, ActionTrace.describeShort(m) + " is pregnant at " + minutes(m.level().getGameTime()
                        - w.start) + " in the gelding's pen; " + geldingCounts(w));
            } else {
                w.controlPregnancies++;
                ActionTrace.log("test yard", "FOAL NAMES: " + ActionTrace.describeShort(m) + " pregnant (control"
                        + " pregnancy " + w.controlPregnancies + ")");
            }
        }
    }

    private static String geldingCounts(West w) {
        return w.geldPregnancies + " pregnancies and " + w.geldFoals + " foals in the gelding pen; "
                + w.inReachLooks + " looks of " + w.looks + " found a mare who could still be covered this heat"
                + " within " + ReproRules.NATURAL_REACH + " blocks of him (" + w.receptiveLooks
                + " mare-looks in heat); control pen " + w.controlPregnancies + " pregnancies; " + w.reheats
                + " re-heats across both pens";
    }

    private static void geldingVerdict(West w, long now) {
        String at = "at " + minutes(now - w.start) + ": ";
        if (w.geldPregnancies > 0 || w.geldFoals > 0) {
            fail(GELDING, at + geldingCounts(w));
        } else if (w.controlPregnancies == 0) {
            inconclusive(GELDING, at + "the control pen never got a mare pregnant either, so the pens could not"
                    + " breed at all; " + geldingCounts(w));
        } else if (w.inReachLooks == 0) {
            inconclusive(GELDING, at + "no look ever found a mare who could be covered within reach of the gelding,"
                    + " so the cover was never on offer; " + geldingCounts(w));
        } else {
            pass(GELDING, at + geldingCounts(w));
        }
    }

    // ------------------------------------------------------------------
    // Foals
    // ------------------------------------------------------------------

    private static void foals(ServerLevel level, West w, long now) {
        for (Horse foal : level.getEntitiesOfClass(Horse.class, w.box.inflate(0.0, 2.0, 0.0),
                h -> h.isBaby() && h.isAlive() && HorseRecords.hasRealRecord(h))) {
            UUID id = foal.getUUID();
            if (!w.seen.add(id)) {
                continue;
            }
            HorseRecord fr = HorseRecords.of(foal);
            UUID mother = fr.motherId().orElse(null);
            if (mother != null && w.geldDams.contains(mother)) {
                w.geldFoals++;
                w.living.put(id, now);
                fail(GELDING, "a foal, " + fr.displayName() + ", of gelding-pen mare " + mother + " at "
                        + minutes(now - w.start) + "; " + geldingCounts(w));
                continue;
            }
            if (mother == null || !w.damLast.containsKey(mother)) {
                w.seen.remove(id);  // not one of ours - some other pen's, or a stray; never counted
                continue;
            }
            w.living.put(id, now);
            nameFoal(w, fr, mother);
        }
        for (var it = w.living.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (now - e.getValue() >= FOAL_KEEP) {
                Horse h = find(level, e.getKey());
                if (h != null) {
                    h.discard();    // read and recorded: out of the way of the crowding cap
                }
                it.remove();
            }
        }
    }

    private static void nameFoal(West w, HorseRecord fr, UUID mother) {
        boolean filly = fr.sex() == Sex.FEMALE;
        String full = (fr.firstName() + " " + fr.lastName()).strip();
        String parentLast = filly ? w.damLast.get(mother) : w.sireLast;
        String parent = filly ? "dam " + w.damName.get(mother) : "sire " + w.sireName;
        int n = w.fullNames.size() + 1;
        boolean dup = w.fullNames.contains(full);
        boolean rightLast = fr.lastName().equals(parentLast);
        boolean ourSire = w.studRecord != null && fr.fatherId().filter(w.studRecord::equals).isPresent();
        if (filly) {
            w.fillies++;
        } else {
            w.colts++;
        }
        w.fullNames.add(full);
        String line = (filly ? "filly" : "colt") + " '" + full + "' of dam " + w.damName.get(mother);
        w.foalLog.append(w.foalLog.length() == 0 ? "" : "; ").append(line);
        ActionTrace.log("test yard", "FOAL NAMES: foal " + n + " is a " + line + " by sire " + w.sireName
                + " - last name " + (rightLast ? "is" : "is NOT") + " her/his " + parent + "'s ('" + parentLast + "')"
                + (dup ? " - and the full name REPEATS an earlier foal's" : ""));
        if (!ourSire) {
            inconclusive(NAMES, "foal " + n + " (" + full + ") has father " + fr.fatherId().map(UUID::toString)
                    .orElse("none") + ", not this pen's stallion " + w.studRecord + "; " + namesCounts(w));
            return;
        }
        if (!rightLast) {
            fail(NAMES, "foal " + n + " is a " + (filly ? "filly" : "colt") + " named '" + full + "', but her/his "
                    + parent + " has the last name '" + parentLast + "'; " + namesCounts(w));
            return;
        }
        if (dup) {
            fail(NAMES, "foal " + n + " repeats the full name '" + full + "' of an earlier foal (the rolled first"
                    + " name is one word of several hundred, so a chance repeat across " + n + " foals is a percent"
                    + " or two - read the foal list before blaming the rule); " + namesCounts(w));
            return;
        }
        if (n >= NAMES_FOALS) {
            pass(NAMES, n + " foals, every filly with her dam's last name and every colt with the sire's, all "
                    + n + " full names different; " + namesCounts(w));
        }
    }

    private static String namesCounts(West w) {
        return w.fullNames.size() + " foals (" + w.fillies + " fillies, " + w.colts + " colts), "
                + w.controlPregnancies + " pregnancies; sire " + w.sireName + ", dams " + w.damName.values()
                + "; foals: " + (w.foalLog.length() == 0 ? "(none)" : w.foalLog);
    }

    /** Both west checks have answered: take the last foals away and stop the pens breeding all night. */
    private static void westFinish(ServerLevel level, West w) {
        for (UUID id : w.living.keySet()) {
            Horse h = find(level, id);
            if (h != null) {
                h.discard();
            }
        }
        w.living.clear();
        for (UUID id : w.geldMares) {
            DebugYardFertility.noNaturalCovers(find(level, id));
        }
        for (UUID id : w.controlMares) {
            DebugYardFertility.noNaturalCovers(find(level, id));
        }
    }

    // ------------------------------------------------------------------
    // EAST - COVER REACH
    // ------------------------------------------------------------------

    /*
     * COVER REACH (wiki/fertility.html, "Covers no longer reach through walls (2026-09-25; the wall seen in the
     * yard, the rest NOT played)"):
     *   "A gate does not. Same pair with an open gate between them. Pass: a cover, as before. This is the one that
     *    matters: the test must not have made ordinary penned breeding impossible."
     *   "Round a corner still works. Two horses at opposite ends of one L-shaped pen, out of sight of each other.
     *    Pass: a cover - this is why it is a path and not a line of sight."
     *   "Touching still works. Two horses pressed together in a one-wide stall, where there may be no path to
     *    compute at all. Pass: a cover, by way of the overlapping-hitbox fallback."
     * (The fourth bullet, the cost of thirty horses in heat, is a tick-lag measurement and is not asked here.)
     *
     * Three sub-pens, each ONE YardPens group holding both halves, each with a tamed stallion and a mare kept in
     * heat:
     *   GATE   (walls x0+25 .. x0+31): a brick divider across z0+6 with a two-wide OPEN oak gate in it (two gate
     *          blocks, x0+28 and x0+29). Two wide, not one, because a horse is about 1.4 blocks wide and the path
     *          finder sizes its nodes to that - it is the same two-wide gate every yard pen has in its north wall.
     *          Mare in the north half, stallion in the south, about 8 blocks apart.
     *   CORNER (walls x0+32 .. x0+41): an L of three-wide corridors - an arm down the west side (x0+33..35) and
     *          one along the south (z0+9..11) - with the inside of the L filled with stone bricks two high. Mare
     *          at the north end, stallion at the east end, about 9.4 blocks apart with the stone block between:
     *          the straight line from one to the other crosses it. Line of sight at the start is logged.
     *   STALL  (x0+42 .. x0+44): oak fences at x0+42 and x0+44 and across both ends, a one-block run of floor
     *          two blocks long. A fence is a 0.25-wide post, so the run is 1.75 wide and 2.75 long - room for one
     *          1.4-wide horse across and not for two end to end (2.8), so the pair are spawned overlapping by
     *          about a tenth of a block and cannot separate. Whether their boxes touch at the start is logged.
     * HeatAttraction's range is 16, so the GATE and CORNER pairs are drawn together through the gate and round
     * the bend by ordinary navigation; the cover itself is the handler's, which only asks for the path once the
     * stallion is inside NATURAL_REACH (3) of her. So what CORNER shows is that a pair which starts out of sight
     * and meets round a corner is covered - whether they stand in sight of each other at that moment depends on
     * where the approach stops them.
     *
     * WHAT A LOOK READS (every 40 ticks, the handler's own scan period): pregnancy; a cover (lastNaturalTry
     * moving); her state, re-heated on diestrus; and, whenever she may still be covered this heat and the
     * stallion is inside the handler's search box (her box inflated by NATURAL_REACH), the handler's own path test
     * repeated - createPath(stallion, 1) and canReach(), else the 0.1-inflated hitbox overlap. Each such look is
     * tallied as path / touching-only / NEITHER. A NEITHER look is a look on which NaturalBreedingHandler would
     * have refused her "no walkable path" (and, in a yard pen, logged "[fertility] ... not covered: no walkable
     * path to ...", once a minute) - this repeats the test rather than parsing that line.
     *
     * PASS: the mare is pregnant (judged the moment it is seen). At 30 minutes without a pregnancy: PASS if at
     * least one cover happened anyway - the wiki's own pass is "a cover", and a cover that did not take is the
     * conception roll's, not the path test's; FAIL if any NEITHER look was seen (the path test refused a pair the
     * check says it must allow); FAIL too if the stallion was never once inside her reach while she could be
     * covered (the pair could not get to each other at all - that is the "ordinary penned breeding impossible"
     * the check fears, though its cause would be the approach, which the detail says); INCONCLUSIVE if she was
     * never receptive, or the horses went missing, or they met with the path test passing and still no cover
     * (some other gate - crowding, health - refused her, which this check is not about).
     *
     * 30 minutes each, all three at once. A PASS is usually minutes: a heat is a minute and a peak cover takes
     * 80% of the time.
     */

    private static final long EAST_LOOK = 40L;
    private static final long REACH_DEADLINE = 30L * 1200;
    private static final int EAST_GONE_LOOKS = 15;

    private static final class Reach {
        final String check;
        final String label;
        final int run;
        @Nullable UUID mare;
        @Nullable UUID stud;
        long start;
        long lastTry;
        int looks;
        int receptiveLooks;
        int mayLooks;
        int inReach;
        int pathLooks;
        int touchLooks;
        int neither;
        int covers;
        int reheats;
        int missing;
        double neitherAt = -1;
        double closest = Double.MAX_VALUE;

        Reach(String check, String label, int run) {
            this.check = check;
            this.label = label;
            this.run = run;
        }

        String counts() {
            return String.format(Locale.ROOT, "%d looks: in heat %d, could be covered %d, stallion in reach %d"
                            + " (path %d, touching only %d, NEITHER %d%s); covers %d; re-heats %d; closest %.2f blocks",
                    looks, receptiveLooks, mayLooks, inReach, pathLooks, touchLooks, neither,
                    neitherAt < 0 ? "" : String.format(Locale.ROOT, ", first NEITHER at %.2f blocks", neitherAt),
                    covers, reheats, closest == Double.MAX_VALUE ? -1.0 : closest);
        }
    }

    private static final String GATE = "COVER REACH GATE - a mare in heat is covered by her stallion through an open"
            + " gate between two halves of one pen";
    private static final String CORNER = "COVER REACH CORNER - a mare in heat is covered by a stallion who starts out"
            + " of sight round the corner of an L-shaped pen";
    private static final String STALL = "COVER REACH STALL - a mare pressed against her stallion in a one-wide stall"
            + " is covered (the touching fallback)";

    private static void east(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        int z1 = z0 + 12;
        DebugYardClockwork.expect(GATE);
        DebugYardClockwork.expect(CORNER);
        DebugYardClockwork.expect(STALL);

        // ---- GATE: walls x0+25 .. x0+31, divider across z0+6 with a two-wide open gate ----
        int ga = x0 + 25;
        int gb = x0 + 31;
        plot(level, gy, ga, gb, z0, z1, "AU REACH GATE", ga + 1,
                List.of("COVER REACH", "(a) open gate", "between them:", "still covered?"));
        BlockState wall = Blocks.BRICK_WALL.defaultBlockState();
        // UNVERIFIED in this repo: an OPEN gate placed by setBlockAndUpdate. BlockStateProperties.OPEN is the
        // property ModGameTests asserts on fence gates; FenceGateBlock.FACING is penWalls' own. That vanilla's
        // path finder walks an open gate (FenceGateBlock.isPathfindable answers OPEN for LAND) is from memory of
        // vanilla, not checked against 26.1.2 - the pen's counts will say if no path goes through.
        BlockState openGate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.OPEN, true);
        int gateX = (ga + gb) / 2;
        for (int x = ga + 1; x < gb; x++) {
            boolean isGate = x == gateX || x == gateX + 1;
            level.setBlockAndUpdate(new BlockPos(x, gy + 1, z0 + 6), isGate ? openGate : wall);
        }
        Horse gMare = DebugYardClockwork.horse(level, gy, ga + 2.5, z0 + 2.5, Sex.FEMALE, "", "GATE MARE");
        Horse gStud = DebugYardClockwork.horse(level, gy, ga + 4.5, z0 + 10.5, Sex.MALE, "", "GATE STUD");

        // ---- CORNER: walls x0+32 .. x0+41; the L's inside filled two high with stone bricks ----
        int ca = x0 + 32;
        int cb = x0 + 41;
        plot(level, gy, ca, cb, z0, z1, "AU REACH CORNER", ca + 1,
                List.of("COVER REACH", "(b) L-pen, out", "of sight: still", "covered?"));
        BlockState solid = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = ca + 4; x < cb; x++) {             // x0+36 .. x0+40
            for (int z = z0 + 1; z <= z0 + 8; z++) {    // the arms are x0+33..35 and z0+9..11
                for (int y = gy + 1; y <= gy + 2; y++) {
                    level.setBlock(new BlockPos(x, y, z), solid, 3);
                }
            }
        }
        Horse cMare = DebugYardClockwork.horse(level, gy, ca + 2.5, z0 + 2.5, Sex.FEMALE, "", "CORNER MARE");
        Horse cStud = DebugYardClockwork.horse(level, gy, cb - 1.5, z0 + 10.5, Sex.MALE, "", "CORNER STUD");

        // ---- STALL: fences at x0+42 and x0+44 and across both ends; one column of floor two blocks long ----
        int sx = x0 + 43;
        int sa = z0 + 4;
        int sb = z0 + 7;
        DebugPenManager.placeSign(level, new BlockPos(sx, gy + 1, z0 - 1), Direction.NORTH,
                List.of("COVER REACH", "(c) touching in", "a 1-wide stall:", "still covered?"));
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int z = sa; z <= sb; z++) {
            for (int x = sx - 1; x <= sx + 1; x++) {
                boolean edge = x != sx || z == sa || z == sb;
                // UNVERIFIED in this repo: Blocks.OAK_FENCE placed (DebugTestYard only tests for it), with the
                // same setBlockAndUpdate penWalls uses for its brick walls, so neighbouring posts connect.
                level.setBlockAndUpdate(new BlockPos(x, gy + 1, z), edge ? fence : air);
                if (!edge) {
                    level.setBlock(new BlockPos(x, gy + 2, z), air, 3);
                }
            }
        }
        YardPens.register(gy, sx - 1, sx + 1, sa, sb, "AU REACH STALL");
        // The run is z sa+0.625 .. sb+0.375 (a fence is 6..10 px). Each horse sits clear of its end fence with a
        // margin of about 0.03, so spawnHorse's noCollision takes the spot, and the two overlap by about 0.1.
        // UNVERIFIED: the 1.3965-wide horse box (vanilla's EntityType.HORSE size, not read in this repo); the
        // start line logs whether the boxes really touch.
        Horse sMare = DebugYardClockwork.horse(level, gy, sx + 0.5, sa + 1.35, Sex.FEMALE, "", "STALL MARE");
        Horse sStud = DebugYardClockwork.horse(level, gy, sx + 0.5, sb - 0.35, Sex.MALE, "", "STALL STUD");

        reach(level, new Reach(GATE, "GATE", thisRun), gMare, gStud);
        reach(level, new Reach(CORNER, "CORNER", thisRun), cMare, cStud);
        reach(level, new Reach(STALL, "STALL", thisRun), sMare, sStud);
    }

    private static void reach(ServerLevel level, Reach c, @Nullable Horse mare, @Nullable Horse stud) {
        c.mare = idOf(mare);
        c.stud = idOf(stud);
        if (mare != null) {
            DebugYardFertility.outOfHeat(mare);     // not yet: the clock starts her, below
        }
        DebugYardHerd.after(level, 20, () -> {
            if (c.run != run) {
                return;
            }
            Horse m = find(level, c.mare);
            Horse s = find(level, c.stud);
            if (m == null || s == null) {
                inconclusive(c.check, "at build: the pen is not stocked - mare " + (m != null) + ", stallion "
                        + (s != null));
                return;
            }
            c.start = level.getGameTime();
            c.lastTry = ReproHandler.of(m).lastNaturalTry();
            DebugYardFertility.inHeat(m);
            ActionTrace.log("test yard", String.format(Locale.ROOT, "COVER REACH %s: start - %.2f blocks apart, mare"
                            + " %s the stallion, boxes %s; mare state %s", c.label, Math.sqrt(m.distanceToSqr(s)),
                    m.hasLineOfSight(s) ? "SEES" : "cannot see",
                    m.getBoundingBox().inflate(0.1).intersects(s.getBoundingBox()) ? "TOUCH" : "apart",
                    ReproHandler.stateOf(m)));
            reachLook(level, c);
        });
    }

    private static void reachLook(ServerLevel level, Reach c) {
        DebugYardHerd.after(level, EAST_LOOK, () -> {
            if (c.run != run || answered(c.check)) {
                return;
            }
            reachLook(level, c);    // first, so a throw below cannot stop the clock
            long now = level.getGameTime();
            String at = "at " + minutes(now - c.start) + ": ";
            Horse m = find(level, c.mare);
            Horse s = find(level, c.stud);
            if (m == null || s == null) {
                if (++c.missing >= EAST_GONE_LOOKS) {
                    inconclusive(c.check, at + "the " + (m == null ? "mare" : "stallion") + " is gone; " + c.counts());
                }
                return;
            }
            c.missing = 0;
            c.looks++;
            c.closest = Math.min(c.closest, Math.sqrt(m.distanceToSqr(s)));
            Reproduction r = ReproHandler.of(m);
            if (r.lastNaturalTry() != c.lastTry) {
                c.lastTry = r.lastNaturalTry();
                c.covers++;
                ActionTrace.log("test yard", "COVER REACH " + c.label + ": cover " + c.covers + " "
                        + at + (r.pregnant() ? "she conceived" : "it did not take"));
            }
            if (r.pregnant()) {
                pass(c.check, at + "the mare is pregnant; " + c.counts());
                DebugYardFertility.noNaturalCovers(m);  // answered: one foal from this pen, not one an hour
                return;
            }
            if (keepInHeat(m)) {
                c.reheats++;
            }
            if (ReproHandler.receptive(m)) {
                c.receptiveLooks++;
            }
            if (mayTry(m)) {
                c.mayLooks++;
                // The handler's search box (NaturalBreedingHandler.onTick) and then its canMeet, repeated.
                if (m.getBoundingBox().inflate(ReproRules.NATURAL_REACH).intersects(s.getBoundingBox())) {
                    c.inReach++;
                    Path path = m.getNavigation().createPath(s, 1);
                    if (path != null && path.canReach()) {
                        c.pathLooks++;
                    } else if (m.getBoundingBox().inflate(0.1).intersects(s.getBoundingBox())) {
                        c.touchLooks++;
                    } else {
                        if (c.neither == 0) {
                            c.neitherAt = Math.sqrt(m.distanceToSqr(s));
                        }
                        c.neither++;
                    }
                }
            }
            if (now - c.start >= REACH_DEADLINE) {
                reachVerdict(c, at);
            }
        });
    }

    private static void reachVerdict(Reach c, String at) {
        if (c.covers > 0) {
            pass(c.check, at + "covered " + c.covers + " time(s) though none took - the wiki's pass is a cover;"
                    + " " + c.counts());
        } else if (c.neither > 0) {
            fail(c.check, at + "never covered, and on " + c.neither + " look(s) the handler's path test found no"
                    + " walkable path and no touching hitboxes (the \"no walkable path\" refusal); " + c.counts());
        } else if (c.receptiveLooks == 0) {
            inconclusive(c.check, at + "she was never receptive; " + c.counts());
        } else if (c.inReach == 0) {
            fail(c.check, at + "never covered: the stallion was never once inside her reach while she could be"
                    + " covered, so the pair could not get to each other at all - the cover's path test was"
                    + " never reached, look at the approach (HerdGoals.HeatAttraction) and the layout; " + c.counts());
        } else {
            inconclusive(c.check, at + "never covered although the path test passed whenever he was in reach -"
                    + " another of the handler's gates (crowding, health, the courtship) refused her, which is"
                    + " not this check's question; " + c.counts());
        }
    }
}
