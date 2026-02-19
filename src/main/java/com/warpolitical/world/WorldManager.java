package com.warpolitical.world;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Town;

import java.util.Optional;

public class WorldManager {

    public Optional<Town> getTownAt(int chunkX, int chunkZ, String worldName) {
        for (Town town : WarPoliticalMod.getInstance().getDataManager().getTowns().values()) {
            if (town.hasClaim(chunkX, chunkZ, worldName)) {
                return Optional.of(town);
            }
        }
        return Optional.empty();
    }

    public boolean isClaimed(int chunkX, int chunkZ, String worldName) {
        return getTownAt(chunkX, chunkZ, worldName).isPresent();
    }

    public static int blockToChunk(int blockCoord) {
        return blockCoord >> 4;
    }
}