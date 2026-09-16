Below is a complete starter mod for **NeoForge 26.1.2** that turns a normal `Horse` into a real flying mount while ridden:

- Mount a horse.
- Press **F** to toggle flight.
- Use normal **forward/back/strafe** controls to steer.
- Use **Jump/Space** to rise.
- Use **Sneak/Shift** to descend.
- The server—not the client—moves the horse, so it works in multiplayer.
- The horse has no gravity while flying and takes no fall damage during/just after flight.

This is a functional “Pegasus movement” implementation, not player creative flight. It uses a client key-state packet because the server cannot reliably know whether a mounted client is holding jump or sneak. NeoForge’s current payload system is designed for custom client/server data and explicitly provides `ClientPacketDistributor.sendToServer` for this pattern. [docs.neoforged](https://docs.neoforged.net/docs/networking/payload/)

## Project structure

Use this layout, replacing `com.ixora.flyinghorses` if your package is different:

```text
src/main/java/
└── com/ixora/flyinghorses/
    ├── FlyingHorses.java
    ├── client/
    │   └── ClientFlightInput.java
    ├── network/
    │   ├── FlightInputPayload.java
    │   └── ModNetworking.java
    └── server/
        └── HorseFlightHandler.java

src/main/resources/
├── META-INF/
│   └── neoforge.mods.toml
└── assets/flyinghorses/lang/
    └── en_us.json
```

The mod ID used below is:

```java
public static final String MOD_ID = "flyinghorses";
```

Your `neoforge.mods.toml`, resource folder, Java package, and identifier paths must all use that same ID consistently.

***

## 1. Main mod class

**File:** `FlyingHorses.java`

```java
package com.ixora.flyinghorses;

import net.neoforged.fml.common.Mod;

@Mod(FlyingHorses.MOD_ID)
public final class FlyingHorses {
    public static final String MOD_ID = "flyinghorses";

    public FlyingHorses() {
    }
}
```

***

## 2. Network payload

This packet sends three pieces of input from the riding client to the server:

- Whether jump is held.
- Whether sneak is held.
- Whether the flight-toggle key was pressed.

**File:** `network/FlightInputPayload.java`

```java
package com.ixora.flyinghorses.network;

import com.ixora.flyinghorses.FlyingHorses;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FlightInputPayload(
        boolean jumpHeld,
        boolean sneakHeld,
        boolean togglePressed
) implements CustomPacketPayload {

    public static final Type<FlightInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    FlyingHorses.MOD_ID,
                    "flight_input"
            ));

    public static final StreamCodec<ByteBuf, FlightInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    FlightInputPayload::jumpHeld,

                    ByteBufCodecs.BOOL,
                    FlightInputPayload::sneakHeld,

                    ByteBufCodecs.BOOL,
                    FlightInputPayload::togglePressed,

                    FlightInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

NeoForge payloads implement `CustomPacketPayload`, define a unique payload type, and provide a `StreamCodec` that encodes and decodes the packet fields. [docs.neoforged](https://docs.neoforged.net/docs/networking/payload/)

***

## 3. Packet registration and server handler

This registers the client-to-server payload and validates the player before changing anything.

The server ignores the packet unless its sender is currently riding a standard vanilla `Horse`. This validation matters: never trust the client to decide which entity it may move.

**File:** `network/ModNetworking.java`

```java
package com.ixora.flyinghorses.network;

import com.ixora.flyinghorses.FlyingHorses;
import com.ixora.flyinghorses.server.HorseFlightHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
        modid = FlyingHorses.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                FlightInputPayload.TYPE,
                FlightInputPayload.STREAM_CODEC,
                ModNetworking::handleFlightInput
        );
    }

    private static void handleFlightInput(
            FlightInputPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity vehicle = player.getVehicle();

            if (!(vehicle instanceof Horse horse)) {
                return;
            }

            HorseFlightHandler.setInput(
                    horse,
                    player,
                    payload.jumpHeld(),
                    payload.sneakHeld()
            );

            if (payload.togglePressed()) {
                HorseFlightHandler.toggleFlight(horse, player);
            }
        });
    }
}
```

Payload handlers run on the main thread by default, and NeoForge also supports `context.enqueueWork(...)` when a handler needs to ensure game-state changes happen on the main game thread. [docs.neoforged](https://docs.neoforged.net/docs/networking/payload/)

***

## 4. Client input and flight toggle key

This code runs only on the physical Minecraft client. It registers the **F** key and sends the current held-state every tick while you are riding a horse.

Sending the state every tick is intentional:

- `Space` and `Shift` are held keys, not one-time clicks.
- The server needs continuous input to apply vertical movement.
- The packet is tiny: three booleans.
- Server-side checks prevent it from doing anything when the player is not mounted on an eligible horse.

**File:** `client/ClientFlightInput.java`

```java
package com.ixora.flyinghorses.client;

import com.ixora.flyinghorses.FlyingHorses;
import com.ixora.flyinghorses.network.FlightInputPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = FlyingHorses.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientFlightInput {
    private ClientFlightInput() {
    }

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath(
                            FlyingHorses.MOD_ID,
                            "flying_horses"
                    )
            );

    public static final KeyMapping TOGGLE_FLIGHT = new KeyMapping(
            "key.flyinghorses.toggle_flight",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_FLIGHT);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        if (minecraft.screen != null) {
            return;
        }

        if (!(minecraft.player.getVehicle() instanceof Horse)) {
            return;
        }

        boolean jumpHeld = minecraft.options.keyJump.isDown();
        boolean sneakHeld = minecraft.options.keyShift.isDown();

        boolean togglePressed = false;

        while (TOGGLE_FLIGHT.consumeClick()) {
            togglePressed = true;
        }

        ClientPacketDistributor.sendToServer(
                new FlightInputPayload(
                        jumpHeld,
                        sneakHeld,
                        togglePressed
                )
        );
    }
}
```

NeoForge key mappings must be registered on the physical client through `RegisterKeyMappingsEvent`; user-configurable mappings can be checked from `ClientTickEvent.Post`. The key will appear in the Controls menu, so players can rebind it from the default `F`. [docs.neoforged](https://docs.neoforged.net/docs/misc/keymappings/)

***

## 5. Server-side horse flight movement

This is the main system. It tracks flight state and rider input per horse, then updates the horse’s velocity on the server.

**File:** `server/HorseFlightHandler.java`

```java
package com.ixora.flyinghorses.server;

import com.ixora.flyinghorses.FlyingHorses;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(modid = FlyingHorses.MOD_ID)
public final class HorseFlightHandler {
    private HorseFlightHandler() {
    }

    /*
     * Persistent-data keys.
     *
     * These live on the horse entity, not the player.
     * That means every individual horse can have independent flight state.
     */
    private static final String FLYING_KEY =
            "flyinghorses:is_flying";

    private static final String RIDER_UUID_KEY =
            "flyinghorses:rider_uuid";

    private static final String JUMP_HELD_KEY =
            "flyinghorses:jump_held";

    private static final String SNEAK_HELD_KEY =
            "flyinghorses:sneak_held";

    /*
     * Movement tuning.
     *
     * Horizontal speed is blocks per game tick.
     * Minecraft runs at 20 ticks per second.
     *
     * 0.50 blocks/tick is approximately 10 blocks/second,
     * before terrain/collision effects.
     */
    private static final double HORIZONTAL_SPEED = 0.50D;

    /*
     * Vertical rise/fall speed while holding space or shift.
     */
    private static final double VERTICAL_SPEED = 0.34D;

    /*
     * Gentle downward motion when the rider is not holding jump or sneak.
     * Set to 0.0D for full hover.
     * Set to around -0.03D to -0.06D for a more natural "gliding" feel.
     */
    private static final double IDLE_VERTICAL_SPEED = -0.025D;

    /*
     * Vertical velocity is capped so accumulated velocity never becomes extreme.
     */
    private static final double MAX_VERTICAL_SPEED = 0.45D;

    public static void setInput(
            Horse horse,
            ServerPlayer player,
            boolean jumpHeld,
            boolean sneakHeld
    ) {
        CompoundTag data = horse.getPersistentData();

        if (!data.getBoolean(FLYING_KEY)) {
            return;
        }

        if (!data.contains(RIDER_UUID_KEY)) {
            return;
        }

        if (!data.getUUID(RIDER_UUID_KEY).equals(player.getUUID())) {
            return;
        }

        data.putBoolean(JUMP_HELD_KEY, jumpHeld);
        data.putBoolean(SNEAK_HELD_KEY, sneakHeld);
    }

    public static void toggleFlight(Horse horse, ServerPlayer player) {
        CompoundTag data = horse.getPersistentData();

        boolean isFlying = data.getBoolean(FLYING_KEY);

        if (isFlying) {
            stopFlying(horse);
        } else {
            startFlying(horse, player);
        }
    }

    private static void startFlying(Horse horse, ServerPlayer player) {
        CompoundTag data = horse.getPersistentData();

        data.putBoolean(FLYING_KEY, true);
        data.putUUID(RIDER_UUID_KEY, player.getUUID());
        data.putBoolean(JUMP_HELD_KEY, false);
        data.putBoolean(SNEAK_HELD_KEY, false);

        horse.setNoGravity(true);
        horse.fallDistance = 0.0F;

        /*
         * Small upward bump prevents an awkward takeoff where the horse
         * remains visually glued to the ground for one tick.
         */
        Vec3 current = horse.getDeltaMovement();

        horse.setDeltaMovement(
                current.x,
                Math.max(current.y, 0.18D),
                current.z
        );
    }

    private static void stopFlying(Horse horse) {
        CompoundTag data = horse.getPersistentData();

        data.putBoolean(FLYING_KEY, false);
        data.remove(RIDER_UUID_KEY);
        data.putBoolean(JUMP_HELD_KEY, false);
        data.putBoolean(SNEAK_HELD_KEY, false);

        horse.setNoGravity(false);
        horse.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Horse horse)) {
            return;
        }

        if (horse.level().isClientSide()) {
            return;
        }

        CompoundTag data = horse.getPersistentData();

        if (!data.getBoolean(FLYING_KEY)) {
            return;
        }

        /*
         * Flight ends if the horse no longer has the original player riding it.
         * This handles normal dismounting, death, force-removal, and another
         * entity somehow replacing the rider.
         */
        if (!(horse.getControllingPassenger() instanceof ServerPlayer rider)) {
            stopFlying(horse);
            return;
        }

        if (!data.contains(RIDER_UUID_KEY)
                || !data.getUUID(RIDER_UUID_KEY).equals(rider.getUUID())) {
            stopFlying(horse);
            return;
        }

        tickFlyingHorse(horse, data);
    }

    private static void tickFlyingHorse(Horse horse, CompoundTag data) {
        horse.setNoGravity(true);
        horse.fallDistance = 0.0F;

        /*
         * Horse yaw is normally controlled by its rider while mounted.
         * Using the horse's yaw makes forward movement follow the direction
         * the mounted player steers the horse.
         */
        float yawRadians = horse.getYRot() * Mth.DEG_TO_RAD;

        double forwardX = -Mth.sin(yawRadians);
        double forwardZ = Mth.cos(yawRadians);

        /*
         * Use the horse's standard forward/strafe inputs.
         *
         * zza: forward/back movement
         * xxa: left/right strafe input
         *
         * A horse normally supports forward/back much better than strafe,
         * but including xxa gives the mount a slightly more free-flight feel.
         */
        float forwardInput = horse.zza;
        float strafeInput = horse.xxa;

        /*
         * Normalize diagonal input so forward+strafe is not faster than
         * moving only forward.
         */
        double inputLength = Math.sqrt(
                forwardInput * forwardInput
                        + strafeInput * strafeInput
        );

        double horizontalX = 0.0D;
        double horizontalZ = 0.0D;

        if (inputLength > 0.001D) {
            double normalizedForward = forwardInput / inputLength;
            double normalizedStrafe = strafeInput / inputLength;

            double rightX = Mth.cos(yawRadians);
            double rightZ = Mth.sin(yawRadians);

            horizontalX = (
                    forwardX * normalizedForward
                            + rightX * normalizedStrafe
            ) * HORIZONTAL_SPEED;

            horizontalZ = (
                    forwardZ * normalizedForward
                            + rightZ * normalizedStrafe
            ) * HORIZONTAL_SPEED;
        }

        boolean jumpHeld = data.getBoolean(JUMP_HELD_KEY);
        boolean sneakHeld = data.getBoolean(SNEAK_HELD_KEY);

        double verticalVelocity;

        if (jumpHeld && !sneakHeld) {
            verticalVelocity = VERTICAL_SPEED;
        } else if (sneakHeld && !jumpHeld) {
            verticalVelocity = -VERTICAL_SPEED;
        } else {
            verticalVelocity = IDLE_VERTICAL_SPEED;
        }

        verticalVelocity = Mth.clamp(
                verticalVelocity,
                -MAX_VERTICAL_SPEED,
                MAX_VERTICAL_SPEED
        );

        horse.setDeltaMovement(
                horizontalX,
                verticalVelocity,
                horizontalZ
        );

        /*
         * Prevent the vanilla horse AI/navigation from fighting the custom
         * aerial movement while it has a rider.
         */
        horse.getNavigation().stop();

        /*
         * Keep vanilla's horse jump bar from trying to produce a ground jump
         * while the Pegasus system controls vertical motion.
         */
        horse.playerJumpPendingScale = 0.0F;
    }

    @SubscribeEvent
    public static void onHorseFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }

        CompoundTag data = horse.getPersistentData();

        if (data.getBoolean(FLYING_KEY)) {
            event.setCanceled(true);
            horse.fallDistance = 0.0F;
        }
    }
}
```

`EntityTickEvent.Post` is fired once per entity per game tick after the entity performs its normal work, which makes it suitable for applying your mount’s final server-authoritative movement for that tick. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/neoforged/neoforge/event/entity/class-use/EntityEvent.html)

## 6. Language file

**File:** `src/main/resources/assets/flyinghorses/lang/en_us.json`

```json
{
  "key.category.flyinghorses.flying_horses": "Flying Horses",
  "key.flyinghorses.toggle_flight": "Toggle Horse Flight"
}
```

This makes the Controls screen display:

```text
Flying Horses
  Toggle Horse Flight: F
```

## 7. What to change first

The easiest values to tune are at the top of `HorseFlightHandler.java`:

```java
private static final double HORIZONTAL_SPEED = 0.50D;
private static final double VERTICAL_SPEED = 0.34D;
private static final double IDLE_VERTICAL_SPEED = -0.025D;
```

| Desired feel | Suggested changes |
|---|---:|
| Slow, believable winged horse | Horizontal `0.30`, vertical `0.22`, idle `-0.03` |
| Fast fantasy mount | Horizontal `0.75`, vertical `0.45`, idle `0.0` |
| Perfect hover | Idle vertical `0.0` |
| Gentle gliding descent | Idle vertical `-0.015` to `-0.04` |
| Strong downward dive | Sneak vertical `-0.50` to `-0.70` |

For your horse-genetics/simulation use case, I would eventually make these values horse-specific rather than global. For example, a horse could have inherited traits such as:

```text
flightEnabled       true / false
flightSpeed         0.30–0.80
climbRate           0.18–0.50
glideRate           -0.01 to -0.08
stamina             0–100
wingSize            visual/stat modifier
```

Then use per-horse persistent data or NeoForge data attachments rather than fixed constants.

## Important limitations

This version is deliberately scoped to normal vanilla `Horse` entities:

```java
player.getVehicle() instanceof Horse
```

If you want it to work for donkeys, mules, skeleton horses, and zombie horses, change every relevant `Horse` type to:

```java
AbstractHorse
```

and import:

```java
import net.minecraft.world.entity.animal.horse.AbstractHorse;
```

However, test that carefully. Undead horses and pack-animal mounts can have slightly different mounted behavior and capabilities.

Also, this code changes physics but does not add:

- Wings to the horse model.
- Flying animations.
- Wing-flap sounds.
- Particles.
- Stamina or cooldown.
- A requirement that the horse be tamed.
- A genetic “pegasus” trait.

The mod currently allows **any mounted vanilla horse** to fly. To require taming, add this inside `toggleFlight` before calling `startFlying`:

```java
if (!horse.isTamed()) {
    return;
}
```

To make flight a special trait, add an NBT condition such as:

```java
if (!horse.getPersistentData().getBoolean("flyinghorses:can_fly")) {
    return;
}
```

Then only horses that have been bred, spawned, converted, or otherwise assigned `flyinghorses:can_fly = true` can fly.

## If it fails to compile

NeoForge 26.1.x is very new, and mappings can differ slightly depending on your exact Minecraft/NeoForge development template. The most likely line needing adjustment is:

```java
horse.playerJumpPendingScale = 0.0F;
```

If that field is inaccessible or renamed in your environment, simply delete that one line. The mount will still fly; the only difference is that pressing Space may briefly interact with the ordinary horse-jump mechanic before your next server tick overrides the vertical velocity.

The other likely mapping difference is the tick event import. If your IDE cannot find:

```java
net.neoforged.neoforge.event.entity.EntityTickEvent
```

search your generated sources for `EntityTickEvent` and use its displayed package. NeoForge’s entity documentation identifies `tick` as the main per-entity update path, and the event API exposes pre/post entity tick events for additional behavior. [nekoyue.github](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/neoforged/neoforge/event/entity/class-use/EntityEvent.html)

## Next improvement: smoother client motion

This code is server-authoritative and should be safe in multiplayer, but the horse may feel slightly less smooth at high latency because movement begins after the client sends input and the server responds.

A polished version would add **client-side prediction**:

- The client calculates the same intended flight velocity locally.
- It visually applies that motion immediately.
- The server remains authoritative and corrects the position if needed.
- The horse’s wing animation is driven from synced `isFlying` data.

Build and test the server-authoritative version first. It establishes the correct control, safety, and dismount behavior; prediction and animations become much easier once the base mechanics are stable.
