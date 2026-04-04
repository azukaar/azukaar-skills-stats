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

        // Serialize aspect levels
        CompoundTag levelsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skills.getAllLevels().entrySet()) {
            levelsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("aspectLevels", levelsTag);

        // Serialize level cap
        tag.putInt("levelCap", skills.getLevelCap());

        // Serialize XP within current level
        CompoundTag xpTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : skills.getAllExperience().entrySet()) {
            xpTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("xpInLevel", xpTag);

        // Serialize skill points
        tag.putInt("skillPoints", skills.getSkillPoints());

        // Serialize skills
        CompoundTag skillsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skills.getAllSkills().entrySet()) {
            skillsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("skills", skillsTag);

        // Serialize active skill slots
        CompoundTag slotsTag = new CompoundTag();
        for (Map.Entry<Integer, String> entry : skills.getAllActiveSkillSlots().entrySet()) {
            slotsTag.putString(String.valueOf(entry.getKey()), entry.getValue());
        }
        tag.put("activeSkillSlots", slotsTag);

        // Serialize cooldowns
        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : skills.getAllSkillCooldowns().entrySet()) {
            cooldownsTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("skillCooldowns", cooldownsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Deserialize aspect levels
        if (tag.contains("aspectLevels")) {
            CompoundTag levelsTag = tag.getCompound("aspectLevels");
            Map<String, Integer> levels = new HashMap<>();
            for (String key : levelsTag.getAllKeys()) {
                levels.put(key, levelsTag.getInt(key));
            }
            skills.setAllLevels(levels);
        }

        // Deserialize level cap
        if (tag.contains("levelCap")) {
            skills.setLevelCap(tag.getInt("levelCap"));
        }

        // Deserialize XP within current level
        if (tag.contains("xpInLevel")) {
            CompoundTag xpTag = tag.getCompound("xpInLevel");
            Map<String, Double> xp = new HashMap<>();
            for (String key : xpTag.getAllKeys()) {
                xp.put(key, xpTag.getDouble(key));
            }
            skills.setAllExperience(xp);
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

        // Deserialize active skill slots
        if (tag.contains("activeSkillSlots")) {
            CompoundTag slotsTag = tag.getCompound("activeSkillSlots");
            for (String key : slotsTag.getAllKeys()) {
                try {
                    int slotIndex = Integer.parseInt(key);
                    String skillId = slotsTag.getString(key);
                    skills.setActiveSkillSlot(slotIndex, skillId);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }

        // Deserialize cooldowns
        if (tag.contains("skillCooldowns")) {
            CompoundTag cooldownsTag = tag.getCompound("skillCooldowns");
            for (String skillId : cooldownsTag.getAllKeys()) {
                long cooldownEnd = cooldownsTag.getLong(skillId);
                skills.setSkillCooldown(skillId, cooldownEnd);
            }
        }
    }

    public void syncToClient(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(
            skills.getAllLevels(),
            skills.getAllExperience(),
            skills.getLevelCap(),
            skills.getSkillPoints(),
            skills.getAllSkills(),
            skills.getAllActiveSkillSlots(),
            skills.getAllSkillCooldowns()
        );
        PacketDistributor.sendToPlayer(player, payload);
    }

    public void syncToTrackingPlayers(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(
            skills.getAllLevels(),
            skills.getAllExperience(),
            skills.getLevelCap(),
            skills.getSkillPoints(),
            skills.getAllSkills(),
            skills.getAllActiveSkillSlots(),
            skills.getAllSkillCooldowns()
        );
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
    }
}
