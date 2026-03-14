package me.cioco.antiafk.gui;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AntiAfkScreen extends Screen {

    private static final int SPACING_Y    = 24;
    private static final int SECTION_GAP  = 35;
    private static final int TITLE_HEIGHT = 20;

    private final Screen parent;
    private final AntiAfkConfig config = new AntiAfkConfig();
    private final List<AbstractButtonWidget> scrollableWidgets = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll;
    private int contentHeight;
    private ButtonWidget doneButton;
    private ButtonWidget globalToggleButton;

    private int[] sectionY    = new int[5];
    private int[] sectionRows = new int[5];

    public AntiAfkScreen(Screen parent) {
        super(new LiteralText("Anti-AFK Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children().clear();
        this.scrollableWidgets.clear();

        int centerX  = width / 2;
        int leftCol  = centerX - 155;
        int rightCol = centerX + 5;
        int y = 70;

        sectionY[0] = y;
        sectionRows[0] = 2;
        addToggleButton(leftCol,  y, "Auto Jump",       "Jumps randomly.",              AntiAfkConfig.autoJumpEnabled,    v -> AntiAfkConfig.autoJumpEnabled = v);
        addToggleButton(rightCol, y, "Sneak Mode",      "Automatically sneaks.",        AntiAfkConfig.sneak,              v -> AntiAfkConfig.sneak = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Swing Hand",      "Swings player's hand.",        AntiAfkConfig.shouldSwing,        v -> AntiAfkConfig.shouldSwing = v);
        addToggleButton(rightCol, y, "Random Pause",    "Randomly pauses actions.",     AntiAfkConfig.randomPauseEnabled, v -> AntiAfkConfig.randomPauseEnabled = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[1] = y;
        sectionRows[1] = 2;
        addToggleButton(leftCol,  y, "Player Movement", "Moves the player.",            AntiAfkConfig.movementEnabled,   v -> AntiAfkConfig.movementEnabled = v);
        addToggleButton(rightCol, y, "Mouse Movement",  "Randomly moves camera.",       AntiAfkConfig.mouseMovement,     v -> AntiAfkConfig.mouseMovement = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Spin",            "Rotates the player's camera.", AntiAfkConfig.autoSpinEnabled,   v -> AntiAfkConfig.autoSpinEnabled = v);
        addToggleButton(rightCol, y, "Random Interval", "Varies time between actions.", AntiAfkConfig.useRandomInterval, v -> { AntiAfkConfig.useRandomInterval = v; this.init(); });
        y += SPACING_Y + SECTION_GAP;

        int timingRows = AntiAfkConfig.useRandomInterval ? 3 : 2;
        sectionY[2] = y;
        sectionRows[2] = timingRows;
        if (AntiAfkConfig.useRandomInterval) {
            addSlider(leftCol,  y, 150, "Min Secs",     AntiAfkConfig.minInterval, 0.1f, 10.0f, v -> AntiAfkConfig.minInterval = v);
            addSlider(rightCol, y, 150, "Max Secs",     AntiAfkConfig.maxInterval, 0.1f, 10.0f, v -> AntiAfkConfig.maxInterval = v);
            y += SPACING_Y;
        } else {
            addSlider(leftCol,  y, 310, "Action Delay", AntiAfkConfig.interval,    0.1f, 10.0f, v -> AntiAfkConfig.interval = v);
            y += SPACING_Y;
        }
        addSlider(leftCol,  y, 150, "Spin Speed",  AntiAfkConfig.spinSpeed,              0.1f, 20.0f, v -> AntiAfkConfig.spinSpeed = v);
        addSlider(rightCol, y, 150, "Look Range",  AntiAfkConfig.horizontalMultiplier,    0.1f,  5.0f, v -> { AntiAfkConfig.horizontalMultiplier = v; AntiAfkConfig.verticalMultiplier = v; });
        y += SPACING_Y + SECTION_GAP;

        sectionY[3] = y;
        sectionRows[3] = 2;
        addToggleButton(leftCol,  y, "Auto Eat",       "Eats food when hungry.",            AntiAfkConfig.autoEatEnabled,         v -> AntiAfkConfig.autoEatEnabled = v);
        addToggleButton(rightCol, y, "Random Hotbar",  "Randomly switches hotbar slot.",    AntiAfkConfig.randomHotbarEnabled,    v -> AntiAfkConfig.randomHotbarEnabled = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Offhand Swap",   "Swaps main/offhand randomly.",      AntiAfkConfig.offhandSwapEnabled,     v -> AntiAfkConfig.offhandSwapEnabled = v);
        addToggleButton(rightCol, y, "Open Inventory", "Randomly opens inventory briefly.", AntiAfkConfig.randomInventoryEnabled, v -> AntiAfkConfig.randomInventoryEnabled = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[4] = y;
        sectionRows[4] = 7;
        addSlider(leftCol,  y, 310, "Eat Below HungerLevel",      AntiAfkConfig.eatFoodLevel,            1.0f,  20.0f, v -> AntiAfkConfig.eatFoodLevel = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Hotbar Min Secs",   AntiAfkConfig.hotbarSwitchMinSeconds,  1.0f,  60.0f, v -> AntiAfkConfig.hotbarSwitchMinSeconds = v);
        addSlider(rightCol, y, 150, "Hotbar Max Secs",   AntiAfkConfig.hotbarSwitchMaxSeconds,  1.0f,  60.0f, v -> AntiAfkConfig.hotbarSwitchMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Offhand Min Secs",  AntiAfkConfig.offhandSwapMinSeconds,   1.0f, 120.0f, v -> AntiAfkConfig.offhandSwapMinSeconds = v);
        addSlider(rightCol, y, 150, "Offhand Max Secs",  AntiAfkConfig.offhandSwapMaxSeconds,   1.0f, 120.0f, v -> AntiAfkConfig.offhandSwapMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 310, "Offhand Hold Secs", AntiAfkConfig.offhandHoldSeconds,      1.0f,  30.0f, v -> AntiAfkConfig.offhandHoldSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Inv Min Secs",      AntiAfkConfig.inventoryOpenMinSeconds,  1.0f, 120.0f, v -> AntiAfkConfig.inventoryOpenMinSeconds = v);
        addSlider(rightCol, y, 150, "Inv Max Secs",      AntiAfkConfig.inventoryOpenMaxSeconds,  1.0f, 120.0f, v -> AntiAfkConfig.inventoryOpenMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 310, "Inv Hold Secs",     AntiAfkConfig.inventoryHoldSeconds,     1.0f,  30.0f, v -> AntiAfkConfig.inventoryHoldSeconds = v);

        contentHeight = y + 40;
        maxScroll = Math.max(0, contentHeight - (height - 90));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        globalToggleButton = new ButtonWidget(
                centerX - 100, height - 60, 200, 20,
                getGlobalToggleText(),
                b -> { Main.toggled = !Main.toggled; b.setMessage(getGlobalToggleText()); }
        );
        addButton(globalToggleButton);

        doneButton = new ButtonWidget(
                centerX - 100, height - 30, 200, 20,
                new LiteralText("SAVE & EXIT").formatted(Formatting.GOLD, Formatting.BOLD),
                b -> this.onClose()
        );
        addButton(doneButton);

        for (AbstractButtonWidget widget : scrollableWidgets) {
            widget.y = widget.y - scrollOffset;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        int cx     = width / 2;
        int panelW = 325;
        int panelX = cx - (panelW / 2);

        drawCenteredString(matrices, textRenderer,
                new LiteralText("Anti-AFK Settings").formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE).getString(),
                cx, 15, 0xFFFFFFFF);

        // Manual scissor using GL11
        double scaleFactor = this.client.getWindow().getScaleFactor();
        int scissorX = 0;
        int scissorY = (int) (40 * scaleFactor);
        int scissorW = (int) (width * scaleFactor);
        int scissorH = (int) ((height - 80) * scaleFactor);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        String[] titles = { "Player Actions", "Movement & Behavior", "Advanced Timing", "Inventory & Eating", "Feature Timing" };
        for (int i = 0; i < sectionY.length; i++) {
            renderSectionGroup(matrices, panelX, sectionY[i] - scrollOffset, panelW, sectionRows[i], titles[i]);
        }

        for (AbstractButtonWidget widget : scrollableWidgets) {
            widget.visible = (widget.y + widget.getHeight() > 40 && widget.y < height - 40);
            if (widget.visible) widget.render(matrices, mouseX, mouseY, delta);
        }
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        globalToggleButton.render(matrices, mouseX, mouseY, delta);
        doneButton.render(matrices, mouseX, mouseY, delta);

        drawScrollBar(matrices);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (maxScroll > 0) {
            int oldOffset = scrollOffset;
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - (amount * 25)));
            int diff = oldOffset - scrollOffset;
            for (AbstractButtonWidget widget : scrollableWidgets) {
                widget.y = widget.y + diff;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void drawScrollBar(MatrixStack matrices) {
        if (maxScroll <= 0) return;
        int trackX      = width - 6;
        int trackY      = 40;
        int trackHeight = height - 80;
        int thumbHeight = Math.max(20, (int) ((float) trackHeight * (trackHeight / (float) contentHeight)));
        int thumbY      = trackY + (int) ((trackHeight - thumbHeight) * ((float) scrollOffset / maxScroll));
        fill(matrices, trackX, trackY, width - 2, trackY + trackHeight, 0x40000000);
        fill(matrices, trackX, thumbY, width - 2, thumbY + thumbHeight, 0xFFFFAA00);
    }

    private void addToggleButton(int x, int y, String label, String desc, boolean val, Consumer<Boolean> action) {
        ButtonWidget btn = new ButtonWidget(x, y, 150, 20, getToggleText(label, val), b -> {
            boolean currentlyOn = b.getMessage().getString().contains("ON");
            action.accept(!currentlyOn);
            b.setMessage(getToggleText(label, !currentlyOn));
        });
        scrollableWidgets.add(btn);
        addButton(btn);
    }

    private void addSlider(int x, int y, int w, String label, float cur, float min, float max, Consumer<Float> action) {
        GenericSlider slider = new GenericSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(slider);
        addButton(slider);
    }

    private void renderSectionGroup(MatrixStack matrices, int x, int y, int w, int rows, String title) {
        int contentH = rows * SPACING_Y;
        drawStyledPanel(matrices, x, y - TITLE_HEIGHT - 5, w, contentH + TITLE_HEIGHT + 10);
        drawStringWithShadow(matrices, textRenderer, "\u00a76\u00a7l\u00bb \u00a7f" + title, x + 8, y - TITLE_HEIGHT + 1, 0xFFFFFFFF);
        fill(matrices, x + 5, y - 6, x + w - 5, y - 5, 0x80FFAA00);
    }

    private void drawStyledPanel(MatrixStack matrices, int x, int y, int width, int height) {
        fill(matrices, x, y, x + width, y + height, 0x90000000);
        fill(matrices, x, y, x + 2, y + height, 0xFFFFAA00);
        fill(matrices, x + width - 2, y, x + width, y + height, 0xFFFFAA00);
    }

    private Text getToggleText(String label, boolean value) {
        LiteralText text = new LiteralText(label + ": ");
        text.append(value ? new LiteralText("ON").formatted(Formatting.GREEN)
                : new LiteralText("OFF").formatted(Formatting.RED));
        return text;
    }

    private Text getGlobalToggleText() {
        LiteralText text = new LiteralText("AntiAFK: ");
        text.append(Main.toggled ? new LiteralText("Enabled").formatted(Formatting.GREEN)
                : new LiteralText("Disabled").formatted(Formatting.RED));
        return text;
    }

    public void refreshGlobalToggle() {
        if (this.globalToggleButton != null) {
            this.globalToggleButton.setMessage(getGlobalToggleText());
        }
    }

    @Override
    public void onClose() {
        this.config.saveConfiguration();
        if (this.client != null) {
            this.client.openScreen(this.parent);
        }
    }

    private static class GenericSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final Consumer<Float> updateAction;

        public GenericSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            super(x, y, w, h, LiteralText.EMPTY, (double) (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float val = min + (float) (this.value * (max - min));
            this.setMessage(new LiteralText(label + ": \u00a7e" + String.format("%.1f", val)));
        }

        @Override
        protected void applyValue() {
            float val = min + (float) (this.value * (max - min));
            updateAction.accept(val);
        }
    }
}
