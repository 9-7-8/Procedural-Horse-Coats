package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.EnumSet;

/**
 * Vanilla's {@code HorseRenderer} is {@code final} in 26.1.2, so we extend
 * {@code AbstractHorseRenderer} directly and replicate its constructor.
 *
 * <p>Since the coat rework <b>every</b> horse - adult <i>and</i> foal - renders
 * a {@code GeneticCoatTextureFactory}-generated 128px texture, the adult on
 * {@link HdHorseModel} and the foal on {@link HdBabyHorseModel} (both 128px,
 * per-part UV). The models are handed straight to the super constructor as the
 * adult / baby model, so there's no per-entity model swap.
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
                state -> state.saddle,
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
            geneticState.emissiveCoatId = emissiveCoatFor(geneticState.coatData, renderState.isBaby);
            com.example.horsegenetics.common.horse.HorseRecord rec =
                    ClientHorseRecordCache.get(horse.getId());
            geneticState.breedLabel = rec == null ? null : rec.lineage().displayName();
        }
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

    /**
     * The full-bright mask for whatever this horse glows with, or {@code null}
     * if nothing does. Cheap: the ability scan is a handful of allele checks and
     * the bake is cached by coat key - including the "nothing glows" answer, so
     * an ordinary horse costs one map lookup a frame.
     *
     * <p>The factory is asked <b>every</b> time, even with no {@code glow}
     * effect in the list, because a built-in gene writes its emissive texels in
     * the coat bake rather than declaring body parts here - which is what lets
     * the light locus glow four hooves and two eyes instead of four whole legs
     * and a head.
     */
    private static Identifier emissiveCoatFor(CoatData coatData, boolean baby) {
        EnumSet<Part> parts = EnumSet.noneOf(Part.class);
        for (HorseAbilities.Active active : HorseAbilities.activeFor(coatData.genotype())) {
            if (active.ability() instanceof GeneAbility.Glow glow) {
                parts.addAll(glow.emissiveParts());
            }
        }
        return GeneticCoatTextureFactory.getOrCreateEmissive(coatData, baby, parts);
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState renderState) {
        CoatData coatData = (renderState instanceof GeneticHorseRenderState geneticState)
                ? geneticState.coatData
                : CoatData.DEFAULT;
        String breedLabel = (renderState instanceof GeneticHorseRenderState g) ? g.breedLabel : null;
        return coatTextureFor(coatData, renderState.isBaby, breedLabel);
    }

    /** The generated coat texture for one horse - shared with the family-tree node. */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        return coatTextureFor(coatData, baby, null);
    }

    public static Identifier coatTextureFor(CoatData coatData, boolean baby, String breedLabel) {
        return GeneticCoatTextureFactory.getOrCreate(coatData, baby, breedLabel);
    }
}
