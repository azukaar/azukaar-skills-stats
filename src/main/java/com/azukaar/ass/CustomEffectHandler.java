package com.azukaar.ass;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Handles custom skill effects that don't use Minecraft's attribute system
 */
public class CustomEffectHandler {
    
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
                // Unknown effect type - log warning
                com.azukaar.ass.AzukaarSkillsStats.LOGGER.warn("Unknown custom effect type: {}", effectType);
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
    
    /**
     * Get the total modifier value for a specific custom effect type across all skills
     */
    public static double getTotalCustomEffectValue(Player player, String effectType) {
        double totalValue = 0.0;
        
        for (SkillEffect skillEffect : SkillDataManager.INSTANCE.getAllSkillEffects()) {
            if (skillEffect.getEffects() != null) {
                for (SkillEffect.Effect effect : skillEffect.getEffects()) {
                    if (effectType.equals(effect.getType())) {
                        int skillLevel = PlayerData.getSkillLevel(player, skillEffect.getSkillId());
                        if (skillLevel > 0) {
                            double value = effect.getScaling().calculateValue(effect.getValue(), skillLevel);
                            totalValue += value;
                        }
                    }
                }
            }
        }
        
        return totalValue;
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