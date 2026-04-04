package com.azukaar.ass.client.gui;

import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;


public class OverviewTab {
  private static final boolean DEBUG_REPEAT = false;
  private static final int DEBUG_REPEAT_COUNT = 3;

  static protected int renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int contentX, int contentY, Font font, Player player) {
        int yOffset = 30;

        // Title
        guiGraphics.drawString(font, "Overview", contentX + 10, contentY + 10, 0xFFFFFF);
        guiGraphics.drawString(font, "Avail. Skill Points: " + PlayerData.getSkillPoints(player), contentX + 120, contentY + 10, 0xFFFF66);

        // Main Level
        int mainLevel = PlayerData.getMainLevel(player);
        guiGraphics.drawString(font, "Level " + mainLevel, contentX + 20, contentY + yOffset, 0xFFFFFF);
        float mainProgress = PlayerData.getMainProgress(player);
        int mainBarX = contentX + 120;
        int mainBarY = contentY + yOffset - 2;
        int mainBarW = 100;
        int mainBarH = 8;
        renderProgressBar(guiGraphics, mainBarX, mainBarY, mainBarW, mainBarH, mainProgress, 0xEEEEEE);

        if (mouseX >= mainBarX && mouseX <= mainBarX + mainBarW && mouseY >= mainBarY && mouseY <= mainBarY + mainBarH) {
            double mainXp = PlayerData.getPathExperience(player, IPlayerSkills.MAIN);
            int xpForNext = PlayerData.getXpForMainLevel(mainLevel + 1);
            guiGraphics.renderTooltip(font,
                Component.literal((int) mainXp + " / " + xpForNext + " aspect level-ups"),
                mouseX, mouseY);
        }

        yOffset += 12;

        guiGraphics.drawString(font, "Aspect Levels:", contentX + 10, contentY + yOffset, 0xAAAAFF);
        yOffset += 15;

        java.util.List<AspectDefinition> aspects = new java.util.ArrayList<>(SkillDataManager.INSTANCE.getAllAspects());
        if (DEBUG_REPEAT) {
            java.util.List<AspectDefinition> repeated = new java.util.ArrayList<>();
            for (int r = 0; r < DEBUG_REPEAT_COUNT; r++) {
                repeated.addAll(aspects);
            }
            aspects = repeated;
        }
        for (AspectDefinition aspect : aspects) {
            String name = aspect.getDisplayNameString() != null ? aspect.getDisplayNameString() : aspect.getId();
            int level = PlayerData.getPathLevel(player, aspect.getId());
            guiGraphics.drawString(font, name + ": lvl " + level, contentX + 20, contentY + yOffset, 0xFFFFFF);
            float progress = PlayerData.getPathProgress(player, aspect.getId());
            int barX = contentX + 120;
            int barY = contentY + yOffset - 2;
            int barW = 100;
            int barH = 8;
            renderProgressBar(guiGraphics, barX, barY, barW, barH, progress, aspect.getColor());

            // Tooltip on hover
            if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
                double currentXp = PlayerData.getPathExperience(player, aspect.getId());
                int xpForNext = PlayerData.getScaledXpForLevel(player, level + 1);
                guiGraphics.renderTooltip(font,
                    Component.literal((int) currentXp + " / " + xpForNext + " XP"),
                    mouseX, mouseY);
            }

            yOffset += 12;
        }

        return yOffset + 10;
  }

  static private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
      // Background
      guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);

      // Progress fill (add full alpha to RGB color)
      int fillWidth = (int)(width * progress);
      guiGraphics.fill(x, y, x + fillWidth, y + height, 0xFF000000 | color);

      // Border
      guiGraphics.fill(x - 1, y - 1, x + width + 1, y, 0xFFAAAAAA); // Top
      guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFAAAAAA); // Bottom
      guiGraphics.fill(x - 1, y, x, y + height, 0xFFAAAAAA); // Left
      guiGraphics.fill(x + width, y, x + width + 1, y + height, 0xFFAAAAAA); // Right
  }
}
