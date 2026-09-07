package com.example.horsegenetics.neoforge.village;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Points of interest this mod adds. One: the <b>Horse Trader's Post</b>, the
 * horseman's job site.
 *
 * <p>A POI is only half the wiring. It has to be in the
 * {@code minecraft:acquirable_job_site} block-entity tag before an unemployed
 * villager will take the job, and that half is a tag file
 * ({@code data/minecraft/tags/point_of_interest_type/acquirable_job_site.json})
 * - tags merge across datapacks, so adding to vanilla's is additive and safe
 * where overriding a worldgen file would not be.
 *
 * <p>{@code maxTickets 1} the way every vanilla job site is: one villager per
 * post, so a second horseman needs a second post.
 */
public final class ModPoiTypes {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, HorseGenetics.MOD_ID);

    public static final ResourceKey<PoiType> HORSE_TRADERS_POST_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_traders_post"));

    public static final DeferredHolder<PoiType, PoiType> HORSE_TRADERS_POST =
            POI_TYPES.register("horse_traders_post", () -> new PoiType(
                    Set.copyOf(ModBlocks.HORSE_TRADERS_POST.get().getStateDefinition().getPossibleStates()),
                    1,   // one horseman per post
                    1)); // valid range, in blocks - vanilla's job sites all use 1

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
    }

    private ModPoiTypes() {
    }
}
