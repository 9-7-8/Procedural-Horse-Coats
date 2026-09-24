package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the mod's global loot modifiers - research papers, breed spawn eggs,
 * golden carrot seeds and horse tack into chests.
 *
 * <p>Three of the four reach <b>every</b> chest table in every namespace, via
 * {@link LootTableMatchesCondition} rather than a hand-written list of ids; the
 * breed spawn egg still names its ten, because a foundation horse turning up in
 * an arbitrary modded chest is a different question from a saddle doing so.
 */
public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HorseGenetics.MOD_ID);

    static {
        SERIALIZERS.register("add_research_paper", () -> AddResearchPaperModifier.CODEC);
        SERIALIZERS.register("add_breed_spawn_egg", () -> AddBreedSpawnEggModifier.CODEC);
        SERIALIZERS.register("add_golden_carrot_seeds", () -> AddGoldenCarrotSeedsModifier.CODEC);
        SERIALIZERS.register("add_horse_tack", () -> AddHorseTackModifier.CODEC);
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private ModLootModifiers() {
    }
}
