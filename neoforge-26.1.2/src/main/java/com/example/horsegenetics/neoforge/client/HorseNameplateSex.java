package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * <b>&#9792; or &#9794; after a horse's name, so you can tell its sex across a
 * paddock.</b>
 *
 * <h2>The symbol is not part of the name</h2>
 * This appends to the <i>rendered</i> nameplate through
 * {@link RenderNameTagEvent.CanRender}, and never touches
 * {@code horse.setCustomName}. That matters more than it looks: the stored name
 * is what the horse record keeps, what a transfer paper prints, what the browser
 * sorts on and what a player typed into the rename box. Baking a glyph into it
 * would put that glyph in all four, and it would then be inherited, saved and
 * eventually renamed <i>around</i>. A display aid belongs at display time.
 *
 * <p>It is also why the switch is {@link ClientConfig} rather than
 * {@link com.example.horsegenetics.neoforge.ServerConfig}: nothing about the
 * world changes, so two players on one server may disagree about it freely.
 *
 * <h2>Where the sex comes from</h2>
 * Sex is a gene, so the client already has it for any horse it can see - the
 * horse record and the coat both arrive when the client starts tracking the
 * entity. The record is asked first and the coat is the fallback; a horse that
 * has neither yet simply gets its plain name for a frame or two.
 */
@EventBusSubscriber(Dist.CLIENT)
public final class HorseNameplateSex {

    /**
     * Mars and Venus. Both are outside ASCII, so they come from Minecraft's
     * unicode fallback font rather than the default sheet - see
     * {@code wiki/verification.html}, because whether they render as glyphs or
     * as boxes has not been looked at in game.
     */
    private static final String FEMALE = " ♀";
    private static final String MALE = " ♂";

    private HorseNameplateSex() {
    }

    @SubscribeEvent
    static void onNameTag(RenderNameTagEvent.CanRender event) {
        if (!ClientConfig.nameplateSexSymbol()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Horse horse)) {
            return;
        }
        Component content = event.getContent();
        if (content == null) {
            return;
        }
        Sex sex = sexOf(horse.getId());
        if (sex == null) {
            return;
        }
        boolean mare = sex == Sex.FEMALE;
        // Coloured as well as shaped. The glyphs are small at range and the two
        // are not that different in silhouette; the colour is what actually does
        // the work of "tell them apart across a paddock".
        event.setContent(content.copy().append(Component.literal(mare ? FEMALE : MALE)
                .withStyle(mare ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)));
    }

    /** The record if the client has it, else the coat, else {@code null}. */
    private static Sex sexOf(int entityId) {
        HorseRecord record = ClientHorseRecordCache.get(entityId);
        if (record != null) {
            return record.sex();
        }
        CoatData coat = ClientCoatCache.get(entityId);
        return coat == null ? null : coat.genome().genotype().sex();
    }
}
