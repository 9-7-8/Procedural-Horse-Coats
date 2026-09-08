package com.example.horsegenetics.common.trait;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>What a miscarriage looked like</b> - one sentence per embryonic-lethal
 * condition, written to be told apart from the others <i>without naming the
 * gene</i>.
 *
 * <h2>Why the disorder is not named</h2>
 * A pairing that comes to nothing is the only evidence a player ever gets that
 * two of their horses share a recessive lethal, and the whole value of that
 * evidence is that they have to work it out. Printing "this pair carries MET"
 * would answer the question the pedigree exists to ask. So the line describes
 * the <i>sign</i> - what the breeder would have seen - and two different lethals
 * read differently, which is enough to sort a herd into two groups over a dozen
 * pairings without ever handing anyone the answer.
 *
 * <p>({@link Condition#description()} does name it, and is what the
 * <b>information screen</b> shows for a horse a player already has in front of
 * them. The distinction is deliberate: a horse you can inspect tells you
 * everything, an event you witnessed tells you what it looked like.)
 *
 * <h2>The fallback is not a bug</h2>
 * A condition with no entry here gets {@link #GENERIC}, which describes a
 * miscarriage without claiming anything specific. That is the right failure:
 * a new lethal gene keeps working the day it is added, it simply reads like
 * every other one until somebody writes it a sign. Add the entry in the same
 * change as the condition.
 */
public final class MiscarriageSigns {

    /** Used for any lethal with no sign of its own - see the class note. */
    public static final String GENERIC =
            "The pregnancy ended early. There was little to see and nothing to recover.";

    private static final Map<String, String> BY_ID = new LinkedHashMap<>();

    static {
        put("met-embryonic-lethal",
                "The pregnancy simply stopped. There was no foal to find - the mare came back "
                        + "into season within days as though nothing had ever started.");
        put("milk-embryonic-lethal",
                "The mare broke out in a hot sweat and lost the pregnancy in a rush of "
                        + "steam. Whatever was in her would not hold together.");
    }

    private static void put(String conditionId, String sign) {
        BY_ID.put(conditionId, sign);
    }

    private MiscarriageSigns() {
    }

    /** The sign for {@code condition}, or {@link #GENERIC} if none is written. */
    public static String of(Condition condition) {
        if (condition == null) {
            return GENERIC;
        }
        return BY_ID.getOrDefault(condition.id(), GENERIC);
    }

    /** Is there a sign written for this condition, or is it falling back? */
    public static boolean isWritten(Condition condition) {
        return condition != null && BY_ID.containsKey(condition.id());
    }
}
