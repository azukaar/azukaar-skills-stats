
package com.azukaar.ass.capabilities;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerSkills implements IPlayerSkills {
    private Map<String, Double> experience = new HashMap<>();
    private int skillPoints = 0;
    private Map<String, Integer> skills = new HashMap<>();

    @Override
    public double getExperience(String pathName) {
        return experience.getOrDefault(pathName, 0.0);
    }

    @Override
    public void setExperience(String pathName, double exp) {
        experience.put(pathName, exp);
    }

    @Override
    public void addExperience(String pathName, double exp) {
        double current = getExperience(pathName);
        setExperience(pathName, current + exp);
        System.out.println("Added " + exp + " to " + current);
    }

    @Override
    public double getLevel(String pathName) {
        double exp = getExperience(pathName);
        return IPlayerSkills.getLevelFromXp((int) exp);
    }

    @Override
    public Map<String, Double> getAllExperience() {
        return new HashMap<>(experience);
    }

    @Override
    public void setAllExperience(Map<String, Double> experience) {
        this.experience = new HashMap<>(experience);
    }

    @Override
    public int getMainLevel() {
        int mainExperience = 0;
        int totalLevel = 0; // Assuming totalLevel is calculated from all paths

        for (String pathName : IPlayerSkills.PATH_NAMES) {
            totalLevel += IPlayerSkills.getLevelFromXp(experience.getOrDefault(pathName, 0.0).intValue());
        }

        for (int i = 0; i < totalLevel; i++) {
            mainExperience += Math.pow(i, 0.75) * 150;
        }
        if (totalLevel == 1) return 1;
        return Math.min(IPlayerSkills.getLevelFromXp(mainExperience), totalLevel);
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
}
