package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.neoforged.fml.ModList;

import java.util.UUID;

/**
 * <b>Is this player on the horse owner's team, or allied to it?</b> One
 * question, asked by {@code server/HorseRiding} and nothing else.
 *
 * <h2>Why FTB Teams and not FTB Chunks</h2>
 * The ask named FTB Chunks, which is the mod a player actually sees - the map,
 * the claims, the "invite to my team" button. But a chunk claim is not a
 * relationship between two players; it is a relationship between a player and
 * some ground. The relationship is kept one mod down, in <b>FTB Teams</b>,
 * which is what FTB Chunks builds its claims out of and what its team screen is
 * really editing. So the question is asked there, and a server running FTB
 * Chunks has FTB Teams by definition - it is a hard dependency.
 *
 * <h2>What counts</h2>
 * <ul>
 *   <li><b>Same team.</b> {@code TeamManager.arePlayersInSameTeam(UUID, UUID)}.
 *       Note that FTB gives every player a one-person team of their own, so
 *       this is <i>not</i> trivially true for everybody - two strangers are in
 *       two different teams and it answers false.</li>
 *   <li><b>An ally of the owner's team.</b> {@code Team.getRankForPlayer} on
 *       the owner's team, {@code ALLY} or better. That is the "allyships" half
 *       of the ask: a neighbour you have allied but not recruited.</li>
 * </ul>
 *
 * <h2>Why touching this mod's classes is allowed</h2>
 * The same deliberate exception {@link JadeHorsePlugin} and
 * {@link WaystonesCompat} already make, and for the same reason
 * {@link HorsePoweredCompat} refuses to: {@code dev.ftb.mods.ftbteams.api} is a
 * package FTB publishes, versions and ships a {@code package-info} for. It is
 * <b>compile-only</b> - the jar is never shipped - and every name lives in
 * {@link Hook} rather than here, so that with FTB Teams absent nothing in the
 * {@code dev.ftb} packages is ever loaded or verified. The guard in
 * {@link #present()} runs first and the inner class is never touched.
 *
 * <p>Both calls take <b>UUIDs</b> rather than a {@code ServerPlayer}, which is
 * the reason this can be asked at all: the horse's owner is usually not online,
 * and an API that needed the owner's entity could only ever answer for a horse
 * whose owner was stood next to it.
 *
 * <p><b>Not verified in-game.</b> Nothing here has been seen running - it needs
 * a client with FTB Teams installed, which the dev instance is not. See
 * {@code wiki/compatibility.html}.
 */
public final class FtbTeamsCompat {

    private static final String FTB_TEAMS = "ftbteams";

    /**
     * Set the first time {@link #allied} is asked and the API throws. One
     * warning, not one per mount attempt - a horse refusing a teammate every
     * second is loud enough on its own without the log filling up too.
     */
    private static boolean warnedOff;

    private FtbTeamsCompat() {
    }

    /** Is FTB Teams installed at all? Cheap, and touches none of its classes. */
    public static boolean present() {
        return ModList.get().isLoaded(FTB_TEAMS);
    }

    /**
     * One line at startup saying which way this went. It registers nothing -
     * there is nothing to register, since the API is only ever <i>asked</i> - but
     * a horse refusing somebody's teammate is a bug report, and the first thing
     * that report needs is whether this half of the rule was even in play.
     */
    public static void announce() {
        if (present()) {
            HorseGenetics.LOGGER.info("FTB Teams found - a horse owner's team and allies "
                    + "may ride their horses (behaviour.riding_allows_teams).");
        }
    }

    /**
     * <b>Are these two on the same FTB team, or allied?</b> {@code false} when
     * FTB Teams is absent, when its manager has not loaded yet (a single-player
     * world mid-load), or when the API moved - never an exception, because the
     * caller is a mount attempt and the honest answer to "I cannot tell" is the
     * one that leaves the horse's owner in charge of it.
     *
     * @param owner the horse's owner
     * @param rider whoever is trying to get on
     */
    public static boolean allied(UUID owner, UUID rider) {
        if (owner.equals(rider) || !present()) {
            return false;
        }
        try {
            return Hook.allied(owner, rider);
        } catch (LinkageError | RuntimeException wrongVersion) {
            if (!warnedOff) {
                warnedOff = true;
                // Loud once, because the failure it otherwise produces is a
                // teammate being thrown off a horse they have ridden for weeks,
                // and nobody would connect that to an FTB update.
                HorseGenetics.LOGGER.warn("FTB Teams is installed but its team API did not match "
                        + "what this build was compiled against, so teammates will NOT be allowed "
                        + "to ride each other's horses. Set behaviour.owner_only_riding = false "
                        + "to turn the rule off entirely.", wrongVersion);
            }
            return false;
        }
    }

    /**
     * Everything that names an FTB type, kept apart from the guard above so the
     * guard can run without loading any of it.
     */
    private static final class Hook {

        static boolean allied(UUID owner, UUID rider) {
            dev.ftb.mods.ftbteams.api.FTBTeamsAPI.API api =
                    dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
            if (api == null || !api.isManagerLoaded()) {
                return false;
            }
            dev.ftb.mods.ftbteams.api.TeamManager manager = api.getManager();
            if (manager.arePlayersInSameTeam(owner, rider)) {
                return true;
            }
            // Allies are a property of the OWNER's team, not the rider's: it is
            // the owner who decided to trust them, and the two teams need not
            // have allied each other back.
            return manager.getTeamForPlayerID(owner)
                    .map(team -> team.getRankForPlayer(rider).isAllyOrBetter())
                    .orElse(false);
        }

        private Hook() {
        }
    }
}
