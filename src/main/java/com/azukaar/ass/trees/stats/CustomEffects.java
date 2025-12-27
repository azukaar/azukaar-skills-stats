package com.azukaar.ass.trees.stats;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Custom effects for the Stats skill tree
 */
public class CustomEffects {

    /**
     * Check if this handler supports the given effect type
     */
    public static boolean handles(String effectType) {
        return switch (effectType) {
            case "azukaarskillsstats:hunger_efficiency",
                 "azukaarskillsstats:mining_speed",
                 "azukaarskillsstats:xp_bonus" -> true;
            default -> false;
        };
    }

    /**
     * Apply a custom effect to a player
     */
    public static void applyCustomEffect(Player player, String effectType, SkillEffect.Effect effect, int skillLevel) {
        switch (effectType) {
            case "azukaarskillsstats:hunger_efficiency":
                applyHungerEfficiency(player, effect, skillLevel);
                break;
            case "azukaarskillsstats:mining_speed":
                applyMiningSpeed(player, effect, skillLevel);
                break;
            case "azukaarskillsstats:xp_bonus":
                applyXpBonus(player, effect, skillLevel);
                break;
            default:
                AzukaarSkillsStats.LOGGER.warn("Unknown custom effect type in stats tree: {}", effectType);
        }
    }

    /**
     * Remove a custom effect from a player
     */
    public static void removeCustomEffect(Player player, String effectType, SkillEffect.Effect effect) {
        switch (effectType) {
            case "azukaarskillsstats:hunger_efficiency":
                removeHungerEfficiency(player, effect);
                break;
            case "azukaarskillsstats:mining_speed":
                removeMiningSpeed(player, effect);
                break;
            case "azukaarskillsstats:xp_bonus":
                removeXpBonus(player, effect);
                break;
        }
    }

    // === Hunger Efficiency ===

    private static void applyHungerEfficiency(Player player, SkillEffect.Effect effect, int skillLevel) {
        // This effect doesn't need active application - it's checked during hunger events
        // The effect is handled in SkillEffectEvents.onHungerDepletion()
    }

    private static void removeHungerEfficiency(Player player, SkillEffect.Effect effect) {
        // No cleanup needed for hunger efficiency
    }

    // === Mining Speed ===

    private static void applyMiningSpeed(Player player, SkillEffect.Effect effect, int skillLevel) {
        // This effect doesn't need active application - it's checked during mining events
        // The effect is handled in SkillEffectEvents.onBlockBreakSpeed()
    }

    private static void removeMiningSpeed(Player player, SkillEffect.Effect effect) {
        // No cleanup needed for mining speed
    }

    // === XP Bonus ===

    private static void applyXpBonus(Player player, SkillEffect.Effect effect, int skillLevel) {
        // This effect doesn't need active application - it's checked during XP gain
        // The effect is handled in PlayerData.addExperienceServerSide()
    }

    private static void removeXpBonus(Player player, SkillEffect.Effect effect) {
        // No cleanup needed for XP bonus
    }

    /**
     * Get the Minecraft XP bonus multiplier for a player based on their Knowledge skill
     */
    public static double getMinecraftXpBonusMultiplier(Player player) {
        int knowledgeLevel = PlayerData.getSkillLevel(player, "azukaarskillsstats:knowledge");
        if (knowledgeLevel <= 0) return 1.0;

        // 5% per level
        return 1.0 + (knowledgeLevel * 0.05);
    }
}
