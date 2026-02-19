package com.warpolitical.scenario;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScenarioParser {

    private final WarPoliticalMod mod;
    private final MinecraftServer server;
    private final Path scenariosDir;

    public ScenarioParser(WarPoliticalMod mod, MinecraftServer server) {
        this.mod = mod;
        this.server = server;
        this.scenariosDir = mod.getConfigDir().resolve("scenarios");
        scenariosDir.toFile().mkdirs();
        copyExampleScenario();
    }

    public List<String> getAvailableScenarios() {
        List<String> result = new ArrayList<>();
        File[] files = scenariosDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File f : files) {
                result.add(f.getName().replaceAll("\\.(yml|yaml)$", ""));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public ScenarioResult loadAndApply(String scenarioName) {
        File file = findFile(scenarioName);
        if (file == null) {
            return ScenarioResult.error("Сценарий '" + scenarioName + "' не найден!");
        }

        try {
            Yaml yaml = new Yaml();
            Map<String, Object> root;
            try (InputStream is = new FileInputStream(file)) {
                root = yaml.load(is);
            }

            Map<String, Object> scenario = (Map<String, Object>) root.get("scenario");
            if (scenario == null) {
                return ScenarioResult.error("Неверный формат: нет секции 'scenario'");
            }

            return parseAndApply(scenario);
        } catch (Exception e) {
            WarPoliticalMod.LOGGER.error("Ошибка парсинга сценария", e);
            return ScenarioResult.error("Ошибка: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ScenarioResult parseAndApply(Map<String, Object> scenario) {
        String name = (String) scenario.getOrDefault("name", "Unnamed");

        WarPoliticalMod.LOGGER.info("══════════════════════════════");
        WarPoliticalMod.LOGGER.info("  Загрузка сценария: {}", name);
        WarPoliticalMod.LOGGER.info("══════════════════════════════");

        mod.getDataManager().clearAll();

        // Мир
        Map<String, Object> worldSection = (Map<String, Object>) scenario.get("world");
        if (worldSection == null) {
            return ScenarioResult.error("Нет секции 'world'");
        }

        String worldName = (String) worldSection.getOrDefault("name", "overworld");
        long seed = getNumber(worldSection, "seed", new Random().nextLong()).longValue();
        int borderRadius = getNumber(worldSection, "border_radius", 640).intValue();

        ServerWorld world = mod.getWorldManager().getOrCreateWorld(worldName, seed, borderRadius);
        if (world == null) {
            return ScenarioResult.error("Не удалось получить мир!");
        }

        String activeWorldId = mod.getWorldManager().getWorldId(world);
        mod.getDataManager().setActiveWorldName(activeWorldId);

        // Нации
        Map<String, Object> nationsSection = (Map<String, Object>) scenario.get("nations");
        if (nationsSection == null) {
            return ScenarioResult.error("Нет секции 'nations'");
        }

        Map<String, Nation> nations = new LinkedHashMap<>();
        Map<String, Town> towns = new LinkedHashMap<>();
        Map<String, Integer> townRadii = new HashMap<>();

        for (Map.Entry<String, Object> nationEntry : nationsSection.entrySet()) {
            String nationId = nationEntry.getKey();
            Map<String, Object> ns = (Map<String, Object>) nationEntry.getValue();

            String nationName = (String) ns.getOrDefault("name", nationId);
            String nationColor = (String) ns.getOrDefault("color", "#FFFFFF");

            Nation nation = new Nation(nationId, nationName, nationColor);
            nation.setLeaderName((String) ns.getOrDefault("leader", "Unknown"));
            nation.setGovernmentType((String) ns.getOrDefault("government", "MONARCHY"));
            nation.setDescription((String) ns.getOrDefault("description", ""));

            List<String> allies = (List<String>) ns.getOrDefault("allies", Collections.emptyList());
            List<String> enemies = (List<String>) ns.getOrDefault("enemies", Collections.emptyList());
            nation.getAllies().addAll(allies);
            nation.getEnemies().addAll(enemies);

            // Города
            Map<String, Object> townsSection = (Map<String, Object>) ns.get("towns");
            if (townsSection != null) {
                for (Map.Entry<String, Object> townEntry : townsSection.entrySet()) {
                    String townLocalId = townEntry.getKey();
                    String townId = nationId + "_" + townLocalId;
                    Map<String, Object> ts = (Map<String, Object>) townEntry.getValue();

                    List<Integer> center = (List<Integer>) ts.getOrDefault(
                            "center_chunk", Arrays.asList(0, 0));
                    int cx = center.get(0);
                    int cz = center.size() > 1 ? center.get(1) : 0;

                    Town town = new Town(townId,
                            (String) ts.getOrDefault("name", townLocalId),
                            nationId, cx, cz, activeWorldId);

                    town.setMayorName((String) ts.getOrDefault("mayor", "Unknown"));
                    town.setColor((String) ts.getOrDefault("color", nationColor));
                    town.setPvpEnabled((Boolean) ts.getOrDefault("pvp", true));
                    town.setExplosionsEnabled((Boolean) ts.getOrDefault("explosions", false));

                    int radius = getNumber(ts, "radius", 5).intValue();
                    townRadii.put(townId, radius);

                    Boolean isCapital = (Boolean) ts.getOrDefault("is_capital", false);
                    if (isCapital) {
                        nation.setCapitalTownId(townId);
                    }

                    nation.addTown(townId);
                    towns.put(townId, town);

                    WarPoliticalMod.LOGGER.info("  Город: {} [{}, {}] радиус={}",
                            town.getName(), cx, cz, radius);
                }
            }

            nations.put(nationId, nation);
            WarPoliticalMod.LOGGER.info("Нация: {} ({} городов)",
                    nationName, nation.getTownIds().size());
        }

        // Сохраняем в DataManager
        for (Nation nation : nations.values()) {
            mod.getDataManager().addNation(nation);
        }
        for (Town town : towns.values()) {
            mod.getDataManager().addTown(town);
        }

        // Дипломатия
        Map<String, Object> diplomacy = (Map<String, Object>) scenario.get("diplomacy");
        if (diplomacy != null) {
            parseDiplomacy(diplomacy, nations);
        }

        // Привязка чанков: начальные радиусы
        for (Map.Entry<String, Integer> entry : townRadii.entrySet()) {
            Town town = towns.get(entry.getKey());
            int radius = entry.getValue();
            claimTownRadius(town, radius, world);
        }

        // Заполнение оставшихся чанков
        Map<String, Object> fillRules = (Map<String, Object>) scenario.get("fill_rules");
        boolean autoFill = true;
        String fillMethod = "VORONOI";
        double waterThreshold = 0.75;

        if (fillRules != null) {
            autoFill = (Boolean) fillRules.getOrDefault("auto_fill_land", true);
            fillMethod = (String) fillRules.getOrDefault("fill_method", "VORONOI");
            waterThreshold = getNumber(fillRules, "water_threshold", 0.75).doubleValue();
        }

        if (autoFill) {
            mod.getClaimEngine().fillRemainingChunks(
                    world, towns, nations, fillMethod, waterThreshold);
        }

        // BlueMap
        if (mod.getBlueMapIntegration().isInitialized()) {
            mod.getBlueMapIntegration().renderAll();
        }

        // Сохранение
        mod.getDataManager().saveAll();

        int totalChunks = mod.getDataManager().getClaimedChunks().size();

        WarPoliticalMod.LOGGER.info("══════════════════════════════");
        WarPoliticalMod.LOGGER.info("  Сценарий '{}' загружен!", name);
        WarPoliticalMod.LOGGER.info("  Наций: {}", nations.size());
        WarPoliticalMod.LOGGER.info("  Городов: {}", towns.size());
        WarPoliticalMod.LOGGER.info("  Чанков: {}", totalChunks);
        WarPoliticalMod.LOGGER.info("══════════════════════════════");

        return ScenarioResult.success(name, nations.size(), towns.size(), totalChunks);
    }

    private void claimTownRadius(Town town, int radius, ServerWorld world) {
        int cx = town.getCenterX();
        int cz = town.getCenterZ();
        int claimed = 0;
        String activeWorld = mod.getDataManager().getActiveWorldName();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    int chunkX = cx + dx;
                    int chunkZ = cz + dz;

                    if (mod.getWorldManager().isInWorldBorder(world, chunkX, chunkZ)) {
                        if (!mod.getDataManager().isChunkClaimed(activeWorld, chunkX, chunkZ)) {
                            mod.getDataManager().claimChunk(chunkX, chunkZ, activeWorld, town.getId());
                            claimed++;
                        }
                    }
                }
            }
        }

        WarPoliticalMod.LOGGER.info("  {} → {} чанков запривачено", town.getName(), claimed);
    }

    @SuppressWarnings("unchecked")
    private void parseDiplomacy(Map<String, Object> diplomacy, Map<String, Nation> nations) {
        List<Map<String, Object>> wars = (List<Map<String, Object>>) diplomacy.get("wars");
        if (wars != null) {
            for (Map<String, Object> war : wars) {
                String attacker = (String) war.get("attacker");
                String defender = (String) war.get("defender");
                String warName = (String) war.getOrDefault("name", "Война");

                Nation n1 = nations.get(attacker);
                Nation n2 = nations.get(defender);
                if (n1 != null && n2 != null) {
                    n1.getEnemies().add(defender);
                    n2.getEnemies().add(attacker);
                    n1.setAtWar(true);
                    n2.setAtWar(true);
                    WarPoliticalMod.LOGGER.info("Война: {} ({} vs {})",
                            warName, attacker, defender);
                }
            }
        }

        List<Map<String, Object>> alliances = (List<Map<String, Object>>) diplomacy.get("alliances");
        if (alliances != null) {
            for (Map<String, Object> alliance : alliances) {
                List<String> members = (List<String>) alliance.get("members");
                if (members != null) {
                    for (int i = 0; i < members.size(); i++) {
                        for (int j = i + 1; j < members.size(); j++) {
                            Nation a = nations.get(members.get(i));
                            Nation b = nations.get(members.get(j));
                            if (a != null && b != null) {
                                a.getAllies().add(b.getId());
                                b.getAllies().add(a.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    private Number getNumber(Map<String, Object> map, String key, Number defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return (Number) val;
        return defaultValue;
    }

    private File findFile(String name) {
        File yml = scenariosDir.resolve(name + ".yml").toFile();
        if (yml.exists()) return yml;
        File yaml = scenariosDir.resolve(name + ".yaml").toFile();
        if (yaml.exists()) return yaml;
        return null;
    }

    private void copyExampleScenario() {
        File target = scenariosDir.resolve("example_scenario.yml").toFile();
        if (target.exists()) return;

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("scenarios/example_scenario.yml")) {
            if (is != null) {
                Files.copy(is, target.toPath());
                WarPoliticalMod.LOGGER.info("Пример сценария скопирован");
            }
        } catch (Exception e) {
            WarPoliticalMod.LOGGER.warn("Не удалось скопировать пример: {}", e.getMessage());
        }
    }

    public static class ScenarioResult {
        public final boolean success;
        public final String message;
        public final String scenarioName;
        public final int nationCount;
        public final int townCount;
        public final int chunkCount;

        private ScenarioResult(boolean success, String message,
                                String scenarioName, int nationCount,
                                int townCount, int chunkCount) {
            this.success = success;
            this.message = message;
            this.scenarioName = scenarioName;
            this.nationCount = nationCount;
            this.townCount = townCount;
            this.chunkCount = chunkCount;
        }

        public static ScenarioResult success(String name, int nations, int towns, int chunks) {
            return new ScenarioResult(true, "OK", name, nations, towns, chunks);
        }

        public static ScenarioResult error(String message) {
            return new ScenarioResult(false, message, null, 0, 0, 0);
        }
    }
}
