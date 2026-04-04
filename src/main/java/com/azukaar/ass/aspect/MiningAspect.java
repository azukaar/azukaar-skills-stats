package com.azukaar.ass.aspect;

import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.AspectHelper;
import com.azukaar.ass.api.AspectType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class MiningAspect implements AspectType {
    private static AspectDefinition definition;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.parse("azukaarskillsstats:mining");
    }

    @Override
    public void onLoad(AspectDefinition def) {
        definition = def;
    }

    @Override
    public void onUnload() {
        definition = null;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (definition == null) return;

        Player player = event.getPlayer();
        BlockState block = event.getState();
        double durability = block.getDestroySpeed(player.level(), event.getPos());
        double rew = (int)(durability * definition.getXpMultiplier());

        boolean isCorrectTool = block.canHarvestBlock(event.getLevel(), event.getPos(), player);
        boolean requireCorrectTool = definition.getBoolean("require_correct_tool", true);

        if (player != null && !player.level().isClientSide() && (!requireCorrectTool || isCorrectTool)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                System.out.println("[MiningAspect] Player " + player.getName().getString()
                    + " mined block " + block.getBlock().getName().getString()
                    + " with durability " + durability
                    + ", earning " + (int) rew + " XP");
                AspectHelper.awardXp(definition, player, rew, event.getPos().getCenter());
            }
        }
    }
}
