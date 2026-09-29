package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.compat.FtbTeamsCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * <b>Whose horse is it to ride?</b> One answer, and the two places that enforce
 * it.
 *
 * <p>Owner, 2026-09-29: <i>"people should not be able to ride a horse they don't
 * own, it should buck them off with a warning about 'that horse doesn't know
 * you', but add in support for teams (like through ftb chunks, which creates
 * teams) or allyships, people that are not the owner who are allowed to ride the
 * horse."</i>
 *
 * <h2>What vanilla does, and why it is not enough</h2>
 * Vanilla has no rule at all: a tamed horse is a saddle anybody can sit in, and
 * ownership governs nothing except whether a lead works and who the horse
 * follows. On a single-player world that costs nothing. On a server it is the
 * difference between a stable and a car park - the horse somebody spent four
 * generations breeding is a vehicle the next person to walk past can ride into
 * a ravine, and this mod's horses are worth a great deal more than a vanilla
 * one.
 *
 * <h2>Who may ride</h2>
 * In the order they are asked, and the first yes wins:
 * <ol>
 *   <li><b>Anybody</b>, if {@code behaviour.owner_only_riding} is off. The
 *       whole rule is one flag, and off it is exactly vanilla.</li>
 *   <li><b>Anybody</b>, on an <b>untamed</b> horse. Climbing on until it stops
 *       bucking is how a horse is tamed, so a rule that refused an untamed
 *       horse would make horses untameable. This is not a loophole: a horse
 *       with no owner has nobody to protect.</li>
 *   <li><b>The owner.</b></li>
 *   <li><b>The owner's vanilla scoreboard team</b>, when
 *       {@code behaviour.riding_allows_teams} is on. Free, works with no other
 *       mod installed, and is what {@code /team} has always meant.</li>
 *   <li><b>The owner's FTB team, or an ally of it</b>, same flag. See
 *       {@link FtbTeamsCompat} for why the question is asked of FTB Teams and
 *       not of FTB Chunks.</li>
 * </ol>
 *
 * <p><b>No operator or creative exception</b>, deliberately, and for the reason
 * {@link HorseOwnership} gives about binding items: creative is for skipping
 * the cost of things, not for acting on horses that are somebody else's, and in
 * the test yard - where the tester is always in creative - an exception would
 * mean the rule never ran at all. An operator who genuinely needs to move
 * somebody's horse has {@code /horsegive} ({@link HorseGiveCommand}), which
 * says in the log who did it.
 *
 * <h2>Two enforcement points, for two different failures</h2>
 * <ul>
 *   <li>{@link #onMount} refuses the mount as it happens. This is the one that
 *       normally fires, and cancelling is cheaper and safer than letting
 *       somebody sit on a flying horse for a tick first.</li>
 *   <li>{@link #onHorseTick} throws off a rider who is <i>already</i> up. That
 *       is not the same case: a horse can change hands under its rider
 *       ({@code /horsegive}, a redeemed transfer paper), a team can be
 *       disbanded, and an operator can edit the config while somebody is in the
 *       saddle. Without this, any of those leaves a stranger riding on for
 *       ever.</li>
 * </ul>
 * Both rear the horse - {@code makeMad}, which plays the angry sound itself -
 * so the refusal is something you see rather than only a line of text.
 *
 * <p><b>Every {@code AbstractHorse}</b>, not only this mod's: a donkey, a mule
 * and a camel are owned the same way and would read very oddly as the one
 * animal in the stable anybody could take.
 *
 * <p><b>Not verified in-game.</b> The team half in particular needs a second
 * player, which no single-player check can supply.
 */
@EventBusSubscriber
public final class HorseRiding {

    /**
     * How often the already-mounted sweep actually looks, in ticks. Staggered
     * by entity id like {@link HorseCareHandler}'s, so a paddock of horses does
     * not all check on the same tick.
     */
    private static final int SWEEP_INTERVAL = 20;

    private HorseRiding() {
    }

    // ------------------------------------------------------------------
    // The question
    // ------------------------------------------------------------------

    /**
     * <b>May this player ride this horse?</b> The only place the rule is
     * written; {@link #onMount}, {@link #onHorseTick} and the Ride button on
     * the information screen all ask it here.
     */
    public static boolean mayRide(AbstractHorse horse, Player player) {
        if (!ServerConfig.ownerOnlyRiding()) {
            return true;
        }
        if (!horse.isTamed()) {
            return true;
        }
        var ownerRef = horse.getOwnerReference();
        UUID owner = ownerRef == null ? null : ownerRef.getUUID();
        if (owner == null || owner.equals(player.getUUID())) {
            return true;
        }
        if (!ServerConfig.ridingAllowsTeams()) {
            return false;
        }
        return sameScoreboardTeam(horse, player, owner)
                || FtbTeamsCompat.allied(owner, player.getUUID());
    }

    /**
     * <b>Vanilla's own teams.</b> A scoreboard team keys its members on the
     * player's <i>name</i> rather than their id, so an offline owner is looked
     * up through the same name caches {@link HorseOwnership#ownerName} uses -
     * which is why this takes the horse and not just two ids.
     *
     * <p>An owner nobody can name answers false rather than true. A horse whose
     * owner has never been seen on this server is precisely the horse that
     * should not be handed to a stranger on a guess.
     */
    private static boolean sameScoreboardTeam(AbstractHorse horse, Player player, UUID owner) {
        PlayerTeam mine = player.getTeam() instanceof PlayerTeam team ? team : null;
        if (mine == null) {
            return false;
        }
        // The owner online is the cheap case and needs no name lookup at all.
        if (horse.getOwner() instanceof Player online && online.getUUID().equals(owner)) {
            return mine.equals(online.getTeam());
        }
        String name = HorseOwnership.ownerName(horse).orElse("");
        if (name.isBlank()) {
            return false;
        }
        Scoreboard scoreboard = horse.level().getScoreboard();
        return mine.equals(scoreboard.getPlayersTeam(name));
    }

    // ------------------------------------------------------------------
    // Refusing the mount
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide() || !event.isMounting()) {
            return;
        }
        if (!(event.getEntityMounting() instanceof Player player)
                || !(event.getEntityBeingMounted() instanceof AbstractHorse horse)) {
            return;
        }
        if (mayRide(horse, player)) {
            return;
        }
        event.setCanceled(true);
        refuse(horse, player);
    }

    // ------------------------------------------------------------------
    // Throwing off somebody already up
    // ------------------------------------------------------------------

    /**
     * The horse changed hands, or the team did, under a rider. Guarded on
     * {@code isVehicle()} first so that the overwhelming majority of horse
     * ticks - every horse in every paddock, nobody on it - cost one boolean.
     */
    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !horse.isVehicle()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SWEEP_INTERVAL != 0) {
            return;
        }
        // A copy, because stopRiding mutates the passenger list underneath us.
        List<Entity> riders = List.copyOf(horse.getPassengers());
        for (Entity rider : riders) {
            if (rider instanceof Player player && !mayRide(horse, player)) {
                player.stopRiding();
                refuse(horse, player);
            }
        }
    }

    // ------------------------------------------------------------------

    /**
     * The buck, and the warning. Public because {@code ModNetworking}'s Ride
     * button makes the same refusal without an {@link EntityMountEvent} to
     * cancel - a button that walked past this would be a way to ride anything
     * on the server from the information screen.
     */
    public static void refuse(AbstractHorse horse, Player player) {
        // Already reared (somebody else just tried, or it is mid-rear): the
        // angry sound on its own, since makeMad on a standing horse does
        // nothing and the refusal would then be silent.
        if (horse.isStanding()) {
            horse.level().playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                    net.minecraft.sounds.SoundEvents.HORSE_ANGRY,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else {
            horse.makeMad();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.horsegenetics.riding.not_yours",
                    Component.literal(nameOf(horse))), true);
        }
    }

    /**
     * What to call the animal in the refusal. Its own name if this mod knows
     * one, so the line reads "Pepper doesn't know you" - the point of the
     * message is that the horse is somebody's, and a name is the shortest way
     * to say so.
     */
    private static String nameOf(AbstractHorse horse) {
        if (horse instanceof net.minecraft.world.entity.animal.equine.Horse known
                && HorseRecords.hasRealRecord(known)) {
            return HorseRecords.of(known).displayName();
        }
        return horse.getName().getString();
    }
}
