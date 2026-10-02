package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


/**
 * <b>Rows AM-AQ: the clockwork pens - every open check a pair of hands could settle, done by a FakePlayer on a
 * clock</b> (owner, 2026-09-30: <i>"remove everything in the debug yard which can't be tested by you without my
 * intervention, and then add in new pens which you can test without me"</i>).
 *
 * <p>The yard is tested by launching the game and reading the log, by a session that never sees the screen. So a pen
 * belongs here only if it starts itself and writes its own answer, and each check below ends in one line:
 * {@code [trace] test yard | CLOCKWORK PASS | <check> | <what was read>} (or {@code FAIL}, or {@code INCONCLUSIVE}
 * when the setup, not the gene, is what went wrong). {@link #summary} prints the tally twice, at ten and thirty
 * minutes, with every check that has not answered named as {@code PENDING} - a check that never reports is a finding
 * too, and it must not read as a pass by being absent.
 *
 * <h2>The hands</h2>
 * The old sheep-spawner pens (DebugYardHands, deleted 2026-09-30) fed wheat with a NeoForge {@link FakePlayer}. {@link Hands} is the same
 * thing with one addition that makes the rest of this file possible: <b>it keeps what it is told.</b> A stock
 * FakePlayer drops every system message, and for the milk refusals, the diets and the seed jar <i>the message is the
 * evidence</i> - a refusal that says nothing and a gene that never fired both leave a bottle in the hand. Each use gets
 * a fresh pair, so what one heard cannot leak into the next reading. They share one fixed UUID, so a horse tamed to
 * the hands at build time is still theirs when the clock reaches it.
 *
 * <p><b>What the hands cannot do</b>, which is why those checks are the test kit's (batch 5) and not here: a FakePlayer
 * is not in the level's player list, so it cannot ride ({@code startRiding} is refused), no aura that scans for players
 * finds it (healer), a love cause pointing at it resolves to nobody (so the same-sex breeding <i>message</i> goes
 * unsent, though the no-foal half is checked), and nothing that looks for an owner by entity finds it (guardian).
 *
 * <p><b>It builds no pens now</b> (2026-09-30): every one passed and was closed - see {@link #build}.
 */
final class DebugYardClockwork {

    private DebugYardClockwork() {
    }

    // ------------------------------------------------------------------
    // Verdicts
    // ------------------------------------------------------------------

    /** Every check this yard asks, in the order they were registered, and what each has said so far. */
    private static final Map<String, String> RESULTS = new LinkedHashMap<>();

    /** Forget the last yard's verdicts. Called before anything is built, since other rows register checks too. */
    static void reset() {
        RESULTS.clear();
    }

    /** Name a check before it runs, so a check that never reports shows as PENDING rather than as nothing. */
    static void expect(String check) {
        RESULTS.putIfAbsent(check, "PENDING");
    }

    static void verdict(String check, boolean pass, String detail) {
        record(check, pass ? "PASS" : "FAIL", detail);
    }

    /** The setup failed (a horse missing, a husk dead of something else) - not evidence either way. */
    static void inconclusive(String check, String detail) {
        record(check, "INCONCLUSIVE", detail);
    }

    private static void record(String check, String result, String detail) {
        RESULTS.put(check, result);
        ActionTrace.log("test yard", "CLOCKWORK " + result + " | " + check + " | " + detail);
    }

    private static void summary(String when) {
        Map<String, List<String>> by = new TreeMap<>();
        for (Map.Entry<String, String> e : RESULTS.entrySet()) {
            by.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : by.entrySet()) {
            sb.append(sb.length() == 0 ? "" : ", ").append(e.getValue().size()).append(' ').append(e.getKey());
        }
        ActionTrace.log("test yard", "CLOCKWORK SUMMARY (" + when + "): " + sb);
        for (String state : List.of("FAIL", "INCONCLUSIVE", "PENDING")) {
            if (by.containsKey(state)) {
                ActionTrace.log("test yard", "CLOCKWORK SUMMARY " + state + ": " + String.join(" / ", by.get(state)));
            }
        }
    }

    // ------------------------------------------------------------------
    // The hands
    // ------------------------------------------------------------------

    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("horsegenetics:yard-hands".getBytes(StandardCharsets.UTF_8)), "[YardHands]");

    /**
     * A FakePlayer that remembers what it was told: the translation key where there is one (the server may not have
     * the mod's language loaded, so the key is the stable half), then the text as rendered.
     */
    static final class Hands extends FakePlayer {
        final List<String> heard = new ArrayList<>();

        Hands(ServerLevel level) {
            super(level, PROFILE);
        }

        @Override
        public void sendSystemMessage(Component message, boolean overlay) {
            heard.add(message.getContents() instanceof TranslatableContents t
                    ? t.getKey() + " \"" + message.getString() + "\"" : message.getString());
        }

        boolean heardAny(String... fragments) {
            for (String line : heard) {
                for (String f : fragments) {
                    if (line.contains(f)) {
                        return true;
                    }
                }
            }
            return false;
        }

        int count(Item item) {
            int n = 0;
            for (int i = 0; i < getInventory().getContainerSize(); i++) {
                ItemStack s = getInventory().getItem(i);
                if (s.is(item)) {
                    n += s.getCount();
                }
            }
            return n;
        }

        /** The first stack of {@code item} anywhere in the inventory, or empty. */
        ItemStack first(Item item) {
            for (int i = 0; i < getInventory().getContainerSize(); i++) {
                ItemStack s = getInventory().getItem(i);
                if (s.is(item)) {
                    return s;
                }
            }
            return ItemStack.EMPTY;
        }

        String said() {
            return heard.isEmpty() ? "(said nothing)" : "said " + heard;
        }
    }

    /** Fresh hands beside {@code at}, holding {@code held}; {@code creative} is the instabuild flag, nothing more. */
    static Hands hands(ServerLevel level, Entity at, ItemStack held, boolean creative) {
        Hands h = new Hands(level);
        h.getAbilities().instabuild = creative;
        h.snapTo(at.getX(), at.getY(), at.getZ() - 1.5, 0.0F, 0.0F);
        h.setItemInHand(InteractionHand.MAIN_HAND, held);
        return h;
    }

    /**
     * Right-click {@code target} the way the server does for a real player - {@code Player.interactOn}, which posts
     * {@code EntityInteract} first and falls through to the entity's own {@code interact} when nothing cancels it.
     * The yard's older fake-player pens posted only the event, which skips the vanilla half (feeding wheat, golden
     * carrots); this one takes the same path a click does. The hit location is unused by a horse.
     */
    static InteractionResult use(Hands h, Entity target) {
        return h.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO);
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    /**
     * <b>Nothing to build (2026-09-30).</b> All forty-five checks passed on the fifth launch, the owner
     * ruled that a clockwork PASS closes a check, and a pen for a closed question is deleted - so every
     * pen that stood in rows J west and AM-AQ went, and their results were written up on the Coding tab of
     * the page each check belonged to. What stays is the part the next open check will want: the hands
     * that keep what they are told, {@link #use}, the verdict lines and their summary, and the glass cell.
     * A new clockwork pen is a method here, called from this, calling {@link #expect} at build time and
     * {@link #verdict} when it knows - and {@link #scheduleSummaries} once, after the last of them.
     */
    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        if (!RESULTS.isEmpty()) {
            scheduleSummaries(level);
        }
    }

    /**
     * The tally at ten and thirty minutes, naming every check that has not answered as PENDING - and, since rows
     * AR-AW (2026-10-01) brought checks that run for hours (bond decay, the foal names, the stud pens), at two, four
     * and seven hours too. A short check still PENDING at thirty minutes is a finding; a long one is not yet.
     */
    static void scheduleSummaries(ServerLevel level) {
        DebugYardHerd.after(level, 12_000, () -> summary("10 minutes"));
        DebugYardHerd.after(level, 36_000, () -> summary("30 minutes - every short check should have answered"));
        DebugYardHerd.after(level, 144_000, () -> summary("2 hours"));
        DebugYardHerd.after(level, 288_000, () -> summary("4 hours"));
        DebugYardHerd.after(level, 504_000, () -> summary("7 hours - every check should have answered"));
    }

    static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         String label) {
        return DebugYardUnattended.horse(level, gy, x, z, sex, code, true, label);
    }

    /**
     * A glass box, walls two high, with a stone lid - one around the whole inclusive rectangle given. A monster
     * in a 3x3 one stays put and cannot see the sky; a horse in a 4x4 one has a 2x2 floor to stand on.
     */
    static void cell(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = gy + 1; y <= gy + 2; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            wall ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(new BlockPos(x, gy + 3, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
    }

    /** A persistent, named mob in a 3x3 glass cell centred on {@code (x, z)}. */
    static @Nullable Mob caged(ServerLevel level, EntityType<? extends Mob> type, int gy, int x, int z,
                                       String label) {
        cell(level, gy, x - 1, x + 1, z - 1, z + 1);
        Mob m = type.create(level, EntitySpawnReason.COMMAND);
        if (m == null) {
            return null;
        }
        m.setPos(x + 0.5, gy + 1, z + 0.5);
        // Or it despawns: nobody is within 128 blocks of a pen nobody is watching.
        m.setPersistenceRequired();
        m.setCustomName(Component.literal(label));
        level.addFreshEntity(m);
        return m;
    }
}
