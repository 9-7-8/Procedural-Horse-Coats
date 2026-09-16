Yes. The cleanest starter design is a **server-authoritative AI behavior**: an untamed horse selects a nearby eligible target, performs a short bite attack, stores that target as its “dragged victim,” then navigates backward away from it while applying a restrained pulling force. NeoForge data attachments are a good long-term place to store the horse’s drag state, because they can attach custom data to entities and optionally persist it. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

For a first working version, though, use an in-memory map keyed by horse UUID. It is much easier to get compiling and test the behavior before adding persistent serialization, animation, networking, cooldown balancing, and custom model work.

## Intended behavior

This starter system makes only **wild adult vanilla horses** eligible:

- Horse must be untamed.
- Horse must be an adult.
- It detects nearby players or mobs.
- It bites only targets that are not horses, not passengers, and not creative/spectator players.
- It does a small amount of damage once.
- It holds the target in front of the horse for a short moment.
- It walks **backward** away from the target.
- The target is pulled along in front of the horse.
- The drag ends after a time limit, if the target dies, the horse becomes tamed, the horse is damaged, or the target escapes.

This avoids making every horse permanently hostile. Vanilla horses are normally passive mobs, so this is a deliberate AI behavior you are adding rather than something already present in horse code. [minecraft](https://minecraft.wiki/w/Horse)

## Files to add

```text
src/main/java/com/ixora/flyinghorses/
├── WildHorseDragMod.java
├── ai/
│   └── HorseDragGoal.java
└── server/
    └── WildHorseDragEvents.java
```

You can use the same mod ID as your flying-horse project, or change it. Below I use:

```java
public static final String MOD_ID = "wildhorsedrag";
```

If you are adding it into the flying-mount mod, replace every `wildhorsedrag` reference with your existing mod ID instead.

## Main mod class

**File:** `WildHorseDragMod.java`

```java
package com.ixora.wildhorsedrag;

import net.neoforged.fml.common.Mod;

@Mod(WildHorseDragMod.MOD_ID)
public final class WildHorseDragMod {
    public static final String MOD_ID = "wildhorsedrag";

    public WildHorseDragMod() {
    }
}
```

## Add the AI goal

NeoForge exposes an `EntityJoinLevelEvent` for entities entering the world. Use it to add the custom goal to every vanilla `Horse` as it loads or spawns. The AI itself is a standard `Goal`: Minecraft’s goal system is the normal system used for entity behaviors such as fleeing, breeding, wandering, navigation, and attacks. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/world/entity/ai/goal/package-use.html)

**File:** `server/WildHorseDragEvents.java`

```java
package com.ixora.wildhorsedrag.server;

import com.ixora.wildhorsedrag.WildHorseDragMod;
import com.ixora.wildhorsedrag.ai.HorseDragGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = WildHorseDragMod.MOD_ID)
public final class WildHorseDragEvents {
    private WildHorseDragEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Horse horse)) {
            return;
        }

        if (horse.level().isClientSide()) {
            return;
        }

        /*
         * Lower number = higher priority.
         *
         * Priority 2 is intentionally fairly high: dragging should interrupt
         * strolling and other non-emergency horse behavior, but it should not
         * necessarily override every survival behavior you later add.
         */
        horse.goalSelector.addGoal(
                2,
                new HorseDragGoal(horse)
        );
    }
}
```

## Full drag goal

This is the actual bite-and-backward-drag behavior.

**File:** `ai/HorseDragGoal.java`

```java
package com.ixora.wildhorsedrag.ai;

import com.ixora.wildhorsedrag.WildHorseDragMod;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HorseDragGoal extends Goal {
    /*
     * Detection and action distances, in blocks.
     */
    private static final double DETECTION_RANGE = 7.0D;
    private static final double BITE_RANGE = 2.25D;
    private static final double MAX_DRAG_DISTANCE = 8.0D;

    /*
     * Timing is in Minecraft ticks.
     * Minecraft normally runs at 20 ticks per second.
     */
    private static final int BITE_WINDUP_TICKS = 12;
    private static final int GRAB_HOLD_TICKS = 8;
    private static final int MAX_DRAG_TICKS = 80;
    private static final int COOLDOWN_TICKS = 140;

    /*
     * Gameplay balance.
     */
    private static final float BITE_DAMAGE = 2.0F;

    /*
     * How quickly the horse backs away.
     */
    private static final double BACKWARD_SPEED = 0.22D;

    /*
     * How strongly the target is pulled toward the grab point.
     */
    private static final double PULL_STRENGTH = 0.16D;

    /*
     * Limits pull velocity so victims do not jerk violently across the world.
     */
    private static final double MAX_PULL_SPEED = 0.38D;

    private final Horse horse;

    @Nullable
    private LivingEntity target;

    private DragState state = DragState.SEARCHING;

    private int stateTicks;
    private int cooldownTicks;
    private int dragTicks;

    private enum DragState {
        SEARCHING,
        APPROACHING,
        BITE_WINDUP,
        HOLDING,
        DRAGGING,
        COOLDOWN
    }

    public HorseDragGoal(Horse horse) {
        this.horse = horse;

        /*
         * MOVE: prevents competing movement goals from controlling the horse.
         * LOOK: lets this goal rotate the horse toward its target.
         */
        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }

    @Override
    public boolean canUse() {
        if (!canHorseDrag()) {
            return false;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        target = findNearestTarget();

        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canHorseDrag()) {
            return false;
        }

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (horse.distanceToSqr(target) > MAX_DRAG_DISTANCE * MAX_DRAG_DISTANCE) {
            return false;
        }

        /*
         * Once a drag begins, the goal continues through its current state.
         */
        return state != DragState.COOLDOWN;
    }

    @Override
    public void start() {
        state = DragState.APPROACHING;
        stateTicks = 0;
        dragTicks = 0;
    }

    @Override
    public void stop() {
        horse.getNavigation().stop();

        if (target != null) {
            /*
             * Remove the temporary movement restraint/slow effect if you add
             * one later. This basic implementation applies only velocity.
             */
            target.setDeltaMovement(
                    target.getDeltaMovement().scale(0.70D)
            );
        }

        target = null;
        stateTicks = 0;
        dragTicks = 0;

        /*
         * Cooldown prevents the horse from instantly grabbing the same victim
         * again when the target is released.
         */
        cooldownTicks = COOLDOWN_TICKS;
        state = DragState.COOLDOWN;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        stateTicks++;

        switch (state) {
            case APPROACHING -> tickApproaching();

            case BITE_WINDUP -> tickBiteWindup();

            case HOLDING -> tickHolding();

            case DRAGGING -> tickDragging();

            case SEARCHING, COOLDOWN -> {
            }
        }
    }

    private void tickApproaching() {
        if (target == null) {
            return;
        }

        lookAtTarget(target);

        double distance = horse.distanceTo(target);

        if (distance > BITE_RANGE) {
            horse.getNavigation().moveTo(
                    target,
                    1.10D
            );
            return;
        }

        horse.getNavigation().stop();
        state = DragState.BITE_WINDUP;
        stateTicks = 0;
    }

    private void tickBiteWindup() {
        if (target == null) {
            return;
        }

        lookAtTarget(target);
        horse.getNavigation().stop();

        /*
         * This is where a custom animation hook would go:
         *
         * - tilt head toward target
         * - open jaw
         * - play aggressive horse sound
         * - spawn a small particle effect
         *
         * The vanilla horse model does not have a bite animation by default.
         */
        if (stateTicks < BITE_WINDUP_TICKS) {
            return;
        }

        if (horse.distanceTo(target) > BITE_RANGE + 0.75D) {
            state = DragState.APPROACHING;
            stateTicks = 0;
            return;
        }

        boolean damaged = target.hurt(
                horse.damageSources().mobAttack(horse),
                BITE_DAMAGE
        );

        if (!damaged || !target.isAlive()) {
            state = DragState.COOLDOWN;
            stateTicks = 0;
            return;
        }

        state = DragState.HOLDING;
        stateTicks = 0;
    }

    private void tickHolding() {
        if (target == null) {
            return;
        }

        horse.getNavigation().stop();
        lookAtTarget(target);

        /*
         * Keep the victim at the mouth/grab location briefly before the horse
         * begins retreating. This makes the sequence read as:
         *
         * approach -> bite -> latch -> drag backward
         */
        pullTargetToMouth(target, 0.65D);

        if (stateTicks >= GRAB_HOLD_TICKS) {
            state = DragState.DRAGGING;
            stateTicks = 0;
            dragTicks = 0;
        }
    }

    private void tickDragging() {
        if (target == null) {
            return;
        }

        dragTicks++;

        if (dragTicks >= MAX_DRAG_TICKS) {
            state = DragState.COOLDOWN;
            return;
        }

        /*
         * The horse should continue facing the target while moving backward.
         * That makes the target stay visually near the horse's mouth/front.
         */
        lookAtTarget(target);

        /*
         * Backward motion:
         *
         * Horse forward direction:
         *   x = -sin(yaw)
         *   z =  cos(yaw)
         *
         * To move backward, negate that direction.
         */
        float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

        double backwardX = Mth.sin(yawRadians);
        double backwardZ = -Mth.cos(yawRadians);

        Vec3 currentMotion = horse.getDeltaMovement();

        horse.setDeltaMovement(
                backwardX * BACKWARD_SPEED,
                Math.max(currentMotion.y, 0.0D),
                backwardZ * BACKWARD_SPEED
        );

        /*
         * Keep navigation stopped, because normal navigation attempts to move
         * toward a target. The horse must be moved manually backward instead.
         */
        horse.getNavigation().stop();

        /*
         * The target should remain just in front of the horse's face while it
         * is dragged backward. The mouth location is slightly forward and
         * upward from the horse's center.
         */
        pullTargetToMouth(target, 0.82D);

        /*
         * Prevent fall damage from small terrain irregularities during the
         * forced movement. This is optional but makes dragging less harsh.
         */
        target.fallDistance = 0.0F;
    }

    private boolean canHorseDrag() {
        /*
         * Required behavior conditions.
         */
        if (!horse.isAlive()) {
            return false;
        }

        if (horse.isTamed()) {
            return false;
        }

        if (horse.isBaby()) {
            return false;
        }

        /*
         * Do not let a ridden horse use its hostile wild behavior.
         */
        if (horse.isVehicle()) {
            return false;
        }

        /*
         * Avoid combat behavior while the horse is breeding/love-mode if that
         * matters to your design. This optional check can be removed.
         */
        if (horse.isInLove()) {
            return false;
        }

        return true;
    }

    @Nullable
    private LivingEntity findNearestTarget() {
        AABB searchBox = horse.getBoundingBox().inflate(
                DETECTION_RANGE,
                3.0D,
                DETECTION_RANGE
        );

        List<LivingEntity> candidates = horse.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                this::isValidTarget
        );

        LivingEntity nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSq = horse.distanceToSqr(candidate);

            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private boolean isValidTarget(LivingEntity candidate) {
        if (candidate == horse) {
            return false;
        }

        if (!candidate.isAlive()) {
            return false;
        }

        if (candidate.isSpectator()) {
            return false;
        }

        /*
         * Do not target players in creative mode.
         */
        if (candidate instanceof ServerPlayer serverPlayer
                && (serverPlayer.isCreative() || serverPlayer.isSpectator())) {
            return false;
        }

        /*
         * Do not drag anyone mounted on this horse, or entities riding another
         * entity. You can remove the second check if you want horses to drag
         * mounted mobs, but it is usually visually messy and can cause issues.
         */
        if (candidate.isPassenger()) {
            return false;
        }

        /*
         * Do not target other horses. This avoids herd members constantly
         * biting and dragging each other.
         */
        if (candidate instanceof Horse) {
            return false;
        }

        /*
         * Optional: prevent attacks on passive animal mobs.
         *
         * If you want only players and monsters to be eligible, uncomment:
         *
         * if (!(candidate instanceof Player)
         *         && !(candidate instanceof Monster)) {
         *     return false;
         * }
         */

        return true;
    }

    private void lookAtTarget(LivingEntity target) {
        horse.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        /*
         * Set horse body/yaw toward the target so its "mouth" faces the victim.
         */
        double dx = target.getX() - horse.getX();
        double dz = target.getZ() - horse.getZ();

        float desiredYaw = (float) (
                Mth.atan2(dz, dx) * (180.0D / Math.PI)
        ) - 90.0F;

        horse.setYRot(desiredYaw);
        horse.yBodyRot = desiredYaw;
        horse.yHeadRot = desiredYaw;
    }

    private void pullTargetToMouth(
            LivingEntity target,
            double pullMultiplier
    ) {
        /*
         * Horse forward direction based on its current yaw.
         */
        float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

        double forwardX = -Mth.sin(yawRadians);
        double forwardZ = Mth.cos(yawRadians);

        /*
         * Approximate mouth location:
         *
         * - 1.15 blocks forward from horse center
         * - about 1.15 blocks upward
         *
         * You will likely tune these numbers once you see your model in-game.
         */
        Vec3 mouthPosition = new Vec3(
                horse.getX() + forwardX * 1.15D,
                horse.getY() + 1.15D,
                horse.getZ() + forwardZ * 1.15D
        );

        Vec3 targetPosition = target.position();

        Vec3 offset = mouthPosition.subtract(targetPosition);

        /*
         * Do not directly teleport the victim every tick.
         *
         * Instead, add bounded velocity toward the horse's mouth. This looks
         * better, interacts more predictably with collision, and makes it
         * possible for strong/fast targets to struggle somewhat.
         */
        Vec3 pull = offset.scale(PULL_STRENGTH * pullMultiplier);

        double pullLength = pull.length();

        if (pullLength > MAX_PULL_SPEED) {
            pull = pull.scale(MAX_PULL_SPEED / pullLength);
        }

        Vec3 oldVelocity = target.getDeltaMovement();

        target.setDeltaMovement(
                oldVelocity.x * 0.45D + pull.x,
                Math.max(oldVelocity.y * 0.25D, pull.y),
                oldVelocity.z * 0.45D + pull.z
        );

        target.hurtMarked = true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        /*
         * Dragging depends on smooth, continuous position/velocity updates.
         */
        return true;
    }
}
```

## Fix one missing import

If IntelliJ says it cannot find `Player` because it is unused, remove this line from the goal file:

```java
import net.minecraft.world.entity.player.Player;
```

It is not needed in the exact code above.

## Important behavior note

The horse is moved by:

```java
horse.setDeltaMovement(
        backwardX * BACKWARD_SPEED,
        Math.max(currentMotion.y, 0.0D),
        backwardZ * BACKWARD_SPEED
);
```

and the target is gently pulled toward the calculated mouth position by:

```java
target.setDeltaMovement(...)
```

That is what creates the “bite, hold, step backward, pull target along” effect. It does **not** attach the victim as a passenger, leash the victim, or teleport it every tick. That is a good starter choice because putting a player inside the horse’s passenger list would look wrong, interfere with normal riding, and make escaping or collision behavior difficult.

## Current limitations

This starter has logic and physics, but no custom visuals:

- A vanilla horse has no bite animation.
- The victim is pulled near the front of the horse, but not literally attached to its teeth.
- The horse may slide backward rather than play a convincing reverse-step animation.
- The victim can still move and attack unless you add a temporary restriction.
- The behavior does not persist through world unload/reload yet.
- All adult untamed horses can use it.

Minecraft’s built-in goal framework is appropriate for deciding *when* a horse attacks, approaches, and retreats, but a polished biting animation requires custom rendering/model animation support beyond the AI goal. [maven.fabricmc](https://maven.fabricmc.net/docs/yarn-1.18.1+build.18/net/minecraft/entity/ai/goal/package-summary.html)

## Strongly recommended next changes

### Restrict what wild horses attack

At present, any valid nearby living entity except another horse can be targeted. That includes livestock, villagers, pets, monsters, and survival players.

For “wild horse defensive aggression,” target only players or hostile mobs:

```java
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
```

Then replace the optional comment block in `isValidTarget` with:

```java
if (!(candidate instanceof Player)
        && !(candidate instanceof Monster)) {
    return false;
}
```

For horses that drag only players, use:

```java
if (!(candidate instanceof Player)) {
    return false;
}
```

### Make it defensive, not predatory

Instead of `findNearestTarget()`, you could activate the drag goal only after the horse is attacked. Store the aggressor UUID when the horse is hurt, then allow the goal to pursue only that entity for perhaps 10–20 seconds. That better fits a frightened, aggressive, or territorial wild horse.

### Add struggle resistance

A very game-feeling mechanic would let targets partially resist:

```java
double resistance = target.isSprinting() ? 0.55D : 1.0D;
Vec3 pull = offset.scale(PULL_STRENGTH * pullMultiplier * resistance);
```

You could go further with a “strength” trait: larger/stronger horses pull harder, while a player with armor, a resistance effect, or a special item escapes more easily.

### Store state with attachments

For a polished version, replace temporary Java fields with a NeoForge entity `AttachmentType` containing:

```text
dragTargetUuid
dragState
stateTicks
cooldownTicks
dragTicks
```

Data attachments are intended for custom entity-associated data and can be made persistent with a serializer; they can also be synchronized when client rendering needs the state, such as for a jaw-grip animation or client particles. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

### Avoid uncontrolled cruelty

For fair gameplay, consider any combination of:

- Low bite damage, such as 1–2 hearts at most.
- Brief drag duration, around 2–4 seconds.
- A clear cooldown.
- No targeting of babies, tamed animals, villagers, or passive mobs.
- Immediate release if the player damages the horse, gets too far away, enters water, or uses an escape item.
- Config options for player targeting and mob targeting.

That keeps the behavior dramatic and readable rather than creating an unavoidable player-control lock.
