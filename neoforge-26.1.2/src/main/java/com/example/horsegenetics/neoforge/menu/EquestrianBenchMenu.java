package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * <b>The Equestrian Bench's menu</b> - where a saddle's three zones are dyed.
 *
 * <h2>Loom-shaped, not furnace-shaped</h2>
 * It computes its result from its input slots the instant they change and holds
 * no state worth saving, so unlike the
 * {@link ResearchShelfMenu research shelf} there is <b>no block entity</b>:
 * {@link ContainerLevelAccess} is the whole connection to the world, exactly as
 * vanilla's loom does it. Close the screen and nothing is lost because nothing
 * was in progress.
 *
 * <h2>One zone per use, and that is the price</h2>
 * Three slots: the saddle, one material, and the result. The material is a dye
 * for the leather zones or an ingot or gem for the metal, and taking the result
 * spends one of each - so recolouring all three zones costs three dyes and three
 * visits, and changing your mind costs another. That is the owner's decision
 * recorded on <a href="https://9-7-8.github.io/Procedural-Horse-Coats/wiki/roadmap.html#equestrian-bench">roadmap
 * &sect;25</a>, and it is what keeps the bench an economy building rather than a
 * settings screen.
 *
 * <h2>The zone is a button, not a packet</h2>
 * Which zone is being dyed travels through {@link #clickMenuButton}, which is
 * vanilla's own plumbing - the same route the loom's pattern picker uses. No
 * custom payload is needed because the choice encodes as a small int.
 */
public final class EquestrianBenchMenu extends AbstractContainerMenu {

    public static final int SLOT_SADDLE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_RESULT = 2;
    private static final int SLOT_COUNT = 3;

    /** Zone ids, and they are {@link SaddleTint}'s layer indices on purpose. */
    public static final int ZONE_SEAT = SaddleTint.LAYER_SEAT;
    public static final int ZONE_BRIDLE = SaddleTint.LAYER_BRIDLE;
    public static final int ZONE_METAL = SaddleTint.LAYER_METAL;

    // ------------------------------------------------------------------
    // Layout, owned here so the screen has one place to read it from.
    // ------------------------------------------------------------------
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int MARGIN = 8;
    public static final int SADDLE_X = 16;
    public static final int MATERIAL_X = 16;
    public static final int SADDLE_Y = 22;
    public static final int MATERIAL_Y = 48;
    public static final int RESULT_X = 143;
    public static final int RESULT_Y = 35;
    public static final int ZONE_BUTTON_X = 46;
    public static final int ZONE_BUTTON_Y = 20;
    public static final int INV_Y = 84;

    /**
     * What the metal zone costs, and what colour each material gives. Iron is
     * the steel the saddle already wears, so choosing it is how you put the
     * hardware back to plain - its value is the metal layer's own undyed
     * constant, which reproduces vanilla exactly.
     */
    public static final java.util.Map<net.minecraft.world.item.Item, Integer> METALS =
            java.util.Map.of(
                    Items.IRON_INGOT, 0x717171,
                    Items.GOLD_INGOT, 0xE0B94A,
                    Items.COPPER_INGOT, 0xC06A44,
                    Items.NETHERITE_INGOT, 0x4A4248,
                    Items.DIAMOND, 0xB8E8E4,
                    Items.EMERALD, 0x3FBF6F,
                    Items.AMETHYST_SHARD, 0xA079D8);

    private final ContainerLevelAccess access;
    private final DataSlot zone = DataSlot.standalone();
    private long lastSoundTime;

    private final Container input = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            EquestrianBenchMenu.this.slotsChanged(this);
        }
    };
    private final Container output = new SimpleContainer(1);

    /** Client constructor - the menu type hands us no world access. */
    public EquestrianBenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public EquestrianBenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.EQUESTRIAN_BENCH.get(), containerId);
        this.access = access;

        addSlot(new Slot(input, 0, SADDLE_X, SADDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.SADDLE);
            }
        });
        addSlot(new Slot(input, 1, MATERIAL_X, MATERIAL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isDye(stack) || METALS.containsKey(stack.getItem());
            }
        });
        addSlot(new Slot(output, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack taken) {
                // Loom semantics: one saddle and one material per result taken.
                getSlot(SLOT_SADDLE).remove(1);
                getSlot(SLOT_MATERIAL).remove(1);
                access.execute((level, pos) -> {
                    long now = level.getGameTime();
                    if (lastSoundTime != now) {
                        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        lastSoundTime = now;
                    }
                });
                super.onTake(taker, taken);
            }
        });

        addStandardInventorySlots(inventory, MARGIN, INV_Y);
        addDataSlot(zone);
    }

    /** A dye, by the component rather than the item class, as the loom tests it. */
    private static boolean isDye(ItemStack stack) {
        return stack.has(DataComponents.DYE);
    }

    /** Which zone the next dye lands on. */
    public int zone() {
        return zone.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId < ZONE_SEAT || buttonId > ZONE_METAL) {
            return false;
        }
        zone.set(buttonId);
        slotsChanged(input);
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        output.setItem(0, result());
        broadcastChanges();
    }

    /**
     * The saddle as it would be with this zone recoloured, or empty when the
     * bench has nothing to do - no saddle, no material, or a material that does
     * not suit the chosen zone (a dye cannot colour steel, an ingot cannot
     * colour leather).
     */
    private ItemStack result() {
        ItemStack saddle = input.getItem(0);
        ItemStack material = input.getItem(1);
        if (saddle.isEmpty() || material.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Integer colour = colourFor(material, zone.get());
        if (colour == null) {
            return ItemStack.EMPTY;
        }
        SaddleTint current = saddle.getOrDefault(ModDataComponents.TACK_TINT.get(), untinted());
        SaddleTint next = switch (zone.get()) {
            case ZONE_SEAT -> new SaddleTint(colour, current.bridle(), current.metal());
            case ZONE_BRIDLE -> new SaddleTint(current.seat(), colour, current.metal());
            default -> new SaddleTint(current.seat(), current.bridle(), colour);
        };
        if (next.equals(current)) {
            return ItemStack.EMPTY;   // already that colour; do not charge for nothing
        }
        ItemStack out = saddle.copyWithCount(1);
        out.set(ModDataComponents.TACK_TINT.get(), next);
        return out;
    }

    /**
     * The three undyed constants from {@code assets/minecraft/equipment/saddle.json}.
     * A saddle with no component has never been dyed, and this is what it looks
     * like - so recolouring one zone keeps the other two exactly as they were
     * rather than resetting them to white.
     */
    private static SaddleTint untinted() {
        return new SaddleTint(0xA06540, 0xA06540, 0x717171);
    }

    private static Integer colourFor(ItemStack material, int zone) {
        if (zone == ZONE_METAL) {
            return METALS.get(material.getItem());
        }
        DyeColor dye = material.get(DataComponents.DYE);
        return dye == null ? null : dye.getTextureDiffuseColor() & 0xFFFFFF;
    }

    /**
     * Shift-click. Out of the bench into the player; from the player, a saddle
     * finds the saddle slot and anything else tries the material slot -
     * {@code moveItemStackTo} asks each slot's {@code mayPlace}, so a stick goes
     * nowhere rather than into the wrong one.
     */
    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (stack.is(Items.SADDLE)) {
            if (!moveItemStackTo(stack, SLOT_SADDLE, SLOT_SADDLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SLOT_MATERIAL, SLOT_MATERIAL + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (index == SLOT_RESULT) {
            slot.onTake(who, original);
        }
        return original;
    }

    /** Give the inputs back rather than eating them when the screen closes. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.EQUESTRIAN_BENCH.get());
    }
}
