package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry.
 *
 * <ul>
 *   <li><b>{@code hay_portal}</b> - the hay-bale portal plane. It has no
 *       {@code BlockItem}: it is never placed by hand, only by
 *       {@link com.example.horsegenetics.neoforge.server.HorsePortalManager}
 *       when a hay frame is lit with a golden carrot.</li>
 *   <li><b>{@code horse_traders_post}</b> - the <b>horseman's</b> workstation,
 *       and the block behind the {@code horsegenetics:horse_traders_post} POI.
 *       An ordinary placeable block; the POI is what turns an unemployed
 *       villager standing next to it into a horseman
 *       ({@code village/ModPoiTypes}, {@code village/ModVillagerProfessions}).</li>
 * </ul>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(HorseGenetics.MOD_ID);

    public static final DeferredBlock<HayPortalBlock> HAY_PORTAL = BLOCKS.registerBlock(
            "hay_portal",
            HayPortalBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .noCollision()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F) // indestructible, like bedrock / nether portal
                    .lightLevel(state -> 11)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable());

    /**
     * The <b>Horse Trader's Post</b> - the horseman's job site. A plain block
     * whose only job is to exist at a position the POI system can index; the
     * profession, the trades and the acquisition rules are all elsewhere.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> HORSE_TRADERS_POST =
            BLOCKS.registerSimpleBlock("horse_traders_post",
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ModBlocks() {
    }
}
