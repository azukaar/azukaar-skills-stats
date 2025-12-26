package com.azukaar.ass.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.IconData;
import com.azukaar.ass.types.Skill;
import com.azukaar.ass.types.SkillTree;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.item.Items;

@OnlyIn(Dist.CLIENT)
public class SkillScreen extends Screen {
    protected final Player player;
    SkillDataManager skills = SkillDataManager.INSTANCE;
    
    // Tab sprite resources (same as creative inventory)
    private static final ResourceLocation[] UNSELECTED_TOP_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_4"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_5"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_6"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_7")
    };
    
    private static final ResourceLocation[] SELECTED_TOP_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_4"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_5"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_6"), 
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_7")
    };

    // Zoom functionality
    private float zoomScale = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

    // Scrolling state
    private double scrollX = 0.0;
    private double scrollY = 0.0;
    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;
    
    // Skill node rendering constants
    public static final int SKILL_NODE_SIZE = 24;
    public static final int SKILL_SPACING = 48;
    
    // Screen dimensions
    public static final int SCREEN_WIDTH = 252;
    public static final int SCREEN_HEIGHT = 140;
    public static final int WINDOW_INSIDE_X = 9;
    public static final int WINDOW_INSIDE_Y = 18;
    public static final int TAB_WIDTH = 26;
    public static final int TAB_HEIGHT = 32;

    private SkillTab selectedTab;
    private Skill selectedSkill;
    private int leftPos;
    private int topPos; 

    SkillTab[] tabs;

    // Scroll
    private int currentTabOffsetY = 0;

    public SkillScreen(Player player) {
        super(Component.literal("Skills"));
        this.player = player;

        // Initialize skill trees tabs
        this.tabs = new SkillTab[]{};

        Collection<SkillTree> skillTrees = skills.getAllSkillTrees();
        if (skillTrees != null && !skillTrees.isEmpty()) {
            List<SkillTab> skillTabs = new ArrayList<>();
            for (SkillTree tree : skillTrees) {
                if (tree.getId() != null && !tree.getId().isEmpty()) {
                    skillTabs.add(new SkillTab(tree.getDisplayName(), tree.getIconData(), tree.getId()));
                }
            }
            // Add skill tree tabs after overview
            this.tabs = new SkillTab[skillTabs.size() + 1];
            this.tabs[0] = new SkillTab("Overview", new IconData("minecraft:book"), "");
            for (int i = 0; i < skillTabs.size(); i++) {
                this.tabs[i + 1] = skillTabs.get(i);
            }
        }

        this.selectedTab = this.tabs[0]; // Default to overview tab
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - SCREEN_WIDTH) / 2;
        this.topPos = (this.height - SCREEN_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render tabs
        this.renderTabs(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render content area
        this.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render tab tooltips
        this.renderTabTooltips(guiGraphics, mouseX, mouseY);
    }

    public void renderBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Set proper render settings
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // render background pattern from dirt
        // ResourceLocation pattern = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
        // guiGraphics.blit(pattern, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 256, 256);

        // render black background
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + SCREEN_WIDTH, this.topPos + SCREEN_HEIGHT, 0xFF000000);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw simple background using container texture
        ResourceLocation backgroundTexture = ResourceLocation.withDefaultNamespace("textures/gui/advancements/window.png");
        
        guiGraphics.blit(backgroundTexture, this.leftPos, this.topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 256, 256);
    }

    protected void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Set proper render settings
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Render unselected tabs first
        for (int i = 0; i < tabs.length; i++) {
            SkillTab tab = tabs[i];
            if (tab != this.selectedTab) {
                this.renderTabButton(guiGraphics, tab, i, false);
            }
        }
        
        // Render background
        this.renderBg(guiGraphics, mouseX, mouseY, partialTick);

        // Render selected tab last (on top)
        for (int i = 0; i < tabs.length; i++) {
            SkillTab tab = tabs[i];
            if (tab == this.selectedTab) {
                this.renderTabButton(guiGraphics, tab, i, true);
                break;
            }
        }
    }

    protected void renderTabButton(GuiGraphics guiGraphics, SkillTab tab, int index, boolean selected) {
        int tabX = this.leftPos + this.getTabX(index);
        int tabY = this.topPos + this.getTabY();

        if(!selected) {
            tabY += 2; // Adjust position for selected tab
        }
        
        // Proper render state setup for sprites
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Select appropriate sprite
        ResourceLocation[] tabSprites = selected ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS;
        ResourceLocation tabSprite = tabSprites[Mth.clamp(index, 0, tabSprites.length - 1)];
        
        // Render tab background
        guiGraphics.blitSprite(tabSprite, tabX, tabY, TAB_WIDTH, TAB_HEIGHT);
        
        // Render tab icon with proper matrix handling
        guiGraphics.pose().pushPose();
        int iconX = tabX + 5;
        int iconY = tabY + 8;
        // guiGraphics.renderItem(tab.getIconItem(), iconX, iconY);
        tab.getIconItem().render(guiGraphics, iconX, iconY);
        // guiGraphics.renderItemDecorations(this.font, tab.getIconItem(), iconX, iconY);
        guiGraphics.pose().popPose();
        
        RenderSystem.disableBlend(); // Clean up
    }

    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        
        // Set up clipping for content area
        int contentX = this.leftPos + WINDOW_INSIDE_X;
        int contentY = this.topPos + WINDOW_INSIDE_Y;
        int contentWidth = SCREEN_WIDTH - 18;
        int contentHeight = SCREEN_HEIGHT - 27;
        
        // Enable scissor test for clipping
        guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        if(selectedTab.getRawName().equals("Overview")) {
            // Save current pose
            guiGraphics.pose().pushPose();
            
            // guiGraphics.pose().scale(zoomScale, zoomScale, 1.0f);
            
            // Apply scroll translation
            guiGraphics.pose().translate(scrollX, scrollY, 0);
            
            // Render overview content
            currentTabOffsetY = OverviewTab.renderContent(guiGraphics, mouseX, mouseY, partialTick, this.leftPos + WINDOW_INSIDE_X, this.topPos + WINDOW_INSIDE_Y, this.font, player);
            
            // disable scissor 
            guiGraphics.disableScissor();
                
            // Restore pose
            guiGraphics.pose().popPose();
        } else {
            // render tree
            SkillTree currentTree = selectedTab.getSkillTree();
            if (currentTree == null)  {
                guiGraphics.disableScissor();
                System.out.println("No skill tree found for tab: " + selectedTab.getRawName());
                return;
            }            // Save current pose
            guiGraphics.pose().pushPose();
            
            // guiGraphics.pose().scale(zoomScale, zoomScale, 1.0f);
            
            // Apply scroll translation
            guiGraphics.pose().translate(scrollX, scrollY, 0);
            
            if(this.selectedSkill == null) {
                selectedTab.renderContent(guiGraphics, mouseX, mouseY, partialTick, this.leftPos + WINDOW_INSIDE_X, this.topPos + WINDOW_INSIDE_Y, this.font, player, leftPos, topPos);
            }

            // Restore pose
            guiGraphics.pose().popPose();

            // Disable scissor test
            guiGraphics.disableScissor();

            if(this.selectedSkill != null) {
                selectedTab.renderModal(this.selectedSkill, guiGraphics, mouseX, mouseY, partialTick, this.leftPos + WINDOW_INSIDE_X, this.topPos + WINDOW_INSIDE_Y, this.font, player, leftPos, topPos);
            } else {
                // Render skill tooltip if hovering over a skill
                renderSkillTooltip(guiGraphics, mouseX, mouseY);
            }
        }
    }

    private void renderSkillTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isInContentArea(mouseX, mouseY)) {
            Skill hoveredSkill = getSkillAtPosition(mouseX, mouseY);
            if (hoveredSkill != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(hoveredSkill.getDisplayName());
                if ( hoveredSkill.getMaxLevel() > 1) {
                    tooltip.add(Component.literal("Level: " + PlayerData.getSkillLevel(player, hoveredSkill.getId()) + "/" + hoveredSkill.getMaxLevel()));
                } else if(hoveredSkill.getMaxLevel() == 1 && PlayerData.getSkillLevel(player, hoveredSkill.getId()) > 0) {
                    tooltip.add(Component.literal("Unlocked").withStyle(ChatFormatting.GREEN));
                } else if (PlayerData.getSkillLevel(player, hoveredSkill.getId()) == 0) {
                    tooltip.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
                }
                
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    protected void renderTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < tabs.length; i++) {
            SkillTab tab = tabs[i];
            if (this.isHoveringTab(tab, i, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, tab.getDisplayName(), mouseX, mouseY);
                break;
            }
        }
    }

    protected boolean isHoveringTab(SkillTab tab, int index, int mouseX, int mouseY) {
        int tabX = this.leftPos + this.getTabX(index);
        int tabY = this.topPos + this.getTabY();
        
        return mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && 
               mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT;
    }

    protected void selectTab(SkillTab tab) {
        if (this.selectedTab != tab) {
            this.selectedTab = tab;
            currentTabOffsetY = 0; // Reset offset when switching tabs
            scrollX = 0.0; // Reset scroll position
            scrollY = 0.0; // Reset scroll position
            // Add any additional logic needed when switching tabs
        }
    }

    protected int getTabX(int index) {
        return 27 * index; // 27 pixels apart
    }

    protected int getTabY() {
        return -28; // Above the main content area
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game when this screen is open
    }

        
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.selectedTab != null && this.selectedTab.handleKeyPress(button, 0,0, selectedSkill)) {
            return true; // Modal click was handled
        }

        if (button == 0) {
            if (this.selectedSkill != null) {
                if (this.selectedTab.handleModalClick(mouseX, mouseY, this.selectedSkill, player)) {
                    return true; // Button click was handled
                }
                
                // Check if click is outside modal to close it
                int modalX = this.leftPos + (SCREEN_WIDTH - 220) / 2;
                int modalY = this.topPos + (SCREEN_HEIGHT - 180) / 2;
                
                if (mouseX < modalX || mouseX > modalX + 220 ||
                    mouseY < modalY || mouseY > modalY + 180) {
                    this.selectedSkill = null; // Close modal
                    this.selectedTab.currentKeybind = ""; // Reset keybind state
                    return true;
                }
                return true; // Click inside modal but not on button - consume it
            }

            for (int i = 0; i < tabs.length; i++) {
                SkillTab tab = tabs[i];
                if (this.isHoveringTab(tab, i, (int)mouseX, (int)mouseY)) {
                    this.selectTab(tab);
                    return true;
                }
            }
            
            // Check for skill node clicks
            if (isInContentArea(mouseX, mouseY)) {
                Skill clickedSkill = getSkillAtPosition(mouseX, mouseY);
                if (clickedSkill != null) {
                    onSkillClicked(clickedSkill);
                    return true;
                }
                
                // Start dragging if clicked in content area but not on a skill
                isDragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDragging) {
            scrollX += mouseX - lastMouseX;
            scrollY += mouseY - lastMouseY;
            
            // Clamp scrolling to reasonable bounds
            clampScrolling();
            
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        int contentX = this.leftPos + WINDOW_INSIDE_X;
        int contentY = this.topPos + WINDOW_INSIDE_Y;
        int contentWidth = SCREEN_WIDTH - 16;
        int contentHeight = SCREEN_HEIGHT - 26;
        
        return mouseX >= contentX && mouseX <= contentX + contentWidth &&
            mouseY >= contentY && mouseY <= contentY + contentHeight;
    }

    private void clampScrolling() {
        int contentWidth = (SCREEN_WIDTH-WINDOW_INSIDE_X) / 2; // Content width minus scrollbar
        int contentHeight = (SCREEN_HEIGHT-WINDOW_INSIDE_Y) / 2; // Content height minus scrollbar
            

        SkillTree currentTree = selectedTab.getSkillTree();
        if (currentTree == null) {
            scrollX = 0.0;
            scrollY = Mth.clamp(scrollY, -currentTabOffsetY + contentHeight -10, 0);
            return;
        }
        // Calculate tree bounds with some padding
        float treeMinX = currentTree.getMinX();
        float treeMaxX = currentTree.getMaxX();
        float treeMinY = currentTree.getMinY();
        float treeMaxY = currentTree.getMaxY();

        float minClampX = -treeMaxX - contentWidth / 2;
        float maxClampX = -treeMinX - contentWidth / 2 + contentWidth;
        float minClampY = -treeMaxY - contentHeight / 2;
        float maxClampY = -treeMinY - contentHeight / 2 + contentHeight;

        // Clamp scrollX
        scrollX = Mth.clamp(scrollX, minClampX, maxClampX);
        scrollY = Mth.clamp(scrollY, minClampY, maxClampY);
    }

    private Skill getSkillAtPosition(double mouseX, double mouseY) {
        SkillTree currentTree = selectedTab.getSkillTree();
        if (currentTree == null) return null;
        
        // Convert screen coordinates to world coordinates
        double worldX = mouseX - (this.leftPos + WINDOW_INSIDE_X) - scrollX - (SCREEN_WIDTH - WINDOW_INSIDE_X) / 2 + SKILL_NODE_SIZE / 2;
        double worldY = mouseY - (this.topPos + WINDOW_INSIDE_Y) - scrollY - (SCREEN_HEIGHT - WINDOW_INSIDE_Y) / 2 + SKILL_NODE_SIZE / 2;
        
        for (Skill skill : currentTree.getAllSkills()) {
            double skillX = skill.getX();
            double skillY = skill.getY();
            
            if (worldX >= skillX && worldX <= skillX + SKILL_NODE_SIZE &&
                worldY >= skillY && worldY <= skillY + SKILL_NODE_SIZE) {
                return skill;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrollX, double mouseScrollY) {
        // Handle zooming with mouse wheel
        if (isInContentArea(mouseX, mouseY)) {
            SkillTree currentTree = selectedTab.getSkillTree();
            if (currentTree == null) {
                scrollY += mouseScrollY * 10; // Allow normal scrolling if no tree
                clampScrolling();
                return true;
            } else {
                float oldZoom = zoomScale;
                
                // Zoom in/out based on scroll direction
                if (mouseScrollY > 0) {
                    zoomScale = Math.min(MAX_ZOOM, zoomScale + ZOOM_STEP);
                } else if (mouseScrollY < 0) {
                    zoomScale = Math.max(MIN_ZOOM, zoomScale - ZOOM_STEP);
                }
                
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, mouseScrollX, mouseScrollY);
    }

    private void onSkillClicked(Skill skill) {
        // Handle skill click logic here
        // For example, open skill details or spend points
        System.out.println("Clicked skill: " + skill.getId());
        
        // Example: Spend a skill point if the skill is unlocked
        // if (PlayerData.getSkillLevel(player, skill.getId()) < skill.getMaxLevel()) {
        //     System.out.println("Spending skill point on: " + skill.getId());

        //     PlayerData.spendSkillPoint(player, 1, skill.getId());
        // } else {
        //     System.out.println("Skill is locked or at max level: " + skill.getId());
        // }

        this.selectedSkill = skill; // Set selected skill for potential further actions
    }
    
    // handle key presses
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(!this.selectedTab.handleKeyPress(keyCode, scanCode, modifiers, selectedSkill)) {
            // Handle other keys if needed
            // For example, you could handle skill navigation or other actions here
            
            return super.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return true; // Key was handled by the tab
        }
    }
}