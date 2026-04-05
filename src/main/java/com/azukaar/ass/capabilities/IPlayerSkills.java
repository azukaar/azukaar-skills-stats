package com.azukaar.ass.capabilities;

import java.util.Map;

import net.minecraft.world.Difficulty;

public interface IPlayerSkills {
    static final String MAIN = "azukaarskillsstats.main";

    static final int LINEAR_COMPONENT = 300;
    static final double POW_COMPONENT = 0.70;
    
    // Difficulty → level cap
    static final int EASY_CAP = 70;
    static final int NORMAL_CAP = 85;
    static final int HARD_CAP = 100;
    static final int BASE_CAP = HARD_CAP;

    // Aspect XP formula (base, for cap 100)
    public static int getXpForSpecificLevel(int level) {
        if (level <= 3) return 300;
        return (int)(LINEAR_COMPONENT * Math.pow(level, POW_COMPONENT));
    }

    // Scaled XP for a given cap — same total XP to reach cap regardless of difficulty
    public static int getScaledXpForLevel(int level, int cap) {
        if (cap >= BASE_CAP) return getXpForSpecificLevel(level);
        double capTotal = getLevelCapTotalXp(cap);
        if (capTotal <= 0) return getXpForSpecificLevel(level);
        return (int)(getXpForSpecificLevel(level) * (double) HARD_TOTAL_XP / capTotal);
    }


    // Pre-calculated total XP costs for each cap
    static final int EASY_TOTAL_XP = getTotalXpForLevel(EASY_CAP);
    static final int NORMAL_TOTAL_XP = getTotalXpForLevel(NORMAL_CAP);
    static final int HARD_TOTAL_XP = getTotalXpForLevel(HARD_CAP);

    public static int getLevelCap(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> EASY_CAP;
            case NORMAL -> NORMAL_CAP;
            case HARD -> HARD_CAP;
        };
    }

    public static int getLevelCapTotalXp(int cap) {
        if (cap <= EASY_CAP) return EASY_TOTAL_XP;
        if (cap <= NORMAL_CAP) return NORMAL_TOTAL_XP;
        return HARD_TOTAL_XP;
    }

    // Main level XP formula: how many aspect level-ups to reach the next main level
    public static int getXpForMainLevel(int level) {
        int res = (int)(Math.pow(level, 0.27));
        return Math.max(2, res);
    }

    // Utility: total cumulative base XP to reach a given level
    static int getTotalXpForLevel(int level) {
        int totalXp = 0;
        for (int i = 1; i <= level; i++) {
            totalXp += getXpForSpecificLevel(i);
        }
        return totalXp;
    }

    // Level cap (per-player, set from difficulty)
    int getLevelCap();
    void setLevelCap(int cap);

    // Rate limiting (not serialized, resets on relog)
    int getRateLimitCounter(String aspectId);
    void incrementRateLimitCounter(String aspectId);
    void decrementOtherCounters(String aspectId, java.util.Collection<String> allAspectIds);
    void decayAllCounters();

    // Aspect XP and levels
    double getExperience(String pathName);
    void setExperience(String pathName, double experience);
    void addExperience(String pathName, double experience);
    int getLevel(String pathName);
    void setLevel(String pathName, int level);
    Map<String, Double> getAllExperience();
    void setAllExperience(Map<String, Double> experience);
    Map<String, Integer> getAllLevels();
    void setAllLevels(Map<String, Integer> levels);

    // Main level
    int getMainLevel();
    void addMainExperience(double xp);

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
