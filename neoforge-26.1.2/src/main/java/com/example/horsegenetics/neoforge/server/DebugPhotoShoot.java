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

    /** One picture: a file name, a sex, a genetic code, and a dragon horn form or -1 for any. */
    public record Shot(String name, Sex sex, String code, int dragonForm) {}

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
        HorseRecords.apply(horse, record(horse, shot));
        level.addFreshEntity(horse);
        current = horse;

        // The head is about 1.8 blocks up and a block west of the middle. Stand forward
        // (west), to the south, a little above, and look at it.
        double ex = hx - 3.2;
        double ey = origin.getY() + 2.4;
        double ez = hz + 3.4;
        double tx = hx - 1.0;
        double ty = origin.getY() + 1.8;
        double tz = hz;
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
        Genotype genotype = Genotype.parse(shot.code());
        HorseRecord rec = null;
        for (int attempt = 0; attempt < 200; attempt++) {
            rec = HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), shot.sex(), genotype);
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

    /** Clear the last horse away. */
    public static void finish() {
        if (current != null) {
            current.discard();
            current = null;
        }
    }
}
