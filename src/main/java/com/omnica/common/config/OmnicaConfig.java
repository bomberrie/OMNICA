package com.omnica.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global configuration for OMNICA.
 * Persisted in config/omnica.json in the Minecraft run directory.
 */
public class OmnicaConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Omnica/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("omnica.json");

    private static OmnicaConfig INSTANCE = new OmnicaConfig();

    // Configurable gameplay and visual properties
    public int maxLinkDistance = 32;           // Range between connected nodes in blocks (8 - 128)
    public int transferIntervalTicks = 8;      // Ticks between network transfer pulses (1 - 40 ticks, 8 = 0.4s)
    public int itemsPerTransfer = 64;          // Max items transferred per pulse (1 - 64)
    public boolean renderLaserBeams = true;    // Render visual tether laser lines between connected nodes
    public boolean renderFloatingItems = true; // Render physical floating item animations during transfer
    public boolean enableSoundEffects = true;  // Play resonant amethyst chime audio cues

    public static OmnicaConfig getInstance() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                OmnicaConfig loaded = GSON.fromJson(reader, OmnicaConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    INSTANCE.validate();
                    LOGGER.info("Loaded OMNICA configuration from {}", CONFIG_FILE);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read omnica.json, defaulting to standard settings", e);
            }
        }
        INSTANCE = new OmnicaConfig();
        save();
    }

    public static void save() {
        try {
            if (CONFIG_FILE.getParent() != null) {
                Files.createDirectories(CONFIG_FILE.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
            LOGGER.info("Saved OMNICA configuration to {}", CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to write omnica.json", e);
        }
    }

    public void validate() {
        if (maxLinkDistance < 8) maxLinkDistance = 8;
        if (maxLinkDistance > 256) maxLinkDistance = 256;
        if (transferIntervalTicks < 1) transferIntervalTicks = 1;
        if (transferIntervalTicks > 100) transferIntervalTicks = 100;
        if (itemsPerTransfer < 1) itemsPerTransfer = 1;
        if (itemsPerTransfer > 64) itemsPerTransfer = 64;
    }
}
