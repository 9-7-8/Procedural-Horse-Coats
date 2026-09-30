package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Now only DEATH DIAMONDS, in row X east</b> (2026-09-30: every other pen here - WARD, BAIT and their controls,
 * SWIM SPEED, BREATH, DEATH LAVA, DEATH WATER, DEATH ITEMS - was answered and deleted; the pure Dia/Dia horse has no
 * recorded run since the volatile allele was retired). What follows is how the class began.
 *
 * <p><b>Rows AB-AD: the five effect genes nobody has watched</b> - mob aura, swim speed, water breathing, on death and
 * item drop (known-gaps gap 121: "have never run in a game"). None of their handlers writes a log line, so every pen
 * here logs its own readings and, where the answer is a yes or a no, PASS or FAIL.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>AB</td><td>WARD - Wrd/Wrd and three husks</td><td>WARD CONTROL - a plain horse and three husks</td></tr>
 *   <tr><td>AC</td><td>BAIT, BAIT CONTROL - does a husk come for the horse</td>
 *       <td>SWIM SPEED - laps of a channel; BREATH - three horses under a lid</td></tr>
 *   <tr><td>AD</td><td>DEATH LAVA, DEATH WATER - on a built floor</td>
 *       <td>DEATH DIAMONDS - a Dia horse; DEATH ITEMS - Egg and Swd</td></tr>
 * </table>
 */
final class DebugYardEffects {

    private DebugYardEffects() {
    }


    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            // Row X east since 2026-09-30: the only pen here whose question was still open.
            deathDiamonds(level, gy, east + 9, mouthZ + ROW_X);
            ActionTrace.log("test yard", "effect pen built (row X east: DEATH DIAMONDS)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AB-AD failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row AB: mob aura, ward
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Row AC: bait, swim speed, water breathing
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Row AD: on death, item drop
    // ------------------------------------------------------------------

    /**
     * A diamond-bearing horse: 2 to 5 diamonds instead of leather.
     *
     * <p>This pen was DEATH BOOM until 2026-09-24, when the volatile allele was retired and took the
     * blast, the witness horse and the rebuild-the-pen-afterwards step with it. The diamond half is
     * kept because nothing else in the yard watches {@code Dia}.
     */
    private static void deathDiamonds(ServerLevel level, int gy, int x0, int z0) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_X_D, "DEATH DIAMONDS", Blocks.STONE.defaultBlockState(),
                List.of("DEATH DIAMONDS", "a Dia/Dia horse:", "2 to 5 diamonds,", "never leather"));
        Horse h = DebugYardUnattended.horse(level, gy, x0 + 4.5, z0 + 4.5, Sex.MALE,
                "horsegenetics.magic_item_drop=Dia/Dia", true, "DEATH DIAMONDS");
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_X_D);
        DebugYardHerd.after(level, 260, () -> {
            if (h == null || !h.isAlive()) {
                ActionTrace.log("test yard", "DEATH DIAMONDS: the horse was gone before its death - nothing to test");
                return;
            }
            ActionTrace.log("test yard",
                    "DEATH DIAMONDS: killing the horse at " + h.blockPosition().toShortString());
            h.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            DebugYardHerd.after(level, 40, () -> {
                Map<String, Integer> found = itemCounts(level, box);
                int diamonds = found.getOrDefault("minecraft:diamond", 0);
                int leather = found.getOrDefault("minecraft:leather", 0);
                ActionTrace.log("test yard", "DEATH DIAMONDS at 2 s: items " + found
                        + " - 2 to 5 diamonds and no leather: "
                        + (diamonds >= 2 && diamonds <= 5 && leather == 0 ? "PASS" : "FAIL"));
            });
        });
    }

    private static Map<String, Integer> itemCounts(ServerLevel level, AABB box) {
        Map<String, Integer> out = new TreeMap<>();
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box.inflate(1.0, 3.0, 1.0))) {
            out.merge(ie.getItem().getItem().builtInRegistryHolder().key().identifier().toString(),
                    ie.getItem().getCount(), Integer::sum);
        }
        return out;
    }

}
