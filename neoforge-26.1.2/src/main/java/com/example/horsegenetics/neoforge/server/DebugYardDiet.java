package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.genes.DietGene;
import com.example.horsegenetics.common.genetics.genes.FoodPreferenceGene;
import com.example.horsegenetics.common.genetics.genes.MagicMilkVolumeGene;
import com.example.horsegenetics.common.genetics.genes.MilkGene;
import com.example.horsegenetics.common.genetics.genes.PassificationGene;
import com.example.horsegenetics.common.genetics.genes.PotionMilkGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.PassificationAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * <b>Row AZ: what a horse eats, judged by clock</b> (2026-10-02). West, GRAZING - four stone-floored glass cells where
 * hungry horses find food for themselves, read off the hunger attachment and the food's own existence. East, HANDS AT
 * FEEDING - the FakePlayer hands of {@link DebugYardClockwork} hold out a favourite, nether wart, buckets and a bottle.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>footprint</th><th>answers by</th></tr>
 *   <tr><td>west</td><td>GRAZING</td><td>x0..x0+19, z0+1..z0+11</td><td>first poll after a meal (seconds) to 10 min</td></tr>
 *   <tr><td>east</td><td>HANDS AT FEEDING</td><td>x0+25..x0+44, z0+1..z0+12</td><td>10 s (favourite, bottle); 5 min
 *       (wart); 40 min (lava)</td></tr>
 * </table>
 *
 * <p>Every check is registered with {@link DebugYardClockwork#expect} at build and answered exactly once; every clock
 * step is run through {@link #at}, which drops a step left over from an older yard (the {@link #run} counter) and turns
 * a step that throws into INCONCLUSIVE lines for the checks it carried. A global deadline at {@link #DEADLINE} answers
 * anything still open as INCONCLUSIVE with what was seen, so nothing here can go silent.
 */
final class DebugYardDiet {

    private DebugYardDiet() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;
    /** The checks this run has answered - each is answered once. */
    private static final Set<String> ANSWERED = new HashSet<>();

    /** Fifty-five minutes: past the last scheduled verdict (the lava count, at about 40). */
    private static final long DEADLINE = 66_000L;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        ANSWERED.clear();
        try {
            for (String c : ALL) {
                DebugYardClockwork.expect(c);
            }
            grazing(level, gy, x0, z0, myRun);
            feeding(level, gy, x0 + 25, z0, myRun);
            at(level, DEADLINE, myRun, ALL, () -> {
                for (String c : ALL) {
                    answer(c, null, "no verdict " + DEADLINE / 1200 + " min after the build - a step never reached its"
                            + " reading (see the row AZ lines above for what was seen)");
                }
            });
            ActionTrace.log("test yard", "row AZ built (west: GRAZING; east: HANDS AT FEEDING) at game tick "
                    + level.getGameTime());
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AZ (diet pens) failed to build", e);
        }
    }

    // ==================================================================
    // Checks
    // ==================================================================

    private static final String WEST = "GRAZING";
    private static final String EAST = "HANDS AT FEEDING";

    private static final String CHECK_HAY = WEST + " - a hungry horse walks to a plain minecraft:hay_block and eats it"
            + " (a no-bale control does not rise)";
    private static final String CHECK_DROPPED = WEST + " - a hungry ordinary horse eats a dropped potato, beetroot and"
            + " melon slice";
    private static final String CHECK_NARROW = WEST + " - a hungry wheat-eater leaves a dropped potato, beetroot and"
            + " melon slice (and eats the dropped wheat beside them)";
    private static final String CHECK_FAV = EAST + " - a wheat-eater whose favourite is apples takes an apple from the"
            + " hand; the same diet with no favourite refuses one";
    private static final String CHECK_WART_FOAL = EAST + " - nether wart does nothing at all to a foal Netherhorse";
    private static final String CHECK_WART_ADULT = EAST + " - nether wart permanently settles a grown Netherhorse";
    private static final String CHECK_LAVA = EAST + " - a Lava/Lava Mlk/Mlk mare fills more lava buckets than a"
            + " Lava/Lava control in 40 min of a try every 30 s";
    private static final String CHECK_CURSE = EAST + " - a Wkn/Drk mare's one bottle carries weakness and darkness,"
            + " both at the weak grade";

    private static final List<String> ALL = List.of(CHECK_HAY, CHECK_DROPPED, CHECK_NARROW, CHECK_FAV,
            CHECK_WART_FOAL, CHECK_WART_ADULT, CHECK_LAVA, CHECK_CURSE);

    // ==================================================================
    // WEST - GRAZING
    // ==================================================================

    /*
     * GRAZING. Three open checks, two pages, all about HungerFoodGoal - the goal a hungry horse uses to find its own
     * food - and none of them answerable by a hand, because the whole point is that nobody feeds these horses.
     *
     * 1. wiki/compatibility.html, "Horses eat other mods' hay bales", With no second mod installed:
     *      "A hungry horse still walks to a plain hay bale and eats it. This is the one that matters most and it is
     *       the regression check, not the feature: grazing hay stopped being a hardcoded block and became a tag lookup,
     *       so if the tag is wrong every horse silently stops eating hay everywhere. The gametest hay_is_still_a_bale
     *       covers the tag itself; this covers the goal that reads it."
     * 2. wiki/horse-care.html, "Dropped food is judged by what a horse grazes", In the game, with your hands:
     *      "A dropped potato is eaten. Drop a potato, a beetroot or a melon slice - none of them in vanilla's
     *       horse-food tag - beside a hungry ordinary horse. Pass: it walks over and eats it, and hunger logs
     *       ate minecraft:potato (dropped). Fail: it walks past to the grass, which means the tag lookup is not
     *       matching."
     * 3. Same section: "A narrow diet still refuses. Drop a potato by a hungry wheat-eater. Pass: ignored; hay and
     *       wheat only. Fail: it eats it, which means the widening leaked past NORMAL/ANYTHING."
     *
     * WHAT THE CODE SAYS (read 2026-10-02). HungerFoodGoal.canUse: below Hunger.HUNGRY (50) the horse searches every
     * 40 ticks (SEARCH_INTERVAL); a dropped ItemEntity it may eat (DietFoods.acceptsFromGround) is the DROPPED rung,
     * worth 25; a block HayBales.isBale accepts is the HAY rung, worth 80, eaten whole by level.destroyBlock (the
     * bale is GONE afterwards) unless EventHooks.canEntityGrief says no, in which case it still gets its mouthful and
     * the block stays. A candidate is only taken if a path reaches it (reachable -> createPath, canReach), so food on
     * the far side of glass is tried once and ignored for 1,200 ticks. acceptsFromGround: NORMAL and ANYTHING take the
     * item form of every grazing rung by the c: tags (crops, fruit, vegetable, berry - the potato, beetroot and melon
     * slice are expected there, UNVERIFIED tag contents, which is the question); every narrow diet answers the
     * hand-feeding list - Diet.WHEAT is WHEAT_ITEMS (wheat, hay block) plus any bale, so a wheat-eater refuses all
     * three and takes dropped wheat.
     *
     * THE CELLS. Four glass cells side by side, each 5 x 11 with a 3 x 9 interior, walls two high and a glass lid at
     * gy + 3 that leaves the yard's light blocks where they are. The whole west half is floored with stone first, so
     * there is no grass, moss or flower anywhere in the footprint; the yard's grass outside it (the aisle at z0 - 1 and
     * the walkway) is within the goal's ten-block search but behind glass, so it is tried, found unreachable and
     * ignored - which is exactly what the control cell proves does not feed anybody.
     *   W1 HAY      (x0..x0+4)      an ordinary mare at the south end, one minecraft:hay_block at the north end.
     *   W2 CONTROL  (x0+5..x0+9)    an ordinary mare, nothing to eat. The bale in W1 is in its search, behind glass.
     *   W3 NARROW   (x0+10..x0+14)  a Dwht/Dwht mare; a potato, a beetroot, a melon slice and, behind them, a wheat.
     *   W4 DROPPED  (x0+15..x0+19)  an ordinary mare; a potato, a beetroot and a melon slice.
     * The items are spawned still (zero motion) and with an unlimited lifetime, or the ten minutes would outlast an
     * item's five-minute despawn. Nothing in the footprint picks items up.
     *
     * THE HUNGER. Written to 20 through the attachment (ModAttachments.HUNGER) 40 ticks after the build, once the
     * horses have joined. A mouthful of dropped food is +25, so a horse at 20 eats one (45, still hungry), then a
     * second (70) and stops looking - so whenever W3's or W4's horse reads 50 or more and food is still on its floor,
     * the poll writes it back to 20 (counted as a re-starve in the detail). The drain only ever lowers hunger, so a
     * rise between polls is a mouthful; the poll sums the rises.
     *
     * TIMING. First poll 140 ticks after the build (100 after the hunger write - an eating goal is picked up up to
     * ~20 ticks after its trigger, never sample sooner than 40), then every 100 ticks, for ten minutes (GRAZE_FOR).
     *
     * PASS / FAIL.
     *   CHECK_HAY: PASS the first poll at which W1's mare has risen by 40 or more above her lowest reading, with the
     *     bale's state (gone, or still there because mob griefing is off) in the detail, and W2's mare never risen
     *     by more than 0.5 between two polls. FAIL: ten minutes with no rise. INCONCLUSIVE: a horse gone or out of its
     *     cell, the bale gone with no rise (something else took it), or W2 rose too (something in the world feeds
     *     horses, so a rise in W1 proves nothing).
     *   CHECK_DROPPED: PASS when all three items are gone and the summed rises are at least 60 (three mouthfuls of 25
     *     less the drain between polls). FAIL: any of the three still on the floor at ten minutes. INCONCLUSIVE: the
     *     items gone with rises under 60 (they vanished without being eaten), or the horse gone or out of its cell.
     *   CHECK_NARROW: FAIL at the first poll a potato, beetroot or melon slice has gone with a rise beside it. PASS at
     *     ten minutes if all three are still there and the wheat has gone (the horse was hungry, looked, found the one
     *     thing it eats, and walked past the rest). INCONCLUSIVE: the wheat never eaten (the horse never went looking,
     *     so the potato lying there proves nothing), a forbidden item gone with no rise, a horse gone or out of its
     *     cell, or the horse not resolving to Diet.WHEAT.
     * About ten minutes; the hay and dropped checks usually answer in the first minute.
     *
     * UNVERIFIED: ItemEntity.setUnlimitedLifetime has no caller in this repo; it is read off the 26.1.2 sources jar
     * (public void setUnlimitedLifetime(), ItemEntity line 451). new ItemEntity(level, x, y, z, stack),
     * setDeltaMovement and level.getEntity(UUID) are used as elsewhere in this package.
     */

    private static final double HUNGRY_AT = 20.0;
    /** Ten minutes of polls. */
    private static final long GRAZE_FOR = 12_000L;
    private static final long POLL = 100L;

    private record Cell(int x0, int x1, int z0, int z1) {
        /** Inside the cell's interior (walls excluded). */
        boolean holds(Horse h) {
            int x = h.getBlockX();
            int z = h.getBlockZ();
            return x > x0 && x < x1 && z > z0 && z < z1;
        }
    }

    private static final class Grazing {
        final int run;
        final Cell hayCell;
        final Cell ctrlCell;
        final Cell narrowCell;
        final Cell normalCell;
        final BlockPos bale;
        @Nullable UUID hay;
        @Nullable UUID ctrl;
        @Nullable UUID narrow;
        @Nullable UUID normal;
        final Map<String, UUID> normalItems = new LinkedHashMap<>();
        final Map<String, UUID> narrowItems = new LinkedHashMap<>();
        final Map<String, Long> eatenAt = new LinkedHashMap<>();
        long t0;
        int polls;
        double hayMin = Double.MAX_VALUE;
        double ctrlLast = Double.NaN;
        double ctrlWorstRise;
        final StringBuilder hayHist = new StringBuilder();
        final StringBuilder ctrlHist = new StringBuilder();
        double normalLast;
        double normalGain;
        int normalRestarve;
        double narrowLast;
        double narrowGain;
        int narrowRestarve;
        final StringBuilder normalHist = new StringBuilder();
        final StringBuilder narrowHist = new StringBuilder();

        Grazing(int run, int gy, int x0, int z0) {
            this.run = run;
            this.hayCell = new Cell(x0, x0 + 4, z0 + 1, z0 + 11);
            this.ctrlCell = new Cell(x0 + 5, x0 + 9, z0 + 1, z0 + 11);
            this.narrowCell = new Cell(x0 + 10, x0 + 14, z0 + 1, z0 + 11);
            this.normalCell = new Cell(x0 + 15, x0 + 19, z0 + 1, z0 + 11);
            this.bale = new BlockPos(x0 + 2, gy + 1, z0 + 3);
        }
    }

    private static void grazing(ServerLevel level, int gy, int x0, int z0, int myRun) {
        // Stone wall to wall over the whole west half: no grass, moss or flower anywhere a horse here could reach.
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = x0; x <= x0 + 19; x++) {
            for (int z = z0; z <= z0 + 12; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
        Grazing g = new Grazing(myRun, gy, x0, z0);
        for (Cell c : List.of(g.hayCell, g.ctrlCell, g.narrowCell, g.normalCell)) {
            glassCell(level, gy, c, gy + 2, true);
            YardPens.register(gy, c.x0(), c.x1(), c.z0(), c.z1(), WEST + " cell " + c.x0());
        }
        DebugPenManager.fastSet(level, g.bale, Blocks.HAY_BLOCK.defaultBlockState());
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("GRAZING", "hay | control", "wheat-eater |", "dropped potato"));

        String plain = DietGene.KEY + "=n/n";
        Horse hay = DebugYardClockwork.horse(level, gy, x0 + 2.5, z0 + 8.5, Sex.FEMALE, plain, WEST + " HAY BALE");
        Horse ctrl = DebugYardClockwork.horse(level, gy, x0 + 7.5, z0 + 8.5, Sex.FEMALE, plain, WEST + " CONTROL");
        Horse narrow = DebugYardClockwork.horse(level, gy, x0 + 12.5, z0 + 8.5, Sex.FEMALE,
                DietGene.KEY + "=Dwht/Dwht", WEST + " WHEAT-EATER");
        Horse normal = DebugYardClockwork.horse(level, gy, x0 + 17.5, z0 + 8.5, Sex.FEMALE, plain,
                WEST + " DROPPED FOOD");
        g.hay = hay == null ? null : hay.getUUID();
        g.ctrl = ctrl == null ? null : ctrl.getUUID();
        g.narrow = narrow == null ? null : narrow.getUUID();
        g.normal = normal == null ? null : normal.getUUID();

        at(level, 40, myRun, List.of(CHECK_HAY, CHECK_DROPPED, CHECK_NARROW), () -> {
            g.t0 = level.getGameTime();
            StringBuilder who = new StringBuilder();
            for (Map.Entry<String, UUID> e : Map.of("hay", nz(g.hay), "control", nz(g.ctrl), "wheat-eater",
                    nz(g.narrow), "dropped", nz(g.normal)).entrySet()) {
                Horse h = find(level, e.getValue());
                if (h != null) {
                    h.setData(ModAttachments.HUNGER.get(), HUNGRY_AT);
                }
                who.append(e.getKey()).append(h == null ? " MISSING; " : " diet "
                        + HorseDietHandler.dietOf(h).diet() + "; ");
            }
            g.normalLast = HUNGRY_AT;
            g.narrowLast = HUNGRY_AT;
            Cell n = g.normalCell;
            g.normalItems.put("potato", drop(level, n.x0() + 1.5, gy + 1, n.z0() + 2.5, Items.POTATO));
            g.normalItems.put("beetroot", drop(level, n.x0() + 2.5, gy + 1, n.z0() + 2.5, Items.BEETROOT));
            g.normalItems.put("melon_slice", drop(level, n.x0() + 3.5, gy + 1, n.z0() + 2.5, Items.MELON_SLICE));
            Cell w = g.narrowCell;
            g.narrowItems.put("potato", drop(level, w.x0() + 1.5, gy + 1, w.z0() + 3.5, Items.POTATO));
            g.narrowItems.put("beetroot", drop(level, w.x0() + 2.5, gy + 1, w.z0() + 3.5, Items.BEETROOT));
            g.narrowItems.put("melon_slice", drop(level, w.x0() + 3.5, gy + 1, w.z0() + 3.5, Items.MELON_SLICE));
            g.narrowItems.put("wheat", drop(level, w.x0() + 2.5, gy + 1, w.z0() + 1.5, Items.WHEAT));
            ActionTrace.log("test yard", WEST + ": t0 at tick " + g.t0 + " - hunger written to " + HUNGRY_AT
                    + " on " + who + "bale at " + g.bale.toShortString() + " is "
                    + BuiltInRegistries.BLOCK.getKey(level.getBlockState(g.bale).getBlock()) + "; polls every "
                    + POLL + " ticks for " + GRAZE_FOR / 1200 + " min");
            grazePoll(level, g);
        });
    }

    private static UUID nz(@Nullable UUID id) {
        return id == null ? new UUID(0L, 0L) : id;
    }

    /** One still, everlasting item on the floor, and its UUID (a missing entity later reads as eaten). */
    private static UUID drop(ServerLevel level, double x, int y, double z, Item item) {
        ItemEntity e = new ItemEntity(level, x, y, z, new ItemStack(item));
        e.setDeltaMovement(0.0, 0.0, 0.0);
        // UNVERIFIED in this repo (read in the 26.1.2 sources): an item otherwise despawns at five minutes.
        e.setUnlimitedLifetime();
        level.addFreshEntity(e);
        return e.getUUID();
    }

    private static boolean present(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof ItemEntity e && e.isAlive() && !e.getItem().isEmpty();
    }

    private static double hunger(Horse h) {
        return h.getData(ModAttachments.HUNGER.get());
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static void grazePoll(ServerLevel level, Grazing g) {
        at(level, POLL, g.run, List.of(CHECK_HAY, CHECK_DROPPED, CHECK_NARROW), () -> {
            if (ANSWERED.containsAll(List.of(CHECK_HAY, CHECK_DROPPED, CHECK_NARROW))) {
                return;
            }
            grazePoll(level, g);    // first, so a throw below cannot stop the clock
            g.polls++;
            long el = level.getGameTime() - g.t0;
            boolean last = el >= GRAZE_FOR;
            hayStep(level, g, el, last);
            droppedStep(level, g, el, last);
            narrowStep(level, g, el, last);
        });
    }

    private static void hayStep(ServerLevel level, Grazing g, long el, boolean last) {
        if (ANSWERED.contains(CHECK_HAY)) {
            return;
        }
        Horse hay = find(level, g.hay);
        Horse ctrl = find(level, g.ctrl);
        if (hay == null || ctrl == null || !g.hayCell.holds(hay) || !g.ctrlCell.holds(ctrl)) {
            answer(CHECK_HAY, null, "at +" + el + " ticks: hay horse " + where(hay, g.hayCell) + ", control "
                    + where(ctrl, g.ctrlCell) + " | hay " + g.hayHist + " | control " + g.ctrlHist);
            return;
        }
        double hh = hunger(hay);
        double ch = hunger(ctrl);
        g.hayMin = Math.min(g.hayMin, hh);
        if (!Double.isNaN(g.ctrlLast)) {
            g.ctrlWorstRise = Math.max(g.ctrlWorstRise, ch - g.ctrlLast);
        }
        g.ctrlLast = ch;
        appendHist(g.hayHist, el, hh);
        appendHist(g.ctrlHist, el, ch);
        boolean baleThere = level.getBlockState(g.bale).is(Blocks.HAY_BLOCK);
        boolean rose = hh >= g.hayMin + 40.0;
        String d = "at +" + el + " ticks (" + g.polls + " polls): hay horse hunger " + f(hh) + " (lowest " + f(g.hayMin)
                + "), bale at " + g.bale.toShortString() + " " + (baleThere ? "still there" : "GONE")
                + "; control hunger " + f(ch) + ", largest rise between polls " + f(g.ctrlWorstRise)
                + " | hay " + g.hayHist + " | control " + g.ctrlHist;
        boolean ctrlRose = g.ctrlWorstRise > 0.5;
        if (rose || !baleThere) {
            if (ctrlRose) {
                answer(CHECK_HAY, null, d + " - the control rose too, so something in this world feeds horses and"
                        + " the rise proves nothing");
            } else if (!rose) {
                answer(CHECK_HAY, null, d + " - the bale went with no rise in the horse beside it; something else"
                        + " took it");
            } else {
                answer(CHECK_HAY, true, d + (baleThere ? " - ate, and the bale stayed: mob griefing must be off"
                        + " (canEntityGrief), which HungerFoodGoal honours" : " - walked to it and ate it whole"));
            }
        } else if (last) {
            answer(CHECK_HAY, ctrlRose ? null : Boolean.FALSE, d + " - " + GRAZE_FOR / 1200 + " min hungry beside a"
                    + " plain hay bale and never ate it" + (ctrlRose ? " (but the control rose, so the world is not"
                    + " the one this test needs)" : ""));
        }
    }

    private static void droppedStep(ServerLevel level, Grazing g, long el, boolean last) {
        if (ANSWERED.contains(CHECK_DROPPED)) {
            return;
        }
        Horse h = find(level, g.normal);
        if (h == null || !g.normalCell.holds(h)) {
            answer(CHECK_DROPPED, null, "at +" + el + " ticks the ordinary horse is " + where(h, g.normalCell)
                    + " | hunger " + g.normalHist);
            return;
        }
        double x = hunger(h);
        if (x > g.normalLast + 1.0) {
            g.normalGain += x - g.normalLast;
        }
        g.normalLast = x;
        appendHist(g.normalHist, el, x);
        List<String> left = new ArrayList<>();
        for (Map.Entry<String, UUID> e : g.normalItems.entrySet()) {
            if (present(level, e.getValue())) {
                left.add(e.getKey());
            } else {
                g.eatenAt.putIfAbsent("ordinary " + e.getKey(), el);
            }
        }
        String d = "at +" + el + " ticks (" + g.polls + " polls): left on the floor " + (left.isEmpty() ? "nothing"
                : left) + "; gone at " + eaten(g, "ordinary ") + "; hunger " + f(x) + ", summed rises " + f(g.normalGain)
                + ", re-starved to " + HUNGRY_AT + " " + g.normalRestarve + " time(s) | hunger " + g.normalHist
                + " | diet " + HorseDietHandler.dietOf(h).diet();
        if (left.isEmpty()) {
            if (g.normalGain >= 60.0) {
                answer(CHECK_DROPPED, true, d + " - all three eaten (three mouthfuls are 75 less the drain)");
            } else {
                answer(CHECK_DROPPED, null, d + " - all three gone but the horse gained under 60, so they were not"
                        + " (all) eaten");
            }
            return;
        }
        if (last) {
            answer(CHECK_DROPPED, false, d + " - " + GRAZE_FOR / 1200 + " min hungry and still not eaten: the tag"
                    + " lookup is not matching " + left);
            return;
        }
        if (x >= 50.0) {
            h.setData(ModAttachments.HUNGER.get(), HUNGRY_AT);
            g.normalLast = HUNGRY_AT;
            g.normalRestarve++;
        }
    }

    private static void narrowStep(ServerLevel level, Grazing g, long el, boolean last) {
        if (ANSWERED.contains(CHECK_NARROW)) {
            return;
        }
        Horse h = find(level, g.narrow);
        if (h == null || !g.narrowCell.holds(h)) {
            answer(CHECK_NARROW, null, "at +" + el + " ticks the wheat-eater is " + where(h, g.narrowCell)
                    + " | hunger " + g.narrowHist);
            return;
        }
        Diet diet = HorseDietHandler.dietOf(h).diet();
        if (diet != Diet.WHEAT) {
            answer(CHECK_NARROW, null, "the Dwht/Dwht horse resolves to diet " + diet + ", not WHEAT");
            return;
        }
        double x = hunger(h);
        double rise = x - g.narrowLast;
        if (rise > 1.0) {
            g.narrowGain += rise;
        }
        g.narrowLast = x;
        appendHist(g.narrowHist, el, x);
        List<String> forbiddenGone = new ArrayList<>();
        boolean wheatGone = false;
        for (Map.Entry<String, UUID> e : g.narrowItems.entrySet()) {
            if (!present(level, e.getValue())) {
                g.eatenAt.putIfAbsent("wheat-eater " + e.getKey(), el);
                if ("wheat".equals(e.getKey())) {
                    wheatGone = true;
                } else {
                    forbiddenGone.add(e.getKey());
                }
            }
        }
        String d = "at +" + el + " ticks (" + g.polls + " polls): wheat " + (wheatGone ? "eaten" : "still there")
                + ", forbidden gone " + forbiddenGone + "; gone at " + eaten(g, "wheat-eater ") + "; hunger " + f(x)
                + ", summed rises " + f(g.narrowGain) + ", re-starved " + g.narrowRestarve + " time(s) | hunger "
                + g.narrowHist;
        if (!forbiddenGone.isEmpty()) {
            if (rise > 10.0 || g.narrowGain > (wheatGone ? 30.0 : 10.0)) {
                answer(CHECK_NARROW, false, d + " - a wheat-eater ate food outside its diet: the ground widening"
                        + " leaked past NORMAL/ANYTHING");
            } else {
                answer(CHECK_NARROW, null, d + " - forbidden food vanished with no mouthful to match");
            }
            return;
        }
        if (last) {
            if (wheatGone && g.narrowGain >= 15.0) {
                answer(CHECK_NARROW, true, d + " - hungry for " + GRAZE_FOR / 1200 + " min, ate the wheat and"
                        + " left all three");
            } else {
                answer(CHECK_NARROW, null, d + " - the wheat was never eaten (or not with a rise), so the horse never"
                        + " went looking and the potato lying there proves nothing");
            }
            return;
        }
        if (x >= 50.0) {
            h.setData(ModAttachments.HUNGER.get(), HUNGRY_AT);
            g.narrowLast = HUNGRY_AT;
            g.narrowRestarve++;
        }
    }

    private static String eaten(Grazing g, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : g.eatenAt.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey().substring(prefix.length())).append(" +")
                        .append(e.getValue());
            }
        }
        return sb.length() == 0 ? "(none)" : "[" + sb + "]";
    }

    /** Appends {@code +t=v} only when the value moved by more than 0.5, so the history stays short. */
    private static void appendHist(StringBuilder hist, long el, double v) {
        int cut = hist.lastIndexOf("=");
        if (cut >= 0) {
            try {
                double lastV = Double.parseDouble(hist.substring(cut + 1));
                if (Math.abs(lastV - v) <= 0.5) {
                    return;
                }
            } catch (NumberFormatException ignored) {
                // a malformed tail is only a log line; append anyway
            }
        }
        hist.append(hist.length() == 0 ? "" : " ").append('+').append(el).append('=').append(f(v));
    }

    private static String where(@Nullable Horse h, Cell c) {
        if (h == null) {
            return "missing";
        }
        return c.holds(h) ? "in its cell" : "OUT of its cell at " + h.blockPosition().toShortString();
    }

    // ==================================================================
    // EAST - HANDS AT FEEDING
    // ==================================================================

    /*
     * HANDS AT FEEDING. Four questions the hands can settle, each with its control in the next cell.
     *
     * 1. FAVOURITE BEATS DIET - wiki/gene-food-preference.html, Verification tab, "Food preference: built, never
     *    confirmed": "Built, never confirmed: food preference - a favourite its diet forbids must still be accepted;
     *    a refusal means HorseDietHandler won the ordering."
     *    What the code says: FoodPreferenceGene's favourites are a fixed list in the class (App = minecraft:apple ...),
     *    not config; favouriteOf reads pair.first(), and App sorts before n. FoodPreferenceHandler subscribes at
     *    EventPriority.HIGH, shrinks the stack, heals 2, adds SPEED for 1,200 ticks and CANCELS, so HorseDietHandler
     *    (default priority, returns on isCanceled) never sees it. Note HorseDietHandler would ALSO accept a favourite
     *    (accepted = accepts || isFavourite), so "accepted" alone does not prove which handler took it: the SPEED
     *    effect is FoodPreferenceHandler's alone and is read as the tell, and reported, not required.
     *    E1 a Dwht/Dwht + App/App mare; E2 a Dwht/Dwht + n/n mare (the control). An apple to each, 200 ticks in.
     *    PASS: E1's apple gone (Hands.count 0) and no "turns its head away" heard; E2 kept the apple and heard the
     *    refusal. FAIL: E1 refused. INCONCLUSIVE: E2 did not refuse (the diet is not refusing apples, so there is no
     *    ordering to see), a horse gone, a diet not WHEAT or a favourite not apple. Answers at 10 s.
     *
     * 2. NETHER WART - wiki/breeds.html, the Netherhorse and Nightmare entry: "The adults-only way in really refuses
     *    a foal. APr/APr is Window.ADULT, and it is the first time a foal-or-adult window has been on a horse anybody
     *    could meet. Pass: nether wart does nothing at all to a foal Netherhorse and permanently settles the grown
     *    one. Fail: the foal takes the wart, which would mean the window is not being asked."
     *    What the code says: netherhorse.json pins passification APr/APr and bands the offering to index 12
     *    (minecraft:nether_wart) with an amount of 2-4; Passification.onOffer cancels and calls accept() only for a
     *    route whose item matches AND whose window is open (ADULT: !isBaby); accept() shrinks one, counts it, and on
     *    the mouthful that completes route.amount() writes until[player] = FOREVER. So both horses are spawned as the
     *    breed (DebugPenManager.spawnBreedHorse - the epigenome band is what makes the offering wart; a genotype
     *    string would roll a random one), the foal made one with setAge(-24000). E6 the foal, E7 the adult, each alone
     *    in a 6 x 6 glass cell with walls four high (the adult stands about three blocks at size 1.9) and no lid, two
     *    blocks apart and a row clear of the rest: Aaa/Aaa fights anything it can reach. One pair of hands with eight
     *    wart is used SIX times on each (MAX_AMOUNT, so any amount the band allows completes) at 200 ticks; the state
     *    is read at +40 and again five minutes later.
     *    PASS (foal): at both readings PassificationAttachment holds nothing for the hands (no fed count, no calm, not
     *    permanent for anyone) and all eight wart are still in the hands. FAIL: the foal took a wart or holds any of
     *    that. PASS (adult): permanent(hands) and permanentForAnyone() at both readings, and 8 - amount wart left.
     *    FAIL: not settled, or settled and then lost it. INCONCLUSIVE: a horse gone, not the breed, the foal grown
     *    before the offer or the adult a foal. Answers at about 5 min.
     *
     * 3. NETHER MILK - wiki/breeds.html, same entry: "Both milk loci do something a bottle can show. The Netherhorse
     *    is Lava/Lava at milk with Mlk/Mlk volume on top, so a grown one should yield lava, and several fillings a day
     *    rather than one."
     *    What the code says: MilkGene Lava/Lava yields minecraft:lava_bucket for a bucket, condition adult only, with
     *    LAVA_COOLDOWN_TICKS 1,200, kind "milk"; MagicMilkVolumeGene adds YieldCharges of that kind (each Mlk copy is
     *    worth at least 1, rounded off its epigenome); GeneYieldHandler.fulfil divides: cd = cooldown / (1 + extras),
     *    stamped in HorseCooldownsAttachment under "yield:horsegenetics.milk", and says
     *    message.horsegenetics.yield.recharging when not ready. So a try every 600 ticks fills every second time for
     *    the control (1,200 / 1) and every time for a Mlk/Mlk mare (at most 1,200 / 3 = 400).
     *    E3 a Lava/Lava Mlk/Mlk mare, E4 a Lava/Lava mare, adult, tamed, full health (the dairy checks of 2026-09-30
     *    refused hurt horses and foals), in 3 x 3 glass cells. Fresh hands with one bucket each try, every 600 ticks
     *    from 400, eighty tries (40 min). PASS: E3 filled more lava buckets than E4 and E4 filled at least one.
     *    FAIL: E3 filled no more than E4. INCONCLUSIVE: a mare gone, grown into anything but an adult, or E4 never
     *    filled (no lava at all, so the volume comparison has nothing under it - reported as such). Answers at 40 min.
     *    (The control's own rhythm - lava, then "recharging", then lava a minute on - is in the detail; it is the
     *    gene-milk page's "lava from a Lava/Lava horse ... lava one minute" reading, offered there, not judged here.)
     *
     * 4. THE NIGHTMARE'S BOTTLE - wiki/breeds.html, same entry: "The Nightmare is Wkn/Drk at potion milk, a compound
     *    heterozygote and the locus's first negative brews: Pass: one bottle, both Weakness and Darkness, both at the
     *    weak grade. Fail: one effect only, which would mean a compound heterozygote is resolving to a single allele
     *    rather than expressing both."
     *    What the code says: PotionMilkGene, two different brews -> two weak bottles (amplifier 0, 900 ticks), each
     *    needing sex_female, adult, tamed, full health; GeneYieldHandler.sameKindEffects merges them into one
     *    PotionContents. E5 a tamed adult Wkn/Drk mare at full health; one glass bottle at 300 ticks. PASS: exactly one
     *    potion, its effects exactly {minecraft:weakness, minecraft:darkness}, every amplifier 0, the bottle spent.
     *    FAIL: anything else. INCONCLUSIVE: the mare gone or hurt. Answers at 15 s. (The retired DAIRY BY CLOCK pen
     *    passed the same bottle on 2026-09-30 for wiki/gene-potion-milk.html; this one is asked for the breeds page.)
     *
     * Reading a potion's effects (PotionContents.getAllEffects, MOB_EFFECT.getKey(e.getEffect().value())) is copied
     * from that retired pen, which compiled and ran in this repo (git d2fdb272^, DebugYardClockwork.bottle).
     */

    private static final long FEED_AT = 200L;
    private static final long BOTTLE_AT = 300L;
    private static final long LAVA_FROM = 400L;
    private static final long LAVA_EVERY = 600L;
    private static final int LAVA_TRIES = 80;
    private static final long WART_SECOND_READ = 6_000L;
    private static final int WART_GIVEN = 8;
    private static final int WART_OFFERS = 6;

    private static final class Feeding {
        final int run;
        @Nullable UUID fav;
        @Nullable UUID favCtrl;
        @Nullable UUID lavaMlk;
        @Nullable UUID lavaCtrl;
        @Nullable UUID curse;
        @Nullable UUID foal;
        @Nullable UUID adult;
        String foalBreed = "?";
        String adultBreed = "?";
        int tries;
        int mlkFilled;
        int ctrlFilled;
        int mlkRecharging;
        int ctrlRecharging;
        final StringBuilder mlkPattern = new StringBuilder();
        final StringBuilder ctrlPattern = new StringBuilder();
        String lavaTrouble = "";
        String foalFirst = "";
        String adultFirst = "";
        String route = "";
        int adultAmount = -1;
        boolean foalFailed;

        Feeding(int run) {
            this.run = run;
        }
    }

    private static void feeding(ServerLevel level, int gy, int ex0, int z0, int myRun) {
        Feeding p = new Feeding(myRun);
        Cell e1 = new Cell(ex0, ex0 + 4, z0 + 1, z0 + 5);
        Cell e2 = new Cell(ex0 + 5, ex0 + 9, z0 + 1, z0 + 5);
        Cell e3 = new Cell(ex0 + 10, ex0 + 14, z0 + 1, z0 + 5);
        Cell e4 = new Cell(ex0 + 15, ex0 + 19, z0 + 1, z0 + 5);
        Cell e5 = new Cell(ex0, ex0 + 4, z0 + 7, z0 + 11);
        Cell e6 = new Cell(ex0 + 6, ex0 + 11, z0 + 7, z0 + 12);
        Cell e7 = new Cell(ex0 + 14, ex0 + 19, z0 + 7, z0 + 12);
        for (Cell c : List.of(e1, e2, e3, e4, e5)) {
            glassCell(level, gy, c, gy + 2, true);
        }
        for (Cell c : List.of(e6, e7)) {
            glassCell(level, gy, c, gy + 4, false);
        }
        int i = 0;
        for (Cell c : List.of(e1, e2, e3, e4, e5, e6, e7)) {
            YardPens.register(gy, c.x0(), c.x1(), c.z0(), c.z1(), EAST + " cell " + (++i));
        }
        DebugPenManager.placeSign(level, new BlockPos(ex0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("HANDS AT FEEDING", "favourite|diet", "wart: foal|adult", "lava x2|curses"));

        String wheat = DietGene.KEY + "=Dwht/Dwht";
        Horse fav = DebugYardClockwork.horse(level, gy, ex0 + 2.5, z0 + 3.5, Sex.FEMALE,
                wheat + "-" + FoodPreferenceGene.KEY + "=App/App", EAST + " WHEAT-EATER LOVES APPLES");
        Horse favCtrl = DebugYardClockwork.horse(level, gy, ex0 + 7.5, z0 + 3.5, Sex.FEMALE,
                wheat + "-" + FoodPreferenceGene.KEY + "=n/n", EAST + " WHEAT-EATER CONTROL");
        Horse lavaMlk = DebugYardClockwork.horse(level, gy, ex0 + 12.5, z0 + 3.5, Sex.FEMALE,
                MilkGene.KEY + "=Lava/Lava-" + MagicMilkVolumeGene.KEY + "=Mlk/Mlk", EAST + " LAVA Mlk/Mlk");
        Horse lavaCtrl = DebugYardClockwork.horse(level, gy, ex0 + 17.5, z0 + 3.5, Sex.FEMALE,
                MilkGene.KEY + "=Lava/Lava", EAST + " LAVA CONTROL");
        Horse curse = DebugYardClockwork.horse(level, gy, ex0 + 2.5, z0 + 9.5, Sex.FEMALE,
                PotionMilkGene.KEY + "=Wkn/Drk", EAST + " Wkn/Drk BOTTLE");
        p.fav = id(fav);
        p.favCtrl = id(favCtrl);
        p.lavaMlk = id(lavaMlk);
        p.lavaCtrl = id(lavaCtrl);
        p.curse = id(curse);

        Breed nether = Breeds.get("netherhorse");
        Horse foal = DebugPenManager.spawnBreedHorse(level, gy + 1, ex0 + 9.0, z0 + 10.0, Sex.FEMALE, nether);
        Horse adult = DebugPenManager.spawnBreedHorse(level, gy + 1, ex0 + 17.0, z0 + 10.0, Sex.FEMALE, nether);
        if (foal != null) {
            foal.setAge(-24_000);
            DebugTestYard.label(foal, EAST + " NETHERHORSE FOAL");
            p.foal = foal.getUUID();
            p.foalBreed = HorseRecords.of(foal).lineage().components().toString();
        }
        if (adult != null) {
            DebugTestYard.label(adult, EAST + " NETHERHORSE ADULT");
            p.adult = adult.getUUID();
            p.adultBreed = HorseRecords.of(adult).lineage().components().toString();
        }
        String breedNote = "Breeds.get(\"netherhorse\") gave " + nether.id();

        at(level, FEED_AT, myRun, List.of(CHECK_FAV), () -> favourite(level, p));
        at(level, BOTTLE_AT, myRun, List.of(CHECK_CURSE), () -> bottle(level, p));
        at(level, FEED_AT, myRun, List.of(CHECK_WART_FOAL, CHECK_WART_ADULT), () -> wartOffer(level, p, breedNote));
        lavaTry(level, p, LAVA_FROM);
    }

    private static @Nullable UUID id(@Nullable Horse h) {
        return h == null ? null : h.getUUID();
    }

    // ---------------------------------------------------------------- favourite

    private static void favourite(ServerLevel level, Feeding p) {
        Horse fav = find(level, p.fav);
        Horse ctrl = find(level, p.favCtrl);
        if (fav == null || ctrl == null) {
            answer(CHECK_FAV, null, "favourite horse " + (fav == null ? "missing" : "present") + ", control "
                    + (ctrl == null ? "missing" : "present"));
            return;
        }
        Diet dFav = HorseDietHandler.dietOf(fav).diet();
        Diet dCtrl = HorseDietHandler.dietOf(ctrl).diet();
        String favItem = FoodPreferenceHandler.favouriteOf(fav);
        String ctrlItem = FoodPreferenceHandler.favouriteOf(ctrl);
        String setup = "diets " + dFav + " / " + dCtrl + ", favourites " + favItem + " / " + ctrlItem
                + ", adult " + !fav.isBaby() + " / " + !ctrl.isBaby();
        if (dFav != Diet.WHEAT || dCtrl != Diet.WHEAT || !"minecraft:apple".equals(favItem) || ctrlItem != null
                || fav.isBaby() || ctrl.isBaby()) {
            answer(CHECK_FAV, null, "not the horses the test needs: " + setup);
            return;
        }
        boolean speedBefore = fav.hasEffect(MobEffects.SPEED);
        DebugYardClockwork.Hands hf = DebugYardClockwork.hands(level, fav, new ItemStack(Items.APPLE), false);
        DebugYardClockwork.use(hf, fav);
        boolean speedAfter = fav.hasEffect(MobEffects.SPEED);
        DebugYardClockwork.Hands hc = DebugYardClockwork.hands(level, ctrl, new ItemStack(Items.APPLE), false);
        DebugYardClockwork.use(hc, ctrl);
        boolean favRefused = hf.heardAny("turns its head away");
        boolean favAte = hf.count(Items.APPLE) == 0;
        boolean ctrlRefused = hc.heardAny("turns its head away") && hc.count(Items.APPLE) == 1;
        String d = setup + " | favourite: apples left " + hf.count(Items.APPLE) + ", speed effect " + speedBefore
                + " -> " + speedAfter + (speedAfter && !speedBefore ? " (FoodPreferenceHandler took it at HIGH; the"
                + " diet handler never saw it)" : " (no speed: if it was eaten, HorseDietHandler's own isFavourite"
                + " took it, not the preference handler)") + ", " + hf.said() + " | control: apples left "
                + hc.count(Items.APPLE) + ", " + hc.said();
        if (favRefused || !favAte) {
            answer(CHECK_FAV, false, d + " - the favourite was refused: HorseDietHandler won the ordering");
        } else if (!ctrlRefused) {
            answer(CHECK_FAV, null, d + " - the control did not refuse the apple, so this diet is not refusing apples"
                    + " and there is no ordering to see");
        } else {
            answer(CHECK_FAV, true, d);
        }
    }

    // ---------------------------------------------------------------- nightmare bottle

    private static void bottle(ServerLevel level, Feeding p) {
        Horse h = find(level, p.curse);
        if (h == null) {
            answer(CHECK_CURSE, null, "the Wkn/Drk mare is missing");
            return;
        }
        String state = String.format(Locale.ROOT, "health %.1f/%.1f, %s, tamed %s", h.getHealth(), h.getMaxHealth(),
                h.isBaby() ? "foal" : "adult", h.isTamed());
        if (h.isBaby() || h.getHealth() < h.getMaxHealth() || !h.isTamed()) {
            answer(CHECK_CURSE, null, "the mare cannot be milked as built: " + state);
            return;
        }
        DebugYardClockwork.Hands hands = DebugYardClockwork.hands(level, h, new ItemStack(Items.GLASS_BOTTLE), false);
        DebugYardClockwork.use(hands, h);
        ItemStack potion = hands.first(Items.POTION);
        List<String> ids = new ArrayList<>();
        List<String> got = new ArrayList<>();
        boolean allWeak = true;
        if (!potion.isEmpty()) {
            PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (MobEffectInstance e : contents.getAllEffects()) {
                    String key = String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()));
                    ids.add(key);
                    got.add(key + " amp " + e.getAmplifier());
                    allWeak &= e.getAmplifier() == 0;
                }
            }
        }
        String d = state + "; potions " + hands.count(Items.POTION) + " " + got + ", bottles left "
                + hands.count(Items.GLASS_BOTTLE) + "; " + hands.said();
        boolean both = ids.size() == 2
                && new TreeSet<>(ids).equals(new TreeSet<>(List.of("minecraft:darkness", "minecraft:weakness")));
        answer(CHECK_CURSE, hands.count(Items.POTION) == 1 && hands.count(Items.GLASS_BOTTLE) == 0 && both && allWeak,
                d);
    }

    // ---------------------------------------------------------------- nether wart

    private static String passState(Horse h, UUID hands) {
        PassificationAttachment s = h.getData(ModAttachments.PASSIFICATION.get());
        return "permanent(hands) " + s.permanent(hands) + ", permanentForAnyone " + s.permanentForAnyone()
                + ", calm(hands) " + s.calm(hands, h.level().getGameTime()) + ", fed wart "
                + s.fed(hands, "minecraft:nether_wart") + ", until " + s.until() + ", progress " + s.progress();
    }

    private static boolean untouched(Horse h, UUID hands) {
        PassificationAttachment s = h.getData(ModAttachments.PASSIFICATION.get());
        return !s.permanentForAnyone() && !s.calm(hands, h.level().getGameTime()) && s.progress().isEmpty()
                && s.until().isEmpty();
    }

    private static boolean settled(Horse h, UUID hands) {
        PassificationAttachment s = h.getData(ModAttachments.PASSIFICATION.get());
        return s.permanent(hands) && s.permanentForAnyone();
    }

    private static void wartOffer(ServerLevel level, Feeding p, String breedNote) {
        Horse foal = find(level, p.foal);
        Horse adult = find(level, p.adult);
        StringBuilder routes = new StringBuilder();
        if (adult != null) {
            for (PassificationGene.Route r : Passification.routesOf(adult)) {
                routes.append(r.kind()).append(' ').append(r.window()).append(' ').append(r.amount()).append("x ")
                        .append(r.item()).append("; ");
                if (r.item().equals("minecraft:nether_wart")) {
                    p.adultAmount = r.amount();
                }
            }
        }
        p.route = "adult routes [" + routes + "] | " + breedNote + "; lineage foal " + p.foalBreed + ", adult "
                + p.adultBreed;
        if (!breedNote.endsWith("netherhorse")) {
            answer(CHECK_WART_FOAL, null, "the Netherhorse breed did not load, so these are not Netherhorses | "
                    + p.route);
            answer(CHECK_WART_ADULT, null, "the Netherhorse breed did not load, so these are not Netherhorses | "
                    + p.route);
            return;
        }
        // The foal.
        if (foal == null) {
            answer(CHECK_WART_FOAL, null, "the foal is missing | " + p.route);
        } else if (!foal.isBaby()) {
            answer(CHECK_WART_FOAL, null, "the foal had grown before the offer | " + p.route);
        } else {
            DebugYardClockwork.Hands h = DebugYardClockwork.hands(level, foal,
                    new ItemStack(Items.NETHER_WART, WART_GIVEN), false);
            for (int k = 0; k < WART_OFFERS; k++) {
                DebugYardClockwork.use(h, foal);
            }
            int left = h.count(Items.NETHER_WART);
            p.foalFirst = "offered " + WART_OFFERS + " of " + WART_GIVEN + " wart, " + left + " left, " + h.said();
            if (left != WART_GIVEN) {
                p.foalFailed = true;
            }
        }
        // The adult.
        if (adult == null) {
            answer(CHECK_WART_ADULT, null, "the adult is missing | " + p.route);
        } else if (adult.isBaby()) {
            answer(CHECK_WART_ADULT, null, "the adult is a foal | " + p.route);
        } else {
            DebugYardClockwork.Hands h = DebugYardClockwork.hands(level, adult,
                    new ItemStack(Items.NETHER_WART, WART_GIVEN), false);
            for (int k = 0; k < WART_OFFERS; k++) {
                DebugYardClockwork.use(h, adult);
            }
            p.adultFirst = "offered " + WART_OFFERS + " of " + WART_GIVEN + " wart, " + h.count(Items.NETHER_WART)
                    + " left (a route of " + p.adultAmount + " should leave " + (WART_GIVEN - p.adultAmount) + "), "
                    + h.said();
        }
        at(level, 40, p.run, List.of(CHECK_WART_FOAL, CHECK_WART_ADULT), () -> wartRead(level, p, false));
        at(level, WART_SECOND_READ, p.run, List.of(CHECK_WART_FOAL, CHECK_WART_ADULT), () -> wartRead(level, p, true));
    }

    private static UUID handsId(ServerLevel level) {
        return new DebugYardClockwork.Hands(level).getUUID();
    }

    private static void wartRead(ServerLevel level, Feeding p, boolean second) {
        UUID hands = handsId(level);
        String when = second ? "+" + (WART_SECOND_READ / 1200) + " min" : "+40 ticks";
        if (!ANSWERED.contains(CHECK_WART_FOAL)) {
            Horse foal = find(level, p.foal);
            if (foal == null) {
                answer(CHECK_WART_FOAL, null, "the foal is gone at " + when + " | " + p.foalFirst);
            } else {
                boolean ok = untouched(foal, hands) && !p.foalFailed;
                String d = "at " + when + ": " + passState(foal, hands) + ", still a foal " + foal.isBaby() + " | "
                        + p.foalFirst + " | " + p.route;
                if (!ok) {
                    answer(CHECK_WART_FOAL, false, d + " - the foal took wart or holds a calm: the window is not"
                            + " being asked");
                } else if (second) {
                    answer(CHECK_WART_FOAL, true, d);
                } else {
                    p.foalFirst += " | at +40 ticks untouched";
                }
            }
        }
        if (!ANSWERED.contains(CHECK_WART_ADULT)) {
            Horse adult = find(level, p.adult);
            if (adult == null) {
                answer(CHECK_WART_ADULT, null, "the adult is gone at " + when + " | " + p.adultFirst);
            } else {
                boolean ok = settled(adult, hands);
                String d = "at " + when + ": " + passState(adult, hands) + " | " + p.adultFirst + " | " + p.route;
                if (!ok) {
                    answer(CHECK_WART_ADULT, false, d + (second ? " - settled at +40 ticks and lost it since"
                            : " - not permanently settled by the wart"));
                } else if (second) {
                    answer(CHECK_WART_ADULT, true, d);
                } else {
                    p.adultFirst += " | at +40 ticks settled";
                }
            }
        }
    }

    // ---------------------------------------------------------------- lava

    private static void lavaTry(ServerLevel level, Feeding p, long delay) {
        at(level, delay, p.run, List.of(CHECK_LAVA), () -> {
            if (ANSWERED.contains(CHECK_LAVA)) {
                return;
            }
            p.tries++;
            int n = p.tries;
            if (n < LAVA_TRIES) {
                lavaTry(level, p, LAVA_EVERY);  // first, so a throw below cannot stop the clock
            }
            int[] mlk = milkOnce(level, p.lavaMlk, p, "Mlk/Mlk");
            int[] ctrl = milkOnce(level, p.lavaCtrl, p, "control");
            p.mlkFilled += mlk[0];
            p.mlkRecharging += mlk[1];
            p.ctrlFilled += ctrl[0];
            p.ctrlRecharging += ctrl[1];
            p.mlkPattern.append(mlk[0] == 1 ? 'L' : mlk[1] == 1 ? '.' : '?');
            p.ctrlPattern.append(ctrl[0] == 1 ? 'L' : ctrl[1] == 1 ? '.' : '?');
            if (n % 20 == 0 && n < LAVA_TRIES) {
                ActionTrace.log("test yard", EAST + " lava after " + n + " tries: Mlk/Mlk " + p.mlkFilled
                        + ", control " + p.ctrlFilled);
            }
            if (n >= LAVA_TRIES) {
                lavaJudge(level, p);
            }
        });
    }

    /** {filled, recharging}; a missing or unfit mare is noted in {@code p.lavaTrouble}. */
    private static int[] milkOnce(ServerLevel level, @Nullable UUID id, Feeding p, String tag) {
        Horse h = find(level, id);
        if (h == null) {
            if (!p.lavaTrouble.contains(tag + " missing")) {
                p.lavaTrouble += tag + " missing at try " + p.tries + "; ";
            }
            return new int[] {0, 0};
        }
        if (h.isBaby()) {
            if (!p.lavaTrouble.contains(tag + " a foal")) {
                p.lavaTrouble += tag + " a foal at try " + p.tries + "; ";
            }
        }
        DebugYardClockwork.Hands hands = DebugYardClockwork.hands(level, h, new ItemStack(Items.BUCKET), false);
        DebugYardClockwork.use(hands, h);
        int filled = hands.count(Items.LAVA_BUCKET) == 1 && hands.count(Items.BUCKET) == 0 ? 1 : 0;
        int recharging = hands.heardAny("yield.recharging") ? 1 : 0;
        if (filled == 0 && recharging == 0 && !p.lavaTrouble.contains(tag + " odd")) {
            p.lavaTrouble += tag + " odd at try " + p.tries + " (" + hands.said() + ", milk "
                    + hands.count(Items.MILK_BUCKET) + ", water " + hands.count(Items.WATER_BUCKET) + "); ";
        }
        return new int[] {filled, recharging};
    }

    private static void lavaJudge(ServerLevel level, Feeding p) {
        String d = p.tries + " tries, one every " + LAVA_EVERY + " ticks: Mlk/Mlk filled " + p.mlkFilled
                + " (recharging " + p.mlkRecharging + ") " + p.mlkPattern + " | control filled " + p.ctrlFilled
                + " (recharging " + p.ctrlRecharging + ") " + p.ctrlPattern + " | L = lava, . = recharging, ? = neither"
                + " | lava cooldown " + MilkGene.LAVA_COOLDOWN_TICKS + " ticks, divided by 1 + extra charges"
                + (p.lavaTrouble.isEmpty() ? "" : " | trouble: " + p.lavaTrouble);
        if (p.lavaTrouble.contains("missing") || p.lavaTrouble.contains("foal")) {
            answer(CHECK_LAVA, null, d);
        } else if (p.ctrlFilled == 0) {
            answer(CHECK_LAVA, p.mlkFilled == 0 ? Boolean.FALSE : null, d + (p.mlkFilled == 0
                    ? " - neither Lava/Lava mare gave lava at all" : " - the control never gave lava, so there is no"
                    + " baseline to beat"));
        } else {
            answer(CHECK_LAVA, p.mlkFilled > p.ctrlFilled, d);
        }
    }

    // ==================================================================
    // Shared
    // ==================================================================

    private static @Nullable Horse find(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof Horse h && h.isAlive() ? h : null;
    }

    /**
     * A glass cell round the inclusive rectangle: walls glass from gy + 1 to {@code top}, the interior cleared to the
     * same height, and - when {@code lid} - a glass lid at gy + 3. The yard's light blocks at gy + 3 are left where
     * they stand, in the lid, the walls and the interior alike: no collision, and the cell stays lit.
     */
    private static void glassCell(ServerLevel level, int gy, Cell c, int top, boolean lid) {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = c.x0(); x <= c.x1(); x++) {
            for (int z = c.z0(); z <= c.z1(); z++) {
                boolean wall = x == c.x0() || x == c.x1() || z == c.z0() || z == c.z1();
                for (int y = gy + 1; y <= top; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (!level.getBlockState(at).is(Blocks.LIGHT)) {
                        DebugPenManager.fastSet(level, at, wall ? glass : air);
                    }
                }
                if (lid) {
                    BlockPos at = new BlockPos(x, gy + 3, z);
                    if (!level.getBlockState(at).is(Blocks.LIGHT)) {
                        DebugPenManager.fastSet(level, at, glass);
                    }
                }
            }
        }
    }

    /** A clock step for this run only, whose throw answers its checks INCONCLUSIVE instead of going silent. */
    private static void at(ServerLevel level, long ticks, int myRun, List<String> checks, Runnable body) {
        DebugYardHerd.after(level, ticks, () -> {
            if (myRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a row AZ step failed", e);
                for (String c : checks) {
                    answer(c, null, "the step reading this check threw " + e);
                }
            }
        });
    }

    private static void answer(String check, @Nullable Boolean pass, String detail) {
        if (!ANSWERED.add(check)) {
            return;
        }
        if (pass == null) {
            DebugYardClockwork.inconclusive(check, detail);
        } else {
            DebugYardClockwork.verdict(check, pass, detail);
        }
    }
}
