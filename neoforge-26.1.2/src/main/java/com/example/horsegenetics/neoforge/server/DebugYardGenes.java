package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.genes.CleansingLightGene;
import com.example.horsegenetics.common.genetics.genes.MagicMeatGene;
import com.example.horsegenetics.common.genetics.genes.MagicMobAuraGene;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Rows BD and BE: four gene checks that needed a number written on the copy, or a count nobody can take by
 * eye</b> (2026-10-08). Each was an open line on a gene page's Verification tab, quoted above its pen below.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>BD</td><td>AURA SILENT - a Wrd/Bai horse and six husks</td><td>AURA CONTROL - a plain horse and
 *       six husks</td></tr>
 *   <tr><td>BE</td><td>MEAT FLOOR (north) - five horses killed for their beef; CLEANSING NARROW (south) - a
 *       radius-4 horse and a ladder of zombies</td><td>CLEANSING CROWD (north) - twelve zombies round one
 *       horse; CLEANSING WIDE (south) - a radius-8 horse and the same ladder</td></tr>
 * </table>
 *
 * <p>Every check answers through {@link DebugYardClockwork#verdict}, inside two minutes of the build. The horses
 * whose test is a number on the allele copy are spawned and then have that number written
 * ({@link #horseWith}), because a founder's is rolled and a pen that waits for the roll it needs answers one
 * launch in ten.
 *
 * <p><b>Where things stand matters.</b> Cleansing light hurts every undead inside its radius and a husk is
 * undead, so the aura row is north of the cleansing row and the widest cleansing horse is at the south end of
 * its own: the nearest husk is fifteen blocks from it. The crowd's horse is written a radius of 6.5 and stands
 * eight from the aura pens and nine from the wide ladder.
 */
@EventBusSubscriber
final class DebugYardGenes {

    private DebugYardGenes() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int zAura, int zGenes) {
        int myRun = ++run;
        WATCHED.clear();
        DROPS.clear();
        guarded("row BD (AURA SILENT)", () -> aura(level, gy, x0, zAura, myRun));
        guarded("row BE west (MEAT FLOOR)", () -> meat(level, gy, x0, zGenes, myRun));
        guarded("row BE (CLEANSING)", () -> cleansing(level, gy, x0, zGenes, myRun));
        ActionTrace.log("test yard", "rows BD and BE built (AURA SILENT, AURA CONTROL; MEAT FLOOR, CLEANSING"
                + " NARROW, CLEANSING WIDE, CLEANSING CROWD)");
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

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /**
     * A yard horse with one named number written on both copies of one gene. The horse is spawned the ordinary
     * way and its record replaced before it has ticked; {@code EpiValues.with} clamps to the gene's own hard
     * bound, so what was written is read back by the caller rather than assumed.
     */
    private static @Nullable Horse horseWith(ServerLevel level, int gy, double x, double z, String code,
                                             boolean tamed, String label, String geneKey, String value,
                                             double first, double second) {
        Horse h = DebugYardUnattended.horse(level, gy, x, z, Sex.FEMALE, code, tamed, label);
        Gene gene = Genes.byKeyOrNull(geneKey);
        if (h == null || gene == null) {
            return h;
        }
        HorseRecord r = HorseRecords.of(h);
        Epigenome.Copies c = r.epigenome().copies(gene);
        Epigenome.Copies next = new Epigenome.Copies(
                new AlleleEpigenetics(c.first().priority(), Epigenome.readable(gene, c.first()).with(value, first)),
                new AlleleEpigenetics(c.second().priority(),
                        Epigenome.readable(gene, c.second()).with(value, second)));
        HorseRecords.apply(h, r.withGenome(new Genome(r.genotype(), r.epigenome().with(geneKey, next))));
        DebugTestYard.label(h, label);   // apply() put the record's own name back on it
        return h;
    }

    /** The two copies' value for {@code name}, read off the horse's record. */
    private static double[] copies(Horse h, String geneKey, String name) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        if (gene == null) {
            return new double[] {Double.NaN, Double.NaN};
        }
        Epigenome.Copies c = HorseRecords.of(h).epigenome().copies(gene);
        return new double[] {Epigenome.readable(gene, c.first()).get(name),
                Epigenome.readable(gene, c.second()).get(name)};
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

    // ==================================================================
    // ROW BE - MEAT FLOOR
    // ==================================================================

    /*
     * MEAT FLOOR (wiki/gene-magic-meat.html, Verification tab, NOT played). The open check:
     *
     *   "Never zero. Kill a Mty/Mty horse whose two yields both read below 0.5. Pass: a meaty horse always
     *    drops at least one beef, however low the yield numbers on its copies drifted."
     *
     * The yard's DEATH DROPS II pen (2026-10-01) could not ask it: a founder's yields are rolled between 2 and 7.
     * So these are written. MagicMeatGene.abilitiesFor gives ItemDrop("meat", n, n) with
     * n = max(1, round(mean of the two copies' yield)); the hard bound on a drifted yield is far below zero
     * (EpiValue's clamp is eight design spans wide), so a negative mean is reachable and is asked too.
     *
     * FIVE UNTAMED Mty/Mty HORSES, each in a glass cell, killed 100 ticks after the build; LivingDropsEvent is
     * read at LOWEST priority, after GeneDeathHandler has added the beef:
     *   0.1 and 0.1 - 0.4 and 0.4 - 0.2 and 0.6 - -3 and -3 (all round to nothing or less: one beef each)
     *   3 and 3     (the control: three beef, or the count this pen reads is not the gene's)
     *
     * PASS: every low horse dropped exactly one beef and the control three. FAIL otherwise. INCONCLUSIVE: a
     * horse missing, alive after the kill, or a written yield that did not read back as written.
     */

    private static final String M_FLOOR = "MEAT FLOOR - a Mty/Mty horse whose yields average under 0.5, or below"
            + " zero, still drops one beef";
    private static final double[][] M_YIELDS = {{0.1, 0.1}, {0.4, 0.4}, {0.2, 0.6}, {-3.0, -3.0}, {3.0, 3.0}};

    /** The horses whose drops are this row's business, and what each left. */
    private static final Set<UUID> WATCHED = new HashSet<>();
    private static final Map<UUID, List<ItemStack>> DROPS = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !WATCHED.contains(horse.getUUID())) {
            return;
        }
        List<ItemStack> copy = new ArrayList<>();
        for (ItemEntity e : event.getDrops()) {
            copy.add(e.getItem().copy());
        }
        DROPS.put(horse.getUUID(), copy);
    }

    private static void meat(ServerLevel level, int gy, int x0, int z0, int myRun) {
        Pen pen = new Pen("MEAT FLOOR", myRun, level, List.of(M_FLOOR), 1_200L);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("MEAT FLOOR", "Mty/Mty, yields", "written near zero:", "one beef each"));
        List<UUID> horses = new ArrayList<>();
        for (int i = 0; i < M_YIELDS.length; i++) {
            int cx = x0 + 1 + 3 * i;
            DebugYardClockwork.cell(level, gy, cx, cx + 3, z0 + 1, z0 + 4);
            Horse h = horseWith(level, gy, cx + 2.0, z0 + 3.0, MagicMeatGene.KEY + "=Mty/Mty", false,
                    "MEAT FLOOR " + (i + 1), MagicMeatGene.KEY, MagicMeatGene.YIELD, M_YIELDS[i][0], M_YIELDS[i][1]);
            horses.add(h == null ? null : h.getUUID());
            if (h != null) {
                WATCHED.add(h.getUUID());
            }
        }
        pen.after(100, () -> {
            StringBuilder each = new StringBuilder();
            int[] want = new int[M_YIELDS.length];
            boolean setup = true;
            for (int i = 0; i < M_YIELDS.length; i++) {
                Horse h = findHorse(level, horses.get(i));
                if (h == null) {
                    setup = false;
                    each.append("horse ").append(i + 1).append(" MISSING; ");
                    continue;
                }
                double[] y = copies(h, MagicMeatGene.KEY, MagicMeatGene.YIELD);
                if (Math.abs(y[0] - M_YIELDS[i][0]) > 1e-9 || Math.abs(y[1] - M_YIELDS[i][1]) > 1e-9) {
                    setup = false;
                }
                // The gene's own arithmetic, done here from the copies rather than asked of the gene.
                want[i] = Math.max(1, (int) Math.round((y[0] + y[1]) / 2.0));
                int listed = -1;
                for (HorseAbilities.Active a : GeneAbilityHandler.abilitiesOf(h)) {
                    if (a.ability() instanceof GeneAbility.ItemDrop d && a.geneKey().equals(MagicMeatGene.KEY)) {
                        listed = d.min();
                    }
                }
                each.append("horse ").append(i + 1).append(" yields ").append(f2(y[0])).append(" and ")
                        .append(f2(y[1])).append(" (ability list says ").append(listed).append("); ");
                h.kill(level);
            }
            boolean ready = setup;
            String read = each.toString();
            pen.after(40, () -> {
                StringBuilder got = new StringBuilder();
                boolean ok = ready;
                boolean all = ready;
                for (int i = 0; i < M_YIELDS.length; i++) {
                    UUID id = horses.get(i);
                    List<ItemStack> drops = id == null ? null : DROPS.get(id);
                    if (drops == null || findHorse(level, id) != null) {
                        all = false;
                        got.append("horse ").append(i + 1).append(" left no drop list; ");
                        continue;
                    }
                    int beef = 0;
                    StringBuilder others = new StringBuilder();
                    for (ItemStack s : drops) {
                        if (s.is(Items.BEEF)) {
                            beef += s.getCount();
                        } else {
                            others.append(' ').append(s.getCount()).append("x ").append(s.getItem());
                        }
                    }
                    ok &= beef == want[i];
                    got.append("horse ").append(i + 1).append(" dropped ").append(beef).append(" beef (want ")
                            .append(want[i]).append(")").append(others.length() == 0 ? "" : " and" + others)
                            .append("; ");
                }
                pen.answer(M_FLOOR, all ? ok : null, read + "| " + got);
            });
        });
    }

    // ==================================================================
    // ROW BE - CLEANSING
    // ==================================================================

    /*
     * CLEANSING (wiki/gene-cleansing-light.html, Verification tab, NOT played). The three open checks:
     *
     *   "The beat is slow, and that is why it is cheap. Watch one zombie's health while it stands in the aura.
     *    Pass: it loses CleansingLightGene.DAMAGE in discrete steps about two seconds apart, not continuously."
     *   "The cap bounds the scan. Crowd more undead into range than MAX_TARGETS. Pass: only that many are being
     *    hurt per beat and the rest wait their turn - not every mob in the box at once."
     *   "The radius reads as a radius. Two expressing horses at either end of the epigenetic range. Pass: the
     *    wider one reaches undead the narrower one does not."
     *
     * WHAT THE CODE DOES, READ 2026-10-08. Cln/Cln grants Healing("group", "undead", radius off the copy,
     * amount -2, every 40 ticks, at most 8). GeneAbilityHandler.heal runs on the horse's own beat, walks the
     * living entities in the radius-inflated box, and hurts (magic damage, which armour does not reduce) each
     * undead whose distance to the horse is within the radius, stopping at eight. So a hurt is a drop of
     * exactly 2.0 in a zombie's health, and every drop of one beat lands in one server tick.
     *
     * THE SAMPLER reads every zombie's health once a tick, in the tick's Post phase, for fourteen beats.
     *
     * NARROW and WIDE: a Cln/Cln horse in a glass cell with radius written 4.0 (NARROW) or 8.0 (WIDE) on both
     * copies, and five zombies in cells of their own in a line, 2.5, 4.5, 6.5, 8.5 and 10.5 blocks from it. Each
     * zombie's nearest and farthest distance over the run is kept, since horse and zombie both shift a little in
     * their cells; a zombie whose distance came within a quarter block of the radius is not judged.
     *   BEAT  - PASS: at least one zombie took four or more hits, every hit on every zombie was exactly DAMAGE,
     *           and every gap between one zombie's consecutive hits was INTERVAL_TICKS. FAIL otherwise.
     *   REACH - PASS: the ability's radius is the number written on the copies for both horses; every zombie
     *           inside its horse's radius was hurt and none outside was; and at least one rung of the ladder was
     *           hurt by WIDE and left alone by NARROW. FAIL otherwise.
     *
     * CROWD: a Cln/Cln horse written radius 6.5 beside one cell of twelve zombies, all within five blocks.
     *   CAP   - PASS: on a beat with more than MAX_TARGETS zombies alive inside the radius, exactly MAX_TARGETS
     *           were hurt, and no beat hurt more. FAIL: any beat hurt more, or a beat with nine or more in
     *           range hurt fewer than eight. The beat on which a zombie outside the first eight was first hurt
     *           is logged: that is "the rest wait their turn".
     * A horse missing, or a radius that did not read back as written, is INCONCLUSIVE.
     */

    private static final String C_BEAT = "CLEANSING - a zombie in the aura loses DAMAGE in steps one beat apart";
    private static final String C_REACH = "CLEANSING - a radius-8 horse hurts zombies a radius-4 horse leaves"
            + " alone, and neither reaches past the radius on its copies";
    private static final String C_CAP = "CLEANSING - with twelve zombies in range no beat hurts more than"
            + " MAX_TARGETS, and the rest wait their turn";
    private static final long C_START = 40L;
    private static final long C_WATCH = 14L * CleansingLightGene.INTERVAL_TICKS + 20L;
    private static final int RUNGS = 5;
    private static final int CROWD = 12;
    private static final double CROWD_RADIUS = 6.5;
    /** A zombie whose distance came this close to the radius is not judged either way. */
    private static final double EDGE = 0.25;

    /** One horse and the zombies it is watched against. */
    private static final class Group {
        final String name;
        final double written;
        @Nullable UUID horse;
        final List<UUID> zombies = new ArrayList<>();
        float[] last;
        double[] minD;
        double[] maxD;
        final List<List<long[]>> hits = new ArrayList<>();     // per zombie: {tick, amount x 100}
        /** Zombies hurt per tick, for ticks on which any was, and how many stood alive inside the radius then. */
        final List<int[]> beats = new ArrayList<>();           // {tick, hurt, inRadius}
        double radius = Double.NaN;

        Group(String name, double written) {
            this.name = name;
            this.written = written;
        }
    }

    private static void cleansing(ServerLevel level, int gy, int x0, int z0, int myRun) {
        Pen pen = new Pen("CLEANSING", myRun, level, List.of(C_BEAT, C_REACH, C_CAP), 2_400L);
        Group narrow = ladder(level, gy, x0, z0 + 8, "CLEANSING NARROW", 4.0);
        Group wide = ladder(level, gy, x0 + 25, z0 + 8, "CLEANSING WIDE", 8.0);
        Group crowd = crowd(level, gy, x0 + 25, z0);
        List<Group> groups = List.of(narrow, wide, crowd);
        pen.after(C_START, () -> {
            for (Group g : groups) {
                Horse h = findHorse(level, g.horse);
                if (h == null) {
                    continue;
                }
                for (HorseAbilities.Active a : GeneAbilityHandler.abilitiesOf(h)) {
                    if (a.ability() instanceof GeneAbility.Healing heal
                            && a.geneKey().equals(CleansingLightGene.KEY)) {
                        g.radius = heal.radius();
                    }
                }
                double[] c = copies(h, CleansingLightGene.KEY, CleansingLightGene.RADIUS);
                ActionTrace.log("test yard", g.name + ": radius written " + g.written + ", copies read "
                        + f2(c[0]) + " and " + f2(c[1]) + ", the ability's radius " + f2(g.radius) + "; "
                        + g.zombies.size() + " zombie(s)");
            }
            cleansingTick(pen, groups, level.getGameTime() + C_WATCH);
        });
    }

    private static Group ladder(ServerLevel level, int gy, int x0, int z0, String name, double radius) {
        Group g = new Group(name, radius);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of(name, "Cln/Cln, radius " + radius, "zombies at 2.5 to", "10.5 blocks"));
        DebugYardClockwork.cell(level, gy, x0 + 1, x0 + 4, z0, z0 + 3);
        Horse h = horseWith(level, gy, x0 + 3.0, z0 + 2.0, CleansingLightGene.KEY + "=Cln/Cln", true, name,
                CleansingLightGene.KEY, CleansingLightGene.RADIUS, radius, radius);
        g.horse = h == null ? null : h.getUUID();
        for (int k = 0; k < RUNGS; k++) {
            Mob z = DebugYardClockwork.caged(level, EntityType.ZOMBIE, gy, x0 + 5 + 2 * k, z0 + 1,
                    name + " zombie " + (k + 1));
            g.zombies.add(z == null ? null : z.getUUID());
        }
        return g;
    }

    private static Group crowd(ServerLevel level, int gy, int x0, int z0) {
        String name = "CLEANSING CROWD";
        Group g = new Group(name, CROWD_RADIUS);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 13, gy + 1, z0 - 1), Direction.NORTH,
                List.of(name, "Cln/Cln and twelve", "zombies: eight a", "beat, no more"));
        DebugYardClockwork.cell(level, gy, x0 + 12, x0 + 16, z0, z0 + 4);
        DebugYardClockwork.cell(level, gy, x0 + 16, x0 + 19, z0 + 1, z0 + 4);
        Horse h = horseWith(level, gy, x0 + 18.0, z0 + 3.0, CleansingLightGene.KEY + "=Cln/Cln", true, name,
                CleansingLightGene.KEY, CleansingLightGene.RADIUS, CROWD_RADIUS, CROWD_RADIUS);
        g.horse = h == null ? null : h.getUUID();
        for (int i = 0; i < CROWD; i++) {
            Mob z = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            if (z == null) {
                g.zombies.add(null);
                continue;
            }
            z.setPos(x0 + 13.5 + i % 3, gy + 1, z0 + 1.5 + (i / 3) % 3);
            z.setPersistenceRequired();
            z.setCustomName(Component.literal(name + " zombie " + (i + 1)));
            level.addFreshEntity(z);
            g.zombies.add(z.getUUID());
        }
        return g;
    }

    private static void cleansingTick(Pen pen, List<Group> groups, long until) {
        ServerLevel level = pen.level;
        long now = level.getGameTime();
        for (Group g : groups) {
            Horse h = findHorse(level, g.horse);
            int n = g.zombies.size();
            if (g.last == null) {
                g.last = new float[n];
                g.minD = new double[n];
                g.maxD = new double[n];
                for (int i = 0; i < n; i++) {
                    g.last[i] = -1.0F;
                    g.minD[i] = Double.MAX_VALUE;
                    g.hits.add(new ArrayList<>());
                }
            }
            int hurt = 0;
            int inRadius = 0;
            for (int i = 0; i < n; i++) {
                Mob z = level.getEntity(g.zombies.get(i) == null ? new UUID(0L, 0L) : g.zombies.get(i))
                        instanceof Mob m ? m : null;
                if (z == null) {
                    continue;
                }
                // A zombie that died this tick is still here for its last reading, at health 0.
                float health = z.isAlive() ? z.getHealth() : 0.0F;
                if (h != null) {
                    double d = Math.sqrt(z.distanceToSqr(h));
                    g.minD[i] = Math.min(g.minD[i], d);
                    g.maxD[i] = Math.max(g.maxD[i], d);
                    if (d <= g.radius && (z.isAlive() || g.last[i] > health)) {
                        inRadius++;
                    }
                }
                if (g.last[i] >= 0.0F && g.last[i] - health > 0.01F) {
                    hurt++;
                    g.hits.get(i).add(new long[] {now, Math.round((g.last[i] - health) * 100.0F)});
                }
                g.last[i] = z.isAlive() ? health : -1.0F;
            }
            if (hurt > 0) {
                g.beats.add(new int[] {(int) now, hurt, inRadius});
            }
        }
        if (now < until) {
            pen.after(1, () -> cleansingTick(pen, groups, until));
        } else {
            cleansingJudge(pen, groups.get(0), groups.get(1), groups.get(2));
        }
    }

    private static String hitText(Group g, int i) {
        StringBuilder sb = new StringBuilder();
        long prev = -1;
        for (long[] hit : g.hits.get(i)) {
            sb.append(sb.length() == 0 ? "" : ",").append(prev < 0 ? "t" + hit[0] : "+" + (hit[0] - prev));
            if (hit[1] != Math.round(-CleansingLightGene.DAMAGE * 100.0)) {
                sb.append("(-").append(hit[1] / 100.0).append(')');
            }
            prev = hit[0];
        }
        return sb.length() == 0 ? "never" : sb.toString();
    }

    private static void cleansingJudge(Pen pen, Group narrow, Group wide, Group crowd) {
        ServerLevel level = pen.level;
        long damage = Math.round(-CleansingLightGene.DAMAGE * 100.0);

        // BEAT - every zombie of both ladders.
        int most = 0;
        boolean steps = true;
        StringBuilder beat = new StringBuilder();
        // The ladders only: each of their zombies is alone in range of its horse's eight, so nothing but the
        // beat decides when it is hit. In the crowd the cap does too.
        for (Group g : List.of(narrow, wide)) {
            for (int i = 0; i < g.hits.size(); i++) {
                List<long[]> hits = g.hits.get(i);
                most = Math.max(most, hits.size());
                for (int k = 0; k < hits.size(); k++) {
                    if (hits.get(k)[1] != damage
                            || (k > 0 && hits.get(k)[0] - hits.get(k - 1)[0] != CleansingLightGene.INTERVAL_TICKS)) {
                        steps = false;
                    }
                }
            }
        }
        for (Group g : List.of(narrow, wide)) {
            beat.append(g.name).append(" zombie 1: ").append(hitText(g, 0)).append("; ");
        }
        pen.answer(C_BEAT, most < 4 ? null : steps, "most hits on one zombie " + most + ", every hit "
                + (steps ? "" : "NOT ") + "exactly " + -CleansingLightGene.DAMAGE + " and " + (steps ? "" : "NOT ")
                + "exactly " + CleansingLightGene.INTERVAL_TICKS + " ticks after that zombie's last | " + beat);

        // REACH - the two ladders.
        boolean setup = true;
        boolean ok = true;
        boolean[][] hurtRung = new boolean[2][RUNGS];
        boolean[][] judged = new boolean[2][RUNGS];
        StringBuilder reach = new StringBuilder();
        int g = 0;
        for (Group l : List.of(narrow, wide)) {
            if (findHorse(level, l.horse) == null || Math.abs(l.radius - l.written) > 1e-6) {
                setup = false;
            }
            reach.append(l.name).append(" (radius ").append(f2(l.radius)).append("): ");
            for (int i = 0; i < l.hits.size(); i++) {
                boolean hurt = !l.hits.get(i).isEmpty();
                boolean inside = l.maxD[i] <= l.radius - EDGE;
                boolean outside = l.minD[i] >= l.radius + EDGE;
                hurtRung[g][i] = hurt;
                judged[g][i] = inside || outside;
                if ((inside && !hurt) || (outside && hurt)) {
                    ok = false;
                }
                reach.append(f1(l.minD[i])).append(l.maxD[i] - l.minD[i] > 0.05 ? "-" + f1(l.maxD[i]) : "")
                        .append(hurt ? " hurt x" + l.hits.get(i).size() : " untouched")
                        .append(inside || outside ? "" : " (on the edge, not judged)").append(", ");
            }
            reach.append("| ");
            g++;
        }
        boolean differ = false;
        for (int i = 0; i < RUNGS; i++) {
            if (judged[0][i] && judged[1][i] && hurtRung[1][i] && !hurtRung[0][i]) {
                differ = true;
            }
        }
        pen.answer(C_REACH, setup ? ok && differ : null, reach + (differ ? "a rung WIDE hurt and NARROW did not"
                : "NO rung that WIDE hurt and NARROW did not") + (setup ? "" : "; a horse is missing or its ability's"
                + " radius is not the number written on its copies"));

        // CAP - the crowd.
        int over = 0;
        int under = 0;
        int asked = 0;
        Set<Integer> firstEight = new HashSet<>();
        StringBuilder cap = new StringBuilder();
        for (int[] b : crowd.beats) {
            if (b[1] > CleansingLightGene.MAX_TARGETS) {
                over++;
            }
            if (b[2] > CleansingLightGene.MAX_TARGETS) {
                asked++;
                if (b[1] < CleansingLightGene.MAX_TARGETS) {
                    under++;
                }
            }
            cap.append(cap.length() == 0 ? "" : ", ").append(b[1]).append('/').append(b[2]);
        }
        long firstBeat = crowd.beats.isEmpty() ? -1 : crowd.beats.get(0)[0];
        long lateFirst = -1;
        for (int i = 0; i < crowd.hits.size(); i++) {
            List<long[]> hits = crowd.hits.get(i);
            if (hits.isEmpty()) {
                continue;
            }
            if (hits.get(0)[0] == firstBeat) {
                firstEight.add(i);
            } else if (lateFirst < 0 || hits.get(0)[0] < lateFirst) {
                lateFirst = hits.get(0)[0];
            }
        }
        String waited = lateFirst < 0 ? "no zombie outside the first beat's " + firstEight.size() + " was hurt in "
                + crowd.beats.size() + " beats" : "a zombie outside the first beat's " + firstEight.size()
                + " was first hurt " + (lateFirst - firstBeat) / CleansingLightGene.INTERVAL_TICKS + " beats later";
        String capDetail = "hurt/alive-in-radius per beat: " + cap + " | cap " + CleansingLightGene.MAX_TARGETS
                + ", radius " + f2(crowd.radius) + "; " + waited;
        if (findHorse(level, crowd.horse) == null || Math.abs(crowd.radius - crowd.written) > 1e-6) {
            pen.answer(C_CAP, null, "the crowd's horse is missing or its radius is not the one written | "
                    + capDetail);
        } else if (over > 0) {
            pen.answer(C_CAP, false, over + " beat(s) hurt more than the cap | " + capDetail);
        } else if (asked == 0) {
            pen.answer(C_CAP, null, "no beat had more than " + CleansingLightGene.MAX_TARGETS
                    + " zombies inside the radius, so the cap was never asked to act | " + capDetail);
        } else {
            pen.answer(C_CAP, under == 0, (under == 0 ? "" : under + " beat(s) with more than the cap in range hurt"
                    + " fewer than the cap | ") + capDetail);
        }
    }
}
