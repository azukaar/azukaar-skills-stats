package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.ActiveSkillEffect;
import com.azukaar.ass.api.ActiveSkillEffectRegistry;
import com.azukaar.ass.api.AspectHelper;
import com.azukaar.ass.api.EffectData;
import com.azukaar.ass.api.PlayerData;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Active effects for the Medic skill tree
 */
public class ActiveEffects {

    private static final String TOUCH_HEAL_SKILL = "azukaarskillsstats:touch_heal";
    private static final String INFUSED_TOUCH_HEAL_SKILL = "azukaarskillsstats:infused_touch_heal";
    private static final String AOE_HEAL_SKILL = "azukaarskillsstats:aoe_heal";
    private static final String AOE_HEAL_EFFECT = "azukaarskillsstats:aoe_heal";
    private static final String INFUSED_AOE_HEAL_SKILL = "azukaarskillsstats:infused_aoe_heal";
    private static final String BREW_REGEN_SKILL = "azukaarskillsstats:brew_regen";
    private static final String BREW_RESISTANCE_SKILL = "azukaarskillsstats:brew_resistance";
    private static final String BREW_STRENGTH_SKILL = "azukaarskillsstats:brew_strength";
    private static final String BREW_O_PLENTY_SKILL = "azukaarskillsstats:brew_o_plenty";
    private static final String RADIANCE_POTION_SKILL = "azukaarskillsstats:radiance_potion";

    public static void registerAll() {
        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "touch_heal"),
            new TouchHealEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "aoe_heal"),
            new AoEHealEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "alchemys_boon"),
            new AlchemysBoonEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "cleanse"),
            new CleanseEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "cocktail"),
            new CocktailEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "overdose"),
            new OverdoseEffect()
        );
    }

    /**
     * Touch Heal — raycast to find target entity, heal it.
     * Triggers Radiance from healed entity's position.
     * Infused variant applies off-hand potion.
     */
    public static class TouchHealEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Raycast to find target entity (16 block reach)
            HitResult hitResult = player.pick(16.0D, 0.0F, false);

            // Entity raycast
            net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
            net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
            net.minecraft.world.phys.Vec3 endPos = eyePos.add(lookVec.scale(16.0));

            AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(16.0)).inflate(1.0);
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive(),
                16.0 * 16.0
            );

            if (entityHit == null) return false;

            LivingEntity target = (LivingEntity) entityHit.getEntity();
            float healAmount = effectData.getFloat("heal_amount", 8.0f);
            healAmount *= TreeEvents.getHealExpertMultiplier(player);

            // Undead: deal damage instead of healing, and trigger Radiance
            if (target.isInvertedHealAndHarm()) {
                float actualDamage = Math.min(healAmount, target.getHealth());
                target.hurt(player.damageSources().indirectMagic(player, player), actualDamage);
                TreeEvents.triggerRadianceFromHeal(player, target, actualDamage);

                // Grant Nature aspect XP (1 XP per HP damaged)
                if (actualDamage > 0) {
                    AspectHelper.awardXp("azukaarskillsstats:nature", player, actualDamage, target.position());
                }

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.02);
                }
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5f, 1.5f);
                return true;
            }

            // Only heal if target is missing health
            float missingHealth = target.getMaxHealth() - target.getHealth();
            if (missingHealth <= 0) return false;

            float actualHeal = Math.min(healAmount, missingHealth);
            target.heal(actualHeal);

            // Grant Nature aspect XP (1 XP per HP healed)
            if (actualHeal > 0) {
                AspectHelper.awardXp("azukaarskillsstats:nature", player, actualHeal, target.position());
            }

            // Infused Touch Heal — apply off-hand potion
            int infusedLevel = PlayerData.getSkillLevel(player, INFUSED_TOUCH_HEAL_SKILL);
            if (infusedLevel > 0) {
                TreeEvents.applyOffhandPotion(player, target);
            }

            // Trigger Radiance from the healed entity's position
            TreeEvents.triggerRadianceFromHeal(player, target, actualHeal);

            // Visual + sound feedback
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                    target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ(),
                    4, 0.3, 0.2, 0.3, 0.0);
            }
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);

            return true;
        }
    }

    /**
     * AoE Heal — heal all living entities (non-hostile) in radius.
     * Triggers Radiance from each healed entity's position.
     * Infused variant applies off-hand potion to all.
     */
    public static class AoEHealEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            float healAmount = effectData.getFloat("heal_amount", 6.0f);
            healAmount *= TreeEvents.getHealExpertMultiplier(player);
            double radius = effectData.getDouble("radius", 5.0);

            AABB area = player.getBoundingBox().inflate(radius);

            // Find all living entities in radius
            List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> entity.isAlive()
            );

            int infusedLevel = PlayerData.getSkillLevel(player, INFUSED_AOE_HEAL_SKILL);
            boolean affected = false;

            for (LivingEntity entity : entities) {
                // Undead: deal damage and trigger Radiance
                if (entity.isInvertedHealAndHarm()) {
                    float actualDamage = Math.min(healAmount, entity.getHealth());
                    entity.hurt(player.damageSources().indirectMagic(player, player), actualDamage);
                    TreeEvents.triggerRadianceFromHeal(player, entity, actualDamage);
                    AspectHelper.awardXp("azukaarskillsstats:nature", player, actualDamage, entity.position());
                    affected = true;

                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                            5, 0.3, 0.3, 0.3, 0.02);
                    }
                    continue;
                }

                // Skip hostile non-undead
                if (entity instanceof net.minecraft.world.entity.monster.Monster) continue;
                if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player) continue;

                float missingHealth = entity.getMaxHealth() - entity.getHealth();
                if (missingHealth <= 0) continue;

                float actualHeal = Math.min(healAmount, missingHealth);
                entity.heal(actualHeal);
                AspectHelper.awardXp("azukaarskillsstats:nature", player, actualHeal, entity.position());
                affected = true;

                // Infused AoE Heal — apply off-hand potion to each
                if (infusedLevel > 0) {
                    TreeEvents.applyOffhandPotion(player, entity);
                }

                // Trigger Radiance from each healed ally's position
                if (entity != player) {
                    TreeEvents.triggerRadianceFromHeal(player, entity, actualHeal);
                }

                // Heart particles on each healed entity
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.5, entity.getZ(),
                        3, 0.3, 0.2, 0.3, 0.0);
                }
            }

            if (!affected) return false;

            // Sound feedback
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);

            return true;
        }
    }

    /**
     * Cleanse — remove all negative effects from the player.
     * Triggers a Radiance pulse with damage = 3 × (amplifier + 1) per effect.
     * Reuses shared triggerCleanseRadiance from TreeEvents.
     */
    public static class CleanseEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Collect all harmful effects
            java.util.List<MobEffectInstance> harmful = new java.util.ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (!effect.getEffect().value().isBeneficial()
                    && effect.getEffect().value().getCategory() != net.minecraft.world.effect.MobEffectCategory.NEUTRAL) {
                    harmful.add(new MobEffectInstance(
                        effect.getEffect(), effect.getDuration(), effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()
                    ));
                }
            }

            if (harmful.isEmpty()) return false;

            // Remove all harmful effects
            for (MobEffectInstance effect : harmful) {
                player.removeEffect(effect.getEffect());
            }

            // Trigger Radiance pulse with cleansed effects
            TreeEvents.triggerCleanseRadiance(player, harmful);

            // Visual feedback
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
            }

            return true;
        }
    }

    /**
     * Alchemy's Boon — brew potions from nothing.
     * Always produces a Healing potion. Brew skills add additional potion types.
     * Brew o' plenty adds extra copies of each potion.
     */
    public static class AlchemysBoonEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            int extraCopies = PlayerData.getSkillLevel(player, BREW_O_PLENTY_SKILL);
            int copies = 1 + extraCopies;

            for (int i = 0; i < copies; i++) {
                // Always produce a Healing potion — tier scales with Alchemy's Boon level
                givePotion(player, createEffectPotion(MobEffects.HEAL, 1, skillLevel - 1,
                    "item.azukaarskillsstats.potion.healing_" + skillLevel));

                // Brew Regen — tier scales with level
                int regenLevel = PlayerData.getSkillLevel(player, BREW_REGEN_SKILL);
                if (regenLevel > 0) {
                    givePotion(player, createEffectPotion(MobEffects.REGENERATION, 900, regenLevel - 1,
                        "item.azukaarskillsstats.potion.regeneration_" + regenLevel));
                }

                // Brew Resistance — tier scales with level
                int resistanceLevel = PlayerData.getSkillLevel(player, BREW_RESISTANCE_SKILL);
                if (resistanceLevel > 0) {
                    givePotion(player, createEffectPotion(MobEffects.DAMAGE_RESISTANCE, 3600, resistanceLevel - 1,
                        "item.azukaarskillsstats.potion.resistance_" + resistanceLevel));
                }

                // Brew Strength — tier scales with level
                int strengthLevel = PlayerData.getSkillLevel(player, BREW_STRENGTH_SKILL);
                if (strengthLevel > 0) {
                    givePotion(player, createEffectPotion(MobEffects.DAMAGE_BOOST, 3600, strengthLevel - 1,
                        "item.azukaarskillsstats.potion.strength_" + strengthLevel));
                }

                // Radiance Potion — boosts Radiance damage and radius for 30s, tier scales with level
                int radiancePotionLevel = PlayerData.getSkillLevel(player, RADIANCE_POTION_SKILL);
                if (radiancePotionLevel > 0) {
                    givePotion(player, createEffectPotion(
                        com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY, 1800, radiancePotionLevel - 1,
                        "item.azukaarskillsstats.potion.radiance_" + radiancePotionLevel));
                }
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.0f);

            return true;
        }
    }

    /**
     * Cocktail — consume one of each potion type from inventory, triggering a Radiance burst.
     * Damage per type = 5 + (5 x potion amplifier). Radiance Potions are consumed first.
     */
    public static class CocktailEffect implements ActiveSkillEffect {
        private static final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>[] POTION_TYPES = new net.minecraft.core.Holder[] {
            MobEffects.HEAL, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE,
            MobEffects.DAMAGE_BOOST, com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY
        };

        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            float totalDamage = 0;
            int typesConsumed = 0;

            // First pass: find and consume Radiance Potion to apply boost before the burst
            int radianceSlot = findPotionSlotWithEffect(player, com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY);
            if (radianceSlot >= 0) {
                ItemStack stack = player.getInventory().getItem(radianceSlot);
                applyAndConsumePotionAt(player, radianceSlot);
                totalDamage += 5; // base damage for Radiance type
                typesConsumed++;
            }

            // Second pass: consume one of each remaining potion type, applying their effects
            for (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType : POTION_TYPES) {
                if (effectType == com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY) continue; // already handled
                int slot = findPotionSlotWithEffect(player, effectType);
                if (slot < 0) continue;

                int amplifier = getPotionAmplifier(player.getInventory().getItem(slot), effectType);
                applyAndConsumePotionAt(player, slot);
                totalDamage += 2 * (amplifier + 1);
                typesConsumed++;
            }

            if (typesConsumed == 0) return false;

            // Fire Radiance burst
            List<LivingEntity> targets = TreeEvents.findRadianceTargetsPublic(player, 10);
            if (!targets.isEmpty()) {
                TreeEvents.fireRadianceMissilesFrom(player,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    targets, totalDamage);
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20 * typesConsumed, 0.5, 0.5, 0.5, 0.1);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);

            return true;
        }
    }

    /**
     * Overdose — consume ALL potions in inventory for a massive Radiance burst.
     * Damage = 5 per potion consumed. Radiance Potions are consumed first.
     */
    public static class OverdoseEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // First pass: find and consume Radiance Potions to apply boost
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.POTION) && hasPotionEffect(stack, com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY)) {
                    applyAndConsumePotionAt(player, i);
                }
            }

            // Second pass: apply and consume all remaining potions
            float totalDamage = 0;
            int potionsConsumed = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(Items.POTION)) continue;
                var contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null) continue;

                // Find highest amplifier among effects for damage calc
                int maxAmplifier = 0;
                for (MobEffectInstance inst : contents.getAllEffects()) {
                    player.addEffect(new MobEffectInstance(
                        inst.getEffect(), inst.getDuration(), inst.getAmplifier(),
                        inst.isAmbient(), inst.isVisible(), inst.showIcon()
                    ));
                    maxAmplifier = Math.max(maxAmplifier, inst.getAmplifier());
                }
                int count = stack.getCount();
                totalDamage += count * (2 * (maxAmplifier + 1));
                player.getInventory().setItem(i, ItemStack.EMPTY);
                potionsConsumed += count;
            }

            if (potionsConsumed == 0) return false;

            // Fire massive Radiance burst
            List<LivingEntity> targets = TreeEvents.findRadianceTargetsPublic(player, 15);
            if (!targets.isEmpty()) {
                TreeEvents.fireRadianceMissilesFrom(player,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    targets, totalDamage);
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    30 + potionsConsumed * 5, 1.0, 1.0, 1.0, 0.15);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);

            return true;
        }
    }

    // ─── Shared Utilities ────────────────────────────────────────────

    static ItemStack createEffectPotion(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier, String nameKey) {
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.List.of(new MobEffectInstance(effect, duration, amplifier))
        ));
        potion.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey));
        return potion;
    }

    static void givePotion(Player player, ItemStack potion) {
        if (!player.getInventory().add(potion)) {
            ItemEntity dropped = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), potion);
            player.level().addFreshEntity(dropped);
        }
    }

    /**
     * Find the first inventory slot containing a potion with the given effect type.
     */
    private static int findPotionSlotWithEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.POTION) && hasPotionEffect(stack, effectType)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if a potion ItemStack contains the given effect type.
     */
    private static boolean hasPotionEffect(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (MobEffectInstance inst : contents.getAllEffects()) {
            if (inst.getEffect().value() == effectType.value()) return true;
        }
        return false;
    }

    /**
     * Get the amplifier of a specific effect type from a potion ItemStack.
     */
    private static int getPotionAmplifier(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return 0;
        for (MobEffectInstance inst : contents.getAllEffects()) {
            if (inst.getEffect().value() == effectType.value()) return inst.getAmplifier();
        }
        return 0;
    }

    /**
     * Consume one potion from the given inventory slot.
     */
    private static void consumePotionAt(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.getCount() > 1) {
            stack.shrink(1);
        } else {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Apply potion effects to the player and consume one from the given slot.
     */
    private static void applyAndConsumePotionAt(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            for (MobEffectInstance inst : contents.getAllEffects()) {
                player.addEffect(new MobEffectInstance(
                    inst.getEffect(), inst.getDuration(), inst.getAmplifier(),
                    inst.isAmbient(), inst.isVisible(), inst.showIcon()
                ));
            }
        }
        consumePotionAt(player, slot);
    }
}
