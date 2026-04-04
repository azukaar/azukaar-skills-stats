package com.azukaar.ass.network;

import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;

import java.util.HashMap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkillsSyncPayload(
        Map<String, Integer> aspectLevels,
        Map<String, Double> xpInLevel,
        int levelCap,
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
        // Write aspect levels
        buf.writeInt(payload.aspectLevels.size());
        for (Map.Entry<String, Integer> entry : payload.aspectLevels.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }

        // Write XP in level
        buf.writeInt(payload.xpInLevel.size());
        for (Map.Entry<String, Double> entry : payload.xpInLevel.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeDouble(entry.getValue());
        }

        // Write level cap
        buf.writeInt(payload.levelCap);

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
            buf.writeInt(entry.getKey());
            buf.writeUtf(entry.getValue());
        }

        // Write skill cooldowns
        buf.writeInt(payload.skillCooldowns.size());
        for (Map.Entry<String, Long> entry : payload.skillCooldowns.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }
    }

    private static PlayerSkillsSyncPayload read(RegistryFriendlyByteBuf buf) {
        // Read aspect levels
        int levelsSize = buf.readInt();
        Map<String, Integer> aspectLevels = new HashMap<>();
        for (int i = 0; i < levelsSize; i++) {
            aspectLevels.put(buf.readUtf(), buf.readInt());
        }

        // Read XP in level
        int xpSize = buf.readInt();
        Map<String, Double> xpInLevel = new HashMap<>();
        for (int i = 0; i < xpSize; i++) {
            xpInLevel.put(buf.readUtf(), buf.readDouble());
        }

        // Read level cap
        int levelCap = buf.readInt();

        // Read skill points
        int skillPoints = buf.readInt();

        // Read skills data
        int skillsSize = buf.readInt();
        Map<String, Integer> skillsData = new HashMap<>();
        for (int i = 0; i < skillsSize; i++) {
            skillsData.put(buf.readUtf(), buf.readInt());
        }

        // Read active skill slots
        int slotsSize = buf.readInt();
        Map<Integer, String> activeSkillSlots = new HashMap<>();
        for (int i = 0; i < slotsSize; i++) {
            activeSkillSlots.put(buf.readInt(), buf.readUtf());
        }

        // Read skill cooldowns
        int cooldownsSize = buf.readInt();
        Map<String, Long> skillCooldowns = new HashMap<>();
        for (int i = 0; i < cooldownsSize; i++) {
            skillCooldowns.put(buf.readUtf(), buf.readLong());
        }

        return new PlayerSkillsSyncPayload(aspectLevels, xpInLevel, levelCap, skillPoints, skillsData, activeSkillSlots, skillCooldowns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
