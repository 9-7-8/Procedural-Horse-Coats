package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.server.HorseOwnership;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <b>Dressing a horse</b> - its nineteen gear slots on a paper doll, and the
 * player's own inventory on the right, as a real container: a carried stack,
 * dragging, shift-click.
 *
 * <h2>Why this is a menu when the information screen is not</h2>
 * {@code HorseInfoScreen} is a plain screen on purpose. It shows horses that
 * are not loaded entities at all - a foal on the Offspring tab, a name in the
 * family tree - and a container menu needs a live entity on the server to hang
 * off. So the Gear tab keeps its read-only doll, which works on any horse, and
 * gains a <i>Dress</i> button into this, which opens only on a live horse the
 * player owns and is standing near. (Owner's call, 2026-09-30: a separate
 * screen rather than converting the information screen.)
 *
 * <h2>The menu keeps nothing</h2>
 * The gear already lives in two places - the saddle and barding in vanilla's
 * equipment, the other seventeen in the {@code HORSE_GEAR} attachment - and a
 * third copy here would be one that could drift from both. On the server the
 * tack slots are a {@link GearView} over the horse itself, and every write goes
 * through {@link HorseTackSlot#set}, the one place that knows which backing a
 * slot has. On the client they are a plain mirror the menu sync fills, like
 * every vanilla client menu - writing through to the client's copy of the horse
 * would be predicting the server's state rather than displaying it.
 *
 * <h2>The rules are the old packet's rules</h2>
 * Everything {@code ModNetworking.handleTackSlot} checked in one place is slot
 * behaviour here, which is a wider surface: a slot drawn in the wrong place is
 * a cosmetic bug, a slot with the wrong rule is a dupe or a deletion.
 * <ul>
 *   <li><b>What fits</b> is {@link HorseTackSlot#accepts} and nothing else -
 *       {@code mayPlace} delegates, it does not restate. That also carries
 *       {@code usableOn}, so a foal's slots refuse.</li>
 *   <li><b>One piece per slot</b>, from {@link GearView#getMaxStackSize}. It is
 *       also what keeps vanilla's merge paths - which grow the stack a slot
 *       returned without writing it back - from ever running here.</li>
 *   <li><b>Owner, and within eight blocks</b>, re-checked every tick by
 *       {@link #stillValid}, not once at the door: a menu that stays open while
 *       the player walks away is a menu that equips a horse from anywhere.</li>
 *   <li><b>Nothing is destroyed.</b> Whatever is on the cursor when the menu
 *       closes - by Escape, by walking off, by the horse dying - goes back
 *       through {@link AbstractContainerMenu#removed}, which puts it in the
 *       inventory or at the player's feet.</li>
 * </ul>
 */
public final class HorseGearMenu extends AbstractContainerMenu {

    public static final int TACK_SLOTS = HorseTackSlot.values().length;
    private static final int INV_START = TACK_SLOTS;
    private static final int INV_END = INV_START + 36;

    // ------------------------------------------------------------------
    // Layout, owned here so the screen reads it from one place. The doll's
    // slots come from HorseTackSlot.anchorX/anchorY as a fraction of this box,
    // the same rule the information screen's Gear tab uses - so the roster is
    // still the layout, and a new slot moves nothing here.
    //
    // The box's size is the smallest the anchors survive: the tightest pairs
    // (pad/saddle and the two near boots across; boot/shoe down) sit 0.12 and
    // 0.16 of the box apart, which needs a box of at least 168 x 131 before two
    // eighteen-pixel slots overlap.
    // ------------------------------------------------------------------

    public static final int MARGIN = 8;
    public static final int TITLE_Y = 6;
    public static final int SLOT = 18;

    public static final int DOLL_X = MARGIN;
    public static final int DOLL_Y = 18;
    public static final int DOLL_W = 216;
    public static final int DOLL_H = 160;

    /** Where the horse sits inside the doll - the information screen's fractions. */
    public static final float PORTRAIT_X0 = 0.27f;
    public static final float PORTRAIT_X1 = 0.73f;
    public static final float PORTRAIT_Y0 = 0.13f;
    public static final float PORTRAIT_Y1 = 0.64f;

    /** The player's inventory: on the right, as asked for. */
    public static final int INV_X = DOLL_X + DOLL_W + MARGIN;
    public static final int INV_LABEL_Y = DOLL_Y;
    public static final int INV_Y = DOLL_Y + 12;
    /** Under the hotbar - where the screen says what the doll is for. */
    public static final int NOTE_Y = INV_Y + 58 + 18 + 8;

    public static final int WIDTH = INV_X + 9 * 18 + MARGIN;
    public static final int HEIGHT = DOLL_Y + DOLL_H + MARGIN;

    /** Where tack slot {@code slot}'s item sits, window-relative - a vanilla slot position. */
    public static int slotX(HorseTackSlot slot) {
        return DOLL_X + Math.round((DOLL_W - SLOT) * slot.anchorX());
    }

    public static int slotY(HorseTackSlot slot) {
        return DOLL_Y + Math.round((DOLL_H - SLOT) * slot.anchorY());
    }

    private final @Nullable AbstractHorse horse;
    private final int horseId;

    /**
     * <b>Client constructor.</b> The entity id arrives in the opening packet,
     * written by {@code ModNetworking.handleOpenHorseGear} - the same extra-data
     * route {@link JumpMenu} takes for a block position. The horse may not be
     * loaded on this client (it should be - the server just checked it is within
     * eight blocks), and then every slot refuses rather than guessing.
     */
    public HorseGearMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(containerId, inventory, extra.readVarInt());
    }

    private HorseGearMenu(int containerId, Inventory inventory, int horseId) {
        this(containerId, inventory,
                inventory.player.level().getEntity(horseId) instanceof AbstractHorse found ? found : null,
                horseId, new SimpleContainer(TACK_SLOTS) {
                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
    }

    /** <b>Server constructor</b> - a window straight onto the horse. */
    public HorseGearMenu(int containerId, Inventory inventory, AbstractHorse horse) {
        this(containerId, inventory, horse, horse.getId(), new GearView(horse));
    }

    private HorseGearMenu(int containerId, Inventory inventory, @Nullable AbstractHorse horse,
                          int horseId, Container tack) {
        super(ModMenus.HORSE_GEAR.get(), containerId);
        this.horse = horse;
        this.horseId = horseId;
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            addSlot(new TackSlot(tack, slot, slotX(slot), slotY(slot)));
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
    }

    /** The horse, on whichever side is asking; null only on a client that lost it. */
    public @Nullable AbstractHorse horse() {
        return horse;
    }

    public int horseId() {
        return horseId;
    }

    /** Which roster entry menu slot {@code index} is, or null for an inventory slot. */
    public static @Nullable HorseTackSlot tackAt(int index) {
        return index >= 0 && index < TACK_SLOTS ? HorseTackSlot.values()[index] : null;
    }

    /**
     * Shift-click. A worn piece comes back to the player; from the player, into
     * whichever gear slot takes it - {@code moveItemStackTo} honours
     * {@code mayPlace} and the one-per-slot limit, so a stack of four of
     * something every hoof takes shoes all four and leaves the rest.
     */
    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, TACK_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(who, stack);
        return original;
    }

    /**
     * A double-click gathers matching stacks onto the cursor from every slot
     * that allows it - which, left alone, would pull the saddle off the horse
     * because the player double-clicked a spare one in their bag. What the horse
     * is wearing comes off only when somebody takes it off.
     */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.index >= TACK_SLOTS && super.canTakeItemForPickAll(carried, slot);
    }

    /**
     * Alive, still the player's, still within the eight blocks the old packet
     * demanded. Server-side the horse is always present; the client's answer
     * does not matter, since the server is the one that closes the menu.
     */
    @Override
    public boolean stillValid(Player who) {
        if (horse == null) {
            return false;
        }
        if (!horse.isAlive() || horse.isRemoved() || !horse.closerThan(who, 8.0)) {
            return false;
        }
        if (who.level().isClientSide()) {
            return true;
        }
        return horse instanceof Horse owned && HorseOwnership.isOwner(owned, who.getUUID());
    }

    /**
     * One gear slot. Every rule is {@link HorseTackSlot}'s, asked of the horse
     * this menu is for - never restated here.
     */
    private final class TackSlot extends Slot {

        private final HorseTackSlot tack;

        TackSlot(Container container, HorseTackSlot tack, int x, int y) {
            super(container, tack.ordinal(), x, y);
            this.tack = tack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return horse != null && tack.accepts(horse, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /**
     * <b>The horse's gear, seen as a container.</b> Server only. It stores
     * nothing: a read is {@link HorseTackSlot#on}, a write is
     * {@link HorseTackSlot#set}, and the attachment's own sync carries the
     * result to every client that draws the horse.
     *
     * <p>Reads hand back a copy, even of the two vanilla slots. The only vanilla
     * paths that mutate a slot's stack in place are the merge paths, and the
     * one-per-slot limit shuts all of them; a copy means that if one is ever
     * missed it loses a count on the screen rather than editing the horse
     * behind {@code set}'s back.
     */
    private static final class GearView implements Container {

        private final AbstractHorse horse;

        GearView(AbstractHorse horse) {
            this.horse = horse;
        }

        private static HorseTackSlot tack(int index) {
            return HorseTackSlot.values()[index];
        }

        @Override
        public int getContainerSize() {
            return TACK_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (HorseTackSlot slot : HorseTackSlot.values()) {
                if (!slot.on(horse).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return tack(index).on(horse).copy();
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            return removeItemNoUpdate(index);
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack worn = tack(index).on(horse).copy();
            if (!worn.isEmpty()) {
                tack(index).set(horse, ItemStack.EMPTY);
            }
            return worn;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            tack(index).set(horse, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return tack(index).accepts(horse, stack);
        }

        @Override
        public void setChanged() {
            // Nothing to flush: set() has already written the horse.
        }

        @Override
        public boolean stillValid(Player player) {
            return true; // the menu's stillValid is the one that decides
        }

        /**
         * Deliberately does nothing. Nothing in this mod calls it, and a
         * container whose clear strips a horse naked - with no player to hand
         * the pieces to - is the one method here that could delete tack.
         */
        @Override
        public void clearContent() {
        }
    }
}
