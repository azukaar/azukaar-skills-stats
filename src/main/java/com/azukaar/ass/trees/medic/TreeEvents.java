package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Event handlers for the Medic skill tree
 */
public class TreeEvents {

    private static final String REACTIVE_HEAL_SKILL = "azukaarskillsstats:reactive_heal";
    private static final String REACTIVE_HEAL_EFFECT = "azukaarskillsstats:reactive_heal";
    private static final String UNDYING_RESOLVE_SKILL = "azukaarskillsstats:undying_resolve";
    private static final String UNDYING_RESOLVE_EFFECT = "azukaarskillsstats:undying_resolve";
    private static final String INFUSED_SELF_HEAL_SKILL = "azukaarskillsstats:infused_self_heal";
    private static final String RADIANCE_SKILL = "azukaarskillsstats:radiance";
    private static final String RADIANCE_ON_HEAL = "azukaarskillsstats:radiance_on_heal";
    private static final String RADIANCE_ON_REGEN = "azukaarskillsstats:radiance_on_regen";
    private static final String ALCHEMY_SKILL = "azukaarskillsstats:alchemy";
    private static final String HARDY_SKILL = "azukaarskillsstats:hardy";
    private static final String HARDY_EFFECT = "azukaarskillsstats:hardy";
    private static final String HEAL_EXPERT_SKILL = "azukaarskillsstats:heal_expert";
    private static final String HEAL_EXPERT_EFFECT = "azukaarskillsstats:heal_expert";
    private static final String BLINDING_LIGHT_SKILL = "azukaarskillsstats:blinding_light";
    private static final String REACTIVE_CLEANSE_SKILL = "azukaarskillsstats:reactive_cleanse";
    private static final String VENOMOUS_REBOUND_SKILL = "azukaarskillsstats:venomous_rebound";
    private static final String RADIANCE_AMPLIFIER_SKILL = "azukaarskillsstats:radiance_amplifier";
    private static final String RADIANCE_AMPLIFIER_EFFECT = "azukaarskillsstats:radiance_amplifier";

    private static final float RADIANCE_RANGE = 32.0f;
    private static final Vector3f RADIANCE_COLOR = new Vector3f(1.0f, 0.85f, 0.2f);
    private static boolean alchemyGuard = false;

    // ─── Heal Expert ─────────────────────────────────────────────────

    /**
     * Get the Heal Expert potency multiplier (1.0 if no Heal Expert).
     * Used by Touch Heal, AoE Heal, and Reactive Heal to scale healing.
     */
    public static float getHealExpertMultiplier(Player player) {
        double bonus = SkillEffect.getSkillParameter(player, HEAL_EXPERT_SKILL, HEAL_EXPERT_EFFECT, "heal_potency_bonus");
        return (float) (1.0 + bonus);
    }

    // ─── Shared Utilities ────────────────────────────────────────────

    /**
     * Apply the potion effects from the healer's off-hand item to a target,
     * without consuming the potion.
     * @return true if a potion was applied
     */
    public static boolean applyOffhandPotion(Player healer, LivingEntity target) {
        ItemStack offhand = healer.getOffhandItem();
        if (offhand.isEmpty()) return false;

        var potionContents = offhand.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (potionContents == null) return false;

        boolean applied = false;
        for (MobEffectInstance effect : potionContents.getAllEffects()) {
            target.addEffect(new MobEffectInstance(
                effect.getEffect(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
            ));
            applied = true;
        }

        return applied;
    }

    /**
     * Fire Radiance missiles from a specific source position.
     * Used by Touch Heal / AoE Heal to fire from the healed entity's position.
     */
    public static void fireRadianceMissilesFrom(Player healer, double sourceX, double sourceY, double sourceZ,
                                                 List<LivingEntity> targets, float damage) {
        fireRadianceMissilesFrom(healer, sourceX, sourceY, sourceZ, targets, damage, null);
    }

    public static void fireRadianceMissilesFrom(Player healer, double sourceX, double sourceY, double sourceZ,
                                                 List<LivingEntity> targets, float damage,
                                                 RadianceMissileSpawnPayload.CarriedEffect carriedEffect) {
        if (!(healer instanceof ServerPlayer serverPlayer)) return;

        damage *= getRadianceDamageMultiplier(healer);

        for (LivingEntity target : targets) {
            RadianceMissileSpawnPayload payload = new RadianceMissileSpawnPayload(
                healer.getId(), target.getId(), damage, sourceX, sourceY, sourceZ, carriedEffect
            );

            PacketDistributor.sendToPlayer(serverPlayer, payload);
            PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, payload);
        }
    }

    /**
     * Trigger Radiance missiles from a healed entity's position.
     * Called by Touch Heal / AoE Heal after healing an ally.
     */
    public static void triggerRadianceFromHeal(Player healer, LivingEntity healedEntity, float healAmount) {
        int radianceLevel = PlayerData.getSkillLevel(healer, RADIANCE_SKILL);
        if (radianceLevel <= 0) return;

        double damagePercent = SkillEffect.getSkillParameter(healer, RADIANCE_SKILL, RADIANCE_ON_HEAL, "damage_percent");
        float damage = (float) (healAmount * damagePercent);
        if (damage <= 0) return;

        int maxTargets = (int) SkillEffect.getSkillParameter(healer, RADIANCE_SKILL, RADIANCE_ON_HEAL, "max_targets");
        List<LivingEntity> targets = findRadianceTargetsFrom(healer, healedEntity, maxTargets);
        if (targets.isEmpty()) return;

        double sx = healedEntity.getX();
        double sy = healedEntity.getY() + healedEntity.getBbHeight() / 2.0;
        double sz = healedEntity.getZ();

        fireRadianceMissilesFrom(healer, sx, sy, sz, targets, damage);
    }

    /**
     * Find angry mobs near a specific entity (not necessarily the healer).
     */
    private static List<LivingEntity> findRadianceTargetsFrom(Player healer, LivingEntity center, int maxTargets) {
        float range = getEffectiveRange(healer);
        AABB area = center.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = healer.level().getEntitiesOfClass(
            LivingEntity.class, area,
            entity -> entity != healer && entity != center && entity.isAlive() &&
                (entity instanceof Monster || entity.getLastAttacker() == healer ||
                 (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == healer))
        );

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return candidates.subList(0, Math.min(maxTargets, candidates.size()));
    }

    // ─── Event Handlers ──────────────────────────────────────────────

    /**
     * Reactive Heal - apply Regeneration when player takes damage.
     * Infused Self Heal - also applies off-hand potion.
     */
    @SubscribeEvent
    public static void onPlayerTakeDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int reactiveHealLevel = PlayerData.getSkillLevel(player, REACTIVE_HEAL_SKILL);
        if (reactiveHealLevel <= 0) return;
        if (!PlayerData.trySetCooldown(player, REACTIVE_HEAL_SKILL)) return;

        int amplifier = (int) SkillEffect.getSkillParameter(player, REACTIVE_HEAL_SKILL, REACTIVE_HEAL_EFFECT, "regen_amplifier") - 1;

        int duration = (int) SkillEffect.getSkillParameter(player, REACTIVE_HEAL_SKILL, REACTIVE_HEAL_EFFECT, "regen_duration");
        int durationBonus = (int) SkillEffect.getSkillParameter(player, UNDYING_RESOLVE_SKILL, UNDYING_RESOLVE_EFFECT, "duration_bonus");
        duration += durationBonus;
        duration = (int) (duration * getHealExpertMultiplier(player));

        player.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION, duration, amplifier, false, true, true
        ));

        // Infused Self Heal — apply off-hand potion to self
        int infusedSelfHealLevel = PlayerData.getSkillLevel(player, INFUSED_SELF_HEAL_SKILL);
        if (infusedSelfHealLevel > 0) {
            applyOffhandPotion(player, player);
        }
    }

    /**
     * Radiance burst — triggered when ANY Regeneration effect is applied to the player.
     */
    @SubscribeEvent
    public static void onRegenApplied(MobEffectEvent.Added event) {
        if (event.getOldEffectInstance() != null) return; // only trigger on fresh application
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (event.getEffectInstance().getEffect() != MobEffects.REGENERATION) return;

        int radianceLevel = PlayerData.getSkillLevel(player, RADIANCE_SKILL);
        if (radianceLevel <= 0) return;

        double burstPercent = SkillEffect.getSkillParameter(player, RADIANCE_SKILL, RADIANCE_ON_REGEN, "burst_percent");
        float damage = (float) (player.getMaxHealth() * burstPercent);
        if (damage <= 0) return;

        int maxTargets = (int) SkillEffect.getSkillParameter(player, RADIANCE_SKILL, RADIANCE_ON_REGEN, "max_targets");
        List<LivingEntity> targets = findRadianceTargets(player, maxTargets);
        if (targets.isEmpty()) return;

        fireRadianceMissiles(player, targets, damage);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /**
     * Alchemy — extend beneficial potion durations by 20% per level.
     */
    @SubscribeEvent
    public static void onAlchemyEffectApplied(MobEffectEvent.Added event) {
        if (alchemyGuard) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int alchemyLevel = PlayerData.getSkillLevel(player, ALCHEMY_SKILL);
        if (alchemyLevel <= 0) return;

        MobEffectInstance instance = event.getEffectInstance();
        if (!instance.getEffect().value().isBeneficial()) return;

        int originalDuration = instance.getDuration();
        int extendedDuration = (int) (originalDuration * (1.0 + 0.2 * alchemyLevel));
        if (extendedDuration <= originalDuration) return;

        alchemyGuard = true;
        player.addEffect(new MobEffectInstance(
            instance.getEffect(),
            extendedDuration,
            instance.getAmplifier(),
            instance.isAmbient(),
            instance.isVisible(),
            instance.showIcon()
        ));
        alchemyGuard = false;
    }

    /**
     * Radiance per-heal trigger — each heal > 2 HP fires missiles.
     */
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int radianceLevel = PlayerData.getSkillLevel(player, RADIANCE_SKILL);
        if (radianceLevel <= 0) return;

        float actualHeal = Math.min(event.getAmount(), player.getMaxHealth() - player.getHealth());
        if (actualHeal <= 2.0f) return;

        double damagePercent = SkillEffect.getSkillParameter(player, RADIANCE_SKILL, RADIANCE_ON_HEAL, "damage_percent");
        float damage = (float) (actualHeal * damagePercent);
        if (damage <= 0) return;

        int maxTargets = (int) SkillEffect.getSkillParameter(player, RADIANCE_SKILL, RADIANCE_ON_HEAL, "max_targets");
        List<LivingEntity> targets = findRadianceTargets(player, maxTargets);
        if (targets.isEmpty()) return;

        fireRadianceMissiles(player, targets, damage);
    }

    // ─── Hardy ───────────────────────────────────────────────────────

    /**
     * Hardy — grant absorption hearts when the player heals (piggybacking on the existing onPlayerHeal).
     * Has its own 1min cooldown separate from Radiance.
     */
    @SubscribeEvent
    public static void onHealForHardy(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int hardyLevel = PlayerData.getSkillLevel(player, HARDY_SKILL);
        if (hardyLevel <= 0) return;
        if (!PlayerData.trySetCooldown(player, HARDY_SKILL)) return;

        int absorptionAmount = (int) SkillEffect.getSkillParameter(player, HARDY_SKILL, HARDY_EFFECT, "absorption_amount");
        if (absorptionAmount <= 0) return;

        // Grant absorption hearts (2 HP per heart)
        float currentAbsorption = player.getAbsorptionAmount();
        player.setAbsorptionAmount(currentAbsorption + absorptionAmount * 2);
    }

    /**
     * Hardy — also trigger on Regeneration application (regen counts as healing trigger).
     */
    @SubscribeEvent
    public static void onRegenForHardy(MobEffectEvent.Added event) {
        if (event.getOldEffectInstance() != null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (event.getEffectInstance().getEffect() != MobEffects.REGENERATION) return;

        int hardyLevel = PlayerData.getSkillLevel(player, HARDY_SKILL);
        if (hardyLevel <= 0) return;
        if (!PlayerData.trySetCooldown(player, HARDY_SKILL)) return;

        int absorptionAmount = (int) SkillEffect.getSkillParameter(player, HARDY_SKILL, HARDY_EFFECT, "absorption_amount");
        if (absorptionAmount <= 0) return;

        float currentAbsorption = player.getAbsorptionAmount();
        player.setAbsorptionAmount(currentAbsorption + absorptionAmount * 2);
    }

    // ─── Cleanse / Reactive Cleanse (shared logic) ──────────────────

    /**
     * Shared Cleanse Radiance logic — used by both active Cleanse and passive Reactive Cleanse.
     * Calculates damage as 3 × (amplifier + 1) per effect, fires Radiance missiles.
     * If Venomous Rebound is active, applies the removed debuffs to hit targets.
     */
    public static void triggerCleanseRadiance(Player player, List<MobEffectInstance> removedEffects) {
        int radianceLevel = PlayerData.getSkillLevel(player, RADIANCE_SKILL);
        if (radianceLevel <= 0) return;

        int maxTargets = (int) SkillEffect.getSkillParameter(player, RADIANCE_SKILL, RADIANCE_ON_REGEN, "max_targets");
        if (maxTargets <= 0) maxTargets = 10;
        List<LivingEntity> targets = findRadianceTargets(player, maxTargets);
        if (targets.isEmpty()) return;

        boolean hasRebound = PlayerData.getSkillLevel(player, VENOMOUS_REBOUND_SKILL) > 0;

        // Fire one Radiance pulse per cleansed effect
        for (MobEffectInstance effect : removedEffects) {
            float damage = 3.0f * (effect.getAmplifier() + 1);

            RadianceMissileSpawnPayload.CarriedEffect carried = null;
            if (hasRebound) {
                String effectId = effect.getEffect().unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("");
                int color = effect.getEffect().value().getColor();
                carried = new RadianceMissileSpawnPayload.CarriedEffect(
                    effectId, effect.getDuration(), effect.getAmplifier(), color
                );
            }

            fireRadianceMissilesFrom(player, player.getX(), player.getY() + 1.0, player.getZ(),
                targets, damage, carried);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    /**
     * Reactive Cleanse — automatically cleanse harmful effects as they are applied.
     * Triggers a Radiance pulse with the same damage formula as Cleanse.
     */
    @SubscribeEvent
    public static void onDebuffApplied(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int reactiveCleanseLevel = PlayerData.getSkillLevel(player, REACTIVE_CLEANSE_SKILL);
        if (reactiveCleanseLevel <= 0) return;

        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect().value().isBeneficial()) return;
        if (instance.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.NEUTRAL) return;

        if (!PlayerData.trySetCooldown(player, REACTIVE_CLEANSE_SKILL)) return;

        // Snapshot the effect and defer removal to next tick (can't remove during Added event)
        List<MobEffectInstance> removed = new ArrayList<>();
        removed.add(new MobEffectInstance(
            instance.getEffect(), instance.getDuration(), instance.getAmplifier(),
            instance.isAmbient(), instance.isVisible(), instance.showIcon()
        ));
        var effectHolder = instance.getEffect();

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                player.removeEffect(effectHolder);
                triggerCleanseRadiance(player, removed);
            });
        }
    }

    // ─── Radiance Internals ──────────────────────────────────────────

    /**
     * Find Radiance Potency effect on the player, comparing by value to avoid Holder mismatch.
     */
    private static MobEffectInstance findRadiancePotency(Player player) {
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect().value() == com.azukaar.ass.trees.medic.MobEffects.RADIANCE_POTENCY.value()) {
                return inst;
            }
        }
        return null;
    }

    /**
     * Get the effective Radiance range, accounting for Radiance Potency effect.
     */
    private static float getEffectiveRange(Player player) {
        var potency = findRadiancePotency(player);
        if (potency != null) {
            return RADIANCE_RANGE * com.azukaar.ass.trees.medic.MobEffects.RadiancePotencyEffect.getMultiplier(potency.getAmplifier());
        }
        return RADIANCE_RANGE;
    }

    /**
     * Get the Radiance damage multiplier, accounting for Radiance Potency effect.
     */
    static float getRadianceDamageMultiplier(Player player) {
        float multiplier = 1.0f;

        // Radiance Potency effect (from potion)
        var potency = findRadiancePotency(player);
        if (potency != null) {
            multiplier *= com.azukaar.ass.trees.medic.MobEffects.RadiancePotencyEffect.getMultiplier(potency.getAmplifier());
        }

        // Radiance Amplifier skill (passive multiplier)
        double ampMultiplier = SkillEffect.getSkillParameter(player, RADIANCE_AMPLIFIER_SKILL, RADIANCE_AMPLIFIER_EFFECT, "damage_multiplier");
        if (ampMultiplier > 0) {
            multiplier *= (float) ampMultiplier;
        }

        return multiplier;
    }

    /**
     * Public accessor for Cocktail/Overdose to find Radiance targets.
     */
    public static List<LivingEntity> findRadianceTargetsPublic(Player player, int maxTargets) {
        return findRadianceTargets(player, maxTargets);
    }

    /**
     * Find up to maxTargets closest angry mobs near the player.
     */
    private static List<LivingEntity> findRadianceTargets(Player player, int maxTargets) {
        float range = getEffectiveRange(player);
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
            LivingEntity.class, area,
            entity -> entity != player && entity.isAlive() &&
                (entity instanceof Monster || entity.getLastAttacker() == player ||
                 (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player))
        );

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return candidates.subList(0, Math.min(maxTargets, candidates.size()));
    }

    /**
     * Fire Radiance missiles from the player's position.
     */
    private static void fireRadianceMissiles(Player player, List<LivingEntity> targets, float damage) {
        fireRadianceMissilesFrom(player, player.getX(), player.getY() + 1.0, player.getZ(), targets, damage);
    }

    /**
     * Server-side handler for missile hit packets from clients.
     */
    public static void handleMissileHit(ServerPlayer player, RadianceMissileHitPacket packet) {
        int radianceLevel = PlayerData.getSkillLevel(player, RADIANCE_SKILL);
        if (radianceLevel <= 0) return;

        Entity entity = player.level().getEntity(packet.targetEntityId());
        if (!(entity instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        double distance = player.distanceTo(target);
        if (distance > RADIANCE_RANGE + 10.0) return;

        AzukaarSkillsStats.LOGGER.debug("[Radiance] {} hit {} for {} damage",
            player.getName().getString(), target.getName().getString(), packet.damage());
        target.hurt(player.damageSources().indirectMagic(player, player), packet.damage());

        // Venomous Rebound — apply carried effect from the missile
        var carried = packet.carriedEffect();
        if (carried != null && PlayerData.getSkillLevel(player, VENOMOUS_REBOUND_SKILL) > 0) {
            var effectLoc = net.minecraft.resources.ResourceLocation.parse(carried.effectId());
            var mobEffect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(effectLoc).orElse(null);
            if (mobEffect != null) {
                target.addEffect(new MobEffectInstance(mobEffect, carried.duration(), carried.amplifier(), false, true, true));
            }
        }

        // Blinding Light — apply Blindness only when missile carries no effect
        if (carried == null && PlayerData.getSkillLevel(player, BLINDING_LIGHT_SKILL) > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            double tx = target.getX();
            double ty = target.getY() + target.getBbHeight() / 2.0;
            double tz = target.getZ();

            serverLevel.sendParticles(
                new RadianceMissileOptions(RADIANCE_COLOR, 0.9f, 0.6f),
                tx, ty, tz, 12, 0.3, 0.3, 0.3, 0.1
            );
        }
    }
}
