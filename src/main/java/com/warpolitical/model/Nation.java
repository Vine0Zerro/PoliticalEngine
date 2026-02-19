package com.warpolitical.model;

import java.util.*;

public class Nation {

    public static final int MAX_CHUNKS = 500;

    private final String id;
    private String name;
    private String capitalTownId;
    private String leaderName;
    private String color;
    private String description;
    private String governmentType = "MONARCHY";
    private final List<String> townIds = new ArrayList<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();
    private boolean atWar = false;

    public Nation(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int getTotalChunks(Map<String, Town> allTowns) {
        int total = 0;
        for (String townId : townIds) {
            Town town = allTowns.get(townId);
            if (town != null) {
                total += town.getChunkCount();
            }
        }
        return total;
    }

    public boolean canClaimMore(Map<String, Town> allTowns) {
        return getTotalChunks(allTowns) < MAX_CHUNKS;
    }

    public void addTown(String townId) {
        if (!townIds.contains(townId)) {
            townIds.add(townId);
        }
    }

    public void removeTown(String townId) {
        townIds.remove(townId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCapitalTownId() {
        return capitalTownId;
    }

    public void setCapitalTownId(String capitalTownId) {
        this.capitalTownId = capitalTownId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGovernmentType() {
        return governmentType;
    }

    public void setGovernmentType(String governmentType) {
        this.governmentType = governmentType;
    }

    public List<String> getTownIds() {
        return townIds;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public Set<String> getEnemies() {
        return enemies;
    }

    public boolean isAtWar() {
        return atWar;
    }

    public void setAtWar(boolean atWar) {
        this.atWar = atWar;
    }
}
