package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.common.trait.HorseUnits;
import com.example.horsegenetics.neoforge.ServerConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * <b>A horse shoe</b> - one to a hoof, worn in one of the four
 * {@code HorseTackSlot.SHOE_*} slots, each its own stack in its own metal.
 *
 * <p>One class and an item per metal, the five that ship
 * ({@code ModItems}, from {@link HorseShoes.Metal}) and one for every metal
 * another mod brings ({@code compat/ModdedShoes}). The metal is the item, not a
 * component, so the colour is a constant of the item: the inventory icon
 * ({@code client/MetalTintSource}) and the shoe drawn on the hoof
 * ({@code GeneticHorseRenderer}) both ask this for it, and cannot disagree.
 *
 * <p>What it does once worn is {@code server/HorseShoeEffects}: its
 * {@link #tier}'s flat speed and jump, and powder snow. The tooltip states the
 * server's own numbers. The enchantments are not built (#248).
 */
public class HorseShoeItem extends Item {

    /**
     * The jump strength the tooltip's "about" is worked out at: the middle of
     * what a horse resolves to. Jump height is not linear in strength, so one
     * shoe is worth a little more on a strong jumper and less on a weak one.
     */
    private static final double TYPICAL_JUMP = 0.7;

    private final int rgb;
    private final HorseShoes.Metal tier;

    /**
     * @param rgb  the metal's colour, {@code 0xRRGGBB}
     * @param tier the rung its bonus is read from: itself for a shipped metal,
     *             {@link HorseShoes#tierOf} for one another mod brought
     */
    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public HorseShoeItem(int rgb, HorseShoes.Metal tier, Properties properties) {
        super(properties);
        this.rgb = rgb & 0x00FFFFFF;
        this.tier = tier;
    }

    /** The rung this shoe's speed and jump are read from. */
    public HorseShoes.Metal tier() {
        return tier;
    }

    /** The rung of the shoe in {@code stack}, or {@code null} when it is not a shoe: a bare hoof. */
    public static HorseShoes.@Nullable Metal tierOf(ItemStack stack) {
        return stack.getItem() instanceof HorseShoeItem shoe ? shoe.tier : null;
    }

    /** The colour this shoe is drawn in, as opaque ARGB - never {@code 0}, which means a bare hoof. */
    public int tint() {
        return HorseShoes.tint(rgb);
    }

    /** The tint of the shoe in {@code stack}, or {@code 0} when it is not a shoe. */
    public static int tintOf(ItemStack stack) {
        return stack.getItem() instanceof HorseShoeItem shoe ? shoe.tint() : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Worn on a hoof, in a horse's Gear tab.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("One shoe to a foot: a horse takes four.")
                .withStyle(ChatFormatting.GRAY));
        // The server's numbers, not the defaults: its config is synced to the client.
        HorseShoes.Rates rates = ServerConfig.shoeRates();
        if (!rates.enabled()) {
            adder.accept(Component.literal("Shoes change nothing on this server.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        HorseShoes.Bonus one = rates.perShoe(tier);
        if (!one.isNone()) {
            adder.accept(Component.literal(String.format(java.util.Locale.ROOT,
                            "Each shoe: speed +%.2f m/s, jump about +%.2f m.",
                            HorseUnits.metresPerSecond(one.speed()),
                            HorseUnits.jumpMetres(TYPICAL_JUMP + one.jump()) - HorseUnits.jumpMetres(TYPICAL_JUMP)))
                    .withStyle(ChatFormatting.BLUE));
        }
        adder.accept(Component.literal("One shoe keeps a horse on top of powder snow.")
                .withStyle(ChatFormatting.BLUE));
    }
}
