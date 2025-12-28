package com.azukaar.ass;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Central registry for all mob effects.
 * Re-exports effects from tree-specific implementations.
 */
public class ModMobEffects {

    // Re-export from stats tree
    public static final DeferredHolder<MobEffect, MobEffect> INSTINCT =
        com.azukaar.ass.trees.stats.MobEffects.INSTINCT;

    // Re-export from farmer tree
    public static final DeferredHolder<MobEffect, MobEffect> ANIMAL_WHISPERER =
        com.azukaar.ass.trees.farmer.MobEffects.ANIMAL_WHISPERER;

    // Re-export from barbarian tree
    public static final DeferredHolder<MobEffect, MobEffect> POWERFUL_STRIKE =
        com.azukaar.ass.trees.barbarian.MobEffects.POWERFUL_STRIKE;
    public static final DeferredHolder<MobEffect, MobEffect> DEVOURING_STRIKE =
        com.azukaar.ass.trees.barbarian.MobEffects.DEVOURING_STRIKE;
    public static final DeferredHolder<MobEffect, MobEffect> GLUTTONY =
        com.azukaar.ass.trees.barbarian.MobEffects.GLUTTONY;
    public static final DeferredHolder<MobEffect, MobEffect> EXHAUSTED =
        com.azukaar.ass.trees.barbarian.MobEffects.EXHAUSTED;
    public static final DeferredHolder<MobEffect, MobEffect> CARNIVOROUS =
        com.azukaar.ass.trees.barbarian.MobEffects.CARNIVOROUS;

    /**
     * Register all mob effects from all trees
     */
    public static void registerAll(IEventBus modEventBus) {
        com.azukaar.ass.trees.stats.MobEffects.MOB_EFFECTS.register(modEventBus);
        com.azukaar.ass.trees.farmer.MobEffects.MOB_EFFECTS.register(modEventBus);
        com.azukaar.ass.trees.barbarian.MobEffects.MOB_EFFECTS.register(modEventBus);
    }
}
