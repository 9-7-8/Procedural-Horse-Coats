package com.example.horsegenetics.common.care;

/**
 * <b>Every sentence an owner reads when their horse is hurt</b>, and the rule
 * that stops those sentences becoming a wall of text.
 *
 * <p>Owner's ask: <i>"When an owned horse takes damage, only its owner should
 * receive a chat message with the horse name, damage source, and, when
 * applicable, how to cure or prevent it."</i>
 *
 * <p>Here rather than in the game module for {@link com.example.horsegenetics.common.repro.ReproText}'s
 * reason - the wording is the feature, so the wording is what a test should be
 * able to pin down. Nothing in here reads a horse, a level or a damage source:
 * the game module looks at vanilla's {@code DamageSource}, picks a {@link Cause},
 * and asks for the line.
 *
 * <h2>Advice is prevention; the cure is one line and always the same</h2>
 * A {@link Cause} carries the advice that is specific to it - get it out of the
 * powder snow, wall the pen, stable it by day. What to do about the injury
 * itself does not vary by cause (a horse heals beside water once it has eaten,
 * per {@link Hunger}), so it is appended once, and only when the horse is
 * actually in danger of dying - see {@link #BADLY_HURT}. A message on every
 * scratch that ends "it heals beside water" would train the owner to stop
 * reading the sentence that matters.
 */
public final class HurtNotice {

    private HurtNotice() {
    }

    /**
     * <b>How long a horse stays quiet after one message</b>, in ticks - five
     * seconds. Per horse, whatever hurt it: a burning horse takes damage every
     * two seconds and a horse among zombies several times a second, so without
     * this the first fire in a pen fills the chat log.
     *
     * <p>Five seconds and not longer because the message is an alarm - the owner
     * is meant to be able to get there - and not shorter because two lines about
     * the same fire tell the owner nothing the first did not.
     */
    public static final long QUIET_TICKS = 100L;

    /**
     * Damage below this (in health points, so a quarter of a heart) is not worth
     * a line. Keeps a thorn-scratch or a rounding remainder from spending a
     * horse's whole quiet period.
     */
    public static final double MIN_DAMAGE = 0.5;

    /** At or below this fraction of its own maximum, a horse is in real danger and the cure line is added. */
    public static final double BADLY_HURT = 0.25;

    /**
     * <b>What hurt a horse</b>, grouped by the advice it earns rather than by
     * vanilla's damage types - a campfire, a lava pool and a burning horse are
     * three damage types and one thing to do about it.
     */
    public enum Cause {
        /**
         * The sun-sensitivity locus burning a horse in daylight. It is vanilla's
         * on-fire damage on the wire, so only the game module can tell it apart
         * from an ordinary fire; {@link #of(String)} never returns it.
         */
        SUNLIGHT("sunlight",
                "Daylight burns this one - shade, a roof or a stable by day. It runs for cover on its own if there is any."),
        FIRE("fire", "Water puts a horse out, and the pen wants to be clear of anything that can set one alight."),
        LAVA("lava", "Nothing walks out of it. Wall the pen away from open lava."),
        DROWNING("drowning", "It cannot reach the surface. Lead it out at a shallow edge."),
        SUFFOCATION("being crushed against a block",
                "It is stuck. Break the block out, and remember a big horse needs a taller doorway than you do."),
        FALL("a fall", "Fence the drops around the pen."),
        EXPLOSION("an explosion", "Light the pen and wall it, so nothing gets close enough to go off."),
        FREEZING("the cold", "Powder snow. Get it out - a horse has nothing that keeps the cold off."),
        PRICKLES("thorns", "Cactus and sweet berries hurt anything that walks into them. Clear them out of the pen."),
        ATTACK("an attack", "Something is attacking it - light and fence the pen, or move the horse."),
        MAGIC("magic", "Nothing wards it off. Get the horse out of range."),
        /**
         * The mod's own {@code horsegenetics:genetic_defect} - a lethal genotype
         * killing a foal. The only cause whose advice is about breeding rather
         * than about the pen, and the only one with no cure at all.
         */
        GENETIC_DEFECT("a genetic defect",
                "A lethal genotype, not an injury, and nothing cures it. The fix is not to pair those two again."),
        /** Anything else, including whatever other mods add. Named, never explained. */
        UNKNOWN("something", "");

        private final String phrase;
        private final String advice;

        Cause(String phrase, String advice) {
            this.phrase = phrase;
            this.advice = advice;
        }

        /** How the message names it, after "from". */
        public String phrase() {
            return phrase;
        }

        /** What to do about it, or empty when there is nothing honest to say. */
        public String advice() {
            return advice;
        }
    }

    /**
     * The {@link Cause} for one of vanilla's damage-type message ids
     * ({@code DamageSource.getMsgId()}), or {@link Cause#UNKNOWN}.
     *
     * <p><b>The ids are matched, not verified.</b> They are vanilla's own
     * strings and a wrong one costs a sentence of advice, never correctness - an
     * unrecognised id is simply "something". Both spellings of the void id are
     * listed for that reason.
     */
    public static Cause of(String msgId) {
        if (msgId == null) {
            return Cause.UNKNOWN;
        }
        switch (msgId) {
            case "inFire":
            case "onFire":
            case "campfire":
            case "hotFloor":
            case "fireball":
            case "unattributed_fireball":
            case "fireworks":
                return Cause.FIRE;
            case "lava":
                return Cause.LAVA;
            case "drown":
                return Cause.DROWNING;
            case "inWall":
            case "cramming":
                return Cause.SUFFOCATION;
            case "fall":
            case "flyIntoWall":
            case "stalagmite":
            case "falling_stalactite":
            case "fallingBlock":
            case "anvil":
                return Cause.FALL;
            case "explosion":
            case "explosion.player":
            case "badRespawnPoint":
                return Cause.EXPLOSION;
            case "freeze":
                return Cause.FREEZING;
            case "cactus":
            case "sweetBerryBush":
            case "thorns":
            case "sting":
                return Cause.PRICKLES;
            case "mob":
            case "player":
            case "arrow":
            case "trident":
            case "mobProjectile":
            case "thrown":
            case "sonic_boom":
            case "mace_smash":
            case "spit":
            case "lightningBolt":
                return Cause.ATTACK;
            case "magic":
            case "indirectMagic":
            case "wither":
            case "witherSkull":
            case "dragonBreath":
                return Cause.MAGIC;
            case "genetic_defect":
                return Cause.GENETIC_DEFECT;
            default:
                return Cause.UNKNOWN;
        }
    }

    /** Is this much damage worth telling the owner about at all? */
    public static boolean worthTelling(double damage) {
        return damage >= MIN_DAMAGE;
    }

    /**
     * May this horse be spoken about again? {@code lastTold} is the tick of its
     * last message, or a negative number for a horse never spoken about.
     */
    public static boolean dueAgain(long lastTold, long now) {
        return lastTold < 0L || now - lastTold >= QUIET_TICKS || now < lastTold;
    }

    /**
     * <b>The line the owner reads.</b> {@code attacker} names what dealt the
     * blow when the game module knows it ("a zombie") and is empty otherwise, in
     * which case the cause speaks for itself.
     *
     * <p>{@code damage}, {@code healthLeft} and {@code maxHealth} are health
     * points, the game's own unit; the sentence is in hearts, the player's.
     */
    public static String line(String horseName, Cause cause, String attacker,
            double damage, double healthLeft, double maxHealth) {
        String from = attacker == null || attacker.isEmpty() ? cause.phrase() : attacker;
        StringBuilder out = new StringBuilder();
        out.append(horseName).append(" took ").append(hearts(damage)).append(" from ").append(from).append('.');
        if (!cause.advice().isEmpty()) {
            out.append(' ').append(cause.advice());
        }
        if (cause != Cause.GENETIC_DEFECT && badlyHurt(healthLeft, maxHealth)) {
            out.append(" It is down to ").append(hearts(healthLeft))
                    .append(" - a horse heals beside water, once it has eaten.");
        }
        return out.toString();
    }

    /** Is the horse close enough to death that the owner should be told how to heal it? */
    public static boolean badlyHurt(double healthLeft, double maxHealth) {
        return maxHealth > 0.0 && healthLeft <= maxHealth * BADLY_HURT;
    }

    /**
     * Health points as the hearts a player counts, to the nearest half - "half a
     * heart", "1 heart", "1.5 hearts". Zero for anything that rounds away, which
     * {@link #worthTelling} has already turned back.
     */
    public static String hearts(double health) {
        long halves = Math.round(Math.max(0.0, health));
        if (halves == 0L) {
            return "no hearts";
        }
        if (halves == 1L) {
            return "half a heart";
        }
        if (halves == 2L) {
            return "1 heart";
        }
        String count = halves % 2L == 0L ? String.valueOf(halves / 2L) : (halves / 2L) + ".5";
        return count + " hearts";
    }
}
