package com.azukaar.ass.network;

import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;

import java.util.HashMap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkillsSyncPayload(Map<String, Double> skillData) implements CustomPacketPayload {
    public static final Type<PlayerSkillsSyncPayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "player_skills_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkillsSyncPayload> STREAM_CODEC = 
        StreamCodec.of(PlayerSkillsSyncPayload::write, PlayerSkillsSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, PlayerSkillsSyncPayload payload) {
        buf.writeInt(payload.skillData.size());
        for (Map.Entry<String, Double> entry : payload.skillData.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeDouble(entry.getValue());
        }
    }

    private static PlayerSkillsSyncPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Double> skillData = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            double value = buf.readDouble();
            skillData.put(key, value);
        }
        return new PlayerSkillsSyncPayload(skillData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}