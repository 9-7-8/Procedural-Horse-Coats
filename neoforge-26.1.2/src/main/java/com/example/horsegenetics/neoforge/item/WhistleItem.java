package com.example.horsegenetics.neoforge.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            int moved = recall(serverLevel, player);
            player.sendSystemMessage(Component.literal(moved == 0
                    ? "No tamed horses of yours within " + radius + " blocks."
                    : "Whistled " + moved + " horse" + (moved == 1 ? "" : "s") + " to you."));
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    private int recall(ServerLevel level, Player player) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, box, horse ->
                horse.isAlive() && horse.isTamed() && !horse.isVehicle()
                        && horse != player.getVehicle() && ownedBy(horse, player));

        int placed = 0;
        for (AbstractHorse horse : horses) {
            if (horse.distanceToSqr(player) < ALREADY_HERE_SQR) {
                continue;
            }
            if (horse.isLeashed()) {
                horse.dropLeash();
            }
            horse.getNavigation().stop();
            BlockPos spot = spreadSpot(level, player, placed);
            horse.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), 0.0F);
            horse.setDeltaMovement(Vec3.ZERO);
            horse.fallDistance = 0.0;
            placed++;
        }
        return placed;
    }

    private static boolean ownedBy(AbstractHorse horse, Player player) {
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        return owner != null && player.getUUID().equals(owner.getUUID());
    }

    /**
     * A landing spot near {@code player}: rings of a 3x3 grid fanning out, then
     * nudged up/down a few blocks to sit on solid ground rather than in it or
     * floating. Best effort - a whistle recall doesn't need to be perfect.
     */
    private static BlockPos spreadSpot(ServerLevel level, Player player, int n) {
        int gx = (n % 3) - 1;
        int gz = ((n / 3) % 3) - 1;
        int ring = 2 + (n / 9) * 2;
        BlockPos p = player.blockPosition().offset(gx * ring, 0, gz * ring);
        for (int i = 0; i < 3 && level.getBlockState(p.below()).isAir(); i++) {
            p = p.below();
        }
        for (int i = 0; i < 4 && !level.getBlockState(p).isAir(); i++) {
            p = p.above();
        }
        return p;
    }
}
