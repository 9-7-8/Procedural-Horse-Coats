package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client &rarr; server: the gene picked on an open Equine Research Shelf's Copy
 * tab. The only packet the shelf still needs - storing and taking papers back
 * are ordinary slot clicks now, which vanilla syncs.
 *
 * <p>The server looks the shelf up from the player's open menu and checks the
 * gene against the papers actually on it, so a forged packet can only pick
 * something absent and get an empty result slot.
 */
public record ShelfActionPayload(Action action, String geneKey) implements CustomPacketPayload {

    public enum Action {
        /** Copy this gene next, if the shelf holds it and a book is in. */
        SELECT
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
