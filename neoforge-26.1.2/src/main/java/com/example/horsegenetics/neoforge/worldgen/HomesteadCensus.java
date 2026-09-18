package com.example.horsegenetics.neoforge.worldgen;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>How often does the cowboy's homestead actually generate in a plains
 * village?</b> Counted, over as many seeds as you like, without generating a
 * single chunk.
 *
 * <p>This exists because that question was written down three times as
 * unanswerable - "nothing automated can answer this, it is a fresh world and a
 * walk". It is not. {@link Structure#generate} <b>is</b> the village generator:
 * it runs the whole jigsaw placement, every bounding-box test included, and
 * hands back the finished piece list. What it does <i>not</i> do is touch the
 * world. So the measurement is: build the overworld generator for a seed, ask
 * the structure placement which chunks are village chunks, generate the start
 * for each, and look in the piece list for our element. A few hundred villages
 * is seconds, and the answer is exact rather than an impression formed while
 * walking.
 *
 * <p><b>What it can and cannot tell you.</b> It measures the thing that was in
 * doubt - whether the piece wins its slot - and nothing downstream of that. A
 * homestead this counts is one the generator committed to; whether it then
 * looks right, whether the villagers claim their posts, and whether the terrain
 * under it is somewhere you would want to live are all still a walk. See
 * {@code wiki/verification.html}.
 *
 * <h2>Reading the result</h2>
 * The interesting number is not really the rate. It is the <b>connector
 * split</b>: which of the piece's three street connectors the generator
 * actually used. The piece carried one for a long time, which fixed its
 * rotation and gave it exactly one way to lie against a road (see
 * {@link BarnPoolInjector}); if the two that were added are never the one that
 * wins, they bought nothing and the rate is telling you something else.
 */
public final class HomesteadCensus {

    /** Plains villages only - the barn is in no other village's terminator pool. */
    private static final ResourceKey<Structure> VILLAGE_PLAINS = ResourceKey.create(
            Registries.STRUCTURE, Identifier.withDefaultNamespace("village_plains"));

    /** The pool this mod owns, whose elements {@link BarnPoolInjector} appends to vanilla's. */
    private static final ResourceKey<StructureTemplatePool> BARN_POOL = ResourceKey.create(
            Registries.TEMPLATE_POOL, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "cowboy_barn"));

    private HomesteadCensus() {
    }

    /** What one run counted. */
    public record Result(int seeds, int villages, int homesteads, Map<Direction, Integer> byConnector) {

        public double rate() {
            return this.villages == 0 ? 0.0 : (double) this.homesteads / this.villages;
        }

        /**
         * One line per run, written so two runs can be diffed by eye - which is
         * how a before-and-after on the piece's shape is read.
         */
        public String line() {
            StringBuilder split = new StringBuilder();
            for (Map.Entry<Direction, Integer> entry : this.byConnector.entrySet()) {
                if (split.length() > 0) {
                    split.append(' ');
                }
                split.append(entry.getKey().getName()).append('=').append(entry.getValue());
            }
            return String.format(
                    "%d seed(s), %d plains village(s), %d homestead(s) - %.1f%% of villages; connector used: %s",
                    this.seeds, this.villages, this.homesteads, this.rate() * 100.0,
                    split.length() == 0 ? "none" : split);
        }
    }

    /**
     * Census {@code seeds} worlds, looking at up to {@code villagesPerSeed}
     * plains villages in each.
     *
     * <p>Seeds are {@code 1..seeds} rather than random, so two runs of this are
     * comparable. That is the whole point of it: the number on its own means
     * very little, and the number before and after a change to the piece means
     * a great deal.
     */
    public static Result run(MinecraftServer server, int seeds, int villagesPerSeed) {
        RegistryAccess registries = server.registryAccess();

        // The barn's pool elements, by identity. BarnPoolInjector appends these
        // very objects to the vanilla terminator pool, so a placed piece whose
        // element is one of them is our homestead - no string matching on a
        // template id, and nothing to go stale if the pool gains a second
        // element one day.
        StructureTemplatePool barnPool = registries.lookupOrThrow(Registries.TEMPLATE_POOL).getValue(BARN_POOL);
        if (barnPool == null) {
            throw new IllegalStateException("no " + BARN_POOL.identifier() + " pool - is the mod's data loaded?");
        }
        Set<StructurePoolElement> barnElements = new java.util.HashSet<>();
        for (Pair<StructurePoolElement, Integer> entry : barnPool.rawTemplates) {
            barnElements.add(entry.getFirst());
        }

        Holder<Structure> village = registries.lookupOrThrow(Registries.STRUCTURE).getOrThrow(VILLAGE_PLAINS);
        Structure structure = village.value();

        // The overworld as the game would build it, but for a seed of our
        // choosing rather than this server's. Everything seed-bearing is made
        // here: the noise (RandomState) and the structure spread
        // (ChunkGeneratorStructureState). The chunk generator itself carries no
        // seed, so it could be shared - it is rebuilt per seed anyway, because
        // a generator that quietly kept a cache of the last seed's columns
        // would be the sort of bug this whole class exists to avoid.
        Holder<NoiseGeneratorSettings> noiseSettings =
                registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        Holder<MultiNoiseBiomeSourceParameterList> preset =
                registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        DimensionType overworld =
                registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD).value();
        LevelHeightAccessor height = LevelHeightAccessor.create(overworld.minY(), overworld.height());

        int villages = 0;
        int homesteads = 0;
        Map<Direction, Integer> byConnector = new EnumMap<>(Direction.class);

        for (long seed = 1; seed <= seeds; seed++) {
            MultiNoiseBiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(preset);
            ChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, noiseSettings);
            RandomState randomState = RandomState.create(
                    noiseSettings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            ChunkGeneratorStructureState spread = ChunkGeneratorStructureState.createForNormal(
                    randomState, seed, biomes, registries.lookupOrThrow(Registries.STRUCTURE_SET));
            List<StructurePlacement> placements = spread.getPlacementsForStructure(village);

            int found = 0;
            for (ChunkPos candidate : villageChunks(spread, placements, villagesPerSeed)) {
                StructureStart start = structure.generate(
                        village, Level.OVERWORLD, registries, generator, biomes, randomState,
                        server.getStructureManager(), seed, candidate, 0, height,
                        structure.biomes()::contains);
                if (!start.isValid()) {
                    continue; // a village chunk whose village did not take - wrong biome, usually
                }
                villages++;
                found++;

                Direction connector = homesteadConnector(server, start, barnElements);
                if (connector != null) {
                    homesteads++;
                    byConnector.merge(connector, 1, Integer::sum);
                }
                if (found >= villagesPerSeed) {
                    break;
                }
            }
        }

        return new Result(seeds, villages, homesteads, byConnector);
    }

    /**
     * Chunks the structure spread says are village chunks, walked outward from
     * the origin in a square ring so the sample is the villages nearest spawn
     * rather than a stripe along one axis.
     *
     * <p>It over-collects deliberately - {@code wanted} villages needs rather
     * more than {@code wanted} village <i>chunks</i>, because a village chunk in
     * the wrong biome produces no village at all.
     */
    private static List<ChunkPos> villageChunks(
            ChunkGeneratorStructureState spread, List<StructurePlacement> placements, int wanted) {
        List<ChunkPos> out = new ArrayList<>();
        int budget = wanted * 8;
        for (int ring = 0; ring < 512 && out.size() < budget; ring++) {
            for (int x = -ring; x <= ring && out.size() < budget; x++) {
                for (int z = -ring; z <= ring; z++) {
                    // the ring's edge only, so a chunk is offered exactly once
                    if (ring != 0 && Math.abs(x) != ring && Math.abs(z) != ring) {
                        continue;
                    }
                    for (StructurePlacement placement : placements) {
                        if (placement.isStructureChunk(spread, x, z)) {
                            out.add(new ChunkPos(x, z));
                            break;
                        }
                    }
                    if (out.size() >= budget) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    /**
     * The homestead's <b>local</b> connector direction if this village has one,
     * else null.
     *
     * <p>Which connector was used is not recorded anywhere, so it is recovered:
     * the piece keeps a {@link JigsawJunction} at the position of the street
     * jigsaw it attached to, and the connector that was used is the one now
     * standing next to it. Asking the element for its jigsaw blocks under the
     * piece's own position and rotation gives those in world space; rotating
     * the winner's facing back through the piece's rotation gives the direction
     * it has in the file, which is the thing worth reporting - west is the
     * original head-on connector, north and south the two that were added.
     */
    private static Direction homesteadConnector(
            MinecraftServer server, StructureStart start, Set<StructurePoolElement> barnElements) {
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool) || !barnElements.contains(pool.getElement())) {
                continue;
            }
            List<JigsawJunction> junctions = pool.getJunctions();
            if (junctions.isEmpty()) {
                return Direction.WEST; // cannot happen for an attached piece; do not lose the count over it
            }
            JigsawJunction junction = junctions.get(0);
            Rotation rotation = pool.getRotation();
            List<StructureTemplate.JigsawBlockInfo> jigsaws = pool.getElement().getShuffledJigsawBlocks(
                    server.getStructureManager(), pool.getPosition(), rotation, RandomSource.create(0L));
            for (StructureTemplate.JigsawBlockInfo jigsaw : jigsaws) {
                BlockPos at = jigsaw.info().pos();
                int dx = Math.abs(at.getX() - junction.getSourceX());
                int dz = Math.abs(at.getZ() - junction.getSourceZ());
                if (dx + dz != 1) {
                    continue; // not the one that met the road
                }
                Direction world = JigsawBlock.getFrontFacing(jigsaw.info().state());
                for (Direction local : Direction.Plane.HORIZONTAL) {
                    if (rotation.rotate(local) == world) {
                        return local;
                    }
                }
            }
            return Direction.WEST;
        }
        return null;
    }
}
