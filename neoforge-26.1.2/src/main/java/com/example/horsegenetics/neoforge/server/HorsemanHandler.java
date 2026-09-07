package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
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


/**
 * Names the <b>horseman</b> when a villager takes the job at a Horseman's Table:
 * a first name of his own, and the family name of the nearest cowboy in town.
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
        Rng rng = new NeoRng(villager.getRandom());
        String surname = familySurname(level, villager.blockPosition(), villager, rng);
        villager.setCustomName(Component.literal(NAMES.generateParts(rng).first() + " " + surname));
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
     * one; everybody after finds him and joins.
     *
     * <p>The <b>nearest</b> namesake wins, which only matters in a village with
     * two outfits in it: a second hitch across town founds the Adamses, and a
     * table put up beside it joins the Adamses rather than the Hargreaves at the
     * far end.
     *
     * <p>It counts the two roles this mod names and nothing else. A player who
     * renames his own farmer "Bob" has not founded a horse family.
     */
    public static String familySurname(ServerLevel level, BlockPos at, @Nullable Entity except, Rng rng) {
        AABB town = new AABB(at).inflate(TOWN);
        String nearest = null;
        double best = Double.MAX_VALUE;

        for (Cowboy cowboy : level.getEntitiesOfClass(Cowboy.class, town)) {
            if (cowboy == except || cowboy.lastName().isEmpty()) {
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
        return nearest != null ? nearest : NAMES.generateParts(rng).last();
    }

    /** The family half of "Wade Hargreave". */
    private static String lastWordOf(String name) {
        int space = name.lastIndexOf(' ');
        return space < 0 ? name : name.substring(space + 1);
    }
}
