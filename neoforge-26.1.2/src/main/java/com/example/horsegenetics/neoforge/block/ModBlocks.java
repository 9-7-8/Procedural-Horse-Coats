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
 *   <li><b>{@code horsemans_table}</b> - the <b>horseman's</b> workstation, and
 *       the block behind the {@code horsegenetics:horsemans_table} POI. An
 *       ordinary placeable block; the POI is what turns an unemployed villager
 *       standing next to it into a horseman ({@code village/ModPoiTypes},
 *       {@code village/ModVillagerProfessions}).</li>
 *   <li><b>{@code cowboy_hitch}</b> - the <b>cowboy's</b> post, the same block
 *       with a different name and no POI at all. A cowboy is an entity rather
 *       than a profession, so there is nothing for one to claim;
 *       {@code server/CowboyHitchHandler} looks for the block instead. Both
 *       share one texture set on purpose - they are a pair.</li>
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
     * The <b>Horseman's Table</b> - the horseman's job site. A plain block whose
     * only job is to exist at a position the POI system can index; the
     * profession, the trades and the acquisition rules are all elsewhere.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> HORSEMANS_TABLE =
            BLOCKS.registerSimpleBlock("horsemans_table", ModBlocks::workPost);

    /**
     * The <b>Cowboy Hitch</b> - the cowboy's post, and the same block in every
     * respect but its name.
     *
     * <p>Two blocks rather than one because one block could not do both jobs.
     * A single post had to hand out a cowboy and then a horseman by alternating,
     * and the villager it converted took their job-site ticket with them - see
     * {@code server/CowboyHitchHandler}. A post each is a great deal less clever
     * and works.
     *
     * <p>Neither of them <i>does</i> anything yet beyond marking a spot. Making
     * them into real workstations - a hitch you tie a horse to, a table you work
     * leather at - is open work on the roadmap.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> COWBOY_HITCH =
            BLOCKS.registerSimpleBlock("cowboy_hitch", ModBlocks::workPost);

    /**
     * The <b>Equine Research Shelf</b> - a bookshelf that files research papers
     * and copies them onto blank books. See {@link EquineResearchShelfBlock}.
     * Properties are a full copy of vanilla's bookshelf, because it is one:
     * same hardness, same wood sound, same flammability, and the shelf's own
     * {@code getEnchantPowerBonus} keeps it feeding an enchanting table.
     */
    public static final DeferredBlock<EquineResearchShelfBlock> RESEARCH_SHELF = BLOCKS.registerBlock(
            "equine_research_shelf",
            EquineResearchShelfBlock::new,
            EquineResearchShelfBlock::shelfProperties);

    /** Shared properties: both posts are plain, breakable, flammable wood. */
    private static BlockBehaviour.Properties workPost() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ModBlocks() {
    }
}
