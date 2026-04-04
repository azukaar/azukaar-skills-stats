package com.azukaar.ass.capabilities;

import java.util.Map;

import net.minecraft.world.entity.player.Player;

public interface IPlayerSkills {
    static final String MAIN = "azukaarskillsstats.main";

    static final int LINEAR_COMPONENT = 60;
    static final double POW_COMPONENT = 1.25;

    public static int getXpForSpecificLevel(int level) {
        if (level <= 3) return 250;
        return (int)(LINEAR_COMPONENT * Math.pow(level, POW_COMPONENT));
    }

    public static int getTotalXpForLevel(int level) {
        int totalXp = 0;
        for (int i = 1; i <= level; i++) {
            totalXp += getXpForSpecificLevel(i);
        }
        return totalXp;
    }

    public static int getLevelFromXp(int totalXp) {
        if (totalXp < LINEAR_COMPONENT) return 0;
        int low = 0;
        int high = 1000;
        while (low < high) {
            int mid = (low + high) / 2;
            int xpAtMid = getTotalXpForLevel(mid);
            if (xpAtMid < totalXp) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low - 1;
    }

    double getExperience(String pathName);
    void setExperience(String pathName, double experience);
    void addExperience(String pathName, double experience);
    double getLevel(String pathName);
    Map<String, Double> getAllExperience();
    void setAllExperience(Map<String, Double> experience);
    int getMainLevel();
    
    // Skill points methods
    int getSkillPoints();
    void setSkillPoints(int skillPoints);
    void addSkillPoints(int skillPoints);
    
    // Skills methods
    int getSkillLevel(String skillName);
    void setSkillLevel(String skillName, int level);
    void addSkillLevel(String skillName, int levels);
    Map<String, Integer> getAllSkills();
    void setAllSkills(Map<String, Integer> skills);
    void spendSkillPoints(int skillPoints, String skill);

    
    // Active skill slot management
    String getActiveSkillInSlot(int slotIndex);
    void setActiveSkillSlot(int slotIndex, String skillId);
    Map<Integer, String> getAllActiveSkillSlots();
    
    // Cooldown management  
    Long getSkillCooldown(String skillId);
    void setSkillCooldown(String skillId, long cooldownEndTime);
    Map<String, Long> getAllSkillCooldowns();
    
    // Utility methods
    boolean isSkillOnCooldown(String skillId, long currentTime);
    int getRemainingCooldown(String skillId, long currentTime);
}
