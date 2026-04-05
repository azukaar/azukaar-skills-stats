package com.azukaar.ass.trees.medic;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

public class RadianceMissileOptions implements ParticleOptions {
    public static final Codec<RadianceMissileOptions> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.FLOAT.fieldOf("red").forGetter(options -> options.color.x()),
            Codec.FLOAT.fieldOf("green").forGetter(options -> options.color.y()),
            Codec.FLOAT.fieldOf("blue").forGetter(options -> options.color.z()),
            Codec.FLOAT.fieldOf("alpha").forGetter(options -> options.alpha),
            Codec.FLOAT.fieldOf("size").forGetter(options -> options.size)
        ).apply(instance, (r, g, b, a, s) -> new RadianceMissileOptions(new Vector3f(r, g, b), a, s))
    );

    public static RadianceMissileOptions fromNetwork(ParticleType<RadianceMissileOptions> type, FriendlyByteBuf buffer) {
        float red = buffer.readFloat();
        float green = buffer.readFloat();
        float blue = buffer.readFloat();
        float alpha = buffer.readFloat();
        float size = buffer.readFloat();
        return new RadianceMissileOptions(new Vector3f(red, green, blue), alpha, size);
    }

    private final Vector3f color;
    private final float alpha;
    private final float size;

    public RadianceMissileOptions(Vector3f color, float alpha, float size) {
        this.color = color;
        this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        this.size = Math.max(0.1F, size);
    }

    public RadianceMissileOptions(int rgbColor, float alpha, float size) {
        this(new Vector3f(
            ((rgbColor >> 16) & 0xFF) / 255.0F,
            ((rgbColor >> 8) & 0xFF) / 255.0F,
            (rgbColor & 0xFF) / 255.0F
        ), alpha, size);
    }

    public Vector3f getColor() { return color; }
    public float getAlpha() { return alpha; }
    public float getSize() { return size; }

    @Override
    public ParticleType<?> getType() {
        return RadianceParticles.RADIANCE_MISSILE.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(color.x());
        buffer.writeFloat(color.y());
        buffer.writeFloat(color.z());
        buffer.writeFloat(alpha);
        buffer.writeFloat(size);
    }
}
