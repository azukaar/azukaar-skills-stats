package com.azukaar.ass.trees.medic;

import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Custom effects for the Medic skill tree
 */
public class CustomEffects {

    /**
     * Check if this handler supports the given effect type
     */
    public static boolean handles(String effectType) {
        return switch (effectType) {
            case "azukaarskillsstats:reactive_heal",
                 "azukaarskillsstats:undying_resolve",
                 "azukaarskillsstats:radiance_on_heal",
                 "azukaarskillsstats:radiance_on_regen",
                 "azukaarskillsstats:infused_self_heal",
                 "azukaarskillsstats:infused_touch_heal",
                 "azukaarskillsstats:infused_aoe_heal",
                 "azukaarskillsstats:alchemy",
                 "azukaarskillsstats:brew_regen",
                 "azukaarskillsstats:brew_resistance",
                 "azukaarskillsstats:brew_strength",
                 "azukaarskillsstats:brew_o_plenty",
                 "azukaarskillsstats:cocktail",
                 "azukaarskillsstats:overdose",
                 "azukaarskillsstats:radiance_potion",
                 "azukaarskillsstats:hardy",
                 "azukaarskillsstats:heal_expert",
                 "azukaarskillsstats:blinding_light",
                 "azukaarskillsstats:reactive_cleanse",
                 "azukaarskillsstats:venomous_rebound",
                 "azukaarskillsstats:radiance_amplifier" -> true;
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
