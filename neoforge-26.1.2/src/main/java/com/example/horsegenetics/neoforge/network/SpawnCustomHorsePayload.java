package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.GenomeCodeCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: spawn a horse with a hand-picked genome, from the custom
 * horse spawn egg's editor screen ({@code client/CustomHorseSpawnScreen}).
 *
 * <ul>
 *   <li>{@code genotypeCode} - a full genotype code string
 *       ({@code Genotype.of(pairs).toCode()}); the server re-parses and
 *       normalises it, and rejects a bad one with a chat message.</li>
 *   <li>{@code epigenomeCode} - the <b>epigenome the editor was previewing</b>
 *       ({@code Epigenome.toCode()}). It travels with the genotype because the
 *       screen shows a live 3D horse and the horse that arrives has to be the
 *       one you were looking at; rolling a fresh one server-side would make the
 *       preview a suggestion. Empty means "roll one", the old behaviour.</li>
 *   <li>{@code baby} - spawn as a foal.</li>
 *   <li>{@code female} - {@code Sex.FEMALE} if true, else {@code Sex.MALE}.</li>
 *   <li>{@code breed} - a {@code BreedLineage} token to stamp on the spawned
 *       horse (the editor's "Breed:" preset), or {@code ""} for Feral Mixed. It is
 *       only a label + stat bands; the genotype above is authoritative.</li>
 *   <li>{@code asEgg} - <b>write the horse into an item instead of into the
 *       world</b>. Everything above is identical either way; this only decides
 *       whether the server spawns the animal now or hands the player a
 *       {@code preset_horse_spawn_egg} carrying it, to keep, to spawn later, or
 *       to give to somebody who is not in creative.</li>
 * </ul>
 *
 * The server applies the genome as a <b>founder record</b> before the entity
 * joins the level, so {@code HorseGeneticsEventHandler} doesn't roll a random
 * genotype over the top. It is <b>creative-only and re-checked server-side</b>:
 * the screen opens on the client, but this payload spawns an arbitrary entity
 * with an arbitrary genome, so a client-side gate would be no gate at all.
 */
public record SpawnCustomHorsePayload(String genotypeCode, String epigenomeCode,
                                      boolean baby, boolean female, String breed,
                                      boolean asEgg)
        implements CustomPacketPayload {

    public static final Type<SpawnCustomHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "spawn_custom_horse"));

    public static final StreamCodec<ByteBuf, SpawnCustomHorsePayload> STREAM_CODEC = StreamCodec.composite(
            // These caps are hard failures, not truncations: NeoForge throws when
            // an encoded string is over, so an undersized cap breaks spawning
            // outright rather than degrading. The epigenome one was 4096 while a
            // full code was already about 4500 characters, which means this
            // packet could not have worked - see EpigenomeSizeTest, which now
            // guards the margin from the common side.
            GenomeCodeCodecs.GENOTYPE_CODE, SpawnCustomHorsePayload::genotypeCode,
            GenomeCodeCodecs.EPIGENOME_CODE, SpawnCustomHorsePayload::epigenomeCode,
            ByteBufCodecs.BOOL, SpawnCustomHorsePayload::baby,
            ByteBufCodecs.BOOL, SpawnCustomHorsePayload::female,
            ByteBufCodecs.stringUtf8(64), SpawnCustomHorsePayload::breed,
            ByteBufCodecs.BOOL, SpawnCustomHorsePayload::asEgg,
            SpawnCustomHorsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
