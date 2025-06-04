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

    // method to sync data to client
    public void syncToClient(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(skills.getAllExperience());
        PacketDistributor.sendToPlayer(player, payload);
    }

    // method to sync to all players tracking an entity
    public void syncToTrackingPlayers(ServerPlayer player) {
        var payload = new PlayerSkillsSyncPayload(skills.getAllExperience());
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
    }

}