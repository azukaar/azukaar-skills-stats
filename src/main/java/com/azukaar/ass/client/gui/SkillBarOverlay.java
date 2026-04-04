package com.azukaar.ass.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.api.ActiveSkillHandler;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.client.KeybindRegistry;
import com.azukaar.ass.types.Skill;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkillBarOverlay {

    private static final int SLOT_SIZE = 12;
    private static final int SLOT_SPACING = 2;
    private static final int MARGIN_RIGHT = 4;
    private static final int MAX_ROWS = 8;

    private static final int COOLDOWN_OVERLAY_COLOR = 0xAAFFFFFF;
    private static final int KEYBIND_TEXT_COLOR = 0xFFFFFF;
    private static final int COOLDOWN_TEXT_COLOR = 0xFF5555;

    private static final boolean DEBUG_FILL = false;
    private static final int DEBUG_TOTAL = 20;

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        List<SkillSlotData> slots = gatherActiveSlots(mc);
        if (slots.isEmpty()) {
            return;
        }

        if (DEBUG_FILL) {
            List<SkillSlotData> filled = new ArrayList<>();
            for (int i = 0; i < DEBUG_TOTAL; i++) {
                filled.add(slots.get(i % slots.size()));
            }
            slots = filled;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int rowCount = Math.min(slots.size(), MAX_ROWS);
        int totalHeight = rowCount * SLOT_SIZE + (rowCount - 1) * SLOT_SPACING;
        int centerY = (screenHeight - totalHeight) / 2;

        for (int i = 0; i < slots.size(); i++) {
            int col = i / MAX_ROWS;
            int row = i % MAX_ROWS;
            int x = screenWidth - MARGIN_RIGHT - (col + 1) * (SLOT_SIZE + SLOT_SPACING);
            int y = centerY + row * (SLOT_SIZE + SLOT_SPACING);
            renderSlot(guiGraphics, font, slots.get(i), x, y, mc);
        }
    }

    private static List<SkillSlotData> gatherActiveSlots(Minecraft mc) {
        List<SkillSlotData> slots = new ArrayList<>();
        Map<String, String> keybinds = KeybindRegistry.getInstance().getAllKeybinds();

        for (Map.Entry<String, String> entry : keybinds.entrySet()) {
            String skillId = entry.getKey();
            String keybind = entry.getValue();

            if (!ActiveSkillHandler.isActivatable(skillId)) {
                continue;
            }

            if (PlayerData.getSkillLevel(mc.player, skillId) <= 0) {
                continue;
            }

            Skill skill = SkillDataManager.INSTANCE.findSkillById(skillId);
            if (skill == null) {
                continue;
            }

            slots.add(new SkillSlotData(skillId, keybind, skill));
        }

        slots.sort((a, b) -> a.keybind.compareTo(b.keybind));
        return slots;
    }

    private static void renderSlot(GuiGraphics guiGraphics, Font font,
                                    SkillSlotData slot, int x, int y, Minecraft mc) {
        // Check cooldown state
        boolean onCooldown = false;
        float cooldownProgress = 0;
        IPlayerSkills skills = mc.player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills != null) {
            long currentTime = mc.level.getGameTime();
            if (skills.isSkillOnCooldown(slot.skillId, currentTime)) {
                onCooldown = true;
                int remainingTicks = skills.getRemainingCooldown(slot.skillId, currentTime);
                int totalCooldown = ActiveSkillHandler.getEffectiveCooldown(mc.player, slot.skillId);
                if (totalCooldown > 0) {
                    cooldownProgress = (float) remainingTicks / totalCooldown;
                }
            }
        }

        // Skill icon (scale from 16x16 down to SLOT_SIZE), dim when on cooldown
        float iconAlpha = onCooldown ? 0.35f : 1.0f;
        float iconScale = SLOT_SIZE / 16.0f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        slot.skill.getIconData().render(guiGraphics, (int)(x / iconScale), (int)(y / iconScale), iconAlpha);
        guiGraphics.pose().popPose();

        // Cooldown overlay + keybind label rendered above item icon z
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 160);

        if (onCooldown && cooldownProgress > 0) {
            renderCooldownOverlay(guiGraphics, x, y, cooldownProgress);
        }

        // Keybind label (top-left, scaled down)
        String keybindText = shortenKeybind(slot.keybind);
        guiGraphics.pose().pushPose();
        float scale = 0.5f;
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, keybindText,
            (int) (x / scale),
            (int) (y / scale),
            KEYBIND_TEXT_COLOR, true);
        guiGraphics.pose().popPose();

        guiGraphics.pose().popPose();
    }

    private static void renderCooldownOverlay(GuiGraphics guiGraphics, int x, int y, float progress) {
        if (progress <= 0) return;

        int fillHeight = (int) Math.ceil(SLOT_SIZE * progress);
        int fillY = y + SLOT_SIZE - fillHeight;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(x, fillY, x + SLOT_SIZE, y + SLOT_SIZE, COOLDOWN_OVERLAY_COLOR);
        RenderSystem.disableBlend();
    }

    private static String shortenKeybind(String keybind) {
        if (keybind == null || keybind.isEmpty()) return "?";
        if (keybind.length() <= 3) return keybind;
        keybind = keybind.replace("Mouse", "M");
        return keybind;
    }

    private static class SkillSlotData {
        final String skillId;
        final String keybind;
        final Skill skill;

        SkillSlotData(String skillId, String keybind, Skill skill) {
            this.skillId = skillId;
            this.keybind = keybind;
            this.skill = skill;
        }
    }
}
