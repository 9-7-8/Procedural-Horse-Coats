package com.example.horsegenetics.common.realm;

/**
 * <b>What a realm restore does to one horse from the backup.</b>
 *
 * <p>A realm backup is taken before anything changes the horse realm in a way
 * that could lose horses (owner, 2026-10-01). Restoring it brings back what
 * the change lost and leaves everything else alone; this is the rule that tells
 * the two apart, one horse at a time. The NeoForge side gathers the facts - it
 * is the only side that can see a world - and asks here what they mean.
 *
 * <p>Every horse in a backup was alive in the realm when it was taken, which is
 * what makes a death mark sufficient evidence on its own: dead now means died
 * since. The order of the tests is the rule:
 * <ol>
 *   <li><b>Standing somewhere loaded.</b> Alive in the realm: check it is not
 *       stuck. Alive anywhere else: somebody took it out; leave it.</li>
 *   <li><b>In a stasis chamber.</b> Somebody bottled it; leave it.</li>
 *   <li><b>Last seen outside the realm.</b> It left, alive or dead, and whatever
 *       happened to it out there is not the realm change's doing; leave it.</li>
 *   <li><b>Dead</b> (and last seen in the realm): raise it.</li>
 *   <li><b>Still in the realm's entity files</b>: alive and unloaded; check it is
 *       not stuck the next time it loads.</li>
 *   <li><b>None of those</b>: it is in no world, no chamber and no file, with no
 *       record of leaving - lost without dying. Raise it.</li>
 * </ol>
 */
public final class RealmRestore {

    private RealmRestore() {
    }

    /** What happens to one backed-up horse. */
    public enum Action {
        /** Dead or lost since the backup: bring it back from the backup's copy. */
        RAISE,
        /** Alive and loaded in the realm: move it to the surface if it is stuck. */
        RESCUE,
        /** Alive in the realm but unloaded: the same check, when it next loads. */
        RESCUE_LATER,
        /** Not the change's to undo: out of the realm, or in a chamber. */
        LEAVE
    }

    /**
     * The facts about one horse, as the world has them now.
     *
     * @param loaded          a living entity with this id is loaded in some level
     * @param loadedInRealm   ...and that level is the realm
     * @param inStasis        the whereabouts record says it is in a chamber
     * @param dead            the whereabouts record says it died
     * @param lastSeenInRealm the whereabouts record places it in the realm, or
     *                        there is no record at all
     * @param inRealmFiles    the realm's current entity files hold it
     */
    public record Facts(boolean loaded, boolean loadedInRealm, boolean inStasis, boolean dead,
                        boolean lastSeenInRealm, boolean inRealmFiles) {
    }

    public static Action decide(Facts f) {
        if (f.loaded()) {
            return f.loadedInRealm() ? Action.RESCUE : Action.LEAVE;
        }
        if (f.inStasis() || !f.lastSeenInRealm()) {
            return Action.LEAVE;
        }
        if (f.dead()) {
            return Action.RAISE;
        }
        return f.inRealmFiles() ? Action.RESCUE_LATER : Action.RAISE;
    }
}
