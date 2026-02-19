package com.warpolitical.scenario;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class ScenarioParser {

    private final Yaml yaml = new Yaml();

    @SuppressWarnings("unchecked")
    public void loadScenario(String scenarioName) {
        String path = "/scenarios/" + scenarioName;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                WarPoliticalMod.LOGGER.warn("Сценарий не найден: {}", path);
                return;
            }

            Map<String, Object> data = yaml.load(is);
            WarPoliticalMod.LOGGER.info("Загрузка сценария: {}", scenarioName);

            List<Map<String, Object>> nations =
                    (List<Map<String, Object>>) data.get("nations");
            if (nations != null) {
                for (Map<String, Object> nationData : nations) {
                    parseNation(nationData);
                }
            }

            List<Map<String, Object>> towns =
                    (List<Map<String, Object>>) data.get("towns");
            if (towns != null) {
                for (Map<String, Object> townData : towns) {
                    parseTown(townData, null);
                }
            }

            WarPoliticalMod.getInstance().getDataManager().saveAll();
            WarPoliticalMod.LOGGER.info("Сценарий '{}' загружен успешно", scenarioName);

        } catch (Exception e) {
            WarPoliticalMod.LOGGER.error("Ошибка загрузки сценария: " + scenarioName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseNation(Map<String, Object> data) {
        String name = (String) data.get("name");
        String capital = (String) data.get("capital");
        UUID leader = UUID.fromString((String) data.getOrDefault("leader",
                UUID.randomUUID().toString()));

        Nation nation = new Nation(name, leader, capital);

        List<Map<String, Object>> towns =
                (List<Map<String, Object>>) data.get("towns");
        if (towns != null) {
            for (Map<String, Object> townData : towns) {
                Town town = parseTown(townData, name);
                nation.addTown(town.getName());
            }
        }

        WarPoliticalMod.getInstance().getDataManager().addNation(nation);
    }

    private Town parseTown(Map<String, Object> data, String nationName) {
        String name = (String) data.get("name");
        UUID mayor = UUID.fromString((String) data.getOrDefault("mayor",
                UUID.randomUUID().toString()));

        Town town = new Town(name, mayor);
        town.setNationName(nationName);

        WarPoliticalMod.getInstance().getDataManager().addTown(town);
        return town;
    }
}