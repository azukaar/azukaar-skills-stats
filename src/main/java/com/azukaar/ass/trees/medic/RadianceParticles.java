package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Particle type registration for the Medic skill tree.
 */
public class RadianceParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, AzukaarSkillsStats.MODID);

    public static final Supplier<ParticleType<RadianceMissileOptions>> RADIANCE_MISSILE =
        PARTICLE_TYPES.register("radiance_missile", () -> new ParticleType<RadianceMissileOptions>(false) {
            @Override
            public MapCodec<RadianceMissileOptions> codec() {
                return RadianceMissileOptions.CODEC.fieldOf("radiance_missile");
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, RadianceMissileOptions> streamCodec() {
                return StreamCodec.of(
                    (buffer, options) -> options.writeToNetwork(buffer),
                    (buffer) -> RadianceMissileOptions.fromNetwork(this, buffer)
                );
            }
        });
}
