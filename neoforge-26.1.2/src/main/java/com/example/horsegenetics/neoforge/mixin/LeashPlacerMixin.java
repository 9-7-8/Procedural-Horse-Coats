package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HorseLeads;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>Writes down who tied the lead on</b>, so that
 * {@link LeashDropMixin} has somebody to give it back to.
 *
 * <h2>Why this has to be recorded at all</h2>
 * A lead tied to a fence has <i>no player in it anywhere</i>. The holder is a
 * {@code LeashFenceKnotEntity}, and by the time vanilla drops the item
 * {@code Leashable.dropLeash} has already run {@code entity.setLeashData(null)}
 * - so even the knot is gone. The only moment the answer exists is when the
 * lead goes on, which is here.
 *
 * <h2>Why {@code setLeashData} is the right moment, and the only one</h2>
 * {@code Leashable.setLeashedTo} calls {@code setLeashData} only when the horse
 * had no leash data at all; when it already has some it takes a
 * {@code leashData.setLeashHolder(holder)} branch instead and never reaches
 * here. That looks like a gap and is the opposite: tying a horse to a fence is
 * always a <i>second</i> attachment - {@code LeadItem.bindPlayerMobs} only
 * considers animals whose holder is already you - so the knot transition is
 * exactly the one that must not overwrite the player recorded a moment earlier.
 * Vanilla's own control flow does that for us.
 *
 * <p>{@code setLeashData} is declared on the {@code Leashable} <i>interface</i>
 * but overridden concretely on {@link Mob}, which is what makes it mixinable at
 * all - the interface's own leash plumbing is {@code private static} and out of
 * reach. See {@link LeashDropMixin} for the same problem solved the other way.
 *
 * <p>Restricted to an {@link AbstractHorse}: this mod's business is horses, and
 * a cow's lead is vanilla's affair. {@code AbstractHorse} rather than
 * {@code Horse} because a donkey or a mule is just as annoying to lose a lead
 * from, and nothing here needs a horse record.
 *
 * <p><b>UNVERIFIED at runtime.</b> Written against the 26.1.2 sources, where
 * {@code Mob.setLeashData} is {@code public void} and the only override on the
 * path. The interesting half is not this injection but the control-flow claim
 * above, which {@code vanilla_lead_comes_back} pins.
 */
@Mixin(Mob.class)
public abstract class LeashPlacerMixin {

    @Inject(method = "setLeashData", at = @At("HEAD"))
    private void horsegenetics$rememberPlacer(Leashable.@org.jetbrains.annotations.Nullable LeashData leashData,
                                              CallbackInfo ci) {
        if ((Object) this instanceof AbstractHorse horse) {
            HorseLeads.rememberPlacer(horse, leashData);
        }
    }
}
