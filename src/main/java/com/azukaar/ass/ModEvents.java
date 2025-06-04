package com.azukaar.ass;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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
                // playerManager.addPlayerExperience("ass.warrior", XP_REWARD, attacker, event.getEntity().getPosition(1));
            }
        }
    }
    
    // Helper method to check if a block is an ore
    private static boolean isOreBlock(BlockState block) {
        // Simple check - you would expand this with proper ore detection
        String blockName = block.getBlock().getDescriptionId().toLowerCase();
        return blockName.contains("ore");
    }
}
