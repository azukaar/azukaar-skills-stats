package com.azukaar.ass.network;

import com.azukaar.ass.AzukaarSkillsStats;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AzukaarSkillsStats.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToClient(
            PlayerSkillsSyncPayload.TYPE,
            PlayerSkillsSyncPayload.STREAM_CODEC,
            (payload, context) -> {
                // Handle on client side
                context.enqueueWork(() -> {
                    var player = context.player();
                    if (player != null) {
                        var skillsProvider = player.getData(com.azukaar.ass.AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                        skillsProvider.getSkills().setAllExperience(payload.skillData());
                    }
                });
            }
        );
    }
}