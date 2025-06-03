
package com.azukaar.ass.capabilities;

import java.util.HashMap;
import java.util.Map;
import com.azukaar.ass.PlayerPath;

public class PlayerSkills implements IPlayerSkills {
    private Map<String, Double> experience = new HashMap<>();

    @Override
    public double getExperience(String pathName) {
        return experience.getOrDefault(pathName, 0.0);
    }

    @Override
    public void setExperience(String pathName, double exp) {
        experience.put(pathName, exp);
    }

    @Override
    public void addExperience(String pathName, double exp) {
        double current = getExperience(pathName);
        setExperience(pathName, current + exp);
    }

    @Override
    public double getLevel(String pathName) {
        double exp = getExperience(pathName);
        return PlayerPath.getLevelFromXp((int) exp);
    }

    @Override
    public Map<String, Double> getAllExperience() {
        return new HashMap<>(experience);
    }

    @Override
    public void setAllExperience(Map<String, Double> experience) {
        this.experience = new HashMap<>(experience);
    }
}
