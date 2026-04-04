package com.azukaar.ass.aspect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.AspectHelper;
import com.azukaar.ass.api.AspectType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class CombatAspect implements AspectType {
    private static AspectDefinition definition;

    private static long expiryTicks;
    private static long cleanupIntervalTicks;
    private static double minDamageThreshold;
    private static double hostileMultiplier;
    private static double passiveMultiplier;

    private static final Map<UUID, MobDamageRecord> damageRecords = new HashMap<>();
    private static long tickCounter = 0;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.parse("azukaarskillsstats:combat");
    }

    @Override
    public void onLoad(AspectDefinition def) {
        definition = def;
        expiryTicks = def.getLong("expiry_ticks", 0);
        cleanupIntervalTicks = def.getLong("cleanup_interval_ticks", 0);
        minDamageThreshold = def.getDouble("min_damage_threshold", 0);
        hostileMultiplier = def.getDouble("hostile_multiplier", 0);
        passiveMultiplier = def.getDouble("passive_multiplier", 0);
    }

    @Override
    public void onUnload() {
        definition = null;
        damageRecords.clear();
        tickCounter = 0;
    }

    private static class MobDamageRecord {
        final Map<UUID, Double> playerDamage = new HashMap<>();
        long lastDamageTick;

        MobDamageRecord(long tick) {
            this.lastDamageTick = tick;
        }

        void addDamage(UUID playerId, double amount, long tick) {
            playerDamage.merge(playerId, amount, Double::sum);
            lastDamageTick = tick;
        }

        boolean isExpired(long currentTick) {
            return currentTick - lastDamageTick > expiryTicks;
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Post event) {
        if (definition == null) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        UUID mobId = target.getUUID();

        float healthBefore = Math.min(target.getHealth() + event.getNewDamage(), target.getMaxHealth());
        float effectiveDamage = Math.min(event.getNewDamage(), healthBefore);

        damageRecords
            .computeIfAbsent(mobId, k -> new MobDamageRecord(tickCounter))
            .addDamage(attacker.getUUID(), effectiveDamage, tickCounter);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (definition == null) return;
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide()) return;
        if (mob instanceof Player) return;

        MobDamageRecord record = damageRecords.remove(mob.getUUID());
        if (record == null) return;

        double mobMultiplier = (mob instanceof Monster) ? hostileMultiplier : passiveMultiplier;
        double baseXp = mob.getMaxHealth() * mobMultiplier;
        ServerLevel level = (ServerLevel) mob.level();

        for (Map.Entry<UUID, Double> entry : record.playerDamage.entrySet()) {
            Player player = level.getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            double totalTrackedDamage = record.playerDamage.values().stream()
                .mapToDouble(Double::doubleValue).sum();
            if (totalTrackedDamage < mob.getMaxHealth() * minDamageThreshold) continue;
            if (totalTrackedDamage < mob.getMaxHealth()) totalTrackedDamage = mob.getMaxHealth();
            double share = entry.getValue() / totalTrackedDamage;
            double xp = baseXp * share;

            double awarded = AspectHelper.awardXp(definition, player, xp, mob.position().add(0, 1, 0));
            System.out.println("[CombatAspect] Player " + player.getName().getString()
                + " dealt " + entry.getValue().intValue() + " damage to mob, earning " + (int) awarded + " XP");
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (definition == null) return;
        tickCounter++;
        if (tickCounter % cleanupIntervalTicks != 0) return;
        
        Iterator<MobDamageRecord> it = damageRecords.values().iterator();
        while (it.hasNext()) {
            if (it.next().isExpired(tickCounter)) {
                it.remove();
            }
        }
    }
}
