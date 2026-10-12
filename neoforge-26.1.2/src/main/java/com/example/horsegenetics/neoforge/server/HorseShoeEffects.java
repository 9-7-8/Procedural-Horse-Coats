package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.item.HorseShoeItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * <b>What worn horse shoes do to the horse</b> (#247): a flat amount of speed
 * and jump from each shoe's own metal, and standing on powder snow.
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
 * <p><b>Not verified in a played game.</b> Written against the 26.1.2 sources;
 * the gametests {@code shoes_add_their_bonus} and {@code a_shod_horse_stands_on_powder_snow}
 * read the attributes and the collision rule off a real horse.
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

    private HorseShoeEffects() {
    }

    /** What the four hooves add right now, by the server's numbers. Both sides. */
    public static HorseShoes.Bonus bonusOf(AbstractHorse horse) {
        return HorseShoes.bonus(ServerConfig.shoeRates(),
                HorseShoeItem.tierOf(HOOVES[0].on(horse)), HorseShoeItem.tierOf(HOOVES[1].on(horse)),
                HorseShoeItem.tierOf(HOOVES[2].on(horse)), HorseShoeItem.tierOf(HOOVES[3].on(horse)));
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

    /** Re-read the hooves and set the two modifiers to match. Server side. */
    public static void refresh(AbstractHorse horse) {
        if (horse.level().isClientSide()) {
            return;
        }
        HorseShoes.Bonus bonus = bonusOf(horse);
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
}
