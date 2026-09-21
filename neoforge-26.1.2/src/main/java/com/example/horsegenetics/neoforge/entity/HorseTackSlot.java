package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseGear;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * <b>The gear a horse wears, as a list this mod can draw and click.</b>
 * Nineteen entries, laid out on the Gear tab of {@code HorseInfoScreen} as a
 * paper doll around the horse's own portrait, and the list is the layout: a
 * slot's zone and its position on the doll are fields here, so adding one is a
 * line in this enum plus a tag, not a screen rewrite.
 *
 * <h2>Two of them are real equipment slots and seventeen are not</h2>
 * {@link EquipmentSlot} has exactly two animal entries, {@code BODY} and
 * {@code SADDLE}, and {@code EquipmentClientInfo.LayerType} is not extensible.
 * That ceiling is the constraint the whole roster is shaped by. The two real
 * slots are spent on the saddle and the barding, which are the two vanilla
 * already draws for free; every other piece is a key in {@link HorseGear}, a
 * synced attachment. {@link #isVanilla()} is the difference and nothing outside
 * this enum should need to care which a slot is - {@link #on} and {@link #set}
 * cover both.
 *
 * <h2>What a slot accepts is a tag, and every tag is empty today</h2>
 * Each attachment-backed slot reads an item tag named for it -
 * {@code horsegenetics:gear/mane} and so on. <b>None of those tags has any item
 * in it yet</b>, because the pieces that fill these slots have not been made;
 * the roster and the screen landed first on purpose, and a slot whose tag is
 * empty correctly refuses everything rather than accepting dirt. Shipping a
 * piece means adding it to its tag, and a datapack can do the same without
 * touching Java.
 *
 * <h2>Why the head is one slot and not two</h2>
 * A bridle and a halter are genuinely different pieces - a haltered horse can
 * be led and lunged, a bridled one can be steered, and neither substitutes for
 * the other - but a horse cannot wear both at once, so they are two items
 * competing for one slot rather than two slots. (Owner's call; it reverses the
 * roadmap's split.)
 */
public enum HorseTackSlot {

    // -- Head ---------------------------------------------------------------
    HEADSTALL(null, Zone.HEAD, "Headstall",
            "A bridle to steer it, or a halter to lead it - never both.", 0.03f, 0.06f),
    MASK(null, Zone.HEAD, "Mask",
            "A fly mask, or something with more in mind than flies.", 0.03f, 0.24f),

    // -- Hair ---------------------------------------------------------------
    MANE(null, Zone.HAIR, "Mane", "Whatever is worked into the mane.", 0.03f, 0.42f),
    TAIL(null, Zone.HAIR, "Tail", "Whatever is worked into the tail.", 0.88f, 0.42f),

    // -- Chest --------------------------------------------------------------
    MARTINGALE(null, Zone.CHEST, "Chest",
            "A martingale or a collar, across the chest.", 0.03f, 0.60f),

    // -- Body ---------------------------------------------------------------
    SADDLE_PAD(null, Zone.BODY, "Pad", "The pad that goes under the saddle.", 0.30f, 0.01f),
    SADDLE(EquipmentSlot.SADDLE, Zone.BODY, "Saddle",
            "Put a saddle on it and it will take direction.", 0.42f, 0.01f),
    BARDING(EquipmentSlot.BODY, Zone.BODY, "Barding",
            "Leather, iron, gold or diamond barding.", 0.54f, 0.01f),
    BLANKET(null, Zone.BODY, "Blanket",
            "A rug or a sheet - what a horse wears when it is not being ridden.", 0.66f, 0.01f),

    // -- Storage ------------------------------------------------------------
    SADDLEBAG_LEFT(null, Zone.STORAGE, "Bag", "A saddlebag, on the near side.", 0.88f, 0.06f),
    SADDLEBAG_RIGHT(null, Zone.STORAGE, "Bag", "A saddlebag, on the off side.", 0.88f, 0.24f),

    // -- Legs ---------------------------------------------------------------
    // Four legs and four hooves rather than one "set of four" apiece: a horse
    // can throw a single shoe, and wear unevenly, and a roster that cannot say
    // which leg could never say that.
    BOOT_FRONT_LEFT(null, Zone.LEGS, "Boot", "A boot, near fore.", 0.22f, 0.70f),
    BOOT_FRONT_RIGHT(null, Zone.LEGS, "Boot", "A boot, off fore.", 0.34f, 0.70f),
    BOOT_HIND_LEFT(null, Zone.LEGS, "Boot", "A boot, near hind.", 0.54f, 0.70f),
    BOOT_HIND_RIGHT(null, Zone.LEGS, "Boot", "A boot, off hind.", 0.66f, 0.70f),

    // -- Hooves -------------------------------------------------------------
    SHOE_FRONT_LEFT(null, Zone.HOOVES, "Shoe", "A shoe, near fore.", 0.22f, 0.86f),
    SHOE_FRONT_RIGHT(null, Zone.HOOVES, "Shoe", "A shoe, off fore.", 0.34f, 0.86f),
    SHOE_HIND_LEFT(null, Zone.HOOVES, "Shoe", "A shoe, near hind.", 0.54f, 0.86f),
    SHOE_HIND_RIGHT(null, Zone.HOOVES, "Shoe", "A shoe, off hind.", 0.66f, 0.86f);

    /** What part of the horse a slot belongs to - the Gear tab labels by these. */
    public enum Zone {
        HEAD("Head"),
        HAIR("Mane and tail"),
        CHEST("Chest"),
        BODY("Body"),
        STORAGE("Storage"),
        LEGS("Legs"),
        HOOVES("Hooves");

        private final String label;

        Zone(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final @Nullable EquipmentSlot slot;
    private final Zone zone;
    private final String label;
    private final String hint;
    private final float anchorX;
    private final float anchorY;
    private final TagKey<Item> tag;

    HorseTackSlot(@Nullable EquipmentSlot slot, Zone zone, String label, String hint,
                  float anchorX, float anchorY) {
        this.slot = slot;
        this.zone = zone;
        this.label = label;
        this.hint = hint;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(
                HorseGenetics.MOD_ID, "gear/" + name().toLowerCase(Locale.ROOT)));
    }

    /** The vanilla slot this rides in, or {@code null} when it rides {@link HorseGear}. */
    public @Nullable EquipmentSlot equipmentSlot() {
        return slot;
    }

    /** Whether vanilla owns this slot - true only for the saddle and the barding. */
    public boolean isVanilla() {
        return slot != null;
    }

    public Zone zone() {
        return zone;
    }

    public String label() {
        return label;
    }

    /** What an empty slot says on hover - what goes in it, in one line. */
    public String hint() {
        return hint;
    }

    /** Where this slot sits on the paper doll, as a fraction of the doll's box. */
    public float anchorX() {
        return anchorX;
    }

    public float anchorY() {
        return anchorY;
    }

    /** The item tag this slot accepts. Empty until the pieces that fill it exist. */
    public TagKey<Item> tag() {
        return tag;
    }

    /** What the horse is wearing here. A copy for gear slots; never mutate it. */
    public ItemStack on(AbstractHorse horse) {
        if (slot != null) {
            return horse.getItemBySlot(slot);
        }
        return horse.getData(ModAttachments.HORSE_GEAR).get(name());
    }

    /**
     * Put {@code stack} here, or clear it with {@link ItemStack#EMPTY}. Server
     * side - a gear slot writes the attachment back through {@code setData}, so
     * the sync reaches the client that drew the slot.
     */
    public void set(AbstractHorse horse, ItemStack stack) {
        if (slot != null) {
            horse.setItemSlot(slot, stack);
            if (!stack.isEmpty()) {
                // Tack a player put on is tack they get back - vanilla would
                // otherwise roll equipment drops.
                horse.setGuaranteedDrop(slot);
            }
            return;
        }
        HorseGear gear = horse.getData(ModAttachments.HORSE_GEAR);
        horse.setData(ModAttachments.HORSE_GEAR, gear.with(name(), stack));
    }

    /**
     * Will this horse take {@code stack} here at all? A vanilla slot asks
     * vanilla; a gear slot asks the tag, which is empty for every slot today.
     */
    public boolean accepts(AbstractHorse horse, ItemStack stack) {
        if (stack.isEmpty() || !usableOn(horse)) {
            return false;
        }
        if (slot != null) {
            return horse.isEquippableInSlot(stack, slot);
        }
        return stack.is(tag);
    }

    /**
     * Whether this slot is usable on this horse at all, before asking what is in
     * anyone's hand. Foals wear nothing: a piece of tack sized for an adult is
     * the one thing a growing horse should not be carrying, and the roster's
     * own note says no pads on foals.
     */
    public boolean usableOn(AbstractHorse horse) {
        if (slot != null) {
            return horse.canUseSlot(slot);
        }
        return !horse.isBaby();
    }

    /**
     * <b>The roster's own invariants, checked at boot.</b> Every way this table
     * can be wrong fails <i>silently</i> in the game, which is why it is worth
     * a check at all: two slots given the same anchor draw exactly on top of
     * each other, so one is unclickable and the Gear tab merely looks like it
     * has one slot fewer; an anchor outside 0..1 puts a slot off the doll,
     * invisible but still holding its click box; and a third constant handed a
     * real {@link EquipmentSlot} would take one the saddle or the barding
     * needs. None of that throws on its own and none of it logs.
     *
     * <p>Called from the mod constructor, beside {@code DietFoods.verify} and
     * for the same reason. It cannot be a unit test: this module's test source
     * set has no Minecraft on its classpath on purpose, and the enum cannot
     * even be loaded without one.
     */
    public static void verify() {
        int vanilla = 0;
        java.util.Map<String, HorseTackSlot> anchors = new java.util.HashMap<>();
        for (HorseTackSlot slot : values()) {
            if (slot.isVanilla()) {
                vanilla++;
            }
            if (slot.anchorX < 0f || slot.anchorX > 1f || slot.anchorY < 0f || slot.anchorY > 1f) {
                throw new IllegalStateException("HorseTackSlot " + slot
                        + " is anchored off the doll at " + slot.anchorX + "," + slot.anchorY);
            }
            HorseTackSlot clash = anchors.put(slot.anchorX + "," + slot.anchorY, slot);
            if (clash != null) {
                throw new IllegalStateException("HorseTackSlot " + clash + " and " + slot
                        + " are anchored at the same place, so one of them cannot be clicked");
            }
        }
        // Two, and only two: EquipmentSlot has exactly two animal entries.
        if (vanilla != 2) {
            throw new IllegalStateException("HorseTackSlot has " + vanilla
                    + " vanilla-backed slots; there are only two animal EquipmentSlots");
        }
    }

    /** The one with this name, or {@code null} - for a packet, which may say anything. */
    public static @Nullable HorseTackSlot byName(String name) {
        for (HorseTackSlot tack : values()) {
            if (tack.name().equals(name)) {
                return tack;
            }
        }
        return null;
    }
}
