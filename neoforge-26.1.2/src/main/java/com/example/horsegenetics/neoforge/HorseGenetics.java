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
        // A player's own trader names. Order-free - a name changes no registry
        // and lengthens no genotype code - but it reads Region, so it sits with
        // the breeds rather than above them. phc/names/.
        ModPersonNames.load();
        // The world's say over the shipped breeds - built from the registry, so
        // it has to come after the load above. phc/breed-spawning.toml.
        BreedSpawningConfig.register(modContainer, modEventBus);
        // The diet locus names categories in common/ and items here; this is
        // the only thing that checks the two agree, and the failure it catches
        // is silent (a horse that would simply never accept anything).
        com.example.horsegenetics.neoforge.server.DietFoods.verify();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModDataComponents.register(modEventBus);
        // What the OTHER mods in this pack brought - the woods we can make a
        // double gate for, the ingots we can make horse armour out of. Read out
        // of their jars, because at this moment no registry is populated and no
        // tag exists; and necessarily HERE, because a DeferredRegister cannot be
        // handed an entry once the bus has it. See compat/ModdedMaterials.
        com.example.horsegenetics.neoforge.compat.ModdedMaterials.scan();
        // A horse armour per modded ingot. Same rule as the gates below: the
        // class registers as it loads, so it has to be touched before ModItems
        // is attached to the bus. The models, recipes and trades all of this
        // needs are written later, by compat/GeneratedPack.
        com.example.horsegenetics.neoforge.compat.ModdedArmour.init();
        // The double gates register themselves into ModBlocks.BLOCKS and
        // ModItems.ITEMS as this class loads, so it has to be touched before
        // either register is attached to the bus below - and after the scan
        // above, which is where its modded woods come from.
        com.example.horsegenetics.neoforge.block.DoubleGates.init();
        // Same contract, same reason: the jumps register themselves as this
        // class loads, so touch it before the registers go on the bus.
        com.example.horsegenetics.neoforge.block.Jumps.init();
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        com.example.horsegenetics.neoforge.particle.ModParticles.register(modEventBus);
        com.example.horsegenetics.neoforge.menu.ModMenus.register(modEventBus);
        ModItems.register(modEventBus);
        com.example.horsegenetics.neoforge.entity.ModEntities.register(modEventBus);
        com.example.horsegenetics.neoforge.entity.ModAttributes.register(modEventBus);
        com.example.horsegenetics.neoforge.village.ModPoiTypes.register(modEventBus);
        com.example.horsegenetics.neoforge.village.ModVillagerProfessions.register(modEventBus);
        com.example.horsegenetics.neoforge.server.recipe.ModRecipes.register(modEventBus);
        com.example.horsegenetics.neoforge.data.loot.ModLootModifiers.register(modEventBus);
        com.example.horsegenetics.neoforge.data.loot.ModLootFunctions.register(modEventBus);
        com.example.horsegenetics.neoforge.world.ModBiomeModifiers.register(modEventBus);
        // The one criterion trigger behind every advancement this mod ships -
        // all of which are baked off ProgressTask. See ModTriggers.
        com.example.horsegenetics.neoforge.advancement.ModTriggers.register(modEventBus);
        // In-game tests. Registration is a no-op unless gametests are enabled
        // (dev only), so this costs a shipped server nothing. See ModGameTests -
        // 26.1.2 has no @GameTest annotation and the two-half registration there
        // is not optional.
        com.example.horsegenetics.neoforge.gametest.ModGameTests.register(modEventBus);
        // Both settings files live in .minecraft/phc/ beside the breeds folder,
        // not in config/ - see ModBreedSpecs.configFile for how, and why.
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, ModBreedSpecs.configFile("client.toml"));
        // Server-side: how much of the health genetics this world plays with.
        // Whether a foal dies has to be one answer for everyone on a server.
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, ModBreedSpecs.configFile("server.toml"));
        // The cart system (horse-drawn wagons, plows and field implements),
        // derived from UsefulCarts under the MIT licence - see
        // THIRD_PARTY_NOTICES.md. It registers through RegisterEvent rather than
        // DeferredRegister, and deliberately: that event fires after the
        // ModdedMaterials.scan() above, so a cart in every modded wood needs no
        // class-load trickery the way DoubleGates and ModdedArmour do.
        // Position in this constructor therefore does not matter; it is here at
        // the end because it registers nothing on the buses attached above.
        // The client half is CartsClient, which picks itself up.
        com.example.horsegenetics.neoforge.carts.HorseCarts.init(modEventBus, modContainer);
        // One log line if an animal-labour mod is installed, saying how to make
        // it respect horse genetics. Reads nothing, patches nothing.
        com.example.horsegenetics.neoforge.compat.HorsePoweredCompat.announce();
        // HorseGeneticsEventHandler, ModNetworking, ClientSetup, DebugKeyBindings,
        // DebugKeyHandler, and DebugPenTickHandler are all @EventBusSubscriber-
        // annotated and pick themselves up automatically - nothing else to wire
        // here for this MVP.
    }
}
