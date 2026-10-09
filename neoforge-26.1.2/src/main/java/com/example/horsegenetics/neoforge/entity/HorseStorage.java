package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorsePacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Is this something you can hang on a horse and put things in?</b> And if
 * so, how many slots it has.
 *
 * <h2>Detected, not listed</h2>
 * The owner's rule (2026-10-08): <i>anything you can place down in the world
 * and store items in, you can place on the side of a horse</i> - other mods'
 * chests included, with nobody maintaining a list. So the answer is asked of
 * the block itself: an item is storage when it places a block, the block has a
 * block entity, and that block entity holds items - it is a {@link Container},
 * or it answers NeoForge's item capability. One is built, off in nowhere and
 * never added to a level, purely to be asked how many slots it has.
 *
 * <p>Three tags bend that, and a datapack can fill any of them:
 * <ul>
 *   <li>{@link #DENIED} - holds items but is not storage. Vanilla's machines
 *       are in it: a furnace is a {@code Container}, and a furnace on a horse
 *       smelts nothing. Denial wins over everything.</li>
 *   <li>The slot's own tag ({@code gear/saddlebag_left}, {@code _right}) - is
 *       storage whatever the probe says. The ender chest is in both: its block
 *       entity holds nothing, because its contents are the player's.</li>
 *   <li>{@link #WEIGHTLESS} and {@link #KEEPS_CONTENTS}, below.</li>
 * </ul>
 *
 * <h2>The slots are the chest's, and the storage is ours</h2>
 * Only the <i>number</i> is taken from the block entity. The stacks live on
 * the horse ({@link HorsePacks}), in plain slots, so a modded drawer that
 * holds two thousand of one thing in the world holds a stack per slot here.
 * Running somebody else's block entity with no block under it was the other
 * option, and it is the kind of thing that works in a test and loses a
 * player's inventory in a modpack.
 *
 * <p><b>UNVERIFIED against another mod's chest.</b> The probe was written
 * against vanilla's block entities; {@code BlockCapability.getCapability} with
 * a block entity that is in no level is marked {@code @ApiStatus.Internal} in
 * NeoForge, and a provider that looks at the world around {@code near} gets
 * whatever is really there. Every failure is caught and reads as "not
 * storage".
 */
public final class HorseStorage {

    /** Holds items, and is not storage: hoppers, furnaces, dispensers and their kind. */
    public static final TagKey<Item> DENIED = tag("horse_storage/denied");

    /**
     * Storage whose contents are not on the horse - "interdimensional", in the
     * owner's word. Nothing in one counts towards the load. The ender chest
     * needs no entry (its contents are never on the horse to begin with); this
     * is for another mod's equivalent.
     */
    public static final TagKey<Item> WEIGHTLESS = tag("horse_storage/weightless");

    /**
     * Storage that travels with its contents inside it, the way a shulker box
     * does - so it comes off a horse full, where anything else must be emptied
     * first. Every {@link ShulkerBoxBlock} is one without being listed.
     */
    public static final TagKey<Item> KEEPS_CONTENTS = tag("horse_storage/keeps_contents");

    /**
     * Another mod's storage that should open as this mod's plain grid rather
     * than as its own block - the off switch, per item, for
     * {@code server/HostedPacks}.
     */
    public static final TagKey<Item> PLAIN_SCREEN = tag("horse_storage/plain_screen");

    /** What a chest that is storage only because a tag says so is given. */
    public static final int UNKNOWN_SIZE = 27;

    /** Slots by item; zero is "asked, and it holds nothing". Both sides fill their own. */
    private static final Map<Item, Integer> PROBED = new ConcurrentHashMap<>();

    private HorseStorage() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, path));
    }

    /**
     * How many slots {@code stack} gives a horse, or zero if it is not storage
     * at all. Never more than {@link HorsePacks#MAX_SLOTS}.
     *
     * @param near somewhere loaded - the horse - for a capability provider that looks around
     */
    public static int slots(ItemStack stack, Level level, BlockPos near) {
        if (stack.isEmpty() || stack.is(DENIED)) {
            return 0;
        }
        int probed = PROBED.computeIfAbsent(stack.getItem(), item -> probe(item, level, near));
        if (probed > 0) {
            return Math.min(probed, HorsePacks.MAX_SLOTS);
        }
        if (stack.is(HorseTackSlot.SADDLEBAG_LEFT.tag()) || stack.is(HorseTackSlot.SADDLEBAG_RIGHT.tag())) {
            return UNKNOWN_SIZE;
        }
        return 0;
    }

    /** The ender chest: a door to the player's own, with nothing of its own behind it. */
    public static boolean isEnderChest(ItemStack stack) {
        return stack.is(Items.ENDER_CHEST);
    }

    /** Whether what is in this chest is somewhere other than on the horse. */
    public static boolean weightless(ItemStack stack) {
        return isEnderChest(stack) || stack.is(WEIGHTLESS);
    }

    /** Whether this chest comes off with its contents still in it. */
    public static boolean keepsContents(ItemStack stack) {
        return stack.is(KEEPS_CONTENTS)
                || stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int probe(Item item, Level level, BlockPos near) {
        if (!(item instanceof BlockItem blockItem)) {
            return 0;
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof EntityBlock entityBlock)) {
            return 0;
        }
        try {
            BlockState state = block.defaultBlockState();
            BlockEntity built = entityBlock.newBlockEntity(near.immutable(), state);
            if (built == null) {
                return 0;
            }
            if (built instanceof Container container) {
                return Math.max(0, container.getContainerSize());
            }
            // Not a vanilla-style container; ask the capability instead. The
            // block entity is told its level first, because a provider may
            // reasonably ask it - but it is never added to that level.
            built.setLevel(level);
            ResourceHandler<ItemResource> handler =
                    Capabilities.Item.BLOCK.getCapability(level, near, state, built, null);
            return handler == null ? 0 : Math.max(0, handler.size());
        } catch (RuntimeException | LinkageError failed) {
            HorseGenetics.LOGGER.warn("Could not ask {} whether it stores items; it will not go on a horse: {}",
                    item, failed.toString());
            return 0;
        }
    }
}
