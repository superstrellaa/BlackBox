package es.superstrellaa.blackbox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import es.superstrellaa.blackbox.BlackBox;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlackBoxConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("blackbox.json");
    private static BlackBoxConfig instance;

    public String webhookUrl = "";
    public boolean enabled = true;
    public int batchIntervalSeconds = 60;

    public boolean anonymous = false;

    public DataOptions data = new DataOptions();
    public TriggerOptions triggers = new TriggerOptions();

    public static class DataOptions {
        public boolean fps = true;
        public boolean memory = true;
        public boolean hardware = true;
        public boolean graphicsSettings = true;
        public boolean modsList = true;
    }

    public static class TriggerOptions {
        public boolean onDisconnect = true;
        public boolean onCrash = true;
        public boolean onGameClose = true;
    }

    public static BlackBoxConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static BlackBoxConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BlackBoxConfig loaded = GSON.fromJson(reader, BlackBoxConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException e) {
                BlackBox.LOGGER.error("Failed to read BlackBox config, using defaults", e);
            }
        }
        BlackBoxConfig config = new BlackBoxConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            BlackBox.LOGGER.error("Failed to save BlackBox config", e);
        }
    }
}