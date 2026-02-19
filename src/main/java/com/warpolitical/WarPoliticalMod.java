package com.warpolitical;

import com.warpolitical.bluemap.BlueMapIntegration;
import com.warpolitical.claim.ClaimEngine;
import com.warpolitical.commands.CommandRegistrar;
import com.warpolitical.data.DataManager;
import com.warpolitical.listeners.ProtectionHandler;
import com.warpolitical.scenario.ScenarioParser;
import com.warpolitical.world.WorldManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class WarPoliticalMod implements ModInitializer {

    public static final String MOD_ID = "warpolitical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static WarPoliticalMod instance;
    private MinecraftServer server;

    private DataManager dataManager;
    private ScenarioParser scenarioParser;
    private WorldManager worldManager;
    private ClaimEngine claimEngine;
    private BlueMapIntegration blueMapIntegration;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("WarPoliticalEngine загружается...");

        Path configDir = FabricLoader.getInstance()
                .getConfigDir().resolve(MOD_ID);
        configDir.toFile().mkdirs();

        this.dataManager = new DataManager(configDir);
        this.claimEngine = new ClaimEngine(this);
        this.blueMapIntegration = new BlueMapIntegration(this);

        CommandRegistrar.register(this);
        ProtectionHandler.register(this);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        LOGGER.info("WarPoliticalEngine зарегистрирован!");
    }

    private void onServerStarted(MinecraftServer server) {
        this.server = server;
        this.worldManager = new WorldManager(this, server);
        this.scenarioParser = new ScenarioParser(this, server);

        dataManager.loadAll();

        if (FabricLoader.getInstance().isModLoaded("bluemap")) {
            blueMapIntegration.initialize();
            LOGGER.info("BlueMap обнаружен, интеграция активна");
        } else {
            LOGGER.warn("BlueMap не найден, маркеры недоступны");
        }

        LOGGER.info("WarPoliticalEngine полностью запущен!");
    }

    private void onServerStopping(MinecraftServer server) {
        if (dataManager != null) dataManager.saveAll();
        if (blueMapIntegration != null) blueMapIntegration.shutdown();
        LOGGER.info("WarPoliticalEngine остановлен");
    }

    public static WarPoliticalMod getInstance() {
        return instance;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ScenarioParser getScenarioParser() {
        return scenarioParser;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ClaimEngine getClaimEngine() {
        return claimEngine;
    }

    public BlueMapIntegration getBlueMapIntegration() {
        return blueMapIntegration;
    }

    public Path getConfigDir() {
        return FabricLoader.getInstance()
                .getConfigDir().resolve(MOD_ID);
    }
}
