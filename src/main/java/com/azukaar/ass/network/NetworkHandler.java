package com.azukaar.ass.network;

import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.api.ActiveSkillHandler;
import com.azukaar.ass.api.PlayerData;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AzukaarSkillsStats.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client-to-Server packets
        registrar.playToServer(
                AddExperiencePacket.TYPE,
                AddExperiencePacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            // Execute the server-side logic
                            PlayerData.addExperienceServerSide(payload.pathName(), payload.experience(), player,
                                    payload.position());
                        }
                    });
                });

        registrar.playToServer(
                AddSkillPointPacket.TYPE,
                AddSkillPointPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            PlayerData.addSkillPointServerSide(payload.getAmount(), player);
                        }
                    });
                });

        registrar.playToServer(
                SpendSkillPointPacket.TYPE,
                SpendSkillPointPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            PlayerData.spendSkillPointServerSide(player, payload.getAmount(), payload.getSkill());
                        }
                    });
                });

        // Server-to-Client sync packet
        registrar.playToClient(
                PlayerSkillsSyncPayload.TYPE,
                PlayerSkillsSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    // Handle on client side
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var skillsProvider = player
                                    .getData(com.azukaar.ass.AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                            var skills = skillsProvider.getSkills();

                            // Sync existing data
                            skills.setAllLevels(payload.aspectLevels());
                            skills.setAllExperience(payload.xpInLevel());
                            skills.setLevelCap(payload.levelCap());
                            skills.setSkillPoints(payload.skillPoints());
                            skills.setAllSkills(payload.skillsData());

                            // NEW: Sync active skill slots
                            for (Map.Entry<Integer, String> entry : payload.activeSkillSlots().entrySet()) {
                                skills.setActiveSkillSlot(entry.getKey(), entry.getValue());
                            }

                            // NEW: Sync cooldowns
                            for (Map.Entry<String, Long> entry : payload.skillCooldowns().entrySet()) {
                                skills.setSkillCooldown(entry.getKey(), entry.getValue());
                            }

                            SkillDataManager.INSTANCE.updateAllSkillEffects(player);
                        }
                    });
                });

        registrar.playToServer(
                UseSkillPacket.TYPE,
                UseSkillPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player instanceof ServerPlayer serverPlayer) {
                            ActiveSkillHandler.useSkill(serverPlayer, payload.skillId());
                        }
                    });
                });

        // Radiance missile packets
        registrar.playToClient(
                com.azukaar.ass.trees.medic.RadianceMissileSpawnPayload.TYPE,
                com.azukaar.ass.trees.medic.RadianceMissileSpawnPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        com.azukaar.ass.trees.medic.RadianceMissileHandler.onMissileSpawn(payload);
                    });
                });

        registrar.playToServer(
                com.azukaar.ass.trees.medic.RadianceMissileHitPacket.TYPE,
                com.azukaar.ass.trees.medic.RadianceMissileHitPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player instanceof ServerPlayer serverPlayer) {
                            com.azukaar.ass.trees.medic.TreeEvents.handleMissileHit(serverPlayer, payload);
                        }
                    });
                });
    }
}