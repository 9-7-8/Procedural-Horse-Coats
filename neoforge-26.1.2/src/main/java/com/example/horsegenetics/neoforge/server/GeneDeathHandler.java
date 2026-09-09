package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.item.PresetHorseSpawnEggItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The translator for the two <b>death</b> verbs - {@code on_death}, which
 * changes the world where the horse died, and {@code item_drop}, which changes
 * what is lying on it afterwards.
 *
 * <p>They are separate verbs on separate loci and they are handled here in one
 * class, because they share exactly one thing: the moment. Everything else
 * about them is different, including which event they can possibly run on -
 * {@link LivingDropsEvent} can rewrite the drop list and cannot place a block
 * usefully, and {@link LivingDeathEvent} is the other way round.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources.
 *
 * <p>Nothing here fires in the read-only gallery dimension, for the same reason
 * the glow light block and the ground spread do not: a debug pen is for looking
 * at horses, and a horse that craters one is a bug report about the debug pen.
 */
@EventBusSubscriber
public final class GeneDeathHandler {

    private GeneDeathHandler() {}

    /** How hard a volatile horse goes off. A creeper is 3.0F; this is deliberately the same. */
    public static final float EXPLOSION_RADIUS = 3.0F;

    // ------------------------------------------------------------------
    // on_death - what happens to the ground
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onHorseDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)
                || level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            return;
        }
        for (HorseAbilities.Active active : abilitiesOf(horse)) {
            if (active.ability() instanceof GeneAbility.OnDeath death) {
                apply(death.effect(), horse, level);
            }
        }
    }

    /**
     * The three things a death may do to the world.
     *
     * <p>The two fluids are placed only into a block that is genuinely free -
     * air, or something a fluid would wash away anyway. A horse that died inside
     * somebody's floor should not eat the floor, and the alternative reading
     * ("replace whatever is there") turns a novelty locus into a griefing tool
     * that a player cannot see coming, since the carrier looks like any other
     * horse.
     */
    private static void apply(String effect, Horse horse, ServerLevel level) {
        BlockPos at = horse.blockPosition();
        switch (effect) {
            case "lava" -> place(level, at, Blocks.LAVA.defaultBlockState());
            case "water" -> place(level, at, Blocks.WATER.defaultBlockState());
            case "explode" -> level.explode(horse, horse.getX(), horse.getY(), horse.getZ(),
                    EXPLOSION_RADIUS, Level.ExplosionInteraction.MOB);
            default -> { }
        }
    }

    private static void place(ServerLevel level, BlockPos at, net.minecraft.world.level.block.state.BlockState state) {
        if (!level.isLoaded(at)) {
            return;
        }
        var existing = level.getBlockState(at);
        if (existing.isAir() || existing.canBeReplaced()
                || existing.getFluidState().getType() != Fluids.EMPTY) {
            level.setBlockAndUpdate(at, state);
        }
    }

    // ------------------------------------------------------------------
    // item_drop - what is left behind
    // ------------------------------------------------------------------

    /**
     * Rewrite the horse's drops.
     *
     * <p>{@code meat} is <b>added</b> to whatever the horse was going to drop;
     * every other value <b>replaces</b> it. That asymmetry is the genes' and not
     * this method's: a horse that leaves diamonds instead of its leather is a
     * different animal, and a meaty one is the same animal with more on it. It
     * is why the two live on two loci and why a diamond horse can also be a meat
     * horse.
     */
    @SubscribeEvent
    static void onHorseDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)
                || level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            return;
        }
        List<ItemStack> added = new ArrayList<>();
        boolean replace = false;
        for (HorseAbilities.Active active : abilitiesOf(horse)) {
            if (!(active.ability() instanceof GeneAbility.ItemDrop drop)) {
                continue;
            }
            ItemStack stack = itemFor(drop, horse, level);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            added.add(stack);
            replace |= !"meat".equals(drop.drop());
        }
        if (added.isEmpty()) {
            return;
        }
        if (replace) {
            event.getDrops().clear();
        }
        for (ItemStack stack : added) {
            event.getDrops().add(new ItemEntity(level, horse.getX(), horse.getY(), horse.getZ(), stack));
        }
    }

    /** One drop, or {@code null} for "this one adds nothing". */
    private static ItemStack itemFor(GeneAbility.ItemDrop drop, Horse horse, ServerLevel level) {
        RandomSource random = level.getRandom();
        int count = drop.min() + (drop.max() > drop.min()
                ? random.nextInt(drop.max() - drop.min() + 1) : 0);
        return switch (drop.drop()) {
            case "diamonds" -> new ItemStack(Items.DIAMOND, count);
            // Horse meat is not a vanilla item. Beef is the closest honest
            // stand-in and needs no new item, no model and no recipe; if the
            // mod ever grows its own, this line is the only thing that moves.
            case "meat" -> new ItemStack(Items.BEEF, count);
            case "spawn_egg" -> cloneEgg(horse);
            case "enchanted_sword" -> enchantedSword(level, random);
            default -> null;   // "vanilla" adds nothing and replaces nothing
        };
    }

    /**
     * A preset egg holding this horse's <b>own</b> genotype and epigenome, so
     * what comes back out is a clone rather than a horse with the same alleles -
     * the same coat, the same particle colours, the same percentages on every
     * copy.
     *
     * <p>It reuses the editor's {@code PresetHorseSpawnEggItem} rather than
     * introducing an item of its own. The two are the same thing arrived at from
     * different directions, and one of them already has a model, a tooltip and a
     * spawner behind it.
     */
    private static ItemStack cloneEgg(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return ItemStack.EMPTY;
        }
        StoredGenome stored = new StoredGenome(
                record.geneticCode(), record.epigenomeCode(),
                horse.getUUID(), record.displayName(), record.breed().orElse(""));
        return PresetHorseSpawnEggItem.of(stored, false);
    }

    /**
     * An iron sword carrying <b>one</b> enchantment, rolled at the moment of
     * death rather than fixed by the gene - so two horses of identical genotype
     * do not leave identical swords, and this is the one drop on the locus that
     * is a gamble rather than a certainty.
     *
     * <p>The enchantment is drawn from whatever the registry holds that a sword
     * will accept, which means a data pack's enchantments are in the pool for
     * free. A world with none at all yields a plain sword rather than nothing.
     */
    private static ItemStack enchantedSword(ServerLevel level, RandomSource random) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        List<Holder<Enchantment>> candidates = new ArrayList<>();
        level.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(registry ->
                registry.listElements().forEach(holder -> {
                    if (holder.value().canEnchant(sword)) {
                        candidates.add(holder);
                    }
                }));
        if (candidates.isEmpty()) {
            return sword;
        }
        Holder<Enchantment> chosen = candidates.get(random.nextInt(candidates.size()));
        int max = Math.max(1, chosen.value().getMaxLevel());
        sword.enchant(chosen, 1 + random.nextInt(max));
        return sword;
    }

    // ------------------------------------------------------------------

    /**
     * The horse's abilities, resolved from its record. Not cached: a horse dies
     * once, and the cache in {@link GeneAbilityHandler} is keyed on a UUID that
     * is about to stop existing.
     */
    private static List<HorseAbilities.Active> abilitiesOf(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return List.of();
        }
        try {
            return HorseAbilities.activeFor(Genotype.parse(record.geneticCode()),
                    Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
