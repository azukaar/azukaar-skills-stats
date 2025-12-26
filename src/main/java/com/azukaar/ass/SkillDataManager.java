package com.azukaar.ass;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.DependencyData;
import com.azukaar.ass.types.Prerequisite;
import com.azukaar.ass.types.Skill;
import com.azukaar.ass.types.SkillEffect;
import com.azukaar.ass.types.SkillTree;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;

public class SkillDataManager implements PreparableReloadListener {
    public static final SkillDataManager INSTANCE = new SkillDataManager();
    
    private static final Gson GSON = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create();
    
    private final Map<ResourceLocation, Skill> skills = new HashMap<>();
    private final Map<String, SkillTree> skillTrees = new HashMap<>();
    private final Map<String, SkillEffect> skillEffects = new HashMap<>();
    
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, 
            Executor backgroundExecutor, Executor gameExecutor) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Clear existing data
            skills.clear();
            skillTrees.clear();
            skillEffects.clear();
            
            // Load all data
            loadSkillTrees(resourceManager);
            loadSkills(resourceManager);
            loadDependencies(resourceManager);
            loadSkillEffects(resourceManager);
            
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
                        for (Prerequisite prereq : dep.getPrerequisites()) {
                            Skill prereqSkill = findSkillById(prereq.getSkillId());
                            if (prereqSkill != null) {
                                prereq.setSkillRef(prereqSkill);
                                skill.addPrerequisite(prereq);
                            } else {
                                AzukaarSkillsStats.LOGGER.warn("Missing prerequisite skill: {} for {}", prereq.getSkillId(), dep.getSkill());
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

    
    private void loadSkillEffects(ResourceManager resourceManager) {
        Map<ResourceLocation, List<Resource>> effectResources =
            resourceManager.listResourceStacks("ass_skill_effects", path -> path.getPath().endsWith(".json"));
        
        for (Map.Entry<ResourceLocation, List<Resource>> entry : effectResources.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            List<Resource> resources = entry.getValue();
            
            // Use the last resource (highest priority datapack)
            if (!resources.isEmpty()) {
                Resource resource = resources.get(resources.size() - 1);
                
                try (Reader reader = resource.openAsReader()) {
                    SkillEffect skillEffect = GSON.fromJson(reader, SkillEffect.class);
                    skillEffect.postDeserialize(); // Initialize effects properly
                    
                    skillEffects.put(skillEffect.getSkillId(), skillEffect);
                    
                    AzukaarSkillsStats.LOGGER.debug("Loaded effects for skill: {}", skillEffect.getSkillId());
                } catch (Exception e) {
                    AzukaarSkillsStats.LOGGER.error("Failed to load skill effects from {}: {}", 
                        resourceLocation, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Apply all effects for a specific skill at the given level
     */
    public void applySkillEffects(Player player, String skillId, int skillLevel) {
        SkillEffect skillEffect = skillEffects.get(skillId);
        if (skillEffect == null) return;
        
        try {
            skillEffect.applyEffects(player, skillLevel);
        } catch (Exception e) {
            AzukaarSkillsStats.LOGGER.error("Failed to apply effects for skill {}: {}", 
                skillId, e.getMessage());
        }
    }
    
    /**
     * Remove all effects for a specific skill
     */
    public void removeSkillEffects(Player player, String skillId) {
        SkillEffect skillEffect = skillEffects.get(skillId);
        if (skillEffect == null) return;
        
        try {
            skillEffect.removeEffects(player);
        } catch (Exception e) {
            AzukaarSkillsStats.LOGGER.error("Failed to remove effects for skill {}: {}", 
                skillId, e.getMessage());
        }
    }

    /**
     * Update skill effects for a player based on their current skill level
     */
    public void updateSkillEffects(Player player, String skillId) {
        int skillLevel = PlayerData.getSkillLevel(player, skillId);
        
        if (skillLevel > 0) {
            applySkillEffects(player, skillId, skillLevel);
        } else {
            removeSkillEffects(player, skillId);
        }
    }
    
    /**
     * Update all skill effects for a player based on their current skill levels
     */
    public void updateAllSkillEffects(Player player) {
        // Get all skills with effects
        for (String skillId : skillEffects.keySet()) {
            int skillLevel = PlayerData.getSkillLevel(player, skillId);
            
            if (skillLevel > 0) {
                applySkillEffects(player, skillId, skillLevel);
            } else {
                removeSkillEffects(player, skillId);
            }
        }
    }
    
    /**
     * Get effect data for a specific skill
     */
    public SkillEffect getSkillEffects(String skillId) {
        return skillEffects.get(skillId);
    }
    
    /**
     * Get all loaded skill effects
     */
    public Collection<SkillEffect> getAllSkillEffects() {
        return skillEffects.values();
    }
    
    /**
     * Check if a skill has any effects defined
     */
    public boolean hasEffects(String skillId) {
        SkillEffect skillEffect = skillEffects.get(skillId);
        return skillEffect != null && skillEffect.getEffects() != null && !skillEffect.getEffects().isEmpty();
    }
    
    public Collection<String> getActivatableSkills() {
        return skillEffects.entrySet().stream()
            .filter(entry -> {
                SkillEffect skillEffect = entry.getValue();
                return skillEffect.getEffects().stream()
                    .anyMatch(effect -> "active".equals(effect.getType()));
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}