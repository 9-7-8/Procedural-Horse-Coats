package com.example.horsegenetics.common.care;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The rules and the wording of the command whistle</b>: which order a horse may take,
 * why it refuses one, and what a whole field of horses answered. Pure, so the NeoForge
 * side only gathers the horse's {@link Situation} and prints what comes back (hard rule 1:
 * the wording lives in common/).
 *
 * <p>The gate, in the order it is asked, so a horse gives the reason a player can act on
 * first: the lead or the cart wins over any order; then the bond. Rejoin herd is never
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

    /** What the horse is doing that an order has to get past. */
    public record Situation(int bondTier, boolean leashed, boolean pullingCart) {
    }

    /** Why a horse would not take an order. {@code text} finishes "Name ...". */
    public enum Refusal {
        ON_A_LEAD("is on a lead", "on a lead"),
        PULLING_A_CART("is pulling a cart", "pulling a cart"),
        NOT_BONDED("is not bonded enough", "not bonded enough");

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

    /** The orders on the wheel, clockwise from the top. */
    public static List<HorseOrder> wheel() {
        return List.of(HorseOrder.STAY, HorseOrder.FOLLOW, HorseOrder.WANDER, HorseOrder.REJOIN_HERD);
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

    /** The item's tooltip, one line each. */
    public static List<String> tooltip() {
        return List.of(
                "Hold use on a horse of yours to give it an order.",
                "Sneak and hold to order every horse of yours within " + REACH_BLOCKS + " blocks.",
                "Stay and Follow need bond 31; Wander needs 61.");
    }
}
