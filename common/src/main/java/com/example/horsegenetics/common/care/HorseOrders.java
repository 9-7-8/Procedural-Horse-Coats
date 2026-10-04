package com.example.horsegenetics.common.care;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.genes.AggressionGene;
import com.example.horsegenetics.common.genetics.genes.MagicFighterGene;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>The rules and the wording of the command whistle</b>: which order a horse may take,
 * why it refuses one, and what a whole field of horses answered. Pure, so the NeoForge
 * side only gathers the horse's {@link Situation} and prints what comes back (hard rule 1:
 * the wording lives in common/).
 *
 * <p>The gate, in the order it is asked, so a horse gives the reason a player can act on
 * first: the lead or the cart wins over any order; then, for a combat order, whether the
 * horse was bred to fight at all; then the bond. Breeding comes before the bond because
 * it is the one answer no amount of care changes - a player told "not bonded enough"
 * would raise the bond for an order the horse can never take. Rejoin herd is never
 * refused - clearing an order is always allowed, even from a horse on a lead.
 */
public final class HorseOrders {

    private HorseOrders() {
    }

    /** How far the whistle reaches: the aimed horse, and every horse a sneak-hold orders. */
    public static final int REACH_BLOCKS = 16;

    /** A Follow horse stops this close to the player. */
    public static final double FOLLOW_STOP_BLOCKS = 3.0;

    /** A Follow horse further behind than this, whose path fails, is brought to the player. */
    public static final double FOLLOW_TELEPORT_BLOCKS = 24.0;

    /** A Stay horse further than this from its spot walks back to it. */
    public static final double STAY_SLACK_BLOCKS = 1.5;

    /** Hunt monsters' reach from its spot, unless the server says otherwise (orders.hunt_radius). */
    public static final int DEFAULT_HUNT_RADIUS = 16;
    public static final int MIN_HUNT_RADIUS = 8;
    public static final int MAX_HUNT_RADIUS = 32;

    /** Defend me's reach from its player, unless the server says otherwise (orders.defend_radius). */
    public static final int DEFAULT_DEFEND_RADIUS = 8;
    public static final int MIN_DEFEND_RADIUS = 4;
    public static final int MAX_DEFEND_RADIUS = 16;

    /** Guard here's reach from its spot, unless the server says otherwise (orders.guard_radius). */
    public static final int DEFAULT_GUARD_RADIUS = 6;
    public static final int MIN_GUARD_RADIUS = 3;
    public static final int MAX_GUARD_RADIUS = 16;

    /** How far from its spot a horse told to Graze nearby may roam (orders.graze_radius). */
    public static final int DEFAULT_GRAZE_RADIUS = 12;
    public static final int MIN_GRAZE_RADIUS = 4;
    public static final int MAX_GRAZE_RADIUS = 32;

    /**
     * A grazing horse that strayed past its radius is walked back until it is within this
     * share of it - well inside, so a horse at the edge is not turned back every step.
     */
    public static final double TETHER_RETURN_SHARE = 0.5;

    /**
     * Below this share of its health a horse on a combat order drops its quarry and goes
     * back (orders.break_off_health; 0 = fight to the death). An order should not be the
     * thing that kills a player's horse.
     */
    public static final double DEFAULT_BREAK_OFF_HEALTH = 0.3;

    /**
     * How far past its radius a fight may drift before the horse lets it go. Without it
     * a monster stepping in and out of the edge would be picked and dropped every scan.
     */
    public static final double LEASH_SLACK_BLOCKS = 4.0;

    /** Ticks between a combat order's scans: the aggression locus's beat, a second. */
    public static final int COMBAT_SCAN_TICKS = AggressionGene.INTERVAL_TICKS;

    /** Most monsters one scan may consider. Every radius effect states a cap. */
    public static final int COMBAT_MAX_TARGETS = AggressionGene.MAX_TARGETS;

    /** What the horse is doing that an order has to get past, and whether it was bred to fight. */
    public record Situation(int bondTier, boolean leashed, boolean pullingCart, boolean fighter) {
    }

    /**
     * <b>The tether on Graze nearby</b>: free inside the radius, walked back once outside
     * it, and let go again only well inside ({@link #TETHER_RETURN_SHARE}). Distances are
     * squared and across the ground, as the Stay order judges its spot.
     *
     * @param radius blocks from the spot the horse may roam
     */
    public record Tether(double radius) {

        public Tether {
            radius = Math.max(1.0, radius);
        }

        /** Has the horse roamed past its radius, so it must be walked back? */
        public boolean strayed(double fromSpotSq) {
            return fromSpotSq > radius * radius;
        }

        /** Is a horse being walked back far enough in to be let go? */
        public boolean backInside(double fromSpotSq) {
            double in = radius * TETHER_RETURN_SHARE;
            return fromSpotSq <= in * in;
        }
    }

    /**
     * <b>The leash on a combat order</b>: how far from its centre (the spot, for Hunt
     * monsters; the player, for Defend me) the horse may pick a fight, and when it gives
     * one up. An order may pursue, but only inside this - the aggression locus's
     * no-chase rule stands for the gene (wiki/gene-aggression.html).
     *
     * @param radius         blocks from the centre a monster may be picked within
     * @param breakOffHealth the share of its health below which the horse stops; 0 never
     */
    public record Leash(double radius, double breakOffHealth) {

        public Leash {
            radius = Math.max(1.0, radius);
            breakOffHealth = Math.max(0.0, Math.min(1.0, breakOffHealth));
        }

        /** Is the horse too hurt to keep fighting for an order? */
        public boolean breaksOff(double health, double maxHealth) {
            return breakOffHealth > 0.0 && health < maxHealth * breakOffHealth;
        }

        /** May a monster this far (squared) from the centre be picked by a horse this healthy? */
        public boolean mayEngage(double targetFromCentreSq, double health, double maxHealth) {
            return targetFromCentreSq <= radius * radius && !breaksOff(health, maxHealth);
        }

        /**
         * Does a fight the order started go on? Not once the quarry or the horse is more
         * than {@link #LEASH_SLACK_BLOCKS} past the radius, nor once the horse breaks off.
         */
        public boolean keepsFighting(double targetFromCentreSq, double horseFromCentreSq,
                                     double health, double maxHealth) {
            double outer = radius + LEASH_SLACK_BLOCKS;
            return targetFromCentreSq <= outer * outer && horseFromCentreSq <= outer * outer
                    && !breaksOff(health, maxHealth);
        }
    }

    /**
     * <b>Was this horse bred to fight?</b> The gate on every combat order (owner, 2026-10-03):
     * it must EXPRESS one of three loci -
     * <ul>
     *   <li>Aggression, a matched pair whose target includes monsters: the {@code h}
     *       pairs, and the {@code a} ("everything") pairs too (owner's call when Piece 3
     *       was built - an order only adds a target; what the gene already goes for, it
     *       still goes for);</li>
     *   <li>Guardian, two copies;</li>
     *   <li>Magic fighter, hitting harder than the {@value MagicFighterGene#BASELINE_DAMAGE}
     *       baseline - a fighter or a champion, or a balanced horse whose gladiator copy
     *       outweighs its wimp copy. A weak or harmless horse is not one.</li>
     * </ul>
     * Read through the abilities each gene grants ({@link HorseAbilities#of}), the same
     * path the genes' own behaviour runs on, so the gate and the gene never disagree.
     *
     * @param epigenome may be {@code null}: each copy then reads its midpoint
     */
    public static boolean fighter(Genotype genotype, Epigenome epigenome) {
        return grants(Genes.AGGRESSION, genotype, epigenome)
                || grants(Genes.GUARDIAN, genotype, epigenome)
                || grants(Genes.MAGIC_FIGHTER, genotype, epigenome);
    }

    private static boolean grants(Gene gene, Genotype genotype, Epigenome epigenome) {
        if (genotype.pair(gene) == null) {
            return false;
        }
        for (GeneAbility ability : HorseAbilities.of(gene, genotype, epigenome)) {
            if (ability instanceof GeneAbility.Temper t && "aggressive".equals(t.mood())
                    && ("hostile".equals(t.towards()) || "all".equals(t.towards()))) {
                return true;
            }
            if (ability instanceof GeneAbility.Combat c && c.damage() > MagicFighterGene.BASELINE_DAMAGE) {
                return true;
            }
        }
        return false;
    }

    /** Why a horse would not take an order. {@code text} finishes "Name ...". */
    public enum Refusal {
        ON_A_LEAD("is on a lead", "on a lead"),
        PULLING_A_CART("is pulling a cart", "pulling a cart"),
        NOT_A_FIGHTER("was not bred to fight", "not bred to fight"),
        NOT_BONDED("is not bonded enough", "not bonded enough"),
        // Go home's own answers, given by the Send home trip after the gate above has
        // passed. The wheel never greys for them: the client does not know the stalls.
        HAS_A_RIDER("has a rider, and stays put", "ridden"),
        NO_HOME("has no stall, and you have no holding pen", "no stall"),
        CANNOT_GET_HOME("cannot get home - check its stall sign is up and there is room", "cannot get home"),
        CANNOT_PAY("cannot go: you cannot pay for the trip home", "unpaid");

        private final String text;
        private final String summary;

        Refusal(String text, String summary) {
            this.text = text;
            this.summary = summary;
        }

        /** "Bramble is not bonded enough" reads as {@code name + " " + text()}. */
        public String text() {
            return text;
        }

        /** The words in a summary line: "1 not bonded enough". */
        public String summary() {
            return summary;
        }
    }

    /** The reason this horse refuses the order, or {@code null} when it obeys. */
    public static Refusal refusal(HorseOrder order, Situation horse) {
        if (order == HorseOrder.REJOIN_HERD) {
            return null;
        }
        if (horse.leashed()) {
            return Refusal.ON_A_LEAD;
        }
        if (horse.pullingCart()) {
            return Refusal.PULLING_A_CART;
        }
        if (order.combat() && !horse.fighter()) {
            return Refusal.NOT_A_FIGHTER;
        }
        if (horse.bondTier() < order.bondTier()) {
            return Refusal.NOT_BONDED;
        }
        return null;
    }

    /** The bond a horse needs for an order, as the wheel's hover says it: "Needs bond 61". */
    public static String bondNeeded(HorseOrder order) {
        return "Needs bond " + switch (order.bondTier()) {
            case 0 -> 0;
            case 1 -> 31;
            case 2 -> 61;
            default -> 81;
        };
    }

    /** Why the wheel greys a slice, under the wheel: "Needs bond 61", "Needs a horse bred to fight". */
    public static String needs(HorseOrder order, Refusal refusal) {
        return switch (refusal) {
            case NOT_BONDED -> bondNeeded(order);
            case NOT_A_FIGHTER -> "Needs a horse bred to fight";
            default -> "Not while " + refusal.summary();
        };
    }

    /**
     * The orders on the wheel, clockwise from the top, kin beside kin: the three that hold
     * a spot, then the two at your side, then the free ones and the ways back.
     */
    public static List<HorseOrder> wheel() {
        return List.of(HorseOrder.STAY, HorseOrder.GUARD_HERE, HorseOrder.HUNT_MONSTERS,
                HorseOrder.DEFEND_ME, HorseOrder.FOLLOW, HorseOrder.WANDER,
                HorseOrder.GRAZE_NEARBY, HorseOrder.GO_HOME, HorseOrder.REJOIN_HERD);
    }

    /** The one chat line for an order given to one horse. */
    public static String oneLine(String horseName, HorseOrder order, Refusal refusal) {
        if (refusal != null) {
            return horseName + " " + refusal.text() + ".";
        }
        return switch (order) {
            case REJOIN_HERD -> horseName + " goes back to its herd.";
            case STAY -> horseName + " stays here.";
            case FOLLOW -> horseName + " follows you.";
            case WANDER -> horseName + " wanders off on its own.";
            case HUNT_MONSTERS -> horseName + " hunts the monsters around here.";
            case DEFEND_ME -> horseName + " stays at your side and sees off monsters.";
            case GRAZE_NEARBY -> horseName + " grazes around here.";
            case GUARD_HERE -> horseName + " guards this spot.";
            case GO_HOME -> horseName + " goes home.";
        };
    }

    /**
     * The one line for a sneak-hold over many horses: "3 obeyed; 1 not bonded enough;
     * 1 on a lead". Refusals are counted per reason, in {@link Refusal} order.
     */
    public static String summary(HorseOrder order, List<Refusal> answers) {
        if (answers.isEmpty()) {
            return "No horses of yours within " + REACH_BLOCKS + " blocks.";
        }
        int obeyed = 0;
        Map<Refusal, Integer> refused = new EnumMap<>(Refusal.class);
        for (Refusal r : answers) {
            if (r == null) {
                obeyed++;
            } else {
                refused.merge(r, 1, Integer::sum);
            }
        }
        List<String> parts = new ArrayList<>();
        parts.add(order.label() + ": " + obeyed + " obeyed");
        for (Map.Entry<Refusal, Integer> e : refused.entrySet()) {
            parts.add(e.getValue() + " " + e.getKey().summary());
        }
        return String.join("; ", parts) + ".";
    }

    /** What holding use with the command whistle opens. */
    public enum WhistleUse {
        /** Sneaking: the wheel for every horse of the player's in reach. */
        EVERY_HORSE,
        /** The wheel for the one aimed horse. */
        AIMED_HORSE,
        /** No wheel: the click goes on as it would without the whistle. */
        NOTHING
    }

    /**
     * What holding use with the whistle does, on the client. {@code aimed} is the synced
     * record of the horse under the crosshair, or null for no horse or no record yet.
     *
     * <p><b>Ownership is the record's, never vanilla's</b> (issue #36). A client's
     * {@code getOwnerReference()} is always null - vanilla syncs the tamed flag and not the
     * owner - so a test against it refused every horse, and only the sneak-hold, which asks
     * no owner, ever opened the wheel. {@link HorseRecord#ownedBy} reads the owner the
     * server mirrors onto the record every two seconds; the server still decides.
     */
    public static WhistleUse whistleUse(boolean sneaking, HorseRecord aimed, UUID player) {
        if (sneaking) {
            return WhistleUse.EVERY_HORSE;
        }
        return aimed != null && aimed.ownedBy(player) ? WhistleUse.AIMED_HORSE : WhistleUse.NOTHING;
    }

    /** The item's tooltip, one line each. */
    public static List<String> tooltip() {
        return List.of(
                "Hold use on a horse of yours to give it an order.",
                "Sneak and hold to order every horse of yours within " + REACH_BLOCKS + " blocks.",
                "Stay and Follow need bond 31; Wander, Graze nearby and Go home need 61.",
                "Hunt monsters, Guard here and Defend me need 61, and a horse bred to fight.");
    }
}
