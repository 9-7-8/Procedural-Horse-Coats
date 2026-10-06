package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HorseGenetics.MOD_ID);

    // The Layer-1 HorseRecord itself is the attachment value, and it carries the
    // whole genome - genotype *and* epigenome. There is no separate coat
    // attachment: two attachments both holding the genotype was one fact stored
    // twice, and the epigenome living only on the entity is why the family tree
    // used to draw ancestors from an invented seed rather than their real coat.
    //
    // The default is a sentinel (blank name, blank epigenome) keyed to the holder
    // entity's own UUID; the spawn / breeding handlers replace it with a real
    // record immediately, and HorseRecord.hasGenome() is the "not yet" test.
    // Serialization is HorseRecordCodecs.MAP_CODEC (kept out of the domain type
    // on purpose).
    public static final Supplier<AttachmentType<HorseRecord>> HORSE_RECORD =
            ATTACHMENT_TYPES.register("horse_record", () -> AttachmentType
                    .<HorseRecord>builder(holder -> HorseRecord.unassigned(
                            holder instanceof Entity entity ? entity.getUUID() : new UUID(0L, 0L)))
                    .serialize(HorseRecordCodecs.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Care + social state. Roadmap wiki: 7.2 gated healing, 13 bond and herds.
    // Non-genetic; default is DEFAULT (bond 0, no herd). HorseCareHandler mutates it.
    // copyOnDeath so a re-summoned horse keeps its bond.
    public static final Supplier<AttachmentType<HorseCareAttachment>> HORSE_CARE =
            ATTACHMENT_TYPES.register("horse_care", () -> AttachmentType
                    .builder(() -> HorseCareAttachment.DEFAULT)
                    .serialize(HorseCareAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Hunger (common.care.Hunger): one invisible number, full by default. HorseCareHandler
    // drains it and spends it on healing; eating refills it. Not synced - nothing on the
    // client reads it - and NOT copyOnDeath: a re-summoned horse starts fed.
    public static final Supplier<AttachmentType<Double>> HUNGER =
            ATTACHMENT_TYPES.register("hunger", () -> AttachmentType
                    .<Double>builder(() -> com.example.horsegenetics.common.care.Hunger.FULL)
                    .serialize(com.mojang.serialization.Codec.DOUBLE.fieldOf("hunger"))
                    .build());

    // Social life: age, dam, natal band and the relationship ledger (common.herd).
    // Non-genetic. copyOnDeath like HORSE_CARE, so a re-summoned horse keeps its friends.
    public static final Supplier<AttachmentType<HorseSocialAttachment>> HORSE_SOCIAL =
            ATTACHMENT_TYPES.register("horse_social", () -> AttachmentType
                    .builder(() -> HorseSocialAttachment.DEFAULT)
                    .serialize(HorseSocialAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Fertility and gestation (common.repro): a mare's cycle phase, what she is
    // carrying, her last birth and the foals she nurses; a stallion's covers today.
    // Keyed to the entity id so an unset mare still has a cycle. NOT copyOnDeath:
    // a pregnancy dies with the mare. Not synced - the info screen gets a line of
    // words through the social summary, and no client needs an embryo's genome.
    public static final Supplier<AttachmentType<com.example.horsegenetics.common.repro.Reproduction>> HORSE_REPRO =
            ATTACHMENT_TYPES.register("horse_repro", () -> AttachmentType
                    .<com.example.horsegenetics.common.repro.Reproduction>builder(holder ->
                            com.example.horsegenetics.common.repro.Reproduction.fresh(
                                    holder instanceof Entity entity ? entity.getUUID() : new UUID(0L, 0L)))
                    .serialize(ReproCodecs.MAP_CODEC)
                    .build());

    // How far this horse has hauled a cart, in metres, for its whole life.
    // SYNCED, because the horse information screen shows it and the screen is
    // client-side; serialized, because a working life that resets on a chunk
    // unload is not a working life. Upstream's carts kept this as a *player*
    // statistic - cm pulled, on the F3 stats screen. It is kept there too, but
    // the number that matters to this mod belongs to the animal: a horse is
    // bought, sold, bred from and inherited, and "how far has this one worked"
    // is a fact about the horse, not about whoever happens to be holding it.
    // See HorseDraft.addHauled.
    public static final Supplier<AttachmentType<Double>> CART_METRES =
            ATTACHMENT_TYPES.register("cart_metres", () -> AttachmentType
                    .<Double>builder(() -> 0.0D)
                    .serialize(com.mojang.serialization.Codec.DOUBLE.fieldOf("cart_metres"))
                    .sync(net.minecraft.network.codec.ByteBufCodecs.DOUBLE)
                    .copyOnDeath()
                    .build());

    // Whether this mare is pregnant, and nothing else. SYNCED, so the client can
    // refuse a breeding food with the server instead of predicting a feed it then
    // takes back (known gap 226). HORSE_REPRO stays the truth; ReproHandler.set
    // keeps this in step. Serialized too, so it is right from the first tick
    // after a load.
    public static final Supplier<AttachmentType<Boolean>> PREGNANT =
            ATTACHMENT_TYPES.register("pregnant", () -> AttachmentType
                    .<Boolean>builder(() -> Boolean.FALSE)
                    .serialize(com.mojang.serialization.Codec.BOOL.fieldOf("pregnant"))
                    .sync(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    // Whether this PLAYER has been told that a jump is customised by
    // right-clicking it. A jump is one item with everything about it hidden
    // behind a screen, so there is nothing about the item to discover the
    // screen from - hence an action-bar line the first few times one is placed.
    //
    // It stops for good the moment they actually open one, which is the only
    // honest signal that the hint has done its job. Serialized, because a hint
    // that comes back after every relog is worse than no hint; NOT synced,
    // since the decision to show it is taken on the server, where the block is
    // placed.
    public static final Supplier<AttachmentType<Boolean>> JUMP_SCREEN_KNOWN =
            ATTACHMENT_TYPES.register("jump_screen_known", () -> AttachmentType
                    .<Boolean>builder(() -> Boolean.FALSE)
                    .serialize(com.mojang.serialization.Codec.BOOL.fieldOf("jump_screen_known"))
                    .build());

    // Timed-interaction stamps (last shear, last per-gene yield, ...). Gated
    // "once per Minecraft day". Replaces the static cooldown map that used to
    // live in GeneYieldHandler. copyOnDeath so a re-summoned horse keeps them.
    public static final Supplier<AttachmentType<HorseCooldownsAttachment>> HORSE_COOLDOWNS =
            ATTACHMENT_TYPES.register("horse_cooldowns", () -> AttachmentType
                    .builder(() -> HorseCooldownsAttachment.DEFAULT)
                    // Only a horse with a stamp writes one (#202), as LYCAN_SHIFT does: the empty default is
                    // what a missing attachment reads as, and every horse that was ever hurt saved one.
                    .serialize(HorseCooldownsAttachment.MAP_CODEC, c -> !c.lastByKey().isEmpty())
                    .copyOnDeath()
                    .build());

    // Breeding-carrot effects waiting on this horse until its next conception
    // (data/ArmedCarrotsAttachment). They do not expire. NOT copyOnDeath.
    public static final Supplier<AttachmentType<ArmedCarrotsAttachment>> ARMED_CARROTS =
            ATTACHMENT_TYPES.register("armed_carrots", () -> AttachmentType
                    .builder(() -> ArmedCarrotsAttachment.EMPTY)
                    .serialize(ArmedCarrotsAttachment.MAP_CODEC)
                    .build());

    // Whose horse this is while it is nobody's: the cowboy who bred it and has
    // not sold it yet (data/CowboyBrand). Read by CowboyHerdGoal to find the man
    // to follow and by TransferPaperHandler to refuse free taming. Synced,
    // because refusing an interaction only on the server makes the client
    // predict a mount it then has to take back - see CowboyBrand.STREAM_CODEC.
    // NOT copyOnDeath - a re-summoned horse is not still their.
    public static final Supplier<AttachmentType<CowboyBrand>> COWBOY_BRAND =
            ATTACHMENT_TYPES.register("cowboy_brand", () -> AttachmentType
                    .builder(() -> CowboyBrand.NONE)
                    // Only a branded horse writes one (#202); the care scan read it off nearly every horse.
                    .serialize(CowboyBrand.MAP_CODEC, CowboyBrand::isBranded)
                    .sync(CowboyBrand.STREAM_CODEC)
                    .build());

    // Who this horse has stopped wanting to kill, and until when
    // (data/PassificationAttachment). Per-player: a horse calmed by one player is
    // still willing to kill their friend. copyOnDeath, so a re-summoned horse
    // remembers the bargain. Not synced - nothing on the client reads it, and the
    // horse's behaviour is server-authoritative anyway.
    public static final Supplier<AttachmentType<PassificationAttachment>> PASSIFICATION =
            ATTACHMENT_TYPES.register("passification", () -> AttachmentType
                    .builder(() -> PassificationAttachment.DEFAULT)
                    // Only a horse with something remembered writes one (#202); targeting read it off every horse.
                    .serialize(PassificationAttachment.MAP_CODEC, a -> !a.until().isEmpty()
                            || !a.lastCalm().isEmpty() || !a.progress().isEmpty())
                    .copyOnDeath()
                    .build());

    // Who may ride this horse temporarily, and until when
    // (data/RidingPassAttachment) - a jockey pass fed to it, or /horsejockey.
    // The standing answers (owner, team, ally) are worked out live in
    // server/HorseRiding and are not stored anywhere; only the borrowed
    // permission needs a deadline written down. Not synced: the refusal happens
    // on the server, and a client that knew would only be able to predict it
    // wrong. copyOnDeath, so a resurrected horse does not strand the jockey who
    // was on it.
    public static final Supplier<AttachmentType<RidingPassAttachment>> RIDING_PASS =
            ATTACHMENT_TYPES.register("riding_pass", () -> AttachmentType
                    .builder(() -> RidingPassAttachment.DEFAULT)
                    .serialize(RidingPassAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Everything a horse wears that vanilla has no equipment slot for - see
    // HorseGear. Synced, because the Gear tab draws the worn stacks on the
    // client and there is no container open to carry them; the click that
    // changes one is still a server-checked packet (TackSlotPayload), so the
    // sync is a read-only view rather than an authority. copyOnDeath so a
    // re-summoned horse is still dressed - the same call HORSE_CARE makes,
    // and losing a full set of tack to a death you did not see would be worse
    // than losing a bond.
    // The command whistle's standing order (HorseOrderAttachment). SYNCED, so the
    // wheel greys and the screens show it without asking the server. NOT
    // copyOnDeath: an order dies with the horse. Default: no order, which is also
    // what every world from before the whistle loads as.
    public static final Supplier<AttachmentType<HorseOrderAttachment>> HORSE_ORDER =
            ATTACHMENT_TYPES.register("horse_order", () -> AttachmentType
                    .builder(() -> HorseOrderAttachment.NONE)
                    .serialize(HorseOrderAttachment.MAP_CODEC)
                    .sync(HorseOrderAttachment.STREAM_CODEC)
                    .build());

    public static final Supplier<AttachmentType<HorseGear>> HORSE_GEAR =
            ATTACHMENT_TYPES.register("horse_gear", () -> AttachmentType
                    .builder(() -> HorseGear.EMPTY)
                    .serialize(HorseGear.MAP_CODEC, g -> !g.isEmpty()) // only a dressed horse writes one (#202)
                    .sync(HorseGear.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    // The horse a shifted lycanthrope used to be, held on the ANIMAL rather than
    // on a horse - at night there is no horse to hold it. See data/LycanShift.
    // Not copyOnDeath: an animal that dies takes the horse inside it with it.
    public static final Supplier<AttachmentType<LycanShift>> LYCAN_SHIFT =
            ATTACHMENT_TYPES.register("lycan_shift", () -> AttachmentType
                    .builder(() -> LycanShift.NONE)
                    // Only a live shift is written (2026-10-02): NONE is what a missing attachment reads as anyway,
                    // and every mob that ever met LycanthropyHandler.getData had been saving one.
                    .serialize(LycanShift.MAP_CODEC, LycanShift::active)
                    .build());

    // Where a PLAYER came into the horse realm from - the Overworld hay portal
    // they stood in, remembered so that any of the realm's hundred exits can put
    // them back at the one they used. Serialized, because a way home that a
    // server restart loses strands somebody in a 16 000-block field; not synced,
    // because nothing on the client reads it and the decision is the server's.
    // Not copyOnDeath - it is on the player, and a player who dies in the realm
    // respawns in their own world with the same way back.
    public static final Supplier<AttachmentType<java.util.Optional<net.minecraft.core.GlobalPos>>> REALM_RETURN =
            ATTACHMENT_TYPES.register("realm_return", () -> AttachmentType
                    .<java.util.Optional<net.minecraft.core.GlobalPos>>builder(java.util.Optional::empty)
                    .serialize(net.minecraft.core.GlobalPos.CODEC.optionalFieldOf("realm_return"))
                    .build());

    // WHO TIED THE LEAD ON. Read at exactly one moment - when vanilla is about
    // to drop an Items.LEAD on the ground - so that the lead can go to them
    // instead. See server/HorseLeads and mixin/LeashPlacerMixin.
    //
    // It has to be stored rather than derived because a lead on a fence knot has
    // no player anywhere in it: the knot is the holder, and by the time the lead
    // is dropped vanilla has already cleared the leash data. Serialized, because
    // a horse can stand tied to a fence across a restart and the lead is still
    // somebody's. Not synced - nothing on the client reads it. Not copyOnDeath:
    // a horse that died was not wearing anybody's lead afterwards.
    //
    // It is deliberately NOT cleared when the lead comes off, because the drop
    // reads it after vanilla has untied the horse. A stale value is harmless -
    // it is only ever consulted while a lead is being dropped, and the next
    // player to tie one on overwrites it.
    public static final Supplier<AttachmentType<java.util.Optional<java.util.UUID>>> LEASH_PLACER =
            ATTACHMENT_TYPES.register("leash_placer", () -> AttachmentType
                    .<java.util.Optional<java.util.UUID>>builder(java.util.Optional::empty)
                    .serialize(net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("leash_placer"))
                    .build());

    private ModAttachments() {
    }
}
