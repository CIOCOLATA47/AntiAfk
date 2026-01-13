package me.cioco.antiafk.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AntiAfkConfig {

    public static final String CONFIG_FILE = "antiafk-config.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(AntiAfkConfig.class);

    public static boolean autoJumpEnabled = true;
    public static boolean mouseMovement = false;
    public static boolean sneak = false;
    public static boolean autoSpinEnabled = false;
    public static boolean shouldSwing = false;
    public static boolean movementEnabled = false;
    public static boolean randomPauseEnabled = false;

    public static float horizontalMultiplier = 2.0f;
    public static float verticalMultiplier = 1.5f;
    public static float spinSpeed = 5.0f;

    public static float interval = 5.0f;
    public static float minInterval = 3.0f;
    public static float maxInterval = 7.0f;
    public static boolean useRandomInterval = false;

    public void saveConfiguration() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());

            try (OutputStream output = Files.newOutputStream(configPath)) {
                Properties props = new Properties();
                props.setProperty("autoJumpEnabled", String.valueOf(autoJumpEnabled));
                props.setProperty("mouseMovement", String.valueOf(mouseMovement));
                props.setProperty("sneak", String.valueOf(sneak));
                props.setProperty("autoSpinEnabled", String.valueOf(autoSpinEnabled));
                props.setProperty("shouldSwing", String.valueOf(shouldSwing));
                props.setProperty("movementEnabled", String.valueOf(movementEnabled));
                props.setProperty("interval", String.valueOf(interval));
                props.setProperty("minInterval", String.valueOf(minInterval));
                props.setProperty("maxInterval", String.valueOf(maxInterval));
                props.setProperty("useRandomInterval", String.valueOf(useRandomInterval));
                props.setProperty("horizontalMultiplier", String.valueOf(horizontalMultiplier));
                props.setProperty("verticalMultiplier", String.valueOf(verticalMultiplier));
                props.setProperty("randomPauseEnabled", String.valueOf(randomPauseEnabled));
                props.setProperty("spinSpeed", String.valueOf(spinSpeed));

                props.store(output, "Anti-AFK Config");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save AntiAFK config", e);
        }
    }

    public void loadConfiguration() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) return;

        try (InputStream input = Files.newInputStream(configPath)) {
            Properties props = new Properties();
            props.load(input);

            autoJumpEnabled = Boolean.parseBoolean(props.getProperty("autoJumpEnabled", "true"));
            mouseMovement = Boolean.parseBoolean(props.getProperty("mouseMovement", "false"));
            sneak = Boolean.parseBoolean(props.getProperty("sneak", "false"));
            autoSpinEnabled = Boolean.parseBoolean(props.getProperty("autoSpinEnabled", "false"));
            shouldSwing = Boolean.parseBoolean(props.getProperty("shouldSwing", "false"));
            movementEnabled = Boolean.parseBoolean(props.getProperty("movementEnabled", "false"));
            interval = Float.parseFloat(props.getProperty("interval", "5.0"));
            minInterval = Float.parseFloat(props.getProperty("minInterval", "3.0"));
            maxInterval = Float.parseFloat(props.getProperty("maxInterval", "7.0"));
            useRandomInterval = Boolean.parseBoolean(props.getProperty("useRandomInterval", "false"));
            horizontalMultiplier = Float.parseFloat(props.getProperty("horizontalMultiplier", "2.0"));
            verticalMultiplier = Float.parseFloat(props.getProperty("verticalMultiplier", "1.5"));
            randomPauseEnabled = Boolean.parseBoolean(props.getProperty("randomPauseEnabled", "false"));
            spinSpeed = Float.parseFloat(props.getProperty("spinSpeed", "5.0"));
        } catch (Exception e) {
            LOGGER.error("Failed to load AntiAFK config", e);
        }
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}