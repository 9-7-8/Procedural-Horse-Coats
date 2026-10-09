package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.pack.HarnessTier;
import com.example.horsegenetics.common.pack.PackLoad;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorsePackLoad;
import com.example.horsegenetics.neoforge.data.HorsePacks;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row BE: PACKS and PACK DEATH</b> - chests on horses and the storage harness, in a running game
 * (owner, 2026-10-09: <i>"build some yard tests for this and test those"</i>).
 *
 * <p>The gametests ({@code horse_carries_chests}, {@code hosted_chest_is_a_real_block}) already pin the
 * rules. What they cannot do is what these are for: a horse that <b>really dies</b>, through every death
 * handler the mod has; one that goes through a <b>whole-entity save and load</b>, which is how a stasis
 * chamber and a were-night carry it; a load read off the <b>live speed attribute</b>; and menus opened for
 * a <b>real player on a real connection</b>, ticking - which a FakePlayer cannot stand in for, because
 * NeoForge's {@code FakePlayer.openMenu} does nothing.
 *
 * <p>Each check ends in one {@code CLOCKWORK PASS|FAIL|INCONCLUSIVE} line, like every other row. The open
 * checks they answer are on wiki/horse-gear.html#verify-packs and wiki/item-storage-harness.html#verify-harness.
 *
 * <p><b>The debug dimension refuses every block an entity places</b>
 * ({@code HorseGeneticsEventHandler.noBlockPlaceInDebugDimension}), so a horse that dies here cannot set its
 * chests down and must fall back to dropping them. PACK DEATH therefore proves the <i>fallback</i> - against
 * a real protection handler - and counts whatever was placed and whatever was dropped together; the placing
 * itself is the gametest's.
 */
final class DebugYardPacks {

    private DebugYardPacks() {
    }

    /** Bumped by every build, so a clock left running for an older yard stops itself. */
    private static int run;

    private static final String HANDS = "PACK HARNESS FIRST - a chest offered to a bare horse is refused in words and"
            + " spent on nothing; a harness and then two chests, by hand, land one on each flank";
    private static final String LOAD = "PACK LOAD - a full chest slows the live horse by PackLoad's own figure less"
            + " the harness's share, and emptying it gives the speed back";
    private static final String STASIS = "PACK STASIS - a loaded, harnessed horse saved whole and loaded again still"
            + " has its harness, its chest, the contents and its load";
    private static final String DEATH = "PACK DEATH - a loaded horse that really dies leaves both chests, every item in"
            + " them and its harness, once each, and keeps nothing";
    private static final String RAISED = "PACK RAISED - the same horse raised from a snapshot taken before it died"
            + " wears and carries nothing";
    private static final String MENU = "PACK REAL MENU - a chest on a horse opens vanilla's chest menu for a real"
            + " player, stays open through real ticks, and what goes in is on the horse";
    private static final String HOSTED = "PACK HOSTED - a chest opened as its own block stands at the bottom of the"
            + " world while a real player has it open, stays open, and the world is put back on close";

    private static final String BAY = "horsegenetics.extension=E/E-horsegenetics.agouti=A/A";

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int myRun = ++run;
        try {
            DebugYardUnattended.pen(level, gy, x0, z0, 18, 11, "PACKS", Blocks.GRASS_BLOCK.defaultBlockState(),
                    List.of("PACKS", "harness, chests,", "load and stasis", "by the hands"));
            DebugYardUnattended.pen(level, gy, x0 + 25, z0, 18, 11, "PACK DEATH",
                    Blocks.GRASS_BLOCK.defaultBlockState(),
                    List.of("PACK DEATH", "a loaded horse", "dies, and is", "raised again"));
            Pen pen = new Pen(myRun, level, List.of(HANDS, LOAD, STASIS, DEATH, RAISED, MENU, HOSTED), 6_000L);
            pen.after(100, () -> hands(pen, gy, x0, z0));
            pen.after(140, () -> load(pen, gy, x0, z0));
            pen.after(180, () -> stasis(pen, gy, x0, z0));
            pen.after(220, () -> death(pen, gy, x0 + 25, z0));
            pen.after(400, () -> realMenu(pen, 1));
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row BE (PACKS) failed to build", e);
        }
        ActionTrace.log("test yard", "row BE built (PACKS, PACK DEATH)");
    }

    // ------------------------------------------------------------------
    // shared
    // ------------------------------------------------------------------

    /** The row's checks: named at build, answered once each, INCONCLUSIVE if a step throws or the deadline comes. */
    private static final class Pen {
        final int run;
        final ServerLevel level;
        final Set<String> answered = new HashSet<>();
        final List<String> checks;

        Pen(int run, ServerLevel level, List<String> checks, long deadline) {
            this.run = run;
            this.level = level;
            this.checks = checks;
            for (String c : checks) {
                DebugYardClockwork.expect(c);
            }
            after(deadline, () -> {
                for (String c : checks) {
                    answer(c, null, "deadline: " + deadline + " ticks after the build and no answer");
                }
            });
        }

        /** {@code pass == null} is INCONCLUSIVE. A second answer for the same check is dropped. */
        void answer(String check, @Nullable Boolean pass, String detail) {
            if (!answered.add(check)) {
                return;
            }
            if (pass == null) {
                DebugYardClockwork.inconclusive(check, detail);
            } else {
                DebugYardClockwork.verdict(check, pass, detail);
            }
        }

        void after(long ticks, Runnable body) {
            DebugYardHerd.after(level, Math.max(1L, ticks), () -> {
                if (run != DebugYardPacks.run) {
                    return;
                }
                try {
                    body.run();
                } catch (RuntimeException e) {
                    HorseGenetics.LOGGER.warn("[Debug] test yard: a PACKS step failed", e);
                    for (String c : checks) {
                        if (!answered.contains(c)) {
                            // Only the step that threw knows which check it was; name them all rather than none.
                            DebugYardClockwork.inconclusive(c, "a timed step threw " + e);
                            answered.add(c);
                        }
                    }
                }
            });
        }
    }

    private static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, String label) {
        Horse h = DebugYardUnattended.horse(level, gy, x, z, Sex.MALE, BAY, true, label);
        if (h != null) {
            h.setNoAi(true);    // these are read, not watched: a horse that wanders is a horse out of reach
        }
        return h;
    }

    private static HorsePacks packs(Horse horse) {
        return horse.getData(ModAttachments.HORSE_PACKS);
    }

    private static String f3(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    // ==================================================================
    // PACK HARNESS FIRST - the hands
    // ==================================================================

    private static void hands(Pen pen, int gy, int x0, int z0) {
        ServerLevel level = pen.level;
        Horse horse = horse(level, gy, x0 + 4.5, z0 + 5.5, "PACK HANDS");
        if (horse == null) {
            pen.answer(HANDS, null, "the horse did not spawn");
            return;
        }
        // 1. A chest, no harness: claimed, said, and the chest still in the hand.
        DebugYardClockwork.Hands first = DebugYardClockwork.hands(level, horse, stack(Items.CHEST, 2), false);
        horse.setOwner(first);
        first.setXRot(-90f);    // looking at the sky: a click on the horse and on no chest
        DebugYardClockwork.use(first, horse);
        boolean refused = first.heardAny("pack.needs_harness") && first.count(Items.CHEST) == 2
                && HorseTackSlot.SADDLEBAG_LEFT.on(horse).isEmpty() && HorseTackSlot.SADDLEBAG_RIGHT.on(horse).isEmpty();
        // 2. The harness by hand.
        DebugYardClockwork.Hands second = DebugYardClockwork.hands(level, horse,
                stack(ModItems.COPPER_STORAGE_HARNESS.get(), 1), false);
        second.setXRot(-90f);
        DebugYardClockwork.use(second, horse);
        boolean harnessed = HorseTackSlot.HARNESS.on(horse).is(ModItems.COPPER_STORAGE_HARNESS.get())
                && second.count(ModItems.COPPER_STORAGE_HARNESS.get()) == 0;
        // 3. Two chests by hand, one click each.
        DebugYardClockwork.Hands third = DebugYardClockwork.hands(level, horse, stack(Items.CHEST, 2), false);
        third.setXRot(-90f);
        DebugYardClockwork.use(third, horse);
        DebugYardClockwork.use(third, horse);
        boolean both = HorseTackSlot.SADDLEBAG_LEFT.on(horse).is(Items.CHEST)
                && HorseTackSlot.SADDLEBAG_RIGHT.on(horse).is(Items.CHEST) && third.count(Items.CHEST) == 0;
        // 4. And the harness now stays put.
        boolean held = !HorseTackSlot.HARNESS.mayTakeOff(horse);
        pen.answer(HANDS, refused && harnessed && both && held,
                "bare horse: " + (refused ? "refused, " : "NOT refused, ") + first.said() + ", " + first.count(Items.CHEST)
                        + " chests still held; harness by hand " + (harnessed ? "on" : "NOT on") + "; chests left="
                        + HorseTackSlot.SADDLEBAG_LEFT.on(horse).getItem() + " right="
                        + HorseTackSlot.SADDLEBAG_RIGHT.on(horse).getItem() + ", " + third.count(Items.CHEST)
                        + " unspent; harness " + (held ? "will not come off" : "WOULD come off") + " under them");
    }

    // ==================================================================
    // PACK LOAD - the live attribute
    // ==================================================================

    private static void load(Pen pen, int gy, int x0, int z0) {
        Horse horse = horse(pen.level, gy, x0 + 9.5, z0 + 5.5, "PACK LOAD");
        if (horse == null) {
            pen.answer(LOAD, null, "the horse did not spawn");
            return;
        }
        HorseTackSlot.HARNESS.set(horse, stack(ModItems.NETHERITE_STORAGE_HARNESS.get(), 1));
        HorseTackSlot.SADDLEBAG_LEFT.set(horse, stack(Items.CHEST, 1));
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || !ServerConfig.packCurve().enabled()) {
            pen.answer(LOAD, null, "no speed attribute, or packs.weight is off");
            return;
        }
        double empty = speed.getValue();
        int items = 27 * 64;
        for (int i = 0; i < 27; i++) {
            packs(horse).set(HorseTackSlot.SADDLEBAG_LEFT.name(), i, stack(Items.COBBLESTONE, 64));
        }
        HorsePackHandler.refresh(horse);
        double full = speed.getValue();
        double eased = HorsePackHandler.harnessReduction(horse);
        double pull = HorseDraft.pullOf(horse);
        double want = PackLoad.retention(ServerConfig.packCurve(), PackLoad.lightened(items, eased), pull);
        double bare = PackLoad.retention(ServerConfig.packCurve(), items, pull);
        packs(horse).take(HorseTackSlot.SADDLEBAG_LEFT.name());
        HorsePackHandler.refresh(horse);
        double after = speed.getValue();
        boolean pass = Math.abs(full / empty - want) < 1.0e-6 && want > bare && want < 1.0
                && Math.abs(eased - HarnessTier.NETHERITE.reduction(ServerConfig.packHarnessBestReduction())) < 1.0e-9
                && Math.abs(after - empty) < 1.0e-9;
        pen.answer(LOAD, pass, items + " items on a pull-" + f3(pull) + " horse in a netherite harness (" + f3(eased)
                + " off): speed " + f3(empty) + " -> " + f3(full) + ", kept " + f3(full / empty) + ", PackLoad says "
                + f3(want) + " (without the harness " + f3(bare) + "); emptied -> " + f3(after));
    }

    // ==================================================================
    // PACK STASIS - a whole-entity save and load
    // ==================================================================

    private static void stasis(Pen pen, int gy, int x0, int z0) {
        ServerLevel level = pen.level;
        Horse horse = horse(level, gy, x0 + 14.5, z0 + 5.5, "PACK STASIS");
        if (horse == null) {
            pen.answer(STASIS, null, "the horse did not spawn");
            return;
        }
        HorseTackSlot.HARNESS.set(horse, stack(ModItems.IRON_STORAGE_HARNESS.get(), 1));
        HorseTackSlot.SADDLEBAG_LEFT.set(horse, stack(Items.CHEST, 1));
        ItemStack shulker = stack(Items.SHULKER_BOX, 1);
        HorseTackSlot.SADDLEBAG_RIGHT.set(horse, shulker);
        packs(horse).set(HorseTackSlot.SADDLEBAG_LEFT.name(), 7, stack(Items.COBBLESTONE, 64));
        packs(horse).set(HorseTackSlot.SADDLEBAG_RIGHT.name(), 3, stack(Items.DIAMOND, 5));
        HorsePackHandler.refresh(horse);
        Vec3 at = horse.position();
        StasisSnapshot snapshot = HorseStasisHandler.snapshot(horse, "PACK STASIS");
        horse.discard();
        pen.after(10, () -> {
            Horse back = HorseStasisHandler.release(level, at, 0f, snapshot);
            if (back == null) {
                pen.answer(STASIS, null, "the snapshot would not release");
                return;
            }
            back.setNoAi(true);
            pen.after(10, () -> {
                HorsePacks p = packs(back);
                HorsePackLoad load = back.getData(ModAttachments.HORSE_PACK_LOAD);
                AttributeInstance speed = back.getAttribute(Attributes.MOVEMENT_SPEED);
                boolean slowed = speed != null && speed.getValue() < speed.getBaseValue();
                boolean pass = HorseTackSlot.HARNESS.on(back).is(ModItems.IRON_STORAGE_HARNESS.get())
                        && HorseTackSlot.SADDLEBAG_LEFT.on(back).is(Items.CHEST)
                        && HorseTackSlot.SADDLEBAG_RIGHT.on(back).is(Items.SHULKER_BOX)
                        && p.get(HorseTackSlot.SADDLEBAG_LEFT.name(), 7).is(Items.COBBLESTONE)
                        && p.get(HorseTackSlot.SADDLEBAG_LEFT.name(), 7).getCount() == 64
                        && p.get(HorseTackSlot.SADDLEBAG_RIGHT.name(), 3).is(Items.DIAMOND)
                        && p.count(HorseTackSlot.SADDLEBAG_RIGHT.name()) == 5
                        && load.left() == 64 && load.right() == 5 && slowed;
                pen.answer(STASIS, pass, "after save and load: harness=" + HorseTackSlot.HARNESS.on(back).getItem()
                        + " left=" + HorseTackSlot.SADDLEBAG_LEFT.on(back).getItem() + " ("
                        + p.count(HorseTackSlot.SADDLEBAG_LEFT.name()) + " items, slot 7 "
                        + p.get(HorseTackSlot.SADDLEBAG_LEFT.name(), 7) + ") right="
                        + HorseTackSlot.SADDLEBAG_RIGHT.on(back).getItem() + " ("
                        + p.count(HorseTackSlot.SADDLEBAG_RIGHT.name()) + " items); synced load " + load
                        + "; speed modifier " + (slowed ? "re-made on join" : "MISSING"));
            });
        });
    }

    // ==================================================================
    // PACK DEATH and PACK RAISED - a real death
    // ==================================================================

    private static void death(Pen pen, int gy, int x0, int z0) {
        ServerLevel level = pen.level;
        Horse horse = horse(level, gy, x0 + 9.5, z0 + 5.5, "PACK DEATH");
        if (horse == null) {
            pen.answer(DEATH, null, "the horse did not spawn");
            pen.answer(RAISED, null, "the horse did not spawn");
            return;
        }
        HorseTackSlot.HARNESS.set(horse, stack(ModItems.GOLDEN_STORAGE_HARNESS.get(), 1));
        HorseTackSlot.SADDLEBAG_LEFT.set(horse, stack(Items.CHEST, 1));
        HorseTackSlot.SADDLEBAG_RIGHT.set(horse, stack(Items.BARREL, 1));
        packs(horse).set(HorseTackSlot.SADDLEBAG_LEFT.name(), 0, stack(Items.COBBLESTONE, 64));
        packs(horse).set(HorseTackSlot.SADDLEBAG_LEFT.name(), 13, stack(Items.DIAMOND, 5));
        packs(horse).set(HorseTackSlot.SADDLEBAG_RIGHT.name(), 2, stack(Items.IRON_INGOT, 32));
        HorsePackHandler.refresh(horse);
        // What the afterlife would hold: taken before the death, with everything still on the horse.
        StasisSnapshot before = HorseStasisHandler.snapshot(horse, "PACK DEATH");
        Vec3 fell = horse.position();
        UUID id = horse.getUUID();
        horse.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        pen.after(10, () -> {
            if (horse.isAlive()) {
                pen.answer(DEATH, null, "the horse would not die of genericKill - something saved it");
                pen.answer(RAISED, null, "no death to raise from");
                return;
            }
            Map<Item, Integer> found = new LinkedHashMap<>();
            for (Item item : List.of(Items.CHEST, Items.BARREL, Items.COBBLESTONE, Items.DIAMOND, Items.IRON_INGOT,
                    ModItems.GOLDEN_STORAGE_HARNESS.get())) {
                found.put(item, 0);
            }
            AABB around = new AABB(fell, fell).inflate(6.0, 4.0, 6.0);
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, around)) {
                found.computeIfPresent(drop.getItem().getItem(), (k, v) -> v + drop.getItem().getCount());
            }
            int placed = 0;
            BlockPos centre = BlockPos.containing(fell);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-4, -3, -4), centre.offset(4, 3, 4))) {
                BlockState state = level.getBlockState(pos);
                if (!state.is(Blocks.CHEST) && !state.is(Blocks.BARREL)) {
                    continue;
                }
                placed++;
                found.computeIfPresent(state.getBlock().asItem(), (k, v) -> v + 1);
                if (level.getBlockEntity(pos) instanceof Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack in = container.getItem(i);
                        found.computeIfPresent(in.getItem(), (k, v) -> v + in.getCount());
                    }
                }
            }
            boolean once = found.get(Items.CHEST) == 1 && found.get(Items.BARREL) == 1
                    && found.get(Items.COBBLESTONE) == 64 && found.get(Items.DIAMOND) == 5
                    && found.get(Items.IRON_INGOT) == 32 && found.get(ModItems.GOLDEN_STORAGE_HARNESS.get()) == 1;
            boolean kept = !HorseTackSlot.HARNESS.on(horse).isEmpty() || !HorseTackSlot.SADDLEBAG_LEFT.on(horse).isEmpty()
                    || !HorseTackSlot.SADDLEBAG_RIGHT.on(horse).isEmpty() || !packs(horse).isEmpty();
            pen.answer(DEATH, once && !kept, "left behind " + found + "; " + placed + " set down as blocks (the debug"
                    + " dimension refuses a placed block, so 0 is the fallback working); the body "
                    + (kept ? "STILL HOLDS something" : "holds nothing"));

            Horse raised = HorseResurrection.raise(level, fell.add(3, 0, 0), 0f, before);
            if (raised == null) {
                pen.answer(RAISED, null, "the snapshot would not raise (id " + id + ")");
                return;
            }
            raised.setNoAi(true);
            boolean bare = HorseTackSlot.HARNESS.on(raised).isEmpty() && HorseTackSlot.SADDLEBAG_LEFT.on(raised).isEmpty()
                    && HorseTackSlot.SADDLEBAG_RIGHT.on(raised).isEmpty() && packs(raised).isEmpty();
            pen.answer(RAISED, bare, "raised: harness=" + HorseTackSlot.HARNESS.on(raised).getItem() + " left="
                    + HorseTackSlot.SADDLEBAG_LEFT.on(raised).getItem() + " right="
                    + HorseTackSlot.SADDLEBAG_RIGHT.on(raised).getItem() + ", store "
                    + (packs(raised).isEmpty() ? "empty" : "NOT empty"));
        });
    }

    // ==================================================================
    // PACK REAL MENU and PACK HOSTED - a real player, a real connection, real ticks
    // ==================================================================

    private static void realMenu(Pen pen, int attempt) {
        ServerLevel level = pen.level;
        ServerPlayer player = null;
        for (ServerPlayer candidate : level.players()) {
            if (!(candidate instanceof FakePlayer) && candidate.isAlive() && !candidate.isSpectator()) {
                player = candidate;
                break;
            }
        }
        if (player == null) {
            if (attempt >= 20) {
                pen.answer(MENU, null, "no real player in the dimension to open a menu for");
                pen.answer(HOSTED, null, "no real player in the dimension to open a menu for");
            } else {
                pen.after(100, () -> realMenu(pen, attempt + 1));
            }
            return;
        }
        ServerPlayer who = player;
        Horse horse = DebugPenManager.spawnHorse(level, who.getBlockY(), who.getX() + 1.5, who.getZ(), Sex.FEMALE,
                BAY, true);
        if (horse == null) {
            pen.answer(MENU, null, "the horse did not spawn beside the player");
            pen.answer(HOSTED, null, "the horse did not spawn beside the player");
            return;
        }
        DebugTestYard.label(horse, "PACK REAL MENU");
        horse.setNoAi(true);
        horse.setOwner(who);
        HorseTackSlot.HARNESS.set(horse, stack(ModItems.IRON_STORAGE_HARNESS.get(), 1));
        HorseTackSlot.SADDLEBAG_LEFT.set(horse, stack(Items.CHEST, 1));

        boolean opened = HorsePackHandler.open(who, horse, HorseTackSlot.SADDLEBAG_LEFT);
        if (!opened || !(who.containerMenu instanceof ChestMenu)) {
            pen.answer(MENU, false, "open() returned " + opened + " and the player's menu is "
                    + who.containerMenu.getClass().getSimpleName() + " at " + f3(Math.sqrt(horse.distanceToSqr(who)))
                    + " blocks");
            hosted(pen, who, horse);
            return;
        }
        who.containerMenu.getSlot(4).set(stack(Items.IRON_INGOT, 10));
        pen.after(40, () -> {
            boolean still = who.containerMenu instanceof ChestMenu;
            int counted = packs(horse).count(HorseTackSlot.SADDLEBAG_LEFT.name());
            int synced = horse.getData(ModAttachments.HORSE_PACK_LOAD).left();
            pen.answer(MENU, still && counted == 10 && synced == 10, "forty ticks on: the menu is "
                    + (still ? "still open" : "CLOSED") + "; the horse counts " + counted + " items and tells clients "
                    + synced);
            who.closeContainer();
            pen.after(5, () -> hosted(pen, who, horse));
        });
    }

    /** The hosted path, driven with a vanilla barrel: {@code hosts()} is the only thing that keeps vanilla off it. */
    private static void hosted(Pen pen, ServerPlayer who, Horse horse) {
        ServerLevel level = pen.level;
        HorseTackSlot right = HorseTackSlot.SADDLEBAG_RIGHT;
        BlockPos bottom = new BlockPos(who.getBlockX(), level.getMinY(), who.getBlockZ());
        BlockState below = level.getBlockState(bottom);
        BlockState above = level.getBlockState(bottom.above());
        ItemStack face = HostedPacks.adopt(horse, right, stack(Items.BARREL, 1));
        if (face == null) {
            pen.answer(HOSTED, false, "a barrel would not be carried whole (adopt returned null)");
            return;
        }
        horse.setData(ModAttachments.HORSE_GEAR, horse.getData(ModAttachments.HORSE_GEAR).with(right.name(), face));
        if (!HostedPacks.open(who, horse, right) || !level.getBlockState(bottom).is(Blocks.BARREL)) {
            pen.answer(HOSTED, false, "it would not open as its block; the bottom of the world at " + bottom + " is "
                    + level.getBlockState(bottom));
            return;
        }
        double down = who.getY() - bottom.getY();
        who.containerMenu.getSlot(0).set(stack(Items.GOLD_INGOT, 7));
        pen.after(40, () -> {
            boolean still = who.containerMenu != who.inventoryMenu;
            int counted = packs(horse).count(right.name());
            who.closeContainer();
            pen.after(5, () -> {
                boolean restored = level.getBlockState(bottom) == below && level.getBlockState(bottom.above()) == above
                        && HostedPacks.standing(level.getServer()) == 0;
                pen.answer(HOSTED, still && counted == 7 && restored, "a barrel stood " + f3(down)
                        + " blocks under the player; forty real ticks on the menu was " + (still ? "still open" : "CLOSED")
                        + (down > 9.0 ? " (far outside block reach, so the mixin kept it)" : " (within block reach -"
                        + " this run does not prove the mixin)") + "; the horse counted " + counted
                        + " items; after closing the world was " + (restored ? "put back" : "NOT put back: "
                        + level.getBlockState(bottom) + " / " + level.getBlockState(bottom.above())));
                horse.discard();
            });
        });
    }
}
