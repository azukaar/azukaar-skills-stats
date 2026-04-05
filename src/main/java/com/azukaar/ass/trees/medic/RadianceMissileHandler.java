package com.azukaar.ass.trees.medic;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side handler for Radiance homing missiles.
 * All clients animate the missile; only the owning client sends the hit packet.
 */
public class RadianceMissileHandler {

    private static final List<MissileAnimation> activeMissiles = new ArrayList<>();
    private static final Vector3f RADIANCE_COLOR = new Vector3f(1.0f, 0.85f, 0.2f);

    /**
     * Called from NetworkHandler when a RadianceMissileSpawnPayload arrives.
     */
    public static void onMissileSpawn(RadianceMissileSpawnPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity targetEntity = mc.level.getEntity(payload.targetEntityId());
        if (targetEntity == null) return;

        boolean isOwner = (mc.player != null && mc.player.getId() == payload.sourcePlayerId());
        Vec3 startPos = new Vec3(payload.startX(), payload.startY(), payload.startZ());

        synchronized (activeMissiles) {
            activeMissiles.add(new MissileAnimation(
                startPos, targetEntity, payload.damage(), isOwner, payload.carriedEffect()
            ));
        }
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        synchronized (activeMissiles) {
            Iterator<MissileAnimation> it = activeMissiles.iterator();
            while (it.hasNext()) {
                MissileAnimation missile = it.next();
                if (missile.update()) {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (activeMissiles) {
            activeMissiles.clear();
        }
    }

    private static class MissileAnimation {
        private Vec3 position;
        private Vec3 velocity;
        private final Entity target;
        private final float damage;
        private final boolean isOwner;
        private final RadianceMissileSpawnPayload.CarriedEffect carriedEffect;
        private int ticksExisted = 0;
        private boolean hitSent = false;

        private static final int MAX_TICKS = 100;
        private static final double HIT_THRESHOLD = 0.8;
        private static final double MAX_RANGE = 32.0;

        MissileAnimation(Vec3 startPos, Entity target, float damage, boolean isOwner,
                         RadianceMissileSpawnPayload.CarriedEffect carriedEffect) {
            this.position = startPos;
            this.target = target;
            this.damage = damage;
            this.isOwner = isOwner;
            this.carriedEffect = carriedEffect;

            // Random initial velocity — creates the curved arc
            double velocityScale = 3.0;
            this.velocity = new Vec3(
                (Math.random() - 0.5) * velocityScale,
                (Math.random() - 0.5) * velocityScale,
                (Math.random() - 0.5) * velocityScale
            );
        }

        boolean update() {
            ticksExisted++;

            if (ticksExisted >= MAX_TICKS) return true;
            if (target == null || !target.isAlive() || target.isRemoved()) return true;

            // Target center position
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
            double distance = position.distanceTo(targetPos);

            if (distance > MAX_RANGE) return true;

            // Ease-in-out progress
            float progress = (float) ticksExisted / MAX_TICKS;
            double easedProgress = progress < 0.5
                ? 2 * progress * progress
                : 1 - Math.pow(-2 * progress + 2, 2) / 2;

            // Direction to target (normalized)
            Vec3 diff = targetPos.subtract(position);
            double length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length, 1.0 / length, 1.0 / length);
            }

            // Blend direction with current velocity (velocity curves the path)
            diff = diff.add(velocity);
            length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length, 1.0 / length, 1.0 / length);
            }

            // Acceleration increases over time (starts slow, speeds up)
            double baseAcceleration = 0.3 * (0.12 + easedProgress);

            Vec3 movement = new Vec3(
                diff.x * baseAcceleration,
                diff.y * baseAcceleration * 0.3,
                diff.z * baseAcceleration
            );

            // Decay the random velocity so it curves toward target over time
            velocity = velocity.multiply(0.95, 0.95, 0.95);
            position = position.add(movement);

            // Spawn trail particle
            spawnTrailParticle();

            // Check collision
            double newDistance = position.distanceTo(targetPos);
            if (newDistance < HIT_THRESHOLD) {
                // Owning client sends hit packet to server with carried effect
                if (isOwner && !hitSent) {
                    hitSent = true;
                    PacketDistributor.sendToServer(
                        new RadianceMissileHitPacket(target.getId(), damage, carriedEffect)
                    );
                }
                // All clients see impact burst
                spawnImpactBurst();
                return true;
            }

            return false;
        }

        private void spawnTrailParticle() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            // Bright core particle (white-gold, larger)
            mc.level.addParticle(
                new RadianceMissileOptions(new Vector3f(1.0f, 0.95f, 0.6f), 0.8f, 0.5f),
                position.x, position.y, position.z, 0, 0, 0
            );

            // Warm outer glow (amber, slightly offset)
            double ox = (Math.random() - 0.5) * 0.15;
            double oy = (Math.random() - 0.5) * 0.15;
            double oz = (Math.random() - 0.5) * 0.15;
            mc.level.addParticle(
                new RadianceMissileOptions(RADIANCE_COLOR, 0.5f, 0.35f),
                position.x + ox, position.y + oy, position.z + oz, 0, 0, 0
            );

            // Dimmer trailing sparkles every other tick
            if (ticksExisted % 2 == 0) {
                for (int i = 0; i < 2; i++) {
                    double sx = (Math.random() - 0.5) * 0.3;
                    double sy = (Math.random() - 0.5) * 0.3;
                    double sz = (Math.random() - 0.5) * 0.3;
                    mc.level.addParticle(
                        new RadianceMissileOptions(new Vector3f(1.0f, 0.7f, 0.1f), 0.3f, 0.15f),
                        position.x + sx, position.y + sy, position.z + sz, 0, 0, 0
                    );
                }
            }

            // Carried effect — potion particle above the missile, drifting upward (after 0.5s delay)
            if (carriedEffect != null && ticksExisted > 20) {
                int c = carriedEffect.color() | 0xFF000000; // ensure full alpha
                double ex = (Math.random() - 0.5) * 0.4;
                double ez = (Math.random() - 0.5) * 0.4;
                mc.level.addParticle(
                    net.minecraft.core.particles.ColorParticleOption.create(
                        net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, c),
                    position.x + ex, position.y + 0.3 + Math.random() * 0.3, position.z + ez,
                    0, 0.05, 0
                );
            }
        }

        private void spawnImpactBurst() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            // Bright flash at center
            mc.level.addParticle(
                new RadianceMissileOptions(new Vector3f(1.0f, 1.0f, 0.8f), 1.0f, 0.8f),
                position.x, position.y, position.z, 0, 0, 0
            );

            // Ring of golden sparks
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 0.6;
                double oy = (Math.random() - 0.5) * 0.6;
                double oz = (Math.random() - 0.5) * 0.6;
                float size = 0.2f + (float) Math.random() * 0.25f;
                mc.level.addParticle(
                    new RadianceMissileOptions(RADIANCE_COLOR, 0.9f, size),
                    position.x + ox, position.y + oy, position.z + oz, 0, 0, 0
                );
            }
        }
    }
}
