package me.cioco.antiafk.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiAfkConfig {

    public static final String CONFIG_FILE = "antiafk-config.properties";

    public static boolean autoJumpEnabled = true;
    public static boolean mouseMovement = false;
    public static boolean sneak = false;
    public static boolean autoSpinEnabled = false;
    public static boolean shouldSwing = false;
    public static boolean movementEnabled = false;
    public static float horizontalMultiplier = 2.0f;
    public static float verticalMultiplier = 1.5f;
    public static boolean randomPauseEnabled = false;
    private static int interval = 100;
    private static int minInterval = 80;
    private static int maxInterval = 120;
    private static boolean isRandomInterval = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(AntiAfkConfig.class);

    public void saveConfiguration() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());

            try (OutputStream output = Files.newOutputStream(configPath)) {
                Properties properties = new Properties();
                properties.setProperty("autoJumpEnabled", String.valueOf(autoJumpEnabled));
                properties.setProperty("mouseMovement", String.valueOf(mouseMovement));
                properties.setProperty("sneak", String.valueOf(sneak));
                properties.setProperty("autoSpinEnabled", String.valueOf(autoSpinEnabled));
                properties.setProperty("shouldSwing", String.valueOf(shouldSwing));
                properties.setProperty("movementEnabled", String.valueOf(movementEnabled));
                properties.setProperty("interval", String.valueOf(interval));
                properties.setProperty("minInterval", String.valueOf(minInterval));
                properties.setProperty("maxInterval", String.valueOf(maxInterval));
                properties.setProperty("isRandomInterval", String.valueOf(isRandomInterval));
                properties.setProperty("horizontalMultiplier", String.valueOf(horizontalMultiplier));
                properties.setProperty("verticalMultiplier", String.valueOf(verticalMultiplier));
                properties.setProperty("randomPauseEnabled", String.valueOf(randomPauseEnabled));

                properties.store(output, null);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save AntiAFK configuration", e);
        }
    }

    public void loadConfiguration() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            return;
        }
        try (InputStream input = Files.newInputStream(configPath)) {
            Properties properties = new Properties();
            properties.load(input);

            autoJumpEnabled = Boolean.parseBoolean(properties.getProperty("autoJumpEnabled", "true"));
            mouseMovement = Boolean.parseBoolean(properties.getProperty("mouseMovement", "false"));
            sneak = Boolean.parseBoolean(properties.getProperty("sneak", "false"));
            autoSpinEnabled = Boolean.parseBoolean(properties.getProperty("autoSpinEnabled", "false"));
            shouldSwing = Boolean.parseBoolean(properties.getProperty("shouldSwing", "false"));
            movementEnabled = Boolean.parseBoolean(properties.getProperty("movementEnabled"));
            interval = Integer.parseInt(properties.getProperty("interval", "100"));
            minInterval = Integer.parseInt(properties.getProperty("minInterval", "80"));
            maxInterval = Integer.parseInt(properties.getProperty("maxInterval", "120"));
            isRandomInterval = Boolean.parseBoolean(properties.getProperty("isRandomInterval", "false"));
            horizontalMultiplier = Float.parseFloat(properties.getProperty("horizontalMultiplier", "2.0"));
            verticalMultiplier = Float.parseFloat(properties.getProperty("verticalMultiplier", "1.5"));
            randomPauseEnabled = Boolean.parseBoolean(properties.getProperty("randomPauseEnabled", "false"));

        } catch (IOException e) {
            LOGGER.error("Failed to load AntiAFK configuration", e);
        }
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public int getInterval() {
        if (isRandomInterval) {
            return minInterval + new Random().nextInt(maxInterval - minInterval + 1);
        } else {
            return interval;
        }
    }

    public void setInterval(int interval) {
        AntiAfkConfig.interval = interval;
    }

    public void setMinInterval(int minInterval) {
        AntiAfkConfig.minInterval = minInterval;
    }

    public void setMaxInterval(int maxInterval) {
        AntiAfkConfig.maxInterval = maxInterval;
    }

    public void setRandomInterval(boolean isRandom) {
        AntiAfkConfig.isRandomInterval = isRandom;
    }
}
