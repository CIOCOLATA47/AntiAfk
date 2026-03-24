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

            if (toggleKeyBinding.consumeClick()) {
                toggled = !toggled;

                if (client.screen instanceof AntiAfkScreen screen) {
                    screen.refreshGlobalToggle();
                }

                Component status = Component.literal("AntiAfk: ")
                        .append(Component.literal(toggled ? "Enabled" : "Disabled")
                                .withStyle(toggled ? ChatFormatting.GREEN : ChatFormatting.RED));

                client.player.sendSystemMessage(status);
            }

            if (guiKeyBinding.consumeClick()) {
                client.setScreen(new AntiAfkScreen(client.screen));
            }
        });
    }
}