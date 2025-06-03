package com.azukaar.ass.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum SkillTab {
    OVERVIEW("Overview", new ItemStack(Items.BOOK)),
    COMBAT("Combat Skills", new ItemStack(Items.IRON_SWORD)),
    ARCHERY("Archery Skills", new ItemStack(Items.BOW)),
    MINING("Mining Skills", new ItemStack(Items.DIAMOND_PICKAXE)),
    CRAFTING("Crafting Skills", new ItemStack(Items.CRAFTING_TABLE)),
    MAGIC("Magic Skills", new ItemStack(Items.ENCHANTED_BOOK));

    private final String displayName;
    private final ItemStack iconItem;

    SkillTab(String displayName, ItemStack iconItem) {
        this.displayName = displayName;
        this.iconItem = iconItem;
    }

    public Component getDisplayName() {
        return Component.literal(this.displayName);
    }

    public ItemStack getIconItem() {
        return this.iconItem;
    }

    public static SkillTab[] getVisibleTabs() {
        return values();
    }
}