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
    // --- XP formula API (delegates to IPlayerSkills) ---

    public static int getXpForSpecificLevel(int level) {
        return IPlayerSkills.getXpForSpecificLevel(level);
    }

    public static int getXpForMainLevel(int level) {
        return IPlayerSkills.getXpForMainLevel(level);
    }

    public static int getTotalXpForLevel(int level) {
        return IPlayerSkills.getTotalXpForLevel(level);
    }

    // --- Level and XP reads ---

    public static int getMainLevel(Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0;
        return skills.getMainLevel();
    }

    public static float getMainProgress(Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0f;

        double xp = skills.getExperience(IPlayerSkills.MAIN);
        int level = skills.getMainLevel();
        int xpNeeded = IPlayerSkills.getXpForMainLevel(level + 1);
        if (xpNeeded <= 0) return 1f;
        return Math.min(1f, (float)(xp / xpNeeded));
    }

    public static double getPathExperience(Player player, String pathName) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0;
        return skills.getExperience(pathName);
    }

    public static float getPathProgress(Player player, String pathName) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0f;

        double xp = skills.getExperience(pathName);
        int level = skills.getLevel(pathName);
        int cap = skills.getLevelCap();
        if (level >= cap) return 1f;
        int xpNeeded = IPlayerSkills.getScaledXpForLevel(level + 1, cap);
        if (xpNeeded <= 0) return 1f;
        return Math.min(1f, (float)(xp / xpNeeded));
    }

    public static int getScaledXpForLevel(Player player, int level) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return IPlayerSkills.getXpForSpecificLevel(level);
        return IPlayerSkills.getScaledXpForLevel(level, skills.getLevelCap());
    }

    public static int getLevelCap(Player player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return IPlayerSkills.BASE_CAP;
        return skills.getLevelCap();
    }

    public static int getPathLevel(Player player, String pathName) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) return 0;
        return skills.getLevel(pathName);
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

        // Update level cap from current difficulty
        if (!player.level().isClientSide()) {
            skills.setLevelCap(IPlayerSkills.getLevelCap(player.level().getDifficulty()));
        }

        int oldLevel = skills.getLevel(pathName);
        int oldMainLevel = skills.getMainLevel();

        // Fire Pre event
        ExperienceGainedEvent.Pre preEvent = new ExperienceGainedEvent.Pre(
            player, pathName, experience, experience, position);

        if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
            return 0;
        }

        experience = preEvent.getAmount();

        // Rate limiting (skip for main level)
        if (!IPlayerSkills.MAIN.equals(pathName)) {
            int counter = skills.getRateLimitCounter(pathName);
            experience *= (100 - counter) / 100.0;
            skills.incrementRateLimitCounter(pathName);
            skills.decrementOtherCounters(pathName, SkillDataManager.INSTANCE.getAspectIds());
        }

        skills.addExperience(pathName, experience);

        int newLevel = skills.getLevel(pathName);
        int levelsGained = newLevel - oldLevel;

        // Award main XP for each aspect level gained
        if (levelsGained > 0) {
            skills.addMainExperience(levelsGained);
        }

        int newMainLevel = skills.getMainLevel();

        // Fire Post event
        ExperienceGainedEvent.Post postEvent = new ExperienceGainedEvent.Post(
            player, pathName, experience, experience, position, oldLevel, newLevel);
        NeoForge.EVENT_BUS.post(postEvent);

        if (oldMainLevel < newMainLevel) {
            int mainLevelsGained = newMainLevel - oldMainLevel;
            var leveledUpEvent = new LeveledUpEvent(player, mainLevelsGained, player.level());
            NeoForge.EVENT_BUS.post(leveledUpEvent);

            skills.addSkillPoints(mainLevelsGained);

            var skillPointGainedEvent = new SkillPointGained(player, mainLevelsGained, player.level());
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