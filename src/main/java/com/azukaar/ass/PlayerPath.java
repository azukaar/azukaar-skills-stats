package com.azukaar.ass;

import java.util.UUID;

import org.checkerframework.checker.units.qual.s;

import com.azukaar.ass.api.events.ExperienceGainedEvent;
import com.jcraft.jorbis.Block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class PlayerPath {
  private PlayerAttribute currentExperience;
  private String pathName;

  public PlayerPath() {
  }

  private void setPlayerRawExperience(String playerUUID, double exp) {
    currentExperience.setRaw(playerUUID, exp);
  }

  protected double addPlayerExperience(Player player, double difference, Vec3 position) {
    if (player instanceof ServerPlayer) {
      boolean isMain = pathName == "ass.main";

      // Store the original level for the post event
      double oldLevel = getLevel(player);
      
      // Get current values
      double current = currentExperience.get(player.getUUID());
      double currentLevel = oldLevel;
      // double lvlBias = Math.max((100.00 - currentLevel) / 100.00, 0.01);
      // double realDiff = Math.floor(difference * lvlBias);

      
      // Fire the Pre event
      if(!isMain) {
        ExperienceGainedEvent.Pre preEvent = new ExperienceGainedEvent.Pre(
            player, this.pathName, difference, difference, position);

        // Post the event to the bus and check if it was cancelled
        if (NeoForge.EVENT_BUS.post(preEvent).isCanceled()) {
          // Event was cancelled, don't add XP
          return 0;
        }

        // Get potentially modified XP amount
        difference = preEvent.getAmount();
      }
      
      
      // Apply the XP
      current += difference;
      this.currentExperience.setRaw(player.getUUID().toString(), current);
      
      // Get the new level after XP addition
      double newLevel = getLevel(player);
      
      // Fire the Post event
      if(!isMain) {
        ExperienceGainedEvent.Post postEvent = new ExperienceGainedEvent.Post(
            player, this.pathName, difference, difference, position, oldLevel, newLevel);

        System.out.println("difference :" + difference);
      
        // Post the event to the bus (cannot be cancelled)
        NeoForge.EVENT_BUS.post(postEvent);
      }
      
      return difference;
    }

    return 0;
  }


  public static PlayerPath load(MinecraftServer server, String pathName) {
    PlayerPath path = new PlayerPath();
    path.pathName = pathName;
    path.currentExperience = PlayerAttribute.load(server, pathName + "_current_experience", 0.0);
    return path;
  }

  protected double getLevel(Player player) {
    double currExp = currentExperience.get(player.getUUID());
    return PlayerPath.getLevelFromXp((int) currExp);
  }

  protected double getExperience(Player player) {
    return currentExperience.get(player.getUUID());
  }

  protected double getNextLevelReq(Player player) {
    return PlayerPath.getTotalXpForLevel((int) getLevel(player) + 1);
  }
  
  protected double getNextLevelMissing(Player player) {
    double currExp = currentExperience.get(player.getUUID());
    return PlayerPath.getTotalXpForLevel((int) getLevel(player) + 1) - currExp;
  }

  static final int LINEAR_COMPONENT = 50;
  static final double POW_COMPONENT = 1.25;

  public static int getXpForSpecificLevel(int level) {
      if (level <= 1) return 100;
      // Combination of linear and square root scaling
      return (int)(LINEAR_COMPONENT * Math.pow(level, POW_COMPONENT));
  }

  /**
   * Calculates the total XP required to reach a specific level from level 0.
   * 
   * @param level The target level
   * @return The total XP required to reach the specified level
   */
  public static int getTotalXpForLevel(int level) {
      int totalXp = 0;
      
      // Sum up XP required for each level from 1 to the target level
      for (int i = 1; i <= level; i++) {
          totalXp += getXpForSpecificLevel(i);
      }
      
      return totalXp;
  }

  public static int getLevelFromXp(int totalXp) {
      if (totalXp < LINEAR_COMPONENT) return 0;
      // Use binary search to find the level corresponding to the total XP
      int low = 0;
      int high = 1000; // Arbitrary high value for search
      while (low < high) {
          int mid = (low + high) / 2;
          int xpAtMid = getTotalXpForLevel(mid);
          if (xpAtMid < totalXp) {
              low = mid + 1;
          } else {
              high = mid;
          }
      }      
      // Return the level corresponding to the total XP
      return low - 1;
  }
}
