package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.common.name.PersonNameGenerator.PersonName;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.entity.ModEntities;
import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * <b>One block, two trades.</b> A Horse Trader's Post hands out a
 * {@linkplain Cowboy cowboy} first and a <b>horseman</b> second, and keeps
 * alternating.
 *
 * <h2>How one post does that</h2>
 * With one villager, twice. He claims the post the ordinary way and takes the
 * horseman profession; this looks at the village and decides which of the two he
 * should actually be. If the answer is <b>cowboy</b>, a {@link Cowboy} is spawned
 * beside him and <b>he is demoted back to no profession</b> - so vanilla's own
 * {@code AssignProfessionFromJobSite} hands him the job again a moment later, the
 * count is now uneven, and he settles as the horseman. One block, one pair, one
 * villager, and no state to save anywhere.
 *
 * <p><b>Demoted and not deleted</b>, which is the whole of the bug this replaced.
 * The first version discarded the villager and relied on the post falling vacant
 * for the next one along to claim. It never fell vacant: a job-site ticket is
 * released by {@code Villager.releaseAllPois}, which is private and runs on
 * <i>death</i>, so a <b>discarded</b> villager holds his post for ever - and with
 * {@code maxTickets} of one, the post produced a single cowboy and then nothing,
 * while every other villager in town stood around unemployed. Exactly what was
 * reported.
 *
 * <p>So the ticket is handed back explicitly, by the one public door there is
 * ({@code Villager.releasePoi}), while its owner is alive and standing on it.
 * He re-claims it a moment later and takes the other job.
 *
 * <p>The count is taken rather than remembered ({@link #wantsACowboy}): cowboys
 * and horsemen within {@link #TOWN} blocks are counted, and the trade with fewer
 * is the one handed out. That is self-correcting - kill the cowboy and the next
 * claimant replaces him - where a saved "whose turn is it" counter would drift
 * the first time something died.
 *
 * <h2>The family name</h2>
 * A cowboy takes a first and last name that no other cowboy in town has; the
 * horseman that follows him takes <b>his</b> surname and a first name of his
 * own. So a village reads as one family per post: Wade Hargreave at the barn
 * and Frank Hargreave behind the counter, and if a player raises a second post,
 * Lula Adams and Joe Adams.
 *
 * <h2>Why a poll and not an event</h2>
 * Taking a job is not something NeoForge fires an event for. It happens deep in
 * vanilla's brain, when a villager's {@code AcquirePoi} behaviour claims a
 * ticket on a job-site POI, and the only outward sign is that his
 * {@code VillagerData} now names a profession. So this watches for that: one
 * profession comparison per villager tick.
 */
@EventBusSubscriber
public final class HorsemanHandler {

    /** How far counts as "this town" - for the alternation and for the surname. */
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
                || villager.isRemoved()) {
            return;
        }
        if (!villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
            return;
        }
        if (wantsACowboy(level, villager)) {
            promoteToCowboy(level, villager);
        } else if (villager.getCustomName() == null) {
            nameTheHorseman(level, villager);
        }
    }

    /**
     * Which of the two trades this village is short of.
     *
     * <p>Ties go to the cowboy, so an empty village gets the man with the horses
     * first - he is the one the post exists for, and a horseman with nobody to
     * take a surname from would be a stranger.
     */
    private static boolean wantsACowboy(ServerLevel level, Villager villager) {
        AABB town = new AABB(villager.blockPosition()).inflate(TOWN);
        int cowboys = level.getEntitiesOfClass(Cowboy.class, town).size();
        int horsemen = 0;
        for (Villager other : level.getEntitiesOfClass(Villager.class, town)) {
            if (other != villager && other.getCustomName() != null
                    && other.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
                horsemen++;
            }
        }
        return cowboys <= horsemen;
    }

    /**
     * Stand a cowboy up beside this villager, and put the villager back to work.
     *
     * <p>A cowboy is a different <b>entity</b>, not a profession - he has a herd,
     * a stock target and a merchant screen that sells papers rather than goods,
     * and none of that fits on a {@code Villager}. So one is spawned rather than
     * converted. He arrives bare and {@link CowboyHandler} builds him on his
     * first tick, exactly as he used to when the barn placed him.
     *
     * <p>The villager is <b>demoted, not deleted</b> - see the class comment for
     * the job-site ticket that made deleting him a dead end. Clearing his
     * profession is enough: he is still standing at his own post holding his own
     * ticket, so vanilla's {@code AssignProfessionFromJobSite} gives him the job
     * back within a tick or two, and by then the village has a cowboy in it and
     * the count says horseman.
     */
    private static void promoteToCowboy(ServerLevel level, Villager villager) {
        Cowboy cowboy = ModEntities.COWBOY.get().create(level, EntitySpawnReason.EVENT);
        if (cowboy == null) {
            return;
        }
        cowboy.snapTo(villager.getX(), villager.getY(), villager.getZ(),
                villager.getYRot(), villager.getXRot());
        level.addFreshEntity(cowboy);

        // Hand the post back and forget it, then clear the profession. Releasing
        // the ticket is what a *dead* villager does; doing it while he is alive
        // and standing on the thing means AcquirePoi simply claims it again for
        // him, AssignProfessionFromJobSite gives him the job, and the count now
        // says horseman. Demoting without releasing would leave him holding a
        // job site he has no profession for, which nothing in vanilla resolves.
        villager.releasePoi(MemoryModuleType.JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), VillagerProfession.NONE));
        DebugAnnounce.sayAt(level, "Cowboy",
                "a villager took the post and stood up a cowboy; he goes back for the horseman job",
                cowboy.blockPosition(), ChatFormatting.YELLOW);
    }

    /**
     * Give him a first name of his own and the surname of the cowboy he is
     * paired with - the nearest one in town.
     *
     * <p>No cowboy within range and he keeps the plain profession name. That
     * should not happen, because the alternation hands out a cowboy first, but a
     * player who breaks the pattern by killing one gets a nameless horseman
     * rather than an exception.
     */
    private static void nameTheHorseman(ServerLevel level, Villager villager) {
        Cowboy family = nearestCowboy(level, villager);
        if (family == null) {
            return;
        }
        Rng rng = new NeoRng(villager.getRandom());
        villager.setCustomName(Component.literal(
                NAMES.generateParts(rng).first() + " " + family.lastName()));
        villager.setCustomNameVisible(true);
        DebugAnnounce.sayAt(level, "Horseman",
                villager.getName().getString() + " took the job, of the " + family.lastName() + " family",
                villager.blockPosition(), ChatFormatting.AQUA);
    }

    /**
     * A first and last name no cowboy in this town is already using.
     *
     * <p>Surnames are the half that matters - they are what pairs him with his
     * horseman - so two families in one village sharing one would make the pair
     * ambiguous. Called from {@link CowboyHandler} when it founds him.
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
