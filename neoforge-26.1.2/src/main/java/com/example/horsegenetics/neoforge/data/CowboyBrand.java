package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * <b>Whose horse this is, when it is nobody's yet.</b> A horse in the cowboy's
 * herd is untamed - it has no vanilla owner - and yet it is emphatically not
 * free for the taking. The brand is what says so: the id of the cowboy who bred
 * it, carried until a player redeems its transfer papers.
 *
 * <p>Two things read it. {@code CowboyHerdGoal} uses it to find the man to
 * follow, and {@code TransferPaperHandler} uses it to refuse the ordinary
 * mount-until-it-gives-in taming that would otherwise let a player walk into
 * the barn and help themselves to the entire herd for free. Redemption clears
 * it, which is the moment the horse stops being their.
 *
 * <p>Not on {@link HorseCareAttachment} on purpose. That record is the horse's
 * own social state - its bond, the wild herd it was born into - and its
 * {@code herd} field means "the lead <i>horse</i>'s UUID", which
 * {@code WildHerdGoal} will happily go looking for. Putting a villager's id in
 * that field would be a lie the wild-herd code then has to be taught to
 * tolerate.
 *
 * <p><b>Not</b> {@code copyOnDeath}: a cowboy horse that dies and is somehow
 * re-summoned is not still their.
 */
public record CowboyBrand(Optional<UUID> cowboy) {

    public static final CowboyBrand NONE = new CowboyBrand(Optional.empty());

    public static final MapCodec<CowboyBrand> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("cowboy").forGetter(CowboyBrand::cowboy)
    ).apply(i, CowboyBrand::new));

    /**
     * The brand is <b>synced to clients</b>, unlike the rest of the horse
     * attachments, and for one specific reason: right-clicking a tameable horse
     * is client-predicted. If only the server knew the horse was branded, the
     * player would see themselves climb on and then get snapped back off.
     * {@code TransferPaperHandler} cancels the interaction on both sides, and
     * this is what lets the client side answer the question.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, CowboyBrand> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CowboyBrand::cowboy,
                    CowboyBrand::new);

    public CowboyBrand {
        cowboy = cowboy == null ? Optional.empty() : cowboy;
    }

    public static CowboyBrand of(UUID cowboyId) {
        return new CowboyBrand(Optional.of(cowboyId));
    }

    public boolean isBranded() {
        return cowboy.isPresent();
    }
}
