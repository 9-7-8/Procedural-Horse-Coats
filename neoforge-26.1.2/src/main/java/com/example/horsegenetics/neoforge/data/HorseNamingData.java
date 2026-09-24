package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.name.NamingPolicy;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Each player's own foal-naming policy.</b> A server-global
 * {@link SavedData} keyed by player {@link UUID}, beside
 * {@link HorseProgressData} and for one of the same reasons and one of its own.
 *
 * <h2>Why it is not simply read off the client</h2>
 * The player <i>chooses</i> it in their client config, but the choice cannot be
 * applied there: a foal is named on the server, at a due date that can arrive
 * while its owner is offline and their client is not running. So the client
 * sends the two values on login ({@code NamingPolicyPayload}) and the server
 * keeps them - a suggestion filed, not a setting obeyed live. That also makes
 * the answer per-<i>player</i> rather than per-world, which a server config
 * could not do: on a shared server everyone names their own horses their own
 * way, and nobody's setting reaches anybody else's herd.
 *
 * <h2>An unknown player is not an error</h2>
 * {@link #policyFor} answers {@link NamingPolicy#DEFAULT} for a player who has
 * never sent one, for a wild birth with no owner at all, and for a player whose
 * client is older than this packet. Naming a foal must never be a path that can
 * fail, so there is no "not set" state for a caller to handle.
 */
public final class HorseNamingData extends SavedData {

    private record PlayerPolicy(UUID player, String half, String source) {
        static final Codec<PlayerPolicy> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerPolicy::player),
                Codec.STRING.fieldOf("half").forGetter(PlayerPolicy::half),
                Codec.STRING.fieldOf("source").forGetter(PlayerPolicy::source)
        ).apply(i, PlayerPolicy::new));
    }

    public static final Codec<HorseNamingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(PlayerPolicy.CODEC).fieldOf("players").forGetter(HorseNamingData::snapshot)
    ).apply(i, HorseNamingData::new));

    public static final SavedDataType<HorseNamingData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "naming"),
            HorseNamingData::new,
            CODEC);

    private final Map<UUID, NamingPolicy> byPlayer = new LinkedHashMap<>();

    private HorseNamingData() {
    }

    private HorseNamingData(List<PlayerPolicy> players) {
        for (PlayerPolicy p : players) {
            byPlayer.put(p.player(), read(p.half(), p.source()));
        }
    }

    public static HorseNamingData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<PlayerPolicy> snapshot() {
        List<PlayerPolicy> out = new ArrayList<>();
        byPlayer.forEach((id, policy) ->
                out.add(new PlayerPolicy(id, policy.half().name(), policy.source().name())));
        return out;
    }

    /**
     * What {@code player} asked for, or {@link NamingPolicy#DEFAULT} for a
     * player who never asked - including {@code null}, which is a wild birth.
     */
    public NamingPolicy policyFor(@Nullable UUID player) {
        if (player == null) {
            return NamingPolicy.DEFAULT;
        }
        return byPlayer.getOrDefault(player, NamingPolicy.DEFAULT);
    }

    /** File this player's choice. Cheap and silent when it hasn't changed. */
    public void set(UUID player, NamingPolicy policy) {
        if (policy.equals(byPlayer.get(player))) {
            return;
        }
        byPlayer.put(player, policy);
        setDirty();
    }

    /** Enum names off disk, with anything unreadable left at the default. */
    private static NamingPolicy read(String half, String source) {
        NamingPolicy.InheritedHalf h = NamingPolicy.DEFAULT.half();
        NamingPolicy.ParentSource s = NamingPolicy.DEFAULT.source();
        try {
            h = NamingPolicy.InheritedHalf.valueOf(half.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            // written by a build that knew a half this one does not
        }
        try {
            s = NamingPolicy.ParentSource.valueOf(source.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            // likewise
        }
        return new NamingPolicy(h, s);
    }
}
