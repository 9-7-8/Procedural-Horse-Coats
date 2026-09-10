package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * <b>Which creatures a radius effect is about.</b> The one place that turns a
 * {@code MOB_GROUPS} vocabulary word from {@code common/} into a test against a
 * live entity.
 *
 * <h2>Tags, not a list of vanilla mobs</h2>
 * This is the whole reason the class exists rather than a switch inlined in each
 * caller. Five genes name a group - intimidating, leader of the pack, meowing,
 * cleansing light and the temper loci - and a hardcoded list of vanilla mobs
 * would make <b>every modded creature invisible to all five at once</b>, silently:
 * the aura simply never fires on anything from another mod and nothing errors.
 *
 * <p>So each group resolves through the broadest thing the game already knows:
 * an {@link EntityTypeTags entity type tag} where one exists ({@code undead} is
 * the important one), a {@link MobCategory} where the question is really "is
 * this a monster", and an interface where the game models it that way
 * ({@link Enemy}). A mod that tags its own undead gets cleansing light for free,
 * which is the correct outcome and is not achievable any other way.
 *
 * <p>{@code non_horse} is the odd one out and earns its place: several genes are
 * about everything <i>except</i> the horse's own kind, and expressing that as a
 * negation would need a combinator the {@code when} vocabulary does not have.
 */
public final class MobGroups {

    private MobGroups() {
    }

    /**
     * Does {@code candidate} belong to the named group?
     *
     * <p>An unknown group name is {@code false} rather than an error - the same
     * rule the rest of the translator follows for a vocabulary word this build
     * has never heard of.
     */
    public static boolean matches(String group, LivingEntity candidate) {
        return switch (group) {
            case "players" -> candidate instanceof Player;
            case "hostile" -> isHostile(candidate);
            case "passive", "animals" -> candidate instanceof Animal && !isHostile(candidate);
            case "undead" -> candidate.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
            case "non_horse" -> !(candidate instanceof AbstractHorse);
            case "all" -> true;
            default -> false;
        };
    }

    /**
     * Monster or not. {@link Enemy} is the interface vanilla marks its hostiles
     * with and most mods follow it; {@link MobCategory#MONSTER} catches the ones
     * that do not. Either is enough - a creature only has to look hostile by one
     * of the two measures for a horse to be entitled to dislike it.
     */
    public static boolean isHostile(LivingEntity candidate) {
        return candidate instanceof Enemy
                || candidate.getType().getCategory() == MobCategory.MONSTER;
    }

    /**
     * A single mob id, or a group. {@code mob} wins when it is set, which is
     * what lets one allele of a per-mob locus name exactly one creature while
     * the group vocabulary stays available to everything else.
     */
    public static boolean matches(String group, String mob, LivingEntity candidate) {
        if (mob != null && !mob.isEmpty()) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()).toString().equals(mob);
        }
        return matches(group, candidate);
    }
}
