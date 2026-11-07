package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.gear.Saddles;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * <b>The mod's own saddle</b> (#75) - worn in the vanilla saddle slot, beside
 * the bridle in the Headstall (#73). The bridle steers; this carries stats.
 *
 * <p>One class and an item per material: the seven vanilla makes a chestplate
 * of ({@code ModItems.HORSE_SADDLES}, from {@link Saddles.Tier}) and one for
 * every material another mod makes a chestplate of ({@code compat/ModdedSaddles}).
 * The material is the item, not a component, so the fittings' colour is a
 * constant of the item: the icon ({@code client/MetalTintSource}) and the
 * fittings on the horse ({@code client/TackClientExtensions}) both ask this.
 *
 * <p>What it does once worn is {@code server/SaddleEffects}: its
 * {@link #tier}'s share of the horse's speed and jump, while a player rides.
 *
 * <p><b>Horses only.</b> Its {@link Equippable} names {@code minecraft:horse}
 * alone: donkeys, mules and llamas keep vanilla's saddle (owner, 2026-10-02), and
 * the equipment asset draws only the horse's layer.
 */
public class HorseSaddleItem extends Item {

    /** The one equipment asset every tier wears: the seat and its own fittings, no bridle. */
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "saddle"));

    private final int rgb;
    private final Saddles.Tier tier;

    /**
     * @param rgb  the fittings' colour, {@code 0xRRGGBB}
     * @param tier the rung its bonus is read from: itself for a shipped material,
     *             {@link Saddles#tierOf} for one another mod brought
     */
    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public HorseSaddleItem(int rgb, Saddles.Tier tier, Properties properties) {
        super(properties);
        this.rgb = rgb & 0x00FFFFFF;
        this.tier = tier;
    }

    /**
     * The properties every saddle is registered with: one to a stack, and worn
     * in the saddle slot by a right-click, as vanilla's own saddle is
     * ({@code Equippable.saddle()} in the 26.1.2 sources), but on a horse only
     * and drawn from {@link #ASSET}.
     */
    public static Properties properties(Properties properties) {
        return properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.SADDLE)
                        .setEquipSound(SoundEvents.HORSE_SADDLE)
                        .setAsset(ASSET)
                        .setAllowedEntities(HolderSet.direct(EntityType.HORSE.builtInRegistryHolder()))
                        .setEquipOnInteract(true)
                        .setCanBeSheared(true)
                        .setShearingSound(SoundEvents.SADDLE_UNEQUIP)
                        .build());
    }

    /** The rung this saddle's bonus is read from. */
    public Saddles.Tier tier() {
        return tier;
    }

    /** The rung of the saddle in {@code stack}, or {@code null} when it is not one of ours. */
    public static Saddles.@Nullable Tier tierOf(ItemStack stack) {
        return stack.getItem() instanceof HorseSaddleItem saddle ? saddle.tier : null;
    }

    /** The fittings' colour as opaque ARGB, or {@code 0} when {@code stack} is not one of ours. */
    public static int tintOf(ItemStack stack) {
        return stack.getItem() instanceof HorseSaddleItem saddle ? 0xFF000000 | saddle.rgb : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Worn in a horse's Saddle slot. The bridle steers; the saddle carries.")
                .withStyle(ChatFormatting.GRAY));
        // The server's numbers, not the defaults: its config is synced to the client.
        Saddles.Rates rates = ServerConfig.saddleRates();
        if (!rates.enabled()) {
            adder.accept(Component.literal("Saddles change nothing on this server.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Saddles.Bonus bonus = rates.of(tier);
        if (!bonus.isNone()) {
            adder.accept(Component.literal(String.format(java.util.Locale.ROOT,
                            "While ridden: speed +%.0f%%, jump +%.0f%%.", bonus.speed() * 100, bonus.jump() * 100))
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
