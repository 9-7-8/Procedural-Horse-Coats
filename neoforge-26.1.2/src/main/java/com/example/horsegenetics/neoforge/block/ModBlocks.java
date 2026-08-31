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
 * Block registry. Only one block so far: the hay-bale portal plane. It has
 * no {@code BlockItem} - it is never placed by hand, only by
 * {@link com.example.horsegenetics.neoforge.server.HorsePortalManager} when a
 * hay frame is lit with a golden carrot.
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

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ModBlocks() {
    }
}
