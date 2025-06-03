package com.azukaar.ass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.client.gui.SkillScreen;

public class SkillTree {
    private final Map<String, Skill> skills;
    private final List<Skill> rootSkills;
    private float minX, maxX, minY, maxY; // Bounds for scrolling
    
    public SkillTree() {
        this.skills = new HashMap<>();
        this.rootSkills = new ArrayList<>();
    }
    
    public void addSkill(Skill skill) {
        skills.put(skill.getId(), skill);
        if (skill.getPrerequisites().isEmpty()) {
            rootSkills.add(skill);
        }
        updateBounds();
    }
    
    private void updateBounds() {
        minX = maxX = minY = maxY = 0;
        for (Skill skill : skills.values()) {
            minX = Math.min(minX, skill.getX());
            maxX = Math.max(maxX, skill.getX() + SkillScreen.SKILL_NODE_SIZE);
            minY = Math.min(minY, skill.getY());
            maxY = Math.max(maxY, skill.getY() + SkillScreen.SKILL_NODE_SIZE);
        }
    }
    
    public Collection<Skill> getAllSkills() { return skills.values(); }
    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
}