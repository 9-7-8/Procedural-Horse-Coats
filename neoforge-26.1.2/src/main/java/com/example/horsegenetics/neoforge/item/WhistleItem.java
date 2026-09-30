package com.example.horsegenetics.neoforge.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.server.HorseLeads;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import com.example.horsegenetics.neoforge.server.WhistleCalls;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A whistle (roadmap wiki &sect;11). Right-click anywhere: every <b>tamed horse
 * you own</b> within {@link #radius} blocks, in the same dimension, that
 * isn&rsquo;t being ridden is <b>recalled to you</b> - teleported to a spot
 * beside you, spread on a grid so a herd doesn&rsquo;t stack. Three tiers, three
 * radii (basic 16 / golden 32 / echo 64), a short use cooldown, and a chat line
 * saying how many came.
 *
 * <p>This is the "certain area &rarr; come back" version the owner asked for;
 * the roadmap&rsquo;s bond-gated "call bonded horses" is a later refinement once
 * bond exists, and "what echo adds" beyond range is still open.
 */
public class WhistleItem extends Item {

    private static final int COOLDOWN_TICKS = 60;
    private static final double ALREADY_HERE_SQR = 9.0; // don't move a horse already within 3 blocks

    private final int radius;

    // Item(Properties) is @Deprecated to nudge modders toward the id-carrying
    // Properties that DeferredRegister.Items#registerItem already supplies here.
    @SuppressWarnings("deprecation")
    public WhistleItem(Properties properties, int radius) {
        super(properties);
        this.radius = radius;
    }

    /**
     * Which checklist task this tier ticks. Keyed off the radius rather than a
     * fourth constructor argument, because the radius is already the thing that
     * distinguishes the three and a second parallel field could disagree with it.
     */
    /** This tier's reach, which is also what tells the three tiers apart. */
    public int radius() {
        return radius;
    }

    private ProgressTask task() {
        if (radius <= 16) {
            return ProgressTask.WHISTLE_BASIC;
        }
        return radius <= 32 ? ProgressTask.WHISTLE_GOLDEN : ProgressTask.WHISTLE_ECHO;
    }

    /**
     * <b>What this tier actually reaches.</b> The three whistles are the same
     * icon with three names, and nothing anywhere told the player the numbers:
     * the recipe blurbs say &ldquo;further than the basic one&rdquo; without ever
     * saying how far, so the only way to learn a radius was to walk out and
     * count. The ender whistle set the shape for this line
     * ({@code EnderWhistleItem#appendHoverText}) - one grey line, no interaction
     * needed to read it.
     *
     * <p>The second line is the two outcomes players report as bugs: a ridden
     * horse is deliberately left where it is, and a leashed one is untied. It
     * stops short of saying where the lead <em>goes</em>, because that is
     * {@code behaviour.leads_return} and a client on a dedicated server does not
     * have the server's config to read.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Calls your tamed horses within " + radius + " blocks.")
                .withStyle(ChatFormatting.GRAY));
        // WALK_PATH_BLOCKS, not a typed 16: the radius line above reads its own
        // field for exactly this reason, and a tooltip that disagrees with the
        // behaviour is worse than no tooltip. "About" because the cap is counted
        // in path steps and a diagonal step covers more than a block.
        adder.accept(Component.literal("Horses within about " + WhistleCalls.WALK_PATH_BLOCKS
                        + " blocks walk to you; farther ones appear beside you.")
                .withStyle(ChatFormatting.DARK_GRAY));
        adder.accept(Component.literal("A ridden horse stays put; a leashed one is untied.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * <b>What one blow did.</b> {@code handled} is every horse this whistle
     * dealt with, walked or teleported, so that
     * {@code server/WhistleBlowing} can skip an ender whistle bound to a horse
     * an area whistle has already called - blowing both at once must not
     * teleport a horse twice or chime twice for it.
     */
    public record Blown(int walked, int teleported, Set<UUID> handled) {

        public static final Blown NOTHING = new Blown(0, 0, Set.of());

        public int total() {
            return walked + teleported;
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            Blown blown = blow(serverPlayer, player.getItemInHand(hand));
            player.sendSystemMessage(Component.literal(blown.total() == 0
                    ? "No tamed horses of yours within " + radius + " blocks."
                    : "Whistled " + blown.total() + " horse" + (blown.total() == 1 ? "" : "s") + " to you."));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * <b>Blow this whistle</b> - the whole of what the item does, minus the chat
     * line, which differs between one whistle blown by hand and several blown by
     * the key.
     *
     * <p>Static and public because the keybind calls <b>this</b> rather than a
     * copy of it. A second implementation would be a way round the cooldown the
     * moment one of the two forgot to set it, so the cooldown is here with the
     * work it belongs to.
     */
    public Blown blow(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        HorseProgress.complete(player, task());
        Blown blown = recall(level, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return blown;
    }

    private Blown recall(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, box, horse ->
                horse.isAlive() && horse.isTamed() && !horse.isVehicle()
                        && horse != player.getVehicle() && ownedBy(horse, player));

        int walked = 0;
        int placed = 0;
        Set<UUID> handled = new HashSet<>();
        for (AbstractHorse horse : horses) {
            if (horse.distanceToSqr(player) < ALREADY_HERE_SQR) {
                continue;
            }
            handled.add(horse.getUUID());
            // Near enough to come on its own feet? Then let it. tryWalk does the
            // untying itself, because a leashed horse refuses to path.
            if (WhistleCalls.tryWalk(player, horse)) {
                walked++;
                continue;
            }
            HorseLeads.untieFor(horse, player);
            WhistleCalls.teleportTo(player, horse, placed);
            placed++;
        }
        return new Blown(walked, placed, handled);
    }

    private static boolean ownedBy(AbstractHorse horse, Player player) {
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        return owner != null && player.getUUID().equals(owner.getUUID());
    }
}
