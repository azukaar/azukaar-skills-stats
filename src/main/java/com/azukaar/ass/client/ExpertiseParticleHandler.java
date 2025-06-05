package com.azukaar.ass.client;

import com.azukaar.ass.api.events.ExperienceGainedEvent;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.client.particles.OrbParticle;
import com.azukaar.ass.client.particles.OrbParticleOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Vector3f;

public class ExpertiseParticleHandler {
    
    // List to track active XP orb animations
    private static final List<XpOrbAnimation> activeAnimations = new ArrayList<>();
    private static final Random random = new Random();
    
    // Constants for XP scaling
    private static final float MIN_XP = 1.0f;
    private static final float MAX_XP = 50.0f;
    private static final float MIN_ALPHA = 0.15f;
    private static final float MAX_ALPHA = 0.45f;
    private static final float MIN_SIZE = 0.35f;
    private static final float MAX_SIZE = 0.9f;
    
    // Register to the event bus
    public static void init() {
        NeoForge.EVENT_BUS.register(ExpertiseParticleHandler.class);
    }
    
    @SubscribeEvent
    public static void onExpertiseGain(ExperienceGainedEvent.Post event) {
        if (event.getAmount() <= 0 || event.getExpertisePath() == IPlayerSkills.MAIN) {
            return; // No XP gained
        }
        
        if (event.hasLeveledUp()) {
            // Optional: Send message to player
            event.getPlayer().sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Leveled up in " + event.getExpertisePath() + " to level " + event.getNewLevel()));
        }

        // Only process if we're on the client side and it's the local player
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        // Check if this event is for the local player
        if (event.getPlayer().getUUID().equals(mc.player.getUUID())) {
            // Get the source position and player position
            Vec3 pos = event.getPosition();
            
            // Get path-specific base color
            int baseColor = getColorForPath(event.getExpertisePath());
            
            // Handle XP distribution
            float remainingXP = (float) event.getAmount();
            
            while (remainingXP > 0) {
                // Calculate XP for this orb (max 50 XP per orb)
                float orbXP = Math.min(remainingXP, MAX_XP);
                remainingXP -= orbXP;
                
                // Calculate orb properties based on XP amount
                OrbProperties orbProps = calculateOrbProperties(orbXP, baseColor);
                
                // Add random offset to starting position
                double offsetX = random.nextDouble() * 0.3 - 0.15;
                double offsetY = random.nextDouble() * 0.3 - 0.15;
                double offsetZ = random.nextDouble() * 0.3 - 0.15;
                
                Vec3 startPos = new Vec3(
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ
                );
                
                // Random velocity (smaller for larger orbs)
                double velocityScale = 4.0; // + (1.0 - orbProps.normalizedXP) * 3.0; // Smaller orbs move faster
                double velX = (random.nextDouble() - 0.5) * velocityScale;
                double velY = (random.nextDouble() - 0.5) * velocityScale;
                double velZ = (random.nextDouble() - 0.5) * velocityScale;

                Vec3 startVel = new Vec3(velX, velY, velZ);
                
                // Create and add the animation
                XpOrbAnimation anim = new XpOrbAnimation(
                    startPos, 
                    startVel,
                    mc.player,
                    200, // Duration in ticks
                    orbProps
                );
                
                synchronized (activeAnimations) {
                    activeAnimations.add(anim);
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        synchronized (activeAnimations) {
            Iterator<XpOrbAnimation> iterator = activeAnimations.iterator();
            while (iterator.hasNext()) {
                XpOrbAnimation anim = iterator.next();
                
                // Update the animation
                boolean finished = anim.update();
                
                // Remove if finished
                if (finished) {
                    iterator.remove();
                }
            }
        }
    }
    
    // Helper method to calculate orb properties based on XP amount
    private static OrbProperties calculateOrbProperties(float xpAmount, int baseColor) {
        // Clamp XP to our range
        float clampedXP = Math.max(MIN_XP, Math.min(MAX_XP, xpAmount));
        
        // Calculate normalized value (0.0 to 1.0)
        float normalizedXP = (clampedXP - MIN_XP) / (MAX_XP - MIN_XP);
        
        // Calculate alpha and size using linear interpolation
        float alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * normalizedXP;
        float size = MIN_SIZE + (MAX_SIZE - MIN_SIZE) * normalizedXP;
        
        // Calculate brightness-adjusted color
        int adjustedColor = adjustColorBrightness(baseColor, normalizedXP);
        
        return new OrbProperties(adjustedColor, alpha, size, normalizedXP);
    }
    
    // Helper method to adjust color brightness based on XP amount
    private static int adjustColorBrightness(int baseColor, float normalizedXP) {
        // Extract RGB components
        int red = (baseColor >> 16) & 0xFF;
        int green = (baseColor >> 8) & 0xFF;
        int blue = baseColor & 0xFF;
        
        // Brightness factor: 0.5 for low XP, 1.5 for high XP
        float brightnessFactor = 0.5f + (1.0f * normalizedXP);
        
        // Apply brightness (but don't exceed 255)
        red = Math.min(255, (int)(red * brightnessFactor));
        green = Math.min(255, (int)(green * brightnessFactor));
        blue = Math.min(255, (int)(blue * brightnessFactor));
        
        // Reconstruct color
        return (red << 16) | (green << 8) | blue;
    }
    
    // Helper method to get base color based on expertise path
    private static int getColorForPath(String path) {
        return switch(path) {
            case IPlayerSkills.WARRIOR_PATH -> 0xFF5555; // Red
            case IPlayerSkills.MINER_PATH -> 0x55FF55;  // Green
            case IPlayerSkills.EXPLORER_PATH -> 0x55FFFF; // Cyan
            default -> 0xFFFF55; // Yellow (default XP color)
        };
    }
    
    // Data class to hold orb properties
    private static class OrbProperties {
        final int color;
        final float alpha;
        final float size;
        final float normalizedXP; // For reference in animations
        
        OrbProperties(int color, float alpha, float size, float normalizedXP) {
            this.color = color;
            this.alpha = alpha;
            this.size = size;
            this.normalizedXP = normalizedXP;
        }
    }
    
    // Class to represent an XP orb animation
    private static class XpOrbAnimation {
        private Vec3 position;
        private Vec3 velocity;
        private final Player targetPlayer;
        private final int maxTicks;
        private int ticksExisted = 0;
        private final OrbProperties orbProps;
        
        public XpOrbAnimation(Vec3 startPos, Vec3 startVel, Player target, int duration, OrbProperties orbProps) {
            this.position = startPos;
            this.velocity = startVel;
            this.targetPlayer = target;
            this.maxTicks = duration;
            this.orbProps = orbProps;
        }
        
        public boolean update() {
            ticksExisted++;
            
            if (ticksExisted >= maxTicks) {
                return true; // Animation finished
            }
            
            // Calculate progress (0.0 to 1.0)
            float progress = (float) ticksExisted / maxTicks;
            
            // Ease-in-out curve for smoother animation
            double easedProgress = progress < 0.5 
                ? 2 * progress * progress 
                : 1 - Math.pow(-2 * progress + 2, 2) / 2;
            
            // Get player eye position for target
            Vec3 playerPos = targetPlayer.getEyePosition();
            playerPos = playerPos.add(0, -0.6, 0);
            
            // Update position logic
            Vec3 diff = playerPos.subtract(position);
            double length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length, 1.0 / length, 1.0 / length);
            }
            
            diff = diff.add(velocity);
            length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length, 1.0 / length, 1.0 / length);
            }
            
            // Larger orbs move slightly slower for visual appeal
            double baseAcceleration = 0.5 * (0.2 + easedProgress);
            double sizeModifier = 1.0 - (orbProps.normalizedXP * 0.1); // 20% slower for max size orbs
            baseAcceleration *= sizeModifier;
            
            double xAccel = baseAcceleration;
            double yAccel = baseAcceleration * 0.3;
            double zAccel = baseAcceleration;
            
            Vec3 movement = new Vec3(
                diff.x * xAccel,
                diff.y * yAccel,
                diff.z * zAccel
            );

            velocity = velocity.multiply(new Vec3(0.95, 0.95, 0.95));
            position = position.add(movement);
            
            // Spawn a particle at the current position every tick
            spawnParticleAtCurrentPosition();
            
            return position.distanceTo(playerPos) < 0.6;
        }
        
        private void spawnParticleAtCurrentPosition() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            
            // Create orb particle with calculated properties
            OrbParticleOptions orbOptions = new OrbParticleOptions(
                orbProps.color, 
                orbProps.alpha, 
                orbProps.size
            );
            
            // Spawn the particle at current position
            mc.level.addParticle(
                orbOptions,
                position.x, position.y, position.z,
                0, 0, 0
            );
        }
    }
}