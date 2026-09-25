package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * <b>Putting a horse into a chamber, and letting it back out.</b>
 *
 * <p>This is the one genuinely new mechanism in the stasis feature: everything
 * else planned on {@code wiki/horse-stasis.html} - the bank, its two tabs, the
 * drop buffer, in-bank breeding - is user interface built on top of these two
 * methods. A horse in stasis is <b>not an entity at all</b>: it is not merely
 * unloaded or out of render distance, it has been discarded, and the server pays
 * nothing for it until somebody releases it.
 *
 * <h2>It is the lycanthropy swap, minus the wolf</h2>
 * {@code LycanthropyHandler} has carried a whole horse through a night inside
 * another entity's attachment since the LYCAN gene shipped, using exactly this
 * pair of calls - {@code saveWithoutId} into a tag, {@code load} back out of one.
 * That is proven in-game across chunk unloads, restarts and deaths, so stasis
 * reuses it rather than inventing a second way to freeze a horse. The wiki page
 * said capture/release had "no existing analog in the mod"; it had one, and this
 * class is it wearing a different hat.
 *
 * <h2>Where the interactions live</h2>
 * Capture is an {@code EntityInteract} subscriber rather than an
 * {@code Item.interactLivingEntity} override, for the reason
 * {@code TicketHandler} spells out: the event fires on both sides and vanilla
 * turns any item used on a tamed horse into a mount, so it has to be cancelled
 * on <b>both</b> sides with the work done server-only. Release is an ordinary
 * {@code useOn} on the item, since nothing hijacks a right-click on a block.
 */
@EventBusSubscriber
public final class HorseStasisHandler {

    private HorseStasisHandler() {
    }

    // ------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof StasisChamberItem chamber)) {
            return;
        }
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
            capture(level, horse, event.getEntity(), stack, chamber, event.getHand());
        }
        // Cancelled on both sides or the client predicts a mount it then has to
        // take back - see TicketHandler.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Put {@code horse} into the chamber {@code player} is holding. Every
     * refusal says why: a chamber is a crafted item used on purpose, and one
     * that silently does nothing is indistinguishable from a bug.
     */
    private static void capture(ServerLevel level, Horse horse, Player player, ItemStack stack,
                                StasisChamberItem chamber,
                                net.minecraft.world.InteractionHand hand) {
        if (StasisChamberItem.snapshotOf(stack) != null) {
            say(player, "That chamber already has a horse in it.");
            return;
        }
        String name = HorseRecords.of(horse).displayName();
        String refusal = HorseOwnership.bindRefusal(horse, player, name);
        if (refusal != null) {
            say(player, refusal);
            return;
        }
        if (horse.isVehicle()) {
            // Passengers would be discarded with it. A rider is the common case
            // and a foal on a boat is not, so refuse rather than guess.
            say(player, name + " has somebody on its back - get them off first.");
            return;
        }

        // A full chamber is a different item, so this is a swap in the hand
        // rather than an edit to the stack the player is holding.
        player.setItemInHand(hand, swallow(level, horse, stack, name));
        say(player, name + " is in stasis.");
        ActionTrace.log("stasis", ActionTrace.describeShort(horse) + " captured into a "
                + chamber.tier().id() + " chamber by " + player.getGameProfile().name());
    }

    /**
     * <b>The horse goes in.</b> The one place a live animal becomes a data
     * component, shared by the two ways that happens: a player right-clicking a
     * horse with a chamber, and {@link EmergencyStasisHandler} catching one that
     * is about to die. Every refusal has already been made by the time anything
     * calls this.
     *
     * <p><b>It returns the filled chamber, and the caller must put it back.</b>
     * A chamber with a horse in it is a different item from an empty one (see
     * {@code StasisChamberItem.withHorse}), and an {@code ItemStack} cannot
     * change its own item, so this cannot fill in place the way it used to. The
     * two callers each know where the stack lives - the hand that used it, or
     * the inventory slot the emergency walk found it in - and each writes the
     * result back there. Everything else riding on the stack is carried over by
     * {@code transmuteCopy}.
     */
    public static ItemStack swallow(ServerLevel level, Horse horse, ItemStack chamber, String name) {
        ItemStack filled = StasisChamberItem.withHorse(chamber, snapshot(horse, name));
        // Written before the discard, while the horse still has a position. This
        // is the last thing that will ever be recorded about where it is: a
        // discarded horse fires no death, leaves no entity to sight, and would
        // otherwise sit in the browser reading "not loaded" for ever.
        HorseWhereabouts.get(level.getServer())
                .enteredStasis(horse.getUUID(), level.dimension(), horse.blockPosition());
        horse.discard();
        level.playSound(null, horse.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.7F, 1.4F);
        return filled;
    }

    /**
     * <b>What a chamber would hold if it took this horse now</b>, without
     * discarding it. Split out of {@link #capture} so the bank's gametest can
     * make a real snapshot of a real horse and check that
     * {@link StasisCare} can still read it - the one thing about the upkeep
     * that fails silently, since a renamed NBT key simply stops matching.
     */
    public static StasisSnapshot snapshot(Horse horse, String name) {
        return new StasisSnapshot(name, horse.getUUID(), save(horse));
    }

    // ------------------------------------------------------------------
    // Release
    // ------------------------------------------------------------------

    /**
     * Re-summon the horse a chamber holds, standing at {@code pos}.
     *
     * @return the live horse, or {@code null} if it could not be restored - in
     *         which case nothing has been consumed and the caller should leave
     *         the chamber full rather than lose the horse.
     */
    public static @Nullable Horse release(ServerLevel level, Vec3 pos, float yRot, StasisSnapshot snapshot) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.LOAD);
        if (horse == null) {
            return null;
        }
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(horse.problemPath(), HorseGenetics.LOGGER)) {
            horse.load(TagValueInput.create(reporter, level.registryAccess(), snapshot.horse()));
        } catch (RuntimeException unreadable) {
            HorseGenetics.LOGGER.error("[stasis] could not restore {} from a chamber - it stays inside",
                    snapshot.horseName(), unreadable);
            return null;
        }
        // The tag's position is wherever it was captured, which may be a world
        // away. Where the chamber was opened is where the horse wakes up.
        horse.snapTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        horse.setDeltaMovement(Vec3.ZERO);
        // Stasis horses do not get hungry, so a released one starts fed - the
        // same call the hunger attachment's own comment makes for a re-summon.
        horse.setData(com.example.horsegenetics.neoforge.data.ModAttachments.HUNGER.get(),
                com.example.horsegenetics.common.care.Hunger.FULL);
        horse.setPersistenceRequired();
        if (!level.addFreshEntity(horse)) {
            return null;
        }
        // Out of the bottle, and back to being a thing that can be seen. Cleared
        // here rather than left for the slow sighting stagger, because until it
        // is cleared the browser is telling the player their horse is in a
        // chamber while it stands in front of them.
        HorseWhereabouts.get(level.getServer())
                .leftStasis(horse.getUUID(), level.dimension(), horse.blockPosition());
        return horse;
    }

    /**
     * Is the horse this chamber holds <b>already out there</b>? Releasing it
     * again would be a second entity claiming one UUID, which the ancestry
     * database, every stall sign and every pedigree key on.
     *
     * <p>Reachable in creative, where copying an item is a keystroke. A map
     * lookup per loaded level, not a scan.
     */
    public static boolean alreadyLoose(MinecraftServer server, StasisSnapshot snapshot) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(snapshot.horseId());
            if (entity instanceof Horse && entity.isAlive()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------

    /**
     * The whole horse as a tag. {@code saveWithoutId} rather than {@code save}
     * because the entity type is never in question, and because on NeoForge it
     * is the call that carries the data attachments - which is the half that
     * matters. Same reasoning, same call, as {@code LycanthropyHandler.save}.
     */
    private static CompoundTag save(Horse horse) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(horse.problemPath(), HorseGenetics.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, horse.registryAccess());
            horse.saveWithoutId(out);
            return out.buildResult();
        }
    }

    static void say(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    /** Where a chamber opened against {@code clicked} on {@code face} sets a horse down. */
    public static Vec3 standingSpot(net.minecraft.world.level.Level level, BlockPos clicked,
                                    net.minecraft.core.Direction face) {
        BlockPos target = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);
        return new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
    }
}
