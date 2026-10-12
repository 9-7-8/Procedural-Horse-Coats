package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.item.HorseShoeItem;
import com.example.horsegenetics.neoforge.item.ShoeEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * <b>What worn horse shoes do to the horse</b> (#247, #248): a flat amount of
 * speed and jump from each shoe's own metal, standing on powder snow, and the
 * two enchantments - grip on ice, and speed on a path or a road.
 *
 * <p>The rule is {@code common/gear/HorseShoes} - this reads the four hoof
 * slots, asks it, and holds the answer up as one shoe-owned modifier on each
 * attribute. It follows {@link HorsePackHandler#refresh} rather than
 * {@code GeneAbilityHandler}'s per-tick reconcile: shoes change only when a
 * hoof slot is written, so that is when this runs
 * ({@code HorseTackSlot.set}), and once more when the horse joins a level,
 * because the modifiers are <b>transient</b> and so are never in the save. A
 * changed {@code shoes.*} number therefore reaches a horse when it next loads
 * or is next shod, not the tick the file is saved.
 *
 * <p><b>The one thing that ticks</b> is the road. Roadster's speed depends on
 * the block under the horse, which no event reports, so a horse that some
 * roadster would speed up is held in {@link #ON_ROAD} and has the ground under
 * it looked at every {@link #ROAD_CHECK_TICKS}; the same {@link #refresh} runs
 * when the answer changes. A horse without that enchantment is never in the
 * map, and with the map empty the tick is one {@code isEmpty()} per entity.
 * The road speed is part of the same {@code shoes/speed} modifier as the
 * metal's, never a second one.
 *
 * <p>Grip is not an attribute: {@link #friction} is asked from the movement
 * code itself, by {@code mixin/ShoeGripMixin}, on both sides.
 *
 * <p><b>Not verified in a played game.</b> Written against the 26.1.2 sources;
 * the gametests {@code shoes_add_their_bonus}, {@code a_shod_horse_stands_on_powder_snow},
 * {@code shoe_enchantments_fold_into_the_total} and {@code grip_shoes_slide_less_on_ice}
 * read the attributes, the collision rule and a real slide off a real horse.
 */
@EventBusSubscriber
public final class HorseShoeEffects {

    /** In {@code PartAnchor.hooves()} order, the order {@link HorseShoes#bonus} takes them in. */
    private static final HorseTackSlot[] HOOVES = {
            HorseTackSlot.SHOE_FRONT_LEFT, HorseTackSlot.SHOE_FRONT_RIGHT,
            HorseTackSlot.SHOE_HIND_LEFT, HorseTackSlot.SHOE_HIND_RIGHT};

    public static final Identifier SPEED_ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "shoes/speed");
    public static final Identifier JUMP_ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "shoes/jump");

    /**
     * How often a roadster-shod horse's ground is looked at. A galloping horse
     * covers under three blocks in this long, so the speed arrives within a
     * stride of the path and leaves within a stride of the grass.
     */
    public static final int ROAD_CHECK_TICKS = 4;

    /**
     * The horses whose ground is watched, and what it last was. Server thread
     * only ({@link #refresh} returns on a client). Weak, so an unloaded horse
     * drops out by itself; a loaded one comes back through its join.
     */
    private static final Map<AbstractHorse, Boolean> ON_ROAD = new WeakHashMap<>();

    private HorseShoeEffects() {
    }

    private static HorseShoes.Shoe[] shoesOf(AbstractHorse horse) {
        HorseShoes.Shoe[] shoes = new HorseShoes.Shoe[HOOVES.length];
        for (int i = 0; i < HOOVES.length; i++) {
            shoes[i] = ShoeEnchantments.shoeOf(HOOVES[i].on(horse));
        }
        return shoes;
    }

    /** What the four hooves add right now, by the server's numbers, on or off a road. Both sides. */
    public static HorseShoes.Bonus bonusOf(AbstractHorse horse, boolean onRoad) {
        HorseShoes.Shoe[] shoes = shoesOf(horse);
        return HorseShoes.bonus(ServerConfig.shoeRates(), onRoad, shoes[0], shoes[1], shoes[2], shoes[3]);
    }

    /**
     * Whether this horse stands on powder snow: one shoe on any hoof. Both
     * sides - the rider's client moves a ridden horse, and the gear attachment
     * it reads is synced.
     */
    public static boolean shod(AbstractHorse horse) {
        int shoes = 0;
        for (HorseTackSlot hoof : HOOVES) {
            if (HorseShoeItem.tierOf(hoof.on(horse)) != null) {
                shoes++;
            }
        }
        return HorseShoes.shod(ServerConfig.shoeRates(), shoes);
    }

    /**
     * What a block of {@code blockFriction} is to this horse: its grip shoes
     * take their share of the slide. Both sides, for the reason {@link #shod}
     * is. The caller has already seen that the block is slicker than ordinary
     * ground, so this costs nothing for a horse on grass.
     */
    public static float friction(AbstractHorse horse, float blockFriction) {
        return HorseShoes.friction(blockFriction, bonusOf(horse, false).grip());
    }

    /** Whether the block the horse stands on is a path or a road ({@code horsegenetics:shoe_roads}). */
    public static boolean onRoad(AbstractHorse horse) {
        return horse.level().getBlockState(horse.getOnPos()).is(ShoeEnchantments.ROADS);
    }

    /** Re-read the hooves and the ground, and set the two modifiers to match. Server side. */
    public static void refresh(AbstractHorse horse) {
        if (horse.level().isClientSide()) {
            return;
        }
        HorseShoes.Shoe[] shoes = shoesOf(horse);
        HorseShoes.Rates rates = ServerConfig.shoeRates();
        boolean onRoad = false;
        if (HorseShoes.watchesRoad(rates, shoes[0], shoes[1], shoes[2], shoes[3])) {
            onRoad = onRoad(horse);
            ON_ROAD.put(horse, onRoad);
        } else {
            ON_ROAD.remove(horse);
        }
        HorseShoes.Bonus bonus = HorseShoes.bonus(rates, onRoad, shoes[0], shoes[1], shoes[2], shoes[3]);
        hold(horse.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, bonus.speed());
        hold(horse.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, bonus.jump());
    }

    private static void hold(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier on = attribute.getModifier(id);
        if (amount == 0.0) {
            if (on != null) {
                attribute.removeModifier(id);
            }
        } else if (on == null || on.amount() != amount) {
            // Only when it differs: a modifier re-made with the same number still
            // marks the attribute dirty (see GeneAbilityHandler.applyAttribute).
            attribute.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse) {
            for (HorseTackSlot hoof : HOOVES) {
                if (!hoof.on(horse).isEmpty()) {
                    refresh(horse);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (ON_ROAD.isEmpty() || !(event.getEntity() instanceof AbstractHorse horse)
                || horse.level().isClientSide() || (horse.tickCount + horse.getId()) % ROAD_CHECK_TICKS != 0) {
            return;
        }
        Boolean was = ON_ROAD.get(horse);
        if (was == null) {
            return;
        }
        BlockState under = horse.level().getBlockState(horse.getOnPos());
        if (under.isAir()) {
            // Mid-jump, or stepping off a ledge: no ground to judge by, so it
            // keeps what it had rather than dropping its speed over every fence.
            return;
        }
        if (under.is(ShoeEnchantments.ROADS) != was) {
            refresh(horse);
        }
    }
}
