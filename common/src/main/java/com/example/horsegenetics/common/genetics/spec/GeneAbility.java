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
 * the <b>execution</b> - it reads {@link HorseAbilities#activeFor} and translates
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
    record Traversal(String flag, String target, Condition when, int minDose) implements GeneAbility {}

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
     * (e.g. {@code "minecraft:dust"}); {@code chance} is the per-fire
     * probability {@code (0,1]} so a dense trail is one number.
     *
     * <p>Three of the fields exist for the minority of particles that carry
     * their own data, and every one of them is <b>ignored by particles that do
     * not take it</b> - the translator knows which is which, so an author (or a
     * gene) may always fill all three in and let the particle decide:
     * <ul>
     *   <li>{@code color} - {@code 0xRRGGBB}, for {@code dust},
     *       {@code dust_color_transition}, {@code effect},
     *       {@code entity_effect} and friends;</li>
     *   <li>{@code color2} - the second {@code 0xRRGGBB}, for the one particle
     *       that fades between two ({@code dust_color_transition});</li>
     *   <li>{@code data} - a normalised {@code [0,1)} number standing in for
     *       whatever else a particle wants: a {@code shriek}'s delay, a
     *       {@code note}'s pitch, a {@code sculk_charge}'s roll. One number
     *       rather than a union type, because the alternative is a parameter per
     *       particle in a vocabulary that is meant to stay small.</li>
     * </ul>
     *
     * <p>{@code count} is how many particles one firing spawns (at least 1) -
     * the difference between a trickle and a plume, and the one knob that reads
     * from across a paddock.
     *
     * <p>{@code cycleTicks} is the <b>rainbow</b> knob, and the one field here
     * that is a function of time rather than a constant: {@code 0} means the two
     * colours above are the colours, and anything larger is how many ticks one
     * full rotation of the hue circle takes - the translator recomputes
     * {@code color} and {@code color2} from the clock on every firing and
     * ignores what the gene put in them. It is a parameter rather than a second
     * verb because everything else about a rotating trail - the particle, the
     * anchor, the count, the chance - is the same emitter it already was, and a
     * whole verb whose only difference is where two ints come from would be a
     * copy of this one.
     */
    record Emitter(String kind, String shape, String anchor, Trigger trigger, int color, int color2,
                   int count, double data, String particle, double chance, int cycleTicks,
                   Condition when, int minDose)
            implements GeneAbility {}

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
     *
     * <p>{@code deniedDamage} / {@code deniedMessage} are the <b>else branch</b> -
     * what happens when a matching interaction's {@link #when()} <i>fails</i>:
     * milking a stallion earns a kick ({@code deniedDamage 1}), milking a foal
     * earns a "nothing to give" message ({@code deniedDamage 0}). A yield with
     * neither is silent when its condition fails, as before. The message is fed
     * through {@code Component.translatable}, so a built-in gene passes a lang
     * key and a spec author passes literal text - both render.
     */
    record Yield(Trigger.OnInteract trigger, String consumes, String produces, int cooldownTicks,
                 double deniedDamage, String deniedMessage, String kind, String potionEffect,
                 int potionAmplifier, int potionDurationTicks,
                 Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>Extra uses of a cooldown-gated {@link Yield}</b>, before its cooldown
     * bites - "this horse can be milked three times a day rather than once".
     *
     * <p>It is a separate verb rather than a bigger {@code Yield} because the
     * gene that <i>produces</i> a thing and the gene that decides <i>how often</i>
     * are different loci, and neither can see the other's epigenome. A yield
     * declares what {@link Yield#kind()} of thing it is; this names a kind and
     * adds {@link #extra()} charges to <b>every</b> yield of that kind the horse
     * expresses. So one volume locus governs plain milk, water and lava at once,
     * and a second gene that one day produces some other milk is governed by it
     * for free rather than by remembering to ask.
     *
     * <p>Charges from several copies add, which is what makes the volume locus
     * codominant without the translator knowing that it is.
     */
    record YieldCharges(String kind, int extra, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>How long the horse lasts under water</b>, as a multiplier on its air
     * supply. {@code 2.0} is twice as long before the first drowning tick;
     * {@code 0.5} is half. Unrelated to {@code underwater_breathing}, which is
     * the absolute version and never runs out - this is the graded one, and the
     * two compose the way you would expect (a horse that cannot drown does not
     * care how slowly it would have).
     */
    record Breath(double factor, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>What the world does when the horse dies</b> - {@code lava},
     * {@code water} or {@code explode}, the choices on
     * {@link AbilityType#ON_DEATH}.
     *
     * <p>Deliberately <b>not</b> about items. What a horse leaves behind and
     * what happens to the ground it died on are two different questions with
     * two different genes behind them, and folding them into one verb would
     * make "drops diamonds and leaves a crater" impossible to express as the
     * two independent loci it is.
     */
    record OnDeath(String effect, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>What the horse leaves behind</b> - one of the {@code drop} choices on
     * {@link AbilityType#ITEM_DROP}, in a count drawn between {@code min} and
     * {@code max} inclusive.
     *
     * <p>{@code vanilla} means "add nothing, leave the usual leather", and is
     * the wild type's answer; every other value <b>replaces</b> the vanilla
     * drop, except {@code meat}, which is added beside it. That asymmetry is
     * the gene's, not the verb's: a horse that drops diamonds instead of
     * leather is a different animal, and a meaty horse is the same animal with
     * more on it.
     */
    record ItemDrop(String drop, int min, int max, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>How mobs feel about the horse</b> - {@code repel} keeps them outside
     * {@code radius} blocks, {@code attract} makes hostile mobs that can see it
     * prefer it to anything else in range.
     *
     * <p>{@code maxTargets} caps how many entities one beat may touch, which
     * every radius effect here is required to do.
     */
    record MobAura(String mode, String group, String mob, double radius, int intervalTicks,
                   int maxTargets, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>How the horse feels about other creatures after dark</b> -
     * {@code aggressive} or {@code flee}, toward one of the {@code towards}
     * groups on {@link AbilityType#NIGHT_TEMPER}.
     *
     * <p>Night is part of the verb rather than a {@code when} condition, and
     * that is deliberate: a horse whose temperament merely happened to be
     * gated on darkness would need the gate written on every allele, and the
     * one thing this locus is <i>about</i> is that the animal changes when the
     * sun goes down.
     */
    record NightTemper(String mood, String towards, double radius, int intervalTicks,
                       int maxTargets, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>What the horse does about the nearest player after dark</b> - one of
     * the {@code mode} choices on {@link AbilityType#NIGHT_WATCH}.
     *
     * <p>{@code silentSteps} suppresses the horse's footfall sound while the
     * mode is active. It rides on this record rather than being a verb of its
     * own because it is not a separate trait: it is what makes a watcher a
     * watcher rather than a horse standing there, and no gene has ever wanted
     * one without the other.
     */
    record NightWatch(String mode, double radius, boolean silentSteps,
                      Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>What the horse hits for</b>, in health points - two per heart. A
     * vanilla horse has no attack at all; this is the whole of the mod's combat
     * side, and the number is absolute rather than a modifier so that a reader
     * of a horse's sheet sees the damage it deals and not an adjustment to a
     * baseline they would have to look up.
     */
    record Combat(double damage, Condition when, int minDose) implements GeneAbility {}

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

    /**
     * A <b>healing aura</b>: while {@link #when()} holds, everything of
     * {@code target} within {@code radius} blocks of the horse regains
     * {@code amount} health every {@code intervalTicks}. See
     * {@link AbilityType#HEALING}.
     *
     * <p>The radius is deliberately small everywhere it is used - the point of
     * the verb is a horse worth standing next to, not a horse that trivialises
     * combat from across a field - and {@code maxTargets} caps how many entities
     * one beat may reach, which every radius effect is required to do.
     */
    record Healing(String target, String group, double radius, double amount, int intervalTicks,
                   int maxTargets, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>Ground cover spreading from the hooves</b>: every
     * {@code intervalTicks}, with probability {@code chance}, one eligible
     * block within {@code radius} of the horse is converted to the named
     * {@code cover}. See {@link AbilityType#SPREAD}.
     *
     * <p>{@code cover} is a <i>vocabulary word</i> ({@code "mycelium"},
     * {@code "moss"}, {@code "grass"}), not a block id: what "spreading moss"
     * means is a small family of conversions and a set of blocks it will and
     * will not eat, and that judgement belongs to the translator that knows the
     * game's blocks - not to a gene file naming one id and hoping.
     */
    record Spread(String cover, double radius, double chance, int intervalTicks,
                  Condition when, int minDose) implements GeneAbility {}


    /**
     * <b>A sound the horse makes.</b> {@code sound} is a sound id
     * ({@code "minecraft:entity.cat.ambient"}, a music disc, a whinny);
     * {@code volume} and {@code pitch} are the usual Minecraft scales.
     *
     * <p>{@code durationTicks} is the <b>stop</b>, and it is the field that makes
     * this verb worth having rather than being a special case of an emitter: a
     * Minecraft sound can only be played from its beginning, so "a section of a
     * record" can only ever mean "the opening, then stopped". {@code 0} means
     * let it run to its own end.
     *
     * <p>{@code cooldownTicks} is not optional in spirit even though it has a
     * default. The hazard on every gene using this verb is not tick cost, it is
     * <i>spam</i> - a firing per beat per target in range is unbearable within
     * seconds, and there is no volume that fixes it. Effects fire on an event or
     * a state change and then hold off.
     */
    record Sound(String sound, Trigger trigger, double volume, double pitch,
                 int durationTicks, int cooldownTicks, Condition when, int minDose)
            implements GeneAbility {}

    /**
     * <b>Something the horse produces on a clock</b>, with nobody asking - an
     * egg, or in principle any item at all.
     *
     * <p>It completes a pattern that was already two thirds built:
     * {@link ItemDrop} produces on death, {@link Yield} produces on interaction,
     * and this produces on an interval. That symmetry is the argument that it is
     * a real verb rather than a special case - it is the missing corner of a
     * table, not a new table.
     *
     * <p>{@code nearbyCap} is the <b>accumulation guard</b> and the reason this
     * verb cannot be a one-line emitter: a timer that drops an item and never
     * looks is the classic way to fill a chunk with entities. When at least
     * {@code nearbyCap} of the same item are already lying within a short radius,
     * the drop is skipped entirely. A pasture of layers left running overnight is
     * the case this is designed for, not a single horse.
     */
    record Produce(String item, int intervalTicks, int min, int max, int nearbyCap,
                   Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>The horse blinks somewhere else.</b> {@code maxDistance} is how far it
     * may travel in one jump and {@code withRider} whether anybody aboard comes
     * along.
     *
     * <p>Two things the translator is required to do, both of which are the
     * whole difficulty of the verb. <b>Validate the destination</b> - never into
     * blocks, never into the void, never past a world border, and refuse the
     * teleport rather than moving the horse somewhere illegal. And
     * <b>respect the authoritative side</b>: a ridden horse's movement is owned
     * by the controlling client, so a server-side move must reach that client as
     * a position it accepts or the rider rubber-bands straight back.
     */
    record Teleport(double maxDistance, boolean withRider, Trigger trigger, int cooldownTicks,
                    Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>The horse makes creatures.</b> {@code mob} is a mob id resolved against
     * the live registry; {@code radius} is how far it looks and places;
     * {@code upTo} is the population it tops the local count <i>up to</i>.
     *
     * <p>{@code upTo} rather than "how many to make" is the entire design.
     * Almost every spawning gene is a lag machine because almost every one counts
     * <i>time</i> and not <i>population</i>: this one counts what is already
     * there first, so a horse in a stocked field does nothing at all, for ever,
     * and only acts when something has been lost. The ceiling is in the
     * definition rather than bolted on as a cooldown.
     *
     * <p>The translator must go through the <b>normal spawn path</b> so that
     * other mods' spawn protections still fire, must never mark what it spawns
     * persistent, and must respect the world's difficulty.
     */
    record Summon(String mob, double radius, int upTo, Trigger trigger,
                  Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>How the horse feels about other creatures</b> - {@link NightTemper}
     * with the night gate lifted out into an ordinary {@link #when()}.
     *
     * <p>{@code NightTemper}'s own note defends welding night into the verb, and
     * that argument holds for that locus: the one thing it is <i>about</i> is
     * that the animal changes after dark. It stops holding the moment a second
     * gene wants the same behaviour on a different gate - "aggressive unless
     * ridden", "aggressive at whatever hurt my owner" - so this is the general
     * version and the two coexist.
     *
     * <p>{@code hold} is the safety rail and defaults true: the horse attacks
     * what comes within {@code radius} and returns, rather than pursuing. A horse
     * that chases is a horse that dies forty blocks from its owner, and the owner
     * blames the gene - correctly.
     */
    record Temper(String mood, String towards, double radius, int intervalTicks,
                  int maxTargets, boolean hold, Trigger trigger, Condition when, int minDose)
            implements GeneAbility {}

    /**
     * <b>Bond, earned by something other than the player.</b> {@code amount} is
     * added every {@code intervalTicks} while {@link #when()} holds.
     *
     * <p>The translator is required to put this through the <b>same daily cap</b>
     * as every other bond source. A source that ignores the cap is an AFK exploit
     * rather than a feature, and it devalues every other way of earning bond in
     * one change.
     */
    record Bond(int amount, int intervalTicks, Condition when, int minDose) implements GeneAbility {}

    /**
     * <b>Nothing hostile appears near the horse.</b> Not pushed away - never
     * spawned, within {@code radius}.
     *
     * <p>It is deliberately <b>not</b> a {@link MobAura} mode. Every other radius
     * effect in the format runs on the horse's own tick and looks outward; this
     * one runs on somebody else's event and looks inward, and folding it into
     * {@code mob_aura} would put a global event handler behind a verb whose whole
     * contract is "a beat, a radius, a cap".
     *
     * <p>Two requirements on the translator, and both are load-bearing.
     * <b>Cancel natural spawns only</b> - cancelling spawner-block, spawn-egg,
     * breeding or structure spawns breaks mob farms, the player's and other
     * mods', and breaks them invisibly because nothing errors. And <b>cache</b>:
     * the handler runs on every spawn attempt in the world, so iterating horses
     * per attempt is a real tick cost.
     */
    record Ward(double radius, Condition when, int minDose) implements GeneAbility {}

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

        /**
         * <b>The horse itself took damage.</b> Fires once per damage event, on
         * the server, before the effect decides whether it cares what hit it.
         *
         * <p>It is a trigger rather than an {@code on_hurt} condition flag
         * because being hurt is an <i>event</i> with no duration: a condition
         * would have to be polled, and the tick a poll would run on is already
         * the tick after the arrow landed.
         */
        record OnHurt() implements Trigger {}

        /**
         * <b>The horse's owner took damage</b>, anywhere in the world.
         *
         * <p>Unlike every other trigger here this one does not start at the
         * horse, which is the whole reason it is worth its own entry: it is the
         * only way a gene can react to something that happened to the
         * <i>player</i>. The translator is required to establish cheaply that
         * the hurt player owns any horse at all before it looks anything up -
         * the event fires on every hit anyone in the world takes.
         */
        record OnOwnerHurt() implements Trigger {}
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
