package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.neoforge.block.ModBlockEntities;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HorseGenetics.MOD_ID)
public final class HorseGenetics {

    public static final String MOD_ID = "horsegenetics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public HorseGenetics(IEventBus modEventBus, ModContainer modContainer) {
        // First, before anything can parse a genotype code: each drop-in gene
        // adds a segment to that code, so registering one late would invalidate
        // codes already read. See ModGeneSpecs.
        ModGeneSpecs.load();
        // ...then anyone else's, once every mod has been constructed. The event
        // is deferred to exactly one point; see GeneRegistration, which also
        // freezes the registry the moment it is done.
        com.example.horsegenetics.neoforge.api.GeneRegistration.listen(modEventBus);
        // Then the breeds, which are mostly references to the genes above - a
        // breed loaded first would report every drop-in gene as missing.
        // Unlike a gene, a breed does not lengthen the genotype code, so this
        // one is not order-critical against anything else.
        ModBreedSpecs.load();
        // The world's say over the shipped breeds - built from the registry, so
        // it has to come after the load above. phc/breed-spawning.toml.
        BreedSpawningConfig.register(modContainer, modEventBus);
        // The diet locus names categories in common/ and items here; this is
        // the only thing that checks the two agree, and the failure it catches
        // is silent (a horse that would simply never accept anything).
        com.example.horsegenetics.neoforge.server.DietFoods.verify();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        com.example.horsegenetics.neoforge.particle.ModParticles.register(modEventBus);
        com.example.horsegenetics.neoforge.menu.ModMenus.register(modEventBus);
        ModItems.register(modEventBus);
        com.example.horsegenetics.neoforge.entity.ModEntities.register(modEventBus);
        com.example.horsegenetics.neoforge.village.ModPoiTypes.register(modEventBus);
        com.example.horsegenetics.neoforge.village.ModVillagerProfessions.register(modEventBus);
        com.example.horsegenetics.neoforge.server.recipe.ModRecipes.register(modEventBus);
        com.example.horsegenetics.neoforge.data.loot.ModLootModifiers.register(modEventBus);
        com.example.horsegenetics.neoforge.data.loot.ModLootFunctions.register(modEventBus);
        com.example.horsegenetics.neoforge.world.ModBiomeModifiers.register(modEventBus);
        // Both settings files live in .minecraft/phc/ beside the breeds folder,
        // not in config/ - see ModBreedSpecs.configFile for how, and why.
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, ModBreedSpecs.configFile("client.toml"));
        // Server-side: how much of the health genetics this world plays with.
        // Whether a foal dies has to be one answer for everyone on a server.
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, ModBreedSpecs.configFile("server.toml"));
        // HorseGeneticsEventHandler, ModNetworking, ClientSetup, DebugKeyBindings,
        // DebugKeyHandler, and DebugPenTickHandler are all @EventBusSubscriber-
        // annotated and pick themselves up automatically - nothing else to wire
        // here for this MVP.
    }
}
