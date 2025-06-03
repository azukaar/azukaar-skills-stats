package com.azukaar.ass;

import java.util.UUID;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;

public class PlayerAttribute extends SavedData {
  private static final String DATA_NAME_PREFIX = "ass_player_attributes";
  private final CompoundTag value;
  private final String attributeName;
  private double defaultValue = 0.0;

  public PlayerAttribute(String attributeName, double defaultValue) {
    this.value = new CompoundTag();
    this.attributeName = attributeName;
    this.defaultValue = defaultValue;
  }

  public PlayerAttribute(String attributeName, CompoundTag nbt, double defaultValue) {
    this.attributeName = attributeName;
    this.value = nbt.getCompound(this.attributeName);
    this.defaultValue = defaultValue;
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
    nbt.put(this.attributeName, value);
    return nbt;
  }

  // Existing max health methods
  public void setRaw(String playerUUID, double exp) {
    value.putDouble(playerUUID, exp);
    setDirty();
  }

  public double get(UUID playerUUID) {
    if (value.contains(playerUUID.toString())) {
      return value.getDouble(playerUUID.toString());
    }
    return defaultValue;
  }

  public double get(Player player) {
    if (player instanceof ServerPlayer) {
      return get(player.getUUID());
    }
    return defaultValue;
  }

  public static PlayerAttribute load(MinecraftServer server, String attributeName, double defaultValue) {
    ServerLevel level = server.overworld();
    PlayerAttribute playerAttribute = level.getDataStorage().computeIfAbsent(
        new SavedData.Factory<PlayerAttribute>(
            () -> new PlayerAttribute(attributeName, defaultValue),
            (tag, provider) -> new PlayerAttribute(attributeName, tag, defaultValue),
            null // or some appropriate DataFixTypes if needed
        ),
        DATA_NAME_PREFIX + "_" + attributeName);
    return playerAttribute;
  }
}
