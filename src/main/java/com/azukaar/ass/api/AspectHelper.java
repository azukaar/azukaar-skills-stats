package com.azukaar.ass.api;

import com.azukaar.ass.SkillDataManager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AspectHelper {
    public static double awardXp(AspectDefinition definition, Player player, double baseXp, Vec3 position) {
        if (definition == null || !definition.isEnabled()) return 0;

        double xp = baseXp * definition.getXpMultiplier();

        return PlayerData.addExperience(definition.getId(), xp, player, position);
    }

    public static double awardXp(String aspectId, Player player, double baseXp, Vec3 position) {
        return awardXp(SkillDataManager.INSTANCE.getAspect(aspectId), player, baseXp, position);
    }

    public static double getProperty(String aspectId, String key, double defaultValue) {
        AspectDefinition def = SkillDataManager.INSTANCE.getAspect(aspectId);
        if (def == null) return defaultValue;
        return def.getDouble(key, defaultValue);
    }
}
