package com.azukaar.ass.api;

import java.util.UUID;

import com.azukaar.ass.PlayerManager;

import net.minecraft.server.MinecraftServer;

public class Player {
  static int getPlayerMainLevel(MinecraftServer server, UUID player) {
    PlayerManager pm = PlayerManager.load(server);
    return pm.getPlayerMainLevel(server.getPlayerList().getPlayer(player));
  }
}
