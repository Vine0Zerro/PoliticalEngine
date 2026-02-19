package com.warpolitical;

import com.warpolitical.commands.CommandRegistrar;
import com.warpolitical.data.DataManager;
import com.warpolitical.listeners.ProtectionHandler;
import com.warpolitical.bluemap.BlueMapIntegration;
import com.warpolitical.world.WorldManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarPoliticalMod implements ModInitializer {

    public static final String MOD_ID = "warpolitical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static WarPoliticalMod instance;
    private DataManager dataManager;
    private WorldManager worldManager;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("WarPoliticalEngine загружается...");

        dataManager = new DataManager();
        dataManager.loadAll();

        worldManager = new WorldManager();

        CommandRegistrar.register();
        ProtectionHandler.register();
        BlueMapIntegration.tryInit();

        LOGGER.info("WarPoliticalEngine успешно загружен!");
    }

    public static WarPoliticalMod getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}