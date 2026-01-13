package me.cioco.antiafk.gui;

import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class AntiAfkScreen extends Screen {
    private final Screen parent;
    private final AntiAfkConfig config = new AntiAfkConfig();

    private int actionsY, movementY, sliderY;

    public AntiAfkScreen(Screen parent) {
        super(Text.literal("Anti-AFK Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int spacing = 24;

        this.actionsY = 45;
        this.movementY = actionsY + (spacing * 3) + 10;
        this.sliderY = movementY + (spacing * 3) + 10;

        this.addDrawableChild(createToggleButton(centerX - 155, actionsY, "Auto Jump", "Jumps randomly.",
                AntiAfkConfig.autoJumpEnabled, val -> AntiAfkConfig.autoJumpEnabled = val));

        this.addDrawableChild(createToggleButton(centerX + 5, actionsY, "Sneak Mode", "Automatically sneaks",
                AntiAfkConfig.sneak, val -> AntiAfkConfig.sneak = val));

        this.addDrawableChild(createToggleButton(centerX - 155, actionsY + spacing, "Swing Hand", "Swings player's hand",
                AntiAfkConfig.shouldSwing, val -> AntiAfkConfig.shouldSwing = val));

        this.addDrawableChild(createToggleButton(centerX + 5, actionsY + spacing, "Random Pause", "Randomly pauses",
                AntiAfkConfig.randomPauseEnabled, val -> AntiAfkConfig.randomPauseEnabled = val));

        this.addDrawableChild(createToggleButton(centerX - 155, movementY, "Player Movement", "Moves the player",
                AntiAfkConfig.movementEnabled, val -> AntiAfkConfig.movementEnabled = val));

        this.addDrawableChild(createToggleButton(centerX + 5, movementY, "MouseMovement", "Randomly moves the palyer's camera.",
                AntiAfkConfig.mouseMovement, val -> AntiAfkConfig.mouseMovement = val));

        this.addDrawableChild(createToggleButton(centerX - 155, movementY + spacing, "Spin", "Rotates the player's camera.",
                AntiAfkConfig.autoSpinEnabled, val -> AntiAfkConfig.autoSpinEnabled = val));

        this.addDrawableChild(createToggleButton(centerX + 5, movementY + spacing, "Random interval", "Varies time between actions.",
                AntiAfkConfig.useRandomInterval, val -> {
                    AntiAfkConfig.useRandomInterval = val;
                    refresh();
                }));

        if (AntiAfkConfig.useRandomInterval) {
            this.addDrawableChild(new GenericSlider(centerX - 155, sliderY, 150, 20, "Min Secs",
                    AntiAfkConfig.minInterval, 0.1f, 10.0f, val -> AntiAfkConfig.minInterval = val));

            this.addDrawableChild(new GenericSlider(centerX + 5, sliderY, 150, 20, "Max Secs",
                    AntiAfkConfig.maxInterval, 0.1f, 10.0f, val -> AntiAfkConfig.maxInterval = val));
        } else {
            this.addDrawableChild(new GenericSlider(centerX - 155, sliderY, 310, 20, "Action Delay",
                    AntiAfkConfig.interval, 0.1f, 10.0f, val -> AntiAfkConfig.interval = val));
        }

        this.addDrawableChild(new GenericSlider(centerX - 155, sliderY + spacing, 150, 20, "Spin Speed",
                AntiAfkConfig.spinSpeed, 0.1f, 20.0f, val -> AntiAfkConfig.spinSpeed = val));

        this.addDrawableChild(new GenericSlider(centerX + 5, sliderY + spacing, 150, 20, "Look Range",
                AntiAfkConfig.horizontalMultiplier, 0.1f, 5.0f, val -> {
            AntiAfkConfig.horizontalMultiplier = val;
            AntiAfkConfig.verticalMultiplier = val;
        }));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§6§lDONE"), (button) -> {
            config.saveConfiguration();
            this.client.setScreen(this.parent);
        }).dimensions(centerX - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);

        int centerX = this.width / 2;
        int panelWidth = 330;

        int panelHeight = 75;

        drawStyledPanel(context, centerX - 165, actionsY - 15, panelWidth, panelHeight);
        drawStyledPanel(context, centerX - 165, movementY - 15, panelWidth, panelHeight);

        drawStyledPanel(context, centerX - 165, sliderY - 15, panelWidth, panelHeight + 5);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l§nANTI-AFK SETTINGS", centerX, 15, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, "§b§l> §fPlayer Actions", centerX - 158, actionsY - 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§b§l> §fMovement & Behavior", centerX - 158, movementY - 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§b§l> §fAdvanced Timing", centerX - 158, sliderY - 10, 0xFFFFFFFF);
    }

    private void drawStyledPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x55000000);
        context.fill(x, y, x + 2, y + height, 0xFFFFAA00);
    }

    @Override
    public void close() {
        config.saveConfiguration();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private void refresh() {
        this.clearChildren();
        this.init();
    }

    private ButtonWidget createToggleButton(int x, int y, String label, String desc, boolean initVal, Consumer<Boolean> action) {
        return ButtonWidget.builder(getToggleText(label, initVal), (button) -> {
            boolean newVal = !button.getMessage().getString().contains("ON");
            action.accept(newVal);
            button.setMessage(getToggleText(label, newVal));
            if (label.equals("Random Wait")) refresh();
        }).dimensions(x, y, 150, 20).tooltip(Tooltip.of(Text.literal("§e" + desc))).build();
    }

    private Text getToggleText(String label, boolean value) {
        return Text.literal(label + ": ").append(value ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
    }

    private static class GenericSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final Consumer<Float> updateAction;

        public GenericSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            super(x, y, w, h, Text.empty(), (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override protected void updateMessage() {
            float val = min + (float)(this.value * (max - min));
            this.setMessage(Text.literal(label + ": §e" + String.format("%.1f", val)));
        }

        @Override protected void applyValue() {
            float val = min + (float)(this.value * (max - min));
            updateAction.accept(val);
        }
    }
}