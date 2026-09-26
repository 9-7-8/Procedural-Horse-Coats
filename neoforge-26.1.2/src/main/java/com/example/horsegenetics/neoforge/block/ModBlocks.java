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
 *   <li><b>{@code hay_portal}</b> - the horse portal plane. It has no
 *       {@code BlockItem}: it is never placed by hand, only by
 *       {@link com.example.horsegenetics.neoforge.server.HorsePortalManager}
 *       when a cobblestone frame is lit with a golden carrot. <b>The id is a
 *       fossil</b>: the frame was hay until a horse ate one, and renaming a
 *       registered block rewrites every world that has one placed.</li>
 *   <li><b>the four <code>*_post</code> blocks</b> - one workstation per
 *       equestrian, each behind a POI of the same name. Ordinary placeable
 *       blocks; the POI is what turns an unemployed villager standing next to
 *       one into that trade ({@code village/ModPoiTypes},
 *       {@code village/ModVillagerProfessions}).</li>
 *   <li><b>{@code cowboy_hitch}</b> - the <b>cowboy's</b> post, the same block
 *       with a different name and no POI at all. A cowboy is an entity rather
 *       than a profession, so there is nothing for one to claim;
 *       {@code server/CowboyHitchHandler} looks for the block instead. All five
 *       share one texture set on purpose - they are a set.</li>
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
     * The <b>four equestrians' posts</b> - one job site each. A plain block whose
     * only job is to exist at a position the POI system can index; the
     * profession, the trades and the acquisition rules are all elsewhere.
     *
     * <p>Four separate blocks rather than one shared post because a POI hands out
     * exactly one profession - see {@code village/ModPoiTypes}. They are
     * identical in every respect but their name and, for now, share the one post
     * model and texture; making each look like the trade it belongs to is open
     * work.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> LEATHERWORKERS_POST =
            BLOCKS.registerSimpleBlock("leatherworkers_post", ModBlocks::workPost);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> SCIENTISTS_POST =
            BLOCKS.registerSimpleBlock("scientists_post", ModBlocks::workPost);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> SUPPLIERS_POST =
            BLOCKS.registerSimpleBlock("suppliers_post", ModBlocks::workPost);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> METALSMITHS_POST =
            BLOCKS.registerSimpleBlock("metalsmiths_post", ModBlocks::workPost);

    /**
     * The <b>Cowboy Hitch</b> - the cowboy's post, and the same block in every
     * respect but its name.
     *
     * <p>A block of its own rather than a fifth equestrian post because one post
     * could not do two jobs. A single post had to hand out a cowboy and then a
     * shopkeeper by alternating, and the villager it converted took their
     * job-site ticket with them - see {@code server/CowboyHitchHandler}. A post
     * each is a great deal less clever and works.
     *
     * <p>None of them <i>does</i> anything yet beyond marking a spot. Making
     * them into real workstations - a hitch you tie a horse to, a bench you work
     * leather at - is open work on the roadmap.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> COWBOY_HITCH =
            BLOCKS.registerSimpleBlock("cowboy_hitch", ModBlocks::workPost);

    /**
     * <b>Golden carrots, as a crop.</b> Vanilla's carrot in every mechanical
     * respect; what is different is that the seeds are rare and that no gold
     * ever comes out of it. See {@link GoldenCarrotCropBlock}.
     *
     * <p>No block item: the seed is its own item and places this, exactly as
     * wheat seeds place wheat. {@code ModItems.GOLDEN_CARROT_SEEDS}.
     */
    public static final DeferredBlock<GoldenCarrotCropBlock> GOLDEN_CARROT_CROP =
            BLOCKS.registerBlock("golden_carrot_crop",
                    GoldenCarrotCropBlock::new,
                    GoldenCarrotCropBlock::cropProperties);

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

    /**
     * The <b>Tack Dyeing Bench</b> - dyes a saddle's seat, bridle and metal
     * hardware independently. No block entity: it computes from its slots, so it
     * is loom-shaped rather than furnace-shaped. See
     * {@link EquestrianBenchBlock}.
     *
     * <p><b>The id is still {@code equestrian_bench}</b>, and so are the class
     * names. It was renamed for players on 2026-09-18 because "equestrian
     * bench" said nothing about what it does; the id was deliberately left
     * alone rather than churn the barn structure NBT, the loot table, the
     * recipe and the baked recipe reference for a cosmetic change. The
     * divergence is the owner's call, not an oversight.
     */
    public static final DeferredBlock<EquestrianBenchBlock> EQUESTRIAN_BENCH = BLOCKS.registerBlock(
            "equestrian_bench",
            EquestrianBenchBlock::new,
            EquestrianBenchBlock::benchProperties);

    /**
     * The <b>Horse Stasis Bank</b> - a chest that takes stasis chambers and
     * nothing else, so a farm's shelved horses live somewhere you can walk up to.
     * See {@link HorseStasisBankBlock}; the Browse tab and everything that acts on
     * a stored horse are later stages of {@code wiki/horse-stasis.html}'s build
     * order.
     */
    public static final DeferredBlock<HorseStasisBankBlock> HORSE_STASIS_BANK = BLOCKS.registerBlock(
            "horse_stasis_bank",
            HorseStasisBankBlock::new,
            HorseStasisBankBlock::bankProperties);

    /**
     * <b>The one jump block.</b> Every style, every wood, every pair of woods.
     *
     * <p>It replaces twelve per-wood blocks. The wood is
     * {@link JumpBlockEntity} data rather than a property because the standards
     * and the rails must be independently choosable, and as properties that is
     * 12 x 12 x everything else - <b>13,824 block states</b>, all allocated at
     * registry bootstrap on the server as well as the client.
     */
    public static final DeferredBlock<JumpBlock> JUMP = BLOCKS.registerBlock(
            "jump",
            JumpBlock::new,
            // Vanilla fence strength and wood sound. noOcclusion() because it is
            // nowhere near a full cube: without it the faces of the blocks
            // behind a jump are culled and a course is full of holes.
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava());

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
