package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * Vanilla's {@code HorseRenderer} is {@code final} in 26.1.2, so we extend
 * {@code AbstractHorseRenderer} directly and replicate its constructor.
 *
 * <p>Since the coat rework <b>every</b> horse - adult <i>and</i> foal - renders
 * a {@code GeneticCoatTextureFactory}-generated 128px texture, the adult on
 * {@link HdHorseModel} and the foal on {@link HdBabyHorseModel} (both 128px,
 * per-part UV). The models are handed straight to the super constructor as the
 * adult / baby model, so there's no per-entity model swap.
 *
 * <p><b>Both textures are resolved once, in {@link #extractRenderState}</b>,
 * and read back from the state - the coat by {@link #getTextureLocation}, the
 * glow mask by {@link EmissiveCoatLayer}. The factory rations new bakes and a
 * horse beyond {@code coats.detailDistance} may not start one; see
 * {@link GeneticCoatTextureFactory} for the freeze that made both necessary.
 */
public class GeneticHorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {

    public GeneticHorseRenderer(EntityRendererProvider.Context context) {
        super(context,
                new HdHorseModel(context.bakeLayer(ClientSetup.HD_HORSE)),
                new HdBabyHorseModel(context.bakeLayer(ClientSetup.HD_HORSE_BABY)));
        // NO HorseMarkingLayer: vanilla's white/roan marking overlays
        // (horse_markings_white.png etc.) would paint a big white patch over the
        // generated coat - a wild horse or foal that rolled Markings.WHITE then
        // renders as a flat white horse. All white markings in this mod come
        // from the splash gene inside the coat texture instead.
        //
        // The emissive layer, on the other hand, IS ours: it redraws a glow
        // gene's emissive coat regions (Suntouched's mane) at full brightness.
        this.addLayer(new EmissiveCoatLayer(this));
        // The cutie-mark emblem, drawn last so it sits on top of the coat and
        // every white pattern (a no-op unless the horse is Cutmrk/Cutmrk).
        this.addLayer(new CutieMarkLayer(this));
        // After the marks: a braid is worked into the hair and sits on top of
        // everything the coat did, the same way the emissive pass does.
        this.addLayer(new BraidLayer(this));
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem,
                new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)),
                null,
                2
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_SADDLE,
                // A phantom saddle is how a top-bond horse is steered bare;
                // it is not something the player put there and must not be
                // drawn. See ModDataComponents.PHANTOM_SADDLE.
                state -> state.saddle != null
                        && state.saddle.has(ModDataComponents.PHANTOM_SADDLE.get())
                        ? net.minecraft.world.item.ItemStack.EMPTY
                        : state.saddle,
                new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)),
                null,
                2
            )
        );
    }

    @Override
    public GeneticHorseRenderState createRenderState() {
        return new GeneticHorseRenderState();
    }

    @Override
    public void extractRenderState(Horse horse, HorseRenderState renderState, float partialTick) {
        super.extractRenderState(horse, renderState, partialTick);
        stretchGaitToSize(renderState);
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            CoatData coatData = ClientCoatCache.get(horse.getId());
            if (coatData != null) {
                geneticState.coatData = coatData;
            }
            com.example.horsegenetics.common.horse.HorseRecord rec =
                    ClientHorseRecordCache.get(horse.getId());
            geneticState.breedLabel = rec == null ? null : rec.lineage().displayName();
            GeneticCoatTextureFactory.Resolved textures = GeneticCoatTextureFactory.resolve(
                    geneticState.coatData, renderState.isBaby, CoatTextureComposer.glowParts(geneticState.coatData.genotype()),
                    geneticState.breedLabel, withinDetailDistance(renderState));
            geneticState.coatId = textures.coat();
            geneticState.emissiveCoatId = textures.glow();
            // Gear is a synced attachment, so the client has the worn stacks
            // without a packet of this layer's own - see HorseGear.
            geneticState.braidMane = braidColour(horse,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.MANE);
            geneticState.braidTail = braidColour(horse,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.TAIL);
        }
    }

    /**
     * The opaque ARGB of the rescuing braid in this slot, or {@code 0} if there
     * is not one. Vanilla's {@code dyed_color} is the whole of a braid's
     * appearance - see {@code RescuingBraidItem} on why it is not the mod's own
     * three-zone tint.
     */
    private static int braidColour(Horse horse,
                                   com.example.horsegenetics.neoforge.entity.HorseTackSlot slot) {
        net.minecraft.world.item.ItemStack worn = slot.on(horse);
        if (!(worn.getItem() instanceof com.example.horsegenetics.neoforge.item.RescuingBraidItem)) {
            return 0;
        }
        return 0xFF000000 | (net.minecraft.world.item.component.DyedItemColor.getOrDefault(
                worn, BRAID_UNDYED) & 0x00FFFFFF);
    }

    /**
     * What an undyed braid is: the colour of horse hair, which is what one is
     * made of. Matches the {@code default} in the item's own model definition,
     * and the two are a pair - a braid that looked one colour in the hand and
     * another on the horse would read as a bug.
     */
    private static final int BRAID_UNDYED = 0xA05740;

    /**
     * <b>Close enough to be worth a coat of its own?</b> {@code distanceToCameraSq}
     * is written by vanilla's {@code EntityRenderer.extractRenderState}, which
     * the call to {@code super} above has already run (checked in bytecode,
     * 2026-09-13 - not assumed).
     *
     * <p>This only decides whether a horse may <i>start</i> a bake. One whose
     * coat already exists keeps it at any range, so walking back and forth
     * across the line swaps nothing and nothing flickers.
     */
    private static boolean withinDetailDistance(HorseRenderState renderState) {
        double blocks = ClientConfig.coatDetailDistance();
        return renderState.distanceToCameraSq <= blocks * blocks;
    }

    /**
     * <b>Make a scaled horse take proportionally longer strides.</b> Without
     * this, a horse from the magical size locus walks with its feet sliding
     * along the ground.
     *
     * <p>Vanilla advances the leg-swing phase (<code>walkAnimationPos</code>)
     * from the <b>world distance the entity moved</b> and nothing else -
     * {@code LivingEntity.updateWalkAnimation} is {@code min(distance * 4, 1)}
     * fed into {@code walkAnimation.update(...)}. The only size compensation
     * anywhere in it is a hard-coded {@code isBaby() ? 3.0F : 1.0F}: a foal's
     * legs are short, so they cycle three times as fast for the same ground.
     * Nothing consults {@link net.minecraft.world.entity.ai.attributes.Attributes#SCALE},
     * because before this mod nothing changed it.
     *
     * <p>So a horse rendered at twice the size covers ground at its ordinary
     * speed while its legs - now twice as long - swing at the ordinary rate. Its
     * feet have to slide to keep up. The bigger the horse, the worse it looks,
     * and a tiny horse gets the mirror image: legs windmilling far faster than
     * the ground goes by.
     *
     * <p>Dividing the phase by the render scale is the whole fix. The phase is a
     * monotonic accumulator, so scaling it after the fact is identical to having
     * accumulated it at {@code 1/scale} the rate, and the scale is a constant of
     * the horse's genotype so the division never jumps mid-stride. Amplitude
     * (<code>walkAnimationSpeed</code>) is deliberately left alone: it is a 0-1
     * multiplier on an angle, and an angle already scales with the model.
     *
     * <p>Nothing happens at scale 1, which is every horse in a world where the
     * size locus is switched off.
     */
    private static void stretchGaitToSize(HorseRenderState renderState) {
        float scale = renderState.scale;
        if (scale > 0.0F && scale != 1.0F) {
            renderState.walkAnimationPos /= scale;
        }
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState renderState) {
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            if (geneticState.coatId != null) {
                return geneticState.coatId;
            }
            return coatTextureFor(geneticState.coatData, renderState.isBaby, geneticState.breedLabel);
        }
        return coatTextureFor(CoatData.DEFAULT, renderState.isBaby, null);
    }

    /**
     * <b>Put a coat on a render state that did not come from a live horse.</b>
     * The one supported way for a GUI to draw a specific genome.
     *
     * <p>A screen builds its model horse as a throwaway client-side entity that
     * was never in the world, so {@link #extractRenderState} finds nothing for
     * it in {@link ClientCoatCache} and resolves {@link #coatId} from
     * {@link CoatData#DEFAULT} - the plain black wild type. Assigning
     * {@code coatData} afterwards then changes nothing, because
     * {@link #getTextureLocation} prefers the already-resolved {@code coatId}
     * and never looks at the coat again. That is how every ancestor in the
     * family tree came out the same black horse.
     *
     * <p>So the coat and the two texture ids have to move together, and they do
     * it here rather than in four screens that each got it right once and then
     * drifted. Baked at full detail - nothing on a screen is far away - but
     * still through the budget, so a pedigree of thirty strangers does not bake
     * thirty coats in one frame; the ones that have to wait show the stand-in
     * and are asked for again next frame.
     *
     * @param state a render state fresh from {@code createRenderState}; anything
     *              that is not one of ours is ignored
     * @param coat  the genome to wear, or {@code null} to leave the state alone
     */
    public static void applyCoat(EntityRenderState state, CoatData coat) {
        if (coat == null || !(state instanceof GeneticHorseRenderState geneticState)) {
            return;
        }
        geneticState.coatData = coat;
        GeneticCoatTextureFactory.Resolved textures = GeneticCoatTextureFactory.resolve(
                coat, geneticState.isBaby, CoatTextureComposer.glowParts(coat.genotype()),
                geneticState.breedLabel, true);
        geneticState.coatId = textures.coat();
        geneticState.emissiveCoatId = textures.glow();
    }

    /** The generated coat texture for one horse - shared with the family-tree node. */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        return coatTextureFor(coatData, baby, null);
    }

    public static Identifier coatTextureFor(CoatData coatData, boolean baby, String breedLabel) {
        return GeneticCoatTextureFactory.getOrCreate(coatData, baby, breedLabel);
    }
}
