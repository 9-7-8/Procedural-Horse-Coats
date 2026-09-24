package com.example.horsegenetics.common.care;

/**
 * <b>The line the whole server reads when somebody's horse dies.</b>
 *
 * <p>Owner's ask: <i>"When an owned horse dies, post a server-wide death
 * message. This should be configurable and on by default."</i> The switch is
 * {@code notices.owned_horse_death}.
 *
 * <p>Here rather than in the game module for {@link HurtNotice}'s reason - the
 * wording is the feature, so a test should be able to pin it down. Nothing in
 * here reads a horse, a level or a damage source.
 *
 * <h2>Why it is not vanilla's death message</h2>
 * Vanilla writes one already ({@code CombatTracker.getDeathMessage()}), and a
 * horse never sees it: {@code AbstractHorse} extends {@code Animal}, not
 * {@code TamableAnimal}, so the block in {@code TamableAnimal.die} that tells an
 * owner their pet died never runs for a horse. Vanilla's sentence also names the
 * entity, and a horse of this mod carries its name in its record rather than on
 * a name tag - so it would broadcast "Horse was slain by Zombie" about a mare
 * with a name, a pedigree and an owner. All three of those are the point of the
 * message, so the sentence is written here instead.
 *
 * <h2>Whose it was, in the middle</h2>
 * The horse is named first because that is what the owner scans for, and the
 * owner second because that is what everybody else scans for. The cause reuses
 * {@link HurtNotice.Cause}: one table of vanilla damage ids, so a horse that
 * burns to death says the same word as a horse that was burning a moment ago.
 * The advice on a {@code Cause} is deliberately <em>not</em> used - there is
 * nothing to do about it now, and a dead horse is not the moment to be told to
 * fence the drops.
 */
public final class DeathNotice {

    private DeathNotice() {
    }

    /** What to call an owner the server's name caches have never seen. */
    public static final String SOMEBODY = "somebody";

    /**
     * <b>The line.</b> {@code killer} names the entity that dealt the killing
     * blow when the game module knows it ("a zombie", or a player's name) and is
     * empty otherwise, in which case the cause speaks for itself.
     *
     * <p>{@code ownerName} is empty for an owner neither name cache can resolve;
     * the sentence keeps its shape and says {@link #SOMEBODY}, because "whose
     * horse" is half of why this line is server-wide at all.
     */
    public static String line(String horseName, String ownerName, HurtNotice.Cause cause, String killer) {
        String owner = ownerName == null || ownerName.isEmpty() ? SOMEBODY : ownerName;
        StringBuilder out = new StringBuilder();
        out.append(horseName).append(", ").append(owner).append("'s horse, ");
        if (cause == HurtNotice.Cause.GENETIC_DEFECT) {
            // Matches the mod's own damage type, which already says this in
            // lang: a lethal genotype is not something that killed the foal, it
            // is something the foal was born with.
            return out.append("did not survive a genetic defect.").toString();
        }
        String by = killer == null || killer.isEmpty() ? cause.phrase() : killer;
        return out.append("was killed by ").append(by).append('.').toString();
    }
}
