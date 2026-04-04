package com.azukaar.ass.api;

import java.util.Map;

import com.azukaar.ass.types.IconData;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.minecraft.network.chat.Component;

public class AspectDefinition {
    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("enabled")
    private boolean enabled = true;

    @Expose
    @SerializedName("xp_multiplier")
    private double xpMultiplier = 1.0;

    @Expose
    @SerializedName("display_name")
    private String displayNameString;

    @Expose
    @SerializedName("icon")
    private IconData icon;

    @Expose
    @SerializedName("description")
    private String description;

    @Expose
    @SerializedName("color")
    private String colorString = "#FFFF55";

    @Expose
    @SerializedName("properties")
    private Map<String, Object> properties;

    private transient Component displayName;
    private transient int color = 0xFFFF55;

    public AspectDefinition() {}

    public void postDeserialize() {
        if (displayNameString != null) {
            displayName = Component.translatable(displayNameString);
        }
        if (colorString != null) {
            String hex = colorString.startsWith("#") ? colorString.substring(1) : colorString;
            color = Integer.parseInt(hex, 16);
        }
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public double getXpMultiplier() { return xpMultiplier; }
    public String getDisplayNameString() { return displayNameString; }
    public Component getDisplayName() { return displayName; }
    public IconData getIcon() { return icon; }
    public String getDescription() { return description; }
    public int getColor() { return color; }
    public Map<String, Object> getProperties() { return properties; }

    public double getDouble(String key, double defaultValue) {
        if (properties == null || !properties.containsKey(key)) return defaultValue;
        Object val = properties.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        if (properties == null || !properties.containsKey(key)) return defaultValue;
        Object val = properties.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        if (properties == null || !properties.containsKey(key)) return defaultValue;
        Object val = properties.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        if (properties == null || !properties.containsKey(key)) return defaultValue;
        Object val = properties.get(key);
        if (val instanceof String) return (String) val;
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (properties == null || !properties.containsKey(key)) return defaultValue;
        Object val = properties.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }
}
