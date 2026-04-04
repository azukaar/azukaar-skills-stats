package com.azukaar.ass.aspect;

import java.util.Map;

import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.AspectHelper;
import com.azukaar.ass.api.AspectType;
import com.azukaar.ass.api.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ExplorationAspect implements AspectType {
    private static AspectDefinition definition;

    // Loaded from datapack properties via onLoad()
    private static int targetMaxLevel;
    private static double biomeRatio;
    private static double structureRatio;
    private static double chunkBonusXp;
    private static long chunkCheckInterval;
    private static long biomeStructureCheckInterval;
    private static int biomeRampStart;
    private static double biomeRampRate;
    private static double structureRampRate;
    private static double maxDiscoveryXp;

    // Cached XP values (computed once from registries)
    private static boolean initialized = false;
    private static double biomeXp = 0;
    private static double structureXp = 0;
    private static int biomeCount = 0;
    private static int structureCount = 0;

    private static long tickCounter = 0;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.parse("azukaarskillsstats:exploration");
    }

    @Override
    public void onLoad(AspectDefinition def) {
        definition = def;
        targetMaxLevel = def.getInt("target_max_level", 0);
        biomeRatio = def.getDouble("biome_ratio", 0);
        structureRatio = def.getDouble("structure_ratio", 0);
        chunkBonusXp = def.getDouble("chunk_bonus_xp", 0);
        chunkCheckInterval = def.getLong("chunk_check_interval", 0);
        biomeStructureCheckInterval = def.getLong("biome_structure_check_interval", 0);
        biomeRampStart = def.getInt("biome_ramp_start", 0);
        biomeRampRate = def.getDouble("biome_ramp_rate", 0);
        structureRampRate = def.getDouble("structure_ramp_rate", 0);
        maxDiscoveryXp = def.getDouble("max_discovery_xp", 0);
        initialized = false;
    }

    @Override
    public void onUnload() {
        definition = null;
        initialized = false;
        tickCounter = 0;
    }

    private static void initialize(MinecraftServer server) {
        int totalXpBudget = PlayerData.getTotalXpForLevel(targetMaxLevel);

        Registry<?> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Registry<Structure> structureRegistry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);

        biomeCount = biomeRegistry.size();
        structureCount = structureRegistry.size();

        biomeXp = (biomeCount > 0) ? (totalXpBudget * biomeRatio / biomeCount) : 0;
        structureXp = (structureCount > 0) ? (totalXpBudget * structureRatio / structureCount) : 0;
        biomeXp = Math.min(biomeXp, maxDiscoveryXp);
        structureXp = Math.min(structureXp, maxDiscoveryXp);

        initialized = true;

        System.out.println("[ExplorationAspect] Initialized — budget: " + totalXpBudget
            + " | biomes: " + biomeCount + " (" + (int) biomeXp + " XP each)"
            + " | structures: " + structureCount + " (" + (int) structureXp + " XP each)"
            + " | chunk bonus: " + (int) chunkBonusXp + " XP");
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (definition == null) return;
        tickCounter++;

        MinecraftServer server = event.getServer();
        if (!initialized) {
            initialize(server);
        }

        boolean checkChunks = tickCounter % chunkCheckInterval == 0;
        boolean checkBiomesStructures = tickCounter % biomeStructureCheckInterval == 0;
        if (!checkChunks && !checkBiomesStructures) return;

        ExplorationSavedData savedData = ExplorationSavedData.get(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkDiscoveries(player, savedData, checkChunks, checkBiomesStructures);
        }
    }

    private static void checkDiscoveries(ServerPlayer player, ExplorationSavedData savedData,
                                          boolean checkChunks, boolean checkBiomesStructures) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        ExplorationSavedData.PlayerDiscoveries discoveries = savedData.getOrCreate(player.getUUID());
        boolean changed = false;

        // Chunk discovery
        if (checkChunks) {
            long chunkKey = ChunkPos.asLong(pos);
            if (discoveries.chunks.add(chunkKey)) {
                AspectHelper.awardXp(definition, player, chunkBonusXp, player.position());
                changed = true;
            }
        }

        // Biome & structure discovery
        if (checkBiomesStructures) {
            // Biome
            ResourceLocation biomeKey = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(pos).value());
            if (biomeKey != null && !discoveries.biomes.contains(biomeKey.toString())) {
                double biomeMultiplier = Math.min((discoveries.biomes.size() - biomeRampStart) * biomeRampRate, 1.0);
                if (biomeMultiplier < 0) biomeMultiplier = 0;
                discoveries.biomes.add(biomeKey.toString());
                double xp = biomeXp * biomeMultiplier;
                if (xp > 0) {
                    AspectHelper.awardXp(definition, player, xp, player.position());
                }
                System.out.println("[ExplorationAspect] Player " + player.getName().getString()
                    + " discovered biome: " + biomeKey + " (" + (int)(biomeMultiplier * 100) + "% = " + (int) xp + " XP)");
                changed = true;
            }

            // Structures
            Map<Structure, LongSet> candidates = level.structureManager().getAllStructuresAt(pos);
            Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (Structure structure : candidates.keySet()) {
                StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
                if (!start.isValid()) continue;
                ResourceLocation structureKey = structureRegistry.getKey(structure);
                if (structureKey != null && !discoveries.structures.contains(structureKey.toString())) {
                    double structMultiplier = Math.min(discoveries.structures.size() * structureRampRate, 1.0);
                    discoveries.structures.add(structureKey.toString());
                    double xp = structureXp * structMultiplier;
                    if (xp > 0) {
                        AspectHelper.awardXp(definition, player, xp, player.position());
                    }
                    System.out.println("[ExplorationAspect] Player " + player.getName().getString()
                        + " discovered structure: " + structureKey + " (" + (int)(structMultiplier * 100) + "% = " + (int) xp + " XP)");
                    changed = true;
                }
            }
        }

        if (changed) {
            savedData.setDirty();
        }
    }
}
