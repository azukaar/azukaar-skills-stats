package com.azukaar.ass.client;

import com.azukaar.ass.api.ActiveSkillHandler;
import com.azukaar.ass.client.gui.SkillScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class GUIClientModEvents {
    
    public static final KeyMapping OPEN_SKILLS_KEY = new KeyMapping(
        "key.ass.open_skills",
        GLFW.GLFW_KEY_H,
        "key.categories.gameplay"
    );
  
    public static final KeyMapping TEST_KEY = new KeyMapping(
        "key.ass.test",
        GLFW.GLFW_KEY_G,
        "key.categories.gameplay"
    );
  
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_SKILLS_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            
            if (minecraft.screen == null) {
                minecraft.setScreen(new SkillScreen(minecraft.player));
            }
        }

        while (TEST_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            
            if (minecraft.screen == null) {
                System.out.println("CAACA");
                ActiveSkillHandler.useSkill(minecraft.player, "azukaarskillsstats:heal");
            }
        }
    }
}