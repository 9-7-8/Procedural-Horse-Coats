package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.common.stable.StallFill;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.DoubleFenceGateBlock;
import com.example.horsegenetics.neoforge.block.DoubleGates;
import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <b>Row AX: the research shelf, and the stall detector's shapes</b> (2026-10-02). Two pens, no horse in either and
 * no hands that have to be in the level: every check drives a block entity or a static detector directly, on the yard
 * clock, and ends in one {@code CLOCKWORK} line.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>footprint</th><th>answers by</th></tr>
 *   <tr><td>west</td><td>RESEARCH SHELF</td><td>x0+2..x0+18, z0+1..z0+5</td><td>2 s (filing, break) to
 *       3 x copy time + 5 s (copying; 35 s for a common gene)</td></tr>
 *   <tr><td>east</td><td>STALL SHAPES</td><td>x0+25..x0+43, z0..z0+12</td><td>2 s and 3 s</td></tr>
 * </table>
 *
 * <h2>RESEARCH SHELF (west)</h2>
 * {@code wiki/item-research-shelf.html}, open check 143: <i>"the whole storage layer - file, withdraw, copy,
 * drop-on-break, save and reload - is unexercised ... A save-and-reload is the single most valuable thing to try: the
 * contents are a gene-key set on the block entity, written through ValueOutput.store, and nothing has round-tripped
 * it."</i> The screen half of that check (whether {@code Slot.isActive} gates hit-testing, ShelfOpenSync's timing) needs
 * a client and is not touched here; the storage half is all of this pen. Five shelves, each placed with
 * {@code setBlock} so the block's own ticker runs them, and filled through the block entity's containers
 * ({@code papers()}, {@code book()}, {@code result()}, {@code select()}) - exactly the containers the menu's slots
 * wrap, so what is read is what a player's clicks would have left.
 *
 * <p><b>1. Save and reload</b> (check 143). A shelf holds Agouti A/a in slot 0, Agouti A/A in slot 7 and Extension E/e
 * in slot 53, three books, and has A/a picked. At 1.5 x the copy time - one copy finished, the second half done - it
 * is saved exactly as the chunk saver saves it ({@code LevelChunk.getBlockEntityNbtForSaving} calls
 * {@code saveWithFullMetadata(registryAccess)}, read in the 26.1.2 sources 2026-10-02), the tag is written to bytes and
 * read back with {@code NbtIo} the way a region file holds it, and loaded with {@code BlockEntity.loadStatic(pos,
 * state, tag, registries)} - the call {@code SerializableChunkData} makes when a chunk loads - at a different position.
 * <b>PASS:</b> all 54 paper slots, the book slot and the result slot come back the same item, components and count,
 * and the pick and the progress are equal. <b>FAIL:</b> any of them differs, or the load returns no shelf.
 * INCONCLUSIVE if the source shelf is gone or had not filed the three papers.
 *
 * <p><b>2. Filing by pair.</b> {@code item-research-shelf.html}, "Filing by pair rather than by gene": <i>"Two papers
 * for one locus - say Agouti: A/n and Agouti: A/A - should both file and show as two rows; a duplicate of either
 * should still refuse the slot."</i> (and the same on {@code carrots.html}). Agouti's alleles are {@code A} and
 * {@code a}, so the carrier pair is {@code A/a}. Both are filed; the Copy list is read with {@code topicsIn}; then a
 * fresh copy of each, and a control {@code a/a}, are offered to an empty paper slot both through the predicate
 * {@code PaperSlot.mayPlace} is ({@code isFiledPaper && !holdsElsewhere}) and through a real
 * {@link ResearchShelfMenu} built on the shelf with a FakePlayer's inventory, slot by slot. <b>PASS:</b> two rows naming
 * the two pairs, both duplicates refused, the control accepted. <b>FAIL:</b> one row, or a duplicate accepted, or the
 * control refused. 2 s.
 *
 * <p><b>3. Taking an original back</b> ("take a filed original back and the gene leaves the list"). The A/A paper is
 * then lifted out of that shelf. <b>PASS:</b> the list is the A/a row alone and {@code stores(A/A)} is false. Same
 * step.
 *
 * <p><b>4. Copying with the screen shut.</b> <i>"Pick a gene, add books, close it and walk away: copies keep arriving,
 * one per book."</i> No menu is ever built for this shelf: A/a filed and picked, three books in. Read at 1.5 x copy
 * time (expect one copy, two books) and at 3 x copy time + 100 ticks. <b>PASS:</b> three copies of A/a in the result
 * slot, no books left, the original still filed. <b>FAIL:</b> anything else - a shelf that only copies while watched
 * reads zero.
 *
 * <p><b>5. Pulling the book mid-copy</b> (<i>"pull the book out mid-copy and progress resets to zero"</i>). Two books,
 * A/a picked; at half the copy time the progress is read and the books are lifted out; ten ticks later the progress is
 * read again and the books go back; ten ticks after that, once more. <b>PASS:</b> progress was above zero, read zero
 * with the book out, came back below 12 (restarted, not resumed at the first reading), and no copy was made.
 * INCONCLUSIVE if the progress was zero before the pull - that is a copy that never began, which check 4 judges.
 *
 * <p><b>6. Breaking drops everything</b> (<i>"Breaking a shelf drops everything ... every paper, book and finished
 * copy"</i>). A shelf in a glass cell holds five papers (Agouti A/a, A/A, a/a, Extension E/e, E/E), four books and two
 * finished A/a copies, with nothing picked so nothing moves. {@code level.destroyBlock(pos, true)} at 2 s - the call
 * the stasis bank's break check passed on, through the same {@code preRemoveSideEffects} hook - and every
 * {@link ItemEntity} in the cell is read 2 s later. <b>PASS:</b> seven research papers carrying exactly those pairs
 * (A/a three times) and four books, and the block gone. <b>FAIL:</b> any short. Whether the shelf itself dropped as an
 * item is in the detail, not the verdict - the page does not say.
 *
 * <h2>STALL SHAPES (east)</h2>
 * {@code wiki/item-stall-signs.html}. Twelve small stalls, each a 2 x 2 floor (2 x 1 in the last row) inside a
 * stone-brick ring 3 high unless said otherwise, one block between stalls, built on the grass and measured by calling
 * {@code StallDetector.forSign(level, wall, WEST)} on the west wall at head height ({@code gy + 2}) - the same call
 * {@code StallSignItem} makes before the sign exists, so no sign, horse or owner is needed. The way in is always on the
 * north wall's west tile. Where the page gives an expectation it is one check; the region must be <b>exactly</b> the
 * stall's floor tiles, so a bind that measured the wrong room does not pass. The questions:
 * <ul>
 *   <li><i>"A doorway with a block over it is a stall; a gap open to the sky is not ... Pass: it binds, and the
 *       entrance square is not counted in the block total. Then break the block above the doorway ... Pass: it refuses
 *       with 'That is not an enclosed stall'"</i> - LINTEL (a two-block gap under the ring's third course), read at 2 s;
 *       then the lintel is set to air and the same stall is read at 3 s (SKY: must be {@code null}).</li>
 *   <li>Gap 153, <i>"bind a stall floored with slabs or carpet (one room), one with a one-block step (one room), one
 *       with a two-block drop (not one room), and one whose gate stands open (still stops at the gate)"</i> - SLAB
 *       (oak bottom slabs over the floor), CARPET (white carpet), STEP (the east column one stone higher), DROP (the
 *       east column two stones higher: PASS is a refusal or a region that does not hold every tile), OPEN GATE (an oak
 *       gate set {@code open=true}, open air above it).</li>
 *   <li><i>"Since 2026-09-17 also: a roofed stall, one whose doorway is only one block tall, and one walled with
 *       nothing but a ring of fences and a gate - all three must bind"</i> - ROOFED (ring 2 high, stone-brick lid at
 *       {@code gy + 3}, a closed gate under it), ONE-TALL (a plain one-block hole, ring above it), FENCES (oak fences
 *       and an oak gate, the sign on a fence at {@code gy + 1}). <i>"getting off a horse inside the roofed one must not
 *       hurt you"</i> needs a rider and is not checked here.</li>
 *   <li><i>"A stall shut with a double gate binds"</i> - DOUBLE (the first registered double gate, both halves, open
 *       air above). The checklist tick it should also give needs a player and is not checked.</li>
 * </ul>
 * Every gate above stands in a <b>full-height gap</b> - open air over it - because that is the stall a horse can walk
 * into; a gate under a wall course is a stall no horse could use. Two stalls the page has no expectation for are read
 * as INFO in the summary check, as controls a reader needs to interpret the rest: PLAIN GATE (a flat stall with a
 * closed gate - the ordinary case) and SLAB + LINTEL (the slab stall with the ring carried over its gate, so the gate's
 * top is not open). Reading the code on 2026-10-02 predicts that a closed gate's top is standable (its collision is
 * 1.5 tall) so the floor walk steps over it, and that the stall is then saved only by the wall-ring search at the
 * floor's own level - which a slab floor, being a level higher, starts above. If SLAB fails and SLAB + LINTEL binds,
 * that is the mechanism. Each detail lists the region tile by tile as {@code (dx,dz)+dy} from the stall's north-west
 * corner and the grass.
 *
 * <p>Every API is copied from a use elsewhere in this repo or read in the 26.1.2 sources where marked UNVERIFIED.
 */
final class DebugYardShelf {

    private DebugYardShelf() {
    }

    // ------------------------------------------------------------------
    // Check names
    // ------------------------------------------------------------------

    static final String SAVE = "RESEARCH SHELF - save and reload brings back every filed paper, the book, the copies"
            + " and the pick (check 143)";
    static final String PAIR = "RESEARCH SHELF - Agouti A/a and A/A file as two rows, and a duplicate of either is"
            + " refused the slot";
    static final String TAKEN = "RESEARCH SHELF - taking a filed original back takes its row off the list";
    static final String COPY = "RESEARCH SHELF - with no screen open, copies keep arriving, one per book";
    static final String RESET = "RESEARCH SHELF - pulling the book out mid-copy resets progress to zero";
    static final String BREAK = "RESEARCH SHELF - breaking a shelf drops every paper, book and finished copy";

    static final String LINTEL = "STALL SHAPES - a two-block doorway under a lintel binds, the doorway square not"
            + " counted";
    static final String SKY = "STALL SHAPES - the same doorway open to the sky refuses";
    static final String SLAB = "STALL SHAPES - a slab-floored stall is one room (gap 153)";
    static final String CARPET = "STALL SHAPES - a carpet-floored stall is one room (gap 153)";
    static final String STEP = "STALL SHAPES - a one-block step in the floor is still one room (gap 153)";
    static final String DROP = "STALL SHAPES - a two-block drop is not one room (gap 153)";
    static final String OPEN_GATE = "STALL SHAPES - a stall whose gate stands open still stops at the gate (gap 153)";
    static final String ROOFED = "STALL SHAPES - a roofed stall binds";
    static final String LOW_DOOR = "STALL SHAPES - a stall whose doorway is one block tall binds";
    static final String FENCES = "STALL SHAPES - a ring of fences and a gate binds";
    static final String DOUBLE = "STALL SHAPES - a stall shut with a double gate binds";
    static final String INFO = "STALL SHAPES - every stall was read (INFO: the detector's answer for each, with two"
            + " controls)";

    private static final List<String> SHELF_CHECKS = List.of(SAVE, PAIR, TAKEN, COPY, RESET, BREAK);
    private static final List<String> STALL_CHECKS = List.of(LINTEL, SKY, SLAB, CARPET, STEP, DROP, OPEN_GATE, ROOFED,
            LOW_DOOR, FENCES, DOUBLE, INFO);

    /** Every check answers once, even if a step throws after answering some of them. */
    private static final Set<String> ANSWERED = new HashSet<>();
    /** Bumped by every build, so a step left on the clock from a yard before this one stops itself. */
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int thisRun = ++run;
        ANSWERED.clear();
        try {
            for (String c : SHELF_CHECKS) {
                DebugYardClockwork.expect(c);
            }
            for (String c : STALL_CHECKS) {
                DebugYardClockwork.expect(c);
            }
            int copy = EquineResearchShelfBlockEntity.copyTicks(topic("A", "a").token());
            shelves(level, gy, x0, z0, thisRun, copy);
            stalls(level, gy, x0 + 25, z0, thisRun);
            ActionTrace.log("test yard", "row AX built (RESEARCH SHELF west: Agouti A/a copies in " + copy
                    + " ticks, last answer at " + (3L * copy + 100) + " ticks; STALL SHAPES east: answers at 40 and 60"
                    + " ticks)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AX (RESEARCH SHELF / STALL SHAPES) failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Verdict plumbing
    // ------------------------------------------------------------------

    private static void pass(String check, boolean ok, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.verdict(check, ok, detail);
        }
    }

    private static void unsure(String check, String detail) {
        if (ANSWERED.add(check)) {
            DebugYardClockwork.inconclusive(check, detail);
        }
    }

    /** A timed step that cannot leave its checks silent: a throw answers each still-open one INCONCLUSIVE. */
    private static void step(ServerLevel level, long ticks, int thisRun, List<String> checks, Runnable body) {
        DebugYardHerd.after(level, ticks, () -> {
            if (thisRun != run) {
                return;
            }
            try {
                body.run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: a row AX step threw", e);
                for (String c : checks) {
                    unsure(c, "the step threw " + e);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // WEST - RESEARCH SHELF
    // ------------------------------------------------------------------

    private static ResearchTopic topic(String a, String b) {
        return new ResearchTopic(AgoutiGene.KEY, a, b);
    }

    private static ItemStack paper(String key, String a, String b) {
        return ResearchPaperItem.of(new ResearchTopic(key, a, b));
    }

    private static @Nullable EquineResearchShelfBlockEntity shelf(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.RESEARCH_SHELF.get().defaultBlockState(), 3);
        return shelfAt(level, pos);
    }

    private static @Nullable EquineResearchShelfBlockEntity shelfAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EquineResearchShelfBlockEntity s ? s : null;
    }

    private static void shelves(ServerLevel level, int gy, int x0, int z0, int thisRun, int copy) {
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("RESEARCH SHELF", "save/reload, pairs,", "copy unwatched,", "pull book, break"));
        String aa = topic("A", "a").token();

        // 1. Save and reload.
        BlockPos savePos = new BlockPos(x0 + 2, gy + 1, z0 + 2);
        EquineResearchShelfBlockEntity save = shelf(level, savePos);
        if (save == null) {
            unsure(SAVE, "no shelf block entity at " + savePos.toShortString() + " after setBlock");
        } else {
            save.papers().setItem(0, paper(AgoutiGene.KEY, "A", "a"));
            save.papers().setItem(7, paper(AgoutiGene.KEY, "A", "A"));
            save.papers().setItem(EquineResearchShelfBlockEntity.SLOTS - 1, paper(ExtensionGene.KEY, "E", "e"));
            save.book().setItem(0, new ItemStack(Items.BOOK, 3));
            save.select(aa);
            step(level, copy + copy / 2, thisRun, List.of(SAVE),
                    () -> saveAndReload(level, savePos, new BlockPos(x0 + 2, gy + 1, z0 + 4)));
        }

        // 2 and 3. Pairs, duplicates, and taking one back.
        BlockPos pairPos = new BlockPos(x0 + 5, gy + 1, z0 + 2);
        EquineResearchShelfBlockEntity pairs = shelf(level, pairPos);
        if (pairs == null) {
            unsure(PAIR, "no shelf block entity at " + pairPos.toShortString());
            unsure(TAKEN, "no shelf block entity at " + pairPos.toShortString());
        } else {
            pairs.papers().setItem(0, paper(AgoutiGene.KEY, "A", "a"));
            pairs.papers().setItem(1, paper(AgoutiGene.KEY, "A", "A"));
            step(level, 40, thisRun, List.of(PAIR, TAKEN), () -> pairsAndTaking(level, pairPos));
        }

        // 4. Copying with nobody looking.
        BlockPos copyPos = new BlockPos(x0 + 8, gy + 1, z0 + 2);
        EquineResearchShelfBlockEntity copier = shelf(level, copyPos);
        if (copier == null) {
            unsure(COPY, "no shelf block entity at " + copyPos.toShortString());
        } else {
            copier.papers().setItem(0, paper(AgoutiGene.KEY, "A", "a"));
            copier.book().setItem(0, new ItemStack(Items.BOOK, 3));
            copier.select(aa);
            String[] mid = {"not read"};
            step(level, copy + copy / 2, thisRun, List.of(COPY), () -> {
                EquineResearchShelfBlockEntity s = shelfAt(level, copyPos);
                mid[0] = s == null ? "shelf gone"
                        : "result " + s.result().getItem(0).getCount() + ", books " + s.book().getItem(0).getCount()
                                + ", progress " + s.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
            });
            step(level, 3L * copy + 100, thisRun, List.of(COPY), () -> copied(level, copyPos, aa, copy, mid[0]));
        }

        // 5. Pull the book mid-copy.
        BlockPos resetPos = new BlockPos(x0 + 11, gy + 1, z0 + 2);
        EquineResearchShelfBlockEntity resetter = shelf(level, resetPos);
        if (resetter == null) {
            unsure(RESET, "no shelf block entity at " + resetPos.toShortString());
        } else {
            resetter.papers().setItem(0, paper(AgoutiGene.KEY, "A", "a"));
            resetter.book().setItem(0, new ItemStack(Items.BOOK, 2));
            resetter.select(aa);
            pullTheBook(level, resetPos, copy, thisRun);
        }

        // 6. Break it.
        DebugYardClockwork.cell(level, gy, x0 + 14, x0 + 18, z0 + 1, z0 + 5);
        BlockPos breakPos = new BlockPos(x0 + 16, gy + 1, z0 + 3);
        EquineResearchShelfBlockEntity breaker = shelf(level, breakPos);
        if (breaker == null) {
            unsure(BREAK, "no shelf block entity at " + breakPos.toShortString());
        } else {
            breaker.papers().setItem(0, paper(AgoutiGene.KEY, "A", "a"));
            breaker.papers().setItem(1, paper(AgoutiGene.KEY, "A", "A"));
            breaker.papers().setItem(2, paper(AgoutiGene.KEY, "a", "a"));
            breaker.papers().setItem(20, paper(ExtensionGene.KEY, "E", "e"));
            breaker.papers().setItem(40, paper(ExtensionGene.KEY, "E", "E"));
            breaker.book().setItem(0, new ItemStack(Items.BOOK, 4));
            ItemStack copies = paper(AgoutiGene.KEY, "A", "a");
            copies.setCount(2);
            breaker.result().setItem(0, copies);
            breakIt(level, gy, x0, z0, breakPos, thisRun);
        }
    }

    /** Same item, same components, same count - or both empty. */
    private static boolean same(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
    }

    private static String stack(ItemStack s) {
        if (s.isEmpty()) {
            return "empty";
        }
        String token = EquineResearchShelfBlockEntity.topicOf(s);
        return s.getCount() + "x " + (token.isEmpty() ? s.getItem().toString()
                : EquineResearchShelfBlockEntity.displayName(token));
    }

    private static void saveAndReload(ServerLevel level, BlockPos pos, BlockPos elsewhere) {
        EquineResearchShelfBlockEntity src = shelfAt(level, pos);
        if (src == null) {
            unsure(SAVE, "the source shelf is gone at " + pos.toShortString());
            return;
        }
        int filed = 0;
        for (int i = 0; i < EquineResearchShelfBlockEntity.SLOTS; i++) {
            if (!src.papers().getItem(i).isEmpty()) {
                filed++;
            }
        }
        int progress = src.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
        String before = "before: " + filed + " papers filed, book " + stack(src.book().getItem(0)) + ", result "
                + stack(src.result().getItem(0)) + ", pick '" + src.selectedTopic() + "', progress " + progress;
        if (filed != 3) {
            unsure(SAVE, "the source shelf does not hold the three papers it was given | " + before);
            return;
        }

        // The chunk saver's call (LevelChunk.getBlockEntityNbtForSaving, 26.1.2 sources).
        CompoundTag saved = src.saveWithFullMetadata(level.registryAccess());
        // To bytes and back, as a region file holds it. UNVERIFIED in this repo: NbtIo.write(CompoundTag, DataOutput)
        // - read in the 26.1.2 sources; NbtIo.read(DataInput) is RealmBackup's.
        CompoundTag back;
        int bytes;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                NbtIo.write(saved, out);
            }
            bytes = buffer.size();
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                back = NbtIo.read(in);
            }
        } catch (IOException e) {
            unsure(SAVE, "the tag would not go to bytes and back: " + e + " | " + before);
            return;
        }
        // The chunk loader's call (SerializableChunkData, 26.1.2 sources): a new block entity of the tag's type,
        // created at the given position, then loadWithComponents. UNVERIFIED in this repo: nothing here calls it yet.
        BlockState state = level.getBlockState(pos);
        BlockEntity loaded = BlockEntity.loadStatic(elsewhere, state, back, level.registryAccess());
        if (!(loaded instanceof EquineResearchShelfBlockEntity dst)) {
            pass(SAVE, false, "loadStatic at " + elsewhere.toShortString() + " returned "
                    + (loaded == null ? "null (see the log for its error)" : loaded.getClass().getSimpleName())
                    + " | tag keys " + saved.keySet() + ", " + bytes + " bytes | " + before);
            return;
        }
        List<String> differ = new ArrayList<>();
        int papersBack = 0;
        for (int i = 0; i < EquineResearchShelfBlockEntity.SLOTS; i++) {
            ItemStack a = src.papers().getItem(i);
            ItemStack b = dst.papers().getItem(i);
            if (!b.isEmpty()) {
                papersBack++;
            }
            if (!same(a, b)) {
                differ.add("slot " + i + ": " + stack(a) + " -> " + stack(b));
            }
        }
        if (!same(src.book().getItem(0), dst.book().getItem(0))) {
            differ.add("book: " + stack(src.book().getItem(0)) + " -> " + stack(dst.book().getItem(0)));
        }
        if (!same(src.result().getItem(0), dst.result().getItem(0))) {
            differ.add("result: " + stack(src.result().getItem(0)) + " -> " + stack(dst.result().getItem(0)));
        }
        if (!src.selectedTopic().equals(dst.selectedTopic())) {
            differ.add("pick: '" + src.selectedTopic() + "' -> '" + dst.selectedTopic() + "'");
        }
        int progressBack = dst.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
        if (progress != progressBack) {
            differ.add("progress: " + progress + " -> " + progressBack);
        }
        pass(SAVE, differ.isEmpty(), before + " | saved with saveWithFullMetadata (keys " + saved.keySet() + "), "
                + bytes + " bytes of NBT, loaded by loadStatic at " + elsewhere.toShortString() + " | after: "
                + papersBack + " papers, rows " + EquineResearchShelfBlockEntity.topicsIn(dst.papers()).size()
                + ", book " + stack(dst.book().getItem(0)) + ", result " + stack(dst.result().getItem(0))
                + ", pick '" + dst.selectedTopic() + "', progress " + progressBack
                + (differ.isEmpty() ? " | identical in all 54 slots, book, result, pick and progress"
                        : " | DIFFERS: " + String.join("; ", differ)));
    }

    /** {@code PaperSlot.mayPlace}, written out: a filed paper, for a pair no other slot holds. */
    private static boolean mayFile(SimpleContainer papers, ItemStack stack, int slot) {
        return EquineResearchShelfBlockEntity.isFiledPaper(stack)
                && !EquineResearchShelfBlockEntity.holdsElsewhere(papers,
                        EquineResearchShelfBlockEntity.topicOf(stack), slot);
    }

    private static void pairsAndTaking(ServerLevel level, BlockPos pos) {
        EquineResearchShelfBlockEntity s = shelfAt(level, pos);
        if (s == null) {
            unsure(PAIR, "the shelf is gone at " + pos.toShortString());
            unsure(TAKEN, "the shelf is gone at " + pos.toShortString());
            return;
        }
        String carrier = topic("A", "a").token();
        String breedsTrue = topic("A", "A").token();
        List<String> rows = EquineResearchShelfBlockEntity.topicsIn(s.papers());
        List<String> labels = new ArrayList<>();
        for (String r : rows) {
            labels.add(EquineResearchShelfBlockEntity.displayName(r));
        }
        ItemStack dupCarrier = paper(AgoutiGene.KEY, "A", "a");
        ItemStack dupTrue = paper(AgoutiGene.KEY, "A", "A");
        ItemStack control = paper(AgoutiGene.KEY, "a", "a");
        int free = 2;
        boolean pCarrier = mayFile(s.papers(), dupCarrier, free);
        boolean pTrue = mayFile(s.papers(), dupTrue, free);
        boolean pControl = mayFile(s.papers(), control, free);

        // The real slot, through a menu nobody opened. Its slots are book (0), result (1), then the 54 paper slots.
        String menuRead;
        Boolean mCarrier = null;
        Boolean mTrue = null;
        Boolean mControl = null;
        try {
            ResearchShelfMenu menu = new ResearchShelfMenu(0, new DebugYardClockwork.Hands(level).getInventory(), s);
            mCarrier = menu.getSlot(2 + free).mayPlace(dupCarrier);
            mTrue = menu.getSlot(2 + free).mayPlace(dupTrue);
            mControl = menu.getSlot(2 + free).mayPlace(control);
            menuRead = "menu slot " + (2 + free) + ".mayPlace: dup A/a " + mCarrier + ", dup A/A " + mTrue
                    + ", control a/a " + mControl;
        } catch (RuntimeException e) {
            menuRead = "the menu could not be built (" + e + "), judged on the predicate alone";
        }
        boolean twoRows = rows.size() == 2 && rows.contains(carrier) && rows.contains(breedsTrue);
        boolean refused = !pCarrier && !pTrue && pControl
                && (mCarrier == null || (!mCarrier && !mTrue && mControl));
        pass(PAIR, twoRows && refused, "rows " + rows.size() + " " + labels + " (tokens " + rows + ") | predicate"
                + " for empty slot " + free + ": dup A/a " + pCarrier + ", dup A/A " + pTrue + ", control a/a "
                + pControl + " | " + menuRead);

        // 3. Lift the A/A paper back out, the way a click takes a slot's stack.
        ItemStack taken = s.papers().removeItem(1, 1);
        List<String> after = EquineResearchShelfBlockEntity.topicsIn(s.papers());
        boolean gone = after.size() == 1 && after.get(0).equals(carrier) && !s.stores(breedsTrue);
        pass(TAKEN, gone && !taken.isEmpty(), "took " + stack(taken) + " from slot 1 | rows before " + rows.size()
                + ", after " + after.size() + " " + after + ", stores(A/A) " + s.stores(breedsTrue)
                + ", stores(A/a) " + s.stores(carrier));
    }

    private static void copied(ServerLevel level, BlockPos pos, String aa, int copy, String mid) {
        EquineResearchShelfBlockEntity s = shelfAt(level, pos);
        if (s == null) {
            unsure(COPY, "the shelf is gone at " + pos.toShortString() + " | at 1.5 copies: " + mid);
            return;
        }
        ItemStack out = s.result().getItem(0);
        int copies = out.getCount();
        boolean rightPair = out.isEmpty() || aa.equals(EquineResearchShelfBlockEntity.topicOf(out));
        int books = s.book().getItem(0).getCount();
        boolean original = s.stores(aa);
        pass(COPY, copies == 3 && rightPair && books == 0 && original,
                "copy time " + copy + " ticks, 3 books in, no menu ever built | at " + (copy + copy / 2) + " ticks: "
                        + mid + " | at " + (3L * copy + 100) + " ticks: result " + stack(out) + " (pair right "
                        + rightPair + "), books left " + books + ", progress "
                        + s.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS) + ", pick '" + s.selectedTopic()
                        + "', original still filed " + original);
    }

    private static void pullTheBook(ServerLevel level, BlockPos pos, int copy, int thisRun) {
        int[] read = new int[3];
        ItemStack[] held = {ItemStack.EMPTY};
        long at = copy / 2;
        step(level, at, thisRun, List.of(RESET), () -> {
            EquineResearchShelfBlockEntity s = shelfAt(level, pos);
            if (s == null) {
                unsure(RESET, "the shelf is gone at " + pos.toShortString());
                return;
            }
            read[0] = s.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
            held[0] = s.book().removeItem(0, 64);
        });
        step(level, at + 10, thisRun, List.of(RESET), () -> {
            EquineResearchShelfBlockEntity s = shelfAt(level, pos);
            if (s == null || ANSWERED.contains(RESET)) {
                unsure(RESET, "the shelf is gone at " + pos.toShortString());
                return;
            }
            read[1] = s.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
            s.book().setItem(0, held[0].copy());
        });
        step(level, at + 20, thisRun, List.of(RESET), () -> {
            EquineResearchShelfBlockEntity s = shelfAt(level, pos);
            if (s == null || ANSWERED.contains(RESET)) {
                unsure(RESET, "the shelf is gone at " + pos.toShortString());
                return;
            }
            read[2] = s.data().get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
            ItemStack out = s.result().getItem(0);
            String detail = "copy time " + copy + " | progress at " + at + " ticks " + read[0] + ", then lifted "
                    + stack(held[0]) + " | 10 ticks later, book out: " + read[1] + " | 10 ticks after putting it back: "
                    + read[2] + " | result " + stack(out) + ", books now " + s.book().getItem(0).getCount();
            if (read[0] <= 0 || held[0].isEmpty()) {
                unsure(RESET, "nothing was copying when the book was pulled, so there was nothing to reset | " + detail);
                return;
            }
            pass(RESET, read[1] == 0 && read[2] < 12 && read[2] < read[0] && out.isEmpty(), detail);
        });
    }

    private static void breakIt(ServerLevel level, int gy, int x0, int z0, BlockPos pos, int thisRun) {
        Map<String, Integer> expected = new TreeMap<>();
        expected.put(EquineResearchShelfBlockEntity.displayName(topic("A", "a").token()), 3);
        expected.put(EquineResearchShelfBlockEntity.displayName(topic("A", "A").token()), 1);
        expected.put(EquineResearchShelfBlockEntity.displayName(topic("a", "a").token()), 1);
        expected.put(EquineResearchShelfBlockEntity.displayName(
                new ResearchTopic(ExtensionGene.KEY, "E", "e").token()), 1);
        expected.put(EquineResearchShelfBlockEntity.displayName(
                new ResearchTopic(ExtensionGene.KEY, "E", "E").token()), 1);
        step(level, 40, thisRun, List.of(BREAK), () -> {
            EquineResearchShelfBlockEntity s = shelfAt(level, pos);
            if (s == null) {
                unsure(BREAK, "the shelf was gone before it was broken");
                return;
            }
            int papersIn = 0;
            for (int i = 0; i < EquineResearchShelfBlockEntity.SLOTS; i++) {
                papersIn += s.papers().getItem(i).getCount();
            }
            String inside = "inside: " + papersIn + " filed papers, book " + stack(s.book().getItem(0)) + ", result "
                    + stack(s.result().getItem(0));
            boolean destroyed = level.destroyBlock(pos, true);
            step(level, 40, thisRun, List.of(BREAK), () -> {
                Map<String, Integer> found = new TreeMap<>();
                int papers = 0;
                int books = 0;
                int shelfItems = 0;
                List<String> other = new ArrayList<>();
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                        DebugTestYard.box(x0 + 14, gy, z0 + 1, x0 + 19, gy + 4, z0 + 6))) {
                    ItemStack st = item.getItem();
                    if (st.is(ModItems.RESEARCH_PAPER.get())) {
                        papers += st.getCount();
                        found.merge(EquineResearchShelfBlockEntity.displayName(
                                EquineResearchShelfBlockEntity.topicOf(st)), st.getCount(), Integer::sum);
                    } else if (st.is(Items.BOOK)) {
                        books += st.getCount();
                    } else if (st.is(ModItems.EQUINE_RESEARCH_SHELF.get())) {
                        shelfItems += st.getCount();
                    } else {
                        other.add(stack(st));
                    }
                }
                boolean stillThere = level.getBlockState(pos).is(ModBlocks.RESEARCH_SHELF.get());
                String detail = inside + " | destroyBlock returned " + destroyed + ", block still a shelf "
                        + stillThere + " | dropped: " + papers + " papers " + found + ", " + books + " books, "
                        + shelfItems + " shelf item(s) (INFO, the page does not say)"
                        + (other.isEmpty() ? "" : ", other " + other) + " | expected papers " + expected
                        + " and 4 books";
                if (stillThere) {
                    unsure(BREAK, "the shelf was not broken | " + detail);
                    return;
                }
                pass(BREAK, found.equals(expected) && books == 4, detail);
            });
        });
    }

    // ------------------------------------------------------------------
    // EAST - STALL SHAPES
    // ------------------------------------------------------------------

    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** What the page expects of a stall. */
    private enum Expect { EXACT, REFUSE, NOT_ONE_ROOM, INFO }

    /**
     * One stall: its outer north-west corner, its outer depth (4, or 3 for a one-tile-deep floor), the height of the
     * sign (and so of the wall block it hangs on), and what is expected of it.
     */
    private record Stall(String name, @Nullable String check, int ox, int oz, int depth, int signY, Expect expect) {

        BlockPos wall() {
            return new BlockPos(ox, signY, oz + 1);
        }

        /** The floor tiles a player means by this stall, as (x, z) keys. */
        Set<Long> interior() {
            Set<Long> out = new HashSet<>();
            for (int x = ox + 1; x <= ox + 2; x++) {
                for (int z = oz + 1; z <= oz + depth - 2; z++) {
                    out.add(key(x, z));
                }
            }
            return out;
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** Clear the stall's whole box to air from gy + 1 to gy + 5, then lay a brick ring {@code h} high. */
    private static void shell(ServerLevel level, int gy, int ox, int oz, int depth, int h) {
        for (int x = ox; x <= ox + 3; x++) {
            for (int z = oz; z <= oz + depth - 1; z++) {
                boolean edge = x == ox || x == ox + 3 || z == oz || z == oz + depth - 1;
                for (int y = gy + 1; y <= gy + 5; y++) {
                    level.setBlock(new BlockPos(x, y, z), edge && y <= gy + h ? BRICK : AIR, 3);
                }
            }
        }
    }

    /** A doorway or gate gap: the north wall's west tile cleared from {@code fromY} to {@code toY}. */
    private static void gap(ServerLevel level, int ox, int oz, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            level.setBlock(new BlockPos(ox + 1, y, oz), AIR, 3);
        }
    }

    private static BlockState gate(boolean open) {
        // FenceGateBlock.FACING is penWalls' own; OPEN as DebugYardStud places an open gate.
        return Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, Direction.NORTH)
                .setValue(FenceGateBlock.OPEN, open);
    }

    private static void floor(ServerLevel level, int gy, Stall s, BlockState what) {
        for (int x = s.ox() + 1; x <= s.ox() + 2; x++) {
            for (int z = s.oz() + 1; z <= s.oz() + s.depth() - 2; z++) {
                level.setBlock(new BlockPos(x, gy + 1, z), what, 3);
            }
        }
    }

    /** The east floor column raised {@code by} blocks of stone. */
    private static void raise(ServerLevel level, int gy, Stall s, int by) {
        for (int z = s.oz() + 1; z <= s.oz() + s.depth() - 2; z++) {
            for (int y = gy + 1; y <= gy + by; y++) {
                level.setBlock(new BlockPos(s.ox() + 2, y, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private static void stalls(ServerLevel level, int gy, int e, int z0, int thisRun) {
        DebugPenManager.placeSign(level, new BlockPos(e + 19, gy + 1, z0 - 1), Direction.NORTH,
                List.of("STALL SHAPES", "lintel/sky, slab,", "carpet, step, drop,", "gates, roof, fences"));
        int head = gy + 2;
        int rowA = z0;
        int rowB = z0 + 5;
        int rowC = z0 + 10;
        List<Stall> all = new ArrayList<>();

        // Row A ---------------------------------------------------------
        Stall lintel = new Stall("LINTEL", LINTEL, e, rowA, 4, head, Expect.EXACT);
        shell(level, gy, lintel.ox(), lintel.oz(), 4, 3);
        gap(level, lintel.ox(), lintel.oz(), gy + 1, gy + 2);   // the ring's third course over it is the lintel
        all.add(lintel);

        Stall stepped = new Stall("STEP", STEP, e + 5, rowA, 4, head, Expect.EXACT);
        shell(level, gy, stepped.ox(), stepped.oz(), 4, 3);
        raise(level, gy, stepped, 1);
        gap(level, stepped.ox(), stepped.oz(), gy + 1, gy + 3);
        level.setBlock(new BlockPos(stepped.ox() + 1, gy + 1, stepped.oz()), gate(false), 3);
        all.add(stepped);

        Stall drop = new Stall("DROP", DROP, e + 10, rowA, 4, head, Expect.NOT_ONE_ROOM);
        shell(level, gy, drop.ox(), drop.oz(), 4, 3);
        raise(level, gy, drop, 2);
        gap(level, drop.ox(), drop.oz(), gy + 1, gy + 3);
        level.setBlock(new BlockPos(drop.ox() + 1, gy + 1, drop.oz()), gate(false), 3);
        all.add(drop);

        Stall open = new Stall("OPEN GATE", OPEN_GATE, e + 15, rowA, 4, head, Expect.EXACT);
        shell(level, gy, open.ox(), open.oz(), 4, 3);
        gap(level, open.ox(), open.oz(), gy + 1, gy + 3);
        // UNVERIFIED in this repo as a setBlock: an open gate, as DebugYardStud places one with setBlockAndUpdate.
        level.setBlock(new BlockPos(open.ox() + 1, gy + 1, open.oz()), gate(true), 3);
        all.add(open);

        // Row B ---------------------------------------------------------
        Stall roofed = new Stall("ROOFED", ROOFED, e, rowB, 4, head, Expect.EXACT);
        shell(level, gy, roofed.ox(), roofed.oz(), 4, 2);
        for (int x = roofed.ox(); x <= roofed.ox() + 3; x++) {
            for (int z = roofed.oz(); z <= roofed.oz() + 3; z++) {
                level.setBlock(new BlockPos(x, gy + 3, z), BRICK, 3);
            }
        }
        gap(level, roofed.ox(), roofed.oz(), gy + 1, gy + 2);
        level.setBlock(new BlockPos(roofed.ox() + 1, gy + 1, roofed.oz()), gate(false), 3);
        all.add(roofed);

        Stall low = new Stall("ONE-TALL", LOW_DOOR, e + 5, rowB, 4, head, Expect.EXACT);
        shell(level, gy, low.ox(), low.oz(), 4, 3);
        gap(level, low.ox(), low.oz(), gy + 1, gy + 1);         // a plain one-block hole, ring over it
        all.add(low);

        Stall fences = new Stall("FENCES", FENCES, e + 10, rowB, 4, gy + 1, Expect.EXACT);
        shell(level, gy, fences.ox(), fences.oz(), 4, 0);
        for (int x = fences.ox(); x <= fences.ox() + 3; x++) {
            for (int z = fences.oz(); z <= fences.oz() + 3; z++) {
                boolean edge = x == fences.ox() || x == fences.ox() + 3 || z == fences.oz() || z == fences.oz() + 3;
                if (edge) {
                    // UNVERIFIED in this repo: Blocks.OAK_FENCE placed - DebugYardStud does the same with
                    // setBlockAndUpdate, which is setBlock with flag 3.
                    level.setBlock(new BlockPos(x, gy + 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(new BlockPos(fences.ox() + 1, gy + 1, fences.oz()), gate(false), 3);
        all.add(fences);

        Stall dbl = new Stall("DOUBLE GATE", DOUBLE, e + 15, rowB, 4, head, Expect.EXACT);
        shell(level, gy, dbl.ox(), dbl.oz(), 4, 3);
        boolean haveDouble = !DoubleGates.gates().isEmpty();
        for (int y = gy + 1; y <= gy + 3; y++) {
            level.setBlock(new BlockPos(dbl.ox() + 1, y, dbl.oz()), AIR, 3);
            level.setBlock(new BlockPos(dbl.ox() + 2, y, dbl.oz()), AIR, 3);
        }
        if (haveDouble) {
            // LEFT's partner is FACING.getCounterClockWise() - west of it, for a gate facing north - so LEFT goes
            // east. LEFT first, then RIGHT: RIGHT's placement shape-updates LEFT, which then finds its partner.
            // UNVERIFIED: no code in this repo places a double gate except the block's own setPlacedBy.
            BlockState base = DoubleGates.gates().get(0).block().get().defaultBlockState()
                    .setValue(FenceGateBlock.FACING, Direction.NORTH);
            level.setBlock(new BlockPos(dbl.ox() + 2, gy + 1, dbl.oz()),
                    base.setValue(DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.LEFT), 3);
            level.setBlock(new BlockPos(dbl.ox() + 1, gy + 1, dbl.oz()),
                    base.setValue(DoubleFenceGateBlock.HALF, DoubleFenceGateBlock.Half.RIGHT), 3);
        }
        all.add(dbl);

        // Row C (a 2 x 1 floor) -----------------------------------------
        Stall slab = new Stall("SLAB", SLAB, e, rowC, 3, head, Expect.EXACT);
        shell(level, gy, slab.ox(), slab.oz(), 3, 3);
        // UNVERIFIED in this repo: Blocks.OAK_SLAB (its default state is a bottom slab) - read in the 26.1.2 Blocks.
        floor(level, gy, slab, Blocks.OAK_SLAB.defaultBlockState());
        gap(level, slab.ox(), slab.oz(), gy + 1, gy + 3);
        level.setBlock(new BlockPos(slab.ox() + 1, gy + 1, slab.oz()), gate(false), 3);
        all.add(slab);

        Stall carpet = new Stall("CARPET", CARPET, e + 5, rowC, 3, head, Expect.EXACT);
        shell(level, gy, carpet.ox(), carpet.oz(), 3, 3);
        // UNVERIFIED in this repo: Blocks.WHITE_CARPET - read in the 26.1.2 Blocks.
        floor(level, gy, carpet, Blocks.WHITE_CARPET.defaultBlockState());
        gap(level, carpet.ox(), carpet.oz(), gy + 1, gy + 3);
        level.setBlock(new BlockPos(carpet.ox() + 1, gy + 1, carpet.oz()), gate(false), 3);
        all.add(carpet);

        Stall plain = new Stall("PLAIN GATE (control)", null, e + 10, rowC, 3, head, Expect.INFO);
        shell(level, gy, plain.ox(), plain.oz(), 3, 3);
        gap(level, plain.ox(), plain.oz(), gy + 1, gy + 3);
        level.setBlock(new BlockPos(plain.ox() + 1, gy + 1, plain.oz()), gate(false), 3);
        all.add(plain);

        Stall slabLintel = new Stall("SLAB + LINTEL (control)", null, e + 15, rowC, 3, head, Expect.INFO);
        shell(level, gy, slabLintel.ox(), slabLintel.oz(), 3, 3);
        floor(level, gy, slabLintel, Blocks.OAK_SLAB.defaultBlockState());
        level.setBlock(new BlockPos(slabLintel.ox() + 1, gy + 1, slabLintel.oz()), gate(false), 3);
        all.add(slabLintel);

        Map<String, String> info = new LinkedHashMap<>();
        step(level, 40, thisRun, STALL_CHECKS, () -> {
            for (Stall s : all) {
                String extra = s == dbl ? " | double gate registered " + haveDouble + ", halves standing: west "
                        + level.getBlockState(new BlockPos(dbl.ox() + 1, gy + 1, dbl.oz())).getBlock()
                        + ", east " + level.getBlockState(new BlockPos(dbl.ox() + 2, gy + 1, dbl.oz())).getBlock()
                        : "";
                if (s == dbl && !haveDouble) {
                    unsure(DOUBLE, "no double gate is registered - DoubleGates.gates() is empty");
                    info.put(s.name(), "not built");
                    continue;
                }
                judge(level, gy, s, extra, info);
            }
            // Now take the lintel away: the same doorway, open to the sky.
            level.setBlock(new BlockPos(lintel.ox() + 1, gy + 3, lintel.oz()), AIR, 3);
        });
        step(level, 60, thisRun, List.of(SKY, INFO), () -> {
            Stall sky = new Stall("SKY (the LINTEL stall, lintel removed)", SKY, lintel.ox(), lintel.oz(), 4, head,
                    Expect.REFUSE);
            boolean lintelGone = level.getBlockState(new BlockPos(lintel.ox() + 1, gy + 3, lintel.oz())).isAir();
            if (!lintelGone) {
                unsure(SKY, "the lintel at " + new BlockPos(lintel.ox() + 1, gy + 3, lintel.oz()).toShortString()
                        + " is still there");
            } else {
                judge(level, gy, sky, "", info);
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> en : info.entrySet()) {
                sb.append(sb.length() == 0 ? "" : " || ").append(en.getKey()).append(": ").append(en.getValue());
            }
            pass(INFO, info.size() == all.size() + 1, info.size() + " of " + (all.size() + 1) + " readings | "
                    + "every stall a 2-wide floor on the grass at gy+1, brick ring 3 high unless named, way in on"
                    + " the north wall's west tile, gates with open air above, sign wall on the west at gy+2"
                    + " (FENCES: gy+1) facing west | " + sb);
        });
    }

    /** Read one stall, record it for the summary, and answer its check if the page has an expectation. */
    private static void judge(ServerLevel level, int gy, Stall s, String extra, Map<String, String> info) {
        StallDetector.Result r = StallDetector.forSign(level, s.wall(), Direction.WEST);
        String read = describe(r, s, gy) + extra;
        info.put(s.name(), read);
        if (s.check() == null) {
            return;
        }
        Set<Long> want = s.interior();
        Set<Long> got = r == null ? Set.of() : StallFill.keysOf(r.region());
        boolean ok = switch (s.expect()) {
            case EXACT -> r != null && got.equals(want);
            case REFUSE -> r == null;
            case NOT_ONE_ROOM -> r == null || !got.containsAll(want);
            case INFO -> true;
        };
        String expected = switch (s.expect()) {
            case EXACT -> "binds to exactly its " + want.size() + " floor tiles";
            case REFUSE -> "refuses (null, 'That is not an enclosed stall')";
            case NOT_ONE_ROOM -> "refuses, or binds to fewer than all " + want.size() + " tiles";
            case INFO -> "no expectation";
        };
        pass(s.check(), ok, s.name() + " at " + new BlockPos(s.ox(), gy, s.oz()).toShortString() + ", sign wall "
                + s.wall().toShortString() + " face west | expected: " + expected + " | read: " + read);
    }

    private static String describe(@Nullable StallDetector.Result r, Stall s, int gy) {
        if (r == null) {
            return "refused (null)";
        }
        List<String> tiles = new ArrayList<>();
        for (StallFill.Column c : r.region().columns()) {
            tiles.add("(" + (c.x() - s.ox()) + "," + (c.z() - s.oz()) + ")+" + (c.y() - gy));
        }
        boolean doorway = StallFill.contains(r.region(), s.ox() + 1, s.oz());
        return "binds " + r.blockCount() + " tiles " + tiles + ", span " + r.sizeX() + "x" + r.sizeY() + "x"
                + r.sizeZ() + ", doorway tile (1,0) counted " + doorway;
    }
}
