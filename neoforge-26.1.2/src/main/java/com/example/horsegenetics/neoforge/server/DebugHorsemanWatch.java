package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev-only: say in chat, once, when a villager becomes the <b>horseman</b>.
 *
 * <h2>Why it is a poll and not an event</h2>
 * Taking a job is not something NeoForge fires an event for. It happens deep in
 * vanilla's brain, when a villager's {@code AcquirePoi} behaviour claims a
 * ticket on a job-site POI, and the only outward sign is that his
 * {@code VillagerData} now names a profession. So this watches for that: one
 * profession comparison per villager tick, behind a {@link
 * DebugAnnounce#enabled()} check that is false in a real build.
 *
 * <h2>Why anyone wants it</h2>
 * The horseman is the harder of the two villagers to test, because he does not
 * generate - he is made by putting a Horse Trader's Post next to an unemployed
 * villager and waiting. When nothing happens there are three possible reasons
 * (the POI did not register, the {@code acquirable_job_site} tag did not merge,
 * or the villager simply has not got round to it yet) and no way to tell them
 * apart from the outside. A line in chat separates "he took it" from all three
 * at once, and carries the coordinates so you can walk back to him.
 *
 * <p>One line per villager per session: the set is never cleared, so a horseman
 * who loses his post and takes it again says nothing the second time. That is
 * the right trade for a line whose job is "did this ever work".
 */
@EventBusSubscriber
public final class DebugHorsemanWatch {

    private static final Set<UUID> ANNOUNCED = ConcurrentHashMap.newKeySet();

    private DebugHorsemanWatch() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!DebugAnnounce.enabled() || !(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }
        if (!villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
            return;
        }
        if (!ANNOUNCED.add(villager.getUUID())) {
            return;
        }
        DebugAnnounce.sayAt(level, "Horseman",
                villager.getName().getString() + " took the job",
                villager.blockPosition(), ChatFormatting.AQUA);
    }
}
