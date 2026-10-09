package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseGear;
import com.example.horsegenetics.neoforge.data.HorsePacks;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.HorseStorage;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>A chest on a horse that opens as the block it is.</b> Another mod's chest
 * has its own screen, its own slots, its own upgrades - and its menu finds all
 * of that by looking for its block entity <i>in the world, at a position</i>,
 * on the server and on the client both. (Read out of Sophisticated Storage's
 * {@code StorageContainerMenu}, which throws if nothing is there, and
 * DimStorage's menu factory; that mod's own author wrote a second set of menus
 * to put its storage on a minecart.) A block entity that was never placed
 * cannot open one.
 *
 * <h2>So it is placed</h2>
 * (Owner's call, 2026-10-08, from four options.) While a hosted chest is open
 * it <b>really exists</b>: at the bottom of the world under the player, where
 * the bedrock is, with the block above it cleared so a lid has room. It is put
 * there the way a hand would put it - the item's block data and components
 * applied, {@code setPlacedBy} called - opened through the block's own
 * {@code useWithoutItem}, and picked back up the way vanilla's
 * pick-block-with-data does when the menu shuts. Whatever was at those two
 * positions is put back exactly.
 *
 * <p>Between openings the chest is one {@link ItemStack} carrying everything -
 * {@link HorsePacks#hold} - because what a modded block entity saves is not a
 * list of stacks this mod could keep any other way.
 *
 * <h2>What makes that safe</h2>
 * <ul>
 *   <li><b>The horse's copy is the authority, every tick.</b> While a chest is
 *       live it is re-picked into the horse each server tick, so the world
 *       block never holds anything the horse does not.</li>
 *   <li><b>A journal</b> ({@link Journal}) names every position before it is
 *       touched. A server that dies with a chest out sweeps the journal on the
 *       next start and puts the original blocks back <i>without</i> dropping
 *       what was in them - the copy that survives is the horse's, so a crash
 *       can lose a tick and can never make a second chest.</li>
 *   <li><b>Nobody else can touch it</b>: clicks and breaks at a live position
 *       are cancelled.</li>
 *   <li><b>Reach is the horse's, not the block's.</b> Every menu asks whether
 *       the player is still near its block, and this block is a hundred blocks
 *       down. {@code mixin/HostedMenu*Mixin} hands that question to
 *       {@link #stillValid}, which answers for the horse instead.</li>
 *   <li><b>Any failure falls back.</b> A block that will not place, will not
 *       open or throws is lifted again, that item is remembered as one that
 *       does not host, and the chest opens as the plain grid.</li>
 * </ul>
 *
 * <h2>Which chests</h2>
 * {@link #hosts}: not vanilla's - a chest, a barrel and a shulker box already
 * open vanilla's own menus over the horse's store, with no block needed - and
 * not anything in {@code horse_storage/plain_screen}, and nothing at all with
 * {@code packs.real_screens} off.
 *
 * <p><b>UNVERIFIED against another mod.</b> No modded chest is installed in the
 * dev run. The gametest drives the whole path with a vanilla chest; what it
 * cannot show is a modded client finding its block entity - the reason the
 * block update is sent to the opening player ahead of the menu packet rather
 * than left to the end-of-tick broadcast.
 */
@EventBusSubscriber
public final class HostedPacks {

    private static final int PLACE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Putting the original back: no drops, no "the container was destroyed" side effects. */
    private static final int LIFT = PLACE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    /** How far the player may get from the column the block stands in before the chest shuts. */
    private static final double COLUMN_REACH = 64.0;

    /** One chest standing in the world: where, and what was there before it and above it. */
    private record Site(ServerLevel level, BlockPos pos, BlockState below, BlockState above) {
    }

    private record Session(ServerPlayer player, Horse horse, HorseTackSlot slot, Site site) {
    }

    /** By player: one menu each. Server thread only. */
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    /** Every live position, chest and cleared block both, as {@code dimension@pos}. */
    private static final Set<String> LIVE = new HashSet<>();
    /** Items that would not place or would not open, until the server restarts. */
    private static final Set<Item> REFUSED = ConcurrentHashMap.newKeySet();

    private HostedPacks() {
    }

    /**
     * Whether {@code chest} is one that opens as its own block. A question
     * about the item alone, and the same answer on both sides.
     */
    public static boolean hosts(ItemStack chest) {
        if (chest.isEmpty() || !(chest.getItem() instanceof BlockItem) || !ServerConfig.packRealScreens()) {
            return false;
        }
        if (HorseStorage.isEnderChest(chest) || chest.is(HorseStorage.PLAIN_SCREEN)
                || REFUSED.contains(chest.getItem())) {
            return false;
        }
        return !Identifier.DEFAULT_NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(chest.getItem()).getNamespace());
    }

    // ------------------------------------------------------------------
    // Going on, and coming off
    // ------------------------------------------------------------------

    /**
     * A chest going onto a horse to be carried whole. It is stood in the world
     * for a moment - so that whatever it holds as an item becomes a real block
     * entity that can be counted - picked up again, and kept on the horse.
     *
     * @return the stack to wear (the chest without its data), or null if it
     *         would not place, in which case the caller carries it the plain way
     */
    public static @Nullable ItemStack adopt(AbstractHorse horse, HorseTackSlot slot, ItemStack chest) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        Site site = place(level, horse.blockPosition(), chest, null);
        if (site == null) {
            return null;
        }
        try {
            ItemStack whole = pick(site, chest);
            if (whole == null) {
                return null;
            }
            horse.getData(ModAttachments.HORSE_PACKS).hold(slot.name(), whole, count(site));
            return face(whole);
        } catch (RuntimeException | LinkageError failed) {
            refuse(chest, "be picked up", failed);
            return null;
        } finally {
            lift(site);
        }
    }

    /** Whether {@code slot}'s chest is one carried whole. */
    public static boolean holds(AbstractHorse horse, HorseTackSlot slot) {
        return horse.getData(ModAttachments.HORSE_PACKS).held(slot.name()) != null;
    }

    /**
     * What a player taking {@code slot}'s chest would be holding: the whole
     * item, or a plain one if it is empty and no different from new. A copy;
     * nothing leaves the horse until {@link #forget}.
     */
    public static ItemStack view(AbstractHorse horse, HorseTackSlot slot, ItemStack worn) {
        settle(horse, slot);
        HorsePacks.Held held = horse.getData(ModAttachments.HORSE_PACKS).held(slot.name());
        if (held == null) {
            return worn;
        }
        return held.items() == 0 && pristine(horse, held.chest()) ? face(held.chest()) : held.chest().copy();
    }

    /** {@code slot}'s chest has left the horse in somebody's hand; stop carrying it. */
    public static void forget(AbstractHorse horse, HorseTackSlot slot) {
        settle(horse, slot);
        horse.getData(ModAttachments.HORSE_PACKS).hold(slot.name(), ItemStack.EMPTY, 0);
    }

    // ------------------------------------------------------------------
    // Opening
    // ------------------------------------------------------------------

    /**
     * Stand {@code slot}'s chest in the world and open it for {@code player}
     * as the block would. False - with nothing left standing - if it will not.
     */
    public static boolean open(ServerPlayer player, Horse horse, HorseTackSlot slot) {
        HorsePacks.Held held = horse.getData(ModAttachments.HORSE_PACKS).held(slot.name());
        if (held == null) {
            return false;
        }
        end(SESSIONS.get(player.getUUID()));
        settle(horse, slot);
        ServerLevel level = player.level();
        Site site = place(level, player.blockPosition(), held.chest(), player);
        if (site == null) {
            return false;
        }
        boolean opened = false;
        try {
            // To this client now, and in this order: a modded menu's client side
            // looks its block entity up the moment the open-screen packet lands,
            // and the ordinary block update is not sent until the end of the tick.
            player.connection.send(new ClientboundBlockUpdatePacket(level, site.pos().above()));
            player.connection.send(new ClientboundBlockUpdatePacket(level, site.pos()));
            BlockEntity entity = level.getBlockEntity(site.pos());
            if (entity != null) {
                Packet<ClientGamePacketListener> update = entity.getUpdatePacket();
                player.connection.send(update != null ? update : ClientboundBlockEntityDataPacket.create(entity));
            }
            AbstractContainerMenu before = player.containerMenu;
            level.getBlockState(site.pos()).useWithoutItem(level, player,
                    new BlockHitResult(Vec3.atCenterOf(site.pos()), Direction.UP, site.pos(), false));
            opened = player.containerMenu != before && player.containerMenu != player.inventoryMenu;
        } catch (RuntimeException | LinkageError failed) {
            refuse(held.chest(), "be opened", failed);
        }
        if (!opened) {
            if (!REFUSED.contains(held.chest().getItem())) {
                refuse(held.chest(), "open a menu", null);
            }
            lift(site);
            return false;
        }
        SESSIONS.put(player.getUUID(), new Session(player, horse, slot, site));
        return true;
    }

    /**
     * The answer to a menu's "is this player still at my block", for a player
     * with a hosted chest open - or null for anybody else, who gets the menu's
     * own answer. Called from the mixins, on every tick and every click.
     */
    public static @Nullable Boolean stillValid(Player player) {
        if (SESSIONS.isEmpty()) {
            return null;
        }
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? null : valid(session);
    }

    private static boolean valid(Session session) {
        Site site = session.site();
        ServerPlayer player = session.player();
        if (player.level() != site.level() || !HorsePackHandler.mayOpen(session.horse(), player)) {
            return false;
        }
        double dx = player.getX() - (site.pos().getX() + 0.5);
        double dz = player.getZ() - (site.pos().getZ() + 0.5);
        return dx * dx + dz * dz <= COLUMN_REACH * COLUMN_REACH
                && site.level().isLoaded(site.pos())
                && site.level().getBlockEntity(site.pos()) != null;
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer player = session.player();
            if (player.isRemoved() || player.hasDisconnected()
                    || player.containerMenu == player.inventoryMenu) {
                end(session);
            } else if (!valid(session)) {
                player.closeContainer();
                end(session);
            } else {
                keep(session);
            }
        }
    }

    /** The horse's copy brought up to the block, and the load with it. */
    private static void keep(Session session) {
        Horse horse = session.horse();
        if (horse.isRemoved()) {
            return;
        }
        HorsePacks packs = horse.getData(ModAttachments.HORSE_PACKS);
        HorsePacks.Held held = packs.held(session.slot().name());
        ItemStack template = held != null ? held.chest() : session.slot().on(horse);
        try {
            ItemStack whole = pick(session.site(), template);
            if (whole == null) {
                return;
            }
            packs.hold(session.slot().name(), whole, count(session.site()));
            // What is worn is the chest's face; keep it true if the block changed it.
            ItemStack face = face(whole);
            if (!ItemStack.matches(face, session.slot().on(horse))) {
                HorseGear gear = horse.getData(ModAttachments.HORSE_GEAR);
                horse.setData(ModAttachments.HORSE_GEAR, gear.with(session.slot().name(), face));
            }
            HorsePackHandler.refresh(horse);
        } catch (RuntimeException | LinkageError failed) {
            HorseGenetics.LOGGER.warn("Could not read a chest back off the world at {}: {}",
                    session.site().pos(), failed.toString());
        }
    }

    /** Shut a session: the last of the block onto the horse, and the world as it was. */
    private static void end(@Nullable Session session) {
        if (session == null || SESSIONS.remove(session.player().getUUID(), session) == false) {
            return;
        }
        keep(session);
        lift(session.site());
    }

    /** If {@code slot}'s chest is standing in the world, bring it home now. */
    private static void settle(AbstractHorse horse, HorseTackSlot slot) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            if (session.horse() == horse && session.slot() == slot) {
                end(session);
                session.player().closeContainer();
            }
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        end(SESSIONS.get(event.getEntity().getUUID()));
    }

    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            end(session);
        }
    }

    // ------------------------------------------------------------------
    // The block in the world
    // ------------------------------------------------------------------

    /**
     * Stand {@code chest} in the world under {@code near}, as a hand would
     * place it. Null, with the world untouched, if there is nowhere or it
     * throws.
     */
    private static @Nullable Site place(ServerLevel level, BlockPos near, ItemStack chest,
                                        @Nullable ServerPlayer placer) {
        if (!(chest.getItem() instanceof BlockItem item)) {
            return null;
        }
        BlockPos pos = find(level, near);
        if (pos == null) {
            return null;
        }
        Site site = new Site(level, pos, level.getBlockState(pos), level.getBlockState(pos.above()));
        Journal.of(level.getServer()).open(site);
        LIVE.add(key(level, pos));
        LIVE.add(key(level, pos.above()));
        try {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), LIFT);
            if (setDown(level, pos, chest, placer, PLACE) == null) {
                lift(site);
                refuse(chest, "make a block entity", null);
                return null;
            }
            return site;
        } catch (RuntimeException | LinkageError failed) {
            lift(site);
            refuse(chest, "be placed", failed);
            return null;
        }
    }

    /**
     * Put {@code chest}'s block at {@code pos} with everything the item carries
     * applied to it - {@code BlockItem.place}'s own steps, in its order: the
     * block, the item's block data, its components, then the block's own say.
     * The block entity, or null if the item's block has none (in which case
     * whatever was set is left for the caller to clear). May throw whatever
     * another mod's block throws; both callers catch.
     */
    public static @Nullable BlockEntity setDown(ServerLevel level, BlockPos pos, ItemStack chest,
                                                net.minecraft.world.entity.@Nullable LivingEntity placer,
                                                int flags) {
        if (!(chest.getItem() instanceof BlockItem item)) {
            return null;
        }
        BlockState state = item.getBlock().defaultBlockState();
        level.setBlock(pos, state, flags);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return null;
        }
        BlockItem.updateCustomBlockEntityTag(level,
                placer instanceof Player player ? player : null, pos, chest);
        entity.applyComponentsFromItemStack(chest);
        entity.setChanged();
        state.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), placer, chest);
        return entity;
    }

    /**
     * Somewhere at the very bottom of the world near {@code near}: loaded, not
     * holding a block entity of its own (its contents could not be put back),
     * and not already in use. Spaced two apart so no two hosted chests ever
     * stand side by side and decide to be one double chest.
     */
    private static @Nullable BlockPos find(ServerLevel level, BlockPos near) {
        int y = level.getMinY();
        for (int ring = 0; ring <= 4; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(near.getX() + dx * 2, y, near.getZ() + dz * 2);
                    if (level.isLoaded(pos) && !LIVE.contains(key(level, pos))
                            && !LIVE.contains(key(level, pos.above()))
                            && level.getBlockEntity(pos) == null
                            && level.getBlockEntity(pos.above()) == null) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    /** Put back what was there. Never drops, never spills: the horse has the contents. */
    private static void lift(Site site) {
        ServerLevel level = site.level();
        level.setBlock(site.pos(), site.below(), LIFT);
        level.setBlock(site.pos().above(), site.above(), LIFT);
        LIVE.remove(key(level, site.pos()));
        LIVE.remove(key(level, site.pos().above()));
        Journal.of(level.getServer()).close(site);
    }

    /**
     * The standing block as an item with everything in it - vanilla's
     * pick-block-with-data ({@code ServerGamePacketListenerImpl.addBlockDataToItem}),
     * laid over {@code template} so that whatever the item carried that the
     * block does not hand back is kept.
     */
    private static @Nullable ItemStack pick(Site site, ItemStack template) {
        BlockEntity entity = site.level().getBlockEntity(site.pos());
        if (entity == null) {
            return null;
        }
        ItemStack whole = template.copyWithCount(1);
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(entity.problemPath(), HorseGenetics.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, site.level().registryAccess());
            entity.saveCustomOnly(output);
            entity.removeComponentsFromTag(output);
            BlockItem.setBlockEntityData(whole, entity.getType(), output);
            whole.applyComponents(entity.collectComponents());
        }
        return whole;
    }

    /** How many items the standing block holds, through whichever door it has. */
    private static int count(Site site) {
        try {
            BlockEntity entity = site.level().getBlockEntity(site.pos());
            long total = 0;
            if (entity instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    total += container.getItem(i).getCount();
                }
            } else if (entity != null) {
                ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(
                        site.level(), site.pos(), site.level().getBlockState(site.pos()), entity, null);
                if (handler != null) {
                    for (int i = 0; i < handler.size(); i++) {
                        total += handler.getAmountAsLong(i);
                    }
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        } catch (RuntimeException | LinkageError failed) {
            return 0;
        }
    }

    /** The whole chest without what is inside it: what is worn, drawn, and sent to clients. */
    private static ItemStack face(ItemStack whole) {
        ItemStack face = whole.copy();
        reset(face, DataComponents.BLOCK_ENTITY_DATA);
        reset(face, DataComponents.CONTAINER);
        return face;
    }

    /** Back to what a fresh one of these carries - which is not always "nothing". */
    private static <T> void reset(ItemStack stack, DataComponentType<T> type) {
        T fresh = stack.getItem().components().get(type);
        if (fresh == null) {
            stack.remove(type);
        } else {
            stack.set(type, fresh);
        }
    }

    /**
     * Whether an empty chest is no different from a new one, so that it may be
     * handed back plain and stack with others. Asked of a block entity that is
     * built and never placed; anything that goes wrong means "it differs".
     */
    private static boolean pristine(AbstractHorse horse, ItemStack whole) {
        try {
            if (!(whole.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof EntityBlock block)) {
                return false;
            }
            BlockEntity fresh = block.newBlockEntity(BlockPos.ZERO, item.getBlock().defaultBlockState());
            if (fresh == null) {
                return false;
            }
            ItemStack plain = face(whole);
            ItemStack again = plain.copy();
            try (ProblemReporter.ScopedCollector reporter =
                         new ProblemReporter.ScopedCollector(fresh.problemPath(), HorseGenetics.LOGGER)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, horse.registryAccess());
                fresh.saveCustomOnly(output);
                fresh.removeComponentsFromTag(output);
                BlockItem.setBlockEntityData(again, fresh.getType(), output);
            }
            ItemStack kept = whole.copy();
            reset(kept, DataComponents.CONTAINER);
            reset(again, DataComponents.CONTAINER);
            return ItemStack.isSameItemSameComponents(kept, again);
        } catch (RuntimeException | LinkageError failed) {
            return false;
        }
    }

    private static void refuse(ItemStack chest, String what, @Nullable Throwable why) {
        if (REFUSED.add(chest.getItem())) {
            HorseGenetics.LOGGER.warn("{} would not {} as a real block for a horse{}; it opens as a plain"
                            + " grid until the server restarts", chest.getItem(), what,
                    why == null ? "" : " (" + why + ")");
        }
    }

    private static String key(Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.asLong();
    }

    // ------------------------------------------------------------------
    // Hands off
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!LIVE.isEmpty() && LIVE.contains(key(event.getLevel(), event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onBreakBlock(BreakBlockEvent event) {
        if (!LIVE.isEmpty() && event.getLevel() instanceof Level level
                && LIVE.contains(key(level, event.getPos()))) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------
    // The journal
    // ------------------------------------------------------------------

    /** How many chests are standing in the world right now, by the journal's count - for a test. */
    public static int standing(MinecraftServer server) {
        return Journal.of(server).standing.size();
    }

    /** After a crash: every position a chest was standing at gets its own block back. */
    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        SESSIONS.clear();
        LIVE.clear();
        Journal.of(event.getServer()).sweep(event.getServer());
    }

    /**
     * <b>Every position this class has changed and not yet changed back</b>,
     * saved with the overworld. Empty except while a chest is open - and after
     * a crash, which is what it is for.
     */
    static final class Journal extends SavedData {

        private record Entry(String dimension, long pos, BlockState below, BlockState above) {
            static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                    Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
                    BlockState.CODEC.fieldOf("below").forGetter(Entry::below),
                    BlockState.CODEC.fieldOf("above").forGetter(Entry::above)
            ).apply(i, Entry::new));
        }

        static final Codec<Journal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Entry.CODEC.listOf().fieldOf("standing").forGetter(journal -> List.copyOf(journal.standing))
        ).apply(i, Journal::new));

        static final SavedDataType<Journal> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "hosted_packs"),
                Journal::new,
                CODEC);

        private final List<Entry> standing;

        private Journal() {
            this.standing = new ArrayList<>();
        }

        private Journal(List<Entry> entries) {
            this.standing = new ArrayList<>(entries);
        }

        static Journal of(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }

        void open(Site site) {
            standing.add(new Entry(site.level().dimension().identifier().toString(), site.pos().asLong(),
                    site.below(), site.above()));
            setDirty();
        }

        void close(Site site) {
            String dimension = site.level().dimension().identifier().toString();
            long pos = site.pos().asLong();
            if (standing.removeIf(entry -> entry.pos() == pos && entry.dimension().equals(dimension))) {
                setDirty();
            }
        }

        void sweep(MinecraftServer server) {
            if (standing.isEmpty()) {
                return;
            }
            for (Entry entry : standing) {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                        Identifier.parse(entry.dimension())));
                if (level == null) {
                    continue;
                }
                BlockPos pos = BlockPos.of(entry.pos());
                level.getChunk(pos);    // loads it; there are never more than a handful
                level.setBlock(pos, entry.below(), LIFT);
                level.setBlock(pos.above(), entry.above(), LIFT);
                HorseGenetics.LOGGER.warn("A horse's chest was left standing at {} in {} when the server last"
                        + " stopped; the block is put back. The horse still has the chest.", pos, entry.dimension());
            }
            standing.clear();
            setDirty();
        }
    }
}
