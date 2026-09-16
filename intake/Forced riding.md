Yes. For a kelpie-style forced rider, I would build it as a **temporary ride-lock state** rather than trying to permanently cancel the player’s dismount key. The horse remains a normal rideable entity, but while its `forcedRide` state is active, the server immediately re-mounts the victim if they dismount and drives the horse toward deep water or another chosen destination.

That approach is more reliable across Minecraft versions because it relies on normal passenger mechanics plus server-side enforcement, rather than intercepting a client keybinding. Player tick events run once per player per game tick and are useful for checking whether a forced rider has been dismounted or escaped. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/neoforged/neoforge/event/tick/PlayerTickEvent.html)

## State machine

Treat the behavior as a short state machine stored on the **horse**, not as scattered booleans.

```text
IDLE
  ↓ player/mob mounts wild kelpie
WARNING
  ↓ after warning delay
FORCED_RIDE
  ↓ reaches water / time limit / escape succeeds
RELEASE
  ↓ cooldown
IDLE
```

A good first version:

| State | Duration | Horse behavior | Rider behavior |
|---|---:|---|---|
| `WARNING` | 20–40 ticks | Stands tense, looks around, rears/snorts | Rider can dismount normally |
| `FORCED_RIDE` | 100–240 ticks | Gallops toward deep water or a selected destination | Dismount is denied; player can struggle |
| `RELEASE` | Immediate | Ejects rider safely, stops horse | Give Slow Falling / brief water breathing if needed |
| `COOLDOWN` | 200–600 ticks | Normal wild behavior | Cannot immediately trap another rider |

Use an `enum`, a timer, the rider’s UUID, and a destination position/UUID. NeoForge attachments are a good eventual storage mechanism because they attach custom data to entities and can be made persistent; for a first prototype, a UUID-keyed server map is simpler. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

## Data structure

Start with an in-memory state record. It is intentionally easy to debug.

**File:** `kelpie/KelpieRideData.java`

```java
package com.ixora.wildhorsedrag.kelpie;

import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

public final class KelpieRideData {
    public enum State {
        IDLE,
        WARNING,
        FORCED_RIDE,
        RELEASE,
        COOLDOWN
    }

    public State state = State.IDLE;

    @Nullable
    public UUID riderUuid;

    @Nullable
    public BlockPos destination;

    /*
     * Counts down once per server tick.
     */
    public int stateTicks = 0;

    /*
     * Anti-repeat protection after release.
     */
    public int cooldownTicks = 0;

    /*
     * Player escape progress.
     *
     * Raise this when the player uses a deliberate struggle action; lower it
     * while they do not struggle. At the threshold, release them.
     */
    public float escapeProgress = 0.0F;

    /*
     * Prevents several “dismount attempts” in the same tick from generating
     * repeated mount calls.
     */
    public long lastRemountGameTime = -1L;
}
```

Then create a singleton state holder.

**File:** `kelpie/KelpieRideState.java`

```java
package com.ixora.wildhorsedrag.kelpie;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.animal.horse.Horse;

public final class KelpieRideState {
    private KelpieRideState() {
    }

    private static final Map<UUID, KelpieRideData> DATA = new HashMap<>();

    public static KelpieRideData get(Horse horse) {
        return DATA.computeIfAbsent(
                horse.getUUID(),
                ignored -> new KelpieRideData()
        );
    }

    public static void clear(Horse horse) {
        DATA.remove(horse.getUUID());
    }
}
```

### Later: migrate to entity attachments

Once the prototype works, replace the map with an entity attachment containing:

```text
state
rider UUID
destination BlockPos
stateTicks
cooldownTicks
escapeProgress
lastRemountGameTime
```

That lets the state survive saving/reloading and makes it easier to synchronize visuals such as “wet mane raised,” “horse is bolting,” or “rider is stuck.” NeoForge attachments are intended for entity-associated custom data and support persistent serialization when a serializer is provided. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

## Start condition

Trigger forced riding only when a **wild kelpie** is ridden. Do not apply it to normal horses or tamed kelpies.

You need a way to identify a kelpie. For now, use a persistent-data marker:

```java
horse.getPersistentData().getBoolean("wildhorsedrag:is_kelpie")
```

Later, replace that with your genetics/phenotype system.

**File:** `kelpie/KelpieRideEvents.java`

```java
package com.ixora.wildhorsedrag.kelpie;

import com.ixora.wildhorsedrag.WildHorseDragMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

@EventBusSubscriber(modid = WildHorseDragMod.MOD_ID)
public final class KelpieRideEvents {
    private KelpieRideEvents() {
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        /*
         * entityMounting is the rider.
         * entityBeingMounted is the horse.
         */
        Entity riderEntity = event.getEntityMounting();
        Entity mountEntity = event.getEntityBeingMounted();

        if (!(mountEntity instanceof Horse horse)) {
            return;
        }

        if (!(riderEntity instanceof ServerPlayer player)) {
            return;
        }

        if (horse.level().isClientSide()) {
            return;
        }

        if (!isWildKelpie(horse)) {
            return;
        }

        KelpieRideData data = KelpieRideState.get(horse);

        /*
         * Do not restart the sequence if a forced rider is being remounted.
         */
        if (data.state != KelpieRideData.State.IDLE) {
            return;
        }

        beginWarning(horse, player, data);
    }

    private static boolean isWildKelpie(Horse horse) {
        return horse.isAlive()
                && !horse.isTamed()
                && !horse.isBaby()
                && horse.getPersistentData()
                        .getBoolean("wildhorsedrag:is_kelpie");
    }

    private static void beginWarning(
            Horse horse,
            ServerPlayer rider,
            KelpieRideData data
    ) {
        data.state = KelpieRideData.State.WARNING;
        data.riderUuid = rider.getUUID();
        data.stateTicks = 30;
        data.escapeProgress = 0.0F;
        data.destination = null;

        /*
         * Optional starter feedback:
         *
         * horse.makeMad();
         * horse.playSound(...);
         * particles...
         *
         * Keep the warning period long enough that this does not feel like an
         * invisible, unavoidable trap.
         */
    }
}
```

NeoForge’s mount-event family is the right area to detect entities mounting and dismounting. Verify the exact current class and accessor names in your 26.1.2 generated sources—event naming has changed across Minecraft/NeoForge versions. [docs.blamejared](https://docs.blamejared.com/1.20/en/forge/api/event/entity/EntityMountEvent)

## Main forced-ride tick loop

This is the core system. It runs server-side once per tick per horse.

**File:** `kelpie/KelpieForcedRideHandler.java`

```java
package com.ixora.wildhorsedrag.kelpie;

import com.ixora.wildhorsedrag.WildHorseDragMod;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTickEvent;

@EventBusSubscriber(modid = WildHorseDragMod.MOD_ID)
public final class KelpieForcedRideHandler {
    private KelpieForcedRideHandler() {
    }

    private static final int WARNING_TICKS = 30;
    private static final int MAX_FORCED_RIDE_TICKS = 200;
    private static final int COOLDOWN_TICKS = 400;

    private static final int WATER_SEARCH_RADIUS = 32;
    private static final int REQUIRED_WATER_DEPTH = 2;

    private static final double BOLT_SPEED = 0.48D;
    private static final double WATER_BOLT_SPEED = 0.34D;

    private static final float ESCAPE_THRESHOLD = 100.0F;
    private static final float PASSIVE_ESCAPE_DECAY = 0.40F;

    @SubscribeEvent
    public static void onHorseTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Horse horse)) {
            return;
        }

        if (horse.level().isClientSide()) {
            return;
        }

        if (!(horse.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        KelpieRideData data = KelpieRideState.get(horse);

        if (data.state == KelpieRideData.State.IDLE) {
            return;
        }

        switch (data.state) {
            case WARNING -> tickWarning(horse, serverLevel, data);

            case FORCED_RIDE -> tickForcedRide(horse, serverLevel, data);

            case RELEASE -> finishRelease(horse, serverLevel, data);

            case COOLDOWN -> tickCooldown(data);

            case IDLE -> {
            }
        }
    }

    private static void tickWarning(
            Horse horse,
            ServerLevel level,
            KelpieRideData data
    ) {
        ServerPlayer rider = findRider(level, data.riderUuid);

        /*
         * The rider escaped during the warning period. That is allowed.
         */
        if (rider == null || rider.getVehicle() != horse) {
            resetToIdle(data);
            return;
        }

        data.stateTicks--;

        /*
         * Optional “uneasy kelpie” behavior:
         * prevent ordinary wandering and face toward nearby water.
         */
        horse.getNavigation().stop();

        BlockPos water = findNearestDeepWater(
                level,
                horse.blockPosition(),
                WATER_SEARCH_RADIUS
        );

        if (water != null) {
            facePosition(horse, Vec3.atCenterOf(water));
        }

        if (data.stateTicks <= 0) {
            data.destination = water;

            /*
             * If there is no nearby deep water, do not trap the rider.
             * This avoids an unfair forced ride that has no kelpie payoff.
             */
            if (data.destination == null) {
                data.state = KelpieRideData.State.RELEASE;
                return;
            }

            data.state = KelpieRideData.State.FORCED_RIDE;
            data.stateTicks = MAX_FORCED_RIDE_TICKS;
            data.escapeProgress = 0.0F;

            horse.makeMad();
        }
    }

    private static void tickForcedRide(
            Horse horse,
            ServerLevel level,
            KelpieRideData data
    ) {
        ServerPlayer rider = findRider(level, data.riderUuid);

        if (rider == null || !rider.isAlive()) {
            data.state = KelpieRideData.State.RELEASE;
            return;
        }

        /*
         * If the player dismounted, remount them.
         *
         * This must occur server-side. The client can request a dismount,
         * but the server decides the actual passenger relationship.
         */
        if (rider.getVehicle() != horse) {
            attemptRemount(horse, rider, data);
        }

        /*
         * Choose/recalculate a water target if needed.
         */
        if (data.destination == null
                || !isDeepWater(level, data.destination)) {
            data.destination = findNearestDeepWater(
                    level,
                    horse.blockPosition(),
                    WATER_SEARCH_RADIUS
            );
        }

        /*
         * No water target means release safely. Do not force a player through
         * a long uncontrollable ride toward nowhere.
         */
        if (data.destination == null) {
            data.state = KelpieRideData.State.RELEASE;
            return;
        }

        Vec3 destination = Vec3.atCenterOf(data.destination);

        moveHorseToward(horse, destination);

        data.stateTicks--;

        /*
         * Escaping:
         *
         * This starter implementation only decays passive progress. A separate
         * client packet, described below, adds real struggle input.
         */
        data.escapeProgress = Math.max(
                0.0F,
                data.escapeProgress - PASSIVE_ESCAPE_DECAY
        );

        if (data.escapeProgress >= ESCAPE_THRESHOLD) {
            data.state = KelpieRideData.State.RELEASE;
            return;
        }

        /*
         * Release at destination or after the maximum time.
         */
        if (horse.distanceToSqr(destination) < 4.0D
                || data.stateTicks <= 0) {
            data.state = KelpieRideData.State.RELEASE;
        }
    }

    private static void finishRelease(
            Horse horse,
            ServerLevel level,
            KelpieRideData data
    ) {
        ServerPlayer rider = findRider(level, data.riderUuid);

        if (rider != null && rider.getVehicle() == horse) {
            /*
             * Server-forced release. This is one of the few places your mod
             * should call stopRiding() itself.
             */
            rider.stopRiding();
            rider.resetFallDistance();
        }

        horse.getNavigation().stop();

        data.riderUuid = null;
        data.destination = null;
        data.escapeProgress = 0.0F;
        data.stateTicks = COOLDOWN_TICKS;
        data.state = KelpieRideData.State.COOLDOWN;
    }

    private static void tickCooldown(KelpieRideData data) {
        data.stateTicks--;

        if (data.stateTicks <= 0) {
            resetToIdle(data);
        }
    }

    private static void resetToIdle(KelpieRideData data) {
        data.state = KelpieRideData.State.IDLE;
        data.riderUuid = null;
        data.destination = null;
        data.stateTicks = 0;
        data.escapeProgress = 0.0F;
        data.lastRemountGameTime = -1L;
    }

    private static void attemptRemount(
            Horse horse,
            ServerPlayer rider,
            KelpieRideData data
    ) {
        long gameTime = horse.level().getGameTime();

        /*
         * Avoid issuing more than one remount attempt per tick.
         */
        if (data.lastRemountGameTime == gameTime) {
            return;
        }

        data.lastRemountGameTime = gameTime;

        /*
         * stopRiding from the player has already happened. Re-add them as a
         * passenger only when the horse is still close enough and valid.
         */
        if (!horse.isAlive()
                || !rider.isAlive()
                || rider.distanceToSqr(horse) > 16.0D) {
            data.state = KelpieRideData.State.RELEASE;
            return;
        }

        rider.startRiding(horse, true);
    }

    private static void moveHorseToward(Horse horse, Vec3 destination) {
        Vec3 current = horse.position();

        Vec3 flatDirection = new Vec3(
                destination.x - current.x,
                0.0D,
                destination.z - current.z
        );

        if (flatDirection.lengthSqr() < 0.01D) {
            return;
        }

        flatDirection = flatDirection.normalize();

        float yaw = (float) (
                Mth.atan2(flatDirection.z, flatDirection.x)
                        * (180.0D / Math.PI)
        ) - 90.0F;

        horse.setYRot(yaw);
        horse.yBodyRot = yaw;
        horse.yHeadRot = yaw;

        boolean inWater = horse.isInWater();

        double speed = inWater
                ? WATER_BOLT_SPEED
                : BOLT_SPEED;

        /*
         * Direct velocity gives a decisive supernatural bolt.
         *
         * For more obstacle-aware movement, use horse.getNavigation().moveTo()
         * until the horse reaches water, then use direct water movement.
         */
        Vec3 oldVelocity = horse.getDeltaMovement();

        horse.setDeltaMovement(
                flatDirection.x * speed,
                oldVelocity.y,
                flatDirection.z * speed
        );

        horse.getNavigation().stop();
    }

    @Nullable
    private static ServerPlayer findRider(
            ServerLevel level,
            @Nullable UUID riderUuid
    ) {
        if (riderUuid == null) {
            return null;
        }

        return level.getServer().getPlayerList()
                .getPlayer(riderUuid);
    }

    @Nullable
    private static BlockPos findNearestDeepWater(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {
        BlockPos best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                /*
                 * Check close-to-far-ish through distance comparison rather
                 * than relying on loop order.
                 */
                BlockPos candidate = origin.offset(x, 0, z);

                for (int y = -4; y <= 4; y++) {
                    BlockPos test = candidate.offset(0, y, 0);

                    if (!isDeepWater(level, test)) {
                        continue;
                    }

                    double distanceSq = origin.distSqr(test);

                    if (distanceSq < bestDistanceSq) {
                        bestDistanceSq = distanceSq;
                        best = test.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static boolean isDeepWater(
            ServerLevel level,
            BlockPos pos
    ) {
        for (int depth = 0; depth < REQUIRED_WATER_DEPTH; depth++) {
            FluidState fluid = level.getFluidState(pos.below(depth));

            if (!fluid.is(Fluids.WATER)) {
                return false;
            }
        }

        return true;
    }

    private static void facePosition(Horse horse, Vec3 position) {
        double dx = position.x - horse.getX();
        double dz = position.z - horse.getZ();

        float yaw = (float) (
                Mth.atan2(dz, dx)
                        * (180.0D / Math.PI)
        ) - 90.0F;

        horse.setYRot(yaw);
        horse.yBodyRot = yaw;
        horse.yHeadRot = yaw;
    }
}
```

## Prevent voluntary dismount

There are two options. Use both if your 26.1.2 event API exposes the needed mount/dismount event.

### Option A: cancel a dismount event

If `EntityMountEvent` in your version distinguishes mounting from dismounting, cancel only the dismount while state is `FORCED_RIDE`.

The exact event fields should be confirmed in your IDE because NeoForge event names/accessors vary with mappings and version. In older NeoForge APIs, `EntityMountEvent` represents an entity mounting or dismounting another entity, while the event provides the entity being mounted and the entity mounting/dismounting. [docs.blamejared](https://docs.blamejared.com/1.20/en/forge/api/event/entity/EntityMountEvent)

Conceptually:

```java
@SubscribeEvent
public static void onDismount(EntityMountEvent event) {
    if (!event.isDismounting()) {
        return;
    }

    if (!(event.getEntityBeingMounted() instanceof Horse horse)) {
        return;
    }

    if (!(event.getEntityMounting() instanceof ServerPlayer player)) {
        return;
    }

    KelpieRideData data = KelpieRideState.get(horse);

    if (data.state == KelpieRideData.State.FORCED_RIDE
            && player.getUUID().equals(data.riderUuid)) {
        event.setCanceled(true);
    }
}
```

If that exact API compiles in your environment, it prevents the dismount before it happens.

### Option B: enforce re-mounting

Keep the `attemptRemount(...)` code in the tick handler even if you cancel an event. It covers cases such as:

- A dismount caused by another mod.
- A server command or teleport side effect.
- An entity collision.
- A vanilla edge case.
- Mapping/event changes that make cancellation impractical.

Do **not** teleport the player directly onto the horse every tick. Only re-mount them when they have actually dismounted, and only while they remain close. This reduces jitter and avoids turning the system into a permanent inescapable restraint.

## Add a struggle mechanic

A forced ride should have a clear escape path. The best implementation is a small **client-to-server payload** that says “the trapped rider pressed the struggle key,” then the server validates that player’s forced-ride state and increments progress.

NeoForge custom payloads are the correct pattern for a client key action that must alter authoritative server state; payload types and codecs register through `RegisterPayloadHandlersEvent`. [docs.neoforged](https://docs.neoforged.net/docs/networking/payload/)

### Payload

**File:** `network/KelpieStrugglePayload.java`

```java
package com.ixora.wildhorsedrag.network;

import com.ixora.wildhorsedrag.WildHorseDragMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record KelpieStrugglePayload()
        implements CustomPacketPayload {

    public static final Type<KelpieStrugglePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    WildHorseDragMod.MOD_ID,
                    "kelpie_struggle"
            ));

    public static final StreamCodec<ByteBuf, KelpieStrugglePayload>
            STREAM_CODEC = StreamCodec.unit(
                    new KelpieStrugglePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Server-side validation

Add this handler to your network registration class:

```java
private static void handleKelpieStruggle(
        KelpieStrugglePayload payload,
        IPayloadContext context
) {
    context.enqueueWork(() -> {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.getVehicle() instanceof Horse horse)) {
            return;
        }

        KelpieRideData data = KelpieRideState.get(horse);

        if (data.state != KelpieRideData.State.FORCED_RIDE) {
            return;
        }

        if (data.riderUuid == null
                || !data.riderUuid.equals(player.getUUID())) {
            return;
        }

        /*
         * Use a rate limit—otherwise a hacked client can spam packets.
         * See the timestamp section below.
         */
        data.escapeProgress += 4.0F;
    });
}
```

### Rate limiting

Add this field to `KelpieRideData`:

```java
public long lastStruggleGameTime = -1L;
```

Then update the handler:

```java
long gameTime = horse.level().getGameTime();

if (data.lastStruggleGameTime == gameTime) {
    return;
}

data.lastStruggleGameTime = gameTime;
data.escapeProgress = Math.min(
        100.0F,
        data.escapeProgress + 4.0F
);
```

That permits at most one successful struggle increment per game tick. You can make it stricter—one press every 3–5 ticks—if you want the escape sequence to last several seconds.

### Client key

Register a key such as `R` or use repeated jump presses. A dedicated key is clearer and avoids fighting vanilla horse-jump behavior.

```java
public static final KeyMapping STRUGGLE = new KeyMapping(
        "key.wildhorsedrag.kelpie_struggle",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
);
```

In client tick:

```java
while (STRUGGLE.consumeClick()) {
    ClientPacketDistributor.sendToServer(
            new KelpieStrugglePayload()
    );
}
```

A player then sees a direct interaction loop:

```text
Mount wild kelpie
→ horse becomes tense
→ it bolts toward water
→ press R repeatedly to struggle free
→ escape bar fills
→ kelpie ejects rider and flees / enters cooldown
```

## Optional escape conditions

Use more than one escape route so players are not forced into button-mashing.

| Escape condition | Code approach |
|---|---|
| Struggle key | Increment validated `escapeProgress` through a rate-limited payload |
| Special bridle | Check a bridle equipment state; immediately set state to `RELEASE` |
| Iron item | Detect item held by rider and reduce lock duration or add escape progress |
| Ally intervention | Another player hits the kelpie, uses an item, or cuts a tether |
| Deep-water weakness | Force release if the kelpie leaves water/enters a hot biome for too long |
| Damage threshold | Release rider if kelpie takes a significant amount of damage |
| Time limit | Always release after 10–12 seconds |
| Safe-mode config | Disable forced riding against players but retain it for mobs |

For multiplayer fairness, I strongly recommend all of these as defaults:

```text
Warning period: 1.5 seconds
Maximum forced ride: 8–10 seconds
Escape: possible in ~3–6 seconds with active struggle
Cooldown: at least 20 seconds
No trigger: creative or spectator players
No trigger: tamed kelpies / owner
No trigger: no nearby deep water
```

## Mob riders

For other mobs, there is no client dismount key, so the design is simpler:

- Store the `LivingEntity` UUID instead of only a `ServerPlayer` UUID.
- Call `target.startRiding(horse, true)`.
- On each tick, if it no longer rides, try to re-add it as a passenger.
- Do not add a struggle packet.
- Release it after the time limit, at water, or on a damage/escape condition.

Change:

```java
@Nullable
public UUID riderUuid;
```

to:

```java
@Nullable
public UUID victimUuid;
```

Then resolve the entity through:

```java
Entity entity = level.getEntity(data.victimUuid);

if (!(entity instanceof LivingEntity victim)) {
    // release/reset
}
```

For an NPC/mob target, you may want the kelpie to select it, bite it, and then mount it as a passenger only during the “carried” portion. For a player, mounting the player first and locking dismount is the more folklore-like “sticky hide” version.

## Important safety and compatibility notes

- Keep all real state changes **server-side**: beginning the forced ride, mounting/remounting, moving the horse, and releasing the rider.
- Never trust a client packet without validating that the sender is the recorded rider and that their vehicle is the recorded kelpie.
- Add a timeout and several escape conditions; otherwise the system risks feeling like an unfun control-loss effect.
- Use protected-block/claim compatibility if the kelpie can alter water or terrain.
- Avoid direct position teleports every tick; use ordinary passenger relationships and horse velocity/navigation where possible.
- Use a data attachment before release if you need save/load persistence, client animation state, or full genetics integration. NeoForge attachments are designed for additional per-entity data and can be serialized, unlike the temporary map shown here. [docs.neoforged](https://docs.neoforged.net/docs/datastorage/attachments/)

## Recommended implementation order

1. Add the kelpie marker and `KelpieRideData` map.
2. Detect a player mounting a wild kelpie.
3. Implement `WARNING` only; log state transitions.
4. Implement `FORCED_RIDE` toward nearest deep water.
5. Add the re-mount enforcement loop.
6. Add safe `RELEASE` and cooldown.
7. Add the dedicated struggle key and server-validated payload.
8. Add particles, sound, horse rearing/bolt animation, and an escape-progress HUD.
9. Move the data into a serialized entity attachment.
10. Add bridle/iron/ally escape options and configurable server rules.
