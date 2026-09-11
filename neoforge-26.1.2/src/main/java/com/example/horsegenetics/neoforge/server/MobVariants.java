package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>Set a freshly spawned mob's colour or variant from one number.</b> The
 * spawner gene carries a value in {@code [0, 1)} on its allele copy; this turns
 * it into "the third of this mob's variants", so every sheep one horse makes is
 * the same dye, and a line breeds true to its shade.
 *
 * <h2>Generic, through the mob's own data components</h2>
 * Since 1.21.5 a mob's colour <i>is</i> a data component on the entity - a
 * sheep's {@code SHEEP_COLOR}, a cow's {@code COW_VARIANT}, a cat's
 * {@code CAT_VARIANT} - readable with {@code Entity.get} and settable with
 * {@code Entity.setComponent}, which is what a spawn egg with those components
 * uses. So this does not know about sheep: it walks {@link #COLOUR_COMPONENTS},
 * takes the first one the mob actually has, and picks from that component's
 * own list of values - an enum's constants, or every entry of the registry a
 * {@code Holder} value lives in, sorted by id so the pick is stable across
 * worlds. A mob with none of them is left as the game made it.
 *
 * <p>The killer rabbit is excluded by name: a rabbit spawner that occasionally
 * made the one hostile rabbit would be a surprise nobody asked for.
 *
 * <p><b>Unverified in-game</b> beyond compiling - written against the 26.1.2
 * sources, where {@code setComponent} routes through each mob's
 * {@code applyImplicitComponent}.
 */
public final class MobVariants {

    private MobVariants() {
    }

    /** Most specific first: a mob with a real variant should not be judged by a secondary colour. */
    private static final List<DataComponentType<?>> COLOUR_COMPONENTS = List.of(
            DataComponents.SHEEP_COLOR,
            DataComponents.COW_VARIANT,
            DataComponents.PIG_VARIANT,
            DataComponents.CHICKEN_VARIANT,
            DataComponents.CAT_VARIANT,
            DataComponents.WOLF_VARIANT,
            DataComponents.FROG_VARIANT,
            DataComponents.FOX_VARIANT,
            DataComponents.PARROT_VARIANT,
            DataComponents.RABBIT_VARIANT,
            DataComponents.LLAMA_VARIANT,
            DataComponents.AXOLOTL_VARIANT,
            DataComponents.MOOSHROOM_VARIANT,
            DataComponents.HORSE_VARIANT,
            DataComponents.VILLAGER_VARIANT,
            DataComponents.SHULKER_COLOR,
            DataComponents.TROPICAL_FISH_BASE_COLOR,
            DataComponents.ZOMBIE_NAUTILUS_VARIANT);

    /**
     * Apply {@code variant} (in {@code [0, 1)}) to whichever colour component
     * {@code entity} has. Returns whether anything was set.
     */
    public static boolean apply(Entity entity, double variant, ServerLevel level) {
        if (variant < 0) {
            return false;
        }
        for (DataComponentType<?> type : COLOUR_COMPONENTS) {
            Object current = entity.get(type);
            if (current == null) {
                continue;
            }
            Object chosen = pick(current, variant, level);
            if (chosen == null) {
                return false;
            }
            set(entity, type, chosen);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(Entity entity, DataComponentType<T> type, Object value) {
        entity.setComponent(type, (T) value);
    }

    private static Object pick(Object current, double variant, ServerLevel level) {
        List<?> options = optionsFor(current, level);
        if (options.isEmpty()) {
            return null;
        }
        int i = Math.min(options.size() - 1, (int) Math.floor(variant * options.size()));
        return options.get(Math.max(0, i));
    }

    /** Every value the component could take, in a stable order. */
    private static List<?> optionsFor(Object current, ServerLevel level) {
        if (current instanceof Enum<?> e) {
            List<Object> out = new ArrayList<>();
            for (Object c : e.getDeclaringClass().getEnumConstants()) {
                if (!"EVIL".equals(((Enum<?>) c).name())) {
                    out.add(c);
                }
            }
            return out;
        }
        if (current instanceof Holder<?> holder) {
            ResourceKey<?> key = holder.unwrapKey().orElse(null);
            if (key == null) {
                return List.of();
            }
            return holdersOf(level, key);
        }
        return List.of();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<?> holdersOf(ServerLevel level, ResourceKey<?> entryKey) {
        ResourceKey<? extends Registry<Object>> registryKey =
                (ResourceKey) ResourceKey.createRegistryKey(entryKey.registry());
        Registry<Object> registry = level.registryAccess().lookupOrThrow(registryKey);
        List<Holder.Reference<Object>> out = new ArrayList<>(registry.listElements().toList());
        out.sort(Comparator.comparing(h -> h.key().identifier().toString()));
        return out;
    }
}
