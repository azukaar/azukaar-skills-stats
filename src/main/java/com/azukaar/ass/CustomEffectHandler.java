package com.azukaar.ass;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.trees.stats.CustomEffects;
import com.azukaar.ass.types.SkillEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Handles custom skill effects that don't use Minecraft's attribute system.
 * Delegates to tree-specific implementations.
 */
public class CustomEffectHandler {

    /**
     * Apply a custom effect to a player
     */
    public static void applyCustomEffect(Player player, String effectType, SkillEffect.Effect effect, int skillLevel) {
        if (CustomEffects.handles(effectType)) {
            CustomEffects.applyCustomEffect(player, effectType, effect, skillLevel);
            return;
        }

        AzukaarSkillsStats.LOGGER.warn("Unknown custom effect type: {}", effectType);
    }

    /**
     * Remove a custom effect from a player
     */
    public static void removeCustomEffect(Player player, String effectType, SkillEffect.Effect effect) {
        if (CustomEffects.handles(effectType)) {
            CustomEffects.removeCustomEffect(player, effectType, effect);
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
}
