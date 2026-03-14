package me.cioco.antiafk.mixin;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ClientPlayerEntity.class)
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

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

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
            mc.openScreen(null);
        }
    }

    @Unique
    private void handleAutoEat(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.options == null) return;

        if (isEating) {
            eatTicksRemaining--;
            if (eatTicksRemaining <= 0) stopEating(mc);
            return;
        }

        int foodLevel = player.getHungerManager().getFoodLevel();
        if (foodLevel <= (int) AntiAfkConfig.eatFoodLevel) {
            int foodSlot = findFoodSlot(player);
            if (foodSlot != -1) startEating(mc, foodSlot);
        }
    }

    @Unique
    private int findFoodSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem().isFood()) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private void startEating(MinecraftClient mc, int slot) {
        lastSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;
        mc.options.useKey.setPressed(true);
        isEating = true;
        eatTicksRemaining = 40;
    }

    @Unique
    private void stopEating(MinecraftClient mc) {
        if (mc.options != null) mc.options.useKey.setPressed(false);
        if (mc.player != null && lastSlot != -1) mc.player.inventory.selectedSlot = lastSlot;
        isEating = false;
        lastSlot = -1;
        eatTicksRemaining = 0;
    }

    @Unique
    private void handleRandomHotbarSwitch(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.currentScreen != null || isEating) return;
        if (activeMovementTicks >= nextHotbarSwitchTick) {
            int current = player.inventory.selectedSlot;
            int newSlot;
            do { newSlot = RANDOM.nextInt(9); } while (newSlot == current);
            player.inventory.selectedSlot = newSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(newSlot));
            int minTicks = (int) (AntiAfkConfig.hotbarSwitchMinSeconds * 20f);
            int maxTicks = (int) (AntiAfkConfig.hotbarSwitchMaxSeconds * 20f);
            nextHotbarSwitchTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
        }
    }

    @Unique
    private void handleRandomOffhandSwap(MinecraftClient mc) {
        if (mc.currentScreen != null || isEating) return;

        if (offhandSwapped && activeMovementTicks >= offhandSwapBackTick) {
            doSwap(mc);
            offhandSwapped = false;
            int minTicks = (int) (AntiAfkConfig.offhandSwapMinSeconds * 20f);
            int maxTicks = (int) (AntiAfkConfig.offhandSwapMaxSeconds * 20f);
            nextOffhandSwapTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
            return;
        }

        if (!offhandSwapped && activeMovementTicks >= nextOffhandSwapTick) {
            doSwap(mc);
            offhandSwapped = true;
            int holdTicks = (int) (AntiAfkConfig.offhandHoldSeconds * 20f);
            offhandSwapBackTick = activeMovementTicks + Math.max(1, holdTicks);
        }
    }

    @Unique
    private void doSwap(MinecraftClient mc) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
        ItemStack main = mc.player.getMainHandStack().copy();
        ItemStack off  = mc.player.getOffHandStack().copy();
        mc.player.inventory.setStack(mc.player.inventory.selectedSlot, off);
        mc.player.inventory.setStack(40, main);
    }

    @Unique
    private void handleRandomInventoryOpen(MinecraftClient mc) {
        if (isEating) return;

        if (inventoryOpenTicksRemaining > 0) {
            inventoryOpenTicksRemaining--;
            if (inventoryOpenTicksRemaining == 0) {
                mc.openScreen(null);
            }
            return;
        }

        if (mc.currentScreen != null) return;

        if (activeMovementTicks >= nextInventoryOpenTick) {
            mc.openScreen(new InventoryScreen(mc.player));
            int holdTicks = (int) (AntiAfkConfig.inventoryHoldSeconds * 20f);
            inventoryOpenTicksRemaining = Math.max(1, holdTicks);
            int minTicks = (int) (AntiAfkConfig.inventoryOpenMinSeconds * 20f);
            int maxTicks = (int) (AntiAfkConfig.inventoryOpenMaxSeconds * 20f);
            nextInventoryOpenTick = activeMovementTicks + minTicks + RANDOM.nextInt(Math.max(1, maxTicks - minTicks));
        }
    }

    @Unique
    private void forceStopAll(MinecraftClient mc) {
        if (mc.options == null) return;
        setKeyState(mc.options.keyForward, false);
        setKeyState(mc.options.keyBack,    false);
        setKeyState(mc.options.keyLeft,    false);
        setKeyState(mc.options.keyRight,   false);
        if (AntiAfkConfig.sneak) setKeyState(mc.options.keySneak, false);
        visualYawVelocity = 0;
    }

    @Unique
    private void handleUltraSmoothSpin(ClientPlayerEntity player) {
        if (!AntiAfkConfig.autoSpinEnabled) {
            visualYawVelocity = 0;
            return;
        }
        visualYawVelocity = MathHelper.lerp(0.02f, visualYawVelocity, AntiAfkConfig.spinSpeed);
        player.yaw = player.yaw + visualYawVelocity;
        float verticalWave = (float) Math.sin(activeMovementTicks * 0.03f) * 20.0f;
        player.pitch = MathHelper.lerp(0.05f, player.pitch, verticalWave);
    }

    @Unique
    private void handleSmoothMovement(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.options == null || !AntiAfkConfig.movementEnabled) return;
        if (mc.currentScreen != null) { forceStopAll(mc); return; }

        int walkTicks  = 40;
        int pauseTicks = 10;
        int phaseTotal = walkTicks + pauseTicks;
        int tickInCycle   = activeMovementTicks % (phaseTotal * 4);
        int currentPhase  = tickInCycle / phaseTotal;
        boolean isWalking = (tickInCycle % phaseTotal) < walkTicks;

        setKeyState(mc.options.keyForward, isWalking && currentPhase == 0);
        setKeyState(mc.options.keyRight,   isWalking && currentPhase == 1);
        setKeyState(mc.options.keyBack,    isWalking && currentPhase == 2);
        setKeyState(mc.options.keyLeft,    isWalking && currentPhase == 3);
    }

    @Unique
    private void executeTimedActions(MinecraftClient mc, ClientPlayerEntity player) {
        if (AntiAfkConfig.autoJumpEnabled && player.isOnGround()) player.jump();
        if (AntiAfkConfig.shouldSwing) player.swingHand(Hand.MAIN_HAND);
        if (AntiAfkConfig.sneak && mc.options != null) {
            setKeyState(mc.options.keySneak, !mc.options.keySneak.isPressed());
        }
    }

    @Unique
    private void handleMouseMovement(ClientPlayerEntity player) {
        if (!AntiAfkConfig.mouseMovement) { isAnchored = false; return; }

        if (!isAnchored) {
            anchorYaw   = player.yaw;
            anchorPitch = player.pitch;
            targetYaw   = anchorYaw;
            targetPitch = anchorPitch;
            isAnchored  = true;
        }
        if (activeMovementTicks % 50 == 0) {
            targetYaw   = anchorYaw + (RANDOM.nextFloat() - 0.5f) * 60f * AntiAfkConfig.horizontalMultiplier;
            targetPitch = MathHelper.clamp(anchorPitch + (RANDOM.nextFloat() - 0.5f) * 30f * AntiAfkConfig.verticalMultiplier, -90f, 90f);
        }
        player.yaw = lerpAngleDegrees(0.03f, player.yaw, targetYaw);
        player.pitch = MathHelper.lerp(0.03f, player.pitch, targetPitch);
    }

    @Unique
    private float lerpAngleDegrees(float delta, float start, float end) {
        float diff = MathHelper.wrapDegrees(end - start);
        return start + delta * diff;
    }

    @Unique
    private void calculateNextInterval() {
        float seconds = AntiAfkConfig.useRandomInterval
                ? AntiAfkConfig.minInterval + RANDOM.nextFloat() * (AntiAfkConfig.maxInterval - AntiAfkConfig.minInterval)
                : AntiAfkConfig.interval;
        currentTargetTicks = Math.max(2, (int) (seconds * 20f));
    }

    @Unique
    private void setKeyState(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
    }
}
