package com.example.horsegenetics.neoforge.gametest;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.DoubleFenceGateBlock;
import com.example.horsegenetics.neoforge.block.DoubleGates;
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
