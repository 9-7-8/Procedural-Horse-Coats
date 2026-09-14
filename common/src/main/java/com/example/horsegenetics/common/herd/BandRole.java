package com.example.horsegenetics.common.herd;

/**
 * <b>A horse's place in its band</b>, as a player reads it on the horse
 * information screen. Derived from facts the game module holds - never stored,
 * so it cannot go stale when a stallion is displaced or a colt leaves.
 */
public enum BandRole {

    BAND_STALLION("Band stallion", "Guards and herds the band's mares."),
    LEAD_MARE("Lead mare", "The band goes where she goes."),
    MARE("Band mare", "A member of a family band."),
    YOUNGSTER("Youngster", "Grown, but still with the band it was born into."),
    FOAL("Foal", "Stays close to its mother."),
    BACHELOR_LEAD("Bachelor lead", "Heads a band of stallions without mares."),
    BACHELOR("Bachelor", "A stallion without mares, running with others like him."),
    TAMED("Kept", "A tamed horse - it keeps its friendships, but not a wild band's life."),
    LONER("On its own", "Not part of any band.");

    private final String label;
    private final String description;

    BandRole(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /**
     * The role these facts add up to.
     *
     * @param tamed          the horse is tamed - a kept horse has no wild band role
     * @param inBand         it belongs to a wild band at all
     * @param bachelorBand   that band is a bachelor band
     * @param isLead         it is the band's lead - the stallion of a family band
     * @param male           it is a stallion or colt
     * @param foal           it has not grown up yet
     * @param leadMare       it is the mare the band follows
     * @param natal          it is still in the band it was born into
     */
    public static BandRole of(boolean tamed, boolean inBand, boolean bachelorBand, boolean isLead, boolean male,
                              boolean foal, boolean leadMare, boolean natal) {
        if (tamed) {
            return TAMED;
        }
        if (!inBand) {
            return LONER;
        }
        if (foal) {
            return FOAL;
        }
        if (bachelorBand) {
            return isLead ? BACHELOR_LEAD : BACHELOR;
        }
        if (isLead) {
            return BAND_STALLION;
        }
        if (leadMare) {
            return LEAD_MARE;
        }
        if (natal) {
            return YOUNGSTER;
        }
        return male ? YOUNGSTER : MARE;
    }
}
