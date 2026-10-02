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
        // the batch key, so sharing one is what keeps them in a single batch.
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
        // One pass over the recipe list inside a single tick; the budget is slack.
        register(event, environment, EVERY_RECIPE_ENCODES, 100);
        // Five recipe lookups in one tick.
        register(event, environment, STASIS_CHAMBERS_CRAFT, 100);
        register(event, environment, BUILDING_BLOCKS_SURVIVES, 100);
        // Two tag lookups in one tick.
        register(event, environment, HAY_IS_STILL_A_BALE, 100);
        // One horse spawned, saved and read back, all inside a single tick.
        register(event, environment, STASIS_TAG_IS_READABLE, 100);
        // Three codec round trips in one tick.
        register(event, environment, OLD_PAPERS_STILL_READ, 100);
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
        // Ten ticks to be founded, a backup and a plan (two flushing saves), then
        // a raise on the next server tick.
        register(event, environment, REALM_BACKUP_RAISES_A_LOST_HORSE, 200);
        // One recipe lookup in a single tick.
        register(event, environment, FREEDOM_STICK_CRAFTS, 100);
        // Spawn, ten ticks to be founded, kill, raise - then read the result.
        register(event, environment, A_DEAD_HORSE_COMES_BACK_WHOLE, 200);
        // Two scratch-box fills and an exhaustive scan, all inside one tick.
        register(event, environment, WAYSTONE_CLEARANCE_IS_NEAREST, 200);
        // Pure arithmetic on a record; no world touched.
        register(event, environment, JOCKEY_PASSES_ADD_UP, 100);
        // One horse spawned and three events posted, all inside a single tick.
        register(event, environment, MOUNTED_MINING_IS_EXEMPT, 100);
        // One horse spawned, two attachment writes, two calls; one tick.
        register(event, environment, WHISTLE_NEEDS_BOND, 100);
        // Four horses spawned and seven interact events posted, all in one tick.
        register(event, environment, RIGHT_CLICK_EQUIPS_TACK, 100);
        // Builds one instance of every mob it classifies, inside one tick.
        register(event, environment, CHAOS_ROSTER, 100);
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

        // ASK FOR THE HUNGER, SO THERE IS ONE TO SAVE. A NeoForge attachment is
        // materialised on first access, and HorseCareHandler is the thing that
        // usually asks - on a slow tick phased by entity id, which by tick ten
        // may or may not have come round. Without this the horse is sometimes
        // snapshotted with no hunger attachment at all, StasisCare.putHunger
        // then refuses to invent one (correctly - inventing it is healing for
        // free), and the healing assertion below fails on one run in several
        // with nothing wrong with the bank. Every horse the bank ever really
        // meets has been ticked for far longer than ten ticks.
        horse.getData(com.example.horsegenetics.neoforge.data.ModAttachments.HUNGER.get());

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
        if (Math.abs(hunger - com.example.horsegenetics.common.care.Hunger.FULL) > 0.01) {
            throw new GameTestAssertException(Component.literal(
                    "the bank cannot read a stored horse's hunger: expected "
                            + com.example.horsegenetics.common.care.Hunger.FULL + " and read " + hunger
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
