package me.cioco.antiafk;

import me.cioco.antiafk.config.AntiAfkConfig;
import me.cioco.antiafk.gui.AntiAfkScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static final AntiAfkConfig config = new AntiAfkConfig();

    public static final KeyMapping.Category CATEGORY_ANTIAFK = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("antiafk", "key_category"));

    public static KeyMapping toggleKeyBinding;
    public static KeyMapping guiKeyBinding;
    public static boolean toggled = false;

    public static long afkStartTime = 0;
    public static long afkElapsedSeconds = 0;
    private static boolean wasToggled = false;
    private static boolean warningSent = false;

    @Override
    public void onInitialize() {
        config.loadConfiguration();

        toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.antiafk.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_ANTIAFK
        ));

        guiKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.antiafk.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_ANTIAFK
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (toggled) {
                if (!wasToggled) {
                    afkStartTime = System.currentTimeMillis();
                    wasToggled = true;
                    warningSent = false;
                }
                afkElapsedSeconds = (System.currentTimeMillis() - afkStartTime) / 1000;

                if (AntiAfkConfig.autoLogoutEnabled) {
                    float maxSeconds = AntiAfkConfig.autoLogoutMinutes * 60;

                    if (afkElapsedSeconds >= maxSeconds) {
                        client.player.sendSystemMessage(
                                Component.literal("§c[AntiAFK] Auto-logout triggered after " +
                                        formatTime(afkElapsedSeconds) + " of AFK time!")
                        );
                        if (client.getConnection() != null) {
                            client.getConnection().getConnection().disconnect(
                                    Component.literal("Auto-logout after " + formatTime(afkElapsedSeconds) + " AFK")
                            );
                        }
                        toggled = false;
                        wasToggled = false;
                        afkElapsedSeconds = 0;
                        warningSent = false;
                        return;
                    }

                    float warningTime = maxSeconds * 0.8f;
                    if (afkElapsedSeconds >= warningTime && !warningSent) {
                        float minutesLeft = (maxSeconds - afkElapsedSeconds) / 60f;
                        client.player.sendSystemMessage(
                                Component.literal("§e[AntiAFK] Auto-logout in " +
                                        String.format("%.1f", minutesLeft) + " minutes!")
                        );
                        warningSent = true;
                    }
                }
            } else {
                if (wasToggled) {
                    wasToggled = false;
                    afkElapsedSeconds = 0;
                    warningSent = false;
                }
            }

            if (toggleKeyBinding.consumeClick()) {
                toggled = !toggled;

                if (client.gui.screen() instanceof AntiAfkScreen screen) {
                    screen.refreshGlobalToggle();
                }

                Component status = Component.literal("AntiAfk: ")
                        .append(Component.literal(toggled ? "Enabled" : "Disabled")
                                .withStyle(toggled ? ChatFormatting.GREEN : ChatFormatting.RED));

                client.player.sendSystemMessage(status);

                if (!toggled) {
                    afkElapsedSeconds = 0;
                    wasToggled = false;
                    warningSent = false;
                }
            }

            if (guiKeyBinding.consumeClick()) {
                client.setScreenAndShow(new AntiAfkScreen(client.gui.screen()));
            }
        });
    }

    public static String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }
}