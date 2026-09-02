package com.example.horsegenetics.common.genetics.spec;

import java.util.List;
import java.util.Locale;

/**
 * The closed vocabularies for {@link GeneAbility} - every flag, attribute and
 * mode a gene's {@code "effects"} block may name.
 *
 * <p>Same job as {@link SpecSchema} does for masks and ops: {@link GeneSpecParser}
 * checks a file against these lists so a mistyped {@code "walk_on_watr"} is a
 * load error that names the key and lists the legal ones, and
 * {@code wiki/horse-traits.html} quotes them. When you add a verb here, add it
 * to the NeoForge translator ({@code server/GeneAbilityHandler} /
 * {@code server/GeneYieldHandler}) in the same change - a flag the game does not
 * read is a trait with no observable signature, which the architecture treats as
 * a bug.
 */
public final class AbilitySchema {

    private AbilitySchema() {}

    /** Movement / survival flags. {@code walk_on_lava} and friends are here so a magical gene can grant them. */
    public static final List<String> TRAVERSAL_FLAGS = List.of(
            "walk_on_water", "walk_on_lava", "fire_immune", "fall_immune",
            "underwater_breathing", "water_averse");

    /** Attributes an {@code attribute} ability may modify. */
    public static final List<String> ATTRIBUTES = List.of(
            "movement_speed", "jump_strength", "max_health", "armor", "armor_toughness",
            "knockback_resistance", "step_height", "safe_fall_distance", "scale", "swim_speed");

    /** How an {@code attribute} ability's amount is applied - vanilla modifier operations. */
    public static final List<String> ATTRIBUTE_OPS = List.of("add", "multiply_base", "multiply_total");

    public static final List<String> EMITTER_KINDS = List.of("particle", "light");

    public static final List<String> EMITTER_SHAPES = List.of("point", "ring", "trail", "burst");

    /** Where an emitter is centred on the horse. */
    public static final List<String> EMITTER_ANCHORS = List.of("feet", "body", "head", "eyes");

    /** Who a {@code mob_effect} ability's effect lands on. */
    public static final List<String> EFFECT_TARGETS = List.of("self", "rider");

    /**
     * Predicate flags a {@code "when"} condition may name. Boolean reads off the
     * live horse; the translator owns the mapping to game state.
     */
    public static final List<String> CONDITION_FLAGS = List.of(
            "sex_female", "sex_male", "tamed", "untamed", "adult", "baby",
            "has_rider", "in_water", "submerged", "on_ground", "on_fire",
            "day", "night", "raining", "thundering", "sky_visible");

    // ------------------------------------------------------------------

    /** {@code true} if {@code value} is in {@code allowed} (case-insensitive). */
    public static boolean allows(List<String> allowed, String value) {
        String v = value.toLowerCase(Locale.ROOT);
        for (String s : allowed) {
            if (s.equals(v)) {
                return true;
            }
        }
        return false;
    }

    /** Throw a message that names the bad value and lists the legal ones. */
    public static String requireOneOf(List<String> allowed, String value, String where) {
        if (!allows(allowed, value)) {
            throw new IllegalArgumentException(where + ": '" + value + "' is not one of " + allowed);
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
