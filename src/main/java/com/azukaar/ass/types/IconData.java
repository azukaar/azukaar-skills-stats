package com.azukaar.ass.types;

import com.google.gson.annotations.Expose;

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
        System.out.println("PIPI " + item + " is " + itemObj);
        if (itemObj == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(itemObj, 1);
    }

    public void render(GuiGraphics graphic, int x, int y) {
        if(isItem()) {
            graphic.renderItem(getItemStack(), x, y);
        } else {
            ResourceLocation textureLocation = ResourceLocation.tryParse(texture);
            if (textureLocation != null) {
                graphic.blit(textureLocation, x, y, 0, 0, 16, 16); // Adjust size as needed
            } else {
                graphic.blit(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/missing.png"), x, y, 0, 0, 16, 16); // Fallback texture
            }
        }
    }
}