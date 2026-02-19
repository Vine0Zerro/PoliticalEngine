package com.warpolitical.bluemap;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.ClaimedChunk;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.*;
import java.util.stream.Collectors;

public class BlueMapIntegration {

    private final WarPoliticalMod mod;
    private BlueMapAPI api;
    private boolean initialized = false;

    private static final String MS_NATIONS = "wpe_nations";
    private static final String MS_TOWNS = "wpe_towns";
    private static final String MS_CAPITALS = "wpe_capitals";

    public BlueMapIntegration(WarPoliticalMod mod) {
        this.mod = mod;
    }

    public void initialize() {
        BlueMapAPI.onEnable(api -> {
            this.api = api;
            this.initialized = true;
            WarPoliticalMod.LOGGER.info("BlueMap API подключён");

            if (!mod.getDataManager().getNations().isEmpty()) {
                renderAll();
            }
        });

        BlueMapAPI.onDisable(api -> {
            this.api = null;
            this.initialized = false;
        });
    }

    public void shutdown() {
        if (initialized && api != null) {
            clearAllMarkers();
        }
    }

    public boolean isInitialized() {
        return initialized && api != null;
    }

    public void renderAll() {
        if (!isInitialized()) {
            WarPoliticalMod.LOGGER.warn("BlueMap не инициализирован");
            return;
        }

        String worldName = mod.getDataManager().getActiveWorldName();
        if (worldName == null) return;

        try {
            clearAllMarkers();
            renderNationTerritories();
            renderTownBorders();
            renderCapitalMarkers();
            WarPoliticalMod.LOGGER.info("BlueMap маркеры обновлены");
        } catch (Exception e) {
            WarPoliticalMod.LOGGER.error("Ошибка рендера BlueMap", e);
        }
    }

    private List<BlueMapMap> getMaps() {
        if (api == null) return Collections.emptyList();

        String worldName = mod.getDataManager().getActiveWorldName();
        if (worldName == null) return Collections.emptyList();

        List<BlueMapMap> result = new ArrayList<>();

        for (BlueMapMap map : api.getMaps()) {
            String mapWorld = map.getWorld().getId();
            if (mapWorld.contains(worldName) || worldName.contains("overworld")) {
                result.add(map);
            }
        }

        if (result.isEmpty()) {
            // Берём все карты overworld
            for (BlueMapMap map : api.getMaps()) {
                if (map.getWorld().getId().contains("overworld")) {
                    result.add(map);
                }
            }
        }

        return result;
    }

    // ═══════════════════════════════════════
    //  ТЕРРИТОРИИ НАЦИЙ
    // ═══════════════════════════════════════

    private void renderNationTerritories() {
        List<BlueMapMap> maps = getMaps();
        if (maps.isEmpty()) return;

        Map<String, Nation> nations = mod.getDataManager().getNations();
        Map<String, Town> towns = mod.getDataManager().getTowns();

        for (BlueMapMap map : maps) {
            MarkerSet markerSet = MarkerSet.builder()
                    .label("Нации")
                    .defaultHidden(false)
                    .toggleable(true)
                    .build();

            for (Nation nation : nations.values()) {
                Set<String> nationChunkKeys = new HashSet<>();
                for (String townId : nation.getTownIds()) {
                    Town town = towns.get(townId);
                    if (town != null) {
                        nationChunkKeys.addAll(town.getClaimedChunkKeys());
                    }
                }

                if (nationChunkKeys.isEmpty()) continue;

                List<ChunkGroup> groups = groupAdjacentChunks(nationChunkKeys);

                int polyIndex = 0;
                for (ChunkGroup group : groups) {
                    List<double[]> outline = calculateOutline(group.chunks);
                    if (outline.size() < 3) continue;

                    Shape shape = createShape(outline);
                    if (shape == null) continue;

                    Color fillColor = parseColor(nation.getColor(), 80);
                    Color lineColor = parseColor(nation.getColor(), 200);

                    ShapeMarker marker = ShapeMarker.builder()
                            .label(nation.getName())
                            .shape(shape, 64)
                            .fillColor(fillColor)
                            .lineColor(lineColor)
                            .lineWidth(2)
                            .depthTestEnabled(false)
                            .detail(buildNationHtml(nation))
                            .build();

                    markerSet.getMarkers().put(
                            "nation_" + nation.getId() + "_" + polyIndex, marker);
                    polyIndex++;
                }
            }

            map.getMarkerSets().put(MS_NATIONS, markerSet);
        }
    }

    // ═══════════════════════════════════════
    //  ГРАНИЦЫ ГОРОДОВ
    // ═══════════════════════════════════════

    private void renderTownBorders() {
        List<BlueMapMap> maps = getMaps();
        if (maps.isEmpty()) return;

        Map<String, Town> towns = mod.getDataManager().getTowns();

        for (BlueMapMap map : maps) {
            MarkerSet markerSet = MarkerSet.builder()
                    .label("Города")
                    .defaultHidden(false)
                    .toggleable(true)
                    .build();

            for (Town town : towns.values()) {
                if (town.getClaimedChunkKeys().isEmpty()) continue;

                List<ChunkGroup> groups = groupAdjacentChunks(town.getClaimedChunkKeys());

                int polyIndex = 0;
                for (ChunkGroup group : groups) {
                    List<double[]> outline = calculateOutline(group.chunks);
                    if (outline.size() < 3) continue;

                    Shape shape = createShape(outline);
                    if (shape == null) continue;

                    Color fillColor = parseColor(town.getColor(), 40);
                    Color lineColor = parseColor(town.getColor(), 255);

                    ShapeMarker marker = ShapeMarker.builder()
                            .label(town.getName())
                            .shape(shape, 65)
                            .fillColor(fillColor)
                            .lineColor(lineColor)
                            .lineWidth(1)
                            .depthTestEnabled(false)
                            .detail(buildTownHtml(town))
                            .build();

                    markerSet.getMarkers().put(
                            "town_" + town.getId() + "_" + polyIndex, marker);
                    polyIndex++;
                }
            }

            map.getMarkerSets().put(MS_TOWNS, markerSet);
        }
    }

    // ═══════════════════════════════════════
    //  МАРКЕРЫ СТОЛИЦ
    // ═══════════════════════════════════════

    private void renderCapitalMarkers() {
        List<BlueMapMap> maps = getMaps();
        if (maps.isEmpty()) return;

        Map<String, Nation> nations = mod.getDataManager().getNations();
        Map<String, Town> towns = mod.getDataManager().getTowns();

        for (BlueMapMap map : maps) {
            MarkerSet markerSet = MarkerSet.builder()
                    .label("Столицы и города")
                    .defaultHidden(false)
                    .toggleable(true)
                    .build();

            for (Nation nation : nations.values()) {
                String capitalId = nation.getCapitalTownId();
                if (capitalId == null) continue;

                Town capital = towns.get(capitalId);
                if (capital == null) continue;

                double x = capital.getCenterX() * 16 + 8;
                double z = capital.getCenterZ() * 16 + 8;

                POIMarker marker = POIMarker.builder()
                        .label("★ " + capital.getName() + " — " + nation.getName())
                        .position(x, 70, z)
                        .maxDistance(10000)
                        .detail(buildCapitalHtml(nation, capital))
                        .build();

                markerSet.getMarkers().put("capital_" + nation.getId(), marker);
            }

            // Обычные города
            for (Town town : towns.values()) {
                Nation nation = nations.get(town.getNationId());
                if (nation != null && town.getId().equals(nation.getCapitalTownId())) {
                    continue;
                }

                double x = town.getCenterX() * 16 + 8;
                double z = town.getCenterZ() * 16 + 8;

                POIMarker marker = POIMarker.builder()
                        .label(town.getName())
                        .position(x, 70, z)
                        .maxDistance(5000)
                        .detail(buildTownHtml(town))
                        .build();

                markerSet.getMarkers().put("town_poi_" + town.getId(), marker);
            }

            map.getMarkerSets().put(MS_CAPITALS, markerSet);
        }
    }

    // ═══════════════════════════════════════
    //  ГЕОМЕТРИЯ
    // ═══════════════════════════════════════

    private List<ChunkGroup> groupAdjacentChunks(Set<String> chunkKeys) {
        Map<String, int[]> coords = new HashMap<>();
        for (String key : chunkKeys) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);
                coords.put(cx + "," + cz, new int[]{cx, cz});
            }
        }

        List<ChunkGroup> groups = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String coordKey : coords.keySet()) {
            if (visited.contains(coordKey)) continue;

            ChunkGroup group = new ChunkGroup();
            Queue<String> queue = new LinkedList<>();
            queue.add(coordKey);
            visited.add(coordKey);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                int[] c = coords.get(current);
                group.chunks.add(c);

                int[][] neighbors = {
                        {c[0] + 1, c[1]}, {c[0] - 1, c[1]},
                        {c[0], c[1] + 1}, {c[0], c[1] - 1}
                };

                for (int[] n : neighbors) {
                    String nKey = n[0] + "," + n[1];
                    if (coords.containsKey(nKey) && !visited.contains(nKey)) {
                        visited.add(nKey);
                        queue.add(nKey);
                    }
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private List<double[]> calculateOutline(List<int[]> chunks) {
        Set<String> occupied = new HashSet<>();
        for (int[] c : chunks) {
            occupied.add(c[0] + "," + c[1]);
        }

        List<double[][]> edges = new ArrayList<>();

        for (int[] c : chunks) {
            int bx = c[0] * 16;
            int bz = c[1] * 16;

            if (!occupied.contains(c[0] + "," + (c[1] - 1))) {
                edges.add(new double[][]{{bx, bz}, {bx + 16, bz}});
            }
            if (!occupied.contains(c[0] + "," + (c[1] + 1))) {
                edges.add(new double[][]{{bx + 16, bz + 16}, {bx, bz + 16}});
            }
            if (!occupied.contains((c[0] - 1) + "," + c[1])) {
                edges.add(new double[][]{{bx, bz + 16}, {bx, bz}});
            }
            if (!occupied.contains((c[0] + 1) + "," + c[1])) {
                edges.add(new double[][]{{bx + 16, bz}, {bx + 16, bz + 16}});
            }
        }

        if (edges.isEmpty()) return Collections.emptyList();
        return chainEdges(edges);
    }

    private List<double[]> chainEdges(List<double[][]> edges) {
        if (edges.isEmpty()) return Collections.emptyList();

        Map<String, List<double[][]>> edgeMap = new HashMap<>();
        for (double[][] edge : edges) {
            String key = edge[0][0] + "," + edge[0][1];
            edgeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(edge);
        }

        List<double[]> result = new ArrayList<>();
        Set<String> used = new HashSet<>();

        double[][] first = edges.get(0);
        used.add(edgeId(first));
        result.add(first[0]);

        double[] target = first[0];
        double[] pos = first[1];

        int maxIter = edges.size() + 1;
        int iter = 0;

        while (iter < maxIter) {
            iter++;

            if (Math.abs(pos[0] - target[0]) < 0.1
                    && Math.abs(pos[1] - target[1]) < 0.1
                    && result.size() > 2) {
                break;
            }

            result.add(pos);

            String posKey = pos[0] + "," + pos[1];
            List<double[][]> candidates = edgeMap.get(posKey);
            boolean found = false;

            if (candidates != null) {
                for (double[][] candidate : candidates) {
                    String cId = edgeId(candidate);
                    if (!used.contains(cId)) {
                        used.add(cId);
                        pos = candidate[1];
                        found = true;
                        break;
                    }
                }
            }

            if (!found) break;
        }

        return result;
    }

    private String edgeId(double[][] edge) {
        return edge[0][0] + "," + edge[0][1] + "->" + edge[1][0] + "," + edge[1][1];
    }

    private Shape createShape(List<double[]> outline) {
        if (outline.size() < 3) return null;

        try {
            Shape.Builder builder = Shape.builder();
            for (double[] point : outline) {
                builder.addPoint(point[0], point[1]);
            }
            return builder.build();
        } catch (Exception e) {
            WarPoliticalMod.LOGGER.warn("Ошибка создания Shape: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════
    //  HTML ПОПАПЫ
    // ═══════════════════════════════════════

    private String buildNationHtml(Nation nation) {
        Map<String, Town> towns = mod.getDataManager().getTowns();
        int totalChunks = nation.getTotalChunks(towns);
        Map<String, Nation> allNations = mod.getDataManager().getNations();

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial;padding:8px;'>");
        sb.append("<h3 style='color:").append(nation.getColor())
                .append(";margin:0;'>").append(nation.getName()).append("</h3>");
        sb.append("<hr style='border-color:").append(nation.getColor()).append("'>");
        sb.append("<b>Строй:</b> ").append(translateGov(nation.getGovernmentType())).append("<br>");
        sb.append("<b>Лидер:</b> ").append(nation.getLeaderName()).append("<br>");
        sb.append("<b>Городов:</b> ").append(nation.getTownIds().size()).append("<br>");
        sb.append("<b>Территория:</b> ").append(totalChunks).append("/")
                .append(Nation.MAX_CHUNKS).append(" чанков<br>");

        if (nation.getDescription() != null && !nation.getDescription().isEmpty()) {
            sb.append("<i>").append(nation.getDescription()).append("</i><br>");
        }

        if (!nation.getAllies().isEmpty()) {
            sb.append("<b>Союзники:</b> ");
            sb.append(nation.getAllies().stream()
                    .map(id -> {
                        Nation a = allNations.get(id);
                        return a != null ? a.getName() : id;
                    }).collect(Collectors.joining(", ")));
            sb.append("<br>");
        }

        if (!nation.getEnemies().isEmpty()) {
            sb.append("<b style='color:red;'>Враги:</b> ");
            sb.append(nation.getEnemies().stream()
                    .map(id -> {
                        Nation e = allNations.get(id);
                        return e != null ? e.getName() : id;
                    }).collect(Collectors.joining(", ")));
            sb.append("<br>");
        }

        if (nation.isAtWar()) {
            sb.append("<b style='color:red;'>⚔ В СОСТОЯНИИ ВОЙНЫ</b><br>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String buildTownHtml(Town town) {
        Nation nation = mod.getDataManager().getNation(town.getNationId());
        String nationName = nation != null ? nation.getName() : "—";

        return "<div style='font-family:Arial;padding:6px;'>"
                + "<h4 style='color:" + town.getColor() + ";margin:0;'>"
                + town.getName() + "</h4>"
                + "<b>Нация:</b> " + nationName + "<br>"
                + "<b>Мэр:</b> " + town.getMayorName() + "<br>"
                + "<b>Чанков:</b> " + town.getChunkCount() + "/" + Town.MAX_CHUNKS + "<br>"
                + "<b>PVP:</b> " + (town.isPvpEnabled() ? "✅" : "❌") + "<br>"
                + "<b>Взрывы:</b> " + (town.isExplosionsEnabled() ? "✅" : "❌")
                + "</div>";
    }

    private String buildCapitalHtml(Nation nation, Town capital) {
        return "<div style='font-family:Arial;padding:10px;border:2px solid "
                + nation.getColor() + ";'>"
                + "<h2 style='color:" + nation.getColor() + ";margin:0;'>★ "
                + nation.getName() + "</h2>"
                + "<h4 style='margin:4px 0;'>Столица: " + capital.getName() + "</h4>"
                + "<hr>"
                + "<b>Лидер:</b> " + nation.getLeaderName() + "<br>"
                + "<b>Строй:</b> " + translateGov(nation.getGovernmentType()) + "<br>"
                + "<b>Городов:</b> " + nation.getTownIds().size() + "<br>"
                + "<b>Территория:</b> "
                + nation.getTotalChunks(mod.getDataManager().getTowns())
                + " чанков<br>"
                + (nation.isAtWar() ? "<b style='color:red;'>⚔ В ВОЙНЕ</b>" : "")
                + "</div>";
    }

    private String translateGov(String type) {
        if (type == null) return "Неизвестно";
        return switch (type) {
            case "MONARCHY" -> "👑 Монархия";
            case "DEMOCRACY" -> "🗳 Демократия";
            case "DICTATORSHIP" -> "⚡ Диктатура";
            case "REPUBLIC" -> "🏛 Республика";
            case "EMPIRE" -> "🦅 Империя";
            case "FEDERATION" -> "🤝 Федерация";
            case "THEOCRACY" -> "⛪ Теократия";
            case "COMMUNISM" -> "☭ Коммунизм";
            default -> type;
        };
    }

    // ═══════════════════════════════════════
    //  УТИЛИТЫ
    // ═══════════════════════════════════════

    private Color parseColor(String hex, int alpha) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b, alpha);
        } catch (Exception e) {
            return new Color(255, 255, 255, alpha);
        }
    }

    private void clearAllMarkers() {
        for (BlueMapMap map : getMaps()) {
            map.getMarkerSets().remove(MS_NATIONS);
            map.getMarkerSets().remove(MS_TOWNS);
            map.getMarkerSets().remove(MS_CAPITALS);
        }
    }

    private static class ChunkGroup {
        List<int[]> chunks = new ArrayList<>();
    }
}
