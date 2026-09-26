package com.example.horsegenetics.neoforge.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>Type a few letters at a dropdown and it jumps to them.</b> The behaviour
 * every desktop list has had for thirty years, and the only way a list of three
 * hundred genes is usable at all. (Owner, 2026-09-26: "all drop down boxes
 * should have the thing where they're alphabetically sorted and you start
 * typing and it jumps to the thing you started typing".)
 *
 * <p>This mod has four hand-rolled dropdowns - the browser's gene and allele
 * pickers, the gear slot picker, the saved-search menu and the query builder's
 * column and value menus. They are hand-rolled because vanilla has no anchored
 * list widget, and each one grew where it was needed. Rather than unify five
 * screens' worth of drawing, the <i>behaviour</i> they were all missing is one
 * object they each hold.
 *
 * <h2>Prefix, not substring</h2>
 * Typing {@code fl} goes to the first entry <b>starting</b> with "fl", which is
 * what the gesture means everywhere else and what makes an alphabetical list
 * navigable: the entries you are walking past are in an order you can predict.
 * A substring filter is a different and also useful thing - it is what the
 * query builder's column menu does with a longer string - but it is not what
 * pressing a letter should do.
 *
 * <h2>Why it forgets</h2>
 * Letters typed within {@value #RESET_MS} milliseconds of each other build one
 * prefix; a pause starts again. Without that, walking a list by pressing the
 * same letter repeatedly - the other half of this gesture - would instead spell
 * {@code sssss} and match nothing, and a list you had typed at ten minutes ago
 * would still be holding the prefix.
 */
public final class TypeAhead {

    /** How long a prefix survives without another keystroke. */
    private static final long RESET_MS = 1000L;

    private String typed = "";
    private long lastAt;

    /** What has been typed so far, for a menu that wants to show it. */
    public String typed() {
        return typed;
    }

    public void reset() {
        typed = "";
        lastAt = 0L;
    }

    /**
     * Take one typed character and say where to go.
     *
     * <p>Pressing the <b>same single letter</b> again walks to the next entry
     * beginning with it rather than looking for a doubled letter, which is the
     * other half of what this gesture means: {@code b}, {@code b}, {@code b}
     * steps through the Bs.
     *
     * @param from  the entry currently in view, so repeats can step past it
     * @return the index to jump to, or {@code -1} for no match (in which case
     *         nothing should move - a list that jumps to the top on a typo is
     *         worse than one that ignores it)
     */
    public int accept(int codepoint, List<String> options, int from) {
        if (codepoint < ' ' || options == null || options.isEmpty()) {
            return -1;
        }
        long now = System.currentTimeMillis();
        String letter = new String(Character.toChars(codepoint)).toLowerCase(Locale.ROOT);
        boolean repeat = typed.length() == 1 && typed.equals(letter)
                && now - lastAt <= RESET_MS;
        if (now - lastAt > RESET_MS) {
            typed = letter;
        } else if (!repeat) {
            typed = typed + letter;
        }
        lastAt = now;

        int start = repeat ? from + 1 : 0;
        int found = search(options, typed, start);
        if (found < 0 && repeat) {
            found = search(options, typed, 0);      // wrap round the Bs
        }
        if (found < 0 && typed.length() > 1) {
            // The prefix has gone past anything that exists. Treat the last
            // letter as a fresh start rather than leaving the player stuck
            // typing into a prefix that can never match again.
            typed = letter;
            found = search(options, typed, 0);
        }
        return found;
    }

    private static int search(List<String> options, String prefix, int from) {
        for (int i = Math.max(0, from); i < options.size(); i++) {
            String option = options.get(i);
            if (option != null && option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Alphabetical, case-insensitively, as a copy. The other half of the same
     * request - type-ahead on an unsorted list is a guessing game.
     */
    public static List<String> sorted(List<String> options) {
        List<String> out = new ArrayList<>(options);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
