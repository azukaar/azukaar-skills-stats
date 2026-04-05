package com.azukaar.ass.trees.barbarian;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effects for the Barbarian skill tree
 */
public class MobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AzukaarSkillsStats.MODID);

    // Powerful Strike effect: marks player for next attack multiplier
    public static final DeferredHolder<MobEffect, MobEffect> POWERFUL_STRIKE = MOB_EFFECTS.register(
        "powerful_strike",
        () -> new PowerfulStrikeEffect(MobEffectCategory.BENEFICIAL, 0xFFAA00) // Orange color
    );

    // Powerful Strike: marker effect, damage handled in TreeEvents
    public static class PowerfulStrikeEffect extends com.azukaar.ass.api.UnclearableMobEffect {
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

    // Devouring Strike: consumes all Gluttony stacks for +2x damage per stack
    public static final DeferredHolder<MobEffect, MobEffect> DEVOURING_STRIKE = MOB_EFFECTS.register(
        "devouring_strike",
        () -> new DevouringStrikeEffect(MobEffectCategory.BENEFICIAL, 0x660000) // Dark red
    );

    public static class DevouringStrikeEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public DevouringStrikeEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        /**
         * Get damage multiplier based on Gluttony stacks consumed
         * Each stack = +2x damage (so Gluttony V = +10x = 11x total)
         */
        public static double getDamageMultiplier(int gluttonyLevel) {
            return gluttonyLevel * 2.0;
        }
    }

    // Gluttony effect: stacking buff that drains hunger for power
    // Amplifier 0 = Gluttony I, Amplifier 4 = Gluttony V
    public static final DeferredHolder<MobEffect, MobEffect> GLUTTONY = MOB_EFFECTS.register(
        "gluttony",
        () -> new GluttonyEffect(MobEffectCategory.BENEFICIAL, 0x8B4513) // Brown color
    );

    // Exhausted effect: prevents Gluttony stacking
    public static final DeferredHolder<MobEffect, MobEffect> EXHAUSTED = MOB_EFFECTS.register(
        "exhausted",
        () -> new ExhaustedEffect(MobEffectCategory.HARMFUL, 0x555555) // Gray color
    );

    // Gluttony: stacking effect that drains hunger
    public static class GluttonyEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public GluttonyEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            // Hunger drain is handled in TreeEvents to access FoodData
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            // Tick rate based on amplifier (stack level)
            // Gluttony I (amp 0): every 100 ticks (5 sec)
            // Gluttony II (amp 1): every 80 ticks (4 sec)
            // Gluttony III (amp 2): every 60 ticks (3 sec)
            // Gluttony IV (amp 3): every 40 ticks (2 sec)
            // Gluttony V (amp 4): every 20 ticks (1 sec)
            int tickRate = 100 - (amplifier * 20);
            return duration % tickRate == 0;
        }

        /**
         * Get the hunger drain rate in ticks based on stack level (amplifier)
         */
        public static int getTickRate(int amplifier) {
            return 100 - (amplifier * 20);
        }
    }

    // Exhausted: prevents Gluttony usage
    public static class ExhaustedEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public ExhaustedEffect(MobEffectCategory category, int color) {
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
    }

    // Carnivorous effect: reduces Gluttony hunger drain by 50%
    public static final DeferredHolder<MobEffect, MobEffect> CARNIVOROUS = MOB_EFFECTS.register(
        "carnivorous",
        () -> new CarnivorousEffect(MobEffectCategory.BENEFICIAL, 0xCC3300) // Dark red color
    );

    public static class CarnivorousEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public CarnivorousEffect(MobEffectCategory category, int color) {
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
    }

    // Fear: mobs flee from all players within 8 blocks
    public static final DeferredHolder<MobEffect, MobEffect> FEAR = MOB_EFFECTS.register(
        "fear",
        () -> new FearEffect(MobEffectCategory.HARMFUL, 0x4B0082)
    );

    public static class FearEffect extends MobEffect {
        public FearEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Mob mob)) return true;

            mob.setTarget(null);

            // Find nearest player within 8 blocks
            Player nearest = mob.level().getNearestPlayer(mob, 8.0);
            if (nearest != null) {
                Vec3 away = mob.position().subtract(nearest.position()).normalize();
                Vec3 fleePos = mob.position().add(away.scale(8));
                mob.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.2);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }
    }

    // Taunt: mobs are drawn to nearest player
    public static final DeferredHolder<MobEffect, MobEffect> TAUNT = MOB_EFFECTS.register(
        "taunt",
        () -> new TauntEffect(MobEffectCategory.HARMFUL, 0xCC0000)
    );

    public static class TauntEffect extends MobEffect {
        public TauntEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Mob mob)) return true;

            Player nearest = mob.level().getNearestPlayer(mob, 16.0);
            if (nearest != null) {
                mob.setTarget(nearest);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }
    }

    // Berserk: massive damage boost when Gluttony ends
    public static final DeferredHolder<MobEffect, MobEffect> BERSERK = MOB_EFFECTS.register(
        "berserk",
        () -> new BerserkEffect(MobEffectCategory.BENEFICIAL, 0xFF0000)
    );

    public static class BerserkEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public BerserkEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        /**
         * +50% damage per level (amp 0 = +50%, amp 5 = +300%)
         */
        public static double getDamageMultiplier(int amplifier) {
            return (amplifier + 1) * 0.5;
        }
    }

    // Last Stand: invulnerability marker
    public static final DeferredHolder<MobEffect, MobEffect> LAST_STAND_EFFECT = MOB_EFFECTS.register(
        "last_stand_effect",
        () -> new LastStandEffect(MobEffectCategory.BENEFICIAL, 0xFFD700)
    );

    public static class LastStandEffect extends com.azukaar.ass.api.UnclearableMobEffect {
        public LastStandEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
