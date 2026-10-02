package com.example.horsegenetics.neoforge.worldgen;

import com.example.horsegenetics.common.stable.StableSite;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Optional;

/**
 * <b>{@code horsegenetics:level_jigsaw}</b>: a vanilla jigsaw structure that
 * refuses uneven ground. The generated stables use it (issue #2).
 *
 * <p>The JSON is a {@code minecraft:jigsaw} structure's, with one extra field:
 * {@code max_ground_spread} (blocks, default 10). The jigsaw places the pieces
 * exactly as before. Then the ground is sampled under the finished footprint's
 * corners and centre, and {@link StableSite#fit} either rejects the site or
 * drops the whole build to the lowest sample. The rule, and why, is on
 * {@link StableSite}.
 *
 * <p>{@link JigsawStructure} is {@code final}, so this delegates to it rather
 * than extending it. The pieces are built here, in {@code findGenerationPoint},
 * instead of lazily as vanilla does, because the footprint is not known until
 * they are. {@code Structure.generate} asks for the builder straight after, so
 * the jigsaw draws from the random source in the same order either way.
 *
 * <p>Biomes, step, spawn overrides and terrain adaptation are this structure's
 * own settings, read from the same JSON, so NeoForge structure modifiers and the
 * beardifier see this structure and not the wrapped one.
 */
public final class LevelJigsawStructure extends Structure {

    public static final int DEFAULT_MAX_GROUND_SPREAD = 10;

    public static final MapCodec<LevelJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            JigsawStructure.CODEC.forGetter(s -> s.jigsaw),
            Codec.intRange(0, 64).optionalFieldOf("max_ground_spread", DEFAULT_MAX_GROUND_SPREAD)
                    .forGetter(s -> s.maxGroundSpread)
    ).apply(i, LevelJigsawStructure::new));

    /**
     * The heightmap the stables' JSON projects onto ({@code project_start_to_heightmap}).
     * The wrapped structure keeps its copy private, so this one is written down
     * again. WG heightmaps hold terrain only, with no trees yet.
     */
    private static final Heightmap.Types GROUND = Heightmap.Types.WORLD_SURFACE_WG;

    private final JigsawStructure jigsaw;
    private final int maxGroundSpread;

    public LevelJigsawStructure(JigsawStructure jigsaw, int maxGroundSpread) {
        super(jigsaw.modifiableStructureInfo().getOriginalStructureInfo().structureSettings());
        this.jigsaw = jigsaw;
        this.maxGroundSpread = maxGroundSpread;
    }

    /** The structure as vanilla would place it, with no ground check: the census's control. */
    public JigsawStructure jigsaw() {
        return this.jigsaw;
    }

    public int maxGroundSpread() {
        return this.maxGroundSpread;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Optional<GenerationStub> stub = this.jigsaw.findGenerationPoint(context);
        if (stub.isEmpty()) {
            return stub;
        }
        BlockPos position = stub.get().position();
        StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
        if (builder.isEmpty()) {
            return Optional.empty();
        }
        StableSite.Fit fit = fit(context.chunkGenerator(), context.heightAccessor(), context.randomState(),
                builder.getBoundingBox(), position, this.maxGroundSpread);
        if (!fit.accepted()) {
            return Optional.empty();
        }
        builder.offsetPiecesVertically(fit.drop());
        return Optional.of(new GenerationStub(position.offset(0, fit.drop(), 0), Either.right(builder)));
    }

    /**
     * The ground under {@code box}'s corners and the start piece's centre
     * ({@code centre}, which is where the jigsaw projected the start), judged by
     * {@link StableSite#fit}. Public for {@link StableSiteCensus}.
     */
    public static StableSite.Fit fit(ChunkGenerator generator, LevelHeightAccessor height, RandomState random,
                                     BoundingBox box, BlockPos centre, int maxSpread) {
        int centreGround = generator.getFirstFreeHeight(centre.getX(), centre.getZ(), GROUND, height, random);
        int[] samples = {
                generator.getFirstFreeHeight(box.minX(), box.minZ(), GROUND, height, random),
                generator.getFirstFreeHeight(box.minX(), box.maxZ(), GROUND, height, random),
                generator.getFirstFreeHeight(box.maxX(), box.minZ(), GROUND, height, random),
                generator.getFirstFreeHeight(box.maxX(), box.maxZ(), GROUND, height, random),
                centreGround,
        };
        return StableSite.fit(samples, centreGround, maxSpread);
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.LEVEL_JIGSAW.get();
    }
}
