package com.example.horsegenetics.common.horse;

import java.util.Collection;
import java.util.UUID;

/**
 * <b>Which records the ancestry store may forget</b> when the horse itself leaves the world for good (owner,
 * 2026-10-06, lag audit #200).
 *
 * <p>The store kept every horse that ever had a record - every wild founder, including the ones wild turnover,
 * a cowboy's rotation, a wandering dealer or a trader took away again. Each record holds a whole genome, the
 * whole store is re-encoded on every save, and the roster and population requests walk all of it. A record is
 * forgotten only when nothing could ever ask for it again:
 * <ul>
 *   <li><b>nobody's horse</b> - no owner, never tamed, not bred by a player. Whistles, the roster and transfer
 *       papers all hang off those;</li>
 *   <li><b>no foal names it</b> as mother or father, so no pedigree has a hole where it was;</li>
 *   <li><b>never covered a mare</b> - a pregnancy carries only his id since #202, and his foal is born after he
 *       may be gone. Conservative: a stallion who has ever covered is kept even once the foal exists.</li>
 * </ul>
 * Pure rule; the game module decides when a horse has left and asks.
 */
public final class AncestryPruning {

    private AncestryPruning() {
    }

    /**
     * @param record     the leaving horse's record
     * @param hasCovered whether the horse has ever covered a mare
     * @param everyone   every record in the store, for the parent check
     */
    public static boolean mayForget(HorseRecord record, boolean hasCovered, Collection<HorseRecord> everyone) {
        if (hasCovered || record.ownerId().isPresent() || record.tamedBy().isPresent()
                || record.bredBy().isPresent()) {
            return false;
        }
        return !isParent(record.id(), everyone);
    }

    /** Does any record name {@code id} as its mother or father? */
    public static boolean isParent(UUID id, Collection<HorseRecord> everyone) {
        for (HorseRecord other : everyone) {
            if (other.motherId().filter(id::equals).isPresent() || other.fatherId().filter(id::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
