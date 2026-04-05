
package com.azukaar.ass.capabilities;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkills implements IPlayerSkills {
    private Map<String, Integer> aspectLevels = new HashMap<>();
    private Map<String, Double> xpInLevel = new HashMap<>();
    private int levelCap = IPlayerSkills.BASE_CAP;
    private int skillPoints = 0;
    private Map<String, Integer> skills = new HashMap<>();

    private Map<Integer, String> activeSkillSlots = new HashMap<>();
    private Map<String, Long> skillCooldowns = new HashMap<>();
    private Map<String, Integer> rateLimitCounters = new HashMap<>();

    @Override
    public int getLevelCap() { return levelCap; }

    @Override
    public void setLevelCap(int cap) { this.levelCap = cap; }

    @Override
    public int getRateLimitCounter(String aspectId) {
        return rateLimitCounters.getOrDefault(aspectId, 0);
    }

    @Override
    public void incrementRateLimitCounter(String aspectId) {
        int val = Math.min(100, getRateLimitCounter(aspectId) + 2);
        rateLimitCounters.put(aspectId, val);
    }

    @Override
    public void decrementOtherCounters(String aspectId, java.util.Collection<String> allAspectIds) {
        for (String id : allAspectIds) {
            if (!id.equals(aspectId)) {
                int val = Math.max(0, getRateLimitCounter(id) - 1);
                rateLimitCounters.put(id, val);
            }
        }
    }

    @Override
    public void decayAllCounters() {
        rateLimitCounters.replaceAll((k, v) -> Math.max(0, v - 1));
    }

    @Override
    public double getExperience(String pathName) {
        return xpInLevel.getOrDefault(pathName, 0.0);
    }

    @Override
    public void setExperience(String pathName, double exp) {
        xpInLevel.put(pathName, exp);
    }

    @Override
    public void addExperience(String pathName, double exp) {
        double current = getExperience(pathName);
        double total = current + exp;
        int level = getLevel(pathName);

        int rateCounter = getRateLimitCounter(pathName);
        System.out.println("[PlayerSkills] Adding " + exp + " XP to " + pathName
            + " (current: " + current + ", total: " + total + ", level: " + level
            + ", rate-limit: " + rateCounter + "% reduction)");

        // Level up with overflow, respecting cap
        int xpNeeded = IPlayerSkills.getScaledXpForLevel(level + 1, levelCap);
        while (total >= xpNeeded && level < levelCap) {
            total -= xpNeeded;
            level++;
            xpNeeded = IPlayerSkills.getScaledXpForLevel(level + 1, levelCap);
        }

        // At cap, discard overflow
        if (level >= levelCap) {
            total = 0;
        }

        aspectLevels.put(pathName, level);
        xpInLevel.put(pathName, total);
    }

    @Override
    public int getLevel(String pathName) {
        return aspectLevels.getOrDefault(pathName, 0);
    }

    @Override
    public void setLevel(String pathName, int level) {
        aspectLevels.put(pathName, level);
        xpInLevel.put(pathName, 0.0);
    }

    @Override
    public Map<String, Double> getAllExperience() {
        return new HashMap<>(xpInLevel);
    }

    @Override
    public void setAllExperience(Map<String, Double> experience) {
        this.xpInLevel = new HashMap<>(experience);
    }

    @Override
    public Map<String, Integer> getAllLevels() {
        return new HashMap<>(aspectLevels);
    }

    @Override
    public void setAllLevels(Map<String, Integer> levels) {
        this.aspectLevels = new HashMap<>(levels);
    }

    @Override
    public int getMainLevel() {
        return getLevel(IPlayerSkills.MAIN);
    }

    @Override
    public void addMainExperience(double xp) {
        double current = xpInLevel.getOrDefault(IPlayerSkills.MAIN, 0.0);
        double total = current + xp;
        int level = aspectLevels.getOrDefault(IPlayerSkills.MAIN, 0);

        int xpNeeded = IPlayerSkills.getXpForMainLevel(level + 1);
        while (total >= xpNeeded) {
            total -= xpNeeded;
            level++;
            xpNeeded = IPlayerSkills.getXpForMainLevel(level + 1);
        }

        aspectLevels.put(IPlayerSkills.MAIN, level);
        xpInLevel.put(IPlayerSkills.MAIN, total);
    }

    // Skill points implementation
    @Override
    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    @Override
    public void addSkillPoints(int skillPoints) {
        this.skillPoints += skillPoints;
    }

    @Override
    public void spendSkillPoints(int skillPoints, String skill) {
        this.skillPoints -= skillPoints;
        addSkillLevel(skill, skillPoints);
    }

    // Skills implementation
    @Override
    public int getSkillLevel(String skillName) {
        return skills.getOrDefault(skillName, 0);
    }

    @Override
    public void setSkillLevel(String skillName, int level) {
        skills.put(skillName, level);
    }

    @Override
    public void addSkillLevel(String skillName, int levels) {
        int current = getSkillLevel(skillName);
        setSkillLevel(skillName, current + levels);
    }

    @Override
    public Map<String, Integer> getAllSkills() {
        return new HashMap<>(skills);
    }

    @Override
    public void setAllSkills(Map<String, Integer> skills) {
        this.skills = new HashMap<>(skills);
    }

    @Override
    public String getActiveSkillInSlot(int slotIndex) {
        return activeSkillSlots.get(slotIndex);
    }

    @Override
    public void setActiveSkillSlot(int slotIndex, String skillId) {
        if (skillId == null) {
            activeSkillSlots.remove(slotIndex);
        } else {
            activeSkillSlots.put(slotIndex, skillId);
        }
    }

    @Override
    public Map<Integer, String> getAllActiveSkillSlots() {
        return new HashMap<>(activeSkillSlots);
    }

    @Override
    public Long getSkillCooldown(String skillId) {
        return skillCooldowns.get(skillId);
    }

    @Override
    public void setSkillCooldown(String skillId, long cooldownEndTime) {
        skillCooldowns.put(skillId, cooldownEndTime);
    }

    @Override
    public Map<String, Long> getAllSkillCooldowns() {
        return new HashMap<>(skillCooldowns);
    }

    @Override
    public boolean isSkillOnCooldown(String skillId, long currentTime) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        return cooldownEnd != null && currentTime < cooldownEnd;
    }

    @Override
    public int getRemainingCooldown(String skillId, long currentTime) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null || currentTime >= cooldownEnd) {
            return 0;
        }
        return (int)(cooldownEnd - currentTime);
    }
}
