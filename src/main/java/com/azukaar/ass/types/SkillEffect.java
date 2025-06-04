package com.azukaar.ass.types;

import java.util.List;

import com.azukaar.ass.CustomEffectHandler;
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
        
        // Transient fields for resolved data
        private transient net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> resolvedAttribute;
        private transient net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation resolvedOperation;
        private transient String parentSkillId; // To track which skill this effect belongs to
        
        public Effect() {
            // Default scaling if not specified
            this.scaling = new ScalingData();
        }
        
        public void postDeserialize() {
            if ("attribute_modifier".equals(type)) {
                resolveAttributeModifier();
            }
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
            } else if ("custom_attribute_modifier".equals(type)) {
                System.out.println("Applying CUSTOM  effect: " + type + " for skill: " + parentSkillId + " at level: " + skillLevel);
                // Delegate to custom effect handler
                CustomEffectHandler.applyCustomEffect(player, attribute, this, skillLevel);
            }
        }
        
        public void remove(Player player) {
            if ("attribute_modifier".equals(type)) {
                removeAttributeModifier(player);
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
            
            // Create unique modifier ID based on skill and attribute - NEEDS SKILL ID
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
    }
}