
package com.azukaar.ass.api;

import java.util.HashMap;
import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.resources.ResourceLocation;

public class ActiveSkillEffectRegistry {
    private static final Map<ResourceLocation, ActiveSkillEffect> ACTIVE_EFFECTS = new HashMap<>();
    
    public static void register(ResourceLocation id, ActiveSkillEffect effect) {
        ACTIVE_EFFECTS.put(id, effect);
        AzukaarSkillsStats.LOGGER.info("Registered active skill effect: {}", id);
    }
    
    public static ActiveSkillEffect get(ResourceLocation id) {
        return ACTIVE_EFFECTS.get(id);
    }
    
    public static ActiveSkillEffect get(String id) {
        return get(ResourceLocation.parse(id));
    }
    
    public static boolean exists(ResourceLocation id) {
        return ACTIVE_EFFECTS.containsKey(id);
    }
    
    public static Map<ResourceLocation, ActiveSkillEffect> getAll() {
        return new HashMap<>(ACTIVE_EFFECTS);
    }
}