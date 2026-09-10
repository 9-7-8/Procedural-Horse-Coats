package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.entity.ModEntities;
import com.example.horsegenetics.neoforge.village.ModVillagerProfessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * <b>The two people the Getting Started tab tells you to look for, drawn rather
 * than described.</b> A horseman is an ordinary villager wearing a profession
 * you will not otherwise have seen, and a cowboy is this mod's own mob - both
 * are much easier to recognise from a picture than from a sentence about hats.
 *
 * <h2>They are entities, not textures</h2>
 * Built once, kept, and never added to a level: the same trick
 * {@link HorsePortrait} uses. That means they cost nothing until the tab is
 * opened, they follow any resource pack the player has on, and a change to the
 * horseman's profession shows up here without anybody remembering to re-export a
 * picture.
 *
 * <p>Everything here fails soft. A portrait that cannot be built draws nothing
 * and the prose beside it still reads, which is the right trade for decoration
 * on a help page.
 */
public final class TutorialPortraits {

    private static Villager horseman;
    private static Entity cowboy;

    private TutorialPortraits() {
    }

    /** Dropped on disconnect - these belong to that world's client level. */
    public static void clear() {
        horseman = null;
        cowboy = null;
    }

    public static void drawHorseman(GuiGraphicsExtractor g, int x, int y, int size, int mouseX, int mouseY) {
        draw(g, horseman(), x, y, size, mouseX, mouseY);
    }

    public static void drawCowboy(GuiGraphicsExtractor g, int x, int y, int size, int mouseX, int mouseY) {
        draw(g, cowboy(), x, y, size, mouseX, mouseY);
    }

    private static Villager horseman() {
        if (horseman != null) {
            return horseman;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        try {
            Villager villager = EntityType.VILLAGER.create(mc.level, EntitySpawnReason.LOAD);
            if (villager == null) {
                return null;
            }
            // The profession is the whole point of the picture - a villager with
            // no job looks like every other villager and teaches nothing.
            villager.setVillagerData(villager.getVillagerData()
                    .withProfession(ModVillagerProfessions.HORSEMAN)
                    .withLevel(1));
            horseman = villager;
            return villager;
        } catch (RuntimeException uncreatable) {
            return null;
        }
    }

    private static Entity cowboy() {
        if (cowboy != null) {
            return cowboy;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        try {
            cowboy = ModEntities.COWBOY.get().create(mc.level, EntitySpawnReason.LOAD);
            return cowboy;
        } catch (RuntimeException uncreatable) {
            return null;
        }
    }

    /**
     * One portrait, looking at the pointer - the same swing the family tree and
     * the roster rows use, so every drawn mob in this mod behaves the same way.
     */
    private static void draw(GuiGraphicsExtractor g, Entity entity,
                             int x, int y, int size, int mouseX, int mouseY) {
        if (entity == null) {
            return;
        }
        int cx = x + size / 2;
        int cy = y + size / 2;
        try {
            EntityRenderer<? super Entity, ?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            EntityRenderState state = renderer.createRenderState(entity, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            float xAngle = (float) Math.atan((cx - mouseX) / 40.0F);
            float yAngle = (float) Math.atan((cy - mouseY) / 40.0F);
            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = 180.0F + xAngle * 42.0F;
                living.yRot = xAngle * 42.0F;
                living.xRot = -yAngle * 22.0F;
                living.walkAnimationSpeed = 0.0F;
            }
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 22.0F * ((float) Math.PI / 180.0F));
            rotation.mul(xRotation);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            // Scaled off the entity's own height, so a villager and a cowboy of
            // different builds both fill the same box.
            g.entity(state, size / Math.max(0.5F, state.boundingBoxHeight * 1.15F),
                    translation, rotation, xRotation, x, y, x + size, y + size);
        } catch (RuntimeException notDrawable) {
            // A help page is not worth a crash.
        }
    }
}
