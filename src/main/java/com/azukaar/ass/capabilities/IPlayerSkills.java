package com.azukaar.ass.capabilities;

import java.util.Map;

import net.minecraft.world.entity.player.Player;

public interface IPlayerSkills {
    static final String MAIN = "ass.main"; 
    static final String WARRIOR_PATH = "ass.warrior"; 
    static final String MINER_PATH = "ass.miner"; 
    static final String EXPLORER_PATH = "ass.explorer";

    static final String[] PATH_NAMES = {
        WARRIOR_PATH, MINER_PATH, EXPLORER_PATH
    };
    
    // Keep your existing static methods...
    static final int LINEAR_COMPONENT = 50;
    static final double POW_COMPONENT = 1.25;

    public static int getXpForSpecificLevel(int level) {
        if (level <= 1) return 100;
        return (int)(LINEAR_COMPONENT * Math.pow(level, POW_COMPONENT));
    }

    public static int getTotalXpForLevel(int level) {
        int totalXp = 0;
        for (int i = 1; i <= level; i++) {
            totalXp += getXpForSpecificLevel(i);
        }
        return totalXp;
    }

    public static int getLevelFromXp(int totalXp) {
        if (totalXp < LINEAR_COMPONENT) return 0;
        int low = 0;
        int high = 1000;
        while (low < high) {
            int mid = (low + high) / 2;
            int xpAtMid = getTotalXpForLevel(mid);
            if (xpAtMid < totalXp) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low - 1;
    }

    double getExperience(String pathName);
    void setExperience(String pathName, double experience);
    void addExperience(String pathName, double experience);
    double getLevel(String pathName);
    Map<String, Double> getAllExperience();
    void setAllExperience(Map<String, Double> experience);
    int getMainLevel();
}
