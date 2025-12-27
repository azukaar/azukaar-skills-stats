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

    /**
     * Register all mob effects from all trees
     */
    public static void registerAll(IEventBus modEventBus) {
        com.azukaar.ass.trees.stats.MobEffects.MOB_EFFECTS.register(modEventBus);
        com.azukaar.ass.trees.farmer.MobEffects.MOB_EFFECTS.register(modEventBus);
    }
}
