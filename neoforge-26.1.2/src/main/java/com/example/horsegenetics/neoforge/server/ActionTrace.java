package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <b>A running account of what the player did and what the mod did about it.</b>
 *
 * <p>The mod's other logging says what went <i>wrong</i>. This says what
 * <i>happened</i>, so that somebody reading the log afterwards can follow a play
 * session as a sequence rather than reconstruct it from the two lines that
 * happened to be interesting. Asked for by the owner (2026-09-12) after a
 * session where three separate reports - "F8 only turns on", "Make egg does
 * nothing", "the game just crashed" - each needed a different kind of evidence,
 * and two of them left none at all in the log.
 *
 * <h2>What it records</h2>
 * Discrete things a person does or causes: a horse coming into the world (with
 * its whole genome), a right-click on an animal, a mod item or block used, a
 * mount or dismount, a taming, a foal, a death, a dimension change. Key presses
 * are logged where they are read, on the client
 * ({@code client/DebugKeyHandler}, {@code client/HorseBrowserKeyBindings}) -
 * in a single-player run both halves write the same file, which is the file
 * that gets pasted into a report.
 *
 * <h2>What it does not record</h2>
 * Anything per-tick, anything per-frame, and any right-click that is not on an
 * animal or with something of this mod's. A trace that logs the player walking
 * is a trace nobody can read, and the point of this is to be readable: one line
 * per thing that happened, all of them prefixed <code>[trace]</code> so a
 * session is one grep away.
 *
 * <h2>The gate</h2>
 * {@link ServerConfig#debugTools()} - on in a dev run, off in a normal install,
 * switchable in both. Same flag as {@code /testkit}, because this is the same
 * kind of thing: a tool for the person testing, not for the person playing.
 */
@EventBusSubscriber
public final class ActionTrace {

    private ActionTrace() {
    }

    /** One line, prefixed so a whole session greps out of the log at once. */
    public static void log(String what, String detail) {
        if (!ServerConfig.debugTools()) {
            return;
        }
        HorseGenetics.LOGGER.info("[trace] {} | {}", what, detail);
    }

    // ------------------------------------------------------------------
    // Horses arriving
    // ------------------------------------------------------------------

    /**
     * <b>Every horse that joins a level, dumped whole - a few ticks later.</b>
     *
     * <p>Not in the handler, and not at the end of the tick either. A wild
     * horse is claimed by {@code HorseFoundingTickHandler} on a <i>later
     * tick</i> - deliberately, it is the deferred-join design - so both of
     * those read the horse before it has a genome and print "NO RECORD" for
     * every wild spawn in the world. The first version of this did exactly
     * that, and a log full of confident "no record" lines is worse than no log:
     * it looks like a finding.
     *
     * <p>So a joining horse is parked for {@link #SETTLE_TICKS} and dumped
     * after. One that still has no record by then really is unclaimed - a
     * vanilla horse from another mod's spawn, or one this mod has decided not
     * to touch - and the line says so meaning it.
     */
    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!ServerConfig.debugTools() || event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (PENDING.size() >= MAX_PENDING) {
            // A burst big enough to fill this is itself worth knowing about,
            // and a trace that grows without bound is a leak in the tool built
            // to find leaks.
            HorseGenetics.LOGGER.warn("[trace] more than {} horses waiting to be described - "
                    + "dropping the oldest; something is spawning a lot of horses", MAX_PENDING);
            PENDING.remove(0);
        }
        PENDING.add(new Pending(horse, tick + SETTLE_TICKS));
    }

    /** How long to let a horse settle before printing it. */
    private static final int SETTLE_TICKS = 5;

    /** Horses waiting to be described, and the tick each is due on. */
    private record Pending(Horse horse, int dueTick) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final int MAX_PENDING = 512;
    private static int tick;

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        tick++;
        if (PENDING.isEmpty()) {
            return;
        }
        // At most a few per tick. Walking into the horse dimension spawns a
        // hundred and twenty horses in one go and each of these is a dozen
        // lines of string building: dumped all at once that is a visible hitch,
        // in the one place the player goes specifically to LOOK at horses.
        // Spread over ticks it is the same information and no frame notices.
        int budget = MAX_PER_TICK;
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext() && budget > 0) {
            Pending p = it.next();
            if (p.dueTick() > tick) {
                continue;
            }
            it.remove();
            if (!p.horse().isRemoved()) {
                log("horse-spawn", describe(p.horse()));
                budget--;
            }
        }
    }

    /**
     * How many horses may be described in one tick. Eight clears a whole
     * dimension's worth in under a second, which is far faster than anybody can
     * walk to the second pen.
     */
    private static final int MAX_PER_TICK = 8;

    /**
     * Everything about one horse, on one line: who it is, what it is carrying,
     * what that resolved to, and where it is standing. Long, deliberately - the
     * whole value of this line is not having to ask a second question.
     */
    public static String describe(Horse horse) {
        BlockPos at = horse.blockPosition();
        StringBuilder sb = new StringBuilder();
        sb.append(horse.getUUID().toString(), 0, 8).append(" ");
        if (!HorseRecords.hasRealRecord(horse)) {
            return sb.append("NO RECORD (vanilla horse, or one this mod has not claimed yet) at ")
                    .append(at.toShortString()).append(" in ")
                    .append(horse.level().dimension().identifier()).toString();
        }
        HorseRecord record = HorseRecords.of(horse);
        Traits traits = HorseRecords.traitsOf(horse);
        sb.append('"').append(record.firstName()).append(' ').append(record.lastName()).append('"');
        record.barnName().ifPresent(b -> sb.append(" [").append(b).append(']'));
        sb.append(" gen").append(record.generation());
        sb.append(horse.isBaby() ? " FOAL" : " adult");
        sb.append(horse.isTamed() ? " tamed" : " untamed");
        record.breed().ifPresent(b -> sb.append(" breed=").append(b));
        sb.append("\n         genotype: ").append(record.geneticCode());
        sb.append("\n         epigenome: ").append(record.epigenomeCode());
        sb.append("\n         traits: speed=").append(round(traits.speed()))
                .append(" health=").append(round(traits.health()))
                .append(" jump=").append(round(traits.jump()))
                .append(" scale=").append(round(traits.scale()))
                .append(" viability=").append(traits.viability());
        if (!traits.conditions().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Condition c : traits.conditions()) {
                names.add(c.name() + "(" + c.severity() + ")");
            }
            sb.append("\n         conditions: ").append(String.join(", ", names));
        }
        record.motherId().ifPresent(m -> sb.append("\n         mother: ").append(m.toString(), 0, 8));
        record.fatherId().ifPresent(f -> sb.append(" father: ").append(f.toString(), 0, 8));
        record.tamedBy().ifPresent(t -> sb.append("\n         tamed by: ").append(t));
        record.bredBy().ifPresent(b -> sb.append(" bred by: ").append(b));
        sb.append("\n         at ").append(at.toShortString())
                .append(" in ").append(horse.level().dimension().identifier());
        return sb.toString();
    }

    private static String round(double v) {
        return String.format("%.3f", v);
    }

    // ------------------------------------------------------------------
    // Things the player does
    // ------------------------------------------------------------------

    /** Right-click on an entity - the interaction most of this mod hangs off. */
    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide().isClient()) {
            return;
        }
        Entity target = event.getTarget();
        String held = describeItem(event.getItemStack());
        if (target instanceof Horse horse) {
            log("right-click HORSE", held + " on " + shortName(horse)
                    + (horse.isBaby() ? " (foal)" : "")
                    + (horse.isTamed() ? " (tamed)" : " (untamed)")
                    + (event.getEntity().isShiftKeyDown() ? " [SHIFT held]" : ""));
            return;
        }
        // Villagers matter here too - the cowboy and the horseman are both
        // right-click conversations, and "nothing happened" is a real report.
        log("right-click entity", held + " on " + target.getType().builtInRegistryHolder()
                .key().identifier());
    }

    /** Using a mod item in the air - jars, papers, tickets, carrots, whistles. */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient() || !isOurs(event.getItemStack())) {
            return;
        }
        log("use item", describeItem(event.getItemStack()) + " by " + playerName(event.getEntity()));
    }

    /** Using anything on one of this mod's blocks - portals, signs, shelves, barns. */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Identifier block = state.getBlock().builtInRegistryHolder().key().identifier();
        boolean ourBlock = HorseGenetics.MOD_ID.equals(block.getNamespace());
        if (!ourBlock && !isOurs(event.getItemStack())) {
            return;
        }
        log("right-click block", describeItem(event.getItemStack()) + " on " + block
                + " at " + event.getPos().toShortString());
    }

    /**
     * Mount and dismount. Worth logging both ways round: a dismount the player
     * asked for and one the mod caused (hydrophobic ejecting a rider, say) look
     * identical from the saddle and different here.
     */
    @SubscribeEvent
    static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntityMounting() instanceof Player player)) {
            return;
        }
        if (!(event.getEntityBeingMounted() instanceof AbstractHorse horse)) {
            return;
        }
        log(event.isMounting() ? "mount" : "dismount",
                playerName(player) + (event.isMounting() ? " got on " : " got off ")
                        + shortName(horse)
                        + (player.isShiftKeyDown() ? " [SHIFT held]" : ""));
    }

    @SubscribeEvent
    static void onTame(AnimalTameEvent event) {
        if (!(event.getAnimal() instanceof Horse horse)) {
            return;
        }
        log("tamed", shortName(horse) + " by " + event.getTamer().getName().getString());
    }

    @SubscribeEvent
    static void onFoal(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof Horse foal)) {
            return;
        }
        // The parents are named here because the foal's own record carries only
        // their ids, and "which two made this" is the first question asked of
        // any inheritance bug.
        log("foal born", shortName(event.getParentA()) + " x " + shortName(event.getParentB())
                + " -> " + shortName(foal));
    }

    /**
     * A horse dying. The lethal disorders kill foals shortly after birth on
     * purpose, and that is indistinguishable from a bug unless the log says
     * which it was.
     */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        String cause = event.getSource().getMsgId();
        String conditions = "";
        if (HorseRecords.hasRealRecord(horse)) {
            Traits traits = HorseRecords.traitsOf(horse);
            if (!traits.conditions().isEmpty()) {
                conditions = " viability=" + traits.viability();
            }
        }
        log("horse died", shortName(horse) + " from " + cause + conditions
                + (horse.isBaby() ? " (FOAL)" : ""));
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        log("dimension", playerName(event.getEntity()) + ": "
                + event.getFrom().identifier() + " -> " + event.getTo().identifier());
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ServerConfig.debugTools()) {
            return;
        }
        // A banner, so a session has a beginning in the log and two sessions in
        // one file cannot be read as one.
        HorseGenetics.LOGGER.info("[trace] ==== {} joined - action trace is ON "
                + "(debug.tools); every line below is prefixed [trace] ====",
                playerName(event.getEntity()));
    }

    // ------------------------------------------------------------------

    private static boolean isOurs(ItemStack stack) {
        return !stack.isEmpty() && HorseGenetics.MOD_ID.equals(
                stack.getItem().builtInRegistryHolder().key().identifier().getNamespace());
    }

    private static String describeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty hand";
        }
        Identifier id = stack.getItem().builtInRegistryHolder().key().identifier();
        String name = stack.getHoverName().getString();
        return id + (name.isEmpty() ? "" : " (\"" + name + "\")");
    }

    private static String shortName(Entity entity) {
        if (entity instanceof Horse horse && HorseRecords.hasRealRecord(horse)) {
            HorseRecord record = HorseRecords.of(horse);
            return record.firstName() + " " + record.lastName()
                    + " [" + horse.getUUID().toString().substring(0, 8) + "]";
        }
        return entity.getType().builtInRegistryHolder().key().identifier()
                + " [" + entity.getUUID().toString().substring(0, 8) + "]";
    }

    private static String playerName(Player player) {
        return player instanceof ServerPlayer sp ? sp.getGameProfile().name() : player.getName().getString();
    }
}
