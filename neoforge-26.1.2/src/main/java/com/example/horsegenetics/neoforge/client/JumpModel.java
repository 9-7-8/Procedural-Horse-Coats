package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>A jump drawn in two woods</b> - the rails one wood, the standards another.
 *
 * <h2>Why this exists at all</h2>
 * The owner's ask was that "the supporting poles and the jumping poles must be
 * able to be made of different woods". As blockstate properties that is
 * 12 x 12 x every other property - <b>13,824 block states</b>, every one of them
 * allocated at registry bootstrap on the server as well as the client. So the
 * woods are block-entity data instead, and this model reads them per position.
 *
 * <h2>The trick is that parts compose, so a product becomes a sum</h2>
 * A {@link BlockStateModel} is a <i>list of {@link BlockStateModelPart}s</i>,
 * not one blob of quads. So there is no need to bake a model per wood
 * <i>pair</i>: bake one rails part per wood and one standards part per wood -
 * <b>12 + 12, not 12 x 12</b> - and push one of each. Every future style and
 * every modded wood costs one more part, not a hundred more combinations.
 *
 * <h2>What replaced what, because every tutorial you find is wrong</h2>
 * {@code BakedModel} and {@code IDynamicBakedModel} <b>do not exist</b> in
 * 26.1.2. The interface is {@code BlockStateModel}, the per-position hook is
 * NeoForge's {@link DynamicBlockStateModel#collectParts}, and registration is
 * the {@code RegisterBlockStateModels} event keyed on a {@code "type"} field in
 * the blockstate JSON. NeoForge's own {@code CompositeBlockModel} is the
 * worked example this is modelled on.
 *
 * <h2>Thread safety is not optional here</h2>
 * {@link #collectParts} runs on <b>chunk-meshing worker threads</b>, against a
 * snapshot of the region. It must not touch the block entity: it reads
 * {@code level.getModelData(pos)}, and what that carries -
 * {@link JumpMaterials} - is an immutable record of two strings for exactly
 * this reason.
 *
 * @see JumpBlockEntity for the data, and for the re-mesh that makes a change visible
 */
public class JumpModel implements DynamicBlockStateModel {

    /** The {@code "type"} that selects this model in a blockstate variant. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "jump");

    private final Map<String, BlockStateModelPart> rails;
    private final Map<String, BlockStateModelPart> standards;
    private final BlockStateModelPart fallbackRails;
    private final BlockStateModelPart fallbackStandards;

    /**
     * <b>The coat of paint</b> - the same boxes a hair larger, flat white at a
     * fixed alpha, tinted, and laid <i>over</i> the wood rather than instead of
     * it.
     *
     * <p>This is the third design and the first that works. <b>Tinting the wood
     * itself is a multiply</b>, so it can only darken and cannot shift a hue the
     * wood does not already have - blue over oak came out a dark navy-brown and
     * blue over a dark modded wood came out "nearly black purple".
     * <b>Painting a pale neutral pole instead</b> fixed the colour and threw the
     * wood away with it: "we're losing too much of the original texture /
     * shading". An overlay is the only one of the three that keeps the grain and
     * the shading at full contrast <i>and</i> lets the colour read as itself,
     * because an alpha blend is not bounded by the texture underneath the way a
     * multiply is.
     *
     * <p>Not per wood: it is white, and the wood is still there underneath.
     */
    private final BlockStateModelPart overlayRails;
    private final BlockStateModelPart overlayStandards;

    /**
     * <b>Crossrails only: one rails part per wood per slice of a crossed
     * pair.</b> Null for every other style.
     *
     * <p>A crossrail is two poles crossing once across the whole obstacle, so
     * what a given block draws depends on how wide its run is and where in it
     * the block sits - see {@link JumpBlock#crossSegment}. Six slices, indexed
     * by that method. Every other style draws the same rails in every block of
     * a run and has nothing to choose between, so it keeps the single part and
     * these stay null rather than holding six copies of one thing.
     */
    private final Map<String, BlockStateModelPart[]> railSegments;
    private final BlockStateModelPart[] overlaySegments;

    private JumpModel(Map<String, BlockStateModelPart> rails,
                      Map<String, BlockStateModelPart> standards,
                      BlockStateModelPart overlayRails,
                      BlockStateModelPart overlayStandards,
                      @Nullable Map<String, BlockStateModelPart[]> railSegments,
                      BlockStateModelPart @Nullable [] overlaySegments) {
        this.rails = rails;
        this.standards = standards;
        this.overlayRails = overlayRails;
        this.overlayStandards = overlayStandards;
        this.railSegments = railSegments;
        this.overlaySegments = overlaySegments;
        // A wood key that no longer resolves - a wood whose mod was removed
        // since the jump was placed - draws as the default rather than as
        // nothing. A jump that vanishes is far worse than one in the wrong wood.
        this.fallbackRails = rails.get(JumpMaterials.DEFAULT_WOOD);
        this.fallbackStandards = standards.get(JumpMaterials.DEFAULT_WOOD);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                             RandomSource random, List<BlockStateModelPart> parts) {
        JumpMaterials materials = level.getModelData(pos).get(JumpBlockEntity.MATERIALS);
        if (materials == null) {
            materials = JumpMaterials.DEFAULT;
        }
        // THE WOOD ALWAYS GOES IN. A painted half gains a second part on top
        // rather than replacing its first, which is the whole reason the grain
        // survives being painted.
        int segment = this.railSegments == null
                ? -1
                : JumpBlock.crossSegment(level, pos, state);
        BlockStateModelPart railPart = segment < 0
                ? this.rails.getOrDefault(materials.rails(), this.fallbackRails)
                : segmentOf(materials.rails(), segment);
        BlockStateModelPart standardPart =
                this.standards.getOrDefault(materials.standards(), this.fallbackStandards);
        if (railPart != null) {
            parts.add(railPart);
        }
        if (standardPart != null) {
            parts.add(standardPart);
        }
        if (materials.railsDye() != JumpMaterials.UNDYED) {
            parts.add(segment < 0 ? this.overlayRails : this.overlaySegments[segment]);
        }
        if (materials.standardsDye() != JumpMaterials.UNDYED) {
            parts.add(this.overlayStandards);
        }
    }

    /** This wood's slice, falling back to the default wood's if it has gone. */
    private BlockStateModelPart segmentOf(String wood, int segment) {
        BlockStateModelPart[] slices = this.railSegments.get(wood);
        if (slices == null) {
            slices = this.railSegments.get(JumpMaterials.DEFAULT_WOOD);
        }
        return slices == null ? this.fallbackRails : slices[segment];
    }

    /**
     * The key a third-party chunk mesher caches on.
     *
     * <p>Two jumps of the same two woods are the same geometry, so the pair of
     * parts <i>is</i> the key. Nothing in vanilla 26.1.2 calls this - it is a
     * hook for Sodium-shaped mesh caches - but returning a real key rather than
     * null is cheap insurance against the day something does.
     */
    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                    RandomSource random) {
        JumpMaterials materials = level.getModelData(pos).get(JumpBlockEntity.MATERIALS);
        JumpMaterials key = materials == null ? JumpMaterials.DEFAULT : materials;
        // The SLICE is part of the geometry too - two crossrails of identical
        // woods draw different poles depending on where in their run they sit,
        // so a cache keyed on the woods alone would hand one the other's.
        return this.railSegments == null
                ? key
                : List.of(key, JumpBlock.crossSegment(level, pos, state));
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.fallbackRails.particleMaterial();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return this.fallbackRails.materialFlags() | this.fallbackStandards.materialFlags();
    }

    /**
     * One blockstate variant's worth of jump, before baking.
     *
     * <p><b>The wood list is not in the JSON</b>, and deliberately: it would be
     * twenty-four entries repeated across ninety-six variants, and half of it
     * depends on which other mods are installed. Instead the variant names a
     * model <i>suffix</i> for each part, and baking composes
     * {@code horsegenetics:block/<wood>_<suffix>} for every wood the game knows
     * about. So the JSON stays two strings and a rotation:
     *
     * <pre>{@code
     * { "type": "horsegenetics:jump",
     *   "rails": "jump_vertical_rails",
     *   "standards": "jump_vertical_standards_lr",
     *   "y": 90, "uvlock": true }
     * }</pre>
     *
     * @param rails     model suffix for the part a horse jumps
     * @param standards model suffix for the uprights, and the intermediate post
     * @param state     the rotation and uv-lock, exactly as a vanilla variant carries them
     */
    public record Unbaked(String rails, String standards, boolean spanning,
                          Variant.SimpleModelState state)
            implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("rails").forGetter(Unbaked::rails),
                Codec.STRING.fieldOf("standards").forGetter(Unbaked::standards),
                // Optional and false by default: only the crossrails variants
                // set it, and a variant that does not is the overwhelming
                // majority of a ninety-six-entry file.
                Codec.BOOL.optionalFieldOf("spanning", false).forGetter(Unbaked::spanning),
                Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::state)
        ).apply(i, Unbaked::new));

        /**
         * The pseudo-wood the paint overlay is filed under, so that one
         * {@code model(wood, suffix)} call composes both the real woods' ids
         * and the overlay's. {@code OVERLAY_TEXTURE} in bake-jumps.mjs writes
         * the files.
         */
        private static final String OVERLAY = "overlay";

        /**
         * The suffix one slice of a crossed pair is filed under -
         * {@code _s3i1} is the middle of a three-wide X. The twin is
         * {@code crossSuffix} in bake-jumps.mjs, and the ORDER is
         * {@code JumpBlock.segmentIndex}.
         */
        private static String slice(int span, int index) {
            return "_s" + span + "i" + index;
        }

        private Identifier model(String wood, String suffix) {
            return Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
                    "block/" + wood + "_" + suffix);
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            Map<String, BlockStateModelPart> railParts = new LinkedHashMap<>();
            Map<String, BlockStateModelPart> standardParts = new LinkedHashMap<>();
            for (String wood : JumpWoods.keys()) {
                railParts.put(wood, SimpleModelWrapper.bake(
                        baker, model(wood, this.rails), this.state.asModelState()));
                standardParts.put(wood, SimpleModelWrapper.bake(
                        baker, model(wood, this.standards), this.state.asModelState()));
            }
            // The six slices, but ONLY for a spanning style. Baking them for
            // every style would be five wasted parts per wood per variant, on a
            // file that already has ninety-six variants.
            Map<String, BlockStateModelPart[]> segments = null;
            BlockStateModelPart[] overlaySegments = null;
            if (this.spanning) {
                segments = new LinkedHashMap<>();
                for (String wood : JumpWoods.keys()) {
                    segments.put(wood, bakeSlices(baker, wood));
                }
                segments = Map.copyOf(segments);
                overlaySegments = bakeSlices(baker, OVERLAY);
            }
            return new JumpModel(Map.copyOf(railParts), Map.copyOf(standardParts),
                    SimpleModelWrapper.bake(baker, model(OVERLAY, this.rails),
                            this.state.asModelState()),
                    SimpleModelWrapper.bake(baker, model(OVERLAY, this.standards),
                            this.state.asModelState()),
                    segments, overlaySegments);
        }

        /** One wood's six slices, in {@code JumpBlock.segmentIndex} order. */
        private BlockStateModelPart[] bakeSlices(ModelBaker baker, String wood) {
            BlockStateModelPart[] slices =
                    new BlockStateModelPart[JumpBlock.CROSS_SEGMENTS];
            for (int span = 1; span <= JumpBlock.CROSS_MAX_SPAN; span++) {
                for (int index = 0; index < span; index++) {
                    slices[JumpBlock.segmentIndex(span, index)] = SimpleModelWrapper.bake(
                            baker, model(wood, this.rails + slice(span, index)),
                            this.state.asModelState());
                }
            }
            return slices;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (String wood : JumpWoods.keys()) {
                resolver.markDependency(model(wood, this.rails));
                resolver.markDependency(model(wood, this.standards));
            }
            resolver.markDependency(model(OVERLAY, this.rails));
            resolver.markDependency(model(OVERLAY, this.standards));
            if (this.spanning) {
                for (int span = 1; span <= JumpBlock.CROSS_MAX_SPAN; span++) {
                    for (int index = 0; index < span; index++) {
                        String slice = slice(span, index);
                        for (String wood : JumpWoods.keys()) {
                            resolver.markDependency(model(wood, this.rails + slice));
                        }
                        resolver.markDependency(model(OVERLAY, this.rails + slice));
                    }
                }
            }
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return MAP_CODEC;
        }
    }
}
