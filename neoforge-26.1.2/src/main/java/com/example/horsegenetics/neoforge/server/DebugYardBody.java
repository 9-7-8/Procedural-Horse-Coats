package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Escape;
import com.example.horsegenetics.common.care.LastStand;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicStatGene;
import com.example.horsegenetics.common.genetics.genes.MagicFighterGene;
import com.example.horsegenetics.common.genetics.genes.Ryr1Gene;
import com.example.horsegenetics.common.genetics.genes.SilverGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <b>Row AR: the body's numbers and the horse that will not die</b> (2026-10-01). Two clockwork pens, each a
 * handful of {@link DebugYardClockwork#cell glass cells}, each check one {@code CLOCKWORK} line in the log.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>answers</th></tr>
 *   <tr><td>west, {@code x0 .. x0+18}</td><td>STAT DOSES</td><td>once, at 1200 ticks (one minute)</td></tr>
 *   <tr><td>east, {@code x0+26 .. x0+44}</td><td>LAST STAND</td><td>within 100 + immunity + 80 ticks (about
 *       fifteen seconds at the default 120-tick window)</td></tr>
 * </table>
 *
 * <h2>STAT DOSES - a number a gene writes, against a control that differs at one locus</h2>
 * Every horse here is the same black ({@code E/E a/a}) with every other locus at its default, so a reading minus
 * the control's reading is exactly what the one listed locus did. Two controls are stocked and must agree - if they
 * do not, something besides the genotype is moving the attribute and every check here is INCONCLUSIVE rather than
 * guessed at. Untamed, one to a 4x4 glass cell (a 2x2 floor), so nothing walks, breeds or fights. Each horse's
 * {@code max_health}, {@code movement_speed}, {@code jump_strength} and {@code attack_damage} <b>base values</b>
 * (the number {@code /attribute ... base get} prints, which is what the pages ask for) are read at 40 ticks and again
 * at 1200; the verdict is on the 1200 reading, and a horse whose number moved in between says so in its line - a
 * stat applied late is a finding even when the late number is right.
 *
 * <p>Every expected number is <b>worked out from the code, not from the page</b>, and where the two disagree the
 * line says so rather than failing on a page typo. Where a number rides on a rolled epigenetic copy (Frail, Leaden,
 * Gld/Wmp) the pen reads that copy off the horse's own record - directly, not through {@code GeneEpigenetics}, so the
 * prediction does not share the reading it is checking (the DRYAD OAK+BIRCH rule in {@link DebugYardLong}).
 *
 * <ul>
 *   <li><b>wiki/gene-silver.html</b>, Verification, "Silver's dose split": <i>"a Z/z horse names ocular cysts and
 *       loses nothing; a Z/Z horse names the ocular anomalies and is down two hearts. A Z/z horse short a heart means
 *       the dose split is inverted or collapsed."</i> (Also wiki/breed-book.html, Rocky Mountain Horse.)
 *       <b>Page and code disagree on the size</b>: {@code SilverGene.contribute} does
 *       {@code addHealth(-MCOA_HEALTH_PENALTY)} with the penalty 2.0, and {@code TraitBuilder.addHealth} is in health
 *       points, two to a heart - so the code takes <i>one</i> heart. Judged on the code: PASS is Z/Z at control
 *       minus {@code MCOA_HEALTH_PENALTY} with {@code SilverGene.MCOA} among its resolved conditions, and Z/z exactly
 *       at the control with {@code MCOA_CYST} and not {@code MCOA}. Needs {@code health.mode} to affect the body
 *       (silver is a {@code HealthContribution}); otherwise INCONCLUSIVE.</li>
 *   <li><b>wiki/gene-ryr1.html</b>, Verification: <i>"an MH/N horse names malignant hyperthermia yet must keep full
 *       hearts, speed and jump ... while MH/MH costs six health"</i>. PASS: MH/N equal to the control on all three
 *       base values with {@code Ryr1Gene.MH} named; MH/MH at control minus 6.0 ({@code affectHomozygote}'s literal;
 *       its speed and jump losses are reported, not judged).</li>
 *   <li><b>wiki/gene-magic-health.html</b>, Verification: <i>"Vmp/n should be exactly twice the control and Vmp/Vmp
 *       exactly three times it. Pass: both numbers land on the multiple, and two Vmp/Vmp horses read identically"</i>
 *       ({@code vampiricPerCopy() == 1.0}); and <i>"A Frail/Frail horse. Pass: clearly below the plain control -
 *       about four fifths of it - and never below half a heart"</i>. Frail's PASS is the attribute equal to
 *       {@code max(MIN_HEALTH, control x (1 - d0 - d1))} off its own two copies' {@code delta}, and below the
 *       control; the ratio is printed against the page's "about four fifths" and not judged on it.</li>
 *   <li><b>wiki/gene-magic-jump.html</b>, Verification: <i>"Vmp/n should be exactly half again the control and
 *       Vmp/Vmp exactly twice it ... two Vmp/Vmp horses read identically"</i> ({@code vampiricPerCopy() == 0.5}), and
 *       <i>"A Leaden/Leaden horse. Pass: clearly below the plain control - about four fifths of it"</i>, judged as
 *       Frail is, floored at {@code MIN_JUMP}.</li>
 *   <li><b>wiki/gene-magic-fighter.html</b>, Verification: <i>"A plain horse reads exactly 3. Pass: base value
 *       3.0"</i> - read off both controls; and <i>"A Gld/Wmp horse sits near the baseline. Pass: base value within
 *       about a point of 3.0"</i>. The code is {@code max(MIN_DAMAGE, 3 x (1 + gladiator - wimp))} from the two
 *       copies, and with gladiator in [1/3, 1] and wimp in [0.15, 0.6] that is anywhere in 2.2 .. 5.55 - so "within
 *       a point" is not something the code promises. PASS is the base value on the code's number off this horse's
 *       own copies; whether it also fell within a point of 3 is printed beside it. The ability is written by
 *       {@code GeneAbilityHandler} on the horse's tick, which is exactly the kind of late write the 40-tick reading is
 *       there to catch.</li>
 * </ul>
 *
 * <h2>LAST STAND - wiki/horse-care.html, "No horse dies to one blow" and "A dying horse bolts"</h2>
 * Four tamed, ownerless horses (so neither the emergency stasis bank nor the afterlife takes one - both need an
 * owner), each in its own lidded glass cell, because a saved horse bolts with {@code Escape.JUMP_BOOST} and clears
 * anything without a roof ({@link DebugTestYard#lidded}'s javadoc, KICK HUNTER 2026-09-30). One zombie in a 3x3 cell
 * is the attacker every blow is credited to ({@code damageSources().mobAttack(zombie)}), so the horse has the live
 * {@code lastHurtByMob} that {@code HorseEscapeGoal.inDanger} reads. Every blow is a direct {@code hurtServer} on the
 * clock; the timings are the handler's own: the window is {@code LastStand.immune} -
 * {@code now - spentAt < last_stand_immunity_ticks}, refused at {@code LivingIncomingDamageEvent} - and the stamp is
 * {@code HorseCooldownsAttachment} key {@code "last_stand"}, cleared by {@code onIncoming} only when the horse's
 * health is at {@code last_stand_rearm_fraction} of its maximum. {@code I} below is the configured window.
 * <ul>
 *   <li><i>"Stand a full-health horse next to a charged creeper, or hit one with a sharpness-V sword it could not
 *       survive. Pass: ... the horse alive on half a heart."</i> Horse A, full health, 1000 from the zombie at
 *       tick 100. PASS: alive, health exactly {@code LastStand.healthLeft(last_stand_health, max)}, stamp written on
 *       this tick.</li>
 *   <li><i>"The window is real immunity, not a hurt cooldown."</i> A again at +30 - past vanilla's ten-tick hurt
 *       cooldown, inside the window - for 5, which on one health point would kill. PASS: alive, health unmoved.</li>
 *   <li><i>"It is spent. Save a horse, wait out the window, hit it again hard without healing it. Pass: it
 *       dies."</i> A at +I+40 for 1000. PASS: dead. INCONCLUSIVE if it had somehow healed to the re-arm line.</li>
 *   <li><i>"It comes back. Heal the same horse to full ... then hit it hard. Pass: saved again."</i> Horse B: saved
 *       at 100, set to full health at +I+40 (the re-arm condition is literally the health, so setting it is the
 *       honest state), hit for 1000 at +I+80. PASS: alive on the last-stand health with the stamp re-written to
 *       the second blow's tick.</li>
 *   <li><i>"The void still kills. ... Fail: an unkillable horse, which is the bypasses_invulnerability check."</i>
 *       Horse C, full and armed, {@code fellOutOfWorld} for 1000. PASS: dead.</li>
 *   <li><i>"The save and the bolt are one behaviour. Pass: a saved horse is already running when the particles
 *       clear, and is still running when the immunity ends."</i> Horse D, saved at 100. PASS: at +10
 *       {@code HorseEscapeGoal.bolting} with both {@code horsegenetics:escape/speed} and
 *       {@code horsegenetics:escape/jump} on, and still bolting at +I-2.</li>
 *   <li><i>"The boost is visible ... drop back when it calms. Fail: ... they stay high after it calms - which means
 *       stop never ran, and the horse is permanently buffed."</i> D set to full at +I+20, which is past
 *       {@code Escape.keepsBolting}; read at +I+60. PASS: not bolting and both modifiers gone.</li>
 * </ul>
 * Everything is INCONCLUSIVE if {@code behaviour.last_stand} is off; the two bolt checks also if
 * {@code behaviour.escape_health_fraction} is 0 or the zombie is gone.
 *
 * <p>UNVERIFIED: {@code DamageSources.fellOutOfWorld()} - nothing else in this repo calls it; it is the 1.19.4+
 * Mojang name and is assumed unchanged in 26.1.2.
 */
final class DebugYardBody {

    private DebugYardBody() {
    }

    private static final double EPS = 1.0e-6;
    /** Every check answers once, even if a step throws after answering some of them. */
    private static final Set<String> ANSWERED = new HashSet<>();
    /** Bumped by every build, so a step left on the clock from a yard before this one stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int thisRun = ++run;
        ANSWERED.clear();
        try {
            statDoses(level, gy, x0, z0, thisRun);
            lastStand(level, gy, x0, z0, thisRun);
            ActionTrace.log("test yard", "row AR built (STAT DOSES west: answers at 1200 ticks;"
                    + " LAST STAND east: answers within about " + (100 + immunity() + 80) + " ticks)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AR (DebugYardBody) failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Verdict plumbing
    // ------------------------------------------------------------------

    private static void pass(String check, boolean ok, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, ok, detail);
        }
    }

    private static void unsure(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.inconclusive(check, detail);
        }
    }

    /** A timed step that cannot leave its checks silent: a throw answers each still-open one INCONCLUSIVE. */
    private static void step(ServerLevel level, long ticks, int thisRun, List<String> checks, Runnable body) {
        DebugYardHerd.after(level, ticks, () -> {
            if (thisRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a row AR step threw", e);
                for (String c : checks) {
                    unsure(c, "the step threw " + e);
                }
            }
        });
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    // ------------------------------------------------------------------
    // WEST - STAT DOSES
    // ------------------------------------------------------------------

    private static final String BLACK = "horsegenetics.extension=E/E-horsegenetics.agouti=a/a";

    private static final String SILVER_HET = "STAT DOSES - silver Z/z names ocular cysts and loses nothing";
    private static final String SILVER_HOM = "STAT DOSES - silver Z/Z names the ocular anomalies and is down"
            + " MCOA_HEALTH_PENALTY";
    private static final String RYR_HET = "STAT DOSES - RYR1 MH/N names malignant hyperthermia and keeps full"
            + " health, speed and jump";
    private static final String RYR_HOM = "STAT DOSES - RYR1 MH/MH loses 6 health";
    private static final String HEALTH_VMP = "STAT DOSES - magic health Vmp/n is exactly twice the control and"
            + " Vmp/Vmp exactly three times, two Vmp/Vmp identical";
    private static final String HEALTH_FRAIL = "STAT DOSES - magic health Frail/Frail is below the control by its"
            + " own copies (about four fifths), never below MIN_HEALTH";
    private static final String JUMP_VMP = "STAT DOSES - magic jump Vmp/n is exactly half again the control and"
            + " Vmp/Vmp exactly twice, two Vmp/Vmp identical";
    private static final String JUMP_LEADEN = "STAT DOSES - magic jump Leaden/Leaden is below the control by its"
            + " own copies (about four fifths)";
    private static final String FIGHTER_PLAIN = "STAT DOSES - magic fighter: a plain horse's attack_damage base"
            + " is exactly 3.0";
    private static final String FIGHTER_MIXED = "STAT DOSES - magic fighter: Gld/Wmp sits where its own two copies"
            + " put it (page: near the baseline)";

    private static final List<String> DOSE_CHECKS = List.of(SILVER_HET, SILVER_HOM, RYR_HET, RYR_HOM, HEALTH_VMP,
            HEALTH_FRAIL, JUMP_VMP, JUMP_LEADEN, FIGHTER_PLAIN, FIGHTER_MIXED);

    /** The base values the pages ask about, at one moment. */
    private record Reading(double health, double speed, double jump, double attack) {
        boolean sameAs(Reading o) {
            return Math.abs(health - o.health) < EPS && Math.abs(speed - o.speed) < EPS
                    && Math.abs(jump - o.jump) < EPS && Math.abs(attack - o.attack) < EPS;
        }

        @Override
        public String toString() {
            return "health " + f(health) + ", speed " + f(speed) + ", jump " + f(jump) + ", attack " + f(attack);
        }
    }

    private static final class Body {
        final String label;
        final @Nullable Horse horse;
        @Nullable Reading early;
        @Nullable Reading late;

        Body(String label, @Nullable Horse horse) {
            this.label = label;
            this.horse = horse;
        }

        /** Empty when the two readings agree; otherwise the 40-tick one, for the line. */
        String moved() {
            return early != null && late != null && !early.sameAs(late)
                    ? " [CHANGED LATE: at 40 ticks " + early + "]" : "";
        }
    }

    private static void statDoses(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        for (String c : DOSE_CHECKS) {
            DebugYardClockwork.expect(c);
        }
        String[][] stock = {
                {"control 1", BLACK},
                {"control 2", BLACK},
                {"silver Z/z", BLACK + "-horsegenetics.silver=Z/z"},
                {"silver Z/Z", BLACK + "-horsegenetics.silver=Z/Z"},
                {"RYR1 MH/N", BLACK + "-horsegenetics.ryr1=MH/N"},
                {"RYR1 MH/MH", BLACK + "-horsegenetics.ryr1=MH/MH"},
                {"health Vmp/n", BLACK + "-horsegenetics.magic_health=Vmp/n"},
                {"health Vmp/Vmp a", BLACK + "-horsegenetics.magic_health=Vmp/Vmp"},
                {"health Vmp/Vmp b", BLACK + "-horsegenetics.magic_health=Vmp/Vmp"},
                {"health Frail/Frail", BLACK + "-horsegenetics.magic_health=Frail/Frail"},
                {"jump Vmp/n", BLACK + "-horsegenetics.magic_jump=Vmp/n"},
                {"jump Vmp/Vmp a", BLACK + "-horsegenetics.magic_jump=Vmp/Vmp"},
                {"jump Vmp/Vmp b", BLACK + "-horsegenetics.magic_jump=Vmp/Vmp"},
                {"jump Leaden/Leaden", BLACK + "-horsegenetics.magic_jump=Leaden/Leaden"},
                {"fighter Gld/Wmp", BLACK + "-horsegenetics.magic_fighter=Gld/Wmp"},
        };
        List<Body> bodies = new ArrayList<>();
        for (int i = 0; i < stock.length; i++) {
            // Six 4x4 cells to a row sharing their side walls (x0 .. x0+18), three rows (z0+1 .. z0+12).
            int cx = x0 + 3 * (i % 6);
            int cz = z0 + 1 + 4 * (i / 6);
            DebugYardClockwork.cell(level, gy, cx, cx + 3, cz, cz + 3);
            Horse h = DebugYardUnattended.horse(level, gy, cx + 2.0, cz + 2.0, Sex.FEMALE, stock[i][1], false,
                    "DOSES " + stock[i][0]);
            bodies.add(new Body(stock[i][0], h));
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("STAT DOSES", "one locus each vs", "a plain black;", "read at 1 minute"));

        step(level, 40, thisRun, DOSE_CHECKS, () -> {
            for (Body b : bodies) {
                b.early = read(b.horse);
            }
        });
        step(level, 1200, thisRun, DOSE_CHECKS, () -> {
            for (Body b : bodies) {
                b.late = read(b.horse);
            }
            judgeDoses(bodies);
        });
    }

    private static @Nullable Reading read(@Nullable Horse h) {
        if (h == null || !h.isAlive()) {
            return null;
        }
        return new Reading(base(h, Attributes.MAX_HEALTH), base(h, Attributes.MOVEMENT_SPEED),
                base(h, Attributes.JUMP_STRENGTH), base(h, Attributes.ATTACK_DAMAGE));
    }

    private static double base(Horse h, Holder<Attribute> attribute) {
        AttributeInstance i = h.getAttribute(attribute);
        return i == null ? Double.NaN : i.getBaseValue();
    }

    private static void judgeDoses(List<Body> b) {
        Body c1 = b.get(0);
        Body c2 = b.get(1);
        if (c1.late == null || c2.late == null) {
            for (String c : DOSE_CHECKS) {
                unsure(c, "a control horse is missing or dead at 1200 ticks (control 1 " + c1.late
                        + "; control 2 " + c2.late + ")");
            }
            return;
        }
        if (!c1.late.sameAs(c2.late)) {
            for (String c : DOSE_CHECKS) {
                unsure(c, "the two identical controls disagree, so something besides the genotype moves these"
                        + " attributes: control 1 " + c1.late + c1.moved() + "; control 2 " + c2.late + c2.moved());
            }
            return;
        }
        Reading ctl = c1.late;
        String ctlLine = "control " + ctl + c1.moved();
        boolean healthOn = ServerConfig.healthGeneticsActive();

        // --- silver ---
        if (!healthOn) {
            String why = "health.mode is " + ServerConfig.healthMode() + ", which keeps HealthContribution loci"
                    + " off the body, so a silver or RYR1 horse cannot differ from the control here";
            unsure(SILVER_HET, why);
            unsure(SILVER_HOM, why);
            unsure(RYR_HET, why);
            unsure(RYR_HOM, why);
        } else {
            Body het = b.get(2);
            Body hom = b.get(3);
            if (het.late == null || hom.late == null) {
                unsure(SILVER_HET, "a silver horse is missing or dead (Z/z " + het.late + ", Z/Z " + hom.late + ")");
                unsure(SILVER_HOM, "a silver horse is missing or dead (Z/z " + het.late + ", Z/Z " + hom.late + ")");
            } else {
                List<Condition> hc = HorseRecords.traitsOf(het.horse).conditions();
                boolean ok = Math.abs(het.late.health() - ctl.health()) < EPS
                        && hc.contains(SilverGene.MCOA_CYST) && !hc.contains(SilverGene.MCOA);
                pass(SILVER_HET, ok, "Z/z max_health base " + f(het.late.health()) + " vs control "
                        + f(ctl.health()) + " (diff " + f(het.late.health() - ctl.health()) + ", code expects 0);"
                        + " names " + names(hc) + " (expects MCOA_CYST, not MCOA)" + het.moved() + "; " + ctlLine);

                List<Condition> oc = HorseRecords.traitsOf(hom.horse).conditions();
                double want = Math.max(HorseTraits.MIN_HEALTH, ctl.health() - SilverGene.MCOA_HEALTH_PENALTY);
                ok = Math.abs(hom.late.health() - want) < EPS && oc.contains(SilverGene.MCOA);
                pass(SILVER_HOM, ok, "Z/Z max_health base " + f(hom.late.health()) + ", code expects control "
                        + f(ctl.health()) + " - MCOA_HEALTH_PENALTY " + SilverGene.MCOA_HEALTH_PENALTY + " = "
                        + f(want) + " (diff read " + f(hom.late.health() - ctl.health()) + "); names " + names(oc)
                        + " (expects MCOA). PAGE/CODE DISAGREE: the page says two hearts (4.0 points), the code"
                        + " takes 2.0 points = one heart; judged on the code" + hom.moved() + "; " + ctlLine);
            }

            // --- RYR1 ---
            Body mh = b.get(4);
            Body mm = b.get(5);
            if (mh.late == null) {
                unsure(RYR_HET, "the MH/N horse is missing or dead");
            } else {
                List<Condition> mc = HorseRecords.traitsOf(mh.horse).conditions();
                boolean ok = Math.abs(mh.late.health() - ctl.health()) < EPS
                        && Math.abs(mh.late.speed() - ctl.speed()) < EPS
                        && Math.abs(mh.late.jump() - ctl.jump()) < EPS
                        && mc.contains(Ryr1Gene.MH);
                pass(RYR_HET, ok, "MH/N " + mh.late + " vs " + ctlLine + "; names " + names(mc)
                        + " (expects Ryr1Gene.MH)" + mh.moved());
            }
            if (mm.late == null) {
                unsure(RYR_HOM, "the MH/MH horse is missing or dead");
            } else {
                // Ryr1Gene.affectHomozygote: addHealth(-6.0).addSpeed(-0.03).addJump(-0.10) - a literal, no constant.
                double want = Math.max(HorseTraits.MIN_HEALTH, ctl.health() - 6.0);
                boolean ok = Math.abs(mm.late.health() - want) < EPS;
                pass(RYR_HOM, ok, "MH/MH max_health base " + f(mm.late.health()) + ", expects control "
                        + f(ctl.health()) + " - 6.0 = " + f(want) + "; speed diff "
                        + f(mm.late.speed() - ctl.speed()) + " (code -0.03 before the speed floor), jump diff "
                        + f(mm.late.jump() - ctl.jump()) + " (code -0.10); names "
                        + names(HorseRecords.traitsOf(mm.horse).conditions()) + mm.moved() + "; " + ctlLine);
            }
        }

        // --- magic health ---
        vampiric(HEALTH_VMP, b.get(6), b.get(7), b.get(8), ctl.health(), true,
                Genes.MAGIC_HEALTH.vampiricPerCopy(), ctlLine);
        mirror(HEALTH_FRAIL, b.get(9), Genes.MAGIC_HEALTH, "Frail/Frail", ctl.health(), true,
                HorseTraits.MIN_HEALTH, ctlLine);

        // --- magic jump ---
        vampiric(JUMP_VMP, b.get(10), b.get(11), b.get(12), ctl.jump(), false,
                Genes.MAGIC_JUMP.vampiricPerCopy(), ctlLine);
        mirror(JUMP_LEADEN, b.get(13), Genes.MAGIC_JUMP, "Leaden/Leaden", ctl.jump(), false,
                HorseTraits.MIN_JUMP, ctlLine);

        // --- magic fighter ---
        boolean plainOk = Math.abs(ctl.attack() - MagicFighterGene.BASELINE_DAMAGE) < EPS;
        pass(FIGHTER_PLAIN, plainOk, "both controls (n/n) attack_damage base " + f(c1.late.attack()) + " / "
                + f(c2.late.attack()) + ", expects BASELINE_DAMAGE " + MagicFighterGene.BASELINE_DAMAGE
                + c1.moved() + c2.moved());
        fighter(b.get(14), ctl);
    }

    private static String names(List<Condition> conditions) {
        List<String> ids = new ArrayList<>();
        for (Condition c : conditions) {
            ids.add(c.id());
        }
        return ids.toString();
    }

    /** Vmp/n at {@code 1 + per}, both Vmp/Vmp at {@code 1 + 2 per}, and the two doubles identical. */
    private static void vampiric(String check, Body one, Body twoA, Body twoB, double control, boolean health,
                                 double per, String ctlLine) {
        if (one.late == null || twoA.late == null || twoB.late == null) {
            unsure(check, "a Vmp horse is missing or dead (Vmp/n " + one.late + ", Vmp/Vmp " + twoA.late + " / "
                    + twoB.late + ")");
            return;
        }
        double v1 = health ? one.late.health() : one.late.jump();
        double va = health ? twoA.late.health() : twoA.late.jump();
        double vb = health ? twoB.late.health() : twoB.late.jump();
        // The page's multiples, which are 1 + vampiricPerCopy() per copy; the per-copy value is printed so a
        // change to it reads as the page going stale rather than the gene breaking.
        double wantOne = control * (health ? 2.0 : 1.5);
        double wantTwo = control * (health ? 3.0 : 2.0);
        boolean ok = Math.abs(v1 - wantOne) < EPS && Math.abs(va - wantTwo) < EPS && Math.abs(vb - wantTwo) < EPS
                && Math.abs(va - vb) < EPS;
        String what = health ? "max_health" : "jump_strength";
        pass(check, ok, what + " base: Vmp/n " + f(v1) + " (x" + f(v1 / control) + ", expects " + f(wantOne)
                + "), Vmp/Vmp " + f(va) + " and " + f(vb) + " (x" + f(va / control) + " / x" + f(vb / control)
                + ", expects " + f(wantTwo) + "); control " + f(control) + "; vampiricPerCopy() " + per
                + one.moved() + twoA.moved() + twoB.moved() + "; " + ctlLine);
    }

    /**
     * The down allele, doubled: {@code max(floor, control x clamp(1 - d0 - d1))} off this horse's own copies -
     * {@code AbstractMagicStatGene.contribute} then {@code TraitBuilder.build}, written out again here.
     */
    private static void mirror(String check, Body body, AbstractMagicStatGene gene, String tokens, double control,
                               boolean health, double floor, String ctlLine) {
        if (body.late == null || body.horse == null) {
            unsure(check, "the " + tokens + " horse is missing or dead");
            return;
        }
        HorseRecord r = HorseRecords.of(body.horse);
        AllelePair pair = r.genotype().pair(gene.key());
        if (pair == null || !tokens.equals(pair.toTokens()) || !r.hasGenome()) {
            unsure(check, "the horse's record is not " + tokens + " with a rolled epigenome (pair "
                    + (pair == null ? "none" : pair.toTokens()) + ", genome " + r.hasGenome() + ")");
            return;
        }
        Epigenome.Copies copies = r.epigenome().copies(gene);
        double d0 = Epigenome.readable(gene, copies.first()).get(AbstractMagicStatGene.DELTA);
        double d1 = Epigenome.readable(gene, copies.second()).get(AbstractMagicStatGene.DELTA);
        double factor = Math.min(HorseTraits.MAGICAL_MAX_FACTOR,
                Math.max(HorseTraits.MAGICAL_MIN_FACTOR, 1.0 - d0 - d1));
        double want = Math.max(floor, control * factor);
        double got = health ? body.late.health() : body.late.jump();
        boolean ok = Math.abs(got - want) < EPS && got < control;
        pass(check, ok, (health ? "max_health" : "jump_strength") + " base " + f(got) + " vs control " + f(control)
                + " = x" + f(got / control) + " (page: about 0.8); copies' delta " + f(d0) + " + " + f(d1)
                + (copies.first().isEmpty() || copies.second().isEmpty() ? " (an EMPTY copy read as the midpoint)" : "")
                + " -> expects " + f(want) + body.moved() + "; " + ctlLine);
    }

    private static void fighter(Body body, Reading ctl) {
        if (body.late == null || body.horse == null) {
            unsure(FIGHTER_MIXED, "the Gld/Wmp horse is missing or dead");
            return;
        }
        HorseRecord r = HorseRecords.of(body.horse);
        Gene gene = Genes.MAGIC_FIGHTER;
        AllelePair pair = r.genotype().pair(MagicFighterGene.KEY);
        if (pair == null || !pair.has(Genes.MAGIC_FIGHTER.Gld) || !pair.has(Genes.MAGIC_FIGHTER.Wmp)
                || !r.hasGenome()) {
            unsure(FIGHTER_MIXED, "the horse's record is not Gld/Wmp with a rolled epigenome (pair "
                    + (pair == null ? "none" : pair.toTokens()) + ", genome " + r.hasGenome() + ")");
            return;
        }
        Epigenome.Copies copies = r.epigenome().copies(gene);
        double s0 = signed(pair.first().token(), Epigenome.readable(gene, copies.first()));
        double s1 = signed(pair.second().token(), Epigenome.readable(gene, copies.second()));
        double want = Math.max(MagicFighterGene.MIN_DAMAGE, MagicFighterGene.BASELINE_DAMAGE * (1.0 + s0 + s1));
        double got = body.late.attack();
        boolean ok = Math.abs(got - want) < EPS;
        pass(FIGHTER_MIXED, ok, "Gld/Wmp attack_damage base " + f(got) + ", code expects max(1, 3 x (1 + "
                + f(s0) + " + " + f(s1) + ")) = " + f(want) + " off copies " + pair.first().token() + "/"
                + pair.second().token() + "; within a point of 3.0 (the page's 'near the baseline'): "
                + (Math.abs(got - MagicFighterGene.BASELINE_DAMAGE) <= 1.0 ? "yes" : "NO - the code allows"
                + " 2.2 .. 5.55 for this pair, so that is the page overstating, not this horse failing")
                + body.moved() + "; control attack " + f(ctl.attack()));
    }

    private static double signed(String token, com.example.horsegenetics.common.genetics.epi.EpiValues v) {
        if ("Gld".equals(token)) {
            return v.get(MagicFighterGene.GLADIATOR_DELTA);
        }
        if ("Wmp".equals(token)) {
            return -v.get(MagicFighterGene.WIMP_DELTA);
        }
        return 0.0;
    }

    // ------------------------------------------------------------------
    // EAST - LAST STAND
    // ------------------------------------------------------------------

    private static final String SAVED = "LAST STAND - a full-health horse hit for 1000 is left alive on"
            + " last_stand_health";
    private static final String WINDOW = "LAST STAND - a 5 hit inside the immunity window changes nothing";
    private static final String SPENT = "LAST STAND - it is spent: after the window a second 1000 hit kills";
    private static final String COMES_BACK = "LAST STAND - it comes back: healed to full, it is saved again";
    private static final String VOID = "LAST STAND - the void still kills";
    private static final String BOLTS = "LAST STAND - the save and the bolt are one behaviour: bolting with both"
            + " escape boosts at once, still bolting when the window ends";
    private static final String CALMS = "LAST STAND - the escape boosts come back off when it calms";

    /** The handler's cooldown key - {@code HorseLastStandHandler.KEY}, private there, copied. */
    private static final String STAMP = "last_stand";
    /** {@code HorseEscapeGoal.SPEED_ID} / {@code JUMP_ID}, private there, copied. */
    private static final Identifier ESCAPE_SPEED = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
            "escape/speed");
    private static final Identifier ESCAPE_JUMP = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
            "escape/jump");

    /** Ticks after the build before the first blow, so every horse has joined and resolved its body. */
    private static final long START = 100;
    /** A blow this far into the window is past vanilla's ten-tick hurt cooldown, so only the window can refuse it. */
    private static final long WINDOW_HIT = 30;

    private static int immunity() {
        return ServerConfig.lastStandImmunityTicks();
    }

    private static void lastStand(ServerLevel level, int gy, int x0, int z0, int thisRun) {
        List<String> all = List.of(SAVED, WINDOW, SPENT, COMES_BACK, VOID, BOLTS, CALMS);
        for (String c : all) {
            DebugYardClockwork.expect(c);
        }
        Horse a = stoodIn(level, gy, x0 + 26, z0 + 1, "LAST STAND A (spent)");
        Horse b = stoodIn(level, gy, x0 + 31, z0 + 1, "LAST STAND B (re-arms)");
        Horse c = stoodIn(level, gy, x0 + 36, z0 + 1, "LAST STAND C (void)");
        Horse d = stoodIn(level, gy, x0 + 41, z0 + 1, "LAST STAND D (bolts)");
        Mob zombie = DebugYardClockwork.caged(level, EntityType.ZOMBIE, gy, x0 + 35, z0 + 9, "LAST STAND attacker");
        DebugPenManager.placeSign(level, new BlockPos(x0 + 26, gy + 1, z0 - 1), Direction.NORTH,
                List.of("LAST STAND", "no one-blow death;", "spent, re-arms,", "void, bolt"));

        step(level, START, thisRun, all, () -> {
            if (!ServerConfig.lastStand()) {
                for (String ch : all) {
                    unsure(ch, "behaviour.last_stand is off in this world's server config");
                }
                return;
            }
            int window = immunity();
            boolean zombieUp = zombie != null && zombie.isAlive();
            DamageSource blow = zombieUp ? level.damageSources().mobAttack(zombie) : null;
            if (blow == null) {
                // A-C need only a source the save may postpone, and magic is not in bypasses_invulnerability; the
                // bolt needs a live attacker, so without the zombie it goes INCONCLUSIVE below.
                blow = level.damageSources().magic();
            }
            final DamageSource hit = blow;

            // ---- A: saved, then the window, then spent ----
            Long aSaved = save(level, a, hit, SAVED, "A");
            if (aSaved == null) {
                unsure(WINDOW, "horse A was not saved, so there is no window to test");
                unsure(SPENT, "horse A was not saved, so there is nothing to have spent");
            } else if (window <= WINDOW_HIT + 5) {
                unsure(WINDOW, "last_stand_immunity_ticks is " + window + ", too short for a blow past vanilla's"
                        + " hurt cooldown to land inside it");
            }
            if (aSaved != null && window > WINDOW_HIT + 5) {
                step(level, WINDOW_HIT, thisRun, List.of(WINDOW), () -> {
                    if (!a.isAlive()) {
                        unsure(WINDOW, "horse A died before the window blow, of something else");
                        return;
                    }
                    float before = a.getHealth();
                    boolean landed = a.hurtServer(level, hit, 5.0F);
                    float after = a.getHealth();
                    boolean alive = a.isAlive() && !a.isDeadOrDying();
                    pass(WINDOW, alive && after >= before, "at +" + WINDOW_HIT + " of a " + window
                            + "-tick window: health " + before + " -> " + after + ", hurtServer returned " + landed
                            + ", alive " + alive + " (5 on that health would kill if the cancel did not land)");
                });
            }
            if (aSaved != null) {
                step(level, window + 40L, thisRun, List.of(SPENT), () -> {
                    if (!a.isAlive()) {
                        unsure(SPENT, "horse A was already dead before the second blow");
                        return;
                    }
                    float before = a.getHealth();
                    if (LastStand.rearms(before, a.getMaxHealth(), ServerConfig.lastStandRearmFraction())) {
                        unsure(SPENT, "horse A healed to the re-arm line (" + before + "/" + a.getMaxHealth()
                                + ") before the second blow, so it is fairly armed again");
                        return;
                    }
                    long stamp = stamp(a);
                    a.hurtServer(level, hit, 1000.0F);
                    boolean dead = !a.isAlive() || a.isDeadOrDying();
                    pass(SPENT, dead, "at +" + (window + 40) + " (window " + window + "), health " + before + "/"
                            + a.getMaxHealth() + ", stamp " + stamp + ": 1000 hit -> " + (dead ? "dead" : "ALIVE on "
                            + a.getHealth() + " - saved a second time without healing"));
                });
            }

            // ---- B: saved, healed to full, saved again ----
            Long bSaved = save(level, b, hit, null, "B");
            if (bSaved == null) {
                unsure(COMES_BACK, "horse B was not saved the first time (see the SAVED line for A), so nothing"
                        + " can come back");
            } else {
                long first = bSaved;
                step(level, window + 40L, thisRun, List.of(COMES_BACK), () -> {
                    if (b.isAlive()) {
                        b.setHealth(b.getMaxHealth());
                    }
                });
                step(level, window + 80L, thisRun, List.of(COMES_BACK), () -> {
                    if (!b.isAlive()) {
                        unsure(COMES_BACK, "horse B died before the second blow, of something else");
                        return;
                    }
                    float before = b.getHealth();
                    long stampBefore = stamp(b);
                    long now = level.getGameTime();
                    b.hurtServer(level, hit, 1000.0F);
                    boolean alive = b.isAlive() && !b.isDeadOrDying();
                    float want = LastStand.healthLeft(ServerConfig.lastStandHealth(), b.getMaxHealth());
                    long stampAfter = stamp(b);
                    pass(COMES_BACK, alive && Math.abs(b.getHealth() - want) < 1.0e-4 && stampAfter == now,
                            "first save at tick " + first + "; healed to " + before + "/" + b.getMaxHealth()
                                    + " (stamp still " + stampBefore + "), then 1000 at tick " + now + " -> "
                                    + (alive ? "alive on " + b.getHealth() + " (expects " + want + "), stamp now "
                                    + stampAfter : "DEAD - the re-arm read never fired"));
                });
            }

            // ---- C: the void ----
            if (c == null || !c.isAlive()) {
                unsure(VOID, "horse C is missing");
            } else {
                c.setHealth(c.getMaxHealth());
                long stamp = stamp(c);
                // UNVERIFIED: DamageSources.fellOutOfWorld() - no other call in this repo (see the class note).
                boolean landed = c.hurtServer(level, level.damageSources().fellOutOfWorld(), 1000.0F);
                boolean dead = !c.isAlive() || c.isDeadOrDying();
                pass(VOID, dead, "full health " + c.getMaxHealth() + ", save armed (stamp " + stamp
                        + "), fellOutOfWorld 1000: hurtServer returned " + landed + " -> "
                        + (dead ? "dead" : "ALIVE on " + c.getHealth() + " - bypasses_invulnerability is not exempt"));
            }

            // ---- D: the bolt ----
            double fraction = ServerConfig.escapeHealthFraction();
            if (!zombieUp) {
                unsure(BOLTS, "the zombie is gone, so no live attacker for HorseEscapeGoal.inDanger to read");
                unsure(CALMS, "the zombie is gone, so the bolt could not be set up");
                return;
            }
            if (fraction <= 0.0) {
                unsure(BOLTS, "behaviour.escape_health_fraction is " + fraction + " - bolting is off");
                unsure(CALMS, "behaviour.escape_health_fraction is " + fraction + " - bolting is off");
                return;
            }
            if (d == null || !d.isAlive()) {
                unsure(BOLTS, "horse D is missing");
                unsure(CALMS, "horse D is missing");
                return;
            }
            boolean boostedBefore = has(d, Attributes.MOVEMENT_SPEED, ESCAPE_SPEED)
                    || has(d, Attributes.JUMP_STRENGTH, ESCAPE_JUMP);
            Long dSaved = save(level, d, hit, null, "D");
            if (dSaved == null) {
                unsure(BOLTS, "horse D was not saved, so it is dead rather than bolting");
                unsure(CALMS, "horse D was not saved");
                return;
            }
            if (!Escape.bolts(d.getHealth(), d.getMaxHealth(), fraction)) {
                unsure(BOLTS, "the save left D on " + d.getHealth() + "/" + d.getMaxHealth() + ", above"
                        + " escape_health_fraction " + fraction + " - by design it does not bolt");
                unsure(CALMS, "D never had a reason to bolt");
                return;
            }
            boolean[] early = new boolean[3];
            step(level, 10, thisRun, List.of(BOLTS, CALMS), () -> {
                early[0] = d.isAlive() && HorseEscapeGoal.bolting(d);
                early[1] = has(d, Attributes.MOVEMENT_SPEED, ESCAPE_SPEED);
                early[2] = has(d, Attributes.JUMP_STRENGTH, ESCAPE_JUMP);
            });
            long end = Math.max(12L, window - 2L);
            step(level, end, thisRun, List.of(BOLTS, CALMS), () -> {
                boolean still = d.isAlive() && HorseEscapeGoal.bolting(d);
                AttributeInstance speed = d.getAttribute(Attributes.MOVEMENT_SPEED);
                String ratio = speed == null ? "?" : f(speed.getValue() / speed.getBaseValue());
                pass(BOLTS, early[0] && early[1] && early[2] && still,
                        "boosts on before the blow: " + boostedBefore + "; at +10: bolting " + early[0]
                                + ", escape/speed " + early[1] + ", escape/jump " + early[2] + "; at +" + end
                                + " (window " + window + "): bolting " + still + ", speed value/base x" + ratio
                                + " (SPEED_BOOST " + Escape.SPEED_BOOST + "), health " + d.getHealth() + "/"
                                + d.getMaxHealth());
            });
            step(level, window + 20L, thisRun, List.of(CALMS), () -> {
                if (d.isAlive()) {
                    d.setHealth(d.getMaxHealth());
                }
            });
            step(level, window + 60L, thisRun, List.of(CALMS), () -> {
                if (!d.isAlive()) {
                    unsure(CALMS, "horse D died before it could calm");
                    return;
                }
                if (!early[0] && !early[1] && !early[2]) {
                    unsure(CALMS, "D never bolted or took a boost, so there was nothing to come off (see BOLTS)");
                    return;
                }
                boolean bolting = HorseEscapeGoal.bolting(d);
                boolean speed = has(d, Attributes.MOVEMENT_SPEED, ESCAPE_SPEED);
                boolean jump = has(d, Attributes.JUMP_STRENGTH, ESCAPE_JUMP);
                pass(CALMS, !bolting && !speed && !jump, "healed to full at +" + (window + 20) + "; at +"
                        + (window + 60) + ": health " + d.getHealth() + "/" + d.getMaxHealth() + ", bolting "
                        + bolting + ", escape/speed " + speed + ", escape/jump " + jump
                        + (speed || jump ? " - stop never ran, the horse is permanently buffed" : ""));
            });
        });
    }

    /** A tamed, ownerless plain black horse in its own 4x4 glass cell whose north-west corner is {@code (x, z)}. */
    private static @Nullable Horse stoodIn(ServerLevel level, int gy, int x, int z, String label) {
        DebugYardClockwork.cell(level, gy, x, x + 3, z, z + 3);
        return DebugYardUnattended.horse(level, gy, x + 2.0, z + 2.0, Sex.FEMALE, BLACK, true, label);
    }

    /**
     * Bring {@code h} to full health and hit it for 1000. Returns the tick of the save, or {@code null} when it was
     * not saved. With a {@code check}, that is the verdict on the save itself; without, a failure is only logged.
     */
    private static @Nullable Long save(ServerLevel level, @Nullable Horse h, DamageSource hit,
                                       @Nullable String check, String who) {
        if (h == null || !h.isAlive()) {
            if (check != null) {
                unsure(check, "horse " + who + " is missing");
            }
            return null;
        }
        h.setHealth(h.getMaxHealth());
        long stampBefore = stamp(h);
        if (!LastStand.armed(stampBefore)) {
            if (check != null) {
                unsure(check, "horse " + who + "'s save was already spent at tick " + stampBefore
                        + " before the test began");
            }
            return null;
        }
        long now = level.getGameTime();
        float max = h.getMaxHealth();
        boolean landed = h.hurtServer(level, hit, 1000.0F);
        boolean alive = h.isAlive() && !h.isDeadOrDying();
        float want = LastStand.healthLeft(ServerConfig.lastStandHealth(), max);
        long stamp = stamp(h);
        boolean ok = alive && Math.abs(h.getHealth() - want) < 1.0e-4 && stamp == now;
        String detail = "horse " + who + " at full " + max + " took 1000 from " + hit.getMsgId() + " at tick " + now
                + ": hurtServer returned " + landed + ", " + (alive ? "alive on " + h.getHealth() : "DEAD")
                + " (expects " + want + " = healthLeft(last_stand_health " + ServerConfig.lastStandHealth()
                + ")), stamp " + stamp + " (expects " + now + "), window " + immunity() + " ticks";
        if (check != null) {
            pass(check, ok, detail);
        } else if (!ok) {
            ActionTrace.log("test yard", "LAST STAND setup: " + detail);
        }
        return ok ? now : null;
    }

    private static long stamp(Horse h) {
        return h.getData(ModAttachments.HORSE_COOLDOWNS.get()).last(STAMP);
    }

    private static boolean has(Horse h, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance i = h.getAttribute(attribute);
        return i != null && i.hasModifier(id);
    }
}
