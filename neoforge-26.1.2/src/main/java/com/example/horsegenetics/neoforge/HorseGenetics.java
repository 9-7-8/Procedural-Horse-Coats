package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(HorseGenetics.MOD_ID)
public final class HorseGenetics {

    public static final String MOD_ID = "horsegenetics";

    public HorseGenetics(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // HorseGeneticsEventHandler, ModNetworking, and ClientSetup are
        // @EventBusSubscriber-annotated and pick themselves up automatically -
        // nothing else to wire here for this MVP.
    }
}
