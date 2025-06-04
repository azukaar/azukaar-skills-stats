package com.azukaar.ass.capabilities;

import java.util.HashMap;
import java.util.Map;

import com.azukaar.ass.network.PlayerSkillsSyncPayload;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlayerSkillsProvider implements INBTSerializable<CompoundTag> {
    private final PlayerSkills skills = new PlayerSkills();

    public IPlayerSkills getSkills() {
        return skills;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        
        // Serialize experience
        CompoundTag experienceTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : skills.getAllExperience().entrySet()) {
            experienceTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("experience", experienceTag);
        
        // Serialize skill points
        tag.putInt("skillPoints", skills.getSkillPoints());
        
        // Serialize skills
        CompoundTag skillsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skills.getAllSkills().entrySet()) {
            skillsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("skills", skillsTag);
        
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Deserialize experience
        if (tag.contains("experience")) {
            CompoundTag experienceTag = tag.getCompound("experience");
            Map<String, Double> experience = new HashMap<>();
            for (String key : experienceTag.getAllKeys()) {
                experience.put(key, experienceTag.getDouble(key));
            }
            skills.setAllExperience(experience);
        } else {
            // Legacy support: if no "experience" tag, read directly from root
            Map<String, Double> experience = new HashMap<>();
            for (String key : tag.getAllKeys()) {
                if (!key.equals("skillPoints") && !key.equals("skills")) {
                    experience.put(key, tag.getDouble(key));
                }
            }
            skills.setAllExperience(experience);
        }
        
        // Deserialize skill points
        if (tag.contains("skillPoints")) {
            skills.setSkillPoints(tag.getInt("skillPoints"));
        }
        
        // Deserialize skills
        if (tag.contains("skills")) {
            CompoundTag skillsTag = tag.getCompound("skills");
            Map<String, Integer> skillsMap = new HashMap<>();
            for (String key : skillsTag.getAllKeys()) {
                skillsMap.put(key, skillsTag.getInt(key));
            }
            skills.setAllSkills(skillsMap);
        }
    }

    // method to sync data to client
    public void syncToClient(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(skills.getAllExperience(),
                                                  skills.getSkillPoints(),
                                                  skills.getAllSkills());
        PacketDistributor.sendToPlayer(player, payload);
    }

    // method to sync to all players tracking an entity
    public void syncToTrackingPlayers(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(skills.getAllExperience(),
                                                  skills.getSkillPoints(),
                                                  skills.getAllSkills());
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
    }
}