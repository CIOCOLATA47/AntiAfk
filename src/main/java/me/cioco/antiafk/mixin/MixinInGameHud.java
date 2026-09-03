package me.cioco.antiafk.mixin;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class MixinInGameHud {

    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void renderAfkTimer(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {

        if (!Main.toggled || !AntiAfkConfig.showAfkTimer) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        String time = Main.formatTime(Main.afkElapsedSeconds);
        String display = "§7[AFK] §a" + time;

        if (AntiAfkConfig.autoLogoutEnabled) {
            float maxSeconds = AntiAfkConfig.autoLogoutMinutes * 60.0F;

            if (maxSeconds > 0.0F && Main.afkElapsedSeconds > 0.0F) {
                float percentage = Math.min(
                        100.0F,
                        (Main.afkElapsedSeconds / maxSeconds) * 100.0F
                );
                String color;
                if (percentage < 50.0F) {
                    color = "§a";
                } else if (percentage < 80.0F) {
                    color = "§e";
                } else {
                    color = "§c";
                }
                display += " §7| " + color + String.format("%.0f%%", percentage);
                float remaining = (maxSeconds - Main.afkElapsedSeconds) / 60.0F;
                if (remaining > 0.0F && remaining < 10.0F) {
                    display += " §7(§c" + String.format("%.1f", remaining) + "m§7)";
                }
            }
        }

        graphics.text(client.font, display, 5, 5, 0xFFFFFFFF, true
        );
    }
}