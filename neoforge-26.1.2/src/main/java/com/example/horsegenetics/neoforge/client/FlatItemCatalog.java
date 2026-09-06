package com.example.horsegenetics.neoforge.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The pool a <b>cutie mark</b> ({@link com.example.horsegenetics.common.genetics.genes.CutieMarkGene})
 * draws its emblem items from: every item registered that is <b>not</b> a
 * {@link BlockItem} - i.e. the flat, {@code item/generated}-style icons (tools,
 * food, materials, mob drops, ...), where a block item would render as a little
 * isometric cube.
 *
 * <p>Built once per session, lazily, off the frozen item registry. A new mod or
 * a new game version is a new session, so that is also when it rebuilds - which
 * is all the cutie-mark gene needs (the item a normalised epigenetic pick lands
 * on is only required to be stable <i>within</i> one install, and it is).
 */
public final class FlatItemCatalog {

    private static volatile List<Item> items;

    private FlatItemCatalog() {
    }

    /** Every flat item, in registry order. Never empty in practice. */
    public static List<Item> items() {
        List<Item> local = items;
        if (local != null) {
            return local;
        }
        synchronized (FlatItemCatalog.class) {
            if (items == null) {
                List<Item> out = new ArrayList<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item == Items.AIR || item instanceof BlockItem) {
                        continue;
                    }
                    out.add(item);
                }
                items = out.isEmpty() ? List.of(Items.STICK) : List.copyOf(out);
            }
            return items;
        }
    }

    /** The item a normalised pick in {@code [0, 1)} lands on. */
    public static Item pick(double normalized) {
        List<Item> list = items();
        int i = (int) (Math.max(0.0, Math.min(0.9999999, normalized)) * list.size());
        return list.get(Math.min(i, list.size() - 1));
    }

    /** Drop the cached list (called on world exit for hygiene; it rebuilds on next use). */
    public static void clear() {
        items = null;
    }
}
