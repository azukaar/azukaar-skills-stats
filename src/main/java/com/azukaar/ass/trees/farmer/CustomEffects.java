package com.azukaar.ass.trees.farmer;

import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Custom effects for the Farmer skill tree
 */
public class CustomEffects {

    /**
     * Check if this handler supports the given effect type
     */
    public static boolean handles(String effectType) {
        return switch (effectType) {
            case "azukaarskillsstats:natures_call",
                 "azukaarskillsstats:harvests_blessing",
                 "azukaarskillsstats:natures_domain",
                 "azukaarskillsstats:harvest_area",
                 "azukaarskillsstats:foragers_appetite",
                 "azukaarskillsstats:planting_area",
                 "azukaarskillsstats:seed_master",
                 "azukaarskillsstats:breeding_mastery" -> true;
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
