package com.azukaar.ass.api;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.events.ExperienceGainedEvent;
import com.azukaar.ass.capabilities.IPlayerSkills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class PlayerData {
    public static int getMainLevel(Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0;

        int totalLevel = 0;
        for (String pathName : IPlayerSkills.PATH_NAMES) {
            totalLevel += skills.getLevel(pathName);
        }
        return getMainLevelFromPathsTotal(totalLevel);
    }

    public static int getPathLevel(Player player, String pathName) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            System.out.println("CAP NOT FOUND");
            return 0;
        }

        return (int)skills.getLevel(pathName);
    }

    public static int getMainLevelFromPathsTotal(int totalLevel) {
        int mainExperience = 0;
        for (int i = 0; i < totalLevel; i++) {
            mainExperience += Math.pow(i, 0.75) * 150;
        }
        if (totalLevel == 1) return 1;
        return Math.min(IPlayerSkills.getLevelFromXp(mainExperience), totalLevel);
    }

    public static double addExperience(String pathName, double experience, Player player, Vec3 position) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS, null);

        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return 0;
        }

        double oldLevel = skills.getLevel(pathName);
        boolean isMain = pathName.equals("ass.main");
        
        // Fire Pre event
        if (!isMain) {
            ExperienceGainedEvent.Pre preEvent = new ExperienceGainedEvent.Pre(
                player, pathName, experience, experience, position);

            if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
                return 0;
            }

            experience = preEvent.getAmount();
        }


        skills.addExperience(pathName, experience);
        
        // Fire Post event
        if (!isMain) {
            double newLevel = skills.getLevel(pathName);
            ExperienceGainedEvent.Post postEvent = new ExperienceGainedEvent.Post(
                player, pathName, experience, experience, position, oldLevel, newLevel);
            NeoForge.EVENT_BUS.post(postEvent);
        }
        
        // Sync to client
        if (player instanceof ServerPlayer serverPlayer) {
            var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
            provider.syncToClient(serverPlayer);
        }

        return experience;
    }
}
