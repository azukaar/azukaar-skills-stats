package com.azukaar.ass;

import com.azukaar.ass.api.ActiveSkillEffect;
import com.azukaar.ass.api.ActiveSkillEffectRegistry;
import com.azukaar.ass.api.EffectData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BuiltinActiveEffects {
    
    public static void registerAll() {
        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "dash"),
            new DashEffect()
        );
        
        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "heal"),
            new HealEffect()
        );
        
        // Add more built-in effects as needed
    }
    
    public static class DashEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            double power = effectData.getDouble("power", 1.5);
            double distance = effectData.getDouble("distance", 8.0);
            
            Vec3 lookDirection = player.getLookAngle();
            Vec3 dashVelocity = lookDirection.scale(power);
            
            player.setDeltaMovement(player.getDeltaMovement().add(dashVelocity));
            player.hurtMarked = true;
            
            AzukaarSkillsStats.LOGGER.info("Player {} dashed with power: {}, distance: {}", 
                player.getName().getString(), power, distance);
            
            return true;
        }
    }
    
    public static class HealEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            double amount = effectData.getDouble("amount", 10.0);
            double range = effectData.getDouble("range", 0.0);

            System.out.println("Executing HealEffect for player: " + player.getName().getString());
            
            if (range <= 0) {
                // Self heal
                player.heal((float) amount);
                AzukaarSkillsStats.LOGGER.info("Player {} healed self for: {}", 
                    player.getName().getString(), amount);
            } else {
                // Area heal - implement as needed
                areaHeal(player, amount, range);
            }
            
            return true;
        }
        
        @Override
        public boolean canUse(Player player, String skillId, int skillLevel, EffectData effectData) {
            double range = effectData.getDouble("range", 0.0);
            
            if (range <= 0) {
                // Self heal - only if not at full health
                return player.getHealth() < player.getMaxHealth();
            } else {
                // Area heal - always usable
                return true;
            }
        }
        
        private void areaHeal(Player caster, double amount, double range) {
            // TODO: Implement area healing logic
            AzukaarSkillsStats.LOGGER.info("Area heal: amount={}, range={}", amount, range);
        }
    }
}