package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * <b>Whose horse is it?</b> One answer for everything that writes a horse into
 * an item.
 *
 * <p>Owner, 2026-09-13: <i>"you shouldn't be able to make any kind of bound item
 * to a horse you don't own, even in creative mode."</i> Before that, a stall sign
 * bound to any horse at all and only warned about it, and a seed jar took a
 * sample from any <i>tamed</i> stallion, whoever had tamed it. The transfer paper
 * already refused and keeps its own check, which asks the same question.
 *
 * <p><b>No creative exception, deliberately.</b> Creative is for skipping the
 * cost of things, not for acting on horses that are somebody else's - and in the
 * test yard, where the tester is always in creative, an exception would mean the
 * rule never ran at all.
 */
public final class HorseOwnership {

    private HorseOwnership() {
    }

    /** Tamed, and tamed by {@code playerId}. */
    public static boolean isOwner(Horse horse, UUID playerId) {
        if (!horse.isTamed()) {
            return false;
        }
        var owner = horse.getOwnerReference();
        return owner != null && playerId.equals(owner.getUUID());
    }

    /**
     * Why {@code player} may not bind an item to this horse, in words that say
     * what to do about it - or {@code null} when they may.
     */
    public static @Nullable String bindRefusal(Horse horse, Player player, String horseName) {
        if (!horse.isTamed()) {
            return horseName + " is not tamed yet - tame it before binding anything to it.";
        }
        if (!isOwner(horse, player.getUUID())) {
            return horseName + " is not your horse, so nothing can be bound to it.";
        }
        return null;
    }

    /**
     * <b>The owner's name, online or not</b> - known gap 228. Loaded players are
     * read directly. An absent owner goes through the server's name cache
     * ({@code usercache.json}, a local lookup), then NeoForge's
     * {@code UsernameCache}, filled at every login. Never a network call.
     * Empty for an untamed horse, or an owner neither cache has seen.
     *
     * <p>API checked against the 26.1.2 sources, not yet exercised in-game.
     */
    public static java.util.Optional<String> ownerName(Horse horse) {
        if (!horse.isTamed()) {
            return java.util.Optional.empty();
        }
        if (horse.getOwner() instanceof Player online) {
            return java.util.Optional.of(online.getGameProfile().name());
        }
        var owner = horse.getOwnerReference();
        if (owner == null || !(horse.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return java.util.Optional.empty();
        }
        UUID id = owner.getUUID();
        java.util.Optional<String> cached = level.getServer().services().nameToIdCache().get(id)
                .map(net.minecraft.server.players.NameAndId::name);
        if (cached.isPresent()) {
            return cached;
        }
        return java.util.Optional.ofNullable(net.neoforged.neoforge.common.UsernameCache.getLastKnownUsername(id));
    }
}
