package com.warpolitical.bluemap;

import com.warpolitical.WarPoliticalMod;

public class BlueMapIntegration {

    private static boolean available = false;

    public static void tryInit() {
        try {
            Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
            available = true;
            WarPoliticalMod.LOGGER.info("BlueMap обнаружен! Интеграция активирована.");
            initBlueMap();
        } catch (ClassNotFoundException e) {
            available = false;
            WarPoliticalMod.LOGGER.info("BlueMap не найден — интеграция отключена.");
        }
    }

    private static void initBlueMap() {
        WarPoliticalMod.LOGGER.info("BlueMap интеграция: инициализация маркеров...");
    }

    public static void updateMarkers() {
        if (!available) return;
    }

    public static boolean isAvailable() {
        return available;
    }
}