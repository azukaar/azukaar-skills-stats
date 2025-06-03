package com.azukaar.ass.capabilities;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PlayerSkillsProvider implements INBTSerializable<CompoundTag> {
    private final PlayerSkills skills = new PlayerSkills();

    public IPlayerSkills getSkills() {
        return skills;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Double> entry : skills.getAllExperience().entrySet()) {
            tag.putDouble(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        Map<String, Double> experience = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            experience.put(key, tag.getDouble(key));
        }
        skills.setAllExperience(experience);
    }
}