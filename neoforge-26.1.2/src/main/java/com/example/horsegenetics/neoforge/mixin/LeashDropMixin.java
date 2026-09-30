package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HorseLeads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A lead vanilla drops on the ground goes to the player who tied it on.</b>
 * The other half of {@code server/HorseLeads} - that one covers the leads
 * <i>this mod</i> causes to be dropped when it teleports a horse; this covers
 * the ones vanilla drops on its own, which is a leash snapping at range, a
 * fence knot broken by hand, or a holder that stopped existing.
 *
 * <h2>Why the seam is here and not on {@code dropLeash}</h2>
 * The obvious target is {@code Leashable.dropLeash}, and it cannot be hit.
 * Both public forms are one-line {@code default} methods delegating to
 * {@code private static void dropLeash(E, boolean, boolean)} <b>on the
 * interface</b>, and an interface's private static method is not a place a
 * mixin can inject. NeoForge 26.1.2 publishes no leash event either - there is
 * no class in its {@code event} package with "leash" in the name, re-checked
 * rather than taken from the note that said so.
 *
 * <p>What is reachable is the line {@code dropLeash} actually spawns the item
 * on: {@code entity.spawnAtLocation(level, Items.LEAD)}. That is an ordinary
 * public method on {@link Entity}, and a search of the whole game finds
 * <b>exactly two</b> callers that pass it a lead, both of them in
 * {@code Leashable} - the drop itself, and the fallback in
 * {@code restoreLeashFromSave} for a leash whose holder no longer exists on
 * load. So one injection covers every path, including the two that reach
 * {@code dropLeash} from completely different directions:
 * {@code tickLeash} for a snap, and a holder failing
 * {@code canInteractWithLevel} for a broken knot.
 *
 * <p><b>That the two callers are the only ones is the claim this rests on</b>,
 * and it is a claim about vanilla, not about this mod. The guard is written to
 * fail safe if it ever stops being true: an {@code AbstractHorse} spawning a
 * lead for some future unrelated reason would have that lead handed to whoever
 * last leashed it rather than dropped, which is a wrong recipient and not a
 * lost item. Another mod calling {@code spawnAtLocation(level, Items.LEAD)} on
 * a horse would be diverted too - the reason
 * {@link HorseLeads#divertLeadDrop} refuses unless a placer was recorded.
 *
 * <p>The identity comparison on {@code resource} is the whole cost on every
 * other item any entity ever drops, which is why the item is tested before the
 * entity.
 *
 * <p><b>UNVERIFIED at runtime.</b> The descriptor is spelled out because
 * {@code spawnAtLocation} has four overloads in 26.1.2 and a bare name would
 * match ambiguously; {@code defaultRequire} is 1, so a signature that stops
 * matching fails the build rather than silently doing nothing.
 */
@Mixin(Entity.class)
public abstract class LeashDropMixin {

    @Inject(
            method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/level/ItemLike;)"
                    + "Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true)
    private void horsegenetics$returnLead(ServerLevel level, ItemLike resource,
                                          CallbackInfoReturnable<ItemEntity> cir) {
        if (resource == Items.LEAD
                && (Object) this instanceof AbstractHorse horse
                && HorseLeads.divertLeadDrop(horse)) {
            // Null is a value this method already returns - it is @Nullable and
            // gives null for an empty stack - so no caller is surprised by one.
            cir.setReturnValue(null);
        }
    }
}
