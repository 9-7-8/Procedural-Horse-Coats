package com.example.horsegenetics.neoforge.worldgen;

import com.example.horsegenetics.common.stable.StableSite;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>How uneven is the ground under a generated stable?</b> Measured without
 * generating a chunk, the way {@link HomesteadCensus} measures the homestead:
 * {@link Structure#generate} runs the whole placement and hands back the pieces.
 *
 * <p>Every stable chunk is generated twice on the same seed. The <b>control</b>
 * is the wrapped vanilla jigsaw ({@link LevelJigsawStructure#jigsaw}), which is
 * what shipped before issue #2. The other is the real {@code level_jigsaw}
 * structure. For each control stable it records the ground spread under the
 * footprint; for each accepted stable it re-measures the spread from the pieces
 * the generator actually produced, so a fit that was computed and then not
 * applied would show up here.
 */
public final class StableSiteCensus {

    /** The three generated stables, all on {@code horsegenetics:level_jigsaw}. */
    public static final List<String> STABLES = List.of("horse_stable", "stables", "tm_u_stable");

    private StableSiteCensus() {
    }

    /**
     * One run's count, over all three stables.
     *
     * @param sites      stable starts the control produced
     * @param uneven     of those, how many stood on ground spreading more than the limit
     * @param worstBefore the largest spread under any control stable
     * @param accepted   stable starts the level-ground type produced
     * @param worstAfter the largest spread under any of those, re-measured
     */
    public record Result(int seeds, int limit, int sites, int uneven, int worstBefore,
                         int accepted, int worstAfter, Map<String, List<Integer>> spreadsBefore) {

        public String line() {
            return String.format(
                    "%d seed(s), limit %d: control %d stable(s), %d on ground spreading over the limit (worst %d); "
                            + "level_jigsaw %d stable(s) (%.1f%% kept), worst spread %d",
                    this.seeds, this.limit, this.sites, this.uneven, this.worstBefore, this.accepted,
                    this.sites == 0 ? 0.0 : 100.0 * this.accepted / this.sites, this.worstAfter);
        }

        /**
         * The control's ground spread per stable, as quartiles. What a limit
         * would cost each building is read off this, not off the totals.
         */
        public String spreads() {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String, List<Integer>> entry : this.spreadsBefore.entrySet()) {
                List<Integer> s = new ArrayList<>(entry.getValue());
                if (s.isEmpty()) {
                    continue;
                }
                s.sort(null);
                out.append(String.format("%s n=%d min=%d q1=%d median=%d q3=%d max=%d; ", entry.getKey(), s.size(),
                        s.get(0), s.get(s.size() / 4), s.get(s.size() / 2), s.get(3 * s.size() / 4),
                        s.get(s.size() - 1)));
            }
            return out.toString();
        }
    }

    public static Result run(MinecraftServer server, int seeds, int perStructurePerSeed, int limit) {
        RegistryAccess registries = server.registryAccess();
        Holder<NoiseGeneratorSettings> noiseSettings =
                registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        Holder<MultiNoiseBiomeSourceParameterList> preset =
                registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        DimensionType overworld =
                registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD).value();
        LevelHeightAccessor height = LevelHeightAccessor.create(overworld.minY(), overworld.height());

        int sites = 0;
        int uneven = 0;
        int worstBefore = 0;
        int accepted = 0;
        int worstAfter = 0;
        Map<String, List<Integer>> spreadsBefore = new LinkedHashMap<>();

        for (String name : STABLES) {
            Holder<Structure> holder = registries.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(
                    Registries.STRUCTURE, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, name)));
            if (!(holder.value() instanceof LevelJigsawStructure level)) {
                throw new IllegalStateException(name + " is not a level_jigsaw structure - the issue #2 fix is not applied");
            }
            Structure control = level.jigsaw();
            List<Integer> spreads = spreadsBefore.computeIfAbsent(name, k -> new ArrayList<>());

            for (long seed = 1; seed <= seeds; seed++) {
                MultiNoiseBiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(preset);
                ChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, noiseSettings);
                RandomState randomState = RandomState.create(
                        noiseSettings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
                ChunkGeneratorStructureState spread = ChunkGeneratorStructureState.createForNormal(
                        randomState, seed, biomes, registries.lookupOrThrow(Registries.STRUCTURE_SET));
                List<StructurePlacement> placements = spread.getPlacementsForStructure(holder);

                int found = 0;
                for (ChunkPos candidate : structureChunks(spread, placements, perStructurePerSeed)) {
                    StructureStart before = control.generate(
                            holder, Level.OVERWORLD, registries, generator, biomes, randomState,
                            server.getStructureManager(), seed, candidate, 0, height, level.biomes()::contains);
                    if (!before.isValid()) {
                        continue; // wrong biome, usually
                    }
                    sites++;
                    found++;
                    int spreadBefore = measure(generator, height, randomState, before, limit).spread();
                    worstBefore = Math.max(worstBefore, spreadBefore);
                    spreads.add(spreadBefore);
                    if (spreadBefore > limit) {
                        uneven++;
                    }

                    StructureStart after = level.generate(
                            holder, Level.OVERWORLD, registries, generator, biomes, randomState,
                            server.getStructureManager(), seed, candidate, 0, height, level.biomes()::contains);
                    if (after.isValid()) {
                        accepted++;
                        worstAfter = Math.max(worstAfter,
                                measure(generator, height, randomState, after, limit).spread());
                    }
                    if (found >= perStructurePerSeed) {
                        break;
                    }
                }
            }
        }
        return new Result(seeds, limit, sites, uneven, worstBefore, accepted, worstAfter, spreadsBefore);
    }

    /** The ground under a finished start's pieces, centred on the first (start) piece as the jigsaw does. */
    private static StableSite.Fit measure(ChunkGenerator generator, LevelHeightAccessor height,
                                          RandomState random, StructureStart start, int limit) {
        List<StructurePiece> pieces = start.getPieces();
        BoundingBox startBox = pieces.get(0).getBoundingBox();
        BoundingBox footprint = BoundingBox.encapsulatingBoxes(
                pieces.stream().map(StructurePiece::getBoundingBox).toList()).orElse(startBox);
        BlockPos centre = new BlockPos(
                (startBox.minX() + startBox.maxX()) / 2, 0, (startBox.minZ() + startBox.maxZ()) / 2);
        return LevelJigsawStructure.fit(generator, height, random, footprint, centre, limit);
    }

    /** Chunks the placement says are this structure's, in square rings out from the origin. */
    private static List<ChunkPos> structureChunks(
            ChunkGeneratorStructureState spread, List<StructurePlacement> placements, int wanted) {
        List<ChunkPos> out = new ArrayList<>();
        int budget = wanted * 8;
        for (int ring = 0; ring < 512 && out.size() < budget; ring++) {
            for (int x = -ring; x <= ring && out.size() < budget; x++) {
                for (int z = -ring; z <= ring && out.size() < budget; z++) {
                    if (ring != 0 && Math.abs(x) != ring && Math.abs(z) != ring) {
                        continue;
                    }
                    for (StructurePlacement placement : placements) {
                        if (placement.isStructureChunk(spread, x, z)) {
                            out.add(new ChunkPos(x, z));
                            break;
                        }
                    }
                }
            }
        }
        return out;
    }
}
