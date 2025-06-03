package com.azukaar.ass.client;

import com.azukaar.ass.api.events.ExperienceGainedEvent;
import net.minecraft.client.Minecraft;
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
    
    // Register to the event bus
    public static void init() {
        NeoForge.EVENT_BUS.register(ExpertiseParticleHandler.class);
    }
    
    @SubscribeEvent
    public static void onExpertiseGain(ExperienceGainedEvent.Post event) {
        // if (!event.getLevel().isClientSide()) {
        //     return;
        // }

        if (event.getAmount() <= 0) {
            return; // No XP gained
        }

        // is player the local player?
        
        
        // Optional: Send message to player
        // event.getPlayer().sendSystemMessage(net.minecraft.network.chat.Component.literal(
        //     "+" + event.getAmount() + " XP in " + event.getExpertisePath() + " ("+event.getExpertiseAmount()+"XP)"));

        // TODO : NEED TO ADD LEVEL + EXPERTISE LEVEL 
        
        if (event.hasLeveledUp()) {
            // Optional: Send message to player
            event.getPlayer().sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Leveled up in " + event.getExpertisePath() + " to level " + event.getNewLevel()));
        }

        // System.out.println("XP Gained: " + event.getAmount() + " in " + event.getExpertisePath() + " ("+event.getExpertiseAmount()+"XP)");

        // Only process if we're on the client side and it's the local player
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        // Check if this event is for the local player
        if (event.getPlayer().getUUID().equals(mc.player.getUUID())) {
            // Get the source position and player position
            Vec3 pos = event.getPosition();
            
            // Create orb animations based on XP amount (minimum 1, maximum 10)
            int orbCount = Math.min(10, Math.max(1, (int)(event.getAmount() / 10.0)));
            
            // Get path-specific color
            int color = getColorForPath(event.getExpertisePath());
            
            for (int i = 0; i < orbCount; i++) {
                // Add random offset to starting position
                double offsetX = random.nextDouble() * 0.3 - 0.15;
                double offsetY = random.nextDouble() * 0.3 - 0.15;
                double offsetZ = random.nextDouble() * 0.3 - 0.15;
                
                Vec3 startPos = new Vec3(
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ
                );
                
                // random velocity
                double velX = (random.nextDouble() - 0.5) * 4;
                double velY = (random.nextDouble() - 0.5) * 4;
                double velZ = (random.nextDouble() - 0.5) * 4;

                // Spawn the initial particle
                Vec3 startVel = new Vec3(velX, velY, velZ);
                
                // Create and add the animation
                XpOrbAnimation anim = new XpOrbAnimation(
                    startPos, 
                    startVel,
                    mc.player,
                    200, // max Duration in ticks (1-2 seconds)
                    color
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
    
    // Helper method to get color based on expertise path
    private static int getColorForPath(String path) {
        return switch(path) {
            case "ass.warrior" -> 0xFF5555; // Red
            case "ass.archer" -> 0x55FF55;  // Green
            case "ass.miner" -> 0xFFAA00;   // Gold
            case "ass.explorer" -> 0x55FFFF; // Cyan
            default -> 0xFFFF55; // Yellow (default XP color)
        };
    }
    
    // Class to represent an XP orb animation
    private static class XpOrbAnimation {
        private Vec3 position;
        private Vec3 velocity;
        private final Player targetPlayer;
        private final int maxTicks;
        private int ticksExisted = 0;
        private final int color;
        
        public XpOrbAnimation(Vec3 startPos, Vec3 startVel, Player target, int duration, int color) {
            this.position = startPos;
            this.velocity = startVel;
            this.targetPlayer = target;
            this.maxTicks = duration;
            this.color = color;
        }
        
        /**
         * Updates the animation
         * @return true if the animation is finished
         */
        public boolean update() {
            ticksExisted++;
            
            if (ticksExisted >= maxTicks) {
                return true; // Animation finished
            }
            
            // Calculate progress (0.0 to 1.0)
            float progress = (float) ticksExisted / maxTicks;
            
            // Ease-in-out curve for smoother animation
            // Slow start, fast middle, slow end
            double easedProgress = progress < 0.5 
                ? 2 * progress * progress 
                : 1 - Math.pow(-2 * progress + 2, 2) / 2;
            
            // Get player eye position for target
            Vec3 playerPos = targetPlayer.getEyePosition();
            playerPos = playerPos.add(0, -0.6, 0); // Adjust Y position
            
            // Interpolate position with much slower acceleration
            Vec3 diff = playerPos.subtract(position);
            
            // Normalize the difference vector
            double length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length,1.0 / length,1.0 / length); // Normalize
            }
            
            // Apply velocity to movement
            diff = diff.add(velocity);

            // Normalize the difference vector
            length = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
            if (length > 0) {
                diff = diff.multiply(1.0 / length,1.0 / length,1.0 / length); // Normalize
            }
            
            // Reduce the acceleration factor significantly (from 0.15 to 0.03)
            // And make Y-axis movement even slower
            double baseAcceleration = 0.5 * (0.2 + easedProgress);
            
            // Apply different acceleration rates for each axis
            double xAccel = baseAcceleration;
            double yAccel = baseAcceleration * 0.3; // Y-axis is 50% slower
            double zAccel = baseAcceleration;
            
            // Create movement vector with different acceleration per axis
            Vec3 movement = new Vec3(
                diff.x * xAccel,
                diff.y * yAccel,
                diff.z * zAccel
            );

            // Apply some damping to the velocity
            velocity = velocity.multiply(new Vec3(0.95, 0.95, 0.95));
            
            // Update position
            position = position.add(movement);
            
            // Spawn particles
            spawnParticles();
            
            // Check if we're close enough to the player to finish early
            return position.distanceTo(playerPos) < 0.6;
        }

        
        private void spawnParticles() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            
            // Extract RGB components (TODO)
            float red = ((color >> 16) & 0xFF) / 255.0F;
            float green = ((color >> 8) & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;

            // Create the dust particle options with color and size
            float size = 1.0F; // Size of the particle
            Vector3f colorVector = new Vector3f(red, green, blue);
            DustParticleOptions dustOptions = new DustParticleOptions(colorVector, size);
            
            // Spawn the particle
            mc.level.addAlwaysVisibleParticle(
                dustOptions,
                position.x, position.y, position.z,
                0, 0, 0 // No velocity
            );
            
            // Trail particles (smaller, less frequent)
            // if (random.nextFloat() < 0.3f) {
            //     mc.level.addParticle(
            //         net.minecraft.core.particles.ParticleTypes.END_ROD,
            //         position.x, position.y, position.z,
            //         (random.nextFloat() - 0.5f) * 0.05f,
            //         (random.nextFloat() - 0.5f) * 0.05f,
            //         (random.nextFloat() - 0.5f) * 0.05f
            //     );
            // }
        }
    }
}
