package me.cioco.antiafk;

import me.cioco.antiafk.config.AntiAfkConfig;
import me.cioco.antiafk.gui.AntiAfkScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static final AntiAfkConfig config = new AntiAfkConfig();
    public static final KeyBinding.Category CATEGORY_ANTIAFK = KeyBinding.Category.create(Identifier.of("antiafk", "key_category"));
    public static KeyBinding toggleKeyBinding;
    public static KeyBinding guiKeyBinding;
    public static boolean toggled = false;

    @Override
    public void onInitialize() {

        config.loadConfiguration();

        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.antiafk.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_ANTIAFK
        ));

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.antiafk.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_ANTIAFK
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (toggleKeyBinding.wasPressed()) {
                toggled = !toggled;
                if (client.currentScreen instanceof AntiAfkScreen screen) {
                    screen.refreshGlobalToggle();
                }
                Text status = Text.literal("AntiAfk: ")
                        .append(Text.literal(toggled ? "Enabled" : "Disabled")
                                .formatted(toggled ? Formatting.GREEN : Formatting.RED));
                client.player.sendMessage(status, true);
            }
            if (guiKeyBinding.wasPressed()) {
                client.setScreen(new AntiAfkScreen(client.currentScreen));
            }
        });
    }
}