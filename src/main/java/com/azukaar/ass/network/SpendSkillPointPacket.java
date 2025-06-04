package com.azukaar.ass.network;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class SpendSkillPointPacket implements CustomPacketPayload {
    public static final Type<SpendSkillPointPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "spend_skill_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpendSkillPointPacket> STREAM_CODEC = 
        StreamCodec.of(SpendSkillPointPacket::write, SpendSkillPointPacket::read);

    private final int amount;
    private final String skill;

    public SpendSkillPointPacket(int amount, String skill) {
        this.amount = amount;
        this.skill = skill;
    }

    private static void write(RegistryFriendlyByteBuf buf, SpendSkillPointPacket packet) {
        buf.writeInt(packet.amount);
        buf.writeUtf(packet.skill);
    }

    private static SpendSkillPointPacket read(RegistryFriendlyByteBuf buf) {
        return new SpendSkillPointPacket(buf.readInt(), buf.readUtf());
    }

    public int getAmount() {
        return amount;
    }

    public String getSkill() {
        return skill;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}