Here is a starter **momentum sprint** system for a Chollima-, Arion-, or Sleipnir-style ground mount. It rewards riding in a mostly straight line: the horse gradually builds momentum above its normal speed, loses it when you brake, turn hard, collide, leave the ground, or stop, and gets a short cooldown after an impact.

Unlike simply applying a permanent Speed modifier, this system creates a readable loop:

```text
Start riding
→ run forward
→ maintain a stable heading
→ momentum builds
→ top speed rises
→ turn sharply / hit something / stop
→ momentum drains
→ rebuild on the next straightaway
```

It runs server-side using an entity tick handler, which is appropriate for continuously updating entity state and movement. `EntityTickEvent.Post` fires once per entity each game tick after normal entity work. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/allclasses-index.html)

## Design values

This draft assumes a normal ridden `Horse` and uses its `deltaMovement` directly for the bonus velocity. That preserves vanilla horse movement as the baseline while adding a momentum component.

| Variable | Default | Meaning |
|---|---:|---|
| Build time | ~5 seconds | Time of clean straight running to reach max momentum |
| Max momentum | 100 | Internal resource/percentage |
| Top bonus | 0.55 blocks/tick | Extra speed at full momentum, about 11 blocks/second |
| Turn tolerance | 12°/tick | Gentle steering is okay; sharp turns drain speed |
| Ground requirement | Yes | No momentum build while jumping/falling/swimming |
| Collision cooldown | 30 ticks | About 1.5 seconds after a crash |
| Minimum forward input | 0.8 | Must be mostly holding forward, not simply moving sideways |

A typical vanilla horse has a movement-speed attribute roughly in the range 0.1125–0.3375, with real-world movement ranging from about 4.85 to 14.23 blocks per second depending on the horse. That is why the bonus in this prototype is deliberately capped rather than multiplying its normal speed without limit. [minecraft.fandom](https://minecraft.fandom.com/wiki/Horse)

## Momentum state class

This first version uses an in-memory state map, which is simple to test. Move it to a NeoForge entity attachment later if you want momentum to survive chunk unloads/restarts or drive client HUD/model effects. Entity custom persistent data and attachments are better long-term storage options than an in-memory map. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/world/entity/Entity.html)

**File:** `momentum/MomentumSprintData.java`

```java
package com.ixora.flyinghorses.momentum;

public final class MomentumSprintData {
    /*
     * 0.0 = no bonus.
     * 100.0 = full sprint bonus.
     */
    public float momentum = 0.0F;

    /*
     * The last stable heading used to judge whether the horse is running
     * straight enough to build momentum.
     */
    public float previousYaw = 0.0F;

    public boolean hadPreviousYaw = false;

    /*
     * Stops momentum buildup for a short time after a crash/collision.
     */
    public int collisionCooldownTicks = 0;

    /*
     * Optional: counts how long this horse has sustained a momentum sprint.
     * Useful later for particles, achievements, stamina, or audio.
     */
    public int sustainedSprintTicks = 0;
}
```

**File:** `momentum/MomentumSprintState.java`

```java
package com.ixora.flyinghorses.momentum;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.animal.horse.Horse;

public final class MomentumSprintState {
    private MomentumSprintState() {
    }

    private static final Map<UUID, MomentumSprintData> DATA =
            new HashMap<>();

    public static MomentumSprintData get(Horse horse) {
        return DATA.computeIfAbsent(
                horse.getUUID(),
                ignored -> new MomentumSprintData()
        );
    }

    public static void remove(Horse horse) {
        DATA.remove(horse.getUUID());
    }
}
```

## Full momentum sprint handler

**File:** `momentum/MomentumSprintHandler.java`

```java
package com.ixora.flyinghorses.momentum;

import com.ixora.flyinghorses.FlyingHorses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTickEvent;

@EventBusSubscriber(modid = FlyingHorses.MOD_ID)
public final class MomentumSprintHandler {
    private MomentumSprintHandler() {
    }

    /*
     * Momentum values.
     */
    private static final float MAX_MOMENTUM = 100.0F;

    /*
     * Build / loss rates per tick.
     *
     * At 1.0 per tick, it takes 100 ticks = 5 seconds to build from 0 to 100.
     */
    private static final float MOMENTUM_BUILD_RATE = 1.00F;

    /*
     * Mild loss for imperfect riding, such as slightly reduced forward input.
     */
    private static final float SOFT_DECAY_RATE = 1.50F;

    /*
     * Stronger loss for sharp turning, stopping, jumping, water, etc.
     */
    private static final float HARD_DECAY_RATE = 4.50F;

    /*
     * Used after a wall/tree/entity impact.
     */
    private static final float COLLISION_MOMENTUM_LOSS = 18.0F;

    /*
     * Extra horizontal velocity at full momentum.
     *
     * 0.55 blocks/tick is approximately 11 blocks/second of added movement.
     * Begin lower, around 0.25–0.35, if this feels too fast.
     */
    private static final double MAX_BONUS_SPEED = 0.40D;

    /*
     * Horse must be mostly moving forward to build momentum.
     *
     * horse.zza is generally:
     *   1.0  = forward
     *   0.0  = no forward/back input
     *  -1.0  = backward
     */
    private static final float MIN_FORWARD_INPUT = 0.80F;

    /*
     * Maximum yaw change per tick to still count as “straight”.
     *
     * This is not total curve over time: it is how much the horse changes
     * direction on one game tick.
     */
    private static final float MAX_BUILD_TURN_DEGREES = 12.0F;

    /*
     * A turn greater than this is treated as a sharp maneuver and drains
     * momentum significantly.
     */
    private static final float SHARP_TURN_DEGREES = 28.0F;

    /*
     * Minimum existing horizontal movement required for momentum to build.
     * Prevents standing still while holding W from generating a bonus.
     */
    private static final double MIN_BUILD_HORIZONTAL_SPEED = 0.12D;

    /*
     * If the horse's horizontal speed drops below this, drain momentum.
     */
    private static final double STOPPED_HORIZONTAL_SPEED = 0.04D;

    /*
     * Cooldown after a collision.
     */
    private static final int COLLISION_COOLDOWN_TICKS = 30;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Horse horse)) {
            return;
        }

        /*
         * Do all physics and persistent game logic on the server.
         */
        if (horse.level().isClientSide()) {
            return;
        }

        /*
         * This starter only enables the effect while a player is actively
         * controlling the horse.
         */
        if (!(horse.getControllingPassenger() instanceof ServerPlayer rider)) {
            resetMomentumWhenUnridden(horse);
            return;
        }

        /*
         * Optional future condition:
         *
         * Only give this to a specific magical lineage.
         *
         * if (!horse.getPersistentData()
         *         .getBoolean("flyinghorses:is_chollima")) {
         *     return;
         * }
         */
        tickMomentumSprint(horse, rider);
    }

    private static void tickMomentumSprint(
            Horse horse,
            ServerPlayer rider
    ) {
        MomentumSprintData data = MomentumSprintState.get(horse);

        Vec3 velocity = horse.getDeltaMovement();

        double horizontalSpeed = horizontalSpeed(velocity);

        float currentYaw = horse.getYRot();

        /*
         * Initialize heading tracking on the first relevant tick.
         */
        if (!data.hadPreviousYaw) {
            data.previousYaw = currentYaw;
            data.hadPreviousYaw = true;
        }

        float yawDelta = Math.abs(
                Mth.wrapDegrees(currentYaw - data.previousYaw)
        );

        data.previousYaw = currentYaw;

        /*
         * horse.zza should come from the rider's forward/back input while the
         * horse is being controlled. If mappings differ in your environment,
         * use the equivalent forward movement input field.
         */
        float forwardInput = horse.zza;

        boolean movingForward = forwardInput >= MIN_FORWARD_INPUT;

        boolean onGround = horse.onGround();

        boolean inFluid = horse.isInWater()
                || horse.isInLava()
                || horse.isInFluidType();

        boolean nearlyStopped =
                horizontalSpeed < STOPPED_HORIZONTAL_SPEED;

        boolean fastEnoughToBuild =
                horizontalSpeed >= MIN_BUILD_HORIZONTAL_SPEED;

        boolean gentleTurn =
                yawDelta <= MAX_BUILD_TURN_DEGREES;

        boolean sharpTurn =
                yawDelta >= SHARP_TURN_DEGREES;

        /*
         * Basic collision detection:
         *
         * horizontalCollision means Minecraft detected a collision against
         * a horizontal surface while the entity moved.
         */
        boolean crashed = horse.horizontalCollision
                && horizontalSpeed > MIN_BUILD_HORIZONTAL_SPEED;

        if (data.collisionCooldownTicks > 0) {
            data.collisionCooldownTicks--;
        }

        if (crashed) {
            data.momentum = Math.max(
                    0.0F,
                    data.momentum - COLLISION_MOMENTUM_LOSS
            );

            data.collisionCooldownTicks = COLLISION_COOLDOWN_TICKS;
            data.sustainedSprintTicks = 0;
        }

        boolean canBuildMomentum =
                movingForward
                        && onGround
                        && !inFluid
                        && !nearlyStopped
                        && fastEnoughToBuild
                        && gentleTurn
                        && data.collisionCooldownTicks <= 0;

        if (canBuildMomentum) {
            data.momentum = Math.min(
                    MAX_MOMENTUM,
                    data.momentum + MOMENTUM_BUILD_RATE
            );

            data.sustainedSprintTicks++;
        } else {
            float decay;

            if (!onGround
                    || inFluid
                    || nearlyStopped
                    || sharpTurn
                    || !movingForward) {
                decay = HARD_DECAY_RATE;
            } else {
                decay = SOFT_DECAY_RATE;
            }

            data.momentum = Math.max(
                    0.0F,
                    data.momentum - decay
            );

            data.sustainedSprintTicks = 0;
        }

        applyMomentumVelocity(
                horse,
                velocity,
                data.momentum,
                movingForward
        );
    }

    private static void applyMomentumVelocity(
            Horse horse,
            Vec3 currentVelocity,
            float momentum,
            boolean movingForward
    ) {
        /*
         * Do not push a horse forward when the player releases W.
         *
         * It can retain natural vanilla momentum for a moment, but the custom
         * bonus should only be applied while there is a forward riding input.
         */
        if (!movingForward || momentum <= 0.0F) {
            return;
        }

        /*
         * Convert 0–100 momentum to 0–1.
         *
         * Squaring it makes the initial buildup gentle and reserves the most
         * dramatic speed gain for sustained, high-momentum running.
         */
        double normalizedMomentum = momentum / MAX_MOMENTUM;
        double curvedMomentum =
                normalizedMomentum * normalizedMomentum;

        double bonusSpeed =
                MAX_BONUS_SPEED * curvedMomentum;

        /*
         * Use horse yaw rather than player yaw.
         *
         * This keeps the impulse aligned with the mount’s actual facing
         * direction and avoids sideways boosts while turning.
         */
        float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

        Vec3 forward = new Vec3(
                -Mth.sin(yawRadians),
                0.0D,
                Mth.cos(yawRadians)
        );

        /*
         * Add only horizontal bonus velocity.
         * Preserve natural vertical velocity for normal jumping/falling.
         */
        Vec3 newVelocity = new Vec3(
                currentVelocity.x + forward.x * bonusSpeed,
                currentVelocity.y,
                currentVelocity.z + forward.z * bonusSpeed
        );

        /*
         * Safety cap: prevents accumulated direct velocity from becoming
         * unlimited when the horse runs for a long time downhill.
         */
        double horizontalSpeed = horizontalSpeed(newVelocity);

        double maxAllowedSpeed =
                vanillaSpeedCapEstimate(horse)
                        + MAX_BONUS_SPEED;

        if (horizontalSpeed > maxAllowedSpeed) {
            double scale = maxAllowedSpeed / horizontalSpeed;

            newVelocity = new Vec3(
                    newVelocity.x * scale,
                    newVelocity.y,
                    newVelocity.z * scale
            );
        }

        horse.setDeltaMovement(newVelocity);
    }

    private static double vanillaSpeedCapEstimate(Horse horse) {
        /*
         * This is intentionally a conservative safety cap in blocks/tick,
         * not a direct copy of the movement-speed attribute.
         *
         * You can later calculate a more exact cap from the movement speed
         * attribute if you want different horse genetics to affect it.
         */
        return 0.70D;
    }

    private static double horizontalSpeed(Vec3 velocity) {
        return Math.sqrt(
                velocity.x * velocity.x
                        + velocity.z * velocity.z
        );
    }

    private static void resetMomentumWhenUnridden(Horse horse) {
        MomentumSprintData data = MomentumSprintState.get(horse);

        data.momentum = 0.0F;
        data.sustainedSprintTicks = 0;
        data.hadPreviousYaw = false;
    }
}
```

## What the code is doing

The key condition is:

```java
boolean canBuildMomentum =
        movingForward
                && onGround
                && !inFluid
                && !nearlyStopped
                && fastEnoughToBuild
                && gentleTurn
                && data.collisionCooldownTicks <= 0;
```

A horse builds momentum only while it is:

- Being actively ridden by a player.
- Moving mostly forward.
- On solid ground.
- Not swimming or falling.
- Already moving at a reasonable speed.
- Turning less than 12 degrees per tick.
- Not recovering from a collision.

The bonus then scales with the **square** of current momentum:

```java
double curvedMomentum =
        normalizedMomentum * normalizedMomentum;
```

So it behaves approximately like this:

| Momentum | Portion of maximum bonus | Feel |
|---:|---:|---|
| 0 | 0% | Normal horse |
| 25 | 6.25% | Almost normal |
| 50 | 25% | Noticeably faster |
| 75 | 56.25% | Strong speed run |
| 100 | 100% | Full mythic sprint |

That curve is useful because it rewards maintaining a long, clean run instead of giving an immediate large boost.

## Fix: avoid repeated velocity addition

There is one important issue with the first direct-velocity approach: if you add the full bonus velocity every tick, it can feel overly forceful and may fight vanilla movement. A better practical implementation is to apply only the **difference between current and desired bonus**, stored in the data object.

Add this field to `MomentumSprintData`:

```java
public double lastAppliedBonus = 0.0D;
```

Then replace `applyMomentumVelocity(...)` with this safer version:

```java
private static void applyMomentumVelocity(
        Horse horse,
        Vec3 currentVelocity,
        float momentum,
        boolean movingForward
) {
    MomentumSprintData data = MomentumSprintState.get(horse);

    double desiredBonus = 0.0D;

    if (movingForward && momentum > 0.0F) {
        double normalizedMomentum = momentum / MAX_MOMENTUM;
        double curvedMomentum =
                normalizedMomentum * normalizedMomentum;

        desiredBonus = MAX_BONUS_SPEED * curvedMomentum;
    }

    /*
     * Only apply the change since the previous tick.
     *
     * Example:
     * last bonus = 0.12
     * desired bonus = 0.14
     * delta = +0.02
     *
     * This prevents continuously stacking the entire bonus every tick.
     */
    double bonusDelta =
            desiredBonus - data.lastAppliedBonus;

    data.lastAppliedBonus = desiredBonus;

    if (Math.abs(bonusDelta) < 0.0001D) {
        return;
    }

    float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

    Vec3 forward = new Vec3(
            -Mth.sin(yawRadians),
            0.0D,
            Mth.cos(yawRadians)
    );

    Vec3 newVelocity = new Vec3(
            currentVelocity.x + forward.x * bonusDelta,
            currentVelocity.y,
            currentVelocity.z + forward.z * bonusDelta
    );

    double horizontalSpeed = horizontalSpeed(newVelocity);

    double maxAllowedSpeed =
            vanillaSpeedCapEstimate(horse)
                    + MAX_BONUS_SPEED;

    if (horizontalSpeed > maxAllowedSpeed) {
        double scale = maxAllowedSpeed / horizontalSpeed;

        newVelocity = new Vec3(
                newVelocity.x * scale,
                newVelocity.y,
                newVelocity.z * scale
        );
    }

    horse.setDeltaMovement(newVelocity);
}
```

Also reset the stored bonus when the horse is no longer ridden:

```java
private static void resetMomentumWhenUnridden(Horse horse) {
    MomentumSprintData data = MomentumSprintState.get(horse);

    data.momentum = 0.0F;
    data.sustainedSprintTicks = 0;
    data.hadPreviousYaw = false;
    data.lastAppliedBonus = 0.0D;
}
```

Use this **second** version of `applyMomentumVelocity`; do not keep both versions.

## Better turn detection

The starter uses `horse.getYRot()`, which is easy and usually good enough. But high speed can make even small heading changes feel harsh.

For a more natural system, compare the actual travel direction rather than only the horse’s yaw:

```java
Vec3 flatVelocity = new Vec3(
        velocity.x,
        0.0D,
        velocity.z
);

if (flatVelocity.lengthSqr() > 0.001D) {
    flatVelocity = flatVelocity.normalize();

    float travelYaw = (float) (
            Mth.atan2(flatVelocity.z, flatVelocity.x)
                    * (180.0D / Math.PI)
    ) - 90.0F;

    float actualTurnDelta = Math.abs(
            Mth.wrapDegrees(travelYaw - data.previousYaw)
    );
}
```

That lets a horse point slightly left/right without instantly losing its “straight run” status, provided it is still actually traveling along a relatively stable path.

## Add a strain system

At full speed, add a **strain** or stamina cost so the mount cannot sprint forever. Add this to `MomentumSprintData`:

```java
public float sprintStamina = 100.0F;
```

Then in the tick:

```java
if (data.momentum >= 80.0F) {
    data.sprintStamina = Math.max(
            0.0F,
            data.sprintStamina - 0.10F
    );

    if (data.sprintStamina <= 0.0F) {
        data.momentum = Math.min(
                data.momentum,
                35.0F
        );
    }
} else if (horse.onGround() && !horse.isInWater()) {
    data.sprintStamina = Math.min(
            100.0F,
            data.sprintStamina + 0.05F
    );
}
```

This supports genetics-style differences:

```text
stride efficiency
sprint stamina
stamina recovery
momentum build rate
top momentum speed
turning tolerance
collision resilience
```

A Chollima-type horse can have a very high top speed but poor turning tolerance. An Arion-type horse can be slightly slower but have excellent endurance and traction. A Sleipnir-type horse might build momentum less quickly but retain it over rough terrain, water, snow, and soul sand.

## Add visual feedback later

The actual sprint mechanism is code-only. But players need to feel when they are building or losing momentum.

Use milestones:

| Momentum | Feedback |
|---:|---|
| 25+ | Faster hoofbeat sound; small dust particles |
| 50+ | Wind particles, mane/tail motion, stronger hoofbeats |
| 75+ | Speed lines, brighter eye/mane effect, screen wind sound |
| 100 | Distinct “mythic stride” sound; trail particles; advancement/stat increment |
| Collision | Skid particles, snort, reduced hoof sound, short lockout |

You can also expose a stamina/momentum bar only while riding an eligible horse:

```text
Momentum: ███████░░░
Stride: Building
```

## Recommended specializations

Use the same code with different parameters instead of writing separate sprint systems for every mythic horse.

| Lineage | Build rate | Bonus speed | Turn tolerance | Special rule |
|---|---:|---:|---:|---|
| Chollima | 1.35 | 0.55 | 8° | Very fast, difficult tight turns |
| Arion | 0.90 | 0.35 | 18° | Stable, endurance-oriented, good terrain traction |
| Sleipnir | 0.75 | 0.40 | 14° | Retains momentum over water/rough terrain; brief wall-run |
| Normal trained horse | 0.45 | 0.12 | 15° | Small reward for long straight gallops |
| Young horse | 0.55 | 0.20 | 10° | Builds quickly but low cap |
| Heavy draft lineage | 0.40 | 0.22 | 20° | Slow build but high collision resilience |

## Important compile notes

The three likely mapping-sensitive fields are:

```java
horse.zza
horse.horizontalCollision
horse.isInFluidType()
```

If your NeoForge 26.1.2 mappings use a different name:

- Search the `Horse`/`LivingEntity` decompiled source in IntelliJ for `zza`, `horizontalCollision`, and `isInFluid`.
- Use the matching forward-input, collision, and fluid-check members.
- If `isInFluidType()` does not exist, remove it and retain:

```java
boolean inFluid = horse.isInWater() || horse.isInLava();
```

Also, make sure this handler is registered only once. The `@EventBusSubscriber` annotation automatically registers the static `@SubscribeEvent` method on the normal NeoForge event bus.
