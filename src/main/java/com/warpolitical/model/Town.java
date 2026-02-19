package com.warpolitical.model;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Town {

    public static final int MAX_CHUNKS = 200;

    private final String id;
    private String name;
    private String nationId;
    private String mayorName;
    private int centerX;
    private int centerZ;
    private String worldName;
    private String color;
    private final Set<String> claimedChunkKeys = new HashSet<>();
    private final Set<String> residents = new HashSet<>();
    private boolean pvpEnabled = true;
    private boolean explosionsEnabled = false;

    public Town(String id, String name, String nationId,
                int centerX, int centerZ, String worldName) {
        this.id = id;
        this.name = name;
        this.nationId = nationId;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.worldName = worldName;
        this.color = generateRandomColor();
    }

    public boolean canClaimMore() {
        return claimedChunkKeys.size() < MAX_CHUNKS;
    }

    public int getChunkCount() {
        return claimedChunkKeys.size();
    }

    public void addChunk(String chunkKey) {
        claimedChunkKeys.add(chunkKey);
    }

    public void removeChunk(String chunkKey) {
        claimedChunkKeys.remove(chunkKey);
    }

    public boolean ownsChunk(String chunkKey) {
        return claimedChunkKeys.contains(chunkKey);
    }

    private String generateRandomColor() {
        Random r = new Random(id.hashCode());
        return String.format("#%02x%02x%02x",
                r.nextInt(200) + 55,
                r.nextInt(200) + 55,
                r.nextInt(200) + 55);
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

    public String getNationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public String getMayorName() {
        return mayorName;
    }

    public void setMayorName(String mayorName) {
        this.mayorName = mayorName;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Set<String> getClaimedChunkKeys() {
        return claimedChunkKeys;
    }

    public Set<String> getResidents() {
        return residents;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public boolean isExplosionsEnabled() {
        return explosionsEnabled;
    }

    public void setExplosionsEnabled(boolean explosionsEnabled) {
        this.explosionsEnabled = explosionsEnabled;
    }
}
