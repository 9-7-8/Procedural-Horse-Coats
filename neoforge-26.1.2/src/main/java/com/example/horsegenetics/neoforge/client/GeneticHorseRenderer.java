package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * Vanilla's {@code HorseRenderer} is {@code final} in 26.1.2, so we extend
 * {@code AbstractHorseRenderer} directly and replicate its constructor
 * (marking + equipment layers, copied from vanilla {@code HorseRenderer}).
 *
 * <p>Since the allele/overlay rework, <b>every adult horse</b> renders with
 * {@link HdHorseModel} (128px, non-mirrored UVs) and a coat texture generated
 * per genotype by {@link GeneticCoatTextureFactory}. So we simply hand the HD
 * model to the super constructor as the adult model - no per-entity model swap
 * any more. Foals keep the vanilla {@code BabyHorseModel} + the vanilla
 * {@code *_baby} textures (the baby UV layout isn't covered by
 * {@code HorseSkinGeometry}).
 */
public class GeneticHorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {

    private static final Identifier BROWN_BABY_TEXTURE = vanilla("horse_brown_baby");
    private static final Identifier BLACK_BABY_TEXTURE = vanilla("horse_black_baby");
    private static final Identifier CHESTNUT_BABY_TEXTURE = vanilla("horse_chestnut_baby");
    private static final Identifier WHITE_BABY_TEXTURE = vanilla("horse_white_baby");

    private static Identifier vanilla(String name) {
        return Identifier.withDefaultNamespace("textures/entity/horse/" + name + ".png");
    }

    public GeneticHorseRenderer(EntityRendererProvider.Context context) {
        super(context,
                new HdHorseModel(context.bakeLayer(ClientSetup.HD_HORSE)),
                new BabyHorseModel(context.bakeLayer(ModelLayers.HORSE_BABY)));
        this.addLayer(new HorseMarkingLayer(this));
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
        }
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState renderState) {
        CoatData coatData = (renderState instanceof GeneticHorseRenderState geneticState)
                ? geneticState.coatData
                : CoatData.DEFAULT;
        return coatTextureFor(coatData, renderState.isBaby);
    }

    /**
     * The coat texture for one horse - shared with the family-tree node.
     * Adults: the generated overlay texture. Foals: the nearest vanilla
     * {@code *_baby} texture (no procedural coat on the baby model yet).
     */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        if (!baby) {
            return GeneticCoatTextureFactory.getOrCreate(coatData);
        }
        return switch (coatData.phenotype()) {
            case BAY -> BROWN_BABY_TEXTURE;
            case BLACK -> BLACK_BABY_TEXTURE;
            case CHESTNUT -> CHESTNUT_BABY_TEXTURE;
            case WHITE -> WHITE_BABY_TEXTURE;
        };
    }
}
