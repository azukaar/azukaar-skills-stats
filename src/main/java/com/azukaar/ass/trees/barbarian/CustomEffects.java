package com.azukaar.ass.trees.barbarian;

import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Custom effects for the Barbarian skill tree
 */
public class CustomEffects {

    /**
     * Check if this handler supports the given effect type
     */
    public static boolean handles(String effectType) {
        return switch (effectType) {
            case "azukaarskillsstats:iron_stomach",
                 "azukaarskillsstats:axe_focus",
                 "azukaarskillsstats:sword_focus",
                 "azukaarskillsstats:axe_power",
                 "azukaarskillsstats:sword_power",
                 "azukaarskillsstats:crushing_blow",
                 "azukaarskillsstats:parry",
                 "azukaarskillsstats:quick_eating",
                 "azukaarskillsstats:efficient_digestion",
                 "azukaarskillsstats:meat_lover",
                 "azukaarskillsstats:carnivorous",
                 "azukaarskillsstats:gorge",
                 "azukaarskillsstats:feeding_edge",
                 "azukaarskillsstats:gluttonous_weapon",
                 "azukaarskillsstats:floor_slam",
                 "azukaarskillsstats:giant_sweep" -> true;
            default -> false;
        };
    }

    /**
     * Apply a custom effect (called when skill level changes)
     */
    public static void applyCustomEffect(Player player, String effectType, SkillEffect.Effect effect, int skillLevel) {
        // These effects are event-based (handled in TreeEvents), no setup needed
    }

    /**
     * Remove a custom effect
     */
    public static void removeCustomEffect(Player player, String effectType, SkillEffect.Effect effect) {
        // These effects are event-based, no cleanup needed
    }
}
