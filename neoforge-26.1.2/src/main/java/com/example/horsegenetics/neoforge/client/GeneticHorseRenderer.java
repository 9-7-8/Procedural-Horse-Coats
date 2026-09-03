package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.SpecAbilities;
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
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            CoatData coatData = ClientCoatCache.get(horse.getId());
            if (coatData != null) {
                geneticState.coatData = coatData;
            }
            geneticState.emissiveCoatId = emissiveCoatFor(geneticState.coatData, renderState.isBaby);
        }
    }

    /**
     * The full-bright mask for whatever {@code glow} genes this horse expresses,
     * or {@code null} if none want an emissive region. Cheap: the ability scan is
     * a handful of allele checks and the bake itself is cached by coat key.
     */
    private static Identifier emissiveCoatFor(CoatData coatData, boolean baby) {
        EnumSet<Part> parts = EnumSet.noneOf(Part.class);
        for (SpecAbilities.Active active : SpecAbilities.activeFor(coatData.genotype())) {
            if (active.ability() instanceof GeneAbility.Glow glow) {
                parts.addAll(glow.emissiveParts());
            }
        }
        return parts.isEmpty() ? null : GeneticCoatTextureFactory.getOrCreateEmissive(coatData, baby, parts);
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState renderState) {
        CoatData coatData = (renderState instanceof GeneticHorseRenderState geneticState)
                ? geneticState.coatData
                : CoatData.DEFAULT;
        return coatTextureFor(coatData, renderState.isBaby);
    }

    /** The generated coat texture for one horse - shared with the family-tree node. */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        return GeneticCoatTextureFactory.getOrCreate(coatData, baby);
    }
}
