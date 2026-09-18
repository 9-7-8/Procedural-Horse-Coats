package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * <b>The Equestrian Bench's menu</b> - dyes a saddle's three zones, and names it.
 *
 * <h2>All three at once</h2>
 * There was a zone selector here and it is gone (owner, 2026-09-16). Three
 * material slots, one per zone, each labelled on the screen: fill whichever you
 * care about and take one result. A zone whose slot is empty keeps the colour it
 * already had, so recolouring just the bridle is one material, not a reset.
 *
 * <h2>Loom-shaped, not furnace-shaped</h2>
 * It computes its result the moment a slot changes and keeps nothing between
 * uses, so there is <b>no block entity</b> - {@link ContainerLevelAccess} is the
 * whole connection to the world, exactly as vanilla's loom does it.
 *
 * <h2>One plan, used twice</h2>
 * {@link #plan()} decides both what the result <i>is</i> and which materials
 * paid for it, and {@link #onTakeResult} spends exactly what that plan named.
 * Computing "what does it make" and "what does it cost" separately is how a
 * bench ends up charging for a dye it did not use.
 */
public final class EquestrianBenchMenu extends AbstractContainerMenu {

    public static final int SLOT_SADDLE = 0;
    public static final int SLOT_SEAT = 1;
    public static final int SLOT_BRIDLE = 2;
    public static final int SLOT_METAL = 3;
    public static final int SLOT_RESULT = 4;
    private static final int SLOT_COUNT = 5;

    // ------------------------------------------------------------------
    // Layout, owned here so the screen has one place to read it from. The
    // window is wider and taller than vanilla's 176x166 because it carries a
    // name field and three labelled rows; VanillaPanel draws the frame
    // procedurally rather than from a fixed texture, so the size is free.
    // ------------------------------------------------------------------
    public static final int WIDTH = 194;
    public static final int HEIGHT = 232;
    public static final int MARGIN = 8;
    public static final int TITLE_Y = 6;

    public static final int NAME_X = 8;
    public static final int NAME_Y = 18;
    public static final int NAME_W = 178;
    public static final int NAME_H = 16;

    public static final int SADDLE_X = 8;
    public static final int SADDLE_Y = 42;
    public static final int RESULT_X = 160;
    public static final int RESULT_Y = 42;

    public static final int ZONE_X = 8;
    public static final int SEAT_Y = 70;
    public static final int BRIDLE_Y = 92;
    public static final int METAL_Y = 114;
    public static final int ZONE_LABEL_X = 30;

    public static final int INV_LABEL_Y = 136;
    public static final int INV_Y = 148;

    /**
     * What the metal zone takes, and what colour each gives. Iron is how you put
     * the fittings back to plain steel: its value is the metal layer's own undyed
     * constant, so an iron-fitted saddle is indistinguishable from one that was
     * never dyed.
     */
    public static final Map<Item, Integer> METALS = Map.ofEntries(
            Map.entry(Items.IRON_INGOT, SaddleTint.IRON),
            Map.entry(Items.GOLD_INGOT, SaddleTint.GOLD),
            Map.entry(Items.COPPER_INGOT, SaddleTint.COPPER),
            Map.entry(Items.NETHERITE_INGOT, SaddleTint.NETHERITE),
            Map.entry(Items.DIAMOND, SaddleTint.DIAMOND),
            Map.entry(Items.EMERALD, SaddleTint.EMERALD),
            Map.entry(Items.AMETHYST_SHARD, SaddleTint.AMETHYST),
            Map.entry(Items.QUARTZ, SaddleTint.QUARTZ),
            Map.entry(Items.REDSTONE, SaddleTint.REDSTONE),
            Map.entry(Items.ENDER_PEARL, SaddleTint.ENDER_PEARL),
            Map.entry(Items.BASALT, SaddleTint.BASALT),
            // BONE, deliberately not BONE_MEAL: bone meal carries a DYE component,
            // so it would also satisfy the seat and bridle slots and read as an
            // ambiguous input in a bench whose whole premise is leather-or-metal.
            Map.entry(Items.BONE, SaddleTint.BONE),
            Map.entry(Items.PRISMARINE_SHARD, SaddleTint.PRISMARINE),
            Map.entry(Items.LAPIS_LAZULI, SaddleTint.LAPIS),
            Map.entry(Items.COAL, SaddleTint.COAL));

    /**
     * What this bench works on. A saddle and leather horse armour, and nothing
     * else - iron, gold and diamond horse armour are not dyeable at all, so
     * accepting them would only offer a result that never comes.
     */
    public static boolean isTack(ItemStack stack) {
        return stack.is(Items.SADDLE) || stack.is(Items.LEATHER_HORSE_ARMOR);
    }

    private final ContainerLevelAccess access;
    private long lastSoundTime;
    private String saddleName = "";

    private final Container input = new SimpleContainer(4) {
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

        addSlot(new Slot(input, SLOT_SADDLE, SADDLE_X, SADDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isTack(stack);
            }
        });
        addSlot(dyeSlot(SLOT_SEAT, SEAT_Y));
        addSlot(dyeSlot(SLOT_BRIDLE, BRIDLE_Y));
        addSlot(new Slot(input, SLOT_METAL, ZONE_X, METAL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return metalColour(stack) != null;
            }
        });
        addSlot(new Slot(output, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack taken) {
                // Only a colour change counts - a take that did nothing but put a
                // name on the saddle is not "dye a piece of tack".
                if (onTakeResult()) {
                    HorseProgress.complete(taker, ProgressTask.DYE_TACK);
                }
                super.onTake(taker, taken);
            }
        });

        addStandardInventorySlots(inventory, MARGIN, INV_Y);
    }

    private Slot dyeSlot(int index, int y) {
        return new Slot(input, index, ZONE_X, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return dyeColour(stack) != null;
            }
        };
    }

    /** From {@code BenchNamePayload}; the client sends it as the box is typed in. */
    public void setSaddleName(String name) {
        this.saddleName = name == null ? "" : name;
        slotsChanged(input);
    }

    public String saddleName() {
        return saddleName;
    }

    @Override
    public void slotsChanged(Container container) {
        Plan plan = plan();
        output.setItem(0, plan == null ? ItemStack.EMPTY : plan.out());
        broadcastChanges();
    }

    /** What the bench would make, and which materials would pay for it. */
    private record Plan(ItemStack out, boolean usedSeat, boolean usedBridle, boolean usedMetal) {}

    private @Nullable Plan plan() {
        ItemStack saddle = input.getItem(SLOT_SADDLE);
        if (saddle.isEmpty()) {
            return null;
        }
        SaddleTint now = saddle.getOrDefault(ModDataComponents.TACK_TINT.get(), SaddleTint.undyedFor(saddle));

        Integer seat = dyeColour(input.getItem(SLOT_SEAT));
        Integer bridle = dyeColour(input.getItem(SLOT_BRIDLE));
        Integer metal = metalColour(input.getItem(SLOT_METAL));

        // A material only counts if it would actually CHANGE that zone, so the
        // bench never charges a dye for a colour the saddle already wears.
        boolean usedSeat = seat != null && seat != now.seat();
        boolean usedBridle = bridle != null && bridle != now.bridle();
        boolean usedMetal = metal != null && metal != now.metal();

        String wanted = saddleName.strip();
        boolean renaming = !wanted.isEmpty() && !wanted.equals(nameOf(saddle));

        if (!usedSeat && !usedBridle && !usedMetal && !renaming) {
            return null;
        }

        ItemStack out = saddle.copyWithCount(1);
        out.set(ModDataComponents.TACK_TINT.get(), new SaddleTint(
                usedSeat ? seat : now.seat(),
                usedBridle ? bridle : now.bridle(),
                usedMetal ? metal : now.metal()));
        if (!wanted.isEmpty()) {
            out.set(DataComponents.CUSTOM_NAME, Component.literal(wanted));
        }
        return new Plan(out, usedSeat, usedBridle, usedMetal);
    }

    /**
     * Spend exactly what the plan named - the saddle, and only the materials used.
     *
     * @return whether a <i>colour</i> actually changed, which is what the
     *         checklist's "dye a piece of tack" asks for. A rename-only take
     *         spends nothing and returns false.
     */
    private boolean onTakeResult() {
        Plan plan = plan();
        getSlot(SLOT_SADDLE).remove(1);
        boolean dyed = false;
        if (plan != null) {
            dyed = plan.usedSeat() || plan.usedBridle() || plan.usedMetal();
            if (plan.usedSeat()) {
                getSlot(SLOT_SEAT).remove(1);
            }
            if (plan.usedBridle()) {
                getSlot(SLOT_BRIDLE).remove(1);
            }
            if (plan.usedMetal()) {
                getSlot(SLOT_METAL).remove(1);
            }
        }
        access.execute((level, pos) -> {
            long now = level.getGameTime();
            if (lastSoundTime != now) {
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                lastSoundTime = now;
            }
        });
        return dyed;
    }

    /**
     * <b>The fitting colour this stack gives</b>, or null if it is not a metal.
     *
     * <p>{@link #METALS} first - those fifteen are hand-picked, and several were
     * deliberately pulled away from their material's true average so that two
     * fittings a player cannot tell apart are not both offered. Then anything
     * another mod filed under {@code c:ingots/*} or {@code c:gems/*}, coloured
     * from its own art. The hand-picked fifteen win where both would answer, so
     * a mod adding a second copper does not quietly redefine copper.
     */
    public static @Nullable Integer metalColour(ItemStack stack) {
        Integer known = METALS.get(stack.getItem());
        if (known != null) {
            return known;
        }
        return com.example.horsegenetics.neoforge.compat.ModdedMaterials.metalColour(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /**
     * <b>The leather colour this stack gives</b>, or null if it is not a dye.
     *
     * <p>Vanilla's component first, which is most modded dyes as well as all
     * sixteen of vanilla's - a mod adding "a red dye" gives it
     * {@code minecraft:dye} and has always worked here. The fallback is the one
     * case that did not: a dye whose colour vanilla has no name for, which
     * carries no component and can only be read off its own art.
     */
    private static @Nullable Integer dyeColour(ItemStack stack) {
        DyeColor dye = stack.get(DataComponents.DYE);
        if (dye != null) {
            return dye.getTextureDiffuseColor() & 0xFFFFFF;
        }
        return com.example.horsegenetics.neoforge.compat.ModdedMaterials.dyes().get(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static String nameOf(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name == null ? "" : name.getString();
    }

    /**
     * Shift-click. Out of the bench into the player; from the player, a saddle
     * finds the saddle slot, a dye the first free leather slot, and a metal its
     * own - {@code moveItemStackTo} asks each slot's {@code mayPlace}, so a stick
     * goes nowhere rather than into the wrong one.
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
        } else if (isTack(stack)) {
            if (!moveItemStackTo(stack, SLOT_SADDLE, SLOT_SADDLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (metalColour(stack) != null) {
            if (!moveItemStackTo(stack, SLOT_METAL, SLOT_METAL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SLOT_SEAT, SLOT_METAL, false)) {
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
