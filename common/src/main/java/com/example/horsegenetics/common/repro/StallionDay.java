package com.example.horsegenetics.common.repro;

/**
 * <b>A stallion's day so far: the covers he has made, and how many this world
 * gives him before his odds drop.</b>
 *
 * <p>The free count travels with the count rather than being read from a
 * constant, because it is a <b>server setting</b> ({@code fertility.free_covers_per_day},
 * default {@link ReproRules#DEFAULT_FREE_COVERS_PER_DAY}) and {@code common/}
 * cannot see a config - the same shape as {@link NaturalCover.Crowd}. Every rule
 * that asks "is he tired?" asks this record, so the halved odds, the mare's
 * preference for a rested stallion and the vet's sentence can never disagree
 * about the number.
 *
 * @param covers covers (or jar fills) he has already made today, before this one
 * @param free   how many this world allows before {@link ReproRules#TIRED_STALLION_FACTOR}
 */
public record StallionDay(int covers, int free) {

    /** This many covers today, on the default allowance. */
    public static StallionDay of(int covers) {
        return new StallionDay(covers, ReproRules.DEFAULT_FREE_COVERS_PER_DAY);
    }

    /**
     * Past his free covers. The split is {@code >=}: a stallion who has made his
     * third cover is already on the tired factor for his fourth.
     */
    public boolean tired() {
        return covers >= free;
    }

    /** His multiplier on the conception odds. */
    public double factor() {
        return tired() ? ReproRules.TIRED_STALLION_FACTOR : 1.0;
    }
}
