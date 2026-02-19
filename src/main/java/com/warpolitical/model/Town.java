package com.warpolitical.model;

import java.util.*;

public class Town {

    private String name;
    private UUID mayor;
    private String nationName;
    private final Set<UUID> residents = new HashSet<>();
    private final Set<ClaimedChunk> claims = new HashSet<>();
    private long createdAt;

    public Town(String name, UUID mayor) {
        this.name = name;
        this.mayor = mayor;
        this.residents.add(mayor);
        this.createdAt = System.currentTimeMillis();
    }

    public boolean addResident(UUID player) {
        return residents.add(player);
    }

    public boolean removeResident(UUID player) {
        if (player.equals(mayor)) return false;
        return residents.remove(player);
    }

    public boolean isResident(UUID player) {
        return residents.contains(player);
    }

    public boolean addClaim(ClaimedChunk chunk) {
        return claims.add(chunk);
    }

    public boolean removeClaim(ClaimedChunk chunk) {
        return claims.remove(chunk);
    }

    public boolean hasClaim(int chunkX, int chunkZ, String world) {
        return claims.stream().anyMatch(c ->
                c.getChunkX() == chunkX &&
                c.getChunkZ() == chunkZ &&
                c.getWorldName().equals(world));
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getMayor() { return mayor; }
    public void setMayor(UUID mayor) { this.mayor = mayor; }

    public String getNationName() { return nationName; }
    public void setNationName(String nationName) { this.nationName = nationName; }

    public Set<UUID> getResidents() { return Collections.unmodifiableSet(residents); }
    public Set<ClaimedChunk> getClaims() { return Collections.unmodifiableSet(claims); }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Town{name='" + name + "', mayor=" + mayor +
               ", residents=" + residents.size() +
               ", claims=" + claims.size() + "}";
    }
}