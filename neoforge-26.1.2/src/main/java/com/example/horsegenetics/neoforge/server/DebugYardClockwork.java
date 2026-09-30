package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_J_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AM;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AM_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AN_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AO;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AO_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AP;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AP_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AQ;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AQ_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

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
 * {@link DebugYardHands} already fed the sheep spawner with a NeoForge {@link FakePlayer}. {@link Hands} is the same
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
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>J</td><td>MILK BY CLOCK</td><td>(the infirmary, {@link DebugYardCombat})</td></tr>
 *   <tr><td>AM</td><td>CLEANSING; CLEANSING CARRIER</td><td>ALARM NEAR; ALARM CARRIER</td></tr>
 *   <tr><td>AN</td><td>EGG MISMATCH; EGG CARRIER</td><td>EGG LEATHER; EGG PEARL</td></tr>
 *   <tr><td>AO</td><td>BIRD BONED DROPS</td><td>BLIGHT; BLIGHT CONTROL</td></tr>
 *   <tr><td>AP</td><td>DAIRY BY CLOCK</td><td>DIET BY CLOCK</td></tr>
 *   <tr><td>AQ</td><td>SEED JAR</td><td>SAME SEX MARES; SAME SEX STUDS; ALARM QUIET</td></tr>
 * </table>
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
        ORBS_SEEN.clear();
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
    private static Hands hands(ServerLevel level, Entity at, ItemStack held, boolean creative) {
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
    private static InteractionResult use(Hands h, Entity target) {
        return h.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO);
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN;
        try {
            cleansing(level, gy, west, mouthZ + ROW_AM, "CLEANSING", "Cln/Cln", true);
            cleansing(level, gy, west + 10, mouthZ + ROW_AM, "CLEANSING CARRIER", "Cln/n", false);
            alarm(level, gy, east, mouthZ + ROW_AM, 9, ROW_AM_D, "ALARM NEAR", "Alm/Alm", true);
            alarm(level, gy, east + 10, mouthZ + ROW_AM, 9, ROW_AM_D, "ALARM CARRIER", "Alm/n", true);

            eggs(level, gy, west, mouthZ + ROW_AN, "EGG MISMATCH", "Egg/Lthr", null);
            eggs(level, gy, west + 10, mouthZ + ROW_AN, "EGG CARRIER", "Egg/n", null);
            eggs(level, gy, east, mouthZ + ROW_AN, "EGG LEATHER", "Lthr/Lthr", Items.LEATHER);
            eggs(level, gy, east + 10, mouthZ + ROW_AN, "EGG PEARL", "Endp/Endp", Items.ENDER_PEARL);

            birdBoned(level, gy, west, mouthZ + ROW_AO);
            blight(level, gy, east, mouthZ + ROW_AO, "BLIGHT", "horsegenetics.blight=Bli/Bli", true);
            blight(level, gy, east + 10, mouthZ + ROW_AO, "BLIGHT CONTROL", "horsegenetics.blight=n/n", false);

            dairy(level, gy, west, mouthZ + ROW_AP);
            diet(level, gy, east, mouthZ + ROW_AP);
            // Row J's west block, empty since the guardian arena went.
            milk(level, gy, west, mouthZ + ROW_J);

            seedJar(level, gy, west, mouthZ + ROW_AQ);
            sameSex(level, gy, east, mouthZ + ROW_AQ, "SAME SEX MARES", Sex.FEMALE, 300);
            sameSex(level, gy, east + 7, mouthZ + ROW_AQ, "SAME SEX STUDS", Sex.MALE, 1_000);
            // SIXTEEN BLOCKS FROM ANYTHING HOSTILE, which is why it is here and not beside its two partners in AM:
            // hostile_near scans sixteen blocks for any monster, walls or not, and this pen is the control that
            // separates "the flag works" from "the flag is stuck true".
            alarm(level, gy, east + 14, mouthZ + ROW_AQ, 5, ROW_AQ_D, "ALARM QUIET", "Alm/Alm", false);

            DebugYardHerd.after(level, 12_000, () -> summary("10 minutes"));
            DebugYardHerd.after(level, 36_000, () -> summary("30 minutes - every check should have answered"));
            ActionTrace.log("test yard", "clockwork pens built (rows AM-AQ) - " + RESULTS.size()
                    + " checks registered; grep CLOCKWORK");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AM-AQ (clockwork) failed to build", e);
        }
    }

    private static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         String label) {
        return DebugYardUnattended.horse(level, gy, x, z, sex, code, true, label);
    }

    private static void floor(ServerLevel level, int gy, int x0, int z0, int width, int depth, BlockState state) {
        for (int x = x0 + 1; x < x0 + width; x++) {
            for (int z = z0 + 1; z < z0 + depth; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, state);
            }
        }
    }

    // ------------------------------------------------------------------
    // Row AM: cleansing light, beside caged undead
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-cleansing-light.html#verification}: the aura has never been seen killing anything, the undead
     * filter is a tag and not a list, and an aura kill drops no experience. The horse stands in a glass ring and four
     * mobs in glass cells about three and a half blocks off it - inside the smallest radius the allele can roll (4) -
     * so the answer does not depend on where a horse happened to wander. A zombie, a skeleton and a drowned must die; a
     * creeper, which is not undead, must be untouched. The carrier's zombie must be untouched too.
     *
     * <p>Every cell has a stone lid: under glass a zombie still sees the sky and burns at dawn, which would read as the
     * aura killing it.
     */
    private static void cleansing(ServerLevel level, int gy, int x0, int z0, String name, String pair, boolean full) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AM_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                full ? List.of(name, "Cln/Cln: caged", "undead must die,", "the creeper live")
                        : List.of(name, "Cln/n: the caged", "zombie must be", "untouched"));
        cell(level, gy, x0 + 3, x0 + 6, z0 + 4, z0 + 7);
        Horse h = horse(level, gy, x0 + 5.0, z0 + 6.0, Sex.FEMALE, "horsegenetics.cleansing_light=" + pair, name);
        Map<String, Mob> mobs = new LinkedHashMap<>();
        // THE CARRIER'S ZOMBIE ON ITS FAR SIDE (2026-09-30, second run). At x0 + 1 it stood six and a half
        // blocks from the Cln/Cln horse in the next pen, whose radius rolls 4 to 8 per horse: the first run
        // rolled short and passed, the second rolled long and killed it - a FAIL that was the pen, not the gene.
        // At x0 + 8 it is thirteen from that horse, past any roll.
        mobs.put("zombie", caged(level, EntityType.ZOMBIE, gy, full ? x0 + 1 : x0 + 8, z0 + 6, name + " ZOMBIE"));
        if (full) {
            mobs.put("skeleton", caged(level, EntityType.SKELETON, gy, x0 + 8, z0 + 6, name + " SKELETON"));
            mobs.put("drowned", caged(level, EntityType.DROWNED, gy, x0 + 5, z0 + 2, name + " DROWNED"));
            mobs.put("creeper", caged(level, EntityType.CREEPER, gy, x0 + 5, z0 + 9, name + " CREEPER"));
        }
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 4, z0 + ROW_AM_D);
        String kills = name + " - an aura kills caged undead nobody touched";
        String filter = name + " - the undead filter spares a creeper";
        String orbs = name + " - an aura kill drops no experience";
        String carrier = name + " - one copy does nothing to a caged zombie";
        if (full) {
            expect(kills);
            expect(filter);
            expect(orbs);
        } else {
            expect(carrier);
        }
        DebugYardHerd.after(level, 7_200, () -> {
            if (h == null || !h.isAlive()) {
                inconclusive(full ? kills : carrier, "the horse is gone");
                return;
            }
            StringBuilder read = new StringBuilder();
            for (Map.Entry<String, Mob> e : mobs.entrySet()) {
                Mob m = e.getValue();
                read.append(read.length() == 0 ? "" : ", ").append(e.getKey()).append(' ')
                        .append(m == null ? "never spawned" : m.isAlive()
                                ? String.format("%.0f/%.0f", m.getHealth(), m.getMaxHealth()) : "DEAD");
            }
            if (!full) {
                Mob z = mobs.get("zombie");
                if (z == null) {
                    inconclusive(carrier, "no zombie spawned");
                } else {
                    verdict(carrier, z.isAlive() && z.getHealth() >= z.getMaxHealth(), read.toString());
                }
                return;
            }
            boolean undeadDead = true;
            for (String k : List.of("zombie", "skeleton", "drowned")) {
                Mob m = mobs.get(k);
                undeadDead &= m != null && !m.isAlive();
            }
            verdict(kills, undeadDead, read + " after six minutes");
            Mob creeper = mobs.get("creeper");
            if (creeper == null) {
                inconclusive(filter, "no creeper spawned");
            } else {
                verdict(filter, creeper.isAlive() && creeper.getHealth() >= creeper.getMaxHealth(), read.toString());
            }
            // Orbs last five minutes, and the zombie is the first to go - so they are counted at every beat until
            // one is found, and here the running count is read.
            if (!undeadDead) {
                inconclusive(orbs, "nothing was killed to drop any");
            } else {
                verdict(orbs, ORBS_SEEN.getOrDefault(name, 0) == 0,
                        ORBS_SEEN.getOrDefault(name, 0) + " experience orb(s) seen in the pen");
            }
        });
        watchOrbs(level, name, box, 1);
    }

    /** Experience orbs seen in each cleansing pen, by pen name - the most seen at any one look. */
    private static final Map<String, Integer> ORBS_SEEN = new HashMap<>();

    private static void watchOrbs(ServerLevel level, String name, AABB box, int n) {
        DebugYardHerd.after(level, 200, () -> {
            int orbs = level.getEntitiesOfClass(ExperienceOrb.class, box.inflate(0.0, 2.0, 0.0)).size();
            ORBS_SEEN.merge(name, orbs, Math::max);
            if (n < 36) {
                watchOrbs(level, name, box, n + 1);
            }
        });
    }

    /**
     * A glass box, walls two high, with a stone lid - one around the whole inclusive rectangle given. A monster
     * in a 3x3 one stays put and cannot see the sky; a horse in a 4x4 one has a 2x2 floor to stand on.
     */
    private static void cell(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
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
    private static @Nullable Mob caged(ServerLevel level, EntityType<? extends Mob> type, int gy, int x, int z,
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

    // ------------------------------------------------------------------
    // Rows AM and AQ: base alarm
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-base-alarm.html#verification}: the alarm has never been heard firing, and its sound is a
     * vanilla horse noise, so only a count can tell. The census tallies gene sounds by id across the whole dimension,
     * which cannot separate these three pens, so {@link DebugWorldWatch#soundsFrom} counts per horse. Beside a caged
     * husk an Alm/Alm and an Alm/n horse must both sound, and no more often than the per-horse cooldown allows; the
     * QUIET pen, sixteen blocks from anything hostile, must stay silent.
     */
    private static void alarm(ServerLevel level, int gy, int x0, int z0, int width, int depth, String name,
                              String pair, boolean hostile) {
        DebugYardUnattended.pen(level, gy, x0, z0, width, depth, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                hostile ? List.of(name, pair + " beside a", "caged husk: the", "alarm must sound")
                        : List.of(name, pair + ", nothing", "hostile within 16:", "must stay SILENT"));
        Horse h = horse(level, gy, x0 + 2.5, z0 + 3.5, Sex.FEMALE, "horsegenetics.base_alarm=" + pair, name);
        Mob husk = hostile ? caged(level, EntityType.HUSK, gy, x0 + width - 2, z0 + depth - 3, name + " HUSK") : null;
        String check = hostile ? name + " - " + pair + " sounds with a hostile in range, within its cooldown"
                : name + " - silent with nothing hostile in range";
        expect(check);
        long span = 6_000;
        DebugYardHerd.after(level, span, () -> {
            if (h == null || !h.isAlive() || (hostile && (husk == null || !husk.isAlive()))) {
                inconclusive(check, "the horse or its husk is gone");
                return;
            }
            int n = DebugWorldWatch.soundsFrom(h.getUUID());
            // One whinny per COOLDOWN_TICKS at most, and a couple of spare for where the first one landed.
            long cap = span / com.example.horsegenetics.common.genetics.genes.BaseAlarmGene.COOLDOWN_TICKS + 2;
            verdict(check, hostile ? n >= 1 && n <= cap : n == 0,
                    n + " gene sound(s) in five minutes" + (hostile ? " (cap " + cap + ")" : ""));
        });
    }

    // ------------------------------------------------------------------
    // Row AN: the egg layer's silent cases
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-egg-layer.html#verification}: every combination that must lay nothing, and the two alleles
     * nobody has watched lay. Each pen's floor is swept for item entities every thirty seconds and every one seen is
     * remembered by UUID - an item despawns after five minutes, and a count would miss one that came and went between
     * two readings. Read after twenty-five minutes, which is past the longest interval the locus can roll.
     *
     * @param want the item that must appear, or {@code null} for a pen that must stay empty
     */
    private static void eggs(ServerLevel level, int gy, int x0, int z0, String name, String pair, @Nullable Item want) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AN_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                want == null ? List.of(name, pair + ": must", "lay NOTHING in", "25 minutes")
                        : List.of(name, pair + ": lays", BuiltInRegistries.ITEM.getKey(want).getPath(), "and nothing else"));
        horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.egg_layer=" + pair, name);
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_AN_D).inflate(0.0, 2.0, 0.0);
        String check = name + " - " + pair + (want == null ? " lays nothing"
                : " lays " + BuiltInRegistries.ITEM.getKey(want));
        expect(check);
        sweep(level, name, box, new HashMap<>(), 1, 50, seen -> {
            Map<String, Integer> byId = new TreeMap<>();
            seen.values().forEach(id -> byId.merge(id, 1, Integer::sum));
            if (want == null) {
                verdict(check, byId.isEmpty(), byId.isEmpty() ? "nothing on the floor in 25 minutes" : "laid " + byId);
            } else {
                String id = BuiltInRegistries.ITEM.getKey(want).toString();
                verdict(check, byId.containsKey(id) && byId.size() == 1,
                        byId.isEmpty() ? "nothing on the floor in 25 minutes" : "laid " + byId);
            }
        });
    }

    private static void sweep(ServerLevel level, String name, AABB box, Map<UUID, String> seen, int n, int of,
                              java.util.function.Consumer<Map<UUID, String>> done) {
        DebugYardHerd.after(level, 600, () -> {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                if (seen.putIfAbsent(item.getUUID(),
                        BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString()) == null) {
                    ActionTrace.log("test yard", name + ": an item appeared - " + item.getItem().getCount() + "x "
                            + BuiltInRegistries.ITEM.getKey(item.getItem().getItem()));
                }
            }
            if (n < of) {
                sweep(level, name, box, seen, n + 1, of, done);
            } else {
                done.accept(seen);
            }
        });
    }

    // ------------------------------------------------------------------
    // Row AO west: bird boned, dropped
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-bird-boned.html#verification}: the rider's half is confirmed, and what is open is the control -
     * a carrier must still be hurt by a fall, and an expressing horse must still be hurt by anything that is not one.
     * Three horses in a dry pen (healing needs water in reach, so nothing refills them between drops) are lifted
     * sixteen blocks and let go three times. A plain horse is dropped too, because if IT is not hurt the
     * drop is too short and nothing else here means anything.
     */
    private static void birdBoned(ServerLevel level, int gy, int x0, int z0) {
        String name = "BIRD BONED DROPS";
        DebugYardUnattended.pen(level, gy, x0, z0, 19, ROW_AO_D, name, Blocks.SMOOTH_STONE.defaultBlockState(),
                List.of(name, "dropped 16 x3:", "Brd/Brd unhurt;", "Brd/n + plain hurt"));
        DebugTestYard.dryPen(level, gy, x0, z0);
        double mz = z0 + ROW_AO_D / 2.0;
        Horse pair = horse(level, gy, x0 + 4.5, mz, Sex.FEMALE, "horsegenetics.bird_boned=Brd/Brd", "BIRD Brd/Brd");
        Horse carrier = horse(level, gy, x0 + 10.5, mz, Sex.FEMALE, "horsegenetics.bird_boned=Brd/n", "BIRD Brd/n");
        Horse plain = horse(level, gy, x0 + 16.5, mz, Sex.FEMALE, "horsegenetics.bird_boned=n/n", "BIRD n/n");
        String immune = name + " - Brd/Brd takes no fall damage";
        String carrierHurt = name + " - a Brd/n carrier is hurt by a fall";
        String otherHurt = name + " - Brd/Brd still takes damage that is not a fall";
        expect(immune);
        expect(carrierHurt);
        expect(otherHurt);
        float[] took = new float[3];     // total damage over the drops, or -1 once dead
        drop(level, gy, name, new Horse[] {pair, carrier, plain}, took, 1, () -> {
            if (took[2] == 0.0F) {
                inconclusive(immune, "the plain horse was not hurt either - the drop is too short to test anything");
                inconclusive(carrierHurt, "as above");
            } else {
                String read = String.format("damage over three drops: Brd/Brd %s, Brd/n %s, n/n %s",
                        dmg(took[0]), dmg(took[1]), dmg(took[2]));
                verdict(immune, took[0] == 0.0F, read);
                verdict(carrierHurt, took[1] != 0.0F, read);
            }
            if (pair == null || !pair.isAlive()) {
                inconclusive(otherHurt, "the Brd/Brd horse is gone");
                return;
            }
            float before = pair.getHealth();
            pair.hurtServer(level, level.damageSources().generic(), 2.0F);
            float after = pair.getHealth();
            verdict(otherHurt, after <= before - 1.9F, String.format("generic damage 2: %.1f -> %.1f", before, after));
        });
    }

    private static String dmg(float d) {
        return d < 0 ? "DIED" : String.format("%.1f", d);
    }

    private static void drop(ServerLevel level, int gy, String name, Horse[] horses, float[] took, int n,
                             Runnable done) {
        DebugYardHerd.after(level, n == 1 ? 600 : 2_400, () -> {
            float[] before = new float[horses.length];
            for (int i = 0; i < horses.length; i++) {
                Horse h = horses[i];
                if (h != null && h.isAlive()) {
                    before[i] = h.getHealth();
                    h.teleportTo(h.getX(), gy + 17, h.getZ());
                    h.resetFallDistance();
                }
            }
            DebugYardHerd.after(level, 100, () -> {
                StringBuilder read = new StringBuilder();
                for (int i = 0; i < horses.length; i++) {
                    Horse h = horses[i];
                    if (h == null || took[i] < 0) {
                        continue;
                    }
                    if (!h.isAlive()) {
                        took[i] = -1.0F;
                    } else {
                        took[i] += Math.max(0.0F, before[i] - h.getHealth());
                    }
                    read.append(read.length() == 0 ? "" : ", ").append(h.getCustomName() == null ? "?"
                            : h.getCustomName().getString()).append(' ').append(h.isAlive()
                            ? String.format("%.1f -> %.1f", before[i], h.getHealth()) : "DIED");
                }
                ActionTrace.log("test yard", name + " drop " + n + ": " + read);
                if (n < 3) {
                    drop(level, gy, name, horses, took, n + 1, done);
                } else {
                    done.run();
                }
            });
        });
    }

    // ------------------------------------------------------------------
    // Row AO east: blight
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-blight.html#verification}: a Bli/Bli horse must turn grass, moss, podzol and mycelium to dirt
     * and wither the wild growth on them, and must not touch a crop or a potted plant. The floor is striped with all
     * four covers, a strip of wheat on farmland and a potted dandelion, and read back after six minutes. The control
     * is the same floor under a plain horse: if the wheat goes there too, a hoof did it and not the gene.
     */
    private static void blight(ServerLevel level, int gy, int x0, int z0, String name, String code, boolean blights) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, ROW_AO_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                blights ? List.of(name, "covers go to dirt;", "the wheat and the", "pot must survive")
                        : List.of(name, "a plain horse on", "the same floor:", "nothing changes"));
        for (int x = x0 + 1; x < x0 + 9; x++) {
            for (int z = z0 + 1; z < z0 + ROW_AO_D; z++) {
                int row = z - z0;
                BlockState ground = row <= 2 ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : row <= 4 ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : row <= 6 ? Blocks.PODZOL.defaultBlockState()
                        : row <= 8 ? Blocks.MYCELIUM.defaultBlockState()
                        : row <= 10 ? Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7)
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                DebugPenManager.groundColumn(level, x, gy, z, ground);
                BlockState above = row == 2 ? ((x & 1) == 0 ? Blocks.SHORT_GRASS : Blocks.DANDELION).defaultBlockState()
                        : row == 9 || row == 10 ? ((CropBlock) Blocks.WHEAT).getStateForAge(3)
                        : row == 11 && x == x0 + 4 ? Blocks.POTTED_DANDELION.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(new BlockPos(x, gy + 1, z), above, 3);
            }
        }
        DebugTestYard.penWater(level, gy, x0 + 1, z0 + 1);
        Horse h = horse(level, gy, x0 + 4.5, z0 + 5.5, Sex.FEMALE, code, name);
        Map<Block, Integer> start = new LinkedHashMap<>();
        String eats = name + " - the gene converts ground cover (counted as it lands)";
        String spares = name + " - the wheat and the potted plant survive";
        String still = name + " - a plain horse leaves the floor as it was";
        if (blights) {
            expect(eats);
            expect(spares);
        } else {
            expect(still);
        }
        DebugYardHerd.after(level, 40, () -> start.putAll(blocks(level, gy, x0, z0)));
        // THE GENE'S OWN CONVERSIONS, not the floor (2026-09-30). Blight's dirt is bare ground in a lit yard
        // beside grass and mycelium, both of which spread back over it, and its two-block reach runs under the
        // pen walls: runs one to four read +4, +8, +0 and +1 dirt at six minutes (a peak of 2 on the fourth)
        // while the watch counted the gene landing sixteen to twenty-two conversions every two minutes. So the
        // verdict counts what the gene placed (DebugWorldWatch.spreadsPlacedBy); the floor stays in the
        // reading, and still decides the crop half and the control.
        int[] peakDirt = {0};
        for (long t = 200; t <= 7_200; t += 200) {
            DebugYardHerd.after(level, t, () -> peakDirt[0] = Math.max(peakDirt[0],
                    blocks(level, gy, x0, z0).getOrDefault(Blocks.DIRT, 0)));
        }
        DebugYardHerd.after(level, 7_210, () -> {
            Map<Block, Integer> end = blocks(level, gy, x0, z0);
            int covers0 = covers(start);
            int covers1 = covers(end);
            int dirt = end.getOrDefault(Blocks.DIRT, 0) - start.getOrDefault(Blocks.DIRT, 0);
            boolean cropsKept = end.getOrDefault(Blocks.WHEAT, 0).equals(start.getOrDefault(Blocks.WHEAT, 0))
                    && end.getOrDefault(Blocks.POTTED_DANDELION, 0).equals(start.getOrDefault(Blocks.POTTED_DANDELION, 0));
            int placed = h == null ? 0 : DebugWorldWatch.spreadsPlacedBy(h.getUUID());
            String read = placed + " conversion(s) landed; covers " + covers0 + " -> " + covers1 + ", dirt " + (dirt >= 0 ? "+" : "") + dirt
                    + " (most seen at once " + peakDirt[0] + ")"
                    + ", growth " + growth(start) + " -> " + growth(end) + " | start " + names(start)
                    + " | now " + names(end);
            if (blights) {
                verdict(eats, placed >= 5, read);
                verdict(spares, cropsKept, read);
            } else {
                verdict(still, placed == 0 && peakDirt[0] - start.getOrDefault(Blocks.DIRT, 0) <= 1 && cropsKept, read);
            }
        });
    }

    private static final List<Block> COUNTED = List.of(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL,
            Blocks.MYCELIUM, Blocks.DIRT, Blocks.FARMLAND, Blocks.SHORT_GRASS, Blocks.DANDELION, Blocks.WHEAT,
            Blocks.POTTED_DANDELION);

    private static Map<Block, Integer> blocks(ServerLevel level, int gy, int x0, int z0) {
        Map<Block, Integer> out = new LinkedHashMap<>();
        for (int x = x0 + 1; x < x0 + 9; x++) {
            for (int z = z0 + 1; z < z0 + ROW_AO_D; z++) {
                for (int y = gy; y <= gy + 1; y++) {
                    Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (COUNTED.contains(b)) {
                        out.merge(b, 1, Integer::sum);
                    }
                }
            }
        }
        return out;
    }

    private static int covers(Map<Block, Integer> m) {
        return m.getOrDefault(Blocks.GRASS_BLOCK, 0) + m.getOrDefault(Blocks.MOSS_BLOCK, 0)
                + m.getOrDefault(Blocks.PODZOL, 0) + m.getOrDefault(Blocks.MYCELIUM, 0);
    }

    private static int growth(Map<Block, Integer> m) {
        return m.getOrDefault(Blocks.SHORT_GRASS, 0) + m.getOrDefault(Blocks.DANDELION, 0);
    }

    private static String names(Map<Block, Integer> m) {
        StringBuilder sb = new StringBuilder();
        m.forEach((b, n) -> sb.append(sb.length() == 0 ? "" : ", ").append(n).append(' ')
                .append(BuiltInRegistries.BLOCK.getKey(b).getPath()));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Row AP west: the dairy, by clock
    // ------------------------------------------------------------------

    /** One horse in the dairy: what it is, and what the bottle held out to it must come back as. */
    private record Milker(String label, Sex sex, String pair, boolean hurt, boolean foal, List<String> effects) {
    }

    /**
     * {@code wiki/gene-potion-milk.html#verification} and what the retired DAIRY + CLIP pen asked by hand: a hurt mare,
     * a stallion and a foal must each refuse a bottle AND say why - the message is the evidence, because a refusal
     * that says nothing and a gene that never fired both leave the bottle in your hand - while a healthy mare fills it.
     * Two mares are the merge nobody has bottled: a Spd/Str compound must give one potion with both effects, and the
     * Nightmare's Wkn/Drk the two curses, darkness included. No water in the pen, and the mare is hurt a second
     * before her bottle, so she is still hurt when it comes.
     */
    private static void dairy(ServerLevel level, int gy, int x0, int z0) {
        String name = "DAIRY BY CLOCK";
        DebugYardUnattended.pen(level, gy, x0, z0, 19, ROW_AP_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "a bottle to each:", "3 refuse + say why,", "3 fill (2 merged)"));
        DebugTestYard.dryPen(level, gy, x0, z0);
        List<Milker> cows = List.of(
                new Milker("HURT MARE", Sex.FEMALE, "Spd/Spd", true, false, List.of()),
                new Milker("STALLION", Sex.MALE, "Spd/Str", false, false, List.of()),
                new Milker("FOAL", Sex.FEMALE, "Spd/Spd", false, true, List.of()),
                new Milker("PLAIN MARE", Sex.FEMALE, "Spd/Spd", false, false, List.of("minecraft:speed")),
                new Milker("COMPOUND MARE", Sex.FEMALE, "Spd/Str", false, false,
                        List.of("minecraft:speed", "minecraft:strength")),
                new Milker("CURSED MARE", Sex.FEMALE, "Wkn/Drk", false, false,
                        List.of("minecraft:darkness", "minecraft:weakness")));
        for (int i = 0; i < cows.size(); i++) {
            Milker c = cows.get(i);
            Horse h = horse(level, gy, x0 + 2.5 + i * 3, z0 + 5.5, c.sex(), "horsegenetics.potion_milk=" + c.pair(),
                    "MILK " + c.label());
            String check = name + " - " + c.label() + " (" + c.pair() + ") "
                    + (c.effects().isEmpty() ? "refuses a bottle and says why" : "fills one bottle with " + c.effects());
            expect(check);
            if (h == null) {
                continue;
            }
            if (c.foal()) {
                h.setAge(-24_000);
            }
            if (c.hurt()) {
                // A second before the bottle, not at the build: a horse hurt at the build was back at full
                // health by the time the bottle came (2026-09-30), and what is being tested is the refusal.
                DebugYardHerd.after(level, 380 + i * 40L, () -> hurt(h));
            }
            DebugYardHerd.after(level, 400 + i * 40L, () -> bottle(level, check, h, c));
        }
    }

    private static void bottle(ServerLevel level, String check, Horse h, Milker c) {
        if (!h.isAlive()) {
            inconclusive(check, "the horse is gone");
            return;
        }
        Hands hands = hands(level, h, new ItemStack(Items.GLASS_BOTTLE), false);
        String state = String.format("health %.1f/%.1f, %s", h.getHealth(), h.getMaxHealth(),
                h.isBaby() ? "foal" : "adult");
        InteractionResult r = use(hands, h);
        ItemStack potion = hands.first(Items.POTION);
        List<String> got = new ArrayList<>();
        if (!potion.isEmpty()) {
            PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (MobEffectInstance e : contents.getAllEffects()) {
                    got.add(String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()))
                            + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : ""));
                }
            }
        }
        String read = state + "; the click said " + r + ", " + hands.said() + "; bottles left "
                + hands.count(Items.GLASS_BOTTLE) + ", potions " + hands.count(Items.POTION) + " " + got;
        if (c.effects().isEmpty()) {
            verdict(check, potion.isEmpty() && hands.count(Items.GLASS_BOTTLE) == 1 && !hands.heard.isEmpty(), read);
        } else {
            List<String> ids = new ArrayList<>();
            got.forEach(g -> ids.add(g.split(" ")[0]));
            verdict(check, hands.count(Items.POTION) == 1 && new TreeSet<>(ids).equals(new TreeSet<>(c.effects()))
                    && ids.size() == c.effects().size(), read);
        }
    }

    // ------------------------------------------------------------------
    // Row AP east: the diets, by clock
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-diet.html#verification}, every half of it that is a feeding: a lava-eater refuses an apple and
     * names its food, eats a lava bucket and hands back the empty bucket, and in creative eats one without it being
     * used up; a blood-drinker turns away food with its own line; a plain horse still heals on wheat. No water in the
     * pen and each patient hurt just before its meal, so a heal is the food and not the gated regen.
     */
    private static void diet(ServerLevel level, int gy, int x0, int z0) {
        String name = "DIET BY CLOCK";
        DebugYardUnattended.pen(level, gy, x0, z0, 19, ROW_AP_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "lava-eater, blood-", "drinker and plain", "horse, fed by clock"));
        DebugTestYard.dryPen(level, gy, x0, z0);
        Horse lava = horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.diet=Dlava/Dlava", "DIET LAVA");
        Horse blood = horse(level, gy, x0 + 9.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.diet=Dbld/Dbld", "DIET BLOOD");
        Horse plain = horse(level, gy, x0 + 15.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.diet=n/n", "DIET PLAIN");
        String refuse = name + " - a lava-eater refuses an apple and names its food";
        String eats = name + " - a lava-eater eats a lava bucket and hands back the bucket";
        String creative = name + " - in creative a lava bucket feeds it and is not used up";
        String bloodRefuses = name + " - a blood-drinker turns every food away with its own line";
        String wheat = name + " - a plain horse still heals on wheat";
        for (String c : List.of(refuse, eats, creative, bloodRefuses, wheat)) {
            expect(c);
        }
        // Each patient is hurt a second before it is fed - one hurt at the build was healed by the time the food
        // came (2026-09-30), and "ate it happily, but did not seem to need it" answers nothing.
        DebugYardHerd.after(level, 380, () -> hurt(lava));
        DebugYardHerd.after(level, 580, () -> hurt(plain));
        DebugYardHerd.after(level, 400, () -> feed(level, refuse, lava, Items.APPLE, false, (h, before, hands) ->
                verdict(refuse, h.getHealth() <= before + 0.01F && hands.count(Items.APPLE) == 1
                        && hands.heardAny("only eats"), state(h, before, hands, Items.APPLE))));
        DebugYardHerd.after(level, 440, () -> feed(level, eats, lava, Items.LAVA_BUCKET, false, (h, before, hands) ->
                verdict(eats, h.getHealth() > before + 0.01F && hands.count(Items.BUCKET) == 1
                        && hands.count(Items.LAVA_BUCKET) == 0, state(h, before, hands, Items.BUCKET))));
        DebugYardHerd.after(level, 480, () -> hurt(lava));
        DebugYardHerd.after(level, 520, () -> feed(level, creative, lava, Items.LAVA_BUCKET, true, (h, before, hands) ->
                verdict(creative, h.getHealth() > before + 0.01F && hands.count(Items.LAVA_BUCKET) == 1,
                        state(h, before, hands, Items.LAVA_BUCKET))));
        DebugYardHerd.after(level, 560, () -> feed(level, bloodRefuses, blood, Items.APPLE, false, (h, before, hands) ->
                verdict(bloodRefuses, hands.count(Items.APPLE) == 1 && hands.heardAny("Nothing you can hold out"),
                        state(h, before, hands, Items.APPLE))));
        DebugYardHerd.after(level, 600, () -> feed(level, wheat, plain, Items.WHEAT, false, (h, before, hands) ->
                verdict(wheat, h.getHealth() > before + 0.01F, state(h, before, hands, Items.WHEAT))));
    }

    private interface Fed {
        void read(Horse h, float before, Hands hands);
    }

    private static void hurt(@Nullable Horse h) {
        if (h != null && h.isAlive()) {
            h.setHealth(h.getMaxHealth() / 2.0F);
        }
    }

    private static void feed(ServerLevel level, String check, @Nullable Horse h, Item item, boolean creative, Fed then) {
        if (h == null || !h.isAlive()) {
            inconclusive(check, "the horse is gone");
            return;
        }
        float before = h.getHealth();
        Hands hands = hands(level, h, new ItemStack(item), creative);
        use(hands, h);
        then.read(h, before, hands);
    }

    private static String state(Horse h, float before, Hands hands, Item watched) {
        return String.format("health %.1f -> %.1f of %.1f; holding %s; %d %s in the inventory; %s", before,
                h.getHealth(), h.getMaxHealth(), BuiltInRegistries.ITEM.getKey(hands.getMainHandItem().getItem()),
                hands.count(watched), BuiltInRegistries.ITEM.getKey(watched).getPath(), hands.said());
    }

    // ------------------------------------------------------------------
    // Row AQ west: the seed jar
    // ------------------------------------------------------------------

    /**
     * {@code wiki/item-seed-jars.html#verification}, what the retired JAR STUD and JAR &amp; KIT BENCH asked a person
     * to do: an unfed stallion refuses the jar; a fed one fills it and is out of love afterwards; a mare out of heat
     * refuses the filled jar and it is kept; a mare in heat takes it; and the foal's record names the jar's stallion as
     * its sire. All three horses are tamed to the hands - the jar checks ownership by UUID, which is the one kind of
     * owner a FakePlayer can be. Natural covers are off for both mares, or the stud would breed the one in heat himself.
     */
    private static void seedJar(ServerLevel level, int gy, int x0, int z0) {
        String name = "SEED JAR";
        DebugYardUnattended.pen(level, gy, x0, z0, 19, ROW_AQ_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "unfed refuses; fed", "fills; out of heat", "refuses; in heat: foal"));
        String fert = "horsegenetics.fertility=n/n";
        Horse stud = horse(level, gy, x0 + 3.5, z0 + 5.5, Sex.MALE, fert, "JAR STUD");
        Horse out = horse(level, gy, x0 + 9.5, z0 + 5.5, Sex.FEMALE, fert, "JAR MARE OUT OF HEAT");
        Horse in = horse(level, gy, x0 + 15.5, z0 + 5.5, Sex.FEMALE, fert, "JAR MARE IN HEAT");
        String unfed = name + " - an unfed stallion refuses the jar and says so";
        String fills = name + " - a fed stallion fills the jar and leaves breeding mode";
        String outRefuses = name + " - a mare out of heat refuses the filled jar, which is kept";
        String inTakes = name + " - a mare in heat takes the jar and is pregnant";
        String sire = name + " - the jar foal's record names the jar stallion as sire";
        String gelded = name + " - the vet's kit gelds the stud, and a jar is refused after";
        for (String c : List.of(unfed, fills, outRefuses, inTakes, sire, gelded)) {
            expect(c);
        }
        if (stud == null || out == null || in == null) {
            inconclusive(unfed, "a horse failed to spawn");
            return;
        }
        Hands owner = new Hands(level);
        for (Horse h : List.of(stud, out, in)) {
            h.setTamed(true);
            h.setOwner(owner);
        }
        DebugYardFertility.outOfHeat(out);
        DebugYardFertility.inHeat(in);
        DebugYardFertility.noNaturalCovers(out);
        DebugYardFertility.noNaturalCovers(in);

        ItemStack[] jar = {ItemStack.EMPTY};
        DebugYardHerd.after(level, 300, () -> {
            Hands h = hands(level, stud, new ItemStack(ModItems.EMPTY_SEED_JAR.get()), false);
            use(h, stud);
            verdict(unfed, h.getMainHandItem().is(ModItems.EMPTY_SEED_JAR.get()) && h.heardAny("breeding mode"),
                    "holding " + BuiltInRegistries.ITEM.getKey(h.getMainHandItem().getItem()) + "; " + h.said());
        });
        DebugYardHerd.after(level, 340, () -> {
            Hands h = hands(level, stud, new ItemStack(Items.GOLDEN_CARROT), false);
            use(h, stud);
            boolean fed = stud.isInLove();
            if (!fed) {
                // The jar is the test, not the carrot: put him in love the way the carrot would have, and say so.
                stud.setInLove(null);
            }
            Hands j = hands(level, stud, new ItemStack(ModItems.EMPTY_SEED_JAR.get()), false);
            use(j, stud);
            jar[0] = j.first(ModItems.STALLION_SEED_JAR.get()).copy();
            verdict(fills, !jar[0].isEmpty() && !stud.isInLove(), "golden carrot " + (fed ? "put him in love"
                    : "did NOT put him in love (forced for the jar test)") + "; now holding "
                    + BuiltInRegistries.ITEM.getKey(j.getMainHandItem().getItem()) + ", in love after: "
                    + stud.isInLove() + "; " + j.said());
        });
        DebugYardHerd.after(level, 400, () -> {
            if (jar[0].isEmpty()) {
                inconclusive(outRefuses, "no filled jar to use");
                return;
            }
            Hands h = hands(level, out, jar[0].copy(), false);
            use(h, out);
            boolean pregnant = ReproHandler.of(out).pregnancy().isPresent();
            verdict(outRefuses, h.getMainHandItem().is(ModItems.STALLION_SEED_JAR.get()) && !pregnant,
                    "receptive " + ReproHandler.receptive(out) + ", pregnant " + pregnant + ", holding "
                            + BuiltInRegistries.ITEM.getKey(h.getMainHandItem().getItem()) + "; " + h.said());
        });
        // THE VET'S KIT, the last step the retired jar bench asked for (item-vet-kit.html): a sneak-use gelds him,
        // and the flag must reach his record - the jar reads the record, not the chat line.
        DebugYardHerd.after(level, 700, () -> {
            Hands kit = hands(level, stud, new ItemStack(ModItems.VET_KIT.get()), false);
            kit.setShiftKeyDown(true);
            use(kit, stud);
            boolean flagged = HorseRecords.of(stud).gelded();
            stud.setInLove(null);
            Hands j = hands(level, stud, new ItemStack(ModItems.EMPTY_SEED_JAR.get()), false);
            use(j, stud);
            verdict(gelded, flagged && j.getMainHandItem().is(ModItems.EMPTY_SEED_JAR.get())
                            && j.heardAny("gelding"),
                    "record gelded " + flagged + "; kit " + kit.said() + "; jar after: holding "
                            + BuiltInRegistries.ITEM.getKey(j.getMainHandItem().getItem()) + ", " + j.said());
        });
        DebugYardHerd.after(level, 460, () -> {
            if (jar[0].isEmpty()) {
                inconclusive(inTakes, "no filled jar to use");
                return;
            }
            boolean receptive = ReproHandler.receptive(in);
            Hands h = hands(level, in, jar[0].copy(), false);
            use(h, in);
            Optional<Pregnancy> p = ReproHandler.of(in).pregnancy();
            String read = "receptive " + receptive + ", jar " + (h.getMainHandItem().isEmpty() ? "used" : "kept")
                    + ", " + h.said();
            if (p.isEmpty()) {
                // A conception can fail on the dice; that is the gene working, not the jar.
                if (h.getMainHandItem().isEmpty()) {
                    inconclusive(inTakes, "the jar was used and did not take (fertility dice) - " + read);
                } else {
                    verdict(inTakes, false, read);
                }
                inconclusive(sire, "no pregnancy to follow");
                return;
            }
            verdict(inTakes, h.getMainHandItem().isEmpty(), read + ", due at tick " + p.get().dueTick());
            long wait = Math.max(200, p.get().dueTick() - level.getGameTime() + 600);
            if (wait > 30_000) {
                inconclusive(sire, "due in " + wait + " ticks, past the end of this run");
                return;
            }
            UUID mare = in.getUUID();
            UUID stallion = stud.getUUID();
            AABB near = DebugTestYard.box(x0 - 8, gy - 2, z0 - 8, x0 + 27, gy + 6, z0 + ROW_AQ_D + 8);
            DebugYardHerd.after(level, wait, () -> {
                List<String> found = new ArrayList<>();
                boolean right = false;
                for (Horse foal : level.getEntitiesOfClass(Horse.class, near, Horse::isAlive)) {
                    HorseRecord r = HorseRecords.of(foal);
                    if (r.motherId().filter(mare::equals).isPresent()) {
                        found.add(foal.getName().getString() + " sire " + r.fatherId().map(UUID::toString).orElse("none"));
                        right |= r.fatherId().filter(stallion::equals).isPresent();
                    }
                }
                if (found.isEmpty()) {
                    inconclusive(sire, "no foal of the jar mare found by tick " + level.getGameTime());
                } else {
                    verdict(sire, right, "stud " + stallion + "; foals " + found);
                }
            });
        });
    }

    // ------------------------------------------------------------------
    // Row J west: milk by the bucket
    // ------------------------------------------------------------------

    /**
     * {@code wiki/gene-milk.html#verification} and {@code wiki/gene-magic-milk-volume.html#verification}: the bucket
     * half of the retired dairy. A mare fills a bucket; a hurt mare, a stallion and a foal each refuse and say why; a
     * Watr/Watr stallion gives water, since the novelty milks are any grown horse's; a mare already milked refuses
     * until her cooldown is up. And a Mlk/Mlk mare must fill again after a fraction of a day - the volume locus
     * divides the cooldown rather than banking fillings - so she is tried every minute until she does, while the
     * plain mare beside her is the control that has not. No water in the pen: the hurt mare must stay hurt.
     */
    private static void milk(ServerLevel level, int gy, int x0, int z0) {
        String name = "MILK BY CLOCK";
        DebugYardUnattended.pen(level, gy, x0, z0, 19, ROW_J_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "a bucket to each:", "3 refuse + say why,", "Mlk/Mlk refills"));
        DebugTestYard.dryPen(level, gy, x0, z0);
        double mz = z0 + ROW_J_D / 2.0;
        Horse mare = horse(level, gy, x0 + 2.5, mz, Sex.FEMALE, "horsegenetics.milk=n/n", "MILK MARE");
        Horse volume = horse(level, gy, x0 + 5.5, mz, Sex.FEMALE, "horsegenetics.magic_milk_volume=Mlk/Mlk",
                "MILK Mlk/Mlk MARE");
        Horse hurt = horse(level, gy, x0 + 8.5, mz, Sex.FEMALE, "horsegenetics.milk=n/n", "MILK HURT MARE");
        Horse stallion = horse(level, gy, x0 + 11.5, mz, Sex.MALE, "horsegenetics.milk=n/n", "MILK STALLION");
        Horse foal = horse(level, gy, x0 + 14.5, mz, Sex.FEMALE, "horsegenetics.milk=n/n", "MILK FOAL");
        Horse water = horse(level, gy, x0 + 17.5, mz, Sex.MALE, "horsegenetics.milk=Watr/Watr", "MILK Watr STALLION");
        if (foal != null) {
            foal.setAge(-24_000);
        }
        bucket(level, 400, name + " - a mare fills a bucket with milk", mare, Items.MILK_BUCKET, null);
        bucket(level, 440, name + " - a Mlk/Mlk mare fills a bucket with milk", volume, Items.MILK_BUCKET, null);
        DebugYardHerd.after(level, 460, () -> hurt(hurt));
        bucket(level, 480, name + " - a hurt mare refuses and says why", hurt, null, "milk.hurt");
        bucket(level, 520, name + " - a stallion refuses and says why", stallion, null, "stallion");
        bucket(level, 560, name + " - a foal refuses and says why", foal, null, "foal");
        bucket(level, 600, name + " - a Watr/Watr stallion gives water", water, Items.WATER_BUCKET, null);
        bucket(level, 2_400, name + " - a mare milked two minutes ago refuses until her cooldown is up", mare, null,
                "recharging");
        String refill = name + " - a Mlk/Mlk mare fills again within a fraction of a day";
        expect(refill);
        refill(level, refill, volume, 1_640, 1_200);
    }

    /**
     * Hold a bucket out to {@code h} at {@code at}. Pass when {@code want} came back (and the bucket went), or - with
     * {@code want} null - when the bucket is kept and something was said containing {@code heard}.
     */
    private static void bucket(ServerLevel level, long at, String check, @Nullable Horse h, @Nullable Item want,
                               @Nullable String heard) {
        expect(check);
        DebugYardHerd.after(level, at, () -> {
            if (h == null || !h.isAlive()) {
                inconclusive(check, "the horse is gone");
                return;
            }
            Hands hands = hands(level, h, new ItemStack(Items.BUCKET), false);
            String state = String.format("health %.1f/%.1f, %s", h.getHealth(), h.getMaxHealth(),
                    h.isBaby() ? "foal" : "adult");
            use(hands, h);
            String read = state + "; holding " + BuiltInRegistries.ITEM.getKey(hands.getMainHandItem().getItem())
                    + ", buckets " + hands.count(Items.BUCKET) + ", milk " + hands.count(Items.MILK_BUCKET)
                    + ", water " + hands.count(Items.WATER_BUCKET) + "; " + hands.said();
            verdict(check, want != null ? hands.count(want) == 1 && hands.count(Items.BUCKET) == 0
                    : hands.count(Items.BUCKET) == 1 && hands.heardAny(heard), read);
        });
    }

    private static void refill(ServerLevel level, String check, @Nullable Horse h, long at, long every) {
        DebugYardHerd.after(level, at, () -> {
            if (h == null || !h.isAlive()) {
                inconclusive(check, "the horse is gone");
                return;
            }
            Hands hands = hands(level, h, new ItemStack(Items.BUCKET), false);
            use(hands, h);
            long since = at - 440;
            if (hands.count(Items.MILK_BUCKET) == 1) {
                verdict(check, since < 24_000, "filled again " + since + " ticks after the first milking (a day is "
                        + "24000; a plain mare waits the whole of it)");
            } else if (at + every - 440 >= 24_000) {
                verdict(check, false, "still refusing " + since + " ticks after the first milking: " + hands.said());
            } else {
                refill(level, check, h, at + every, every);
            }
        });
    }

    // ------------------------------------------------------------------
    // Row AQ east: a same-sex pair on golden carrots
    // ------------------------------------------------------------------

    /**
     * {@code wiki/item-breeding-carrots.html#verification}: a same-sex pair fed breeding carrots court, spend them,
     * and make no foal. The one-line message that should follow goes to the love cause, which for these hands
     * resolves to nobody, so that half stays the test kit's. The mares and the stallions are fed seven hundred
     * ticks apart: vanilla's breeding goal pairs any two horses in love within three blocks through a wall, and a
     * mare and a stallion in love at once in neighbouring pens would make a foal that answers nothing.
     */
    private static void sameSex(ServerLevel level, int gy, int x0, int z0, String name, Sex sex, long at) {
        DebugYardUnattended.pen(level, gy, x0, z0, 5, ROW_AQ_D, name, Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of(name, "golden carrots on", "both: they court,", "NO foal"));
        String fert = "horsegenetics.fertility=n/n";
        String word = sex == Sex.FEMALE ? "MARE" : "STUD";
        Horse a = horse(level, gy, x0 + 2.0, z0 + 4.5, sex, fert, name + " " + word + " 1");
        Horse b = horse(level, gy, x0 + 3.5, z0 + 6.5, sex, fert, name + " " + word + " 2");
        String check = name + " - carrots are spent and no foal comes";
        expect(check);
        AABB box = DebugTestYard.box(x0, gy, z0, x0 + 5, gy + 3, z0 + ROW_AQ_D).inflate(0.0, 2.0, 0.0);
        DebugYardHerd.after(level, at, () -> {
            if (a == null || b == null || !a.isAlive() || !b.isAlive()) {
                inconclusive(check, "a horse is gone");
                return;
            }
            StringBuilder fed = new StringBuilder();
            for (Horse h : List.of(a, b)) {
                Hands hands = hands(level, h, new ItemStack(Items.GOLDEN_CARROT), false);
                use(hands, h);
                boolean inLove = h.isInLove();
                if (!inLove) {
                    h.setInLove(null);
                }
                fed.append(fed.length() == 0 ? "" : "; ").append(h.getCustomName() == null ? "?"
                        : h.getCustomName().getString()).append(": carrot ").append(hands.count(Items.GOLDEN_CARROT) == 0
                        ? "eaten" : "NOT eaten").append(inLove ? ", in love" : ", not in love (forced)");
            }
            DebugYardHerd.after(level, 400, () -> {
                long foals = level.getEntitiesOfClass(Horse.class, box, h -> h.isAlive() && h.isBaby()).size();
                boolean spent = !a.isInLove() && !b.isInLove();
                verdict(check, foals == 0, fed + " | after 20s: " + foals + " foal(s), love "
                        + (spent ? "spent" : "STILL ON") + ", breeding cooldown ages " + a.getAge() + " / " + b.getAge());
            });
        });
    }
}
