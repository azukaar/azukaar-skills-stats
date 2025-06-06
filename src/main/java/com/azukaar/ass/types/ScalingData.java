package com.azukaar.ass.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ScalingData {
    @Expose @SerializedName("type")
    private String type = "fixed";
    
    @Expose @SerializedName("multiplier")
    private double multiplier = 1.0;
    
    @Expose @SerializedName("base")
    private double base = 0.0;
    
    @Expose @SerializedName("exponent")
    private double exponent = 1.0;
    
    public double calculateValue(double baseValue, int skillLevel) {
        switch (type) {
            case "fixed":
                return baseValue;
            case "per_level":
                return baseValue + ((skillLevel - 1) * multiplier);
            case "exponential":
                return baseValue + (Math.pow(skillLevel - 1, exponent) * multiplier);
            case "logarithmic":
                return baseValue + (Math.log(skillLevel + 1) * multiplier);
            case "threshold":
                // Only applies if skill level meets threshold
                return skillLevel >= multiplier ? baseValue : 0;
            default:
                return baseValue;
        }
    }
    
    // Getters
    public String getType() { return type; }
    public double getMultiplier() { return multiplier; }
    public double getBase() { return base; }
    public double getExponent() { return exponent; }
}