package com.azukaar.ass;

import java.util.Map;
import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerManager {
  private static PlayerManager instance;

  private PlayerPath main;

  private Map<String, PlayerPath> playerPaths = new HashMap<String, PlayerPath>();

  PlayerManager() {}

  public static PlayerManager load(MinecraftServer server) {
    PlayerManager pm = new PlayerManager();
    pm.main = PlayerPath.load(server, "ass.main");

    pm.playerPaths.put("ass.warrior", PlayerPath.load(server, "ass.warrior"));
    pm.playerPaths.put("ass.miner", PlayerPath.load(server, "ass.miner"));
    pm.playerPaths.put("ass.explorer", PlayerPath.load(server, "ass.explorer"));
    
    return pm;
  }

  public int getPlayerMainLevel(Player player) {
    int totalLevel = 0;

    for(PlayerPath path : playerPaths.values()) {
        totalLevel += path.getLevel(player);
    }

    return getPlayerMainLevel(totalLevel);
  }

  public static int getPlayerMainLevel(int totalLevel) {
    int mainExperience = 0;

    for(int i = 0; i < totalLevel; i++) {
      mainExperience += Math.pow(i, 0.75) * 150;
    }

    if (totalLevel == 1) return 1;

    return Math.min(PlayerPath.getLevelFromXp(mainExperience), totalLevel);
  }

  public double addPlayerExperience(String pathName, double experience, Player player, Vec3 position) {
    if(playerPaths.containsKey(pathName)) {
      PlayerPath path = playerPaths.get(pathName);
      double added = path.addPlayerExperience(player, experience, position);
      // if(added > 0) {
      //   main.addPlayerExperience(player, added, position);
      // }

      System.out.println("Added " + added + " experience to " + pathName);
      System.out.println("Player " + player.getName().getString() + " has " + path.getLevel(player) + " levels in " + pathName);
      System.out.println("Player " + player.getName().getString() + " has " + path.getExperience(player) + " experience in " + pathName);
      System.out.println("Player " + player.getName().getString() + " has " + path.getNextLevelReq(player) + " experience to next level in " + pathName);

      System.out.println("Player " + player.getName().getString() + " has " + getPlayerMainLevel(player) + " levels in main");
      // System.out.println("Player " + player.getName().getString() + " has " + main.getLevel(player) + " levels in main");
      // System.out.println("Player " + player.getName().getString() + " has " + main.getExperience(player) + " experience in main");
      // System.out.println("Player " + player.getName().getString() + " has " + main.getNextLevelReq(player) + " experience to next level in main");
    
      return added;
    }
    return 0;
  }
}
