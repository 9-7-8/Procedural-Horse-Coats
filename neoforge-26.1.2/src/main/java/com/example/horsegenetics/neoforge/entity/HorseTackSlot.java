package com.example.horsegenetics.neoforge.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/**
 * <b>The tack a horse wears, as a list this mod can draw and click.</b> Two
 * entries today - the saddle and the body armour - and the shape is a list
 * precisely because it is not going to stay two: a bridle, a blanket and shoes
 * are the obvious next ones, and adding one should be a line here plus an icon,
 * not a screen rewrite.
 *
 * <h2>Why not vanilla's horse inventory</h2>
 * Vanilla's screen is a fixed sprite with exactly these two slots cut into it
 * and no room for a third. This mod's information screen draws its own, so the
 * row can be as long as the list - see {@code HorseInfoScreen.drawTack} for the
 * drawing and {@code ModNetworking.handleTackSlot} for the click.
 *
 * <h2>The items are on the entity, not in a container</h2>
 * Both of these are real {@link EquipmentSlot}s on the horse since the saddle
 * became equipment, which is what makes a click cheap on both sides: the client
 * already has them off the entity's synced equipment and needs no container
 * open, and the server sets them with {@code setItemSlot}. The chest a donkey
 * carries is <i>not</i> one of these and is not here.
 */
public enum HorseTackSlot {

    SADDLE(EquipmentSlot.SADDLE, "Saddle", "Put a saddle on it and it will take direction."),
    ARMOUR(EquipmentSlot.BODY, "Armour", "Leather, iron, gold or diamond barding.");

    private final EquipmentSlot slot;
    private final String label;
    private final String hint;

    HorseTackSlot(EquipmentSlot slot, String label, String hint) {
        this.slot = slot;
        this.label = label;
        this.hint = hint;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public String label() {
        return label;
    }

    /** What an empty slot says on hover - what goes in it, in one line. */
    public String hint() {
        return hint;
    }

    public ItemStack on(AbstractHorse horse) {
        return horse.getItemBySlot(slot);
    }

    /** Will this horse take {@code stack} here at all? Tame, grown, and the right kind of item. */
    public boolean accepts(AbstractHorse horse, ItemStack stack) {
        return !stack.isEmpty() && horse.canUseSlot(slot) && horse.isEquippableInSlot(stack, slot);
    }

    /** The one with this name, or {@code null} - for a packet, which may say anything. */
    public static HorseTackSlot byName(String name) {
        for (HorseTackSlot tack : values()) {
            if (tack.name().equals(name)) {
                return tack;
            }
        }
        return null;
    }
}
