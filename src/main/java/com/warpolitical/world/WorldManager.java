package com.warpolitical.world;

import com.warpolitical.WarPoliticalMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;

public class WorldManager {

    private final WarPoliticalMod mod;
    private final MinecraftServer server;

    public WorldManager(WarPoliticalMod mod, MinecraftServer server) {
        this.mod = mod;
        this.server = server;
    }

    public ServerWorld getOrCreateWorld(String worldName, long seed, int borderRadius) {
        ServerWorld world = getWorldByName(worldName);

        if (world == null) {
            WarPoliticalMod.LOGGER.warn("Мир '{}' не найден, используем overworld", worldName);
            world = server.getOverworld();
        }

        setupWorldBorder(world, borderRadius);
        setupGameRules(world);

        WarPoliticalMod.LOGGER.info("Мир '{}' настроен, граница: {}x{} блоков",
                worldName, borderRadius * 2, borderRadius * 2);

        return world;
    }

    public ServerWorld getWorldByName(String name) {
        for (ServerWorld world : server.getWorlds()) {
            String worldId = world.getRegistryKey().getValue().toString();
            String worldPath = world.getRegistryKey().getValue().getPath();

            if (worldId.equals(name) || worldPath.equals(name)
                    || worldId.contains(name) || worldPath.contains(name)) {
                return world;
            }
        }

        if (name.equals("overworld") || name.contains("overworld")) {
            return server.getOverworld();
        }

        return null;
    }

    public ServerWorld getOverworld() {
        return server.getOverworld();
    }

    public void setupWorldBorder(ServerWorld world, int radiusBlocks) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(radiusBlocks * 2);
    }

    private void setupGameRules(ServerWorld world) {
        GameRules rules = world.getGameRules();
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(true, server);
        rules.get(GameRules.MOB_GRIEFING).set(false, server);
        rules.get(GameRules.DO_FIRE_TICK).set(false, server);
        rules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server);
        rules.get(GameRules.SPAWN_RADIUS).set(0, server);
    }

    public boolean isWaterChunk(ServerWorld world, int chunkX, int chunkZ, double threshold) {
        int waterBlocks = 0;
        int totalBlocks = 256;

        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                BlockPos pos = new BlockPos(worldX, topY - 1, worldZ);
                Block block = world.getBlockState(pos).getBlock();

                if (block == Blocks.WATER
                        || block == Blocks.SEAGRASS
                        || block == Blocks.TALL_SEAGRASS
                        || block == Blocks.KELP
                        || block == Blocks.KELP_PLANT) {
                    waterBlocks++;
                }
            }
        }

        return (waterBlocks / (double) totalBlocks) >= threshold;
    }

    public int getWorldRadiusChunks(ServerWorld world) {
        return (int) (world.getWorldBorder().getSize() / 2) / 16;
    }

    public boolean isInWorldBorder(ServerWorld world, int chunkX, int chunkZ) {
        WorldBorder border = world.getWorldBorder();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double half = border.getSize() / 2;

        double blockX = chunkX * 16.0;
        double blockZ = chunkZ * 16.0;

        return blockX >= (centerX - half) && (blockX + 16) <= (centerX + half)
                && blockZ >= (centerZ - half) && (blockZ + 16) <= (centerZ + half);
    }

    public String getWorldId(ServerWorld world) {
        return world.getRegistryKey().getValue().getPath();
    }
}
