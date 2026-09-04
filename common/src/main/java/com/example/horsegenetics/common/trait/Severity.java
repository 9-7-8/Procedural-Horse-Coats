package com.example.horsegenetics.common.trait;

/**
 * How badly a {@link Condition} affects the horse carrying it. The ordinal
 * order is "increasingly bad", so a horse's overall {@link Viability} is just
 * the worst severity in its condition list.
 */
public enum Severity {

    /**
     * Named and shown, with no mechanical consequence of its own. Deafness and
     * the ocular defects are here: they are real, they are worth telling the
     * player about, and the mod has no sense the horse could lose.
     */
    INFORMATIONAL,

    /**
     * The horse lives, but worse off - <b>fewer hearts</b>, usually alongside a
     * smaller body or a lower jump. This is the mod's whole sub-lethal
     * vocabulary; see {@code wiki/roadmap.html} §6.4.
     */
    IMPAIRING,

    /**
     * The foal <b>is born</b> - it gets a record, a name and a place in the
     * family tree - and then dies within a few seconds. Overo lethal white and
     * the four recessive foal lethals are here.
     */
    LETHAL_AT_BIRTH,

    /**
     * The embryo never develops, so <b>no foal is produced at all</b>. Checked
     * against the genotype the Mendelian draw produced, before anything is
     * spawned.
     */
    LETHAL_AT_CONCEPTION;

    /** Does carrying this condition kill the horse, at either point? */
    public boolean lethal() {
        return this == LETHAL_AT_BIRTH || this == LETHAL_AT_CONCEPTION;
    }
}
