package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * Client-to-server packet: owning client reports a missile reached its target.
 * Server validates and applies damage + optional carried effect (Venomous Rebound).
 */
public record RadianceMissileHitPacket(
    int targetEntityId,
    float damage,
    RadianceMissileSpawnPayload.CarriedEffect carriedEffect
) implements CustomPacketPayload {

    public static final Type<RadianceMissileHitPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "radiance_missile_hit"));

    public static final StreamCodec<ByteBuf, RadianceMissileHitPacket> STREAM_CODEC =
        StreamCodec.of(RadianceMissileHitPacket::write, RadianceMissileHitPacket::read);

    private static void write(ByteBuf buf, RadianceMissileHitPacket packet) {
        buf.writeInt(packet.targetEntityId);
        buf.writeFloat(packet.damage);
        buf.writeBoolean(packet.carriedEffect != null);
        if (packet.carriedEffect != null) {
            byte[] bytes = packet.carriedEffect.effectId().getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            buf.writeInt(packet.carriedEffect.duration());
            buf.writeInt(packet.carriedEffect.amplifier());
            buf.writeInt(packet.carriedEffect.color());
        }
    }

    private static RadianceMissileHitPacket read(ByteBuf buf) {
        int targetEntityId = buf.readInt();
        float damage = buf.readFloat();
        RadianceMissileSpawnPayload.CarriedEffect effect = null;
        if (buf.readBoolean()) {
            int len = buf.readInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            String effectId = new String(bytes, StandardCharsets.UTF_8);
            int duration = buf.readInt();
            int amplifier = buf.readInt();
            int color = buf.readInt();
            effect = new RadianceMissileSpawnPayload.CarriedEffect(effectId, duration, amplifier, color);
        }
        return new RadianceMissileHitPacket(targetEntityId, damage, effect);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
