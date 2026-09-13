package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_I;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_I_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_K;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_K_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MAX;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>The six genes that could not be tested here until the dimension stopped
 * protecting the horses.</b>
 *
 * <p>Owner, 2026-09-13: <i>"remove the 'no horse damage' exception in the
 * yard."</i> That rule cancelled every {@code LivingIncomingDamageEvent} aimed
 * at a horse in this dimension - it was written when the place was a viewing
 * gallery - and it made six genes untestable in the one dimension built for
 * testing:
 *
 * <ul>
 *   <li><b>Guardian</b> fires on its owner being hurt and retaliates.</li>
 *   <li><b>Gladiator</b> picks fights while ridden.</li>
 *   <li><b>Healer</b> heals horses that are below full - and nothing here could
 *       <i>get</i> below full.</li>
 *   <li><b>Cleansing light</b> damages undead, and the undead were being
 *       deleted by a different rule until yesterday.</li>
 *   <li><b>Ender echo</b> teleports when hit. It was hooked to an event that
 *       never fired.</li>
 *   <li><b>Magic item drop</b> and <b>magic on death</b> both need the horse to
 *       <i>die</i>, which a horse that cannot be damaged never does.</li>
 * </ul>
 *
 * <p>Five separate protections in this dimension have now turned out to be the
 * reason a gene "did nothing" - the mob deleter, the spread verb's early
 * return, the missing night, the dhampir's cancelled sunburn, and this. The
 * shape is always the same and is worth naming once more: <b>a gene that does
 * nothing and a gene that is forbidden from doing anything are identical from
 * inside the game.</b> Nothing on a sign, in a log, or on the horse
 * distinguishes them.
 *
 * <h2>What changes for every other pen</h2>
 * Horses in this dimension can now be killed, by zombies and by falling and by
 * the tester. That is a real cost - the night pens run unattended and a dead
 * subject reports nothing - and it is paid deliberately rather than worked
 * around, because the alternative is six genes that stay unplayed. The two
 * arenas are walled, roofed and at the far end of the yard for that reason, and
 * their spawners are the only monster source inside a pen.
 */
final class DebugYardCombat {

    private DebugYardCombat() {
    }

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        buildEyesightPen(level, gy, cx, mouthZ);
        buildArena(level, gy, cx, mouthZ);
        buildInfirmary(level, gy, cx, mouthZ);
        buildDeathbed(level, gy, cx, mouthZ);
        buildEnderEchoPen(level, gy, cx, mouthZ);
    }

    // ==================================================================
    // ROW I EAST - eyesight
    // ==================================================================

    /**
     * <b>Half in the dark, half in the light, and the census reads the
     * number.</b>
     *
     * <p>Eyesight is a speed modifier gated on {@code dark} - caveborn is
     * faster in it, daywalker is slower - so it is exactly the shape that the
     * weather loci turned out to be: a <b>conditional</b> attribute modifier,
     * invisible to the eye in both states, and reported as broken on 2026-09-13
     * for precisely that reason before an attribute readout settled it.
     *
     * <p>So this pen is built the way that one should have been from the start.
     * The pen's south half is a sealed unlit room and the north half is open
     * yard; a {@code watchAttribute} on each half prints {@code movement_speed}
     * for the horses in it. Two horses, one of each allele, and the pass is a
     * <b>difference between the halves</b> rather than any particular value.
     *
     * <p>The dark half carries the same invisible light-1 blocks the glow room
     * does, which needs saying because it sounds like it defeats the purpose:
     * light level 1 is still {@code dark} for the gene's condition, and it is
     * what stops the room filling with zombies overnight. Without them this pen
     * would measure the speed of a horse being eaten.
     */
    private static void buildEyesightPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MIN + DebugTestYard.BLOCK_W;
        int z0 = mouthZ + ROW_I;
        int z1 = z0 + ROW_I_D;
        int split = z0 + 9;

        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, split);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("EYESIGHT: LIGHT", "the control half.", "Census prints the", "speed of both"));
        DebugTestYard.stock(level, gy, x0 + 4.0, z0 + 4.0, "horsegenetics.eyesight",
                "EYES: CAVEBORN (lit)", 1, 0, "Cav/Cav");
        DebugTestYard.stock(level, gy, x0 + 8.0, z0 + 4.0, "horsegenetics.eyesight",
                "EYES: DAYWALKER (lit)", 1, 0, "Day/Day");
        DebugWorldWatch.watchAttribute("EYESIGHT - LIT",
                DebugTestYard.box(x0, gy, z0, x1, gy + 1, split), Attributes.MOVEMENT_SPEED);

        DebugTestYard.darkRoom(level, gy, x0, x1, split + 3, z1, x0 + 9, true);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 9, gy + 1, split + 2), Direction.NORTH,
                List.of("EYESIGHT: DARK", "same two alleles.", "The DIFFERENCE is", "the gene"));
        DebugTestYard.stock(level, gy, x0 + 4.0, split + 6.0, "horsegenetics.eyesight",
                "EYES: CAVEBORN (dark)", 1, 0, "Cav/Cav");
        DebugTestYard.stock(level, gy, x0 + 8.0, split + 6.0, "horsegenetics.eyesight",
                "EYES: DAYWALKER (dark)", 1, 0, "Day/Day");
        DebugWorldWatch.watchAttribute("EYESIGHT - DARK",
                DebugTestYard.box(x0, gy, split + 3, x1, gy + 1, z1), Attributes.MOVEMENT_SPEED);
    }

    // ==================================================================
    // ROW J WEST - the arena
    // ==================================================================

    /**
     * <b>Guardian and gladiator, one spawner each, side by side but not
     * together.</b>
     *
     * <p>They are the same verb - {@code Temper("aggressive")} - with different
     * triggers, and that is exactly why they must not share a pen: in one pen
     * with one batch of zombies, a dead zombie tells you nothing about which
     * horse killed it.
     *
     * <h2>They want opposite things from you, which I had backwards</h2>
     * <b>Gladiator fights while NOBODY is riding it.</b> Its condition is
     * {@code Flag("has_rider", negate = true)} and its own description says so
     * outright - <i>"while nobody is riding it, the horse attacks hostile mobs
     * that come within reach... mount it and it stops immediately."</i> This
     * file said the reverse for most of a day, the pen's sign told the owner to
     * ride it, and the zombies were taken out of its arena on the strength of
     * that misreading. They are back: an unridden gladiator among hostiles is
     * exactly the test, and the reason it kept dying was the panic bug, not a
     * gene that was switched off.
     *
     * <p><b>Guardian is the one that needs you.</b> It fires on
     * {@code OnOwnerHurt}, so it wants an owner - which is why it arrives
     * untamed - and something hurting that owner. Placed zombies would only
     * kill it while its gene had nothing to react to, so its arena starts
     * empty and the eggs are in the chest.
     *
     * <p>The lesson is cheap and I paid full price for it: <b>the gene's own
     * description is the specification</b>, and it was one line away the whole
     * time.
     */
    private static void buildArena(ServerLevel level, int gy, int cx, int mouthZ) {
        int z0 = mouthZ + ROW_J;
        int z1 = z0 + ROW_J_D;

        int gx0 = cx + WEST_MIN;
        int gx1 = gx0 + 8;
        arenaBox(level, gy, gx0, gx1, z0, z1);
        // UNTAMED, WHICH IS THE OPPOSITE OF EVERY OTHER PEN IN THE YARD - and
        // it is the fix for "guardian does not react to me being damaged at
        // all" (owner, 2026-09-13). The yard stocks its horses TAMED WITH NO
        // OWNER on purpose: DebugPenManager.evacuateTamedHorses walks tamed AND
        // owned horses back to the overworld when you leave, so an owned yard
        // is a yard that empties itself into the portal. That convention
        // silently breaks this one gene, because GeneReactionHandler.onOwnerHurt
        // scans for horses whose owner is the hurt player and a horse with no
        // owner matches nobody.
        //
        // So this one arrives wild and you tame it. That gives it a real owner
        // (tameWithName sets one), which is what the gene needs, and the cost -
        // it follows you home afterwards - is the correct outcome for a horse
        // you actually tamed.
        DebugPenManager.placeSign(level, new BlockPos(gx0 + 4, gy + 1, z0 - 1), Direction.NORTH,
                List.of("GUARDIAN - TAME IT", "it needs a real", "OWNER. Then let a", "zombie hit YOU"));
        Horse guardian = DebugPenManager.spawnHorse(level, gy + 1, gx0 + 4.0, (z0 + z1) / 2.0,
                com.example.horsegenetics.common.horse.Sex.FEMALE,
                "horsegenetics.guardian=Grd/Grd", false);
        DebugTestYard.label(guardian, "GUARDIAN - TAME ME");
        DebugTestYard.saddleAll(level, gy, gx0, gx1, z0, z1);
        DebugWorldWatch.watch("ARENA - GUARDIAN",
                DebugTestYard.box(gx0, gy, z0, gx1, gy + 4, z1), null);

        int lx0 = gx0 + 10;
        int lx1 = cx + WEST_MAX;
        arenaBox(level, gy, lx0, lx1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(lx0 + 4, gy + 1, z0 - 1), Direction.NORTH,
                List.of("GLADIATOR", "LEAVE IT ALONE: it", "fights while NOT", "ridden. Mount=stop"));
        // ARMED THE WAY ITS OWN PAGE SAYS TO ARM IT: "it picks fights, so it
        // takes damage; pair it with magic health and a real attack or expect
        // losses." A bare Gld/Gld horse is a 22-health animal with a 3-damage
        // kick, which is a zombie's exact statline - and the gene is about
        // starting fights, not winning them.
        DebugTestYard.stock(level, gy, lx0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.gladiator",
                "GLADIATOR", 1, 0, "Gld/Gld",
                "horsegenetics.magic_fighter=Gld/Gld-horsegenetics.magic_health=Hardy/Hardy");

        // ONE opponent, not three, and this is the second time I have got the
        // number wrong in the same pen. Three zombies is nine damage a second
        // against twenty-two health: dead inside three seconds of contact,
        // whatever the horse does, and it died eleven seconds after spawning
        // with two zombies still standing. That is not a gene failing and it is
        // not a panic bug either - it is arithmetic, and no amount of attack
        // speed fixes being outnumbered three to one.
        //
        // One is a fight the horse can win and therefore a fight worth
        // watching. More are in the chest, and the choice of how many is the
        // tester's - which is where it should have been from the start.
        arenaOpponents(level, gy, lx0, lx1, z1 - 3, 1);
        DebugTestYard.saddleAll(level, gy, lx0, lx1, z0, z1);
        // The fight's scale is in the tester's hand, not the spawner's - see
        // arenaOpponents. A stick too, because the guardian next door has to be
        // tamed before its gene can see an owner at all.
        DebugYardGameplay.chest(level, gy, gx0 + 1, z0 - 2, "ARENA SUPPLIES", List.of(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, 16),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK, 8),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD, 1),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE, 2),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 8)));
        DebugWorldWatch.watch("ARENA - GLADIATOR",
                DebugTestYard.box(lx0, gy, z0, lx1, gy + 4, z1), null);
    }

    // ==================================================================
    // ROW J EAST - the infirmary
    // ==================================================================

    /**
     * <b>Healer with patients that are actually hurt, and cleansing light with
     * undead to cleanse.</b>
     *
     * <p>Healer's aura only does anything to a horse below full health, so the
     * pen stocks three companions and <b>hurts them at build time</b> - which
     * this dimension made impossible until today. A healer pen full of healthy
     * horses is a pen that measures nothing, and it would have read as the gene
     * being dead.
     *
     * <p>Cleansing light is the mirror: a healing verb pointed at
     * {@code undead}, doing damage rather than healing. It needs zombies, which
     * this dimension deleted outright until 2026-09-13 and which the new biome
     * now supplies - and a spawner here means it does not have to wait for
     * night.
     *
     * <p>Both halves are read off the watch's {@code horse hurt} and
     * {@code horse healed} lines, built for dhampir and doing double duty here.
     * A healer working reads as a run of heals with no damage between them,
     * which is a shape rather than a moment, and the only way to see a shape is
     * to log every frame of it.
     */
    private static void buildInfirmary(ServerLevel level, int gy, int cx, int mouthZ) {
        int z0 = mouthZ + ROW_J;
        int z1 = z0 + ROW_J_D;

        int hx0 = cx + EAST_MIN;
        int hx1 = hx0 + 8;
        DebugTestYard.fencedPlot(level, gy, hx0, hx1, z0, z1);
        // IT HEALS PLAYERS, NOT HORSES, and the first version of this pen had
        // that backwards. HealerGene's aura is Healing("players", ...) - the
        // owner spotted it from the other end within minutes of walking in:
        // "it seems like the healer horses are healing me?" They are, and that
        // is the gene.
        //
        // So the three hurt horses are not the subject, they are the CONTROL,
        // and keeping them is worth more than fixing the sign: if they heal
        // too, matchesHealTarget is not reading the target field and the aura
        // is hitting everything alive in range.
        DebugPenManager.placeSign(level, new BlockPos(hx0 + 4, gy + 1, z0 - 1), Direction.NORTH,
                List.of("HEALER: heals YOU", "hurt yourself, stand", "here. The 3 horses", "must NOT heal"));
        DebugTestYard.stock(level, gy, hx0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.healer",
                "HEALER", 1, 0, "Hlr/Hlr");
        for (int i = 0; i < 3; i++) {
            DebugTestYard.label(DebugPenManager.spawnHorse(level, gy + 1,
                            hx0 + 2.0 + i * 2.0, z0 + 3.0,
                            com.example.horsegenetics.common.horse.Sex.FEMALE,
                            DebugTestYard.PALE, true),
                    "PATIENT " + (i + 1));
        }
        hurtPatients(level, gy, hx0, hx1, z0, z1);
        DebugWorldWatch.watchAttribute("INFIRMARY - HEALER",
                DebugTestYard.box(hx0, gy, z0, hx1, gy + 1, z1), Attributes.MAX_HEALTH);

        // CLEANSING LIGHT IS CONFIRMED and its arena is gone (owner,
        // 2026-09-13: "cleansing light works"). It was built this morning and
        // retired the same afternoon, which is the yard working as intended -
        // a pen for a settled question is the most expensive thing in it.
    }

    /**
     * Take the healer's companions to half health, once, at build time.
     *
     * <p>Half rather than a sliver on purpose: the aura's job is to close a
     * gap, and a horse at one heart is a horse that dies to the first thing
     * that goes wrong overnight - which would look exactly like the healer
     * never firing.
     */
    private static void hurtPatients(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        for (Horse horse : level.getEntitiesOfClass(Horse.class,
                DebugTestYard.box(x0, gy, z0, x1 + 1, gy + 4, z1 + 1))) {
            String name = horse.getCustomName() == null ? "" : horse.getCustomName().getString();
            if (name.startsWith("PATIENT")) {
                horse.setHealth(horse.getMaxHealth() * 0.5F);
            }
        }
    }

    // ==================================================================
    // ROW K WEST - the deathbed
    // ==================================================================

    /**
     * <b>Six horses whose entire gene is what happens when they die.</b>
     *
     * <p>Three drop something ({@code Dia} diamonds, {@code Egg} another egg of
     * itself, {@code Swd} a sword) and three leave something behind
     * ({@code Lav} a lava spring, {@code Wat} water, {@code Xpl} an explosion).
     * A test that needs a horse killed is one no other pen in this yard can
     * host, and the reason is the rule removed today.
     *
     * <p><b>Stone floor, stone walls, and the watch already counts the
     * drops.</b> The floor is stone because two of these six leave fluid on it
     * and a third detonates; grass would burn through into whatever is below
     * and the yard's floor is the only thing between a pen and the void.
     * {@code DebugWorldWatch.onItemJoin} logs every item that appears, so the
     * three dropping alleles answer themselves from the log - what the tester
     * has to judge is the three that do not drop anything, which have to be
     * <i>seen</i>.
     *
     * <p>Each is named for its allele, because six horses in one pen and one
     * sword on the ground is not an answer unless you know which one died.
     */
    private static void buildDeathbed(ServerLevel level, int gy, int cx, int mouthZ) {
        // FIVE OF THE SIX ARE CONFIRMED and their horses are gone. Owner,
        // 2026-09-13: "on death now works perfectly", and the log backs every
        // word of it - an iron_sword and a preset_horse_spawn_egg in the drop
        // list, brick_wall and cobblestone debris where the volatile one went
        // off, and a neighbouring horse killed "from lava" by the spring the
        // next one left behind.
        //
        // The diamond allele is the exception, and only because she ran out of
        // horses: the census read "THE DEATHBED | horses 1" when the killing
        // stopped, so the one still standing was the one that had not been
        // tested. It is not in doubt - it shares its code path with the sword
        // and the egg - it is simply unseen, and unseen is what this yard
        // exists to fix.
        String[][] rows = {
                {"horsegenetics.magic_item_drop", "Dia", "DROPS DIAMOND"}};
        int x0 = cx + WEST_MIN;
        int x1 = cx + WEST_MAX;
        int z0 = mouthZ + ROW_K;
        int z1 = z0 + ROW_K_D;
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, stone);
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("THE DEATHBED", "one left: KILL IT.", "The other five are", "confirmed + gone"));
        // Kept apart: the explosive one must not take the other five with it
        // before anybody has killed them, and a lava spring next to a
        // neighbour is a second death nobody asked for.
        for (int i = 0; i < rows.length; i++) {
            DebugTestYard.stock(level, gy, x0 + 3.0 + (i % 3) * 6.0, z0 + 4.0 + (i / 3) * 7.0,
                    rows[i][0], rows[i][2], 1, 0, rows[i][1] + "/" + rows[i][1]);
        }
        DebugWorldWatch.watch("THE DEATHBED", DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    // ==================================================================
    // ROW K EAST - ender echo
    // ==================================================================

    /**
     * <b>Ender echo: hit it and it should be somewhere else.</b>
     *
     * <p>The kit called this "the likeliest desync in the mod" and put it in a
     * hotbar as a <i>ridden</i> test, which is the sharpest version of it - a
     * teleport under a rider is where client and server disagree visibly. That
     * one still needs a person, so the pen holds a saddled pair.
     *
     * <p>What it adds is the unridden half, which nobody has ever watched: the
     * gene's trigger is {@code OnHurt} with a cooldown, and until today nothing
     * in this dimension could hurt a horse - so the trigger had never fired
     * here at all. A wide open pen, because a sixteen-block teleport inside a
     * nine-block stall either fails or puts the horse through a wall, and both
     * of those read as the gene being broken.
     */
    private static void buildEnderEchoPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MIN + DebugTestYard.BLOCK_W;
        int z0 = mouthZ + ROW_K;
        int z1 = z0 + ROW_K_D;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("ENDER ECHO", "HIT it: it blinks.", "Then RIDE one and", "hit it again"));
        DebugTestYard.stock(level, gy, x0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.ender_echo",
                "ENDER ECHO", 1, 1, "End/End");
        DebugTestYard.saddleAll(level, gy, x0, x1, z0, z1);
        DebugWorldWatch.watch("ENDER ECHO", DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    // ==================================================================
    // Building blocks
    // ==================================================================

    /**
     * A pen with a <b>roof</b> and solid walls, holding its own monsters in.
     *
     * <p>The difference from {@link DebugTestYard#fencedPlot} is the only thing
     * keeping the rest of the yard testable: a zombie spawner behind a
     * one-block fence empties itself into the walkway within a minute, and
     * every unattended pen in the yard then measures a fight instead of a gene.
     * The north wall carries the gate gap so the tester can walk in.
     */
    private static void arenaBox(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z,
                        edge ? wall : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = gy + 1; y <= gy + 4; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? wall : Blocks.AIR.defaultBlockState());
                }
                boolean lamp = !edge && (x - x0) % 6 == 3 && (z - z0) % 6 == 3;
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 5, z),
                        lamp ? Blocks.GLOWSTONE.defaultBlockState() : wall);
            }
        }
        // The way in: a two-block gap in the north wall, on the centre line,
        // where every other pen in the yard puts its gate.
        int doorX = (x0 + x1) / 2;
        for (int y = gy + 1; y <= gy + 2; y++) {
            DebugPenManager.fastSet(level, new BlockPos(doorX, y, z0), Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * A charged zombie spawner. Charged here rather than left empty for the
     * tester to fill: an empty spawner produces nothing, which is
     * indistinguishable from the gene under test having stopped it - the exact
     * confound the ward room was already caught on.
     */
    private static void zombieSpawner(ServerLevel level, int gy, int x, int z) {
        BlockPos pos = new BlockPos(x, gy + 1, z);
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity be) {
            be.setEntityId(EntityType.ZOMBIE, level.getRandom());
            be.setChanged();
        }
    }

    /**
     * <b>NOT CALLED ANY MORE, and the reason is the whole design of these two
     * pens.</b>
     *
     * <p>The arenas were given a charged spawner, then three placed zombies,
     * and the gladiator died to both: <i>"gladiator died to a zombie"</i>, then
     * <i>"the gladiator horse died to the zombie again"</i>. Faster swings did
     * not save it and were never going to, because the horse was not fighting.
     *
     * <p><b>Neither of these genes acts unless the player is engaged.</b>
     * Gladiator's temper is conditioned on {@code has_rider} - it picks fights
     * <em>while ridden</em> and does nothing whatever on its own. Guardian
     * fires on {@code OnOwnerHurt}: no owner, or an owner nobody is hitting,
     * and it never takes a target. So a pen stocked with zombies and left alone
     * is not an arena, it is an execution - the subject cannot defend itself by
     * design, and every reading it produces is of a horse being killed while
     * its gene is switched off.
     *
     * <p>The zombies are in the chest as <b>spawn eggs</b> instead. Mount the
     * gladiator, then make the fight; tame the guardian, stand next to it, then
     * make the fight. That is not a workaround for a pen that cannot hold
     * monsters - it is the only arrangement in which either gene is <em>on</em>.
     *
     * <p>Kept rather than deleted because a pen that wants a bounded, repeatable
     * group of opponents is a reasonable thing to want, and this is how to place
     * one. A spawner is not: it keeps six alive within range and refills them as
     * fast as they die, so one horse is not being tested, it is being counted
     * down.
     */
    private static void arenaOpponents(ServerLevel level, int gy, int x0, int x1, int z, int count) {
        for (int i = 0; i < count; i++) {
            var zombie = EntityType.ZOMBIE.create(level,
                    net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (zombie == null) {
                return;
            }
            zombie.setPos(x0 + 2.0 + i * 2.0, gy + 1, z);
            zombie.setPersistenceRequired();
            level.addFreshEntity(zombie);
        }
    }
}
