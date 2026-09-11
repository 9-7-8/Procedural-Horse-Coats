package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.ModBlockEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

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
        event.registerEntityRenderer(
                com.example.horsegenetics.neoforge.entity.ModEntities.COWBOY.get(), CowboyRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HAY_PORTAL.get(), ctx -> new HayPortalRenderer());
    }

    /**
     * The signed transfer paper draws as a small model of the horse it names
     * rather than as a picture of a paper - see {@link TransferDeedRenderer}.
     * An item model can only reach a special renderer by id, so the id has to be
     * registered here before {@code items/signed_transfer_paper.json} can name
     * it.
     */
    @SubscribeEvent
    static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(TransferDeedRenderer.ID, TransferDeedRenderer.Unbaked.MAP_CODEC);
    }

    /**
     * The generated coats are composed from pack resources, so a resource
     * reload has to be able to throw them away - see {@link CoatAssetReload}.
     */
    /** The one menu this mod has: the Equine Research Shelf's. */
    @SubscribeEvent
    static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.example.horsegenetics.neoforge.menu.ModMenus.RESEARCH_SHELF.get(),
                ResearchShelfScreen::new);
    }

    /** Molten hooves' glowing prints - see {@link HoofprintParticle}. */
    @SubscribeEvent
    static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.example.horsegenetics.neoforge.particle.ModParticles.HOOFPRINT.get(),
                HoofprintParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(CoatAssetReload.ID, new CoatAssetReload());
    }

    private ClientSetup() {
    }
}
