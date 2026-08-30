package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HorseGenetics.MOD_ID)
public final class HorseGenetics {

    public static final String MOD_ID = "horsegenetics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public HorseGenetics(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // HorseGeneticsEventHandler, ModNetworking, ClientSetup, DebugKeyBindings,
        // DebugKeyHandler, and DebugPenTickHandler are all @EventBusSubscriber-
        // annotated and pick themselves up automatically - nothing else to wire
        // here for this MVP.
    }
}
