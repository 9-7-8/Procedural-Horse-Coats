package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.common.genetics.genes.AgoutiGene;
import com.example.horsegenetics.common.genetics.genes.ExtensionGene;
import com.example.horsegenetics.neoforge.HorseGenetics;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <b>Row AX: the research shelf</b> (2026-10-02). No horse and no hands that have to be in the level: every check
 * drives a block entity directly, on the yard clock, and ends in one {@code CLOCKWORK} line. The STALL SHAPES pen that
 * stood east of it was retired on 2026-10-04: its last two open shapes (a slab floor, a one-block step - issue #25)
 * are now the gametest {@code stall_shapes_bind_as_one_room}, and the rest had passed here on 2026-10-02.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>footprint</th><th>answers by</th></tr>
 *   <tr><td>west</td><td>RESEARCH SHELF</td><td>x0+2..x0+18, z0+1..z0+5</td><td>2 s (filing, break) to
 *       3 x copy time + 5 s (copying; 35 s for a common gene)</td></tr>
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

    private static final List<String> SHELF_CHECKS = List.of(SAVE, PAIR, TAKEN, COPY, RESET, BREAK);

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
            int copy = EquineResearchShelfBlockEntity.copyTicks(topic("A", "a").token());
            shelves(level, gy, x0, z0, thisRun, copy);
            ActionTrace.log("test yard", "row AX built (RESEARCH SHELF west: Agouti A/a copies in " + copy
                    + " ticks, last answer at " + (3L * copy + 100) + " ticks)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AX (RESEARCH SHELF) failed to build", e);
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
}
