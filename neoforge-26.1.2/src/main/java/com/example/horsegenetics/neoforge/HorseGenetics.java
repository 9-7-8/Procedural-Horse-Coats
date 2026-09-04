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
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Server-side: how much of the health genetics this world plays with.
        // Whether a foal dies has to be one answer for everyone on a server.
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        // HorseGeneticsEventHandler, ModNetworking, ClientSetup, DebugKeyBindings,
        // DebugKeyHandler, and DebugPenTickHandler are all @EventBusSubscriber-
        // annotated and pick themselves up automatically - nothing else to wire
        // here for this MVP.
    }
}
