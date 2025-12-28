package com.azukaar.ass.trees.barbarian;

import java.util.Set;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Event handlers for the Barbarian skill tree
 */
public class TreeEvents {

    private static final String IRON_STOMACH_SKILL = "azukaarskillsstats:iron_stomach";
    private static final String AXE_FOCUS_SKILL = "azukaarskillsstats:axe_focus";
    private static final String AXE_FOCUS_EFFECT = "azukaarskillsstats:axe_focus";
    private static final String AXE_POWER_SKILL = "azukaarskillsstats:axe_power";
    private static final String AXE_POWER_EFFECT = "azukaarskillsstats:axe_power";
    private static final String SWORD_FOCUS_SKILL = "azukaarskillsstats:sword_focus";
    private static final String SWORD_FOCUS_EFFECT = "azukaarskillsstats:sword_focus";
    private static final String SWORD_POWER_SKILL = "azukaarskillsstats:sword_power";
    private static final String SWORD_POWER_EFFECT = "azukaarskillsstats:sword_power";
    private static final String CRUSHING_BLOW_SKILL = "azukaarskillsstats:crushing_blow";
    private static final String CRUSHING_BLOW_EFFECT = "azukaarskillsstats:crushing_blow";

    // Raw meats and other "unsafe" foods that Iron Stomach allows
    private static final Set<Item> UNSAFE_FOODS = Set.of(
        Items.ROTTEN_FLESH,
        Items.SPIDER_EYE,
        Items.POISONOUS_POTATO,
        Items.PUFFERFISH,
        Items.CHICKEN,  // Raw chicken
        Items.BEEF,     // Raw beef (safe but included for consistency)
        Items.PORKCHOP, // Raw porkchop
        Items.MUTTON,   // Raw mutton
        Items.RABBIT,   // Raw rabbit
        Items.COD,      // Raw cod
        Items.SALMON    // Raw salmon
    );

    /**
     * Handle damage dealt by player - apply Rage and Powerful Strike multipliers
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        float damage = event.getNewDamage();
        float originalDamage = damage;

        // Check held weapon for focus/power skills
        ItemStack heldItem = player.getMainHandItem();

        // Apply Axe skills damage bonuses
        if (heldItem.is(ItemTags.AXES)) {
            // Axe Focus
            int axeFocusLevel = PlayerData.getSkillLevel(player, AXE_FOCUS_SKILL);
            if (axeFocusLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, AXE_FOCUS_SKILL, AXE_FOCUS_EFFECT, "damage");
            }
            // Axe Power
            int axePowerLevel = PlayerData.getSkillLevel(player, AXE_POWER_SKILL);
            if (axePowerLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, AXE_POWER_SKILL, AXE_POWER_EFFECT, "damage");
            }
            // Crushing Blow (damage + knockback)
            int crushingBlowLevel = PlayerData.getSkillLevel(player, CRUSHING_BLOW_SKILL);
            if (crushingBlowLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, CRUSHING_BLOW_SKILL, CRUSHING_BLOW_EFFECT, "damage");
                // Knockback is handled separately in attack event
            }
        }

        // Apply Sword skills damage bonuses
        if (heldItem.is(ItemTags.SWORDS)) {
            // Sword Focus
            int swordFocusLevel = PlayerData.getSkillLevel(player, SWORD_FOCUS_SKILL);
            if (swordFocusLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, SWORD_FOCUS_SKILL, SWORD_FOCUS_EFFECT, "damage");
            }
            // Sword Power
            int swordPowerLevel = PlayerData.getSkillLevel(player, SWORD_POWER_SKILL);
            if (swordPowerLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, SWORD_POWER_SKILL, SWORD_POWER_EFFECT, "damage");
            }
        }

        // Apply Rage damage bonus
        float multiplier = 1.0f;
        MobEffectInstance rageEffect = player.getEffect(MobEffects.RAGE);
        if (rageEffect != null) {
            multiplier += MobEffects.RageEffect.getDamageMultiplier(rageEffect.getAmplifier());
        }

        // Apply Powerful Strike damage multiplier (and consume the effect)
        MobEffectInstance strikeEffect = player.getEffect(MobEffects.POWERFUL_STRIKE);
        if (strikeEffect != null) {
            multiplier += MobEffects.PowerfulStrikeEffect.getDamageMultiplier(strikeEffect.getAmplifier());
            // Remove the effect after use
            player.removeEffect(MobEffects.POWERFUL_STRIKE);
        }

        damage *= (float) multiplier;

        if (damage != originalDamage) {
            event.setNewDamage(damage);
        }
    }

    /**
     * Handle knockback - Crushing Blow adds knockback with axes
     */
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        // Find the attacker (last damage source)
        if (!(target.getLastDamageSource() != null &&
              target.getLastDamageSource().getEntity() instanceof Player player)) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.is(ItemTags.AXES)) return;

        int crushingBlowLevel = PlayerData.getSkillLevel(player, CRUSHING_BLOW_SKILL);
        if (crushingBlowLevel <= 0) return;

        // Add knockback bonus
        double knockbackBonus = SkillEffect.getSkillParameter(player, CRUSHING_BLOW_SKILL, CRUSHING_BLOW_EFFECT, "knockback");
        event.setStrength(event.getStrength() + (float) knockbackBonus);
    }

    /**
     * Handle food consumption - Iron Stomach removes negative effects from raw meat
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int ironStomachLevel = PlayerData.getSkillLevel(player, IRON_STOMACH_SKILL);
        if (ironStomachLevel <= 0) return;

        // Remove negative effects that would be applied by unsafe foods
        // Schedule for next tick to ensure effects are already applied
        player.level().getServer().execute(() -> {
            // Remove Hunger effect (from rotten flesh)
            player.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
            
            Item item = event.getItem().getItem();
            if (!UNSAFE_FOODS.contains(item)) return;
            
            // Remove Poison effect (from spider eye, pufferfish, poisonous potato)
            player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            // Remove Nausea effect (from pufferfish)
            player.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
        });
    }
}
