package com.azukaar.ass.trees.stats;

import com.azukaar.ass.AzukaarSkillsStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effects for the Stats skill tree
 */
public class MobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AzukaarSkillsStats.MODID);

    // Instinct effect: gives crit chance based on amplifier
    // Amplifier 0 = 30%, 1 = 50%, 2 = 100%
    public static final DeferredHolder<MobEffect, MobEffect> INSTINCT = MOB_EFFECTS.register(
        "instinct",
        () -> new InstinctEffect(MobEffectCategory.BENEFICIAL, 0xFFD700) // Gold color
    );

    public static class InstinctEffect extends MobEffect {
        public InstinctEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        /**
         * Get crit chance percentage based on amplifier level
         * Level 1 (amp 0) = 30%, Level 2 (amp 1) = 50%, Level 3 (amp 2) = 100%
         */
        public static double getCritChance(int amplifier) {
            return switch (amplifier) {
                case 0 -> 0.30;
                case 1 -> 0.50;
                default -> 1.00;
            };
        }
    }
}
