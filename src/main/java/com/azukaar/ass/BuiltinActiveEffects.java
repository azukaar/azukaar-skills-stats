package com.azukaar.ass;

import com.azukaar.ass.api.ActiveSkillEffect;
import com.azukaar.ass.api.ActiveSkillEffectRegistry;
import com.azukaar.ass.api.EffectData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "feed"),
            new FeedEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "meditate"),
            new MeditateEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "repair"),
            new RepairEffect()
        );
    }
    
    public static class DashEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            double power = effectData.getDouble("power", 1.5);
            
            Vec3 lookDirection = player.getLookAngle();
            Vec3 dashVelocity = lookDirection.scale(power);
            
            player.setDeltaMovement(player.getDeltaMovement().add(dashVelocity));
            player.hurtMarked = true;
            
            AzukaarSkillsStats.LOGGER.info("Player {} dashed with power: {}", 
                player.getName().getString(), power);
            
            return true;
        }
    }
    
    public static class HealEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            double amount = effectData.getDouble("health_restored", 0.0);

            System.out.println("Executing HealEffect for player: " + player.getName().getString());

            // Self heal
            player.heal((float) amount);
            AzukaarSkillsStats.LOGGER.info("Player {} healed self for: {}",
                player.getName().getString(), amount);

            return true;
        }
    }

    public static class FeedEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            int hungerAmount = (int) effectData.getDouble("hunger_restored", 4.0);
            float saturationAmount = (float) effectData.getDouble("saturation_restored", 2.0);

            System.out.println("Executing FeedEffect for player: " + player.getName().getString());

            // Restore hunger and saturation
            player.getFoodData().eat(hungerAmount, saturationAmount);
            AzukaarSkillsStats.LOGGER.info("Player {} restored {} hunger and {} saturation",
                player.getName().getString(), hungerAmount, saturationAmount);

            return true;
        }
    }

    public static class MeditateEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            int xpAmount = (int) effectData.getDouble("xp_gained", 10.0);

            System.out.println("Executing MeditateEffect for player: " + player.getName().getString());

            // Grant Minecraft XP
            player.giveExperiencePoints(xpAmount);
            AzukaarSkillsStats.LOGGER.info("Player {} gained {} XP from meditation",
                player.getName().getString(), xpAmount);

            return true;
        }
    }

    public static class RepairEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            double percent = effectData.getDouble("percent", 10.0);

            System.out.println("Executing RepairEffect for player: " + player.getName().getString());

            int itemsRepaired = 0;

            // Repair main hand
            ItemStack mainHand = player.getMainHandItem();
            if (repairItem(mainHand, percent)) itemsRepaired++;

            // Repair off hand
            ItemStack offHand = player.getOffhandItem();
            if (repairItem(offHand, percent)) itemsRepaired++;

            // Repair armor
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = player.getItemBySlot(slot);
                if (repairItem(armor, percent)) itemsRepaired++;
            }

            AzukaarSkillsStats.LOGGER.info("Player {} repaired {} items by {}%",
                player.getName().getString(), itemsRepaired, percent);

            return itemsRepaired > 0;
        }

        private boolean repairItem(ItemStack stack, double percent) {
            if (!Utils.isRepairable(stack)) return false;

            int maxDamage = stack.getMaxDamage();
            int repairAmount = (int) Math.ceil(maxDamage * percent / 100.0);
            int currentDamage = stack.getDamageValue();
            int newDamage = Math.max(0, currentDamage - repairAmount);

            stack.setDamageValue(newDamage);
            return true;
        }
    }
}