package com.azukaar.ass.types;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class DependencyData {
    @Expose
    private String skill;
    
    @Expose
    private List<String> prerequisites = new ArrayList<>();
    
    public String getSkill() { return skill; }
    public List<String> getPrerequisites() { return prerequisites; }
}