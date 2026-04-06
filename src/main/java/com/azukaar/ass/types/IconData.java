package com.azukaar.ass.types;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.RegistryOps;
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

    @Expose
    private JsonObject components;

    private ItemStack cachedStack;

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
        if (cachedStack != null) return cachedStack;

        if (item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (components != null) {
            var mc = Minecraft.getInstance();
            if (mc.level != null) {
                JsonObject fullItem = new JsonObject();
                fullItem.addProperty("id", item);
                fullItem.add("components", components);
                var ops = RegistryOps.create(JsonOps.INSTANCE, mc.level.registryAccess());
                var result = ItemStack.CODEC.parse(ops, fullItem);
                cachedStack = result.resultOrPartial(err -> {}).orElse(ItemStack.EMPTY);
                return cachedStack;
            }
        }

        Item itemObj = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(item));
        if (itemObj == null) {
            return ItemStack.EMPTY;
        }
        cachedStack = new ItemStack(itemObj, 1);
        return cachedStack;
    }

    public void render(GuiGraphics graphic, int x, int y, float alpha) {
        if (isItem()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            graphic.renderItem(getItemStack(), x, y);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ResourceLocation textureLocation = ResourceLocation.tryParse(texture);
            if (textureLocation != null) {
                graphic.blit(textureLocation, x, y, 0, 0, 16, 16);
            } else {
                graphic.blit(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/missing.png"), x, y, 0, 0, 16, 16);
            }
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public void render(GuiGraphics graphic, int x, int y) {
        render(graphic, x, y, 1.0f);
    }
}
