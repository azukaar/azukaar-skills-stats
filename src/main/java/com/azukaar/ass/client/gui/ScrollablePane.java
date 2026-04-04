package com.azukaar.ass.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScrollablePane {

    @FunctionalInterface
    public interface ScrollableContent {
        int render(GuiGraphics guiGraphics, int x, int y);
    }

    private int x, y, width, height;
    private double scrollOffset = 0;
    private int contentHeight = 0;
    private static final double SCROLL_SPEED = 10.0;

    public ScrollablePane(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics guiGraphics, ScrollableContent content) {
        guiGraphics.enableScissor(x, y, x + width, y + height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollOffset, 0);

        contentHeight = content.render(guiGraphics, x, y);

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        // Clamp scroll
        double maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isInside(mouseX, mouseY)) return false;
        if (contentHeight <= height) return false;

        scrollOffset -= delta * SCROLL_SPEED;
        double maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double dragY) {
        if (!isInside(mouseX, mouseY)) return false;
        if (contentHeight <= height) return false;

        scrollOffset -= dragY;
        double maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    public void resetScroll() {
        scrollOffset = 0;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
