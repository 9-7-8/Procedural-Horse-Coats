package com.example.horsegenetics.common.care;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Which of the whistles a player is carrying are worth blowing at once.</b>
 *
 * <p>One key blows every whistle in the pack, which would be silly taken
 * literally: a player who has all three area tiers on them would blow three
 * whistles and pay three cooldowns to do exactly what the largest one did on
 * its own. So the key blows only the whistles that are <b>not degenerate to one
 * another</b>, and that rule is this class.
 *
 * <ul>
 *   <li><b>Area whistles are degenerate.</b> A bigger radius contains a smaller
 *       one, so only the largest is blown and the rest are left alone -
 *       cooldowns and all, so the player still has them for later.</li>
 *   <li><b>Ender whistles are not</b>, because each is bound to a different
 *       horse. Every distinct binding is blown.</li>
 *   <li><b>Two ender whistles bound to the same horse are.</b> Binding is per
 *       whistle, not per horse, so a player can hold two for one animal; that
 *       horse is called once.</li>
 *   <li>An <b>unbound</b> ender whistle is not a whistle you can blow.</li>
 * </ul>
 *
 * <p><b>What is deliberately not here:</b> the case where an area whistle
 * already reached the horse an ender whistle is bound to. That is not a
 * property of the whistles - it depends on where the horse happens to be
 * standing - so it cannot be decided without the world, and the caller does it
 * after the area call reports which horses it handled.
 *
 * <p>Pure, and in {@code common/} so it can be unit-tested without a game; the
 * NeoForge side does the inventory scanning, the cooldowns and the messages.
 */
public final class WhistleSelection {

    private WhistleSelection() {
    }

    /**
     * One whistle a player is carrying, as this rule needs to see it.
     *
     * @param slot      the caller's own handle for the whistle - an inventory
     *                  index, a container id, whatever it uses. Never read here
     *                  beyond being handed back, so the caller may key it
     *                  however it likes
     * @param radius    an area whistle's reach, or <b>0 for an ender whistle</b>
     * @param boundId   the horse an ender whistle is bound to, or null/blank for
     *                  an area whistle or an unbound ender one. A plain string
     *                  rather than a UUID so this stays free of anything the
     *                  backport target would have to supply
     */
    public record Candidate(int slot, int radius, String boundId) {

        public boolean isArea() {
            return radius > 0;
        }

        public boolean isBoundEnder() {
            return radius <= 0 && boundId != null && !boundId.isBlank();
        }
    }

    /**
     * The whistles to blow, <b>area whistle first</b>, then the ender whistles
     * in the order they were carried.
     *
     * <p>The ordering is a contract, not a detail: the caller finishes the job
     * by skipping any ender whistle whose horse the area call already reached,
     * and it can only know which those are once the area whistle has been
     * blown. Returning them in inventory order would make that work or not
     * depending on which pocket the player kept the whistle in.
     */
    public static List<Candidate> pick(List<Candidate> carried) {
        Candidate biggestArea = null;
        for (Candidate c : carried) {
            // Strictly greater, so a tie keeps the first one seen and the answer
            // does not depend on how the inventory scan happened to be ordered.
            if (c.isArea() && (biggestArea == null || c.radius() > biggestArea.radius())) {
                biggestArea = c;
            }
        }

        List<Candidate> picked = new ArrayList<>();
        if (biggestArea != null) {
            picked.add(biggestArea);
        }
        Set<String> alreadyBound = new HashSet<>();
        for (Candidate c : carried) {
            if (c.isBoundEnder() && alreadyBound.add(c.boundId())) {
                picked.add(c);
            }
        }
        return picked;
    }
}
