package com.azukaar.ass.api;

import java.util.HashMap;
import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.resources.ResourceLocation;

public class AspectTypeRegistry {
    private static final Map<ResourceLocation, AspectType> ASPECT_TYPES = new HashMap<>();

    public static void register(AspectType type) {
        ASPECT_TYPES.put(type.getId(), type);
        AzukaarSkillsStats.LOGGER.info("Registered aspect type: {}", type.getId());
    }

    public static AspectType get(ResourceLocation id) {
        return ASPECT_TYPES.get(id);
    }

    public static AspectType get(String id) {
        return get(ResourceLocation.parse(id));
    }

    public static boolean exists(ResourceLocation id) {
        return ASPECT_TYPES.containsKey(id);
    }

    public static Map<ResourceLocation, AspectType> getAll() {
        return new HashMap<>(ASPECT_TYPES);
    }
}
