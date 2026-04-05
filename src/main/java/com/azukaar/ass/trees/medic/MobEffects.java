package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effects for the Medic skill tree
 */
public class MobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AzukaarSkillsStats.MODID);

    // Radiance Potency: temporarily increases Radiance damage and range
    // Amplifier 0 = +20% damage/range, 1 = +40%, 2 = +60%
    public static final DeferredHolder<MobEffect, MobEffect> RADIANCE_POTENCY = MOB_EFFECTS.register(
        "radiance_potency",
        () -> new RadiancePotencyEffect(MobEffectCategory.BENEFICIAL, 0xFFD700) // Gold color
    );

    public static class RadiancePotencyEffect extends MobEffect {
        public RadiancePotencyEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        /**
         * Get the multiplier for Radiance damage/range based on amplifier.
         * Amplifier 0 (tier 1) = 1.2x, 1 (tier 2) = 1.4x, 2 (tier 3) = 1.6x
         */
        public static float getMultiplier(int amplifier) {
            return 1.2f + (amplifier * 0.2f);
        }
    }
}
