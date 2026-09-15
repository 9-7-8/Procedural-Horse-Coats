package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_V;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_V_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Row V: hunger</b> (owner, 2026-09-14). Four pens on stone floors, so the only food in
 * each is the food put there, and every one logs its own answer.
 *
 * <table>
 *   <tr><th>west</th><th>east</th></tr>
 *   <tr><td>HUNGER ORDER - a hungry horse, food laid nearest-first and worst-first</td>
 *       <td>HUNGER FED - a hurt, fed horse beside water</td></tr>
 *   <tr><td>HUNGER STARVING - a hurt, starving horse beside water</td>
 *       <td>HUNGER CARNIVORE - a hungry meat-eater and three chickens</td></tr>
 * </table>
 *
 * <p>The order pen puts the worst food nearest, so a horse that took the nearest thing would
 * eat the poppy and fail; the right answer is the cake at the far end, then the hay. The
 * starving pen is also the test that vanilla's free heal is off (HorseRegenMixin): nothing
 * else in there could raise its health.
 */
final class DebugYardHunger {

    private DebugYardHunger() {
    }

    private static final int PEN_W = 9;

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        int z0 = mouthZ + ROW_V;
        try {
            order(level, gy, west, z0);
            starving(level, gy, west + PEN_W, z0);
            fed(level, gy, east, z0);
            carnivore(level, gy, east + PEN_W, z0);
            ActionTrace.log("test yard", "hunger pens built (row V: food order, starving, fed, carnivore)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: hunger row failed to build", e);
        }
    }

    private static void order(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, "HUNGER ORDER",
                List.of("HUNGER: ORDER", "hungry: cake first", "then hay - never", "the poppy or moss"),
                Blocks.CAKE, Blocks.HAY_BLOCK, Blocks.POPPY, Blocks.RED_MUSHROOM, Blocks.MOSS_BLOCK, Blocks.DIRT);
        int z = z0 + 5;
        int y = gy + 1;
        DebugPenManager.groundColumn(level, x0 + 3, gy, z, Blocks.DIRT.defaultBlockState());
        level.setBlock(new BlockPos(x0 + 3, y, z), Blocks.POPPY.defaultBlockState(), 3);
        DebugPenManager.groundColumn(level, x0 + 4, gy, z, Blocks.PODZOL.defaultBlockState());
        level.setBlock(new BlockPos(x0 + 4, y, z), Blocks.RED_MUSHROOM.defaultBlockState(), 3);
        DebugPenManager.groundColumn(level, x0 + 5, gy, z, Blocks.MOSS_BLOCK.defaultBlockState());
        level.setBlock(new BlockPos(x0 + 6, y, z), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x0 + 8, y, z), Blocks.CAKE.defaultBlockState(), 3);
        Horse h = spawn(level, gy, x0 + 1.5, z + 0.5, "horsegenetics.scn4a=N/N", "ORDER HORSE");
        set(level, h, 20.0, 1.0);
        ActionTrace.log("test yard", "HUNGER ORDER: a horse at hunger 20 with, nearest first, a poppy, a"
                + " mushroom, moss, hay and a cake. Expect '[trace] hunger | ... ate minecraft:cake (cake)', then"
                + " '... ate minecraft:hay_block (hay)', then nothing until it is hungry again, about 20 minutes on."
                + " Anything else eaten first is a FAIL");
    }

    private static void starving(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, "HUNGER STARVING",
                List.of("HUNGER: STARVING", "hurt, starving, by", "water: must NOT", "heal, all day"));
        water(level, gy, x0 + 3, z0 + 3);    // two clear blocks from every wall (owner, gap 247), off the horse
        Horse h = spawn(level, gy, x0 + 4.5, z0 + 5.5, "horsegenetics.scn4a=N/N", "STARVING HORSE");
        set(level, h, 5.0, 0.5);
        clock(level, h, "HUNGER STARVING", "expect hp never to rise for as long as this runs - a starving"
                + " horse cannot heal, and vanilla's free heal is off, so any rise is a FAIL", 0);
    }

    private static void fed(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, "HUNGER FED",
                List.of("HUNGER: FED", "hurt, fed, by water:", "full health in", "about 15 seconds"));
        water(level, gy, x0 + 3, z0 + 3);    // two clear blocks from every wall (owner, gap 247), off the horse
        Horse h = spawn(level, gy, x0 + 4.5, z0 + 5.5, "horsegenetics.scn4a=N/N", "FED HORSE");
        set(level, h, 100.0, 0.5);
        clock(level, h, "HUNGER FED", "expect full hp within about 15 s of the first line, and hunger to"
                + " fall 2 for every point healed (plus a slow drain)", 0);
    }

    private static void carnivore(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, "HUNGER CARNIVORE",
                List.of("HUNGER: CARNIVORE", "hungry meat-eater", "+ 3 chickens: hunt,", "then eat the drop"));
        Horse h = spawn(level, gy, x0 + 2.5, z0 + 5.5, "horsegenetics.diet=Dmeat/Dmeat", "CARNIVORE HORSE");
        set(level, h, 20.0, 1.0);
        for (int i = 0; i < 3; i++) {
            Entity chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            if (chicken == null) {
                continue;
            }
            chicken.snapTo(x0 + 6.5, gy + 1, z0 + 3.5 + i * 2, 0.0F, 0.0F);
            if (chicken instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(chicken);
        }
        ActionTrace.log("test yard", "HUNGER CARNIVORE: a meat-eater at hunger 20 and three chickens. Expect"
                + " '[trace] hunger | ... hunted minecraft:chicken', then '... ate minecraft:chicken (dropped)',"
                + " and again until it reaches 90 - most likely all three");
    }

    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, String name, List<String> sign,
                            net.minecraft.world.level.block.Block... watched) {
        int x1 = x0 + PEN_W;
        int z1 = z0 + ROW_V_D;
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = x0 + 1; x < x1; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        YardPens.register(gy, x0, x1, z0, z1, name);
        DebugWorldWatch.watch(name, DebugTestYard.box(x0, gy, z0, x1, gy + 3, z1), null, watched);
    }

    private static void water(ServerLevel level, int gy, int x, int z) {
        level.setBlock(new BlockPos(x, gy + 1, z),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
    }

    private static @Nullable Horse spawn(ServerLevel level, int gy, double x, double z, String code, String label) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, Sex.FEMALE, code, true);
        DebugTestYard.label(h, label);
        return h;
    }

    /** A second after spawning, so nothing the spawn does afterwards resets them. */
    private static void set(ServerLevel level, @Nullable Horse h, double hunger, double healthShare) {
        if (h == null) {
            return;
        }
        DebugYardHerd.after(level, 20, () -> {
            if (h.isAlive()) {
                h.setData(ModAttachments.HUNGER.get(), hunger);
                h.setHealth((float) Math.max(1.0, h.getMaxHealth() * healthShare));
            }
        });
    }

    /** Every five seconds for the first minute, then every minute. */
    private static void clock(ServerLevel level, @Nullable Horse h, String name, String expect, int round) {
        if (h == null) {
            return;
        }
        DebugYardHerd.after(level, round == 0 ? 40 : round < 12 ? 100 : 1_200, () -> {
            if (!h.isAlive()) {
                ActionTrace.log("test yard", name + ": the horse is gone");
                return;
            }
            ActionTrace.log("test yard", String.format(Locale.ROOT, "%s: hp %.1f/%.1f, hunger %.0f | %s",
                    name, h.getHealth(), h.getMaxHealth(), h.getData(ModAttachments.HUNGER.get()), expect));
            clock(level, h, name, expect, round + 1);
        });
    }
}
