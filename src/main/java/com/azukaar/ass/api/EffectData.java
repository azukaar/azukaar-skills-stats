package com.azukaar.ass.api;

import java.util.HashMap;
import java.util.Map;

public class EffectData {
    private final Map<String, Object> data;
    
    public EffectData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }
    
    public double getDouble(String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    public float getFloat(String key, float defaultValue) {
        return (float) getDouble(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        return (int) getDouble(key, defaultValue);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public String getString(String key, String defaultValue) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    public boolean has(String key) {
        return data.containsKey(key);
    }
}