package com.azukaar.ass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.capabilities.PlayerSkillsProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ModEvents {
    private static final int XP_REWARD = 10;
    
    // Initialize event handlers
    public static void init() {
        NeoForge.EVENT_BUS.register(ModEvents.class);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialize player data if needed
        System.out.println("Player joined: " + event.getEntity().getName().getString());
        
        // Get the player manager
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                // Sync skills when player logs in
                var provider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                provider.syncToClient(serverPlayer);

                // Apply all effects for the player
                SkillDataManager.INSTANCE.updateAllSkillEffects(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            
            if (newPlayer instanceof ServerPlayer serverPlayer) {
                if (oldPlayer instanceof ServerPlayer oldServerPlayer) {
                    // Don't cast to PlayerSkillsProvider - work with IPlayerSkills/PlayerSkills
                    var newSkillsProvider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                    var oldSkillsProvider = oldServerPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
                    
                    if (newSkillsProvider != null && oldSkillsProvider != null) {      
                        CompoundTag data = oldSkillsProvider.serializeNBT(newPlayer.registryAccess());
                        newSkillsProvider.deserializeNBT(newPlayer.registryAccess(), data);      
                    }
                }
            }
        }
    }

    // on respawn, reapply all effects
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        System.out.println("Player respawned: " + player.getName().getString());

        // Reapply all effects for the player
        if (player instanceof ServerPlayer serverPlayer) {
            SkillDataManager.INSTANCE.updateAllSkillEffects(serverPlayer);
            var skillsProvider = serverPlayer.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
            if (skillsProvider != null) {
                // Sync skills to client after respawn
                skillsProvider.syncToClient(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockState block = event.getState();
        double durability = block.getDestroySpeed(player.level(), event.getPos());
        double rew = (int)(durability * 4);

        // check if right tool for the block's loot
        boolean isCorrectTool = block.canHarvestBlock(event.getLevel(), event.getPos(), player);

        if (player != null && !player.level().isClientSide() && isCorrectTool) {
            // Award mining XP when breaking any block
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                PlayerData.addExperience(IPlayerSkills.MINER_PATH, rew, player, event.getPos().getCenter());
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Post event) {
        // Check if damage source is a player
        if (event.getSource().getEntity() instanceof Player attacker && 
            !attacker.level().isClientSide()) {
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                // PlayerManager playerManager = PlayerManager.load(server);

                // Award warrior XP for direct melee hits
                // playerManager.addPlayerExperience(IPlayerSkills., XP_REWARD, attacker, event.getEntity().getPosition(1));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        // Only process on server side to avoid duplication
        if (player.level().isClientSide) {
            return;
        }
        
        // Simply read the mining speed attribute
        double miningSpeedBonus = player.getAttributeValue(ModAttributes.MINING_SPEED);
        
        if (miningSpeedBonus > 0) {
            // Apply the mining speed bonus
            float newSpeed = event.getOriginalSpeed() * (1.0f + (float)miningSpeedBonus);
            event.setNewSpeed(newSpeed);
        }
    }

    private static final Map<UUID, Integer> previousHungerLevels = new HashMap<>();
    private static final Map<UUID, Double> realHungerLevels = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Only process players on server side
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        
        // Check every 20 ticks (1 second) to avoid performance issues
        if (player.tickCount % 20 != 0) {
            return;
        }
        
        UUID playerId = player.getUUID();
        FoodData foodData = player.getFoodData();
        int currentHunger = foodData.getFoodLevel();
        
        // Get previous hunger level
        Integer previousHunger = previousHungerLevels.get(playerId);
        if (previousHunger == null) {
            previousHungerLevels.put(playerId, currentHunger);
            realHungerLevels.put(playerId, (double) currentHunger);
            return;
        }
        
        // Check if hunger decreased
        if (currentHunger < previousHunger) {
            double hungerEfficiency = player.getAttributeValue(ModAttributes.HUNGER_EFFICIENCY);
            
            // Calculate how much hunger to restore based on efficiency
            // efficiency of 0.5 = 50% chance to not lose hunger
            int hungerLost = previousHunger - currentHunger;
     
            if (hungerLost > 0) {
                double hungerToLose = (double)(hungerLost) * (1 - hungerEfficiency);

                // Restore some of the lost hunger
                double newHungerLevel = Math.max(0, realHungerLevels.get(playerId) - hungerToLose);
                
                // Update the real hunger level
                realHungerLevels.put(playerId, newHungerLevel);
                foodData.setFoodLevel(Mth.ceil(newHungerLevel));
                previousHungerLevels.put(playerId, Mth.ceil(newHungerLevel));
            }
        } else if (currentHunger > previousHunger) {
            // If hunger increased, reset the previous level
            previousHungerLevels.put(playerId, currentHunger);
            realHungerLevels.put(playerId, (double) currentHunger);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up tracking when player leaves
        previousHungerLevels.remove(event.getEntity().getUUID());
        realHungerLevels.remove(event.getEntity().getUUID());
    }
}
