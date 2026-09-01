package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.ModBlockEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientSetup {

    /**
     * Baked layer for {@link HdHorseModel} - the 128px, non-mirrored horse
     * geometry used (for now) only by white horses. Registered below; consumed
     * by {@link GeneticHorseRenderer} via {@code context.bakeLayer(...)}.
     */
    public static final ModelLayerLocation HD_HORSE = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "hd_horse"), "main");
    public static final ModelLayerLocation HD_HORSE_BABY = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "hd_horse_baby"), "main");

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HD_HORSE, HdHorseModel::createHdLayer);
        event.registerLayerDefinition(HD_HORSE_BABY, HdBabyHorseModel::createHdLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.HORSE, GeneticHorseRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HAY_PORTAL.get(), ctx -> new HayPortalRenderer());
    }

    private ClientSetup() {
    }
}
