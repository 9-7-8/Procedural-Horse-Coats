package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.mojang.serialization.Codec;
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
     * The same two halves drawn on <b>pale neutral timber</b>, for a half that
     * has been painted.
     *
     * <p><b>A tint can only darken</b> - it is a multiply - so painting a half
     * by tinting its own wood cannot produce a colour the wood does not already
     * have. Blue over oak came out a dark brown-navy and blue over a dark
     * modded wood came out "nearly black purple" (owner, 2026-09-20). Vanilla
     * dyes leather against a greyscale base for the same reason, and these are
     * that base: swapping the PART, not just the tint, is what lets a painted
     * jump be the colour it was painted.
     *
     * <p>Not per wood, deliberately - one set for all of them. A real
     * showjumping pole is painted a solid colour and you do not see the grain
     * through it, so a painted half genuinely has no wood to show. The wood is
     * still stored, and comes back the instant the paint is stripped.
     */
    private final BlockStateModelPart paintedRails;
    private final BlockStateModelPart paintedStandards;

    private JumpModel(Map<String, BlockStateModelPart> rails,
                      Map<String, BlockStateModelPart> standards,
                      BlockStateModelPart paintedRails,
                      BlockStateModelPart paintedStandards) {
        this.rails = rails;
        this.standards = standards;
        this.paintedRails = paintedRails;
        this.paintedStandards = paintedStandards;
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
        // A painted half is a different PART, not just a different tint - see
        // the field note. The tint still does the colouring; this only decides
        // what it is colouring.
        BlockStateModelPart railPart =
                materials.railsDye() != JumpMaterials.UNDYED
                        ? this.paintedRails
                        : this.rails.getOrDefault(materials.rails(), this.fallbackRails);
        BlockStateModelPart standardPart =
                materials.standardsDye() != JumpMaterials.UNDYED
                        ? this.paintedStandards
                        : this.standards.getOrDefault(materials.standards(), this.fallbackStandards);
        if (railPart != null) {
            parts.add(railPart);
        }
        if (standardPart != null) {
            parts.add(standardPart);
        }
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
        return materials == null ? JumpMaterials.DEFAULT : materials;
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
    public record Unbaked(String rails, String standards, Variant.SimpleModelState state)
            implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("rails").forGetter(Unbaked::rails),
                Codec.STRING.fieldOf("standards").forGetter(Unbaked::standards),
                Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::state)
        ).apply(i, Unbaked::new));

        /**
         * The pseudo-wood the painted parts are filed under, so that one
         * {@code model(wood, suffix)} call composes both the real woods'
         * ids and the painted ones. {@code PAINTED_TEXTURE} in
         * bake-jumps.mjs writes the files.
         */
        private static final String PAINTED = "painted";

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
            return new JumpModel(Map.copyOf(railParts), Map.copyOf(standardParts),
                    SimpleModelWrapper.bake(baker, model(PAINTED, this.rails),
                            this.state.asModelState()),
                    SimpleModelWrapper.bake(baker, model(PAINTED, this.standards),
                            this.state.asModelState()));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (String wood : JumpWoods.keys()) {
                resolver.markDependency(model(wood, this.rails));
                resolver.markDependency(model(wood, this.standards));
            }
            resolver.markDependency(model(PAINTED, this.rails));
            resolver.markDependency(model(PAINTED, this.standards));
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return MAP_CODEC;
        }
    }
}
