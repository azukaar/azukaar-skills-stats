package com.azukaar.ass.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.CustomEffectHandler;
import com.azukaar.ass.api.PlayerData;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.player.Player;

public class SkillEffect {
    @Expose @SerializedName("skill")
    private String skillId;
    
    @Expose @SerializedName("effects")
    private List<Effect> effects;
    
    public SkillEffect() {}
    
    /**
     * Call this after deserialization to properly initialize all effects
     */
    public void postDeserialize() {
        if (effects != null) {
            for (Effect effect : effects) {
                effect.setParentSkillId(skillId); // Set the parent skill ID
                effect.postDeserialize();
            }
        }
    }
    
    // Getters
    public String getSkillId() { return skillId; }
    public List<Effect> getEffects() { return effects; }
    
    // Apply all effects for this skill
    public void applyEffects(Player player, int skillLevel) {
        if (effects != null) {
            for (Effect effect : effects) {
                effect.apply(player, skillLevel);
            }
        }
    }
    
    // Remove all effects for this skill
    public void removeEffects(Player player) {
        if (effects != null) {
            for (Effect effect : effects) {
                effect.remove(player);
            }
        }
    }
    
    /**
     * Get current value of a skill parameter based on player's skill level
     * Note: Cooldown is no longer handled here - it's moved to the Skill level
     */
    public static double getSkillParameter(Player player, String skillId, String parameter) {
        // Get the skill effect data
        var skillDataManager = com.azukaar.ass.SkillDataManager.INSTANCE;
        SkillEffect skillEffect = skillDataManager.getSkillEffects(skillId);
        if (skillEffect == null) return 0.0;
        
        // Get player's current skill level
        int skillLevel = PlayerData.getSkillLevel(player, skillId);
        if (skillLevel <= 0) return 0.0;
        
        // Find the active effect and calculate the parameter value
        for (Effect effect : skillEffect.getEffects()) {
            if ("active".equals(effect.getType())) {
                // Check data parameters
                ScalingData scalingData = effect.getData().get(parameter);
                if (scalingData != null) {
                    System.out.println("Calculating with " + scalingData.getBase() + " and " + skillLevel);
                    return scalingData.calculateValue(scalingData.getBase(), skillLevel);
                }
            }
        }
        
        return 0.0;
    }
    
    public static class Effect {
        @Expose @SerializedName("type")
        private String type;
        
        @Expose @SerializedName("attribute")
        private String attribute;
        
        @Expose @SerializedName("value")
        private double value;
        
        @Expose @SerializedName("operation")
        private String operation = "add_value";
        
        @Expose @SerializedName("scaling")
        private ScalingData scaling;
            
        // For active effects
        @Expose @SerializedName("active_effect")
        private String activeEffectId;
        
        @Expose @SerializedName("data")
        private Map<String, ScalingData> data;

        @Expose @SerializedName("description")
        private String description;
        
        // Cooldown removed - now handled at skill level

        // Transient fields for resolved data (only for attribute modifiers)
        private transient net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> resolvedAttribute;
        private transient net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation resolvedOperation;
        private transient String parentSkillId; // To track which skill this effect belongs to

        public Effect() {
            // Default scaling if not specified
            this.scaling = new ScalingData();
            this.data = new HashMap<>();
        }
        
        public void postDeserialize() {
            if ("attribute_modifier".equals(type)) {
                resolveAttributeModifier();
            }
            // Active effects don't need any special setup
        }

        private void resolveAttributeModifier() {
            // Resolve attribute
            net.minecraft.resources.ResourceLocation attrLoc = net.minecraft.resources.ResourceLocation.parse(attribute);
            this.resolvedAttribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(attrLoc).orElse(null);
            
            if (this.resolvedAttribute == null) {
                throw new IllegalArgumentException("Unknown attribute: " + attribute);
            }
            
            // Resolve operation
            switch (operation.toLowerCase()) {
                case "add_value":
                case "add":
                    this.resolvedOperation = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
                    break;
                case "add_multiplied_base":
                case "multiply_base":
                    this.resolvedOperation = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    break;
                case "add_multiplied_total":
                case "multiply_total":
                    this.resolvedOperation = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        }
        
        public void apply(Player player, int skillLevel) {
            if ("attribute_modifier".equals(type)) {
                System.out.println("Applying effect: " + type + " for skill: " + parentSkillId + " at level: " + skillLevel);
                applyAttributeModifier(player, skillLevel);
            } else if ("active".equals(type)) {
                System.out.println("Registered active effect: " + type + " for skill: " + parentSkillId + " at level: " + skillLevel);
                // Active effects don't need to "apply" anything - they're just data definitions
                // The values are calculated on-demand when skills are used
            } else if ("custom_attribute_modifier".equals(type)) {
                System.out.println("Applying CUSTOM effect: " + type + " for skill: " + parentSkillId + " at level: " + skillLevel);
                // Delegate to custom effect handler
                CustomEffectHandler.applyCustomEffect(player, attribute, this, skillLevel);
            }
        }
        
        public void remove(Player player) {
            if ("attribute_modifier".equals(type)) {
                removeAttributeModifier(player);
            } else if ("active".equals(type)) {
                // Active effects don't need cleanup - they're just data
                System.out.println("Unregistered active effect for skill: " + parentSkillId);
            } else if ("custom_attribute_modifier".equals(type)) {
                // Delegate to custom effect handler
                CustomEffectHandler.removeCustomEffect(player, attribute, this);
            }
        }

        private void applyAttributeModifier(Player player, int skillLevel) {
            if (resolvedAttribute == null) {
                postDeserialize(); // Try to resolve if not already done
            }
            
            var instance = player.getAttribute(resolvedAttribute);
            if (instance == null) {
                System.err.println("Failed to get attribute instance for: " + attribute);
                return;
            }
            
            // Remove existing modifier first
            removeAttributeModifier(player);
            
            // Calculate the actual value based on skill level
            double actualValue = scaling.calculateValue(value, skillLevel);

            System.out.println("Applying attribute modifier: " + attribute + 
                " with value: " + actualValue + 
                " operation: " + resolvedOperation + 
                " for skill: " + parentSkillId);
            
            // Create unique modifier ID based on skill and attribute
            net.minecraft.resources.ResourceLocation modifierId = getModifierId();
            
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                modifierId,
                actualValue,
                resolvedOperation
            );
            
            instance.addPermanentModifier(modifier);
        }
        
        private void removeAttributeModifier(Player player) {
            if (resolvedAttribute == null) return;
            
            var instance = player.getAttribute(resolvedAttribute);
            if (instance == null) return;
            
            // Remove the modifier by ID
            instance.removeModifier(getModifierId());
        }
        
        public void setParentSkillId(String skillId) {
            this.parentSkillId = skillId;
        }
                
        private net.minecraft.resources.ResourceLocation getModifierId() {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                "azukaarskillsstats", 
                parentSkillId.replace(":", "_") + "_" + attribute.replace(":", "_")
            );
        }

        public String getAttribute() { return attribute; }
        public double getValue() { return value; }
        public String getOperation() { return operation; }
        public ScalingData getScaling() { return scaling; }
        public String getType() { return type; }
        public String getActiveEffectId() { return activeEffectId; }
        public Map<String, ScalingData> getData() { return data != null ? new HashMap<>(data) : new HashMap<>(); }
        public String getDescription() { return description; }
        // getCooldown() method removed
    }
}