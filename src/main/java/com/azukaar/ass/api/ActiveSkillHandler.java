package com.azukaar.ass.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.network.UseSkillPacket;
import com.azukaar.ass.types.ScalingData;
import com.azukaar.ass.types.Skill;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class ActiveSkillHandler {
    
    /**
     * Check if a skill has any active effects
     */
    public static boolean isActivatable(String skillId) {
        SkillEffect skillEffect = SkillDataManager.INSTANCE.getSkillEffects(skillId);
        if (skillEffect == null) return false;
        
        return skillEffect.getEffects().stream()
            .anyMatch(effect -> "active".equals(effect.getType()));
    }
    
    /**
     * Get current cooldown for a skill - now reads from skill level instead of effect level
     */
    public static int getEffectiveCooldown(Player player, String skillId) {
        Skill skill = SkillDataManager.INSTANCE.findSkillById(skillId);
        if (skill == null || skill.getCooldown() == null) {
            return 0; // No cooldown means instant reuse
        }
        
        int skillLevel = PlayerData.getSkillLevel(player, skillId);
        return skill.getEffectiveCooldown(skillLevel);
    }
    
    /**
     * Check if a skill has a cooldown defined
     */
    public static boolean hasCooldown(String skillId) {
        Skill skill = SkillDataManager.INSTANCE.findSkillById(skillId);
        return skill != null && skill.hasCooldown();
    }
    
    /**
     * Use an active skill
     */
    public static boolean useSkill(Player player, String skillId) {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new UseSkillPacket(skillId));
            return true;
        } else {
            return useSkillServerSide(player, skillId);
        }
    }
    
    private static boolean useSkillServerSide(Player player, String skillId) {
        // Check if player has the skill
        if (PlayerData.getSkillLevel(player, skillId) <= 0) {
            return false;
        }
        
        // Check if skill is activatable
        if (!isActivatable(skillId)) {
            return false;
        }
        
        // Check cooldown
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return false;
        
        long currentTime = player.level().getGameTime();
        if (skills.isSkillOnCooldown(skillId, currentTime)) {
            return false;
        }
        
        // Execute active effects
        SkillEffect skillEffect = SkillDataManager.INSTANCE.getSkillEffects(skillId);
        int skillLevel = PlayerData.getSkillLevel(player, skillId);
        boolean anySucceeded = false;
        
        for (SkillEffect.Effect effect : skillEffect.getEffects()) {
            if ("active".equals(effect.getType())) {
                if (executeActiveEffect(player, skillId, skillLevel, effect)) {
                    anySucceeded = true;
                }
            }
        }
        
        if (anySucceeded) {
            // Set cooldown using the new skill-level cooldown system
            int cooldown = getEffectiveCooldown(player, skillId);
            if (cooldown > 0) {
                skills.setSkillCooldown(skillId, currentTime + cooldown);
            }
            
            // Sync to client
            if (player instanceof ServerPlayer serverPlayer) {
                var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                provider.syncToClient(serverPlayer);
            }
        }
        
        return anySucceeded;
    }
    
    private static boolean executeActiveEffect(Player player, String skillId, int skillLevel, SkillEffect.Effect effect) {
        String activeEffectId = effect.getActiveEffectId();
        if (activeEffectId == null) return false;
        
        ActiveSkillEffect activeEffect = ActiveSkillEffectRegistry.get(activeEffectId);
        if (activeEffect == null) {
            AzukaarSkillsStats.LOGGER.warn("Unknown active effect: {} for skill: {}", activeEffectId, skillId);
            return false;
        }
        
        // Create effect data from current attribute values
        EffectData effectData = createEffectDataFromAttributes(player, skillId, effect);
        
        // Check if effect can be used
        if (!activeEffect.canUse(player, skillId, skillLevel, effectData)) {
            return false;
        }
        
        // Execute the effect
        return activeEffect.execute(player, skillId, skillLevel, effectData);
    }
    
    private static EffectData createEffectDataFromAttributes(Player player, String skillId, SkillEffect.Effect effect) {
        Map<String, Object> currentData = new HashMap<>();
        
        // Read current values using the parameter system
        for (Map.Entry<String, ScalingData> entry : effect.getData().entrySet()) {
            String dataKey = entry.getKey();
            double value = SkillEffect.getSkillParameter(player, skillId, dataKey);
            currentData.put(dataKey, value);
        }
        
        return new EffectData(currentData);
    }
}