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
    /** This mod's menus: the Equine Research Shelf's, and the Tack Dyeing Bench's. */
    /**
     * <b>The jump's two-wood model.</b> The first custom block model in the mod.
     *
     * <p>26.1.2 has no {@code BakedModel} and no {@code IDynamicBakedModel} -
     * both were removed - so this is NOT the shape any tutorial will show. The
     * interface is {@code BlockStateModel}, the per-position hook is NeoForge's
     * {@code DynamicBlockStateModel}, and a model is selected by a
     * {@code "type"} field inside a blockstate variant, dispatched through this
     * event. See {@link JumpModel}.
     */
    @SubscribeEvent
    static void registerBlockStateModels(
            net.neoforged.neoforge.client.event.RegisterBlockStateModels event) {
        event.registerModel(JumpModel.ID, JumpModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.example.horsegenetics.neoforge.menu.ModMenus.RESEARCH_SHELF.get(),
                ResearchShelfScreen::new);
        event.register(com.example.horsegenetics.neoforge.menu.ModMenus.EQUESTRIAN_BENCH.get(),
                EquestrianBenchScreen::new);
        event.register(com.example.horsegenetics.neoforge.menu.ModMenus.JUMP.get(),
                JumpScreen::new);
    }

    /** Molten hooves' glowing prints - see {@link HoofprintParticle}. */
    @SubscribeEvent
    static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.example.horsegenetics.neoforge.particle.ModParticles.HOOFPRINT.get(),
                HoofprintParticle.Provider::new);
    }

    /**
     * The saddle's three dyeable zones. Vanilla gives one dye colour per stack
     * however many layers an equipment asset declares, so the seat, the bridle
     * and the metal reach their own colours through this hook - see
     * {@link TackClientExtensions}. The first client item extension this mod has
     * registered.
     */
    @SubscribeEvent
    static void registerClientExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new TackClientExtensions(),
                net.minecraft.world.item.Items.SADDLE,
                net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR);
    }

    /**
     * The saddle icon reads our three-colour component rather than vanilla's
     * single {@code dyed_color} - see {@link TackTintSource}. Registered under
     * the id {@code horsegenetics:tack_tint}, which is what
     * {@code assets/minecraft/items/saddle.json} names.
     */
    @SubscribeEvent
    static void registerItemTintSources(
            net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "tack_tint"),
                TackTintSource.MAP_CODEC);
        // The icon half of a generated horse armour's colour - the worn half is
        // the equipment asset's own dyeable layer. See MetalTintSource.
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "metal_tint"),
                MetalTintSource.MAP_CODEC);
        // The golden carrot seed icon, which is vanilla's wheat seeds in gold.
        // See GoldTintSource, and registerBlockColours below for the other half.
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "gold_tint"),
                GoldTintSource.MAP_CODEC);
        // A jump's two coats of paint on its icon, one per half. The ids are
        // what the tints[] array in assets/horsegenetics/items/jump.json names,
        // and their ORDER there is what decides which half each colours - see
        // JumpTintSource.
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        HorseGenetics.MOD_ID, "jump_rails_tint"),
                JumpTintSource.Item.RAILS_CODEC);
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        HorseGenetics.MOD_ID, "jump_standards_tint"),
                JumpTintSource.Item.STANDARDS_CODEC);
    }

    /**
     * The planted golden carrot crop, coloured.
     *
     * <p>Its stage models are vanilla's carrot sheets with a {@code tintindex}
     * added - that index is what this handler answers, and a model without one
     * would simply render orange carrots however loud this shouted. The colour
     * is {@link GoldTintSource#GOLD}, shared with the item tint so the seed in
     * your hand and the crop in the ground are the same gold.
     *
     * <p>The mod's first block tint. 26.1.2 has no lambda-per-tint-index block
     * colour handler: the event takes a <b>list</b> of
     * {@link net.minecraft.client.color.block.BlockTintSource}, indexed by the
     * model's tint index, and {@code BlockTintSource} is a one-method interface
     * over the block state. One entry, so index 0, which is the index the stage
     * models carry.
     */
    @SubscribeEvent
    static void registerBlockTintSources(
            net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                java.util.List.of(state -> GoldTintSource.GOLD),
                com.example.horsegenetics.neoforge.block.ModBlocks.GOLDEN_CARROT_CROP.get());
        // THE JUMP'S TWO HALVES, IN TINT-INDEX ORDER: rails 0, standards 1.
        // Two entries rather than one, which is the whole reason a jump can be
        // painted per part at all - the list index IS the model's tintindex,
        // and the two halves are already separate model parts.
        event.register(JumpTintSource.BLOCK_SOURCES,
                com.example.horsegenetics.neoforge.block.ModBlocks.JUMP.get());
    }

    @SubscribeEvent
    static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(CoatAssetReload.ID, new CoatAssetReload());
    }

    private ClientSetup() {
    }
}
