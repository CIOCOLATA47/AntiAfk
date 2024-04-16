package me.cioco.antiafk;

import me.cioco.antiafk.commands.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static KeyBinding keyBinding;
    public static boolean toggled = false;

    @Override
    public void onInitialize() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.antiafk.toggle",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.antiafk"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                if (client.world.getTime() == 1L) {
                    toggled = false;
                    client.player.sendMessage(Text.literal("AntiAfk: Disabled").formatted(Formatting.RED), false);
                }

                if (keyBinding.wasPressed()) {
                    toggled = !toggled;
                    String statusMessage = toggled ? "Enabled" : "Disabled";
                    Formatting statusColor = toggled ? Formatting.GREEN : Formatting.RED;
                    client.player.sendMessage(Text.literal("AntiAfk: " + statusMessage).formatted(statusColor), false);
                }
            }
        });
        addCommands();
    }

    private void addCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            IntervalCommand.register(dispatcher);
            JumpCommand.register(dispatcher);
            SneakCommand.register(dispatcher);
            SpinCommand.register(dispatcher);
            SwingCommand.register(dispatcher);
            MouseMovementCommand.register(dispatcher);
        });
    }
}