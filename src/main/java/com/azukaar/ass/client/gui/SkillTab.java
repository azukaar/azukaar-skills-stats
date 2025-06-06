package com.azukaar.ass.client.gui;

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
    public boolean keybindInputVisible;
    public boolean keybindInputFocused;
    public String currentKeybind = ""; // Stores the current keybind (empty if none)
    public boolean waitingForKeybind = false; // True when waiting for user to press a key

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
        
        // Modal dimensions (base height, will be adjusted based on content)
        int modalWidth = 220;
        int baseModalHeight = 145;
        
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
                    if ("active".equals(effect.getType())) {
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
        
        // Render semi-transparent backdrop
        guiGraphics.fill(leftPos, topPos, leftPos + SkillScreen.SCREEN_WIDTH, topPos + SkillScreen.SCREEN_HEIGHT, 0x80000000);
        
        // Render modal background
        guiGraphics.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, 0xFF2D2D30);
        
        // Render modal border
        int borderColor = 0xFF666666;
        guiGraphics.fill(modalX, modalY, modalX + modalWidth, modalY + 1, borderColor); // Top
        guiGraphics.fill(modalX, modalY + modalHeight - 1, modalX + modalWidth, modalY + modalHeight, borderColor); // Bottom
        guiGraphics.fill(modalX, modalY, modalX + 1, modalY + modalHeight, borderColor); // Left
        guiGraphics.fill(modalX + modalWidth - 1, modalY, modalX + modalWidth, modalY + modalHeight, borderColor); // Right
        
        // Reset content positioning with new modal position
        contentStartX = modalX + 10;
        contentStartY = modalY + 10;
        currentY = contentStartY;
        
        // Render skill icon
        selectedSkill.getIconData().render(guiGraphics, contentStartX, currentY);
        
        // Render skill name next to icon
        guiGraphics.drawString(font, selectedSkill.getDisplayName(), 
            contentStartX + 20, currentY + 4, 0xFFFFFF, false);
        
        currentY += 24;
                
        // Render keybind input if skill has active effects
        if (hasActiveEffects) {
            // currentY += 4;
            
            // Keybind input dimensions
            int inputWidth = 100;
            int inputHeight = 16;
            int inputX = contentStartX + 45;
            int inputY = currentY;
            
            // Render keybind label
            guiGraphics.drawString(font, "Keybind:", inputX - 45, inputY + inputHeight/2 - lineHeight/2 + 3, 0xCCCCCC, false);
            
            // Render input background
            int inputBgColor = keybindInputFocused ? 0xFF4A4A4A : 0xFF333333;
            int inputBorderColor = keybindInputFocused ? 0xFF8888FF : 0xFF666666;
            
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
            
            currentY += inputHeight + 6;
        } else {
            this.keybindInputVisible = false;
        }
        
        // Render skill level information
        int currentLevel = PlayerData.getSkillLevel(player, selectedSkill.getId());
        int maxLevel = selectedSkill.getMaxLevel();

        String levelText;
        int levelColor;
        if (maxLevel == 1) {
            levelText = currentLevel > 0 ? "Unlocked" : "Locked";
            levelColor = currentLevel > 0 ? 0x00FF00 : 0xFF4444;
        } else {
            levelText = "Level: " + currentLevel + "/" + maxLevel;
            levelColor = currentLevel > 0 ? 0x00AAFF : 0xFF4444;
        }
        
        guiGraphics.drawString(font, levelText, contentStartX, currentY, levelColor, false);
        currentY += lineHeight + 4;
        
        // Show description if available
        if(selectedSkill.getDescription() != null && !"".equals(selectedSkill.getDescription())) {
            guiGraphics.drawString(font, selectedSkill.getDescription(), contentStartX, currentY, 0xCCCCCC, false);
            currentY += lineHeight + 4;
        }
        
        // Render cooldown information if applicable
        if (selectedSkill.hasCooldown()) {
            int cooldown = selectedSkill.getEffectiveCooldown(Math.max(1, currentLevel));
            String cooldownText = "Cooldown: " + cooldown + " seconds";
            guiGraphics.drawString(font, cooldownText, contentStartX, currentY, 0xFFFF88, false);
            currentY += lineHeight + 4;
        }
        
        // Render skill effects information
        try {
            SkillDataManager skillDataManager = SkillDataManager.INSTANCE;
            SkillEffect skillEffect = skillDataManager.getSkillEffects(selectedSkill.getId());
            
            if (skillEffect != null && skillEffect.getEffects() != null && !skillEffect.getEffects().isEmpty()) {
                guiGraphics.drawString(font, "Effects:", contentStartX, currentY, 0xCCCCCC, false);
                currentY += lineHeight;
                
                for (SkillEffect.Effect effect : skillEffect.getEffects()) {
                    String effectText = "";
                    int effectColor = 0xAAAAAAA;
                    
                    if ("attribute_modifier".equals(effect.getType())) {
                        // Calculate effect value at current level
                        double value = effect.getScaling().calculateValue(effect.getValue(), Math.max(1, currentLevel));
                        String operation = effect.getOperation();
                        String attribute = effect.getAttribute();

                        System.out.println("Rendering effect: " + effect.getType() + " for skill: " + selectedSkill.getId());
                        System.out.println("  Attribute: " + attribute + ", Value: " + value + ", Operation: " + operation + ", Level: " + currentLevel);

                        // Simplify attribute name for display
                        String displayAttribute = Utils.toDisplayString(attribute);

                        if(effect.getDescription() != null && !"".equals(effect.getDescription()))
                            displayAttribute = effect.getDescription();
                        
                        if ("add_value".equals(operation) || "add".equals(operation)) {
                            effectText = "+" + String.valueOf(value) + " " + displayAttribute;
                        } else if ("add_multiplied_base".equals(operation) || "multiply_base".equals(operation)) {
                            effectText = "+" + String.valueOf(value * 100) + "% " + displayAttribute;
                        } else if ("add_multiplied_total".equals(operation) || "multiply_total".equals(operation)) {
                            effectText = "+" + String.valueOf(value * 100) + "% to total " + displayAttribute;
                        } else {
                            effectText = String.valueOf(value) + displayAttribute;
                        }

                        effectColor = 0x00FF88;
                    } else if ("active".equals(effect.getType())) {
                        effectText = Utils.toDisplayString(effect.getActiveEffectId());
                        if(effect.getDescription() != null && !"".equals(effect.getDescription()))
                            effectText = effect.getDescription();
                        
                        effectColor = 0xFF8800;
                    } else if ("custom_attribute_modifier".equals(effect.getType())) {
                        effectText = "Special Effect: " + effect.getAttribute();
                        effectColor = 0x8800FF;
                    }
                    
                    if (!effectText.isEmpty() && currentY + lineHeight < modalY + modalHeight - 10) {
                        guiGraphics.drawString(font, "  " + effectText, contentStartX, currentY, effectColor, false);
                        currentY += lineHeight;

                        if ("active".equals(effect.getType())) {
                            // display all the "data" lines of the active effect
                            Map<String, ScalingData> data = effect.getData();

                            if (data != null && !data.isEmpty()) {
                                for (Map.Entry<String, ScalingData> entry : data.entrySet()) {
                                    String dataKey = entry.getKey();
                                    
                                    // Calculate value based on current level
                                    double value = SkillEffect.getSkillParameter(player, selectedSkill.getId(), dataKey);
                                    
                                    // Display the data line
                                    String dataText = "  " + Utils.toDisplayString(dataKey) + ": " + value;
                                    guiGraphics.drawString(font, dataText, contentStartX + 20, currentY, 0xAAAAAA, false);
                                    currentY += lineHeight;
                                }
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            // Fallback if skill effects can't be retrieved
            guiGraphics.drawString(font, "No effect data available", contentStartX, currentY, 0x888888, false);
        }
        
        // Upgrade button section
        currentY += 4;
        int buttonWidth = 90;
        int buttonHeight = 16;
        int buttonX = contentStartX + modalWidth - buttonWidth - 20;
        int buttonY = contentStartY;
        
        // Check if skill can be upgraded
        boolean canUpgrade = currentLevel < maxLevel;
        int upgradeCost = 1;
        int availablePoints = PlayerData.getSkillPoints(player);
        boolean canAfford = availablePoints >= upgradeCost;
        boolean buttonEnabled = canUpgrade && canAfford;
        
        // Render upgrade button background
        int buttonBgColor = buttonEnabled ? 0xFF4CAF50 : 0xFF666666;
        int buttonBorderColor = buttonEnabled ? 0xFF2E7D32 : 0xFF444444;
        
        guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonBgColor);
        guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 1, buttonBorderColor); // Top
        guiGraphics.fill(buttonX, buttonY + buttonHeight - 1, buttonX + buttonWidth, buttonY + buttonHeight, buttonBorderColor); // Bottom
        guiGraphics.fill(buttonX, buttonY, buttonX + 1, buttonY + buttonHeight, buttonBorderColor); // Left
        guiGraphics.fill(buttonX + buttonWidth - 1, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonBorderColor); // Right
        
        // Render button text
        String buttonText;
        int buttonTextColor = 0xFFFFFF;
        
        if (!canUpgrade) {
            buttonText = maxLevel == 1 ? "Unlocked" : "Max Level";
            buttonTextColor = 0xCCCCCC;
        } else if (!canAfford) {
            buttonText = "Need " + upgradeCost + " SP";
            buttonTextColor = 0xFFAAAA;
        } else {
            buttonText = "Upgrade (" + upgradeCost + " SP)";
        }
        
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
    }
    
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int contentX, int contentY, Font font, Player player, int leftPos, int topPos) {             
        // Render skill connections first (so they appear behind nodes)
        renderSkillConnections(guiGraphics, getSkillTree(), leftPos, topPos);
        
        // Render skill nodes
        renderSkillNodes(guiGraphics, getSkillTree(), mouseX, mouseY, font, player, leftPos, topPos);
    }

    private void renderSkillConnections(GuiGraphics guiGraphics, SkillTree tree, int leftPos, int topPos) {
        for (Skill skill : tree.getAllSkills()) {
            for (Skill child : skill.getChildren()) {
                renderConnection(guiGraphics, skill, child, leftPos, topPos);
            }
        }
    }

    private void renderConnection(GuiGraphics guiGraphics, Skill from, Skill to, int leftPos, int topPos) {
        int fromX = (int)from.getX() + (leftPos + SkillScreen.WINDOW_INSIDE_X) + (SkillScreen.SCREEN_WIDTH-SkillScreen.WINDOW_INSIDE_X) / 2;
        int fromY = (int)from.getY() + (topPos + SkillScreen.WINDOW_INSIDE_Y) + (SkillScreen.SCREEN_HEIGHT-SkillScreen.WINDOW_INSIDE_Y) / 2;

        int toX = (int)to.getX() + (leftPos + SkillScreen.WINDOW_INSIDE_X) + (SkillScreen.SCREEN_WIDTH-SkillScreen.WINDOW_INSIDE_X) / 2;
        int toY = (int)to.getY() + (topPos + SkillScreen.WINDOW_INSIDE_Y) + (SkillScreen.SCREEN_HEIGHT-SkillScreen.WINDOW_INSIDE_Y) / 2;

        // Color based on unlock status
        int color = 0xFF00FF00;
        
        // Draw line connection
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
        
        // Background color based on skill state
        int backgroundColor = 0xFF333333;
        if (PlayerData.getSkillLevel(player, skill.getId()) > 0) {
            backgroundColor = PlayerData.getSkillLevel(player, skill.getId()) == skill.getMaxLevel() ? 0xFF00AA00 : 0xFF0066AA;
        } else {
            backgroundColor = 0xFF333333;
        }
        
        // Draw skill background
        guiGraphics.fill(x, y, x + SkillScreen.SKILL_NODE_SIZE, y + SkillScreen.SKILL_NODE_SIZE, backgroundColor);
        
        // Draw border
        int borderColor = PlayerData.getSkillLevel(player, skill.getId()) > 0 ? 0xFFFFFFFF : 0xFF666666;
        guiGraphics.fill(x, y, x + SkillScreen.SKILL_NODE_SIZE, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + SkillScreen.SKILL_NODE_SIZE - 1, x + SkillScreen.SKILL_NODE_SIZE, y + SkillScreen.SKILL_NODE_SIZE, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + SkillScreen.SKILL_NODE_SIZE, borderColor); // Left
        guiGraphics.fill(x + SkillScreen.SKILL_NODE_SIZE - 1, y, x + SkillScreen.SKILL_NODE_SIZE, y + SkillScreen.SKILL_NODE_SIZE, borderColor); // Right
        
        // Render skill icon
        IconData iconData = skill.getIconData();
        iconData.render(guiGraphics, x + 4, y + 4);
        
        // Render level indicator if skill has levels
       if (PlayerData.getSkillLevel(player, skill.getId()) > 0 && skill.getMaxLevel() > 1) {
            String levelText = String.valueOf(PlayerData.getSkillLevel(player, skill.getId()));
            int textWidth = font.width(levelText);
            guiGraphics.drawString(font, levelText, 
                x + SkillScreen.SKILL_NODE_SIZE - textWidth - 2, 
                y + SkillScreen.SKILL_NODE_SIZE - 8, 
                0xFFFFFF, true);
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
            
            // Mouse buttons (if you want to support them)
            case 0: return "LMB";
            case 1: return "RMB";
            case 2: return "MMB";
            
            default:
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
}