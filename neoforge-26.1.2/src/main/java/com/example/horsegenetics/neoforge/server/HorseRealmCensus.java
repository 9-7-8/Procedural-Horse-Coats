package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * <b>Counting the horses in the realm, and growing the field when there are too
 * many for it.</b> The census half is a per-horse tick; the growth half is one
 * check on a slow clock.
 *
 * <h2>Counted by the horses themselves</h2>
 * A horse ticking <b>in</b> the realm puts its own id in the census; a horse
 * ticking <b>anywhere else</b> takes it out. That is the same shape as
 * {@link HorseRealmFeral}, and for the same reason: there is no single event
 * meaning <i>this horse has left the realm</i>. It can walk out through the
 * portal, be led out, be carried out in somebody's arms, be teleported out by a
 * command, or simply be in a chunk nobody has loaded since the last restart. A
 * condition covers all of those; a hook covers whichever ones somebody
 * remembered.
 *
 * <p><b>A horse in an unloaded chunk keeps its place in the count</b>, which is
 * the behaviour that matters most: most of the field is unloaded most of the
 * time, and a census that only counted what it could see would shrink every time
 * a player logged off and the field would be sized for nobody.
 *
 * <h2>What it costs</h2>
 * Two field reads per horse per tick in the whole game - is it a horse, is this
 * its turn - and past that only in the realm. The set write is once per horse
 * per visit, not once per tick: {@code note} returns false when it already knew,
 * and only a change marks the data dirty.
 *
 * <h2>Growing</h2>
 * Every {@value #GROW_INTERVAL} ticks the field is asked whether the census has
 * outgrown it. The answer is almost always no and costs one multiply and one
 * comparison; when it is yes, the old wall is retired and
 * {@link HorseRealmTerrain} takes it down as those chunks load.
 *
 * <p>The clock is slow on purpose. A field that grew the instant a foal was born
 * would move its own wall while somebody was standing at it, and nothing about
 * this is urgent - a field that is the right size ten seconds late is the right
 * size.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseRealmCensus {

    /** Once every five seconds per horse, staggered by entity id. */
    private static final int SCAN = 100;

    /** How often the field asks whether it has been outgrown - ten seconds. */
    private static final int GROW_INTERVAL = 200;

    private HorseRealmCensus() {
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !horse.isAlive()) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        HorseRealmSize size = HorseRealmSize.get(server);
        if (HorseRealm.isRealm(level)) {
            size.note(horse.getUUID());
        } else {
            size.forget(horse.getUUID());
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % GROW_INTERVAL != 0) {
            return;
        }
        // Only if the realm exists at all. A world whose datapack has been
        // removed should not be growing a field nobody can reach.
        if (server.getLevel(HorseRealm.REALM_LEVEL) == null) {
            return;
        }
        int was = HorseRealmSize.get(server).growToFit();
        if (was >= 0) {
            ActionTrace.log("realm", "the field outgrew radius " + was + " and is now "
                    + HorseRealmSize.get(server).radius() + " blocks across its middle");
        }
    }
}
