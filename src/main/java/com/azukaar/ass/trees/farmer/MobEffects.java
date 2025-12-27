package com.azukaar.ass.trees.farmer;

import com.azukaar.ass.AzukaarSkillsStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Mob effects for the Farmer skill tree
 */
public class MobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AzukaarSkillsStats.MODID);

    // Animal Whisperer effect: attracts passive animals
    // Amplifier determines radius: 0 = 10 blocks, 1 = 15, 2 = 20, 3 = 25, 4 = 30
    public static final DeferredHolder<MobEffect, MobEffect> ANIMAL_WHISPERER = MOB_EFFECTS.register(
        "animal_whisperer",
        () -> new AnimalWhispererEffect(MobEffectCategory.BENEFICIAL, 0x90EE90) // Light green color
    );

    // Forager's Appetite effect: makes seeds edible
    public static final DeferredHolder<MobEffect, MobEffect> FORAGERS_APPETITE = MOB_EFFECTS.register(
        "foragers_appetite",
        () -> new ForagersAppetiteEffect(MobEffectCategory.BENEFICIAL, 0x7CFC00) // Lawn green color
    );

    public static class AnimalWhispererEffect extends MobEffect {
        public AnimalWhispererEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            // Tick every 20 ticks
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Player player)) return true;
            if (player.level().isClientSide()) return true;

            // Calculate radius based on amplifier (skill level - 1)
            // Level 1 = amp 0 = 10 blocks, Level 5 = amp 4 = 30 blocks
            int radius = 10 + (amplifier * 5);
            int height = 10;

            // Find all passive animals in range
            AABB searchBox = new AABB(
                player.getX() - radius, player.getY() - height, player.getZ() - radius,
                player.getX() + radius, player.getY() + height, player.getZ() + radius
            );

            List<Animal> animals = player.level().getEntitiesOfClass(Animal.class, searchBox);

            for (Animal animal : animals) {
                // Skip animals that are aggressive or have a target
                if (animal.getTarget() != null) continue;
                // Skip tameable animals that aren't tame (wolves, cats, etc.)
                if (animal instanceof net.minecraft.world.entity.TamableAnimal tamable) continue;

                // Use navigation system to pathfind toward player
                animal.getNavigation().moveTo(player, 1.0);
            }

            return true;
        }

        /**
         * Get radius based on amplifier level
         */
        public static int getRadius(int amplifier) {
            return 10 + (amplifier * 5);
        }
    }

    // Forager's Appetite: marker effect that makes seeds edible (handled in TreeEvents)
    public static class ForagersAppetiteEffect extends MobEffect {
        public ForagersAppetiteEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
