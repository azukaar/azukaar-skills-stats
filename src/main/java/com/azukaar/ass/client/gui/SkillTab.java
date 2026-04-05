package com.azukaar.ass.client.gui;

import java.util.List;
import java.util.Map;

import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.Utils;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.client.KeybindRegistry;
import com.azukaar.ass.types.IconData;
import com.azukaar.ass.types.ScalingData;
import com.azukaar.ass.types.Skill;
import com.azukaar.ass.types.SkillTree;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SkillTab {
    private final String displayName;
    private final IconData icon;
    private final String skillTree;

    // Upgrade button properties for modal
    public int upgradeButtonX, upgradeButtonY, upgradeButtonWidth, upgradeButtonHeight;
    public boolean upgradeButtonEnabled;
    public boolean upgradeButtonVisible;

    // Keybind input properties
    public int keybindInputX, keybindInputY, keybindInputWidth, keybindInputHeight;
    public int keybindClearX, keybindClearY, keybindClearSize;
    public boolean keybindClearVisible;
    public boolean keybindInputVisible;
    public boolean keybindInputFocused;
    public String currentKeybind = ""; // Stores the current keybind (empty if none)
    public boolean waitingForKeybind = false; // True when waiting for user to press a key

    // Modal scroll pane
    private ScrollablePane modalPane;
    private String lastModalSkillId;

    SkillTab(String displayName, IconData icon, String skillTree) {
        this.displayName = displayName;
        this.icon = icon;
        this.skillTree = skillTree;
        this.upgradeButtonVisible = false;
        this.keybindInputVisible = false;
        this.keybindInputFocused = false;
    }

    protected void renderModal(Skill selectedSkill, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int contentX, int contentY, Font font, Player player, int leftPos, int topPos) {
        if (selectedSkill == null) return;

        if("".equals(currentKeybind)) {
            currentKeybind = KeybindRegistry.getInstance().getSkillKeybind(selectedSkill.getId());
        }
        
        // Modal dimensions
        int modalWidth = 230;
        int baseModalHeight = 160;
        
        // Track if skill has active effects
        boolean hasActiveEffects = false;
        
        // Center the modal on screen
        int modalX = leftPos + (SkillScreen.SCREEN_WIDTH - modalWidth) / 2;
        int modalY = topPos + (SkillScreen.SCREEN_HEIGHT - baseModalHeight) / 2;
        
        // Content positioning
        int contentStartX = modalX + 10;
        int contentStartY = modalY + 10;
        int lineHeight = 12;
        int currentY = contentStartY;
        
        // Calculate content height first to determine modal size
        int tempY = currentY;
        tempY += 24; // Icon and title
        tempY += lineHeight + 4; // Level text
        
        // Check for description
        if(selectedSkill.getDescription() != null && !"".equals(selectedSkill.getDescription())) {
            tempY += lineHeight + 4;
        }
        
        // Check for cooldown
        if (selectedSkill.hasCooldown()) {
            tempY += lineHeight + 4;
        }
        
        // Check for effects and count lines
        int effectLines = 0;
        try {
            SkillDataManager skillDataManager = SkillDataManager.INSTANCE;
            SkillEffect skillEffect = skillDataManager.getSkillEffects(selectedSkill.getId());
            
            if (skillEffect != null && skillEffect.getEffects() != null && !skillEffect.getEffects().isEmpty()) {
                tempY += lineHeight; // "Effects:" line
                effectLines++;
                
                for (SkillEffect.Effect effect : skillEffect.getEffects()) {
                    if ("active".equals(effect.getType()) || "potion".equals(effect.getType())) {
                        hasActiveEffects = true;
                    }
                    tempY += lineHeight;
                    effectLines++;
                }
            }
        } catch (Exception e) {
            tempY += lineHeight; // Fallback line
            effectLines++;
        }
        
        // Add space for keybind input if needed
        if (hasActiveEffects) {
            tempY += 20; // Keybind input height
        }
        
        tempY += 10; // Bottom padding
        
        // Calculate final modal height (purposefully disabled for now)
        // int modalHeight = Math.max(baseModalHeight, tempY - modalY + 10);
        int modalHeight = baseModalHeight;
        
        // Re-center modal with new height
        // modalY = topPos + (SkillScreen.SCREEN_HEIGHT - modalHeight) / 2;
        
        // Push entire modal above tab item icons (renderItem uses z~150 internally)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);

        // Render semi-transparent backdrop
        guiGraphics.fill(leftPos, topPos, leftPos + SkillScreen.SCREEN_WIDTH, topPos + SkillScreen.SCREEN_HEIGHT, 0x88000000);

        // Modal shadow (offset dark rect behind modal)
        guiGraphics.fill(modalX + 2, modalY + 2, modalX + modalWidth + 2, modalY + modalHeight + 2, 0x40000000);

        // Modal background
        guiGraphics.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, 0xFF1E1E22);

        // Outer border - subtle highlight
        guiGraphics.fill(modalX, modalY, modalX + modalWidth, modalY + 1, 0xFF555555); // Top
        guiGraphics.fill(modalX, modalY + modalHeight - 1, modalX + modalWidth, modalY + modalHeight, 0xFF333333); // Bottom
        guiGraphics.fill(modalX, modalY, modalX + 1, modalY + modalHeight, 0xFF555555); // Left
        guiGraphics.fill(modalX + modalWidth - 1, modalY, modalX + modalWidth, modalY + modalHeight, 0xFF333333); // Right

        // Inner border - slight bevel
        guiGraphics.fill(modalX + 1, modalY + 1, modalX + modalWidth - 1, modalY + 2, 0xFF3A3A3E); // Top inner
        guiGraphics.fill(modalX + 1, modalY + 1, modalX + 2, modalY + modalHeight - 1, 0xFF3A3A3E); // Left inner

        // Content positioning
        contentStartX = modalX + 10;
        contentStartY = modalY + 8;
        currentY = contentStartY;

        // Header area: icon + name
        selectedSkill.getIconData().render(guiGraphics, contentStartX, currentY);
        guiGraphics.drawString(font, selectedSkill.getDisplayName(),
            contentStartX + 20, currentY + 4, 0xFFFFFF, true);

        currentY += 22;

        // Header separator line
        guiGraphics.fill(modalX + 6, currentY, modalX + modalWidth - 6, currentY + 1, 0xFF3A3A3E);
        currentY += 5;
                
        // Render keybind input if skill has active effects
        if (hasActiveEffects) {
            // currentY += 4;
            
            // Keybind input dimensions
            int inputWidth = 100;
            int inputHeight = 16;
            int inputX = contentStartX + 45;
            int inputY = currentY;
            
            // Render keybind label
            guiGraphics.drawString(font, "Keybind:", inputX - 45, inputY + inputHeight/2 - lineHeight/2 + 3, 0xFF9999AA, false);

            // Render input background
            int inputBgColor = keybindInputFocused ? 0xFF3A3A4A : 0xFF2A2A2E;
            int inputBorderColor = keybindInputFocused ? 0xFF6677CC : 0xFF444448;
            
            guiGraphics.fill(inputX, inputY, inputX + inputWidth, inputY + inputHeight, inputBgColor);
            guiGraphics.fill(inputX, inputY, inputX + inputWidth, inputY + 1, inputBorderColor); // Top
            guiGraphics.fill(inputX, inputY + inputHeight - 1, inputX + inputWidth, inputY + inputHeight, inputBorderColor); // Bottom
            guiGraphics.fill(inputX, inputY, inputX + 1, inputY + inputHeight, inputBorderColor); // Left
            guiGraphics.fill(inputX + inputWidth - 1, inputY, inputX + inputWidth, inputY + inputHeight, inputBorderColor); // Right
            
            // Render input text
            String inputText;
            int inputTextColor = 0xFFFFFF;
            
            if (waitingForKeybind) {
                inputText = "Press key...";
                inputTextColor = 0xFFFF88;
            } else if (currentKeybind.isEmpty()) {
                inputText = "Click to set";
                inputTextColor = 0x888888;
            } else {
                inputText = currentKeybind;
                inputTextColor = 0xFFFFFF;
            }
            
            guiGraphics.drawString(font, inputText, inputX + 4, inputY + 4, inputTextColor, false);
            
            // Store input bounds for click handling
            this.keybindInputX = inputX;
            this.keybindInputY = inputY;
            this.keybindInputWidth = inputWidth;
            this.keybindInputHeight = inputHeight;
            this.keybindInputVisible = true;

            // Render clear button (X) next to input if a keybind is set
            if (!currentKeybind.isEmpty() && !waitingForKeybind) {
                int clearSize = inputHeight;
                int clearX = inputX + inputWidth + 2;
                int clearY = inputY;

                guiGraphics.fill(clearX, clearY, clearX + clearSize, clearY + clearSize, 0xFF3A2A2A);
                guiGraphics.fill(clearX, clearY, clearX + clearSize, clearY + 1, 0xFF664444);
                guiGraphics.fill(clearX, clearY + clearSize - 1, clearX + clearSize, clearY + clearSize, 0xFF664444);
                guiGraphics.fill(clearX, clearY, clearX + 1, clearY + clearSize, 0xFF664444);
                guiGraphics.fill(clearX + clearSize - 1, clearY, clearX + clearSize, clearY + clearSize, 0xFF664444);

                int xTextWidth = font.width("\u2715");
                guiGraphics.drawString(font, "\u2715",
                    clearX + (clearSize - xTextWidth) / 2,
                    clearY + 4, 0xCC6666, false);

                this.keybindClearX = clearX;
                this.keybindClearY = clearY;
                this.keybindClearSize = clearSize;
                this.keybindClearVisible = true;
            } else {
                this.keybindClearVisible = false;
            }

            currentY += inputHeight + 6;
        } else {
            this.keybindInputVisible = false;
            this.keybindClearVisible = false;
        }
        
        // Scrollable content area for level/description/cooldown/effects
        int scrollAreaY = currentY;
        int scrollAreaHeight = modalY + modalHeight - scrollAreaY - 2;
        int scrollAreaWidth = modalWidth - 20;

        // Reset modal pane when skill changes
        if (!selectedSkill.getId().equals(lastModalSkillId)) {
            lastModalSkillId = selectedSkill.getId();
            modalPane = null;
        }
        if (modalPane == null) {
            modalPane = new ScrollablePane(contentStartX, scrollAreaY, scrollAreaWidth, scrollAreaHeight);
        } else {
            modalPane.setPosition(contentStartX, scrollAreaY, scrollAreaWidth, scrollAreaHeight);
        }

        final int fContentStartX = contentStartX;
        final int fCurrentLevel = PlayerData.getSkillLevel(player, selectedSkill.getId());
        final int fMaxLevel = selectedSkill.getMaxLevel();

        modalPane.render(guiGraphics, (g, px, py) -> {
            int cy = py;

            // Level info — compact badge style
            String levelText;
            int levelColor;
            if (fMaxLevel == 1) {
                levelText = fCurrentLevel > 0 ? "\u2713 Unlocked" : "\u2717 Locked";
                levelColor = fCurrentLevel > 0 ? 0xFF55CC55 : 0xFFAA5555;
            } else {
                levelText = "Level " + fCurrentLevel + " / " + fMaxLevel;
                levelColor = fCurrentLevel > 0 ? 0xFF66AAEE : 0xFFAA5555;
            }
            g.drawString(font, levelText, fContentStartX, cy, levelColor, false);
            cy += lineHeight + 4;

            // Description
            if (selectedSkill.getDescription() != null && !"".equals(selectedSkill.getDescription())) {
                List<String> descLines = Utils.wrapText(selectedSkill.getDescription(), 35);
                for (String line : descLines) {
                    g.drawString(font, line, fContentStartX, cy, 0xCCCCCC, false);
                    cy += lineHeight;
                }
                cy += 4;
            }

            // Cooldown
            if (selectedSkill.hasCooldown()) {
                int cd = selectedSkill.getEffectiveCooldown(Math.max(1, fCurrentLevel));
                g.drawString(font, "Cooldown: " + cd + " seconds", fContentStartX, cy, 0xFFFF88, false);
                cy += lineHeight + 4;
            }

            // Effects
            try {
                SkillEffect skillEffect = SkillDataManager.INSTANCE.getSkillEffects(selectedSkill.getId());
                if (skillEffect != null && skillEffect.getEffects() != null && !skillEffect.getEffects().isEmpty()) {
                    // Separator before effects
                    cy += 2;
                    g.fill(fContentStartX, cy, fContentStartX + scrollAreaWidth - 10, cy + 1, 0xFF3A3A3E);
                    cy += 5;
                    g.drawString(font, "Effects:", fContentStartX, cy, 0xFF9999AA, false);
                    cy += lineHeight;

                    for (SkillEffect.Effect effect : skillEffect.getEffects()) {
                        String effectText = "";
                        int effectColor = 0xAAAAAA;

                        if ("attribute_modifier".equals(effect.getType())) {
                            double perLevelValue = effect.getValue();
                            double currentValue = effect.getScaling().calculateValue(perLevelValue, Math.max(1, fCurrentLevel));
                            String operation = effect.getOperation();
                            String displayAttribute = Utils.toDisplayString(effect.getAttribute());
                            if (effect.getDescription() != null && !"".equals(effect.getDescription()))
                                displayAttribute = effect.getDescription();

                            boolean isPercent = "add_multiplied_base".equals(operation) || "multiply_base".equals(operation)
                                || "add_multiplied_total".equals(operation) || "multiply_total".equals(operation);
                            String suffix = isPercent ? "%" : "";
                            double displayPerLevel = isPercent ? perLevelValue * 100 : perLevelValue;
                            double displayCurrent = isPercent ? currentValue * 100 : currentValue;

                            if ("add_value".equals(operation) || "add".equals(operation)) {
                                effectText = "+" + perLevelValue + " " + displayAttribute + " per lvl";
                            } else if ("add_multiplied_base".equals(operation) || "multiply_base".equals(operation)) {
                                effectText = "+" + displayPerLevel + "% " + displayAttribute + " per lvl";
                            } else if ("add_multiplied_total".equals(operation) || "multiply_total".equals(operation)) {
                                effectText = "+" + displayPerLevel + "% total " + displayAttribute + " per lvl";
                            } else {
                                effectText = displayPerLevel + suffix + " " + displayAttribute;
                            }
                            effectColor = 0x00FF88;
                        } else if ("active".equals(effect.getType())) {
                            effectText = Utils.toDisplayString(effect.getActiveEffectId());
                            if (effect.getDescription() != null && !"".equals(effect.getDescription()))
                                effectText = effect.getDescription();
                            effectColor = 0xFF8800;
                        } else if ("custom_attribute_modifier".equals(effect.getType())) {
                            effectText = (effect.getDescription() != null && !"".equals(effect.getDescription()))
                                ? effect.getDescription() : Utils.toDisplayString(effect.getAttribute());
                            effectColor = 0x8800FF;
                        } else if ("potion".equals(effect.getType())) {
                            String potionName = Utils.toDisplayString(effect.getPotion());
                            int durationSecs = effect.getDuration(Math.max(1, fCurrentLevel)) / 20;
                            int lvl = effect.getAmplifier(Math.max(1, fCurrentLevel)) + 1;
                            effectText = potionName + " " + toRomanNumeral(lvl) + " (" + durationSecs + "s)";
                            effectColor = 0xFF88FF;
                        }

                        if (!effectText.isEmpty()) {
                            List<String> wrappedEffectText = Utils.wrapText(effectText, 32);
                            boolean firstLine = true;
                            for (String line : wrappedEffectText) {
                                g.drawString(font, (firstLine ? " \u2022 " : "   ") + line, fContentStartX, cy, effectColor, false);
                                firstLine = false;
                                cy += lineHeight;
                            }

                            // Show current/next totals for multi-level attribute modifiers
                            if ("attribute_modifier".equals(effect.getType()) && fMaxLevel > 1 && fCurrentLevel > 0) {
                                double perLvl = effect.getValue();
                                boolean isPct = "add_multiplied_base".equals(effect.getOperation())
                                    || "multiply_base".equals(effect.getOperation())
                                    || "add_multiplied_total".equals(effect.getOperation())
                                    || "multiply_total".equals(effect.getOperation());
                                double cur = effect.getScaling().calculateValue(perLvl, fCurrentLevel);
                                if (isPct) cur *= 100;
                                String sfx = isPct ? "%" : "";
                                String currentNextText;
                                if (fCurrentLevel < fMaxLevel) {
                                    double nxt = effect.getScaling().calculateValue(perLvl, fCurrentLevel + 1);
                                    if (isPct) nxt *= 100;
                                    currentNextText = "  (Current: +" + cur + sfx + ", next: +" + nxt + sfx + ")";
                                } else {
                                    currentNextText = "  (Current: +" + cur + sfx + ", max)";
                                }
                                g.drawString(font, Component.literal(currentNextText).withStyle(net.minecraft.ChatFormatting.ITALIC), fContentStartX + 10, cy, 0x888888, false);
                                cy += lineHeight;
                            }

                            Map<String, ScalingData> data = effect.getData();
                            if (data != null && !data.isEmpty()) {
                                int skillLevel = Math.max(PlayerData.getSkillLevel(player, selectedSkill.getId()), 1);
                                for (Map.Entry<String, ScalingData> dataEntry : data.entrySet()) {
                                    double val = dataEntry.getValue().getValue(skillLevel);
                                    String dataText = "  " + Utils.toDisplayString(dataEntry.getKey()) + ": " + val;
                                    g.drawString(font, dataText, fContentStartX + 20, cy, 0xAAAAAA, false);
                                    cy += lineHeight;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                g.drawString(font, "No effect data available", fContentStartX, cy, 0x888888, false);
            }

            return cy - py;
        });
        
        // Upgrade button section
        currentY += 4;
        int buttonWidth = 90;
        int buttonHeight = 16;
        int buttonX = contentStartX + modalWidth - buttonWidth - 20;
        int buttonY = contentStartY;
        
        // Check if skill can be upgraded
        int currentLevel = fCurrentLevel;
        int maxLevel = fMaxLevel;
        boolean canUpgrade = currentLevel < maxLevel;
        boolean prerequisitesMet = selectedSkill.arePrerequisitesMet(player);
        int upgradeCost = 1;
        int availablePoints = PlayerData.getSkillPoints(player);
        boolean canAfford = availablePoints >= upgradeCost;
        boolean buttonEnabled = canUpgrade && canAfford && prerequisitesMet;
        
        // Render upgrade button
        int buttonBgColor, buttonBorderTop, buttonBorderBottom;
        String buttonText;
        int buttonTextColor;

        if (!canUpgrade) {
            buttonBgColor = 0xFF3A3A3E;
            buttonBorderTop = 0xFF4A4A4E;
            buttonBorderBottom = 0xFF2A2A2E;
            buttonText = maxLevel == 1 ? "\u2713 Unlocked" : "Max Level";
            buttonTextColor = 0xFF888888;
        } else if (!prerequisitesMet) {
            buttonBgColor = 0xFF3A2A2A;
            buttonBorderTop = 0xFF4A3A3A;
            buttonBorderBottom = 0xFF2A1A1A;
            buttonText = "\u26D4 Locked";
            buttonTextColor = 0xFFAA6666;
        } else if (!canAfford) {
            buttonBgColor = 0xFF3A3A2A;
            buttonBorderTop = 0xFF4A4A3A;
            buttonBorderBottom = 0xFF2A2A1A;
            buttonText = "Need " + upgradeCost + " SP";
            buttonTextColor = 0xFFAAAA66;
        } else {
            buttonBgColor = 0xFF2A4A2E;
            buttonBorderTop = 0xFF3A6A3E;
            buttonBorderBottom = 0xFF1A3A1E;
            buttonText = "\u25B2 Upgrade (" + upgradeCost + " SP)";
            buttonTextColor = 0xFF88DD88;
        }

        guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonBgColor);
        guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 1, buttonBorderTop);
        guiGraphics.fill(buttonX, buttonY + buttonHeight - 1, buttonX + buttonWidth, buttonY + buttonHeight, buttonBorderBottom);
        guiGraphics.fill(buttonX, buttonY, buttonX + 1, buttonY + buttonHeight, buttonBorderTop);
        guiGraphics.fill(buttonX + buttonWidth - 1, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonBorderBottom);

        int buttonTextWidth = font.width(buttonText);
        guiGraphics.drawString(font, buttonText,
            buttonX + (buttonWidth - buttonTextWidth) / 2,
            buttonY + 4, buttonTextColor, false);
        
        // Store button bounds for click handling
        this.upgradeButtonX = buttonX;
        this.upgradeButtonY = buttonY;
        this.upgradeButtonWidth = buttonWidth;
        this.upgradeButtonHeight = buttonHeight;
        this.upgradeButtonEnabled = buttonEnabled;
        this.upgradeButtonVisible = true;

        // Tooltip on locked button showing missing prerequisites
        if (!prerequisitesMet && mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            for (String missing : selectedSkill.getMissingPrerequisites(player)) {
                tooltip.add(Component.literal(missing).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        guiGraphics.pose().popPose();
    }
    
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int contentX, int contentY, Font font, Player player, int leftPos, int topPos) {
        // Render skill connections first (so they appear behind nodes)
        renderSkillConnections(guiGraphics, getSkillTree(), leftPos, topPos, player);

        // Render skill nodes
        renderSkillNodes(guiGraphics, getSkillTree(), mouseX, mouseY, font, player, leftPos, topPos);
    }

    private void renderSkillConnections(GuiGraphics guiGraphics, SkillTree tree, int leftPos, int topPos, Player player) {
        for (Skill skill : tree.getAllSkills()) {
            for (Skill child : skill.getChildren()) {
                renderConnection(guiGraphics, skill, child, leftPos, topPos, player);
            }
        }
    }

    private void renderConnection(GuiGraphics guiGraphics, Skill from, Skill to, int leftPos, int topPos, Player player) {
        int fromX = (int)from.getX() + (leftPos + SkillScreen.WINDOW_INSIDE_X) + (SkillScreen.SCREEN_WIDTH-SkillScreen.WINDOW_INSIDE_X) / 2;
        int fromY = (int)from.getY() + (topPos + SkillScreen.WINDOW_INSIDE_Y) + (SkillScreen.SCREEN_HEIGHT-SkillScreen.WINDOW_INSIDE_Y) / 2;

        int toX = (int)to.getX() + (leftPos + SkillScreen.WINDOW_INSIDE_X) + (SkillScreen.SCREEN_WIDTH-SkillScreen.WINDOW_INSIDE_X) / 2;
        int toY = (int)to.getY() + (topPos + SkillScreen.WINDOW_INSIDE_Y) + (SkillScreen.SCREEN_HEIGHT-SkillScreen.WINDOW_INSIDE_Y) / 2;

        boolean dependencyMet = to.arePrerequisitesMet(player);
        int toLevel = PlayerData.getSkillLevel(player, to.getId());

        int color;
        if (toLevel > 0) {
            color = 0xFF44AA88; // Teal — completed path
        } else if (dependencyMet) {
            color = 0xFF4477AA; // Blue — available
        } else {
            color = 0xFF3A3A3E; // Dark gray — locked
        }

        drawLine(guiGraphics, fromX, fromY, toX, toY, color);
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        drawThickLine(guiGraphics, x1, y1, x2, y2, 2, color);
    }

    // Helper to draw a line with thickness using Bresenham's algorithm
    private void drawThickLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int thickness, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Draw a square of size thickness x thickness centered at (x1, y1)
            int half = thickness / 2;
            guiGraphics.fill(x1 - half, y1 - half, x1 - half + thickness, y1 - half + thickness, color);

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void renderSkillNodes(GuiGraphics guiGraphics, SkillTree tree, int mouseX, int mouseY, Font font, Player player, int leftPos, int topPos) {
        for (Skill skill : tree.getAllSkills()) {
            renderSkillNode(guiGraphics, skill, font, player, leftPos, topPos);
        }
    }

    private void renderSkillNode(GuiGraphics guiGraphics, Skill skill, Font font, Player player, int leftPos, int topPos) {
        int x = (int)skill.getX() + (leftPos + SkillScreen.WINDOW_INSIDE_X) + (SkillScreen.SCREEN_WIDTH-SkillScreen.WINDOW_INSIDE_X) / 2 - SkillScreen.SKILL_NODE_SIZE / 2;
        int y = (int)skill.getY() + (topPos + SkillScreen.WINDOW_INSIDE_Y) + (SkillScreen.SCREEN_HEIGHT-SkillScreen.WINDOW_INSIDE_Y) / 2 - SkillScreen.SKILL_NODE_SIZE / 2;

        int currentLevel = PlayerData.getSkillLevel(player, skill.getId());
        boolean prerequisitesMet = skill.arePrerequisitesMet(player);

        int s = SkillScreen.SKILL_NODE_SIZE;

        // Background + border colors based on state
        int bgColor, borderTop, borderBottom;
        if (currentLevel > 0 && currentLevel == skill.getMaxLevel()) {
            bgColor = 0xFF1A3A1A;       // Dark green — maxed
            borderTop = 0xFF44AA44;
            borderBottom = 0xFF1A551A;
        } else if (currentLevel > 0) {
            bgColor = 0xFF1A2A3A;       // Dark blue — in progress
            borderTop = 0xFF4488CC;
            borderBottom = 0xFF1A3355;
        } else if (prerequisitesMet) {
            bgColor = 0xFF2A2A2E;       // Dark — available
            borderTop = 0xFF5577BB;
            borderBottom = 0xFF2A3355;
        } else {
            bgColor = 0xFF1E1E22;       // Darkest — locked
            borderTop = 0xFF444448;
            borderBottom = 0xFF2A2A2E;
        }

        // Background fill
        guiGraphics.fill(x, y, x + s, y + s, bgColor);

        // Beveled border
        guiGraphics.fill(x, y, x + s, y + 1, borderTop);           // Top
        guiGraphics.fill(x, y + s - 1, x + s, y + s, borderBottom); // Bottom
        guiGraphics.fill(x, y, x + 1, y + s, borderTop);           // Left
        guiGraphics.fill(x + s - 1, y, x + s, y + s, borderBottom); // Right

        // Inner highlight (subtle)
        guiGraphics.fill(x + 1, y + 1, x + s - 1, y + 2, 0x15FFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + s - 1, 0x15FFFFFF);
        
        // Render skill icon (dimmed if locked)
        IconData iconData = skill.getIconData();
        float iconAlpha = (currentLevel == 0 && !prerequisitesMet) ? 0.35f : 1.0f;
        iconData.render(guiGraphics, x + 4, y + 4, iconAlpha);
        
        // Render level indicator if skill has levels (above item icon z)
        if (currentLevel > 0 && skill.getMaxLevel() > 1) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 160);
            String levelText = String.valueOf(currentLevel);
            int textWidth = font.width(levelText);
            int lx = x + s - textWidth - 3;
            int ly = y + s - 9;
            guiGraphics.fill(lx - 1, ly - 1, lx + textWidth + 1, ly + 8, 0xAA000000);
            guiGraphics.drawString(font, levelText, lx, ly, 0xFFDDDDDD, false);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Handle click events on the modal (upgrade button and keybind input)
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param selectedSkill The currently selected skill
     * @param player The player
     * @return true if the click was handled (consumed), false otherwise
     */
    public boolean handleModalClick(double mouseX, double mouseY, Skill selectedSkill, Player player) {
        if (selectedSkill == null) return false;
        
        // Handle keybind clear button click
        if (keybindClearVisible &&
            mouseX >= keybindClearX && mouseX <= keybindClearX + keybindClearSize &&
            mouseY >= keybindClearY && mouseY <= keybindClearY + keybindClearSize) {

            currentKeybind = "";
            waitingForKeybind = false;
            keybindInputFocused = false;
            KeybindRegistry.getInstance().removeSkillKeybind(selectedSkill.getId());
            return true;
        }

        // Handle keybind input click
        if (keybindInputVisible &&
            mouseX >= keybindInputX && mouseX <= keybindInputX + keybindInputWidth &&
            mouseY >= keybindInputY && mouseY <= keybindInputY + keybindInputHeight) {
            
            keybindInputFocused = !keybindInputFocused;
            if (keybindInputFocused) {
                waitingForKeybind = true;
                System.out.println("Keybind input focused, waiting for key press");
            }
            return true; // Consumed the click
        }
        
        // Handle upgrade button click
        if (upgradeButtonVisible &&
            mouseX >= upgradeButtonX && mouseX <= upgradeButtonX + upgradeButtonWidth &&
            mouseY >= upgradeButtonY && mouseY <= upgradeButtonY + upgradeButtonHeight) {
            
            if (upgradeButtonEnabled) {
                // Perform the upgrade
                int currentLevel = PlayerData.getSkillLevel(player, selectedSkill.getId());
                if (currentLevel < selectedSkill.getMaxLevel()) {
                    System.out.println("Spending skill point on: " + selectedSkill.getId());
                    PlayerData.spendSkillPoint(player, 1, selectedSkill.getId());
                    return true; // Consumed the click
                }
            }
            return true; // Consumed the click even if disabled
        }
        
        // Click outside of interactive elements - unfocus keybind input
        if (keybindInputFocused) {
            keybindInputFocused = false;
            waitingForKeybind = false;
        }
        
        return false; // Click not on any interactive element
    }

    /**
     * Handle key press events for keybind input
     * @param keyCode The key code that was pressed
     * @param scanCode The scan code
     * @param modifiers Key modifiers (Ctrl, Shift, Alt)
     * @param selectedSkill The currently selected skill
     * @return true if the key press was handled (consumed), false otherwise
     */
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers, Skill selectedSkill) {
        if (!waitingForKeybind || !keybindInputFocused || selectedSkill == null) {
            return false;
        }
        
        // Handle ESC key to cancel keybind setting
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            waitingForKeybind = false;
            keybindInputFocused = false;
            System.out.println("Keybind setting cancelled");
            return true;
        }
        
        // Handle DELETE/BACKSPACE to clear keybind
        if (keyCode == 261 || keyCode == 259) { // GLFW.GLFW_KEY_DELETE or GLFW.GLFW_KEY_BACKSPACE
            currentKeybind = "";
            waitingForKeybind = false;
            keybindInputFocused = false;
            System.out.println("Keybind cleared for skill: " + selectedSkill.getId());
            // TODO: Save the cleared keybind to your keybind storage system
            return true;
        }
        
        // Convert keyCode to readable string
        String keyName = getKeyName(keyCode);
        if (keyName == null || keyName.isEmpty()) {
            return false; // Unknown key, don't handle
        }
        
        // Build the keybind string with modifiers
        StringBuilder keybindBuilder = new StringBuilder();
        
        // Check for modifiers (these are bit flags)
        boolean hasCtrl = (modifiers & 2) != 0;   // GLFW_MOD_CONTROL
        boolean hasShift = (modifiers & 1) != 0;  // GLFW_MOD_SHIFT
        boolean hasAlt = (modifiers & 4) != 0;    // GLFW_MOD_ALT
        
        if (hasCtrl) keybindBuilder.append("Ctrl+");
        if (hasShift) keybindBuilder.append("Shift+");
        if (hasAlt) keybindBuilder.append("Alt+");
        
        keybindBuilder.append(keyName);
        
        // Set the new keybind
        currentKeybind = keybindBuilder.toString();
        waitingForKeybind = false;
        keybindInputFocused = false;
        
        System.out.println("Keybind set to '" + currentKeybind + "' for skill: " + selectedSkill.getId());
        
        KeybindRegistry.getInstance().setSkillKeybind(selectedSkill.getId(), currentKeybind);
        
        return true;
    }
    
    /**
     * Convert GLFW key code to readable key name
     * @param keyCode The GLFW key code
     * @return The readable key name, or null if unknown
     */
    private String getKeyName(int keyCode) {
        // Common keys mapping (GLFW key codes)
        switch (keyCode) {
            case 32: return "Space";
            case 39: return "'";
            case 44: return ",";
            case 45: return "-";
            case 46: return ".";
            case 47: return "/";
            case 48: case 49: case 50: case 51: case 52:
            case 53: case 54: case 55: case 56: case 57:
                return String.valueOf((char) keyCode); // 0-9
            case 59: return ";";
            case 61: return "=";
            case 65: case 66: case 67: case 68: case 69: case 70: case 71: case 72: case 73:
            case 74: case 75: case 76: case 77: case 78: case 79: case 80: case 81: case 82:
            case 83: case 84: case 85: case 86: case 87: case 88: case 89: case 90:
                return String.valueOf((char) keyCode); // A-Z
            case 91: return "[";
            case 92: return "\\";
            case 93: return "]";
            case 96: return "`";
            
            // Function keys
            case 290: case 291: case 292: case 293: case 294: case 295: case 296: case 297:
            case 298: case 299: case 300: case 301:
                return "F" + (keyCode - 289); // F1-F12
                
            // Arrow keys
            case 262: return "Right";
            case 263: return "Left";
            case 264: return "Down";
            case 265: return "Up";
            
            // Other special keys
            case 257: return "Enter";
            case 258: return "Tab";
            case 280: return "CapsLock";
            case 284: return "Pause";
            case 285: return "ScrollLock";
            case 286: return "NumLock";
            case 287: return "PrintScreen";
            case 260: return "Insert";
            case 268: return "Home";
            case 266: return "PageUp";
            case 267: return "PageDown";
            case 269: return "End";
            
            // Numpad
            case 320: case 321: case 322: case 323: case 324:
            case 325: case 326: case 327: case 328: case 329:
                return "Num" + (keyCode - 320); // Num0-Num9
            case 330: return "Num.";
            case 331: return "Num/";
            case 332: return "Num*";
            case 333: return "Num-";
            case 334: return "Num+";
            case 335: return "NumEnter";
            case 336: return "Num=";
            
            // Mouse buttons (passed as key codes from mouseClicked)
            case 0: return "LMB";
            case 1: return "RMB";
            case 2: return "MMB";

            default:
                // Mouse buttons 3+ (GLFW supports up to 7)
                if (keyCode >= 3 && keyCode <= 7) {
                    return "Mouse" + (keyCode + 1);
                }
                return "Key" + keyCode; // Fallback for unknown keys
        }
    }
    
    /**
     * Get the current keybind for display purposes
     * @return The current keybind string
     */
    public String getCurrentKeybind() {
        return currentKeybind;
    }
    
    /**
     * Set the keybind (useful for loading saved keybinds)
     * @param keybind The keybind string to set
     */
    public void setKeybind(String keybind) {
        this.currentKeybind = keybind != null ? keybind : "";
    }
    
    /**
     * Check if the keybind input is currently waiting for a key press
     * @return true if waiting for keybind input
     */
    public boolean isWaitingForKeybind() {
        return waitingForKeybind;
    }

    public boolean mouseScrolledModal(double mouseX, double mouseY, double delta) {
        if (modalPane != null) {
            return modalPane.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }

    public Component getDisplayName() {
        return Component.literal(this.displayName);
    }

    public String getRawName() {
        return this.displayName;
    }

    public IconData getIconItem() {
        return this.icon;
    }

    public String getSkillTreeName() {
        return this.skillTree;
    }

    public SkillTree getSkillTree() {
        if(this.skillTree == null || "".equals(this.skillTree)) {
            return null;
        }

        return SkillDataManager.INSTANCE.getSkillTree(this.skillTree);
    }

    private String toRomanNumeral(int num) {
        if (num <= 0 || num > 10) return String.valueOf(num);
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return numerals[num];
    }
}