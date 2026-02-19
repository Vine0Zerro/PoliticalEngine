package com.warpolitical.model;

import java.util.Objects;

public class ClaimedChunk {

    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private String townName;

    public ClaimedChunk(int chunkX, int chunkZ, String worldName, String townName) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldName = worldName;
        this.townName = townName;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public String getWorldName() { return worldName; }
    public String getTownName() { return townName; }
    public void setTownName(String townName) { this.townName = townName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimedChunk other)) return false;
        return chunkX == other.chunkX &&
               chunkZ == other.chunkZ &&
               worldName.equals(other.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkX, chunkZ, worldName);
    }

    @Override
    public String toString() {
        return "ClaimedChunk{" + worldName + " [" + chunkX + ", " + chunkZ + "] -> " + townName + "}";
    }
}