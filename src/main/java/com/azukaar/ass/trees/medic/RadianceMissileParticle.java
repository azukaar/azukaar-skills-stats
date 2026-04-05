package com.azukaar.ass.trees.medic;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * A golden orb trail particle for Radiance missiles.
 * Spawned each tick along the missile's path.
 * Pulses in size and shifts from bright white-gold core to warm amber as it fades.
 */
public class RadianceMissileParticle extends TextureSheetParticle {
    private final float initialAlpha;
    private float initialSize;
    private final float initialR, initialG, initialB;

    protected RadianceMissileParticle(ClientLevel level, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed,
                                      RadianceMissileOptions options, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.initialAlpha = options.getAlpha();
        this.initialSize = options.getSize();

        this.lifetime = 12;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;

        // Slight upward drift for a wispy feel
        this.xd = 0.0D;
        this.yd = 0.015D;
        this.zd = 0.0D;

        this.scale(initialSize);
        // Store the properly scaled quadSize for pulsing
        this.initialSize = this.quadSize;
        this.alpha = initialAlpha;

        // Start brighter than the base color (white-gold core)
        this.initialR = Math.min(1.0f, options.getColor().x() + 0.2f);
        this.initialG = Math.min(1.0f, options.getColor().y() + 0.15f);
        this.initialB = Math.min(1.0f, options.getColor().z() + 0.1f);
        this.rCol = initialR;
        this.gCol = initialG;
        this.bCol = initialB;

        this.pickSprite(sprites);
        this.setSize(0.15F, 0.15F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Drift upward
        this.y += this.yd;

        float ageRatio = (float) this.age / (float) this.lifetime;

        // Pulse size — grows slightly then shrinks
        float pulse = 1.0f + 0.3f * (float) Math.sin(ageRatio * Math.PI);
        this.quadSize = initialSize * pulse;

        // Shift from bright white-gold → warm amber as it fades
        this.rCol = initialR;
        this.gCol = initialG * (1.0f - ageRatio * 0.4f);
        this.bCol = initialB * (1.0f - ageRatio * 0.8f);

        // Fade out — fast after halfway
        if (ageRatio < 0.5f) {
            this.alpha = initialAlpha;
        } else {
            float fadeRatio = (ageRatio - 0.5f) / 0.5f;
            this.alpha = initialAlpha * (1.0f - fadeRatio);
        }
    }

    @Override
    public void move(double x, double y, double z) {
        // Only allow our controlled drift
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<RadianceMissileOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(RadianceMissileOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new RadianceMissileParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        }
    }
}
