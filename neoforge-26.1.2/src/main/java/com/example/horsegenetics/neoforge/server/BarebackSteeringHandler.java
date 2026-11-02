package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.gear.Reins;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>A horse that trusts you enough to be steered without a saddle.</b>
 *
 * <p>At the top bond tier ({@link HorseCareAttachment#behaviourTier()} 3, where
 * the horse already follows you around) its owner can climb on bare and ride
 * it properly. Below that tier, and for anyone who is not the owner, a bareback
 * rider is a passenger and the horse goes where it likes - which is vanilla's
 * behaviour and stays the default.
 *
 * <p><b>And a horse in a bridle steers for anyone</b> (#73). On a horse the
 * vanilla saddle item is the bridle and is worn in the Headstall, a slot vanilla
 * cannot see, so it reaches vanilla's steering the same way: a lent saddle. Who
 * gets one is {@link Reins}, in {@code common/}; this class only reads the horse
 * and does what it says. It is also where a bridle that lands in a horse's saddle
 * slot is moved to the head ({@link #moveBridle}).
 *
 * <h2>How, given vanilla will not be asked</h2>
 * Vanilla decides who may steer inside
 * {@code AbstractHorse.getControllingPassenger()}, which hands control to a
 * player passenger <b>only if {@code isSaddled()}</b>. There is no event on
 * that call. (The module does have mixins - {@code CartHorseMixin} overrides
 * that very method for a hitched horse - but one lent saddle keeps every part
 * of vanilla's mounted path, client prediction included, without a second
 * override racing the cart's.)
 *
 * <p>The first version of this drove the horse from outside on the entity tick
 * - read the rider's {@code zza}/{@code xxa}, point the animal, push it. It
 * worked and it felt wrong: server-driven, so a beat behind the input, with no
 * jump and none of vanilla's acceleration curve.
 *
 * <p>So it does the blunt thing instead and <b>puts a saddle on</b>. A real one,
 * in the real slot, so every part of vanilla's mounted path runs exactly as it
 * does for a saddled horse: client prediction, jumping, the lot. The saddle
 * carries {@link ModDataComponents#PHANTOM_SADDLE}, which is what stops it
 * being a lie:
 *
 * <ul>
 *   <li><b>It is never drawn.</b> {@code client/GeneticHorseRenderer} feeds the
 *       saddle layer an empty stack when the component is present. The mod owns
 *       the horse renderer, so this costs nothing.</li>
 *   <li><b>It is taken back off</b> the moment the rider is gone, the bond
 *       drops, the rider turns out not to be the owner, or the horse bolts -
 *       checked every tick, so the window in which a phantom saddle exists
 *       without an eligible rider on it is one tick.</li>
 *   <li><b>It cannot be kept.</b> A player <i>can</i> open a horse's inventory
 *       while riding it, so {@link #reclaim} sweeps the rider's inventory for
 *       phantom saddles and destroys them. Without that, "ride your best horse
 *       bareback" would be a saddle duplicator.</li>
 * </ul>
 *
 * <p><b>A horse the player has saddled themselves is never touched</b> - the
 * component is the only thing that marks a saddle as ours, and it is set on
 * exactly the stacks this class creates.
 *
 * <h2>And it stands down while the horse is bolting</h2>
 * {@link HorseEscapeGoal} takes a horse's saddle off at low health precisely so
 * the rider stops steering and the animal's own navigation can carry them out.
 * This class lends a saddle <b>every tick</b>, so without the
 * {@link HorseEscapeGoal#bolting} check in {@link #eligibleRider} the reins
 * would be back in the rider's hands on the next tick - and only for horses at
 * the top bond tier, which is to say only for the horses a player cares most
 * about. A bug of that shape would have looked like the escape behaviour working
 * fine in testing and failing on the one animal anybody rides.
 */
@EventBusSubscriber
public final class BarebackSteeringHandler {

    private BarebackSteeringHandler() {
    }

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide()) {
            return;
        }
        moveBridle(horse);
        boolean phantom = isPhantom(horse.getItemBySlot(EquipmentSlot.SADDLE));
        Player rider = horse.isAlive() && horse.isVehicle()
                && horse.getFirstPassenger() instanceof Player first ? first : null;
        Reins.Who who = rider == null ? Reins.Who.NOBODY : steers(horse, rider);
        if (who == Reins.Who.NOBODY) {
            rider = null;
        }

        if (rider != null && !horse.isSaddled()) {
            ItemStack saddle = new ItemStack(Items.SADDLE);
            saddle.set(ModDataComponents.PHANTOM_SADDLE.get(), true);
            horse.setItemSlot(EquipmentSlot.SADDLE, saddle);
            // The tick this fires on is the moment a horse first takes direction
            // bare, which is the whole of what the task is asking for. A bridled
            // ride is not bare (owner, 2026-10-02).
            if (who == Reins.Who.BAREBACK) {
                HorseProgress.complete(rider, ProgressTask.STEER_BAREBACK);
            }
            return;
        }
        if (phantom && rider == null) {
            horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
        }
        if (phantom && rider != null) {
            reclaim(rider);
        }
    }

    /**
     * Who steers, by {@link Reins}: anyone, when the horse wears a bridle; the
     * owner, bare, at the top bond tier; else nobody.
     *
     * <p>{@code getFirstPassenger} rather than {@code getControllingPassenger},
     * because on the tick the saddle goes on there is by definition no
     * controlling passenger yet.
     *
     * <p><b>And not while it is running for its life.</b> HorseEscapeGoal takes
     * the bridle off precisely to stop the rider steering, and this class
     * re-lends a saddle every tick - so without the bolting check the
     * best-bonded horses in the game would be exactly the ones the escape
     * behaviour never worked for. It is asked last because it is the most
     * expensive question, and only of a horse that would otherwise steer.
     */
    private static Reins.Who steers(AbstractHorse horse, Player rider) {
        LivingEntity owner = horse.getOwner();
        boolean isOwner = owner != null && owner.getUUID().equals(rider.getUUID());
        int tier = horse.getData(ModAttachments.HORSE_CARE.get()).behaviourTier();
        Reins.Who who = Reins.steers(true, horse.isTamed(), HorseTackSlot.bridled(horse), isOwner, tier, false);
        if (who != Reins.Who.NOBODY && HorseEscapeGoal.bolting(horse)) {
            return Reins.Who.NOBODY;
        }
        return who;
    }

    /**
     * <b>A bridle never stays in a horse's saddle slot</b> (#73). On a horse the
     * vanilla saddle is the bridle, and it lives in the Headstall: this is the
     * one place that puts it there, whatever put it in the saddle slot - a world
     * saved before the split (the migration hard rule 10 asks for), a dispenser,
     * vanilla's horse inventory, another mod. A right-click never gets here:
     * {@code TackEquipHandler} puts it straight on the head.
     *
     * <p>No flag: it runs on every tick, and costs one slot read, because the
     * rule is true forever - a bridle in a horse's saddle slot is always wrong.
     * With the Headstall already taken the second bridle is dropped at the
     * horse's feet, never deleted.
     */
    private static void moveBridle(AbstractHorse horse) {
        if (!HorseTackSlot.wearsBridles(horse)) {
            return;
        }
        ItemStack inSaddle = horse.getItemBySlot(EquipmentSlot.SADDLE);
        if (!HorseTackSlot.isBridle(inSaddle)) {
            return;
        }
        ItemStack bridle = inSaddle.copy();
        horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
        if (HorseTackSlot.HEADSTALL.on(horse).isEmpty()) {
            HorseTackSlot.HEADSTALL.set(horse, bridle);
        } else if (horse.level() instanceof net.minecraft.server.level.ServerLevel level) {
            horse.spawnAtLocation(level, bridle);
        }
    }

    /**
     * Destroy any phantom saddle that has found its way into a player's hands.
     *
     * <p>The only route in is opening the horse's inventory while mounted and
     * dragging it out, which is a real thing a player can do and would otherwise
     * mint a free saddle every time. Destroying rather than returning it to the
     * horse keeps this from fighting the player over a slot: the tick after,
     * they are still eligible, and the horse gets a fresh one.
     */
    private static void reclaim(Player rider) {
        for (int i = 0; i < rider.getInventory().getContainerSize(); i++) {
            ItemStack stack = rider.getInventory().getItem(i);
            if (isPhantom(stack)) {
                rider.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (isPhantom(rider.containerMenu.getCarried())) {
            rider.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    /**
     * <b>Is this horse being ridden bare?</b> Either it has no saddle at all, or
     * the only saddle on it is one this class put there.
     *
     * <p>The second half is the part worth having: at the top bond tier a
     * bareback rider is given a real saddle so vanilla's mounted path works, so
     * {@code isSaddled()} says <i>true</i> about a horse with nothing on it.
     * Anything asking "is this bareback" and trusting {@code isSaddled()} alone
     * gets the wrong answer for exactly the horses this mod is about.
     */
    static boolean bareback(AbstractHorse horse) {
        // A bridle-only ride is not bareback (owner, 2026-10-02): the phantom
        // saddle under a bridled rider is lent for the bridle, not the bond.
        return (!horse.isSaddled() || isPhantom(horse.getItemBySlot(EquipmentSlot.SADDLE)))
                && !HorseTackSlot.bridled(horse);
    }

    private static boolean isPhantom(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModDataComponents.PHANTOM_SADDLE.get());
    }
}
