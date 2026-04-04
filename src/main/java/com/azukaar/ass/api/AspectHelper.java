package com.azukaar.ass.api;

import com.azukaar.ass.api.events.AspectXpAwardEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class AspectHelper {
    public static double awardXp(AspectDefinition definition, Player player, double baseXp, Vec3 position) {
        if (definition == null || !definition.isEnabled()) return 0;

        double xp = baseXp * definition.getXpMultiplier();

        AspectXpAwardEvent event = new AspectXpAwardEvent(player, definition, xp, position);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return 0;
        }

        xp = event.getXpAmount();

        return PlayerData.addExperience(definition.getId(), xp, player, position);
    }
}
