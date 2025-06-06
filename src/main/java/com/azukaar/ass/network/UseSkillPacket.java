package com.azukaar.ass.network;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.ActiveSkillHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UseSkillPacket(String skillId) implements CustomPacketPayload {
    public static final Type<UseSkillPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "use_skill"));

    public static final StreamCodec<ByteBuf, UseSkillPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, UseSkillPacket::skillId,
        UseSkillPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UseSkillPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ActiveSkillHandler.useSkill(serverPlayer, packet.skillId());
            }
        });
    }
}