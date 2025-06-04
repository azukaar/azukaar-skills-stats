package com.azukaar.ass.api;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.api.events.ExperienceGainedEvent;
import com.azukaar.ass.api.events.LeveledUpEvent;
import com.azukaar.ass.api.events.SkillPointGained;
import com.azukaar.ass.api.events.SkillPointSpent;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.network.AddExperiencePacket;
import com.azukaar.ass.network.AddSkillPointPacket;
import com.azukaar.ass.network.SpendSkillPointPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

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

    public static int getSkillPoints(Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return 0;
        }
        return skills.getSkillPoints();
    }

    public static int getSkillLevel(Player player, String skillName) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return 0;
        }
        return skills.getSkillLevel(skillName);
    }

    // Public API methods that handle client/server logic
    public static double addExperience(String pathName, double experience, Player player, Vec3 position) {
        if (player.level().isClientSide) {
            // Send packet to server
            PacketDistributor.sendToServer(new AddExperiencePacket(pathName, experience, position));
            return experience; // Return expected value for client
        } else {
            // Execute server-side logic
            return addExperienceServerSide(pathName, experience, player, position);
        }
    }

    public static double addSkillPoint(double amount, Player player) {
        if (player.level().isClientSide) {
            // Send packet to server
            PacketDistributor.sendToServer(new AddSkillPointPacket(amount));
            return amount; // Return expected value for client
        } else {
            // Execute server-side logic
            return addSkillPointServerSide(amount, player);
        }
    }

    public static void spendSkillPoint(Player player, int amount, String skill) {
        if (player.level().isClientSide) {
            // Send packet to server
            PacketDistributor.sendToServer(new SpendSkillPointPacket(amount, skill));
        } else {
            // Execute server-side logic
            spendSkillPointServerSide(player, amount, skill);
        }
    }

    // Server-side only methods (called by packet handlers)
    public static double addExperienceServerSide(String pathName, double experience, Player player, Vec3 position) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS, null);

        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return 0;
        }

        double oldLevel = skills.getLevel(pathName);
        double oldMainLevel = getMainLevel(player);
        
        // Fire Pre event
        ExperienceGainedEvent.Pre preEvent = new ExperienceGainedEvent.Pre(
            player, pathName, experience, experience, position);

        if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
            return 0;
        }

        experience = preEvent.getAmount();

        skills.addExperience(pathName, experience);
        
        // Fire Post event
        double newLevel = skills.getLevel(pathName);
        double newMainLevel = getMainLevel(player);
        
        ExperienceGainedEvent.Post postEvent = new ExperienceGainedEvent.Post(
            player, pathName, experience, experience, position, oldLevel, newLevel);
        NeoForge.EVENT_BUS.post(postEvent);

        if(oldMainLevel < newMainLevel) {
            var leveledUpEvent = new LeveledUpEvent(player, newMainLevel-oldMainLevel, player.level());
            NeoForge.EVENT_BUS.post(leveledUpEvent);

            skills.addSkillPoints((int)(newMainLevel - oldMainLevel));
            
            var skillPointGainedEvent = new SkillPointGained(player, newMainLevel-oldMainLevel, player.level());
            NeoForge.EVENT_BUS.post(skillPointGainedEvent);
        }
                
        // Sync to client
        if (player instanceof ServerPlayer serverPlayer) {
            var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
            provider.syncToClient(serverPlayer);
        }

        return experience;
    }
    
    public static double addSkillPointServerSide(double amount, Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS, null);

        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return 0;
        }
        
        skills.addSkillPoints((int)amount);
        
        // Fire Post event
        NeoForge.EVENT_BUS.post(new SkillPointGained(player, amount, player.level()));

        // Sync to client
        if (player instanceof ServerPlayer serverPlayer) {
            var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
            provider.syncToClient(serverPlayer);
        }

        return amount;
    }

    public static void spendSkillPointServerSide(Player player, int amount, String skill) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS, null);

        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return;
        }

        if (skills.getSkillPoints() >= amount) {
            skills.setSkillPoints(skills.getSkillPoints() - amount);
            skills.addSkillLevel(skill, amount);

            // Fire Post event
            NeoForge.EVENT_BUS.post(new SkillPointSpent(player, amount, skill, player.level()));

            SkillDataManager.INSTANCE.updateSkillEffects(player, skill);

            // Sync to client
            if (player instanceof ServerPlayer serverPlayer) {
                var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                provider.syncToClient(serverPlayer);
            }
        } else {
            System.out.println("No skill points to spend for " + player.getName().getString());
        }
    }

    public static void getSkillLevels(Player player, String skill) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS, null);
        if (skills == null) {
            System.err.println("Player skills capability not found for " + player.getName().getString());
            return;
        }
        int level = skills.getSkillLevel(skill);
        System.out.println("Skill " + skill + " level for " + player.getName().getString() + ": " + level);
    }
}