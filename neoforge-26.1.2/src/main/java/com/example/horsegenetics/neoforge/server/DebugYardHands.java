package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AH;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AH_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AI;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AI_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows AH-AI: checks that used to need a player's hands</b> (owner, 2026-09-15: "put everything which can be
 * tested unattended into the yard"). Row P's GOLD ANY HEAT, SUBFERTILE GOLD and row O's WEANING wait for somebody with
 * golden carrots and a lead; these do the same thing on a clock. The spawner's sheep need a meal, which only an
 * interaction gives, so a NeoForge {@link FakePlayer} holds the wheat. It can hold an item, but it has no chat and no
 * client - which costs nothing here, because every reading is logged.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>AH</td><td>GOLD TIMER; SUBFERTILE TIMER</td><td>SPAWNER A; SPAWNER B</td></tr>
 *   <tr><td>AI</td><td>WEANING AWAY; WEANING CONTROL</td><td>WEANING HOLD (the away foal)</td></tr>
 * </table>
 */
final class DebugYardHands {

    private DebugYardHands() {
    }

    private static final String FERT = "horsegenetics.fertility=";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            loveClock(level, gy, west, mouthZ + ROW_AH, "GOLD TIMER", FERT + "n/n", true, 5);
            loveClock(level, gy, west + 9, mouthZ + ROW_AH, "SUBFERTILE TIMER", FERT + "sf/sf", false, 12);
            spawner(level, gy, east, mouthZ + ROW_AH, "SPAWNER A", 200);
            spawner(level, gy, east + 9, mouthZ + ROW_AH, "SPAWNER B", 350);
            weaning(level, gy, west, mouthZ + ROW_AI, east + 9, "WEANING AWAY", true);
            weaning(level, gy, west + 9, mouthZ + ROW_AI, 0, "WEANING CONTROL", false);
            ActionTrace.log("test yard", "hands pens built (rows AH-AI: gold timer, subfertile timer, spawner A and B,"
                    + " weaning away and control)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AH-AI (hands) failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Gold, any heat; subfertile gold
    // ------------------------------------------------------------------

    /**
     * What a golden carrot on both horses does, minus the carrot: both put in love on a clock, the vanilla breeding
     * cooldown cleared first, and the pen counted for foals 300 ticks later.
     */
    private static void loveClock(ServerLevel level, int gy, int x0, int z0, String name, String code, boolean outOfHeat,
                                  int tries) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AH_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                outOfHeat ? List.of(name, "mare OUT of heat,", "both in love: a", "foal every time")
                        : List.of(name, "sf/sf, both in", "love twelve times:", "~1 foal in 4"));
        Horse mare = DebugYardUnattended.horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.FEMALE, code, true, name + " MARE");
        Horse stud = DebugYardUnattended.horse(level, gy, x0 + 5.5, z0 + 5.5, Sex.MALE, code, true, name + " STUD");
        if (mare == null || stud == null) {
            return;
        }
        if (outOfHeat) {
            DebugYardFertility.outOfHeat(mare);
        }
        DebugYardFertility.noNaturalCovers(mare);
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AH_D);
        tryLove(level, name, mare, stud, box, 1, tries, outOfHeat, new int[2]);
    }

    private static void tryLove(ServerLevel level, String name, Horse mare, Horse stud, AABB box, int n, int tries,
                                boolean everyTime, int[] tally) {
        DebugYardHerd.after(level, n == 1 ? 200 : 1_200, () -> {
            if (!mare.isAlive() || !stud.isAlive()) {
                ActionTrace.log("test yard", name + ": a parent is gone - stopping at try " + n);
                return;
            }
            boolean inHeat = ReproHandler.receptive(mare);
            mare.setAge(0);
            stud.setAge(0);
            mare.setInLove(null);
            stud.setInLove(null);
            DebugYardHerd.after(level, 300, () -> {
                List<Horse> foals = level.getEntitiesOfClass(Horse.class, box.inflate(0.0, 2.0, 0.0),
                        h -> h.isAlive() && h.isBaby());
                tally[foals.isEmpty() ? 1 : 0]++;
                foals.forEach(Entity::discard);
                String verdict = "";
                if (n == tries) {
                    boolean pass = everyTime ? tally[0] == tries : tally[0] >= 1 && tally[0] <= tries / 2;
                    verdict = " - " + (everyTime ? "expect a foal every try" : "expect about one in four") + ": "
                            + (pass ? "PASS" : "FAIL");
                }
                ActionTrace.log("test yard", name + " try " + n + " (mare " + (inHeat ? "in heat" : "NOT in heat")
                        + "): " + (foals.isEmpty() ? "no foal" : foals.size() + " foal(s)") + " | " + tally[0]
                        + " with a foal, " + tally[1] + " without, so far" + verdict);
                if (n < tries) {
                    tryLove(level, name, mare, stud, box, n + 1, tries, everyTime, tally);
                }
            });
        });
    }

    // ------------------------------------------------------------------
    // The spawner's sheep
    // ------------------------------------------------------------------

    /**
     * Verification &sect;0-BY: "Feed the sheep spawner several times: every sheep the same colour. A different spawner
     * horse should make a different colour (usually)." The summon lands anywhere within 32 blocks, so each meal's sheep
     * are told apart by UUID - the sheep that were not there before it - and the two pens eat 150 ticks apart.
     */
    private static void spawner(ServerLevel level, int gy, int x0, int z0, String name, int firstMeal) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AH_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "Shp/Shp fed wheat", "four times: every", "sheep one colour"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.spawner=Shp/Shp",
                true, name);
        feed(level, name, h, firstMeal, 1, new TreeMap<>());
    }

    private static void feed(ServerLevel level, String name, @Nullable Horse h, int delay, int n,
                             Map<String, Integer> colours) {
        DebugYardHerd.after(level, delay, () -> {
            if (h == null || !h.isAlive()) {
                ActionTrace.log("test yard", name + ": the horse is gone - no meal " + n);
                return;
            }
            AABB near = h.getBoundingBox().inflate(34.0, 6.0, 34.0);
            Set<UUID> before = new HashSet<>();
            for (Sheep s : level.getEntitiesOfClass(Sheep.class, near)) {
                before.add(s.getUUID());
            }
            FakePlayer hands = FakePlayerFactory.getMinecraft(level);
            hands.getAbilities().instabuild = true;      // so the meal counts without the stack shrinking
            hands.snapTo(h.getX(), h.getY(), h.getZ() - 1.5, 0.0F, 0.0F);
            hands.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
            InteractionResult said = CommonHooks.onInteractEntity(hands, h, InteractionHand.MAIN_HAND);
            DebugYardHerd.after(level, 60, () -> {
                Map<String, Integer> meal = new TreeMap<>();
                for (Sheep s : level.getEntitiesOfClass(Sheep.class, near)) {
                    if (s.isAlive() && !before.contains(s.getUUID())) {
                        String c = s.getColor().getSerializedName();
                        meal.merge(c, 1, Integer::sum);
                        colours.merge(c, 1, Integer::sum);
                    }
                }
                String verdict = n < 4 ? "" : " - expect every sheep one colour: "
                        + (colours.size() == 1 ? "PASS" : colours.isEmpty() ? "FAIL (no sheep came)" : "FAIL");
                ActionTrace.log("test yard", name + " meal " + n + " (the interaction said " + said + "): this meal "
                        + (meal.isEmpty() ? "no sheep" : meal) + ", all so far " + colours + verdict);
                if (n < 4) {
                    feed(level, name, h, 240, n + 1, colours);
                }
            });
        });
    }

    // ------------------------------------------------------------------
    // Weaning by distance
    // ------------------------------------------------------------------

    /**
     * Verification &sect;0-DV: a nursing mare's line ends "&bull; nursing", and a foal kept more than 32 blocks away for a
     * breeding day ends it. WEANING AWAY's foal is moved to a holding pen 35 blocks east; WEANING CONTROL's stays at
     * its dam's side. Both pens print the breeding readout at every census, and log the vet's report now and later.
     */
    private static void weaning(ServerLevel level, int gy, int x0, int z0, int holdX, String name, boolean away) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AI_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                away ? List.of(name, "the foal is kept", "35 blocks away: she", "stops nursing")
                        : List.of(name, "the foal stays by", "her side: she", "keeps nursing"));
        Horse mare = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.FEMALE, FERT + "n/n", true, name + " MARE");
        Horse foal = DebugYardUnattended.horse(level, gy, x0 + 5.5, z0 + 5.5, Sex.FEMALE, FERT + "n/n", true, name + " FOAL");
        if (mare == null || foal == null) {
            return;
        }
        foal.setAge(-72_000);
        foal.setData(ModAttachments.HORSE_SOCIAL.get(),
                foal.getData(ModAttachments.HORSE_SOCIAL.get()).withDam(Optional.of(mare.getUUID())));
        ReproHandler.set(mare, ReproHandler.of(mare).foaled(level.getGameTime() - 10, List.of(foal.getUUID())));
        DebugYardFertility.noNaturalCovers(mare);
        DebugWorldWatch.watchBreeding(name, DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AI_D));
        if (away) {
            DebugYardUnattended.pen(level, gy, holdX, z0, 9, ROW_AI_D, "WEANING HOLD", Blocks.GRASS_BLOCK.defaultBlockState(),
                    List.of("WEANING HOLD", "WEANING AWAY's", "foal, kept 35", "blocks from its dam"));
            foal.teleportTo(holdX + 4.5, gy + 1, z0 + 5.5);
        }
        long day = ServerConfig.reproTiming().dayTicks();
        report(level, name, mare, foal, "at the start", 200, away, false);
        report(level, name, mare, foal, "a breeding day and a half later", day + day / 2, away, true);
    }

    private static void report(ServerLevel level, String name, Horse mare, Horse foal, String when, long ticks,
                               boolean away, boolean last) {
        DebugYardHerd.after(level, ticks, () -> {
            if (!mare.isAlive()) {
                ActionTrace.log("test yard", name + ": the mare is gone");
                return;
            }
            double apart = foal.isAlive() ? Math.sqrt(foal.distanceToSqr(mare)) : -1.0;
            ActionTrace.log("test yard", name + " " + when + String.format(" (foal %.0f blocks off): ", apart)
                    + String.join(" / ", ReproHandler.vetReport(mare))
                    + (last ? (away ? " - expect no longer nursing (the watch line has lost '• nursing')"
                    : " - expect still nursing") : ""));
        });
    }
}
