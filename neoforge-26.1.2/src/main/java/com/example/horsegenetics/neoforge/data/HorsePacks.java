package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>What is inside the chests a horse carries.</b> The chest itself - the
 * barrel, the shulker box, whatever was hung there - is a worn piece like any
 * other and lives in {@link HorseGear}, under the two storage slots of
 * {@code HorseTackSlot}. This is the other half: the stacks in it, keyed by the
 * same slot name.
 *
 * <h2>Why it is not in the gear</h2>
 * {@code HorseGear} is synced to every client that can see the horse and is
 * rebuilt whole on every change. Fifty-four stacks a side, re-sent to a whole
 * paddock each time somebody moves a carrot, is the wrong cost for something
 * only the player with the chest open needs - and a chest's contents are
 * nobody else's business. So this attachment is <b>saved and never synced</b>;
 * a client sees the contents through the menu it has open and otherwise knows
 * only how many items there are ({@link HorsePackLoad}).
 *
 * <h2>Why it is mutable, when the gear is not</h2>
 * A container hands out its <i>live</i> stacks and vanilla's menus grow and
 * shrink them in place. An immutable value that copies on the way out would
 * turn every merge into a silent loss. So the stacks here are the real ones,
 * the menu's container is a window straight onto them, and the entity saving
 * itself reads whatever is true at that moment. Entities are written with
 * their chunk whether or not anything says they changed, so there is no dirty
 * flag to forget.
 *
 * <h2>The lists grow and are never cut</h2>
 * How many slots a chest has is asked of the chest
 * ({@code HorseStorage.slots}), not stored here. A list is as long as the
 * highest slot ever written, and a chest that comes back smaller - the mod that
 * made it was updated, or removed - still shows everything that was in it:
 * the container is never smaller than what it holds.
 *
 * <h2>Keyed by name</h2>
 * {@link HorseGear}'s reason: an ordinal in a save silently re-points when the
 * enum grows.
 */
public final class HorsePacks {

    /**
     * The most slots one chest may have. {@link ItemStackWithSlot} writes the
     * slot as an unsigned byte, and eighteen columns by nine rows is the most
     * the screen draws.
     */
    public static final int MAX_SLOTS = 162;

    private static final Codec<Map<String, List<ItemStackWithSlot>>> SAVED =
            Codec.unboundedMap(Codec.STRING, ItemStackWithSlot.CODEC.listOf());

    /**
     * A chest carried <i>whole</i>: the item as vanilla's pick-block-with-data
     * would hand it over, everything the block held inside it, and how many
     * items that was the last time the block was live. See {@link #hold}.
     */
    public record Held(ItemStack chest, int items) {
        static final Codec<Held> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("chest").forGetter(Held::chest),
                Codec.INT.optionalFieldOf("items", 0).forGetter(Held::items)
        ).apply(i, Held::new));
    }

    /** The plain store alone - what {@code packs} has always held, and all a test needs. */
    public static final Codec<HorsePacks> CODEC = SAVED.xmap(HorsePacks::fromSaved, HorsePacks::toSaved);

    /**
     * {@code packs} is the plain store, as it was first saved; {@code held} was
     * added beside it rather than inside it, so a horse saved before there were
     * held chests reads exactly as it did.
     */
    public static final MapCodec<HorsePacks> MAP_CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
                    SAVED.optionalFieldOf("packs", Map.of()).forGetter(HorsePacks::toSaved),
                    Codec.unboundedMap(Codec.STRING, Held.CODEC).optionalFieldOf("held", Map.of())
                            .forGetter(packs -> packs.held)
            ).apply(i, (saved, held) -> {
                HorsePacks packs = fromSaved(saved);
                held.forEach((slot, chest) -> {
                    if (!chest.chest().isEmpty()) {
                        packs.held.put(slot, chest);
                    }
                });
                return packs;
            }));

    private final Map<String, List<ItemStack>> bySlot = new LinkedHashMap<>();
    private final Map<String, Held> held = new LinkedHashMap<>();

    public HorsePacks() {
    }

    private static HorsePacks fromSaved(Map<String, List<ItemStackWithSlot>> saved) {
        HorsePacks packs = new HorsePacks();
        saved.forEach((slot, stacks) -> {
            for (ItemStackWithSlot entry : stacks) {
                if (entry.slot() >= 0 && entry.slot() < MAX_SLOTS && !entry.stack().isEmpty()) {
                    packs.set(slot, entry.slot(), entry.stack());
                }
            }
        });
        return packs;
    }

    /** Only what is there: an empty slot is an absent entry, an empty chest an absent key. */
    private Map<String, List<ItemStackWithSlot>> toSaved() {
        Map<String, List<ItemStackWithSlot>> saved = new LinkedHashMap<>();
        bySlot.forEach((slot, stacks) -> {
            List<ItemStackWithSlot> out = new ArrayList<>();
            for (int i = 0; i < stacks.size(); i++) {
                if (!stacks.get(i).isEmpty()) {
                    out.add(new ItemStackWithSlot(i, stacks.get(i)));
                }
            }
            if (!out.isEmpty()) {
                saved.put(slot, out);
            }
        });
        return saved;
    }

    /** The live stack at {@code index} of {@code slot}'s chest - not a copy. */
    public ItemStack get(String slot, int index) {
        List<ItemStack> stacks = bySlot.get(slot);
        return stacks == null || index < 0 || index >= stacks.size() ? ItemStack.EMPTY : stacks.get(index);
    }

    /** Put {@code stack} at {@code index}. An index past {@link #MAX_SLOTS} is refused. */
    public boolean set(String slot, int index, ItemStack stack) {
        if (index < 0 || index >= MAX_SLOTS) {
            return false;
        }
        List<ItemStack> stacks = bySlot.get(slot);
        if (stacks == null) {
            if (stack.isEmpty()) {
                return true;
            }
            stacks = new ArrayList<>();
            bySlot.put(slot, stacks);
        }
        while (stacks.size() <= index) {
            stacks.add(ItemStack.EMPTY);
        }
        stacks.set(index, stack);
        return true;
    }

    /**
     * Put {@code stack} in the first empty place at or after {@code from}, or
     * failing that anywhere. False only when all {@link #MAX_SLOTS} are taken.
     */
    public boolean add(String slot, int from, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        for (int pass = 0; pass < 2; pass++) {
            int start = pass == 0 ? Math.max(0, from) : 0;
            int end = pass == 0 ? MAX_SLOTS : Math.max(0, from);
            for (int i = start; i < end; i++) {
                if (get(slot, i).isEmpty()) {
                    return set(slot, i, stack);
                }
            }
        }
        return false;
    }

    /** One past the highest slot holding anything; zero for an empty chest. */
    public int used(String slot) {
        List<ItemStack> stacks = bySlot.get(slot);
        if (stacks == null) {
            return 0;
        }
        for (int i = stacks.size() - 1; i >= 0; i--) {
            if (!stacks.get(i).isEmpty()) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * <b>Carry {@code slot}'s chest whole.</b> For a chest that opens as its own
     * block ({@code HostedPacks}): its contents are not stacks this class can
     * list, they are whatever the block entity saved, so the whole item is kept
     * - block data, components and all - and put back into the world to be
     * opened. {@code items} is the count taken while it was last live, which is
     * the only time anybody can count it.
     */
    public void hold(String slot, ItemStack chest, int items) {
        if (chest.isEmpty()) {
            held.remove(slot);
        } else {
            held.put(slot, new Held(chest, Math.max(0, items)));
        }
    }

    /** The chest carried whole on {@code slot}, or null - the stored stack, not a copy. */
    public @org.jspecify.annotations.Nullable Held held(String slot) {
        return held.get(slot);
    }

    /** How many items are in {@code slot}'s chest, whatever they are and however it is carried. */
    public int count(String slot) {
        Held whole = held.get(slot);
        long total = whole == null ? 0 : whole.items();
        List<ItemStack> stacks = bySlot.get(slot);
        if (stacks == null) {
            return (int) Math.min(Integer.MAX_VALUE, total);
        }
        for (ItemStack stack : stacks) {
            total += stack.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Everything in {@code slot}'s chest, by position, and the chest left empty. */
    public List<ItemStack> take(String slot) {
        List<ItemStack> stacks = bySlot.remove(slot);
        return stacks == null ? new ArrayList<>() : stacks;
    }

    /** Every slot name with anything stored under it - including one whose chest is gone. */
    public List<String> slots() {
        List<String> names = new ArrayList<>();
        bySlot.forEach((slot, stacks) -> {
            if (used(slot) > 0) {
                names.add(slot);
            }
        });
        return names;
    }

    public boolean isEmpty() {
        return held.isEmpty() && slots().isEmpty();
    }
}
