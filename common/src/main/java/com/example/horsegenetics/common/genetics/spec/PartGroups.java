package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Names a gene author can use in a {@code parts} list instead of spelling out
 * every {@link Part} - {@code "LEGS"}, {@code "HAIR"}, {@code "POINTS"}.
 *
 * <p>They exist because the interesting regions of a horse are almost never one
 * box: "the points" is mane + tail + ears + muzzle + lower legs, and an author
 * who has to list those by hand will get one wrong. The groups are also what the
 * gene creator's region picker offers, so the tool and the format agree on what
 * a region is called.
 */
public final class PartGroups {

    private static final Map<String, List<Part>> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("ALL", List.of(Part.values()));
        GROUPS.put("LEGS", List.of(Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG,
                Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG));
        GROUPS.put("FRONT_LEGS", List.of(Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG));
        GROUPS.put("HIND_LEGS", List.of(Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG));
        GROUPS.put("EARS", List.of(Part.LEFT_EAR, Part.RIGHT_EAR));
        GROUPS.put("HAIR", List.of(Part.MANE, Part.TAIL));
        GROUPS.put("FACE", List.of(Part.HEAD, Part.MUZZLE));
        GROUPS.put("POINTS", List.of(Part.MANE, Part.TAIL, Part.LEFT_EAR, Part.RIGHT_EAR, Part.MUZZLE,
                Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG));
        GROUPS.put("BARREL", List.of(Part.BODY, Part.NECK));
    }

    private PartGroups() {}

    /** Every alias, in a stable order - what the creator lists in its picker. */
    public static List<String> names() {
        return List.copyOf(GROUPS.keySet());
    }

    public static boolean isGroup(String name) {
        return GROUPS.containsKey(name);
    }

    /**
     * Expand a list of part names and group aliases into distinct {@link Part}s,
     * in the order they were first named.
     */
    public static List<Part> expand(List<String> names) {
        LinkedHashSet<Part> out = new LinkedHashSet<>();
        for (String raw : names) {
            String name = raw.trim().toUpperCase(java.util.Locale.ROOT);
            List<Part> group = GROUPS.get(name);
            if (group != null) {
                out.addAll(group);
                continue;
            }
            try {
                out.add(Part.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown part or group '" + raw + "'; parts are "
                        + List.of(Part.values()) + " and groups are " + names());
            }
        }
        return List.copyOf(new ArrayList<>(out));
    }
}
