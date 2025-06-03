package com.azukaar.ass;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class Skill {
    private final String id;
    private final Component displayName;
    private final ItemStack icon;
    private final float x, y; // Position in world coordinates
    private final List<Skill> prerequisites;
    private final List<Skill> children;
    private boolean unlocked;
    private int level;
    private int maxLevel;
    
    public Skill(String id, Component displayName, ItemStack icon, float x, float y) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.prerequisites = new ArrayList<>();
        this.children = new ArrayList<>();
        this.unlocked = false;
        this.level = 0;
        this.maxLevel = 5;
    }
    
    // Getters and setters
    public float getX() { return x; }
    public float getY() { return y; }
    public String getId() { return id; }
    public ItemStack getIcon() { return icon; }
    public Component getDisplayName() { return displayName; }
    public boolean isUnlocked() { return unlocked; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public List<Skill> getPrerequisites() { return prerequisites; }
    public List<Skill> getChildren() { return children; }
    
    public void addPrerequisite(Skill skill) {
        prerequisites.add(skill);
        skill.children.add(this);
    }
}