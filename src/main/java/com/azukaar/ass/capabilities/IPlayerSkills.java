package com.azukaar.ass.capabilities;

import java.util.Map;

public interface IPlayerSkills {
    double getExperience(String pathName);
    void setExperience(String pathName, double experience);
    void addExperience(String pathName, double experience);
    double getLevel(String pathName);
    Map<String, Double> getAllExperience();
    void setAllExperience(Map<String, Double> experience);
}
