package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
@EventBusSubscriber
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
            spawnerWard(level, gy, east, mouthZ + ROW_AI, "SPAWNER WARD", 260);
            ActionTrace.log("test yard", "hands pens built (rows AH-AI: gold timer, subfertile timer, spawner A and B,"
                    + " weaning away and control, spawner ward)");
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
            // 10 up and down: a summon now lands within GeneAbilityHandler.SPAWN_RISE (8) of the horse's feet.
            AABB near = h.getBoundingBox().inflate(34.0, 10.0, 34.0);
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
                StringBuilder heights = new StringBuilder();
                for (Sheep s : level.getEntitiesOfClass(Sheep.class, near)) {
                    if (s.isAlive() && !before.contains(s.getUUID())) {
                        String c = s.getColor().getSerializedName();
                        meal.merge(c, 1, Integer::sum);
                        colours.merge(c, 1, Integer::sum);
                        heights.append(heights.length() == 0 ? "" : ", ")
                                .append(String.format("%+d", s.getBlockY() - h.getBlockY()));
                        // Counted, then gone: a sheep 32 blocks off lands in some other pen, where it is a blood
                        // horse's prey or a night-shy horse's scare, and none of those pens asked for one.
                        s.discard();
                    }
                }
                String verdict = n < 4 ? "" : " - expect every sheep one colour: "
                        + (colours.size() == 1 ? "PASS" : colours.isEmpty() ? "FAIL (no sheep came)" : "FAIL");
                ActionTrace.log("test yard", name + " meal " + n + " (the interaction said " + said + "): this meal "
                        + (meal.isEmpty() ? "no sheep" : meal) + ", all so far " + colours + verdict
                        + (heights.length() == 0 ? "" : " | heights off the horse " + heights
                        + " (expect within 8; a sheep 'from outOfWorld' is a FAIL)"));
                if (n < 4) {
                    feed(level, name, h, 240, n + 1, colours);
                }
            });
        });
    }

    /**
     * <b>A spawner fed beside a holy ward</b> (checklist audit, 2026-09-15). A summon goes through the NATURAL spawn
     * path, which the ward refuses for anything hostile, so a zombie spawner fed beside a warding horse made nothing
     * whenever the spot fell inside the ward. {@code GeneAbilityHandler.summoning()} lets a summon through now. A summon
     * lands anywhere within 32 blocks and the ward reaches 8 to 16, so the pen also counts zombies that landed within 8
     * of the ward horse: only those prove anything, and eight meals make a run with none unlikely (about one in ten).
     *
     * <h2>Pin the zombies; do not try to time them</h2>
     * <b>Two runs were lost to counting on a tick number.</b> A fed summon is not made during the interaction:
     * {@code GeneAbilityHandler.onFeed} queues a {@code PendingFeed} and {@code drainPendingFeeds} runs the summon on
     * {@code ServerTickEvent.Post}, at the <i>end</i> of the feed's tick. The zombies are then ordinary hostiles
     * carrying no persistence - the summon deliberately does not set it, "it must despawn like anything else" - so
     * vanilla's {@code Mob.checkDespawn} discards them on their first mob tick when no player is near, which in an
     * unattended yard is always. The window they exist in is about one tick wide, so every tick number is a race: the
     * five-tick count read 0 of 16 and blamed the ward (15:36 run), and a same-tick count read 0 of 16 because it ran
     * <i>before</i> the post-tick summon (16:05 run). Both failures belonged to the pen, not to the gene.
     *
     * <p>So the pen stops racing the despawn and removes it. While a meal is armed, {@link #keepWardMealZombies} gives
     * each arriving zombie {@code setPersistenceRequired}, exactly as {@code DebugYardHerd} already does for the DAM
     * DEFENCE zombie. Nothing is cancelled, so {@code [Spawner] made 2 of 2 Zombie} - the measurement itself - is
     * untouched, and the five-tick sweep counts and discards them. The sheep pens never needed this: a vanilla
     * {@code Animal} does not despawn, which is the whole reason this was the only pen that failed.
     */
    private static void spawnerWard(ServerLevel level, int gy, int x0, int z0, String name, int firstMeal) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AI_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "Zmb/Zmb fed beside", "a Hly/Hly ward:", "2 zombies a meal"));
        Horse spawner = DebugYardUnattended.horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.FEMALE,
                "horsegenetics.spawner=Zmb/Zmb", true, name);
        Horse ward = DebugYardUnattended.horse(level, gy, x0 + 5.5, z0 + 5.5, Sex.FEMALE,
                "horsegenetics.holy_ward=Hly/Hly", true, name + " HORSE");
        wardMeal(level, name, spawner, ward, firstMeal, 1, new int[2]);
    }

    private static void wardMeal(ServerLevel level, String name, @Nullable Horse h, @Nullable Horse ward, int delay,
                                 int n, int[] tally) {
        DebugYardHerd.after(level, delay, () -> {
            if (h == null || !h.isAlive() || ward == null || !ward.isAlive()) {
                ActionTrace.log("test yard", name + ": a horse is gone - no meal " + n);
                return;
            }
            AABB near = h.getBoundingBox().inflate(34.0, 10.0, 34.0);
            Set<UUID> before = new HashSet<>();
            for (Entity z : level.getEntities(EntityType.ZOMBIE, near, e -> true)) {
                before.add(z.getUUID());
            }
            FakePlayer hands = FakePlayerFactory.getMinecraft(level);
            hands.getAbilities().instabuild = true;
            hands.snapTo(h.getX(), h.getY(), h.getZ() - 1.5, 0.0F, 0.0F);
            hands.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
            mealArmed = true;       // the summon lands at the END of this tick - see spawnerWard's note
            CommonHooks.onInteractEntity(hands, h, InteractionHand.MAIN_HAND);
            DebugYardHerd.after(level, 5, () -> {
                mealArmed = false;
                int meal = 0;
                int inside = 0;
                for (Entity z : level.getEntities(EntityType.ZOMBIE, near, e -> e.isAlive() && !before.contains(e.getUUID()))) {
                    meal++;
                    if (z.distanceToSqr(ward) <= 8.0 * 8.0) {
                        inside++;
                    }
                    z.discard();
                }
                tally[0] += meal;
                tally[1] += inside;
                String verdict = n < 8 ? "" : " - expect 16 made, some within the ward: "
                        + (tally[0] < 16 ? "FAIL (the ward or something else refused a summon)"
                        : tally[1] == 0 ? "INCONCLUSIVE (none landed inside the ward)" : "PASS");
                ActionTrace.log("test yard", name + " meal " + n + ": " + meal + " zombie(s), " + inside
                        + " within 8 of the ward | " + tally[0] + " made, " + tally[1] + " inside, so far" + verdict);
                if (n < 8) {
                    wardMeal(level, name, h, ward, 240, n + 1, tally);
                }
            });
        });
    }

    /**
     * True from the ward pen's wheat going in until its sweep five ticks later - the window its summons arrive in.
     * Server thread only, like every spawn.
     */
    private static boolean mealArmed;

    /**
     * Keep the ward pen's summons alive long enough to be counted - see {@link #spawnerWard}, where the reasoning is.
     * Persistence <i>is</i> the fix: a summoned zombie has none, and vanilla despawns it on its first mob tick with no
     * player near, sooner than any sweep the pen can schedule. Narrow by construction - it does nothing outside the
     * five ticks after that one pen's own feed, and pinning some other zombie inside them would be harmless.
     */
    @SubscribeEvent
    static void keepWardMealZombies(EntityJoinLevelEvent event) {
        if (mealArmed && !event.getLevel().isClientSide() && event.getEntity() instanceof Zombie zombie) {
            zombie.setPersistenceRequired();
        }
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
            // A PLATFORM 36 UP, NOT A PEN ACROSS THE ROW (09:09 run). The nursing radius is a 3D 32 blocks
            // (ReproHandler.nurse, distanceToSqr), and a holding pen in the east block left the foal 29 blocks from its
            // dam when both leaned on the facing walls - inside the radius, so nothing was tested. Straight up, every
            // position is at least 36 away wherever the two wander.
            int hy = gy + 36;
            for (int x = holdX; x <= holdX + 9; x++) {
                for (int z = z0; z <= z0 + ROW_AI_D; z++) {
                    level.setBlock(new net.minecraft.core.BlockPos(x, hy, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
            DebugTestYard.fencedPlot(level, hy, holdX, holdX + 9, z0, z0 + ROW_AI_D);
            foal.teleportTo(holdX + 4.5, hy + 1, z0 + 5.5);
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
