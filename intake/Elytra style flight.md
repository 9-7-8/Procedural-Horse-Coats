Yes. Here is starter code for a **true elytra-style flying horse**: it preserves momentum, gains speed while diving, converts speed into lift when the rider pulls up, slowly loses speed to air drag, and cannot simply hover in place.

This is intentionally a physics prototype rather than a full polished Pegasus system. It reuses the packet/key structure from the earlier mount example, but replaces the “fixed velocity every tick” logic with an elytra-like glide calculation based on the rider’s yaw, pitch, and the horse’s existing velocity. Vanilla elytra flight itself is a gliding system rather than free hover flight. [gist.github](https://gist.github.com/samsartor/a7ec457aca23a7f3f120)

## Behavior

Controls in this starter version:

| Control | Action |
|---|---|
| `F` | Toggle the horse’s gliding state |
| `W` | Optional flap/boost input—adds a small forward push |
| Mouse look | Steers the direction of flight |
| Look down | Dive, gain speed |
| Look up | Pull up, exchange speed for altitude |
| Space | Optional wing-flap boost |
| Shift | Does not force descent; pitch controls descent like elytra |
| Stop moving / low speed | Horse eventually stalls and falls |

Unlike the hovering version:

- Space does **not** continuously lift the horse.
- A horse cannot remain still in midair.
- Its current velocity carries over from tick to tick.
- Forward momentum matters.
- The rider must manage altitude and speed through diving and climbing.

## Keep these files

Keep these from the earlier example, unchanged in concept:

- `FlyingHorses.java`
- `network/FlightInputPayload.java`
- `network/ModNetworking.java`
- `client/ClientFlightInput.java`

However, update the packet to include a **boost/flap** key state instead of treating Space as vertical ascent.

***

## 1. Updated payload

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
        boolean boostHeld,
        boolean forwardHeld,
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
                    FlightInputPayload::boostHeld,

                    ByteBufCodecs.BOOL,
                    FlightInputPayload::forwardHeld,

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

NeoForge payloads are registered with a type plus `StreamCodec`; the codec defines how the boolean fields are serialized between the player’s client and server. [docs.neoforged](https://docs.neoforged.net/docs/networking/streamcodecs/)

## 2. Updated server packet handler

Replace the handler body in `ModNetworking.java` with this version.

```java
package com.ixora.flyinghorses.network;

import com.ixora.flyinghorses.FlyingHorses;
import com.ixora.flyinghorses.server.HorseGlideHandler;
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

            HorseGlideHandler.setRiderInput(
                    horse,
                    player,
                    payload.boostHeld(),
                    payload.forwardHeld()
            );

            if (payload.togglePressed()) {
                HorseGlideHandler.toggleGliding(horse, player);
            }
        });
    }
}
```

Custom client-to-server input needs server-side validation—as above—to ensure the player is really riding the affected horse before the mod applies any movement. NeoForge registers serverbound payload handlers through `RegisterPayloadHandlersEvent` and `PayloadRegistrar`. [docs.neoforged](https://docs.neoforged.net/docs/1.20.6/networking/payload/)

## 3. Updated client input

This version sends:

- `boostHeld`: whether Space is held.
- `forwardHeld`: whether W is held.
- `togglePressed`: whether the player pressed F.

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

    public static final KeyMapping TOGGLE_GLIDE = new KeyMapping(
            "key.flyinghorses.toggle_glide",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_GLIDE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (!(minecraft.player.getVehicle() instanceof Horse)) {
            return;
        }

        boolean boostHeld = minecraft.options.keyJump.isDown();
        boolean forwardHeld = minecraft.options.keyUp.isDown();

        boolean togglePressed = false;

        while (TOGGLE_GLIDE.consumeClick()) {
            togglePressed = true;
        }

        ClientPacketDistributor.sendToServer(
                new FlightInputPayload(
                        boostHeld,
                        forwardHeld,
                        togglePressed
                )
        );
    }
}
```

Key mappings are registered only on the physical client, and `ClientTickEvent.Post` is an appropriate place to poll the state of a mapping while the game is running. [docs.neoforged](https://docs.neoforged.net/docs/misc/keymappings/)

## 4. Elytra-style horse physics

This is the core replacement for the old `HorseFlightHandler`.

**File:** `server/HorseGlideHandler.java`

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
public final class HorseGlideHandler {
    private HorseGlideHandler() {
    }

    private static final String GLIDING_KEY =
            "flyinghorses:is_gliding";

    private static final String RIDER_UUID_KEY =
            "flyinghorses:glide_rider_uuid";

    private static final String BOOST_HELD_KEY =
            "flyinghorses:boost_held";

    private static final String FORWARD_HELD_KEY =
            "flyinghorses:forward_held";

    /*
     * Core physics constants.
     *
     * Tune these after testing in-game. These are deliberately less extreme
     * than literal vanilla elytra behavior so a large horse mount remains
     * controllable.
     */
    private static final double GRAVITY = 0.08D;

    /*
     * Lift applied while the mount has horizontal speed.
     * Larger value = easier to stay aloft.
     */
    private static final double LIFT_STRENGTH = 0.055D;

    /*
     * How much a downward-facing rider converts altitude into forward speed.
     */
    private static final double DIVE_ACCELERATION = 0.060D;

    /*
     * Direction correction: nudges velocity toward rider look direction.
     * Bigger values make steering tighter and less momentum-based.
     */
    private static final double STEERING_STRENGTH = 0.095D;

    /*
     * Multiplied into velocity every tick.
     * Lower = more drag, slower flight.
     * Higher = more momentum, easier long glides.
     */
    private static final double AIR_DRAG = 0.990D;

    /*
     * Horizontal speed limits in blocks per tick.
     *
     * 0.50 blocks/tick = roughly 10 blocks per second.
     * 1.20 blocks/tick = roughly 24 blocks per second.
     */
    private static final double MAX_HORIZONTAL_SPEED = 1.20D;

    /*
     * Below this speed, lift becomes ineffective and the horse stalls.
     */
    private static final double STALL_SPEED = 0.12D;

    /*
     * Forward thrust while Space is held.
     * This represents one or more powerful wing beats.
     */
    private static final double FLAP_FORWARD_BOOST = 0.025D;

    /*
     * Small lift supplied by flap input. This is a helpful fantasy-mount
     * concession; set to 0.0D for closer-to-pure-elytra behavior.
     */
    private static final double FLAP_VERTICAL_BOOST = 0.013D;

    /*
     * Limit vertical velocity to keep dives/climbs manageable.
     */
    private static final double MAX_RISE_SPEED = 0.55D;
    private static final double MAX_FALL_SPEED = -1.25D;

    public static void setRiderInput(
            Horse horse,
            ServerPlayer player,
            boolean boostHeld,
            boolean forwardHeld
    ) {
        CompoundTag data = horse.getPersistentData();

        if (!data.getBoolean(GLIDING_KEY)) {
            return;
        }

        if (!data.contains(RIDER_UUID_KEY)) {
            return;
        }

        if (!data.getUUID(RIDER_UUID_KEY).equals(player.getUUID())) {
            return;
        }

        data.putBoolean(BOOST_HELD_KEY, boostHeld);
        data.putBoolean(FORWARD_HELD_KEY, forwardHeld);
    }

    public static void toggleGliding(Horse horse, ServerPlayer player) {
        CompoundTag data = horse.getPersistentData();

        if (data.getBoolean(GLIDING_KEY)) {
            stopGliding(horse);
        } else {
            startGliding(horse, player);
        }
    }

    private static void startGliding(Horse horse, ServerPlayer player) {
        CompoundTag data = horse.getPersistentData();

        data.putBoolean(GLIDING_KEY, true);
        data.putUUID(RIDER_UUID_KEY, player.getUUID());
        data.putBoolean(BOOST_HELD_KEY, false);
        data.putBoolean(FORWARD_HELD_KEY, false);

        horse.setNoGravity(true);
        horse.fallDistance = 0.0F;

        /*
         * Start the horse with forward momentum. Without this, a stationary
         * horse would immediately stall before it had enough airspeed to lift.
         */
        Vec3 look = player.getLookAngle();

        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);

        if (flatLook.lengthSqr() < 0.0001D) {
            flatLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flatLook = flatLook.normalize();
        }

        Vec3 oldVelocity = horse.getDeltaMovement();

        horse.setDeltaMovement(
                flatLook.x * 0.45D,
                Math.max(oldVelocity.y, 0.08D),
                flatLook.z * 0.45D
        );
    }

    private static void stopGliding(Horse horse) {
        CompoundTag data = horse.getPersistentData();

        data.putBoolean(GLIDING_KEY, false);
        data.remove(RIDER_UUID_KEY);
        data.putBoolean(BOOST_HELD_KEY, false);
        data.putBoolean(FORWARD_HELD_KEY, false);

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

        if (!data.getBoolean(GLIDING_KEY)) {
            return;
        }

        if (!(horse.getControllingPassenger() instanceof ServerPlayer rider)) {
            stopGliding(horse);
            return;
        }

        if (!data.contains(RIDER_UUID_KEY)
                || !data.getUUID(RIDER_UUID_KEY).equals(rider.getUUID())) {
            stopGliding(horse);
            return;
        }

        tickGlidingHorse(horse, rider, data);
    }

    private static void tickGlidingHorse(
            Horse horse,
            ServerPlayer rider,
            CompoundTag data
    ) {
        horse.setNoGravity(true);
        horse.fallDistance = 0.0F;

        Vec3 velocity = horse.getDeltaMovement();

        /*
         * Rider look direction:
         *
         * X rotation / pitch:
         *   Negative = looking upward
         *   Positive = looking downward
         *
         * Y rotation / yaw:
         *   The horizontal facing direction.
         */
        float pitchRadians = rider.getXRot() * Mth.DEG_TO_RAD;

        Vec3 look = rider.getLookAngle();

        Vec3 flatLook = new Vec3(
                look.x,
                0.0D,
                look.z
        );

        if (flatLook.lengthSqr() < 0.0001D) {
            flatLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flatLook = flatLook.normalize();
        }

        /*
         * Horizontal airspeed before this tick's acceleration.
         */
        double horizontalSpeed = Math.sqrt(
                velocity.x * velocity.x
                        + velocity.z * velocity.z
        );

        /*
         * pitchCos is 1.0 when looking level, 0.0 when looking straight
         * up/down. It controls how much "wing area" produces lift.
         */
        double pitchCos = Mth.cos(pitchRadians);

        /*
         * liftFactor is strongest while looking roughly level.
         * It shrinks when looking straight up/down.
         */
        double liftFactor = pitchCos * pitchCos;

        /*
         * 1. Base gravity. This keeps the mount descending unless it has
         * enough horizontal speed to generate lift.
         */
        velocity = velocity.add(0.0D, -GRAVITY, 0.0D);

        /*
         * 2. Lift. Faster horizontal movement produces more lift.
         *
         * With enough speed, level flight becomes possible.
         * At low speed, lift is weak and the horse stalls.
         */
        double lift = horizontalSpeed
                * horizontalSpeed
                * liftFactor
                * LIFT_STRENGTH;

        velocity = velocity.add(0.0D, lift, 0.0D);

        /*
         * 3. Dive acceleration.
         *
         * Mth.sin(pitch) is positive when the rider looks down.
         * Looking down adds momentum in the current look direction,
         * including a little extra downward velocity.
         */
        double diveAmount = Math.max(
                0.0D,
                Mth.sin(pitchRadians)
        );

        if (diveAmount > 0.0D) {
            velocity = velocity.add(
                    look.x * diveAmount * DIVE_ACCELERATION,
                    look.y * diveAmount * DIVE_ACCELERATION,
                    look.z * diveAmount * DIVE_ACCELERATION
            );
        }

        /*
         * 4. Steering.
         *
         * Blend the current horizontal velocity toward the rider's
         * horizontal look direction. This provides controllable turns,
         * while still preserving momentum.
         */
        if (horizontalSpeed > 0.001D) {
            Vec3 desiredHorizontal = flatLook.scale(horizontalSpeed);

            double steeredX = Mth.lerp(
                    STEERING_STRENGTH,
                    velocity.x,
                    desiredHorizontal.x
            );

            double steeredZ = Mth.lerp(
                    STEERING_STRENGTH,
                    velocity.z,
                    desiredHorizontal.z
            );

            velocity = new Vec3(
                    steeredX,
                    velocity.y,
                    steeredZ
            );
        }

        /*
         * 5. Wing flap / boost.
         *
         * Space adds limited forward thrust. This is optional; remove this
         * block for a nearly pure elytra-style glide that cannot self-power.
         */
        boolean boostHeld = data.getBoolean(BOOST_HELD_KEY);
        boolean forwardHeld = data.getBoolean(FORWARD_HELD_KEY);

        if (boostHeld || forwardHeld) {
            velocity = velocity.add(
                    flatLook.x * FLAP_FORWARD_BOOST,
                    FLAP_VERTICAL_BOOST,
                    flatLook.z * FLAP_FORWARD_BOOST
            );
        }

        /*
         * 6. Air drag.
         *
         * This prevents unlimited acceleration and makes the mount gradually
         * lose speed if it is not diving or using boost.
         */
        velocity = velocity.scale(AIR_DRAG);

        /*
         * 7. Clamp horizontal and vertical speeds.
         */
        double newHorizontalSpeed = Math.sqrt(
                velocity.x * velocity.x
                        + velocity.z * velocity.z
        );

        if (newHorizontalSpeed > MAX_HORIZONTAL_SPEED) {
            double scale = MAX_HORIZONTAL_SPEED / newHorizontalSpeed;

            velocity = new Vec3(
                    velocity.x * scale,
                    velocity.y,
                    velocity.z * scale
            );

            newHorizontalSpeed = MAX_HORIZONTAL_SPEED;
        }

        double clampedY = Mth.clamp(
                velocity.y,
                MAX_FALL_SPEED,
                MAX_RISE_SPEED
        );

        velocity = new Vec3(
                velocity.x,
                clampedY,
                velocity.z
        );

        /*
         * 8. Stall behavior.
         *
         * A very slow horse cannot create lift. Let it fall normally until
         * a dive or flap gets it moving again.
         */
        if (newHorizontalSpeed < STALL_SPEED) {
            velocity = new Vec3(
                    velocity.x,
                    Math.min(velocity.y, -0.10D),
                    velocity.z
            );
        }

        horse.setDeltaMovement(velocity);

        /*
         * Stop ground pathfinding/horse-jump behavior from competing with
         * aerial motion.
         */
        horse.getNavigation().stop();
        horse.playerJumpPendingScale = 0.0F;
    }

    @SubscribeEvent
    public static void onHorseFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }

        if (horse.getPersistentData().getBoolean(GLIDING_KEY)) {
            event.setCanceled(true);
            horse.fallDistance = 0.0F;
        }
    }
}
```

The physics model is based on the essential characteristics of elytra behavior: gravity, lift proportional to speed, dive-based acceleration, retained momentum, and drag. The well-known vanilla glide simulation shows the same broad structure: apply gravity, add lift based on horizontal velocity and pitch, use diving to increase speed, then damp velocity. [gist.github](https://gist.github.com/samsartor/a7ec457aca23a7f3f120)

## Important tuning

Start with these values in `HorseGlideHandler`:

```java
private static final double LIFT_STRENGTH = 0.055D;
private static final double DIVE_ACCELERATION = 0.060D;
private static final double STEERING_STRENGTH = 0.095D;
private static final double AIR_DRAG = 0.990D;
private static final double MAX_HORIZONTAL_SPEED = 1.20D;
```

| If you want… | Change |
|---|---|
| Longer, easier glides | Raise `LIFT_STRENGTH` to `0.065–0.085` |
| Faster dive acceleration | Raise `DIVE_ACCELERATION` to `0.075–0.11` |
| More responsive turning | Raise `STEERING_STRENGTH` to `0.12–0.20` |
| More slippery/momentum-heavy flight | Lower steering, raise drag multiplier closer to `0.995` |
| Slower, more grounded flying | Lower max speed to `0.70–0.90` |
| Almost no self-propelled flight | Set both flap constants to `0.0D` |
| Strong magical wingbeats | Raise forward flap to `0.04–0.08` and vertical flap to `0.02–0.04` |

## Pure-glide variant

If you want a horse that behaves closest to an unpowered vanilla elytra—no sustained flapping—change:

```java
private static final double FLAP_FORWARD_BOOST = 0.025D;
private static final double FLAP_VERTICAL_BOOST = 0.013D;
```

to:

```java
private static final double FLAP_FORWARD_BOOST = 0.0D;
private static final double FLAP_VERTICAL_BOOST = 0.0D;
```

Then remove this block from `tickGlidingHorse`:

```java
boolean boostHeld = data.getBoolean(BOOST_HELD_KEY);
boolean forwardHeld = data.getBoolean(FORWARD_HELD_KEY);

if (boostHeld || forwardHeld) {
    velocity = velocity.add(
            flatLook.x * FLAP_FORWARD_BOOST,
            FLAP_VERTICAL_BOOST,
            flatLook.z * FLAP_FORWARD_BOOST
    );
}
```

The player must launch from a hill, tower, cliff, or a strong initial takeoff. They must dive to build speed, then pull up carefully to trade that speed for altitude—exactly the style of gameplay associated with elytra gliding. [minecraft](https://minecraft.wiki/w/Elytra)

## First changes I recommend

For a Pegasus that feels physically believable but remains fun:

```java
private static final double LIFT_STRENGTH = 0.070D;
private static final double DIVE_ACCELERATION = 0.070D;
private static final double STEERING_STRENGTH = 0.125D;
private static final double AIR_DRAG = 0.992D;
private static final double MAX_HORIZONTAL_SPEED = 1.00D;

private static final double FLAP_FORWARD_BOOST = 0.030D;
private static final double FLAP_VERTICAL_BOOST = 0.008D;
```

That gives the horse a gentler turning radius, meaningful dives, a little recovery power from wings, and avoids the frustrating “I instantly fell out of the sky” feeling of an entirely unassisted glider.

## Compile note

As with the previous code, this line may differ slightly in your exact mappings:

```java
horse.playerJumpPendingScale = 0.0F;
```

If IntelliJ says that it is private, protected, missing, or has a different name, remove just that line first. The flight calculation itself does not depend on it; the line merely prevents vanilla horse-jump behavior from competing with your glide system.

Also, remember that a normal horse model will remain visually in its regular running pose. For the next implementation stage, add a synced “gliding” flag to drive a custom pose/animation, wing flaps, sounds, particles, and potentially horse-specific flight statistics.
