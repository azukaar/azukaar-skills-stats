package com.azukaar.ass.network;

import com.azukaar.ass.AzukaarSkillsStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// Packet for adding experience
public record AddExperiencePacket(String pathName, double experience, Vec3 position) implements CustomPacketPayload {
    public static final Type<AddExperiencePacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "add_experience"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddExperiencePacket> STREAM_CODEC = 
        StreamCodec.of(AddExperiencePacket::write, AddExperiencePacket::read);

    private static void write(RegistryFriendlyByteBuf buf, AddExperiencePacket packet) {
        buf.writeUtf(packet.pathName);
        buf.writeDouble(packet.experience);
        buf.writeDouble(packet.position.x);
        buf.writeDouble(packet.position.y);
        buf.writeDouble(packet.position.z);
    }

    private static AddExperiencePacket read(RegistryFriendlyByteBuf buf) {
        String pathName = buf.readUtf();
        double experience = buf.readDouble();
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new AddExperiencePacket(pathName, experience, position);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
