package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.common.name.PersonNameGenerator.PersonName;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Names the <b>horseman</b> when a villager takes the job at a Horseman's Table:
 * a first name of his own, and the family name of the nearest cowboy in town.
 *
 * <h2>One family per village</h2>
 * A cowboy takes a surname no other cowboy in town has ({@link #uniqueName});
 * every horseman that follows takes the nearest one's. So a village reads as one
 * family in the horse trade - Wade Hargreave at the hitch and Frank Hargreave
 * behind the table - and a second cowboy somewhere across town founds the
 * Adamses.
 *
 * <p>The <b>nearest</b> cowboy rather than a paired one, because the horseman is
 * nobody's property: a player can put a table anywhere, and the horseman across
 * the square should still belong to the family that village buys horses from.
 * No cowboy within {@link #TOWN} blocks and he keeps the plain profession name.
 *
 * <h2>Why a poll and not an event</h2>
 * Taking a job is not something NeoForge fires an event for. It happens deep in
 * vanilla's brain, when a villager's {@code AcquirePoi} behaviour claims a
 * ticket on a job-site POI, and the only outward sign is that his
 * {@code VillagerData} now names a profession. So this watches for that: one
 * profession comparison per villager tick.
 *
 * <p>That also makes it the cheapest answer to the hardest thing about the
 * horseman to test. He does not generate ready-made - he exists when a table is
 * placed near an unemployed villager and he gets round to claiming it - and when
 * nothing happens there are three indistinguishable reasons (the POI did not
 * register, the {@code acquirable_job_site} tag did not merge, or he simply has
 * not yet). The line this prints separates the third from the first two.
 */
@EventBusSubscriber
public final class HorsemanHandler {

    /** How far counts as "this town", for the surname. */
    private static final int TOWN = 128;

    /** Attempts at a surname nobody in town already has. */
    private static final int NAME_TRIES = 12;

    private static final PersonNameGenerator NAMES = PersonNameGenerator.cowboys();

    private HorsemanHandler() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel level)
                || villager.isRemoved()
                || villager.getCustomName() != null) {
            return;
        }
        if (!villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
            return;
        }
        Cowboy family = nearestCowboy(level, villager);
        if (family == null) {
            return; // no horse family here yet; he stays "Horseman" until there is one
        }
        Rng rng = new NeoRng(villager.getRandom());
        villager.setCustomName(Component.literal(
                NAMES.generateParts(rng).first() + " " + family.lastName()));
        villager.setCustomNameVisible(true);
        DebugAnnounce.sayAt(level, "Horseman",
                villager.getName().getString() + " took the table, of the " + family.lastName() + " family",
                villager.blockPosition(), ChatFormatting.AQUA);
    }

    /**
     * A first and last name no cowboy in this town is already using.
     *
     * <p>Surnames are the half that matters - they are what says two people are
     * the same outfit - so two families in one village sharing one would make
     * that meaningless. Called from {@link CowboyHandler} when it founds him.
     */
    public static PersonName uniqueName(ServerLevel level, Cowboy cowboy, Rng rng) {
        Set<String> taken = new HashSet<>();
        for (Cowboy other : level.getEntitiesOfClass(
                Cowboy.class, new AABB(cowboy.blockPosition()).inflate(TOWN))) {
            if (other != cowboy && !other.lastName().isEmpty()) {
                taken.add(other.lastName());
            }
        }
        PersonName name = NAMES.generateParts(rng);
        for (int attempt = 0; attempt < NAME_TRIES && taken.contains(name.last()); attempt++) {
            name = NAMES.generateParts(rng);
        }
        return name;
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
