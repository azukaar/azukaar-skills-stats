package com.azukaar.ass.client;

import com.azukaar.ass.api.ActiveSkillHandler;
import com.azukaar.ass.client.gui.SkillScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_SKILLS_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            
            if (minecraft.screen == null) {
                minecraft.setScreen(new SkillScreen(minecraft.player));
            }
        }
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            return;
        }
        
        if (shouldIgnoreInput()) {
            return;
        }
        
        String skillId = KeybindRegistry.getInstance().getSkillFromKeyPress(
            event.getKey(), 
            event.getModifiers()
        );
        
        if (skillId != null) {
            Minecraft minecraft = Minecraft.getInstance();
            ActiveSkillHandler.useSkill(minecraft.player, skillId);
        }
    }
    
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            return;
        }
        
        if (shouldIgnoreInput()) {
            return;
        }
        
        String skillId = KeybindRegistry.getInstance().getSkillFromMousePress(event.getButton());
        
        if (skillId != null) {
            Minecraft minecraft = Minecraft.getInstance();
            ActiveSkillHandler.useSkill(minecraft.player, skillId);
        }
    }
    
    private static boolean shouldIgnoreInput() {
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player == null || mc.level == null) {
            return true;
        }
        
        Screen currentScreen = mc.screen;
        if (currentScreen != null) {
          return true;
        }
        
        return false;
    }
}