package com.azukaar.ass.trees.barbarian;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.ModEvents;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event handlers for the Barbarian skill tree
 */
public class TreeEvents {

    private static final String IRON_STOMACH_SKILL = "azukaarskillsstats:iron_stomach";
    private static final String AXE_FOCUS_SKILL = "azukaarskillsstats:axe_focus";
    private static final String AXE_FOCUS_EFFECT = "azukaarskillsstats:axe_focus";
    private static final String AXE_POWER_SKILL = "azukaarskillsstats:axe_power";
    private static final String AXE_POWER_EFFECT = "azukaarskillsstats:axe_power";
    private static final String SWORD_FOCUS_SKILL = "azukaarskillsstats:sword_focus";
    private static final String SWORD_FOCUS_EFFECT = "azukaarskillsstats:sword_focus";
    private static final String SWORD_POWER_SKILL = "azukaarskillsstats:sword_power";
    private static final String SWORD_POWER_EFFECT = "azukaarskillsstats:sword_power";
    private static final String CRUSHING_BLOW_SKILL = "azukaarskillsstats:crushing_blow";
    private static final String CRUSHING_BLOW_EFFECT = "azukaarskillsstats:crushing_blow";
    private static final String PARRY_SKILL = "azukaarskillsstats:parry";
    private static final String PARRY_EFFECT = "azukaarskillsstats:parry";
    private static final String QUICK_EATING_SKILL = "azukaarskillsstats:quick_eating";
    private static final String EFFICIENT_DIGESTION_SKILL = "azukaarskillsstats:efficient_digestion";
    private static final String MEAT_LOVER_SKILL = "azukaarskillsstats:meat_lover";
    private static final String FEEDING_EDGE_SKILL = "azukaarskillsstats:feeding_edge";
    private static final String GLUTTONOUS_WEAPON_SKILL = "azukaarskillsstats:gluttonous_weapon";
    private static final String FLOOR_SLAM_SKILL = "azukaarskillsstats:floor_slam";
    private static final String FLOOR_SLAM_EFFECT = "azukaarskillsstats:floor_slam";
    private static final String AUTO_FEED_SKILL = "azukaarskillsstats:auto_feed";
    private static final String INTIMIDATING_PRESENCE_SKILL = "azukaarskillsstats:intimidating_presence";
    private static final String BERSERKER_SKILL = "azukaarskillsstats:berserker";
    private static final String SECOND_WIND_SKILL = "azukaarskillsstats:second_wind";
    private static final String LAST_STAND_SKILL = "azukaarskillsstats:last_stand";

    // Item tags for extensible datapack support
    public static final TagKey<Item> UNSAFE_FOOD_TAG = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "unsafe_food"));
    public static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "meat"));
    public static final TagKey<Item> AXE_TAG = ItemTags.AXES;
    public static final TagKey<Item> SWORD_TAG = ItemTags.SWORDS;

    /**
     * Handle damage dealt by player - apply Powerful Strike multiplier
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        float damage = event.getNewDamage();
        float originalDamage = damage;

        float multiplier = 1.0f;
        boolean logDamage = false;

        // Check held weapon for focus/power skills
        ItemStack heldItem = player.getMainHandItem();

        // Apply Axe skills damage bonuses
        if (heldItem.is(ItemTags.AXES)) {
            // Axe Focus
            int axeFocusLevel = PlayerData.getSkillLevel(player, AXE_FOCUS_SKILL);
            if (axeFocusLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, AXE_FOCUS_SKILL, AXE_FOCUS_EFFECT, "damage");
                logDamage = true;
            }
            // Axe Power
            int axePowerLevel = PlayerData.getSkillLevel(player, AXE_POWER_SKILL);
            if (axePowerLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, AXE_POWER_SKILL, AXE_POWER_EFFECT, "damage");
                logDamage = true;
            }
            // Crushing Blow (damage + knockback)
            int crushingBlowLevel = PlayerData.getSkillLevel(player, CRUSHING_BLOW_SKILL);
            if (crushingBlowLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, CRUSHING_BLOW_SKILL, CRUSHING_BLOW_EFFECT, "damage");
                logDamage = true;
                // Knockback is handled separately in attack event
            }

            // Sword skills penalize axe damage
            int swordPowerPenalty = PlayerData.getSkillLevel(player, SWORD_POWER_SKILL);
            if (swordPowerPenalty > 0) {
                damage -= (float) SkillEffect.getSkillParameter(player, SWORD_POWER_SKILL, SWORD_POWER_EFFECT, "axe_penalty");
                logDamage = true;
            }
            int parryPenalty = PlayerData.getSkillLevel(player, PARRY_SKILL);
            if (parryPenalty > 0) {
                damage -= (float) SkillEffect.getSkillParameter(player, PARRY_SKILL, PARRY_EFFECT, "axe_penalty");
                logDamage = true;
            }
        }

        // Apply Sword skills damage bonuses
        if (heldItem.is(ItemTags.SWORDS)) {
            // Sword Focus
            int swordFocusLevel = PlayerData.getSkillLevel(player, SWORD_FOCUS_SKILL);
            if (swordFocusLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, SWORD_FOCUS_SKILL, SWORD_FOCUS_EFFECT, "damage");
                logDamage = true;
            }
            // Sword Power
            int swordPowerLevel = PlayerData.getSkillLevel(player, SWORD_POWER_SKILL);
            if (swordPowerLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, SWORD_POWER_SKILL, SWORD_POWER_EFFECT, "damage");
                logDamage = true;
            }
            // Parry damage bonus
            int parryLevel = PlayerData.getSkillLevel(player, PARRY_SKILL);
            if (parryLevel > 0) {
                damage += (float) SkillEffect.getSkillParameter(player, PARRY_SKILL, PARRY_EFFECT, "damage");
                logDamage = true;
            }

            // Axe skills penalize sword damage
            int axePowerPenalty = PlayerData.getSkillLevel(player, AXE_POWER_SKILL);
            if (axePowerPenalty > 0) {
                damage -= (float) SkillEffect.getSkillParameter(player, AXE_POWER_SKILL, AXE_POWER_EFFECT, "sword_penalty");
                logDamage = true;
            }
            int crushingBlowPenalty = PlayerData.getSkillLevel(player, CRUSHING_BLOW_SKILL);
            if (crushingBlowPenalty > 0) {
                damage -= (float) SkillEffect.getSkillParameter(player, CRUSHING_BLOW_SKILL, CRUSHING_BLOW_EFFECT, "sword_penalty");
                logDamage = true;
            }
        }

        if (damage < 0) damage = 0;

        // Feeding Edge: Multiply Sharpness/Power enchant bonus by Gluttony level while Carnivorous
        if (player.hasEffect(MobEffects.CARNIVOROUS)) {
            int feedingEdgeLevel = PlayerData.getSkillLevel(player, FEEDING_EDGE_SKILL);
            MobEffectInstance gluttonyForEdge = player.getEffect(MobEffects.GLUTTONY);
            if (feedingEdgeLevel > 0 && gluttonyForEdge != null) {
                int gluttonyLevel = gluttonyForEdge.getAmplifier() + 1;
                var enchantmentRegistry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                // Check Sharpness (melee)
                var sharpness = enchantmentRegistry.getHolderOrThrow(Enchantments.SHARPNESS);
                int sharpnessLevel = heldItem.getEnchantmentLevel(sharpness);
                if (sharpnessLevel > 0) {
                    // Sharpness bonus: 0.5 * level + 0.5
                    float sharpnessBonus = 0.5f * sharpnessLevel + 0.5f;
                    // Add (gluttony_level - 1) copies of the bonus
                    damage += sharpnessBonus * (gluttonyLevel - 1);
                    logDamage = true;
                }

                // Check Power (bows) - bonus is 0.5 * (level + 1) per arrow
                var power = enchantmentRegistry.getHolderOrThrow(Enchantments.POWER);
                int powerLevel = heldItem.getEnchantmentLevel(power);
                if (powerLevel > 0) {
                    float powerBonus = 0.5f * (powerLevel + 1);
                    damage += powerBonus * (gluttonyLevel - 1);
                    logDamage = true;
                }
            }
        }

        // Apply Powerful Strike damage multiplier (and consume the effect)
        MobEffectInstance strikeEffect = player.getEffect(MobEffects.POWERFUL_STRIKE);
        if (strikeEffect != null) {
            multiplier += MobEffects.PowerfulStrikeEffect.getDamageMultiplier(strikeEffect.getAmplifier());
            // Remove the effect after use
            ModEvents.removeModEffect(player, MobEffects.POWERFUL_STRIKE);
            logDamage = true;
        }

        // Apply Gluttony damage multiplier (+10% per stack level)
        MobEffectInstance gluttonyEffect = player.getEffect(MobEffects.GLUTTONY);

        // If Exhausted, apply stacking damage penalty (0.75^level)
        // Level I: 0.75x, Level II: 0.56x, Level III: 0.42x, Level IV: 0.32x, Level V: 0.24x
        MobEffectInstance exhaustedEffect = player.getEffect(MobEffects.EXHAUSTED);
        if (exhaustedEffect != null) {
            int exhaustedLevel = exhaustedEffect.getAmplifier() + 1; // amp 0 = level 1
            multiplier = (float) Math.pow(0.75, exhaustedLevel);
            logDamage = true;
        }

        // Berserk: +200% damage per level
        MobEffectInstance berserkEffect = player.getEffect(MobEffects.BERSERK);
        if (berserkEffect != null) {
            multiplier += (float) MobEffects.BerserkEffect.getDamageMultiplier(berserkEffect.getAmplifier());
            logDamage = true;
        }

        // Apply Devouring Strike: consume all Gluttony stacks for +2x damage per stack (multiplicative)
        MobEffectInstance devouringEffect = player.getEffect(MobEffects.DEVOURING_STRIKE);
        if (devouringEffect != null && gluttonyEffect != null) {
            // Consume Gluttony — Devouring Strike, not food depletion, so no Berserker
            endGluttony(player, gluttonyEffect.getAmplifier(), false);
            // Remove Devouring Strike
            ModEvents.removeModEffect(player, MobEffects.DEVOURING_STRIKE);
            logDamage = true;
        } else if (devouringEffect != null) {
            // No Gluttony to consume, just remove the buff
            ModEvents.removeModEffect(player, MobEffects.DEVOURING_STRIKE);
            logDamage = true;
        }

        damage *= (float) multiplier;

        // Devouring Strike: multiplicative, applied after other multipliers
        if (devouringEffect != null && gluttonyEffect != null) {
            int gluttonyLevel = gluttonyEffect.getAmplifier() + 1;
            double devouringMult = 1.0 + MobEffects.DevouringStrikeEffect.getDamageMultiplier(gluttonyLevel);
            damage *= (float) devouringMult;
        }

        // Gluttony: +20% damage per stack level (multiplicative, separate from other multipliers)
        if (gluttonyEffect != null && devouringEffect == null) {
            int stackLevel = gluttonyEffect.getAmplifier() + 1;
            damage *= (1.0f + stackLevel * 0.20f);
            logDamage = true;
        }

        if (damage != originalDamage) {
            event.setNewDamage(damage);
        }

        if (logDamage) {
            AzukaarSkillsStats.LOGGER.info("[Barbarian] {} did {} damage (base: {}, x{} total)",
                player.getName().getString(), damage, originalDamage, damage / originalDamage);
        }
    }

    /**
     * Handle incoming damage to player - Last Stand, Parry
     */
    @SubscribeEvent
    public static void onPlayerTakeDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Last Stand: invulnerable while effect is active
        if (player.hasEffect(MobEffects.LAST_STAND_EFFECT)) {
            event.setNewDamage(0);
            return;
        }

        // Last Stand: trigger at ≤1 heart while Exhausted
        if (player.hasEffect(MobEffects.EXHAUSTED)) {
            float healthAfterDamage = player.getHealth() - event.getNewDamage();
            if (healthAfterDamage <= 2.0f) {
                int lastStandLevel = PlayerData.getSkillLevel(player, LAST_STAND_SKILL);
                if (lastStandLevel > 0 && PlayerData.trySetCooldown(player, LAST_STAND_SKILL)) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.LAST_STAND_EFFECT, 300, 0, false, true, true // 15 seconds
                    ));
                    event.setNewDamage(0);

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            30, 1.0, 1.0, 1.0, 0.5);
                    }

                    AzukaarSkillsStats.LOGGER.info("[Barbarian] {} Last Stand triggered!",
                        player.getName().getString());
                    return;
                }
            }
        }

        // Parry: block chance with sword
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.is(ItemTags.SWORDS)) return;

        int parryLevel = PlayerData.getSkillLevel(player, PARRY_SKILL);
        if (parryLevel <= 0) return;

        double blockChance = SkillEffect.getSkillParameter(player, PARRY_SKILL, PARRY_EFFECT, "block_chance");

        if (player.getRandom().nextDouble() < blockChance) {
            event.setNewDamage(0);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);

            AzukaarSkillsStats.LOGGER.info("Player {} parried an attack ({}% chance)",
                player.getName().getString(), (int)(blockChance * 100));
        }
    }

    /**
     * Handle knockback - Crushing Blow adds knockback with axes
     */
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        // Find the attacker (last damage source)
        if (!(target.getLastDamageSource() != null &&
              target.getLastDamageSource().getEntity() instanceof Player player)) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.is(ItemTags.AXES)) return;

        int crushingBlowLevel = PlayerData.getSkillLevel(player, CRUSHING_BLOW_SKILL);
        if (crushingBlowLevel <= 0) return;

        // Add knockback bonus
        double knockbackBonus = SkillEffect.getSkillParameter(player, CRUSHING_BLOW_SKILL, CRUSHING_BLOW_EFFECT, "knockback");
        event.setStrength(event.getStrength() + (float) knockbackBonus);
    }

    /**
     * Gluttonous Weapon + Intimidating Presence - on kill effects
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // Check if killed by a player
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // Gluttonous Weapon: restore hunger on kill while Carnivorous
        if (player.hasEffect(MobEffects.CARNIVOROUS)) {
            int gluttonousWeaponLevel = PlayerData.getSkillLevel(player, GLUTTONOUS_WEAPON_SKILL);
            if (gluttonousWeaponLevel > 0) {
                int hungerRestore = (int) Math.floor(gluttonousWeaponLevel * 0.67);
                float saturationRestore = (gluttonousWeaponLevel * 0.67f) - hungerRestore;
                player.getFoodData().eat(hungerRestore, saturationRestore);
            }
        }

        // Intimidating Presence: apply Fear to nearby mobs on kill
        int intimidatingLevel = PlayerData.getSkillLevel(player, INTIMIDATING_PRESENCE_SKILL);
        if (intimidatingLevel > 0 && PlayerData.trySetCooldown(player, INTIMIDATING_PRESENCE_SKILL)) {
            AABB area = player.getBoundingBox().inflate(8.0);
            var mobs = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof Monster && entity.isAlive() && entity != event.getEntity());

            for (LivingEntity mob : mobs) {
                mob.addEffect(new MobEffectInstance(
                    MobEffects.FEAR, 40, 0, false, true, true // 2 seconds
                ));
            }

            if (!mobs.isEmpty()) {
                AzukaarSkillsStats.LOGGER.info("[Barbarian] {} Intimidating Presence feared {} mobs",
                    player.getName().getString(), mobs.size());
            }
        }
    }

    /**
     * Handle Quick Eating - instant food consumption
     */
    @SubscribeEvent
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Check if player has Quick Eating skill
        int quickEatingLevel = PlayerData.getSkillLevel(player, QUICK_EATING_SKILL);
        if (quickEatingLevel <= 0) return;

        // Only apply to food items
        ItemStack item = event.getItem();
        if (item.getFoodProperties(player) == null) return;

        // Set duration to 1 to finish eating instantly on next tick
        event.setDuration(1);
    }

    /**
     * Handle food consumption - Iron Stomach removes negative effects from raw meat
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int ironStomachLevel = PlayerData.getSkillLevel(player, IRON_STOMACH_SKILL);
        if (ironStomachLevel <= 0) return;

        // Remove negative effects that would be applied by unsafe foods
        // Schedule for next tick to ensure effects are already applied
        player.level().getServer().execute(() -> {
            // Remove Hunger effect (from rotten flesh)
            player.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);

            if (!event.getItem().is(UNSAFE_FOOD_TAG)) return;

            // add hunger/saturation bonus for Iron Stomach
            player.getFoodData().eat(2, 1.5f); // +2 hunger, +1.5 saturation


            // Remove Poison effect (from spider eye, pufferfish, poisonous potato)
            player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            // Remove Nausea effect (from pufferfish)
            player.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
        });

        // Meat Lover: +50% hunger from meat
        int meatLoverLevel = PlayerData.getSkillLevel(player, MEAT_LOVER_SKILL);
        if (meatLoverLevel > 0 && event.getItem().is(MEAT_TAG)) {
            var foodProps = event.getItem().getFoodProperties(player);
            if (foodProps != null) {
                // Add 50% bonus hunger
                int bonusHunger = (int) Math.ceil(foodProps.nutrition() * 0.5);
                float bonusSaturation = foodProps.saturation() * 0.5f;
                player.getFoodData().eat(bonusHunger, bonusSaturation);
            }
        }
    }

    /**
     * Handle Gluttony tick and Auto-Feed
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Gluttony hunger drain
        MobEffectInstance gluttonyEffect = player.getEffect(MobEffects.GLUTTONY);
        if (gluttonyEffect != null) {
            int amplifier = gluttonyEffect.getAmplifier();
            int tickRate = MobEffects.GluttonyEffect.getTickRate(amplifier);

            if (player.tickCount % tickRate == 0) {
                // Calculate drain reduction from Efficient Digestion (10% per level)
                int efficientDigestionLevel = PlayerData.getSkillLevel(player, EFFICIENT_DIGESTION_SKILL);
                double drainReduction = efficientDigestionLevel * 0.10; // 10% per level, max 30%

                // Carnivorous effect adds another 50% reduction
                if (player.hasEffect(MobEffects.CARNIVOROUS)) {
                    drainReduction += 0.50;
                }

                // Check if this drain should be skipped due to reduction
                if (drainReduction > 0 && player.getRandom().nextDouble() < drainReduction) {
                    // Drain skipped this tick
                } else {
                    // Drain 1 hunger point (half drumstick)
                    int currentHunger = player.getFoodData().getFoodLevel();

                    if (currentHunger > 0) {
                        player.getFoodData().setFoodLevel(currentHunger - 1);
                    } else {
                        // Hunger depleted - end Gluttony (Berserker can trigger)
                        endGluttony(player, amplifier, true);
                    }
                }
            }
        }

        // Auto-Feed: automatically eat from inventory when hunger drops below 14
        if (player.tickCount % 20 == 0) {
            int autoFeedLevel = PlayerData.getSkillLevel(player, AUTO_FEED_SKILL);
            if (autoFeedLevel > 0 && player.getFoodData().getFoodLevel() < 14) {
                ActiveEffects.consumeFoodFromInventory(player);
            }
        }

    }

    /**
     * Berserk expiry — apply Exhausted when Berserk effect expires naturally
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        var effect = event.getEffectInstance();
        if (effect != null && effect.getEffect() == MobEffects.BERSERK) {
            int storedAmp = player.getPersistentData().getInt("ass_berserk_gluttony_amp");
            applyExhausted(player, storedAmp);
            player.getPersistentData().remove("ass_berserk_gluttony_amp");

            AzukaarSkillsStats.LOGGER.info("[Barbarian] {} Berserk expired, now Exhausted",
                player.getName().getString());
        }
    }

    /**
     * End Gluttony effect — apply Berserk if skilled (only on food depletion), otherwise Exhausted
     * @param fromFoodDepletion true if Gluttony ended because food ran out, false if consumed by Devouring Strike
     */
    private static void endGluttony(Player player, int gluttonyAmplifier, boolean fromFoodDepletion) {
        ModEvents.removeModEffect(player, MobEffects.GLUTTONY);

        if (fromFoodDepletion) {
            int berserkerLevel = PlayerData.getSkillLevel(player, BERSERKER_SKILL);
            if (berserkerLevel > 0) {
                // Store gluttony amp for Exhausted after Berserk expires
                player.getPersistentData().putInt("ass_berserk_gluttony_amp", gluttonyAmplifier);

                // Apply Berserk for 10 seconds + Speed (FOV change)
                player.addEffect(new MobEffectInstance(
                    MobEffects.BERSERK, 200, berserkerLevel - 1, false, true, true
                ));
                player.addEffect(new MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 1, false, true, true
                ));

                // Second Wind: full heal on Berserk trigger
                int secondWindLevel = PlayerData.getSkillLevel(player, SECOND_WIND_SKILL);
                if (secondWindLevel > 0) {
                    player.setHealth(player.getMaxHealth());
                }

                // Sound
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.5f);

                AzukaarSkillsStats.LOGGER.info("[Barbarian] {} Berserker {} triggered! (Gluttony {} ended)",
                    player.getName().getString(), berserkerLevel, gluttonyAmplifier + 1);
                return;
            }
        }

        applyExhausted(player, gluttonyAmplifier);
    }

    /**
     * Apply Exhausted effect based on Gluttony level
     */
    private static void applyExhausted(Player player, int gluttonyAmplifier) {
        int exhaustedAmplifier = gluttonyAmplifier;
        int exhaustedDuration = (gluttonyAmplifier + 1) * 1200; // 1 min per level

        player.addEffect(new MobEffectInstance(
            MobEffects.EXHAUSTED, exhaustedDuration, exhaustedAmplifier, false, true, true
        ));

        AzukaarSkillsStats.LOGGER.info("[Barbarian] {} now Exhausted {} for {} seconds",
            player.getName().getString(), exhaustedAmplifier + 1, exhaustedDuration / 20);
    }
}
