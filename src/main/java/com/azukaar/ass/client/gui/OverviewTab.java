package com.azukaar.ass.client.gui;

import java.util.UUID;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;


public class OverviewTab {
  static protected int renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int contentX, int contentY, Font font, Player player) {
        // Mock archetype levels
        int yOffset = 30;

        // Title
        guiGraphics.drawString(font, "Overview", contentX + 10, contentY + 10, 0xFFFFFF);
        guiGraphics.drawString(font, "Avail. Skill Points: " + PlayerData.getSkillPoints(player), contentX + 120, contentY + 10, 0xFFFF66);

        // Main Level
        guiGraphics.drawString(font, "Level " + PlayerData.getMainLevel(player), contentX + 20, contentY + yOffset, 0xFFFFFF);
        renderProgressBar(guiGraphics, contentX + 120, contentY + yOffset - 2, 100, 8, 0.75f, 0xFF6666);
        yOffset += 12;
        
        guiGraphics.drawString(font, "Archetype Levels:", contentX + 10, contentY + yOffset, 0xAAAAFF);
        yOffset += 15;
        
        // Warrior
        guiGraphics.drawString(font, "Warrior: Level " + PlayerData.getPathLevel(player, IPlayerSkills.WARRIOR_PATH), contentX + 20, contentY + yOffset, 0xFFFFFF);
        renderProgressBar(guiGraphics, contentX + 120, contentY + yOffset - 2, 100, 8, 0.75f, 0xFF6666);
        yOffset += 12;
        
        // Miner  
        guiGraphics.drawString(font, "Miner: Level " + PlayerData.getPathLevel(player, IPlayerSkills.MINER_PATH), contentX + 20, contentY + yOffset, 0xFFFFFF);
        renderProgressBar(guiGraphics, contentX + 120, contentY + yOffset - 2, 100, 8, 0.4f, 0x66FF66);
        yOffset += 12;
        
        // Explorer
        guiGraphics.drawString(font, "Explorer: Level " + PlayerData.getPathLevel(player, IPlayerSkills.EXPLORER_PATH), contentX + 20, contentY + yOffset, 0xFFFFFF);
        renderProgressBar(guiGraphics, contentX + 120, contentY + yOffset - 2, 100, 8, 0.9f, 0x6666FF);
        // yOffset += 20;
        
        // Skill points
        // guiGraphics.drawString(font, "Total Points Spent: 12", contentX + 10, contentY + yOffset, 0xCCCCCC);

        return yOffset + 10; // Return the new Y position after rendering
  }
    
  static private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
      // Background
      guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);
      
      // Progress fill
      int fillWidth = (int)(width * progress);
      guiGraphics.fill(x, y, x + fillWidth, y + height, color);
      
      // Border
      guiGraphics.fill(x - 1, y - 1, x + width + 1, y, 0xFFAAAAAA); // Top
      guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFAAAAAA); // Bottom
      guiGraphics.fill(x - 1, y, x, y + height, 0xFFAAAAAA); // Left
      guiGraphics.fill(x + width, y, x + width + 1, y + height, 0xFFAAAAAA); // Right
  }
}
