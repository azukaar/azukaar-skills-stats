package com.azukaar.ass.aspect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class ExplorationSavedData extends SavedData {
    private final Map<UUID, PlayerDiscoveries> players = new HashMap<>();

    public static class PlayerDiscoveries {
        public final Set<String> biomes = new HashSet<>();
        public final Set<String> structures = new HashSet<>();
        public final Set<Long> chunks = new HashSet<>();
    }

    public PlayerDiscoveries getOrCreate(UUID playerId) {
        return players.computeIfAbsent(playerId, k -> new PlayerDiscoveries());
    }

    public static ExplorationSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ExplorationSavedData data = new ExplorationSavedData();
        CompoundTag playersTag = tag.getCompound("players");

        for (String uuidStr : playersTag.getAllKeys()) {
            UUID uuid = UUID.fromString(uuidStr);
            CompoundTag playerTag = playersTag.getCompound(uuidStr);
            PlayerDiscoveries discoveries = data.getOrCreate(uuid);

            ListTag biomeList = playerTag.getList("biomes", Tag.TAG_STRING);
            for (int i = 0; i < biomeList.size(); i++) {
                discoveries.biomes.add(biomeList.getString(i));
            }

            ListTag structureList = playerTag.getList("structures", Tag.TAG_STRING);
            for (int i = 0; i < structureList.size(); i++) {
                discoveries.structures.add(structureList.getString(i));
            }

            ListTag chunkList = playerTag.getList("chunks", Tag.TAG_LONG);
            for (int i = 0; i < chunkList.size(); i++) {
                discoveries.chunks.add(((LongTag) chunkList.get(i)).getAsLong());
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag playersTag = new CompoundTag();

        for (Map.Entry<UUID, PlayerDiscoveries> entry : players.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            PlayerDiscoveries d = entry.getValue();

            ListTag biomeList = new ListTag();
            for (String biome : d.biomes) {
                biomeList.add(StringTag.valueOf(biome));
            }
            playerTag.put("biomes", biomeList);

            ListTag structureList = new ListTag();
            for (String structure : d.structures) {
                structureList.add(StringTag.valueOf(structure));
            }
            playerTag.put("structures", structureList);

            ListTag chunkList = new ListTag();
            for (long chunk : d.chunks) {
                chunkList.add(LongTag.valueOf(chunk));
            }
            playerTag.put("chunks", chunkList);

            playersTag.put(entry.getKey().toString(), playerTag);
        }

        tag.put("players", playersTag);
        return tag;
    }

    private static final SavedData.Factory<ExplorationSavedData> FACTORY =
        new SavedData.Factory<>(ExplorationSavedData::new, ExplorationSavedData::load);

    public static ExplorationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "ass_exploration");
    }
}
