package com.azukaar.ass.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class OrbParticle extends TextureSheetParticle {
    private final float initialAlpha;
    private final float initialSize;

    protected OrbParticle(ClientLevel level, double x, double y, double z, 
                         double xSpeed, double ySpeed, double zSpeed, 
                         OrbParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        
        this.initialAlpha = options.getAlpha();
        this.initialSize = options.getSize();
        
        // Set particle properties - KEY CHANGES HERE
        this.lifetime = 5; // Very short lifetime (5 ticks) since we spawn new ones each tick
        this.hasPhysics = false; // DISABLE PHYSICS to prevent dispersion
        this.friction = 1.0F; // No friction loss
        this.gravity = 0.0F; // No gravity
        
        // IMPORTANT: Set velocities to zero to prevent drifting
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        
        // Set initial size and alpha
        this.scale(initialSize);
        this.alpha = initialAlpha;
        
        // Set RGB color
        this.rCol = options.getColor().x();
        this.gCol = options.getColor().y();
        this.bCol = options.getColor().z();
        
        // Set the sprite
        this.pickSprite(sprites);
        
        // Set bounding box
        this.setSize(0.2F, 0.2F);
    }

    @Override
    public void tick() {
        // Store previous position
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        // Increment age
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // DON'T call super.tick() - this prevents the default physics behavior
        // that causes random movement and dispersion
        
        // Keep velocities at zero to prevent any movement
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        
        // Optional: Fade out over time (uncomment if desired)
        float ageRatio = (float) this.age / (float) this.lifetime;
        this.alpha = initialAlpha * (1.0F - ageRatio);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // Alternative approach: If you want the particle to move in a controlled way,
    // you can override the move method to prevent random dispersion
    @Override
    public void move(double x, double y, double z) {
        // Don't move at all, or implement your own controlled movement
        // super.move(x, y, z); // Comment this out to prevent movement
    }

    // Particle provider factory
    public static class Provider implements ParticleProvider<OrbParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(OrbParticleOptions options, ClientLevel level, 
                                     double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            return new OrbParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        }
    }
}