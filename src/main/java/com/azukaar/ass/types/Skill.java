package com.azukaar.ass.types;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.minecraft.network.chat.Component;

public class Skill {
    @Expose @SerializedName("id")
    private String id;
    
    @Expose @SerializedName("display_name")
    private String displayNameString;
    
    @Expose @SerializedName("icon")
    private IconData iconData;
    
    @Expose @SerializedName("position")
    private PositionData position;
    
    @Expose @SerializedName("max_level")
    private int maxLevel = 5;
    
    @Expose @SerializedName("skill_tree")
    private String skillTree;
    
    @Expose @SerializedName("cooldown")
    private ScalingData cooldown;
    
    private Component displayName;
    private final List<Skill> prerequisites;
    private final List<Skill> children;
    
    public Skill(String id, String displayName, IconData iconData, PositionData position) {
        this.id = id;
        this.displayNameString = displayName;
        this.iconData = iconData;
        this.position = position;
        this.prerequisites = new ArrayList<>();
        this.children = new ArrayList<>();
        
        // Convert string to Component after deserialization
        this.displayName = Component.literal(displayNameString);
    }
    
    // Gson needs this for deserialization
    public Skill() {
        this.prerequisites = new ArrayList<>();
        this.children = new ArrayList<>();
    }
    
    // Called after Gson deserialization
    public void postDeserialize() {
        this.displayName = Component.literal(displayNameString);
    }
    
    // Getters
    public String getId() { return id; }
    public Component getDisplayName() { return displayName; }
    public IconData getIconData() { return iconData; }
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public int getMaxLevel() { return maxLevel; }
    public List<Skill> getPrerequisites() { return prerequisites; }
    public List<Skill> getChildren() { return children; }
    public String getSkillTree() { return skillTree; }
    public ScalingData getCooldown() { return cooldown; }
    
    public void addPrerequisite(Skill skill) {
        prerequisites.add(skill);
        skill.children.add(this);
    }
    
    /**
     * Calculate the effective cooldown for this skill at the given level
     */
    public int getEffectiveCooldown(int skillLevel) {
        if (cooldown == null) return 0; // No cooldown means instant reuse
        
        double cooldownValue = cooldown.calculateValue(cooldown.getBase(), skillLevel);
        return Math.max(1, (int) cooldownValue);
    }
    
    /**
     * Check if this skill has a cooldown defined
     */
    public boolean hasCooldown() {
        return cooldown != null;
    }
}