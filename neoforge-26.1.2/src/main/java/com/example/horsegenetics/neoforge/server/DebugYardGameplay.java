package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_B;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_B_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_C;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_C_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_D_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_E;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_E_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_L;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_L_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_M;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_M_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_N;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_N_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MAX;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>The half of the mod that is items, blocks and people - in the yard at
 * last.</b>
 *
 * <p>Owner, 2026-09-13: <i>"add the yard able and entire item and gameplay
 * layer as well as villagers. Add literally everything that CAN be tested in
 * the yard."</i>
 *
 * <h2>Why none of this was here</h2>
 * Every pen {@link DebugTestYard} had built until now tests a <b>gene</b>, and
 * a gene test is a thing you can walk away from: stock the pen, leave, read the
 * log. The item layer is the opposite - it is thirty-odd items that only do
 * anything when a person is holding them - so it lived in the test kit's
 * hotbar instead, nine slots at a time, and the kit's own legend is full of
 * lines like "hang on a pen wall" and "fill a stall solid" that describe
 * <i>building work</i> the tester has to do first.
 *
 * <p>That is the cost this file removes. Everything those tests need is
 * <b>already placed</b>: the stall that is too small, the stall packed with
 * hay, the workstation with a villager standing at it, the hitch with a cowboy
 * beside it, and a chest of the items within arm's reach of each. Nothing here
 * is cleverer than the kit was. It is the same list with the fetching taken
 * out, which is the only part that was ever expensive.
 *
 * <h2>What a chest is for</h2>
 * A chest rather than a hotbar because the hotbar holds nine things and this
 * layer has far more than nine, and because a chest <b>stays where the test
 * is</b>. The kit's batches were mutually exclusive - taking batch 4 threw away
 * batch 3 - so a question that occurred to you while holding the wrong batch
 * was a question you did not answer.
 */
final class DebugYardGameplay {

    private DebugYardGameplay() {
    }

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        buildTackRoom(level, gy, cx, mouthZ);

        // ROW B EAST IS EMPTY: the horseman's study is gone, confirmed whole.
        // Owner, 2026-09-13: "the horseman and cowboy work totally fine, those
        // were also extensively tested in a previous run", and then "transfer
        // papers have also already been tested" - which is every occupant of
        // that room.
        //
        // The FIRST of those two messages alone would not have been enough. The
        // room also held the PAPERS chest - two Silver papers, where the
        // research shelf must take one and REFUSE the second - and knocking it
        // down for its two confirmed occupants would have taken an unanswered
        // test with it. Check what else is standing in a room before retiring
        // it for the thing you were told about.
        buildTicketStalls(level, gy, cx, mouthZ);
        buildCarrotPens(level, gy, cx, mouthZ);
        buildDairyAndClip(level, gy, cx, mouthZ);
        buildEggLayerPen(level, gy, cx, mouthZ);
        buildFoodPreferencePen(level, gy, cx, mouthZ);
        buildDeepWaterPen(level, gy, cx, mouthZ);
    }

    // ==================================================================
    // ROW B WEST - the tack room
    // ==================================================================

    /**
     * <b>Four chests holding one of everything, because "go and get it" is
     * where an item test dies.</b>
     *
     * <p>This is not a test. It is the thing every other test in this row and
     * the next two needs, and it is here because the alternative is the tester
     * opening creative search thirty times. The fourth chest is worth calling
     * out: it is <b>every food the food-preference locus knows about</b>, in
     * the order the gene lists them, so the pen at the end of this row can be
     * answered by walking over with one chest's contents.
     */
    private static void buildTackRoom(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + WEST_MIN;
        int x1 = cx + WEST_MAX;
        int z0 = mouthZ + ROW_B;
        int z1 = z0 + ROW_B_D;
        // OPEN, not a room. Owner on first sight of it: "the tack room does not
        // need to be an enclosed room" - and she is right, because nothing in
        // it needs containing. A room is for keeping something in or keeping
        // the light out, and five chests want neither; all a door bought was a
        // wall between her and the thing the room exists to hand her.
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 9, gy + 1, z0 - 1), Direction.NORTH,
                List.of("TACK ROOM", "one of every item", "in the mod. Take", "what you need"));

        chest(level, gy, x0 + 2, z0 + 2, "SPAWN EGGS", List.of(
                stack(ModItems.CUSTOM_HORSE_SPAWN_EGG.get(), 1),
                stack(ModItems.PRESET_HORSE_SPAWN_EGG.get(), 1),
                stack(ModItems.BREED_SPAWN_EGG.get(), 1),
                new ItemStack(Items.STICK, 8),
                new ItemStack(Items.SADDLE, 4),
                new ItemStack(Items.NAME_TAG, 8),
                new ItemStack(Items.LEAD, 8),
                new ItemStack(Items.GOLDEN_CARROT, 64)));

        // The whistles need DISTANCE, and the yard's own walkway is the only
        // straight line in the dimension long enough to give it to them: from
        // this door to the back wall is most of two hundred blocks.
        chest(level, gy, x0 + 5, z0 + 2, "WHISTLES + ROPE", List.of(
                stack(ModItems.BASIC_WHISTLE.get(), 1),
                stack(ModItems.GOLDEN_WHISTLE.get(), 1),
                stack(ModItems.ECHO_WHISTLE.get(), 1),
                stack(ModItems.BRAIDED_ROPE.get(), 4),
                stack(ModItems.INTERDIMENSIONAL_TICKET.get(), 4),
                stack(ModItems.BASIC_TICKET.get(), 4),
                stack(ModItems.BLANK_TICKET.get(), 4)));

        chest(level, gy, x0 + 8, z0 + 2, "SEED JARS + HAY", List.of(
                stack(ModItems.EMPTY_SEED_JAR.get(), 4),
                stack(ModItems.STALLION_SEED_JAR.get(), 4),
                stack(ModItems.HORSE_HAIR.get(), 16),
                stack(ModItems.HORSE_HAIR_BUNDLE.get(), 4),
                stack(ModItems.HAIR_CLOTH.get(), 4),
                new ItemStack(Items.HAY_BLOCK, 64),
                new ItemStack(Items.OAK_FENCE, 64),
                new ItemStack(Items.OAK_FENCE_GATE, 16),
                new ItemStack(Items.BONE_MEAL, 64)));

        // EVERY FAVOURITE THE LOCUS HAS, in FoodPreferenceGene's own order.
        // The gene's whole claim is that one of these is special to a given
        // horse and the rest are not, which is only answerable by offering all
        // of them to the same animal.
        chest(level, gy, x0 + 11, z0 + 2, "FEED - all 12 loves", List.of(
                new ItemStack(Items.APPLE, 16),
                new ItemStack(Items.CARROT, 16),
                new ItemStack(Items.SUGAR, 16),
                new ItemStack(Items.MELON_SLICE, 16),
                new ItemStack(Items.BREAD, 16),
                new ItemStack(Items.CAKE, 4),
                new ItemStack(Items.BEETROOT, 16),
                new ItemStack(Items.BAKED_POTATO, 16),
                new ItemStack(Items.COOKED_COD, 16),
                new ItemStack(Items.COOKED_BEEF, 16),
                new ItemStack(Items.SWEET_BERRIES, 16),
                new ItemStack(Items.COCOA_BEANS, 16),
                new ItemStack(Items.WHEAT, 64),
                new ItemStack(Items.GOLDEN_APPLE, 8)));

        chest(level, gy, x0 + 14, z0 + 2, "TOOLS", List.of(
                new ItemStack(Items.SHEARS, 1),
                new ItemStack(Items.BUCKET, 4),
                new ItemStack(Items.GLASS_BOTTLE, 16),
                new ItemStack(Items.BOOK, 16),
                new ItemStack(Items.CLOCK, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.WATER_BUCKET, 2),
                new ItemStack(Items.TORCH, 32)));
    }

    // ==================================================================
    // ROW C WEST - the ticket stalls
    // ==================================================================

    /**
     * <b>Five enclosures, each one a case the ticket items have to get right -
     * and the building work is already done.</b>
     *
     * <p>The test kit's batch 3 hands you the signs, the tickets, sixty-four
     * fences and sixty-four hay bales, with a legend reading "fill a stall
     * solid", "even L-shaped or narrower than the horse". Every one of those is
     * an <i>instruction to build something</i> before the item can be tried,
     * and the awkward shapes are exactly the ones nobody builds twice.
     *
     * <p>So they are standing here:
     * <ol>
     *   <li><b>A plain holding pen</b> - the control. A ticket used on a horse
     *       you own should land it dead centre.</li>
     *   <li><b>A stall exactly one block wider than a horse</b> - the tight
     *       fit, where a centring bug shows as a horse in a wall.</li>
     *   <li><b>An L-shaped stall</b> - "dead centre" of an L is not the centre
     *       of its bounding box, and that is the whole point of it.</li>
     *   <li><b>A stall packed solid with hay</b> - the ticket must
     *       <b>refuse</b>, and must not be consumed doing it. A ticket spent on
     *       a failed teleport is the worst outcome here, because it looks like
     *       it worked.</li>
     *   <li><b>A roofed stall</b> - the bind message quotes a height, and a
     *       stall with a lid is the only way to know whether the number is the
     *       real one or the sky.</li>
     * </ol>
     */
    private static void buildTicketStalls(ServerLevel level, int gy, int cx, int mouthZ) {
        int z0 = mouthZ + ROW_C;
        int z1 = z0 + ROW_C_D;

        // THE HOLDING PEN keeps the west block to itself. It is the control -
        // a plain fenced ring, nothing awkward about it - and the one thing a
        // holding-pen ticket needs is somewhere unambiguous to land.
        int hx0 = cx + WEST_MIN;
        int hx1 = cx + WEST_MAX;
        DebugTestYard.fencedPlot(level, gy, hx0, hx1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(hx0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("HOLDING PEN", "hang the SIGN on", "a wall, then use", "a pen TICKET"));
        chest(level, gy, hx0 + 1, z0 - 2, "PEN SIGNS", List.of(
                stack(ModItems.HOLDING_PEN_SIGN.get(), 4),
                stack(ModItems.HOLDING_PEN_TICKET.get(), 16),
                stack(ModItems.STALL_SIGN.get(), 8),
                stack(ModItems.BOUND_STALL_SIGN.get(), 2),
                stack(ModItems.BOUND_TICKET.get(), 16),
                new ItemStack(Items.STICK, 8),
                new ItemStack(Items.HAY_BLOCK, 64)));
        // UNTAMED, for the same reason the guardian is. Owner, 2026-09-13: the
        // bound ticket answered "That is not your horse." It was not - the yard
        // stocks TAMED WITH NO OWNER so that leaving the dimension does not walk
        // the whole yard home with you, and every ticket in this mod checks
        // OWNERSHIP rather than tameness. That convention has now broken two
        // separate features this way, which makes it worth stating plainly: in
        // this yard, anything gated on "your horse" must arrive wild.
        DebugTestYard.label(DebugPenManager.spawnHorse(level, gy + 1, hx0 + 6.0, (z0 + z1) / 2.0,
                Sex.FEMALE, DebugTestYard.PALE, false), "TICKET MARE - TAME ME");

        // ROW C EAST IS EMPTY: the four awkward stalls are confirmed and gone.
        // Owner, 2026-09-13: "all the stalls that should work, do." The tight
        // and L-shaped stalls bind and land a horse dead centre with the sign
        // hung outside (it used to measure the sign's own top as a one-tile
        // room - gap 223), the roofed stall does the same, and the hay-packed
        // stall refuses the sign because it has no floor. The holding pen above
        // stays: nobody has tested it yet, and the ticket mare is its horse.
    }

    // ==================================================================
    // ROWS L, M and N - one breeding pair per carrot
    // ==================================================================

    /** One carrot: the item, the short name on the sign, and what it claims to do. */
    private record Splice(java.util.function.Supplier<net.minecraft.world.item.Item> item,
                          String name, String claim) {
    }

    /**
     * <b>Ten carrots, ten pairs of pens, and nothing shared between them.</b>
     *
     * <p>Owner, 2026-09-13: <i>"we need to structure the gene splice carrots
     * better so that it's two horses to breed, and one small pair of pens per
     * carrot."</i>
     *
     * <p>What this replaces was one pen holding four horses and a chest with
     * every carrot in it, and it could not have answered a single question.
     * <b>A splice carrot does nothing to the horse that eats it</b> - it biases
     * the gamete that parent contributes, so the whole result lives in a
     * <i>foal</i>. That makes three things mandatory, and the bench had none of
     * them: a known mare, a known stallion, and certainty about which carrot
     * went into which parent. Feed two different carrots and the horse carries
     * both ({@code ArmedCarrotsAttachment.plus}), so the foal cannot tell you
     * which one it came from - and with eleven carrots in one chest beside four
     * horses, that is the likely outcome rather than a corner case.
     *
     * <h2>Why a divided pen rather than one pen or two</h2>
     * It was built when a carrot put its eater straight into breeding mode, so
     * two horses in one pen bred the moment the second was fed. A carrot only
     * arms the horse now (2026-09-13), and breeding is a golden carrot on both
     * with the mare in heat - but keeping them apart still means knowing
     * exactly which one ate what.
     *
     * <p>So: one enclosure, a fence down the middle, and a <b>gate in it</b>.
     * Mare on the left, stallion on the right: feed the carrot to whichever you
     * mean to, clock the mare into heat, open the gate, and golden-carrot both.
     * The foal comes when the pregnancy ends. The pair is small on purpose -
     * these horses are being fed and bred, never chased, and a big paddock only
     * means walking after them.
     */
    private static void buildCarrotPens(ServerLevel level, int gy, int cx, int mouthZ) {
        List<Splice> carrots = List.of(
                new Splice(ModItems.UNKNOWN_GENE_SPLICE_CARROT, "UNKNOWN GENE", "any locus, unseen"),
                new Splice(ModItems.KNOWN_GENE_SPLICE_CARROT, "KNOWN GENE", "the locus you chose"),
                new Splice(ModItems.MARKING_GENE_SPLICE_CARROT, "MARKING", "marking loci only"),
                new Splice(ModItems.DILUTION_GENE_SPLICE_CARROT, "DILUTION", "dilution loci only"),
                new Splice(ModItems.WHITE_GENE_SPLICE_CARROT, "WHITE", "white loci only"),
                new Splice(ModItems.PERFORMANCE_GENE_SPLICE_CARROT, "PERFORMANCE", "performance loci"),
                new Splice(ModItems.MAGICAL_GENE_SPLICE_CARROT, "MAGICAL", "magical loci only"),
                new Splice(ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT, "EPIGENETIC", "rerolls the epigenome"),
                new Splice(ModItems.STABILIZER_CARROT, "STABILIZER", "favours one copy"),
                new Splice(ModItems.MAGNIFIER_CARROT, "MAGNIFIER", "favours the other"));

        int[][] slots = {
                {cx + WEST_MIN, mouthZ + ROW_L, ROW_L_D}, {cx + WEST_MIN + 10, mouthZ + ROW_L, ROW_L_D},
                {cx + EAST_MIN, mouthZ + ROW_L, ROW_L_D}, {cx + EAST_MIN + 10, mouthZ + ROW_L, ROW_L_D},
                {cx + WEST_MIN, mouthZ + ROW_M, ROW_M_D}, {cx + WEST_MIN + 10, mouthZ + ROW_M, ROW_M_D},
                {cx + EAST_MIN, mouthZ + ROW_M, ROW_M_D}, {cx + EAST_MIN + 10, mouthZ + ROW_M, ROW_M_D},
                {cx + WEST_MIN, mouthZ + ROW_N, ROW_N_D}, {cx + WEST_MIN + 10, mouthZ + ROW_N, ROW_N_D}};

        for (int i = 0; i < carrots.size() && i < slots.length; i++) {
            splicePair(level, gy, carrots.get(i), slots[i][0], slots[i][1], slots[i][2]);
        }
    }

    /** The pen width of one pair: two halves of three, plus the divider and two walls. */
    private static final int PAIR_W = 8;

    private static void splicePair(ServerLevel level, int gy, Splice carrot, int x0, int z0, int depth) {
        int x1 = x0 + PAIR_W;
        int z1 = z0 + depth;
        int mid = x0 + 4;

        // A GATE INTO EACH HALF, side by side. Owner, 2026-09-13: "you need to
        // put two gates adjacent to each other for the breeding horse pens."
        // penWalls cuts ONE two-wide gate in the middle of the north wall, and
        // the middle of this pen is the divider - so the single entrance
        // straddled the fence and opened into the west half only. The stallion
        // was behind a wall with no door.
        DebugPenManager.penWalls(level, gy + 1, x0, x1, z0, z1, x0 + 1, z0, Direction.NORTH);
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, Direction.NORTH);
        for (int x = x0 + 5; x <= x0 + 6; x++) {
            level.setBlockAndUpdate(new BlockPos(x, gy + 1, z0), gate);
        }

        // The divider, and the gate pair that makes this one pen rather than
        // two. TWO gates wide for the same reason the outer ones are: a horse
        // is 1.4 blocks across, and a single-block opening it has to be led
        // through is the difference between "open the gate and breed them" and
        // "spend five minutes shoving a horse at a gap".
        int gateZ = (z0 + z1) / 2;
        BlockState divider = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, Direction.EAST);
        for (int z = z0 + 1; z < z1; z++) {
            boolean isGate = z == gateZ || z == gateZ + 1;
            if (isGate) {
                level.setBlockAndUpdate(new BlockPos(mid, gy + 1, z), divider);
            } else {
                DebugPenManager.fastSet(level, new BlockPos(mid, gy + 1, z),
                        Blocks.OAK_FENCE.defaultBlockState());
            }
        }

        // Since 2026-09-13 a carrot only ARMS the horse, the mare has to be in
        // heat, and breeding makes a pregnancy - so the sign is three steps, and
        // the chest has the clock that puts her in heat.
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of(carrot.name(), carrot.claim(), "carrot ONE, clock", "mare, gold both"));

        DebugTestYard.stock(level, gy, x0 + 2.0, gateZ - 2.0,
                "horsegenetics.extension", carrot.name() + " MARE", 1, 0, "E/e");
        DebugTestYard.stock(level, gy, x0 + 6.0, gateZ - 2.0,
                "horsegenetics.extension", carrot.name() + " STUD", 0, 1, "E/e");

        chest(level, gy, x0 + 5, z0 - 2, carrot.name(), List.of(
                stack(carrot.item().get(), 16),
                new ItemStack(Items.GOLDEN_CARROT, 16),
                new ItemStack(Items.CLOCK),
                new ItemStack(Items.STICK, 4)));
    }

    // ==================================================================
    // ROW D WEST - the dairy and the clip
    // ==================================================================

    /**
     * <b>The three milk refusals, and the horse-hair chain.</b>
     *
     * <p>Potion milk is the one place in the mod where <i>nothing happening</i>
     * is the correct behaviour three times over, which makes it exceptionally
     * easy to pass by accident. A hurt mare must refuse and say so; a stallion
     * must rear and say so; a foal has nothing to give. All three of those look
     * identical to a gene that was never registered - so the message is the
     * evidence, not the absence of a potion.
     *
     * <p>The mare is stocked and then <b>hurt</b>, here, at build time. That
     * was impossible until today: the dimension cancelled all horse damage, so
     * "hurt her, then bottle her" could not be set up inside the one dimension
     * built for setting things up.
     */
    private static void buildDairyAndClip(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + WEST_MIN;
        int x1 = cx + WEST_MAX;
        int z0 = mouthZ + ROW_D;
        int z1 = z0 + ROW_D_D;
        // STONE, NOT GRASS, AND THAT IS THE WHOLE TEST. Owner, 2026-09-13: "I
        // was able to milk the injured horse I think, or it healed before I got
        // there." It healed. HorseCareHandler's gated regen needs a food block
        // and a water block near the horse, and the yard's floor is grass -
        // which is in the HORSE_FOOD tag - so the mare this pen hurts at build
        // time was back to full within a minute or two, every time, and "a hurt
        // mare refuses milk" could never be reached.
        //
        // A stone floor shuts the gate. No new machinery and no debug damager:
        // the injury simply stays put, which is what the pen always assumed.
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DAIRY + CLIP", "3 milk refusals.", "Stone floor: the", "mare STAYS hurt"));
        chest(level, gy, x0 + 1, z0 - 2, "BOTTLES + SHEARS", List.of(
                new ItemStack(Items.GLASS_BOTTLE, 32),
                new ItemStack(Items.BUCKET, 4),
                new ItemStack(Items.SHEARS, 2),
                new ItemStack(Items.STICK, 4),
                new ItemStack(Items.IRON_SWORD, 1),
                stack(ModItems.HORSE_HAIR.get(), 8),
                stack(ModItems.HORSE_HAIR_BUNDLE.get(), 4),
                stack(ModItems.HAIR_CLOTH.get(), 4),
                stack(ModItems.BRAIDED_ROPE.get(), 4)));

        double mid = (z0 + z1) / 2.0;
        DebugTestYard.stock(level, gy, x0 + 5.0, mid, "horsegenetics.potion_milk",
                "MILK: HURT MARE", 1, 0, "Spd/Spd");
        DebugTestYard.stock(level, gy, x0 + 8.0, mid, "horsegenetics.potion_milk",
                "MILK: STALLION", 0, 1, "Spd/Str");
        DebugTestYard.stock(level, gy, x0 + 11.0, mid, "horsegenetics.potion_milk",
                "MILK: FOAL", 1, 0, "Spd/Spd");
        // Two plain horses to shear. Nothing names a locus: horse hair comes
        // off any horse, and stocking one with a coat gene would only invite
        // the question of whether the gene was why.
        DebugTestYard.label(DebugPenManager.spawnHorse(level, gy + 1, x0 + 14.0, mid,
                Sex.FEMALE, DebugTestYard.PALE, true), "SHEAR ME 1");
        DebugTestYard.label(DebugPenManager.spawnHorse(level, gy + 1, x0 + 16.0, mid,
                Sex.MALE, DebugTestYard.PALE, true), "SHEAR ME 2");

        hurtAndAge(level, gy, x0, x1, z0, z1);
        DebugWorldWatch.watch("DAIRY", DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    /**
     * Hurt the mare and turn the foal back into one.
     *
     * <p>Both are set-up rather than test: the refusal messages are gated on
     * <i>hurt</i> and on <i>baby</i>, and a pen whose three horses are all
     * healthy adults tests one case instead of three. The mare is taken to
     * about half health - enough that the gate is unambiguously closed, not so
     * little that a passing heal opens it again.
     */
    private static void hurtAndAge(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        for (var horse : level.getEntitiesOfClass(net.minecraft.world.entity.animal.equine.Horse.class,
                DebugTestYard.box(x0, gy, z0, x1 + 1, gy + 4, z1 + 1))) {
            String name = horse.getCustomName() == null ? "" : horse.getCustomName().getString();
            if ("MILK: HURT MARE".equals(name)) {
                horse.setHealth(horse.getMaxHealth() * 0.5F);
            } else if ("MILK: FOAL".equals(name)) {
                horse.setBaby(true);
            }
        }
    }

    // ==================================================================
    // ROW D EAST - the egg layer
    // ==================================================================

    /**
     * <b>All eight laying alleles at once, because the watch counts the item
     * and not the horse.</b>
     *
     * <p>Egg layer is a matched-pair locus - two of the same allele or nothing
     * at all - and each pair drops a <i>different item</i>. That is what makes
     * eight horses in one pen legitimate rather than a confound: a feather on
     * the ground can only have come from the feather horse, so the pen
     * separates itself and the {@code item join} lines in the log are the
     * whole result.
     *
     * <p>It also answers the cap for free. {@code NEARBY_CAP} says a layer
     * stops bothering when enough is already lying about, and eight layers in
     * one pen with nobody picking anything up is precisely the condition that
     * should trip it. A pen that fills and then <b>stops</b> is the pass; a pen
     * that fills for ever is the cap not working.
     */
    private static void buildEggLayerPen(ServerLevel level, int gy, int cx, int mouthZ) {
        // ALL EIGHT ALLELES ARE CONFIRMED. The log caught every one of them
        // dropping its own item - leather, wool, bone, slime, ink, feather,
        // string and egg - so the "does each allele produce its own thing"
        // half is finished and its eight horses are gone.
        //
        // WHAT IS LEFT IS THE CAP, AND THE OLD PEN COULD NOT TEST IT. I said
        // it was close to answerable and that was wrong: NEARBY_CAP counts
        // items within six blocks OF EACH LAYING HORSE, and eight horses spread
        // over a nineteen-wide pen each have their own neighbourhood, so a pen
        // total of eleven is perfectly legal and proves nothing. What bounded
        // that pen was the 6000-tick despawn timer, exactly as gap 207
        // predicted.
        //
        // Four layers of the SAME allele, packed into one corner so their
        // six-block circles overlap almost completely. Now the pen total IS the
        // local count, the cap is reachable, and a floor that fills and then
        // STOPS is the pass.
        String[][] lays = {
                {"Egg", "EGGS 1"}, {"Egg", "EGGS 2"}, {"Egg", "EGGS 3"}, {"Egg", "EGGS 4"}};
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MIN + DebugTestYard.BLOCK_W;
        int z0 = mouthZ + ROW_D;
        int z1 = z0 + ROW_D_D;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("EGG LAYER: THE CAP", "4 layers, ONE spot.", "All 8 alleles are", "confirmed already"));
        for (int i = 0; i < lays.length; i++) {
            DebugTestYard.stock(level, gy, x0 + 3.0 + (i % 2) * 2.0, z0 + 3.0 + (i / 2) * 2.0,
                    "horsegenetics.egg_layer", "LAYS " + lays[i][1], 1, 0,
                    lays[i][0] + "/" + lays[i][0]);
        }
        DebugWorldWatch.watch("EGG LAYER", DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    // ==================================================================
    // ROW E EAST - food preference
    // ==================================================================

    /**
     * <b>Four favourites, named on the horse, with the feed chest one row
     * north.</b>
     *
     * <p>The kit's own legend for this gene is the reason it is here: "offer it
     * everything: only carrots. <i>Silently does nothing if another mod took
     * the event first.</i>" That is a failure with no symptom - the horse
     * accepts the food the ordinary way and nothing says the locus was skipped
     * - so the test is a <i>comparison</i>, and a comparison needs more than
     * one horse.
     *
     * <p>Four different favourites in one pen, each wearing the name of what it
     * loves. Feed all four the same apple: exactly one should react. One
     * reacting is the gene; none reacting is the event being eaten; all four
     * reacting is the favourite not being read at all.
     */
    private static void buildFoodPreferencePen(ServerLevel level, int gy, int cx, int mouthZ) {
        String[][] loves = {{"App", "LOVES APPLES"}, {"Sug", "LOVES SUGAR"},
                {"Cke", "LOVES CAKE"}, {"Bef", "LOVES STEAK"}};
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MIN + DebugTestYard.BLOCK_W;
        int z0 = mouthZ + ROW_E;
        int z1 = z0 + ROW_E_D;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 3, gy + 1, z0 - 1), Direction.NORTH,
                List.of("FOOD PREFERENCE", "4 loves, 1 pen.", "Feed all four the", "SAME thing"));
        for (int i = 0; i < loves.length; i++) {
            DebugTestYard.stock(level, gy, x0 + 3.0 + i * 4.0, (z0 + z1) / 2.0,
                    "horsegenetics.food_preference", loves[i][1], 1, 0,
                    loves[i][0] + "/" + loves[i][0]);
        }
        chest(level, gy, x0 + 1, z0 - 2, "THE SAME FOUR", List.of(
                new ItemStack(Items.APPLE, 16),
                new ItemStack(Items.SUGAR, 16),
                new ItemStack(Items.CAKE, 4),
                new ItemStack(Items.COOKED_BEEF, 16),
                new ItemStack(Items.WHEAT, 32)));
    }

    // ==================================================================
    // ROW E WEST - the pool
    // ==================================================================

    /**
     * <b>Somewhere deep enough to drown in, which the yard did not have.</b>
     *
     * <p>Ocean-born grants {@code underwater_breathing} to the horse <i>and</i>
     * its rider, and it is the one rider-immunity gene that has never been
     * played. The kit has handed out a water bucket for it twice; a bucket
     * makes a puddle, and a puddle cannot test breathing.
     *
     * <p>Five blocks of water in a stone tank, with a saddled pair already in
     * it. Five because a mounted rider's head sits about three and a half
     * blocks up and the test is whether the <b>air bar moves</b> - at three
     * deep the rider is simply standing in a pond and the answer is yes for
     * reasons that have nothing to do with the gene.
     *
     * <p>Stone rather than the yard's grass: this is the only body of water in
     * the dimension and water on a grass floor with void underneath is a leak
     * waiting for the first block anybody breaks.
     */
    private static void buildDeepWaterPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + WEST_MIN + 6;
        int x1 = cx + WEST_MAX;
        int z0 = mouthZ + ROW_E;
        int z1 = z0 + ROW_E_D;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z, wall);
                for (int y = gy + 1; y <= gy + 5; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? wall : Blocks.WATER.defaultBlockState());
                }
            }
        }
        // NO DOORWAY. Every other pen in the yard has a gate cut in its north
        // wall and this one must not: a gap anywhere below the surface drains
        // the tank into the walkway, and the yard's floor is the only thing
        // between here and the void. The tank is open at the top and the
        // tester is in creative, which is the way in.
        int signX = (x0 + x1) / 2;
        DebugPenManager.placeSign(level, new BlockPos(signX, gy + 1, z0 - 1), Direction.NORTH,
                List.of("OCEAN-BORN", "5 deep, open top.", "RIDE it under: does", "YOUR air bar move?"));
        DebugTestYard.stock(level, gy, x0 + 3.0, (z0 + z1) / 2.0, "horsegenetics.ocean_born",
                "OCEAN-BORN", 1, 1, "Ocn/Ocn");
        DebugTestYard.saddleAll(level, gy, x0, x1, z0, z1);
    }

    // ==================================================================
    // Building blocks
    // ==================================================================

    /**
     * <b>A lit, roofed room.</b> Unlike {@link DebugTestYard#darkRoom} this one
     * wants to be seen inside: the two rooms in row B are read rather than
     * measured, and a chest you cannot find is a chest you do not open.
     *
     * <p>The glowstone is in the <i>ceiling</i> and the room is sealed, which
     * matters more than it sounds now that the dimension has a biome that
     * spawns zombies: an unlit enclosed box in this yard fills up overnight.
     */
    /**
     * <b>Four walls, no roof and no way through them.</b> A villager pen: the
     * walls are too high to jump and there is no door to open, and the missing
     * roof is the door - for a player in creative, which is the only kind of
     * visitor this yard has.
     *
     * <p>Lit from the inside at head height rather than by the yard's own grid,
     * because the walls are five tall and would shade their own floor.
     */
    private static void openPen(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z,
                        edge ? wall : Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = gy + 1; y <= gy + 5; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockState lamp = Blocks.LIGHT.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
        for (int x = x0 + 3; x < x1; x += 4) {
            for (int z = z0 + 3; z < z1; z += 4) {
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 3, z), lamp);
            }
        }
    }

    private static void hall(ServerLevel level, int gy, int x0, int x1, int z0, int z1, int doorX) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z, edge ? wall : floor);
                for (int y = gy + 1; y <= gy + 4; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? wall : Blocks.AIR.defaultBlockState());
                }
                boolean lamp = !edge && (x - x0) % 5 == 2 && (z - z0) % 5 == 2;
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 5, z),
                        lamp ? Blocks.GLOWSTONE.defaultBlockState() : wall);
            }
        }
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(new BlockPos(doorX, gy + 1, z0), lower);
        level.setBlockAndUpdate(new BlockPos(doorX, gy + 2, z0), upper);
        level.setBlockAndUpdate(new BlockPos(doorX + 1, gy + 1, z0), lower);
        level.setBlockAndUpdate(new BlockPos(doorX + 1, gy + 2, z0), upper);
    }


    /**
     * <b>A chest, filled, with its purpose written beside it.</b>
     *
     * <p><b>Never put one inside a fenced pen.</b> A pen wall is one block
     * high, a chest is another, and a horse will stand on the chest and step
     * over the wall - which the owner found within minutes of the first build:
     * <i>"you put a chest near a wall, and the horses jumped on it to
     * escape."</i> Every chest that serves a pen sits in the <b>aisle</b> north
     * of it now, beside the sign, which is better on both counts: it is also
     * reachable without opening the gate, and a gate opened to fetch something
     * is a gate somebody forgets to close.
     *
     * <p>A null or absent item is skipped rather than fatal: the yard is built
     * from a list of registry lookups and one missing item must not cost the
     * other thirty-five. That has already happened once in this file's history,
     * with a gene, which is why {@code stock} logs and carries on too.
     */
    static void chest(ServerLevel level, int gy, int x, int z, String label,
                              List<ItemStack> contents) {
        BlockPos pos = new BlockPos(x, gy + 1, z);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH), 3);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity be) {
            int slot = 0;
            for (ItemStack stack : contents) {
                if (stack == null || stack.isEmpty() || slot >= be.getContainerSize()) {
                    continue;
                }
                be.setItem(slot++, stack);
            }
            be.setChanged();
        }
        // BESIDE the chest, not on top of it. A standing sign needs a sturdy
        // block under it and a chest is not one, so a sign placed on the lid
        // pops off at the first block update - which is to say every label in
        // this file would have been an item on the floor by the time anybody
        // walked in. Every caller leaves x+1 clear for it.
        DebugPenManager.placeSign(level, new BlockPos(x + 1, gy + 1, z), Direction.NORTH,
                List.of(label, "", "", ""));
    }

    /** An item stack, or an empty one when this build has no such item. */
    private static ItemStack stack(@Nullable net.minecraft.world.item.Item item, int count) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }
}
