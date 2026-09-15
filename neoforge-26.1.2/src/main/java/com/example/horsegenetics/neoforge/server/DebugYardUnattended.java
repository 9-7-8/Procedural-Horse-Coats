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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Z;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Z_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AA;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AA_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows X and Y: open tests that need nobody at the keyboard</b> (owner, 2026-09-15: "put
 * everything which can be tested unattended into the yard"). Each pen logs what to expect beside
 * what happened, so the answer is in {@code latest.log}.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>X</td><td>NIGHT SHY - a night-shy horse, a band and two cows</td>
 *       <td>BONE MEAL - a bone-meal dryad beside a fenced wheat patch; GELDING BAND</td></tr>
 *   <tr><td>Y</td><td>REACH WALL, REACH FENCE - cover reach and courtship</td>
 *       <td>STATS - speed and health floors and stacking; LETHAL FOALS - on a clock</td></tr>
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
            boneMeal(level, gy, east, mouthZ + ROW_X);
            geldingBand(level, gy, east + 9, mouthZ + ROW_X);
            reachWall(level, gy, west, mouthZ + ROW_Y);
            reachFence(level, gy, west + 9, mouthZ + ROW_Y);
            stats(level, gy, east, mouthZ + ROW_Y);
            lethalFoals(level, gy, east + 9, mouthZ + ROW_Y);
            lycanRoundTrip(level, gy, west, mouthZ + ROW_Z);
            lycanDoomed(level, gy, west + 9, mouthZ + ROW_Z);
            wereCow(level, gy, east, mouthZ + ROW_Z);
            kick(level, gy, west, mouthZ + ROW_AA, "KICK GLADIATOR", "horsegenetics.gladiator=Gld/Gld");
            kick(level, gy, west + 9, mouthZ + ROW_AA, "KICK PLAIN", PLAIN);
            waterborn(level, gy, east, mouthZ + ROW_AA);
            suntouched(level, gy, east + 10, mouthZ + ROW_AA);
            ActionTrace.log("test yard", "unattended pens built (rows X-AA: night shy, bone meal, gelding band,"
                    + " reach, stats, lethal foals, lycan round trip, lycan doomed, were-cow, kick, waterborn,"
                    + " suntouched)");
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
                List.of("NIGHT SHY", "Flc/Flc + a band", "+ two cows: flees", "cows, never horses"));
        horse(level, gy, x0 + 3.5, z0 + 6.5, Sex.FEMALE, "horsegenetics.magic_night_temper=Flc/Flc", false, "NIGHT SHY");
        horse(level, gy, x0 + 9.5, z0 + 4.5, Sex.MALE, PLAIN, false, "SHY BAND STALLION");
        horse(level, gy, x0 + 10.5, z0 + 7.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 1");
        horse(level, gy, x0 + 12.5, z0 + 5.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 2");
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 3.5);
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 8.5);
        ActionTrace.log("test yard", "NIGHT SHY: expect '[trace] night flee | ... NIGHT SHY ... from minecraft:cow'"
                + " after dark, and never 'from minecraft:horse' - a flee from a horse is a FAIL (gap 241)");
    }

    /** Gap 237 (bone-meal half): a bone-meal dryad feeds grass and saplings, never a crop. */
    private static void boneMeal(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_X_D, "BONE MEAL", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("BONE MEAL", "Bone/Bone dryad:", "saplings yes,", "the wheat never"),
                Blocks.WHEAT, Blocks.OAK_SAPLING, Blocks.OAK_LOG);
        for (int i = 0; i < 4; i++) {
            level.setBlock(new BlockPos(x0 + 2 + i, gy + 1, z0 + 2), Blocks.OAK_SAPLING.defaultBlockState(), 3);
        }
        // A 3x3 wheat patch round a water block, fenced in so a hungry horse cannot eat it and hide the answer.
        int wx = x0 + 3;
        int wz = z0 + 7;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = new BlockPos(wx + dx, gy, wz + dz);
                if (dx == 0 && dz == 0) {
                    level.setBlock(floor, Blocks.WATER.defaultBlockState(), 3);
                    continue;
                }
                level.setBlock(floor, Blocks.FARMLAND.defaultBlockState(), 3);   // hydrates off the water block beside it
                level.setBlock(floor.above(), Blocks.WHEAT.defaultBlockState(), 3);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    level.setBlock(new BlockPos(wx + dx, gy + 1, wz + dz), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }
        horse(level, gy, x0 + 6.5, z0 + 3.5, Sex.FEMALE, "horsegenetics.dryad=Bone/Bone", true, "BONE MEAL");
        ActionTrace.log("test yard", "BONE MEAL: expect '[watch] bonemeal | on' sapling or grass lines and never"
                + " on minecraft:wheat; a 'tree held back' line beside the horse; no inWall (gap 237)");
    }

    /** Gap 229: a gelding in a band never challenges and never covers. */
    private static void geldingBand(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_X_D, "GELDING BAND", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("GELDING BAND", "a stallion, 2 mares", "and a gelding: he", "never covers"));
        horse(level, gy, x0 + 2.5, z0 + 3.5, Sex.MALE, PLAIN, false, "GB STALLION");
        Horse mare1 = horse(level, gy, x0 + 5.5, z0 + 4.5, Sex.FEMALE, PLAIN, false, "GB MARE 1");
        Horse mare2 = horse(level, gy, x0 + 6.5, z0 + 8.5, Sex.FEMALE, PLAIN, false, "GB MARE 2");
        Horse gelding = horse(level, gy, x0 + 2.5, z0 + 8.5, Sex.MALE, PLAIN, false, "GB GELDING");
        if (gelding != null) {
            HorseRecords.apply(gelding, HorseRecords.of(gelding).withGelded(true));
            DebugTestYard.label(gelding, "GB GELDING");     // applying a record clears the label
        }
        DebugYardFertility.inHeat(mare1);
        DebugYardFertility.inHeat(mare2);
        ActionTrace.log("test yard", "GELDING BAND: expect covers only by GB STALLION; any 'natural cover' or"
                + " 'challenged' line naming GB GELDING is a FAIL (gap 229)");
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
        water(level, gy, x0 + 1, z0 + 1);
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

    /** Gap 23: every lethal foal dies despite healing beside water; survivable controls and an adult do not. */
    private static void lethalFoals(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Y_D, "LETHAL FOALS", Blocks.STONE.defaultBlockState(),
                List.of("LETHAL FOALS", "every 20 min: seven", "lethal foals die by", "water; controls live"));
        water(level, gy, x0 + 4, z0 + 1);
        water(level, gy, x0 + 4, z0 + 10);
        Horse adult = horse(level, gy, x0 + 7.5, z0 + 6.5, Sex.FEMALE, "horsegenetics.plod1=ffs/ffs", true,
                "LF ADULT PLOD1");
        wave(level, gy, x0, z0, adult, 1);
    }

    private static final String[] LETHAL = {
            "horsegenetics.plod1=ffs/ffs", "horsegenetics.prkdc=scid/scid", "horsegenetics.gbe1=gbed/gbed",
            "horsegenetics.st14=nfs/nfs", "horsegenetics.rapgef5=efih/efih", "horsegenetics.myo5a=lfs/lfs",
            "horsegenetics.scn4a=H/H"};
    private static final String[] CONTROL = {"horsegenetics.ppib=herda/herda", "horsegenetics.toe1=ca/ca"};

    private static void wave(ServerLevel level, int gy, int x0, int z0, @Nullable Horse adult, int n) {
        DebugYardHerd.after(level, n == 1 ? 60 : 24_000, () -> {
            List<Horse> lethal = new ArrayList<>();
            List<Horse> controls = new ArrayList<>();
            for (int i = 0; i < LETHAL.length; i++) {
                Horse f = foal(level, gy, x0 + 1.5 + i, z0 + 3.5, LETHAL[i], "LF LETHAL " + gene(LETHAL[i]));
                if (f != null) {
                    lethal.add(f);
                }
            }
            for (int i = 0; i < CONTROL.length; i++) {
                Horse f = foal(level, gy, x0 + 2.5 + i * 3, z0 + 8.5, CONTROL[i], "LF CONTROL " + gene(CONTROL[i]));
                if (f != null) {
                    controls.add(f);
                }
            }
            ActionTrace.log("test yard", "LETHAL FOALS wave " + n + ": " + lethal.size() + " lethal foals and "
                    + controls.size() + " controls beside water; expect '[trace] horse died | ... genetic_defect"
                    + " viability=LETHAL_AT_BIRTH (FOAL)' for each lethal one, none for the controls or LF ADULT PLOD1");
            DebugYardHerd.after(level, 1_800, () -> {
                int dead = 0;
                for (Horse f : lethal) {
                    if (!f.isAlive()) {
                        dead++;
                    } else {
                        f.discard();
                    }
                }
                int alive = 0;
                for (Horse f : controls) {
                    if (f.isAlive()) {
                        alive++;
                        f.discard();       // counted; out of the way before the next wave
                    }
                }
                boolean adultAlive = adult != null && adult.isAlive();
                boolean pass = dead == lethal.size() && alive == controls.size() && adultAlive;
                ActionTrace.log("test yard", "LETHAL FOALS wave " + n + " at 90 s: " + dead + "/" + lethal.size()
                        + " lethal foals dead, " + alive + "/" + controls.size() + " controls alive, adult "
                        + (adultAlive ? "alive" : "DEAD") + " - " + (pass ? "PASS" : "FAIL"));
            });
            wave(level, gy, x0, z0, adult, n + 1);
        });
    }

    // ------------------------------------------------------------------
    // Row Z
    // ------------------------------------------------------------------

    private static final String WOLF = "horsegenetics.lycan=Wlf/Wlf";

    /** A night as a wolf gives back the same horse: record, genome, epigenome, bond, age class. */
    private static void lycanRoundTrip(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Z_D, "LYCAN ROUND TRIP", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("LYCAN ROUND TRIP", "two wolf lycans and", "a foal: the same", "horses at dawn"));
        water(level, gy, x0 + 1, z0 + 1);
        bond(horse(level, gy, x0 + 2.5, z0 + 4.5, Sex.MALE, WOLF, true, "LYCAN WOLF STALLION"), 70);
        bond(horse(level, gy, x0 + 6.5, z0 + 4.5, Sex.FEMALE, WOLF, true, "LYCAN WOLF MARE"), 35);
        Horse foal = horse(level, gy, x0 + 4.5, z0 + 7.5, Sex.FEMALE, WOLF, true, "LYCAN WOLF FOAL");
        if (foal != null) {
            foal.setAge(-72_000);    // still a foal through the first night, so the dawn line tests baby=true
        }
        bond(foal, 90);
        ActionTrace.log("test yard", "LYCAN ROUND TRIP: expect, per horse, '[trace] lycan | ... shifted into a"
                + " minecraft:wolf at dusk' and then '... back from a minecraft:wolf at dawn | ... | round trip SAME';"
                + " CHANGED on any of them is a FAIL");
    }

    /** A lycan killed in animal form dies as the horse: a death line, its armour on the ground, nothing left alive. */
    private static void lycanDoomed(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_Z_D, "LYCAN DOOMED", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("LYCAN DOOMED", "an armoured wolf", "lycan, killed at", "night: it drops"));
        Horse h = horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.MALE, WOLF, true, "LYCAN DOOMED");
        if (h != null) {
            h.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR));
        }
        killWhenShifted(level, DebugTestYard.box(x0, gy, z0, x0 + 8, gy + 3, z0 + ROW_Z_D));
    }

    private static void killWhenShifted(ServerLevel level, AABB box) {
        DebugYardHerd.after(level, 100, () -> {
            List<Mob> shifted = shiftedIn(level, box);
            if (shifted.isEmpty()) {
                killWhenShifted(level, box);
                return;
            }
            Mob animal = shifted.get(0);
            ActionTrace.log("test yard", "LYCAN DOOMED: killing the " + animal.getType().getDescriptionId()
                    + " it became");
            animal.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            DebugYardHerd.after(level, 40, () -> {
                int armour = level.getEntitiesOfClass(ItemEntity.class, box.inflate(1.0, 3.0, 1.0),
                        i -> i.getItem().is(Items.IRON_HORSE_ARMOR)).size();
                int horses = level.getEntitiesOfClass(Horse.class, box.inflate(0.0, 2.0, 0.0), Horse::isAlive).size();
                int animals = level.getEntitiesOfClass(Mob.class, box.inflate(0.0, 2.0, 0.0),
                        m -> !(m instanceof Horse) && m.isAlive()).size();
                boolean pass = armour > 0 && horses == 0 && animals == 0;
                ActionTrace.log("test yard", "LYCAN DOOMED at 2 s: iron armour on the ground " + armour
                        + ", live horses " + horses + ", live animals " + animals + " - " + (pass ? "PASS" : "FAIL")
                        + "; also expect '[trace] lycan | ... died as a minecraft:wolf ... round trip SAME' and a"
                        + " '[trace] horse died' line for LYCAN DOOMED");
            });
        });
    }

    /** What a were-cow does with real cows at night: bred, is the calf a plain cow, and does the horse still come back. */
    private static void wereCow(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 17, ROW_Z_D, "WERE-COW", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("WERE-COW", "a cow lycan among", "three cows, put in", "love at night"));
        water(level, gy, x0 + 1, z0 + 1);
        horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.lycan=Cow/Cow", true, "WERE-COW");
        for (int i = 0; i < 3; i++) {
            animal(level, EntityType.COW, gy, x0 + 9.5 + i * 2, z0 + 5.5);
        }
        breedWhenShifted(level, DebugTestYard.box(x0, gy, z0, x0 + 17, gy + 3, z0 + ROW_Z_D));
    }

    private static void breedWhenShifted(ServerLevel level, AABB box) {
        DebugYardHerd.after(level, 100, () -> {
            List<Mob> shifted = shiftedIn(level, box);
            if (shifted.isEmpty() || !(shifted.get(0) instanceof Animal were)) {
                breedWhenShifted(level, box);
                return;
            }
            List<Animal> cows = level.getEntitiesOfClass(Animal.class, box.inflate(0.0, 2.0, 0.0),
                    a -> a != were && !(a instanceof Horse) && a.isAlive() && !a.isBaby());
            if (cows.isEmpty()) {
                ActionTrace.log("test yard", "WERE-COW: no adult cow left to pair with - nothing to test");
                return;
            }
            were.setInLove(null);
            cows.get(0).setInLove(null);
            ActionTrace.log("test yard", "WERE-COW: the were-cow and a cow put in love at night");
            DebugYardHerd.after(level, 1_200, () -> {
                List<Animal> calves = level.getEntitiesOfClass(Animal.class, box.inflate(0.0, 2.0, 0.0),
                        a -> !(a instanceof Horse) && a.isAlive() && a.isBaby());
                long shiftedCalves = calves.stream()
                        .filter(c -> c.getData(ModAttachments.LYCAN_SHIFT.get()).active()).count();
                ActionTrace.log("test yard", "WERE-COW at 60 s: calves " + calves.size() + " (" + shiftedCalves
                        + " carrying a horse - must be 0), were-cow still a cow: " + were.isAlive()
                        + "; at dawn expect '[trace] lycan | ... back from a minecraft:cow ... round trip SAME'");
            });
        });
    }

    // ------------------------------------------------------------------
    // Row AA
    // ------------------------------------------------------------------

    /**
     * Gap 222: horses swing every 10 ticks, so a gladiator lands about two blows to a husk's one. Husks, because
     * they do not burn in the yard's daylight. The plain horse is the control: husks ignore horses, so it should
     * have no {@code [watch] kick} lines at all. Husks are topped back up to two every half day.
     */
    private static void kick(ServerLevel level, int gy, int x0, int z0, String name, String code) {
        pen(level, gy, x0, z0, 9, ROW_AA_D, name, Blocks.STONE.defaultBlockState(),
                List.of(name, code.equals(PLAIN) ? "a plain horse and" : "a gladiator and",
                        "two husks: blows", "every 10 ticks"));
        horse(level, gy, x0 + 4.5, z0 + 3.5, Sex.MALE, code, true, name);
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AA_D);
        husks(level, gy, x0, z0, box, name, 1);
        ActionTrace.log("test yard", name + ": expect '[watch] kick | ... \"" + name + "\" hit ... husk' lines "
                + (code.equals(PLAIN) ? "NEVER - any is a FAIL" : "about 10 ticks apart, and 'horse hurt' from the"
                + " husks about 20 apart (gap 222)"));
    }

    private static void husks(ServerLevel level, int gy, int x0, int z0, AABB box, String name, int wave) {
        int alive = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                box.inflate(0.0, 2.0, 0.0), Mob::isAlive).size();
        for (int i = alive; i < 2; i++) {
            animal(level, EntityType.HUSK, gy, x0 + 2.5 + i * 4, z0 + 9.5);
        }
        if (alive < 2) {
            ActionTrace.log("test yard", name + " husk wave " + wave + ": " + (2 - alive) + " husk(s) in");
        }
        DebugYardHerd.after(level, 12_000, () -> husks(level, gy, x0, z0, box, name, wave + 1));
    }

    /** Waterborn rides at the surface of deep water when grown, and a foal does not (the {@code when: adult} gate). */
    private static void waterborn(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 10, ROW_AA_D, "WATERBORN", Blocks.STONE.defaultBlockState(),
                List.of("WATERBORN", "adult and foal in a", "4-deep pool: the", "adult rides on top"));
        // A pool four deep, walled in stone below the floor so it cannot drain into the fill.
        for (int x = x0 + 1; x <= x0 + 8; x++) {
            for (int z = z0 + 2; z <= z0 + 10; z++) {
                boolean rim = x == x0 + 1 || x == x0 + 8 || z == z0 + 2 || z == z0 + 10;
                for (int y = gy - 4; y <= gy; y++) {
                    level.setBlock(new BlockPos(x, y, z), rim || y == gy - 4
                            ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState(), 3);
                }
            }
        }
        Horse adult = horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.waterborn=Wtb/Wtb", true,
                "WATERBORN ADULT");
        Horse foal = horse(level, gy, x0 + 6.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.waterborn=Wtb/Wtb", true,
                "WATERBORN FOAL");
        if (foal != null) {
            foal.setAge(-72_000);
        }
        Horse control = horse(level, gy, x0 + 4.5, z0 + 8.5, Sex.FEMALE, PLAIN, true, "WATERBORN CONTROL");
        bob(level, gy, new Horse[]{adult, foal, control}, 1);
    }

    private static void bob(ServerLevel level, int gy, Horse[] horses, int n) {
        DebugYardHerd.after(level, n <= 6 ? 200 : 6_000, () -> {
            StringBuilder sb = new StringBuilder();
            for (Horse h : horses) {
                if (h == null || !h.isAlive()) {
                    continue;
                }
                sb.append(sb.length() == 0 ? "" : "; ").append(h.getCustomName() == null ? "?" : h.getCustomName().getString())
                        .append(String.format(" y %+.2f", h.getY() - (gy + 1)))
                        .append(h.isInWater() ? ", in water" : ", dry")
                        .append(h.isUnderWater() ? ", HEAD UNDER" : "")
                        .append(String.format(", air %d/%d", h.getAirSupply(), h.getMaxAirSupply()));
            }
            ActionTrace.log("test yard", "WATERBORN reading " + n + " (y against the pen floor; the water's top is"
                    + " y -0.1): " + sb + " - expect the adult at or above -0.1 with its head out, the foal and the"
                    + " control lower");
            bob(level, gy, horses, n + 1);
        });
    }

    /** Suntouched's light verb is skipped in the horse dimension on purpose: no light block should ever appear here. */
    private static void suntouched(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_AA_D, "SUNTOUCHED", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("SUNTOUCHED", "adult and foal: no", "light blocks, ever,", "in this dimension"),
                Blocks.LIGHT);
        horse(level, gy, x0 + 2.5, z0 + 4.5, Sex.MALE, "horsegenetics.suntouched=Sntch/Sntch", true, "SUNTOUCHED ADULT");
        Horse foal = horse(level, gy, x0 + 5.5, z0 + 7.5, Sex.FEMALE, "horsegenetics.suntouched=Sntch/Sntch", true,
                "SUNTOUCHED FOAL");
        if (foal != null) {
            foal.setAge(-72_000);
        }
        ActionTrace.log("test yard", "SUNTOUCHED: expect the SUNTOUCHED watch line to count minecraft:light 0 at"
                + " every census; any light block is a FAIL");
    }

    private static List<Mob> shiftedIn(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(Mob.class, box.inflate(0.0, 2.0, 0.0),
                m -> !(m instanceof Horse) && m.isAlive() && m.getData(ModAttachments.LYCAN_SHIFT.get()).active());
    }

    private static void bond(@Nullable Horse h, int bond) {
        if (h != null) {
            h.setData(ModAttachments.HORSE_CARE.get(), h.getData(ModAttachments.HORSE_CARE.get()).withBond(bond));
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String name,
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

    private static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         boolean tamed, String label) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, tamed);
        DebugTestYard.label(h, label);
        return h;
    }

    private static @Nullable Horse foal(ServerLevel level, int gy, double x, double z, String code, String label) {
        Horse h = horse(level, gy, x, z, Sex.FEMALE, code, true, label);
        if (h != null) {
            h.setAge(-24_000);
        }
        return h;
    }

    private static void animal(ServerLevel level, EntityType<?> type, int gy, double x, double z) {
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        if (e == null) {
            return;
        }
        e.snapTo(x, gy + 1, z, 0.0F, 0.0F);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(e);
    }

    private static void water(ServerLevel level, int gy, int x, int z) {
        level.setBlock(new BlockPos(x, gy + 1, z),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
    }

    private static String gene(String code) {
        return code.substring(code.indexOf('.') + 1, code.indexOf('=')).toUpperCase(java.util.Locale.ROOT);
    }
}
