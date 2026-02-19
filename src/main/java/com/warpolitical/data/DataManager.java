package com.warpolitical.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.ClaimedChunk;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final Path configDir;
    private final Gson gson;

    private final Map<String, Nation> nations = new ConcurrentHashMap<>();
    private final Map<String, Town> towns = new ConcurrentHashMap<>();
    private final Map<String, ClaimedChunk> claimedChunks = new ConcurrentHashMap<>();
    private String activeWorldName = null;

    public DataManager(Path configDir) {
        this.configDir = configDir;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Map<String, Nation> getNations() {
        return nations;
    }

    public Map<String, Town> getTowns() {
        return towns;
    }

    public Map<String, ClaimedChunk> getClaimedChunks() {
        return claimedChunks;
    }

    public Nation getNation(String id) {
        return nations.get(id);
    }

    public Town getTown(String id) {
        return towns.get(id);
    }

    public void addNation(Nation nation) {
        nations.put(nation.getId(), nation);
    }

    public void addTown(Town town) {
        towns.put(town.getId(), town);
    }

    public void claimChunk(int x, int z, String world, String townId) {
        ClaimedChunk chunk = new ClaimedChunk(x, z, world, townId);
        claimedChunks.put(chunk.getKey(), chunk);
        Town town = towns.get(townId);
        if (town != null) {
            town.addChunk(chunk.getKey());
        }
    }

    public ClaimedChunk getChunkAt(String world, int chunkX, int chunkZ) {
        String key = ClaimedChunk.makeKey(world, chunkX, chunkZ);
        return claimedChunks.get(key);
    }

    public boolean isChunkClaimed(String world, int chunkX, int chunkZ) {
        String key = ClaimedChunk.makeKey(world, chunkX, chunkZ);
        return claimedChunks.containsKey(key);
    }

    public Town getTownAtChunk(String world, int chunkX, int chunkZ) {
        ClaimedChunk chunk = getChunkAt(world, chunkX, chunkZ);
        if (chunk == null) return null;
        return towns.get(chunk.getTownId());
    }

    public Nation getNationAtChunk(String world, int chunkX, int chunkZ) {
        Town town = getTownAtChunk(world, chunkX, chunkZ);
        if (town == null) return null;
        return nations.get(town.getNationId());
    }

    public String getActiveWorldName() {
        return activeWorldName;
    }

    public void setActiveWorldName(String name) {
        this.activeWorldName = name;
    }

    public void clearAll() {
        nations.clear();
        towns.clear();
        claimedChunks.clear();
        activeWorldName = null;
    }

    public void saveAll() {
        Path dataDir = configDir.resolve("data");
        dataDir.toFile().mkdirs();

        saveToFile(dataDir.resolve("nations.json").toFile(), nations);
        saveToFile(dataDir.resolve("towns.json").toFile(), towns);
        saveToFile(dataDir.resolve("chunks.json").toFile(), claimedChunks);

        Map<String, String> meta = new ConcurrentHashMap<>();
        meta.put("activeWorld", activeWorldName != null ? activeWorldName : "");

        saveToFile(dataDir.resolve("meta.json").toFile(), meta);

        WarPoliticalMod.LOGGER.info("Данные сохранены: {} наций, {} городов, {} чанков",
                nations.size(), towns.size(), claimedChunks.size());
    }

    public void loadAll() {
        Path dataDir = configDir.resolve("data");
        if (!dataDir.toFile().exists()) return;

        File nationsFile = dataDir.resolve("nations.json").toFile();
        if (nationsFile.exists()) {
            Type type = new TypeToken<Map<String, Nation>>() {}.getType();
            Map<String, Nation> loaded = loadFromFile(nationsFile, type);
            if (loaded != null) nations.putAll(loaded);
        }

        File townsFile = dataDir.resolve("towns.json").toFile();
        if (townsFile.exists()) {
            Type type = new TypeToken<Map<String, Town>>() {}.getType();
            Map<String, Town> loaded = loadFromFile(townsFile, type);
            if (loaded != null) towns.putAll(loaded);
        }

        File chunksFile = dataDir.resolve("chunks.json").toFile();
        if (chunksFile.exists()) {
            Type type = new TypeToken<Map<String, ClaimedChunk>>() {}.getType();
            Map<String, ClaimedChunk> loaded = loadFromFile(chunksFile, type);
            if (loaded != null) claimedChunks.putAll(loaded);
        }

        File metaFile = dataDir.resolve("meta.json").toFile();
        if (metaFile.exists()) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> meta = loadFromFile(metaFile, type);
            if (meta != null && meta.containsKey("activeWorld")) {
                String w = meta.get("activeWorld");
                if (w != null && !w.isEmpty()) {
                    activeWorldName = w;
                }
            }
        }

        WarPoliticalMod.LOGGER.info("Загружено: {} наций, {} городов, {} чанков",
                nations.size(), towns.size(), claimedChunks.size());
    }

    private void saveToFile(File file, Object data) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            WarPoliticalMod.LOGGER.error("Ошибка сохранения {}: {}",
                    file.getName(), e.getMessage());
        }
    }

    private <T> T loadFromFile(File file, Type type) {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            WarPoliticalMod.LOGGER.error("Ошибка загрузки {}: {}",
                    file.getName(), e.getMessage());
            return null;
        }
    }
}
