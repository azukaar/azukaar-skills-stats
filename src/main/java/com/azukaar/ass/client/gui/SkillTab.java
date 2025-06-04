package com.azukaar.ass.client.gui;

import com.azukaar.ass.SkillDataManager;
import com.azukaar.ass.types.IconData;
import com.azukaar.ass.types.SkillTree;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SkillTab {
    private final String displayName;
    private final IconData icon;
    private final String skillTree;

    SkillTab(String displayName, IconData icon, String skillTree) {
        this.displayName = displayName;
        this.icon = icon;
        this.skillTree = skillTree;
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