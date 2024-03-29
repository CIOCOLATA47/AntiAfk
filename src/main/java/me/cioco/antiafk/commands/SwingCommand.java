package me.cioco.antiafk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SwingCommand {
    public static boolean shouldSwing = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("antiafk")
                .then(ClientCommandManager.literal("swing").executes(SwingCommand::toggleSwing)));
    }

    private static int toggleSwing(CommandContext<FabricClientCommandSource> context) {
        shouldSwing = !shouldSwing;
        String statusMessage = shouldSwing ? "Swinging Enabled" : "Swinging Disabled";
        Formatting statusColor = shouldSwing ? Formatting.GREEN : Formatting.RED;
        context.getSource().sendFeedback(Text.literal("AntiAfk: " + statusMessage).formatted(statusColor));
        return 1;
    }
}