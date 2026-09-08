package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Loot-item functions this mod adds. One: {@link SetRandomGeneFunction}, which
 * is what lets the horseman's data-driven trades sell "a random uncommon gene
 * carrot" without a line of trade-generation code.
 *
 * <p>{@link SetRandomBreedFunction} is the same trick for breed spawn eggs, and
 * it is what lets the horseman stock "a rare breed's egg" without a listing
 * class and without naming a breed the player may have switched off.
 *
 * <p>The registry holds the {@code MapCodec} itself rather than a wrapper type,
 * so that is what is registered.
 */
public final class ModLootFunctions {

    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, HorseGenetics.MOD_ID);

    static {
        FUNCTIONS.register("set_random_gene", () -> SetRandomGeneFunction.MAP_CODEC);
        FUNCTIONS.register("set_random_breed", () -> SetRandomBreedFunction.MAP_CODEC);
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    private ModLootFunctions() {
    }
}
