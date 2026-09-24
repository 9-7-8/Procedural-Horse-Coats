package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Loot conditions this mod adds. There is one, {@link LootTableMatchesCondition},
 * and it is what lets the chest injections say "every chest table, any mod"
 * instead of listing ids.
 *
 * <p>As with {@link ModLootFunctions}, 26.1.2's registry holds the
 * {@code MapCodec} itself rather than a wrapper type - there is no
 * {@code LootItemConditionType} class any more - so that is what is registered.
 */
public final class ModLootConditions {

    public static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, HorseGenetics.MOD_ID);

    static {
        CONDITIONS.register("loot_table_matches", () -> LootTableMatchesCondition.MAP_CODEC);
    }

    public static void register(IEventBus modEventBus) {
        CONDITIONS.register(modEventBus);
    }

    private ModLootConditions() {
    }
}
