package com.azukaar.ass.trees.barbarian;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.ActiveSkillEffect;
import com.azukaar.ass.api.ActiveSkillEffectRegistry;
import com.azukaar.ass.api.EffectData;
import com.azukaar.ass.api.PlayerData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Active effects for the Barbarian skill tree
 */
public class ActiveEffects {

    public static void registerAll() {
        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "gluttony"),
            new GluttonyActiveEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "gorge"),
            new GorgeActiveEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "floor_slam"),
            new FloorSlamActiveEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "giant_sweep"),
            new GiantSweepActiveEffect()
        );
    }

    /**
     * Gluttony active effect - adds a stack of Gluttony when activated
     */
    public static class GluttonyActiveEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Cannot use while Exhausted or Berserk
            if (player.hasEffect(MobEffects.EXHAUSTED)) {
                AzukaarSkillsStats.LOGGER.info("Player {} cannot use Gluttony while Exhausted",
                    player.getName().getString());
                return false;
            }

            // Get current Gluttony level (if any)
            int currentAmplifier = -1;
            MobEffectInstance currentEffect = player.getEffect(MobEffects.GLUTTONY);
            if (currentEffect != null) {
                currentAmplifier = currentEffect.getAmplifier();
            }

            // Max stack = skill level (skill level 1 = max Gluttony I = amp 0)
            int maxAmplifier = skillLevel - 1;
            int newAmplifier = Math.min(currentAmplifier + 1, maxAmplifier);

            // Apply Gluttony effect with infinite duration (-1)
            // Effect ends when hunger reaches 0, handled in TreeEvents
            player.addEffect(new MobEffectInstance(
                MobEffects.GLUTTONY,
                -1, // Infinite duration
                newAmplifier,
                false, // ambient
                true,  // visible
                true   // show icon
            ));

            AzukaarSkillsStats.LOGGER.info("Player {} activated Gluttony level {} (max: {})",
                player.getName().getString(), newAmplifier + 1, skillLevel);

            return true;
        }
    }

    /**
     * Gorge active effect - instantly fill hunger from inventory
     */
    public static class GorgeActiveEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            int foodConsumed = consumeFoodFromInventory(player);

            if (foodConsumed > 0) {
                AzukaarSkillsStats.LOGGER.info("Player {} gorged, consumed {} food items",
                    player.getName().getString(), foodConsumed);
                return true;
            }

            return false; // No food consumed, don't trigger cooldown
        }
    }

    /**
     * Floor Slam active effect - damages all nearby enemies
     */
    public static class FloorSlamActiveEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Check if player is holding an axe
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.is(TreeEvents.AXE_TAG)) {
                AzukaarSkillsStats.LOGGER.info("Player {} tried to use Floor Slam without an axe",
                    player.getName().getString());
                return false;
            }

            float baseDamage = 6.0f + (skillLevel * 3.0f);
            float radius = 4.0f;

            // Play slam sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.8f);

            // Spawn particles in a circle around the player
            if (player.level() instanceof ServerLevel serverLevel) {
                double x = player.getX();
                double y = player.getY() + 0.1;
                double z = player.getZ();

                // Ring of particles
                for (int i = 0; i < 32; i++) {
                    double angle = (2 * Math.PI * i) / 32;
                    double px = x + Math.cos(angle) * radius;
                    double pz = z + Math.sin(angle) * radius;
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, px, y, pz, 1, 0, 0, 0, 0);
                }

                // Ground crack particles
                for (int i = 0; i < 20; i++) {
                    double offsetX = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                    double offsetZ = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                    serverLevel.sendParticles(ParticleTypes.CRIT, x + offsetX, y, z + offsetZ, 2, 0, 0.1, 0, 0.1);
                }
            }

            // Find all entities in radius
            AABB area = player.getBoundingBox().inflate(radius);
            var entities = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());

            int entitiesHit = 0;
            for (LivingEntity entity : entities) {
                entity.hurt(player.damageSources().playerAttack(player), baseDamage);

                // Knock enemies up and away from player
                double dx = entity.getX() - player.getX();
                double dz = entity.getZ() - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    dx /= dist;
                    dz /= dist;
                }
                double horizontalStrength = 0.3 + (skillLevel * 0.1);
                double verticalStrength = 0.6 + (skillLevel * 0.2);
                entity.setDeltaMovement(
                    dx * horizontalStrength,
                    verticalStrength,
                    dz * horizontalStrength
                );
                entity.hurtMarked = true;

                entitiesHit++;
            }

            AzukaarSkillsStats.LOGGER.info("Player {} used Floor Slam (level {}), hit {} entities for {} damage",
                player.getName().getString(), skillLevel, entitiesHit, baseDamage);

            return true; // Always succeed if holding axe (even if no enemies hit)
        }
    }

    /**
     * Giant Sweep active effect - 180 degree arc attack in front of player
     */
    public static class GiantSweepActiveEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Check if player is holding a sword
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.is(TreeEvents.SWORD_TAG)) {
                AzukaarSkillsStats.LOGGER.info("Player {} tried to use Giant Sweep without a sword",
                    player.getName().getString());
                return false;
            }

            float baseDamage = 5.0f + (skillLevel * 2.0f);
            float radius = 6.0f; // Longer range than floor slam

            // Play sweep sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);

            // Get player's look direction (horizontal only)
            double lookX = -Math.sin(Math.toRadians(player.getYRot()));
            double lookZ = Math.cos(Math.toRadians(player.getYRot()));

            // Spawn sweep particles in a 180-degree arc
            if (player.level() instanceof ServerLevel serverLevel) {
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();

                // Arc of sweep particles
                for (int i = 0; i < 24; i++) {
                    // -90 to +90 degrees from look direction
                    double angleOffset = Math.toRadians(-90 + (180.0 * i / 23));
                    double cos = Math.cos(angleOffset);
                    double sin = Math.sin(angleOffset);
                    // Rotate the look vector
                    double px = lookX * cos - lookZ * sin;
                    double pz = lookX * sin + lookZ * cos;

                    for (double dist = 1.0; dist <= radius; dist += 1.0) {
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            x + px * dist, y, z + pz * dist, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Find all entities in radius
            AABB area = player.getBoundingBox().inflate(radius);
            var entities = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());

            int entitiesHit = 0;
            for (LivingEntity entity : entities) {
                // Check if entity is in the 180-degree arc in front of player
                double dx = entity.getX() - player.getX();
                double dz = entity.getZ() - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 0 && dist <= radius) {
                    // Normalize direction to entity
                    double dirX = dx / dist;
                    double dirZ = dz / dist;

                    // Dot product with look direction (positive = in front)
                    double dot = dirX * lookX + dirZ * lookZ;

                    if (dot >= 0) { // Entity is in front 180-degree arc
                        entity.hurt(player.damageSources().playerAttack(player), baseDamage);

                        // Knockback away from player
                        double knockbackStrength = 0.4 + (skillLevel * 0.1);
                        entity.setDeltaMovement(
                            dirX * knockbackStrength,
                            0.2,
                            dirZ * knockbackStrength
                        );
                        entity.hurtMarked = true;

                        entitiesHit++;
                    }
                }
            }

            AzukaarSkillsStats.LOGGER.info("Player {} used Giant Sweep (level {}), hit {} entities for {} damage",
                player.getName().getString(), skillLevel, entitiesHit, baseDamage);

            return true;
        }
    }

    private static final String CARNIVOROUS_SKILL = "azukaarskillsstats:carnivorous";
    private static final String MEAT_LOVER_SKILL = "azukaarskillsstats:meat_lover";

    /**
     * Reusable helper: Consume food from inventory until hunger is full.
     * Used by Gorge (active) and Auto-Feed (passive).
     * @param player The player to feed
     * @return Number of food items consumed
     */
    public static int consumeFoodFromInventory(Player player) {
        int foodConsumed = 0;
        int maxHunger = 20;
        boolean ateMeat = false;

        var foodData = player.getFoodData();
        int carnivorousLevel = PlayerData.getSkillLevel(player, CARNIVOROUS_SKILL);
        int meatLoverLevel = PlayerData.getSkillLevel(player, MEAT_LOVER_SKILL);

        // Loop through inventory and consume food until full
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (foodData.getFoodLevel() >= maxHunger) {
                break; // Hunger is full
            }

            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            FoodProperties food = stack.getFoodProperties(player);
            if (food == null) continue;

            boolean isMeat = stack.is(TreeEvents.MEAT_TAG);

            // Consume one item at a time until full
            while (!stack.isEmpty() && foodData.getFoodLevel() < maxHunger) {
                int nutrition = food.nutrition();
                float saturation = food.saturation();

                // Apply Meat Lover bonus (+50% for meat)
                if (isMeat && meatLoverLevel > 0) {
                    nutrition = (int) Math.ceil(nutrition * 1.5);
                    saturation = saturation * 1.5f;
                }

                // Apply food effects
                foodData.eat(nutrition, saturation);

                // Track if meat was eaten for Carnivorous
                if (isMeat) {
                    ateMeat = true;
                }

                // Consume the item
                stack.shrink(1);
                foodConsumed++;

                // Update the slot
                player.getInventory().setItem(i, stack);
            }
        }

        // Apply Carnivorous effect if meat was consumed and player has the skill
        if (ateMeat && carnivorousLevel > 0) {
            player.addEffect(new MobEffectInstance(
                MobEffects.CARNIVOROUS,
                600, // 30 seconds
                0,
                false,
                true,
                true
            ));
        }

        return foodConsumed;
    }
}
