package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client &rarr; server: what the player did to an open Equine Research Shelf -
 * picked a gene to copy, or asked for a filed paper back.
 *
 * <p>One payload for both because they are the same shape and the same
 * re-check: the server looks the shelf up from the player's open menu and does
 * nothing at all if the gene is not on it. A forged packet can select something
 * absent (and get an empty result slot) or withdraw something absent (and get
 * nothing); neither reaches the world.
 */
public record ShelfActionPayload(Action action, String geneKey) implements CustomPacketPayload {

    public enum Action {
        /** Copy this gene next, if the shelf holds it and a book is in. */
        SELECT,
        /** Take this gene's filed paper back out of the shelf. */
        WITHDRAW
    }

    public static final Type<ShelfActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "shelf_action"));

    public static final StreamCodec<ByteBuf, ShelfActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), ShelfActionPayload::action,
            ByteBufCodecs.stringUtf8(256), ShelfActionPayload::geneKey,
            ShelfActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
