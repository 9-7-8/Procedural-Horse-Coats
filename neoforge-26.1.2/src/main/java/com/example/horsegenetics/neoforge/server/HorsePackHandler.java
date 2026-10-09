package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.pack.PackBox;
import com.example.horsegenetics.common.pack.PackLoad;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorsePackLoad;
import com.example.horsegenetics.neoforge.data.HorsePacks;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.HorseStorage;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.menu.HorsePackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Chests on horses.</b> Hang anything that stores items on either flank,
 * open it by clicking it, and the horse carries what is in it.
 *
 * <h2>Three places, one rule each</h2>
 * <ul>
 *   <li><b>The chest</b> is a worn piece: a stack in {@code HorseGear} under
 *       {@link HorseTackSlot#SADDLEBAG_LEFT} or {@code _RIGHT}, put on and
 *       taken off like a braid. What counts as one is {@link HorseStorage}.</li>
 *   <li><b>The contents</b> are {@link HorsePacks}, saved on the horse and
 *       never synced. A menu is a window straight onto them
 *       ({@link PackContainer}).</li>
 *   <li><b>The load</b> is a count of those items against the horse's pulling
 *       ability - {@link PackLoad}, in {@code common/} - applied here as a
 *       movement-speed modifier and told to clients as {@link HorsePackLoad}.</li>
 * </ul>
 *
 * <h2>The chest's own screen</h2>
 * Nothing about the chest's screen is changed and no slot is ever locked
 * (owner, 2026-10-08): a chest offers the slots it has, and the horse answers
 * for what goes in them with its speed. A size vanilla already has a screen
 * for opens vanilla's - {@code generic_9xN}, the shulker box's - so it looks,
 * sorts and shift-clicks exactly as the block does. Any other size (a shelf's
 * three, a modded crate's hundred) opens {@link HorsePackMenu}, a plain grid.
 *
 * <h2>The ender chest</h2>
 * Opens the <i>opening player's</i> ender chest - which is what an ender chest
 * does wherever it stands - and nothing in it is on the horse, so nothing in it
 * is weighed.
 *
 * <h2>Nothing is lost, nothing is doubled</h2>
 * A chest with things in it will not come off ({@link #mayTakeOff}); a shulker
 * box comes off with them packed inside ({@link #pack}). A horse that dies
 * drops both chests and everything in them ({@link #onDrops}); one that is
 * sold or turned loose hands them back first ({@link #giveBack}); one raised
 * from the afterlife comes back without them, because they are already on the
 * ground where it fell ({@link #strip}). Every other way a horse leaves and
 * returns - a stasis chamber, a were-night, a ticket - is a whole-entity save,
 * and the chests ride in it.
 */
@EventBusSubscriber
public final class HorsePackHandler {

    /** The two flanks, in the order a load is counted and a side is tried. */
    public static final HorseTackSlot[] SLOTS = {HorseTackSlot.SADDLEBAG_LEFT, HorseTackSlot.SADDLEBAG_RIGHT};

    private static final Identifier LOAD_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "pack_load");

    /** How close a player must stay to a chest they have open - the Dress window's distance. */
    private static final double REACH = 8.0;

    private HorsePackHandler() {
    }

    /** The slot on that flank. */
    public static HorseTackSlot slotOn(PackBox.Side side) {
        return side == PackBox.Side.LEFT ? HorseTackSlot.SADDLEBAG_LEFT : HorseTackSlot.SADDLEBAG_RIGHT;
    }

    private static HorsePacks packs(AbstractHorse horse) {
        return horse.getData(ModAttachments.HORSE_PACKS);
    }

    // ------------------------------------------------------------------
    // Putting a chest on, and taking one off
    // ------------------------------------------------------------------

    /**
     * A chest going onto {@code slot}: whatever it holds <i>as an item</i> -
     * a shulker box's contents, a chest picked with its inventory - moves into
     * the horse's store, and the stack that is returned to be worn has none.
     * A stack that will not fit stays inside the item rather than vanishing.
     */
    public static ItemStack unpack(AbstractHorse horse, HorseTackSlot slot, ItemStack chest) {
        if (HostedPacks.hosts(chest)) {
            // Another mod's chest is carried whole, and opened as its own block.
            ItemStack face = HostedPacks.adopt(horse, slot, chest);
            if (face != null) {
                return face;
            }
        }
        ItemContainerContents inside = chest.get(DataComponents.CONTAINER);
        // Nothing inside is the ordinary case and must leave the stack exactly
        // as it was: a barrel and a shulker box carry an EMPTY container
        // component by default, and one that came back without it would never
        // stack with another again.
        if (inside == null || !inside.nonEmptyItems().iterator().hasNext()) {
            return chest;
        }
        HorsePacks packs = packs(horse);
        NonNullList<ItemStack> stacks = NonNullList.withSize(Math.max(1, inside.getSlots()), ItemStack.EMPTY);
        inside.copyInto(stacks);
        List<ItemStack> left = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty() && !packs.add(slot.name(), i, stack)) {
                left.add(stack);
            }
        }
        ItemStack worn = chest.copy();
        if (!left.isEmpty()) {
            worn.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(left));
            return worn;
        }
        // Back to what a fresh one of these carries, which is not always "no
        // component" - see above.
        ItemContainerContents fresh = worn.getItem().components().get(DataComponents.CONTAINER);
        if (fresh == null) {
            worn.remove(DataComponents.CONTAINER);
        } else {
            worn.set(DataComponents.CONTAINER, fresh);
        }
        return worn;
    }

    /**
     * Whether the chest on {@code slot} may come off: it is empty, or it is the
     * kind that leaves with its contents inside. The server counts the chest;
     * a client reads the count it was sent, which is the same number.
     */
    public static boolean mayTakeOff(AbstractHorse horse, HorseTackSlot slot) {
        ItemStack worn = slot.on(horse);
        if (worn.isEmpty() || HorseStorage.keepsContents(worn)) {
            return true;
        }
        if (horse.level().isClientSide()) {
            return horse.getData(ModAttachments.HORSE_PACK_LOAD).in(slot == HorseTackSlot.SADDLEBAG_LEFT) == 0;
        }
        return packs(horse).count(slot.name()) == 0;
    }

    /**
     * A chest coming off {@code slot}: for the kind that keeps its contents,
     * they go back inside the item and out of the horse's store. Called only
     * after {@link #mayTakeOff}, so for anything else there is nothing to move.
     */
    public static ItemStack pack(AbstractHorse horse, HorseTackSlot slot, ItemStack chest) {
        if (HostedPacks.holds(horse, slot)) {
            ItemStack whole = HostedPacks.view(horse, slot, chest);
            HostedPacks.forget(horse, slot);
            return whole;
        }
        if (!HorseStorage.keepsContents(chest)) {
            return chest;
        }
        List<ItemStack> stacks = packs(horse).take(slot.name());
        boolean any = false;
        for (ItemStack stack : stacks) {
            any |= !stack.isEmpty();
        }
        if (any) {
            chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        }
        return chest;
    }

    /**
     * What a player holding the chest on {@code slot} would be holding: the worn
     * stack, with a copy of its contents inside it if it is the kind that keeps
     * them. Nothing moves - this is for a menu to <i>show</i>, and to hand to
     * vanilla's click code, which reads a slot before it clears it. The store is
     * emptied by {@link #pack} or {@link #departed}, whichever the click turns
     * out to be.
     */
    public static ItemStack packedView(AbstractHorse horse, HorseTackSlot slot, ItemStack chest) {
        if (chest.isEmpty() || horse.level().isClientSide()) {
            return chest;
        }
        if (HostedPacks.holds(horse, slot)) {
            return HostedPacks.view(horse, slot, chest);
        }
        if (!HorseStorage.keepsContents(chest)) {
            return chest;
        }
        HorsePacks packs = packs(horse);
        int used = packs.used(slot.name());
        if (used == 0) {
            return chest;
        }
        List<ItemStack> copies = new ArrayList<>(used);
        for (int i = 0; i < used; i++) {
            copies.add(packs.get(slot.name(), i).copy());
        }
        ItemStack shown = chest.copy();
        shown.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(copies));
        return shown;
    }

    /**
     * The chest on {@code slot} has left in somebody's hand as its
     * {@link #packedView}, so the contents it took with it are no longer here.
     * Only for a keeps-its-contents chest: anything else left empty, and stacks
     * still filed under the slot are stranded ones that must not be thrown away.
     */
    public static void departed(AbstractHorse horse, HorseTackSlot slot) {
        if (!horse.level().isClientSide() && HostedPacks.holds(horse, slot)) {
            HostedPacks.forget(horse, slot);
            return;
        }
        if (!horse.level().isClientSide() && HorseStorage.keepsContents(slot.on(horse))) {
            packs(horse).take(slot.name());
        }
    }

    /**
     * Both chests and everything in them, off the horse, as a list of stacks to
     * hand to somebody or drop. A keeps-its-contents chest leaves full; any
     * other leaves empty with its contents beside it.
     */
    public static List<ItemStack> unload(AbstractHorse horse) {
        List<ItemStack> out = new ArrayList<>();
        HorsePacks packs = packs(horse);
        for (HorseTackSlot slot : SLOTS) {
            ItemStack worn = slot.on(horse).copy();
            if (!worn.isEmpty()) {
                out.add(pack(horse, slot, worn));
                slot.set(horse, ItemStack.EMPTY);
            }
        }
        // And whatever is stored under any name at all - a chest's contents,
        // or stacks stranded behind a slot whose chest has gone.
        for (String name : packs.slots()) {
            for (ItemStack stack : packs.take(name)) {
                if (!stack.isEmpty()) {
                    out.add(stack);
                }
            }
        }
        refresh(horse);
        return out;
    }

    /**
     * The chests and their contents back to {@code player} - into their
     * inventory, or at their feet - or onto the ground under the horse when
     * there is nobody to give them to. For a horse that is leaving its owner
     * without dying: sold, or turned loose.
     */
    public static void giveBack(AbstractHorse horse, @Nullable Player player) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        for (ItemStack stack : unload(horse)) {
            if (player == null) {
                horse.spawnAtLocation(level, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /**
     * Remove both chests and their contents and give them to nobody. Only for a
     * horse being raised from a snapshot taken as it died: those same stacks
     * are on the ground where it fell, and keeping them here would mint a
     * second set. See {@code HorseResurrection.revive}.
     */
    public static void strip(AbstractHorse horse) {
        unload(horse);
    }

    // ------------------------------------------------------------------
    // The load
    // ------------------------------------------------------------------

    /**
     * Recount what the horse carries, tell the clients, and set its speed.
     * Server side; cheap enough to call on every change to a chest, which is
     * when it is called.
     *
     * <p>The modifier is <b>transient</b> and re-made when the horse joins a
     * level, like the carts' - so a config change, or a pulling score that
     * resolves differently after an update, is picked up on the next load
     * rather than baked into the save.
     */
    public static void refresh(AbstractHorse horse) {
        if (horse.level().isClientSide()) {
            return;
        }
        HorsePacks packs = packs(horse);
        ItemStack left = HorseTackSlot.SADDLEBAG_LEFT.on(horse);
        ItemStack right = HorseTackSlot.SADDLEBAG_RIGHT.on(horse);
        HorsePackLoad load = HorsePackLoad.of(
                packs.count(HorseTackSlot.SADDLEBAG_LEFT.name()), HorseStorage.weightless(left),
                packs.count(HorseTackSlot.SADDLEBAG_RIGHT.name()), HorseStorage.weightless(right));
        if (!load.equals(horse.getData(ModAttachments.HORSE_PACK_LOAD))) {
            horse.setData(ModAttachments.HORSE_PACK_LOAD, load);
        }

        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double modifier = PackLoad.speedModifier(ServerConfig.packCurve(),
                PackLoad.lightened(load.weighed(), harnessReduction(horse)), HorseDraft.pullOf(horse));
        AttributeModifier current = speed.getModifier(LOAD_MODIFIER_ID);
        if (modifier >= 0.0) {
            if (current != null) {
                speed.removeModifier(LOAD_MODIFIER_ID);
            }
        } else if (current == null || current.amount() != modifier) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    LOAD_MODIFIER_ID, modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /**
     * The share of its load this horse's harness takes off: its fittings' tier
     * of the server's {@code packs.harness_best_reduction}, and nothing for a
     * horse wearing none. Both sides - the Gear tab works the same sum out.
     */
    public static double harnessReduction(AbstractHorse horse) {
        return HorseTackSlot.HARNESS.on(horse).getItem()
                instanceof com.example.horsegenetics.neoforge.item.StorageHarnessItem harness
                ? harness.reduction() : 0.0;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Horse horse
                && (!HorseTackSlot.SADDLEBAG_LEFT.on(horse).isEmpty()
                || !HorseTackSlot.SADDLEBAG_RIGHT.on(horse).isEmpty()
                || !packs(horse).isEmpty())) {
            refresh(horse);
        }
    }

    // ------------------------------------------------------------------
    // Death
    // ------------------------------------------------------------------

    /**
     * <b>A dead horse sets its chests down where it fell</b> - each one a real
     * block in the nearest open space, with everything still inside it.
     * (Owner, 2026-10-08, #221; it spilled them as items before, as a donkey
     * does.) A chest that cannot be set down - no room, somebody's claim, a
     * block that refuses - is dropped as it used to be, and so is anything that
     * was on the horse and does not fit the placed block.
     *
     * <p>{@code LOWEST}, so it runs after {@code GeneDeathHandler.onHorseDrops},
     * which removes the horse's loot for some genes: a horse that turns to
     * glass when it dies still leaves your luggage.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        List<ItemStack> loose = new ArrayList<>();
        if (ServerConfig.packPlaceOnDeath()) {
            for (HorseTackSlot slot : SLOTS) {
                if (!slot.on(horse).isEmpty()) {
                    setDown(level, horse, slot, loose);
                }
            }
        }
        loose.addAll(unload(horse));
        for (ItemStack stack : loose) {
            event.getDrops().add(new ItemEntity(level, horse.getX(), horse.getY() + 0.5, horse.getZ(), stack));
        }
    }

    /** How far from where the horse fell a chest may be set down, across and up or down. */
    private static final int SET_DOWN_REACH = 3;
    private static final int SET_DOWN_RISE = 2;

    /**
     * Set {@code slot}'s chest down near the horse, full. On success the slot
     * and its store are empty and anything that did not fit is in
     * {@code loose}; on failure nothing has moved and the caller drops it.
     */
    private static boolean setDown(ServerLevel level, Horse horse, HorseTackSlot slot, List<ItemStack> loose) {
        BlockPos pos = openSpace(level, horse.blockPosition());
        if (pos == null) {
            return false;
        }
        // Somebody's claim says no: the same question a placing hand is asked.
        if (net.neoforged.neoforge.event.EventHooks.onBlockPlace(horse,
                net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos),
                net.minecraft.core.Direction.UP)) {
            return false;
        }
        net.minecraft.world.level.block.state.BlockState before = level.getBlockState(pos);
        boolean whole = HostedPacks.holds(horse, slot);
        // A chest carried whole goes down as the whole item; any other goes down
        // as what is worn and is then filled from the horse's store.
        ItemStack chest = whole ? HostedPacks.view(horse, slot, slot.on(horse)) : slot.on(horse);
        try {
            net.minecraft.world.level.block.entity.BlockEntity entity =
                    HostedPacks.setDown(level, pos, chest, null, net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (entity == null && !HorseStorage.isEnderChest(chest)) {
                level.setBlock(pos, before, net.minecraft.world.level.block.Block.UPDATE_ALL);
                return false;
            }
            if (whole) {
                HostedPacks.forget(horse, slot);
            } else {
                List<ItemStack> stacks = packs(horse).take(slot.name());
                for (int i = 0; i < stacks.size(); i++) {
                    ItemStack stack = stacks.get(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (entity instanceof Container container && i < container.getContainerSize()
                            && container.getItem(i).isEmpty()) {
                        container.setItem(i, stack);
                    } else {
                        loose.add(stack);
                    }
                }
            }
        } catch (RuntimeException | LinkageError failed) {
            HorseGenetics.LOGGER.warn("Could not set a dead horse's {} down at {}; it is dropped instead: {}",
                    chest.getItem(), pos, failed.toString());
            level.setBlock(pos, before, net.minecraft.world.level.block.Block.UPDATE_ALL
                    | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            return false;
        }
        slot.set(horse, ItemStack.EMPTY);
        return true;
    }

    /**
     * The nearest place a chest can stand: empty (or only grass and the like),
     * dry, and for preference with something under it. Nearest first, so a
     * horse that dies in a field leaves its chests where it lay and one that
     * dies in a tunnel leaves them in the tunnel.
     */
    private static @Nullable BlockPos openSpace(ServerLevel level, BlockPos fell) {
        BlockPos floating = null;
        double floatingDistance = Double.MAX_VALUE;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                fell.offset(-SET_DOWN_REACH, -SET_DOWN_RISE, -SET_DOWN_REACH),
                fell.offset(SET_DOWN_REACH, SET_DOWN_RISE, SET_DOWN_REACH))) {
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                continue;
            }
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || level.getBlockEntity(pos) != null) {
                continue;
            }
            double distance = pos.distSqr(fell);
            if (!level.getBlockState(pos.below()).canBeReplaced()) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            } else if (distance < floatingDistance) {
                floatingDistance = distance;
                floating = pos.immutable();
            }
        }
        return best != null ? best : floating;
    }

    // ------------------------------------------------------------------
    // Opening one
    // ------------------------------------------------------------------

    /**
     * Whether {@code player} may open a chest on {@code horse} right now: it is
     * alive, it is theirs, and they are beside it or on it.
     */
    public static boolean mayOpen(Horse horse, Player player) {
        return horse.isAlive() && !horse.isRemoved() && horse.level() == player.level()
                && horse.closerThan(player, REACH)
                && HorseOwnership.isOwner(horse, player.getUUID());
    }

    /** Open the chest on {@code slot} for {@code player}. False if there is none or they may not. */
    public static boolean open(ServerPlayer player, Horse horse, HorseTackSlot slot) {
        ItemStack worn = slot.on(horse);
        if (worn.isEmpty() || !slot.isStorage() || !mayOpen(horse, player)) {
            return false;
        }
        if (HorseStorage.isEnderChest(worn)) {
            // The player's own, as an ender chest is wherever it stands. The
            // menu is vanilla's; only "still valid" is ours, because the block
            // it would normally measure the distance to is walking about.
            player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
                    new ChestMenu(MenuType.GENERIC_9x3, id, inventory, who.getEnderChestInventory(), 3) {
                        @Override
                        public boolean stillValid(Player asking) {
                            return mayOpen(horse, asking) && HorseStorage.isEnderChest(slot.on(horse));
                        }
                    }, Component.translatable("container.enderchest")));
            player.awardStat(Stats.OPEN_ENDERCHEST);
            play(horse, SoundEvents.ENDER_CHEST_OPEN);
            return true;
        }

        if (HostedPacks.holds(horse, slot) && HostedPacks.hosts(worn) && HostedPacks.open(player, horse, slot)) {
            play(horse, openSound(worn));
            return true;
        }

        int slots = slots(horse, slot, worn);
        if (slots <= 0) {
            return false;
        }
        boolean nests = HorseStorage.keepsContents(worn);
        PackContainer container = new PackContainer(horse, slot, worn, slots, nests);
        Component title = worn.getHoverName();
        if (nests && slots == 27) {
            player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
                    new ShulkerBoxMenu(id, inventory, container), title));
        } else if (!nests && slots % 9 == 0 && slots <= 54) {
            int rows = slots / 9;
            player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
                    new ChestMenu(rowsType(rows), id, inventory, container, rows), title));
        } else {
            player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
                    new HorsePackMenu(id, inventory, container), title),
                    buffer -> {
                        buffer.writeVarInt(slots);
                        buffer.writeBoolean(nests);
                    });
        }
        play(horse, openSound(worn));
        return true;
    }

    /**
     * How many slots the chest on {@code slot} shows: what the chest says, and
     * never fewer than it already holds - a chest whose mod shrank it must not
     * hide a player's things.
     */
    private static int slots(AbstractHorse horse, HorseTackSlot slot, ItemStack worn) {
        int declared = HorseStorage.slots(worn, horse.level(), horse.blockPosition());
        return Math.min(HorsePacks.MAX_SLOTS, Math.max(declared, packs(horse).used(slot.name())));
    }

    private static MenuType<?> rowsType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private static SoundEvent openSound(ItemStack worn) {
        if (worn.getItem() instanceof BlockItem item) {
            if (item.getBlock() instanceof ShulkerBoxBlock) {
                return SoundEvents.SHULKER_BOX_OPEN;
            }
            if (item.getBlock() instanceof BarrelBlock) {
                return SoundEvents.BARREL_OPEN;
            }
        }
        return SoundEvents.CHEST_OPEN;
    }

    private static void play(Horse horse, SoundEvent sound) {
        horse.level().playSound(null, horse.blockPosition(), sound, SoundSource.NEUTRAL, 0.5F,
                horse.level().getRandom().nextFloat() * 0.1F + 0.9F);
    }

    /**
     * <b>Right-click the chest and it opens.</b> Anywhere else on the horse does
     * what it always did - which is usually to get on.
     *
     * <p>The test is the player's own line of sight against the box the chest
     * is drawn in ({@link PackBox}), not which face of the hitbox was struck: a
     * horse's hitbox is a square column that does not turn with it, so "the
     * left face" is the horse's flank only one time in four.
     *
     * <p>{@code LOW}: after every interaction that claims a click by what is in
     * the hand - a carrot is still fed, a paper still read - and before
     * {@code TackEquipHandler}, so a second chest in the hand opens the first
     * when that is what was clicked. Sneaking is left alone; it means "read
     * this horse".
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onClickChest(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        boolean hasLeft = !HorseTackSlot.SADDLEBAG_LEFT.on(horse).isEmpty();
        boolean hasRight = !HorseTackSlot.SADDLEBAG_RIGHT.on(horse).isEmpty();
        if (!hasLeft && !hasRight) {
            return;
        }
        Player player = event.getEntity();
        if (player.isSecondaryUseActive()) {
            return;
        }
        Vec3 eye = player.getEyePosition().subtract(horse.position());
        Vec3 look = player.getLookAngle();
        PackBox.Side side = PackBox.hit(eye.x, eye.y, eye.z, look.x, look.y, look.z,
                player.entityInteractionRange(), horse.yBodyRot, horse.getScale(), hasLeft, hasRight);
        if (side == null) {
            return;
        }
        if (!horse.isTamed()) {
            return;     // somebody else's story - a wild horse wearing a chest is not ours to explain
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            if (player instanceof ServerPlayer told) {
                String name = HorseRecords.hasRealRecord(horse)
                        ? HorseRecords.of(horse).displayName() : "That horse";
                told.sendSystemMessage(Component.translatable("message.horsegenetics.pack.not_yours", name), true);
            }
        } else if (player instanceof ServerPlayer opener) {
            open(opener, horse, slotOn(side));
        }
        // Cancelled on both sides, as TackEquipHandler does: uncancelled, the
        // client predicts a mount and the horse flickers under the player.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ------------------------------------------------------------------
    // The container a menu sees
    // ------------------------------------------------------------------

    /**
     * <b>One chest's contents, as a container.</b> Server only, and it stores
     * nothing: every read and write is the horse's {@link HorsePacks}, and the
     * stacks it hands out are the live ones, because vanilla's menus grow and
     * shrink a slot's stack in place and a copy would lose that.
     */
    public static final class PackContainer implements Container {

        private final Horse horse;
        private final HorseTackSlot slot;
        private final ItemStack chest;
        private final int size;
        private final boolean nests;

        PackContainer(Horse horse, HorseTackSlot slot, ItemStack chest, int size, boolean nests) {
            this.horse = horse;
            this.slot = slot;
            this.chest = chest.copy();
            this.size = size;
            this.nests = nests;
        }

        private HorsePacks packs() {
            return HorsePackHandler.packs(horse);
        }

        @Override
        public int getContainerSize() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return packs().used(slot.name()) == 0;
        }

        @Override
        public ItemStack getItem(int index) {
            return packs().get(slot.name(), index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack held = getItem(index);
            if (held.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = held.split(count);
            if (held.isEmpty()) {
                packs().set(slot.name(), index, ItemStack.EMPTY);
            }
            setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack held = getItem(index);
            packs().set(slot.name(), index, ItemStack.EMPTY);
            return held;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (index < 0 || index >= size) {
                return;
            }
            packs().set(slot.name(), index, stack);
            setChanged();
        }

        /** A chest that travels with its contents takes nothing that is one itself. */
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return !nests || stack.canFitInsideContainerItems();
        }

        /** Whether this is the kind of chest that refuses another of its kind - for the menu's slots. */
        public boolean nests() {
            return nests;
        }

        @Override
        public void setChanged() {
            refresh(horse);
        }

        /**
         * The chest is shut: recount once more. {@code removeItemNoUpdate} is by
         * contract silent, so a menu that used it has changed the load without
         * saying so.
         */
        @Override
        public void stopOpen(net.minecraft.world.entity.ContainerUser user) {
            refresh(horse);
        }

        /**
         * Still that horse, still beside it, still theirs - and still the chest
         * that was opened: a container outliving the chest it was a window onto
         * would be writing into a slot with nothing hanging there.
         */
        @Override
        public boolean stillValid(Player player) {
            return mayOpen(horse, player) && ItemStack.isSameItem(slot.on(horse), chest);
        }

        /** Deliberately nothing, for {@code GearView}'s reason: there is nobody to hand the stacks to. */
        @Override
        public void clearContent() {
        }
    }
}
