package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Dev-only: when the "Spawn Test Horse World" title-screen button
 * ({@code client/DebugTitleScreenButton}) creates a world, it sets
 * {@link #pendingHotbarFill}; on the next player login we stock the player up
 * for exercising horse features and put them somewhere worth testing from.
 *
 * <ul>
 *   <li><b>Hotbar</b>: the custom horse spawn egg (slot 0), then the vanilla
 *       tools the portal / taming / aging / inspect / lead features use
 *       (hay block, golden carrot, stick, clock, paper, lead).</li>
 *   <li><b>Main inventory</b>: one of every gameplay-layer item
 *       ({@link ModItems#TAB_ITEMS} minus the spawn egg) - horse hair, the
 *       breeding + gene carrots, the placeholder gene book, the seed jars, the
 *       tickets, the whistles and the transfer papers - so their icons,
 *       tooltips and behaviour can be checked without crafting them.</li>
 *   <li><b>Position</b>: the middle of the nearest <b>plains village</b>
 *       ({@link #moveToPlainsVillage}), because that is where both villagers
 *       live and hunting for one on foot is most of the cost of testing
 *       them.</li>
 * </ul>
 *
 * Inert in production (nothing sets the flag).
 */
@EventBusSubscriber
public final class DebugTestWorldHandler {

    public static volatile boolean pendingHotbarFill = false;

    /**
     * How far to hunt for a plains village, in chunks. Villages sit on a
     * 34-chunk grid, so this is many cells in every direction and a miss means
     * the spawn landed somewhere with no plains for a thousand blocks rather
     * than that the search was too tight.
     */
    private static final int VILLAGE_SEARCH_CHUNKS = 100;

    private DebugTestWorldHandler() {
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!pendingHotbarFill || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        pendingHotbarFill = false;
        fillInventory(player);
        moveToPlainsVillage(player);
    }

    private static void fillInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();

        // Hotbar: spawn egg first, then the feature-test tools.
        give(inv, 0, ModItems.CUSTOM_HORSE_SPAWN_EGG.get());
        give(inv, 1, Items.HAY_BLOCK);
        give(inv, 2, Items.GOLDEN_CARROT);
        give(inv, 3, Items.STICK);
        give(inv, 4, Items.CLOCK);
        give(inv, 5, Items.PAPER);
        give(inv, 6, Items.LEAD);

        // Main inventory: one of every new gameplay-layer item.
        int slot = 9;
        for (var item : ModItems.TAB_ITEMS) {
            if (item.get() == ModItems.CUSTOM_HORSE_SPAWN_EGG.get()) {
                continue;
            }
            give(inv, slot++, item.get());
        }
    }

    /**
     * Drop the player in the middle of the nearest plains village, and make
     * that the world spawn so dying does not send them back to an empty
     * meadow.
     *
     * <p><b>The middle, not the edge</b>, even though the cowboy's barn is on
     * the outskirts: from the town centre you can see which way the streets
     * run and walk out along each of them, where from a random point on the
     * edge you cannot even tell which side of the village you are on. The bell
     * is also the landmark the cowboy's own routine navigates by, so standing
     * at it is standing at the origin of his map.
     *
     * <p>Plains villages only, because that is the only kind the {@link com.example.horsegenetics.neoforge.entity.Cowboy cowboy} spawns with ({@code wiki/villagers.html}) - a savanna or desert
     * village is a wasted trip.
     *
     * <p>This runs a synchronous structure search, which generates chunks and
     * can take a moment on a fresh world. That is acceptable here and nowhere
     * else: this handler only ever fires once, in a throwaway dev world, on the
     * one login that created it.
     */
    private static void moveToPlainsVillage(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos found = level.findNearestMapStructure(
                StructureTags.ON_PLAINS_VILLAGE_MAPS, player.blockPosition(), VILLAGE_SEARCH_CHUNKS, false);
        if (found == null) {
            HorseGenetics.LOGGER.warn("Test world: no plains village within {} chunks of spawn", VILLAGE_SEARCH_CHUNKS);
            tell(player, Component.literal("No plains village within "
                            + (VILLAGE_SEARCH_CHUNKS * 16) + " blocks - you are at world spawn.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found);
        player.teleportTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        if (level.getLevelData() instanceof ServerLevelData data) {
            data.setSpawn(LevelData.RespawnData.of(level.dimension(), surface, 0.0F, 0.0F));
        }

        HorseGenetics.LOGGER.info("Test world: dropped the player at the plains village at {}", surface);
        tell(player, Component.literal("Plains village at "
                        + surface.getX() + ", " + surface.getZ()
                        + " - walk out along each street to find the cowboy's barn.")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void tell(ServerPlayer player, Component message) {
        player.sendSystemMessage(Component.literal("[Test world] ")
                .withStyle(ChatFormatting.GRAY).append(message));
    }

    private static void give(Inventory inv, int slot, Item item) {
        inv.setItem(slot, new ItemStack(item));
    }
}
