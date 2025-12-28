package com.azukaar.ass.trees.barbarian;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effects for the Barbarian skill tree
 */
public class MobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AzukaarSkillsStats.MODID);

    // Rage effect: +100% damage per amplifier level
    public static final DeferredHolder<MobEffect, MobEffect> RAGE = MOB_EFFECTS.register(
        "rage",
        () -> new RageEffect(MobEffectCategory.BENEFICIAL, 0xFF4444) // Red color
    );

    // Powerful Strike effect: marks player for next attack multiplier
    public static final DeferredHolder<MobEffect, MobEffect> POWERFUL_STRIKE = MOB_EFFECTS.register(
        "powerful_strike",
        () -> new PowerfulStrikeEffect(MobEffectCategory.BENEFICIAL, 0xFFAA00) // Orange color
    );

    public static class RageEffect extends MobEffect {
        public RageEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return false;
        }

        /**
         * Get damage multiplier based on amplifier (skill level - 1)
         * Level 1 = amp 0 = 1.20x damage (+20%)
         * Level 2 = amp 1 = 1.40 damage (+40%)
         * Level 3 = amp 2 = 1.60 damage (+60%)
         */
        public static double getDamageMultiplier(int amplifier) {
            return 0.20 + amplifier * 0.20;
        }
    }

    // Powerful Strike: marker effect, damage handled in TreeEvents
    public static class PowerfulStrikeEffect extends MobEffect {
        public PowerfulStrikeEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        /**
         * Get damage multiplier based on amplifier (skill level - 1)
         * Level 1 = amp 0 = 1.34x damage (+34%)
         * Level 2 = amp 1 = 1.67x damage (+67%)
         * Level 3 = amp 2 = 2x damage (+100%)
         */
        public static double getDamageMultiplier(int amplifier) {
            return 0.34 + amplifier * 0.33;
        }
    }
}
