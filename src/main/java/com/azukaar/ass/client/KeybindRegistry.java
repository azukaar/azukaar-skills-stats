package com.azukaar.ass.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton registry for managing skill keybinds on the client side.
 * Handles saving/loading keybinds to/from a JSON file in the config directory.
 */
public class KeybindRegistry {
    private static final String KEYBIND_FILE_NAME = "ass_skill_keybinds.json";
    private static KeybindRegistry INSTANCE;
    
    private final Map<String, String> skillKeybinds; // skillId -> keybind
    private final Map<String, String> keybindToSkill; // keybind -> skillId (for reverse lookup)
    private final Gson gson;
    private final Path configPath;
    
    private KeybindRegistry() {
        this.skillKeybinds = new ConcurrentHashMap<>();
        this.keybindToSkill = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Get the config directory path
        this.configPath = FMLPaths.CONFIGDIR.get().resolve(KEYBIND_FILE_NAME);
    }
    
    /**
     * Get the singleton instance of KeybindRegistry
     * @return The KeybindRegistry instance
     */
    public static KeybindRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KeybindRegistry();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize the keybind registry. Call this during mod initialization.
     * Loads existing keybinds from the config file.
     */
    public void init() {
        loadKeybinds();
        System.out.println("KeybindRegistry initialized. Loaded " + skillKeybinds.size() + " keybinds.");
    }
    
    /**
     * Set a keybind for a skill. Automatically saves to file.
     * @param skillId The skill ID
     * @param keybind The keybind string (e.g., "R", "Ctrl+F", "LMB")
     */
    public void setSkillKeybind(String skillId, String keybind) {
        if (skillId == null) return;
        
        // Remove old keybind if it exists
        String oldKeybind = skillKeybinds.remove(skillId);
        if (oldKeybind != null) {
            keybindToSkill.remove(oldKeybind);
        }
        
        // Add new keybind if not empty
        if (keybind != null && !keybind.trim().isEmpty()) {
            // Check if keybind is already used by another skill
            String existingSkill = keybindToSkill.get(keybind);
            if (existingSkill != null && !existingSkill.equals(skillId)) {
                // Remove the old mapping
                skillKeybinds.remove(existingSkill);
                System.out.println("Keybind '" + keybind + "' was reassigned from skill '" + existingSkill + "' to '" + skillId + "'");
            }
            
            skillKeybinds.put(skillId, keybind);
            keybindToSkill.put(keybind, skillId);
        }
        
        // Save to file
        saveKeybinds();
        
        System.out.println("Set keybind '" + keybind + "' for skill: " + skillId);
    }
    
    /**
     * Get the keybind for a skill
     * @param skillId The skill ID
     * @return The keybind string, or empty string if no keybind is set
     */
    public String getSkillKeybind(String skillId) {
        return skillKeybinds.getOrDefault(skillId, "");
    }
    
    /**
     * Get the skill ID from a keybind
     * @param keybind The keybind string
     * @return The skill ID, or null if no skill is bound to this keybind
     */
    public String getSkillFromKeybind(String keybind) {
        return keybindToSkill.get(keybind);
    }
    
    /**
     * Get the skill ID from a key press event
     * @param keyCode The GLFW key code
     * @param modifiers The key modifiers (Ctrl, Shift, Alt)
     * @return The skill ID, or null if no skill is bound to this key combination
     */
    public String getSkillFromKeyPress(int keyCode, int modifiers) {
        String keybind = buildKeybindString(keyCode, modifiers);
        return getSkillFromKeybind(keybind);
    }
    
    /**
     * Get the skill ID from a mouse button press
     * @param button The mouse button (0=left, 1=right, 2=middle, etc.)
     * @return The skill ID, or null if no skill is bound to this mouse button
     */
    public String getSkillFromMousePress(int button) {
        String keybind = getMouseButtonName(button);
        return getSkillFromKeybind(keybind);
    }
    
    /**
     * Remove a keybind for a skill
     * @param skillId The skill ID
     */
    public void removeSkillKeybind(String skillId) {
        String keybind = skillKeybinds.remove(skillId);
        if (keybind != null) {
            keybindToSkill.remove(keybind);
            saveKeybinds();
            System.out.println("Removed keybind for skill: " + skillId);
        }
    }
    
    /**
     * Get all keybinds as a copy of the map
     * @return A copy of the skill keybinds map
     */
    public Map<String, String> getAllKeybinds() {
        return new HashMap<>(skillKeybinds);
    }
    
    /**
     * Clear all keybinds
     */
    public void clearAllKeybinds() {
        skillKeybinds.clear();
        keybindToSkill.clear();
        saveKeybinds();
        System.out.println("Cleared all keybinds");
    }
    
    /**
     * Check if a keybind is already in use
     * @param keybind The keybind string
     * @return true if the keybind is already assigned to a skill
     */
    public boolean isKeybindInUse(String keybind) {
        return keybindToSkill.containsKey(keybind);
    }
    
    /**
     * Build a keybind string from key code and modifiers
     * @param keyCode The GLFW key code
     * @param modifiers The key modifiers
     * @return The keybind string
     */
    private String buildKeybindString(int keyCode, int modifiers) {
        StringBuilder keybindBuilder = new StringBuilder();
        
        // Check for modifiers (these are bit flags)
        boolean hasCtrl = (modifiers & 2) != 0;   // GLFW_MOD_CONTROL
        boolean hasShift = (modifiers & 1) != 0;  // GLFW_MOD_SHIFT
        boolean hasAlt = (modifiers & 4) != 0;    // GLFW_MOD_ALT
        
        if (hasCtrl) keybindBuilder.append("Ctrl+");
        if (hasShift) keybindBuilder.append("Shift+");
        if (hasAlt) keybindBuilder.append("Alt+");
        
        String keyName = getKeyName(keyCode);
        if (keyName != null) {
            keybindBuilder.append(keyName);
            return keybindBuilder.toString();
        }
        
        return null;
    }
    
    /**
     * Convert GLFW key code to readable key name
     * @param keyCode The GLFW key code
     * @return The readable key name, or null if unknown
     */
    private String getKeyName(int keyCode) {
        switch (keyCode) {
            case 32: return "Space";
            case 39: return "'";
            case 44: return ",";
            case 45: return "-";
            case 46: return ".";
            case 47: return "/";
            case 48: case 49: case 50: case 51: case 52:
            case 53: case 54: case 55: case 56: case 57:
                return String.valueOf((char) keyCode); // 0-9
            case 59: return ";";
            case 61: return "=";
            case 65: case 66: case 67: case 68: case 69: case 70: case 71: case 72: case 73:
            case 74: case 75: case 76: case 77: case 78: case 79: case 80: case 81: case 82:
            case 83: case 84: case 85: case 86: case 87: case 88: case 89: case 90:
                return String.valueOf((char) keyCode); // A-Z
            case 91: return "[";
            case 92: return "\\";
            case 93: return "]";
            case 96: return "`";
            
            // Function keys
            case 290: case 291: case 292: case 293: case 294: case 295: case 296: case 297:
            case 298: case 299: case 300: case 301:
                return "F" + (keyCode - 289); // F1-F12
                
            // Arrow keys
            case 262: return "Right";
            case 263: return "Left";
            case 264: return "Down";
            case 265: return "Up";
            
            // Other special keys
            case 257: return "Enter";
            case 258: return "Tab";
            case 280: return "CapsLock";
            case 284: return "Pause";
            case 285: return "ScrollLock";
            case 286: return "NumLock";
            case 287: return "PrintScreen";
            case 260: return "Insert";
            case 268: return "Home";
            case 266: return "PageUp";
            case 267: return "PageDown";
            case 269: return "End";
            
            // Numpad
            case 320: case 321: case 322: case 323: case 324:
            case 325: case 326: case 327: case 328: case 329:
                return "Num" + (keyCode - 320); // Num0-Num9
            case 330: return "Num.";
            case 331: return "Num/";
            case 332: return "Num*";
            case 333: return "Num-";
            case 334: return "Num+";
            case 335: return "NumEnter";
            case 336: return "Num=";
            
            default:
                return "Key" + keyCode; // Fallback for unknown keys
        }
    }
    
    /**
     * Convert mouse button to readable name
     * @param button The mouse button (0=left, 1=right, 2=middle, etc.)
     * @return The readable mouse button name
     */
    private String getMouseButtonName(int button) {
        switch (button) {
            case 0: return "LMB";
            case 1: return "RMB";
            case 2: return "MMB";
            case 3: return "Mouse4";
            case 4: return "Mouse5";
            default: return "Mouse" + button;
        }
    }
    
    /**
     * Save keybinds to the config file
     */
    private void saveKeybinds() {
        try {
            // Ensure the config directory exists
            Files.createDirectories(configPath.getParent());
            
            // Convert to a simple map for JSON serialization
            Map<String, String> saveData = new HashMap<>(skillKeybinds);
            
            // Write to file
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                gson.toJson(saveData, writer);
            }
            
            System.out.println("Saved " + skillKeybinds.size() + " keybinds to " + configPath);
            
        } catch (IOException e) {
            System.err.println("Failed to save keybinds: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load keybinds from the config file
     */
    private void loadKeybinds() {
        try {
            if (!Files.exists(configPath)) {
                System.out.println("No keybind file found, starting with empty keybinds");
                return;
            }
            
            // Read from file
            try (FileReader reader = new FileReader(configPath.toFile())) {
                Type mapType = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loadedData = gson.fromJson(reader, mapType);
                
                if (loadedData != null) {
                    skillKeybinds.clear();
                    keybindToSkill.clear();
                    
                    // Rebuild both maps
                    for (Map.Entry<String, String> entry : loadedData.entrySet()) {
                        String skillId = entry.getKey();
                        String keybind = entry.getValue();
                        
                        if (skillId != null && keybind != null && !keybind.trim().isEmpty()) {
                            skillKeybinds.put(skillId, keybind);
                            keybindToSkill.put(keybind, skillId);
                        }
                    }
                    
                    System.out.println("Loaded " + skillKeybinds.size() + " keybinds from " + configPath);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to load keybinds: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to parse keybind file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the path to the keybind config file
     * @return The path to the keybind file
     */
    public Path getConfigPath() {
        return configPath;
    }
}