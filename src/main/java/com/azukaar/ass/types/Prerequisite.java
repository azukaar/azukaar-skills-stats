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

    @Expose @SerializedName("aspect")
    private String aspectId;

    @Expose @SerializedName("level")
    private int requiredLevel = 1;

    // Reference to the actual skill (set after loading)
    private transient Skill skillRef;

    public Prerequisite() {}

    public Prerequisite(String skillId, int requiredLevel) {
        this.skillId = skillId;
        this.requiredLevel = requiredLevel;
    }

    // Getters
    public String getSkillId() { return skillId; }
    public String getAspectId() { return aspectId; }
    public int getRequiredLevel() { return requiredLevel; }
    public Skill getSkillRef() { return skillRef; }
    public boolean isAspectPrerequisite() { return aspectId != null; }
    public boolean isSkillPrerequisite() { return skillId != null; }

    // Setters
    public void setSkillRef(Skill skill) { this.skillRef = skill; }
}
