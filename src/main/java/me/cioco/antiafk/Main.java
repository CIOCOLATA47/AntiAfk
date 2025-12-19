package me.cioco.antiafk;

import me.cioco.antiafk.commands.*;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static final AntiAfkConfig config = new AntiAfkConfig();
    public static KeyBinding keyBinding;
    public static boolean toggled = false;

    public static final KeyBinding.Category CATEGORY_ANTIAFK = KeyBinding.Category.create(Identifier.of("antiafk", "key_category"));



    @Override
    public void onInitialize() {

        config.loadConfiguration();


        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.antiafk.toggle",
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_ANTIAFK
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
            MovementCommand.register(dispatcher);
        });
    }
}