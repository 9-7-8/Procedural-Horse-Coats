package com.example.horsegenetics.neoforge.village;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The <b>four equestrians</b> - ordinary villager professions who between them
 * sell what a horse operation needs, each from a post of their own.
 *
 * <ul>
 *   <li><b>Leatherworker</b> - saddles and leather horse armour, dyed more
 *       exotically the higher his tier.</li>
 *   <li><b>Scientist</b> - breeding carrots, the vet kit and breed eggs; buys
 *       stallion seed jars as research stock.</li>
 *   <li><b>Supplier</b> - carrots, leads, whistles, tickets, papers, and the
 *       <i>empty</i> seed jar. The general store.</li>
 *   <li><b>Metalsmith</b> - ingot and crystal horse armour, and nothing else.</li>
 * </ul>
 *
 * <p>These four <b>replace</b> a single {@code horsegenetics:horseman}. There is
 * no transitional profession and no alias: a villager who held the old job in an
 * existing world comes back unemployed, which is hard rule 6 working as intended.
 *
 * <p>Unlike the {@code Cowboy}, all four <i>want</i> vanilla's brain: they are
 * shopkeepers who should walk to a workstation in the morning, restock at it,
 * gossip, panic at a raid and go to bed. Everything about that is free once a
 * profession points at a POI.
 *
 * <h2>Why four posts and not one</h2>
 * A POI maps to exactly one profession - vanilla picks the job from the job site
 * claimed, not the other way round - so four jobs need four blocks. They are the
 * same block four times over for now, down to the texture; see
 * {@code block/ModBlocks}.
 *
 * <h2>Where the trades live</h2>
 * Trades are fully data-driven in this version - {@code minecraft:villager_trade}
 * and {@code minecraft:trade_set} are datapack registries - so a profession only
 * names five {@link TradeSet} keys and the tier files under
 * {@code data/horsegenetics/trade_set/<profession>/} say what is in them. There
 * is no {@code ItemListing} to write and no event to hook.
 *
 * <p>The tiers follow <b>vanilla's</b> ladder - Novice / Apprentice /
 * Journeyman / Expert / Master.
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

    /**
     * One profession, its five tier files named by convention off its own id.
     *
     * <p>The convention is load-bearing in both directions: the trade files live
     * at {@code trade_set/<id>/level_N}, and the profession texture vanilla looks
     * up is {@code horsegenetics:textures/entity/villager/profession/<id>.png}.
     * Rename one of these and two other things move with it.
     */
    private static DeferredHolder<VillagerProfession, VillagerProfession> profession(
            String id, ResourceKey<net.minecraft.world.entity.ai.village.poi.PoiType> poi, SoundEvent workSound) {
        return PROFESSIONS.register(id, () -> new VillagerProfession(
                Component.translatable("entity.horsegenetics.villager." + id),
                held -> held.is(poi),
                acquirable -> acquirable.is(poi),
                ImmutableSet.of(),  // nothing they pick up off the ground
                ImmutableSet.of(),  // no secondary POI (a farmer's farmland, a fisherman's water)
                workSound,
                Int2ObjectMap.ofEntries(
                        Int2ObjectMap.entry(1, tradeSet(id + "/level_1")),
                        Int2ObjectMap.entry(2, tradeSet(id + "/level_2")),
                        Int2ObjectMap.entry(3, tradeSet(id + "/level_3")),
                        Int2ObjectMap.entry(4, tradeSet(id + "/level_4")),
                        Int2ObjectMap.entry(5, tradeSet(id + "/level_5")))));
    }

    public static final DeferredHolder<VillagerProfession, VillagerProfession> LEATHERWORKER =
            profession("equestrian_leatherworker", ModPoiTypes.LEATHERWORKERS_POST_KEY,
                    SoundEvents.VILLAGER_WORK_LEATHERWORKER);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> SCIENTIST =
            profession("equestrian_scientist", ModPoiTypes.SCIENTISTS_POST_KEY,
                    SoundEvents.VILLAGER_WORK_CLERIC);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> SUPPLIER =
            profession("equestrian_supplier", ModPoiTypes.SUPPLIERS_POST_KEY,
                    SoundEvents.VILLAGER_WORK_FARMER);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> METALSMITH =
            profession("equestrian_metalsmith", ModPoiTypes.METALSMITHS_POST_KEY,
                    SoundEvents.VILLAGER_WORK_ARMORER);

    /**
     * <b>All four, for anything that asks "is this one of ours?"</b> - the family
     * name scan, the tutorial, the debug level-up. Kept here rather than written
     * out at each call site because a fifth equestrian must not be able to join
     * the roster and be silently left out of the family.
     */
    private static final List<DeferredHolder<VillagerProfession, VillagerProfession>> ALL =
            List.of(LEATHERWORKER, SCIENTIST, SUPPLIER, METALSMITH);

    public static List<DeferredHolder<VillagerProfession, VillagerProfession>> all() {
        return ALL;
    }

    /** Whether {@code profession} is one of this mod's four. */
    public static boolean isEquestrian(net.minecraft.core.Holder<VillagerProfession> profession) {
        for (DeferredHolder<VillagerProfession, VillagerProfession> ours : ALL) {
            if (profession.is(ours.getKey())) {
                return true;
            }
        }
        return false;
    }

    public static void register(IEventBus modEventBus) {
        PROFESSIONS.register(modEventBus);
    }

    private ModVillagerProfessions() {
    }
}
