package com.azukaar.ass.aspect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.AspectHelper;
import com.azukaar.ass.api.AspectType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class NatureAspect implements AspectType {
    private static AspectDefinition definition;

    private static double interactXp;
    private static long interactCooldownTicks;
    private static double breedMultiplier;
    private static double tameMultiplier;
    private static double cropHarvestXp;
    private static long cleanupIntervalTicks;

    private static final Map<UUID, Map<UUID, Long>> interactCooldowns = new HashMap<>();
    private static long tickCounter = 0;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.parse("azukaarskillsstats:nature");
    }

    @Override
    public void onLoad(AspectDefinition def) {
        definition = def;
        interactXp = def.getDouble("interact_xp", 5.0);
        interactCooldownTicks = def.getLong("interact_cooldown_ticks", 100);
        breedMultiplier = def.getDouble("breed_multiplier", 2.0);
        tameMultiplier = def.getDouble("tame_multiplier", 5.0);
        cropHarvestXp = def.getDouble("crop_harvest_xp", 15.0);
        cleanupIntervalTicks = def.getLong("cleanup_interval_ticks", 1200);
    }

    @Override
    public void onUnload() {
        definition = null;
        interactCooldowns.clear();
        tickCounter = 0;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (definition == null) return;
        if (!(event.getTarget() instanceof Animal target)) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (player.distanceTo(target) > 6.0) return;

        Map<UUID, Long> playerCooldowns = interactCooldowns
            .computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        UUID targetId = target.getUUID();
        Long lastInteract = playerCooldowns.get(targetId);

        if (lastInteract != null && tickCounter - lastInteract < interactCooldownTicks) return;

        playerCooldowns.put(targetId, tickCounter);
        System.out.println("[NatureAspect] Player " + player.getName().getString()
            + " interacted with " + target.getName().getString()
            + ", earning " + (int) interactXp + " XP");
        AspectHelper.awardXp(definition, player, interactXp, target.position());
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (definition == null) return;
        if (!(event.getCausedByPlayer() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        LivingEntity parent = event.getParentA();
        double xp = parent.getMaxHealth() * breedMultiplier;
        System.out.println("[NatureAspect] Player " + player.getName().getString()
            + " bred " + parent.getName().getString()
            + ", earning " + (int) xp + " XP");
        AspectHelper.awardXp(definition, player, xp, parent.position());
    }

    @SubscribeEvent
    public static void onAnimalTame(net.neoforged.neoforge.event.entity.living.AnimalTameEvent event) {
        if (definition == null) return;

        Player player = event.getTamer();
        if (player.level().isClientSide()) return;

        LivingEntity animal = event.getAnimal();
        double xp = animal.getMaxHealth() * tameMultiplier;
        System.out.println("[NatureAspect] Player " + player.getName().getString()
            + " tamed " + animal.getName().getString()
            + ", earning " + (int) xp + " XP");
        AspectHelper.awardXp(definition, player, xp, animal.position());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (definition == null) return;

        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) return;

        BlockState state = event.getState();
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            System.out.println("[NatureAspect] Player " + player.getName().getString()
                + " harvested " + state.getBlock().getName().getString()
                + ", earning " + (int) cropHarvestXp + " XP");
            AspectHelper.awardXp(definition, player, cropHarvestXp, event.getPos().getCenter());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (definition == null) return;
        tickCounter++;
        if (tickCounter % cleanupIntervalTicks != 0) return;

        for (Iterator<Map.Entry<UUID, Map<UUID, Long>>> it = interactCooldowns.entrySet().iterator(); it.hasNext(); ) {
            Map<UUID, Long> playerMap = it.next().getValue();
            playerMap.values().removeIf(lastTick -> tickCounter - lastTick > interactCooldownTicks * 2);
            if (playerMap.isEmpty()) it.remove();
        }
    }
}
