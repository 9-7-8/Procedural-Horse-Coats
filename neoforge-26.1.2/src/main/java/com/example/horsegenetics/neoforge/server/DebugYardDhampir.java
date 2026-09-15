package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AF;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AF_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AG;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AG_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows AF-east and AG: the dhampir split</b> (verification &sect;0-R: "everything about the split is unplayed").
 * The old single gene is four loci now - {@code magic_white} (the coat), {@code sun_sensitivity} (burning, and running
 * for shade), {@code diet=Dbld} (blood only) and {@code Vmp} on the stat loci. Each pen isolates one, and the full white
 * strain checks the confirmed behaviour survived the rename. Readings are the existing {@code [trace] sun},
 * {@code [trace] blood} and {@code [trace] hunger} lines; each pen logs what they should and should not say.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>AF</td><td>(births, {@link DebugYardBirths})</td><td>SUN ONLY; BLOOD ONLY</td></tr>
 *   <tr><td>AG</td><td>WHITE ONLY; HUNT ORDER</td><td>DHAMPIR FULL</td></tr>
 * </table>
 */
final class DebugYardDhampir {

    private DebugYardDhampir() {
    }

    private static final String PLAIN = "horsegenetics.scn4a=N/N";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            sunOnly(level, gy, east, mouthZ + ROW_AF);
            bloodOnly(level, gy, east + 9, mouthZ + ROW_AF);
            whiteOnly(level, gy, west, mouthZ + ROW_AG);
            huntOrder(level, gy, west + 9, mouthZ + ROW_AG);
            full(level, gy, east, mouthZ + ROW_AG);
            ActionTrace.log("test yard", "dhampir pens built (rows AF-AG: sun only, blood only, white only, hunt order,"
                    + " full white strain)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AF-AG (dhampir) failed to build", e);
        }
    }

    /** Sun/Sun alone: burns and runs for the roofed corner by day - and eats hay like any horse. */
    private static void sunOnly(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AF_D, "SUN ONLY", List.of("SUN ONLY", "Sun/Sun: burns,", "hides under the", "roof, eats hay"));
        roof(level, gy, x0, z0);
        hay(level, gy, x0 + 5, z0 + 6);
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 6.5, z0 + 7.5, Sex.FEMALE,
                "horsegenetics.sun_sensitivity=Sun/Sun", true, "SUN ONLY");
        state(level, h, 20.0, 1.0);
        ActionTrace.log("test yard", "SUN ONLY: expect '[trace] sun | ... \"SUN ONLY\" burning at' by day, then 'in shade',"
                + " and '[trace] hunger | ... \"SUN ONLY\" ate minecraft:hay_block' - it eats normally; any"
                + " '[trace] blood' line naming it is a FAIL");
    }

    /** Dbld/Dbld alone: hunts at any hour, daylight included, and never burns. */
    private static void bloodOnly(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AF_D, "BLOOD ONLY", List.of("BLOOD ONLY", "Dbld/Dbld: bites", "the cow by day,", "never burns"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 7.5, Sex.FEMALE,
                "horsegenetics.diet=Dbld/Dbld", true, "BLOOD ONLY");
        DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + 6.5, z0 + 3.5);
        state(level, h, 100.0, 0.5);
        ActionTrace.log("test yard", "BLOOD ONLY: hurt to half. Expect '[trace] blood | ... \"BLOOD ONLY\" bit minecraft:cow'"
                + " in daylight within a minute or two, and no '[trace] sun' line naming it");
    }

    /** Wm/Wm alone: a white coat and nothing else - no burn, no bite, eats hay. */
    private static void whiteOnly(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AG_D, "WHITE ONLY", List.of("WHITE ONLY", "Wm/Wm: a white", "coat, no burn, no", "bite, eats hay"));
        hay(level, gy, x0 + 5, z0 + 5);
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 7.5, Sex.FEMALE,
                "horsegenetics.magic_white=Wm/Wm", true, "WHITE ONLY");
        DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + 6.5, z0 + 3.5);
        state(level, h, 20.0, 0.5);
        ActionTrace.log("test yard", "WHITE ONLY: hurt to half and hungry, beside hay and a cow. Expect '[trace] hunger | ..."
                + " \"WHITE ONLY\" ate minecraft:hay_block', and never a '[trace] sun' or '[trace] blood' line naming it");
    }

    /**
     * The hunt order: a mob first, then a player, then a tamed pet - never another horse and never anything undead - and
     * one bite per animal per day. Here the only fair prey is the cow, so the day's bites are exactly one, on it.
     */
    private static void huntOrder(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AG_D, "HUNT ORDER", List.of("HUNT ORDER", "Dbld + a cow, a", "husk, a horse: one", "bite, the cow"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.FEMALE,
                "horsegenetics.diet=Dbld/Dbld", true, "HUNT ORDER");
        DebugYardUnattended.horse(level, gy, x0 + 6.5, z0 + 7.5, Sex.MALE, PLAIN, true, "HUNT ORDER BYSTANDER");
        DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + 6.5, z0 + 3.0);
        DebugYardUnattended.animal(level, EntityType.HUSK, gy, x0 + 2.5, z0 + 8.5);
        state(level, h, 100.0, 0.3);
        ActionTrace.log("test yard", "HUNT ORDER: hurt to 30%. Expect exactly one '[trace] blood | ... \"HUNT ORDER\" bit"
                + " minecraft:cow' per game day, and never a bite naming minecraft:husk or HUNT ORDER BYSTANDER");
    }

    /** The whole white strain: the confirmed cycle - burn, shade, bite, heal - must have survived the split. */
    private static void full(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AG_D, "DHAMPIR FULL", List.of("DHAMPIR FULL", "the white strain:", "burn, shade, bite,", "heal - as before"));
        roof(level, gy, x0, z0);
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 6.5, z0 + 7.5, Sex.FEMALE,
                "horsegenetics.magic_white=Wm/Wm-horsegenetics.sun_sensitivity=Sun/Sun-horsegenetics.diet=Dbld/Dbld"
                        + "-horsegenetics.magic_health=Vmp/Vmp-horsegenetics.magic_speed=Vmp/Vmp", true, "DHAMPIR FULL");
        DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + 3.0, z0 + 7.5);
        state(level, h, 100.0, 0.7);
        ActionTrace.log("test yard", "DHAMPIR FULL: expect '[trace] sun | ... burning at' then 'in shade' by day, and"
                + " '[trace] blood | ... bit minecraft:cow' with health rising - the cycle the old gene was confirmed on");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, int depth, String name, List<String> sign) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, depth, name, Blocks.GRASS_BLOCK.defaultBlockState(), sign);
    }

    /** Planks four up over a 4x4 corner: shade a sun-sensitive horse can reach, and not a step over any wall. */
    private static void roof(ServerLevel level, int gy, int x0, int z0) {
        for (int x = x0 + 1; x <= x0 + 4; x++) {
            for (int z = z0 + 1; z <= z0 + 4; z++) {
                level.setBlock(new BlockPos(x, gy + 4, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }
    }

    /** A hay bale two clear of every wall (the owner's rule for raised blocks in a pen). */
    private static void hay(ServerLevel level, int gy, int x, int z) {
        level.setBlock(new BlockPos(x, gy + 1, z), Blocks.HAY_BLOCK.defaultBlockState(), 3);
    }

    /** Hunger and a share of health, a second after spawn so the founding tick cannot overwrite them. */
    private static void state(ServerLevel level, @Nullable Horse h, double hunger, double healthShare) {
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
}
