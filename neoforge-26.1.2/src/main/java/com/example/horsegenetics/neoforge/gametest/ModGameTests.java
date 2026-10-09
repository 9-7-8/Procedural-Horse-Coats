package com.example.horsegenetics.neoforge.gametest;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.DoubleFenceGateBlock;
import com.example.horsegenetics.neoforge.block.DoubleGates;
import com.example.horsegenetics.neoforge.compat.HayBales;
import com.example.horsegenetics.neoforge.compat.WaystonesCompat;
import com.example.horsegenetics.neoforge.data.RidingPassAttachment;
import com.example.horsegenetics.neoforge.server.JockeyPassHandler;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.EnderWhistleCalls;
import com.example.horsegenetics.neoforge.server.HorseLeads;
import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import com.example.horsegenetics.neoforge.server.HorseRealm;
import com.example.horsegenetics.neoforge.server.StasisCare;
import com.example.horsegenetics.neoforge.worldgen.HomesteadCensus;
import com.example.horsegenetics.neoforge.worldgen.StableSiteCensus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <b>The mod's in-game tests.</b>
 *
 * <p>Read this before adding one, because almost everything written about
 * GameTest elsewhere is about a version this is not. <b>26.1.2 has no
 * {@code @GameTest} annotation at all</b> - no {@code @GameTestHolder}, no
 * {@code @PrefixGameTestTemplate}, none of it. A test is an object in the
 * {@code minecraft:test_instance} datapack registry, whose body is a
 * {@code Consumer<GameTestHelper>} held in {@code minecraft:test_function}. So a
 * test is registered in two halves: the function here, and the instance that
 * points at it in {@link #onRegisterGameTests}.
 *
 * <p>Related: the three {@code neoforge.enabledGameTestNamespaces} lines that
 * used to sit in {@code build.gradle} set a property <b>nothing in this version
 * reads</b>. The live one is {@code neoforge.enableGameTest}, which the
 * {@code client} and {@code gameTestServer} run types set themselves.
 *
 * <p>Run them with {@code ./gradlew :neoforge-26.1.2:runGameTest}. It has its
 * own game directory, because every run defaults to {@code run/} and would take
 * the world lock off a client that is already up.
 */
public final class ModGameTests {

    private ModGameTests() {
    }

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, HorseGenetics.MOD_ID);

    /**
     * The smoke test: does any of this reach the runner at all? It asserts
     * something that cannot fail on its own merits, so a red here is the harness
     * and never the mod - which is the only thing worth knowing from the first
     * test in a codebase that has never had one.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HARNESS_REACHES_THE_WORLD =
            TEST_FUNCTIONS.register("harness_reaches_the_world", () -> ModGameTests::harnessReachesTheWorld);

    private static void harnessReachesTheWorld(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 0, 0);
        helper.setBlock(at, Blocks.HAY_BLOCK);
        helper.assertBlockPresent(Blocks.HAY_BLOCK, at);
        helper.succeed();
    }

    /**
     * <b>Redstone moves the pair, not a half.</b> A vanilla fence gate asks only
     * about its own block, so the rule applied unchanged to a two-block gate
     * would swing the powered half and leave the other shut - which is the whole
     * reason {@link DoubleFenceGateBlock#neighborChanged} reads the partner's
     * position as well as its own. Power is put beside <b>one</b> half on
     * purpose; both must open.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DOUBLE_GATE_REDSTONE =
            TEST_FUNCTIONS.register("double_gate_redstone", () -> ModGameTests::doubleGateRedstone);

    private static void doubleGateRedstone(GameTestHelper helper) {
        DoubleFenceGateBlock gate = DoubleGates.gates().get(0).block().get();
        BlockPos left = new BlockPos(0, 0, 0);
        BlockState leftState = gate.defaultBlockState()
                .setValue(DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.LEFT);
        BlockPos right = left.relative(DoubleFenceGateBlock.partnerDirection(leftState));
        BlockState rightState = leftState.setValue(DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.RIGHT);

        helper.setBlock(left, leftState);
        helper.setBlock(right, rightState);
        helper.assertBlockProperty(left, BlockStateProperties.OPEN, false);
        helper.assertBlockProperty(right, BlockStateProperties.OPEN, false);

        // Beside the LEFT half only. If the pair rule is ever lost this still
        // opens that half, so asserting on the right half is what carries the test.
        helper.setBlock(left.above(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(left, BlockStateProperties.OPEN, true);
            helper.assertBlockProperty(right, BlockStateProperties.OPEN, true);
            helper.assertBlockProperty(right, BlockStateProperties.POWERED, true);
        });
    }

    /**
     * <b>Breaking a double gate gives back one gate, not two.</b>
     *
     * <p>A pair is two blocks and a break removes both: the half that was struck,
     * and the orphan its partner becomes, which
     * {@link DoubleFenceGateBlock#updateShape} turns to air. Both removals run
     * the loot table, so the table has to be conditioned on {@code half=left} or
     * the gate duplicates on every break - which it did, in play, until the
     * owner reported it. This is the tripwire, and it is aimed at the
     * <b>data</b>: the condition is written by two separate generators
     * ({@code tools/bake-double-gates.mjs} and {@code compat/GeneratedGates}) and
     * neither the build nor the game says a word if one loses it.
     *
     * <p>Both halves are struck in turn, because they are not symmetric - LEFT is
     * the half that pays, so breaking RIGHT is the case where the drop has to
     * come from the partner's removal instead. Breaking one and calling it done
     * would pass with the condition on the wrong half.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DOUBLE_GATE_DROPS_ONE =
            TEST_FUNCTIONS.register("double_gate_drops_one", () -> ModGameTests::doubleGateDropsOne);

    private static void doubleGateDropsOne(GameTestHelper helper) {
        DoubleFenceGateBlock gate = DoubleGates.gates().get(0).block().get();
        net.minecraft.world.item.Item item = DoubleGates.gates().get(0).item().get();

        for (DoubleFenceGateBlock.Half struck : DoubleFenceGateBlock.Half.values()) {
            BlockPos left = new BlockPos(0, 0, 0);
            BlockState leftState = gate.defaultBlockState()
                    .setValue(DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.LEFT);
            BlockPos right = left.relative(DoubleFenceGateBlock.partnerDirection(leftState));
            helper.setBlock(left, leftState);
            helper.setBlock(right, leftState.setValue(
                    DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.RIGHT));

            // NOT helper.destroyBlock, which passes dropBlock=false and would make
            // this pass however wrong the loot table is.
            BlockPos hit = struck == DoubleFenceGateBlock.Half.LEFT ? left : right;
            helper.getLevel().destroyBlock(helper.absolutePos(hit), true);

            helper.assertBlockPresent(Blocks.AIR, left);
            helper.assertBlockPresent(Blocks.AIR, right);
            // Radius 2 covers both cells, so it counts the pair's whole yield
            // wherever the item landed.
            helper.assertItemEntityCountIs(item, left, 2.0, 1);

            for (net.minecraft.world.entity.item.ItemEntity dropped
                    : helper.getEntities(net.minecraft.world.entity.EntityType.ITEM)) {
                dropped.discard();
            }
        }
        helper.succeed();
    }

    /**
     * <b>The cowboy's homestead still wins village slots.</b> The piece is
     * 16x18 going into a terminator pool vanilla fills with 2x3 stubs, so the
     * bounding-box check is the thing most likely to have quietly stopped
     * passing - and the failure is silent, because a homestead that has become
     * rare looks exactly like a seed that did not roll one.
     *
     * <p><b>It was not rare, and this is how that was settled.</b> The worry had
     * been written down three times as unanswerable without a fresh world and a
     * walk. It is a real measurement instead: {@link HomesteadCensus} runs the
     * village generator over a run of seeds and reads the finished piece lists,
     * without generating a chunk. The assertion is a <b>floor</b> and a
     * deliberately loose one - a guard against the piece falling off a cliff,
     * not a pin holding a rate steady, because a worldgen number pinned exactly
     * gets deleted the first time somebody moves a wall.
     *
     * <p>The sample is small so the suite stays quick. For a real count, raise
     * it: {@code PHC_CENSUS_SEEDS} and {@code PHC_CENSUS_VILLAGES} are read
     * from the environment, so a few hundred villages is one run of
     * {@code runGameTest} with a variable in front of it, and the breakdown
     * lands in the log either way.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOMESTEAD_STILL_GENERATES =
            TEST_FUNCTIONS.register("homestead_still_generates", () -> ModGameTests::homesteadStillGenerates);

    /** Small enough to keep the suite quick; see the note about the environment overrides. */
    private static final int CENSUS_SEEDS = 6;
    private static final int CENSUS_VILLAGES_PER_SEED = 6;

    /**
     * The floor, set well under what was measured, because what this is
     * protecting against is the rate falling off a cliff - and a test that
     * fails on ordinary drift is a test whose number gets edited rather than
     * read.
     *
     * <p><b>Measured 2026-09-18</b>, 40 seeds and 265 plains villages, the same
     * villages both times: <b>93.2%</b> with the piece's original single
     * connector, <b>98.9%</b> with three. Both numbers are the surprise. The
     * standing worry was that the homestead had become rare or stopped
     * generating altogether, and it never had - see
     * {@code wiki/villagers.html#measured}.
     */
    private static final double CENSUS_FLOOR = 0.75;

    private static void homesteadStillGenerates(GameTestHelper helper) {
        int seeds = fromEnv("PHC_CENSUS_SEEDS", CENSUS_SEEDS);
        int villages = fromEnv("PHC_CENSUS_VILLAGES", CENSUS_VILLAGES_PER_SEED);

        HomesteadCensus.Result result = HomesteadCensus.run(helper.getLevel().getServer(), seeds, villages);
        HorseGenetics.LOGGER.info("[census] homestead in plains villages: {}", result.line());

        if (result.villages() == 0) {
            helper.fail("the census found no plains villages at all - the harness is broken, not the piece");
        }
        if (result.rate() < CENSUS_FLOOR) {
            helper.fail(String.format(
                    "the homestead is down to %.1f%% of plains villages (floor %.0f%%): %s",
                    result.rate() * 100.0, CENSUS_FLOOR * 100.0, result.line()));
        }
        helper.succeed();
    }

    /**
     * <b>No generated stable stands on ground spreading more than ten blocks</b>
     * (issue #2). The jigsaw gave a stable one height, the surface under its
     * centre, so a big one started on a hilltop floated over the valley beside it.
     * {@link StableSiteCensus} generates every stable chunk near spawn twice on
     * the same seeds: the wrapped vanilla jigsaw as the control, and the real
     * structure. It re-measures the ground under each stable that was kept.
     *
     * <p>The limit is written here rather than read from the structure, so
     * loosening {@code max_ground_spread} in the JSON turns this red. The census
     * size can be raised with {@code PHC_STABLE_SEEDS} / {@code PHC_STABLE_SITES}.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STABLES_STAND_ON_LEVEL_GROUND =
            TEST_FUNCTIONS.register("stables_stand_on_level_ground", () -> ModGameTests::stablesStandOnLevelGround);

    private static final int STABLE_GROUND_LIMIT = 10;

    private static void stablesStandOnLevelGround(GameTestHelper helper) {
        int seeds = fromEnv("PHC_STABLE_SEEDS", 4);
        int sites = fromEnv("PHC_STABLE_SITES", 6);
        StableSiteCensus.Result result = StableSiteCensus.run(
                helper.getLevel().getServer(), seeds, sites, STABLE_GROUND_LIMIT);
        HorseGenetics.LOGGER.info("[census] stable ground: {}", result.line());
        HorseGenetics.LOGGER.info("[census] stable ground spread before, per stable: {}", result.spreads());

        if (result.sites() == 0) {
            helper.fail("the census found no stable sites at all - the harness is broken, not the stables");
        }
        if (result.accepted() == 0) {
            helper.fail("no stable was kept anywhere: " + result.line());
        }
        if (result.worstAfter() > STABLE_GROUND_LIMIT) {
            helper.fail("a stable stands on ground spreading " + result.worstAfter()
                    + " blocks (limit " + STABLE_GROUND_LIMIT + "): " + result.line());
        }
        helper.succeed();
    }

    /**
     * <b>Two vanilla fence gates in a grid really do resolve to our double gate.</b>
     *
     * <p>This recipe is the <a href="items.html#gate-exception">one exception</a> to
     * the house rule that every recipe in the mod carries a modded ingredient. The
     * rule was the guard against a collision - two recipes with the same inputs
     * are not an error in Minecraft, the manager simply resolves one of them and
     * the other item becomes uncraftable with <b>nothing logged</b>. Outside the
     * rule, the only guard left is this test.
     *
     * <p>So it does not ask whether the JSON parsed - {@code every_recipe_encodes}
     * covers that. It puts two oak fence gates in a crafting grid and asserts the
     * <em>server's own recipe manager</em> hands back our gate, which is the thing
     * a player actually does. A pack whose recipe wins instead fails here.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DOUBLE_GATE_CRAFTS =
            TEST_FUNCTIONS.register("double_gate_crafts", () -> ModGameTests::doubleGateCrafts);

    private static void doubleGateCrafts(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        net.minecraft.world.item.Item want = DoubleGates.gates().get(0).item().get();

        // A 2x1 grid of oak fence gates - the whole recipe.
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                        List.of(new ItemStack(Items.OAK_FENCE_GATE), new ItemStack(Items.OAK_FENCE_GATE)));

        // assemble(input) - one argument in 26.1.2, no RegistryAccess.
        ItemStack out = server.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .map(r -> r.value().assemble(input))
                .orElse(ItemStack.EMPTY);

        if (out.isEmpty()) {
            helper.fail("two oak fence gates craft nothing - the double gate recipe did not resolve");
        } else if (!out.is(want)) {
            // The collision case the house rule used to make impossible.
            helper.fail("two oak fence gates craft " + out.getItem() + ", not the double gate -"
                    + " something else claims those inputs");
        }
        helper.succeed();
    }

    /**
     * <b>Every stasis chamber crafts, and every one of them can be shown to a
     * player.</b>
     *
     * <p><b>Crafting was never the part that was broken</b>, which is why this
     * test is mostly not about crafting. The family shipped in v0.5.024 and
     * again in v0.5.026 making the right items from the right grids the whole
     * time; what it could not do was tell anybody the grids. A test that laid
     * out the ingredients and checked the output passed through both releases.
     *
     * <p>{@code CustomRecipe} defaults three separate flags the wrong way for a
     * recipe meant to be seen - {@code isSpecial() == true},
     * {@code placementInfo() == NOT_PLACEABLE}, and (inherited from
     * {@code Recipe}) {@code display() == List.of()}. They answer three
     * different questions: is it listed, can it be placed for you, and what is
     * drawn. v0.5.026 fixed the first two, the owner's report came back word for
     * word unchanged, and the third turned out to be the one that mattered.
     * {@link #craftsInto} now asserts all three.
     *
     * <p>Since then each chamber has <b>two</b> recipes: a plain JSON one for an
     * empty chamber, which is what a player is shown, and a Java one for a
     * chamber with a horse in it, which is deliberately undrawn and carries the
     * animal across. {@link #carriesHorse} covers that half, and it is the
     * assertion with real teeth - the others failing means an item nobody can
     * craft, and that one failing means a pedigreed horse deleted in silence.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STASIS_CHAMBERS_CRAFT =
            TEST_FUNCTIONS.register("stasis_chambers_craft", () -> ModGameTests::stasisChambersCraft);

    private static void stasisChambersCraft(GameTestHelper helper) {
        ItemStack basic = new ItemStack(ModItems.BASIC_STASIS_CHAMBER.get());
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);

        // HALF ONE: the empty grids. These are the plain JSON recipes, and they
        // are the ones a player is ever actually shown, so they carry the whole
        // showable-to-a-player assertion.
        craftsInto(helper, "the Basic chamber",
                CraftingInput.of(2, 2, List.of(
                        new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.WHEAT),
                        new ItemStack(ModItems.HORSE_HAIR.get()), new ItemStack(Items.WATER_BUCKET))),
                ModItems.BASIC_STASIS_CHAMBER.get());

        craftsInto(helper, "the Intermediate upgrade",
                CraftingInput.of(2, 1, List.of(basic, new ItemStack(Items.BOOK))),
                ModItems.INTERMEDIATE_STASIS_CHAMBER.get());

        craftsInto(helper, "the Advanced upgrade",
                CraftingInput.of(2, 1, List.of(
                        new ItemStack(ModItems.INTERMEDIATE_STASIS_CHAMBER.get()),
                        new ItemStack(Items.GOLD_INGOT))),
                ModItems.ADVANCED_STASIS_CHAMBER.get());

        craftsInto(helper, "the Spacer upgrade",
                CraftingInput.of(2, 1, List.of(
                        new ItemStack(ModItems.ADVANCED_STASIS_CHAMBER.get()),
                        new ItemStack(Items.DIAMOND))),
                ModItems.SPACER_STASIS_CHAMBER.get());

        craftsInto(helper, "the Emergency chamber",
                CraftingInput.of(3, 3, List.of(
                        pearl, pearl, pearl,
                        pearl, basic, pearl,
                        pearl, pearl, pearl)),
                ModItems.EMERGENCY_STASIS_CHAMBER.get());

        // HALF TWO: the same four grids with a horse in the chamber. These are
        // the Java recipes, and what they have to prove is the opposite thing -
        // not that they can be drawn (they deliberately cannot) but that the
        // animal comes out the other side. A JSON recipe reaching one of these
        // grids would craft the right item and silently delete the horse, which
        // is exactly the failure the component ingredient exists to prevent.
        carriesHorse(helper, "the Intermediate upgrade",
                input -> CraftingInput.of(2, 1, List.of(input, new ItemStack(Items.BOOK))),
                ModItems.BASIC_STASIS_CHAMBER.get(),
                ModItems.OCCUPIED_INTERMEDIATE_STASIS_CHAMBER.get());

        carriesHorse(helper, "the Advanced upgrade",
                input -> CraftingInput.of(2, 1, List.of(input, new ItemStack(Items.GOLD_INGOT))),
                ModItems.INTERMEDIATE_STASIS_CHAMBER.get(),
                ModItems.OCCUPIED_ADVANCED_STASIS_CHAMBER.get());

        carriesHorse(helper, "the Spacer upgrade",
                input -> CraftingInput.of(2, 1, List.of(input, new ItemStack(Items.DIAMOND))),
                ModItems.ADVANCED_STASIS_CHAMBER.get(),
                ModItems.OCCUPIED_SPACER_STASIS_CHAMBER.get());

        carriesHorse(helper, "the Emergency chamber",
                input -> CraftingInput.of(3, 3, List.of(
                        new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_PEARL),
                        input,
                        new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_PEARL))),
                ModItems.BASIC_STASIS_CHAMBER.get(),
                ModItems.OCCUPIED_EMERGENCY_STASIS_CHAMBER.get());

        // AND THE TWO BASIC RECIPES MUST NOT BOTH WANT THE VANILLA GRID.
        // StasisChamberRecipe subtracts minecraft:water_bucket from its tag for
        // exactly this reason. If that subtraction is ever dropped, both it and
        // the JSON twin match the grid above, the recipe manager picks one by
        // registry order, and the other becomes unreachable - with nothing red
        // anywhere. The grid would still craft, so no assertion above can see it.
        if (com.example.horsegenetics.neoforge.server.recipe.StasisChamberRecipe.INSTANCE.matches(
                CraftingInput.of(2, 2, List.of(
                        new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.WHEAT),
                        new ItemStack(ModItems.HORSE_HAIR.get()), new ItemStack(Items.WATER_BUCKET))),
                helper.getLevel())) {
            helper.fail("the modded-water chamber recipe also claims the plain vanilla grid - "
                    + "it and recipe/basic_stasis_chamber.json now collide, and one of them is "
                    + "unreachable depending on registry order");
        }

        helper.succeed();
    }

    /**
     * Asserts one grid makes one item - and that the recipe behind it is one a
     * player could have been shown.
     */
    private static void craftsInto(GameTestHelper helper, String what,
                                   CraftingInput input, net.minecraft.world.item.Item want) {
        MinecraftServer server = helper.getLevel().getServer();
        RecipeHolder<?> found = server.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        if (found == null) {
            helper.fail(what + ": that grid resolves to no recipe at all");
            return;
        }

        ItemStack out = ((Recipe<CraftingInput>) found.value()).assemble(input);
        if (!out.is(want)) {
            helper.fail(what + ": that grid crafts " + out.getItem() + ", not " + want);
        }

        // The half that was actually broken - three times over, because there
        // are three separate flags and the family has now shipped failing two
        // of them in two different releases.
        Recipe<?> recipe = found.value();
        if (recipe.isSpecial()) {
            helper.fail(what + ": the recipe is special, so the recipe book and JEI both "
                    + "skip it - the item is craftable and undiscoverable");
        }
        if (recipe.placementInfo().isImpossibleToPlace()) {
            helper.fail(what + ": the recipe advertises no ingredients, so nothing can draw it");
        }
        // v0.5.026 shipped with the two above correct and this one still wrong,
        // which is why the owner's report came back unchanged. Recipe.display()
        // defaults to List.of() and CustomRecipe never overrides it; a recipe
        // with no display is one the recipe book has no entry to draw, however
        // findable and placeable it claims to be.
        if (recipe.display().isEmpty()) {
            helper.fail(what + ": the recipe produces no RecipeDisplay, so the recipe book has "
                    + "nothing to draw - this is the flag v0.5.026 still had wrong");
        }
    }

    /**
     * <b>A horse in the chamber survives the grid.</b> Asserts that crafting an
     * <i>occupied</i> chamber resolves, makes the right item, and carries the
     * {@code stasis_snapshot} across.
     *
     * <p>The last of those is the one with consequences: every other assertion
     * in this test failing leaves a player unable to craft something, and this
     * one failing eats a pedigreed animal with no message and nothing in the
     * log. It is also the assertion that guards the JSON twins - if one of them
     * ever widens far enough to claim an occupied grid, it will win the
     * resolution here and this will go red.
     *
     * <p>The snapshot is built by hand rather than by capturing a real horse:
     * what is under test is the recipe carrying a component, not
     * {@code HorseStasisHandler} making a good one, and a real capture would
     * need the horse to be founded first - ten ticks this test does not have.
     */
    private static void carriesHorse(GameTestHelper helper, String what,
                                     java.util.function.Function<ItemStack, CraftingInput> grid,
                                     net.minecraft.world.item.Item from,
                                     net.minecraft.world.item.Item want) {
        com.example.horsegenetics.neoforge.data.StasisSnapshot snapshot =
                new com.example.horsegenetics.neoforge.data.StasisSnapshot(
                        "Tripwire", java.util.UUID.randomUUID(), new net.minecraft.nbt.CompoundTag());
        // A chamber with a horse in it is a DIFFERENT ITEM, so this is what the
        // occupied half of each rung actually takes. Building it by hand through
        // withHorse is also the cheapest check that the pairing is wired up: if
        // occupiedChamber() does not know this item, the stack below is still the
        // empty one and every assertion that follows fails.
        ItemStack occupied = com.example.horsegenetics.neoforge.item.StasisChamberItem
                .withHorse(new ItemStack(from), snapshot);
        if (!(occupied.getItem() instanceof com.example.horsegenetics.neoforge.item.StasisChamberItem held
                && held.occupied())) {
            helper.fail(what + ": " + from + " has no occupied twin registered, so a horse put "
                    + "into it would stay in the empty item and the plain JSON recipe would eat it");
            return;
        }

        CraftingInput input = grid.apply(occupied);
        MinecraftServer server = helper.getLevel().getServer();
        RecipeHolder<?> found = server.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        if (found == null) {
            helper.fail(what + ", with a horse inside: that grid resolves to no recipe at all");
            return;
        }

        ItemStack out = ((Recipe<CraftingInput>) found.value()).assemble(input);
        if (!out.is(want)) {
            helper.fail(what + ", with a horse inside: that grid crafts " + out.getItem()
                    + ", not " + want);
            return;
        }

        com.example.horsegenetics.neoforge.data.StasisSnapshot carried =
                out.get(com.example.horsegenetics.neoforge.data.ModDataComponents.STASIS_SNAPSHOT.get());
        if (carried == null) {
            helper.fail(what + ", with a horse inside: the result carries no stasis_snapshot, so "
                    + "the horse was deleted by crafting - a JSON recipe has claimed this grid");
            return;
        }
        if (!carried.horseId().equals(snapshot.horseId())) {
            helper.fail(what + ", with a horse inside: the result carries a different horse");
        }
        // And the result must itself be an occupied item, or the horse is riding
        // in a stack that every plain recipe downstream is free to eat.
        if (!(out.getItem() instanceof com.example.horsegenetics.neoforge.item.StasisChamberItem made
                && made.occupied())) {
            helper.fail(what + ", with a horse inside: the result holds the horse but is the EMPTY "
                    + "item, so the next upgrade's plain recipe would destroy it");
        }
    }

    /**
     * <b>Every recipe the server would send a joining client actually encodes.</b>
     *
     * <p>This is the cheapest possible version of "a player joins a dedicated
     * server", and it exists because the expensive version is the only other
     * one. NeoForge sends the entire recipe set to every joining client as a
     * single {@code neoforge:recipe_content} payload; <b>one recipe that will
     * not encode fails the payload, the packet and the login</b>, so the symptom
     * is not a missing recipe but every player being kicked with
     * {@code EncoderException} before terrain loads.
     *
     * <p><b>Singleplayer never encodes it</b> - there is no packet - which is
     * exactly how v0.5.014 shipped with both of this mod's special recipes
     * unencodable. {@code StreamCodec.unit} writes no bytes and instead checks
     * that the value you gave it {@code equals} the one it captured; build the
     * map codec from a supplier and the {@code RecipeManager} decodes a
     * different object every time, so that check can never pass. See
     * {@link com.example.horsegenetics.neoforge.server.recipe.CarrotCombineRecipe#INSTANCE}.
     *
     * <p>It encodes through {@link Recipe#STREAM_CODEC}, which is the same
     * dispatch-on-serializer the sync itself uses, rather than reaching for each
     * serializer by hand - so it tests the real path and not a reconstruction of
     * it. Every recipe is covered, not only this mod's: the failure is a
     * property of the packet, and a recipe from anywhere kicks the same player.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EVERY_RECIPE_ENCODES =
            TEST_FUNCTIONS.register("every_recipe_encodes", () -> ModGameTests::everyRecipeEncodes);

    private static void everyRecipeEncodes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> broken = new ArrayList<>();
        int checked = 0;

        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            checked++;
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess(), ConnectionType.NEOFORGE);
            try {
                Recipe.STREAM_CODEC.encode(buf, holder.value());
            } catch (RuntimeException wontEncode) {
                broken.add(holder.id().identifier() + " - " + wontEncode);
            } finally {
                buf.release();
            }
        }

        if (checked == 0) {
            helper.fail("no recipes were loaded at all - the harness is broken, not the recipes");
        }
        if (!broken.isEmpty()) {
            helper.fail(broken.size() + " of " + checked + " recipes will not encode, so every client is "
                    + "kicked on join: " + String.join("; ", broken));
        }
        HorseGenetics.LOGGER.info("[gametest] all {} recipes encode for the join packet", checked);
        helper.succeed();
    }

    /**
     * <b>Building Blocks still has things in it, and our gates are among them.</b>
     *
     * <p>The tab is built by an event every mod may write into, and
     * {@code CreativeModeTab.buildContents} assigns the finished list
     * <i>after</i> that event returns. <b>So a listener that throws does not
     * lose its own item - it leaves the tab holding what it had before, which on
     * a first build is nothing.</b> One mod's bad anchor empties Building Blocks
     * for vanilla items too, with no crash and nothing in the log; the player
     * sees a whole category missing and has no way to tell who did it. v0.5.014
     * did exactly that to anyone with Regions Unexplored installed.
     *
     * <p>So the assertion that matters is the <b>blunt</b> one: the tab is not
     * empty. Our own gates being present is the second check and the weaker one
     * &mdash; it is the empty tab that is the disaster.
     *
     * <p>This cannot reproduce the original anchor, which needed a mod that is
     * not here. What it does catch is the shape: any future throw out of
     * {@code addToCreativeTab}, from any cause, on the one tab this mod writes
     * into.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILDING_BLOCKS_SURVIVES =
            TEST_FUNCTIONS.register("building_blocks_survives", () -> ModGameTests::buildingBlocksSurvives);

    private static void buildingBlocksSurvives(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        // Normally only the client ever asks for this. It is plain data, so a
        // server can ask too - which is the whole reason this test is cheap.
        CreativeModeTabs.tryRebuildTabContents(
                server.getWorldData().enabledFeatures(), true, server.registryAccess());

        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.BUILDING_BLOCKS);
        if (tab == null) {
            helper.fail("there is no Building Blocks tab at all - the harness is broken, not the tab");
            return;
        }
        Collection<ItemStack> contents = tab.getDisplayItems();
        if (contents.isEmpty()) {
            helper.fail("Building Blocks is EMPTY - a listener threw and the tab kept its previous "
                    + "(unbuilt) contents. Every vanilla block is gone from the menu, not just ours.");
        }

        List<String> missing = new ArrayList<>();
        for (com.example.horsegenetics.neoforge.block.DoubleGates.Gate gate
                : com.example.horsegenetics.neoforge.block.DoubleGates.gates()) {
            net.minecraft.world.item.Item ours = gate.item().get();
            if (contents.stream().noneMatch(stack -> stack.is(ours))) {
                missing.add(gate.item().getId().toString());
            }
        }
        if (!missing.isEmpty()) {
            helper.fail(missing.size() + " double gates are not in Building Blocks: "
                    + String.join(", ", missing));
        }
        HorseGenetics.LOGGER.info("[gametest] Building Blocks holds {} stacks, including all {} double gates",
                contents.size(), com.example.horsegenetics.neoforge.block.DoubleGates.gates().size());
        helper.succeed();
    }

    private static int fromEnv(String name, int fallback) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            HorseGenetics.LOGGER.warn("[census] {} is not a number: {}", name, raw);
            return fallback;
        }
    }

    /**
     * <b>The realm's origin cell comes out of the ground the shape it is supposed
     * to be.</b> Its water where the water goes, its exit lit, and its edge one
     * block outside the field.
     *
     * <p>Almost everything about {@code HorseRealm} is arithmetic on a chunk
     * position, and arithmetic that is wrong by one produces a field that looks
     * entirely convincing until somebody walks 1 600 blocks to an exit that is not
     * there. None of it is visible from a diff, and all of it is a pure function
     * of the chunk position, which is exactly the shape a test is good at.
     *
     * <p><b>It usually runs in the Overworld, and that is not a mistake.</b>
     * {@code GameTestServer} builds its world with a hand-written three-dimension
     * list and never reads the datapack's {@code dimension/} folder, so the realm
     * <i>cannot</i> exist in this harness however correct its JSON is - the log
     * shows the runner saving the overworld, the nether and the end and nothing
     * else. {@link HorseRealmTerrain} does not care which level it writes into, so
     * the geometry is asserted against whatever level is available and the two
     * ground assertions, which belong to the dimension's flat generator rather
     * than to this code, are asked only when the real one is there.
     *
     * <p>That leaves one thing this cannot see: whether the dimension loads at
     * all. {@code runServer} answers that - its log names
     * {@code ServerLevel[world]/horsegenetics:horse_realm} - and it is the check
     * to repeat after any edit to the three JSON files, because a typo in them is
     * silent everywhere else.
     *
     * <p>The decoration is invoked directly rather than waited for. In a running
     * game it is queued from {@code ChunkEvent.Load} and applied on the next
     * server tick; here that would cost a tick this test does not have, and the
     * thing under test is what {@code HorseRealmTerrain} writes, not when the
     * queue drains. It is idempotent by design, so calling it is safe whether or
     * not the event already did.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HORSE_REALM_IS_BUILT =
            TEST_FUNCTIONS.register("horse_realm_is_built", () -> ModGameTests::horseRealmIsBuilt);

    private static void horseRealmIsBuilt(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        net.minecraft.server.level.ServerLevel realm =
                server.getLevel(com.example.horsegenetics.neoforge.server.HorseRealm.REALM_LEVEL);
        boolean theRealThing = realm != null;
        if (!theRealThing) {
            realm = server.overworld();     // see the note above - the harness has no realm
        }

        // The origin chunk is the exit AND a pool, which is why it is the one
        // worth generating. The wall is no longer anywhere near it: the field is
        // a circle round the origin now, so the perimeter is a whole radius
        // away and needs a chunk of its own.
        int radius = com.example.horsegenetics.neoforge.data.HorseRealmSize.get(server).radius();
        int wallChunk = radius / 16;
        net.minecraft.world.level.ChunkPos cell = new net.minecraft.world.level.ChunkPos(0, 0);
        net.minecraft.world.level.ChunkPos edge =
                new net.minecraft.world.level.ChunkPos(wallChunk, 0);
        realm.getChunk(cell.x(), cell.z());
        realm.getChunk(edge.x(), edge.z());
        com.example.horsegenetics.neoforge.server.HorseRealmTerrain.decorateNow(realm, cell);
        com.example.horsegenetics.neoforge.server.HorseRealmTerrain.decorateNow(realm, edge);

        // 1. The ground. Bedrock at -1 and one dirt layer at 0 - away from the pool
        // at the cell origin, which replaces it. This one belongs to the flat
        // generator in dimension/horse_realm.json rather than to any Java here, so
        // it is asked only of the real dimension; in the harness's overworld it
        // would be asserting something about a plains biome.
        if (theRealThing) {
            BlockPos onGround = new BlockPos(40, HorseRealm.GROUND_Y, 40);
            assertRealmBlock(helper, realm, onGround.below(), Blocks.BEDROCK, "the floor");
            assertRealmBlock(helper, realm, onGround, Blocks.GRASS_BLOCK, "the surface");
        }

        // 2. The water. A hurt horse cannot heal away from it, so a field with no
        // pools is a field that quietly sterilises everything in it (gap 258).
        assertRealmBlock(helper, realm, new BlockPos(0, HorseRealm.GROUND_Y, 0),
                Blocks.WATER, "the cell's pool");
        assertRealmBlock(helper, realm, new BlockPos(1, HorseRealm.GROUND_Y, 1),
                Blocks.WATER, "the cell's pool");

        // 3. The exit, lit. An unlit frame is a hay wall.
        BlockPos portal = new BlockPos(HorseRealm.PORTAL_DX + 1, HorseRealm.STAND_Y,
                HorseRealm.PORTAL_DZ);
        if (!realm.getBlockState(portal).is(
                com.example.horsegenetics.neoforge.block.ModBlocks.HAY_PORTAL.get())) {
            helper.fail("the realm's exit at " + portal.toShortString() + " is "
                    + realm.getBlockState(portal).getBlock() + ", not a lit portal");
        }

        // 4. The edge, and tall enough to matter. Found rather than guessed at:
        // the wall is a circle, so "one block outside the field" is a column the
        // geometry has to name. It has to lie in the chunk decorated above,
        // since the wall is built per chunk and asserting a column the test
        // never asked for reads as a missing wall.
        BlockPos wall = null;
        for (int x = edge.getMinBlockX(); x <= edge.getMaxBlockX() && wall == null; x++) {
            for (int z = edge.getMinBlockZ(); z <= edge.getMaxBlockZ(); z++) {
                if (HorseRealm.isWall(x, z, radius)) {
                    wall = new BlockPos(x, HorseRealm.GROUND_Y, z);
                    break;
                }
            }
        }
        if (wall == null) {
            helper.fail("no column of chunk " + edge + " is wall at radius " + radius
                    + " - the test is looking in the wrong place, or isWall is");
            return;
        }
        assertRealmBlock(helper, realm, wall, Blocks.BARRIER, "the perimeter");
        assertRealmBlock(helper, realm,
                wall.above(HorseRealm.WALL_HEIGHT - 1),
                Blocks.BARRIER, "the top of the perimeter");

        // ...and the middle of the field is NOT walled. The old square's west
        // wall ran along x = -1, which is now well inside the circle; a barrier
        // there would be an invisible fence a few steps from the portal.
        assertRealmBlock(helper, realm, new BlockPos(-1, HorseRealm.GROUND_Y, 8),
                Blocks.AIR, "the old square's wall, inside the new field");

        // 5. None of it anywhere else. The debug corridor generates a single air
        // layer and builds its own floor; a pool or a barrier turning up in it
        // would mean a rule written against a Level rather than against a
        // dimension, which is the failure this whole split exists to avoid.
        net.minecraft.server.level.ServerLevel debug = server.getLevel(
                com.example.horsegenetics.neoforge.server.DebugPenManager.DEBUG_LEVEL);
        if (debug != null && debug.getBlockState(new BlockPos(0, 0, 0)).is(Blocks.WATER)) {
            helper.fail("the realm's water grid reached the debug dimension");
        }
        helper.succeed();
    }

    private static void assertRealmBlock(GameTestHelper helper, net.minecraft.server.level.ServerLevel realm,
                                         BlockPos at, net.minecraft.world.level.block.Block want, String what) {
        BlockState found = realm.getBlockState(at);
        if (!found.is(want)) {
            helper.fail(what + " at " + at.toShortString() + " is " + found.getBlock() + ", not " + want);
        }
    }

    /**
     * <b>Every Overworld portal lands at the middle exit, and the middle exit is
     * a chunk that actually carries one.</b>
     *
     * <p>This test's assertion is now the <i>reverse</i> of the one it was
     * written with, and that is the interesting thing about it. The hundred exits
     * used to be handed out by hashing the portal's position, and this asserted
     * the hash <b>spread</b> - a collapse onto a handful of cells would have put
     * a whole server in one corner of a 16 000-block field and looked, from
     * inside, exactly like a popular meeting spot. It turned out that spreading
     * arrivals scattered the <i>horses</i>, which is the one thing the realm
     * exists to keep together, so everybody arrives at the centre now and the
     * property to defend is that they all still do.
     *
     * <p>Kept rather than deleted for that reason: a hash creeping back in would
     * otherwise be noticed only by a player wondering where the herd went.
     *
     * <p>It now also carries <b>the field's own shape</b>: that the size rule
     * never shrinks, never goes under the floor and always covers its own
     * density promise, and that the circular wall is watertight against a
     * diagonal. All of those are silent when broken - a field that shrinks
     * strands horses outside their own wall, and a wall with corner gaps is a
     * bounded dimension that is not bounded.
     *
     * <p>Pure arithmetic, so it needs no world; it is here rather than in JUnit
     * only because the NeoForge module has no Minecraft on its test classpath and
     * {@code BlockPos} is Minecraft's.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HORSE_REALM_GRID_IS_STABLE =
            TEST_FUNCTIONS.register("horse_realm_grid_is_stable", () -> ModGameTests::horseRealmGridIsStable);

    private static void horseRealmGridIsStable(GameTestHelper helper) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.Random rng = new java.util.Random(20260925L);

        for (int i = 0; i < 4000; i++) {
            BlockPos portal = new BlockPos(rng.nextInt(-2_000_000, 2_000_000),
                    rng.nextInt(-60, 300), rng.nextInt(-2_000_000, 2_000_000));
            net.minecraft.world.level.ChunkPos cell = HorseRealm.arrivalCell(portal);

            if (!HorseRealm.isPortalChunk(cell.x(), cell.z())) {
                helper.fail("a portal at " + portal.toShortString() + " arrives at chunk "
                        + cell + ", which carries no exit");
                return;
            }
            seen.add(cell.pack());
        }

        // ONE cell, and it is the middle one. This assertion is the exact
        // reverse of the one it replaces - the grid used to be asked to spread
        // arrivals evenly and is now asked to put every one of them in the same
        // place, because spreading them scattered the HORSES (see the realm
        // page). Left as a test rather than deleted: "everybody lands together"
        // is now the load-bearing property, and a hash creeping back in would
        // otherwise be noticed only by a player wondering where the herd went.
        if (seen.size() != 1) {
            helper.fail("4 000 portals reached " + seen.size() + " different exits - every one of "
                    + "them is supposed to arrive at the middle of the field");
            return;
        }
        net.minecraft.world.level.ChunkPos only = net.minecraft.world.level.ChunkPos.unpack(
                seen.iterator().next());
        if (only.x() != 0 || only.z() != 0) {
            helper.fail("portals arrive at " + only + ", not at the origin");
            return;
        }

        // --- the field's shape and how it grows -------------------------
        // Pure arithmetic and the cheapest thing in this file, but every one of
        // these is silent when broken: a field that can shrink strands horses
        // outside their own wall, and a wall with a diagonal gap in it is a
        // bounded dimension that is not bounded.
        int start = HorseRealmSize.START_RADIUS;
        if (HorseRealmSize.radiusFor(0) != start || HorseRealmSize.radiusFor(1) != start) {
            helper.fail("an empty field is not the starting size");
            return;
        }
        int last = start;
        for (int horses = 0; horses <= 40_000; horses += 137) {
            int r = HorseRealmSize.radiusFor(horses);
            if (r < start) {
                helper.fail(horses + " horses gave a radius of " + r + ", under the floor of " + start);
                return;
            }
            if (r < last) {
                helper.fail("the field shrank: " + horses + " horses want " + r
                        + " after a smaller count wanted " + last);
                return;
            }
            // The density it promises: area must cover every horse.
            double area = Math.PI * (double) r * r;
            if (area < (double) horses * HorseRealmSize.BLOCKS_PER_HORSE) {
                helper.fail(horses + " horses in a radius of " + r + " is denser than the rule allows");
                return;
            }
            last = r;
        }

        // The wall is watertight against a diagonal. Walk the boundary band and
        // check no in-bounds column touches an out-of-bounds one without a wall
        // column between them - eight-neighbour, because entities move
        // continuously and a four-neighbour ring has corner gaps.
        int r = 200;
        for (int x = -r - 3; x <= r + 3; x++) {
            for (int z = -r - 3; z <= r + 3; z++) {
                if (!HorseRealm.inBounds(x, z, r)) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = x + dx;
                        int nz = z + dz;
                        if (!HorseRealm.inBounds(nx, nz, r) && !HorseRealm.isWall(nx, nz, r)) {
                            helper.fail("a gap in the wall: (" + x + ", " + z + ") is field and its "
                                    + "neighbour (" + nx + ", " + nz + ") is neither field nor wall");
                            return;
                        }
                    }
                }
            }
        }

        // And the one exit is inside the field it is the exit from.
        if (!HorseRealm.inBounds(HorseRealm.arrivalBlock().getX(),
                HorseRealm.arrivalBlock().getZ(), start)) {
            helper.fail("the arrival spot is outside the starting field");
            return;
        }
        helper.succeed();
    }

    /**
     * <b>The realm lift loads no chunk but its own.</b> Issue #13: the live
     * server logged {@code HorseRealmLift.lift} forcing a synchronous chunk load
     * 101 times. Not by reading - every block it reads is in the chunk it is
     * lifting - but by writing: {@code setBlock} without flag 16 asks each
     * neighbour to re-shape, and the neighbour of an edge block is in the next
     * chunk over, which nothing had loaded.
     *
     * <p>So: a chunk nobody has a ticket on, given the old floor's bedrock mark
     * and a ring of stone round its edge, lifted through the real
     * {@link com.example.horsegenetics.neoforge.server.HorseRealmLift#liftNow}
     * in whatever level the harness has (the lift does not ask which). All four
     * neighbours must still be unloaded afterwards. The setup writes with 16 for
     * the same reason the fix does, or it would load them itself.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REALM_LIFT_LOADS_NO_NEIGHBOUR =
            TEST_FUNCTIONS.register("realm_lift_loads_no_neighbour", () -> ModGameTests::realmLiftLoadsNoNeighbour);

    private static void realmLiftLoadsNoNeighbour(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        // A long way from the test structures and the spawn chunks.
        net.minecraft.world.level.ChunkPos at = new net.minecraft.world.level.ChunkPos(-3907, 4111);
        net.minecraft.world.level.ChunkPos[] around = {
                new net.minecraft.world.level.ChunkPos(at.x() - 1, at.z()),
                new net.minecraft.world.level.ChunkPos(at.x() + 1, at.z()),
                new net.minecraft.world.level.ChunkPos(at.x(), at.z() - 1),
                new net.minecraft.world.level.ChunkPos(at.x(), at.z() + 1)};
        level.getChunk(at.x(), at.z());
        for (net.minecraft.world.level.ChunkPos n : around) {
            if (level.hasChunk(n.x(), n.z())) {
                helper.fail("chunk " + n + " was loaded before the lift ran - the test proves "
                        + "nothing from here; move it somewhere nobody has been");
                return;
            }
        }

        int x0 = at.getMinBlockX();
        int z0 = at.getMinBlockZ();
        int y = com.example.horsegenetics.neoforge.server.HorseRealmLift.BUILT_AT_Y;
        BlockState stone = Blocks.STONE.defaultBlockState();
        level.setBlock(new BlockPos(x0, y - 1, z0), Blocks.BEDROCK.defaultBlockState(), 2 | 16);
        for (int i = 0; i < 16; i++) {
            level.setBlock(new BlockPos(x0, y, z0 + i), stone, 2 | 16);
            level.setBlock(new BlockPos(x0 + 15, y, z0 + i), stone, 2 | 16);
            level.setBlock(new BlockPos(x0 + i, y, z0), stone, 2 | 16);
            level.setBlock(new BlockPos(x0 + i, y, z0 + 15), stone, 2 | 16);
        }

        com.example.horsegenetics.neoforge.server.HorseRealmLift.liftNow(level, at);

        BlockPos lifted = new BlockPos(x0 + 15, y + com.example.horsegenetics.neoforge.server.HorseRealmLift.OFFSET, z0 + 7);
        if (!level.getBlockState(lifted).is(Blocks.STONE)) {
            helper.fail("the lift did not run - no stone at " + lifted.toShortString()
                    + ", so the neighbour check below would be checking nothing");
            return;
        }
        for (net.minecraft.world.level.ChunkPos n : around) {
            if (level.hasChunk(n.x(), n.z())) {
                helper.fail("lifting chunk " + at + " loaded its neighbour " + n
                        + " - a shape update crossed the edge (issue #13)");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * <b>Building the F6 corridor loads no chunk but the one it writes.</b>
     * Issue #31: one operator pressing F6 froze the live server for three
     * seconds, and 59 of the 60 chunk loads it forced came from
     * {@code DebugPenManager.fastSet} - the same flag-2 shape update as #13,
     * crossing a chunk edge into a neighbour nobody had loaded.
     *
     * <p>So: every edge cell of an untouched chunk gets a corridor column
     * (bedrock, dirt, gravel) and a plank wall block through the real
     * {@code fastSet}. All four neighbours must still be unloaded afterwards.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEBUG_CORRIDOR_LOADS_NO_NEIGHBOUR =
            TEST_FUNCTIONS.register("debug_corridor_loads_no_neighbour", () -> ModGameTests::debugCorridorLoadsNoNeighbour);

    private static void debugCorridorLoadsNoNeighbour(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        // Far from the test structures, the spawn chunks and the realm-lift test's chunk.
        net.minecraft.world.level.ChunkPos at = new net.minecraft.world.level.ChunkPos(4133, -3881);
        net.minecraft.world.level.ChunkPos[] around = {
                new net.minecraft.world.level.ChunkPos(at.x() - 1, at.z()),
                new net.minecraft.world.level.ChunkPos(at.x() + 1, at.z()),
                new net.minecraft.world.level.ChunkPos(at.x(), at.z() - 1),
                new net.minecraft.world.level.ChunkPos(at.x(), at.z() + 1)};
        level.getChunk(at.x(), at.z());
        for (net.minecraft.world.level.ChunkPos n : around) {
            if (level.hasChunk(n.x(), n.z())) {
                helper.fail("chunk " + n + " was loaded before the build ran - the test proves "
                        + "nothing from here; move it somewhere nobody has been");
                return;
            }
        }

        int x0 = at.getMinBlockX();
        int z0 = at.getMinBlockZ();
        int gy = 100;
        BlockState[] column = {Blocks.BEDROCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState()};
        for (int i = 0; i < 16; i++) {
            for (BlockPos edge : new BlockPos[] {new BlockPos(x0, 0, z0 + i), new BlockPos(x0 + 15, 0, z0 + i),
                    new BlockPos(x0 + i, 0, z0), new BlockPos(x0 + i, 0, z0 + 15)}) {
                for (int dy = 0; dy < column.length; dy++) {
                    com.example.horsegenetics.neoforge.server.DebugPenManager.fastSet(
                            level, edge.atY(gy - 3 + dy), column[dy]);
                }
            }
        }

        BlockPos built = new BlockPos(x0 + 15, gy + 1, z0 + 7);
        if (!level.getBlockState(built).is(Blocks.OAK_PLANKS)) {
            helper.fail("the build did not run - no planks at " + built.toShortString()
                    + ", so the neighbour check below would be checking nothing");
            return;
        }
        for (net.minecraft.world.level.ChunkPos n : around) {
            if (level.hasChunk(n.x(), n.z())) {
                helper.fail("building the corridor in chunk " + at + " loaded its neighbour " + n
                        + " - a shape update crossed the edge (issue #31)");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * <b>A horse lost after a realm backup comes back from it, as itself.</b>
     * The whole of {@code RealmBackup} end to end, in whatever level the harness
     * has (it has no realm; the backup does not ask which level it is): take a
     * backup, lose a horse <i>without</i> it dying - discarded, which is what
     * losing its entity data looks like, and the case nothing else in the mod
     * can undo - then plan the restore and carry it out.
     *
     * <p>The plan must call it RAISE: not loaded, no death mark, no record of
     * leaving, and gone from the level's files. The raise must give back the
     * same UUID, alive. The plan is cut down to this one horse with
     * {@code only}, so the other tests' horses are not raised alongside it.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REALM_BACKUP_RAISES_A_LOST_HORSE =
            TEST_FUNCTIONS.register("realm_backup_raises_a_lost_horse", () -> ModGameTests::realmBackupRaisesALostHorse);

    private static void realmBackupRaisesALostHorse(GameTestHelper helper) {
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        // Founded on a later tick - see A_DEAD_HORSE_COMES_BACK_WHOLE.
        helper.runAfterDelay(10L, () -> backUpLoseAndRestore(helper, horse));
    }

    private static void backUpLoseAndRestore(GameTestHelper helper,
                                             net.minecraft.world.entity.animal.equine.Horse horse) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        java.util.UUID id = horse.getUUID();
        String name = com.example.horsegenetics.neoforge.server.HorseRecords.of(horse).displayName();
        com.example.horsegenetics.neoforge.server.RealmBackup.Plan plan;
        try {
            com.example.horsegenetics.neoforge.data.RealmBackups.Backup backup =
                    com.example.horsegenetics.neoforge.server.RealmBackup.take(server, level, "gametest");
            horse.discard();
            plan = com.example.horsegenetics.neoforge.server.RealmBackup
                    .plan(server, level, backup.name()).only(id);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException(Component.literal("the backup could not be taken or read: " + e), 0);
        }
        if (!plan.entries().containsKey(id)) {
            throw new GameTestAssertException(Component.literal(
                    "the backup does not hold " + name + " - the region files were copied before the "
                            + "flush reached them, or the reader is not finding horses in them"), 0);
        }
        com.example.horsegenetics.common.realm.RealmRestore.Action action = plan.actions().get(id);
        if (action != com.example.horsegenetics.common.realm.RealmRestore.Action.RAISE) {
            throw new GameTestAssertException(Component.literal(
                    "a discarded horse was planned as " + action + ", not RAISE"), 0);
        }
        com.example.horsegenetics.neoforge.server.RealmBackup.execute(server, plan);
        helper.succeedWhen(() -> {
            net.minecraft.world.entity.Entity back = level.getEntity(id);
            if (!(back instanceof net.minecraft.world.entity.animal.equine.Horse h) || !h.isAlive()) {
                throw new GameTestAssertException(Component.literal(name + " has not been raised yet"), 0);
            }
            String raisedName = com.example.horsegenetics.neoforge.server.HorseRecords.of(h).displayName();
            if (!raisedName.equals(name)) {
                throw new GameTestAssertException(Component.literal(
                        "the raised horse is called \"" + raisedName + "\", not \"" + name + "\""), 0);
            }
        });
    }

    /**
     * <b>The freedom stick crafts, and is drawable.</b> A feather, a stick and one
     * horse hair. Same three flags as every other recipe here: an item that is
     * craftable but not <i>findable</i> is an item nobody will ever make, and this
     * one is the only door out of owning a horse.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FREEDOM_STICK_CRAFTS =
            TEST_FUNCTIONS.register("freedom_stick_crafts", () -> ModGameTests::freedomStickCrafts);

    private static void freedomStickCrafts(GameTestHelper helper) {
        craftsInto(helper, "the freedom stick",
                CraftingInput.of(3, 1, List.of(
                        new ItemStack(Items.FEATHER), new ItemStack(Items.STICK),
                        new ItemStack(ModItems.HORSE_HAIR.get()))),
                ModItems.FREEDOM_STICK.get());
        helper.succeed();
    }

    /**
     * <b>A horse arriving by waystone is not put inside the wall.</b>
     *
     * <p>{@code WaystonesCompat.nearestClearance} is the only piece of that
     * integration with real logic in it, and it is the only piece that can be
     * <i>silently</i> wrong: everything else either throws or visibly does
     * nothing. A shell search that quietly returned the second-nearest spot, or
     * that accepted a cube one block short, would look exactly like a working
     * fix until somebody's Shire came out of a waystone in a hillside.
     *
     * <p>Three things are asserted, and the third is the one that matters most:
     * <ol>
     *   <li>An arrival in open air <b>does not move</b>.</li>
     *   <li>An arrival walled in finds a spot, and the spot really is clear.</li>
     *   <li>The spot it finds is the <b>nearest</b> clear one - checked by
     *       measuring it against an exhaustive scan of the same box, because
     *       "found somewhere" and "found the nearest" are the two answers the
     *       shell search is between.</li>
     * </ol>
     *
     * <p>It builds its own scratch box in the air well above the test structure
     * and clears it again, rather than using the 1x1x1 template: the thing under
     * test takes a {@code BlockGetter} and needs a few hundred blocks of world
     * to have an opinion about.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WAYSTONE_CLEARANCE_IS_NEAREST =
            TEST_FUNCTIONS.register("waystone_clearance_is_nearest",
                    () -> ModGameTests::waystoneClearanceIsNearest);

    /** Half-width of the scratch box, comfortably outside the search radius. */
    private static final int SCRATCH = 12;

    private static void waystoneClearanceIsNearest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Up in the air above the structure, so nothing here touches the test's
        // own 1x1x1 template or anything else in the batch.
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).above(40);
        try {
            fill(level, origin, Blocks.AIR.defaultBlockState());

            // 1. Open air: the spot Waystones chose is kept exactly.
            BlockPos open = WaystonesCompat.nearestClearance(level, origin);
            if (!origin.equals(open)) {
                helper.fail("a waystone arrival in open air was moved, to " + open
                        + " instead of staying at " + origin
                        + " - every ordinary hop on the server would be relocated.");
            }

            // 2 and 3. Wall it in: a solid 5x5x5 around the arrival, so the
            // nearest clear cube is genuinely somewhere else and there is a
            // right answer to get wrong.
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        level.setBlock(origin.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
            BlockPos found = WaystonesCompat.nearestClearance(level, origin);
            if (found == null) {
                helper.fail("a walled-in waystone arrival found no clear spot at all, "
                        + "in a box that is otherwise nothing but air.");
                return;
            }
            if (!clearIn(level, found)) {
                helper.fail("the spot chosen for a walled-in arrival, " + found
                        + ", is not actually a clear 3x3x3 - the horse would arrive "
                        + "inside a block and suffocate, which is the whole bug.");
            }
            long best = Long.MAX_VALUE;
            for (int x = -SCRATCH; x <= SCRATCH; x++) {
                for (int y = -SCRATCH; y <= SCRATCH; y++) {
                    for (int z = -SCRATCH; z <= SCRATCH; z++) {
                        if (clearIn(level, origin.offset(x, y, z))) {
                            best = Math.min(best, (long) x * x + (long) y * y + (long) z * z);
                        }
                    }
                }
            }
            // Integer arithmetic throughout: BlockPos.distSqr answers a double,
            // and comparing the search's answer to the scan's for EQUALITY is
            // the point of the assertion.
            long fx = found.getX() - origin.getX();
            long fy = found.getY() - origin.getY();
            long fz = found.getZ() - origin.getZ();
            long chosen = fx * fx + fy * fy + fz * fz;
            if (chosen != best) {
                helper.fail("the shell search did not find the nearest clear spot: it chose "
                        + found + ", " + chosen + " blocks squared away, and an exhaustive scan "
                        + "of the same box found one at " + best + ".");
            }
        } finally {
            // The scratch box goes back to air whatever happened, so a failure
            // here does not leave stone hanging over the next test in the batch.
            fill(level, origin, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /** {@link WaystonesCompat#nearestClearance}'s own clearance rule, spelled out again on purpose. */
    private static boolean clearIn(ServerLevel level, BlockPos feet) {
        for (int y = 0; y < 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!level.getBlockState(feet.offset(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void fill(ServerLevel level, BlockPos origin, BlockState state) {
        for (int x = -SCRATCH; x <= SCRATCH; x++) {
            for (int y = -SCRATCH; y <= SCRATCH; y++) {
                for (int z = -SCRATCH; z <= SCRATCH; z++) {
                    level.setBlock(origin.offset(x, y, z), state, 2);
                }
            }
        }
    }

    /**
     * <b>A horse that grows under a low ceiling does not end up inside it</b> (#35).
     *
     * <p>A stone floor and a stone ceiling two blocks over it: room for a vanilla
     * horse (1.6 tall) and not for a scale-1.75 one (2.8). The horse is put in
     * at scale 1 and grown, the way the founding tick grows every horse placed at
     * vanilla size. Vanilla's own nudge finds no room for the new height and, by
     * its fallback, keeps the horse's eyes in the ceiling; {@code HorseClearance}
     * has to get it out. Passes when the grown box collides with nothing and the
     * eyes are in air.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GROWN_HORSE_LEAVES_THE_CEILING =
            TEST_FUNCTIONS.register("grown_horse_leaves_the_ceiling",
                    () -> ModGameTests::grownHorseLeavesTheCeiling);

    private static void grownHorseLeavesTheCeiling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).above(40);
        fill(level, origin, Blocks.AIR.defaultBlockState());
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(origin.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO.above(40));
        horse.setNoAi(true);
        net.minecraft.world.entity.ai.attributes.AttributeInstance scale =
                horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        boolean[] grown = {false};
        helper.onEachTick(() -> {
            if (grown[0] || !com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
                return;
            }
            // Founded (which rolled a scale of its own): back to vanilla size, in
            // the gap, and grown a tick later so the growth is its own refresh.
            if (scale.getBaseValue() != 1.0) {
                scale.setBaseValue(1.0);
                horse.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
                return;
            }
            horse.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
            scale.setBaseValue(1.75);
            grown[0] = true;
        });
        helper.succeedWhen(() -> {
            // The box, not getScale(): the attribute reads 1.75 at once, and the
            // hitbox follows only on the horse's next tick.
            if (!grown[0] || horse.getBbHeight() < 2.7F) {
                throw new GameTestAssertException(Component.literal("the horse has not grown yet"), 0);
            }
            if (!level.noCollision(horse, horse.getBoundingBox()) || horse.isInWall()) {
                throw new GameTestAssertException(Component.literal("a horse grown to scale 1.75 under a "
                        + "two-block ceiling is still inside the blocks at " + horse.blockPosition().toShortString()
                        + " - it will suffocate there."), 0);
            }
            fill(level, origin, Blocks.AIR.defaultBlockState());
        });
    }

    /**
     * <b>A second jockey pass adds a day; it does not replace one.</b>
     *
     * <p>{@code RidingPassAttachment} is arithmetic on deadlines, and every way
     * of getting it wrong is invisible from inside the game. A second pass that
     * <em>overwrote</em> the first would look identical on the day it was fed and
     * would quietly have cost somebody a day; a grant that counted from a stale
     * deadline would hand out a pass that had already expired; a lapsed entry
     * that was never pruned would sit in the save naming a jockey for ever. None
     * of those produce an error, a log line or a visible symptom, so they are
     * checked here rather than in the yard.
     *
     * <p>No world is touched at all - it is a record and a clock.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JOCKEY_PASSES_ADD_UP =
            TEST_FUNCTIONS.register("jockey_passes_add_up", () -> ModGameTests::jockeyPassesAddUp);

    private static void jockeyPassesAddUp(GameTestHelper helper) {
        final long day = JockeyPassHandler.DAY_TICKS;
        UUID jockey = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Nothing, to start with.
        RidingPassAttachment passes = RidingPassAttachment.DEFAULT;
        if (passes.allows(jockey, 0L)) {
            helper.fail("a horse nobody has lent lets a stranger ride it.");
        }

        // One pass at t=0 is one day.
        passes = passes.grant(jockey, 0L, day);
        if (!passes.allows(jockey, 0L) || !passes.allows(jockey, day - 1)) {
            helper.fail("a freshly fed pass does not cover its own day.");
        }
        if (passes.allows(jockey, day)) {
            helper.fail("a one-day pass is still good at exactly one day - it should have lapsed; "
                    + "the comparison is off by a tick and every pass is longer than it was sold as.");
        }
        if (passes.remaining(jockey, 0L) != day) {
            helper.fail("a fresh pass reports " + passes.remaining(jockey, 0L)
                    + " ticks left rather than " + day + ".");
        }

        // A SECOND PASS ADDS. This is the one that matters.
        passes = passes.grant(jockey, 100L, day);
        long expected = 2 * day - 100L;
        if (passes.remaining(jockey, 100L) != expected) {
            helper.fail("feeding a second pass 100 ticks into the first left "
                    + passes.remaining(jockey, 100L) + " ticks rather than " + expected
                    + " - a second pass is being spent to replace the first rather than extend it, "
                    + "which costs the jockey a day and shows no symptom.");
        }

        // A pass granted after the old one lapsed counts from now, not from the
        // dead deadline - otherwise it would arrive already expired.
        long late = 5 * day;
        passes = passes.grant(other, late, day);
        if (passes.remaining(other, late) != day) {
            helper.fail("a pass fed long after an earlier one lapsed is worth "
                    + passes.remaining(other, late) + " rather than a full " + day + ".");
        }
        // ...and that write is what prunes the first jockey, whose pass is long gone.
        if (passes.allows(jockey, late) || passes.until().containsKey(jockey.toString())) {
            if (passes.until().containsKey(jockey.toString())) {
                helper.fail("a lapsed pass is still stored on the horse - it will sit in the save "
                        + "naming a jockey for ever.");
            }
            helper.fail("a lapsed pass still lets its holder ride.");
        }

        // Revoking takes it back, and revoking nothing changes nothing.
        RidingPassAttachment before = passes;
        if (passes.revoke(jockey) != before) {
            helper.fail("revoking a pass nobody holds returned a new object - the command reads "
                    + "that as a real revocation and tells the owner it worked.");
        }
        passes = passes.revoke(other);
        if (passes.allows(other, late)) {
            helper.fail("a revoked pass still lets its holder ride.");
        }

        // And a sale clears the lot.
        passes = passes.grant(jockey, late, day).grant(other, late, day);
        if (passes.cleared().allows(jockey, late) || !passes.cleared().isEmpty()) {
            helper.fail("selling a horse leaves its jockeys aboard - the new owner's horse would "
                    + "be rideable by strangers they never agreed to and could not find out about.");
        }
        helper.succeed();
    }

    /**
     * <b>"Eject empties" takes out every empty chamber, and nothing else.</b>
     * The Chambers tab's one-press button, driven through the real
     * {@code clickMenuButton} id the screen sends.
     *
     * <p>Two empties of different tiers, one occupied chamber, and an empty
     * chamber sitting in a drop-buffer slot - which is not a chamber slot and
     * must not be walked. The player's pack has room for exactly one, so the
     * first empty goes into it and the second drops at the bank. Then a second
     * press, with nothing left to eject, must do nothing and say so.
     *
     * <p>The dropped chamber is cleared up afterwards: a test that leaves items
     * on the floor is a test that fails its neighbours (see
     * {@link #VANILLA_LEAD_COMES_BACK}).
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STASIS_BANK_EJECTS_EMPTIES =
            TEST_FUNCTIONS.register("stasis_bank_ejects_empties", () -> ModGameTests::stasisBankEjectsEmpties);

    private static void stasisBankEjectsEmpties(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, com.example.horsegenetics.neoforge.block.ModBlocks.HORSE_STASIS_BANK.get());
        BlockPos abs = helper.absolutePos(at);
        if (!(helper.getLevel().getBlockEntity(abs)
                instanceof com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity bank)) {
            helper.fail("placing a Horse Stasis Bank made no bank block entity - the premise is broken");
            return;
        }
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(abs.getCenter().add(0.0, 1.0, 0.0));
        net.minecraft.world.entity.player.Inventory pack = player.getInventory();
        for (int i = 1; i < 36; i++) {
            pack.setItem(i, new ItemStack(Items.DIRT, 64));
        }
        pack.setItem(0, ItemStack.EMPTY);

        net.minecraft.world.Container chambers = bank.chambers();
        chambers.setItem(0, new ItemStack(ModItems.BASIC_STASIS_CHAMBER.get()));
        chambers.setItem(1, com.example.horsegenetics.neoforge.item.StasisChamberItem.withHorse(
                new ItemStack(ModItems.INTERMEDIATE_STASIS_CHAMBER.get()),
                new com.example.horsegenetics.neoforge.data.StasisSnapshot(
                        "Keeper", UUID.randomUUID(), new net.minecraft.nbt.CompoundTag())));
        chambers.setItem(2, new ItemStack(ModItems.ADVANCED_STASIS_CHAMBER.get()));
        int dropSlot = com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity.FIRST_DROP_SLOT;
        bank.supplies().setItem(dropSlot, new ItemStack(ModItems.BASIC_STASIS_CHAMBER.get()));

        com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu menu =
                new com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu(0, pack, bank);
        if (menu.empties() != 2) {
            helper.fail("the bank counts " + menu.empties() + " empties, not 2 - the button's label "
                    + "would be wrong before it was ever pressed");
            return;
        }
        int button = com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu.EJECT_EMPTIES_BUTTON;
        if (!menu.clickMenuButton(player, button)) {
            helper.fail("pressing Eject empties with two empties in the bank was refused");
            return;
        }
        if (!chambers.getItem(0).isEmpty() || !chambers.getItem(2).isEmpty()) {
            helper.fail("an empty chamber was left in the bank after Eject empties");
            return;
        }
        if (com.example.horsegenetics.neoforge.item.StasisChamberItem.snapshotOf(chambers.getItem(1)) == null) {
            helper.fail("Eject empties took a chamber WITH A HORSE IN IT out of the bank");
            return;
        }
        if (!bank.supplies().getItem(dropSlot).is(ModItems.BASIC_STASIS_CHAMBER.get())) {
            helper.fail("Eject empties reached into the drop buffer - only chamber slots are its business");
            return;
        }
        if (!pack.getItem(0).is(ModItems.BASIC_STASIS_CHAMBER.get())) {
            helper.fail("the first empty did not reach the player's one free slot");
            return;
        }
        if (menu.empties() != 0) {
            helper.fail("the bank still counts " + menu.empties() + " empties after ejecting them");
            return;
        }
        if (menu.clickMenuButton(player, button)) {
            helper.fail("a second press with no empties left claimed to have done something");
            return;
        }
        // The pack was full, so the second empty is on the ground at the bank.
        helper.runAfterDelay(5L, () -> {
            helper.assertItemEntityPresent(ModItems.ADVANCED_STASIS_CHAMBER.get(), at, 2.0);
            helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
            helper.succeed();
        });
    }

    /**
     * <b>Send home is free by default, and a set price is all-or-nothing.</b>
     *
     * <p>The browser's button runs through {@code StallRecall}, which needs a
     * {@code ServerPlayer} - and a gametest cannot make one without joining the
     * player list (see {@link #VANILLA_LEAD_COMES_BACK}). So this drives the part
     * that decides money, {@code SendHomePayment}, against a mock player's real
     * inventory, and leaves the landing in a stall to the in-game check on
     * wiki/horse-browser.html's Verification tab. The cooldown is pure arithmetic
     * and pinned by {@code SendHomeTest}.
     *
     * <p>Asserted: this run's default config is free and lets an empty pack
     * through; a price the pack cannot meet is refused <b>and takes nothing</b>;
     * a price it can meet is taken exactly, across split stacks; a creative
     * player is never charged; and an id naming no item is free rather than a
     * lock-out.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEND_HOME_PAYMENT =
            TEST_FUNCTIONS.register("send_home_payment", () -> ModGameTests::sendHomePayment);

    private static void sendHomePayment(GameTestHelper helper) {
        com.example.horsegenetics.neoforge.server.SendHomePayment.Charge configured =
                com.example.horsegenetics.neoforge.server.SendHomePayment.current();
        if (!configured.free()) {
            helper.fail("behaviour.send_home_payment_* sets a price in this run's server config, so the"
                    + " free default is not what is being run - reset it rather than deleting the test");
            return;
        }
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Inventory pack = player.getInventory();
        pack.clearContent();
        if (!com.example.horsegenetics.neoforge.server.SendHomePayment.canPay(player, configured)) {
            helper.fail("the free default refused a player with an empty pack");
            return;
        }

        com.example.horsegenetics.neoforge.server.SendHomePayment.Charge emeralds =
                com.example.horsegenetics.neoforge.server.SendHomePayment.resolve(
                        com.example.horsegenetics.common.care.SendHome.Price.of("minecraft:emerald", 3));
        if (emeralds.free() || emeralds.item() != Items.EMERALD) {
            helper.fail("minecraft:emerald x3 did not resolve to a price in emeralds");
            return;
        }
        pack.setItem(4, new ItemStack(Items.EMERALD, 2));
        if (com.example.horsegenetics.neoforge.server.SendHomePayment.canPay(player, emeralds)) {
            helper.fail("two emeralds were accepted for a price of three");
            return;
        }
        if (pack.countItem(Items.EMERALD) != 2) {
            helper.fail("a refused price took something from the pack");
            return;
        }
        pack.setItem(9, new ItemStack(Items.EMERALD, 5));
        if (!com.example.horsegenetics.neoforge.server.SendHomePayment.canPay(player, emeralds)) {
            helper.fail("seven emeralds in two stacks were refused for a price of three");
            return;
        }
        com.example.horsegenetics.neoforge.server.SendHomePayment.take(player, emeralds);
        if (pack.countItem(Items.EMERALD) != 4) {
            helper.fail("paying three of seven emeralds left " + pack.countItem(Items.EMERALD) + ", not 4");
            return;
        }

        net.minecraft.world.entity.player.Player creative =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        creative.getInventory().clearContent();
        // The mock's game type overrides isCreative() and nothing else; its
        // abilities are a fresh survival set. instabuild is what the real game
        // sets for creative and what every spend in this mod checks.
        creative.getAbilities().instabuild = true;
        if (!com.example.horsegenetics.neoforge.server.SendHomePayment.canPay(creative, emeralds)) {
            helper.fail("a creative player was asked to pay");
            return;
        }

        com.example.horsegenetics.neoforge.server.SendHomePayment.Charge typo =
                com.example.horsegenetics.neoforge.server.SendHomePayment.resolve(
                        com.example.horsegenetics.common.care.SendHome.Price.of("minecraft:emerlad", 3));
        if (!typo.free()) {
            helper.fail("an id naming no item became a price nobody can pay, not free");
            return;
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Undead horses - UndeadHorseConverter (wiki/undead-horses.html)
    // ------------------------------------------------------------------

    /**
     * <b>A vanilla zombie and skeleton horse become this mod's horses, whole.</b>
     * The zombie is the hard case: tamed by a player, named, saddled and at half
     * health. Afterwards each UUID must hold exactly one entity, a
     * {@code minecraft:horse} with a real record expressing its undeath gene, of the
     * right breed; the zombie must keep its owner, its tame flag, its name (as the
     * barn name), its saddle and its health <i>fraction</i>, and arrive passified
     * for its owner so it never turns on them after dark (D25).
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNDEAD_HORSES_CONVERT =
            TEST_FUNCTIONS.register("undead_horses_convert", () -> ModGameTests::undeadHorsesConvert);

    private static void undeadHorsesConvert(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player owner =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.ZombieHorse zombie =
                helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE_HORSE, BlockPos.ZERO);
        zombie.tameWithName(owner);
        zombie.setCustomName(Component.literal("Mortimer"));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        zombie.setHealth(zombie.getMaxHealth() / 2f);
        // The test spot nicks a horse a point now and then; invulnerable (which the
        // tag carries across too) so the fraction measured is the converter's alone.
        zombie.setInvulnerable(true);
        net.minecraft.world.entity.animal.equine.SkeletonHorse skeleton =
                helper.spawn(net.minecraft.world.entity.EntityType.SKELETON_HORSE, new BlockPos(2, 0, 0));
        // undead.convert ships off; the flag converts these two as /horseundead test's subjects are.
        zombie.getPersistentData().putBoolean(com.example.horsegenetics.neoforge.server.UndeadHorseConverter.CONVERT_ANYWAY, true);
        skeleton.getPersistentData().putBoolean(com.example.horsegenetics.neoforge.server.UndeadHorseConverter.CONVERT_ANYWAY, true);
        UUID zombieId = zombie.getUUID();
        UUID skeletonId = skeleton.getUUID();
        UUID ownerId = owner.getUUID();
        ServerLevel level = helper.getLevel();
        helper.succeedWhen(() -> {
            net.minecraft.world.entity.animal.equine.Horse z = convertedOrFail(level, zombieId, "the zombie horse",
                    com.example.horsegenetics.common.genetics.Undeath.Kind.ZOMBIE, "graveborn_warmblood");
            convertedOrFail(level, skeletonId, "the skeleton horse",
                    com.example.horsegenetics.common.genetics.Undeath.Kind.SKELETON, "great_valley_skeleton_horse");
            if (!z.isTamed() || z.getOwnerReference() == null || !ownerId.equals(z.getOwnerReference().getUUID())) {
                throw new GameTestAssertException(Component.literal("the converted zombie lost its owner or its tame flag"), 0);
            }
            com.example.horsegenetics.common.horse.HorseRecord record =
                    com.example.horsegenetics.neoforge.server.HorseRecords.of(z);
            if (!record.ownerId().equals(java.util.Optional.of(ownerId))) {
                throw new GameTestAssertException(Component.literal("the record's owner does not mirror the entity's"), 0);
            }
            if (!"Mortimer".equals(record.displayName())) {
                throw new GameTestAssertException(Component.literal(
                        "the converted zombie is called \"" + record.displayName() + "\", not Mortimer"), 0);
            }
            if (!z.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).is(Items.SADDLE)) {
                throw new GameTestAssertException(Component.literal("the converted zombie lost its saddle"), 0);
            }
            float fraction = z.getHealth() / z.getMaxHealth();
            if (Math.abs(fraction - 0.5f) > 0.02f) {
                throw new GameTestAssertException(Component.literal(
                        "the converted zombie is at " + fraction + " of its health, not half"), 0);
            }
            if (!z.getData(com.example.horsegenetics.neoforge.data.ModAttachments.PASSIFICATION.get()).permanent(ownerId)) {
                throw new GameTestAssertException(Component.literal(
                        "a converted horse its owner already had is not passified toward them"), 0);
            }
        });
    }

    /** The entity under {@code id} is a converted horse of {@code kind} and {@code breed}, and the only one. */
    private static net.minecraft.world.entity.animal.equine.Horse convertedOrFail(ServerLevel level, UUID id,
            String what, com.example.horsegenetics.common.genetics.Undeath.Kind kind, String breed) {
        net.minecraft.world.entity.Entity e = level.getEntity(id);
        if (!(e instanceof net.minecraft.world.entity.animal.equine.Horse h) || !h.isAlive()) {
            throw new GameTestAssertException(Component.literal(what + " has not been converted yet ("
                    + (e == null ? "nothing" : e.getType().toShortString()) + " under its UUID)"), 0);
        }
        if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(h)) {
            throw new GameTestAssertException(Component.literal(what + " converted with no record"), 0);
        }
        if (com.example.horsegenetics.neoforge.server.UndeadHorses.kindOf(h) != kind) {
            throw new GameTestAssertException(Component.literal(what + " converted without its undeath gene"), 0);
        }
        String token = com.example.horsegenetics.neoforge.server.HorseRecords.of(h).breed().orElse("");
        if (!token.contains(breed)) {
            throw new GameTestAssertException(Component.literal(what + " converted as " + token + ", not " + breed), 0);
        }
        long same = level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(
                net.minecraft.world.entity.Entity.class), o -> o.getUUID().equals(id)).size();
        if (same != 1) {
            throw new GameTestAssertException(Component.literal(same + " entities hold " + what + "'s UUID"), 0);
        }
        return h;
    }

    /**
     * <b>An unsprung skeleton trap is vanilla's, and only the horse it leaves is
     * ours</b> (D5). Armed, it must still be a vanilla skeleton horse a dozen ticks
     * on; disarmed, it converts like any other.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNDEAD_TRAP_WAITS =
            TEST_FUNCTIONS.register("undead_trap_waits", () -> ModGameTests::undeadTrapWaits);

    private static void undeadTrapWaits(GameTestHelper helper) {
        net.minecraft.world.entity.animal.equine.SkeletonHorse trap =
                helper.spawn(net.minecraft.world.entity.EntityType.SKELETON_HORSE, BlockPos.ZERO);
        trap.setTrap(true);
        trap.getPersistentData().putBoolean(com.example.horsegenetics.neoforge.server.UndeadHorseConverter.CONVERT_ANYWAY, true);
        UUID id = trap.getUUID();
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(12L, () -> {
            if (!(level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse still)
                    || !still.isTrap()) {
                throw new GameTestAssertException(Component.literal(
                        "an armed skeleton trap was converted - the trap is disarmed for good"), 0);
            }
            still.setTrap(false);
            helper.succeedWhen(() -> convertedOrFail(level, id, "the sprung trap's horse",
                    com.example.horsegenetics.common.genetics.Undeath.Kind.SKELETON, "great_valley_skeleton_horse"));
        });
    }

    /**
     * <b>With {@code undead.convert} at its default (off), a vanilla undead horse stays
     * vanilla</b> - conversion reaches a world only through {@code /horseundead enable}.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNDEAD_OFF_BY_DEFAULT =
            TEST_FUNCTIONS.register("undead_off_by_default", () -> ModGameTests::undeadOffByDefault);

    private static void undeadOffByDefault(GameTestHelper helper) {
        // The declared default, not the live value: phc/server.toml belongs to the game
        // directory, and run-gametest's may still hold a value written under the old default.
        if (com.example.horsegenetics.neoforge.ServerConfig.UNDEAD_CONVERT.getDefault()) {
            throw new GameTestAssertException(Component.literal("undead.convert is on by default"), 0);
        }
        // In memory only (never saved): every other undead test converts through its own flag.
        com.example.horsegenetics.neoforge.ServerConfig.UNDEAD_CONVERT.set(false);
        net.minecraft.world.entity.animal.equine.SkeletonHorse skeleton =
                helper.spawn(net.minecraft.world.entity.EntityType.SKELETON_HORSE, BlockPos.ZERO);
        UUID id = skeleton.getUUID();
        helper.runAfterDelay(12L, () -> {
            if (!(helper.getLevel().getEntity(id) instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse)) {
                throw new GameTestAssertException(Component.literal(
                        "a skeleton horse was converted with undead.convert off"), 0);
            }
            helper.succeed();
        });
    }

    /**
     * <b>{@code /horseundead test} works through the real dispatcher</b>: {@code enable}
     * is refused before a test has passed, and the test then converts its skeleton and
     * zombie subjects and reports a pass. {@code enable} is not run after it, because it
     * writes the config file.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNDEAD_TEST_COMMAND =
            TEST_FUNCTIONS.register("undead_test_command", () -> ModGameTests::undeadTestCommand);

    private static void undeadTestCommand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.commands.CommandSourceStack source = level.getServer().createCommandSourceStack()
                .withSuppressedOutput().withLevel(level)
                // At the structure itself: the subjects stand two blocks either side, inside
                // this test's own ground (see order_stay_walks_back on the harness's sweep).
                .withPosition(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)));
        com.example.horsegenetics.neoforge.ServerConfig.UNDEAD_CONVERT.set(false);
        level.getServer().getCommands().performPrefixedCommand(source, "horseundead enable");
        if (com.example.horsegenetics.neoforge.ServerConfig.undeadConvert()
                && !com.example.horsegenetics.neoforge.server.HorseUndeadCommand.testPassed()) {
            throw new GameTestAssertException(Component.literal("/horseundead enable was accepted before a test"), 0);
        }
        level.getServer().getCommands().performPrefixedCommand(source, "horseundead test");
        helper.succeedWhen(() -> {
            if (!com.example.horsegenetics.neoforge.server.HorseUndeadCommand.testPassed()) {
                throw new GameTestAssertException(Component.literal("/horseundead test has not passed yet"), 0);
            }
        });
    }

    /** <b>A horse marked to stay vanilla stays vanilla</b> - the debug pens' control case. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNDEAD_KEEP_VANILLA =
            TEST_FUNCTIONS.register("undead_keep_vanilla", () -> ModGameTests::undeadKeepVanilla);

    private static void undeadKeepVanilla(GameTestHelper helper) {
        net.minecraft.world.entity.animal.equine.ZombieHorse zombie =
                helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE_HORSE, BlockPos.ZERO);
        zombie.getPersistentData().putBoolean(
                com.example.horsegenetics.neoforge.server.UndeadHorseConverter.KEEP_VANILLA, true);
        // Asked to convert anyway as well: keep_vanilla must still win.
        zombie.getPersistentData().putBoolean(com.example.horsegenetics.neoforge.server.UndeadHorseConverter.CONVERT_ANYWAY, true);
        UUID id = zombie.getUUID();
        helper.runAfterDelay(12L, () -> {
            if (!(helper.getLevel().getEntity(id) instanceof net.minecraft.world.entity.animal.equine.ZombieHorse)) {
                throw new GameTestAssertException(Component.literal(
                        "a zombie horse marked keep_vanilla was converted anyway"), 0);
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // Wild horse turnover (WildTurnover, WildTopUp). The gametest server runs
    // with mob spawning off, so the top-up's own tick never fires here; these
    // call its pieces directly. "Nobody leaves while watched" is pinned in
    // common (WildLifetimeTest) and is a play check: a mock player joins the whole
    // level, and would hold every other test's horse in place too.
    // ------------------------------------------------------------------

    private static final long WILD_DAY = com.example.horsegenetics.common.wild.WildLifetime.DAY_TICKS;

    /** A natural spawn from server code, as the top-up makes one - the path that must fire FinalizeSpawnEvent. */
    private static net.minecraft.world.entity.animal.equine.Horse naturalHorse(GameTestHelper helper, BlockPos rel) {
        net.minecraft.world.entity.animal.equine.Horse horse = net.minecraft.world.entity.EntityType.HORSE.spawn(
                helper.getLevel(), helper.absolutePos(rel), net.minecraft.world.entity.EntitySpawnReason.NATURAL);
        if (horse == null) {
            throw new GameTestAssertException(Component.literal(
                    "a NATURAL horse spawn was refused here - the breed settings allow nothing in this biome?"), 0);
        }
        if (!com.example.horsegenetics.neoforge.server.WildTurnover.stamped(horse)) {
            throw new GameTestAssertException(Component.literal(
                    "a NATURAL spawn from server code carries no wild_born stamp - FinalizeSpawnEvent did not "
                            + "reach BreedSpawnHandler"), 0);
        }
        return horse;
    }

    /**
     * Force-load the 3x3 chunks round a wild test. The harness forces only the
     * chunk its 1x1 structure stands in, and a horse a few blocks over can be in a
     * neighbour that never ticks - so no scan, no founding, no placement. The
     * harness unforces every forced chunk when the batch ends. Forcing takes effect
     * some ticks later, not at once: {@link #fieldTicks} waits for it (issue #70).
     */
    private static void keepTicking(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(BlockPos.ZERO);
        int cx = at.getX() >> 4;
        int cz = at.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.getLevel().setChunkForced(cx + dx, cz + dz, true);
            }
        }
    }

    /**
     * Ground for a test that looks further than its own 1x1 cell (issue #37). Such a test
     * is registered with {@link #registerAlone}, so it is the only test running - and what
     * is left round it is the litter of tests that have finished: their horses and
     * monsters, which a hunting horse sixteen blocks out will pick, and their pens and
     * roofs, which cut its sight of the monster it was given. Both are cleared before the
     * test spawns anything, which is safe only because nothing else is running.
     */
    private static void aloneOnClearGround(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        // Hunt monsters' default radius plus the leash's slack, round a spot two blocks out.
        double reach = com.example.horsegenetics.common.care.HorseOrders.DEFAULT_HUNT_RADIUS
                + com.example.horsegenetics.common.care.HorseOrders.LEASH_SLACK_BLOCKS + 4;
        for (net.minecraft.world.entity.Entity e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new net.minecraft.world.phys.AABB(o).inflate(reach, 8, reach),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            e.discard();
        }
        // The field the order tests use (x 0..2, z -6..2) and a horse's wander round it.
        // The ground is two blocks below the origin (each test stands on a pedestal), so
        // what is cleared is feet level, y -2, up past a horse's head. The floor under
        // that is left alone, and so is every block with a block entity - this test's own
        // test-instance block among them.
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, -2, -8), o.offset(6, 2, 6))) {
            if (level.getBlockEntity(p) == null && !level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /** Make a stamped horse four days old. */
    private static void age(net.minecraft.world.entity.animal.equine.Horse horse, ServerLevel level) {
        horse.getPersistentData().putLong(com.example.horsegenetics.neoforge.server.WildTurnover.BORN_KEY,
                level.getGameTime() - 4 * WILD_DAY);
    }

    /** <b>An untouched wild horse past its days, with nobody near, is gone.</b> */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_HORSE_MOVES_ON =
            TEST_FUNCTIONS.register("wild_horse_moves_on", () -> ModGameTests::wildHorseMovesOn);

    private static void wildHorseMovesOn(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.animal.equine.Horse horse = naturalHorse(helper, BlockPos.ZERO);
        UUID id = horse.getUUID();
        // Founded first (twenty ticks), so what goes is a real horse with a record.
        helper.runAfterDelay(25L, () -> {
            if (level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse h) {
                age(h, level);
                // Its own herd lead. A herd member is judged on its lead's stamp, and
                // another wild test's NATURAL horse spawned beside this one can be made
                // its lead - a young one, so this horse stayed (issue #32, which flaked
                // whenever a new test moved the grid so two wild tests sat together).
                var care = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get();
                h.setData(care, h.getData(care).withHerd(java.util.Optional.of(id)));
            }
            helper.succeedWhen(() -> {
                if (level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse still) {
                    var care = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get();
                    throw new GameTestAssertException(Component.literal(
                            "a four-day-old untouched wild horse is still here (herd lead "
                                    + still.getData(care).herd().map(l -> l.equals(id) ? "itself" : l.toString())
                                            .orElse("none") + ")"), 0);
                }
            });
        });
    }

    /** <b>A wild horse a player worked with goes to the realm, wild, instead of vanishing.</b> */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_TOUCHED_HORSE_GOES_TO_THE_REALM =
            TEST_FUNCTIONS.register("wild_touched_horse_goes_to_the_realm", () -> ModGameTests::wildTouchedHorseGoesToTheRealm);

    private static void wildTouchedHorseGoesToTheRealm(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        // The harness has no realm (see HORSE_REALM_IS_BUILT), so there this proves the
        // other half of the rule: a hand-off that cannot happen never throws the horse away.
        ServerLevel realm = level.getServer().getLevel(HorseRealm.REALM_LEVEL);
        net.minecraft.world.entity.animal.equine.Horse horse = naturalHorse(helper, BlockPos.ZERO);
        UUID id = horse.getUUID();
        helper.runAfterDelay(25L, () -> {
            if (!(level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse h)) {
                throw new GameTestAssertException(Component.literal("the horse was gone before it was aged"), 0);
            }
            String name = com.example.horsegenetics.neoforge.server.HorseRecords.of(h).displayName();
            h.getPersistentData().putBoolean(com.example.horsegenetics.neoforge.server.WildTurnover.TOUCHED_KEY, true);
            age(h, level);
            if (realm == null) {
                helper.runAfterDelay(65L, () -> {
                    if (!(level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse still)
                            || !still.isAlive()) {
                        throw new GameTestAssertException(Component.literal(
                                "with no realm to go to, the touched horse was thrown away"), 0);
                    }
                    if (!com.example.horsegenetics.neoforge.server.WildTurnover.stamped(still)
                            || !still.getPersistentData().getBooleanOr(
                                    com.example.horsegenetics.neoforge.server.WildTurnover.TOUCHED_KEY, false)) {
                        throw new GameTestAssertException(Component.literal(
                                "a failed hand-off lost the horse's stamp or its touched mark, so it never tries again"), 0);
                    }
                    helper.succeed();
                });
                return;
            }
            helper.succeedWhen(() -> {
                if (level.getEntity(id) != null) {
                    throw new GameTestAssertException(Component.literal("the touched horse has not left yet"), 0);
                }
                if (!(realm.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse there)) {
                    throw new GameTestAssertException(Component.literal(
                            "the touched horse left but is not in the realm - it was thrown away"), 0);
                }
                if (there.isTamed() || !there.isPersistenceRequired()
                        || com.example.horsegenetics.neoforge.server.WildTurnover.stamped(there)) {
                    throw new GameTestAssertException(Component.literal(
                            "the horse in the realm is tamed, not persistent, or still on a wild lifetime"), 0);
                }
                String arrived = com.example.horsegenetics.neoforge.server.HorseRecords.of(there).displayName();
                if (!arrived.equals(name)) {
                    throw new GameTestAssertException(Component.literal(
                            "it left as " + name + " and arrived as " + arrived), 0);
                }
                there.discard();
            });
        });
    }

    /** <b>A tamed horse never leaves, and taming takes its lifetime away.</b> */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_TAMED_HORSE_STAYS =
            TEST_FUNCTIONS.register("wild_tamed_horse_stays", () -> ModGameTests::wildTamedHorseStays);

    private static void wildTamedHorseStays(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.animal.equine.Horse horse = naturalHorse(helper, BlockPos.ZERO);
        UUID id = horse.getUUID();
        helper.runAfterDelay(25L, () -> {
            if (!(level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse h)) {
                throw new GameTestAssertException(Component.literal("the horse was gone before it was tamed"), 0);
            }
            h.setTamed(true);
            age(h, level);
            // Two scans' worth.
            helper.runAfterDelay(65L, () -> {
                if (!(level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse still)
                        || !still.isAlive()) {
                    throw new GameTestAssertException(Component.literal("a tamed horse was turned over"), 0);
                }
                if (com.example.horsegenetics.neoforge.server.WildTurnover.stamped(still)) {
                    throw new GameTestAssertException(Component.literal(
                            "a tamed horse still carries its wild lifetime"), 0);
                }
                helper.succeed();
            });
        });
    }

    /**
     * <b>A wild horse from before the stamp gets a fresh lifetime; a kept one does not.</b>
     * Both are founded wild-herd members; only the persistent one - what every
     * deliberate path (release, cowboy, stall) makes a horse - must be left alone.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_OLD_HORSE_IS_STAMPED =
            TEST_FUNCTIONS.register("wild_old_horse_is_stamped", () -> ModGameTests::wildOldHorseIsStamped);

    private static void wildOldHorseIsStamped(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.animal.equine.Horse old = net.minecraft.world.entity.EntityType.HORSE.spawn(
                level, helper.absolutePos(BlockPos.ZERO), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        net.minecraft.world.entity.animal.equine.Horse kept = net.minecraft.world.entity.EntityType.HORSE.spawn(
                level, helper.absolutePos(new BlockPos(1, 0, 0)), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (old == null || kept == null) {
            throw new GameTestAssertException(Component.literal("a summoned horse was refused"), 0);
        }
        kept.setPersistenceRequired();
        if (com.example.horsegenetics.neoforge.server.WildTurnover.stamped(old)) {
            throw new GameTestAssertException(Component.literal("a summoned horse was stamped at spawn"), 0);
        }
        UUID oldId = old.getUUID();
        UUID keptId = kept.getUUID();
        helper.runAfterDelay(5L, () -> {
            for (UUID id : new UUID[] {oldId, keptId}) {
                if (level.getEntity(id) instanceof net.minecraft.world.entity.animal.equine.Horse h) {
                    var care = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get();
                    h.setData(care, h.getData(care).withWildHerd(id, "feral_mixed", "TRADITIONAL"));
                }
            }
            helper.succeedWhen(() -> {
                if (!(level.getEntity(oldId) instanceof net.minecraft.world.entity.animal.equine.Horse o)
                        || !com.example.horsegenetics.neoforge.server.WildTurnover.stamped(o)) {
                    throw new GameTestAssertException(Component.literal(
                            "an old non-persistent wild-herd horse has not been stamped yet"), 0);
                }
                if (level.getEntity(keptId) instanceof net.minecraft.world.entity.animal.equine.Horse k
                        && com.example.horsegenetics.neoforge.server.WildTurnover.stamped(k)) {
                    throw new GameTestAssertException(Component.literal(
                            "a persistent (kept) horse was given a wild lifetime"), 0);
                }
            });
        });
    }

    /**
     * <b>A top-up pack is stamped and founds as one herd.</b> The pack is placed by
     * the top-up's own code, so this is what its natural spawn from server code
     * does: every member stamped through BreedSpawnHandler, and, once founded, all
     * of them following one lead - one of their own.
     *
     * <p>Alone, on ground cleared of horses (issue #119). Founding joins any wild herd
     * within {@link com.example.horsegenetics.neoforge.server.HerdManager#HERD_RADIUS}
     * of a pack member, and the tests sit six blocks apart: in the shared batch the pack
     * was founded into one herd with the other wild tests' horses, a neighbour then aged
     * its own horse four days, and WildTurnover sent the whole herd on by its lead's
     * stamp ("moved on after 0.0 days").
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_PACK_FOUNDS_ONE_HERD =
            TEST_FUNCTIONS.register("wild_pack_founds_one_herd", () -> ModGameTests::wildPackFoundsOneHerd);

    private static void wildPackFoundsOneHerd(GameTestHelper helper) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        java.util.Map<UUID, net.minecraft.world.entity.animal.equine.Horse> pack = new java.util.LinkedHashMap<>();
        String[] refused = {null};
        helper.succeedWhen(() -> {
            if (refused[0] != null) {
                throw new GameTestAssertException(Component.literal(refused[0]), 0);
            }
            if (pack.isEmpty()) {
                fieldTicks(helper); // issue #70: the members are placed only where entities tick
                clearHerdReach(helper);
                refused[0] = spawnTestPack(helper, pack);
                if (refused[0] != null) {
                    throw new GameTestAssertException(Component.literal(refused[0]), 0);
                }
            }
            java.util.Set<UUID> leads = new java.util.HashSet<>();
            for (var member : pack.entrySet()) {
                if (!(level.getEntity(member.getKey()) instanceof net.minecraft.world.entity.animal.equine.Horse h)) {
                    throw new GameTestAssertException(Component.literal(
                            "a pack member is gone: " + goneState(level, member.getValue())), 0);
                }
                var care = h.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get());
                if (!care.inWildHerd()) {
                    throw new GameTestAssertException(Component.literal("a pack member is not founded into a herd yet"), 0);
                }
                leads.add(care.herd().orElseThrow());
            }
            if (leads.size() != 1) {
                throw new GameTestAssertException(Component.literal(
                        "one top-up pack founded " + leads.size() + " herds"), 0);
            }
            UUID lead = leads.iterator().next();
            if (!pack.containsKey(lead)) {
                throw new GameTestAssertException(Component.literal(
                        "the pack joined a herd led by a horse from outside it: " + lead), 0);
            }
        });
    }

    /**
     * Place the pack into {@code pack}; a message if the top-up refused it, else null.
     * Every member is stamped through BreedSpawnHandler.
     */
    private static String spawnTestPack(GameTestHelper helper,
                                        java.util.Map<UUID, net.minecraft.world.entity.animal.equine.Horse> pack) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos spot = com.example.horsegenetics.neoforge.server.WildTopUp.groundAt(level, origin.getX(), origin.getZ());
        java.util.List<net.minecraft.world.entity.animal.equine.Horse> placed =
                com.example.horsegenetics.neoforge.server.WildTopUp.spawnPack(level, spot, 4, level.getRandom());
        for (net.minecraft.world.entity.animal.equine.Horse h : placed) {
            pack.put(h.getUUID(), h);
        }
        if (placed.size() < 2) {
            return "the top-up placed " + placed.size() + " of a pack of 4 at " + spot.toShortString();
        }
        for (net.minecraft.world.entity.animal.equine.Horse h : placed) {
            if (!com.example.horsegenetics.neoforge.server.WildTurnover.stamped(h)) {
                return "a top-up horse carries no wild_born stamp";
            }
        }
        return null;
    }

    /**
     * Discard every horse a pack member's founding could reach (issue #119): the herd
     * radius, plus the pack's own spread and a margin for wander. Leftovers of earlier
     * batches would otherwise be joined, and their clock taken with them.
     */
    private static void clearHerdReach(GameTestHelper helper) {
        double reach = com.example.horsegenetics.neoforge.server.HerdManager.HERD_RADIUS + 16;
        for (net.minecraft.world.entity.animal.equine.Horse h : helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.animal.equine.Horse.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(reach, 16, reach))) {
            h.discard();
        }
    }

    /** Why a horse the level no longer finds is gone, for a failure message (issue #119). */
    private static String goneState(ServerLevel level, net.minecraft.world.entity.animal.equine.Horse h) {
        var care = h.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get());
        return h.getUUID() + " removed " + h.getRemovalReason() + " at " + h.blockPosition().toShortString()
                + ", chunk ticking " + level.isPositionEntityTicking(h.blockPosition())
                + ", herd " + care.herd().map(UUID::toString).orElse("none")
                + ", born " + h.getPersistentData().getLongOr(
                        com.example.horsegenetics.neoforge.server.WildTurnover.BORN_KEY, -1L)
                + " (now " + level.getGameTime() + ")";
    }

    /** <b>A cell is rolled once a day:</b> the second pass on the same day does nothing and spawns nothing. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_CELL_ROLLS_ONCE_A_DAY =
            TEST_FUNCTIONS.register("wild_cell_rolls_once_a_day", () -> ModGameTests::wildCellRollsOnceADay);

    private static void wildCellRollsOnceADay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The largest minimum, so the first pass is sure to roll whatever stands here.
        com.example.horsegenetics.common.wild.TopUpPlan.Settings settings =
                new com.example.horsegenetics.common.wild.TopUpPlan.Settings(
                        com.example.horsegenetics.common.wild.TopUpPlan.MAX_MINIMUM, 2, 96);
        BlockPos at = helper.absolutePos(BlockPos.ZERO);
        long cell = com.example.horsegenetics.common.wild.TopUpPlan.cellKey(
                com.example.horsegenetics.common.wild.TopUpPlan.cellOf(at.getX(), 2),
                com.example.horsegenetics.common.wild.TopUpPlan.cellOf(at.getZ(), 2));
        long today = com.example.horsegenetics.common.wild.TopUpPlan.dayOf(level.getGameTime());
        long sig = 0x5eed_5eedL;
        com.example.horsegenetics.neoforge.data.WildCellLedger ledger =
                com.example.horsegenetics.neoforge.data.WildCellLedger.get(level);
        com.example.horsegenetics.neoforge.server.WildTopUp.rollCell(level, cell, settings, today, sig, ledger);
        if (!com.example.horsegenetics.common.wild.TopUpPlan.current(ledger.stampOf(cell, today), today, sig)) {
            throw new GameTestAssertException(Component.literal("a rolled cell was not stamped"), 0);
        }
        int second = com.example.horsegenetics.neoforge.server.WildTopUp.rollCell(level, cell, settings, today, sig, ledger);
        if (second != 0) {
            throw new GameTestAssertException(Component.literal(
                    "the second pass on the same day spawned " + second + " horses"), 0);
        }
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(ModGameTests::onRegisterGameTests);
    }

    /**
     * Fired from inside {@code RegistryDataLoader.load}, after the datapack
     * registries are populated and before they freeze, and only when
     * {@code GameTestHooks.isGametestEnabled()} - so this costs a production
     * server nothing.
     */
    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        // ONE environment for all of them. registerEnvironment is a registry put,
        // not a get-or-create, so calling it per test throws "Adding duplicate key
        // ... horsegenetics:default" out of RegistryDataLoader and takes the whole
        // run down with exit -1 before a single test runs. The environment is also
        // the batch key, so sharing one is what keeps them in a single batch - except
        // the few that reach past their cell, which registerAlone gives a key each.
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "default"),
                        new TestEnvironmentDefinition.AllOf(java.util.List.of()));
        register(event, environment, HARNESS_REACHES_THE_WORLD, 100);
        register(event, environment, DOUBLE_GATE_REDSTONE, 100);
        // Two place-and-break cycles, all inside one tick each.
        register(event, environment, DOUBLE_GATE_DROPS_ONE, 100);
        // One recipe lookup in a single tick.
        register(event, environment, DOUBLE_GATE_CRAFTS, 100);
        // The census is one synchronous burst of worldgen arithmetic inside a
        // single tick, so its tick budget is not what bounds it - the sample
        // size is. The generous number is for the environment overrides, which
        // are meant to be raised a long way.
        register(event, environment, HOMESTEAD_STILL_GENERATES, 400);
        // The same kind of census, for the stables' ground.
        register(event, environment, STABLES_STAND_ON_LEVEL_GROUND, 400);
        // One pass over the recipe list inside a single tick; the budget is slack.
        register(event, environment, EVERY_RECIPE_ENCODES, 100);
        // Five recipe lookups in one tick.
        register(event, environment, STASIS_CHAMBERS_CRAFT, 100);
        register(event, environment, BUILDING_BLOCKS_SURVIVES, 100);
        // Two tag lookups in one tick.
        register(event, environment, HAY_IS_STILL_A_BALE, 100);
        // Founding (up to 20 ticks), a search every 40, a short walk and one mouthful.
        register(event, environment, HAY_UNDER_A_LOW_ROOF_IS_EATEN, 400);
        register(event, environment, STACKED_HAY_IS_EATEN, 400);
        // One horse spawned, saved and read back, all inside a single tick.
        register(event, environment, STASIS_TAG_IS_READABLE, 100);
        // Three codec round trips in one tick.
        register(event, environment, OLD_PAPERS_STILL_READ, 100);
        // Nine codec round trips through bytes, in one tick.
        register(event, environment, LONG_EPIGENOME_STILL_SAVES, 100);
        // Leash, untie, then five ticks for a ground drop to become visible.
        register(event, environment, WHISTLED_LEAD_COMES_BACK, 100);
        // One horse, two mock players, all synchronous - nothing is dropped.
        register(event, environment, VANILLA_LEAD_COMES_BACK, 100);
        // One chunk generated and decorated, then six block reads, in one tick.
        register(event, environment, HORSE_REALM_IS_BUILT, 200);
        // Four thousand hashes; no world touched at all.
        register(event, environment, HORSE_REALM_GRID_IS_STABLE, 100);
        // One chunk generated, one lifted; all in one tick.
        register(event, environment, REALM_LIFT_LOADS_NO_NEIGHBOUR, 200);
        // One chunk generated, its edge built; all in one tick.
        register(event, environment, DEBUG_CORRIDOR_LOADS_NO_NEIGHBOUR, 200);
        // Ten ticks to be founded, a backup and a plan (two flushing saves), then
        // a raise on the next server tick.
        register(event, environment, REALM_BACKUP_RAISES_A_LOST_HORSE, 200);
        // One recipe lookup in a single tick.
        register(event, environment, FREEDOM_STICK_CRAFTS, 100);
        // Spawn, ten ticks to be founded, kill, raise - then read the result.
        register(event, environment, A_DEAD_HORSE_COMES_BACK_WHOLE, 200);
        // Two scratch-box fills and an exhaustive scan, all inside one tick.
        register(event, environment, WAYSTONE_CLEARANCE_IS_NEAREST, 200);
        register(event, environment, GROWN_HORSE_LEAVES_THE_CEILING, 200);
        // Pure arithmetic on a record; no world touched.
        register(event, environment, JOCKEY_PASSES_ADD_UP, 100);
        // One horse spawned and three events posted, all inside a single tick.
        register(event, environment, MOUNTED_MINING_IS_EXEMPT, 100);
        // A small scratch box filled and read, all inside one tick.
        register(event, environment, NO_HORSE_STANDS_IN_POWDER_SNOW, 100);
        // Five stalls built and read, all inside one tick.
        register(event, environment, STALL_SHAPES_BIND_AS_ONE_ROOM, 100);
        // A scratch shore and basin built, three horses spawned and checked; one tick.
        register(event, environment, COWBOY_STOCK_LEAVES_THE_WHIRLPOOL, 100);
        // One horse spawned, two attachment writes, two calls; one tick.
        register(event, environment, WHISTLE_NEEDS_BOND, 100);
        // Four horses spawned and seven interact events posted, all in one tick.
        register(event, environment, RIGHT_CLICK_EQUIPS_TACK, 100);
        // One horse, a dozen registry probes, four interact events and a drops event; one tick.
        register(event, environment, HORSE_CARRIES_CHESTS, 100);
        // One horse and one unjoined player; a chest stood at the bottom of the world for four ticks.
        register(event, environment, HOSTED_CHEST_IS_A_REAL_BLOCK, 100);
        // Builds one instance of every mob it classifies, inside one tick.
        register(event, environment, CHAOS_ROSTER, 100);
        // One bank filled and one button pressed twice, then five ticks for the
        // dropped chamber to be findable on the ground.
        register(event, environment, STASIS_BANK_EJECTS_EMPTIES, 100);
        // Two mock players and a dozen inventory reads, all inside one tick.
        register(event, environment, SEND_HOME_PAYMENT, 100);
        // A converted zombie and skeleton: one tick to convert, a few to settle.
        register(event, environment, UNDEAD_HORSES_CONVERT, 100);
        // Twelve ticks armed, then disarmed and converted.
        register(event, environment, UNDEAD_TRAP_WAITS, 100);
        register(event, environment, UNDEAD_KEEP_VANILLA, 100);
        register(event, environment, UNDEAD_OFF_BY_DEFAULT, 100);
        register(event, environment, UNDEAD_TEST_COMMAND, 300);
        registerAlone(event, ORDER_STAY_WALKS_BACK, 300);
        register(event, environment, ORDER_YIELDS_TO_NEEDS, 100);
        register(event, environment, ORDER_CLEARS, 100);
        // Twenty-five ticks to be founded, then the gate is read once.
        register(event, environment, ORDER_COMBAT_GATE, 100);
        // Founded at 25, then three owner-mirror passes, one every 40 ticks.
        register(event, environment, WHISTLE_SEES_THE_OWNER, 300);
        // Synchronous: one cross-world move, checked in the same tick.
        register(event, environment, TURNOUT_RELEASES_THE_HORSE_THAT_ARRIVED, 20);
        // Founded at 25, then a combat scan once a second - after the wait for the
        // field's forced chunks to tick (issue #70: up to 369 ticks seen in 120 tries,
        // most of them one), so a thousand ticks on top of what the test itself needs.
        registerAlone(event, ORDER_HUNT_PICKS_A_MONSTER, 1200);
        registerAlone(event, ORDER_GUARD_HOLDS_ITS_REACH, 1300);
        register(event, environment, ORDER_GRAZE_TETHER, 200);
        // Twenty-five ticks to be founded, then a scan (every thirty) to act on it.
        register(event, environment, WILD_HORSE_MOVES_ON, 200);
        register(event, environment, WILD_TOUCHED_HORSE_GOES_TO_THE_REALM, 200);
        register(event, environment, WILD_TAMED_HORSE_STAYS, 200);
        register(event, environment, WILD_OLD_HORSE_IS_STAMPED, 200);
        // Alone (issue #119), so no other test's horse is in the herd's reach. The wait
        // for the field's forced chunks (issue #70: up to 369 ticks), then a pack placed
        // at once and about twenty ticks for the founder to run.
        registerAlone(event, WILD_PACK_FOUNDS_ONE_HERD, 1200);
        // Two synchronous passes over one cell.
        register(event, environment, WILD_CELL_ROLLS_ONCE_A_DAY, 100);
    }

    /**
     * <b>A resurrected horse is the same horse, alive, and wearing nothing.</b>
     *
     * <p>{@code /horseresurrect} rests on three things that are each invisible
     * when they break, and this is the only place that can see any of them.
     *
     * <p><b>The identity.</b> The whole point of keeping an entity tag rather
     * than rebuilding from the {@code HorseRecord} is that the tag carries the
     * UUID, and the UUID is what every pedigree, stall sign and bound whistle
     * keys on. A resurrection that quietly minted a new id would look perfect -
     * right name, right coat, right stats - and would have orphaned every foal
     * the horse ever had. Nothing in the game would say so.
     *
     * <p><b>The health.</b> The snapshot is taken in {@link LivingDeathEvent},
     * so the tag says zero. {@code HorseResurrection.revive} refills it from the
     * genotype; without that the horse stands up and dies again inside a tick,
     * and the death loop would read as "the command does nothing".
     *
     * <p><b>The gear.</b> This is the one that is a <i>dupe</i>. The snapshot is
     * taken before vanilla drops the saddle and the armour, so both are in the
     * tag <b>and</b> on the ground. {@code revive} empties the slots, and if
     * that ever stops matching where a horse keeps its tack - the saddle moved
     * to its own equipment slot once already - the test fails here instead of a
     * player quietly printing saddles.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> A_DEAD_HORSE_COMES_BACK_WHOLE =
            TEST_FUNCTIONS.register("a_dead_horse_comes_back_whole",
                    () -> ModGameTests::aDeadHorseComesBackWhole);

    private static void aDeadHorseComesBackWhole(GameTestHelper helper) {
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        // Founded on a later tick, not the one it spawned on - the same wait
        // STASIS_TAG_IS_READABLE has to make, and for the same reason.
        helper.runAfterDelay(10L, () -> killAndRaise(helper, horse));
    }

    /**
     * <b>The afterlife store's size budget, on a real horse's tag</b> (issue
     * #15). A kept horse has a cost; a wake saved before the budget existed -
     * no {@code bytes} field - is measured as it loads, so an old save is
     * budgeted too (rule 10); and {@code trimTo} lets it go over budget and
     * keeps it under "no cap". Run on a decoded copy, so the server's own store
     * still holds the wake for the resurrection that follows.
     */
    private static void afterlifeBudgetHolds(com.example.horsegenetics.neoforge.data.HorseAfterlife.Wake wake) {
        if (wake.bytes() <= 0) {
            throw new GameTestAssertException(Component.literal(
                    "a freshly kept horse has no cost (" + wake.bytes() + " bytes), so the size"
                            + " budget can never let it go. Check HorseAfterlife.Wake.of / measure."), 0);
        }
        net.minecraft.nbt.Tag encoded = com.example.horsegenetics.neoforge.data.HorseAfterlife.Wake.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, wake).getOrThrow();
        net.minecraft.nbt.CompoundTag old = ((net.minecraft.nbt.CompoundTag) encoded).copy();
        old.remove("bytes");
        net.minecraft.nbt.ListTag wakes = new net.minecraft.nbt.ListTag();
        wakes.add(old);
        net.minecraft.nbt.CompoundTag file = new net.minecraft.nbt.CompoundTag();
        file.put("wakes", wakes);
        com.example.horsegenetics.neoforge.data.HorseAfterlife loaded =
                com.example.horsegenetics.neoforge.data.HorseAfterlife.CODEC
                        .parse(net.minecraft.nbt.NbtOps.INSTANCE, file).getOrThrow();
        long measured = loaded.lookup(wake.horse()).map(w -> w.bytes()).orElse(-1L);
        if (measured != wake.bytes()) {
            throw new GameTestAssertException(Component.literal(
                    "a wake saved before the size budget loaded with a cost of " + measured
                            + " bytes, not the " + wake.bytes() + " it measures at death - an old"
                            + " save's dead horses would sit outside the budget."), 0);
        }
        if (!loaded.trimTo(0).isEmpty() || loaded.size() != 1) {
            throw new GameTestAssertException(Component.literal(
                    "ops.resurrect_budget_mb = 0 is meant to be no cap, and trimTo(0) dropped a horse"), 0);
        }
        if (loaded.trimTo(wake.bytes() - 1).size() != 1 || loaded.size() != 0) {
            throw new GameTestAssertException(Component.literal(
                    "a store one byte over its budget kept its only horse - trimTo is not enforcing"
                            + " ops.resurrect_budget_mb"), 0);
        }
    }

    private static void killAndRaise(GameTestHelper helper,
                                     net.minecraft.world.entity.animal.equine.Horse horse) {
        if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
            throw new GameTestAssertException(Component.literal(
                    "the spawned horse still has no record ten ticks in - this test's premise is"
                            + " broken, not the resurrection"), 0);
        }
        // Only an OWNED horse is kept, so give it one. A plain UUID rather than
        // a real player: HorseAfterlifeHandler reads the record's ownerId and
        // nothing else, and a gametest has no players in it.
        java.util.UUID owner = java.util.UUID.randomUUID();
        com.example.horsegenetics.neoforge.server.HorseRecords.setOwner(horse, owner);
        horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE,
                new ItemStack(Items.SADDLE));

        java.util.UUID id = horse.getUUID();
        String name = com.example.horsegenetics.neoforge.server.HorseRecords.of(horse).displayName();
        net.minecraft.server.level.ServerLevel level = helper.getLevel();

        horse.kill(level);

        com.example.horsegenetics.neoforge.data.HorseAfterlife afterlife =
                com.example.horsegenetics.neoforge.data.HorseAfterlife.get(level.getServer());
        com.example.horsegenetics.neoforge.data.HorseAfterlife.Wake wake =
                afterlife.lookup(id).orElse(null);
        if (wake == null) {
            throw new GameTestAssertException(Component.literal(
                    "an owned horse died and nothing was kept - /horseresurrect will never have"
                            + " anything to offer. Check HorseAfterlifeHandler.onHorseDeath and"
                            + " whether the record still carries an ownerId at death."), 0);
        }
        afterlifeBudgetHolds(wake);

        // The entity is discarded by now, so the id is free for it to take back.
        net.minecraft.world.entity.animal.equine.Horse raised =
                com.example.horsegenetics.neoforge.server.HorseResurrection.raise(
                        level, helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO), 0.0F,
                        wake.snapshot());
        if (raised == null) {
            throw new GameTestAssertException(Component.literal(
                    "the kept snapshot of " + name + " could not be raised at all"), 0);
        }

        if (!raised.getUUID().equals(id)) {
            throw new GameTestAssertException(Component.literal(
                    "the resurrected horse has a NEW id (" + raised.getUUID() + " was " + id
                            + "). It looks like the same horse and is not one: every foal's"
                            + " pedigree, every stall sign and any bound whistle still point at"
                            + " the dead id. Check that StasisSnapshot is still carrying the"
                            + " entity's UUID through saveWithoutId."), 0);
        }
        if (!raised.isAlive() || raised.getHealth() <= 0.0F) {
            throw new GameTestAssertException(Component.literal(
                    name + " came back at " + raised.getHealth() + " health and will die again"
                            + " immediately. HorseResurrection.revive is meant to refill it from"
                            + " the genotype - see applyTraitsToEntity(.., true)."), 0);
        }
        ItemStack saddle = raised.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE);
        if (!saddle.isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    name + " came back still wearing its saddle. It also dropped one when it"
                            + " died, so that is a duplication bug. HorseResurrection.revive"
                            + " clears every EquipmentSlot; check the saddle has not moved to a"
                            + " container the slots do not cover."), 0);
        }
        String raisedName = com.example.horsegenetics.neoforge.server.HorseRecords.of(raised).displayName();
        if (!raisedName.equals(name)) {
            throw new GameTestAssertException(Component.literal(
                    "the resurrected horse is called \"" + raisedName + "\" and the dead one was \""
                            + name + "\" - the record did not survive the tag round trip"), 0);
        }
        helper.succeed();
    }

    /**
     * <b>A lead survives the horse being whistled out from under it.</b>
     *
     * <p>Every path in this mod that teleports a horse has to untie it first, and
     * vanilla's {@code dropLeash()} does that by dropping an {@code Items.LEAD}
     * <i>where the horse was standing</i> - which for an ender whistle or an
     * interdimensional ticket is a different dimension, where it is simply gone.
     * {@link HorseLeads#untieFor} hands it to the player who caused the move
     * instead; see {@code behaviour.leads_return}.
     *
     * <p><b>Here rather than in JUnit</b> for the usual reason: the NeoForge
     * module's test classpath carries no Minecraft, and the whole assertion is
     * about a real {@code Leashable}, a real inventory and whether an
     * {@code ItemEntity} appeared in a real level.
     *
     * <p>It asserts the two halves separately, because passing one and failing
     * the other is the shape of the bug: <b>no lead on the ground</b> (the leak
     * this closes) and <b>a lead in the player's pack</b> (that it went
     * somewhere rather than nowhere). Getting only the first would be worse than
     * the behaviour it replaced.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WHISTLED_LEAD_COMES_BACK =
            TEST_FUNCTIONS.register("whistled_lead_comes_back", () -> ModGameTests::whistledLeadComesBack);

    private static void whistledLeadComesBack(GameTestHelper helper) {
        if (!com.example.horsegenetics.neoforge.ServerConfig.leadsReturn()) {
            throw new GameTestAssertException(Component.literal(
                    "behaviour.leads_return is off in this run's server config, so this test"
                            + " asserts the wrong branch - turn it back on rather than deleting"
                            + " the test"), 0);
        }
        // Not makeMockServerPlayerInLevel(): that one is @Deprecated(forRemoval)
        // and really joins the player list. A leash holder needs to be an Entity
        // and nothing more - Leashable.setLeashedTo never asks whether it is in
        // the level - and this player's inventory is a real one.
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        horse.setLeashedTo(player, true);
        if (!horse.isLeashed()) {
            throw new GameTestAssertException(Component.literal(
                    "the test horse would not take a leash at all - this test's premise is"
                            + " broken, not HorseLeads"), 0);
        }

        HorseLeads.untieFor(horse, player);

        if (horse.isLeashed()) {
            throw new GameTestAssertException(Component.literal(
                    "HorseLeads.untieFor left the horse leashed - a teleport would drag the"
                            + " leash to a holder that is no longer anywhere near it"), 0);
        }
        if (!player.getInventory().contains(st -> st.is(Items.LEAD))) {
            throw new GameTestAssertException(Component.literal(
                    "the lead did not reach the player who whistled - it has been destroyed"
                            + " rather than returned"), 0);
        }
        // Five ticks, because a spawnAtLocation on the tick under test would not
        // necessarily be findable on that same tick - and this assertion is the
        // one that passes wrongly if it is asked too early.
        helper.runAfterDelay(5L, () -> {
            helper.assertItemEntityNotPresent(Items.LEAD, BlockPos.ZERO, 16.0);
            helper.succeed();
        });
    }

    /**
     * <b>A lead that <i>vanilla</i> drops goes back to whoever tied it on.</b>
     * The other half of {@link #WHISTLED_LEAD_COMES_BACK}, and a much less
     * direct piece of machinery: two mixins and a stored UUID, where that one
     * was a method call.
     *
     * <p>Four things are asserted, and three of them are about the parts that
     * are <b>invisible when they break</b>.
     *
     * <ol>
     *   <li><b>The placer is recorded at all.</b> If {@code LeashPlacerMixin}
     *       never fires there is nobody to give anything to and the whole
     *       feature is silently absent - which looks exactly like vanilla,
     *       because it <i>is</i> vanilla.</li>
     *   <li><b>Tying to a fence does not erase it.</b> This is the load-bearing
     *       claim in {@code LeashPlacerMixin}'s javadoc and it is a claim about
     *       vanilla's control flow, not about this mod: that
     *       {@code Leashable.setLeashedTo} reaches {@code setLeashData} only on
     *       a <i>first</i> attachment and takes a {@code setLeashHolder} branch
     *       afterwards. If that is wrong, the fence case - the one the whole
     *       design exists for, since a knot has no player in it - loses its
     *       placer at the moment of tying.</li>
     *   <li><b>An unreachable placer is refused</b>, so vanilla drops the lead
     *       where it always did. The rule is never hold a lead for later, and
     *       a lead that goes nowhere is worse than doing nothing at all.</li>
     *   <li><b>The right player gets it, the record is cleared, and a
     *       different player is refused</b> - a lead handed to whoever happens
     *       to ask would be a quiet theft, and a record left behind would mint
     *       a second lead on the next drop.</li>
     * </ol>
     *
     * <p><b>It drops nothing on the ground, deliberately.</b> The first draft
     * asserted case 3 by really calling {@code dropLeash()} and finding the
     * lead on the floor - and it broke {@code whistled_lead_comes_back}, whose
     * own {@code assertItemEntityNotPresent} reaches sixteen blocks and found
     * this test's lead in the neighbouring plot. A test that litters the shared
     * world is a test that fails its neighbours.
     *
     * <p><b>Nor does it make a real player.</b> A gametest server has nobody on
     * it, so the only player {@code getPlayerList()} could find is one built by
     * {@code makeMockServerPlayerInLevel} - {@code @Deprecated(forRemoval)},
     * and it really joins the player list, which fires this mod's join payloads
     * down an {@code EmbeddedChannel} that has negotiated nothing and takes the
     * run down with <i>"Payload horsegenetics:gene_database_sync may not be
     * sent to the client"</i>. So the test drives
     * {@link HorseLeads#giveRecordedLeadTo} and leaves the one line above it,
     * the player-list lookup, to the in-game check.
     *
     * <p><b>That the mixins are applied is not this test's job.</b> The mixin
     * config's {@code defaultRequire} of 1 is a stronger guarantee than a
     * gametest could give: a target that stops matching refuses to boot the
     * game rather than quietly doing nothing. What is asserted here is
     * everything that would still be wrong with both mixins happily applied.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VANILLA_LEAD_COMES_BACK =
            TEST_FUNCTIONS.register("vanilla_lead_comes_back", () -> ModGameTests::vanillaLeadComesBack);

    private static void vanillaLeadComesBack(GameTestHelper helper) {
        if (!com.example.horsegenetics.neoforge.ServerConfig.leadsReturn()) {
            throw new GameTestAssertException(Component.literal(
                    "behaviour.leads_return is off in this run's server config, so this test"
                            + " asserts the wrong branch - turn it back on rather than deleting"
                            + " the test"), 0);
        }

        net.minecraft.world.entity.player.Player placer =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player stranger =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        if (placer.getUUID().equals(stranger.getUUID())) {
            throw new GameTestAssertException(Component.literal(
                    "the two mock players share a UUID, so the wrong-player case below would"
                            + " pass whatever the code did - the premise is broken"), 0);
        }
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);

        // --- 1. the placer is written down when the lead goes on -------------
        horse.setLeashedTo(placer, true);
        if (!horse.isLeashed()) {
            throw new GameTestAssertException(Component.literal(
                    "the test horse would not take a leash at all - the premise is broken,"
                            + " not the mixin"), 0);
        }
        if (!HorseLeads.placerOf(horse).filter(placer.getUUID()::equals).isPresent()) {
            throw new GameTestAssertException(Component.literal(
                    "nobody was recorded as tying the lead on, so there would be nobody to"
                            + " give it back to and the feature is silently absent."
                            + " LeashPlacerMixin is not firing on Mob.setLeashData."), 0);
        }

        // --- 2. and moving that lead onto a fence knot does not wipe it ------
        net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot =
                net.minecraft.world.entity.decoration.LeashFenceKnotEntity.getOrCreateKnot(
                        helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        horse.setLeashedTo(knot, true);
        if (!HorseLeads.placerOf(horse).filter(placer.getUUID()::equals).isPresent()) {
            throw new GameTestAssertException(Component.literal(
                    "tying the horse to a fence erased who tied the lead on. A knot has no"
                            + " player in it anywhere, so that is precisely the case the stored"
                            + " UUID exists for - vanilla's setLeashedTo is no longer taking the"
                            + " setLeashHolder branch this rests on."), 0);
        }

        // --- 3. a placer nobody can reach is refused, so vanilla drops it ----
        if (HorseLeads.divertLeadDrop(horse)) {
            throw new GameTestAssertException(Component.literal(
                    "the lead was taken off the ground for a placer who is not on the server"
                            + " - it has gone nowhere at all, which is worse than the ground"
                            + " drop this replaces"), 0);
        }

        // --- 4. the right player gets it; the wrong one does not -------------
        if (HorseLeads.giveRecordedLeadTo(horse, stranger)) {
            throw new GameTestAssertException(Component.literal(
                    "a player who did not tie the lead on was handed it anyway"), 0);
        }
        if (!HorseLeads.giveRecordedLeadTo(horse, placer)) {
            throw new GameTestAssertException(Component.literal(
                    "the player who tied the lead on was refused it"), 0);
        }
        if (!placer.getInventory().contains(st -> st.is(Items.LEAD))) {
            throw new GameTestAssertException(Component.literal(
                    "giveRecordedLeadTo said yes but no lead reached the pack - it has been"
                            + " destroyed rather than returned"), 0);
        }
        if (HorseLeads.placerOf(horse).isPresent()) {
            throw new GameTestAssertException(Component.literal(
                    "the placer was not cleared after the lead was handed over, so a second"
                            + " drop would mint a second lead out of nothing"), 0);
        }
        if (HorseLeads.giveRecordedLeadTo(horse, placer)) {
            throw new GameTestAssertException(Component.literal(
                    "a second lead was handed out for the same leash - this is a dupe"), 0);
        }
        helper.succeed();
    }

    /**
     * <b>A rider mines at full speed; nobody else gets a boost.</b>
     *
     * <p>{@link com.example.horsegenetics.neoforge.server.MountedMiningHandler}
     * multiplies break speed by five to undo vanilla's airborne penalty, and the
     * dangerous failure is not that it does nothing - it is that it fires when
     * the penalty was never charged and hands out <b>five times</b> the mining
     * speed. So the three branches are asserted separately: the exemption, and
     * the two refusals that bound it.
     *
     * <p><b>Here rather than in JUnit</b> for the module's usual reason - the
     * NeoForge test classpath carries no Minecraft - and it goes through
     * {@code NeoForge.EVENT_BUS} rather than calling the handler directly, so a
     * handler that was written but never registered fails it.
     *
     * <p>The ground flags are <b>set by hand</b> rather than played out. Whether
     * a real mounted player's {@code onGround} is genuinely false is a claim
     * about vanilla's movement code, not about this handler, and it is on the
     * page's Verification tab to be timed in-game. What this test owns is that
     * given each posture, the handler does the right thing.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOUNTED_MINING_IS_EXEMPT =
            TEST_FUNCTIONS.register("mounted_mining_is_exempt", () -> ModGameTests::mountedMiningIsExempt);

    private static void mountedMiningIsExempt(GameTestHelper helper) {
        if (!com.example.horsegenetics.neoforge.ServerConfig.mountedMiningPenaltyRemoved()) {
            throw new GameTestAssertException(Component.literal(
                    "behaviour.mounted_mining_penalty_removed is off in this run's server"
                            + " config, so this test asserts the wrong branch - turn it back on"
                            + " rather than deleting the test"), 0);
        }
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        // force, and without the mount game event: a mock player is not in the
        // level's entity list, and only getVehicle() matters to the handler.
        player.startRiding(horse, true, false);
        if (player.getVehicle() != horse) {
            throw new GameTestAssertException(Component.literal(
                    "the mock player would not mount at all - this test's premise is broken,"
                            + " not MountedMiningHandler"), 0);
        }

        // Riding a horse that is standing still: vanilla charged the fifth, so
        // the handler gives it back whole.
        horse.setOnGround(true);
        player.setOnGround(false);
        assertBreakSpeed(helper, player, 2.0F, 10.0F,
                "a rider on a standing horse was not exempted from the airborne penalty");

        // The dangerous one. Nothing was divided, so nothing may be multiplied.
        player.setOnGround(true);
        assertBreakSpeed(helper, player, 2.0F, 2.0F,
                "break speed was multiplied for a player vanilla never penalised - this is a"
                        + " 5x mining boost, not a comfort fix");

        // Horse mid-jump or flying: the rider really is in the air.
        player.setOnGround(false);
        horse.setOnGround(false);
        assertBreakSpeed(helper, player, 2.0F, 2.0F,
                "the penalty was removed for a rider whose horse was itself off the ground");

        helper.succeed();
    }

    /**
     * Posts a real {@code BreakSpeed} for {@code player} at {@code given} and
     * asserts the listeners left it at {@code expected}.
     */
    private static void assertBreakSpeed(GameTestHelper helper,
            net.minecraft.world.entity.player.Player player,
            float given, float expected, String complaint) {
        net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event =
                new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(
                        player, Blocks.STONE.defaultBlockState(), given, BlockPos.ZERO);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        // Exact powers of two through one multiply, so the epsilon is only
        // guarding against a future non-integer factor, not against drift.
        if (Math.abs(event.getNewSpeed() - expected) > 1.0E-4F) {
            throw new GameTestAssertException(Component.literal(
                    complaint + " - gave " + given + ", expected " + expected
                            + ", got " + event.getNewSpeed()), 0);
        }
    }

    /**
     * <b>An ender whistle will not bind to a horse that does not trust you.</b>
     *
     * <p>The gate is one tier boundary, so the test is the two values either
     * side of it: bond 60 refused, bond 61 accepted. Anything else would be
     * testing {@code behaviourTier()}, which is not this feature.
     *
     * <p>It calls {@link EnderWhistleCalls#bindRefusal} rather than posting an
     * {@code EntityInteract}, because that method is the seam the real
     * {@code onBind} goes through - the handler has exactly one call to it, so a
     * gate that worked here and was never wired in could not happen without
     * deleting that line. Posting the interact event would additionally test
     * vanilla's interaction plumbing, which is not what is new.
     *
     * <p>A gametest and not JUnit for the module's usual reason, plus a specific
     * one: the bond lives on a real data attachment, and setting it needs a real
     * horse in a real level.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WHISTLE_NEEDS_BOND =
            TEST_FUNCTIONS.register("whistle_needs_bond", () -> ModGameTests::whistleNeedsBond);

    private static void whistleNeedsBond(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        horse.setTamed(true);
        horse.setOwner(player);
        if (com.example.horsegenetics.neoforge.server.HorseOwnership
                .bindRefusal(horse, player, "Test") != null) {
            throw new GameTestAssertException(Component.literal(
                    "the test horse is not owned by the mock player, so this would be testing"
                            + " ownership rather than bond - the premise is broken, not the gate"), 0);
        }

        setBond(horse, 60);
        if (EnderWhistleCalls.bindRefusal(horse, player, "Test") == null) {
            throw new GameTestAssertException(Component.literal(
                    "a horse one point below the bond threshold accepted a whistle - the gate"
                            + " is not being applied"), 0);
        }
        setBond(horse, 61);
        String atTier = EnderWhistleCalls.bindRefusal(horse, player, "Test");
        if (atTier != null) {
            throw new GameTestAssertException(Component.literal(
                    "a horse at the bond threshold was refused: " + atTier), 0);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // The command whistle's orders (HorseOrdering, OrderStayGoal, OrderFollowGoal).
    // ------------------------------------------------------------------

    /** An owned, bonded horse of a mock player's, with an order set as the server sets one. */
    private static net.minecraft.world.entity.animal.equine.Horse orderedHorse(GameTestHelper helper,
            net.minecraft.world.entity.player.Player owner, com.example.horsegenetics.common.care.HorseOrder order) {
        return orderedHorse(helper, owner, order, BlockPos.ZERO);
    }

    private static net.minecraft.world.entity.animal.equine.Horse orderedHorse(GameTestHelper helper,
            net.minecraft.world.entity.player.Player owner, com.example.horsegenetics.common.care.HorseOrder order,
            BlockPos rel) {
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, rel);
        horse.setTamed(true);
        horse.setOwner(owner);
        setBond(horse, 90);
        horse.setData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_ORDER.get(),
                new com.example.horsegenetics.neoforge.data.HorseOrderAttachment(order,
                        order.anchored() ? java.util.Optional.of(horse.blockPosition()) : java.util.Optional.empty(),
                        horse.level().dimension().identifier().toString(), java.util.Optional.of(owner.getUUID())));
        return horse;
    }

    /**
     * <b>A stayed horse that something moved walks back to its spot</b> - the command
     * whistle treatment's "a stayed horse drifts" risk, and the walk-back that answers it.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_STAY_WALKS_BACK =
            TEST_FUNCTIONS.register("order_stay_walks_back", () -> ModGameTests::orderStayWalksBack);

    /** Counts the ticks a goal of {@code type} has been running, in {@code counter[0]}; true past 60. */
    private static boolean running(net.minecraft.world.entity.animal.equine.Horse horse, Class<?> type, int[] counter) {
        boolean now = horse.goalSelector.getAvailableGoals().stream()
                .anyMatch(w -> w.isRunning() && type.isInstance(w.getGoal()));
        counter[0] = now ? counter[0] + 1 : counter[0];
        return counter[0] > 60;
    }

    private static void orderStayWalksBack(GameTestHelper helper) {
        aloneOnClearGround(helper);
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // Beside the structure, not on it: the harness stands each test on a one-block
        // pedestal (its test-instance block), and a horse ordered up there and pushed off
        // can never climb back - which is right, and not what this test is about.
        net.minecraft.world.entity.animal.equine.Horse horse =
                orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.STAY, new BlockPos(2, 0, 2));
        BlockPos anchor = com.example.horsegenetics.neoforge.server.HorseOrdering.groundAt(helper.getLevel(), horse
                .getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_ORDER.get()).anchor().orElseThrow());
        // Three blocks, and toward the structure rather than away: a test that succeeds
        // discards every entity within one block of its structure (GameTestInfo.succeed),
        // and the grid puts the next one six blocks along - a horse moved five landed in
        // that sweep.
        horse.snapTo(horse.getX(), horse.getY(), horse.getZ() - 3, horse.getYRot(), 0F);
        int[] stayTicks = {0};
        helper.runAfterDelay(2L, () -> {
            double dx0 = horse.getX() - (anchor.getX() + 0.5);
            double dz0 = horse.getZ() - (anchor.getZ() + 0.5);
            if (dx0 * dx0 + dz0 * dz0 < 2.5 * 2.5) {
                throw new GameTestAssertException(Component.literal("the horse did not stay moved ("
                        + Math.sqrt(dx0 * dx0 + dz0 * dz0) + " from its spot) - the premise is broken"), 0);
            }
        });
        boolean[] headedHome = {false};
        boolean[] plain = {false};
        helper.succeedWhen(() -> {
            // A plain horse, once founded (issue #40): a founding roll that hunts horses
            // (Ade/Ade, Aae/Aae, ...) takes any horse within sixteen blocks, and a fight
            // out-ranks Stay by design - so it never stood long enough, or never came
            // home. What is under test is the walk-back, not the temper.
            if (!plain[0]) {
                if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
                    throw new GameTestAssertException(Component.literal("the horse is not founded yet"), 0);
                }
                makePlain(horse);
                plain[0] = true;
            }
            // Judged only once the order has been in force a while: a gametest horse can
            // stand frozen for a hundred ticks or so after it is spawned (seen in the
            // logs).
            boolean longEnough = running(horse, com.example.horsegenetics.neoforge.server.OrderStayGoal.class, stayTicks);
            // And it must have been STEERED to its spot, not merely drifted near it: the
            // neighbouring tests' horses pull it about (sparring and the like, at 3), and
            // with the walk-back cut out it still wandered 1.6 blocks back once.
            headedHome[0] |= anchor.equals(horse.getNavigation().getTargetPos());
            if (!longEnough || !headedHome[0]) {
                double hx = horse.getX() - (anchor.getX() + 0.5);
                double hz = horse.getZ() - (anchor.getZ() + 0.5);
                String goals = horse.goalSelector.getAvailableGoals().stream().filter(w -> w.isRunning())
                        .map(w -> w.getPriority() + ":" + w.getGoal().getClass().getSimpleName())
                        .collect(java.util.stream.Collectors.joining(","));
                var nav = horse.getNavigation();
                throw new GameTestAssertException(Component.literal("the stay order has not steered it home yet"
                        + " [stayTicks " + stayTicks[0] + ", headedHome " + headedHome[0]
                        + ", target " + nav.getTargetPos() + ", anchor " + anchor
                        + ", at " + horse.blockPosition() + String.format(" (%.2f across, dy %.2f)",
                                Math.sqrt(hx * hx + hz * hz), horse.getY() - anchor.getY())
                        + ", onGround " + horse.onGround() + ", navDone " + nav.isDone()
                        + ", path " + (nav.getPath() == null ? "none" : nav.getPath().getTarget()
                                + " " + nav.getPath().getNextNodeIndex() + "/" + nav.getPath().getNodeCount())
                        + ", order " + horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_ORDER.get())
                        + ", vehicle " + horse.isVehicle() + ", leashed " + horse.isLeashed()
                        + ", running [" + goals + "], age " + horse.tickCount + "]"), 0);
            }
            double dx = horse.getX() - (anchor.getX() + 0.5);
            double dz = horse.getZ() - (anchor.getZ() + 0.5);
            double d = dx * dx + dz * dz; // across the ground, as OrderStayGoal judges it
            double slack = com.example.horsegenetics.common.care.HorseOrders.STAY_SLACK_BLOCKS;
            if (d > slack * slack) {
                throw new GameTestAssertException(Component.literal(
                        "a stayed horse moved three blocks has not walked back (" + Math.sqrt(d) + " away)"), 0);
            }
        });
    }

    /**
     * <b>An order never out-ranks a need</b> (owner: an order must never starve a horse).
     * Structural, because "a horse that would have eaten did" needs food the neighbouring
     * tests' horses could take: the order goals sit below hunger, panic and escape, so
     * each of those pre-empts them, and bond-follow stands down under any order.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_YIELDS_TO_NEEDS =
            TEST_FUNCTIONS.register("order_yields_to_needs", () -> ModGameTests::orderYieldsToNeeds);

    private static void orderYieldsToNeeds(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.STAY);
        int stay = -1;
        int follow = -1;
        int worstNeed = -1;
        com.example.horsegenetics.neoforge.server.BondFollowGoal bond = null;
        for (net.minecraft.world.entity.ai.goal.WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            var g = w.getGoal();
            if (g instanceof com.example.horsegenetics.neoforge.server.OrderStayGoal) {
                stay = w.getPriority();
            } else if (g instanceof com.example.horsegenetics.neoforge.server.OrderFollowGoal) {
                follow = w.getPriority();
            } else if (g instanceof com.example.horsegenetics.neoforge.server.HungerFoodGoal
                    || g instanceof com.example.horsegenetics.neoforge.server.HorseEscapeGoal
                    || g instanceof net.minecraft.world.entity.ai.goal.PanicGoal) {
                worstNeed = Math.max(worstNeed, w.getPriority());
            } else if (g instanceof com.example.horsegenetics.neoforge.server.BondFollowGoal b) {
                bond = b;
            }
        }
        if (stay < 0 || follow < 0) {
            throw new GameTestAssertException(Component.literal("the order goals were not added to a horse"), 0);
        }
        if (worstNeed < 0 || stay <= worstNeed || follow <= worstNeed) {
            throw new GameTestAssertException(Component.literal("an order goal (stay " + stay + ", follow " + follow
                    + ") is not below every need (worst " + worstNeed + "): a stayed horse could starve"), 0);
        }
        if (bond == null || bond.canUse()) {
            throw new GameTestAssertException(Component.literal("bond-follow is missing, or runs under an order"), 0);
        }
        helper.succeed();
    }

    /**
     * <b>An order is dropped when the horse leaves its giver's keeping</b>: sold or
     * transferred (another owner), in another dimension, or stored in a stasis chamber.
     * The whistle recall's clear needs a real ServerPlayer, and is a play check instead.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_CLEARS =
            TEST_FUNCTIONS.register("order_clears", () -> ModGameTests::orderClears);

    private static void orderClears(GameTestHelper helper) {
        var type = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_ORDER.get();
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player buyer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        var sold = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.FOLLOW);
        if (!com.example.horsegenetics.neoforge.server.HorseOrdering.hasOrder(sold)) {
            throw new GameTestAssertException(Component.literal("a fresh order does not stand - the premise is broken"), 0);
        }
        sold.setOwner(buyer);
        if (com.example.horsegenetics.neoforge.server.HorseOrdering.hasOrder(sold) || sold.getData(type).hasOrder()) {
            throw new GameTestAssertException(Component.literal("a horse with a new owner kept the old owner's order"), 0);
        }

        var moved = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.WANDER);
        var o = moved.getData(type);
        moved.setData(type, new com.example.horsegenetics.neoforge.data.HorseOrderAttachment(
                o.order(), o.anchor(), "minecraft:the_nether", o.orderedBy()));
        if (com.example.horsegenetics.neoforge.server.HorseOrdering.hasOrder(moved)) {
            throw new GameTestAssertException(Component.literal("an order given in another dimension still stands"), 0);
        }

        var stored = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.STAY);
        helper.runAfterDelay(25L, () -> {
            com.example.horsegenetics.neoforge.server.HorseStasisHandler.swallow(helper.getLevel(), stored,
                    new net.minecraft.world.item.ItemStack(
                            com.example.horsegenetics.neoforge.item.ModItems.INTERMEDIATE_STASIS_CHAMBER.get()), "Test");
            if (stored.getData(type).hasOrder()) {
                throw new GameTestAssertException(Component.literal("a horse went into stasis with its order"), 0);
            }
            helper.succeed();
        });
    }

    /** A guardian (Grd/Grd): bred to fight, and - unlike a monster-hunting pair - starts nothing by itself. */
    private static void makeGuardian(net.minecraft.world.entity.animal.equine.Horse horse) {
        var gene = com.example.horsegenetics.common.genetics.Genes.GUARDIAN;
        var grd = gene.alleles().stream().filter(a -> a.token().equals("Grd")).findFirst().orElseThrow();
        refound(horse, com.example.horsegenetics.common.genetics.Genotype.wildType()
                .with(new com.example.horsegenetics.common.genetics.AllelePair(grd, grd)));
    }

    /**
     * A horse bred NOT to fight: the wild type at every locus. A founding roll is no
     * plain horse - six in a hundred carry a gladiator copy, and the gate rightly lets
     * it fight (issue #37: order_combat_gate's "plain" horse was Gld/n whenever it failed).
     */
    private static void makePlain(net.minecraft.world.entity.animal.equine.Horse horse) {
        refound(horse, com.example.horsegenetics.common.genetics.Genotype.wildType());
    }

    private static void refound(net.minecraft.world.entity.animal.equine.Horse horse,
                                com.example.horsegenetics.common.genetics.Genotype genotype) {
        com.example.horsegenetics.neoforge.server.HorseRecords.apply(horse,
                com.example.horsegenetics.neoforge.server.HorseRecords.newFounder(horse,
                        new com.example.horsegenetics.neoforge.NeoRng(horse.getRandom()), genotype));
    }

    /**
     * <b>Only a horse bred to fight takes a combat order</b>, on the server's own gate: a
     * plain horse refuses Hunt monsters and Defend me with "not bred to fight", a guardian
     * takes both, and the plain horse still takes Stay.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_COMBAT_GATE =
            TEST_FUNCTIONS.register("order_combat_gate", () -> ModGameTests::orderCombatGate);

    private static void orderCombatGate(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var plain = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.REJOIN_HERD);
        var guardian = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.REJOIN_HERD);
        boolean[] checked = {false};
        helper.succeedWhen(() -> {
            if (checked[0]) {
                return;
            }
            // Only once both are founded: a founding after makeGuardian would roll it a new genome.
            if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(plain)
                    || !com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(guardian)) {
                throw new GameTestAssertException(Component.literal("the horses are not founded yet"), 0);
            }
            makePlain(plain);
            makeGuardian(guardian);
            var nope = com.example.horsegenetics.common.care.HorseOrders.Refusal.NOT_A_FIGHTER;
            for (var order : java.util.List.of(com.example.horsegenetics.common.care.HorseOrder.HUNT_MONSTERS,
                    com.example.horsegenetics.common.care.HorseOrder.DEFEND_ME)) {
                var p = com.example.horsegenetics.common.care.HorseOrders.refusal(order,
                        com.example.horsegenetics.neoforge.server.HorseOrdering.situation(plain));
                if (p != nope) {
                    throw new GameTestAssertException(Component.literal("a plain horse answered " + p + " to " + order), 0);
                }
                var g = com.example.horsegenetics.common.care.HorseOrders.refusal(order,
                        com.example.horsegenetics.neoforge.server.HorseOrdering.situation(guardian));
                if (g != null) {
                    throw new GameTestAssertException(Component.literal("a bonded guardian refused " + order + ": " + g), 0);
                }
            }
            if (com.example.horsegenetics.common.care.HorseOrders.refusal(com.example.horsegenetics.common.care.HorseOrder.STAY,
                    com.example.horsegenetics.neoforge.server.HorseOrdering.situation(plain)) != null) {
                throw new GameTestAssertException(Component.literal("the fighting gate reached a non-combat order"), 0);
            }
            checked[0] = true;
        });
    }

    /**
     * <b>The whistle knows whose horse it is from the record</b> (issue #36). A client never
     * has vanilla's owner, so the plain hold asks {@code HorseOrders.whistleUse} of the
     * synced record; this proves the server keeps that record's owner true: it names the
     * taming player, moves with a sale, and empties when the horse goes wild - and the rule
     * answers each state as the wheel would. The press itself is an in-game check.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WHISTLE_SEES_THE_OWNER =
            TEST_FUNCTIONS.register("whistle_sees_the_owner", () -> ModGameTests::whistleSeesTheOwner);

    private static void whistleSeesTheOwner(GameTestHelper helper) {
        keepTicking(helper);
        var aimed = com.example.horsegenetics.common.care.HorseOrders.WhistleUse.AIMED_HORSE;
        var nothing = com.example.horsegenetics.common.care.HorseOrders.WhistleUse.NOTHING;
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player buyer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, new BlockPos(2, 0, 2));
        horse.setTamed(true);
        horse.setOwner(owner);
        int[] stage = {0};
        helper.succeedWhen(() -> {
            if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
                throw new GameTestAssertException(Component.literal("the horse is not founded yet"), 0);
            }
            var record = com.example.horsegenetics.neoforge.server.HorseRecords.of(horse);
            java.util.function.BiFunction<net.minecraft.world.entity.player.Player, Boolean,
                    com.example.horsegenetics.common.care.HorseOrders.WhistleUse> use =
                    (p, sneak) -> com.example.horsegenetics.common.care.HorseOrders.whistleUse(sneak, record, p.getUUID());
            switch (stage[0]) {
                case 0 -> {
                    if (use.apply(owner, false) != aimed) {
                        throw new GameTestAssertException(Component.literal(
                                "the taming player's own horse opens no wheel: the record's owner is " + record.ownerId()), 0);
                    }
                    if (use.apply(buyer, false) != nothing) {
                        throw new GameTestAssertException(Component.literal("a stranger's whistle opened the wheel on it"), 0);
                    }
                    horse.setOwner(buyer);
                    stage[0] = 1;
                    throw new GameTestAssertException(Component.literal("sold; waiting for the mirror"), 0);
                }
                case 1 -> {
                    if (use.apply(buyer, false) != aimed || use.apply(owner, false) != nothing) {
                        throw new GameTestAssertException(Component.literal(
                                "the record has not followed the sale: its owner is " + record.ownerId()), 0);
                    }
                    horse.setTamed(false);
                    stage[0] = 2;
                    throw new GameTestAssertException(Component.literal("gone wild; waiting for the mirror"), 0);
                }
                default -> {
                    if (record.ownerId().isPresent() || use.apply(buyer, false) != nothing) {
                        throw new GameTestAssertException(Component.literal(
                                "a horse gone wild still answers to " + record.ownerId()), 0);
                    }
                }
            }
        });
    }

    /**
     * <b>A turnout releases the horse that arrived, not the one that left</b> (issue #29).
     * A cross-world move builds a new entity, so a release applied to the old reference
     * left the horse in the realm tamed, owned and saddled while its saddle was also handed
     * back. The harness has no realm, so the horse is turned out into the Nether: what is
     * under test is the cross-world move, not the realm. One saddle must exist afterwards,
     * in the player's pack, and the horse standing in the other world must be wild.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TURNOUT_RELEASES_THE_HORSE_THAT_ARRIVED =
            TEST_FUNCTIONS.register("turnout_releases_the_horse_that_arrived",
                    () -> ModGameTests::turnoutReleasesTheHorseThatArrived);

    private static void turnoutReleasesTheHorseThatArrived(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel other = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (other == null || other == level) {
            helper.fail("the harness has no Nether, so there is no second world to turn a horse out into");
            return;
        }
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        owner.getInventory().clearContent();
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, new BlockPos(2, 0, 2));
        horse.setTamed(true);
        horse.setOwner(owner);
        horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        UUID id = horse.getUUID();

        // Above the Nether's roof: open air in a chunk forced for the length of the move.
        other.setChunkForced(0, 0, true);
        try {
            net.minecraft.world.entity.animal.equine.Horse arrived =
                    com.example.horsegenetics.neoforge.server.TicketHandler.turnOutTo(
                            level, other, new net.minecraft.world.phys.Vec3(0.5, 200.0, 0.5), horse, owner);
            if (arrived == null) {
                helper.fail("the turnout did not move the horse at all");
                return;
            }
            // The returned entity itself, not other.getEntity(id): a forced chunk's
            // entity section loads asynchronously, so the lookup misses a horse
            // that is there.
            net.minecraft.world.entity.animal.equine.Horse there = arrived;
            if (there.level() != other || there.isRemoved() || there == horse || !there.getUUID().equals(id)
                    || !horse.isRemoved()) {
                helper.fail("the move did not replace the horse with a live copy of it in the other world");
                return;
            }
            boolean wild = !there.isTamed() && there.getOwnerReference() == null;
            boolean bare = there.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).isEmpty();
            int handed = owner.getInventory().countItem(Items.SADDLE);
            there.discard();
            if (!wild) {
                helper.fail("the horse in the other world is still tamed or owned - the release went to the"
                        + " entity that was left behind");
                return;
            }
            if (!bare || handed != 1) {
                helper.fail("saddles: " + (bare ? 0 : 1) + " on the arrived horse, " + handed
                        + " in the pack - there must be exactly one, in the pack");
                return;
            }
        } finally {
            other.setChunkForced(0, 0, false);
        }
        helper.succeed();
    }

    /**
     * <b>A hunting horse picks the monster and leaves the creeper</b>: a guardian told to
     * Hunt monsters, with a husk four blocks off and a creeper nearer, takes the husk as
     * its target (OrderCombatGoal). A guardian, because it starts nothing by itself - the
     * target can only be the order's. Both monsters are frozen (no AI), so neither moves
     * nor explodes; the pick is what is under test, the fight is HorseMeleeGoal's.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_HUNT_PICKS_A_MONSTER =
            TEST_FUNCTIONS.register("order_hunt_picks_a_monster", () -> ModGameTests::orderHuntPicksAMonster);

    private static void orderHuntPicksAMonster(GameTestHelper helper) {
        keepTicking(helper);
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse[] horse = {null};
        net.minecraft.world.entity.LivingEntity[] creeper = {null};
        net.minecraft.world.entity.LivingEntity[] husk = {null};
        String[] founded = {null};
        helper.succeedWhen(() -> {
            if (horse[0] == null) {
                fieldTicks(helper);
                aloneOnClearGround(helper);
                horse[0] = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.HUNT_MONSTERS,
                        new BlockPos(2, 0, 2));
                creeper[0] = helper.spawn(net.minecraft.world.entity.EntityType.CREEPER, new BlockPos(2, 0, 0));
                ((net.minecraft.world.entity.Mob) creeper[0]).setNoAi(true);
            }
            // Made a guardian only once founded: a founding afterwards would roll a new genome.
            if (husk[0] == null && com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse[0])) {
                founded[0] = "founded at age " + horse[0].tickCount;
                makeGuardian(horse[0]);
                husk[0] = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(2, 0, -2));
                ((net.minecraft.world.entity.Mob) husk[0]).setNoAi(true);
                // NOT invulnerable: Mob.getTarget() answers null for a target the horse cannot
                // attack, so an invulnerable husk is picked and never held. It is checked the
                // tick it is picked, well before four kicks could kill it.
            }
            var t = horse[0].getTarget();
            if (t == creeper[0]) {
                helper.fail("a hunting horse went for a creeper");
            }
            if (husk[0] == null || t != husk[0]) {
                throw new GameTestAssertException(Component.literal("the hunting horse has not picked the husk (target "
                        + (t == null ? "none" : t.getName().getString()) + ") " + huntState(horse[0], husk[0], founded[0])), 0);
            }
        });
    }

    /**
     * Throws until every chunk {@link #keepTicking} forced ticks its entities (issue #70).
     * Forcing is a ticket, and a ticket takes effect some ticks later; the harness waits
     * only for the structure's own chunk, and a test's 1x1 structure is one chunk while
     * its field is often two. A horse spawned into a neighbour that is not ticking yet
     * stands frozen - never founded, never scanning - until it does. Mostly that is one
     * tick; 369 was the longest in 120 tries, more than the old 200-tick budget. The wait
     * comes out of the test's own budget, so a test using this needs room for it.
     * Spawn only after this passes, from inside the test's own wait.
     *
     * <p>The wait is wall-clock, not ticks (issue #150): what lags is the asynchronous
     * entity-section load, about 100-200 ms cold, while GameTestServer.waitUntilNextTick
     * never sleeps, so ticks run anywhere from 0.4 to 6 a millisecond - 1202 ticks went by
     * in 207 ms once, and the test timed out with the holder already ENTITY_TICKING. So a
     * tick spent waiting is paced to a real one: the budget then buys about a minute.
     * Only alone tests call this, so the sleep slows no other test.
     */
    private static final java.util.Map<BlockPos, long[]> FIELD_WAIT = new java.util.concurrent.ConcurrentHashMap<>();

    private static void fieldTicks(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(BlockPos.ZERO);
        long[] start = FIELD_WAIT.computeIfAbsent(at.immutable(),
                k -> new long[] {helper.getLevel().getGameTime(), System.nanoTime(), 0});
        long ticks = helper.getLevel().getGameTime() - start[0];
        long ms = (System.nanoTime() - start[1]) / 1_000_000;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var chunk = new net.minecraft.world.level.ChunkPos((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
                if (!helper.getLevel().areEntitiesActuallyLoadedAndTicking(chunk)) {
                    try {
                        Thread.sleep(50); // one real tick (issue #150)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new GameTestAssertException(Component.literal("waiting for the field's chunk " + chunk
                            + " to tick before anything is spawned in it [waited " + ticks + " ticks, " + ms
                            + " ms; holder " + helper.getLevel().getChunkSource().getChunkDebugData(chunk)
                                    .replaceAll("\u00a7.", "").replace('\n', ' ') + "]"), 0);
                }
            }
        }
        if (start[2] == 0) {
            start[2] = 1;
            HorseGenetics.LOGGER.info("[gametest] field at {} ticking after {} ticks, {} ms", at, ticks, ms);
        }
    }

    /** What OrderCombatGoal's pick hinges on, for a hunt test's failure message (issue #70). */
    private static String huntState(net.minecraft.world.entity.animal.equine.Horse horse,
                                    net.minecraft.world.entity.LivingEntity quarry, String founded) {
        var record = com.example.horsegenetics.neoforge.server.HorseRecords.of(horse);
        var order = com.example.horsegenetics.neoforge.server.HorseOrdering.current(horse);
        String goals = horse.goalSelector.getAvailableGoals().stream().filter(w -> w.isRunning())
                .map(w -> w.getPriority() + ":" + w.getGoal().getClass().getSimpleName())
                .collect(java.util.stream.Collectors.joining(","));
        String targets = horse.targetSelector.getAvailableGoals().stream().filter(w -> w.isRunning())
                .map(w -> w.getPriority() + ":" + w.getGoal().getClass().getSimpleName())
                .collect(java.util.stream.Collectors.joining(","));
        StringBuilder s = new StringBuilder("[").append(founded == null ? "not founded" : founded)
                .append("; removed=").append(horse.getRemovalReason())
                .append(" chunk ticking now=").append(((ServerLevel) horse.level()).areEntitiesActuallyLoadedAndTicking(horse.chunkPosition()))
                .append(" level holds it=").append(horse.level().getEntity(horse.getId()) == horse)
                .append(" fighter=").append(com.example.horsegenetics.neoforge.server.HorseOrdering.fighter(record))
                .append(" hp ").append(horse.getHealth()).append("/").append(horse.getMaxHealth())
                .append(" order ").append(order.order()).append(" anchor ").append(order.anchor())
                .append(" horse at ").append(horse.blockPosition())
                .append(" tick ").append(horse.tickCount).append(" id ").append(horse.getId())
                .append(" tamed=").append(horse.isTamed()).append(" vehicle=").append(horse.isVehicle())
                .append(" leashed=").append(horse.isLeashed())
                .append(" goals ").append(goals).append(" targets ").append(targets);
        if (quarry == null) {
            s.append("; no husk spawned");
        } else {
            s.append("; husk at ").append(quarry.blockPosition()).append(" alive=").append(quarry.isAlive())
                    .append(" removed=").append(quarry.isRemoved())
                    .append(" sight=").append(horse.hasLineOfSight(quarry))
                    .append(" canAttack=").append(horse.canAttack(quarry))
                    .append(" hostile=").append(com.example.horsegenetics.neoforge.server.MobGroups.isHostile(quarry))
                    .append(" passified=").append(com.example.horsegenetics.neoforge.server.Passification.suppresses(horse, quarry))
                    .append(" d=").append(String.format("%.1f", Math.sqrt(horse.distanceToSqr(quarry))));
        }
        return s.append("]").toString();
    }

    /**
     * <b>A guarding horse goes for a monster inside its short reach and no further</b>
     * (Piece 2): a guardian told to Guard here, with a frozen husk eight blocks off -
     * inside Hunt monsters' reach, outside Guard here's six - ignores it for three scans;
     * a second husk four blocks off is then picked, and the creeper nearer still never is.
     * Guards against Guard here quietly taking the hunt radius.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_GUARD_HOLDS_ITS_REACH =
            TEST_FUNCTIONS.register("order_guard_holds_its_reach", () -> ModGameTests::orderGuardHoldsItsReach);

    private static void orderGuardHoldsItsReach(GameTestHelper helper) {
        keepTicking(helper);
        if (com.example.horsegenetics.neoforge.ServerConfig.ordersGuardRadius() >= 8) {
            throw new GameTestAssertException(Component.literal("orders.guard_radius is "
                    + com.example.horsegenetics.neoforge.ServerConfig.ordersGuardRadius()
                    + " in this run's server config, so the far husk is inside it - set it below 8"), 0);
        }
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse[] guard = {null};
        net.minecraft.world.entity.LivingEntity[] creeper = {null};
        net.minecraft.world.entity.LivingEntity[] far = {null};
        net.minecraft.world.entity.LivingEntity[] near = {null};
        int[] since = {0};
        helper.succeedWhen(() -> {
            if (guard[0] == null) {
                fieldTicks(helper); // issue #70
                aloneOnClearGround(helper);
                guard[0] = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.GUARD_HERE,
                        new BlockPos(2, 0, 2));
                creeper[0] = helper.spawn(net.minecraft.world.entity.EntityType.CREEPER, new BlockPos(2, 0, 0));
                ((net.minecraft.world.entity.Mob) creeper[0]).setNoAi(true);
            }
            var horse = guard[0];
            if (far[0] == null && com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
                makeGuardian(horse);
                far[0] = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(2, 0, -6));
                ((net.minecraft.world.entity.Mob) far[0]).setNoAi(true);
            }
            var t = horse.getTarget();
            if (t == creeper[0]) {
                helper.fail("a guarding horse went for a creeper");
            }
            if (far[0] != null && t == far[0]) {
                helper.fail("a guarding horse went for a husk eight blocks off - past orders.guard_radius");
            }
            if (far[0] == null) {
                throw new GameTestAssertException(Component.literal("the horse is not founded yet"), 0);
            }
            // Three scans with only the far husk to pick from; then the near one.
            if (near[0] == null) {
                if (++since[0] < com.example.horsegenetics.common.care.HorseOrders.COMBAT_SCAN_TICKS * 3) {
                    throw new GameTestAssertException(Component.literal("waiting out the far husk"), 0);
                }
                near[0] = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(2, 0, -2));
                ((net.minecraft.world.entity.Mob) near[0]).setNoAi(true);
            }
            if (t != near[0]) {
                throw new GameTestAssertException(Component.literal("the guarding horse has not picked the near husk (target "
                        + (t == null ? "none" : t.getName().getString()) + ")"), 0);
            }
        });
    }

    /**
     * <b>Graze nearby leaves a horse alone inside its tether and walks it back outside</b>
     * (Piece 2). Two horses: one anchored where it stands, whose graze goal must never
     * run; one whose spot is set past {@code orders.graze_radius}, whose goal must run
     * and keep running. Not where it steers: that spot is outside the test's space, where
     * the floor (and so a path) is not promised. The walk is the same navigation call as
     * Stay's, which order_stay_walks_back proves; what is under test is the tether deciding when.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORDER_GRAZE_TETHER =
            TEST_FUNCTIONS.register("order_graze_tether", () -> ModGameTests::orderGrazeTether);

    private static void orderGrazeTether(GameTestHelper helper) {
        keepTicking(helper);
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var home = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.GRAZE_NEARBY,
                new BlockPos(2, 0, 2));
        var strayed = orderedHorse(helper, owner, com.example.horsegenetics.common.care.HorseOrder.GRAZE_NEARBY,
                new BlockPos(0, 0, 2));
        var type = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_ORDER.get();
        var o = strayed.getData(type);
        int past = com.example.horsegenetics.neoforge.ServerConfig.ordersGrazeRadius() + 3;
        // The spot moved, not the horse: the horse stays inside its own test's space.
        BlockPos spot = strayed.blockPosition().south(past);
        strayed.setData(type, new com.example.horsegenetics.neoforge.data.HorseOrderAttachment(
                o.order(), java.util.Optional.of(spot), o.dimension(), o.orderedBy()));
        int[] homeRan = {0};
        int[] strayRan = {0};
        int[] ticks = {0};
        helper.succeedWhen(() -> {
            ticks[0]++;
            running(home, com.example.horsegenetics.neoforge.server.OrderGrazeGoal.class, homeRan);
            if (homeRan[0] > 0) {
                helper.fail("the graze goal pulled in a horse standing on its own spot");
            }
            running(strayed, com.example.horsegenetics.neoforge.server.OrderGrazeGoal.class, strayRan);
            if (strayRan[0] < 20 || ticks[0] < 60) {
                throw new GameTestAssertException(Component.literal("the strayed horse's graze goal has run "
                        + strayRan[0] + " ticks (" + ticks[0] + " in)"), 0);
            }
        });
    }

    private static void setBond(net.minecraft.world.entity.animal.equine.Horse horse, int bond) {
        var attachment = com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_CARE.get();
        horse.setData(attachment, horse.getData(attachment).withBond(bond));
    }

    /**
     * <b>Right-clicking a horse with a piece of tack puts it on - and the four
     * clicks it must refuse, refuses in two different ways.</b>
     *
     * <p>It posts real {@code EntityInteract} events through
     * {@code NeoForge.EVENT_BUS} rather than calling
     * {@code TackEquipHandler} directly, for the reason
     * {@code mounted_mining_is_exempt} does the same: a handler written but
     * never registered would otherwise pass.
     *
     * <p><b>The two kinds of refusal are the point, and asserting them together
     * would hide the one that matters.</b> Not-your-horse and a foal are
     * <i>claimed</i> refusals - the event is cancelled and a line is sent -
     * because they are rules this mod invented and a silent invented rule reads
     * as a bug. Untamed, and "every slot that takes it is already full", are
     * <i>unclaimed</i>: the event must come back uncancelled so vanilla still
     * rears the wild horse and still lets you mount your own. A handler that
     * cancelled all four would look correct in-game right up to the moment
     * somebody could not get on a horse while holding a braid.
     *
     * <p>The other thing worth pinning is that the slot chosen is the first
     * <i>empty</i> one and never a swap: two braids fill the mane and then the
     * tail, and a third is refused with nothing consumed. A swap would look
     * identical from the outside - a braid in the mane either way - and would
     * quietly destroy the one already in it.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RIGHT_CLICK_EQUIPS_TACK =
            TEST_FUNCTIONS.register("right_click_equips_tack",
                    () -> ModGameTests::rightClickEquipsTack);

    private static void rightClickEquipsTack(GameTestHelper helper) {
        if (!com.example.horsegenetics.neoforge.ServerConfig.rightClickEquipsTack()) {
            throw new GameTestAssertException(Component.literal(
                    "behaviour.rightclick_equips_tack is off in this run's server config, so"
                            + " this test asserts the wrong branch - turn it back on rather than"
                            + " deleting the test"), 0);
        }
        var mane = com.example.horsegenetics.neoforge.entity.HorseTackSlot.MANE;
        var tail = com.example.horsegenetics.neoforge.entity.HorseTackSlot.TAIL;
        ItemStack probe = new ItemStack(com.example.horsegenetics.neoforge.item.ModItems.RESCUING_BRAID.get());
        if (!probe.is(mane.tag()) || !probe.is(tail.tag())) {
            throw new GameTestAssertException(Component.literal(
                    "the rescuing braid is not in gear/mane and gear/tail, so there is no item"
                            + " in the game this handler could equip and this test exercises"
                            + " nothing - the premise is broken, not TackEquipHandler"), 0);
        }

        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        horse.setTamed(true);
        horse.setOwner(player);
        if (!com.example.horsegenetics.neoforge.server.HorseOwnership.isOwner(horse, player.getUUID())) {
            throw new GameTestAssertException(Component.literal(
                    "the test horse is not owned by the mock player, so every case below would"
                            + " be testing ownership - the premise is broken, not the handler"), 0);
        }

        // Three braids in hand, so the count is the record of what was spent.
        ItemStack held = new ItemStack(
                com.example.horsegenetics.neoforge.item.ModItems.RESCUING_BRAID.get(), 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);

        assertClaimed(helper, player, horse, true, "a braid offered to a bare mane");
        if (mane.on(horse).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "the first braid was not put in the mane"), 0);
        }
        if (!tail.on(horse).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "the first braid went to the tail, so the roster is not being walked in"
                            + " order and which slot a piece lands in is unpredictable"), 0);
        }
        assertHeld(helper, player, 2, "one braid should have been spent on the mane");

        assertClaimed(helper, player, horse, true, "a second braid, the mane already full");
        if (tail.on(horse).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "the second braid did not fall through to the tail, so a full slot is not"
                            + " being skipped in favour of the next one that fits"), 0);
        }
        assertHeld(helper, player, 1, "a second braid should have been spent on the tail");

        // Both slots full. The click must be left alone: cancelling it here is
        // how a player ends up unable to mount their own horse.
        assertClaimed(helper, player, horse, false, "a third braid with nowhere left to put it");
        assertHeld(helper, player, 1, "a third braid was consumed with nowhere to put it");

        // A foal: claimed and refused, because "foals wear nothing" is our rule.
        net.minecraft.world.entity.animal.equine.Horse foal =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        foal.setTamed(true);
        foal.setOwner(player);
        foal.setBaby(true);
        assertClaimed(helper, player, foal, true, "a braid offered to a foal");
        if (!mane.on(foal).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "a foal was tacked up - HorseTackSlot.usableOn says foals wear nothing"), 0);
        }
        assertHeld(helper, player, 1, "a braid was spent on a foal that cannot wear it");

        // Somebody else's horse: claimed and refused, for the same reason.
        net.minecraft.world.entity.player.Player stranger =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        if (stranger.getUUID().equals(player.getUUID())) {
            throw new GameTestAssertException(Component.literal(
                    "the two mock players share a UUID, so the ownership case below would pass"
                            + " whatever the handler did - the premise is broken"), 0);
        }
        net.minecraft.world.entity.animal.equine.Horse theirs =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        theirs.setTamed(true);
        theirs.setOwner(stranger);
        assertClaimed(helper, player, theirs, true, "a braid offered to somebody else's horse");
        if (!mane.on(theirs).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "a stranger tacked up a horse they do not own"), 0);
        }
        assertHeld(helper, player, 1, "a braid was spent on a horse the player does not own");

        // Untamed: NOT claimed. Vanilla rears the horse up, which is the answer.
        net.minecraft.world.entity.animal.equine.Horse wild =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        assertClaimed(helper, player, wild, false, "a braid offered to an untamed horse");
        if (!mane.on(wild).isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "an untamed horse was tacked up"), 0);
        }
        assertHeld(helper, player, 1, "a braid was spent on an untamed horse");

        helper.succeed();
    }

    /**
     * <b>A horse carries a chest on each flank</b> - what counts as one, where it
     * goes, what a load costs, what will and will not come off, and what a dead
     * horse leaves behind.
     *
     * <p>Everything a player would notice going wrong here goes wrong silently:
     * a tag that failed to load makes a furnace hang on a horse or an ender
     * chest refuse to; a handler never registered makes a click mount instead
     * of open; a chest taken off full is a stack of items that simply stop
     * existing. So each is asserted against the real registries and through
     * real events, the way {@code right_click_equips_tack} is.
     *
     * <p>It does not open a menu - that needs a real {@code ServerPlayer}, and
     * the harness's mock player is not one. What a menu writes is
     * {@code HorsePacks}, which is written directly here instead.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HORSE_CARRIES_CHESTS =
            TEST_FUNCTIONS.register("horse_carries_chests", () -> ModGameTests::horseCarriesChests);

    private static void packFail(String what) {
        throw new GameTestAssertException(Component.literal(what), 0);
    }

    private static void horseCarriesChests(GameTestHelper helper) {
        var left = com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_LEFT;
        var right = com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_RIGHT;
        var curve = com.example.horsegenetics.neoforge.ServerConfig.packCurve();
        if (!curve.enabled() || !com.example.horsegenetics.neoforge.ServerConfig.rightClickEquipsTack()) {
            packFail("packs.weight or behaviour.rightclick_equips_tack is off in this run's server"
                    + " config, so this test asserts the wrong branch - turn it back on rather than"
                    + " deleting the test");
        }

        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setYRot(0f);
        horse.yBodyRot = 0f;
        var level = helper.getLevel();
        BlockPos at = horse.blockPosition();

        // 1. What counts. Sizes come off the real block entities; the two that
        //    are decided by a tag prove the tag files loaded.
        Object[][] sizes = {
                {Items.CHEST, 27}, {Items.TRAPPED_CHEST, 27}, {Items.BARREL, 27},
                {Items.SHULKER_BOX, 27}, {Items.OAK_SHELF, 3}, {Items.DECORATED_POT, 1},
                {Items.ENDER_CHEST, com.example.horsegenetics.neoforge.entity.HorseStorage.UNKNOWN_SIZE},
                {Items.HOPPER, 0}, {Items.FURNACE, 0}, {Items.DISPENSER, 0},
                // Ours, and it answers the item capability (hoppers feed it): a machine.
                {ModItems.HORSE_STASIS_BANK.get(), 0},
                {Items.DIRT, 0}, {Items.SADDLE, 0}};
        for (Object[] row : sizes) {
            ItemStack stack = new ItemStack((net.minecraft.world.item.Item) row[0]);
            int found = com.example.horsegenetics.neoforge.entity.HorseStorage.slots(stack, level, at);
            if (found != (int) row[1]) {
                packFail(stack.getItem() + " should give a horse " + row[1] + " slots and gives " + found
                        + " - a furnace or hopper above zero means horse_storage/denied did not load,"
                        + " an ender chest at zero means gear/saddlebag_* did not");
            }
        }
        if (!left.accepts(horse, new ItemStack(Items.CHEST))
                || com.example.horsegenetics.neoforge.entity.HorseTackSlot.MANE
                        .accepts(horse, new ItemStack(Items.CHEST))) {
            packFail("a chest must fit a pack slot and no other slot");
        }

        // 2. Right-click with a chest in hand: it goes on the flank the player
        //    is standing at. Yaw 0 faces +Z, so +X is the horse's near side.
        //    Looking straight up, so the click is on the horse and on no chest.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST, 2));
        player.setPos(horse.getX() - 2.0, horse.getY(), horse.getZ());
        player.setXRot(-90f);
        assertClaimed(helper, player, horse, true, "a chest offered from the off side");
        if (!right.on(horse).is(Items.CHEST) || !left.on(horse).isEmpty()) {
            packFail("a chest offered from the horse's off side did not go on its off side");
        }
        assertClaimed(helper, player, horse, true, "a second chest, the off side already full");
        if (!left.on(horse).is(Items.CHEST)) {
            packFail("a second chest did not go on the remaining flank");
        }
        assertHeld(helper, player, 0, "two chests should have been spent on two flanks");

        // 3. Clicking the chest itself is claimed - it opens, it does not mount -
        //    and clicking past it is not. Eye level with the chest, looking along
        //    -X at the near flank from two blocks off.
        double scale = horse.getScale();
        player.setPos(horse.getX() + 2.0,
                horse.getY() + com.example.horsegenetics.common.pack.PackBox.CENTRE_UP * scale
                        - player.getEyeHeight(),
                horse.getZ() + com.example.horsegenetics.common.pack.PackBox.CENTRE_FORWARD * scale);
        player.setYRot(90f);
        player.setXRot(0f);
        assertClaimed(helper, player, horse, true, "a click on the chest on the near flank");
        player.setXRot(-90f);
        assertClaimed(helper, player, horse, false, "a click that is on the horse and not on a chest");

        // 4. A stack in the near chest: counted, weighed, and the chest stays on.
        var packs = horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_PACKS);
        packs.set(left.name(), 0, new ItemStack(Items.COBBLESTONE, 64));
        com.example.horsegenetics.neoforge.server.HorsePackHandler.refresh(horse);
        var load = horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_PACK_LOAD);
        if (load.left() != 64 || load.right() != 0 || load.weighed() != 64) {
            packFail("64 cobblestone in the near chest counted as " + load);
        }
        double expected = com.example.horsegenetics.common.pack.PackLoad.speedModifier(curve, 64,
                com.example.horsegenetics.neoforge.server.HorseDraft.pullOf(horse));
        var speed = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var modifier = speed.getModifier(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "pack_load"));
        if (expected >= 0.0 || modifier == null || Math.abs(modifier.amount() - expected) > 1.0e-9) {
            packFail("a loaded horse should carry a speed modifier of " + expected + " and carries "
                    + (modifier == null ? "none" : String.valueOf(modifier.amount())));
        }
        if (left.mayTakeOff(horse) || !left.takeOff(horse).isEmpty() || !left.on(horse).is(Items.CHEST)
                || packs.count(left.name()) != 64) {
            packFail("a chest with a stack in it came off the horse, or lost the stack trying");
        }

        // 5. The ender chest weighs nothing whatever is filed under it, and a
        //    shulker box goes on full, comes off full, and is one copy throughout.
        if (!right.takeOff(horse).is(Items.CHEST)) {
            packFail("an empty chest did not come off");
        }
        right.set(horse, new ItemStack(Items.ENDER_CHEST));
        packs.set(right.name(), 0, new ItemStack(Items.IRON_INGOT, 10));
        com.example.horsegenetics.neoforge.server.HorsePackHandler.refresh(horse);
        if (horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_PACK_LOAD).weighed() != 64) {
            packFail("what is behind an ender chest was weighed");
        }
        packs.take(right.name());
        right.set(horse, ItemStack.EMPTY);

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        net.minecraft.core.NonNullList<ItemStack> inside =
                net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        inside.set(5, new ItemStack(Items.DIAMOND, 3));
        shulker.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(inside));
        right.set(horse, shulker);
        ItemStack worn = right.on(horse);
        if (worn.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY)
                .nonEmptyItems().iterator().hasNext()
                || !packs.get(right.name(), 5).is(Items.DIAMOND) || packs.count(right.name()) != 3) {
            packFail("a shulker box went on a horse and its contents are not in the horse's store,"
                    + " once, at the slot they were in");
        }
        // What the Dress window shows, and hands to a click, is the box WITH its
        // contents - a copy, the store untouched until the click is finished.
        ItemStack shown = com.example.horsegenetics.neoforge.server.HorsePackHandler
                .packedView(horse, right, right.on(horse));
        net.minecraft.core.NonNullList<ItemStack> seen =
                net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        shown.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(seen);
        if (!seen.get(5).is(Items.DIAMOND) || packs.count(right.name()) != 3
                || seen.get(5) == packs.get(right.name(), 5)) {
            packFail("a shulker box on a horse should show its contents as a copy, and leave the"
                    + " store as it was");
        }
        ItemStack back = right.takeOff(horse);
        net.minecraft.core.NonNullList<ItemStack> returned =
                net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        back.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(returned);
        if (!back.is(Items.SHULKER_BOX) || !returned.get(5).is(Items.DIAMOND) || returned.get(5).getCount() != 3
                || packs.count(right.name()) != 0 || !right.on(horse).isEmpty()) {
            packFail("a shulker box did not come off the horse with its contents inside it");
        }
        // And an empty barrel comes back the barrel it went on as, so it stacks.
        right.set(horse, new ItemStack(Items.BARREL));
        if (!ItemStack.isSameItemSameComponents(right.takeOff(horse), new ItemStack(Items.BARREL))) {
            packFail("a barrel came off a horse differing from a fresh barrel - it will not stack");
        }

        // 6. What is saved is what was there.
        var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var reread = com.example.horsegenetics.neoforge.data.HorsePacks.CODEC
                .encodeStart(ops, packs)
                .flatMap(tag -> com.example.horsegenetics.neoforge.data.HorsePacks.CODEC.parse(ops, tag))
                .result().orElse(null);
        if (reread == null || reread.count(left.name()) != 64 || !reread.get(left.name(), 0).is(Items.COBBLESTONE)) {
            packFail("a horse's packs did not survive being saved and read back");
        }

        // 6b. A horse leaving its owner alive hands back what it wears in this
        //     mod's own slots (#218) - a braid used to go with it and be deleted.
        var mane = com.example.horsegenetics.neoforge.entity.HorseTackSlot.MANE;
        mane.set(horse, new ItemStack(ModItems.RESCUING_BRAID.get()));
        com.example.horsegenetics.neoforge.server.TackEquipHandler.returnGear(horse, player);
        if (!mane.on(horse).isEmpty()
                || player.getInventory().countItem(ModItems.RESCUING_BRAID.get()) != 1
                || !left.on(horse).is(Items.CHEST)) {
            packFail("returnGear should hand the braid to the player and leave the chest to"
                    + " HorsePackHandler.giveBack");
        }

        // 7. A dead horse sets the chest down where it fell, full, and keeps
        //    nothing (#221). Nothing of it is left to drop as an item.
        if (!com.example.horsegenetics.neoforge.ServerConfig.packPlaceOnDeath()) {
            packFail("packs.place_on_death is off in this run's server config, so this test asserts"
                    + " the wrong branch - turn it back on rather than deleting the test");
        }
        java.util.List<net.minecraft.world.entity.item.ItemEntity> drops = new java.util.ArrayList<>();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(
                        horse, level.damageSources().generic(), drops, false));
        BlockPos stood = null;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-3, -2, -3), at.offset(3, 2, 3))) {
            if (level.getBlockState(pos).is(Blocks.CHEST)) {
                stood = pos.immutable();
            }
        }
        int loose = 0;
        for (var drop : drops) {
            if (drop.getItem().is(Items.CHEST) || drop.getItem().is(Items.COBBLESTONE)) {
                loose += drop.getItem().getCount();
            }
        }
        if (stood == null || loose != 0
                || !(level.getBlockEntity(stood) instanceof net.minecraft.world.Container placed)
                || !placed.getItem(0).is(Items.COBBLESTONE) || placed.getItem(0).getCount() != 64) {
            packFail("a dying horse should set its chest down nearby with the 64 cobblestone in the slot"
                    + " they were in, and drop none of it; chest at " + stood + ", " + loose + " dropped");
        }
        if (!left.on(horse).isEmpty() || !packs.isEmpty()
                || speed.getModifier(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "pack_load")) != null) {
            packFail("a horse that has set its chest down is still carrying it");
        }
        // Leave the harness as it was found.
        level.setBlock(stood, Blocks.AIR.defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL
                        | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        horse.discard();
        helper.succeed();
    }

    /**
     * <b>A chest that opens as its own block really is one while it is open, and
     * the world is as it was afterwards.</b> The path another mod's chest takes
     * ({@code server/HostedPacks}), driven with a vanilla chest because no
     * modded one is installed here - {@code hosts()} is the only thing that
     * tells them apart, and this calls past it.
     *
     * <p>What it pins: the chest goes on carrying what it held as an item, and
     * is counted; opening it stands a real chest at the bottom of the world
     * with the block above cleared, and opens <i>that block's</i> menu; the
     * menu stays open with the player forty blocks out of the block's reach
     * (the mixins - vanilla's own {@code doTick} is called, and would close it)
     * and shuts when the player leaves the <i>horse</i>; what is put in is on
     * the horse a tick later; and closing puts both original blocks back with
     * nothing left in the journal.
     *
     * <p>The player is a real {@code ServerPlayer} that never joins the player
     * list: {@code makeMockServerPlayerInLevel} does join, which fires this
     * mod's login payloads down a channel that has negotiated nothing and takes
     * the run down. Nothing on this path sends a payload of ours.
     *
     * <p><b>It cannot show a modded client finding the block</b> - there is no
     * client. That is the open check on the gear page.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOSTED_CHEST_IS_A_REAL_BLOCK =
            TEST_FUNCTIONS.register("hosted_chest_is_a_real_block", () -> ModGameTests::hostedChestIsARealBlock);

    private static void hostedChestIsARealBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var left = com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_LEFT;

        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "pack-test");
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                server, level, profile, net.minecraft.server.level.ClientInformation.createDefault());
        net.minecraft.network.Connection connection =
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server, connection,
                player, net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));

        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO);
        horse.setNoAi(true);
        horse.setNoGravity(true);
        horse.setTamed(true);
        horse.setOwner(player);
        // Forty blocks up: far outside any block reach of the bottom of the world.
        horse.snapTo(horse.getX(), horse.getY() + 40.0, horse.getZ());
        player.snapTo(horse.getX() + 1.0, horse.getY(), horse.getZ());

        BlockPos bottom = new BlockPos(player.getBlockX(), level.getMinY(), player.getBlockZ());
        BlockState wasBelow = level.getBlockState(bottom);
        BlockState wasAbove = level.getBlockState(bottom.above());

        // 1. On it goes, carrying three diamonds as an item.
        ItemStack chest = new ItemStack(Items.CHEST);
        net.minecraft.core.NonNullList<ItemStack> inside =
                net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        inside.set(5, new ItemStack(Items.DIAMOND, 3));
        chest.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(inside));
        ItemStack face = com.example.horsegenetics.neoforge.server.HostedPacks.adopt(horse, left, chest);
        var packs = horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_PACKS);
        if (face == null || packs.held(left.name()) == null || packs.held(left.name()).items() != 3
                || packs.count(left.name()) != 3) {
            packFail("a chest carried whole should arrive counted at 3 items; face=" + face
                    + " held=" + packs.held(left.name()));
        }
        if (face.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext()) {
            packFail("the worn face of a carried chest still has its contents in it, which every client"
                    + " in sight would be sent");
        }
        if (level.getBlockState(bottom) != wasBelow || level.getBlockState(bottom.above()) != wasAbove
                || com.example.horsegenetics.neoforge.server.HostedPacks.standing(server) != 0) {
            packFail("putting a chest on a horse left a block standing at the bottom of the world");
        }
        horse.setData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_GEAR,
                horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HORSE_GEAR)
                        .with(left.name(), face));

        // 2. Open: a real chest, a real chest menu, room above it for the lid.
        if (!com.example.horsegenetics.neoforge.server.HostedPacks.open(player, horse, left)) {
            packFail("a carried chest would not open as its block");
        }
        if (!level.getBlockState(bottom).is(Blocks.CHEST) || !level.getBlockState(bottom.above()).isAir()
                || !(player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu)
                || com.example.horsegenetics.neoforge.server.HostedPacks.standing(server) != 1) {
            packFail("opening a carried chest should stand a chest at " + bottom + " with air above and"
                    + " open its menu; found " + level.getBlockState(bottom) + " and "
                    + player.containerMenu.getClass().getSimpleName());
        }
        if (!player.containerMenu.getSlot(5).getItem().is(Items.DIAMOND)) {
            packFail("the standing chest does not hold what the horse was carrying");
        }

        // 3. Vanilla's own per-tick check, forty blocks from the block: still open.
        player.doTick();
        if (!(player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu)) {
            packFail("vanilla closed the menu for being out of the block's reach - the stillValid"
                    + " mixins are not answering for the horse");
        }
        player.containerMenu.getSlot(0).set(new ItemStack(Items.IRON_INGOT, 10));

        helper.runAfterDelay(2L, () -> {
            // 4. A tick later the horse has it, without anything being closed.
            if (packs.count(left.name()) != 13) {
                packFail("ten ingots put into an open chest should be on the horse within a tick;"
                        + " it counts " + packs.count(left.name()));
            }
            // 5. Walk away from the HORSE and it shuts, and the world is put back.
            player.snapTo(horse.getX() + 20.0, horse.getY(), horse.getZ());
            player.doTick();
            if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu) {
                packFail("the menu stayed open twenty blocks from the horse");
            }
            helper.runAfterDelay(2L, () -> {
                if (level.getBlockState(bottom) != wasBelow || level.getBlockState(bottom.above()) != wasAbove
                        || com.example.horsegenetics.neoforge.server.HostedPacks.standing(server) != 0) {
                    packFail("closing a carried chest did not put the world back: "
                            + level.getBlockState(bottom) + " / " + level.getBlockState(bottom.above()));
                }
                // 6. Off it comes, whole - and an empty one comes off plain.
                ItemStack whole = left.takeOff(horse);
                if (!whole.isEmpty()) {
                    packFail("a carried chest with thirteen items in it came off the horse");
                }
                ItemStack shown = com.example.horsegenetics.neoforge.server.HostedPacks.view(horse, left, face);
                net.minecraft.core.NonNullList<ItemStack> back =
                        net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
                shown.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                        net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(back);
                if (!back.get(0).is(Items.IRON_INGOT) || back.get(0).getCount() != 10
                        || !back.get(5).is(Items.DIAMOND)) {
                    packFail("the chest as an item does not hold what was put in it while it stood");
                }
                com.example.horsegenetics.neoforge.server.HostedPacks.forget(horse, left);
                ItemStack emptyFace = com.example.horsegenetics.neoforge.server.HostedPacks
                        .adopt(horse, left, new ItemStack(Items.CHEST));
                ItemStack plain = com.example.horsegenetics.neoforge.server.HostedPacks.view(horse, left, emptyFace);
                if (!ItemStack.isSameItemSameComponents(plain, new ItemStack(Items.CHEST))) {
                    packFail("an empty chest carried whole came back differing from a fresh chest: "
                            + plain.getComponentsPatch());
                }
                com.example.horsegenetics.neoforge.server.HostedPacks.forget(horse, left);
                horse.discard();
                helper.succeed();
            });
        });
    }

    /**
     * Posts a real main-hand {@code EntityInteract} at {@code target} and
     * asserts whether the listeners cancelled it. {@code claimed} is the whole
     * assertion: an uncancelled event is one vanilla goes on to handle.
     */
    private static void assertClaimed(GameTestHelper helper,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity target, boolean claimed, String what) {
        net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event =
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                        player, net.minecraft.world.InteractionHand.MAIN_HAND, target);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() != claimed) {
            throw new GameTestAssertException(Component.literal(what + ": the click was "
                    + (event.isCanceled() ? "claimed" : "left to vanilla")
                    + ", and it should have been "
                    + (claimed ? "claimed" : "left to vanilla")), 0);
        }
    }

    private static void assertHeld(GameTestHelper helper,
            net.minecraft.world.entity.player.Player player, int expected, String complaint) {
        int count = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount();
        if (count != expected) {
            throw new GameTestAssertException(Component.literal(
                    complaint + " - expected " + expected + " left in hand, found " + count), 0);
        }
    }

    /**
     * <b>A research paper written before papers named a pair still reads.</b>
     *
     * <p>{@code horsegenetics:research_gene} used to be a bare {@code String}
     * gene key and is now a {@link com.example.horsegenetics.common.genetics.ResearchTopic}.
     * The owner has papers in chests, on shelves and in villager windows holding
     * the old shape, and the whole of the compatibility is one
     * {@code Codec.either(record, string)} in
     * {@link com.example.horsegenetics.neoforge.data.ResearchTopicCodecs} -
     * the first back-compat path in this repo, written before CLAUDE.md hard
     * rule 10 made one the norm.
     *
     * <p><b>It fails silently and could not be unit-tested.</b> A component whose
     * persistent codec refuses its stored value is <i>dropped</i> on load, with a
     * warning nobody reads and no crash: the paper becomes a blank, files into no
     * shelf and crafts no carrot. That is also why the check is here rather than
     * in JUnit - the NeoForge module's test classpath deliberately carries no
     * Minecraft, so {@code NbtOps} and a real {@link ItemStack} only exist inside
     * a booted game.
     *
     * <p>Three assertions, in the order they would break: the legacy string
     * decodes at all; it decodes to the homozygous non-wild pair the owner asked
     * for; and a real stack carrying the legacy tag comes back out of
     * {@code ItemStack.CODEC} with a usable topic on it.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OLD_PAPERS_STILL_READ =
            TEST_FUNCTIONS.register("old_papers_still_read", () -> ModGameTests::oldPapersStillRead);

    private static void oldPapersStillRead(GameTestHelper helper) {
        com.example.horsegenetics.common.genetics.Gene gene =
                com.example.horsegenetics.common.genetics.Genes.codeOrder().stream()
                        .filter(com.example.horsegenetics.common.genetics.Gene::hasGeneCarrot)
                        .findFirst()
                        .orElseThrow(() -> new GameTestAssertException(Component.literal(
                                "no gene in this build has a gene carrot - this test's premise is"
                                        + " broken, not the codec"), 0));

        // 1. The bare string a pre-change paper stored decodes at all.
        com.example.horsegenetics.common.genetics.ResearchTopic decoded =
                com.example.horsegenetics.neoforge.data.ResearchTopicCodecs.CODEC
                        .parse(net.minecraft.nbt.NbtOps.INSTANCE,
                                net.minecraft.nbt.StringTag.valueOf(gene.key()))
                        .result()
                        .orElse(null);
        if (decoded == null) {
            throw new GameTestAssertException(Component.literal(
                    "research_gene no longer decodes a bare gene-key string - every research paper"
                            + " in the owner's world has just become blank. See"
                            + " ResearchTopicCodecs.CODEC."), 0);
        }

        // 2. It decodes to what the owner asked for: the homozygous non-wild pair.
        com.example.horsegenetics.common.genetics.ResearchTopic expected =
                com.example.horsegenetics.common.genetics.ResearchTopic.wholeGene(gene.key());
        if (!expected.equals(decoded)) {
            throw new GameTestAssertException(Component.literal(
                    "a legacy paper for " + gene.key() + " decoded to " + decoded.token()
                            + " instead of " + expected.token()), 0);
        }

        // 3. The same thing through a real item stack, which is the path the game
        // actually takes - the component codec is only reached via ItemStack.CODEC.
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", "horsegenetics:research_paper");
        tag.putInt("count", 1);
        net.minecraft.nbt.CompoundTag components = new net.minecraft.nbt.CompoundTag();
        components.putString("horsegenetics:research_gene", gene.key());
        tag.put("components", components);

        ItemStack revived = ItemStack.CODEC
                .parse(helper.getLevel().registryAccess().createSerializationContext(
                        net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result()
                .orElse(null);
        if (revived == null || revived.isEmpty()) {
            throw new GameTestAssertException(Component.literal(
                    "a research paper stack holding the legacy research_gene string would not load"), 0);
        }
        com.example.horsegenetics.common.genetics.ResearchTopic onStack =
                com.example.horsegenetics.neoforge.item.ResearchPaperItem.topicOf(revived);
        if (onStack == null || !onStack.isResolved()) {
            throw new GameTestAssertException(Component.literal(
                    "a loaded legacy paper carries no usable topic (" + onStack + ") - it would show"
                            + " as Blank, file into no shelf and craft no carrot"), 0);
        }
        if (!com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity
                .isFiledPaper(revived)) {
            throw new GameTestAssertException(Component.literal(
                    "a loaded legacy paper is not accepted by the research shelf"), 0);
        }

        // 4. And the other direction, which is the mirror failure: a NEW paper
        // has to survive being written and read back, or every paper in a chest
        // is blank the next time the world loads. The compound pair is the case
        // the legacy string could never have produced.
        com.example.horsegenetics.common.genetics.ResearchTopic compound = null;
        for (com.example.horsegenetics.common.genetics.ResearchTopic candidate
                : com.example.horsegenetics.common.genetics.ResearchTopic.lootPool(gene)) {
            if (!candidate.homozygous()) {
                compound = candidate;
                break;
            }
        }
        com.example.horsegenetics.common.genetics.ResearchTopic written =
                compound == null ? expected : compound;
        var ops = helper.getLevel().registryAccess()
                .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        net.minecraft.nbt.Tag encoded = ItemStack.CODEC
                .encodeStart(ops, com.example.horsegenetics.neoforge.item.ResearchPaperItem.of(written))
                .result()
                .orElse(null);
        ItemStack roundTripped = encoded == null ? null
                : ItemStack.CODEC.parse(ops, encoded).result().orElse(null);
        com.example.horsegenetics.common.genetics.ResearchTopic back = roundTripped == null ? null
                : com.example.horsegenetics.neoforge.item.ResearchPaperItem.topicOf(roundTripped);
        if (!written.equals(back)) {
            throw new GameTestAssertException(Component.literal(
                    "a research paper for " + written.token() + " does not survive a save and load"
                            + " (came back as " + back + ") - every paper in the world would blank"
                            + " on the next reload"), 0);
        }
        helper.succeed();
    }

    /**
     * <b>A genome code too long for one NBT string still saves</b> (issue #211).
     *
     * <p>Every NBT string is written with {@code DataOutput.writeUTF}, which throws
     * above 65,535 bytes, and an epigenome code grows with every gene that has an
     * epigenetic schema. Past that line a horse's record would no longer write: the
     * chunk it stands in, the ancestry file, a seed jar or a transfer paper holding
     * it, and the record payloads, which are NBT too. So the saved codecs write a
     * long code in pieces ({@code CodeChunks}), and this is the check that they do.
     *
     * <p>It is here and not in JUnit because the failure is in the game's own NBT
     * writer, which the module's test classpath does not carry. Each codec is taken
     * to bytes and back the way a region file holds it. Then the mirror: a code that
     * fits is still the bare string it always was, and a bare string still reads, so
     * no existing save changes shape (CLAUDE.md hard rule 10).
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LONG_EPIGENOME_STILL_SAVES =
            TEST_FUNCTIONS.register("long_epigenome_still_saves", () -> ModGameTests::longEpigenomeStillSaves);

    private static void longEpigenomeStillSaves(GameTestHelper helper) {
        // The codecs never parse the code, so its content does not matter - only its length.
        String longCode = "ab:1.5;".repeat(10000);
        String shortCode = "ab:1.5;";
        String genotype = com.example.horsegenetics.common.genetics.Genotype
                .random(new com.example.horsegenetics.common.SeededRng(1L)).toCode();
        java.util.UUID id = java.util.UUID.fromString("00000000-0000-0211-0000-000000000211");

        for (String code : new String[] {longCode, shortCode, ""}) {
            com.example.horsegenetics.common.horse.HorseRecord record =
                    new com.example.horsegenetics.common.horse.HorseRecord(id, "Long", "Code",
                            java.util.Optional.empty(), genotype, code, java.util.Optional.empty(),
                            java.util.Optional.empty(), java.util.Optional.empty(),
                            java.util.Optional.empty(), java.util.Optional.empty(), 0,
                            java.util.Optional.empty(), false, java.util.Optional.empty());
            String what = code.length() + "-char epigenome";
            com.example.horsegenetics.common.horse.HorseRecord recordBack = throughDisk(
                    com.example.horsegenetics.neoforge.data.HorseRecordCodecs.CODEC, record,
                    "a horse record with a " + what);
            if (!record.equals(recordBack)) {
                throw new GameTestAssertException(Component.literal(
                        "a horse record with a " + what + " came back from disk different"), 0);
            }
            com.example.horsegenetics.neoforge.data.StoredGenome jar =
                    new com.example.horsegenetics.neoforge.data.StoredGenome(genotype, code, id, "Long Code", "");
            if (!jar.equals(throughDisk(com.example.horsegenetics.neoforge.data.StoredGenome.CODEC, jar,
                    "a stored genome with a " + what))) {
                throw new GameTestAssertException(Component.literal(
                        "a stored genome with a " + what + " came back from disk different"), 0);
            }
            com.example.horsegenetics.common.horse.TransferDeed deed =
                    new com.example.horsegenetics.common.horse.TransferDeed(id, "Long Code",
                            java.util.Optional.empty(), java.util.Optional.empty(), "", genotype, code);
            if (!deed.equals(throughDisk(com.example.horsegenetics.neoforge.data.TransferDeedCodecs.CODEC, deed,
                    "a transfer paper with a " + what))) {
                throw new GameTestAssertException(Component.literal(
                        "a transfer paper with a " + what + " came back from disk different"), 0);
            }
            // The wire: a record payload is this codec as NBT, so it has the same limit.
            io.netty.buffer.ByteBuf buf = Unpooled.buffer();
            try {
                com.example.horsegenetics.neoforge.data.HorseRecordCodecs.STREAM_CODEC.encode(buf, record);
                if (!record.equals(com.example.horsegenetics.neoforge.data.HorseRecordCodecs.STREAM_CODEC.decode(buf))) {
                    throw new GameTestAssertException(Component.literal(
                            "a horse record with a " + what + " came off the wire different"), 0);
                }
            } catch (RuntimeException e) {
                if (e instanceof GameTestAssertException) {
                    throw e;
                }
                throw new GameTestAssertException(Component.literal(
                        "a horse record with a " + what + " does not cross the wire: " + e), 0);
            } finally {
                buf.release();
            }
        }

        // The mirror: a code that fits is saved as the bare string every existing world holds...
        com.example.horsegenetics.neoforge.data.StoredGenome small =
                new com.example.horsegenetics.neoforge.data.StoredGenome(genotype, shortCode, id, "Short", "");
        net.minecraft.nbt.Tag smallTag = com.example.horsegenetics.neoforge.data.StoredGenome.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, small).getOrThrow();
        net.minecraft.nbt.Tag field = ((net.minecraft.nbt.CompoundTag) smallTag).get("epigenome");
        if (!(field instanceof net.minecraft.nbt.StringTag)) {
            throw new GameTestAssertException(Component.literal(
                    "a short epigenome code is no longer saved as a bare string (" + field
                            + ") - an older release could not read a save this one wrote"), 0);
        }
        // ...and a long one is not.
        net.minecraft.nbt.Tag bigField = ((net.minecraft.nbt.CompoundTag)
                com.example.horsegenetics.neoforge.data.StoredGenome.CODEC.encodeStart(
                        net.minecraft.nbt.NbtOps.INSTANCE,
                        new com.example.horsegenetics.neoforge.data.StoredGenome(genotype, longCode, id, "Long", ""))
                        .getOrThrow()).get("epigenome");
        if (!(bigField instanceof net.minecraft.nbt.ListTag)) {
            throw new GameTestAssertException(Component.literal(
                    "a " + longCode.length() + "-char epigenome code was not saved in pieces"), 0);
        }
        helper.succeed();
    }

    /** Encode, write as a region file would, read and decode; a failure anywhere names the step. */
    private static <T> T throughDisk(com.mojang.serialization.Codec<T> codec, T value, String what) {
        net.minecraft.nbt.Tag encoded = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, value)
                .result().orElse(null);
        if (!(encoded instanceof net.minecraft.nbt.CompoundTag tag)) {
            throw new GameTestAssertException(Component.literal(what + " does not encode"), 0);
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
                net.minecraft.nbt.NbtIo.write(tag, out);
            }
            net.minecraft.nbt.CompoundTag back;
            try (java.io.DataInputStream in = new java.io.DataInputStream(
                    new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                back = net.minecraft.nbt.NbtIo.read(in);
            }
            return codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, back).result().orElse(null);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException(Component.literal(
                    what + " cannot be written to disk: " + e), 0);
        }
    }

    /**
     * <b>Vanilla hay is still in the bale tags.</b> Grazing and hand-feeding hay
     * used to be {@code st.is(Blocks.HAY_BLOCK)} in Java; they are now a datapack
     * tag, so that another mod's bale can join without a code change
     * ({@link HayBales}). The cost of that trade is a new silent failure: rename
     * either tag file, or break its JSON, and every horse in the game stops eating
     * hay at all, with nothing logged and no crash. This is the tripwire.
     *
     * <p>It deliberately asserts only about <b>vanilla's</b> bale. The modded
     * entries are {@code required: false} by design and the test must pass with no
     * other mod installed, which is how it will almost always be run.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAY_IS_STILL_A_BALE =
            TEST_FUNCTIONS.register("hay_is_still_a_bale", () -> ModGameTests::hayIsStillABale);

    private static void hayIsStillABale(GameTestHelper helper) {
        if (!HayBales.isBale(Blocks.HAY_BLOCK.defaultBlockState())) {
            throw new GameTestAssertException(Component.literal(
                    "minecraft:hay_block is not in horsegenetics:hay_bales - horses have stopped"
                            + " grazing hay. Check data/horsegenetics/tags/block/hay_bales.json."), 0);
        }
        if (!HayBales.isBale(new ItemStack(Items.HAY_BLOCK))) {
            throw new GameTestAssertException(Component.literal(
                    "minecraft:hay_block is not in the horsegenetics:hay_bales ITEM tag - a"
                            + " wheat-diet horse has stopped taking hay from the hand."
                            + " Check data/horsegenetics/tags/item/hay_bales.json."), 0);
        }
        helper.succeed();
    }

    /**
     * <b>A hungry horse eats a hay bale under a low roof</b> (#26). The yard's GRAZING cell,
     * block for block: three wide, a stone floor, glass walls two high and a glass lid
     * on them, one bale a row in from the north wall, and a vanilla-size horse five
     * blocks south of it. A horse used to reach a bale only by standing on top of it
     * ({@code HungerFoodGoal#pathTo}), and under this lid there is no room to, so it
     * starved beside it. Passes when the horse has eaten.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAY_UNDER_A_LOW_ROOF_IS_EATEN =
            TEST_FUNCTIONS.register("hay_under_a_low_roof_is_eaten", () -> h -> hayCell(h, 80, true, false));

    /**
     * <b>A hungry horse eats from a stack of bales</b> (#26) - the same cell open to the
     * sky, with a second bale on the first. Standing on top of a two-bale stack is a
     * jump no horse makes, so this failed the same way the low roof did, and a stack is
     * how a stable keeps its hay.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STACKED_HAY_IS_EATEN =
            TEST_FUNCTIONS.register("stacked_hay_is_eaten", () -> h -> hayCell(h, 90, false, true));

    private static void hayCell(GameTestHelper helper, int up, boolean roofed, boolean stacked) {
        keepTicking(helper);
        ServerLevel level = helper.getLevel();
        // In the air, so the harness floor and its neighbours are out of reach: walls
        // at x -2 and 2 and z -2 and 8 round a 3 x 9 floor, the bale at the origin.
        BlockPos bale = helper.absolutePos(BlockPos.ZERO).above(up);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 8; z++) {
                level.setBlock(bale.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                boolean wall = x == -2 || x == 2 || z == -2 || z == 8;
                for (int y = 0; y <= 1; y++) {
                    level.setBlock(bale.offset(x, y, z),
                            wall ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(bale.offset(x, 2, z),
                        roofed ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        }
        level.setBlock(bale, Blocks.HAY_BLOCK.defaultBlockState(), 2);
        if (stacked) {
            level.setBlock(bale.above(), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        }
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, BlockPos.ZERO.above(up).offset(0, 0, 5));
        var hunger = com.example.horsegenetics.neoforge.data.ModAttachments.HUNGER.get();
        boolean[] ready = {false};
        helper.onEachTick(() -> {
            if (ready[0] || !com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
                return;
            }
            // Founded: now an ordinary eater (the wild type), vanilla size and hungry.
            com.example.horsegenetics.neoforge.server.HorseRecords.apply(horse,
                    com.example.horsegenetics.neoforge.server.HorseRecords.newFounder(horse,
                            new com.example.horsegenetics.neoforge.NeoRng(horse.getRandom()),
                            com.example.horsegenetics.common.genetics.Genotype.wildType()));
            horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.0);
            horse.snapTo(bale.getX() + 0.5, bale.getY(), bale.getZ() + 5.5, 0.0F, 0.0F);
            horse.setData(hunger, 20.0);
            ready[0] = true;
        });
        helper.succeedWhen(() -> {
            if (!ready[0] || horse.getData(hunger) < 60.0) {
                throw new GameTestAssertException(Component.literal("a hungry horse has not eaten the "
                        + (stacked ? "stacked bales" : "bale under a two-block roof") + " in its cell (hunger "
                        + Math.round(horse.getData(hunger)) + ", bale " + (level.getBlockState(bale).is(Blocks.HAY_BLOCK)
                        ? "still there" : "gone") + ", horse at " + horse.blockPosition().subtract(bale).toShortString()
                        + " from it) - HungerFoodGoal is asking whether it can stand on the bale, not beside it"
                        + " (#26), or took a null or cached createPath for unreachable (#39)"), 0);
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 8; z++) {
                    for (int y = -1; y <= 2; y++) {
                        level.setBlock(bale.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        });
    }

    /**
     * <b>The stasis bank can still read the horse inside a chamber.</b>
     *
     * <p>A stored horse is a {@code saveWithoutId} tag on an item, and the
     * bank's upkeep changes it by reaching into that tag by name -
     * {@code Health}, the {@code base} of the {@code minecraft:max_health}
     * attribute, and the {@code hunger} and {@code horse_record} attachments
     * under {@code neoforge:attachments}. See {@link StasisCare}.
     *
     * <p><b>Every one of those names fails silently.</b> Rename an attachment,
     * or let a version change the attribute layout, and nothing throws and
     * nothing is logged: the reads simply return their defaults, the bank
     * decides the horse is not hurt, and it quietly never heals anything again.
     * A player would see a bank that eats hay and mends nothing, or - worse -
     * a bank that mends without ever charging hunger for it. This is the
     * tripwire, and it is the only thing that can be.
     *
     * <p>It takes a real horse, hurts it, snapshots it exactly as a capture
     * would, and then asserts both directions: that the four numbers come back
     * out, and that one turn of upkeep actually moves the health in the tag.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STASIS_TAG_IS_READABLE =
            TEST_FUNCTIONS.register("stasis_tag_is_readable", () -> ModGameTests::stasisTagIsReadable);

    /** Fed enough to buy healing, and not {@code Hunger.FULL}, which a failed read returns. */
    private static final double STASIS_TRIPWIRE_HUNGER = 80.0;

    private static void stasisTagIsReadable(GameTestHelper helper) {
        net.minecraft.world.entity.animal.equine.Horse horse =
                helper.spawn(net.minecraft.world.entity.EntityType.HORSE, net.minecraft.core.BlockPos.ZERO);
        // A horse is founded on a later tick, not on the one it is spawned on -
        // see HorseFoundingTickHandler. Snapshotting it immediately would test a
        // horse with no papers, which is not the case the bank ever meets.
        helper.runAfterDelay(10L, () -> stasisTagIsReadable(helper, horse));
    }

    private static void stasisTagIsReadable(GameTestHelper helper,
                                            net.minecraft.world.entity.animal.equine.Horse horse) {
        if (!com.example.horsegenetics.neoforge.server.HorseRecords.hasRealRecord(horse)) {
            throw new GameTestAssertException(Component.literal(
                    "the spawned horse still has no record ten ticks in - this test's premise is"
                            + " broken, not the bank"), 0);
        }
        float max = horse.getMaxHealth();
        horse.setHealth(max / 2.0F);

        // WRITE A HUNGER, SO THERE IS ONE TO SAVE. A NeoForge attachment is
        // materialised on first access, and HorseCareHandler is the thing that
        // usually asks - on a slow tick phased by entity id, which by tick ten
        // may or may not have come round. Without this the horse is sometimes
        // snapshotted with no hunger attachment at all, StasisCare.putHunger
        // then refuses to invent one (correctly - inventing it is healing for
        // free), and the healing assertion below fails on one run in several
        // with nothing wrong with the bank. Every horse the bank ever really
        // meets has been ticked for far longer than ten ticks.
        //
        // Not Hunger.FULL, for two reasons (#38). That same slow tick may have
        // drained it a little already, so the stored value is not FULL on some
        // runs. And StasisCare.hunger answers FULL for a tag it cannot read, so
        // comparing against FULL could never catch the renamed attachment this
        // check exists for. A value the default can't produce catches both.
        horse.setData(com.example.horsegenetics.neoforge.data.ModAttachments.HUNGER.get(), STASIS_TRIPWIRE_HUNGER);

        com.example.horsegenetics.neoforge.data.StasisSnapshot snapshot =
                com.example.horsegenetics.neoforge.server.HorseStasisHandler.snapshot(horse, "Tripwire");
        net.minecraft.nbt.CompoundTag tag = snapshot.horse();

        float readMax = StasisCare.maxHealth(tag, StasisCare.record(tag));
        if (Math.abs(readMax - max) > 0.01F) {
            throw new GameTestAssertException(Component.literal(
                    "the bank cannot read a stored horse's maximum health: the entity says " + max
                            + " and its saved tag reads " + readMax + ". The upkeep will decide every"
                            + " horse in every bank is unhurt and heal nothing, silently."
                            + " Check LivingEntity.TAG_ATTRIBUTES and the 'base' field in StasisCare."), 0);
        }
        double hunger = StasisCare.hunger(tag);
        if (Math.abs(hunger - STASIS_TRIPWIRE_HUNGER) > 0.01) {
            throw new GameTestAssertException(Component.literal(
                    "the bank cannot read a stored horse's hunger: expected "
                            + STASIS_TRIPWIRE_HUNGER + " and read " + hunger
                            + ". Healing in a bank would be free, or would never happen."
                            + " Check the hunger attachment's id in StasisCare."), 0);
        }

        // ...and the write half, through the real component on a real chamber.
        net.minecraft.world.item.ItemStack chamber = new net.minecraft.world.item.ItemStack(
                com.example.horsegenetics.neoforge.item.ModItems.INTERMEDIATE_STASIS_CHAMBER.get());
        chamber.set(com.example.horsegenetics.neoforge.data.ModDataComponents.STASIS_SNAPSHOT.get(), snapshot);
        if (!StasisCare.isHurt(chamber)) {
            throw new GameTestAssertException(Component.literal(
                    "a chamber holding a horse on half health does not read as hurt"), 0);
        }
        com.example.horsegenetics.common.horse.StasisUpkeep.Result turn = StasisCare.turn(
                chamber, new net.minecraft.world.item.ItemStack(Items.HAY_BLOCK),
                com.example.horsegenetics.common.horse.StasisUpkeep.WATER_PER_BUCKET);
        if (turn == null || turn.healed() <= 0.0) {
            throw new GameTestAssertException(Component.literal(
                    "one turn of bank upkeep on a hurt, fed, watered horse healed nothing"
                            + " (result " + turn + ", attachments "
                            + tag.getCompoundOrEmpty(net.neoforged.neoforge.attachment.AttachmentHolder
                                    .ATTACHMENTS_NBT_KEY).keySet() + ")"), 0);
        }
        float after = snapshotHealth(chamber);
        if (after <= max / 2.0F) {
            throw new GameTestAssertException(Component.literal(
                    "the upkeep reported healing " + turn.healed() + " but the chamber still holds a"
                            + " horse on " + after + " health - the mended tag was not written back"), 0);
        }
        stasisAgeIsReadable(horse);

        // The horse goes first, because the last half of this releases a horse
        // out of that same tag - and a tag remembers its UUID, so letting it out
        // while the original is still standing there is the one thing
        // addFreshEntity refuses. That refusal is not a bug, it is
        // HorseStasisHandler.alreadyLoose's whole reason for existing.
        horse.discard();
        stasisReproSurvivesTheChamber(helper, snapshot);

        HorseGenetics.LOGGER.info("[gametest] a stored horse reads back: max {}, hunger {}, and healed to {}",
                readMax, hunger, after);
        helper.succeed();
    }

    /**
     * <b>The drop buffer's half of the tripwire.</b> A stored horse's age is the
     * one field {@code StasisCare} names as a bare literal - {@code AgeableMob}
     * writes {@code "Age"} and exports no constant for it - and a produce
     * ability gated on {@code adult} reads it. A rename would not throw: every
     * shelved foal would simply start laying eggs, quietly.
     */
    private static void stasisAgeIsReadable(net.minecraft.world.entity.animal.equine.Horse horse) {
        net.minecraft.nbt.CompoundTag grown =
                com.example.horsegenetics.neoforge.server.HorseStasisHandler.snapshot(horse, "Tripwire").horse();
        if (StasisCare.isBaby(grown)) {
            throw new GameTestAssertException(Component.literal(
                    "a grown horse's saved tag reads as a foal - StasisCare.isBaby has the wrong key,"
                            + " and every produce ability gated on 'adult' now refuses in a bank"), 0);
        }
        horse.setBaby(true);
        try {
            net.minecraft.nbt.CompoundTag young =
                    com.example.horsegenetics.neoforge.server.HorseStasisHandler.snapshot(horse, "Tripwire").horse();
            if (!StasisCare.isBaby(young)) {
                throw new GameTestAssertException(Component.literal(
                        "a foal's saved tag reads as an adult - StasisCare.isBaby has the wrong key,"
                                + " and a shelved foal would produce as if it were grown."
                                + " Check AgeableMob's 'Age' field in 26.1.2."), 0);
            }
        } finally {
            horse.setBaby(false);
        }
    }

    /**
     * <b>In-bank breeding's half of the tripwire.</b> The bank conceives a
     * pregnancy into a stored mare's tag and she is released to foal days later,
     * so the round trip has to survive a write, a save and a real
     * {@code Horse.load}. Every step of it fails silently: a wrong attachment id
     * writes into nowhere, a broken codec parses to a default, and either way
     * the mare comes out of the chamber empty and nobody is told she lost
     * anything.
     */
    private static void stasisReproSurvivesTheChamber(
            GameTestHelper helper, com.example.horsegenetics.neoforge.data.StasisSnapshot snapshot) {
        net.minecraft.nbt.CompoundTag tag = snapshot.horse().copy();

        com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment cooldowns =
                StasisCare.cooldowns(tag).stamp("stasis_tripwire", 1234L);
        if (!StasisCare.putCooldowns(tag, cooldowns) || StasisCare.cooldowns(tag).last("stasis_tripwire") != 1234L) {
            throw new GameTestAssertException(Component.literal(
                    "a cooldown stamp does not survive a round trip through a stored horse's tag -"
                            + " the drop buffer would produce on every single turn, forever"), 0);
        }

        com.example.horsegenetics.common.repro.Reproduction bred =
                StasisCare.repro(tag, snapshot.horseId()).withNaturalTry(4321L);
        if (!StasisCare.putRepro(tag, bred) || StasisCare.repro(tag, snapshot.horseId()).lastNaturalTry() != 4321L) {
            throw new GameTestAssertException(Component.literal(
                    "a mare's reproductive record does not survive a round trip through a stored"
                            + " horse's tag - a bank would cover her again every turn of her heat."
                            + " Check the HORSE_REPRO attachment id and ReproCodecs in StasisCare."), 0);
        }

        // ...and the half no amount of tag-reading proves: that the game itself
        // reads it back. This is the call the bank makes when a mare is due.
        net.minecraft.world.entity.animal.equine.Horse released =
                com.example.horsegenetics.neoforge.server.HorseStasisHandler.release(
                        helper.getLevel(), helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO), 0.0F,
                        new com.example.horsegenetics.neoforge.data.StasisSnapshot(
                                snapshot.horseName(), snapshot.horseId(), tag));
        if (released == null) {
            throw new GameTestAssertException(Component.literal(
                    "a horse written back by StasisCare would not load - the bank cannot release a"
                            + " mare to foal, and she would sit in her chamber past her due date"), 0);
        }
        long readBack = com.example.horsegenetics.neoforge.server.ReproHandler.of(released).lastNaturalTry();
        released.discard();
        if (readBack != 4321L) {
            throw new GameTestAssertException(Component.literal(
                    "a mare released from a chamber has lost the breeding the bank wrote onto her:"
                            + " expected lastNaturalTry 4321 and the live horse says " + readBack
                            + ". A bank-bred pregnancy would vanish the moment she came out."), 0);
        }
    }

    /** The health in the chamber's snapshot, for the assertion above. */
    private static float snapshotHealth(net.minecraft.world.item.ItemStack chamber) {
        com.example.horsegenetics.neoforge.data.StasisSnapshot held =
                com.example.horsegenetics.neoforge.item.StasisChamberItem.snapshotOf(chamber);
        return held == null ? -1.0F
                : held.horse().getFloatOr(net.minecraft.world.entity.LivingEntity.TAG_HEALTH, -1.0F);
    }

    /**
     * <b>The Chaos roster</b> - what the Chaos allele at Lycanthropy, Leader of the
     * pack and Spawner can turn a seed into. Two halves. The classifier is put
     * through vanilla mobs whose answers are known, because a dev run has no
     * other mod with mobs in it and the modded lists are empty here; that is the
     * half that proves the rules. Then the real lists: sorted, never vanilla or
     * ours, the same twice, nested lycan-in-pack-in-spawner, and an empty list
     * picks nothing rather than throwing. A real modded mob is a play check on
     * gene-lycan.html's Verification tab.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHAOS_ROSTER =
            TEST_FUNCTIONS.register("chaos_roster", () -> ModGameTests::chaosRoster);

    private static void chaosRoster(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var L = com.example.horsegenetics.neoforge.server.ChaosRoster.Use.LYCAN;
        var P = com.example.horsegenetics.neoforge.server.ChaosRoster.Use.PACK;
        var S = com.example.horsegenetics.neoforge.server.ChaosRoster.Use.SPAWNER;
        expectUses(helper, level, net.minecraft.world.entity.EntityType.WOLF, java.util.EnumSet.of(L, P, S));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.BAT, java.util.EnumSet.of(P, S));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.COD, java.util.EnumSet.of(P, S));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.BEE, java.util.EnumSet.of(P, S));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.ZOMBIE, java.util.EnumSet.of(S));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.HORSE,
                java.util.EnumSet.noneOf(com.example.horsegenetics.neoforge.server.ChaosRoster.Use.class));
        expectUses(helper, level, net.minecraft.world.entity.EntityType.ARROW,
                java.util.EnumSet.noneOf(com.example.horsegenetics.neoforge.server.ChaosRoster.Use.class));

        List<String> previous = null;
        for (var use : com.example.horsegenetics.neoforge.server.ChaosRoster.Use.values()) {
            var list = com.example.horsegenetics.neoforge.server.ChaosRoster.list(use, level);
            if (list != com.example.horsegenetics.neoforge.server.ChaosRoster.list(use, level)) {
                helper.fail("the " + use + " roster was rebuilt between two calls with the config unchanged");
            }
            List<String> ids = new ArrayList<>();
            for (var type : list) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                if (id.startsWith("minecraft:") || id.startsWith(HorseGenetics.MOD_ID + ":")) {
                    helper.fail("the " + use + " roster names " + id + " - Chaos is other mods' mobs only");
                }
                ids.add(id);
            }
            List<String> sorted = new ArrayList<>(ids);
            java.util.Collections.sort(sorted);
            if (!sorted.equals(ids)) {
                helper.fail("the " + use + " roster is not sorted by id, so a seed means different mobs on two servers");
            }
            if (previous != null && !ids.containsAll(previous)) {
                helper.fail("the " + use + " roster is missing a mob the stricter list before it has: " + previous);
            }
            previous = ids;
            if (ids.isEmpty() && com.example.horsegenetics.neoforge.server.ChaosRoster.pick(use, 42L, level) != null) {
                helper.fail("an empty " + use + " roster picked a mob");
            }
        }
        helper.succeed();
    }

    private static void expectUses(GameTestHelper helper, ServerLevel level,
                                   net.minecraft.world.entity.EntityType<?> type,
                                   java.util.Set<com.example.horsegenetics.neoforge.server.ChaosRoster.Use> expected) {
        var got = com.example.horsegenetics.neoforge.server.ChaosRoster.classify(type, level);
        if (!got.equals(expected)) {
            helper.fail(BuiltInRegistries.ENTITY_TYPE.getKey(type) + " classified as " + got + ", expected " + expected);
        }
    }

    /**
     * <b>A cowboy never stands a horse in powder snow</b> (issue #18).
     *
     * <p>Powder snow has no collision shape for a box test, so the old
     * {@code roomForAHorse} - solid below, no liquid, no collision - passed a
     * spot full of it, and a dealer in a snowy biome froze his whole string in
     * minutes. Four spots are asked about: a plain one first, so a check that
     * refuses everything cannot pass; then powder snow at the feet, under the
     * feet, and beside them inside the horse's own width, since a horse is
     * wider than its block and touching the snow is enough to freeze.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NO_HORSE_STANDS_IN_POWDER_SNOW =
            TEST_FUNCTIONS.register("no_horse_stands_in_powder_snow",
                    () -> ModGameTests::noHorseStandsInPowderSnow);

    private static void noHorseStandsInPowderSnow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos feet = helper.absolutePos(BlockPos.ZERO).above(40);
        BlockState powder = Blocks.POWDER_SNOW.defaultBlockState();
        try {
            reset(level, feet);
            if (!com.example.horsegenetics.neoforge.server.CowboyHandler.roomForAHorse(level, feet)) {
                helper.fail("a plain spot on a stone floor was refused, so this test proves nothing.");
                return;
            }
            String[] where = {"at its feet", "under its feet", "beside it, inside its width"};
            BlockPos[] snow = {feet, feet.below(), feet.east()};
            for (int i = 0; i < snow.length; i++) {
                reset(level, feet);
                level.setBlock(snow[i], powder, 2);
                if (com.example.horsegenetics.neoforge.server.CowboyHandler.roomForAHorse(level, feet)) {
                    helper.fail("a horse would be placed with powder snow " + where[i]
                            + " - it cannot get out and freezes to death (#18).");
                    return;
                }
            }
        } finally {
            fill(level, feet, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /**
     * <b>A cowboy's string keeps off the water, and a whirlpool gives it back</b>
     * (issue #30).
     *
     * <p>A stone shore beside a basin of water five deep, with a magma block
     * under its middle and a downward bubble column over it - the sea-floor
     * whirlpool that took a whole string on seed 20261002. Three horses, all in
     * one tick: a branded one in the column must come out onto the shore, dry,
     * on solid ground; an unbranded one in the same column must be left alone
     * (only the dealer's own are his to protect); and a branded one on the shore
     * must get a water malus that keeps every path out of the basin.
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COWBOY_STOCK_LEAVES_THE_WHIRLPOOL =
            TEST_FUNCTIONS.register("cowboy_stock_leaves_the_whirlpool",
                    () -> ModGameTests::cowboyStockLeavesTheWhirlpool);

    private static void cowboyStockLeavesTheWhirlpool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).above(40);
        try {
            fill(level, origin, Blocks.AIR.defaultBlockState());
            // The shore: z -8..-1. The basin's stone shell: z -1..5, y -6..-1.
            for (int x = -6; x <= 6; x++) {
                for (int z = -8; z <= 5; z++) {
                    for (int y = -6; y <= -1; y++) {
                        level.setBlock(origin.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = 0; z <= 4; z++) {
                    for (int y = -5; y <= -1; y++) {
                        level.setBlock(origin.offset(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
            level.setBlock(origin.offset(0, -6, 2), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
            for (int y = -5; y <= -1; y++) {
                level.setBlock(origin.offset(0, y, 2), Blocks.BUBBLE_COLUMN.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.BubbleColumnBlock.DRAG_DOWN, true), 2);
            }
            com.example.horsegenetics.neoforge.data.CowboyBrand brand = com.example.horsegenetics.neoforge.data.CowboyBrand.of(java.util.UUID.randomUUID());
            net.minecraft.world.entity.EntityType<net.minecraft.world.entity.animal.equine.Horse> horseType =
                    net.minecraft.world.entity.EntityType.HORSE;

            // A wild horse in the whirlpool first: the rescue is the dealer's, so
            // one that moves this horse is moving everyone's.
            net.minecraft.world.entity.animal.equine.Horse wild = helper.spawn(horseType, BlockPos.ZERO.above(37).south(2));
            wild.setNoAi(true);
            BlockPos wildAt = wild.blockPosition();
            if (com.example.horsegenetics.neoforge.server.CowboyWaterSafety.check(wild, level) != null
                    || !wild.blockPosition().equals(wildAt)) {
                helper.fail("an unbranded horse was moved out of a whirlpool - only a cowboy's string is his to rescue.");
                return;
            }

            net.minecraft.world.entity.animal.equine.Horse caught = helper.spawn(horseType, BlockPos.ZERO.above(37).south(2));
            caught.setNoAi(true);
            caught.setData(com.example.horsegenetics.neoforge.data.ModAttachments.COWBOY_BRAND.get(), brand);
            String moved = com.example.horsegenetics.neoforge.server.CowboyWaterSafety.check(caught, level);
            BlockPos at = caught.blockPosition();
            if (moved == null) {
                helper.fail("a cowboy's horse in a downward bubble column was left there (#30).");
                return;
            }
            // Its own feet, then the basin's whole footprint, then the margin rule.
            boolean inBasin = Math.abs(at.getX() - origin.getX()) <= 2
                    && at.getZ() - origin.getZ() >= 0 && at.getZ() - origin.getZ() <= 4;
            if (!level.getFluidState(at).isEmpty() || inBasin || at.getY() != origin.getY()
                    || !com.example.horsegenetics.neoforge.server.CowboyWaterSafety.fits(caught, level, at)) {
                helper.fail("the rescued horse was put at " + at.toShortString() + ", which is not dry ground "
                        + "a step back from the water (" + moved + ").");
                return;
            }

            net.minecraft.world.entity.animal.equine.Horse shore = helper.spawn(horseType, BlockPos.ZERO.above(40).north(5));
            shore.setNoAi(true);
            shore.setData(com.example.horsegenetics.neoforge.data.ModAttachments.COWBOY_BRAND.get(), brand);
            com.example.horsegenetics.neoforge.server.CowboyWaterSafety.check(shore, level);
            float malus = shore.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER);
            if (malus >= 0.0F) {
                helper.fail("a cowboy's horse on dry land has water malus " + malus
                        + ", so a path can still take it into the sea (#30).");
                return;
            }
            caught.discard();
            wild.discard();
            shore.discard();
        } finally {
            fill(level, origin, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /**
     * <b>A slab floor and a one-block step are each one stall</b> (issue #25).
     *
     * <p>The yard's STALL SHAPES pen, cut to the five stalls that settle it, all
     * read in one tick with {@code StallDetector.forSign} - the call the stall
     * sign makes before it exists. Each is a 2-wide floor inside a stone-brick
     * ring three high, shut by a closed oak gate on the north wall's west tile
     * with open air over it, the sign wall on the west at head height:
     * <ul>
     *   <li><b>SLAB</b> (oak bottom slabs, a 2 x 1 floor) and <b>STEP</b> (the east
     *       column one stone higher) must bind exactly their floor tiles. They
     *       did not: a closed gate is 1.5 tall, so its top read as floor and the
     *       walk climbed over it into the open, and only the wall ring at the
     *       floor's own level brought it back - a ring the slab floor starts
     *       above, and the raised stone cuts in half.</li>
     *   <li><b>PLAIN</b> (flat, closed gate) and <b>FENCES</b> (a ring of oak
     *       fences and a gate) are the shapes that already bound, so a fix that
     *       loses them fails here.</li>
     *   <li><b>DROP</b> (the east column two stones higher) must still not be
     *       one room.</li>
     * </ul>
     */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STALL_SHAPES_BIND_AS_ONE_ROOM =
            TEST_FUNCTIONS.register("stall_shapes_bind_as_one_room",
                    () -> ModGameTests::stallShapesBindAsOneRoom);

    private static void stallShapesBindAsOneRoom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO).above(40);
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, false);
        List<String> wrong = new ArrayList<>();
        try {
            fill(level, o, Blocks.AIR.defaultBlockState());
            // The whole scratch box: the open floor round the stalls must be more
            // than StallDetector.MAX_COLUMNS, as the yard's is, or it closes as a
            // "room" of its own and the controls read differently from the yard.
            for (int x = -SCRATCH; x <= SCRATCH; x++) {
                for (int z = -SCRATCH; z <= SCRATCH; z++) {
                    level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
            // {name, ox, oz, depth, ring (brick 3 high, or fences), floor (0 bare, 1 slab), raise}
            Object[][] stalls = {
                    {"SLAB", -8, -8, 3, true, 1, 0},
                    {"STEP", -3, -8, 4, true, 0, 1},
                    {"DROP", 2, -8, 4, true, 0, 2},
                    {"PLAIN", -8, 0, 3, true, 0, 0},
                    {"FENCES", -3, 0, 4, false, 0, 0},
            };
            for (Object[] s : stalls) {
                String name = (String) s[0];
                int ox = (int) s[1];
                int oz = (int) s[2];
                int depth = (int) s[3];
                boolean bricks = (boolean) s[4];
                for (int x = ox; x <= ox + 3; x++) {
                    for (int z = oz; z <= oz + depth - 1; z++) {
                        boolean edge = x == ox || x == ox + 3 || z == oz || z == oz + depth - 1;
                        if (!edge) {
                            if ((int) s[5] == 1) {
                                level.setBlock(o.offset(x, 0, z), Blocks.OAK_SLAB.defaultBlockState(), 2);
                            }
                            for (int y = 0; y < (x == ox + 2 ? (int) s[6] : 0); y++) {
                                level.setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                            }
                        } else if (bricks) {
                            for (int y = 0; y <= 2; y++) {
                                level.setBlock(o.offset(x, y, z), brick, 2);
                            }
                        } else {
                            // Flag 3, so each post connects to the one before it.
                            level.setBlock(o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                        }
                    }
                }
                // The way in: the north wall's west tile, a closed gate with open air over it.
                for (int y = 0; y <= 2; y++) {
                    level.setBlock(o.offset(ox + 1, y, oz), Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(o.offset(ox + 1, 0, oz), gate, 2);

                BlockPos wall = o.offset(ox, bricks ? 1 : 0, oz + 1);
                com.example.horsegenetics.neoforge.server.StallDetector.Result r =
                        com.example.horsegenetics.neoforge.server.StallDetector.forSign(level, wall,
                                net.minecraft.core.Direction.WEST);
                java.util.Set<Long> want = new java.util.HashSet<>();
                for (int x = ox + 1; x <= ox + 2; x++) {
                    for (int z = oz + 1; z <= oz + depth - 2; z++) {
                        want.add(((long) o.getX() + x << 32) ^ ((o.getZ() + z) & 0xFFFFFFFFL));
                    }
                }
                java.util.Set<Long> got = r == null ? java.util.Set.of()
                        : com.example.horsegenetics.common.stable.StallFill.keysOf(r.region());
                String read = r == null ? "refused" : "bound " + r.blockCount() + " tiles";
                if (name.equals("DROP")) {
                    if (r != null && got.containsAll(want)) {
                        wrong.add("DROP (a two-block drop) " + read + " - it is not one room");
                    }
                } else if (!got.equals(want)) {
                    wrong.add(name + " " + read + ", expected exactly its " + want.size() + " floor tiles");
                }
            }
        } finally {
            fill(level, o, Blocks.AIR.defaultBlockState());
        }
        if (!wrong.isEmpty()) {
            helper.fail("stall shapes (#25): " + String.join("; ", wrong));
            return;
        }
        helper.succeed();
    }

    /** Air around {@code feet}, on a 7x7 stone floor one block down. */
    private static void reset(ServerLevel level, BlockPos feet) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    level.setBlock(feet.offset(x, y, z),
                            (y == -1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * A test in a batch of its own (issue #37). The environment is the batch key and the
     * batches run one after another, so a test with an environment nobody else has runs
     * with no other test alive - for a test that reaches past its cell (a hunting horse
     * looks sixteen blocks out; the grid puts tests six apart), whose neighbours'
     * monsters and pens otherwise answer for it. Pair it with {@link #aloneOnClearGround}.
     * One key per test, so registerEnvironment's duplicate-key throw cannot happen.
     */
    private static void registerAlone(RegisterGameTestsEvent event,
                                      DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> fn,
                                      int maxTicks) {
        Holder<TestEnvironmentDefinition<?>> alone = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "alone_" + fn.getKey().identifier().getPath()),
                new TestEnvironmentDefinition.AllOf(java.util.List.of()));
        register(event, alone, fn, maxTicks);
    }

    private static void register(RegisterGameTestsEvent event,
                                 Holder<TestEnvironmentDefinition<?>> environment,
                                 DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> fn,
                                 int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> key = fn.getKey();
        // minecraft:empty is a real shipped 1x1x1 template of one air block, so a
        // test needs no .nbt of its own. The area really is 1x1x1 though: the
        // no-position assertion overloads measure against the structure bounds,
        // so reach for the ones that take a BlockPos and a distance.
        Identifier structure = Identifier.withDefaultNamespace("empty");
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, structure, maxTicks, 0, true);
        // identifier(), not location() - ResourceLocation is Identifier in this
        // version and ResourceKey's accessor was renamed with it.
        event.registerTest(key.identifier(), d -> new FunctionGameTestInstance(key, d), data);
    }
}
