package me.cioco.antiafk.mixin;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity {

    @Unique private static final Random RANDOM = new Random();

    @Unique private int timerTicks = 0;
    @Unique private int currentTargetTicks = 20;
    @Unique private boolean wasActive = false;

    @Unique private float targetYaw;
    @Unique private float targetPitch;
    @Unique private float visualYawVelocity = 0f;
    @Unique private float anchorYaw;
    @Unique private float anchorPitch;
    @Unique private boolean isAnchored = false;

    @Unique private int pauseTicksRemaining = 0;
    @Unique private int activeMovementTicks = 0;

    @Unique private boolean isEating = false;
    @Unique private int lastSlot = -1;
    @Unique private int eatTicksRemaining = 0;

    @Unique private int nextHotbarSwitchTick = 0;

    @Unique private int nextOffhandSwapTick = 0;
    @Unique private boolean offhandSwapped = false;
    @Unique private int offhandSwapBackTick = 0;

    @Unique private int nextInventoryOpenTick = 0;
    @Unique private int inventoryOpenTicksRemaining = 0;

    @Unique private int nextJitterTick = 0;
    @Unique private float jitterYaw = 0f;
    @Unique private float jitterPitch = 0f;

    @Unique private int nextChatMessageTick = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (!Main.toggled) {
            if (wasActive) {
                forceStopAll(mc);
                stopEating(mc);
                wasActive = false;
            }
            return;
        }

        if (AntiAfkConfig.randomPauseEnabled) {
            if (pauseTicksRemaining > 0) {
                pauseTicksRemaining--;
                forceStopAll(mc);
                return;
            }
            if (RANDOM.nextFloat() < 0.005f) {
                pauseTicksRemaining = 20 + RANDOM.nextInt(40);
                forceStopAll(mc);
                return;
            }
        }

        wasActive = true;
        activeMovementTicks++;

        if (AntiAfkConfig.autoDisconnectEnabled && activeMovementTicks % 10 == 0) {
            handleAutoDisconnect(mc, player);
        }

        if (AntiAfkConfig.autoEatEnabled) {
            handleAutoEat(mc, player);
        } else if (isEating) {
            stopEating(mc);
        }

        handleSmoothMovement(mc, player);

        if (++timerTicks >= currentTargetTicks) {
            timerTicks = 0;
            executeTimedActions(mc, player);
            calculateNextInterval();
        }

        handleUltraSmoothSpin(player);
        handleMouseMovement(player);
        handleMouseJitter(player);

        if (AntiAfkConfig.randomHotbarEnabled) {
            handleRandomHotbarSwitch(mc, player);
        }

        if (AntiAfkConfig.offhandSwapEnabled) {
            handleRandomOffhandSwap(mc);
        } else if (offhandSwapped) {
            doSwap(mc);
            offhandSwapped = false;
        }

        if (AntiAfkConfig.randomInventoryEnabled) {
            handleRandomInventoryOpen(mc);
        } else if (inventoryOpenTicksRemaining > 0) {
            inventoryOpenTicksRemaining = 0;
            mc.setScreen(null);
        }

        handleChatMessages(mc);
    }

    @Unique
    private void handleAutoDisconnect(Minecraft mc, LocalPlayer self) {
        if (mc.level == null || mc.player == null) return;

        String ignoredRaw = AntiAfkConfig.autoDisconnectIgnoredPlayers;
        Set<String> ignored = (ignoredRaw != null && !ignoredRaw.isBlank())
                ? Arrays.stream(ignoredRaw.split(";"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet())
                : Set.of();

        double radiusSq = AntiAfkConfig.autoDisconnectRadius * AntiAfkConfig.autoDisconnectRadius;

        boolean threat = mc.level.players().stream()
                .filter(p -> p != self)
                .filter(p -> !ignored.contains(p.getName().getString().toLowerCase()))
                .anyMatch(p -> p.distanceToSqr(self) <= radiusSq);

        if (threat) {
            mc.execute(() -> {
                if (mc.getConnection() != null) {
                    mc.getConnection().getConnection().disconnect(
                            Component.literal("[AntiAFK] Player nearby — disconnected")
                    );
                }
            });
        }
    }

    @Unique
    private void handleAutoEat(Minecraft mc, LocalPlayer player) {
        if (mc.options == null) return;
        if (isEating) {
            eatTicksRemaining--;
            if (eatTicksRemaining <= 0) stopEating(mc);
            return;
        }
        int foodLevel = player.getFoodData().getFoodLevel();
        if (foodLevel <= (int) AntiAfkConfig.eatFoodLevel) {
            int foodSlot = findFoodSlot(player);
            if (foodSlot != -1) startEating(mc, foodSlot);
        }
    }

    @Unique
    private int findFoodSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private void startEating(Minecraft mc, int slot) {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;
        lastSlot = mc.player.getInventory().getSelectedSlot();
        ((InventoryAccessor) mc.player.getInventory()).setSelected(slot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.options.keyUse.setDown(true);
        isEating = true;
        eatTicksRemaining = 40;
    }

    @Unique
    private void stopEating(Minecraft mc) {
        if (mc.options != null) mc.options.keyUse.setDown(false);
        if (mc.player != null && mc.getConnection() != null && lastSlot != -1) {
            ((InventoryAccessor) mc.player.getInventory()).setSelected(lastSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(lastSlot));
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN
            ));
        }
        isEating = false;
        lastSlot = -1;
    }

    @Unique
    private void handleRandomHotbarSwitch(Minecraft mc, LocalPlayer player) {
        if (mc.screen != null || isEating) return;
        if (activeMovementTicks >= nextHotbarSwitchTick) {
            int current = player.getInventory().getSelectedSlot();
            int newSlot;
            do { newSlot = RANDOM.nextInt(9); } while (newSlot == current);
            ((InventoryAccessor) mc.player.getInventory()).setSelected(newSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(newSlot));
            int minTicks = (int)(AntiAfkConfig.hotbarSwitchMinSeconds * 20f);
            int maxTicks = (int)(AntiAfkConfig.hotbarSwitchMaxSeconds * 20f);
            nextHotbarSwitchTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
        }
    }

    @Unique
    private void handleRandomOffhandSwap(Minecraft mc) {
        if (mc.screen != null || isEating) return;
        if (offhandSwapped && activeMovementTicks >= offhandSwapBackTick) {
            doSwap(mc);
            offhandSwapped = false;
            int minTicks = (int)(AntiAfkConfig.offhandSwapMinSeconds * 20f);
            int maxTicks = (int)(AntiAfkConfig.offhandSwapMaxSeconds * 20f);
            nextOffhandSwapTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
            return;
        }
        if (!offhandSwapped && activeMovementTicks >= nextOffhandSwapTick) {
            doSwap(mc);
            offhandSwapped = true;
            int holdTicks = (int)(AntiAfkConfig.offhandHoldSeconds * 20f);
            offhandSwapBackTick = activeMovementTicks + Math.max(1, holdTicks);
        }
    }

    @Unique
    private void doSwap(Minecraft mc) {
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
        ));
        ItemStack main = mc.player.getMainHandItem().copy();
        ItemStack off  = mc.player.getOffhandItem().copy();
        mc.player.getInventory().setItem(mc.player.getInventory().getSelectedSlot(), off);
        mc.player.getInventory().setItem(40, main);
    }

    @Unique
    private void handleRandomInventoryOpen(Minecraft mc) {
        if (isEating) return;
        if (inventoryOpenTicksRemaining > 0) {
            inventoryOpenTicksRemaining--;
            if (inventoryOpenTicksRemaining == 0) mc.setScreen(null);
            return;
        }
        if (mc.screen != null) return;
        if (activeMovementTicks >= nextInventoryOpenTick) {
            mc.setScreen(new InventoryScreen(mc.player));
            int holdTicks = (int)(AntiAfkConfig.inventoryHoldSeconds * 20f);
            inventoryOpenTicksRemaining = Math.max(1, holdTicks);
            int minTicks = (int)(AntiAfkConfig.inventoryOpenMinSeconds * 20f);
            int maxTicks = (int)(AntiAfkConfig.inventoryOpenMaxSeconds * 20f);
            nextInventoryOpenTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
        }
    }

    @Unique
    private void forceStopAll(Minecraft mc) {
        if (mc.options == null) return;
        setKeyState(mc.options.keyUp,    false);
        setKeyState(mc.options.keyDown,  false);
        setKeyState(mc.options.keyLeft,  false);
        setKeyState(mc.options.keyRight, false);
        if (AntiAfkConfig.sneak) setKeyState(mc.options.keyShift, false);
        visualYawVelocity = 0;
    }

    @Unique
    private void handleUltraSmoothSpin(LocalPlayer player) {
        if (!AntiAfkConfig.autoSpinEnabled) { visualYawVelocity = 0; return; }
        visualYawVelocity = Mth.lerp(0.02f, visualYawVelocity, AntiAfkConfig.spinSpeed);
        player.setYRot(player.getYRot() + visualYawVelocity);
        float verticalWave = (float) Math.sin(activeMovementTicks * 0.03f) * 20.0f;
        player.setXRot(Mth.lerp(0.05f, player.getXRot(), verticalWave));
    }

    @Unique
    private void handleSmoothMovement(Minecraft mc, LocalPlayer player) {
        if (mc.options == null || !AntiAfkConfig.movementEnabled) return;
        if (mc.screen != null) { forceStopAll(mc); return; }
        int walkTicks  = 40;
        int pauseTicks = 10;
        int phaseTotal = walkTicks + pauseTicks;
        int tickInCycle  = activeMovementTicks % (phaseTotal * 4);
        int currentPhase = tickInCycle / phaseTotal;
        boolean isWalking = (tickInCycle % phaseTotal) < walkTicks;
        setKeyState(mc.options.keyUp,    isWalking && currentPhase == 0);
        setKeyState(mc.options.keyRight, isWalking && currentPhase == 1);
        setKeyState(mc.options.keyDown,  isWalking && currentPhase == 2);
        setKeyState(mc.options.keyLeft,  isWalking && currentPhase == 3);
    }

    @Unique
    private void executeTimedActions(Minecraft mc, LocalPlayer player) {
        if (AntiAfkConfig.autoJumpEnabled && player.onGround()) player.jumpFromGround();
        if (AntiAfkConfig.shouldSwing) player.swing(InteractionHand.MAIN_HAND);
        if (AntiAfkConfig.sneak && mc.options != null) {
            setKeyState(mc.options.keyShift, !mc.options.keyShift.isDown());
        }
    }

    @Unique
    private void handleMouseMovement(LocalPlayer player) {
        if (!AntiAfkConfig.mouseMovement) { isAnchored = false; return; }
        if (!isAnchored) {
            anchorYaw   = player.getYRot();
            anchorPitch = player.getXRot();
            targetYaw   = anchorYaw;
            targetPitch = anchorPitch;
            isAnchored  = true;
        }
        if (activeMovementTicks % 50 == 0) {
            targetYaw   = anchorYaw + (RANDOM.nextFloat() - 0.5f) * 60f * AntiAfkConfig.horizontalMultiplier;
            targetPitch = Mth.clamp(anchorPitch + (RANDOM.nextFloat() - 0.5f) * 30f * AntiAfkConfig.verticalMultiplier, -90f, 90f);
        }
        player.setYRot(Mth.rotLerp(0.03f, player.getYRot(), targetYaw));
        player.setXRot(Mth.lerp(0.03f, player.getXRot(), targetPitch));
    }

    @Unique
    private void handleMouseJitter(LocalPlayer player) {
        if (!AntiAfkConfig.mouseJitterEnabled) return;
        if (activeMovementTicks >= nextJitterTick) {
            float range = AntiAfkConfig.jitterStrength;
            jitterYaw   = (RANDOM.nextFloat() - 0.5f) * range;
            jitterPitch = (RANDOM.nextFloat() - 0.5f) * range * 0.5f;
            nextJitterTick = activeMovementTicks + 2 + RANDOM.nextInt(4);
        }
        player.setYRot(player.getYRot() + jitterYaw);
        player.setXRot(Mth.clamp(player.getXRot() + jitterPitch, -90f, 90f));
    }

    @Unique
    private void handleChatMessages(Minecraft mc) {
        if (!AntiAfkConfig.chatMessagesEnabled) return;
        if (mc.player == null || mc.getConnection() == null) return;
        if (activeMovementTicks >= nextChatMessageTick) {
            String raw = AntiAfkConfig.chatMessages;
            if (raw != null && !raw.isBlank()) {
                String[] messages = raw.split(";");
                if (messages.length > 0) {
                    String msg = messages[RANDOM.nextInt(messages.length)].trim();
                    if (!msg.isEmpty()) {
                        mc.getConnection().sendChat(msg);
                    }
                }
            }
            int minT = (int)(AntiAfkConfig.chatMessageMinSeconds * 20f);
            int maxT = (int)(AntiAfkConfig.chatMessageMaxSeconds * 20f);
            nextChatMessageTick = activeMovementTicks + minT + RANDOM.nextInt(Math.max(1, maxT - minT));
        }
    }

    @Unique
    private void calculateNextInterval() {
        float seconds = AntiAfkConfig.useRandomInterval
                ? AntiAfkConfig.minInterval + RANDOM.nextFloat() * (AntiAfkConfig.maxInterval - AntiAfkConfig.minInterval)
                : AntiAfkConfig.interval;
        currentTargetTicks = Math.max(2, (int)(seconds * 20f));
    }

    @Unique
    private void setKeyState(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }
}