package com.azukaar.ass;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.azukaar.ass.types.DependencyData;
import com.azukaar.ass.types.Skill;
import com.azukaar.ass.types.SkillTree;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class SkillDataManager implements PreparableReloadListener {
    public static final SkillDataManager INSTANCE = new SkillDataManager();
    
    private static final Gson GSON = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create();
    
    private final Map<ResourceLocation, Skill> skills = new HashMap<>();
    private final Map<String, SkillTree> skillTrees = new HashMap<>();
    
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, 
            Executor backgroundExecutor, Executor gameExecutor) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Clear existing data
            skills.clear();
            skillTrees.clear();
            
            // Load all data
            loadSkillTrees(resourceManager);
            loadSkills(resourceManager);
            loadDependencies(resourceManager);
            
            return null;
        }, backgroundExecutor).thenCompose(barrier::wait).thenRunAsync(() -> {
            // Build final skill trees on main thread
            buildSkillTrees();
            
            // Log completion
            AzukaarSkillsStats.LOGGER.info("Loaded {} skills across {} skill trees", 
                       skills.size(), skillTrees.size());
        }, gameExecutor);
    }
    
    private void loadSkills(ResourceManager resourceManager) {
        // Method 1: Using listResourceStacks (ass_current NeoForge API)
        Map<ResourceLocation, List<Resource>> skillResources = 
            resourceManager.listResourceStacks("ass_skills", path -> path.getPath().endsWith(".json"));
            
        for (Map.Entry<ResourceLocation, List<Resource>> entry : skillResources.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            List<Resource> resources = entry.getValue();
            
            // Use the last resource (highest priority datapack)
            if (!resources.isEmpty()) {
                Resource resource = resources.get(resources.size() - 1);
                
                try (Reader reader = resource.openAsReader()) {
                    Skill skill = GSON.fromJson(reader, Skill.class);
                    skill.postDeserialize(); // Convert strings to Components
                    
                    ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath(skill.getId().split(":")[0], skill.getId().split(":")[1]);
                    skills.put(skillId, skill);
                    
                    AzukaarSkillsStats.LOGGER.debug("Loaded skill: {}", skill.getId());
                } catch (Exception e) {
                    AzukaarSkillsStats.LOGGER.error("Failed to load skill from {}: {}", resourceLocation, e.getMessage());
                }
            }
        }
    }

    private void loadSkillTrees(ResourceManager resourceManager) {
        Map<ResourceLocation, List<Resource>> treeResources = 
            resourceManager.listResourceStacks("ass_skill_trees", path -> path.getPath().endsWith(".json"));
            
        for (Map.Entry<ResourceLocation, List<Resource>> entry : treeResources.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            List<Resource> resources = entry.getValue();
            
            // Use the last resource (highest priority datapack)
            if (!resources.isEmpty()) {
                Resource resource = resources.get(resources.size() - 1);
                
                try (Reader reader = resource.openAsReader()) {
                    SkillTree tree = GSON.fromJson(reader, SkillTree.class);
                    skillTrees.put(tree.getId(), tree);
                    
                    AzukaarSkillsStats.LOGGER.debug("Loaded skill tree: {}", tree.getId());
                } catch (Exception e) {
                    AzukaarSkillsStats.LOGGER.error("Failed to load skill tree from {}: {}", resourceLocation, e.getMessage());
                }
            }
        }
    }
    
    private void loadDependencies(ResourceManager resourceManager) {
        Map<ResourceLocation, List<Resource>> dependencyResources = 
            resourceManager.listResourceStacks("ass_skill_dependencies", path -> path.getPath().endsWith(".json"));
            
        for (Map.Entry<ResourceLocation, List<Resource>> entry : dependencyResources.entrySet()) {
            List<Resource> resources = entry.getValue();
            
            if (!resources.isEmpty()) {
                Resource resource = resources.get(resources.size() - 1);
                
                try (Reader reader = resource.openAsReader()) {
                    DependencyData dep = GSON.fromJson(reader, DependencyData.class);
                    
                    Skill skill = findSkillById(dep.getSkill());
                    if (skill != null) {
                        for (String prereqId : dep.getPrerequisites()) {
                            Skill prereq = findSkillById(prereqId);
                            if (prereq != null) {
                                skill.addPrerequisite(prereq);
                            } else {
                                AzukaarSkillsStats.LOGGER.warn("Missing prerequisite skill: {} for {}", prereqId, dep.getSkill());
                            }
                        }
                    } else {
                        AzukaarSkillsStats.LOGGER.warn("Missing skill for dependency: {}", dep.getSkill());
                    }
                } catch (Exception e) {
                    AzukaarSkillsStats.LOGGER.error("Failed to load dependency from {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }
    
    private void buildSkillTrees() {
        // Group skills by tree and create SkillTree objects
        for (Skill skill : skills.values()) {
            String treeId = skill.getSkillTree();
            if (treeId != null) {
                SkillTree tree = skillTrees.computeIfAbsent(treeId, id -> new SkillTree());
                tree.addSkill(skill);
            }
        }
    }
    
    public Skill findSkillById(String skillId) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            skillId.split(":")[0], 
            skillId.split(":")[1]
        );
        return skills.get(location);
    }
    
    public Collection<Skill> getAllSkills() {
        return skills.values();
    }
    
    public SkillTree getSkillTree(String treeId) {
        return skillTrees.get(treeId);
    }
    
    public Collection<SkillTree> getAllSkillTrees() {
        return skillTrees.values();
    }
}