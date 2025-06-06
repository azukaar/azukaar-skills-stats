package com.azukaar.ass.types;

import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IconData {
    @Expose
    private String type = "item";
    
    @Expose
    private String item;
    
    @Expose
    private String texture;
    
    public IconData(String item) {
        this.type = "item";
        this.item = item;
    }
    
    public String getType() { return type; }
    public String getItem() { return item; }
    public String getTexture() { return texture; }    
    public boolean isItem() { return "item".equals(type); }
    public boolean isTexture() { return "texture".equals(type); }

    public ItemStack getItemStack() {
        if (item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item itemObj = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(item));
        if (itemObj == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(itemObj, 1);
    }

    public void render(GuiGraphics graphic, int x, int y) {
        if(isItem()) {
            // Save the current pose and properly isolate item rendering
            graphic.pose().pushPose();
            
            // Store current Z-level and increment it for the item
            float currentZ = graphic.pose().last().pose().m23(); // Get current Z translation
            graphic.pose().translate(0, 0, -1); // Move item slightly back in Z
            
            // Ensure proper render state for GUI rendering
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Disable depth testing to prevent item from interfering with GUI depth
            RenderSystem.disableDepthTest();
            
            // Render the item
            graphic.renderItem(getItemStack(), x, y);
            
            // Re-enable depth testing and restore render state
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            
            // Restore the pose
            graphic.pose().popPose();
            
            // Force reset any lingering render states
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            
        } else {
            // For texture rendering, use standard 2D rendering
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            
            ResourceLocation textureLocation = ResourceLocation.tryParse(texture);
            if (textureLocation != null) {
                graphic.blit(textureLocation, x, y, 0, 0, 16, 16);
            } else {
                graphic.blit(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/missing.png"), x, y, 0, 0, 16, 16);
            }
            
            RenderSystem.disableBlend();
        }
    }
}