package me.cioco.antiafk.mixin;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Hand;
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
    @Unique private boolean hasTarget = false;
    @Unique private float visualYawVelocity = 0f;
    @Unique private float anchorYaw;
    @Unique private float anchorPitch;
    @Unique private boolean isAnchored = false;

    @Unique private int pauseTicksRemaining = 0;
    @Unique private int activeMovementTicks = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (!Main.toggled) {
            if (wasActive) {
                forceStopAll(mc);
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
                pauseTicksRemaining = 20;
                forceStopAll(mc);
                return;
            }
        }

        wasActive = true;
        activeMovementTicks++;

        handleSmoothMovement(mc, player);

        if (++timerTicks >= currentTargetTicks) {
            timerTicks = 0;
            executeTimedActions(mc, player);
            calculateNextInterval();
        }

        handleUltraSmoothSpin(player);
        handleMouseMovement(player);
    }

    @Unique
    private void forceStopAll(MinecraftClient mc) {
        if (mc.options == null) return;

        setKeyState(mc.options.forwardKey, false);
        setKeyState(mc.options.backKey, false);
        setKeyState(mc.options.leftKey, false);
        setKeyState(mc.options.rightKey, false);

        if (AntiAfkConfig.sneak) {
            setKeyState(mc.options.sneakKey, false);
        }

        visualYawVelocity = 0;
    }

    @Unique
    private void handleUltraSmoothSpin(ClientPlayerEntity player) {
        if (!AntiAfkConfig.autoSpinEnabled) {
            visualYawVelocity = 0;
            return;
        }

        visualYawVelocity = MathHelper.lerp(0.02f, visualYawVelocity, AntiAfkConfig.spinSpeed);
        player.setYaw(player.getYaw() + visualYawVelocity);

        float verticalWave = (float) Math.sin(activeMovementTicks * 0.03f) * 20.0f;
        player.setPitch(MathHelper.lerp(0.05f, player.getPitch(), verticalWave));
    }

    @Unique
    private void handleSmoothMovement(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.options == null || !AntiAfkConfig.movementEnabled) return;

        if (mc.currentScreen != null) {
            forceStopAll(mc);
            return;
        }

        int walkTicks = 40;
        int pauseTicks = 10;
        int phaseTotal = walkTicks + pauseTicks;

        int tickInCycle = activeMovementTicks % (phaseTotal * 4);
        int currentPhase = tickInCycle / phaseTotal;
        boolean isWalking = (tickInCycle % phaseTotal) < walkTicks;

        setKeyState(mc.options.forwardKey, isWalking && currentPhase == 0);
        setKeyState(mc.options.rightKey,   isWalking && currentPhase == 1);
        setKeyState(mc.options.backKey,    isWalking && currentPhase == 2);
        setKeyState(mc.options.leftKey,    isWalking && currentPhase == 3);
    }

    @Unique
    private void executeTimedActions(MinecraftClient mc, ClientPlayerEntity player) {
        if (AntiAfkConfig.autoJumpEnabled && player.isOnGround()) player.jump();
        if (AntiAfkConfig.shouldSwing) player.swingHand(Hand.MAIN_HAND);

        if (AntiAfkConfig.sneak && mc.options != null) {
            boolean currentState = mc.options.sneakKey.isPressed();
            setKeyState(mc.options.sneakKey, !currentState);
        }
    }

    @Unique
    private void handleMouseMovement(ClientPlayerEntity player) {
        if (!AntiAfkConfig.mouseMovement) {
            isAnchored = false;
            return;
        }

        if (!isAnchored) {
            anchorYaw = player.getYaw();
            anchorPitch = player.getPitch();
            targetYaw = anchorYaw;
            targetPitch = anchorPitch;
            isAnchored = true;
        }

        if (activeMovementTicks % 50 == 0) {
            float yawOffset = (RANDOM.nextFloat() - 0.5f) * 60f * AntiAfkConfig.horizontalMultiplier;
            float pitchOffset = (RANDOM.nextFloat() - 0.5f) * 30f * AntiAfkConfig.verticalMultiplier;

            targetYaw = anchorYaw + yawOffset;
            targetPitch = MathHelper.clamp(anchorPitch + pitchOffset, -90f, 90f);
        }

        player.setYaw(MathHelper.lerpAngleDegrees(0.03f, player.getYaw(), targetYaw));
        player.setPitch(MathHelper.lerp(0.03f, player.getPitch(), targetPitch));
    }

    @Unique
    private void calculateNextInterval() {
        float seconds = AntiAfkConfig.useRandomInterval ?
                AntiAfkConfig.minInterval + RANDOM.nextFloat() * (AntiAfkConfig.maxInterval - AntiAfkConfig.minInterval) :
                AntiAfkConfig.interval;
        currentTargetTicks = Math.max(2, (int)(seconds * 20f));
    }

    @Unique
    private void setKeyState(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
    }
}