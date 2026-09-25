package com.example.horsegenetics.neoforge.gametest;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.DoubleFenceGateBlock;
import com.example.horsegenetics.neoforge.block.DoubleGates;
import com.example.horsegenetics.neoforge.compat.HayBales;
import com.example.horsegenetics.neoforge.server.HorseLeads;
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
        register(event, environment, BUILDING_BLOCKS_SURVIVES, 100);
        // Two tag lookups in one tick.
        register(event, environment, HAY_IS_STILL_A_BALE, 100);
        // One horse spawned, saved and read back, all inside a single tick.
        register(event, environment, STASIS_TAG_IS_READABLE, 100);
        // Three codec round trips in one tick.
        register(event, environment, OLD_PAPERS_STILL_READ, 100);
        // Leash, untie, then five ticks for a ground drop to become visible.
        register(event, environment, WHISTLED_LEAD_COMES_BACK, 100);
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
     * <b>A research paper written before papers named a pair still reads.</b>
     *
     * <p>{@code horsegenetics:research_gene} used to be a bare {@code String}
     * gene key and is now a {@link com.example.horsegenetics.common.genetics.ResearchTopic}.
     * The owner has papers in chests, on shelves and in villager windows holding
     * the old shape, and the whole of the compatibility is one
     * {@code Codec.either(record, string)} in
     * {@link com.example.horsegenetics.neoforge.data.ResearchTopicCodecs} -
     * documented as this repo's single deliberate exception to the
     * no-back-compat rule.
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
