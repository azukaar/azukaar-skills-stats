package com.azukaar.ass.network;

import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;

import java.util.HashMap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkillsSyncPayload(
        Map<String, Double> experienceData, 
        int skillPoints, 
        Map<String, Integer> skillsData,
        Map<Integer, String> activeSkillSlots, 
        Map<String, Long> skillCooldowns        
) implements CustomPacketPayload {
    
    public static final Type<PlayerSkillsSyncPayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "player_skills_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkillsSyncPayload> STREAM_CODEC = 
        StreamCodec.of(PlayerSkillsSyncPayload::write, PlayerSkillsSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, PlayerSkillsSyncPayload payload) {
        // Write experience data
        buf.writeInt(payload.experienceData.size());
        for (Map.Entry<String, Double> entry : payload.experienceData.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeDouble(entry.getValue());
        }
        
        // Write skill points
        buf.writeInt(payload.skillPoints);
        
        // Write skills data
        buf.writeInt(payload.skillsData.size());
        for (Map.Entry<String, Integer> entry : payload.skillsData.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        
        // Write active skill slots
        buf.writeInt(payload.activeSkillSlots.size());
        for (Map.Entry<Integer, String> entry : payload.activeSkillSlots.entrySet()) {
            buf.writeInt(entry.getKey());   // slot index
            buf.writeUtf(entry.getValue()); // skill id
        }
        
        // Write skill cooldowns
        buf.writeInt(payload.skillCooldowns.size());
        for (Map.Entry<String, Long> entry : payload.skillCooldowns.entrySet()) {
            buf.writeUtf(entry.getKey());   // skill id
            buf.writeLong(entry.getValue()); // cooldown end time
        }
    }

    private static PlayerSkillsSyncPayload read(RegistryFriendlyByteBuf buf) {
        // Read experience data
        int experienceSize = buf.readInt();
        Map<String, Double> experienceData = new HashMap<>();
        for (int i = 0; i < experienceSize; i++) {
            String key = buf.readUtf();
            double value = buf.readDouble();
            experienceData.put(key, value);
        }
        
        // Read skill points
        int skillPoints = buf.readInt();
        
        // Read skills data
        int skillsSize = buf.readInt();
        Map<String, Integer> skillsData = new HashMap<>();
        for (int i = 0; i < skillsSize; i++) {
            String key = buf.readUtf();
            int value = buf.readInt();
            skillsData.put(key, value);
        }
        
        // Read active skill slots
        int slotsSize = buf.readInt();
        Map<Integer, String> activeSkillSlots = new HashMap<>();
        for (int i = 0; i < slotsSize; i++) {
            int slotIndex = buf.readInt();
            String skillId = buf.readUtf();
            activeSkillSlots.put(slotIndex, skillId);
        }
        
        // Read skill cooldowns
        int cooldownsSize = buf.readInt();
        Map<String, Long> skillCooldowns = new HashMap<>();
        for (int i = 0; i < cooldownsSize; i++) {
            String skillId = buf.readUtf();
            long cooldownEnd = buf.readLong();
            skillCooldowns.put(skillId, cooldownEnd);
        }
        
        return new PlayerSkillsSyncPayload(experienceData, skillPoints, skillsData, activeSkillSlots, skillCooldowns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}