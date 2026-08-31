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
 * Vanilla's {@code HorseRenderer} is {@code final} in 26.1.2, so we cannot
 * subclass it (CLAUDE.md flagged this as the likely outcome). Instead we
 * extend {@code AbstractHorseRenderer} directly and replicate vanilla's
 * constructor (models + marking/equipment layers, copied verbatim from
 * {@code net.minecraft.client.renderer.entity.HorseRenderer}), then override
 * the three hook points we care about.
 *
 * <p>The render state generic stays vanilla's {@link HorseRenderState} so the
 * copied layers type-check unchanged; {@link #createRenderState()} covariantly
 * returns our {@link GeneticHorseRenderState} subclass, and because
 * {@code EntityRenderer.createRenderState(entity, partialTicks)} always routes
 * through that method, every state instance is really a
 * {@code GeneticHorseRenderState} - the {@code instanceof} checks below never
 * fail in practice.
 */
public class GeneticHorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {

    private static final Identifier BLACK_TEXTURE = vanilla("horse_black");
    private static final Identifier CHESTNUT_TEXTURE = vanilla("horse_chestnut");
    private static final Identifier BROWN_TEXTURE = vanilla("horse_brown");
    private static final Identifier WHITE_TEXTURE = vanilla("horse_white");
    // Foals use a distinct model (BabyHorseModel) with its own UV layout, so
    // they need the vanilla *_baby texture. We don't composite bay legs onto
    // foals - they grow up quickly and re-extract the adult texture then.
    private static final Identifier BLACK_BABY_TEXTURE = vanilla("horse_black_baby");
    private static final Identifier CHESTNUT_BABY_TEXTURE = vanilla("horse_chestnut_baby");
    private static final Identifier BROWN_BABY_TEXTURE = vanilla("horse_brown_baby");
    private static final Identifier WHITE_BABY_TEXTURE = vanilla("horse_white_baby");

    private static Identifier vanilla(String name) {
        return Identifier.withDefaultNamespace("textures/entity/horse/" + name + ".png");
    }

    public GeneticHorseRenderer(EntityRendererProvider.Context context) {
        super(context, new HorseModel(context.bakeLayer(ModelLayers.HORSE)), new BabyHorseModel(context.bakeLayer(ModelLayers.HORSE_BABY)));
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
                : null;
        if (coatData == null) {
            return renderState.isBaby ? BROWN_BABY_TEXTURE : BROWN_TEXTURE;
        }
        return coatTextureFor(coatData, renderState.isBaby);
    }

    /** The coat texture for a phenotype - shared with the family-tree coat swatch. */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        if (baby) {
            return switch (coatData.phenotype()) {
                case BAY -> BROWN_BABY_TEXTURE;      // bay's adult base is brown; no leg compositing on foals
                case BLACK -> BLACK_BABY_TEXTURE;
                case CHESTNUT -> CHESTNUT_BABY_TEXTURE;
                case WHITE -> WHITE_BABY_TEXTURE;
            };
        }
        return switch (coatData.phenotype()) {
            case BAY -> GeneticCoatTextureFactory.getOrCreate(coatData);
            case BLACK -> BLACK_TEXTURE;
            case CHESTNUT -> CHESTNUT_TEXTURE;
            case WHITE -> WHITE_TEXTURE;
        };
    }
}
