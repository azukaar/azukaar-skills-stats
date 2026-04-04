package com.azukaar.ass.api;

import net.minecraft.resources.ResourceLocation;

public interface AspectType {
    ResourceLocation getId();
    void onLoad(AspectDefinition definition);
    void onUnload();
}
