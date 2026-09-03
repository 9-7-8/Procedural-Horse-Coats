package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

import java.util.List;

/**
 * A <b>Minecraft-specific effect</b> a gene grants the horse that carries it -
 * everything a gene does that is <i>not</i> a coat pixel.
 *
 * <p>The coat side of a spec ({@link GeneSpec.Layer}s) is pure maths and lives
 * entirely in {@code common/}. An ability is the opposite: it only means
 * something with a running game around it - a traversal flag, an attribute
 * modifier, a particle trail, a mob effect, a thing the horse can be milked
 * for. {@code common/} still owns the <b>vocabulary</b> and the parsing (this
 * file, {@link AbilityType}, {@link GeneSpecParser}); the NeoForge module owns
 * the <b>execution</b> - it reads {@link SpecAbilities#activeFor} and translates
 * each record into game calls. That split is the same one the rest of the mod
 * uses, and it is what keeps a future 1.12.2 backport cheap: the ability
 * definitions port unchanged, only the translator is rewritten.
 *
 * <p>The set is <b>closed</b>, on purpose (see {@code wiki/horse-traits.html}).
 * It is a deliberately small slice of the full trait architecture - enough for
 * "this gene changes how the horse moves / what it emits / what it produces".
 * Selectors, auras, resource pools, goals and cooldown-gated abilities from that
 * document are <i>not</i> here yet; they are roadmap.
 *
 * <h2>Shape</h2>
 * Every ability carries:
 * <ul>
 *   <li>a {@link Condition} - when it is active. {@link Condition#ALWAYS} if the
 *       file leaves {@code "when"} out. Conditions are evaluated by the
 *       translator against the live horse.</li>
 *   <li>a {@code minDose} - {@code 1} (any expressing copy) or {@code 2}
 *       (homozygous only), so an incomplete-dominant gene can gate the stronger
 *       half of its effect on two copies without an expression language.</li>
 * </ul>
 * Some also carry a {@link Trigger} - the event that fires them. A missing
 * trigger means {@link Trigger.Continuous} (evaluated every tick).
 */
public sealed interface GeneAbility {

    /** When this ability is active. {@link Condition#ALWAYS} unless the file says otherwise. */
    Condition when();

    /** {@code 1} = any expressing copy; {@code 2} = homozygous variant only. */
    int minDose();

    // ------------------------------------------------------------------
    // The verbs
    // ------------------------------------------------------------------

    /**
     * A traversal flag - {@code walk_on_water}, {@code fire_immune}, ... - one of
     * the {@code flag} choices on {@link AbilityType#TRAVERSAL}. Condition-gated: a
     * {@code walk_on_water} with {@code "when": {"flag": "adult"}} only holds up
     * grown horses.
     */
    record Traversal(String flag, Condition when, int minDose) implements GeneAbility {}

    /**
     * A temporary attribute modifier - {@code attribute} and {@code op} are the
     * choices on {@link AbilityType#ATTRIBUTE}. Present while {@link #when()} holds,
     * removed when it stops.
     */
    record AttributeMod(String attribute, String op, double amount, Condition when, int minDose)
            implements GeneAbility {}

    /**
     * A particle / light emitter. {@code kind} / {@code shape} / {@code anchor}
     * are the choices on {@link AbilityType#EMITTER}; {@code particle} is a particle id
     * (e.g. {@code "minecraft:dust"}); {@code color} is {@code 0xRRGGBB}, used by
     * particle types that take a colour; {@code chance} is the per-fire
     * probability {@code (0,1]} so a dense trail is one number.
     */
    record Emitter(String kind, String shape, String anchor, Trigger trigger, int color,
                   String particle, double chance, Condition when, int minDose) implements GeneAbility {}

    /**
     * A mob effect kept on {@code target} ({@code self} or {@code rider}) while
     * {@link #when()} holds - the "aura on self" pattern. Re-applied every
     * {@code refreshTicks}; {@code amplifier} is 0-based.
     */
    record SelfEffect(String effect, String target, int amplifier, int refreshTicks,
                      Condition when, int minDose) implements GeneAbility {}

    /**
     * Something the horse produces on interaction. {@code consumes} is an item
     * id taken from the player's hand (or {@code ""} for nothing); {@code produces}
     * is the item id handed back. {@code cooldownTicks} throttles it. The
     * trigger is always {@link Trigger.OnInteract}.
     */
    record Yield(Trigger.OnInteract trigger, String consumes, String produces, int cooldownTicks,
                 Condition when, int minDose) implements GeneAbility {}

    /**
     * Makes the carrier <b>glow</b>: emit world light and/or render some coat
     * regions full-bright. {@code light} is a 0-15 light level the horse gives
     * off (0 = none - the translator maintains a {@code minecraft:light} block
     * that follows the horse); {@code emissiveParts} are the body parts the
     * client re-draws at full brightness over the generated coat (empty = none).
     * The two are independent - a gene can light its surroundings without an
     * emissive texture, or vice versa. See {@link AbilityType#GLOW}.
     */
    record Glow(int light, List<Part> emissiveParts, Condition when, int minDose) implements GeneAbility {}

    // ------------------------------------------------------------------
    // Triggers
    // ------------------------------------------------------------------

    /** The event that fires an ability. A small closed set. */
    sealed interface Trigger {

        /** Every tick the condition allows. The default. */
        record Continuous() implements Trigger {}

        /** Every tick the horse is moving under its own power on the ground. */
        record OnMove() implements Trigger {}

        /** Every {@code ticks} game ticks. */
        record Interval(int ticks) implements Trigger {}

        /** A player right-clicks the horse holding {@code item} (an item id, or {@code ""} = anything). */
        record OnInteract(String item) implements Trigger {}
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    /**
     * A predicate on the live horse, evaluated by the translator. Deliberately
     * boolean for now (the full architecture's 0-1 scalar model is roadmap);
     * the combinators are here so a real "mare, tamed, in daylight" reads
     * naturally.
     */
    sealed interface Condition {

        Condition ALWAYS = new Always();

        record Always() implements Condition {}

        /** One of {@link AbilityType#CONDITION_FLAGS}, optionally negated. */
        record Flag(String name, boolean negate) implements Condition {}

        record All(List<Condition> terms) implements Condition {}

        record Any(List<Condition> terms) implements Condition {}

        record Not(Condition term) implements Condition {}
    }
}
