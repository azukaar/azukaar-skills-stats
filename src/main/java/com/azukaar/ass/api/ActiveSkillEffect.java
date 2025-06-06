package com.azukaar.ass.api;

import net.minecraft.world.entity.player.Player;

public interface ActiveSkillEffect {
    /**
     * Execute the active skill effect
     */
    boolean execute(Player player, String skillId, int skillLevel, EffectData effectData);
    
    /**
     * Check if the skill can be used
     */
    default boolean canUse(Player player, String skillId, int skillLevel, EffectData effectData) {
        return true;
    }
}