package com.warpolitical.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = Paths.get("config", WarPoliticalMod.MOD_ID);
    private static final Path TOWNS_FILE = DATA_DIR.resolve("towns.json");
    private static final Path NATIONS_FILE = DATA_DIR.resolve("nations.json");

    private final Map<String, Town> towns = new ConcurrentHashMap<>();
    private final Map<String, Nation> nations = new ConcurrentHashMap<>();

    public void loadAll() {
        try {
            Files.createDirectories(DATA_DIR);
            loadTowns();
            loadNations();
            WarPoliticalMod.LOGGER.info("Загружено {} городов, {} наций",
                    towns.size(), nations.size());
        } catch (IOException e) {
            WarPoliticalMod.LOGGER.error("Ошибка загрузки данных", e);
        }
    }

    private void loadTowns() throws IOException {
        if (!Files.exists(TOWNS_FILE)) return;
        String json = Files.readString(TOWNS_FILE);
        Type type = new TypeToken<Map<String, Town>>() {}.getType();
        Map<String, Town> loaded = GSON.fromJson(json, type);
        if (loaded != null) towns.putAll(loaded);
    }

    private void loadNations() throws IOException {
        if (!Files.exists(NATIONS_FILE)) return;
        String json = Files.readString(NATIONS_FILE);
        Type type = new TypeToken<Map<String, Nation>>() {}.getType();
        Map<String, Nation> loaded = GSON.fromJson(json, type);
        if (loaded != null) nations.putAll(loaded);
    }

    public void saveAll() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(TOWNS_FILE, GSON.toJson(towns));
            Files.writeString(NATIONS_FILE, GSON.toJson(nations));
        } catch (IOException e) {
            WarPoliticalMod.LOGGER.error("Ошибка сохранения данных", e);
        }
    }

    public Map<String, Town> getTowns() { return towns; }
    public Map<String, Nation> getNations() { return nations; }

    public Optional<Town> getTown(String name) {
        return Optional.ofNullable(towns.get(name));
    }

    public Optional<Nation> getNation(String name) {
        return Optional.ofNullable(nations.get(name));
    }

    public void addTown(Town town) {
        towns.put(town.getName(), town);
        saveAll();
    }

    public void removeTown(String name) {
        towns.remove(name);
        saveAll();
    }

    public void addNation(Nation nation) {
        nations.put(nation.getName(), nation);
        saveAll();
    }

    public void removeNation(String name) {
        nations.remove(name);
        saveAll();
    }
}