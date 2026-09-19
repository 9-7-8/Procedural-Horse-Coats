package com.example.horsegenetics.neoforge.gametest;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.DoubleFenceGateBlock;
import com.example.horsegenetics.neoforge.block.DoubleGates;
import com.example.horsegenetics.neoforge.worldgen.HomesteadCensus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
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

import java.util.Collection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.List;
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
        // The census is one synchronous burst of worldgen arithmetic inside a
        // single tick, so its tick budget is not what bounds it - the sample
        // size is. The generous number is for the environment overrides, which
        // are meant to be raised a long way.
        register(event, environment, HOMESTEAD_STILL_GENERATES, 400);
        // One pass over the recipe list inside a single tick; the budget is slack.
        register(event, environment, EVERY_RECIPE_ENCODES, 100);
        register(event, environment, BUILDING_BLOCKS_SURVIVES, 100);
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
