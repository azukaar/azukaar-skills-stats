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
        int yOffset = 28;

        // Header
        guiGraphics.drawString(font, "Overview", contentX + 10, contentY + 10, 0xFFDDDDDD, true);
        int sp = PlayerData.getSkillPoints(player);
        int spColor = sp > 0 ? 0xFF88DD88 : 0xFF888888;
        guiGraphics.drawString(font, sp + " Skill Points", contentX + 120, contentY + 10, spColor, false);

        // Separator
        guiGraphics.fill(contentX + 6, contentY + 22, contentX + 220, contentY + 23, 0xFF3A3A3E);

        // Main Level
        int mainLevel = PlayerData.getMainLevel(player);
        guiGraphics.drawString(font, "Level " + mainLevel, contentX + 20, contentY + yOffset, 0xFFEEEEEE, true);
        float mainProgress = PlayerData.getMainProgress(player);
        int mainBarX = contentX + 120;
        int mainBarY = contentY + yOffset - 2;
        int mainBarW = 100;
        int mainBarH = 8;
        renderProgressBar(guiGraphics, mainBarX, mainBarY, mainBarW, mainBarH, mainProgress, 0xEEEEEE);

        // Tooltip rendered separately via renderTooltips()

        yOffset += 16;

        // Separator
        guiGraphics.fill(contentX + 6, contentY + yOffset - 2, contentX + 220, contentY + yOffset - 1, 0xFF3A3A3E);

        guiGraphics.drawString(font, "Aspects", contentX + 10, contentY + yOffset, 0xFF9999AA, false);
        yOffset += 14;

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

            // Tooltip rendered separately via renderTooltips()

            yOffset += 12;
        }

        return yOffset + 10;
  }

  static protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int contentX, int contentY, Font font, Player player, double scrollOffset) {
        int yOffset = 28;

        // Main level bar tooltip
        int mainBarX = contentX + 120;
        int mainBarY = (int)(contentY + yOffset - 2 - scrollOffset);
        int mainBarW = 100;
        int mainBarH = 8;
        if (mouseX >= mainBarX && mouseX <= mainBarX + mainBarW && mouseY >= mainBarY && mouseY <= mainBarY + mainBarH) {
            int mainLevel = PlayerData.getMainLevel(player);
            double mainXp = PlayerData.getPathExperience(player, IPlayerSkills.MAIN);
            int xpForNext = PlayerData.getXpForMainLevel(mainLevel + 1);
            guiGraphics.renderTooltip(font,
                Component.literal((int) mainXp + " / " + xpForNext + " aspect level-ups"),
                mouseX, mouseY);
        }

        yOffset += 16 + 14; // separator + "Aspects" header

        for (AspectDefinition aspect : SkillDataManager.INSTANCE.getAllAspects()) {
            int barX = contentX + 120;
            int barY = (int)(contentY + yOffset - 2 - scrollOffset);
            int barW = 100;
            int barH = 8;
            if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
                int level = PlayerData.getPathLevel(player, aspect.getId());
                double currentXp = PlayerData.getPathExperience(player, aspect.getId());
                int xpForNext = PlayerData.getScaledXpForLevel(player, level + 1);
                guiGraphics.renderTooltip(font,
                    Component.literal((int) currentXp + " / " + xpForNext + " XP"),
                    mouseX, mouseY);
            }
            yOffset += 12;
        }
  }

  static private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
      // Background — dark inset
      guiGraphics.fill(x, y, x + width, y + height, 0xFF1A1A1E);

      // Progress fill
      int fillWidth = (int)(width * progress);
      if (fillWidth > 0) {
          int baseColor = 0xFF000000 | color;
          guiGraphics.fill(x, y, x + fillWidth, y + height, baseColor);
          // Highlight on top edge of fill
          guiGraphics.fill(x, y, x + fillWidth, y + 1, 0x33FFFFFF);
      }

      // Border — subtle bevel
      guiGraphics.fill(x - 1, y - 1, x + width + 1, y, 0xFF2A2A2E); // Top
      guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF444448); // Bottom
      guiGraphics.fill(x - 1, y, x, y + height, 0xFF2A2A2E); // Left
      guiGraphics.fill(x + width, y, x + width + 1, y + height, 0xFF444448); // Right
  }
}
