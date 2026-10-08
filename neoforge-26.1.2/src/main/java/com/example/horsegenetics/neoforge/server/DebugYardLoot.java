package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.GoldenCarrotCropBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.compat.ModdedArmour;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * <b>Row AY: CHEST LOOT (west) and CARROT CROP (east)</b> (2026-10-02). No horses. Two halves of things that ship
 * inside the mod's data and fail without a sound: global loot modifiers that nobody has ever taken anything out of,
 * and a crop whose growth rests on one {@code randomTicks()} call. Every check is a clockwork one - built, run and
 * answered with nobody at the keyboard, each ending in one {@code CLOCKWORK} line.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>footprint</th><th>answers by</th></tr>
 *   <tr><td>west</td><td>CHEST LOOT</td><td>nothing built but a sign (x0+1, z0-1)</td><td>~1 min after build
 *       (the rolls are spread over ticks, 12 ms a tick); deadline 20 min</td></tr>
 *   <tr><td>east</td><td>CARROT CROP</td><td>growth cell x0+25..x0+35, z0+1..z0+9; planting cell x0+38..x0+44,
 *       z0+1..z0+5</td><td>5 s (drops, planting); 5 to 90 min (growth)</td></tr>
 * </table>
 *
 * <h2>CHEST LOOT (west)</h2>
 *
 * <p>The questions. {@code wiki/items.html}, "Tack in chest loot (2026-09-24, NOT played)": <i>"Proven only to the
 * depth a server boot can prove it ... Nothing has been taken out of a chest."</i> <i>"The regex reaches modded
 * chests, and only chests. Open another mod's structure chest and tack should turn up at about one in five; open a
 * block drop, a fishing catch or a mob drop and it must not."</i> <i>"A found saddle and leather armour are visibly
 * dyed, in two unrelated leather colours with cheap fittings - and never a gem fitting"</i>. <i>"No gem fitting, and
 * no diamond flood. Diamond horse armour is weight 1 of 29; several chests in a row producing one means the weighted
 * chain is wrong."</i> <i>"Rates feel right rather than merely different. 20% seeds, 30% research papers and 22% tack
 * per chest"</i>. "Golden carrot seeds (2026-09-18)": <i>"They turn up in chests - now 20% in any chest rather than
 * 7% across eight named ones"</i>. {@code wiki/item-research-papers.html}: <i>"A chest and the supplier. Found and
 * bought papers should only ever be carrier or true-breeding - a compound pair out of a chest means
 * ResearchTopic.lootPool is not what the draw is reading."</i> {@code wiki/item-spawn-eggs.html}: <i>"a breed egg
 * (~7% of a dungeon chest ...)"</i>. The rate half of "feel right" is the owner's; this pen answers the half that can
 * be wrong invisibly - that each rate is the one configured.
 *
 * <p><b>The path, read in the 26.1.2 sources (2026-10-02).</b> A structure chest opening runs
 * {@code RandomizableContainer.unpackLootTable}: {@code LootParams.Builder(level).withParameter(ORIGIN,
 * Vec3.atCenterOf(pos))}, {@code create(LootContextParamSets.CHEST)}, then {@code lootTable.fill(container, params,
 * lootTableSeed)}. {@code fill} calls the private {@code LootTable.getRandomItems(LootContext)}, which rolls the raw
 * pools through {@code createStackSplitter} and then hands the list to NeoForge's
 * {@code CommonHooks.modifyLoot(getLootTableId(), list, context)} - and that (read in the NeoForge 26.1.2.100 sources
 * jar) sets the queried table id on the context and applies every loaded global loot modifier in priority order. The
 * public {@code getRandomItems(LootParams, long seed)} is that same private method with the seed applied the way
 * {@code fill} applies a chest's seed, so it is the chest path minus only the slot shuffling; GLMs run there. The
 * table comes from {@code server.reloadableRegistries().getLootTable(key)}, as the chest gets it (so the id the regex
 * reads is set). The pen enumerates <b>every</b> loot table in the live registry whose path starts {@code chests/},
 * in any namespace - which brings in this mod's own {@code horsegenetics:chests/cowboy_house}, the one non-vanilla
 * chest table a dev world has, so "the regex reaches modded chests" gets a reading too. The enumeration tests the
 * path with {@code startsWith}, not with the modifier's regex, so it does not share the code it checks.
 *
 * <p><b>Telling the modifier's items from the table's own.</b> Vanilla chests already hold saddles and iron, gold
 * and diamond horse armour, so counting tack in the output would count vanilla's too. Each roll is therefore made
 * twice with one seed: once through the GLM path above, once through {@code getRandomItemsRaw} with a
 * {@code LootContext} seeded the same way. The pools consume the seeded random identically, so the raw list is an
 * exact prefix of the full one and the modifiers' additions are whatever follows it. A roll whose prefix does not
 * match is counted as unpaired and left out of every rate; if more than 1% of rolls are unpaired the rate checks
 * answer INCONCLUSIVE (the method, not the mod).
 *
 * <p><b>The rates</b>, from {@code data/horsegenetics/loot_modifiers/*.json} (hard-coded here, as configured on
 * 2026-10-02; a change to a JSON must change {@link #SEEDS_P} and friends): seeds 0.2, tack 0.22 and papers 0.3 on
 * any chest table ({@code [^:]+:chests/.+}); breed eggs 0.07 on ten named vanilla chests only ({@link #EGG_TABLES}).
 * Each modifier draws one {@code nextFloat() < chance}, so the count of rolls it added to is binomial. 2,000 rolls per
 * table. <b>PASS</b> per modifier: the pooled rate over its tables is inside the 99.9% binomial band (z = 3.29), AND
 * no single table is outside a z = 4.5 band (per-table tests are ~200, so 4.5 keeps the family-wise false alarm near
 * 0.1%); eggs additionally appear in no table outside the ten. <b>FAIL</b>: outside a band - the table(s) named.
 *
 * <p><b>Tack weights</b> ({@code AddHorseTackModifier}): leather armour 10, saddle 8, iron 6, gold 4, diamond 1 (of
 * 29), plus a modded bucket of 5 when {@code ModdedArmour.armours()} is non-empty. PASS: each share of the pooled
 * modifier tack within its 99.9% band. <b>Fittings</b>: every added leather armour and saddle carries a
 * {@code tack_tint} whose seat and bridle are both dye colours and whose metal is one of iron, copper, bone, coal,
 * basalt or gold - FAIL on any gem (diamond, emerald, amethyst) or any other metal, or an undyed piece. <b>Papers</b>:
 * every research paper anywhere in any chest roll names a gene with a gene carrot, is a variant twice (X/X) or a
 * variant with the wild type in either order (X/n or n/X - the token keeps the locus's allele order, #212), and
 * its token is in {@code ResearchTopic.lootPool}
 * for that gene. FAIL on any other - a compound pair is printed.
 *
 * <p><b>Negative controls</b>, 2,000 rolls each with the parameters their sets require, read in
 * {@code LootContextParamSets}: {@code gameplay/fishing} (ORIGIN, TOOL = a fishing rod), the zombie's entity table
 * (THIS_ENTITY = a zombie made by {@code EntityType.create} and never added to the world, ORIGIN, DAMAGE_SOURCE =
 * magic) and {@code blocks/stone} (BLOCK_STATE, ORIGIN, TOOL = empty). PASS: no roll's full list is longer than its
 * raw one, and no stack from this mod or carrying a {@code tack_tint} appears in any of them.
 *
 * <p><b>Cost.</b> About 57 chest tables x 2,000 x 2 rolls; each server tick spends at most 12 ms on it and the yard
 * clock carries on next tick. The map functions in shipwreck and ruin chests search for structures, which could be
 * slow - in this dimension they are not: its flat generator has {@code structure_overrides: []}, so the generator's
 * structure state is empty and {@code findNearestMapStructure} returns nothing at once (read in
 * {@code FlatLevelSource.createState}). A table whose single roll still takes over 250 ms is stopped and named.
 * One tally line per table is logged.
 *
 * <h2>CARROT CROP (east)</h2>
 *
 * <p>{@code wiki/items.html}, "Golden carrot seeds (2026-09-18, NOT played)": <i>"It grows at all. Plant on
 * farmland, wait, or bone-meal it. A crop that sits at age 0 forever means the randomTicks property did not
 * take."</i> <i>"Breaking it early returns the seed, and a mature one gives one golden carrot and two seeds. Nothing
 * gold ever drops."</i> <i>"The seed will not plant on anything but farmland, and a golden carrot still will not plant
 * at all."</i>
 *
 * <p><b>(a) Growth.</b> Twelve crops at age 0, two blocks apart (so none has a crop beside or diagonal to it, which
 * would halve its speed in {@code CropBlock.getGrowthSpeed}), on moisture-7 farmland with one water source within 4
 * of every farmland block ({@code FarmlandBlock.isNearWater}: a 9x9x2 box). The cell has a stone lid, so each crop gets
 * a level-15 light block directly above it: {@code CropBlock.randomTick} grows only at raw brightness 9 or more, and
 * the crop survives only at 8. Growth comes from the world's real random tick (force-loaded chunks are block-ticking
 * - {@code ServerChunkCache.tickChunks} random-ticks every chunk {@code forEachBlockTickingChunk} visits). Vanilla's
 * arithmetic for this layout: speed 1 + 3 + 8 x 0.75 = 10, so each random tick advances an age with chance 1/3, and a
 * block is random-ticked about once a minute at {@code randomTickSpeed} 3 - mature in roughly 20-25 minutes. Ages are
 * read every 5 minutes. <b>PASS:</b> at least one crop at max age within 90 minutes. <b>FAIL:</b> all twelve still
 * at age 0 at 60 minutes, or none mature at 90. <b>INCONCLUSIVE:</b> {@code randomTickSpeed} is 0, every crop reads
 * brightness under 9, or the crops are gone.
 *
 * <p><b>(b) Drops.</b> {@code Block.getDrops(state, level, pos, null)} - the static the game uses, which builds the
 * BLOCK params with an empty tool and runs the crop's own loot table through the GLM path - 200 times at age 0 and
 * 200 at max age. The table ({@code loot_table/blocks/golden_carrot_crop.json}): one seed always, plus one golden
 * carrot and one seed at age 7. <b>PASS:</b> every age-0 roll is exactly one seed, every mature roll is exactly two
 * seeds and one golden carrot, and nothing else ever drops. <b>FAIL:</b> anything else, gold above all.
 *
 * <p><b>(c) Planting.</b> The clockwork hands ({@link DebugYardClockwork.Hands}) right-click the top face of dirt,
 * grass and farmland with one seed each, and farmland with a golden carrot, through
 * {@code ServerPlayerGameMode.useItemOn} - the server's own click path (the RightClickBlock event, then the block, then
 * the item). Each target has a light block above it, so a refusal cannot be the light. <b>PASS:</b> a crop stands on
 * the farmland (read at once and again 40 ticks later) and on nothing else, and the golden carrot placed nothing.
 * <b>FAIL:</b> a crop on dirt or grass, none on farmland, or anything placed by the golden carrot.
 *
 * <p>UNVERIFIED (no other call in this repo; signatures read in the 26.1.2 sources jar):
 * {@code LootTable.getRandomItems(LootParams, long)}, {@code getRandomItemsRaw(LootContext, Consumer)},
 * {@code LootTable.createStackSplitter}, {@code LootContext.Builder(params).withOptionalRandomSeed(seed)
 * .create(Optional.empty())}, {@code LootParams.Builder}, {@code reloadableRegistries().lookup().lookupOrThrow(
 * Registries.LOOT_TABLE).listElementIds()}, {@code EntityType.getDefaultLootTable()},
 * {@code BlockBehaviour.getLootTable()}, {@code Block.getDrops(state, level, pos, null)},
 * {@code level.getRawBrightness(pos, 0)}, {@code FarmlandBlock.MOISTURE},
 * {@code ServerPlayerGameMode.useItemOn(player, level, stack, hand, hit)} and {@code new BlockHitResult(...)}.
 */
final class DebugYardLoot {

    private DebugYardLoot() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    /** Every check answered this run, so none answers twice. */
    private static final Set<String> ANSWERED = new HashSet<>();

    // ------------------------------------------------------------------
    // Check names
    // ------------------------------------------------------------------

    private static final String LOOT = "CHEST LOOT";
    static final String SEEDS = LOOT + " - golden carrot seeds are added to every chest table at the configured 20%";
    static final String TACK = LOOT + " - horse tack is added to every chest table at the configured 22%";
    static final String PAPERS = LOOT + " - research papers are added to every chest table at the configured 30%";
    static final String EGGS = LOOT + " - breed eggs are added to the ten named chests at 7% and to no other chest";
    static final String WEIGHTS = LOOT + " - found tack comes in its weights (leather 10, saddle 8, iron 6, gold 4,"
            + " diamond 1 of 29)";
    static final String FITTINGS = LOOT + " - found leather armour and saddles are dyed, with cheap fittings, never a gem";
    static final String PAIRS = LOOT + " - every research paper from a chest is a carrier (X/n) or true-breeding (X/X)"
            + " pair from ResearchTopic.lootPool";
    static final String CONTROLS = LOOT + " - fishing, a zombie and a stone block get nothing from this mod's modifiers";

    private static final String CROP = "CARROT CROP";
    static final String GROWS = CROP + " - golden carrots grow to maturity under the world's random tick";
    static final String DROPS = CROP + " - age 0 drops one seed; mature drops one golden carrot and two seeds; nothing"
            + " gold";
    static final String PLANTS = CROP + " - a seed plants only on farmland, and a golden carrot plants nowhere";

    private static final List<String> LOOT_CHECKS =
            List.of(SEEDS, TACK, PAPERS, EGGS, WEIGHTS, FITTINGS, PAIRS, CONTROLS);

    // ------------------------------------------------------------------
    // CHEST LOOT parameters
    // ------------------------------------------------------------------

    /** The configured chances, from data/horsegenetics/loot_modifiers/*.json as of 2026-10-02. */
    private static final double SEEDS_P = 0.2;
    private static final double TACK_P = 0.22;
    private static final double PAPERS_P = 0.3;
    private static final double EGGS_P = 0.07;

    /** add_breed_spawn_egg.json's any_of list. */
    private static final Set<String> EGG_TABLES = Set.of(
            "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/stronghold_crossing",
            "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple",
            "minecraft:chests/woodland_mansion", "minecraft:chests/pillager_outpost",
            "minecraft:chests/underwater_ruin_big", "minecraft:chests/bastion_other");

    private static final int ROLLS = 2_000;
    private static final long START = 100L;
    private static final long LOOT_DEADLINE = 24_000L;
    private static final long BUDGET_NS = 12_000_000L;
    private static final long SLOW_ROLL_NS = 250_000_000L;
    /** Two-sided 99.9% normal quantile. */
    private static final double Z_POOLED = 3.2905;
    /** Per-table band: two-sided ~7e-6 each, so ~200 of them stay near a 0.1% family-wise false alarm. */
    private static final double Z_TABLE = 4.5;

    /** AddHorseTackModifier's weights. */
    private static final String[] TACK_NAMES = {"leather", "saddle", "iron", "gold", "diamond", "modded"};
    private static final int[] TACK_WEIGHTS = {10, 8, 6, 4, 1, 5};

    /** The fittings a found piece may have (AddHorseTackModifier.FOUND_FITTINGS, written out independently). */
    private static final Map<Integer, String> CHEAP = Map.of(
            SaddleTint.IRON, "iron", SaddleTint.COPPER, "copper", SaddleTint.BONE, "bone",
            SaddleTint.COAL, "coal", SaddleTint.BASALT, "basalt", SaddleTint.GOLD, "gold");
    private static final Map<Integer, String> GEMS = Map.of(
            SaddleTint.DIAMOND, "DIAMOND", SaddleTint.EMERALD, "EMERALD", SaddleTint.AMETHYST, "AMETHYST");

    private enum Kind { CHEST, CONTROL }

    /** One table being rolled, and what it gave. */
    private static final class Target {
        final String id;
        final LootTable table;
        final LootParams params;
        final Kind kind;
        int n;
        int unpaired;
        int seeds;
        int tack;
        int papers;
        int eggs;
        int other;
        int modItems;
        int tinted;
        int longer;
        long maxNs;
        boolean slow;
        final List<String> otherSeen = new ArrayList<>();

        Target(String id, LootTable table, LootParams params, Kind kind) {
            this.id = id;
            this.table = table;
            this.params = params;
            this.kind = kind;
        }
    }

    private static final class Sampler {
        final int run;
        final long started;
        final List<Target> targets = new ArrayList<>();
        final SplittableRandom rng;
        int at;
        final int[] tackBy = new int[TACK_NAMES.length];
        int tackPieces;
        int dyedOk;
        int undyed;
        int badDye;
        int gemFittings;
        int otherFittings;
        final Map<String, Integer> fittingsSeen = new TreeMap<>();
        final int[] seedCounts = new int[5];
        int papersSeen;
        int carriers;
        int trueBreeding;
        final Set<String> paperGenes = new HashSet<>();
        final List<String> badPapers = new ArrayList<>();
        int badPaperCount;
        boolean modded;

        Sampler(int run, long started) {
            this.run = run;
            this.started = started;
            this.rng = new SplittableRandom(started * 31L + 17L);
        }
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        ANSWERED.clear();
        try {
            for (String c : LOOT_CHECKS) {
                DebugYardClockwork.expect(c);
            }
            for (String c : List.of(GROWS, DROPS, PLANTS)) {
                DebugYardClockwork.expect(c);
            }
            chestLoot(level, gy, x0, z0, myRun);
            carrotCrop(level, gy, x0 + 25, z0, myRun);
            ActionTrace.log("test yard", "row AY built (west: CHEST LOOT; east: CARROT CROP) at game tick "
                    + level.getGameTime());
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AY (CHEST LOOT / CARROT CROP) failed to build", e);
        }
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

    /** A clock step that stops itself for a stale yard, and answers its checks INCONCLUSIVE if it throws. */
    private static void step(ServerLevel level, long ticks, int myRun, List<String> checks, Runnable body) {
        DebugYardHerd.after(level, ticks, () -> {
            if (myRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: row AY step failed", e);
                for (String c : checks) {
                    unsure(c, "the step threw " + e);
                }
            }
        });
    }

    // ==================================================================
    // WEST - CHEST LOOT
    // ==================================================================

    private static void chestLoot(ServerLevel level, int gy, int x0, int z0, int myRun) {
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("CHEST LOOT", "2000 rolls a chest:", "seeds, tack, papers,", "eggs at their rates?"));
        BlockPos origin = new BlockPos(x0 + 10, gy + 1, z0 + 6);
        step(level, START, myRun, LOOT_CHECKS, () -> {
            Sampler s = new Sampler(myRun, level.getGameTime());
            s.modded = !ModdedArmour.armours().isEmpty();
            setUp(level, origin, s);
            ActionTrace.log("test yard", "CHEST LOOT rolling " + s.targets.size() + " tables x " + ROLLS
                    + " (x2: raw and through the global loot modifiers); modded armour bucket "
                    + (s.modded ? "IN (" + ModdedArmour.armours().size() + " armours)" : "absent"));
            batch(level, s);
        });
    }

    private static void setUp(ServerLevel level, BlockPos origin, Sampler s) {
        Vec3 at = Vec3.atCenterOf(origin);
        LootParams chest = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, at)
                .create(LootContextParamSets.CHEST);
        // UNVERIFIED: listElementIds over the reloadable loot table registry (HolderLookup, read in the sources).
        List<ResourceKey<LootTable>> keys = new ArrayList<>(level.getServer().reloadableRegistries().lookup()
                .lookupOrThrow(Registries.LOOT_TABLE).listElementIds().toList());
        keys.sort((a, b) -> a.identifier().toString().compareTo(b.identifier().toString()));
        for (ResourceKey<LootTable> key : keys) {
            if (!key.identifier().getPath().startsWith("chests/")) {
                continue;
            }
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            if (table != LootTable.EMPTY) {
                s.targets.add(new Target(key.identifier().toString(), table, chest, Kind.CHEST));
            }
        }

        // Fishing: ORIGIN and TOOL required.
        LootParams fishing = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        control(level, s, BuiltInLootTables.FISHING, fishing);

        // A zombie: THIS_ENTITY, ORIGIN, DAMAGE_SOURCE required. Made, never added to the world.
        Entity zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        Optional<ResourceKey<LootTable>> zombieTable = EntityType.ZOMBIE.getDefaultLootTable();
        if (zombie != null && zombieTable.isPresent()) {
            zombie.setPos(at.x, at.y, at.z);
            LootParams mob = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, zombie)
                    .withParameter(LootContextParams.ORIGIN, at)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().magic())
                    .create(LootContextParamSets.ENTITY);
            control(level, s, zombieTable.get(), mob);
        }

        // Stone: BLOCK_STATE, ORIGIN, TOOL required.
        Optional<ResourceKey<LootTable>> stoneTable = Blocks.STONE.getLootTable();
        if (stoneTable.isPresent()) {
            LootParams block = new LootParams.Builder(level)
                    .withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
                    .withParameter(LootContextParams.ORIGIN, at)
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .create(LootContextParamSets.BLOCK);
            control(level, s, stoneTable.get(), block);
        }
    }

    private static void control(ServerLevel level, Sampler s, ResourceKey<LootTable> key, LootParams params) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table != LootTable.EMPTY) {
            s.targets.add(new Target(key.identifier().toString(), table, params, Kind.CONTROL));
        }
    }

    /** As many rolls as fit in this tick's budget, then the next tick. */
    private static void batch(ServerLevel level, Sampler s) {
        if (s.run != run) {
            return;
        }
        if (level.getGameTime() - s.started > LOOT_DEADLINE) {
            for (String c : LOOT_CHECKS) {
                unsure(c, "the rolls did not finish within " + LOOT_DEADLINE / 1200 + " minutes: stopped at table "
                        + s.at + " of " + s.targets.size());
            }
            return;
        }
        long t0 = System.nanoTime();
        try {
            while (s.at < s.targets.size() && System.nanoTime() - t0 < BUDGET_NS) {
                Target t = s.targets.get(s.at);
                if (t.n >= ROLLS || t.slow) {
                    tally(t);
                    s.at++;
                    continue;
                }
                roll(level, s, t);
            }
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: CHEST LOOT roll failed", e);
            for (String c : LOOT_CHECKS) {
                unsure(c, "a roll of " + s.targets.get(Math.min(s.at, s.targets.size() - 1)).id + " threw " + e);
            }
            return;
        }
        if (s.at >= s.targets.size()) {
            judge(s);
        } else {
            DebugYardHerd.after(level, 1L, () -> batch(level, s));
        }
    }

    @SuppressWarnings("deprecation") // getRandomItemsRaw: deliberately the un-modified roll, as the comparison
    private static void roll(ServerLevel level, Sampler s, Target t) {
        long seed = s.rng.nextLong();
        if (seed == 0L) {
            seed = 1L; // 0 means "use the table's random sequence" to withOptionalRandomSeed
        }
        long r0 = System.nanoTime();
        // UNVERIFIED: the GLM path. Same private getRandomItems(LootContext) that LootTable.fill runs for a chest.
        ObjectArrayList<ItemStack> full = t.table.getRandomItems(t.params, seed);
        List<ItemStack> raw = new ArrayList<>();
        // UNVERIFIED: the raw path, seeded the same way, through the same stack splitter.
        t.table.getRandomItemsRaw(new LootContext.Builder(t.params).withOptionalRandomSeed(seed)
                .create(Optional.empty()), LootTable.createStackSplitter(level, raw::add));
        long took = System.nanoTime() - r0;
        t.maxNs = Math.max(t.maxNs, took);
        t.n++;
        if (took > SLOW_ROLL_NS) {
            t.slow = true;
        }

        for (ItemStack stack : full) {
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(HorseGenetics.MOD_ID)) {
                t.modItems++;
            }
            if (stack.get(ModDataComponents.TACK_TINT.get()) != null) {
                t.tinted++;
            }
            if (t.kind == Kind.CHEST && stack.is(ModItems.RESEARCH_PAPER.get())) {
                paper(s, t, stack);
            }
        }
        if (full.size() > raw.size()) {
            t.longer++;
        }

        boolean paired = full.size() >= raw.size();
        for (int i = 0; paired && i < raw.size(); i++) {
            ItemStack a = raw.get(i);
            ItemStack b = full.get(i);
            paired = a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
        }
        if (!paired) {
            t.unpaired++;
            return;
        }
        boolean seedsHere = false;
        boolean tackHere = false;
        boolean paperHere = false;
        boolean eggHere = false;
        for (int i = raw.size(); i < full.size(); i++) {
            ItemStack added = full.get(i);
            Item item = added.getItem();
            int tackKind = tackKind(item);
            if (item == ModItems.GOLDEN_CARROT_SEEDS.get()) {
                seedsHere = true;
                s.seedCounts[Math.min(added.getCount(), 4)]++;
            } else if (tackKind >= 0) {
                tackHere = true;
                if (t.kind == Kind.CHEST) {
                    s.tackBy[tackKind]++;
                    s.tackPieces++;
                    if (tackKind <= 1) {
                        fitting(s, added);
                    }
                }
            } else if (item == ModItems.RESEARCH_PAPER.get()) {
                paperHere = true;
            } else if (item == ModItems.BREED_SPAWN_EGG.get()) {
                eggHere = true;
            } else {
                t.other++;
                if (t.otherSeen.size() < 3) {
                    t.otherSeen.add(BuiltInRegistries.ITEM.getKey(item).toString());
                }
            }
        }
        t.seeds += seedsHere ? 1 : 0;
        t.tack += tackHere ? 1 : 0;
        t.papers += paperHere ? 1 : 0;
        t.eggs += eggHere ? 1 : 0;
    }

    /** 0 leather, 1 saddle, 2 iron, 3 gold, 4 diamond, 5 modded; -1 not tack. */
    private static int tackKind(Item item) {
        if (item == Items.LEATHER_HORSE_ARMOR) {
            return 0;
        }
        if (item == Items.SADDLE) {
            return 1;
        }
        if (item == Items.IRON_HORSE_ARMOR) {
            return 2;
        }
        if (item == Items.GOLDEN_HORSE_ARMOR) {
            return 3;
        }
        if (item == Items.DIAMOND_HORSE_ARMOR) {
            return 4;
        }
        for (ModdedArmour.Armour a : ModdedArmour.armours()) {
            if (a.item().get() == item) {
                return 5;
            }
        }
        return -1;
    }

    private static Set<Integer> dyes() {
        Set<Integer> out = new HashSet<>();
        for (DyeColor d : DyeColor.values()) {
            out.add(d.getTextureDiffuseColor() & 0xFFFFFF);
        }
        return out;
    }

    private static void fitting(Sampler s, ItemStack piece) {
        SaddleTint tint = piece.get(ModDataComponents.TACK_TINT.get());
        if (tint == null) {
            s.undyed++;
            return;
        }
        Set<Integer> dyes = dyes();
        if (!dyes.contains(tint.seat()) || !dyes.contains(tint.bridle())) {
            s.badDye++;
        }
        String name;
        if (GEMS.containsKey(tint.metal())) {
            s.gemFittings++;
            name = GEMS.get(tint.metal());
        } else if (CHEAP.containsKey(tint.metal())) {
            s.dyedOk++;
            name = CHEAP.get(tint.metal());
        } else {
            s.otherFittings++;
            name = String.format(Locale.ROOT, "0x%06X", tint.metal());
        }
        s.fittingsSeen.merge(name, 1, Integer::sum);
    }

    private static void paper(Sampler s, Target t, ItemStack stack) {
        s.papersSeen++;
        ResearchTopic topic = ResearchPaperItem.topicOf(stack);
        String why = null;
        Gene gene = topic == null ? null : topic.gene();
        if (topic == null) {
            why = "no topic";
        } else if (gene == null) {
            why = "unknown gene " + topic.geneKey();
        } else {
            String wild = gene.defaultAllele().token();
            boolean known = false;
            for (ResearchTopic p : ResearchTopic.lootPool(gene)) {
                known |= p.token().equals(topic.token());
            }
            if (!gene.hasGeneCarrot()) {
                why = "gene has no gene carrot";
            } else if (topic.alleleB().equals(topic.alleleA())) {
                if (topic.alleleA().equals(wild)) {
                    why = "wild/wild pair";
                } else {
                    s.trueBreeding++;
                }
            } else if (topic.alleleA().equals(wild) || topic.alleleB().equals(wild)) {
                // Either order: ResearchTopic.of writes the pair as AllelePair holds it, in the locus's own
                // allele order, so a carrier of a variant listed after the wild type reads n/X (#212).
                s.carriers++;
            } else {
                why = "COMPOUND pair";
            }
            if (why == null && !known) {
                why = "not in ResearchTopic.lootPool";
            }
            s.paperGenes.add(gene.key());
        }
        if (why != null) {
            s.badPaperCount++;
            if (s.badPapers.size() < 6) {
                s.badPapers.add((topic == null ? "?" : topic.token()) + " (" + why + ", " + t.id + ")");
            }
        }
    }

    private static String pct(int x, int n) {
        return n == 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", 100.0 * x / n);
    }

    private static void tally(Target t) {
        int n = t.n - t.unpaired;
        StringBuilder sb = new StringBuilder("CHEST LOOT tally ").append(t.kind == Kind.CONTROL ? "[control] " : "")
                .append(t.id).append(" n=").append(t.n);
        if (t.kind == Kind.CHEST) {
            sb.append(" | seeds ").append(t.seeds).append(" (").append(pct(t.seeds, n)).append(')')
                    .append(" tack ").append(t.tack).append(" (").append(pct(t.tack, n)).append(')')
                    .append(" papers ").append(t.papers).append(" (").append(pct(t.papers, n)).append(')')
                    .append(" eggs ").append(t.eggs).append(" (").append(pct(t.eggs, n)).append(')');
        } else {
            sb.append(" | longer than raw ").append(t.longer).append(", mod items ").append(t.modItems)
                    .append(", tack_tint stacks ").append(t.tinted);
        }
        sb.append(" | unpaired ").append(t.unpaired);
        if (t.other > 0) {
            sb.append(" | other added ").append(t.other).append(' ').append(t.otherSeen);
        }
        sb.append(String.format(Locale.ROOT, " | slowest roll %.1f ms", t.maxNs / 1e6));
        if (t.slow) {
            sb.append(" | STOPPED: a roll over ").append(SLOW_ROLL_NS / 1_000_000).append(" ms");
        }
        ActionTrace.log("test yard", sb.toString());
    }

    private static boolean inBand(int x, int n, double p, double z) {
        if (n == 0) {
            return false;
        }
        double sd = Math.sqrt(p * (1 - p) / n);
        return Math.abs((double) x / n - p) <= z * sd;
    }

    private static String band(int x, int n, double p, double z) {
        double sd = n == 0 ? 0 : Math.sqrt(p * (1 - p) / n);
        return String.format(Locale.ROOT, "%d/%d = %.4f (want %.3f, band %.4f..%.4f)", x, n,
                n == 0 ? Double.NaN : (double) x / n, p, p - z * sd, p + z * sd);
    }

    private interface Count {
        int of(Target t);
    }

    private static void judge(Sampler s) {
        List<Target> chests = new ArrayList<>();
        List<Target> controls = new ArrayList<>();
        int rolls = 0;
        int unpaired = 0;
        List<String> slow = new ArrayList<>();
        for (Target t : s.targets) {
            (t.kind == Kind.CHEST ? chests : controls).add(t);
            rolls += t.n;
            unpaired += t.unpaired;
            if (t.slow) {
                slow.add(t.id);
            }
        }
        String method = chests.size() + " chest tables, " + rolls + " rolls, " + unpaired + " unpaired"
                + (slow.isEmpty() ? "" : ", stopped as slow: " + slow);

        if (chests.isEmpty()) {
            for (String c : LOOT_CHECKS) {
                unsure(c, "no chests/ loot table found in the registry");
            }
            return;
        }
        boolean methodOk = unpaired * 100 <= rolls;
        if (!methodOk) {
            for (String c : List.of(SEEDS, TACK, PAPERS, EGGS, WEIGHTS)) {
                unsure(c, "the raw roll was not a prefix of the GLM roll in more than 1% of rolls - the pairing"
                        + " method failed, not the mod: " + method);
            }
        } else {
            rate(s, chests, SEEDS, SEEDS_P, t -> t.seeds, t -> true, method
                    + String.format(Locale.ROOT, "; seed counts 1/2/3 = %d/%d/%d (want 1-3 evenly), other %d",
                    s.seedCounts[1], s.seedCounts[2], s.seedCounts[3], s.seedCounts[0] + s.seedCounts[4]));
            rate(s, chests, TACK, TACK_P, t -> t.tack, t -> true, method);
            rate(s, chests, PAPERS, PAPERS_P, t -> t.papers, t -> true, method);
            rate(s, chests, EGGS, EGGS_P, t -> t.eggs, t -> EGG_TABLES.contains(t.id), method);
            weights(s);
        }

        // Fittings.
        int pieces = s.tackBy[0] + s.tackBy[1];
        String fitDetail = String.format(Locale.ROOT,
                "%d leather armours and saddles added: %d dyed with a cheap fitting, %d undyed, %d with a non-dye"
                        + " seat or bridle, %d GEM fittings, %d other fittings; fittings seen %s",
                pieces, s.dyedOk, s.undyed, s.badDye, s.gemFittings, s.otherFittings, s.fittingsSeen);
        if (pieces == 0) {
            unsure(FITTINGS, "no dyeable tack was added to measure; " + fitDetail);
        } else {
            answer(FITTINGS, s.undyed == 0 && s.badDye == 0 && s.gemFittings == 0 && s.otherFittings == 0, fitDetail);
        }

        // Papers.
        String paperDetail = s.papersSeen + " papers in chest rolls: " + s.carriers + " carrier (X/n), "
                + s.trueBreeding + " true-breeding (X/X), " + s.badPaperCount + " bad"
                + (s.badPapers.isEmpty() ? "" : " e.g. " + s.badPapers) + "; " + s.paperGenes.size() + " genes seen";
        if (s.papersSeen == 0) {
            unsure(PAIRS, "no paper turned up to read; " + paperDetail);
        } else {
            answer(PAIRS, s.badPaperCount == 0, paperDetail);
        }

        // Controls.
        if (controls.size() < 3) {
            unsure(CONTROLS, "only " + controls.size() + " of the three control tables could be set up (fishing,"
                    + " zombie, stone)");
        } else {
            StringBuilder d = new StringBuilder();
            boolean ok = true;
            for (Target t : controls) {
                boolean clean = t.longer == 0 && t.modItems == 0 && t.tinted == 0 && t.n == ROLLS;
                ok &= clean;
                d.append(d.length() == 0 ? "" : "; ").append(t.id).append(": ").append(t.n).append(" rolls, ")
                        .append(t.longer).append(" longer than raw, ").append(t.modItems).append(" mod items, ")
                        .append(t.tinted).append(" tinted").append(clean ? "" : " DIRTY");
            }
            answer(CONTROLS, ok, d.toString());
        }
    }

    private interface Applies {
        boolean to(Target t);
    }

    private static void rate(Sampler s, List<Target> chests, String check, double p, Count count, Applies applies,
                             String method) {
        int x = 0;
        int n = 0;
        int strays = 0;
        List<String> outliers = new ArrayList<>();
        List<String> strayTables = new ArrayList<>();
        Target lowest = null;
        Target highest = null;
        for (Target t : chests) {
            int tn = t.n - t.unpaired;
            int tx = count.of(t);
            if (!applies.to(t)) {
                if (tx > 0) {
                    strays += tx;
                    strayTables.add(t.id + " " + tx);
                }
                continue;
            }
            x += tx;
            n += tn;
            if (!inBand(tx, tn, p, Z_TABLE)) {
                outliers.add(t.id + " " + tx + "/" + tn);
            }
            if (tn > 0) {
                double r = (double) tx / tn;
                if (lowest == null || r < (double) count.of(lowest) / Math.max(1, lowest.n - lowest.unpaired)) {
                    lowest = t;
                }
                if (highest == null || r > (double) count.of(highest) / Math.max(1, highest.n - highest.unpaired)) {
                    highest = t;
                }
            }
        }
        boolean pooledOk = inBand(x, n, p, Z_POOLED);
        String detail = "pooled " + band(x, n, p, Z_POOLED)
                + (lowest == null ? "" : "; lowest table " + lowest.id + " " + pct(count.of(lowest),
                lowest.n - lowest.unpaired) + ", highest " + highest.id + " " + pct(count.of(highest),
                highest.n - highest.unpaired))
                + "; tables outside the z=4.5 band: " + (outliers.isEmpty() ? "none" : outliers)
                + (check.equals(EGGS) ? "; eggs in tables outside the ten: " + (strayTables.isEmpty() ? "none"
                : strayTables) : "")
                + " | " + method;
        if (n == 0) {
            unsure(check, "no table this modifier targets was rolled; " + detail);
        } else {
            answer(check, pooledOk && outliers.isEmpty() && strays == 0, detail);
        }
    }

    private static void weights(Sampler s) {
        int total = 0;
        for (int i = 0; i < TACK_WEIGHTS.length; i++) {
            if (i < 5 || s.modded) {
                total += TACK_WEIGHTS[i];
            }
        }
        StringBuilder d = new StringBuilder(s.tackPieces + " added tack pieces: ");
        boolean ok = true;
        for (int i = 0; i < TACK_NAMES.length; i++) {
            if (i == 5 && !s.modded) {
                if (s.tackBy[5] > 0) {
                    ok = false;
                    d.append("; modded ").append(s.tackBy[5]).append(" WITH NO MODDED BUCKET");
                }
                continue;
            }
            double p = (double) TACK_WEIGHTS[i] / total;
            boolean in = inBand(s.tackBy[i], s.tackPieces, p, Z_POOLED);
            ok &= in;
            d.append(i == 0 ? "" : "; ").append(TACK_NAMES[i]).append(' ')
                    .append(band(s.tackBy[i], s.tackPieces, p, Z_POOLED)).append(in ? "" : " OUT");
        }
        d.append(" | weights of ").append(total);
        if (s.tackPieces < 100) {
            unsure(WEIGHTS, "too little tack to weigh; " + d);
        } else {
            answer(WEIGHTS, ok, d.toString());
        }
    }

    // ==================================================================
    // EAST - CARROT CROP
    // ==================================================================

    private static final long READ_EVERY = 6_000L;
    private static final int FAIL_IF_FLAT_AT = 12;   // readings: 60 minutes
    private static final int GIVE_UP_AT = 18;        // readings: 90 minutes
    private static final int DROP_ROLLS = 200;

    private static void carrotCrop(ServerLevel level, int gy, int ex0, int z0, int myRun) {
        DebugPenManager.placeSign(level, new BlockPos(ex0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("CARROT CROP", "does it grow? what", "does it drop? does", "it plant on dirt?"));
        GoldenCarrotCropBlock crop = ModBlocks.GOLDEN_CARROT_CROP.get();
        BlockState lamp = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        // UNVERIFIED: FarmlandBlock.MOISTURE (public static, read in the sources).
        BlockState wet = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);

        // (a) growth cell: walls ex0..ex0+10 x z0+1..z0+9, floor ex0+1..ex0+9 x z0+2..z0+8.
        DebugYardClockwork.cell(level, gy, ex0, ex0 + 10, z0 + 1, z0 + 9);
        for (int x = ex0 + 1; x <= ex0 + 9; x++) {
            for (int z = z0 + 2; z <= z0 + 8; z++) {
                level.setBlock(new BlockPos(x, gy, z), wet, 3);
            }
        }
        level.setBlock(new BlockPos(ex0 + 5, gy, z0 + 5), Blocks.WATER.defaultBlockState(), 3);
        List<BlockPos> crops = new ArrayList<>();
        for (int z = z0 + 3; z <= z0 + 7; z += 2) {
            for (int x = ex0 + 2; x <= ex0 + 8; x += 2) {
                BlockPos p = new BlockPos(x, gy + 1, z);
                level.setBlock(p, crop.getStateForAge(0), 3);
                level.setBlock(p.above(), lamp, 3);
                crops.add(p);
            }
        }

        // (c) planting cell: walls ex0+13..ex0+19 x z0+1..z0+5; targets on row z0+3.
        DebugYardClockwork.cell(level, gy, ex0 + 13, ex0 + 19, z0 + 1, z0 + 5);
        BlockPos dirt = new BlockPos(ex0 + 14, gy, z0 + 3);
        BlockPos grass = new BlockPos(ex0 + 15, gy, z0 + 3);
        BlockPos farm = new BlockPos(ex0 + 16, gy, z0 + 3);
        BlockPos farmCarrot = new BlockPos(ex0 + 18, gy, z0 + 3);
        level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(grass, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(farm, wet, 3);
        level.setBlock(farmCarrot, wet, 3);
        // Water within 4 of both farmland blocks, in the cell's floor, away from the targets.
        level.setBlock(new BlockPos(ex0 + 17, gy, z0 + 2), Blocks.WATER.defaultBlockState(), 3);
        for (BlockPos t : List.of(dirt, grass, farm, farmCarrot)) {
            level.setBlock(t.above(2), lamp, 3);
        }

        // (a) readings.
        step(level, START, myRun, List.of(GROWS), () -> growth(level, crops, crop, 0, myRun));
        // (b) drops, at a spot nothing stands on (only ORIGIN is read from it).
        BlockPos dropAt = new BlockPos(ex0 + 16, gy + 1, z0 + 8);
        step(level, START, myRun, List.of(DROPS), () -> drops(level, crop, dropAt));
        // (c) planting.
        step(level, START, myRun, List.of(PLANTS), () -> plant(level, crop, dirt, grass, farm, farmCarrot, myRun));
    }

    // ------------------------------------------------------------------
    // (a) growth
    // ------------------------------------------------------------------

    private static void growth(ServerLevel level, List<BlockPos> crops, GoldenCarrotCropBlock crop, int reading,
                               int myRun) {
        int speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        int[] ages = new int[crops.size()];
        int present = 0;
        int mature = 0;
        int flat = 0;
        int minLight = 99;
        StringBuilder agesText = new StringBuilder();
        for (int i = 0; i < crops.size(); i++) {
            BlockState st = level.getBlockState(crops.get(i));
            if (st.is(crop)) {
                present++;
                ages[i] = crop.getAge(st);
                mature += crop.isMaxAge(st) ? 1 : 0;
                flat += ages[i] == 0 ? 1 : 0;
                agesText.append(ages[i]);
            } else {
                ages[i] = -1;
                agesText.append('x');
            }
            // UNVERIFIED: getRawBrightness(pos, 0), the call CropBlock.randomTick makes (read in the sources).
            minLight = Math.min(minLight, level.getRawBrightness(crops.get(i), 0));
        }
        boolean ticking = crop.isRandomlyTicking(crop.getStateForAge(0));
        String detail = String.format(Locale.ROOT,
                "at %d min: ages [%s] (x = gone), %d of %d at max age %d, %d still at 0; min brightness %d (grows at"
                        + " 9+); randomTickSpeed %d; isRandomlyTicking(age 0) %s",
                reading * READ_EVERY / 1200, agesText, mature, crops.size(), crop.getMaxAge(), flat, minLight, speed,
                ticking);
        ActionTrace.log("test yard", "CARROT CROP reading " + detail);

        if (present == 0) {
            unsure(GROWS, "every crop is gone; " + detail);
            return;
        }
        if (mature > 0) {
            answer(GROWS, true, detail);
            return;
        }
        if (speed <= 0) {
            unsure(GROWS, "randomTickSpeed is " + speed + " in this world, so nothing random-ticks; " + detail);
            return;
        }
        if (minLight < 9 && reading > 0) {
            // A dark crop does not grow by vanilla's rule; that is the light, not the crop.
            boolean anyLit = false;
            for (BlockPos p : crops) {
                anyLit |= level.getRawBrightness(p, 0) >= 9;
            }
            if (!anyLit) {
                unsure(GROWS, "no crop is lit to 9, so vanilla's rule forbids growth; " + detail);
                return;
            }
        }
        if (reading >= FAIL_IF_FLAT_AT && flat == present) {
            answer(GROWS, false, "all still at age 0 at 60 minutes - the randomTicks property did not take; " + detail);
            return;
        }
        if (reading >= GIVE_UP_AT) {
            answer(GROWS, false, "none mature at 90 minutes, against a vanilla expectation of ~25; " + detail);
            return;
        }
        int next = reading + 1;
        step(level, READ_EVERY, myRun, List.of(GROWS), () -> growth(level, crops, crop, next, myRun));
    }

    // ------------------------------------------------------------------
    // (b) drops
    // ------------------------------------------------------------------

    private static void drops(ServerLevel level, GoldenCarrotCropBlock crop, BlockPos at) {
        Item seed = ModItems.GOLDEN_CARROT_SEEDS.get();
        int youngOk = 0;
        int matureOk = 0;
        Map<String, Integer> youngSeen = new TreeMap<>();
        Map<String, Integer> matureSeen = new TreeMap<>();
        for (int age : new int[] {0, crop.getMaxAge()}) {
            BlockState state = crop.getStateForAge(age);
            for (int i = 0; i < DROP_ROLLS; i++) {
                // UNVERIFIED: Block.getDrops(state, level, pos, null) - the static the game's own breaking uses.
                List<ItemStack> out = Block.getDrops(state, level, at, null);
                int seeds = 0;
                int carrots = 0;
                int other = 0;
                for (ItemStack st : out) {
                    String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                    (age == 0 ? youngSeen : matureSeen).merge(id, st.getCount(), Integer::sum);
                    if (st.is(seed)) {
                        seeds += st.getCount();
                    } else if (st.is(Items.GOLDEN_CARROT)) {
                        carrots += st.getCount();
                    } else {
                        other += st.getCount();
                    }
                }
                if (age == 0 && seeds == 1 && carrots == 0 && other == 0) {
                    youngOk++;
                } else if (age != 0 && seeds == 2 && carrots == 1 && other == 0) {
                    matureOk++;
                }
            }
        }
        Set<String> gold = new HashSet<>();
        for (Map<String, Integer> m : List.of(youngSeen, matureSeen)) {
            for (String id : m.keySet()) {
                // The seed is named for the carrot, so its id holds "gold" too (#212).
                if (id.contains("gold") && !id.equals("minecraft:golden_carrot")
                        && !id.equals(BuiltInRegistries.ITEM.getKey(seed).toString())) {
                    gold.add(id);
                }
            }
        }
        String detail = String.format(Locale.ROOT,
                "age 0: %d/%d rolls exactly one seed, totals %s; age %d: %d/%d rolls exactly two seeds + one golden"
                        + " carrot, totals %s; gold other than golden carrots: %s",
                youngOk, DROP_ROLLS, youngSeen, crop.getMaxAge(), matureOk, DROP_ROLLS, matureSeen,
                gold.isEmpty() ? "none" : gold);
        answer(DROPS, youngOk == DROP_ROLLS && matureOk == DROP_ROLLS && gold.isEmpty(), detail);
    }

    // ------------------------------------------------------------------
    // (c) planting
    // ------------------------------------------------------------------

    private static void plant(ServerLevel level, GoldenCarrotCropBlock crop, BlockPos dirt, BlockPos grass,
                              BlockPos farm, BlockPos farmCarrot, int myRun) {
        Map<String, BlockPos> targets = new LinkedHashMap<>();
        targets.put("dirt", dirt);
        targets.put("grass", grass);
        targets.put("farmland", farm);
        StringBuilder d = new StringBuilder();
        for (Map.Entry<String, BlockPos> e : targets.entrySet()) {
            String ground = BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.getValue()).getBlock()).getPath();
            InteractionResult r = click(level, e.getValue(), new ItemStack(ModItems.GOLDEN_CARROT_SEEDS.get()));
            d.append(d.length() == 0 ? "" : "; ").append("seed on ").append(e.getKey()).append(" (").append(ground)
                    .append("): ").append(r).append(", above now ")
                    .append(BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.getValue().above()).getBlock()));
        }
        String carrotGround = BuiltInRegistries.BLOCK.getKey(level.getBlockState(farmCarrot).getBlock()).getPath();
        InteractionResult rc = click(level, farmCarrot, new ItemStack(Items.GOLDEN_CARROT));
        d.append("; golden carrot on farmland (").append(carrotGround).append("): ").append(rc).append(", above now ")
                .append(BuiltInRegistries.BLOCK.getKey(level.getBlockState(farmCarrot.above()).getBlock()));
        String first = d.toString();
        step(level, 40L, myRun, List.of(PLANTS), () -> {
            boolean onFarm = level.getBlockState(farm.above()).is(crop);
            boolean onDirt = level.getBlockState(dirt.above()).is(crop);
            boolean onGrass = level.getBlockState(grass.above()).is(crop);
            boolean carrotPlaced = !level.getBlockState(farmCarrot.above()).isAir()
                    && !level.getBlockState(farmCarrot.above()).is(Blocks.LIGHT);
            // Grass spreads onto the dirt target under the lamp within a minute, so either is the dirt
            // case still standing; what this guards is the click having tilled or replaced a target (#212).
            boolean groundsRight = (level.getBlockState(dirt).is(Blocks.DIRT)
                    || level.getBlockState(dirt).is(Blocks.GRASS_BLOCK))
                    && level.getBlockState(grass).is(Blocks.GRASS_BLOCK);
            String detail = first + " | 40 ticks later: crop on farmland " + onFarm + ", on dirt " + onDirt
                    + ", on grass " + onGrass + ", anything above the golden-carrot farmland " + carrotPlaced;
            if (!groundsRight && !onFarm) {
                unsure(PLANTS, "the dirt or grass target changed under the test; " + detail);
                return;
            }
            answer(PLANTS, onFarm && !onDirt && !onGrass && !carrotPlaced, detail);
        });
    }

    /** The hands right-click the top face of {@code ground} holding {@code held}, through the server's click path. */
    private static InteractionResult click(ServerLevel level, BlockPos ground, ItemStack held) {
        DebugYardClockwork.Hands h = new DebugYardClockwork.Hands(level);
        // Creative, or HorseGeneticsEventHandler.noBlockPlaceInDebugDimension cancels the placing and every
        // click reads Fail - which is what this check reported from 2026-10-05 to 2026-10-08 (#212). The
        // flag keeps the stack's count and changes nothing BlockItem.place decides.
        h.getAbilities().instabuild = true;
        h.snapTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() - 0.5, 0.0F, 30.0F);
        h.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5), Direction.UP, ground, false);
        // UNVERIFIED: ServerPlayerGameMode.useItemOn(ServerPlayer, Level, ItemStack, InteractionHand, BlockHitResult),
        // read in the 26.1.2 sources - the call the use-item-on packet handler makes.
        return h.gameMode.useItemOn(h, level, h.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND,
                hit);
    }
}
