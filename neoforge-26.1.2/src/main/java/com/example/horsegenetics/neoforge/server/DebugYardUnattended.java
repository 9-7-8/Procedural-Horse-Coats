package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows X and Y</b> (row AA emptied and went the same day), since 2026-09-30 minus every pen whose question was answered (BONE MEAL, GELDING BAND,
 * LYCAN ROUND TRIP, WERE-COW, WATERBORN; then LYCAN DOOMED and LETHAL FOALS, on their own PASS lines the same
 * day - owner: an automatic yard PASS closes a check "same as clockwork").
 *
 * <p><b>Rows X and Y: open tests that need nobody at the keyboard</b> (owner, 2026-09-15: "put
 * everything which can be tested unattended into the yard"). Each pen logs what to expect beside
 * what happened, so the answer is in {@code latest.log}.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>X</td><td>NIGHT SHY - a night-shy horse, a band and two cows</td><td>SUNTOUCHED</td></tr>
 *   <tr><td>Y</td><td>REACH WALL, REACH FENCE - cover reach and courtship</td>
 *       <td>STATS - speed and health floors and stacking</td></tr>
 * </table>
 */
final class DebugYardUnattended {

    private DebugYardUnattended() {
    }

    private static final String PLAIN = "horsegenetics.scn4a=N/N";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            nightShy(level, gy, west, mouthZ + ROW_X);
            reachWall(level, gy, west, mouthZ + ROW_Y);
            reachFence(level, gy, west + 9, mouthZ + ROW_Y);
            stats(level, gy, east, mouthZ + ROW_Y);
            // KICK HUNTER and KICK PLAIN went on their own verdicts, 2026-09-30: the hunter's blows 11, 12 and 17
            // ticks apart (gap 222's ten-tick swing), the plain horse none. DebugWorldWatch.kicksBy and
            // DebugTestYard.lidded, which that pen needed against Last Stand's jump, stay for the next fighter.
            suntouched(level, gy, east, mouthZ + ROW_X);   // row X east since row AA emptied, 2026-09-30
            ActionTrace.log("test yard", "unattended pens built (rows X and Y: night shy, suntouched, reach, stats)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows X-AA failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row X
    // ------------------------------------------------------------------

    /** Gap 241: a night-shy horse flees passive animals after dark, and never its own kind. */
    private static void nightShy(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 18, ROW_X_D, "NIGHT SHY", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("NIGHT SHY", "Fnc/Fnc + a band", "+ two cows: flees", "cows, never horses"));
        horse(level, gy, x0 + 3.5, z0 + 6.5, Sex.FEMALE, "horsegenetics.skittish=Fnc/Fnc", false, "NIGHT SHY");
        horse(level, gy, x0 + 9.5, z0 + 4.5, Sex.MALE, PLAIN, false, "SHY BAND STALLION");
        horse(level, gy, x0 + 10.5, z0 + 7.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 1");
        horse(level, gy, x0 + 12.5, z0 + 5.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 2");
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 3.5);
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 8.5);
        ActionTrace.log("test yard", "NIGHT SHY: expect '[trace] night flee | ... NIGHT SHY ... from minecraft:cow'"
                + " after dark, and never 'from minecraft:horse' - a flee from a horse is a FAIL (gap 241)");
    }

    // ------------------------------------------------------------------
    // Row Y
    // ------------------------------------------------------------------

    /** Gap 230: a stallion cannot cover a mare through a three-thick wall. */
    private static void reachWall(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_Y_D, "REACH WALL", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("REACH: WALL", "mare in heat and a", "stallion either side", "of stone: no cover"));
        for (int x = x0 + 1; x < x0 + 8; x++) {
            for (int z = z0 + 5; z <= z0 + 7; z++) {
                for (int y = gy + 1; y <= gy + 3; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        Horse mare = horse(level, gy, x0 + 4.5, z0 + 3.5, Sex.FEMALE, PLAIN, true, "RW MARE");
        horse(level, gy, x0 + 4.5, z0 + 9.5, Sex.MALE, PLAIN, true, "RW STUD");
        DebugYardFertility.inHeat(mare);
        YardPens.register(gy, x0, x0 + 8, z0, z0 + 5, "REACH WALL NORTH");
        YardPens.register(gy, x0, x0 + 8, z0 + 7, z0 + ROW_Y_D, "REACH WALL SOUTH");
        ActionTrace.log("test yard", "REACH WALL: expect no 'natural cover' line naming RW MARE, ever (gap 230)");
    }

    /** Gap 230: across a single fence, a cover happens - but only after courtship in reach. */
    private static void reachFence(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Y_D, "REACH FENCE", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("REACH: FENCE", "one fence between", "them: covered, but", "only after 60 ticks"));
        for (int x = x0 + 1; x < x0 + 9; x++) {
            level.setBlock(new BlockPos(x, gy + 1, z0 + 6), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        Horse mare = horse(level, gy, x0 + 4.5, z0 + 5.3, Sex.FEMALE, PLAIN, true, "RF MARE");
        horse(level, gy, x0 + 4.5, z0 + 6.7, Sex.MALE, PLAIN, true, "RF STUD");
        DebugYardFertility.inHeat(mare);
        ActionTrace.log("test yard", "REACH FENCE: expect a 'natural cover' line naming RF MARE within a heat,"
                + " with the pair in reach at least 60 ticks first (gap 230)");
    }

    /** Stat floors and stacking: the slowest speed, Swift with myostatin, identical horses, a frail floor. */
    private static void stats(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Y_D, "STATS", Blocks.STONE.defaultBlockState(),
                List.of("STATS", "speed floor, Swift x", "MSTN, three equal", "C/T, a frail floor"));
        water(level, gy, x0 + 4, z0 + 4);    // clear of the walls and the horses (gap 247)
        horse(level, gy, x0 + 2.5, z0 + 2.5, Sex.FEMALE, "horsegenetics.magic_speed=Sluggish/Sluggish", true, "STAT SLUGGISH");
        horse(level, gy, x0 + 6.5, z0 + 2.5, Sex.FEMALE, "horsegenetics.magic_speed=Swift/Swift", true, "STAT SWIFT");
        horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.mstn=C/C", true, "STAT MSTN CC");
        horse(level, gy, x0 + 6.5, z0 + 5.5, Sex.FEMALE,
                "horsegenetics.magic_speed=Swift/Swift-horsegenetics.mstn=C/C", true, "STAT SWIFT+CC");
        for (int i = 0; i < 3; i++) {
            horse(level, gy, x0 + 2.5 + i * 2, z0 + 8.5, Sex.FEMALE, "horsegenetics.mstn=C/T", true, "STAT MSTN CT " + (i + 1));
        }
        horse(level, gy, x0 + 7.5, z0 + 10.0, Sex.FEMALE,
                "horsegenetics.magic_health=Frail/Frail-horsegenetics.b4galt7=d/d", true, "STAT FRAIL");
        DebugWorldWatch.watchAttribute("STATS SPEED", DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_Y_D),
                Attributes.MOVEMENT_SPEED);
        DebugWorldWatch.watchAttribute("STATS HEALTH", DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_Y_D),
                Attributes.MAX_HEALTH);
        ActionTrace.log("test yard", "STATS: read the STATS SPEED census - STAT SLUGGISH at 0.1125 or above, STAT"
                + " SWIFT+CC about STAT SWIFT times STAT MSTN CC over base, the three STAT MSTN CT identical; STATS HEALTH:"
                + " STAT FRAIL at or above the floor and alive");
    }

    // ------------------------------------------------------------------
    // Row AA
    // ------------------------------------------------------------------

    /** Suntouched's light verb is skipped in the horse dimension on purpose: no light block should ever appear here. */
    private static void suntouched(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_X_D, "SUNTOUCHED", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("SUNTOUCHED", "adult and foal: no", "light blocks, ever,", "in this dimension"),
                Blocks.LIGHT);
        horse(level, gy, x0 + 2.5, z0 + 4.5, Sex.MALE, "horsegenetics.suntouched=Sntch/Sntch", true, "SUNTOUCHED ADULT");
        Horse foal = horse(level, gy, x0 + 5.5, z0 + 7.5, Sex.FEMALE, "horsegenetics.suntouched=Sntch/Sntch", true,
                "SUNTOUCHED FOAL");
        if (foal != null) {
            foal.setAge(-72_000);
        }
        ActionTrace.log("test yard", "SUNTOUCHED: the pen holds the yard's own lamps (11 light blocks in the 07:10"
                + " run), so expect the SUNTOUCHED watch line's minecraft:light count never to rise above its first"
                + " reading; any increase is a FAIL");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String name,
                            BlockState floor, List<String> sign, net.minecraft.world.level.block.Block... watched) {
        int x1 = x0 + width;
        int z1 = z0 + depth;
        for (int x = x0 + 1; x < x1; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, floor);
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        YardPens.register(gy, x0, x1, z0, z1, name);
        DebugWorldWatch.watch(name, DebugTestYard.box(x0, gy, z0, x1, gy + 3, z1), null, watched);
    }

    static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         boolean tamed, String label) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, tamed);
        DebugTestYard.label(h, label);
        return h;
    }

    static @Nullable Entity animal(ServerLevel level, EntityType<?> type, int gy, double x, double z) {
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        if (e == null) {
            return null;
        }
        e.snapTo(x, gy + 1, z, 0.0F, 0.0F);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        return e;
    }

    private static void water(ServerLevel level, int gy, int x, int z) {
        level.setBlock(new BlockPos(x, gy + 1, z),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
    }

}
