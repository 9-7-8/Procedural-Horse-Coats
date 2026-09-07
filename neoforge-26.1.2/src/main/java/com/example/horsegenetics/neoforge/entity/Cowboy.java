package com.example.horsegenetics.neoforge.entity;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.TransferDeed;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.CowboyHandler;
import com.example.horsegenetics.neoforge.server.HorsePrices;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The <b>cowboy</b> - a horse breeder who lives at a barn on the edge of a
 * plains village, rides everywhere, and sells his stock as
 * {@linkplain TransferDeed transfer papers} rather than as animals.
 *
 * <h2>What he is, and is not</h2>
 * He is <b>not a villager profession</b>. A profession is acquired from a job
 * site by a villager whose brain runs vanilla's schedule, and that schedule
 * would dismount him every morning to go stand at his workstation. Everything
 * that makes this character himself - always mounted, home is a barn and not a
 * bed, stock is a live herd and not a trade table - is a fight with that brain.
 * So he is his own {@link AbstractVillager}: he keeps the merchant screen, the
 * villager silhouette and the trading goals, and none of the schedule. The
 * <b>horseman</b> ({@code ModVillagerProfessions}) is the one that is a real
 * profession, because a shopkeeper standing at a workstation is exactly what
 * vanilla's brain is for.
 *
 * <h2>Selling a horse he does not hand over</h2>
 * Each offer is one emerald price against one signed transfer paper, named for
 * one horse in his herd. Buying it does not move the animal: the buyer has to
 * walk out to the horse the paper names and redeem it there
 * ({@code TransferPaperHandler}). That is what keeps a paper worth trading
 * onward - it is a claim someone else can still collect.
 *
 * <p>A horse he has written a paper for leaves the offer list for good
 * ({@link #hasSold}), whether or not the paper was ever redeemed - the animal
 * is spoken for even if the buyer never walks out to collect it.
 *
 * <h2>Restocking</h2>
 * What he does <b>not</b> do is run out. {@code CowboyHandler} breeds him back
 * up to {@link #stockTarget} - the number he had for sale the day he set up -
 * and now and then retires a horse nobody is looking at and breeds a
 * replacement. So a player who comes back a week later finds a different string
 * of the same size, rather than the same six horses he was not interested in
 * the first time.
 *
 * <h2>State</h2>
 * <ul>
 *   <li>{@code home} - the barn he was generated in, and the place he and his
 *       herd return to at night.</li>
 *   <li>{@code herd} - the horses he bred, in the order they were made. The
 *       first is his own mount and is never for sale.</li>
 *   <li>{@code sold} - herd members he has already issued a paper for.</li>
 *   <li>{@code stockTarget} - how many horses he had for sale on the day he
 *       set up, and the number {@code CowboyHandler} breeds him back up to. A
 *       fixed cast no longer, but a fixed <i>size</i> of cast.</li>
 *   <li>{@code preferredBreed} - the breed he is known for. Half his string is
 *       it and the rest is whatever else the country round him produces, which
 *       is how a real breeder's yard looks.</li>
 *   <li>{@code founded} - has {@code CowboyHandler} given him his name, his
 *       mount and his herd yet? He is baked into {@code cowboy_barn.nbt} bare;
 *       everything about him is built on his first server tick.</li>
 * </ul>
 */
public class Cowboy extends AbstractVillager {

    /** Fewest horses in the herd, on top of the one he rides. */
    public static final int MIN_HERD = 4;
    /** Most horses in the herd, on top of the one he rides. */
    public static final int MAX_HERD = 10;

    /**
     * How far into the saddle he is drawn, in blocks.
     *
     * <p>A villager has no sitting pose - vanilla's own villagers stand bolt
     * upright in boats and minecarts - so a cowboy placed properly on a horse's
     * back is a man standing on it. Sinking him until the horse's barrel takes
     * his legs reads as riding from any normal distance. It is a bandaid and
     * knowingly so: the real fix is a villager mesh with a seated leg pose, and
     * this is what it costs until there is one.
     *
     * <p>Applied to the <b>attachment point</b> and not to the renderer, so his
     * hitbox goes down with the picture and you click him where you see him.
     */
    private static final double SADDLE_SINK = 0.5;

    private @Nullable BlockPos home;
    private final List<UUID> herd = new ArrayList<>();
    private final Set<UUID> sold = new LinkedHashSet<>();
    private boolean founded;
    private int stockTarget;
    private @Nullable String preferredBreed;
    private int restockCooldown;

    public Cowboy(EntityType<? extends Cowboy> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 0.6));
        // Only ever used on foot - the day his horse is killed. While he is
        // mounted, CowboyMountGoal on the horse does all the moving and these
        // never get the chance to path anywhere.
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    // --- identity -------------------------------------------------------

    /** His full name - the one written on his horses as their breeder. */
    public String cowboyName() {
        Component name = getCustomName();
        return name == null ? "" : name.getString();
    }

    public boolean isFounded() {
        return founded;
    }

    public void markFounded() {
        this.founded = true;
    }

    /**
     * How many horses he keeps for sale. Set once, at founding, from the roll
     * that made his first string - so one cowboy is a four-horse outfit and the
     * next is a ten-horse one, for good.
     */
    public int stockTarget() {
        return stockTarget;
    }

    public void setStockTarget(int target) {
        this.stockTarget = Math.max(0, target);
    }

    /** The breed he is known for, if he has been founded. */
    public Optional<String> preferredBreed() {
        return Optional.ofNullable(preferredBreed);
    }

    public void setPreferredBreed(String breedId) {
        this.preferredBreed = breedId;
    }

    // --- home and herd --------------------------------------------------

    /** The barn, if he has been founded. */
    public Optional<BlockPos> home() {
        return Optional.ofNullable(home);
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
    }

    /** Every horse he bred, mount first. Ids only - some may be unloaded or dead. */
    public List<UUID> herdIds() {
        return List.copyOf(herd);
    }

    public void addToHerd(UUID horseId) {
        if (!herd.contains(horseId)) {
            herd.add(horseId);
        }
    }

    /**
     * Forget a horse entirely - it was retired in a restock, or it has been
     * tamed and is somebody else's now. <b>Never slot 0</b>: that id is the
     * herd's lead, every other member carries it, and dropping it would orphan
     * the string even if the mount is dead.
     */
    public void removeFromHerd(UUID horseId) {
        if (!herd.isEmpty() && herd.get(0).equals(horseId)) {
            return;
        }
        herd.remove(horseId);
        sold.remove(horseId);
    }

    /**
     * Count down to the next look over his string; true on the tick it comes
     * due. Deliberately <b>not saved</b> - a reload simply brings the next look
     * forward, which is the harmless direction to be wrong in.
     */
    public boolean tickRestockClock() {
        if (restockCooldown > 0) {
            restockCooldown--;
            return false;
        }
        return true;
    }

    public void setRestockCooldown(int ticks) {
        this.restockCooldown = Math.max(0, ticks);
    }

    /**
     * The horse he rides - <b>herd slot 0</b>, and therefore the herd's lead:
     * every other member of his string carries this id as its
     * {@code HorseCareAttachment.herd}. Never offered for sale; empty once it
     * has been killed.
     */
    public Optional<UUID> mountId() {
        return herd.isEmpty() ? Optional.empty() : Optional.of(herd.get(0));
    }

    /** The lead horse as a live entity, or {@code null} if dead or unloaded. */
    public @Nullable AbstractHorse mount(ServerLevel level) {
        return mountId().map(id -> liveHorse(level, id)).orElse(null);
    }

    /** Has a paper already been written for this horse? */
    public boolean hasSold(UUID horseId) {
        return sold.contains(horseId);
    }

    public void markSold(UUID horseId) {
        sold.add(horseId);
        this.offers = null; // rebuilt on the next look
    }

    // --- trading --------------------------------------------------------

    /**
     * One offer per herd horse that is still his to sell: alive, still untamed,
     * not his mount, and not already papered. Rebuilt from scratch every time
     * the list is asked for, because the herd is a live thing - a horse can be
     * killed by a wolf between one player looking and the next.
     */
    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers merchantOffers = this.getOffers();
        merchantOffers.clear();
        List<UUID> ids = herdIds();
        for (int i = 1; i < ids.size(); i++) { // 0 is his own mount
            UUID id = ids.get(i);
            if (hasSold(id)) {
                continue;
            }
            if (!(level.getEntity(id) instanceof Horse horse) || !horse.isAlive() || horse.isTamed()) {
                continue;
            }
            HorseRecord record = HorseRecords.of(horse);
            if (!record.hasName()) {
                continue; // not founded yet - it will be offered next time
            }
            merchantOffers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, HorsePrices.emeraldsFor(record)),
                    papersFor(record),
                    1,   // one paper per horse, ever
                    0,   // he is not a levelling villager
                    0.0F));
        }
    }

    /** The item one of his horses is sold as: a signed paper, named for the horse. */
    private ItemStack papersFor(HorseRecord record) {
        ItemStack stack = new ItemStack(ModItems.SIGNED_TRANSFER_PAPER.get());
        stack.set(ModDataComponents.HORSE_DEED.get(), TransferDeed.forHorse(record, cowboyName()));
        return stack;
    }

    /**
     * Take the horse off the market the moment its paper is bought - not when
     * the paper is redeemed. The buyer may never redeem it, or may sell it on;
     * either way the cowboy considers that horse spoken for.
     */
    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        TransferDeed deed = offer.getResult().get(ModDataComponents.HORSE_DEED.get());
        if (deed != null) {
            markSold(deed.horseId());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.VILLAGER_SPAWN_EGG) || !isAlive() || isTrading() || isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND) {
            player.awardStat(Stats.TALKED_TO_VILLAGER);
        }
        if (!level().isClientSide()) {
            this.offers = null; // the herd may have changed since the last look
            if (getOffers().isEmpty()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.horsegenetics.cowboy.sold_out"), true);
                }
                return InteractionResult.CONSUME;
            }
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // He is not a levelling villager: no XP, no restock, no tiers.
    }

    // --- the rest -------------------------------------------------------

    /** He is generated with a village and stays with it, however far the player wanders. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /**
     * He never gets off by choice. Vanilla dismounts a passenger whose vehicle
     * it thinks is unsuitable, and a player can shake a rider loose; this makes
     * the horse the only thing that can end the ride, by dying.
     */
    @Override
    public void stopRiding() {
        if (getVehicle() instanceof AbstractHorse horse && horse.isAlive() && !isRemoved()) {
            return;
        }
        super.stopRiding();
    }

    /** Nothing shoves a mounted man off his horse. */
    @Override
    public boolean isPushable() {
        return !isPassenger();
    }

    /**
     * Sit him <i>into</i> the horse rather than on top of it - see
     * {@link #SADDLE_SINK} for why that is the best a villager mesh can do.
     *
     * <p>The vehicle attachment is subtracted from the saddle position by
     * {@code Entity.positionRider}, so adding to its Y moves him down.
     */
    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        Vec3 point = super.getVehicleAttachmentPoint(vehicle);
        return vehicle instanceof AbstractHorse ? point.add(0.0, SADDLE_SINK, 0.0) : point;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            CowboyHandler.onCowboyDied(this, level);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Founded", founded);
        output.putInt("StockTarget", stockTarget);
        output.putString("PreferredBreed", preferredBreed == null ? "" : preferredBreed);
        output.storeNullable("Home", BlockPos.CODEC, home);
        output.store("Herd", UUIDUtil.CODEC.listOf(), List.copyOf(herd));
        output.store("Sold", UUIDUtil.CODEC.listOf(), List.copyOf(sold));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.founded = input.getBooleanOr("Founded", false);
        this.stockTarget = input.getIntOr("StockTarget", 0);
        String breed = input.getStringOr("PreferredBreed", "");
        this.preferredBreed = breed.isEmpty() ? null : breed;
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
        this.herd.clear();
        this.herd.addAll(input.read("Herd", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        this.sold.clear();
        this.sold.addAll(input.read("Sold", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        setAge(Math.max(0, getAge()));
    }

    /** Convenience for the goals: the living horse behind an id, if it is loaded. */
    public static @Nullable Horse liveHorse(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Horse horse && horse.isAlive() ? horse : null;
    }

    /** Every living, loaded member of the herd. */
    public List<Horse> liveHerd(ServerLevel level) {
        List<Horse> out = new ArrayList<>();
        for (UUID id : herd) {
            Horse horse = liveHorse(level, id);
            if (horse != null) {
                out.add(horse);
            }
        }
        return out;
    }
}
