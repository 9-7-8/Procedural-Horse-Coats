package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.AbstractWeatherGene;
import com.example.horsegenetics.common.genetics.genes.WeatherJumpGene;
import com.example.horsegenetics.common.genetics.genes.WeatherSpeedGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * <b>Row AW west: WEATHER COPIES - does a weather gene's second copy count?</b> (2026-10-01, built with the fix for
 * check 304, GitHub issue on the same day).
 *
 * <p>The question, from {@code wiki/gene-weather-speed.html} and {@code gene-weather-jump.html}: <i>"Two copies add.
 * This is the page's central claim and the thing most likely to be wrong ... Spawn an R+/R+ horse and an R+/n horse
 * in one watched pen and read the range in rain. Pass: the homozygote's value is about twice as far off the dry
 * baseline as the heterozygote's."</i> And: <i>"A storm is not just rain again ... Spawn T+/T+ and R+/Tv horses and
 * read the pen dry, under /weather rain, and under /weather thunder"</i>, and <i>"The modifier comes back off.
 * /weather clear after each reading. Pass: the pen returns to exactly its dry baseline."</i>
 *
 * <p>Until 2026-10-01 every gene modifier was named {@code gene/<key>/<attribute>}, one id per gene, so the second
 * copy's {@code addOrUpdateTransientModifier} replaced the first. {@code GeneAbilityHandler.attributeModifierId} now
 * adds the copy number. This pen is the proof, and it does not trust "about twice": each copy's percentage is rolled
 * on that copy's epigenome, so the pen reads both rolled deltas off the horse's own record and predicts the exact
 * factor - {@code multiply_total} stacks multiplicatively, so a reading is {@code dry x (1 + d1) x (1 + d2)} for two
 * copies whose weather holds. A horse carrying one copy's worth reads {@code dry x (1 + d)} for one of them, which a
 * relative tolerance of 1e-4 tells apart from the product for any delta over 0.02 ({@code MIN_DELTA}).
 *
 * <p><b>The storm, as the code stands.</b> Vanilla's {@code isRaining} is true in a thunderstorm, so an
 * {@code R+/Tv} horse in a storm has both conditions holding. The pages say "two kinds of weather do not both hold";
 * that is false of the game, and with the per-copy id both halves now apply: the check asserts the product of the
 * rain copy and the storm copy, and the detail line names the page claim it contradicts, for the owner.
 *
 * <p><b>The weather is the whole server's.</b> {@code MinecraftServer.setWeatherParameters} is what {@code /weather}
 * calls (read off the 26.1.2 WeatherCommand); the debug dimension takes its rain from the same data, being
 * skylit and roofless ({@code Level.canHaveWeather}). So the sequence waits {@value #START} ticks for the rest of the
 * yard's short checks - the stasis bank's fire test above all, which rain would put out - and ends by setting a long
 * clear spell, so no later reading anywhere in the yard happens in rain this pen started. Every reading first checks
 * the level really is in the weather it asked for and answers INCONCLUSIVE when it is not: that is the world, not the
 * gene. About 12 minutes from build to the last verdict.
 */
final class DebugYardWeather {

    private DebugYardWeather() {
    }

    /** Five minutes before the sky is touched: every short pen in the yard has answered by then. */
    private static final long START = 6_000L;
    /** Rain level climbs 0.01 a tick and counts from 0.2; thunder from 0.9 - so a hundred ticks, and margin. */
    private static final long SETTLE = 300L;
    /** A clear spell long enough to see the night out. */
    private static final int CLEAR_AFTER = 24_000 * 10;
    private static final double TOLERANCE = 1e-4;

    private static final String PEN = "WEATHER COPIES";
    private static final String CHECK_TWO = PEN + " - two R+ copies both move speed and jump in rain (check 304)";
    private static final String CHECK_ONE = PEN + " - one R+ copy moves its stat by its own rolled percentage";
    private static final String CHECK_STORM = PEN + " - a storm applies T+ copies, and both halves of R+/Tv";
    private static final String CHECK_OFF = PEN + " - every weather modifier comes off in clear weather";

    private record Subject(UUID id, String label, String geneKey, boolean jump) {
    }

    /** The readings, by phase, in subject order. */
    private static final class Run {
        final List<Subject> subjects = new ArrayList<>();
        double[] dry;
        double[] rain;
        double[] storm;
    }

    static void build(ServerLevel level, int gy, int x0, int z0) {
        try {
            Run run = new Run();
            // Four glass cells a row, two rows: a 2x2 floor each, so nobody wanders or bolts.
            stock(level, gy, x0, z0 + 1, run, WeatherSpeedGene.KEY, false, "R+/R+", "R+/n", "R+/Tv", "T+/T+");
            stock(level, gy, x0, z0 + 7, run, WeatherSpeedGene.KEY, false, "n/n");
            stock(level, gy, x0 + 5, z0 + 7, run, WeatherJumpGene.KEY, true, "R+/R+", "R+/n", "n/n");
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of("WEATHER COPIES", "dry, rain, storm,", "clear - two copies", "must both count"));
            for (String c : List.of(CHECK_TWO, CHECK_ONE, CHECK_STORM, CHECK_OFF)) {
                DebugYardClockwork.expect(c);
            }
            sequence(level, run);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: WEATHER COPIES failed to build", e);
        }
    }

    private static void stock(ServerLevel level, int gy, int x0, int z0, Run run, String key, boolean jump,
                              String... pairs) {
        int x = x0;
        for (String pair : pairs) {
            DebugYardClockwork.cell(level, gy, x, x + 3, z0, z0 + 3);
            String label = PEN + " " + (jump ? "JUMP " : "SPEED ") + pair;
            Horse h = DebugYardClockwork.horse(level, gy, x + 2.0, z0 + 2.0, Sex.MALE, key + "=" + pair, label);
            if (h != null) {
                run.subjects.add(new Subject(h.getUUID(), label, key, jump));
            }
            x += 5;
        }
    }

    private static void sequence(ServerLevel level, Run run) {
        DebugYardHerd.after(level, START, () -> {
            weather(level, false, false);
            DebugYardHerd.after(level, SETTLE, () -> {
                if (level.isRaining()) {
                    allInconclusive("the sky would not clear for the dry reading");
                    return;
                }
                run.dry = read(level, run);
                weather(level, true, false);
                DebugYardHerd.after(level, SETTLE, () -> {
                    if (!level.isRaining() || level.isThundering()) {
                        allInconclusive("asked for rain and the level reads raining " + level.isRaining()
                                + ", thundering " + level.isThundering() + " - this dimension may take no weather");
                        weather(level, false, false);
                        return;
                    }
                    run.rain = read(level, run);
                    weather(level, true, true);
                    DebugYardHerd.after(level, SETTLE, () -> {
                        boolean storm = level.isThundering() && level.isRaining();
                        if (storm) {
                            run.storm = read(level, run);
                        }
                        weather(level, false, false);
                        DebugYardHerd.after(level, SETTLE, () -> judge(level, run, storm));
                    });
                });
            });
        });
    }

    /** What {@code /weather clear|rain|thunder} does, with a clear spell long enough to see the night out. */
    private static void weather(ServerLevel level, boolean rain, boolean thunder) {
        if (rain) {
            level.getServer().setWeatherParameters(0, 6_000, true, thunder);
        } else {
            level.getServer().setWeatherParameters(CLEAR_AFTER, 0, false, false);
        }
    }

    private static double[] read(ServerLevel level, Run run) {
        double[] out = new double[run.subjects.size()];
        for (int i = 0; i < out.length; i++) {
            Horse h = horse(level, run.subjects.get(i));
            out[i] = h == null ? Double.NaN
                    : h.getAttributeValue(run.subjects.get(i).jump() ? Attributes.JUMP_STRENGTH : Attributes.MOVEMENT_SPEED);
        }
        return out;
    }

    private static @Nullable Horse horse(ServerLevel level, Subject s) {
        return level.getEntity(s.id()) instanceof Horse h && h.isAlive() ? h : null;
    }

    /**
     * The factor this horse's copies predict under the given weather: the product of {@code 1 + delta} for every
     * thriving copy whose weather holds and {@code 1 - delta} for every suffering one. Read off the copies
     * directly, not through the ability list - the prediction must not share the code it checks.
     */
    private static double predicted(ServerLevel level, Subject s, boolean raining, boolean thundering,
                                     StringBuilder why) {
        Horse h = horse(level, s);
        Gene gene = Genes.byKeyOrNull(s.geneKey());
        if (h == null || gene == null || !HorseRecords.hasRealRecord(h)) {
            return Double.NaN;
        }
        var record = HorseRecords.of(h);
        AllelePair pair = record.genotype().pair(s.geneKey());
        if (pair == null) {
            return Double.NaN;
        }
        Epigenome.Copies copies = record.epigenome().copies(gene);
        double f = 1.0;
        f *= copyFactor(gene, pair.first(), Epigenome.readable(gene, copies.first()).get(AbstractWeatherGene.DELTA),
                raining, thundering, why);
        f *= copyFactor(gene, pair.second(), Epigenome.readable(gene, copies.second()).get(AbstractWeatherGene.DELTA),
                raining, thundering, why);
        return f;
    }

    private static double copyFactor(Gene gene, Allele allele, double delta, boolean raining, boolean thundering,
                                     StringBuilder why) {
        String t = allele.token();
        if (t.equals("n")) {
            return 1.0;
        }
        why.append(why.length() == 0 ? "" : " ").append(t).append(String.format(Locale.ROOT, " %.4f", delta));
        boolean holds = (t.startsWith("R") && raining) || (t.startsWith("T") && thundering);
        if (!holds) {
            return 1.0;
        }
        return t.endsWith("+") ? 1.0 + delta : 1.0 - delta;
    }

    private static void judge(ServerLevel level, Run run, boolean stormHappened) {
        if (level.isRaining()) {
            DebugYardClockwork.inconclusive(CHECK_OFF, "the sky would not clear again");
        }
        double[] after = read(level, run);
        StringBuilder twoDetail = new StringBuilder();
        StringBuilder oneDetail = new StringBuilder();
        StringBuilder stormDetail = new StringBuilder();
        StringBuilder offDetail = new StringBuilder();
        boolean twoOk = true;
        boolean oneOk = true;
        boolean stormOk = true;
        boolean offOk = true;
        int twos = 0;
        int ones = 0;
        for (int i = 0; i < run.subjects.size(); i++) {
            Subject s = run.subjects.get(i);
            double dry = run.dry[i];
            if (Double.isNaN(dry) || Double.isNaN(run.rain[i]) || Double.isNaN(after[i])) {
                allInconclusive(s.label() + " is gone or unreadable mid-run");
                return;
            }
            StringBuilder why = new StringBuilder();
            double wantRain = predicted(level, s, true, false, why);
            double gotRain = run.rain[i] / dry;
            String pair = s.label().substring(s.label().lastIndexOf(' ') + 1);
            boolean rainOk = close(gotRain, wantRain);
            String line = String.format(Locale.ROOT, "%s rain x%.5f (copies %s predict x%.5f)%s",
                    s.label().substring(PEN.length() + 1), gotRain, why.length() == 0 ? "none" : why, wantRain,
                    rainOk ? "" : " WRONG");
            if (pair.equals("R+/R+")) {
                twos++;
                twoOk &= rainOk;
                twoDetail.append(twoDetail.length() == 0 ? "" : "; ").append(line);
            } else if (pair.equals("R+/n") || pair.equals("n/n")) {
                ones++;
                oneOk &= rainOk;
                oneDetail.append(oneDetail.length() == 0 ? "" : "; ").append(line);
            }
            if (pair.equals("R+/Tv") || pair.equals("T+/T+")) {
                stormOk &= rainOk;  // in plain rain T+/T+ must sit at baseline, R+/Tv carry only its R+
                stormDetail.append(stormDetail.length() == 0 ? "" : "; ").append(line);
                if (run.storm != null) {
                    StringBuilder w2 = new StringBuilder();
                    double wantStorm = predicted(level, s, true, true, w2);
                    double gotStorm = run.storm[i] / dry;
                    boolean ok = close(gotStorm, wantStorm);
                    stormOk &= ok;
                    stormDetail.append(String.format(Locale.ROOT, ", storm x%.5f (predict x%.5f)%s", gotStorm,
                            wantStorm, ok ? "" : " WRONG"));
                }
            }
            boolean back = Math.abs(after[i] - dry) <= 1e-9;
            offOk &= back;
            offDetail.append(offDetail.length() == 0 ? "" : "; ").append(String.format(Locale.ROOT, "%s %.6f -> %.6f%s",
                    s.label().substring(PEN.length() + 1), dry, after[i], back ? "" : " STUCK"));
        }
        if (twos < 2) {
            DebugYardClockwork.inconclusive(CHECK_TWO, "only " + twos + " R+/R+ horse(s) stocked; " + twoDetail);
        } else {
            DebugYardClockwork.verdict(CHECK_TWO, twoOk, twoDetail.toString());
        }
        if (ones < 3) {
            DebugYardClockwork.inconclusive(CHECK_ONE, "only " + ones + " control horse(s) stocked; " + oneDetail);
        } else {
            DebugYardClockwork.verdict(CHECK_ONE, oneOk, oneDetail.toString());
        }
        if (!stormHappened) {
            DebugYardClockwork.inconclusive(CHECK_STORM, "asked for thunder and the level never read thundering; "
                    + stormDetail);
        } else {
            DebugYardClockwork.verdict(CHECK_STORM, stormOk, stormDetail
                    + " | NOTE for the pages: in a storm isRaining is also true, so R+/Tv carries BOTH copies -"
                    + " 'two kinds of weather do not both hold' is not what the game does");
        }
        if (!level.isRaining()) {
            DebugYardClockwork.verdict(CHECK_OFF, offOk, offDetail.toString());
        }
    }

    private static boolean close(double got, double want) {
        return !Double.isNaN(want) && Math.abs(got - want) <= TOLERANCE * Math.max(1.0, Math.abs(want));
    }

    private static void allInconclusive(String why) {
        for (String c : List.of(CHECK_TWO, CHECK_ONE, CHECK_STORM, CHECK_OFF)) {
            DebugYardClockwork.inconclusive(c, why);
        }
    }
}
