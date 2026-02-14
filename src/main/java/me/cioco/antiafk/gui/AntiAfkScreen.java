package me.cioco.antiafk.gui;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AntiAfkScreen extends Screen {

    private static final int SPACING_Y = 24;
    private static final int SECTION_MARGIN = 35;
    private static final int TITLE_HEIGHT = 20;

    private final Screen parent;
    private final AntiAfkConfig config = new AntiAfkConfig();
    private final List<ClickableWidget> scrollableWidgets = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll;
    private int contentHeight;
    private ButtonWidget doneButton;
    private ButtonWidget globalToggleButton;

    public AntiAfkScreen(Screen parent) {
        super(Text.literal("Anti-AFK Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearChildren();
        this.scrollableWidgets.clear();

        int centerX = width / 2;
        int leftCol = centerX - 155;
        int rightCol = centerX + 5;
        int startY = 70;
        int currentY = startY;

        addToggleButton(leftCol, currentY, "Auto Jump", "Jumps randomly.", AntiAfkConfig.autoJumpEnabled, v -> AntiAfkConfig.autoJumpEnabled = v);
        addToggleButton(rightCol, currentY, "Sneak Mode", "Automatically sneaks", AntiAfkConfig.sneak, v -> AntiAfkConfig.sneak = v);
        currentY += SPACING_Y;
        addToggleButton(leftCol, currentY, "Swing Hand", "Swings player's hand", AntiAfkConfig.shouldSwing, v -> AntiAfkConfig.shouldSwing = v);
        addToggleButton(rightCol, currentY, "Random Pause", "Randomly pauses", AntiAfkConfig.randomPauseEnabled, v -> AntiAfkConfig.randomPauseEnabled = v);

        currentY += SPACING_Y + SECTION_MARGIN;

        addToggleButton(leftCol, currentY, "Player Movement", "Moves the player", AntiAfkConfig.movementEnabled, v -> AntiAfkConfig.movementEnabled = v);
        addToggleButton(rightCol, currentY, "Mouse Movement", "Randomly moves camera.", AntiAfkConfig.mouseMovement, v -> AntiAfkConfig.mouseMovement = v);
        currentY += SPACING_Y;
        addToggleButton(leftCol, currentY, "Spin", "Rotates the player's camera.", AntiAfkConfig.autoSpinEnabled, v -> AntiAfkConfig.autoSpinEnabled = v);
        addToggleButton(rightCol, currentY, "Random Interval", "Varies time between actions.", AntiAfkConfig.useRandomInterval, v -> {
            AntiAfkConfig.useRandomInterval = v;
            this.init();
        });

        currentY += SPACING_Y + SECTION_MARGIN;

        if (AntiAfkConfig.useRandomInterval) {
            addSlider(leftCol, currentY, 150, "Min Secs", AntiAfkConfig.minInterval, 0.1f, 10.0f, v -> AntiAfkConfig.minInterval = v);
            addSlider(rightCol, currentY, 150, "Max Secs", AntiAfkConfig.maxInterval, 0.1f, 10.0f, v -> AntiAfkConfig.maxInterval = v);
        } else {
            addSlider(leftCol, currentY, 310, "Action Delay", AntiAfkConfig.interval, 0.1f, 10.0f, v -> AntiAfkConfig.interval = v);
        }
        currentY += SPACING_Y;
        addSlider(leftCol, currentY, 150, "Spin Speed", AntiAfkConfig.spinSpeed, 0.1f, 20.0f, v -> AntiAfkConfig.spinSpeed = v);
        addSlider(rightCol, currentY, 150, "Look Range", AntiAfkConfig.horizontalMultiplier, 0.1f, 5.0f, v -> {
            AntiAfkConfig.horizontalMultiplier = v;
            AntiAfkConfig.verticalMultiplier = v;
        });

        contentHeight = currentY + 40;
        maxScroll = Math.max(0, contentHeight - (height - 90));

        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        globalToggleButton = ButtonWidget.builder(
                getGlobalToggleText(),
                b -> {
                    Main.toggled = !Main.toggled;
                    b.setMessage(getGlobalToggleText());
                }
        ).dimensions(centerX - 100, height - 60, 200, 20).build();
        addDrawableChild(globalToggleButton);

        doneButton = ButtonWidget.builder(
                Text.literal("SAVE & EXIT").formatted(Formatting.GOLD, Formatting.BOLD),
                b -> this.close()
        ).dimensions(centerX - 100, height - 30, 200, 20).build();
        addDrawableChild(doneButton);

        for (ClickableWidget widget : scrollableWidgets) {
            widget.setY(widget.getY() - scrollOffset);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderInGameBackground(ctx);

        int cx = width / 2;
        int panelW = 325;
        int panelX = cx - (panelW / 2);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Anti-AFK Settings").formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE), cx, 15, 0xFFFFFFFF);

        ctx.enableScissor(0, 40, width, height - 40);

        int currentY = 70 - scrollOffset;
        renderSectionGroup(ctx, panelX, currentY, panelW, 2, "Player Actions");
        currentY += (SPACING_Y * 2) + SECTION_MARGIN;

        renderSectionGroup(ctx, panelX, currentY, panelW, 2, "Movement & Behavior");
        currentY += (SPACING_Y * 2) + SECTION_MARGIN;

        renderSectionGroup(ctx, panelX, currentY, panelW, 2, "Advanced Timing");

        for (ClickableWidget widget : scrollableWidgets) {
            widget.visible = (widget.getY() + widget.getHeight() > 40 && widget.getY() < height - 40);
            if (widget.visible) {
                widget.render(ctx, mouseX, mouseY, delta);
            }
        }
        ctx.disableScissor();

        globalToggleButton.render(ctx, mouseX, mouseY, delta);
        doneButton.render(ctx, mouseX, mouseY, delta);

        drawScrollBar(ctx);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            int oldOffset = scrollOffset;
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - (verticalAmount * 25)));

            int diff = oldOffset - scrollOffset;
            for (ClickableWidget widget : scrollableWidgets) {
                widget.setY(widget.getY() + diff);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawScrollBar(DrawContext ctx) {
        if (maxScroll <= 0) return;

        int trackX = width - 6;
        int trackY = 40;
        int trackHeight = height - 80;

        int thumbHeight = Math.max(20, (int) ((float) trackHeight * (trackHeight / (float) contentHeight)));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * ((float) scrollOffset / maxScroll));

        ctx.fill(trackX, trackY, width - 2, trackY + trackHeight, 0x40000000);
        ctx.fill(trackX, thumbY, width - 2, thumbY + thumbHeight, 0xFFFFAA00);
    }

    private void addToggleButton(int x, int y, String label, String desc, boolean val, Consumer<Boolean> action) {
        ButtonWidget btn = ButtonWidget.builder(getToggleText(label, val), b -> {
            boolean currentlyOn = b.getMessage().getString().contains("ON");
            action.accept(!currentlyOn);
            b.setMessage(getToggleText(label, !currentlyOn));
        }).dimensions(x, y, 150, 20).tooltip(Tooltip.of(Text.literal("§e" + desc))).build();

        scrollableWidgets.add(btn);
        addDrawableChild(btn);
    }

    private void addSlider(int x, int y, int w, String label, float cur, float min, float max, Consumer<Float> action) {
        GenericSlider slider = new GenericSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(slider);
        addDrawableChild(slider);
    }

    private void renderSectionGroup(DrawContext ctx, int x, int y, int w, int buttonRows, String title) {
        int contentH = (buttonRows * SPACING_Y);
        drawStyledPanel(ctx, x, y - TITLE_HEIGHT - 5, w, contentH + TITLE_HEIGHT + 10);
        ctx.drawTextWithShadow(textRenderer, "§6§l» §f" + title, x + 8, y - TITLE_HEIGHT + 1, 0xFFFFFFFF);
        ctx.fill(x + 5, y - 6, x + w - 5, y - 5, 0x80FFAA00);
    }

    private void drawStyledPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x90000000);
        context.fill(x, y, x + 2, y + height, 0xFFFFAA00);
        context.fill(x + width - 2, y, x + width, y + height, 0xFFFFAA00);
    }

    private Text getToggleText(String label, boolean value) {
        return Text.literal(label + ": ").append(value ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
    }

    private Text getGlobalToggleText() {
        return Text.literal("AntiAFK: ").append(
                Main.toggled ? Text.literal("Enabled").formatted(Formatting.GREEN)
                        : Text.literal("Disabled").formatted(Formatting.RED)
        );
    }

    public void refreshGlobalToggle() {
        if (this.globalToggleButton != null) {
            this.globalToggleButton.setMessage(getGlobalToggleText());
        }
    }


    @Override
    public void close() {
        this.config.saveConfiguration();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private static class GenericSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final Consumer<Float> updateAction;

        public GenericSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            super(x, y, w, h, Text.empty(), (double) (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float val = min + (float) (this.value * (max - min));
            this.setMessage(Text.literal(label + ": §e" + String.format("%.1f", val)));
        }

        @Override
        protected void applyValue() {
            float val = min + (float) (this.value * (max - min));
            updateAction.accept(val);
        }
    }
}