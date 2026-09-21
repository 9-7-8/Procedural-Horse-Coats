package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>Everything a horse wears that vanilla has no slot for.</b> Seventeen of the
 * nineteen in {@code HorseTackSlot} live here; the saddle and the barding do
 * not, because they are real {@link net.minecraft.world.entity.EquipmentSlot}s
 * and vanilla draws them for free.
 *
 * <h2>Why an attachment and not a container</h2>
 * {@code EquipmentSlot} has exactly two animal entries, {@code BODY} and
 * {@code SADDLE}, and {@code EquipmentClientInfo.LayerType} is not extensible,
 * so the roster could never have been nineteen equipment slots. A container was
 * the other option and is worse: a widened container is reachable by hoppers,
 * and {@code AbstractMountInventoryMenu.quickMoveStack} computes every
 * shift-click range from hardcoded indices it shares with donkeys, mules and
 * llamas. An attachment is out of both reaches.
 *
 * <p>This is the same shape vanilla itself uses - the saddle and armour slots
 * are not backed by the horse's container either, {@code
 * Mob.createEquipmentSlotContainer} fronts the equipment array with a view that
 * owns no storage.
 *
 * <h2>One authority, and stacks are copied in and out</h2>
 * The attachment is the only place a worn piece lives - it is never mirrored
 * into a container - and every stack crossing this boundary is copied, so a
 * caller holding the stack it passed in cannot mutate what the horse is
 * wearing. The map is unmodifiable and every "change" returns a new
 * {@code HorseGear}.
 *
 * <h2>Keyed by name, not by ordinal</h2>
 * Same reason {@code TackSlotPayload} sends the name: an ordinal in saved data
 * silently re-points at a different slot the moment the enum gains a constant
 * in the middle, and this enum is expected to keep growing. An unknown key is
 * kept rather than dropped, so a piece parked behind a removed slot is still
 * there if the slot comes back.
 */
public record HorseGear(Map<String, ItemStack> worn) {

    public static final HorseGear EMPTY = new HorseGear(Collections.emptyMap());

    /**
     * Only non-empty stacks are written. An empty slot is an absent key rather
     * than a key holding air, so a horse wearing nothing serialises to nothing.
     */
    public static final Codec<HorseGear> CODEC =
            Codec.unboundedMap(Codec.STRING, ItemStack.CODEC)
                    .xmap(HorseGear::new, gear -> gear.worn);

    public static final MapCodec<HorseGear> MAP_CODEC = CODEC.fieldOf("gear");

    public static final StreamCodec<RegistryFriendlyByteBuf, HorseGear> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, String, ItemStack, Map<String, ItemStack>>map(
                            HashMap::new, ByteBufCodecs.STRING_UTF8, ItemStack.STREAM_CODEC)
                    .map(HorseGear::new, gear -> gear.worn);

    public HorseGear(Map<String, ItemStack> worn) {
        Map<String, ItemStack> copy = new LinkedHashMap<>();
        worn.forEach((slot, stack) -> {
            if (stack != null && !stack.isEmpty()) {
                copy.put(slot, stack.copy());
            }
        });
        this.worn = Collections.unmodifiableMap(copy);
    }

    /** What is in {@code slot}, or {@link ItemStack#EMPTY} - a copy, never the stored stack. */
    public ItemStack get(String slot) {
        ItemStack stack = worn.get(slot);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    /** This gear with {@code slot} holding {@code stack} - empty clears it. */
    public HorseGear with(String slot, ItemStack stack) {
        Map<String, ItemStack> next = new LinkedHashMap<>(worn);
        if (stack == null || stack.isEmpty()) {
            next.remove(slot);
        } else {
            next.put(slot, stack.copy());
        }
        return new HorseGear(next);
    }

    public boolean isEmpty() {
        return worn.isEmpty();
    }
}
