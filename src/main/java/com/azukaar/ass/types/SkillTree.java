package com.azukaar.ass.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.client.gui.SkillScreen;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.minecraft.world.entity.player.Player;

public class SkillTree {
    
    @Expose @SerializedName("id")
    private String id;

    @Expose @SerializedName("display_name")
    private String displayName;

    @Expose @SerializedName("icon")
    private IconData iconData;

    @Expose @SerializedName("requirements")
    private List<Prerequisite> requirements;

    @Expose @SerializedName("locked_hint")
    private String lockedHint;

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
    public List<Prerequisite> getRequirements() { return requirements; }
    public String getLockedHint() { return lockedHint; }

    public boolean areRequirementsMet(Player player) {
        if (requirements == null || requirements.isEmpty()) return true;
        for (Prerequisite prereq : requirements) {
            if (prereq.isAspectPrerequisite()) {
                int level = PlayerData.getPathLevel(player, prereq.getAspectId());
                if (level < prereq.getRequiredLevel()) return false;
            } else if (prereq.isSkillPrerequisite()) {
                int level = PlayerData.getSkillLevel(player, prereq.getSkillId());
                if (level < prereq.getRequiredLevel()) return false;
            }
        }
        return true;
    }
}