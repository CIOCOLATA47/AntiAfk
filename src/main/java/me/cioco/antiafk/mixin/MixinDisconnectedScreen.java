package me.cioco.antiafk.mixin;

import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends Screen {

    @Shadow @Final private LinearLayout layout;

    @Unique private Button reconnectBtn;
    @Unique private Thread reconnectThread;
    @Unique private ServerData cachedServerData;

    protected MixinDisconnectedScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void captureServerInfo(CallbackInfo ci) {
        ServerData current = Minecraft.getInstance().getCurrentServer();
        if (current != null) {
            this.cachedServerData = current;
        }
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void addReconnectButton(CallbackInfo ci) {
        if (!AntiAfkConfig.autoReconnectEnabled || cachedServerData == null) return;

        reconnectBtn = Button.builder(
                Component.literal("Reconnect Now"),
                btn -> tryReconnect()
        ).build();

        layout.addChild(reconnectBtn);
        startReconnectThread();
    }

    @Unique
    private void startReconnectThread() {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            reconnectThread.interrupt();
        }

        reconnectThread = new Thread(() -> {
            try {
                int seconds = (int) AntiAfkConfig.reconnectDelaySeconds;

                for (int i = seconds; i > 0; i--) {
                    if (Thread.interrupted()) return;

                    final String timeLeft = String.valueOf(i);
                    Minecraft.getInstance().execute(() -> {
                        if (reconnectBtn != null) {
                            reconnectBtn.setMessage(Component.literal("Reconnecting in " + timeLeft + "s..."));
                        }
                    });

                    Thread.sleep(1000L);
                }

                Minecraft.getInstance().execute(this::tryReconnect);

            } catch (InterruptedException ignored) {
            }
        }, "AntiAFK-Reconnect-Thread");

        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    @Unique
    private void tryReconnect() {
        if (reconnectThread != null) reconnectThread.interrupt();
        if (cachedServerData == null) return;

        Minecraft mc = Minecraft.getInstance();
        ServerAddress address = ServerAddress.parseString(cachedServerData.ip);

        ConnectScreen.startConnecting(new TitleScreen(), mc, address, cachedServerData, false, null);
    }

    @Override
    public void onClose() {
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }
        super.onClose();
    }
}