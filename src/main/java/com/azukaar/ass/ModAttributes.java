package com.azukaar.ass;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = 
        DeferredRegister.create(Registries.ATTRIBUTE, AzukaarSkillsStats.MODID);
    
    // Hunger efficiency: 0.0 = normal hunger depletion, 1.0 = no hunger depletion
    public static final DeferredHolder<Attribute, Attribute> HUNGER_EFFICIENCY = ATTRIBUTES.register(
        "hunger_efficiency",
        () -> new RangedAttribute(
            "attribute.name.azukaarskillsstats.hunger_efficiency",
            0.0, // default value
            0.0, // min value 
            1.0  // max value (100% efficiency = no hunger loss)
        )
    );
    
    // Mining speed multiplier: 0.0 = normal speed, 1.0 = 2x speed, etc.
    public static final DeferredHolder<Attribute, Attribute> MINING_SPEED = ATTRIBUTES.register(
        "mining_speed",
        () -> new RangedAttribute(
            "attribute.name.azukaarskillsstats.mining_speed",
            0.0,    // default value
            0.0,    // min value
            100.0    // max value (up to 100x mining speed)
        )
    );
}