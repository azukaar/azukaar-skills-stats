package com.azukaar.ass.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AspectHelper {
    public static double awardXp(AspectDefinition definition, Player player, double baseXp, Vec3 position) {
        if (definition == null || !definition.isEnabled()) return 0;

        double xp = baseXp * definition.getXpMultiplier();

        return PlayerData.addExperience(definition.getId(), xp, player, position);
    }
}
