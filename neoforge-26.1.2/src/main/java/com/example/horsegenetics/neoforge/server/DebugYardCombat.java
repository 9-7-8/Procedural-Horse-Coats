package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.ArrayList;
import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J_D;

/**
 * <b>What is left of the damage genes' rows: the infirmary.</b>
 *
 * <p>Owner, 2026-09-13: <i>"remove the 'no horse damage' exception in the
 * yard."</i> That rule cancelled every {@code LivingIncomingDamageEvent} aimed
 * at a horse in this dimension, and it made six genes untestable in the one
 * dimension built for testing - guardian, healer, cleansing light, ender echo and
 * both on-death loci. Five separate protections in this dimension turned out to
 * be the reason a gene "did nothing", and the shape is worth naming once more:
 * <b>a gene that does nothing and a gene that is forbidden from doing anything are
 * identical from inside the game.</b>
 *
 * <p><b>Most of this file is gone (2026-09-30).</b> The arena's guardian needed a
 * person to tame it and then be hurt, and ender echo's open half is a hit on its
 * <i>rider</i> - both are the test kit's now (batch 5), because the yard only
 * keeps what runs with nobody in it. Cleansing light moved to
 * {@link DebugYardClockwork}, beside caged undead. What stays is the healer's
 * control, which needs nobody: three hurt horses that must not mend.
 */
final class DebugYardCombat {

    private DebugYardCombat() {
    }

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        // ROW J WEST IS EMPTY: the guardian arena went on 2026-09-30.
        buildInfirmary(level, gy, cx, mouthZ);
    }

    // ==================================================================
    // ROW J EAST - the infirmary
    // ==================================================================

    /**
     * <b>Healer's control: three hurt horses beside a healer, which must stay hurt.</b>
     *
     * <p>{@code HealerGene}'s aura is {@code Healing("players", ...)} - the owner
     * spotted it from the other end within minutes of walking in: <i>"it seems like
     * the healer horses are healing me?"</i> They are, and that is the gene. So the
     * three patients are the CONTROL, not the subject: if they heal, the aura is not
     * reading its target field and is hitting everything alive in range. The
     * positive half - a hurt player mending - needs a player, and is not asked here.
     *
     * <p><b>No water in this pen, since 2026-09-30</b> - see
     * {@link DebugTestYard#dryPen}. Healing is hunger plus water nearby, and with the
     * pen's own cauldron the patients healed on their own: the first clockwork run
     * read two of the three back at full health, which is the gated heal and not the
     * aura, and the control could only ever have failed.
     */
    private static void buildInfirmary(ServerLevel level, int gy, int cx, int mouthZ) {
        int z0 = mouthZ + ROW_J;
        int z1 = z0 + ROW_J_D;

        int hx0 = cx + EAST_MIN;
        int hx1 = hx0 + 8;
        DebugTestYard.fencedPlot(level, gy, hx0, hx1, z0, z1);
        DebugTestYard.dryPen(level, gy, hx0, z0);
        DebugPenManager.placeSign(level, new BlockPos(hx0 + 4, gy + 1, z0 - 1), Direction.NORTH,
                List.of("HEALER CONTROL", "heals players only.", "The 3 hurt horses", "must NOT heal"));
        DebugTestYard.stock(level, gy, hx0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.healer",
                "HEALER", 1, 0, "Hlr/Hlr");
        List<Horse> patients = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Horse h = DebugPenManager.spawnHorse(level, gy + 1, hx0 + 2.0 + i * 2.0, z0 + 3.0,
                    com.example.horsegenetics.common.horse.Sex.FEMALE, DebugTestYard.PALE, true);
            DebugTestYard.label(h, "PATIENT " + (i + 1));
            if (h != null) {
                patients.add(h);
            }
        }
        DebugWorldWatch.watchAttribute("INFIRMARY - HEALER",
                DebugTestYard.box(hx0, gy, z0, hx1, gy + 1, z1), Attributes.MAX_HEALTH);
        hurtPatients(level, patients);
    }

    /**
     * Take the patients to half health a second after the build - set at spawn it is
     * undone as the horse's traits give it a new max health (the HURT MARE pen's
     * lesson) - then read them back five minutes later. Half rather than a sliver,
     * so a horse is never one bad tick from dying and reading as the healer idle.
     */
    private static void hurtPatients(ServerLevel level, List<Horse> patients) {
        String check = "HEALER CONTROL - hurt horses beside a healer stay hurt";
        DebugYardClockwork.expect(check);
        float[] set = new float[patients.size()];
        DebugYardHerd.after(level, 20, () -> {
            for (int i = 0; i < patients.size(); i++) {
                Horse h = patients.get(i);
                if (h.isAlive()) {
                    h.setHealth(h.getMaxHealth() * 0.5F);
                    set[i] = h.getHealth();
                }
            }
        });
        DebugYardHerd.after(level, 6_000, () -> {
            StringBuilder read = new StringBuilder();
            boolean healed = false;
            for (int i = 0; i < patients.size(); i++) {
                Horse h = patients.get(i);
                float now = h.isAlive() ? h.getHealth() : -1.0F;
                healed |= now > set[i] + 0.01F;
                read.append(read.length() == 0 ? "" : ", ").append(String.format("%.1f (set %.1f)", now, set[i]));
            }
            DebugYardClockwork.verdict(check, !patients.isEmpty() && !healed, "patients now " + read);
        });
    }
}
