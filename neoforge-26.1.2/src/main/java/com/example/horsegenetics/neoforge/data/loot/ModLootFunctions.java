package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Loot-item functions this mod adds. The first, {@link SetRandomGeneFunction},
 * is what lets the equestrians' data-driven trades sell "a random uncommon gene
 * carrot" without a line of trade-generation code.
 *
 * <p>{@link SetRandomBreedFunction} is the same trick for breed spawn eggs, and
 * it is what lets the scientist stock "a rare breed's egg" without a listing
 * class and without naming a breed the player may have switched off.
 *
 * <p>{@link SetRandomDyeFunction} is the third, and the cheapest: it stamps the
 * vanilla {@code dyed_color} component, so "a red leather armour" costs one
 * trade file rather than sixteen items - and depends on no other mod, because
 * the component it writes is vanilla's.
 *
 * <p>{@link SetTackTintFunction} is the fourth and the only one that writes a
 * component of ours. It stamps {@code tack_tint}, which carries three zones and
 * therefore reaches the saddle and the metal fittings that {@code dyed_color}
 * cannot - which is what makes the leatherworker's five tiers visibly different
 * from one another.
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
        FUNCTIONS.register("set_random_dye", () -> SetRandomDyeFunction.MAP_CODEC);
        FUNCTIONS.register("set_tack_tint", () -> SetTackTintFunction.MAP_CODEC);
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    private ModLootFunctions() {
    }
}
