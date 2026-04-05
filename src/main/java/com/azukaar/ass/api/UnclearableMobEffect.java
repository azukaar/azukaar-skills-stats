package com.azukaar.ass.api;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Base class for mod effects that cannot be removed by vanilla /effect clear or milk.
 * Effects extending this class will be protected from external removal.
 */
public class UnclearableMobEffect extends MobEffect {
    public UnclearableMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
