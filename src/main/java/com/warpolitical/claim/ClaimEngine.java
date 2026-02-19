package com.warpolitical.claim;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

public class ClaimEngine {

    private final WarPoliticalMod mod;

    public ClaimEngine(WarPoliticalMod mod) {
        this.mod = mod;
    }

    public void fillRemainingChunks(ServerWorld world,
                                     Map<String, Town> towns,
                                     Map<String, Nation> nations,
                                     String fillMethod,
                                     double waterThreshold) {

        WarPoliticalMod.LOGGER.info("Заполнение чанков методом: {}", fillMethod);

        switch (fillMethod.toUpperCase()) {
            case "EXPAND":
                fillExpand(world, towns, nations, waterThreshold);
                break;
            case "VORONOI":
            default:
                fillVoronoi(world, towns, nations, waterThreshold);
                break;
        }
    }

    // ═══════════════════════════════════════
    //  VORONOI
    // ═══════════════════════════════════════

    private void fillVoronoi(ServerWorld world,
                              Map<String, Town> towns,
                              Map<String, Nation> nations,
                              double waterThreshold) {

        int chunkRadius = mod.getWorldManager().getWorldRadiusChunks(world);
        String worldName = mod.getDataManager().getActiveWorldName();

        // Собираем незанятые сухопутные чанки
        List<int[]> unclaimed = new ArrayList<>();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                if (mod.getDataManager().isChunkClaimed(worldName, cx, cz)) {
                    continue;
                }
                if (!mod.getWorldManager().isInWorldBorder(world, cx, cz)) {
                    continue;
                }
                // Пропускаем проверку воды для скорости при первом запуске
                // Водные чанки проверятся при необходимости
                unclaimed.add(new int[]{cx, cz});
            }
        }

        WarPoliticalMod.LOGGER.info("Незанятых чанков: {}", unclaimed.size());

        // Центры городов
        List<TownCenter> centers = new ArrayList<>();
        for (Town town : towns.values()) {
            centers.add(new TownCenter(town.getId(), town.getNationId(),
                    town.getCenterX(), town.getCenterZ()));
        }

        if (centers.isEmpty()) {
            WarPoliticalMod.LOGGER.warn("Нет городов для заполнения!");
            return;
        }

        // Сортируем по расстоянию до ближайшего центра
        unclaimed.sort((a, b) -> {
            double distA = minDistToCenter(a[0], a[1], centers);
            double distB = minDistToCenter(b[0], b[1], centers);
            return Double.compare(distA, distB);
        });

        int assigned = 0;
        int skippedWater = 0;
        int skippedLimit = 0;

        for (int[] chunk : unclaimed) {
            int cx = chunk[0];
            int cz = chunk[1];

            // Проверяем воду
            try {
                if (mod.getWorldManager().isWaterChunk(world, cx, cz, waterThreshold)) {
                    skippedWater++;
                    continue;
                }
            } catch (Exception e) {
                // Чанк может быть не загружен — пропускаем проверку воды
            }

            String bestTown = findBestTownForChunk(cx, cz, centers, towns, nations);

            if (bestTown != null) {
                mod.getDataManager().claimChunk(cx, cz, worldName, bestTown);
                assigned++;
            } else {
                skippedLimit++;
            }
        }

        WarPoliticalMod.LOGGER.info(
                "Voronoi завершён: назначено={}, водные={}, лимит={}",
                assigned, skippedWater, skippedLimit);
    }

    private String findBestTownForChunk(int cx, int cz,
                                         List<TownCenter> centers,
                                         Map<String, Town> towns,
                                         Map<String, Nation> nations) {

        // Копируем и сортируем по расстоянию
        List<TownCenter> sorted = new ArrayList<>(centers);
        sorted.sort((a, b) -> {
            double distA = distance(cx, cz, a.x, a.z);
            double distB = distance(cx, cz, b.x, b.z);
            return Double.compare(distA, distB);
        });

        for (TownCenter center : sorted) {
            Town town = towns.get(center.townId);
            Nation nation = nations.get(center.nationId);

            if (town == null || nation == null) continue;
            if (!town.canClaimMore()) continue;
            if (!nation.canClaimMore(towns)) continue;

            return center.townId;
        }

        return null;
    }

    // ═══════════════════════════════════════
    //  EXPAND (BFS)
    // ═══════════════════════════════════════

    private void fillExpand(ServerWorld world,
                             Map<String, Town> towns,
                             Map<String, Nation> nations,
                             double waterThreshold) {

        String worldName = mod.getDataManager().getActiveWorldName();
        int chunkRadius = mod.getWorldManager().getWorldRadiusChunks(world);

        // Допустимые чанки
        Set<String> allowed = new HashSet<>();
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                if (mod.getWorldManager().isInWorldBorder(world, cx, cz)) {
                    allowed.add(cx + "," + cz);
                }
            }
        }

        // Очереди BFS для каждого города
        Map<String, Queue<int[]>> queues = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();

        for (Town town : towns.values()) {
            Queue<int[]> queue = new LinkedList<>();

            for (String chunkKey : town.getClaimedChunkKeys()) {
                String[] parts = chunkKey.split(":");
                if (parts.length < 3) continue;
                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);

                for (int[] n : getNeighbors(cx, cz)) {
                    String nKey = n[0] + "," + n[1];
                    if (allowed.contains(nKey) && !visited.contains(nKey)
                            && !mod.getDataManager().isChunkClaimed(worldName, n[0], n[1])) {
                        queue.add(n);
                        visited.add(nKey);
                    }
                }
            }

            queues.put(town.getId(), queue);
        }

        // Итеративное расширение
        boolean anyExpanded = true;
        int totalAssigned = 0;

        while (anyExpanded) {
            anyExpanded = false;

            for (Map.Entry<String, Queue<int[]>> entry : queues.entrySet()) {
                String townId = entry.getKey();
                Queue<int[]> queue = entry.getValue();
                Town town = towns.get(townId);
                Nation nation = nations.get(town.getNationId());

                if (queue.isEmpty()) continue;
                if (!town.canClaimMore()) continue;
                if (nation != null && !nation.canClaimMore(towns)) continue;

                int expandCount = Math.min(4, queue.size());
                for (int i = 0; i < expandCount && !queue.isEmpty(); i++) {
                    int[] next = queue.poll();

                    if (mod.getDataManager().isChunkClaimed(worldName, next[0], next[1])) {
                        continue;
                    }
                    if (!town.canClaimMore()) break;
                    if (nation != null && !nation.canClaimMore(towns)) break;

                    mod.getDataManager().claimChunk(next[0], next[1], worldName, townId);
                    totalAssigned++;
                    anyExpanded = true;

                    for (int[] nb : getNeighbors(next[0], next[1])) {
                        String nbKey = nb[0] + "," + nb[1];
                        if (allowed.contains(nbKey) && !visited.contains(nbKey)
                                && !mod.getDataManager().isChunkClaimed(worldName, nb[0], nb[1])) {
                            queue.add(nb);
                            visited.add(nbKey);
                        }
                    }
                }
            }
        }

        WarPoliticalMod.LOGGER.info("Expand завершён: назначено {} чанков", totalAssigned);
    }

    // ═══════════════════════════════════════
    //  ПРОВЕРКИ ДЛЯ ГЕЙМПЛЕЯ
    // ═══════════════════════════════════════

    public boolean canBuild(String playerName, String worldName, int chunkX, int chunkZ) {
        Town town = mod.getDataManager().getTownAtChunk(worldName, chunkX, chunkZ);
        if (town == null) return true;
        return town.getResidents().contains(playerName);
    }

    public boolean canBreak(String playerName, String worldName, int chunkX, int chunkZ) {
        return canBuild(playerName, worldName, chunkX, chunkZ);
    }

    public boolean isPvpAllowed(String worldName, int chunkX, int chunkZ) {
        Town town = mod.getDataManager().getTownAtChunk(worldName, chunkX, chunkZ);
        if (town == null) return true;
        return town.isPvpEnabled();
    }

    // ═══════════════════════════════════════
    //  УТИЛИТЫ
    // ═══════════════════════════════════════

    private double distance(int x1, int z1, int x2, int z2) {
        int dx = x1 - x2;
        int dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double minDistToCenter(int cx, int cz, List<TownCenter> centers) {
        double min = Double.MAX_VALUE;
        for (TownCenter c : centers) {
            double d = distance(cx, cz, c.x, c.z);
            if (d < min) min = d;
        }
        return min;
    }

    private int[][] getNeighbors(int cx, int cz) {
        return new int[][]{
                {cx + 1, cz}, {cx - 1, cz},
                {cx, cz + 1}, {cx, cz - 1}
        };
    }

    private static class TownCenter {
        final String townId;
        final String nationId;
        final int x;
        final int z;

        TownCenter(String townId, String nationId, int x, int z) {
            this.townId = townId;
            this.nationId = nationId;
            this.x = x;
            this.z = z;
        }
    }
}
