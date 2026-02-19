package com.warpolitical.model;

import java.util.*;

public class Nation {

    private String name;
    private UUID leader;
    private String capitalTownName;
    private final Set<String> memberTowns = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();
    private long createdAt;

    public Nation(String name, UUID leader, String capitalTownName) {
        this.name = name;
        this.leader = leader;
        this.capitalTownName = capitalTownName;
        this.memberTowns.add(capitalTownName);
        this.createdAt = System.currentTimeMillis();
    }

    public boolean addTown(String townName) {
        return memberTowns.add(townName);
    }

    public boolean removeTown(String townName) {
        if (townName.equals(capitalTownName)) return false;
        return memberTowns.remove(townName);
    }

    public boolean hasTown(String townName) {
        return memberTowns.contains(townName);
    }

    public void addAlly(String nationName) {
        enemies.remove(nationName);
        allies.add(nationName);
    }

    public void addEnemy(String nationName) {
        allies.remove(nationName);
        enemies.add(nationName);
    }

    public boolean isAlly(String nationName) { return allies.contains(nationName); }
    public boolean isEnemy(String nationName) { return enemies.contains(nationName); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public String getCapitalTownName() { return capitalTownName; }
    public void setCapitalTownName(String capitalTownName) { this.capitalTownName = capitalTownName; }

    public Set<String> getMemberTowns() { return Collections.unmodifiableSet(memberTowns); }
    public Set<String> getAllies() { return Collections.unmodifiableSet(allies); }
    public Set<String> getEnemies() { return Collections.unmodifiableSet(enemies); }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}