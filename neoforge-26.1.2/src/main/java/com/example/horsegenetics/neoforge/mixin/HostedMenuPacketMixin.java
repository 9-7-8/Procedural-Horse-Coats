package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HostedPacks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * <b>"Is this player still at my block?" - answered for the horse.</b> A chest
 * on a horse that opens as its own block ({@link HostedPacks}) is standing at
 * the bottom of the world while its screen is open, so every menu's own answer
 * is no and the screen would shut the tick it opened.
 *
 * <p>{@code AbstractContainerMenu.stillValid} is abstract, written once per
 * menu class by every mod, so it cannot be changed where it is defined; what
 * can be wrapped is where vanilla <i>asks</i>. This is the half in
 * {@code ServerGamePacketListenerImpl}; {@code HostedMenuPlayerMixin} is the other. For a player with no hosted chest
 * open - which is everybody, nearly always - {@link HostedPacks#stillValid} is
 * one empty-map check and the menu's own answer stands.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HostedMenuPacketMixin {

    @WrapOperation(
            method = {"handleContainerClick", "handlePlaceRecipe", "handleContainerButtonClick"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean horsegenetics$hostedChestIsHeldByTheHorse(AbstractContainerMenu menu, Player player,
                                                             Operation<Boolean> original) {
        Boolean hosted = HostedPacks.stillValid(player);
        return hosted != null ? hosted : original.call(menu, player);
    }
}
