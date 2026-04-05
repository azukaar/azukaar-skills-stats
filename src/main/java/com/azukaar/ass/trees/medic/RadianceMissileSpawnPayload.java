package com.azukaar.ass.trees.medic;

import com.azukaar.ass.AzukaarSkillsStats;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * Server-to-client packet: tells all nearby clients to spawn a homing Radiance missile.
 * Optionally carries a single effect (for Venomous Rebound) that travels with the missile.
 */
public record RadianceMissileSpawnPayload(
    int sourcePlayerId,
    int targetEntityId,
    float damage,
    double startX,
    double startY,
    double startZ,
    CarriedEffect carriedEffect
) implements CustomPacketPayload {

    public record CarriedEffect(String effectId, int duration, int amplifier, int color) {}

    public static final Type<RadianceMissileSpawnPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "radiance_missile_spawn"));

    public static final StreamCodec<ByteBuf, RadianceMissileSpawnPayload> STREAM_CODEC =
        StreamCodec.of(RadianceMissileSpawnPayload::write, RadianceMissileSpawnPayload::read);

    private static void write(ByteBuf buf, RadianceMissileSpawnPayload payload) {
        buf.writeInt(payload.sourcePlayerId);
        buf.writeInt(payload.targetEntityId);
        buf.writeFloat(payload.damage);
        buf.writeDouble(payload.startX);
        buf.writeDouble(payload.startY);
        buf.writeDouble(payload.startZ);
        buf.writeBoolean(payload.carriedEffect != null);
        if (payload.carriedEffect != null) {
            byte[] bytes = payload.carriedEffect.effectId.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            buf.writeInt(payload.carriedEffect.duration);
            buf.writeInt(payload.carriedEffect.amplifier);
            buf.writeInt(payload.carriedEffect.color);
        }
    }

    private static RadianceMissileSpawnPayload read(ByteBuf buf) {
        int sourcePlayerId = buf.readInt();
        int targetEntityId = buf.readInt();
        float damage = buf.readFloat();
        double startX = buf.readDouble();
        double startY = buf.readDouble();
        double startZ = buf.readDouble();
        CarriedEffect effect = null;
        if (buf.readBoolean()) {
            int len = buf.readInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            String effectId = new String(bytes, StandardCharsets.UTF_8);
            int duration = buf.readInt();
            int amplifier = buf.readInt();
            int color = buf.readInt();
            effect = new CarriedEffect(effectId, duration, amplifier, color);
        }
        return new RadianceMissileSpawnPayload(sourcePlayerId, targetEntityId, damage, startX, startY, startZ, effect);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
