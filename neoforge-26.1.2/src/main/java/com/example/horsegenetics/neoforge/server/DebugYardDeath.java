package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.MagicItemDropGene;
import com.example.horsegenetics.common.genetics.genes.MagicMeatGene;
import com.example.horsegenetics.common.genetics.genes.MagicOnDeathGene;
import com.example.horsegenetics.common.genetics.genes.ShadowcreatureGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.item.PresetHorseSpawnEggItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row AS: what a death leaves behind, and what a glowing horse leaves behind it</b> (2026-10-01). Two
 * clockwork pens, both of which start themselves at build, run on {@link DebugYardHerd#after} and answer every
 * question they ask with one {@code CLOCKWORK PASS/FAIL/INCONCLUSIVE} line - nobody has to be there.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>pages</th></tr>
 *   <tr><td>west, x0 .. x0+19</td><td>DEATH DROPS II - fifteen glass cells, 26 horses killed in two waves</td>
 *       <td>gene-magic-on-death, gene-magic-item-drop, gene-magic-meat</td></tr>
 *   <tr><td>east, x0+25 .. x0+44</td><td>SHADOW LIGHT - two sealed glass corridors, one glowing horse walked
 *       down each</td><td>gene-shadowcreature, gene-suntouched</td></tr>
 * </table>
 *
 * <h2>Why drops are read off the event, not off the ground</h2>
 * Every horse here is killed by {@code genericKill} (not fire, not lava - a burning horse's drops would be
 * cooked, and lava would eat them), and what it left is taken from {@link LivingDropsEvent} at
 * {@link EventPriority#LOWEST}, scoped to the UUIDs this pen spawned. LOWEST runs after
 * {@code GeneDeathHandler.onHorseDrops} (default priority), so the list read is the final one - vanilla's
 * leather from the loot table plus or minus what the gene did - and it is exactly what
 * {@code LivingEntity.dropAllDeathLoot} then adds to the world (read in the 26.1.2 sources: it spawns the
 * captured list unless the event was cancelled, and a cancelled event never reaches this listener). A box scan
 * would have to separate one cell's leather from the next; a UUID cannot be confused. The ground is still read
 * where the ground is the question: the fluid a death places, and the egg, which is taken out of the drop and
 * actually hatched.
 *
 * <h2>Leather is 0 to 2</h2>
 * Vanilla's horse loot table rolls leather uniformly from 0 to 2, so any one kill dropping no leather proves
 * nothing. Every leather claim is therefore judged over a <b>set</b> of kills: "no leather from all six" for a
 * correct gene is (1/3)^6, about 1 in 700. A set whose control set also dropped none is INCONCLUSIVE (the
 * world is not giving leather - {@code mobDrops} off, or a loot table changed), never a FAIL.
 */
@EventBusSubscriber
final class DebugYardDeath {

    private DebugYardDeath() {
    }

    // ------------------------------------------------------------------
    // Shared state
    // ------------------------------------------------------------------

    /** Bumped by every build, so a clock step left over from the yard before this one stops itself. */
    private static int run;
    /** Every check this row registered, and those it has answered - the deadline sweeps the difference. */
    private static final Set<String> MINE = new LinkedHashSet<>();
    private static final Set<String> ANSWERED = new HashSet<>();
    /** The horses whose drops are this pen's business, and what each left. */
    private static final Set<UUID> WATCHED = new HashSet<>();
    private static final Map<UUID, List<ItemStack>> DROPS = new HashMap<>();

    /** Every check answers by this, INCONCLUSIVE if its own clock never got there. Two minutes. */
    private static final long DEADLINE = 2400L;

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

    /** The entry point: the yard calls this once per build. */
    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        MINE.clear();
        ANSWERED.clear();
        WATCHED.clear();
        DROPS.clear();
        try {
            buildDrops(level, gy, x0, z0, myRun);
            ActionTrace.log("test yard", "row AS west built: " + DROPS_PEN + " (kills at +" + KILL_1 + " and +"
                    + KILL_2 + " ticks, answers by +" + READ_2 + ")");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AS west (" + DROPS_PEN + ") failed to build", e);
        }
        try {
            buildLight(level, gy, x0 + 25, z0, myRun);
            ActionTrace.log("test yard", "row AS east built: " + LIGHT_PEN + " (walks start at +" + LIGHT_START
                    + " ticks)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AS east (" + LIGHT_PEN + ") failed to build", e);
        }
        DebugYardHerd.after(level, DEADLINE, () -> {
            if (myRun != run) {
                return;
            }
            for (String c : MINE) {
                unsure(c, "no answer " + DEADLINE / 20 + " s after build - the pen's own clock never reached it"
                        + " (look for a '[Debug] herd rows: a timed step failed' WARN, or a build WARN for row AS)");
            }
        });
    }

    private static void expect(String check) {
        MINE.add(check);
        DebugYardClockwork.expect(check);
    }

    private static void answer(String check, boolean pass, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }

    private static void unsure(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.inconclusive(check, detail);
        }
    }

    // ==================================================================
    // WEST - DEATH DROPS II
    // ==================================================================

    /*
     * THE QUESTIONS (each quoted from its page's Verification tab, 2026-10-01).
     *
     * wiki/gene-magic-on-death.html - "A mixed pair does nothing. Kill a Lav/Wat horse on the same stone
     *   floor. Pass: no fluid, and the ordinary leather."
     *   Three Lav/Wat kills, each on its own stone pad; 40 ticks later a 5x5x3 box around each death block
     *   is scanned for any block holding a fluid. A Wat/Wat CONTROL dies in the far cell: if it leaves no
     *   water, the handler is not running at all and "no fluid" from Lav/Wat would prove nothing, so the
     *   check is INCONCLUSIVE rather than PASS.
     *   PASS: no fluid in any of the three boxes, every drop is leather, at least one leather across the
     *   three. FAIL: any fluid, or anything but leather. INCONCLUSIVE: control dry, a horse that did not die,
     *   or no leather at all (vanilla's 0-2 rolled 0 three times - 1 in 27).
     *
     * wiki/gene-magic-item-drop.html - "The egg's horse is the same horse, not a horse with the same
     *   alleles. Note an Egg/Egg horse's genetic code and its epigenome code, kill it, and place the egg it
     *   leaves. Pass: both codes on the new horse are identical to the dead one's."
     *   A NAMED (name-tagged) Egg/Egg horse's two codes are read just before the kill; the egg is taken out
     *   of its drop list and hatched through HorseEggSpawner.spawnPreset - the same call
     *   PresetHorseSpawnEggItem.useOn makes, minus the clicked-face arithmetic and the stack shrink - and the
     *   new horse's record is read 20 ticks later, after it has joined and ticked.
     *   PASS: both codes identical. FAIL: no egg in the drop, the egg unreadable, or either code differs
     *   (the detail gives the first differing character). INCONCLUSIVE: the hatch returned no horse.
     *
     *   THE NAME QUESTION (owner to judge). GeneDeathHandler.cloneEgg returns EMPTY when the RECORD has no
     *   name - but every founder is given a registered first and last name at spawn (HorseRecords.newFounder),
     *   so a record without one is only the pre-join sentinel and cannot be killed in a world. What a player
     *   CAN have is a horse with no name tag, so that is what this pen kills: an Egg/Egg horse never
     *   labelled. Its check is named so that PASS means "an untagged Egg/Egg horse still drops its egg" and it
     *   FAILs if it does not; the detail records record.hasName() and the record's display name.
     *
     *   "Two variants together drop the ordinary leather. A Dia/Egg horse. Pass: leather and nothing else -
     *   no diamonds, no egg." Three kills, judged together like the mixed death above
     *   (MagicItemDropGene.expressionOf gives WILD for any pair not homozygous for one variant).
     *
     *   "The sword is rolled per death ... Pass: each sword carries exactly one enchantment, and the two are
     *   not reliably the same one." Two Swd/Swd kills. Only the first half is judged - "not reliably the
     *   same" needs many swords - and the two enchantments are logged for the reader.
     *
     * wiki/gene-magic-meat.html - "The beef is added, not substituted. Kill a Mty/Mty horse. Pass: beef and
     *   the horse's usual leather on the ground." Plus "A carrier drops nothing extra" (Mty/n), "It stacks
     *   with the treasure locus" (Mty/Mty + Dia/Dia: diamonds and beef, no leather) and "Never zero".
     *   Six Mty/Mty, six Mty/n, three Mty/Mty + Dia/Dia. Each meaty kill's beef is compared with the count
     *   MagicMeatGene.abilitiesFor computes - max(1, round(mean of the two copies' "yield"))) - read here
     *   straight off the horse's own epigenome copies (Epigenome.copies + readable, not through the gene),
     *   and the sum is logged beside it so "averaged, not summed" is visible in the numbers.
     *   Added-not-substituted: PASS if the six meaty kills between them dropped leather and every one
     *   dropped beef; FAIL if all six lost their leather while the six carriers kept theirs.
     *
     * HOW LONG: everything answers 15 seconds after build (+300 ticks).
     *
     * LAYOUT: fifteen 4x4 glass cells (2x2 stone floor, stone-brick lid at gy+3 - DebugYardClockwork.cell),
     * five across and three deep, so every pad is four blocks from the next. Cells 0-2 Lav/Wat and cell 14
     * the Wat/Wat control are used once; cells 3-13 hold wave A, are cleared of items, then hold wave B.
     *   wave A (cells 3-13): Egg/Egg named, Egg/Egg untagged, Dia/Egg x3, Swd/Swd x2, Mty/Mty x4
     *   wave B (cells 3-13): Mty/Mty x2, Mty/n x6, Mty/Mty+Dia/Dia x3
     * The horses are spawned untamed, so the emergency stasis bank (which insures an OWNED horse) cannot
     * catch a kill.
     */

    private static final String DROPS_PEN = "DEATH DROPS II";

    static final String C_MIXED_DEATH = DROPS_PEN
            + " - Lav/Wat leaves no fluid and only the ordinary leather (Wat/Wat control places water)";
    static final String C_EGG_CLONE = DROPS_PEN
            + " - Egg/Egg: the egg's horse has the dead horse's genetic code and epigenome code";
    static final String C_EGG_UNTAGGED = DROPS_PEN + " - an Egg/Egg horse with no name tag still drops its egg";
    static final String C_DIA_EGG = DROPS_PEN + " - Dia/Egg drops leather only: no diamond, no egg";
    static final String C_SWORD = DROPS_PEN + " - Swd/Swd: each sword carries exactly one enchantment";
    static final String C_MEAT_ADDED = DROPS_PEN + " - Mty/Mty: beef lands beside the leather, not instead of it";
    static final String C_MEAT_COUNT = DROPS_PEN
            + " - Mty/Mty: beef per kill is max(1, round(mean of the two copies' yield)), never zero";
    static final String C_MEAT_CARRIER = DROPS_PEN + " - Mty/n carrier drops leather only, no beef";
    static final String C_MEAT_DIA = DROPS_PEN + " - Mty/Mty + Dia/Dia drops diamonds and beef, no leather";

    private static final long KILL_1 = 100L;
    private static final long READ_1 = 140L;
    private static final long CLONE_READ = 20L;
    private static final long SPAWN_2 = 180L;
    private static final long KILL_2 = 260L;
    private static final long READ_2 = 300L;

    private static final String ON_DEATH = MagicOnDeathGene.KEY + "=";
    private static final String ITEM_DROP = MagicItemDropGene.KEY + "=";
    private static final String MEAT = MagicMeatGene.KEY + "=";

    /** One horse spawned to be killed, and what was read off it just before. */
    private static final class Kill {
        final String role;
        final int cell;
        final boolean tagged;
        @Nullable Horse horse;
        @Nullable UUID id;
        @Nullable BlockPos at;
        String code = "";
        String epi = "";
        boolean recordNamed;
        String recordName = "";
        boolean died;
        int expectedBeef = -1;
        String yields = "";

        Kill(String role, int cell, boolean tagged) {
            this.role = role;
            this.cell = cell;
            this.tagged = tagged;
        }

        String who() {
            return role + " (cell " + cell + ")";
        }
    }

    private static int cellX(int x0, int idx) {
        return x0 + 4 * (idx % 5);
    }

    private static int cellZ(int z0, int idx) {
        return z0 + 4 * (idx / 5);
    }

    private static void buildDrops(ServerLevel level, int gy, int x0, int z0, int myRun) {
        for (String c : List.of(C_MIXED_DEATH, C_EGG_CLONE, C_EGG_UNTAGGED, C_DIA_EGG, C_SWORD, C_MEAT_ADDED,
                C_MEAT_COUNT, C_MEAT_CARRIER, C_MEAT_DIA)) {
            expect(c);
        }
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = x0; x <= x0 + 19; x++) {
            for (int z = z0; z <= z0 + 12; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
        for (int idx = 0; idx < 15; idx++) {
            int cx = cellX(x0, idx);
            int cz = cellZ(z0, idx);
            DebugYardClockwork.cell(level, gy, cx, cx + 3, cz, cz + 3);
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DEATH DROPS II", "kills itself at", "+5 s: fluid, egg", "clone, beef, sword"));

        List<Kill> mixed = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mixed.add(spawn(level, gy, x0, z0, new Kill("Lav/Wat " + (i + 1), i, true), ON_DEATH + "Lav/Wat"));
        }
        Kill control = spawn(level, gy, x0, z0, new Kill("Wat/Wat control", 14, true), ON_DEATH + "Wat/Wat");

        Kill eggNamed = spawn(level, gy, x0, z0, new Kill("Egg/Egg named", 3, true), ITEM_DROP + "Egg/Egg");
        Kill eggBare = spawn(level, gy, x0, z0, new Kill("Egg/Egg untagged", 4, false), ITEM_DROP + "Egg/Egg");
        List<Kill> diaEgg = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            diaEgg.add(spawn(level, gy, x0, z0, new Kill("Dia/Egg " + (i + 1), 5 + i, true), ITEM_DROP + "Dia/Egg"));
        }
        List<Kill> swords = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            swords.add(spawn(level, gy, x0, z0, new Kill("Swd/Swd " + (i + 1), 8 + i, true), ITEM_DROP + "Swd/Swd"));
        }
        List<Kill> meaty = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            meaty.add(spawn(level, gy, x0, z0, new Kill("Mty/Mty " + (i + 1), 10 + i, true), MEAT + "Mty/Mty"));
        }

        List<Kill> waveOne = new ArrayList<>(mixed);
        waveOne.add(control);
        waveOne.add(eggNamed);
        waveOne.add(eggBare);
        waveOne.addAll(diaEgg);
        waveOne.addAll(swords);
        waveOne.addAll(meaty);

        List<Kill> carriers = new ArrayList<>();
        List<Kill> meatyDia = new ArrayList<>();

        DebugYardHerd.after(level, KILL_1, () -> {
            if (myRun != run) {
                return;
            }
            for (Kill k : waveOne) {
                kill(level, k);
            }
        });
        DebugYardHerd.after(level, READ_1, () -> {
            if (myRun != run) {
                return;
            }
            judgeMixedDeath(level, mixed, control);
            judgeEggUntagged(eggBare);
            judgeDiaEgg(diaEgg);
            judgeSwords(swords);
            for (int idx = 3; idx <= 13; idx++) {
                clearItems(level, gy, x0, z0, idx);
            }
            hatch(level, gy, x0, z0, eggNamed, myRun);
        });
        DebugYardHerd.after(level, SPAWN_2, () -> {
            if (myRun != run) {
                return;
            }
            for (int i = 0; i < 2; i++) {
                meaty.add(spawn(level, gy, x0, z0, new Kill("Mty/Mty " + (5 + i), 3 + i, true), MEAT + "Mty/Mty"));
            }
            for (int i = 0; i < 6; i++) {
                carriers.add(spawn(level, gy, x0, z0, new Kill("Mty/n " + (i + 1), 5 + i, true), MEAT + "Mty/n"));
            }
            for (int i = 0; i < 3; i++) {
                meatyDia.add(spawn(level, gy, x0, z0, new Kill("Mty/Mty+Dia/Dia " + (i + 1), 11 + i, true),
                        MEAT + "Mty/Mty-" + ITEM_DROP + "Dia/Dia"));
            }
        });
        DebugYardHerd.after(level, KILL_2, () -> {
            if (myRun != run) {
                return;
            }
            for (Kill k : meaty.subList(4, meaty.size())) {
                kill(level, k);
            }
            for (Kill k : carriers) {
                kill(level, k);
            }
            for (Kill k : meatyDia) {
                kill(level, k);
            }
        });
        DebugYardHerd.after(level, READ_2, () -> {
            if (myRun != run) {
                return;
            }
            judgeMeat(meaty, carriers, meatyDia);
            for (int idx = 3; idx <= 13; idx++) {
                clearItems(level, gy, x0, z0, idx);
            }
        });
    }

    private static Kill spawn(ServerLevel level, int gy, int x0, int z0, Kill k, String code) {
        double x = cellX(x0, k.cell) + 2.0;
        double z = cellZ(z0, k.cell) + 2.0;
        Sex sex = k.cell % 2 == 0 ? Sex.FEMALE : Sex.MALE;
        Horse h = k.tagged
                ? DebugYardUnattended.horse(level, gy, x, z, sex, code, false, "DEATH " + k.role)
                : DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, false);
        k.horse = h;
        if (h != null) {
            k.id = h.getUUID();
            WATCHED.add(k.id);
        }
        return k;
    }

    /** Read the horse's codes, then kill it with a damage source that is neither fire nor lava. */
    private static void kill(ServerLevel level, Kill k) {
        Horse h = k.horse;
        if (h == null || !h.isAlive() || !HorseRecords.hasRealRecord(h)) {
            return;     // k.died stays false; the judge says which
        }
        HorseRecord r = HorseRecords.of(h);
        k.code = r.geneticCode();
        k.epi = r.epigenomeCode();
        k.recordNamed = r.hasName();
        k.recordName = r.displayName();
        k.at = h.blockPosition();
        Gene meat = Genes.byKeyOrNull(MagicMeatGene.KEY);
        if (meat != null) {
            Epigenome.Copies copies = r.epigenome().copies(meat);
            double a = Epigenome.readable(meat, copies.first()).get(MagicMeatGene.YIELD);
            double b = Epigenome.readable(meat, copies.second()).get(MagicMeatGene.YIELD);
            k.expectedBeef = Math.max(1, (int) Math.round((a + b) / 2.0));
            k.yields = String.format(Locale.ROOT, "yields %.2f + %.2f: mean %.2f, expect %d (a sum would be %d)",
                    a, b, (a + b) / 2.0, k.expectedBeef, Math.round(a + b));
        }
        // The same call LycanthropyHandler makes to finish a horse. genericKill is not fire, so nothing is cooked.
        h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        k.died = h.isDeadOrDying();
    }

    /** Null when every kill in the set died and was seen dropping; otherwise what went wrong, for INCONCLUSIVE. */
    private static @Nullable String unsettled(List<Kill> kills) {
        for (Kill k : kills) {
            if (k.horse == null) {
                return k.who() + " was never spawned";
            }
            if (!k.died) {
                return k.who() + " did not die to genericKill (alive " + k.horse.isAlive() + ", record "
                        + HorseRecords.hasRealRecord(k.horse) + ")";
            }
            if (k.id == null || DROPS.get(k.id) == null) {
                return k.who() + " died but no LivingDropsEvent for it reached the pen (cancelled upstream?)";
            }
        }
        return null;
    }

    private static List<ItemStack> drops(Kill k) {
        List<ItemStack> d = k.id == null ? null : DROPS.get(k.id);
        return d == null ? List.of() : d;
    }

    private static int count(List<ItemStack> drops, Item item) {
        int n = 0;
        for (ItemStack s : drops) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Stacks that are none of {@code allowed}. */
    private static List<ItemStack> others(List<ItemStack> drops, Item... allowed) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : drops) {
            boolean ok = false;
            for (Item a : allowed) {
                ok |= s.is(a);
            }
            if (!ok) {
                out.add(s);
            }
        }
        return out;
    }

    private static String describe(List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return "nothing";
        }
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : drops) {
            sb.append(sb.length() == 0 ? "" : ", ").append(s.getCount()).append("x ")
                    .append(BuiltInRegistries.ITEM.getKey(s.getItem()));
            if (PresetHorseSpawnEggItem.genomeOf(s) != null) {
                sb.append(" (holding a genome)");
            }
        }
        return sb.toString();
    }

    private static String eachDropped(List<Kill> kills) {
        StringBuilder sb = new StringBuilder();
        for (Kill k : kills) {
            sb.append(sb.length() == 0 ? "" : "; ").append(k.role).append(": ").append(describe(drops(k)));
        }
        return sb.toString();
    }

    private static ItemStack firstEgg(List<ItemStack> drops) {
        for (ItemStack s : drops) {
            if (PresetHorseSpawnEggItem.genomeOf(s) != null) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void clearItems(ServerLevel level, int gy, int x0, int z0, int idx) {
        int cx = cellX(x0, idx);
        int cz = cellZ(z0, idx);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class,
                DebugTestYard.box(cx, gy, cz, cx + 4, gy + 4, cz + 4))) {
            e.discard();
        }
    }

    // ------------------------------------------------------------------
    // The judges
    // ------------------------------------------------------------------

    /** Every block holding any fluid in the 5x5x3 box (feet level and two up) around a death. */
    private static List<String> fluidsAround(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = at.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getFluidState().getType() != Fluids.EMPTY) {
                        out.add((s.is(Blocks.LAVA) ? "lava" : s.is(Blocks.WATER) ? "water" : "fluid in "
                                + BuiltInRegistries.BLOCK.getKey(s.getBlock())) + " at " + p.toShortString());
                    }
                }
            }
        }
        return out;
    }

    private static void judgeMixedDeath(ServerLevel level, List<Kill> mixed, Kill control) {
        List<Kill> all = new ArrayList<>(mixed);
        all.add(control);
        String bad = unsettled(all);
        if (bad != null) {
            unsure(C_MIXED_DEATH, bad);
            return;
        }
        List<String> controlFluid = fluidsAround(level, control.at);
        boolean controlWater = false;
        for (String f : controlFluid) {
            controlWater |= f.startsWith("water");
        }
        StringBuilder each = new StringBuilder();
        boolean fluid = false;
        int leather = 0;
        List<ItemStack> wrong = new ArrayList<>();
        for (Kill k : mixed) {
            List<String> f = fluidsAround(level, k.at);
            fluid |= !f.isEmpty();
            List<ItemStack> d = drops(k);
            leather += count(d, Items.LEATHER);
            wrong.addAll(others(d, Items.LEATHER));
            each.append(each.length() == 0 ? "" : "; ").append(k.role).append(" died at ").append(k.at.toShortString())
                    .append(": ").append(f.isEmpty() ? "no fluid" : f.size() + " fluid block(s) " + f)
                    .append(", dropped ").append(describe(d));
        }
        String detail = each + " | control Wat/Wat: " + (controlWater ? controlFluid.size() + " fluid block(s), water"
                + " present" : "NO water " + controlFluid) + ", dropped " + describe(drops(control))
                + " | leather across the three: " + leather;
        if (!controlWater) {
            unsure(C_MIXED_DEATH, detail + " - the Wat/Wat control left no water, so the on_death handler is not"
                    + " running here and 'no fluid' proves nothing");
        } else if (fluid) {
            answer(C_MIXED_DEATH, false, detail + " - a mixed pair placed fluid: the locus picked a winner");
        } else if (!wrong.isEmpty()) {
            answer(C_MIXED_DEATH, false, detail + " - something other than leather dropped: " + describe(wrong));
        } else if (leather == 0) {
            unsure(C_MIXED_DEATH, detail + " - no fluid and nothing wrong, but no leather either (vanilla's 0-2 rolled"
                    + " 0 three times, 1 in 27, or the world drops no mob loot)");
        } else {
            answer(C_MIXED_DEATH, true, detail);
        }
    }

    private static void judgeEggUntagged(Kill k) {
        String bad = unsettled(List.of(k));
        if (bad != null) {
            unsure(C_EGG_UNTAGGED, bad);
            return;
        }
        List<ItemStack> d = drops(k);
        ItemStack egg = firstEgg(d);
        String detail = "no custom name on the horse; its record hasName " + k.recordNamed + " (\"" + k.recordName
                + "\"); dropped " + describe(d);
        if (egg.isEmpty()) {
            answer(C_EGG_UNTAGGED, false, detail + " - no egg (GeneDeathHandler.cloneEgg returns EMPTY for a record"
                    + " without a name; an owner's call whether an untagged horse should count as that)");
        } else {
            answer(C_EGG_UNTAGGED, true, detail);
        }
    }

    private static void judgeDiaEgg(List<Kill> kills) {
        String bad = unsettled(kills);
        if (bad != null) {
            unsure(C_DIA_EGG, bad);
            return;
        }
        int leather = 0;
        int diamonds = 0;
        int eggs = 0;
        List<ItemStack> wrong = new ArrayList<>();
        for (Kill k : kills) {
            List<ItemStack> d = drops(k);
            leather += count(d, Items.LEATHER);
            diamonds += count(d, Items.DIAMOND);
            for (ItemStack s : d) {
                if (PresetHorseSpawnEggItem.genomeOf(s) != null) {
                    eggs += s.getCount();
                }
            }
            wrong.addAll(others(d, Items.LEATHER));
        }
        String detail = eachDropped(kills) + " | totals: leather " + leather + ", diamonds " + diamonds + ", eggs "
                + eggs;
        if (diamonds > 0 || eggs > 0) {
            answer(C_DIA_EGG, false, detail + " - a compound heterozygote expressed a variant");
        } else if (!wrong.isEmpty()) {
            answer(C_DIA_EGG, false, detail + " - something other than leather dropped: " + describe(wrong));
        } else if (leather == 0) {
            unsure(C_DIA_EGG, detail + " - nothing at all dropped (0-2 leather rolled 0 three times, 1 in 27)");
        } else {
            answer(C_DIA_EGG, true, detail);
        }
    }

    private static void judgeSwords(List<Kill> kills) {
        String bad = unsettled(kills);
        if (bad != null) {
            unsure(C_SWORD, bad);
            return;
        }
        boolean ok = true;
        StringBuilder each = new StringBuilder();
        for (Kill k : kills) {
            List<ItemStack> d = drops(k);
            int swords = count(d, Items.IRON_SWORD);
            each.append(each.length() == 0 ? "" : "; ").append(k.role).append(": ").append(describe(d));
            if (swords != 1) {
                ok = false;
                each.append(" [").append(swords).append(" iron swords, want 1]");
                continue;
            }
            for (ItemStack s : d) {
                if (!s.is(Items.IRON_SWORD)) {
                    continue;
                }
                // UNVERIFIED: no caller in this repo of ItemStack.getEnchantments / ItemEnchantments.keySet,
                // getLevel or Holder.getRegisteredName - all four read from the 26.1.2 sources jar.
                ItemEnchantments ench = s.getEnchantments();
                each.append(" [").append(ench.size()).append(" enchantment(s):");
                for (Holder<Enchantment> e : ench.keySet()) {
                    each.append(' ').append(e.getRegisteredName()).append(' ').append(ench.getLevel(e));
                }
                each.append(']');
                ok &= ench.size() == 1;
            }
        }
        answer(C_SWORD, ok, each + (ok ? "" : " - a sword without exactly one enchantment")
                + " (whether two swords differ is not judged on two; read the names)");
    }

    /** The named Egg/Egg: take the egg out of its drop, hatch it the item's way, read the new horse. */
    private static void hatch(ServerLevel level, int gy, int x0, int z0, Kill k, int myRun) {
        String bad = unsettled(List.of(k));
        if (bad != null) {
            unsure(C_EGG_CLONE, bad);
            return;
        }
        List<ItemStack> d = drops(k);
        ItemStack egg = firstEgg(d);
        if (egg.isEmpty()) {
            answer(C_EGG_CLONE, false, "the named Egg/Egg horse dropped " + describe(d) + " - no egg to hatch");
            return;
        }
        StoredGenome stored = PresetHorseSpawnEggItem.genomeOf(egg);
        String inEgg = "in the egg: genetic code " + diff(k.code, stored.genotypeCode()) + ", epigenome "
                + (stored.epigenomeCode().isEmpty() ? "EMPTY (spawnPreset would roll a fresh one)"
                : diff(k.epi, stored.epigenomeCode()));
        Horse clone;
        try {
            clone = HorseEggSpawner.spawnPreset(level,
                    new Vec3(cellX(x0, k.cell) + 2.0, gy + 1, cellZ(z0, k.cell) + 2.0), 0.0F, stored, false);
        } catch (RuntimeException e) {
            answer(C_EGG_CLONE, false, inEgg + " | the egg would not hatch: " + e);
            return;
        }
        if (clone == null) {
            unsure(C_EGG_CLONE, inEgg + " | HorseEggSpawner.spawnPreset returned no horse");
            return;
        }
        Horse born = clone;
        DebugYardHerd.after(level, CLONE_READ, () -> {
            if (myRun != run) {
                return;
            }
            if (!born.isAlive() || !HorseRecords.hasRealRecord(born)) {
                unsure(C_EGG_CLONE, inEgg + " | the hatched horse was gone or had no record " + CLONE_READ
                        + " ticks later");
                born.discard();
                return;
            }
            HorseRecord r = HorseRecords.of(born);
            boolean sameCode = k.code.equals(r.geneticCode());
            boolean sameEpi = k.epi.equals(r.epigenomeCode());
            String detail = "dead \"" + k.recordName + "\" vs hatched \"" + r.displayName() + "\" " + CLONE_READ
                    + " ticks after hatching: genetic code " + diff(k.code, r.geneticCode()) + ", epigenome code "
                    + diff(k.epi, r.epigenomeCode()) + " | " + inEgg;
            answer(C_EGG_CLONE, sameCode && sameEpi, detail
                    + (sameCode && sameEpi ? "" : " - the egg's horse is not the dead horse"));
            born.discard();
        });
    }

    private static String diff(String a, String b) {
        if (a.equals(b)) {
            return "identical (" + a.length() + " chars)";
        }
        int i = 0;
        while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return "DIFFERS at char " + i + " (lengths " + a.length() + "/" + b.length() + "): dead '" + snip(a, i)
                + "' vs '" + snip(b, i) + "'";
    }

    private static String snip(String s, int i) {
        return s.substring(Math.max(0, i - 12), Math.min(s.length(), i + 24));
    }

    private static void judgeMeat(List<Kill> meaty, List<Kill> carriers, List<Kill> meatyDia) {
        // Carriers first: their leather is the control the meaty set's leather is read against.
        int carrierLeather = 0;
        String carrierBad = unsettled(carriers);
        if (carrierBad != null) {
            unsure(C_MEAT_CARRIER, carrierBad);
        } else {
            int beef = 0;
            List<ItemStack> wrong = new ArrayList<>();
            for (Kill k : carriers) {
                List<ItemStack> d = drops(k);
                carrierLeather += count(d, Items.LEATHER);
                beef += count(d, Items.BEEF);
                wrong.addAll(others(d, Items.LEATHER, Items.BEEF));
            }
            String detail = eachDropped(carriers) + " | totals: leather " + carrierLeather + ", beef " + beef;
            if (beef > 0) {
                answer(C_MEAT_CARRIER, false, detail + " - a carrier dropped beef: the locus is not recessive");
            } else if (!wrong.isEmpty()) {
                answer(C_MEAT_CARRIER, false, detail + " - something other than leather dropped: " + describe(wrong));
            } else if (carrierLeather == 0) {
                unsure(C_MEAT_CARRIER, detail + " - no leather from six kills (1 in 700 for vanilla's 0-2): the world"
                        + " is not dropping mob loot");
            } else {
                answer(C_MEAT_CARRIER, true, detail);
            }
        }

        String meatyBad = unsettled(meaty);
        if (meatyBad != null) {
            unsure(C_MEAT_ADDED, meatyBad);
            unsure(C_MEAT_COUNT, meatyBad);
        } else {
            int leather = 0;
            int noBeef = 0;
            boolean countsOk = true;
            List<ItemStack> wrong = new ArrayList<>();
            StringBuilder each = new StringBuilder();
            for (Kill k : meaty) {
                List<ItemStack> d = drops(k);
                int beef = count(d, Items.BEEF);
                leather += count(d, Items.LEATHER);
                wrong.addAll(others(d, Items.LEATHER, Items.BEEF));
                if (beef == 0) {
                    noBeef++;
                }
                boolean match = k.expectedBeef >= 1 && beef == k.expectedBeef;
                countsOk &= match;
                each.append(each.length() == 0 ? "" : "; ").append(k.role).append(": ").append(describe(d))
                        .append(" [").append(k.yields.isEmpty() ? "yield unread" : k.yields)
                        .append(match ? "" : " - MISMATCH").append(']');
            }
            String added = each + " | leather across the six " + leather + "; carriers' leather " + carrierLeather;
            if (noBeef > 0) {
                answer(C_MEAT_ADDED, false, added + " - " + noBeef + " meaty kill(s) dropped no beef at all");
            } else if (!wrong.isEmpty()) {
                answer(C_MEAT_ADDED, false, added + " - something other than leather and beef: " + describe(wrong));
            } else if (leather == 0 && carrierLeather > 0) {
                answer(C_MEAT_ADDED, false, added + " - all six lost their leather while the carriers kept theirs: the"
                        + " meat took the replace branch");
            } else if (leather == 0) {
                unsure(C_MEAT_ADDED, added + " - no leather from the meaty set OR the carriers: the world is not"
                        + " dropping mob loot, so 'beside' cannot be seen");
            } else {
                answer(C_MEAT_ADDED, true, added);
            }
            answer(C_MEAT_COUNT, countsOk, each + (countsOk ? "" : " - a kill's beef is not the averaged yield"));
        }

        String diaBad = unsettled(meatyDia);
        if (diaBad != null) {
            unsure(C_MEAT_DIA, diaBad);
        } else {
            boolean ok = true;
            StringBuilder each = new StringBuilder();
            for (Kill k : meatyDia) {
                List<ItemStack> d = drops(k);
                int diamonds = count(d, Items.DIAMOND);
                int beef = count(d, Items.BEEF);
                int leather = count(d, Items.LEATHER);
                List<ItemStack> wrong = others(d, Items.DIAMOND, Items.BEEF);
                boolean one = diamonds >= MagicItemDropGene.DIAMOND_MIN && diamonds <= MagicItemDropGene.DIAMOND_MAX
                        && beef == k.expectedBeef && leather == 0 && wrong.isEmpty();
                ok &= one;
                each.append(each.length() == 0 ? "" : "; ").append(k.role).append(": ").append(describe(d))
                        .append(" [want ").append(MagicItemDropGene.DIAMOND_MIN).append('-')
                        .append(MagicItemDropGene.DIAMOND_MAX).append(" diamonds, ").append(k.expectedBeef)
                        .append(" beef, no leather").append(one ? "" : " - WRONG").append(']');
            }
            answer(C_MEAT_DIA, ok, each + (ok ? "" : " - the diamonds did not replace the leather, or the beef"
                    + " did not stack on top"));
        }
    }

    // ==================================================================
    // EAST - SHADOW LIGHT
    // ==================================================================

    /*
     * THE QUESTIONS, wiki/gene-shadowcreature.html, Verification tab:
     *   "One light block, following the horse, and only one. ... Pass: exactly one minecraft:light at level
     *    5 near the horse and none along the path it walked. A trail of light blocks is the failure, and it
     *    is permanent."
     *   "The light is taken back when the horse is gone. Kill it ... Pass: no minecraft:light left anywhere
     *    it stood."
     *   "The light never eats a block somebody placed. ... Pass: nothing of yours is replaced or destroyed."
     * and wiki/gene-suntouched.html, the glow verb's own record: "Still unchecked: the light cleaning up on
     * death or unload" - Suntouched's glow is light 12 with no condition (genes/suntouched.json), so it
     * gets the second corridor and the same four checks.
     *
     * HOW. Each corridor is a sealed glass tube (walls gy+1..gy+3, lid gy+4), interior three wide and
     * eighteen long, floored in stone with glowstone set into it every third block - the yard's own lamps
     * are minecraft:light blocks at gy+3, so every light block in the whole east half is REMOVED at build,
     * and the corridor is counted at t0 (+20 ticks), before its horse exists: that count must be 0 or all
     * four checks are INCONCLUSIVE, since the yard's lamps would then be in the tally. Then one horse is
     * spawned at the west end and teleported one block east every 15 ticks, sixteen steps.
     * GeneAbilityHandler.reconcileGlow runs on EVERY horse tick (EntityTickEvent.Post), so 15 ticks is
     * fifteen chances to settle. Just before each move the corridor's light blocks are counted.
     *
     * The torch, chest and glass. glowTarget only ever looks at the horse's own column - its body block, the
     * one above, then its feet - and takes only air or an existing light. So the blocks are put ON the
     * path, where they become candidates: a torch the horse is stepped into (its body and feet block are
     * the torch; the light must go above it), a chest it is stood on (its feet block is the chest), and a
     * glass block it is stood on. Glass cannot be put in the body or the block above without the horse
     * standing inside it, so it is the one of the three that only guards against a light placed anywhere
     * but the column; it is there because a light that ate a solid block is the worst version of the bug.
     *
     * PASS / FAIL, per horse:
     *   - never more than one light in the corridor at any step (FAIL: two or more - a trail);
     *   - at every step exactly one, at the gene's level (5 for Shc, 12 for Sntch), within 2 blocks
     *     (Chebyshev, from the feet block) of the horse (FAIL: none, two, the wrong level, or too far);
     *   - 40 ticks after the kill (genericKill), no light anywhere in the corridor (FAIL: any left). The
     *     dead horse is removed at deathTime 20 and EntityLeaveLevelEvent queues the clear for the next
     *     server tick; if the body is somehow still there at 40 it waits 60 more and says so;
     *   - the torch, chest and glass are what they were, at every step and at the end.
     *   A horse that never lit at all makes the "one" and "gone" checks INCONCLUSIVE (nothing to trail or
     *   take back) and the "exactly one" check a FAIL.
     *
     * HOW LONG: t0 at +20 ticks, sixteen steps of 15, the kill at about +260, the last answer at about +300
     * ticks (15 seconds), +360 if the body lingers.
     */

    private static final String LIGHT_PEN = "SHADOW LIGHT";
    private static final String SUNTOUCHED_KEY = "horsegenetics.suntouched";
    /** suntouched.json's glow "light": 12. Read from the file, not from a constant - the gene is JSON. */
    private static final int SUNTOUCHED_LIGHT = 12;

    private static final long LIGHT_START = 20L;
    private static final long STEP = 15L;
    private static final long AFTER_DEATH = 40L;
    private static final int PATH_STEPS = 16;

    private static final class Walk {
        final String tokens;
        final String code;
        final int light;
        final int zc;
        final int ex0;
        final int ex1;
        final int gy;
        final String cOne;
        final String cNear;
        final String cGone;
        final String cIntact;
        final List<BlockPos> path = new ArrayList<>();
        final List<Double> pathY = new ArrayList<>();
        final Map<BlockPos, Block> placed = new LinkedHashMap<>();
        @Nullable Horse horse;
        int steps;
        int stepsRight;
        int maxLights;
        int everLit;
        @Nullable String firstTrail;
        final List<String> off = new ArrayList<>();
        int offCount;
        @Nullable String broken;

        Walk(String tokens, String code, int light, int gy, int ex0, int ex1, int zc) {
            this.tokens = tokens;
            this.code = code;
            this.light = light;
            this.gy = gy;
            this.ex0 = ex0;
            this.ex1 = ex1;
            this.zc = zc;
            String p = LIGHT_PEN + " - " + tokens;
            this.cOne = p + ": never more than one minecraft:light in its corridor while it walks";
            this.cNear = p + ": exactly one light, at level " + light + ", within 2 blocks of the horse at every step";
            this.cGone = p + ": no light left in its corridor " + AFTER_DEATH + " ticks after it dies";
            this.cIntact = p + ": the torch, chest and glass on its path are never replaced";
        }

        List<String> checks() {
            return List.of(cOne, cNear, cGone, cIntact);
        }
    }

    private static void buildLight(ServerLevel level, int gy, int ex0, int z0, int myRun) {
        int ex1 = ex0 + 19;
        List<Walk> walks = new ArrayList<>();
        walks.add(new Walk("Shc/Shc", ShadowcreatureGene.KEY + "=Shc/Shc", ShadowcreatureGene.LIGHT_LEVEL,
                gy, ex0, ex1, z0 + 3));
        if (Genes.byKeyOrNull(SUNTOUCHED_KEY) != null) {
            walks.add(new Walk("Sntch/Sntch (Suntouched)", SUNTOUCHED_KEY + "=Sntch/Sntch", SUNTOUCHED_LIGHT,
                    gy, ex0, ex1, z0 + 9));
        } else {
            ActionTrace.log("test yard", LIGHT_PEN + ": no " + SUNTOUCHED_KEY + " gene loaded - its corridor is"
                    + " built empty and asks nothing");
        }
        for (Walk w : walks) {
            for (String c : w.checks()) {
                expect(c);
            }
        }

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = ex0; x <= ex1; x++) {
            for (int z = z0; z <= z0 + 12; z++) {
                // Light from the floor, as DebugYardLong.lightFromTheFloor does, because the lamps come out.
                boolean lamp = Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0;
                DebugPenManager.groundColumn(level, x, gy, z, lamp ? glow : stone);
                for (int y = gy + 1; y <= gy + 8; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (level.getBlockState(at).is(Blocks.LIGHT)) {
                        DebugPenManager.fastSet(level, at, air);
                    }
                }
            }
        }
        corridor(level, gy, ex0, ex1, z0 + 3);
        corridor(level, gy, ex0, ex1, z0 + 9);
        DebugPenManager.placeSign(level, new BlockPos(ex0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("SHADOW LIGHT", "Shc and Sntch", "walked: one light,", "none left behind"));

        for (Walk w : walks) {
            for (int i = 0; i < PATH_STEPS; i++) {
                int x = ex0 + 2 + i;
                w.path.add(new BlockPos(x, gy + 1, w.zc));
                w.pathY.add((double) (gy + 1));
            }
            // On the path, so each is a glowTarget candidate when the horse is there (see the note above).
            obstacle(level, w, 4, Blocks.TORCH, gy + 1.0);
            obstacle(level, w, 8, Blocks.CHEST, gy + 2.0);
            obstacle(level, w, 12, Blocks.GLASS, gy + 2.0);
            DebugYardHerd.after(level, LIGHT_START, () -> {
                if (myRun == run) {
                    startWalk(level, w, myRun);
                }
            });
        }
    }

    private static void corridor(ServerLevel level, int gy, int ex0, int ex1, int zc) {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = ex0; x <= ex1; x++) {
            for (int z = zc - 2; z <= zc + 2; z++) {
                boolean wall = x == ex0 || x == ex1 || z == zc - 2 || z == zc + 2;
                for (int y = gy + 1; y <= gy + 3; y++) {
                    level.setBlock(new BlockPos(x, y, z), wall ? glass : air, 3);
                }
                level.setBlock(new BlockPos(x, gy + 4, z), glass, 3);
            }
        }
    }

    /** A block at path step {@code i}, and the height the horse is put at when it gets there. */
    private static void obstacle(ServerLevel level, Walk w, int i, Block block, double standY) {
        BlockPos at = w.path.get(i);
        level.setBlock(at, block.defaultBlockState(), 3);
        w.placed.put(at, block);
        w.pathY.set(i, standY);
    }

    private static List<BlockPos> lightsIn(ServerLevel level, Walk w) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = w.ex0 + 1; x <= w.ex1 - 1; x++) {
            for (int z = w.zc - 1; z <= w.zc + 1; z++) {
                for (int y = w.gy + 1; y <= w.gy + 3; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(Blocks.LIGHT)) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    private static void startWalk(ServerLevel level, Walk w, int myRun) {
        List<BlockPos> t0 = lightsIn(level, w);
        if (!t0.isEmpty()) {
            for (String c : w.checks()) {
                unsure(c, t0.size() + " light block(s) in the corridor at t0, before the horse exists ("
                        + shortList(t0) + ") - the yard's own lamps or another source would be counted");
            }
            return;
        }
        BlockPos start = w.path.get(0);
        Horse h = DebugYardUnattended.horse(level, w.gy, start.getX() + 0.5, start.getZ() + 0.5, Sex.MALE, w.code,
                false, "LIGHT " + w.tokens);
        if (h == null) {
            for (String c : w.checks()) {
                unsure(c, "the " + w.tokens + " horse could not be spawned");
            }
            return;
        }
        w.horse = h;
        ActionTrace.log("test yard", LIGHT_PEN + " " + w.tokens + ": t0 0 lights in the corridor; horse spawned at "
                + start.toShortString() + ", " + PATH_STEPS + " steps of " + STEP + " ticks");
        DebugYardHerd.after(level, STEP, () -> step(level, w, 1, myRun));
    }

    private static void step(ServerLevel level, Walk w, int i, int myRun) {
        if (myRun != run) {
            return;
        }
        Horse h = w.horse;
        if (h == null || !h.isAlive()) {
            for (String c : w.checks()) {
                unsure(c, "the horse was gone or dead at step " + i + " of " + PATH_STEPS + ", before the pen killed"
                        + " it (" + w.steps + " steps observed, max " + w.maxLights + " lights)");
            }
            return;
        }
        observe(level, w, i - 1, h);
        if (i < PATH_STEPS) {
            BlockPos next = w.path.get(i);
            h.getNavigation().stop();
            // UNVERIFIED: snapTo on a horse already in the level, as a teleport. The repo calls it on entities
            // in the world (CowboyHandler.snapTo(paddock, ...)); Entity.teleportTo(x, y, z) is snapTo on the
            // server in the 26.1.2 sources.
            h.snapTo(next.getX() + 0.5, w.pathY.get(i), next.getZ() + 0.5, h.getYRot(), 0.0F);
            DebugYardHerd.after(level, STEP, () -> step(level, w, i + 1, myRun));
            return;
        }
        h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        boolean died = h.isDeadOrDying();
        DebugYardHerd.after(level, AFTER_DEATH, () -> finish(level, w, h, died, 0, myRun));
    }

    private static void observe(ServerLevel level, Walk w, int at, Horse h) {
        List<BlockPos> lights = lightsIn(level, w);
        BlockPos feet = h.blockPosition();
        w.steps++;
        w.maxLights = Math.max(w.maxLights, lights.size());
        if (!lights.isEmpty()) {
            w.everLit++;
        }
        if (lights.size() > 1 && w.firstTrail == null) {
            w.firstTrail = "step " + at + " (horse at " + feet.toShortString() + "): " + lights.size() + " lights "
                    + shortList(lights);
        }
        String wrong = null;
        if (lights.size() != 1) {
            wrong = lights.size() + " lights";
        } else {
            BlockPos l = lights.get(0);
            int lvl = level.getBlockState(l).getValue(LightBlock.LEVEL);
            int d = Math.max(Math.abs(l.getX() - feet.getX()),
                    Math.max(Math.abs(l.getY() - feet.getY()), Math.abs(l.getZ() - feet.getZ())));
            if (lvl != w.light || d > 2) {
                wrong = "light at " + l.toShortString() + " level " + lvl + ", " + d + " blocks off";
            }
        }
        if (wrong == null) {
            w.stepsRight++;
        } else {
            w.offCount++;
            if (w.off.size() < 4) {
                w.off.add("step " + at + " (horse at " + feet.toShortString() + "): " + wrong);
            }
        }
        if (w.broken == null) {
            w.broken = brokenBlock(level, w, "at step " + at);
        }
    }

    private static @Nullable String brokenBlock(ServerLevel level, Walk w, String when) {
        for (Map.Entry<BlockPos, Block> e : w.placed.entrySet()) {
            BlockState s = level.getBlockState(e.getKey());
            if (!s.is(e.getValue())) {
                return BuiltInRegistries.BLOCK.getKey(e.getValue()) + " at " + e.getKey().toShortString() + " is "
                        + BuiltInRegistries.BLOCK.getKey(s.getBlock()) + " " + when;
            }
        }
        return null;
    }

    private static void finish(ServerLevel level, Walk w, Horse h, boolean died, int attempt, int myRun) {
        if (myRun != run) {
            return;
        }
        if (!h.isRemoved() && attempt == 0) {
            // Still in the level 40 ticks after a kill: let the death run its course before judging the clear.
            DebugYardHerd.after(level, 60L, () -> finish(level, w, h, died, 1, myRun));
            return;
        }
        List<BlockPos> left = lightsIn(level, w);
        String walked = w.steps + " steps observed, " + w.stepsRight + " with exactly one light at level " + w.light
                + " within 2 blocks, at most " + w.maxLights + " light(s) at once, lit at " + w.everLit + " step(s)";
        String placed = w.broken != null ? w.broken : brokenBlock(level, w, "after the death");

        if (w.maxLights == 0) {
            unsure(w.cOne, walked + " - the horse never lit, so there was nothing to trail");
        } else {
            answer(w.cOne, w.maxLights <= 1, walked + (w.firstTrail == null ? "" : " | first trail: " + w.firstTrail));
        }
        answer(w.cNear, w.offCount == 0 && w.stepsRight == w.steps && w.steps > 0, walked
                + (w.offCount == 0 ? "" : " | " + w.offCount + " step(s) off, first: " + w.off));
        String gone = (attempt == 0 ? AFTER_DEATH : AFTER_DEATH + 60) + " ticks after the kill (died " + died
                + ", body removed " + h.isRemoved() + "): " + left.size() + " light(s) in the corridor"
                + (left.isEmpty() ? "" : " " + shortList(left));
        if (!died) {
            unsure(w.cGone, gone + " - genericKill did not kill it, so 'gone' was never tested");
        } else if (w.maxLights == 0) {
            unsure(w.cGone, gone + " - it never lit, so there was nothing to take back");
        } else {
            answer(w.cGone, left.isEmpty(), gone);
        }
        answer(w.cIntact, placed == null, placed == null
                ? "torch, chest and glass all intact through " + w.steps + " steps and the death " + w.placed.keySet()
                : placed);
    }

    private static String shortList(List<BlockPos> ps) {
        StringBuilder sb = new StringBuilder();
        for (BlockPos p : ps) {
            sb.append(sb.length() == 0 ? "[" : ", ").append(p.toShortString());
        }
        return sb.append(']').toString();
    }
}
