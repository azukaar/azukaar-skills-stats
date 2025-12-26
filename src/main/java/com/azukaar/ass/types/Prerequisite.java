package com.azukaar.ass.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a prerequisite requirement for a skill.
 * Extensible for future prerequisite types.
 */
public class Prerequisite {
    @Expose @SerializedName("skill")
    private String skillId;

    @Expose @SerializedName("level")
    private int requiredLevel = 1; // Default to level 1 if not specified

    // Reference to the actual skill (set after loading)
    private transient Skill skillRef;

    public Prerequisite() {}

    public Prerequisite(String skillId, int requiredLevel) {
        this.skillId = skillId;
        this.requiredLevel = requiredLevel;
    }

    // Getters
    public String getSkillId() { return skillId; }
    public int getRequiredLevel() { return requiredLevel; }
    public Skill getSkillRef() { return skillRef; }

    // Setters
    public void setSkillRef(Skill skill) { this.skillRef = skill; }
}
