package com.example.horsegenetics.neoforge.world;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registers the mod's biome modifier types - see {@link BreedHerdsBiomeModifier}. */
public final class ModBiomeModifiers {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, HorseGenetics.MOD_ID);

    static {
        SERIALIZERS.register("breed_herds", () -> BreedHerdsBiomeModifier.CODEC);
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private ModBiomeModifiers() {
    }
}
