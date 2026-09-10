package com.example.horsegenetics.neoforge.village;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The <b>horseman</b> - an ordinary villager profession that sells horse tack
 * and horse-genetics goods from a Horseman's Table.
 *
 * <p>Unlike the {@code Cowboy}, this one <i>wants</i> vanilla's brain: it is a
 * shopkeeper who should walk to a workstation in the morning, restock at it,
 * gossip, panic at a raid and go to bed. Everything about that is free once the
 * profession points at a POI.
 *
 * <h2>Where the trades live</h2>
 * Trades are fully data-driven in this version - {@code minecraft:villager_trade}
 * and {@code minecraft:trade_set} are datapack registries - so the profession
 * only names five {@link TradeSet} keys and the five tier files under
 * {@code data/horsegenetics/trade_set/horseman/} say what is in them. There is
 * no {@code ItemListing} to write and no event to hook.
 *
 * <p>The tiers follow <b>vanilla's</b> ladder - Novice / Apprentice /
 * Journeyman / Expert / Master - not the design document's, which invented an
 * "Adept" rung that does not exist.
 *
 * <h2>Registration</h2>
 * {@code Registries.VILLAGER_PROFESSION} is a built-in registry (vanilla fills
 * it from {@code VillagerProfession.bootstrap}), so this is a
 * {@link DeferredRegister} and not a JSON file - the opposite of the trades it
 * points at.
 */
public final class ModVillagerProfessions {

    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, HorseGenetics.MOD_ID);

    private static ResourceKey<TradeSet> tradeSet(String path) {
        return ResourceKey.create(Registries.TRADE_SET,
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, path));
    }

    public static final ResourceKey<TradeSet> HORSEMAN_LEVEL_1 = tradeSet("horseman/level_1");
    public static final ResourceKey<TradeSet> HORSEMAN_LEVEL_2 = tradeSet("horseman/level_2");
    public static final ResourceKey<TradeSet> HORSEMAN_LEVEL_3 = tradeSet("horseman/level_3");
    public static final ResourceKey<TradeSet> HORSEMAN_LEVEL_4 = tradeSet("horseman/level_4");
    public static final ResourceKey<TradeSet> HORSEMAN_LEVEL_5 = tradeSet("horseman/level_5");

    public static final DeferredHolder<VillagerProfession, VillagerProfession> HORSEMAN =
            PROFESSIONS.register("horseman", () -> new VillagerProfession(
                    Component.translatable("entity.horsegenetics.villager.horseman"),
                    poi -> poi.is(ModPoiTypes.HORSEMANS_TABLE_KEY),
                    poi -> poi.is(ModPoiTypes.HORSEMANS_TABLE_KEY),
                    ImmutableSet.of(),  // nothing they pick up off the ground
                    ImmutableSet.of(),  // no secondary POI (a farmer's farmland, a fisherman's water)
                    SoundEvents.VILLAGER_WORK_LEATHERWORKER,
                    Int2ObjectMap.ofEntries(
                            Int2ObjectMap.entry(1, HORSEMAN_LEVEL_1),
                            Int2ObjectMap.entry(2, HORSEMAN_LEVEL_2),
                            Int2ObjectMap.entry(3, HORSEMAN_LEVEL_3),
                            Int2ObjectMap.entry(4, HORSEMAN_LEVEL_4),
                            Int2ObjectMap.entry(5, HORSEMAN_LEVEL_5))));

    public static void register(IEventBus modEventBus) {
        PROFESSIONS.register(modEventBus);
    }

    private ModVillagerProfessions() {
    }
}
