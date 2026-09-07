package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Lets the cowboy's outfit walk through each other, for as long as it takes to
 * get through a doorway.
 *
 * <h2>The problem</h2>
 * Eleven horses and a two-block gap. Mobs in Minecraft do not block each other,
 * they <b>shove</b> each other - and a shove is exactly the wrong response to
 * "the animal in front of me is also trying to get through this door". The
 * string arrives at the barn together, jams in the entrance, and shoulders each
 * other back out into the field; the logs showed a cowboy stuck four blocks
 * short of his own barn with a clear path and nothing in his way but his own
 * horses.
 *
 * <h2>The trick</h2>
 * A <b>scoreboard team</b> with {@link Team.CollisionRule#NEVER}. That rule is
 * checked by {@code EntitySelector.pushableBy} at both ends - a member pushes
 * nothing and nothing pushes a member - so putting the cowboy and his horses on
 * it turns the jam into a queue that walks through itself. No mixin, no
 * subclass, and nothing that touches <b>block</b> collision: they still cannot
 * walk through the wall, only through each other.
 *
 * <p>It is on for one part of one night: from the moment the cowboy is home
 * with the doors open behind him until the moment they shut. Outside that
 * window the herd shoves like anything else, because a barn full of horses
 * standing inside one another is a worse picture than a jammed doorway.
 *
 * <p>The team is created on demand and never removed. It carries no colour, no
 * prefix and no other rule, so the only thing it does to a horse is this - and
 * an empty leftover team in a save is not worth a cleanup path.
 */
public final class HerdCollision {

    /** Scoreboard team the outfit joins while it is filing into the barn. */
    private static final String TEAM = "horsegenetics_barn_door";

    private HerdCollision() {
    }

    /** Let the outfit walk through each other. Idempotent - safe to call every tick. */
    public static void off(ServerLevel level, Cowboy cowboy) {
        PlayerTeam team = team(level);
        join(level, team, cowboy);
        for (Horse horse : cowboy.liveHerd(level)) {
            join(level, team, horse);
        }
    }

    /** Shoving as normal again. Also idempotent, and called from every duty that is not sheltering. */
    public static void on(ServerLevel level, Cowboy cowboy) {
        PlayerTeam team = level.getScoreboard().getPlayerTeam(TEAM);
        if (team == null) {
            return;
        }
        leave(level, team, cowboy);
        for (Horse horse : cowboy.liveHerd(level)) {
            leave(level, team, horse);
        }
    }

    private static PlayerTeam team(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM);
            team.setCollisionRule(Team.CollisionRule.NEVER);
        }
        return team;
    }

    private static void join(ServerLevel level, PlayerTeam team, Entity entity) {
        if (entity.getTeam() != team) {
            level.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), team);
        }
    }

    private static void leave(ServerLevel level, PlayerTeam team, Entity entity) {
        if (entity.getTeam() == team) {
            level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
        }
    }
}
