package com.example.horsegenetics.neoforge.village;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Points of interest this mod adds: <b>one post per equestrian</b>, four in all.
 *
 * <p>Four rather than one because a POI maps to exactly one profession - vanilla
 * decides the job from the job site claimed - so a single shared post could only
 * ever hand out a single job. See {@link ModVillagerProfessions}.
 *
 * <p>The <b>Cowboy Hitch</b> is deliberately <i>not</i> here. It is not a job
 * site - no profession claims it, because a cowboy is an entity and not a
 * profession - so it is found by looking for the block rather than by asking the
 * POI system. See {@code server/CowboyHitchHandler}. <b>Nor may it be named in a
 * POI tag</b>: {@code minecraft:village} carried it for four days, and a tag
 * with one missing entry fails whole - vanilla's own village POIs with it.
 *
 * <p>A POI is only half the wiring. It has to be in the
 * {@code minecraft:acquirable_job_site} tag before an unemployed villager will
 * take the job, and that half is a tag file
 * ({@code data/minecraft/tags/point_of_interest_type/acquirable_job_site.json})
 * - tags merge across datapacks, so adding to vanilla's is additive and safe
 * where overriding a worldgen file would not be.
 *
 * <p>{@code maxTickets 1} the way every vanilla job site is: one villager per
 * post, so a second leatherworker needs a second post.
 */
public final class ModPoiTypes {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, HorseGenetics.MOD_ID);

    private static ResourceKey<PoiType> key(String path) {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, path));
    }

    private static DeferredHolder<PoiType, PoiType> post(String path, Supplier<? extends Block> block) {
        return POI_TYPES.register(path, () -> new PoiType(
                Set.copyOf(block.get().getStateDefinition().getPossibleStates()),
                1,   // one villager per post
                1)); // valid range, in blocks - vanilla's job sites all use 1
    }

    public static final ResourceKey<PoiType> LEATHERWORKERS_POST_KEY = key("leatherworkers_post");
    public static final ResourceKey<PoiType> SCIENTISTS_POST_KEY = key("scientists_post");
    public static final ResourceKey<PoiType> SUPPLIERS_POST_KEY = key("suppliers_post");
    public static final ResourceKey<PoiType> METALSMITHS_POST_KEY = key("metalsmiths_post");

    public static final DeferredHolder<PoiType, PoiType> LEATHERWORKERS_POST =
            post("leatherworkers_post", ModBlocks.LEATHERWORKERS_POST);
    public static final DeferredHolder<PoiType, PoiType> SCIENTISTS_POST =
            post("scientists_post", ModBlocks.SCIENTISTS_POST);
    public static final DeferredHolder<PoiType, PoiType> SUPPLIERS_POST =
            post("suppliers_post", ModBlocks.SUPPLIERS_POST);
    public static final DeferredHolder<PoiType, PoiType> METALSMITHS_POST =
            post("metalsmiths_post", ModBlocks.METALSMITHS_POST);

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
    }

    private ModPoiTypes() {
    }
}
