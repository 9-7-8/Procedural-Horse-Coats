Yes—combine it as an **aerial recovery and landing mechanic**, not as permanent hovering. The best feel is: your Pegasus uses elytra-like momentum physics in open air, but when it touches a cloud-run zone it can temporarily regain lift, restore stamina, and skim/run across the cloud instead of stalling.

That means cloud running becomes part of the flight loop:

```text
Dive to gain airspeed
→ glide and trade speed for altitude
→ approach a cloud/updraft
→ touch cloud layer
→ run across it to recover stamina and momentum
→ launch back into an elytra-style glide
```

This keeps the Pegasus distinct from unrestricted creative flight. It cannot simply freeze at any altitude, but it has magical “rest stops” in the sky.

## Design choice

Use **three related states**, not one permanent `isFlying` boolean:

```java
public enum FlightState {
    GROUNDED,
    GLIDING,
    CLOUD_RUNNING,
    STALLED
}
```

| State | Physics | Player experience |
|---|---|---|
| `GROUNDED` | Normal horse gravity/movement | Ordinary riding |
| `GLIDING` | Elytra-style gravity, lift, drag, pitch steering | Dive, soar, climb, stall |
| `CLOUD_RUNNING` | Horse gets a cloud-surface floor plus normal/boosted horizontal gait | Run across clouds, rebuild stamina, prepare takeoff |
| `STALLED` | Little/no lift; gravity resumes | Horse falls until it dives, flaps, lands, or finds cloud support |

The cloud-running state should have **limited vertical authority**. It can let the horse maintain itself near a cloud layer, but it should not permit unlimited upward flight.

## Best cloud implementation

For a first version, do **not** create thousands of real temporary cloud blocks. Instead, define a cloud “surface” in code:

- A cloud is a region/volume detected by biome, Y level, weather, a custom cloud block, or an invisible marker entity.
- When the horse is gliding and close enough above that surface, switch to `CLOUD_RUNNING`.
- Clamp the horse’s Y coordinate to the cloud’s top surface.
- Restore some stamina and let the horse accelerate horizontally.
- When it runs off the edge, it returns to `GLIDING`.

For a visual production version, you can later add temporary cloud blocks or a custom cloud block with a soft collision shape. Blocks that need timers/state commonly use block entities and tickers, but their tick methods run every game tick, so mass-spawning them for every flight can be unnecessarily expensive. [docs.neoforged](https://docs.neoforged.net/docs/blockentities/)

## Add flight state data

Replace a simple `isGliding` flag with data like this.

**File:** `pegasus/PegasusFlightData.java`

```java
package com.ixora.flyinghorses.pegasus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

public final class PegasusFlightData {
    public enum FlightState {
        GROUNDED,
        GLIDING,
        CLOUD_RUNNING,
        STALLED
    }

    public FlightState state = FlightState.GROUNDED;

    /*
     * 0.0 to 100.0.
     *
     * Spend it on flaps/boosts and recover it while resting or cloud-running.
     */
    public float stamina = 100.0F;

    /*
     * Saved cloud support position. This is the center/top of the cloud region
     * currently being traversed.
     */
    @Nullable
    public BlockPos cloudAnchor;

    /*
     * Counts down after leaving a cloud. It prevents flickering between
     * GLIDING and CLOUD_RUNNING every tick at the cloud boundary.
     */
    public int cloudGraceTicks = 0;

    /*
     * Allows a short takeoff burst after cloud running.
     */
    public int cloudLaunchTicks = 0;
}
```

Eventually, make this an entity attachment so the data persists and can be synchronized for rendering. NeoForge’s attachment system supports associating custom data with entities, including data that can be serialized and synchronized as needed. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

## Detect cloud support

Start with a simple system: define cloud-running areas using a custom block tag. This is much easier than writing dynamic volumetric clouds immediately.

Create a tag file:

**File:** `data/flyinghorses/tags/blocks/pegasus_clouds.json`

```json
{
  "replace": false,
  "values": [
    "flyinghorses:cloud_block",
    "minecraft:white_wool",
    "minecraft:white_concrete_powder"
  ]
}
```

The vanilla blocks are only useful for testing. In the finished mod, remove them and use your own cloud blocks.

Then use this helper:

```java
private static final TagKey<Block> PEGASUS_CLOUDS =
        TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(
                        FlyingHorses.MOD_ID,
                        "pegasus_clouds"
                )
        );

@Nullable
private static BlockPos findCloudSurface(
        ServerLevel level,
        Horse horse
) {
    BlockPos origin = horse.blockPosition();

    /*
     * Search 4 blocks downward, because the horse is normally just above
     * the cloud rather than inside it.
     */
    for (int dy = 0; dy <= 4; dy++) {
        BlockPos candidate = origin.below(dy);

        if (level.getBlockState(candidate).is(PEGASUS_CLOUDS)) {
            return candidate;
        }
    }

    return null;
}
```

For a cloud block, give it a collision shape so entities can stand on it. Collision shape controls where entities physically collide with a block; for a visual cloud, you may want a shallow, broad shape rather than a full cube. [learn.microsoft](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/blockreference/examples/blockcomponents/minecraftblock_collision_box?view=minecraft-bedrock-stable)

## Integrate into glide tick

The important change is at the beginning of your existing `tickGlidingHorse(...)` method. Before running ordinary elytra calculations, check whether the horse can land on or remain on a cloud.

```java
private static void tickPegasusFlight(
        Horse horse,
        ServerPlayer rider,
        PegasusFlightData data
) {
    ServerLevel level = (ServerLevel) horse.level();

    BlockPos cloudSurface = findCloudSurface(level, horse);

    if (cloudSurface != null && canEnterCloudRun(horse, data)) {
        data.state = PegasusFlightData.FlightState.CLOUD_RUNNING;
        data.cloudAnchor = cloudSurface;
        data.cloudGraceTicks = 10;
    }

    switch (data.state) {
        case GLIDING -> tickGliding(horse, rider, data);

        case CLOUD_RUNNING -> tickCloudRunning(
                horse,
                rider,
                data
        );

        case STALLED -> tickStalled(horse, rider, data);

        case GROUNDED -> tickGrounded(horse, data);
    }
}
```

Use a short grace timer because collision/position rounding at a cloud edge can otherwise make the state alternate:

```text
GLIDING → CLOUD_RUNNING → GLIDING → CLOUD_RUNNING → GLIDING
```

every other tick.

## Enter cloud running

Do not allow cloud-running from arbitrarily far below or above a cloud. Require the horse to be approximately at cloud level and either descending gently or already gliding.

```java
private static boolean canEnterCloudRun(
        Horse horse,
        PegasusFlightData data
) {
    if (data.state != PegasusFlightData.FlightState.GLIDING
            && data.state != PegasusFlightData.FlightState.STALLED) {
        return false;
    }

    /*
     * Avoid snapping upward onto clouds while the Pegasus is far beneath them.
     */
    if (horse.getDeltaMovement().y > 0.25D) {
        return false;
    }

    return true;
}
```

You can add a minimum horizontal airspeed requirement if you want cloud running to feel like a landing/skim rather than a midair rescue:

```java
double horizontalSpeed = horizontalSpeed(horse.getDeltaMovement());

return horizontalSpeed >= 0.18D;
```

## Cloud-running physics

This is the core. It treats the cloud as a very high-altitude runway, preserves some momentum, gives gentle traction, restores stamina, and allows a launch back into glide.

```java
private static final double CLOUD_RUN_SPEED = 0.48D;
private static final double CLOUD_RUN_ACCELERATION = 0.06D;
private static final double CLOUD_RUN_DRAG = 0.94D;

private static final float CLOUD_STAMINA_REGEN = 0.45F;
private static final float MAX_STAMINA = 100.0F;

private static final double CLOUD_SURFACE_OFFSET = 1.00D;
private static final double CLOUD_LAUNCH_VERTICAL_SPEED = 0.34D;
private static final double CLOUD_LAUNCH_FORWARD_SPEED = 0.55D;

private static void tickCloudRunning(
        Horse horse,
        ServerPlayer rider,
        PegasusFlightData data
) {
    ServerLevel level = (ServerLevel) horse.level();

    BlockPos cloudPos = findCloudSurface(level, horse);

    if (cloudPos == null) {
        data.cloudGraceTicks--;

        if (data.cloudGraceTicks <= 0) {
            data.state = PegasusFlightData.FlightState.GLIDING;
            data.cloudAnchor = null;
            return;
        }

        cloudPos = data.cloudAnchor;
    }

    if (cloudPos == null) {
        data.state = PegasusFlightData.FlightState.GLIDING;
        return;
    }

    /*
     * The top of a normal block is y + 1.
     *
     * Set a small offset as needed after testing your actual horse model and
     * cloud collision shape. The horse should look like its hooves touch the
     * cloud rather than sink into it or hover too far above it.
     */
    double cloudSurfaceY = cloudPos.getY() + CLOUD_SURFACE_OFFSET;

    /*
     * Maintain the mount on the cloud surface.
     *
     * For a true soft cloud, you can use a small vertical spring instead of a
     * hard snap; this simple version is stable and easy to debug.
     */
    if (horse.getY() < cloudSurfaceY - 0.10D) {
        horse.setPos(
                horse.getX(),
                cloudSurfaceY,
                horse.getZ()
        );
    }

    horse.setNoGravity(true);
    horse.fallDistance = 0.0F;

    /*
     * Standard mounted movement inputs.
     *
     * zza: forward/back
     * xxa: left/right strafe
     */
    float forwardInput = horse.zza;
    float strafeInput = horse.xxa;

    float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

    Vec3 forward = new Vec3(
            -Mth.sin(yawRadians),
            0.0D,
            Mth.cos(yawRadians)
    );

    Vec3 right = new Vec3(
            Mth.cos(yawRadians),
            0.0D,
            Mth.sin(yawRadians)
    );

    Vec3 desiredDirection = forward.scale(forwardInput)
            .add(right.scale(strafeInput));

    Vec3 velocity = horse.getDeltaMovement();

    if (desiredDirection.lengthSqr() > 0.0001D) {
        desiredDirection = desiredDirection.normalize();

        Vec3 desiredVelocity = desiredDirection.scale(
                CLOUD_RUN_SPEED
        );

        velocity = new Vec3(
                Mth.lerp(
                        CLOUD_RUN_ACCELERATION,
                        velocity.x,
                        desiredVelocity.x
                ),
                0.0D,
                Mth.lerp(
                        CLOUD_RUN_ACCELERATION,
                        velocity.z,
                        desiredVelocity.z
                )
        );
    } else {
        velocity = new Vec3(
                velocity.x * CLOUD_RUN_DRAG,
                0.0D,
                velocity.z * CLOUD_RUN_DRAG
        );
    }

    /*
     * Stamina recovery while the horse is supported by magical cloud.
     */
    data.stamina = Math.min(
            MAX_STAMINA,
            data.stamina + CLOUD_STAMINA_REGEN
    );

    /*
     * Launch:
     *
     * Hold Jump while moving forward to turn cloud running back into glide.
     * The horse gets only a brief upward and forward impulse—not sustained
     * creative flight.
     */
    boolean wantsLaunch = rider.input.jumping
            && forwardInput > 0.0F
            && data.stamina >= 8.0F;

    if (wantsLaunch) {
        data.stamina -= 8.0F;
        data.state = PegasusFlightData.FlightState.GLIDING;
        data.cloudAnchor = null;
        data.cloudGraceTicks = 0;
        data.cloudLaunchTicks = 10;

        horse.setDeltaMovement(
                forward.x * CLOUD_LAUNCH_FORWARD_SPEED,
                CLOUD_LAUNCH_VERTICAL_SPEED,
                forward.z * CLOUD_LAUNCH_FORWARD_SPEED
        );

        return;
    }

    horse.setDeltaMovement(velocity);
}
```

The `rider.input.jumping` field may be inaccessible or renamed in your mappings. Since you already have a client-to-server packet for Pegasus flight input, the more robust production approach is to include `jumpHeld` in that packet and store it in your Pegasus data, rather than reading client-only input from a server entity.

Use this instead of direct rider input in the completed system:

```java
boolean wantsLaunch = data.jumpHeld
        && forwardInput > 0.0F
        && data.stamina >= 8.0F;
```

## Add cloud launch to your network input

Extend your existing flight input payload with a `jumpHeld` boolean:

```java
public record FlightInputPayload(
        boolean jumpHeld,
        boolean boostHeld,
        boolean forwardHeld,
        boolean togglePressed
) implements CustomPacketPayload {
    // codec fields here
}
```

On the client:

```java
boolean jumpHeld = minecraft.options.keyJump.isDown();
```

On the server, after validating that the sender is riding the correct Pegasus:

```java
data.jumpHeld = payload.jumpHeld();
```

This is the correct multiplayer model: client controls are read on the client, sent through a registered payload, and validated before they affect server-authoritative movement. NeoForge payloads are specifically meant for structured data sent between the client and server. [docs.neoforged](https://docs.neoforged.net/docs/networking/payload/)

## Cloud surface variants

You do not have to commit to solid cloud blocks. These options all work with the same flight-state logic.

| Cloud type | Technical approach | Best use |
|---|---|---|
| Solid cloud block | Custom block with a shallow collision shape | Easy prototype, sky islands, visible tracks |
| Temporary cloud trail | Spawn temporary cloud blocks under/behind hooves; remove after a short timer | Celestial Tianma or rare high-level Pegasus |
| Invisible cloud volume | Store region bounds or marker entity; clamp horse to an invisible surface | Natural-looking procedural clouds |
| Weather cloud | Use altitude + biome + weather checks; no placed blocks | Simple world-gen-free prototype |
| Updraft cloud | No physical landing; add lift/slow descent within a volume | More aerodynamic, less “horse running on a platform” |
| Custom cloud entity | Entity with size, lifetime, and client rendering | Moving clouds, storm clouds, animated sky creatures |

For your first implementation, I recommend **custom cloud blocks** or a tagged test block. It gives you reliable collision, straightforward testing, and a visible place to debug flight transitions. Later, replace the block with procedurally spawned cloud regions or a custom visual/cloud entity.

## Better hybrid: cloud skim

If you want cloud running to feel less like walking on wool blocks, use a **cloud skim** mode:

- Horse remains 0.3–0.8 blocks above cloud surface.
- Downward velocity is canceled at the surface.
- Horizontal speed remains glide-like, not normal ground speed.
- Hooves create cloud puffs.
- Holding jump launches; looking down lets the horse drop off the cloud.
- Stamina regenerates more slowly than on a full cloud landing.

Replace the hard Y snap with a spring-like lift:

```java
double desiredY = cloudPos.getY() + 1.25D;
double yError = desiredY - horse.getY();

double verticalCorrection = Mth.clamp(
        yError * 0.16D,
        -0.18D,
        0.18D
);

velocity = new Vec3(
        velocity.x,
        verticalCorrection,
        velocity.z
);
```

That makes the Pegasus feel like it is riding a cushion of air rather than standing on a block.

## Balance rules

To prevent cloud running from becoming creative flying with extra steps:

- Require the Pegasus to already be gliding or falling; do not allow it to enter cloud running while rising rapidly.
- Make cloud-running areas rare, altitude-gated, weather-gated, or biome-gated.
- Give cloud-running a finite stamina-restoration rate.
- Require forward movement to launch; do not allow stationary vertical launches.
- On leaving cloud support, restore normal glide gravity immediately.
- Do not let clouds generate underneath the mount continuously unless that is an explicit late-game celestial ability.
- Make a Pegasus’ cloud affinity an inherited stat: some can only use dense storm clouds; elite lineages can skim thin clouds.

## Recommended genetics hooks

Add these to your Pegasus phenotype/ability data:

```text
cloud_affinity          0.00–1.00
cloud_run_speed         0.20–0.75
cloud_stamina_regen     0.05–0.75 stamina/tick
cloud_launch_strength   0.15–0.60
minimum_cloud_density   0.00–1.00
updraft_efficiency      0.00–1.00
glide_lift              0.00–1.00
wing_power              0.00–1.00
```

Then use them in formulas rather than hardcoded values:

```java
double launchStrength = Mth.lerp(
        pegasus.cloudLaunchStrength(),
        0.20D,
        0.60D
);

float staminaRecovery = Mth.lerp(
        pegasus.cloudAffinity(),
        0.10F,
        0.60F
);
```

That supports meaningful breeding outcomes:

- A Pegasus with high wing power but low cloud affinity is a great diver/climber but cannot rest on thin clouds.
- A cloud-runner type has average flight speed but excellent aerial endurance.
- A rare celestial line has strong cloud launch and can use weather clouds.
- A weakly expressed wing trait allows only short cloud-assisted glides.

## Visual work later

The entire cloud-running mechanic can function as code-only first, but it will read far better with:

- A slightly faster, higher-stepping run gait.
- Hoof cloud-puff particles.
- Mane/tail wind simulation or particle trails.
- Wings partly tucked while running and flared during launch.
- A cloud-launch flap animation.
- Soft wind/hoofbeat sounds with a muffled “cloud” impact sound.

The actual cloud area itself can initially be a standard block; if you later create timed, stateful cloud blocks, remember that block entities/tickers execute every tick and should avoid expensive per-block work or large uncontrolled quantities. [docs.neoforged](https://docs.neoforged.net/docs/blockentities/)
