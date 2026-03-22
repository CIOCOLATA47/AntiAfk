package me.cioco.antiafk.mixin;

import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends Screen {

    @Shadow @Final private DirectionalLayoutWidget grid;

    @Unique private ButtonWidget reconnectBtn;
    @Unique private Thread reconnectThread;
    @Unique private ServerInfo cachedServerInfo;

    protected MixinDisconnectedScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void captureServerInfo(CallbackInfo ci) {
        ServerInfo current = MinecraftClient.getInstance().getCurrentServerEntry();
        if (current != null) {
            this.cachedServerInfo = current;
        }
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void addReconnectButton(CallbackInfo ci) {
        if (!AntiAfkConfig.autoReconnectEnabled || cachedServerInfo == null) return;

        reconnectBtn = ButtonWidget.builder(
                Text.literal("Reconnect Now"),
                btn -> tryReconnect()
        ).build();

        grid.add(reconnectBtn);
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
                    MinecraftClient.getInstance().execute(() -> {
                        if (reconnectBtn != null) {
                            reconnectBtn.setMessage(Text.literal("Reconnecting in " + timeLeft + "s..."));
                        }
                    });

                    Thread.sleep(1000L);
                }

                MinecraftClient.getInstance().execute(this::tryReconnect);

            } catch (InterruptedException ignored) {
            }
        }, "AntiAFK-Reconnect-Thread");

        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    @Unique
    private void tryReconnect() {
        if (reconnectThread != null) reconnectThread.interrupt();
        if (cachedServerInfo == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ServerAddress address = ServerAddress.parse(cachedServerInfo.address);

        ConnectScreen.connect(new TitleScreen(), mc, address, cachedServerInfo, false, null);
    }

    @Override
    public void removed() {
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }
        super.removed();
    }
}