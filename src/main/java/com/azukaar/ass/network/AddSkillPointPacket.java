package com.azukaar.ass.network;

import com.azukaar.ass.AzukaarSkillsStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class AddSkillPointPacket implements CustomPacketPayload {
    public static final Type<AddSkillPointPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "add_skill_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddSkillPointPacket> STREAM_CODEC = 
        StreamCodec.of(AddSkillPointPacket::write, AddSkillPointPacket::read);

    private final double amount;

    public AddSkillPointPacket(double amount) {
        this.amount = amount;
    }

    private static void write(RegistryFriendlyByteBuf buf, AddSkillPointPacket packet) {
        buf.writeDouble(packet.amount);
    }

    private static AddSkillPointPacket read(RegistryFriendlyByteBuf buf) {
        return new AddSkillPointPacket(buf.readDouble());
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}