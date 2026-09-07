package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names the <b>horseman</b> when a villager takes the job: a first name of his
 * own, and the family name of the nearest cowboy in town.
 *
 * <h2>Why the nearest cowboy, and not the one who owns the barn</h2>
 * Because the horseman is not the barn's property. The post beside the cowboy's
 * barn is the one a generated village comes with, but a player can craft
 * another and put it anywhere, and a village that has a cowboy in it should read
 * as a village where <em>that family</em> deals in horses wherever the counter
 * happens to be. So the question asked is "is there a cowboy in this town?" and
 * the answer is taken from whichever one is closest - which for the barn's own
 * post is the barn's own cowboy, and for a post the player raised across the
 * square is still the man everyone there buys horses from.
 *
 * <p>No cowboy within {@link #TOWN} blocks and he keeps the plain profession
 * name. A horseman in a village with no horse family is nobody's relative.
 *
 * <h2>Why a poll and not an event</h2>
 * Taking a job is not something NeoForge fires an event for. It happens deep in
 * vanilla's brain, when a villager's {@code AcquirePoi} behaviour claims a
 * ticket on a job-site POI, and the only outward sign is that his
 * {@code VillagerData} now names a profession. So this watches for that: one
 * profession comparison per villager tick.
 *
 * <p>That also makes it the cheapest possible answer to the hardest thing about
 * the horseman to test. He does not generate ready-made - he exists when a post
 * is placed near an unemployed villager and he gets round to claiming it - and
 * when nothing happens there are three indistinguishable reasons (the POI did
 * not register, the {@code acquirable_job_site} tag did not merge, or he simply
 * has not yet). The line this prints separates the third from the first two.
 */
@EventBusSubscriber
public final class HorsemanHandler {

    /** How far a cowboy can be and still count as "in this town". */
    private static final int TOWN = 128;

    /**
     * Villagers already dealt with. One pass per villager per session: a
     * horseman who loses his post and takes it again keeps the name he has, and
     * one who was named before a reload is skipped because he has a name.
     */
    private static final Set<UUID> SEEN = ConcurrentHashMap.newKeySet();

    private static final PersonNameGenerator NAMES = PersonNameGenerator.cowboys();

    private HorsemanHandler() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        if (!villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
            return;
        }
        if (!SEEN.add(villager.getUUID())) {
            return;
        }
        DebugAnnounce.sayAt(level, "Horseman", villager.getName().getString() + " took the job",
                villager.blockPosition(), ChatFormatting.AQUA);

        if (villager.getCustomName() != null) {
            return; // named already, on an earlier login
        }
        Cowboy family = nearestCowboy(level, villager);
        if (family == null) {
            return; // no horse family here; he stays "Horseman"
        }
        Rng rng = new NeoRng(villager.getRandom());
        villager.setCustomName(Component.literal(
                NAMES.generateParts(rng).first() + " " + family.lastName()));
        villager.setCustomNameVisible(true);
        DebugAnnounce.sayAt(level, "Horseman",
                villager.getName().getString() + " takes the name of " + family.cowboyName(),
                villager.blockPosition(), ChatFormatting.AQUA);
    }

    /** The closest founded cowboy within {@link #TOWN}, or {@code null}. */
    private static @Nullable Cowboy nearestCowboy(ServerLevel level, Villager villager) {
        Cowboy nearest = null;
        double best = Double.MAX_VALUE;
        AABB town = new AABB(villager.blockPosition()).inflate(TOWN);
        for (Cowboy cowboy : level.getEntitiesOfClass(Cowboy.class, town)) {
            if (!cowboy.isFounded() || cowboy.lastName().isEmpty()) {
                continue;
            }
            double distance = cowboy.distanceToSqr(villager);
            if (distance < best) {
                best = distance;
                nearest = cowboy;
            }
        }
        return nearest;
    }
}
