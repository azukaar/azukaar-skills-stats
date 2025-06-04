package com.azukaar.ass.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.azukaar.ass.client.gui.SkillScreen;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SkillTree {
    
    @Expose @SerializedName("id")
    private String id;

    @Expose @SerializedName("display_name")
    private String displayName;

    @Expose @SerializedName("icon")
    private IconData iconData;

    private final Map<String, Skill> skills;
    private float minX, maxX, minY, maxY; // Bounds for scrolling
    
    public SkillTree() {
        this.skills = new HashMap<>();
    }
    
    public void addSkill(Skill skill) {
        skills.put(skill.getId(), skill);
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

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public IconData getIconData() { return iconData; }
}