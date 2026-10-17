package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnInventoryEntity;
import com.example.horsegenetics.neoforge.carts.entity.WagonEntity;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * <b>Dev-only: the server half of the menu crawler</b> ({@code client/crawl/MenuCrawlClient}).
 * Inert unless the client was started with {@code -Dhorsegenetics.menuCrawl=true}; nothing
 * else calls it.
 *
 * <p>It builds one flat yard beside the player with everything a mod screen opens on - the
 * four menu blocks, three owned horses (a mare, a stallion, a foal), a stasis bank with
 * horses filed in it, and the carts - and it opens the container menus, which only the
 * server can. The client half reads the server's state straight off this class and the
 * entities: singleplayer only, which is what makes it a dev tool and nothing more.
 *
 * <p>Everything here happens in the throwaway test world ({@code DebugTitleScreenButton}),
 * which is deleted when the client shuts down, so a click that spawns a horse, spends a
 * name tag or ejects a chamber costs nothing.
 */
public final class DebugMenuCrawl {

    /** Where things stand, relative to the yard's centre. */
    public static BlockPos centre = BlockPos.ZERO;
    public static BlockPos shelf = BlockPos.ZERO;
    public static BlockPos jump = BlockPos.ZERO;
    public static BlockPos jump2 = BlockPos.ZERO;
    public static BlockPos bank = BlockPos.ZERO;
    public static BlockPos bench = BlockPos.ZERO;

    public static @Nullable UUID mare;
    public static @Nullable UUID stallion;
    public static @Nullable UUID foal;
    public static @Nullable UUID plow;
    public static @Nullable UUID drill;
    public static @Nullable UUID wagonSmall;
    public static @Nullable UUID wagonBig;

    /** The research paper filed on the shelf, and a second one the crawl files by hand. */
    public static String shelfTopic = "";

    private static final int HALF = 10;

    private DebugMenuCrawl() {
    }

    /** Clear and floor a yard round the player, place the blocks, spawn the horses and carts. */
    public static void build(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos at = player.blockPosition();
        centre = at;
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                level.setBlock(at.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 0; y < 6; y++) {
                    level.setBlock(at.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        command(level, "time set noon");
        command(level, "weather clear");
        command(level, "gamerule doMobSpawning false");

        shelf = at.offset(3, 0, 0);
        jump = at.offset(-3, 0, 0);
        jump2 = at.offset(-3, 0, 1);
        bank = at.offset(0, 0, 3);
        bench = at.offset(0, 0, -3);
        level.setBlock(shelf, ModBlocks.RESEARCH_SHELF.get().defaultBlockState(), 3);
        var jumpState = ModBlocks.JUMP.get().defaultBlockState();
        // A run of jumps is joined across the way it faces (JumpBlock.run), so the second
        // one goes on its left.
        jump2 = jump.relative(jumpState.getValue(com.example.horsegenetics.neoforge.block.JumpBlock.FACING)
                .getCounterClockWise());
        level.setBlock(jump, jumpState, 3);
        level.setBlock(jump2, jumpState, 3);
        level.setBlock(bank, ModBlocks.HORSE_STASIS_BANK.get().defaultBlockState(), 3);
        level.setBlock(bench, ModBlocks.EQUESTRIAN_BENCH.get().defaultBlockState(), 3);

        mare = spawnHorse(level, at.offset(2, 0, 2)).getUUID();
        stallion = spawnHorse(level, at.offset(-2, 0, 2)).getUUID();
        Horse baby = spawnHorse(level, at.offset(2, 0, -2));
        baby.setAge(-24000);
        foal = baby.getUUID();

        plow = spawnCart(level, HorseCarts.PLOW_ENTITY, at.offset(6, 0, 6)).getUUID();
        drill = spawnCart(level, HorseCarts.SEED_DRILL_ENTITY, at.offset(-6, 0, 6)).getUUID();
        wagonSmall = spawnWagon(level, at.offset(6, 0, -6), 1).getUUID();
        wagonBig = spawnWagon(level, at.offset(-6, 0, -6), 3).getUUID();
        HorseGenetics.LOGGER.info("[crawl] yard built at {}", at);
    }

    /**
     * The horses' founding tick has run by now ({@link HorseRecords#hasRealRecord}), so a
     * sex can be stamped without being rolled over: give each its sex, tame it to the
     * player and bond it, so every horse screen opens on a horse the player owns.
     */
    public static boolean finishHorses(ServerPlayer player) {
        Horse m = horse(player, mare);
        Horse s = horse(player, stallion);
        Horse f = horse(player, foal);
        if (m == null || s == null || f == null
                || !HorseRecords.hasRealRecord(m) || !HorseRecords.hasRealRecord(s) || !HorseRecords.hasRealRecord(f)) {
            return false;
        }
        own(player, m, Sex.FEMALE);
        own(player, s, Sex.MALE);
        own(player, f, Sex.FEMALE);
        // The foal is the mare's and the stallion's, so the family tree has a node to click.
        HorseRecords.apply(f, HorseRecords.of(f).withParents(m.getUUID(), s.getUUID()));
        return true;
    }

    private static void own(ServerPlayer player, Horse horse, Sex sex) {
        NeoRng rng = new NeoRng(horse.getRandom());
        if (HorseRecords.of(horse).sex() != sex) {
            HorseRecords.apply(horse, HorseRecords.newFounder(horse, rng, Breeds.FERAL_MIXED, sex));
        }
        horse.setTamed(true);
        horse.setOwner(player);
        HorseRecords.setOwner(horse, player.getUUID());
        HorseRecords.setTamedBy(horse, player.getGameProfile().name());
        var care = ModAttachments.HORSE_CARE.get();
        horse.setData(care, horse.getData(care).withBond(90));
    }

    /**
     * File horses in the bank and papers on the shelf: an occupied spacer (the only tier
     * that may stand at stud), an occupied basic, and an empty basic for Eject.
     */
    public static void stock(ServerPlayer player) {
        ServerLevel level = player.level();
        if (level.getBlockEntity(bank) instanceof com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity b) {
            b.claimIfUnowned(player.getUUID());
            b.chambers().setItem(0, filed(level, player, ModItems.OCCUPIED_SPACER_STASIS_CHAMBER.get()));
            b.chambers().setItem(1, filed(level, player, ModItems.OCCUPIED_BASIC_STASIS_CHAMBER.get()));
            b.chambers().setItem(2, new ItemStack(ModItems.BASIC_STASIS_CHAMBER.get()));
            b.setChanged();
        }
        if (level.getBlockEntity(shelf) instanceof com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity sh) {
            ResearchTopic topic = ResearchTopic.defaultFor(Genes.byKey("horsegenetics.extension"));
            sh.papers().setItem(0, ResearchPaperItem.of(topic));
            ResearchTopic other = ResearchTopic.defaultFor(Genes.byKey("horsegenetics.agouti"));
            sh.papers().setItem(1, ResearchPaperItem.of(other));
            shelfTopic = topic.token();
            sh.setChanged();
        }
    }

    /** A chamber holding a fresh owned horse, which leaves the world as a captured one would. */
    private static ItemStack filed(ServerLevel level, ServerPlayer player, net.minecraft.world.item.Item chamber) {
        Horse h = spawnHorse(level, player.blockPosition().above(3));
        HorseRecords.apply(h, HorseRecords.newFounder(h, new NeoRng(h.getRandom())));
        HorseRecords.setOwner(h, player.getUUID());
        h.setTamed(true);
        h.setOwner(player);
        ItemStack stack = StasisChamberItem.withHorse(new ItemStack(chamber),
                HorseStasisHandler.snapshot(h, HorseRecords.of(h).displayName()));
        h.discard();
        return stack;
    }

    // ------------------------------------------------------------------ opening menus

    /** Right-click a menu block with an empty hand, as a player would. */
    public static void openBlock(ServerPlayer player, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.level().getBlockState(pos).useWithoutItem(player.level(), player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    /** A cart's own inventory - the sneak-right-click path. */
    public static void openCart(ServerPlayer player, @Nullable UUID id) {
        if (id != null && player.level().getEntity(id) instanceof AbstractDrawnInventoryEntity cart) {
            cart.openCustomInventoryScreen(player);
        }
    }

    // ------------------------------------------------------------------ reads for the checks

    public static @Nullable Horse horse(ServerPlayer player, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        return player.level().getEntity(id) instanceof Horse h ? h : null;
    }

    public static @Nullable Entity entity(ServerPlayer player, @Nullable UUID id) {
        return id == null ? null : player.level().getEntity(id);
    }

    /** Put {@code stack} in the player's main hand (the spawn egg, a name tag). */
    public static void hold(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    /** Count the horses within {@code r} blocks of the yard centre. */
    public static int horsesNear(ServerPlayer player, double r) {
        return player.level().getEntitiesOfClass(Horse.class,
                new net.minecraft.world.phys.AABB(centre).inflate(r)).size();
    }

    /** Stand the player at the yard centre, facing north, out of everything's way. */
    public static void home(ServerPlayer player) {
        player.teleportTo(player.level(), centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5,
                Set.of(), 180f, 0f, false);
    }

    // ------------------------------------------------------------------ spawning

    private static Horse spawnHorse(ServerLevel level, BlockPos pos) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            throw new IllegalStateException("could not create a horse");
        }
        horse.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // Still, so a horse is where the crawl left it when a check looks for it; a still
        // horse is never moved by its AI (test-tiers, #248), which is the point here.
        horse.setNoAi(true);
        level.addFreshEntity(horse);
        return horse;
    }

    private static <T extends Entity> T spawnCart(ServerLevel level, EntityType<T> type, BlockPos pos) {
        T cart = type.create(level, EntitySpawnReason.COMMAND);
        if (cart == null) {
            throw new IllegalStateException("could not create " + type);
        }
        cart.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(cart);
        return cart;
    }

    @SuppressWarnings("unchecked")
    private static WagonEntity spawnWagon(ServerLevel level, BlockPos pos, int chests) {
        WagonEntity wagon = spawnCart(level, HorseCarts.WAGON_ENTITY, pos);
        // The chest count has no setter: the way in is a right-click with a chest, which
        // needs a hit position on the wagon. Set the synced value it writes.
        try {
            java.lang.reflect.Field f = WagonEntity.class.getDeclaredField("CHEST_COUNT");
            f.setAccessible(true);
            wagon.getEntityData().set((EntityDataAccessor<Integer>) f.get(null), chests);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("WagonEntity.CHEST_COUNT moved", e);
        }
        return wagon;
    }

    private static void command(ServerLevel level, String command) {
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** A name tag, for the rename screen's server half (which wants one in a hand). */
    public static ItemStack nameTag() {
        return new ItemStack(Items.NAME_TAG);
    }

    public static ItemStack spawnEgg() {
        return new ItemStack(ModItems.CUSTOM_HORSE_SPAWN_EGG.get());
    }
}
