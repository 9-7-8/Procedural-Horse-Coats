package com.example.horsegenetics.common.horse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>Which dead horses the afterlife store lets go when it is over its size
 * budget</b> - the oldest deaths first, and only as many as it takes to get
 * back under.
 *
 * <h2>A size, not a clock</h2>
 * The store keeps each owned horse that died as a whole entity tag, so that an
 * operator can hand it back with {@code /horseresurrect}. What that costs is
 * bytes in the world save, so the limit is in bytes: a dead horse is kept until
 * keeping it would push the store past {@code ops.resurrect_budget_mb}, however
 * long ago it died (owner, issue #15). The older owner-online window,
 * {@code ops.resurrect_grace_minutes}, still exists as an optional extra limit
 * and is off by default.
 *
 * <p><b>Oldest death first.</b> The newest loss is the one somebody is most
 * likely still to ask about. Nothing here can touch a living horse, because the
 * store holds only dead ones, and nothing here touches the ancestry data: a
 * dropped entry means "can no longer be resurrected", and the pedigree keeps
 * its record as before.
 *
 * <p>Pure arithmetic, so it can be tested without a game ({@code AfterlifeBudgetTest}).
 * The game module measures each entry and drops whatever this returns.
 */
public final class AfterlifeBudget {

    private AfterlifeBudget() {
    }

    /** {@code ops.resurrect_budget_mb}'s default: about 100 MB, the owner's figure (issue #15). */
    public static final int DEFAULT_BUDGET_MB = 100;

    /** Bytes in one megabyte, as the setting counts them. */
    public static final long BYTES_PER_MB = 1024L * 1024L;

    /**
     * One kept horse, as far as the budget cares.
     *
     * @param id     whatever the caller keys it by
     * @param diedAt when it died, on any clock where larger is later
     * @param bytes  what it costs to keep
     */
    public record Entry<K>(K id, long diedAt, long bytes) {
    }

    /** The total cost of everything kept. */
    public static <K> long total(List<Entry<K>> entries) {
        long sum = 0;
        for (Entry<K> entry : entries) {
            sum += entry.bytes();
        }
        return sum;
    }

    /**
     * The ids to drop to bring {@code entries} within {@code budgetBytes},
     * oldest death first. Empty when they already fit, and always empty for a
     * budget of {@code 0} or less, which means "no cap". Ties on the time of
     * death go in the order given.
     */
    public static <K> List<K> overflow(List<Entry<K>> entries, long budgetBytes) {
        List<K> drop = new ArrayList<>();
        if (budgetBytes <= 0) {
            return drop;
        }
        long total = total(entries);
        if (total <= budgetBytes) {
            return drop;
        }
        List<Entry<K>> oldestFirst = new ArrayList<>(entries);
        oldestFirst.sort(Comparator.comparingLong(Entry::diedAt));
        for (Entry<K> entry : oldestFirst) {
            if (total <= budgetBytes) {
                break;
            }
            drop.add(entry.id());
            total -= entry.bytes();
        }
        return drop;
    }
}
