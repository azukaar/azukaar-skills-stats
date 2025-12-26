package com.azukaar.ass.types;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class DependencyData {
    @Expose
    private String skill;

    @Expose
    private List<Prerequisite> prerequisites = new ArrayList<>();

    public String getSkill() { return skill; }
    public List<Prerequisite> getPrerequisites() { return prerequisites; }
}