package com.example.horsegenetics.common.repro;

/**
 * <b>Which kinds of breeding a world allows</b> - one switch per kind, and a
 * master over all of them (owner, 2026-10-24, issue #276: "absolutely ways in the
 * config to disable every single kind of autonomous breeding", each one on its own
 * and all at once, and each switchable while the server runs).
 *
 * <p>The game module fills this from the server config ({@code breeding.*} and
 * {@code fertility.natural_breeding}) and asks {@link #allows} at the one place
 * each kind starts a pregnancy or a foal. common/ never reads config.
 *
 * <p><b>A switch stops new breedings only.</b> A mare already in foal still foals,
 * whichever kind put her in foal (owner, 2026-10-24) - the same rule the
 * natural-breeding switch has always had.
 */
public record BreedingSwitches(boolean master, boolean naturalCover, RealmCover realmCover,
                               boolean stasisStud, boolean goldenCarrot, boolean seedJar) {

    /** {@code breeding.enabled}'s default: today's behaviour, every kind its own switch. */
    public static final boolean DEFAULT_MASTER = true;
    /** {@code breeding.stasis_stud}'s default: on, as before the switch existed. */
    public static final boolean DEFAULT_STASIS_STUD = true;
    /** {@code breeding.golden_carrot}'s default: on, as before the switch existed. */
    public static final boolean DEFAULT_GOLDEN_CARROT = true;
    /** {@code breeding.seed_jar}'s default: on, as before the switch existed. */
    public static final boolean DEFAULT_SEED_JAR = true;
    /** {@code breeding.realm_natural_cover}'s default: the realm does what the Overworld does. */
    public static final RealmCover DEFAULT_REALM_COVER = RealmCover.FOLLOW;

    /**
     * Natural cover in the horse realm. {@code FOLLOW} is what the realm did before it
     * had a switch of its own: whatever {@code fertility.natural_breeding} says.
     */
    public enum RealmCover { FOLLOW, ON, OFF }

    /** Every kind, by the word the config and {@code /horsebreeding} use for it. */
    public enum Kind {
        NATURAL_COVER("natural_cover", "Natural cover (a stallion left with a mare in heat)", true),
        REALM_COVER("realm_cover", "Natural cover in the horse realm", true),
        STASIS_STUD("stasis_stud", "Stud breeding inside a stasis bank", true),
        GOLDEN_CARROT("golden_carrot", "Golden-carrot foals (fed by a player)", false),
        SEED_JAR("seed_jar", "Stallion seed jars (used by a player)", false);

        private final String id;
        private final String label;
        private final boolean autonomous;

        Kind(String id, String label, boolean autonomous) {
            this.id = id;
            this.label = label;
            this.autonomous = autonomous;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        /** Whether it happens with no player acting at that moment. */
        public boolean autonomous() {
            return autonomous;
        }

        /** The kind with this id, or null. */
        public static Kind byId(String id) {
            for (Kind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    public BreedingSwitches {
        if (realmCover == null) {
            realmCover = DEFAULT_REALM_COVER;
        }
    }

    /** Everything at its default - what a world gets before its config loads. */
    public static BreedingSwitches defaults() {
        return new BreedingSwitches(DEFAULT_MASTER, ReproRules.DEFAULT_NATURAL_BREEDING, DEFAULT_REALM_COVER,
                DEFAULT_STASIS_STUD, DEFAULT_GOLDEN_CARROT, DEFAULT_SEED_JAR);
    }

    /** May this kind start a breeding now? Needs the master and its own switch. */
    public boolean allows(Kind kind) {
        return master && own(kind);
    }

    /** The kind's own switch, ignoring the master - what {@code /horsebreeding} reports beside it. */
    public boolean own(Kind kind) {
        switch (kind) {
            case NATURAL_COVER:
                return naturalCover;
            case REALM_COVER:
                return realmCover == RealmCover.FOLLOW ? naturalCover : realmCover == RealmCover.ON;
            case STASIS_STUD:
                return stasisStud;
            case GOLDEN_CARROT:
                return goldenCarrot;
            case SEED_JAR:
                return seedJar;
            default:
                throw new IllegalArgumentException(String.valueOf(kind));
        }
    }

    /** Natural cover where the mare stands: the realm's switch in the realm, the Overworld's elsewhere. */
    public boolean naturalCoverIn(boolean realm) {
        return allows(realm ? Kind.REALM_COVER : Kind.NATURAL_COVER);
    }
}
