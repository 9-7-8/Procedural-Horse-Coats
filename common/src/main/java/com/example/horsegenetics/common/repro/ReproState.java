package com.example.horsegenetics.common.repro;

/**
 * Where a mare is in her reproductive life at one moment. One at a time, in
 * priority order - {@link ReproRules#stateAt} returns the first that applies.
 * Nursing is not a state: it runs beside any of these.
 *
 * <p>Stallions have no state; they are asked nothing but how many covers they
 * have made today.
 */
public enum ReproState {

    /** Carrying. Cannot conceive, refuses golden carrots. */
    PREGNANT("Pregnant"),
    /** Just foaled; foal heat has not begun. Cannot conceive. */
    POSTPARTUM("Recently foaled"),
    /** The first heat after birth. Can conceive, at slightly lower odds. */
    FOAL_HEAT("In foal heat"),
    /** In heat. Can conceive, best in the second half. */
    ESTRUS("In heat"),
    /** Between heats. Cannot conceive. */
    DIESTRUS("Not in heat");

    private final String label;

    ReproState(String label) {
        this.label = label;
    }

    /** Does a breeding carrot, seed jar or spontaneous pairing have any chance now? */
    public boolean receptive() {
        return this == ESTRUS || this == FOAL_HEAT;
    }

    public String label() {
        return label;
    }
}
