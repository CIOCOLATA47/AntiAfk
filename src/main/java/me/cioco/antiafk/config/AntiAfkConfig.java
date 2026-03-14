package me.cioco.antiafk.config;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AntiAfkConfig {

    public static final String CONFIG_FILE = "antiafk-config.properties";
    private static final Logger LOGGER = LogManager.getLogger(AntiAfkConfig.class);

    public static boolean autoJumpEnabled = true;
    public static boolean mouseMovement = false;
    public static boolean sneak = false;
    public static boolean autoSpinEnabled = false;
    public static boolean shouldSwing = false;
    public static boolean movementEnabled = false;
    public static boolean randomPauseEnabled = false;

    public static boolean autoEatEnabled = true;
    public static boolean randomHotbarEnabled = false;
    public static boolean offhandSwapEnabled = false;
    public static boolean randomInventoryEnabled = false;

    public static float horizontalMultiplier = 2.0f;
    public static float verticalMultiplier = 1.5f;
    public static float spinSpeed = 5.0f;

    public static float interval = 5.0f;
    public static float minInterval = 3.0f;
    public static float maxInterval = 7.0f;
    public static boolean useRandomInterval = false;

    public static float eatFoodLevel = 16.0f;

    public static float hotbarSwitchMinSeconds = 5.0f;
    public static float hotbarSwitchMaxSeconds = 15.0f;

    public static float offhandSwapMinSeconds = 15.0f;
    public static float offhandSwapMaxSeconds = 45.0f;
    public static float offhandHoldSeconds = 3.0f;

    public static float inventoryOpenMinSeconds = 10.0f;
    public static float inventoryOpenMaxSeconds = 25.0f;
    public static float inventoryHoldSeconds = 2.0f;

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
                props.setProperty("randomPauseEnabled", String.valueOf(randomPauseEnabled));
                props.setProperty("autoEatEnabled", String.valueOf(autoEatEnabled));
                props.setProperty("randomHotbarEnabled", String.valueOf(randomHotbarEnabled));
                props.setProperty("offhandSwapEnabled", String.valueOf(offhandSwapEnabled));
                props.setProperty("randomInventoryEnabled", String.valueOf(randomInventoryEnabled));
                props.setProperty("interval", String.valueOf(interval));
                props.setProperty("minInterval", String.valueOf(minInterval));
                props.setProperty("maxInterval", String.valueOf(maxInterval));
                props.setProperty("useRandomInterval", String.valueOf(useRandomInterval));
                props.setProperty("horizontalMultiplier", String.valueOf(horizontalMultiplier));
                props.setProperty("verticalMultiplier", String.valueOf(verticalMultiplier));
                props.setProperty("spinSpeed", String.valueOf(spinSpeed));
                props.setProperty("eatFoodLevel", String.valueOf(eatFoodLevel));
                props.setProperty("hotbarSwitchMinSeconds", String.valueOf(hotbarSwitchMinSeconds));
                props.setProperty("hotbarSwitchMaxSeconds", String.valueOf(hotbarSwitchMaxSeconds));
                props.setProperty("offhandSwapMinSeconds", String.valueOf(offhandSwapMinSeconds));
                props.setProperty("offhandSwapMaxSeconds", String.valueOf(offhandSwapMaxSeconds));
                props.setProperty("offhandHoldSeconds", String.valueOf(offhandHoldSeconds));
                props.setProperty("inventoryOpenMinSeconds", String.valueOf(inventoryOpenMinSeconds));
                props.setProperty("inventoryOpenMaxSeconds", String.valueOf(inventoryOpenMaxSeconds));
                props.setProperty("inventoryHoldSeconds", String.valueOf(inventoryHoldSeconds));
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
            randomPauseEnabled = Boolean.parseBoolean(props.getProperty("randomPauseEnabled", "false"));
            autoEatEnabled = Boolean.parseBoolean(props.getProperty("autoEatEnabled", "true"));
            randomHotbarEnabled = Boolean.parseBoolean(props.getProperty("randomHotbarEnabled", "false"));
            offhandSwapEnabled = Boolean.parseBoolean(props.getProperty("offhandSwapEnabled", "false"));
            randomInventoryEnabled = Boolean.parseBoolean(props.getProperty("randomInventoryEnabled", "false"));
            interval = Float.parseFloat(props.getProperty("interval", "5.0"));
            minInterval = Float.parseFloat(props.getProperty("minInterval", "3.0"));
            maxInterval = Float.parseFloat(props.getProperty("maxInterval", "7.0"));
            useRandomInterval = Boolean.parseBoolean(props.getProperty("useRandomInterval", "false"));
            horizontalMultiplier = Float.parseFloat(props.getProperty("horizontalMultiplier", "2.0"));
            verticalMultiplier = Float.parseFloat(props.getProperty("verticalMultiplier", "1.5"));
            spinSpeed = Float.parseFloat(props.getProperty("spinSpeed", "5.0"));
            eatFoodLevel = Float.parseFloat(props.getProperty("eatFoodLevel", "16.0"));
            hotbarSwitchMinSeconds = Float.parseFloat(props.getProperty("hotbarSwitchMinSeconds", "5.0"));
            hotbarSwitchMaxSeconds = Float.parseFloat(props.getProperty("hotbarSwitchMaxSeconds", "15.0"));
            offhandSwapMinSeconds = Float.parseFloat(props.getProperty("offhandSwapMinSeconds", "15.0"));
            offhandSwapMaxSeconds = Float.parseFloat(props.getProperty("offhandSwapMaxSeconds", "45.0"));
            offhandHoldSeconds = Float.parseFloat(props.getProperty("offhandHoldSeconds", "3.0"));
            inventoryOpenMinSeconds = Float.parseFloat(props.getProperty("inventoryOpenMinSeconds", "10.0"));
            inventoryOpenMaxSeconds = Float.parseFloat(props.getProperty("inventoryOpenMaxSeconds", "25.0"));
            inventoryHoldSeconds = Float.parseFloat(props.getProperty("inventoryHoldSeconds", "2.0"));
        } catch (Exception e) {
            LOGGER.error("Failed to load AntiAFK config", e);
        }
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}