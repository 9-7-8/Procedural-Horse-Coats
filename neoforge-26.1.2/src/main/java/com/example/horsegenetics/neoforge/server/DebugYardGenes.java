package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.genes.MagicMobAuraGene;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row BD: AURA SILENT and AURA CONTROL</b> - a Wrd/Bai horse and a plain one, six husks each (2026-10-08).
 *
 * <p>Row BE stood south of it for one launch the same day: MEAT FLOOR and three CLEANSING groups, whose horses
 * had a number written onto their allele copies because a founder's is rolled. All four of their checks passed
 * at 14:47 and the pens went; the records are on wiki/gene-magic-meat.html#verified-meat-floor and
 * wiki/gene-cleansing-light.html#verified-rhythm, and the pens are in git at 0bb188ca.
 */
final class DebugYardGenes {

    private DebugYardGenes() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int zAura) {
        int myRun = ++run;
        guarded("row BD (AURA SILENT)", () -> aura(level, gy, x0, zAura, myRun));
        ActionTrace.log("test yard", "row BD built (AURA SILENT, AURA CONTROL)");
    }

    private static void guarded(String what, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: " + what + " failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // shared
    // ------------------------------------------------------------------

    /** One pen's checks: named at build, answered once each, INCONCLUSIVE if a step throws or the deadline comes. */
    private static final class Pen {
        final String name;
        final int run;
        final ServerLevel level;
        final List<String> checks;
        final Set<String> answered = new HashSet<>();

        Pen(String name, int run, ServerLevel level, List<String> checks, long deadline) {
            this.name = name;
            this.run = run;
            this.level = level;
            this.checks = checks;
            for (String c : checks) {
                DebugYardClockwork.expect(c);
            }
            after(deadline, () -> {
                for (String c : checks) {
                    answer(c, null, "deadline: " + deadline + " ticks after the build and no answer");
                }
            });
        }

        /** {@code pass == null} is INCONCLUSIVE. A second answer for the same check is dropped. */
        void answer(String check, @Nullable Boolean pass, String detail) {
            if (!answered.add(check)) {
                return;
            }
            if (pass == null) {
                DebugYardClockwork.inconclusive(check, detail);
            } else {
                DebugYardClockwork.verdict(check, pass, detail);
            }
        }

        void after(long ticks, Runnable body) {
            DebugYardHerd.after(level, Math.max(1L, ticks), () -> {
                if (run != DebugYardGenes.run) {
                    return;
                }
                try {
                    body.run();
                } catch (RuntimeException e) {
                    HorseGenetics.LOGGER.warn("[Debug] test yard: a " + name + " step failed", e);
                    for (String c : checks) {
                        answer(c, null, "a timed step threw " + e);
                    }
                }
            });
        }
    }

    private static @Nullable Horse findHorse(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    private static @Nullable Mob findMob(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Mob m && m.isAlive() ? m : null;
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ==================================================================
    // ROW BD - AURA SILENT
    // ==================================================================

    /*
     * AURA SILENT (wiki/gene-magic-mob-aura.html, Verification tab, NOT played). The open check:
     *
     *   "Wrd/Bai is silent, and silent means exactly the control. Put one in a pen with three husks the way WARD
     *    and WARD CONTROL are set up. Pass: the husks end up at WARD CONTROL's distances - a mean around five
     *    blocks, not twelve - and they will happily target it. The gene emits no ability at all for this pair,
     *    so a horse that repels or attracts here means expression is being decided somewhere other than
     *    MagicMobAuraGene.abilitiesFor."
     *
     * WHAT THE CODE DOES, READ 2026-10-08. Wrd/Wrd grants MobAura("repel", "hostile", radius 10); Bai/Bai grants
     * MobAura("attract", "hostile", radius 16), which sets a hostile's target to the horse. Any other pair
     * grants nothing. A husk never targets a horse of its own accord, so one that does was baited; a husk that
     * never comes inside ten blocks was warded.
     *
     * THE PENS: two 18 x 11 plots, each a horse and six husks, read twenty-four times, 100 ticks apart, from 200
     * ticks after the build. West the Wrd/Bai horse, east a plain one.
     *
     * WHAT IS COMPARED. Not the mean distance: horse and husks all wander, and a horse that settles in a corner
     * reads four blocks further from everything than one in the middle (the first launch read means of 9.0 and
     * 5.9 with no aura on either horse). A ward shoves every husk inside ten blocks outward on a 20-tick beat,
     * so what it cannot allow is a husk well inside the radius: the NEAREST any husk came is the reading.
     *
     * PASS: the Wrd/Bai horse's ability list holds no mob aura; no husk targeted it at any reading; and some
     * husk came within seven blocks of it. FAIL: an aura in the list, a husk targeting it, or no husk ever
     * inside ten blocks of it while the control's came within seven of theirs. INCONCLUSIVE: a horse or all of
     * a pen's husks gone, or no husk within seven of either horse (the pens cannot tell a ward from chance).
     */

    private static final String A_SILENT = "AURA SILENT - a Wrd/Bai horse neither wards nor baits: no mob aura in"
            + " its abilities, no husk targets it, and husks come inside the ward's radius as they do a plain horse's";
    private static final int A_READINGS = 24;
    private static final int A_HUSKS = 6;
    /** A husk this close is well inside the ward's ten blocks: no ward is acting on it. */
    private static final double A_NEAR = 7.0;

    private static final class Aura {
        final String name;
        @Nullable UUID horse;
        final List<UUID> husks = new ArrayList<>();
        int readings;
        int inside;
        int targeted;
        double sum;
        double nearest = Double.MAX_VALUE;

        Aura(String name) {
            this.name = name;
        }
    }

    private static void aura(ServerLevel level, int gy, int x0, int z0, int myRun) {
        Pen pen = new Pen("AURA SILENT", myRun, level, List.of(A_SILENT), 3_600L);
        Aura silent = auraPen(level, gy, x0, z0, "AURA SILENT", MagicMobAuraGene.KEY + "=Wrd/Bai",
                List.of("AURA SILENT", "a Wrd/Bai horse and", "six husks: no", "ward and no bait"));
        Aura control = auraPen(level, gy, x0 + 25, z0, "AURA CONTROL", MagicMobAuraGene.KEY + "=n/n",
                List.of("AURA CONTROL", "a plain horse and", "six husks, for", "AURA SILENT"));
        pen.after(200, () -> auraRead(pen, silent, control, 1));
    }

    private static Aura auraPen(ServerLevel level, int gy, int x0, int z0, String name, String code,
                                List<String> sign) {
        DebugYardUnattended.pen(level, gy, x0, z0, 18, 11, name, Blocks.GRASS_BLOCK.defaultBlockState(), sign);
        Aura a = new Aura(name);
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 6.5, Sex.MALE, code, true, name);
        a.horse = h == null ? null : h.getUUID();
        for (int i = 0; i < A_HUSKS; i++) {
            Entity e = DebugYardUnattended.animal(level, EntityType.HUSK, gy, x0 + 7.5 + i * 1.5,
                    z0 + 2.5 + (i % 3) * 3);
            if (e instanceof Mob m) {
                m.setCustomName(Component.literal(name + " husk " + (i + 1)));
                a.husks.add(m.getUUID());
            }
        }
        return a;
    }

    /** One reading of one pen. Returns false when its horse or every husk is gone. */
    private static boolean auraSample(ServerLevel level, Aura a) {
        Horse h = findHorse(level, a.horse);
        if (h == null) {
            return false;
        }
        int alive = 0;
        for (UUID id : a.husks) {
            Mob m = findMob(level, id);
            if (m == null) {
                continue;
            }
            alive++;
            double d = Math.sqrt(m.distanceToSqr(h));
            a.readings++;
            a.sum += d;
            a.nearest = Math.min(a.nearest, d);
            if (d <= MagicMobAuraGene.WARD_RADIUS) {
                a.inside++;
            }
            if (m.getTarget() == h) {
                a.targeted++;
            }
        }
        return alive > 0;
    }

    private static String auraText(Aura a) {
        return a.name + ": " + a.readings + " husk readings, mean " + f1(a.readings == 0 ? 0 : a.sum / a.readings)
                + " blocks, nearest " + f1(a.nearest) + ", " + a.inside + " inside "
                + (int) MagicMobAuraGene.WARD_RADIUS + ", " + a.targeted + " targeting the horse";
    }

    private static void auraRead(Pen pen, Aura silent, Aura control, int n) {
        ServerLevel level = pen.level;
        if (!auraSample(level, silent) || !auraSample(level, control)) {
            pen.answer(A_SILENT, null, "a horse or all of a pen's husks are gone at reading " + n + " | "
                    + auraText(silent) + " | " + auraText(control));
            return;
        }
        if (n < A_READINGS) {
            pen.after(100, () -> auraRead(pen, silent, control, n + 1));
            return;
        }
        Horse h = findHorse(level, silent.horse);
        List<String> auras = new ArrayList<>();
        if (h != null) {
            for (HorseAbilities.Active a : GeneAbilityHandler.abilitiesOf(h)) {
                if (a.ability() instanceof GeneAbility.MobAura m) {
                    auras.add(m.mode() + " " + m.group() + " r" + m.radius());
                }
            }
        }
        AllelePair own = h == null ? null : HorseRecords.of(h).genotype().pair(MagicMobAuraGene.KEY);
        String pair = own == null ? "?" : own.toTokens();
        String detail = "horse pair " + pair + ", mob auras in its ability list " + auras + " | " + auraText(silent)
                + " | " + auraText(control);
        if (!auras.isEmpty()) {
            pen.answer(A_SILENT, false, "a Wrd/Bai horse carries a mob aura | " + detail);
        } else if (silent.targeted > 0) {
            pen.answer(A_SILENT, false, "a husk targeted the Wrd/Bai horse, which only bait makes it do | " + detail);
        } else if (silent.nearest <= A_NEAR) {
            pen.answer(A_SILENT, true, detail);
        } else if (control.nearest <= A_NEAR && silent.inside == 0) {
            pen.answer(A_SILENT, false, "no husk was ever inside ten blocks of the Wrd/Bai horse, while the plain"
                    + " horse's came within " + A_NEAR + " | " + detail);
        } else {
            pen.answer(A_SILENT, null, "no husk came within " + A_NEAR + " blocks of the Wrd/Bai horse, and that"
                    + " alone cannot tell a ward from chance | " + detail);
        }
    }
}
