package me.cioco.antiafk.mixin;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.commands.SpinCommand;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(PlayerEntity.class)
public class MixinClientPlayerEntity {

    @Unique
    private static final Random RANDOM = new Random();

    @Unique
    private final AntiAfkConfig config = new AntiAfkConfig();

    @Unique
    private float pitchOffset = 0f;

    @Unique
    private boolean pitchIncreasing = true;

    @Unique
    private float targetYaw;

    @Unique
    private float targetPitch;

    @Unique
    private boolean hasTarget = false;

    @Unique
    private boolean wasMovementActive = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity player = mc.player;
        boolean utilTick = player.age % config.getInterval() == 0;

        handleAutoJump(player, utilTick);
        handleMouseMovement(player, utilTick);
        handleAutoSpin(player);
        handleSneak(mc, utilTick);
        handleMovement(mc, player);
        handleSwing(mc, utilTick);
    }

    @Unique
    private void handleAutoJump(PlayerEntity player, boolean utilTick) {
        if (Main.toggled && AntiAfkConfig.autoJumpEnabled && utilTick && player.isOnGround()) {
            player.jump();
        }
    }

    @Unique
    private void handleMouseMovement(PlayerEntity player, boolean utilTick) {
        if (!Main.toggled || !AntiAfkConfig.mouseMovement || !utilTick) {
            hasTarget = false;
            return;
        }

        if (!hasTarget) {
            targetYaw = player.getYaw();
            targetPitch = player.getPitch();
            hasTarget = true;
        }

        targetYaw += (RANDOM.nextFloat() * 2 - 1) * 50f * AntiAfkConfig.horizontalMultiplier;
        targetPitch = MathHelper.clamp(
                targetPitch + (RANDOM.nextFloat() * 2 - 1) * 24f * AntiAfkConfig.verticalMultiplier,
                -90.0f, 90.0f
        );

        float smoothing = 0.05f;
        float newYaw = lerp(player.getYaw(), targetYaw, smoothing);
        float newPitch = lerp(player.getPitch(), targetPitch, smoothing);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }

    @Unique
    private float lerp(float current, float target, float alpha) {
        return current + (target - current) * alpha;
    }

    @Unique
    private void handleAutoSpin(PlayerEntity player) {
        if (!(Main.toggled && AntiAfkConfig.autoSpinEnabled)) return;

        player.setYaw(player.getYaw() + SpinCommand.spinSpeed);

        float pitchStep = 0.2f;
        float maxPitchChange = 2.0f;
        if (pitchIncreasing) {
            pitchOffset += pitchStep;
            if (pitchOffset >= maxPitchChange) pitchIncreasing = false;
        } else {
            pitchOffset -= pitchStep;
            if (pitchOffset <= -maxPitchChange) pitchIncreasing = true;
        }

        float newPitch = MathHelper.clamp(player.getPitch() + pitchOffset, -90f, 90f);
        player.setPitch(newPitch);
    }

    @Unique
    private void handleSneak(MinecraftClient mc, boolean utilTick) {
        if (Main.toggled && AntiAfkConfig.sneak) {
            mc.options.sneakKey.setPressed(utilTick);
        }
    }

    @Unique
    private void handleMovement(MinecraftClient mc, PlayerEntity player) {
        if (Main.toggled && AntiAfkConfig.movementEnabled) {
            wasMovementActive = true;

            boolean toggleLR = (player.age / 5) % 2 == 0;
            boolean toggleFB = (player.age / 10) % 2 == 0;

            mc.options.leftKey.setPressed(toggleLR);
            mc.options.rightKey.setPressed(!toggleLR);
            mc.options.forwardKey.setPressed(toggleFB);
            mc.options.backKey.setPressed(!toggleFB);

        } else if (wasMovementActive) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            wasMovementActive = false;
        }
    }

    @Unique
    private void handleSwing(MinecraftClient mc, boolean utilTick) {
        if (Main.toggled && AntiAfkConfig.shouldSwing && mc.world != null && utilTick) {
            assert mc.player != null;
            mc.player.swingHand(mc.player.getActiveHand());
        }
    }
}
