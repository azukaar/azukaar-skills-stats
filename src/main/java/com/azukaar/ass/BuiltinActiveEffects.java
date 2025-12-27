package com.azukaar.ass;

/**
 * Registry for all built-in active effects.
 * Delegates to tree-specific implementations.
 */
public class BuiltinActiveEffects {

    public static void registerAll() {
        // Register effects from each skill tree
        com.azukaar.ass.trees.stats.ActiveEffects.registerAll();
    }
}
