package com.warpolitical.claim;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.ClaimedChunk;
import com.warpolitical.model.Town;

import java.util.Optional;
import java.util.UUID;

public class ClaimEngine {

    private static final int MAX_CLAIMS_PER_TOWN = 100;

    public static ClaimResult claim(String townName, int chunkX, int chunkZ,
                                     String worldName, UUID claimedBy) {
        var dataManager = WarPoliticalMod.getInstance().getDataManager();
        var worldManager = WarPoliticalMod.getInstance().getWorldManager();

        Optional<Town> optTown = dataManager.getTown(townName);
        if (optTown.isEmpty()) {
            return ClaimResult.TOWN_NOT_FOUND;
        }
        Town town = optTown.get();

        if (!town.isResident(claimedBy)) {
            return ClaimResult.NOT_RESIDENT;
        }

        if (worldManager.isClaimed(chunkX, chunkZ, worldName)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        if (town.getClaims().size() >= MAX_CLAIMS_PER_TOWN) {
            return ClaimResult.LIMIT_REACHED;
        }

        ClaimedChunk chunk = new ClaimedChunk(chunkX, chunkZ, worldName, townName);
        town.addClaim(chunk);
        dataManager.saveAll();

        return ClaimResult.SUCCESS;
    }

    public static ClaimResult unclaim(String townName, int chunkX, int chunkZ,
                                       String worldName, UUID unclaimedBy) {
        var dataManager = WarPoliticalMod.getInstance().getDataManager();

        Optional<Town> optTown = dataManager.getTown(townName);
        if (optTown.isEmpty()) {
            return ClaimResult.TOWN_NOT_FOUND;
        }
        Town town = optTown.get();

        if (!town.getMayor().equals(unclaimedBy)) {
            return ClaimResult.NOT_MAYOR;
        }

        ClaimedChunk chunk = new ClaimedChunk(chunkX, chunkZ, worldName, townName);
        if (!town.removeClaim(chunk)) {
            return ClaimResult.NOT_CLAIMED;
        }

        dataManager.saveAll();
        return ClaimResult.SUCCESS;
    }

    public enum ClaimResult {
        SUCCESS,
        TOWN_NOT_FOUND,
        NOT_RESIDENT,
        NOT_MAYOR,
        ALREADY_CLAIMED,
        NOT_CLAIMED,
        LIMIT_REACHED
    }
}