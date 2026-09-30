package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AA;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AA_D;

/**
 * <b>Now only BLOOD ONLY, in row AA east</b> (2026-09-30: sun only, white only, hunt order and the full strain are
 * answered and deleted; a blood-only horse going a whole day without biting is still gap 250). What follows is how
 * the class began.
 *
 * <p><b>Rows AF-east and AG: the dhampir split</b> (verification &sect;0-R: "everything about the split is unplayed").
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


    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int east = cx + EAST_MIN + 1;
        try {
            // Row AA east since 2026-09-30, where WATERBORN stood: the only dhampir pen still open (gap 250).
            bloodOnly(level, gy, east, mouthZ + ROW_AA);
            ActionTrace.log("test yard", "dhampir pen built (row AA east: BLOOD ONLY)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AF-AG (dhampir) failed to build", e);
        }
    }

    /** Dbld/Dbld alone: hunts at any hour, daylight included, and never burns. */
    private static void bloodOnly(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, ROW_AA_D, "BLOOD ONLY", List.of("BLOOD ONLY", "Dbld/Dbld: bites", "the cow by day,", "never burns"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 2.5, z0 + 7.5, Sex.FEMALE,
                "horsegenetics.diet=Dbld/Dbld", true, "BLOOD ONLY");
        DebugYardUnattended.animal(level, EntityType.COW, gy, x0 + 6.5, z0 + 3.5);
        state(level, h, 100.0, 0.5);
        ActionTrace.log("test yard", "BLOOD ONLY: hurt to half. Expect '[trace] blood | ... \"BLOOD ONLY\" bit minecraft:cow'"
                + " in daylight within a minute or two, and no '[trace] sun' line naming it");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, int depth, String name, List<String> sign) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, depth, name, Blocks.GRASS_BLOCK.defaultBlockState(), sign);
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
