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
        double result;
        switch (type) {
            case "fixed":
                result = baseValue;
                break;
            case "per_level":
                result = baseValue + ((skillLevel - 1) * multiplier);
                break;
            case "exponential":
                result = baseValue + (Math.pow(skillLevel - 1, exponent) * multiplier);
                break;
            case "logarithmic":
                result = baseValue + (Math.log(skillLevel + 1) * multiplier);
                break;
            case "threshold":
                // Only applies if skill level meets threshold
                result = skillLevel >= multiplier ? baseValue : 0;
                break;
            default:
                result = baseValue;
                break;
        }
        // Round to max 3 decimal places
        return Math.round(result * 1000.0) / 1000.0;
    }
    
    // Getters
    public String getType() { return type; }
    public double getMultiplier() { return multiplier; }
    public double getBase() { return base; }
    public double getExponent() { return exponent; }
}