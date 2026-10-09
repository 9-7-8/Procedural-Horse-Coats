package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.NeoRng;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * <b>Dev-only: a photo stage for grown parts.</b> Driven by
 * {@code client/DebugPhotoShootClient} when {@code -Dhorsegenetics.photoShoot=true}
 * (gradle {@code -PphotoShoot}), and inert otherwise - nothing else calls it.
 *
 * <p>One horse at a time stands on a white platform high in the sky, AI off and
 * turned side-on, and the player is put at a three-quarter front view of its head.
 * The client then saves a screenshot. Written so a session can show the owner what a
 * part looks like without her spawning it by hand.
 *
 * <p>The shots are {@link #SHOTS}. A dragon horn's form is epigenetic, not genetic,
 * so a shot can ask for one and {@link #record} re-rolls the founder until the
 * expressed copy carries it.
 */
public final class DebugPhotoShoot {

    /**
     * One picture: a file name, a sex, a genetic code, a dragon horn form or -1 for any,
     * and the framing. A code of {@code breed:<id>} rolls a founder of that breed instead,
     * re-rolled until every {@code <key>=<pair>} after a {@code |} is on it - how a shot asks
     * for one strain, or one of a count group's horns.
     */
    public record Shot(String name, Sex sex, String code, int dragonForm, Framing framing,
                       java.util.function.Predicate<HorseRecord> want,
                       java.util.function.Consumer<Horse> dress) {
        public Shot(String name, Sex sex, String code, int dragonForm) {
            this(name, sex, code, dragonForm, Framing.HEAD);
        }

        public Shot(String name, Sex sex, String code, int dragonForm, Framing framing) {
            this(name, sex, code, dragonForm, framing, r -> true);
        }

        public Shot(String name, Sex sex, String code, int dragonForm, Framing framing,
                    java.util.function.Predicate<HorseRecord> want) {
            this(name, sex, code, dragonForm, framing, want, horse -> { });
        }

        /**
         * This shot with something done to the horse once it is standing on the stage -
         * how a picture gets gear on it. {@link DebugPhotoShoot#packs} hangs a chest on
         * each flank; {@link DebugPhotoShoot#facingEast} turns the horse round, so a
         * {@link Framing#SIDE} camera sees its off side instead of its near one.
         */
        public Shot dressed(java.util.function.Consumer<Horse> how) {
            return new Shot(name, sex, code, dragonForm, framing, want, dress.andThen(how));
        }
    }

    /** Hang {@code near} and {@code off} on the horse's flanks; null leaves a flank bare. */
    public static java.util.function.Consumer<Horse> packs(
            net.minecraft.world.item.@Nullable Item near, net.minecraft.world.item.@Nullable Item off) {
        return horse -> {
            if (near != null) {
                com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_LEFT.set(horse,
                        new net.minecraft.world.item.ItemStack(near));
            }
            if (off != null) {
                com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_RIGHT.set(horse,
                        new net.minecraft.world.item.ItemStack(off));
            }
        };
    }

    /** Put a storage harness on the horse, its leather dyed {@code rgb} - or left as it is for -1. */
    public static java.util.function.Consumer<Horse> harness(net.minecraft.world.item.Item harness, int rgb) {
        return horse -> {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(harness);
            if (rgb >= 0) {
                stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                        new net.minecraft.world.item.component.DyedItemColor(rgb));
            }
            com.example.horsegenetics.neoforge.entity.HorseTackSlot.HARNESS.set(horse, stack);
        };
    }

    /** Turn the horse to face east, so the south-side cameras see its off flank. */
    public static java.util.function.Consumer<Horse> facingEast() {
        return horse -> {
            horse.setYRot(-90f);
            horse.setYBodyRot(-90f);
            horse.setYHeadRot(-90f);
        };
    }

    /**
     * Head and shoulders (the default), the whole horse from a little forward of side-on, a
     * whole foal, the back seen from the side (a part along the spine, which WIDE shows
     * end-on), or the back from behind and above (a part standing out from the back or the
     * flanks, with the coat behind it - what a see-through part needs to show it is), or
     * the muzzle close up from three-quarters front (a part of the mouth: tusks, fangs,
     * which HEAD leaves a few pixels across), or the whole head close up from three-quarters
     * front (a part on the skull or the cheek: fins, spikes, a ridge - HEAD shows the whole
     * horse and leaves them a dozen pixels across).
     */
    public enum Framing { HEAD, WIDE, FOAL, SIDE, BACK, MUZZLE, FACE }

    private static final String BAY = "horsegenetics.extension=E/E-horsegenetics.agouti=A/A";
    private static final String BLACK = "horsegenetics.extension=E/E-horsegenetics.agouti=a/a";
    private static final String CHESTNUT = "horsegenetics.extension=e/e-horsegenetics.agouti=A/A";
    private static final String GREY = "horsegenetics.extension=E/E-horsegenetics.agouti=A/A-horsegenetics.grey=G2/G2";

    public static final List<Shot> SHOTS = List.of(
            new Shot("01-unicorn-white", Sex.FEMALE,
                    GREY + "-horsegenetics.unicorn_horn=Horn/Horn", -1),
            new Shot("02-unicorn-red-blue-glow", Sex.MALE,
                    BLACK + "-horsegenetics.unicorn_horn=Horn/Horn-horsegenetics.horn_colour=Red/Blu"
                            + "-horsegenetics.horn_glow=Glw/Glw", -1),
            new Shot("03-dragon-swept-white", Sex.FEMALE,
                    CHESTNUT + "-horsegenetics.dragon_horns=Drg/Drg", 0),
            new Shot("04-dragon-straight-black", Sex.MALE,
                    BAY + "-horsegenetics.dragon_horns=Drg/Drg-horsegenetics.dragon_horn_colour=Blk/Blk", 1),
            new Shot("05-dragon-curled-red-yellow", Sex.MALE,
                    BLACK + "-horsegenetics.dragon_horns=Drg/Drg-horsegenetics.dragon_horn_colour=Red/Yel", 2),
            new Shot("06-dragon-swept-violet-and-unicorn", Sex.FEMALE,
                    CHESTNUT + "-horsegenetics.dragon_horns=Drg/Drg-horsegenetics.dragon_horn_colour=Vio/Vio"
                            + "-horsegenetics.unicorn_horn=Horn/Horn", 0),
            new Shot("07-ram-curl", Sex.MALE,
                    BLACK + "-horsegenetics.ram_horns=Rh/n-horsegenetics.ram_horn_form=Crl/Crl", -1),
            new Shot("08-ram-four-horned-tipped", Sex.MALE,
                    BAY + "-horsegenetics.ram_horns=Rh/n-horsegenetics.ram_horn_form=Fhn/Fhn"
                            + "-horsegenetics.ram_horn_tip=Tip/Tip", -1),
            new Shot("09-ram-corkscrew", Sex.FEMALE,
                    GREY + "-horsegenetics.ram_horns=Rh/n-horsegenetics.ram_horn_form=Crk/Crk", -1),
            new Shot("10-everything-at-once", Sex.MALE,
                    BAY + "-horsegenetics.unicorn_horn=Horn/Horn-horsegenetics.dragon_horns=Drg/Drg"
                            + "-horsegenetics.dragon_horn_colour=Gry/Gry-horsegenetics.ram_horns=Rh/n"
                            + "-horsegenetics.antlers=Ant/n-horsegenetics.horn_colour=Yel/Yel", 1));

    /** Stage floor height. High enough to be over every terrain and under every ceiling. */
    private static final int STAGE_Y = 200;
    private static final int HALF = 6;

    private static @Nullable Horse current;

    private DebugPhotoShoot() {
    }

    /** Build the stage (once) in the player's level and set the sky to noon and clear. */
    public static void buildStage(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockState floor = Blocks.WHITE_CONCRETE.defaultBlockState();
        int cx = player.blockPosition().getX();
        int cz = player.blockPosition().getZ();
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                level.setBlock(new BlockPos(cx + x, STAGE_Y - 1, cz + z), floor, 2);
            }
        }
        origin = new BlockPos(cx, STAGE_Y, cz);
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(), "time set noon");
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(), "weather clear");
    }

    private static BlockPos origin = BlockPos.ZERO;

    /**
     * Shot {@code i}: remove the last horse, stand this one side-on facing west, and put the
     * player at a three-quarter front view of its head.
     */
    public static void stage(ServerPlayer player, int i) {
        ServerLevel level = player.level();
        if (current != null) {
            current.discard();
            current = null;
        }
        Shot shot = SHOTS.get(i);
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            return;
        }
        double hx = origin.getX() + 0.5;
        double hz = origin.getZ() + 0.5;
        horse.setPos(hx, origin.getY(), hz);
        horse.setNoAi(true);
        horse.setYRot(90f);
        horse.setYBodyRot(90f);
        horse.setYHeadRot(90f);
        if (shot.framing() == Framing.FOAL) {
            horse.setAge(-24000);
        }
        HorseRecords.apply(horse, record(horse, shot));
        level.addFreshEntity(horse);
        shot.dress().accept(horse);
        current = horse;

        // A head-and-shoulders portrait. The head is about 1.8 blocks up and a block west
        // of the middle; aim a little below it, at the throat, so the horns have room at
        // the top of the frame and the neck and chest fill the bottom. Stand forward (west),
        // to the south and a little above, about two and a half blocks off. (Owner,
        // 2026-10-02: the first framing stood twice as far back and the gallery's crop
        // cut the bodies off.)
        double tx = hx - 0.8;
        double ty = origin.getY() + 1.45;
        double tz = hz;
        double ex = tx - 1.5;
        double ey = ty + 0.5;
        double ez = tz + 2.0;
        if (shot.framing() == Framing.MUZZLE) {
            // The muzzle is about a block forward (west) of the middle and a block and a
            // half up, pointing forward and down. Stand a block and a half off, forward
            // and to the south, a little below its top so the underside of the jaw shows.
            tx = hx - 1.1;
            ty = origin.getY() + 1.4;
            tz = hz;
            ex = tx - 0.9;
            ey = ty + 0.1;
            ez = tz + 1.1;
        } else if (shot.framing() == Framing.FACE) {
            // The head from ears to muzzle: aim at the eye, a block west of the middle and
            // under two up, and stand a block and a half off, forward and to the south.
            tx = hx - 0.9;
            ty = origin.getY() + 1.85;
            tz = hz;
            ex = tx - 0.9;
            ey = ty + 0.35;
            ez = tz + 1.3;
        } else if (shot.framing() != Framing.HEAD) {
            // The whole horse from a little forward of side-on, for a look that is the
            // whole body (the skeleton's cut-outs). A foal is half the size, so half the way.
            double k = shot.framing() == Framing.FOAL ? 0.6 : 1.0;
            tx = hx - 0.3 * k;
            ty = origin.getY() + 1.05 * k;
            tz = hz;
            ex = tx - 1.2 * k;
            ey = ty + 0.45 * k;
            ez = tz + 2.9 * k;
        }
        if (shot.framing() == Framing.SIDE) {
            // Square to the flank from the south, a little above the line of the back.
            tx = hx + 0.1;
            ty = origin.getY() + 1.3;
            tz = hz;
            ex = tx;
            ey = ty + 0.9;
            ez = tz + 3.4;
        } else if (shot.framing() == Framing.BACK) {
            // Behind the croup (east), to the south and well above, looking down the back.
            tx = hx;
            ty = origin.getY() + 1.4;
            tz = hz;
            ex = tx + 2.4;
            ey = ty + 1.6;
            ez = tz + 1.5;
        }
        double dx = tx - ex;
        double dy = ty - ey;
        double dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(level, ex, ey - 1.62, ez, Set.of(), yaw, pitch, false);
        com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.info("[photo] staged {}", shot.name());
    }

    /** A founder for this shot - re-rolled until a dragon horn form, if one is asked for, is expressed. */
    private static HorseRecord record(Horse horse, Shot shot) {
        if (shot.code().startsWith("breed:")) {
            return breedRecord(horse, shot);
        }
        Genotype genotype = Genotype.parse(shot.code());
        HorseRecord rec = null;
        for (int attempt = 0; attempt < 200; attempt++) {
            rec = HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), shot.sex(), genotype);
            if (!shot.want().test(rec)) {
                continue;
            }
            if (shot.dragonForm() < 0) {
                return rec;
            }
            EpiValues epi = GeneEpigenetics.forGene(Genes.DRAGON_HORNS, rec.genome().genotype(),
                    rec.epigenome()).expressed();
            if (epi.category(com.example.horsegenetics.common.genetics.genes.DragonHornsGene.FORM)
                    == shot.dragonForm()) {
                return rec;
            }
        }
        return rec;
    }

    /** A founder of the shot's breed carrying every pair the shot asks for, or the last try. */
    private static HorseRecord breedRecord(Horse horse, Shot shot) {
        String[] parts = shot.code().substring("breed:".length()).split(java.util.regex.Pattern.quote("|"));
        com.example.horsegenetics.common.breed.Breed breed =
                com.example.horsegenetics.common.breed.Breeds.get(parts[0]);
        // "+key=pair" is stamped onto the roll (BreedFounder's forced pairs, so the breed's
        // other pins and bands still apply); "key=pair" is waited for by re-rolling.
        List<com.example.horsegenetics.common.genetics.AllelePair> forced = new java.util.ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("+")) {
                String[] kv = parts[i].substring(1).split("=");
                com.example.horsegenetics.common.genetics.Gene gene = Genes.byKey(kv[0]);
                String[] ab = kv[1].split("/");
                forced.add(new com.example.horsegenetics.common.genetics.AllelePair(
                        gene.fromToken(ab[0]), gene.fromToken(ab[1])));
            }
        }
        HorseRecord rec = null;
        for (int attempt = 0; attempt < 400; attempt++) {
            NeoRng rng = new NeoRng(horse.getRandom());
            com.example.horsegenetics.common.genetics.Genome genome =
                    com.example.horsegenetics.common.breed.BreedFounder.roll(breed, rng, shot.sex(), forced);
            com.example.horsegenetics.common.name.HorseNameGenerator.NameParts name = HorseRecords.newNameParts(rng);
            rec = HorseRecord.founder(horse.getUUID(), name.first(), name.last(), genome,
                    com.example.horsegenetics.common.breed.BreedLineage.pure(breed.id()).toToken());
            Genotype g = rec.genome().genotype();
            boolean all = true;
            for (int i = 1; i < parts.length && all; i++) {
                if (parts[i].startsWith("+")) {
                    continue;
                }
                String[] kv = parts[i].split("=");
                all = g.pair(Genes.byKey(kv[0])).toTokens().equals(kv[1]);
            }
            if (all) {
                return rec;
            }
        }
        com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.warn("[photo] {}: no founder matched", shot.name());
        return rec;
    }

    /** Clear the last horse away. */
    public static void finish() {
        if (current != null) {
            current.discard();
            current = null;
        }
    }
}
