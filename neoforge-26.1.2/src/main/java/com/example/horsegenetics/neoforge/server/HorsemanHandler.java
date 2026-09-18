package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.Region;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;


/**
 * Names the <b>horseman</b> when a villager takes the job at a Horseman's Table:
 * a first name of their own, and the family name of the nearest cowboy in town.
 *
 * <h2>One family per village</h2>
 * Whoever is hired first names the family, and everyone after joins it - see
 * {@link #familySurname}. It does not matter whether that is the cowboy or the
 * horseman, which is the point: the hitch and the table hand out their jobs
 * independently and in whatever order the villagers get round to them, so
 * neither can be the one that decides.
 *
 * <h2>Why a poll and not an event</h2>
 * Taking a job is not something NeoForge fires an event for. It happens deep in
 * vanilla's brain, when a villager's {@code AcquirePoi} behaviour claims a
 * ticket on a job-site POI, and the only outward sign is that their
 * {@code VillagerData} now names a profession. So this watches for that: one
 * profession comparison per villager tick.
 *
 * <p>That also makes it the cheapest answer to the hardest thing about the
 * horseman to test. They do not generate ready-made - they exist when a table is
 * placed near an unemployed villager and they get round to claiming it - and when
 * nothing happens there are three indistinguishable reasons (the POI did not
 * register, the {@code acquirable_job_site} tag did not merge, or they simply have
 * not yet). The line this prints separates the third from the first two.
 */
@EventBusSubscriber
public final class HorsemanHandler {

    /** How far counts as "this town", for the surname. */
    private static final int TOWN = 128;

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
        Rng rng = new NeoRng(villager.getRandom());
        String regionId = regionIdNear(level, villager.blockPosition(), villager, rng);
        String surname = familySurname(level, villager.blockPosition(), villager, rng, regionId);
        villager.setCustomName(Component.literal(
                PersonNameGenerator.forRegion(regionId).generateParts(rng).first() + " " + surname));
        villager.setCustomNameVisible(true);
        DebugAnnounce.sayAt(level, "Horseman",
                villager.getName().getString() + " took the table, of the " + surname + " family",
                villager.blockPosition(), ChatFormatting.AQUA);
    }

    /**
     * <b>The surname this village's horse trade already uses</b>, or a new one if
     * it has not got one yet.
     *
     * <p>Asked by <i>both</i> handlers, and that is the whole design. The hitch
     * and the table hand out their jobs independently, in whatever order the
     * villagers get round to claiming them - so neither role can be the one that
     * owns the name. Whoever is hired first looks round, finds nobody, and coins
     * one; everybody after finds them and joins.
     *
     * <p>The <b>nearest</b> namesake wins, which only matters in a village with
     * two outfits in it: a second hitch across town founds the Adamses, and a
     * table put up beside it joins the Adamses rather than the Hargreaves at the
     * far end.
     *
     * <p>It counts the two roles this mod names and nothing else. A player who
     * renames their own farmer "Bob" has not founded a horse family.
     */
    public static String familySurname(ServerLevel level, BlockPos at, @Nullable Entity except, Rng rng,
                                       String regionId) {
        AABB town = new AABB(at).inflate(TOWN);
        String nearest = null;
        double best = Double.MAX_VALUE;

        for (Cowboy cowboy : level.getEntitiesOfClass(Cowboy.class, town)) {
            // An arcane dealer never founds the village's horse family and never
            // joins it: he is passing through with a string of magical horses,
            // not somebody's brother. Skipped here as well as when he is named,
            // or the stable hand ends up a Blackthorn.
            if (cowboy == except || cowboy.isArcane() || cowboy.lastName().isEmpty()) {
                continue;
            }
            double distance = cowboy.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            if (distance < best) {
                best = distance;
                nearest = cowboy.lastName();
            }
        }
        for (Villager villager : level.getEntitiesOfClass(Villager.class, town)) {
            if (villager == except || villager.getCustomName() == null
                    || !villager.getVillagerData().profession().is(ModVillagerProfessions.HORSEMAN.getKey())) {
                continue;
            }
            String surname = lastWordOf(villager.getName().getString());
            if (surname.isEmpty()) {
                continue;
            }
            double distance = villager.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            if (distance < best) {
                best = distance;
                nearest = surname;
            }
        }
        return nearest != null ? nearest : PersonNameGenerator.forRegion(regionId).generateParts(rng).last();
    }

    /**
     * <b>Which part of the world this village's horse trade came from.</b>
     *
     * <p>A horseman has no breed of his own to read it off, so he takes it from
     * the nearest cowboy who has one - the same nearest-wins scan the surname
     * uses, and for the same reason: two outfits in one town are two families
     * from two places, and a table beside the second one joins the second one.
     *
     * <p>With no cowboy in town at all he is the founder, and where he is from is
     * simply rolled. That is not a fallback so much as the other half of the
     * premise: somebody arrived here first, and they came from somewhere.
     */
    /**
     * <b>The region of the nearest cowboy in town</b>, or empty when there is no
     * cowboy to take one from.
     *
     * <p>This is the whole of "their connected cowboy": nearest wins, inside
     * {@link #TOWN} blocks, skipping any cowboy with no breed of their own to
     * read a region off. It is deliberately <i>not</i> stored on the horseman -
     * he has no region of his own, and a cowboy who moves in or dies changes the
     * answer, which is the behaviour a village ought to have.
     *
     * <p>Two callers want different things when it comes back empty, which is
     * why the fallback lives with them rather than here: naming rolls a region
     * (somebody arrived first, and they came from somewhere), while the breed
     * egg trades simply stop filtering.
     */
    public static Optional<Region> nearestCowboyRegion(ServerLevel level, BlockPos at, @Nullable Entity except) {
        AABB town = new AABB(at).inflate(TOWN);
        Region nearest = null;
        double best = Double.MAX_VALUE;

        for (Cowboy cowboy : level.getEntitiesOfClass(Cowboy.class, town)) {
            if (cowboy == except) {
                continue;
            }
            Region region = CowboyHandler.regionOf(cowboy).orElse(null);
            if (region == null) {
                continue;
            }
            double distance = cowboy.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            if (distance < best) {
                best = distance;
                nearest = region;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static String regionIdNear(ServerLevel level, BlockPos at, @Nullable Entity except, Rng rng) {
        return nearestCowboyRegion(level, at, except)
                .map(Region::id)
                .orElseGet(() -> Region.values()[rng.nextInt(Region.values().length)].id());
    }

    /** The family half of "Wade Hargreave". */
    private static String lastWordOf(String name) {
        int space = name.lastIndexOf(' ');
        return space < 0 ? name : name.substring(space + 1);
    }
}
