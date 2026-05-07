package me.cioco.antiafk.gui;

import me.cioco.antiafk.Main;
import me.cioco.antiafk.config.AntiAfkConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AntiAfkScreen extends Screen {

    private static final int SPACING_Y   = 24;
    private static final int SECTION_GAP = 35;
    private static final int TITLE_HEIGHT = 20;

    private final Screen parent;
    private final AntiAfkConfig config = new AntiAfkConfig();
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll;
    private int contentHeight;
    private Button doneButton;
    private Button globalToggleButton;

    private final int[] sectionY    = new int[8];
    private final int[] sectionRows = new int[8];

    public AntiAfkScreen(Screen parent) {
        super(Component.literal("Anti-AFK Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.scrollableWidgets.clear();

        int centerX  = width / 2;
        int leftCol  = centerX - 155;
        int rightCol = centerX + 5;
        int y = 70;

        sectionY[0] = y; sectionRows[0] = 2;
        addToggleButton(leftCol,  y, "Auto Jump",    "Jumps randomly.",          AntiAfkConfig.autoJumpEnabled,    v -> AntiAfkConfig.autoJumpEnabled = v);
        addToggleButton(rightCol, y, "Sneak Mode",   "Automatically sneaks.",    AntiAfkConfig.sneak,              v -> AntiAfkConfig.sneak = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Swing Hand",   "Swings player's hand.",    AntiAfkConfig.shouldSwing,        v -> AntiAfkConfig.shouldSwing = v);
        addToggleButton(rightCol, y, "Random Pause", "Randomly pauses actions.", AntiAfkConfig.randomPauseEnabled, v -> AntiAfkConfig.randomPauseEnabled = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[1] = y; sectionRows[1] = 2;
        addToggleButton(leftCol,  y, "Player Movement", "Moves the player.",            AntiAfkConfig.movementEnabled,   v -> AntiAfkConfig.movementEnabled = v);
        addToggleButton(rightCol, y, "Mouse Movement",  "Randomly moves camera.",       AntiAfkConfig.mouseMovement,     v -> AntiAfkConfig.mouseMovement = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Spin",            "Rotates player camera.",       AntiAfkConfig.autoSpinEnabled,   v -> AntiAfkConfig.autoSpinEnabled = v);
        addToggleButton(rightCol, y, "Random Interval", "Varies time between actions.", AntiAfkConfig.useRandomInterval, v -> { AntiAfkConfig.useRandomInterval = v; this.init(); });
        y += SPACING_Y + SECTION_GAP;

        int timingRows = AntiAfkConfig.useRandomInterval ? 3 : 2;
        sectionY[2] = y; sectionRows[2] = timingRows;
        if (AntiAfkConfig.useRandomInterval) {
            addSlider(leftCol,  y, 150, "Min Secs", AntiAfkConfig.minInterval, 0.1f, 10.0f, v -> AntiAfkConfig.minInterval = v);
            addSlider(rightCol, y, 150, "Max Secs", AntiAfkConfig.maxInterval, 0.1f, 10.0f, v -> AntiAfkConfig.maxInterval = v);
            y += SPACING_Y;
        } else {
            addSlider(leftCol,  y, 310, "Action Delay Secs", AntiAfkConfig.interval, 0.1f, 10.0f, v -> AntiAfkConfig.interval = v);
            y += SPACING_Y;
        }
        addSlider(leftCol,  y, 150, "Spin Speed", AntiAfkConfig.spinSpeed,             0.1f, 20.0f, v -> AntiAfkConfig.spinSpeed = v);
        addSlider(rightCol, y, 150, "Look Range", AntiAfkConfig.horizontalMultiplier,  0.1f,  5.0f, v -> { AntiAfkConfig.horizontalMultiplier = v; AntiAfkConfig.verticalMultiplier = v; });
        y += SPACING_Y + SECTION_GAP;

        sectionY[3] = y; sectionRows[3] = 2;
        addToggleButton(leftCol,  y, "Mouse Jitter", "Adds subtle micro camera shake.", AntiAfkConfig.mouseJitterEnabled, v -> AntiAfkConfig.mouseJitterEnabled = v);
        y += SPACING_Y;
        addSlider(leftCol, y, 310, "Jitter Strength", AntiAfkConfig.jitterStrength, 0.1f, 10.0f, v -> AntiAfkConfig.jitterStrength = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[4] = y; sectionRows[4] = 2;
        addToggleButton(leftCol,  y, "Auto Eat",       "Eats food when hungry.",            AntiAfkConfig.autoEatEnabled,         v -> AntiAfkConfig.autoEatEnabled = v);
        addToggleButton(rightCol, y, "Random Hotbar",  "Randomly switches hotbar slot.",    AntiAfkConfig.randomHotbarEnabled,    v -> AntiAfkConfig.randomHotbarEnabled = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Offhand Swap",   "Swaps main/offhand randomly.",      AntiAfkConfig.offhandSwapEnabled,     v -> AntiAfkConfig.offhandSwapEnabled = v);
        addToggleButton(rightCol, y, "Open Inventory", "Randomly opens inventory briefly.", AntiAfkConfig.randomInventoryEnabled, v -> AntiAfkConfig.randomInventoryEnabled = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[5] = y; sectionRows[5] = 4;
        addToggleButton(leftCol,  y, "Chat Messages",  "Sends random chat messages.", AntiAfkConfig.chatMessagesEnabled,  v -> AntiAfkConfig.chatMessagesEnabled = v);
        addToggleButton(rightCol, y, "Auto Reconnect", "Reconnects on disconnect.",   AntiAfkConfig.autoReconnectEnabled, v -> AntiAfkConfig.autoReconnectEnabled = v);
        y += SPACING_Y;

        EditBox chatField = new EditBox(font, leftCol, y, 310, 20, Component.literal("Messages"));
        chatField.setMaxLength(512);
        chatField.setValue(AntiAfkConfig.chatMessages != null ? AntiAfkConfig.chatMessages : "");
        chatField.setHint(Component.literal("§8Hey!;Still here!;Just farming"));
        chatField.setResponder(val -> AntiAfkConfig.chatMessages = val);
        scrollableWidgets.add(chatField);
        addRenderableWidget(chatField);
        y += SPACING_Y;

        addSlider(leftCol,  y, 150, "Chat Min Secs", AntiAfkConfig.chatMessageMinSeconds, 10f, 300f, v -> AntiAfkConfig.chatMessageMinSeconds = v);
        addSlider(rightCol, y, 150, "Chat Max Secs", AntiAfkConfig.chatMessageMaxSeconds, 10f, 300f, v -> AntiAfkConfig.chatMessageMaxSeconds = v);
        y += SPACING_Y + SECTION_GAP;

        sectionY[6] = y; sectionRows[6] = 3;
        addToggleButton(leftCol, y, "Auto Disconnect", "Disconnects if a player enters the set radius.", AntiAfkConfig.autoDisconnectEnabled, v -> AntiAfkConfig.autoDisconnectEnabled = v);
        y += SPACING_Y;
        addSlider(leftCol, y, 310, "Radius", AntiAfkConfig.autoDisconnectRadius, 1.0f, 64.0f, v -> AntiAfkConfig.autoDisconnectRadius = v);
        y += SPACING_Y;

        EditBox ignoreField = new EditBox(font, leftCol, y, 310, 20, Component.literal("Ignored players (semicolon separated)"));
        ignoreField.setMaxLength(512);
        ignoreField.setValue(AntiAfkConfig.autoDisconnectIgnoredPlayers != null ? AntiAfkConfig.autoDisconnectIgnoredPlayers : "");
        ignoreField.setHint(Component.literal("Ignored Players: example (§8Steve;Alex;Notch)"));
        ignoreField.setResponder(val -> AntiAfkConfig.autoDisconnectIgnoredPlayers = val);
        scrollableWidgets.add(ignoreField);
        addRenderableWidget(ignoreField);
        y += SPACING_Y + SECTION_GAP;

        sectionY[7] = y; sectionRows[7] = 8;
        addSlider(leftCol,  y, 310, "Hunger Threshold",      AntiAfkConfig.eatFoodLevel,            1.0f,  20.0f, v -> AntiAfkConfig.eatFoodLevel = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Hotbar Min Secs",       AntiAfkConfig.hotbarSwitchMinSeconds,  1.0f,  60.0f, v -> AntiAfkConfig.hotbarSwitchMinSeconds = v);
        addSlider(rightCol, y, 150, "Hotbar Max Secs",       AntiAfkConfig.hotbarSwitchMaxSeconds,  1.0f,  60.0f, v -> AntiAfkConfig.hotbarSwitchMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Offhand Min Secs",      AntiAfkConfig.offhandSwapMinSeconds,   1.0f, 120.0f, v -> AntiAfkConfig.offhandSwapMinSeconds = v);
        addSlider(rightCol, y, 150, "Offhand Max Secs",      AntiAfkConfig.offhandSwapMaxSeconds,   1.0f, 120.0f, v -> AntiAfkConfig.offhandSwapMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 310, "Offhand Hold Secs",     AntiAfkConfig.offhandHoldSeconds,      1.0f,  30.0f, v -> AntiAfkConfig.offhandHoldSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Inv Min Secs",          AntiAfkConfig.inventoryOpenMinSeconds, 1.0f, 120.0f, v -> AntiAfkConfig.inventoryOpenMinSeconds = v);
        addSlider(rightCol, y, 150, "Inv Max Secs",          AntiAfkConfig.inventoryOpenMaxSeconds, 1.0f, 120.0f, v -> AntiAfkConfig.inventoryOpenMaxSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 310, "Inv Hold Secs",         AntiAfkConfig.inventoryHoldSeconds,    1.0f,  30.0f, v -> AntiAfkConfig.inventoryHoldSeconds = v);
        y += SPACING_Y;
        addSlider(leftCol,  y, 150, "Reconnect Delay Secs",  AntiAfkConfig.reconnectDelaySeconds,   1.0f,  60.0f, v -> AntiAfkConfig.reconnectDelaySeconds = v);
        y += SPACING_Y;

        contentHeight = y + 40;
        maxScroll = Math.max(0, contentHeight - (height - 90));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        globalToggleButton = Button.builder(getGlobalToggleText(), b -> {
            Main.toggled = !Main.toggled;
            b.setMessage(getGlobalToggleText());
        }).bounds(centerX - 100, height - 60, 200, 20).build();

        addRenderableWidget(globalToggleButton);

        doneButton = Button.builder(
                Component.literal("SAVE & EXIT").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                b -> this.onClose()).bounds(centerX - 100, height - 30, 200, 20).build();
        addRenderableWidget(doneButton);

        for (AbstractWidget widget : scrollableWidgets) {
            widget.setY(widget.getY() - scrollOffset);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int cx = width / 2;
        int panelW = 325;
        int panelX = cx - (panelW / 2);

        ctx.centeredText(font, Component.literal("Anti-AFK Settings").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD, ChatFormatting.UNDERLINE), cx, 15, 0xFFFFFFFF);
        ctx.enableScissor(0, 40, width, height - 70);

        String[] titles = {"Player Actions", "Movement & Behavior", "Advanced Timing", "Mouse Jitter", "Inventory & Eating", "Chat & Reconnect", "Auto Disconnect", "Feature Timing"};
        for (int i = 0; i < sectionY.length; i++) {
            renderSectionGroup(ctx, panelX, sectionY[i] - scrollOffset, panelW, sectionRows[i], titles[i]);
        }

        for (AbstractWidget widget : scrollableWidgets) {
            if (widget.getY() + widget.getHeight() > 40 && widget.getY() < height - 70) {
                widget.visible = true;
                widget.extractRenderState(ctx, mouseX, mouseY, delta);
            } else {
                widget.visible = false;
            }
        }

        ctx.disableScissor();

        globalToggleButton.extractRenderState(ctx, mouseX, mouseY, delta);
        doneButton.extractRenderState(ctx, mouseX, mouseY, delta);

        drawScrollBar(ctx);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            int oldOffset = scrollOffset;
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - (verticalAmount * 25)));
            int diff = oldOffset - scrollOffset;
            for (AbstractWidget widget : scrollableWidgets) {
                widget.setY(widget.getY() + diff);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawScrollBar(GuiGraphicsExtractor ctx) {
        if (maxScroll <= 0) return;
        int trackX = width - 6;
        int trackY = 40;
        int trackHeight = height - 110;
        int thumbHeight = Math.max(20, (int) ((float) trackHeight * (trackHeight / (float) contentHeight)));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * ((float) scrollOffset / maxScroll));
        ctx.fill(trackX, trackY, width - 2, trackY + trackHeight, 0x40000000);
        ctx.fill(trackX, thumbY, width - 2, thumbY + thumbHeight, 0xFFFFAA00);
    }

    private void addToggleButton(int x, int y, String label, String desc, boolean val, Consumer<Boolean> action) {
        Button btn = Button.builder(getToggleText(label, val), b -> {
            boolean currentlyOn = b.getMessage().getString().contains("ON");
            action.accept(!currentlyOn);
            b.setMessage(getToggleText(label, !currentlyOn));
        }).bounds(x, y, 150, 20).tooltip(Tooltip.create(Component.literal("§e" + desc))).build();
        scrollableWidgets.add(btn);
        addRenderableWidget(btn);
    }

    private void addSlider(int x, int y, int w, String label, float cur, float min, float max, Consumer<Float> action) {
        CompoundSlider compound = new CompoundSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(compound.slider);
        scrollableWidgets.add(compound.textField);
        addRenderableWidget(compound.slider);
        addRenderableWidget(compound.textField);
    }

    private void renderSectionGroup(GuiGraphicsExtractor ctx, int x, int y, int w, int rows, String title) {
        int contentH = rows * SPACING_Y;
        drawStyledPanel(ctx, x, y - TITLE_HEIGHT - 5, w, contentH + TITLE_HEIGHT + 10);
        ctx.text(font, "§6§l» §f" + title, x + 8, y - TITLE_HEIGHT + 1, 0xFFFFFFFF);
        ctx.fill(x + 5, y - 6, x + w - 5, y - 5, 0x80FFAA00);
    }

    private void drawStyledPanel(GuiGraphicsExtractor ctx, int x, int y, int width, int height) {
        ctx.fill(x, y, x + width, y + height, 0x90000000);
        ctx.fill(x, y, x + 2, y + height, 0xFFFFAA00);
        ctx.fill(x + width - 2, y, x + width, y + height, 0xFFFFAA00);
    }

    private Component getToggleText(String label, boolean value) {
        return Component.literal(label + ": ").append(
                value ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                        : Component.literal("OFF").withStyle(ChatFormatting.RED)
        );
    }

    private Component getGlobalToggleText() {
        return Component.literal("AntiAFK: ").append(
                Main.toggled ? Component.literal("Enabled").withStyle(ChatFormatting.GREEN)
                        : Component.literal("Disabled").withStyle(ChatFormatting.RED)
        );
    }

    public void refreshGlobalToggle() {
        if (globalToggleButton != null) {
            globalToggleButton.setMessage(getGlobalToggleText());
        }
    }

    @Override
    public void onClose() {
        config.saveConfiguration();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private class CompoundSlider {
        public final CustomSlider slider;
        public final EditBox textField;
        private boolean isUpdating = false;

        public CompoundSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            int textWidth = 45;
            this.textField = new EditBox(font, x + w - textWidth, y, textWidth, h, Component.empty());
            this.textField.setValue(String.format("%.2f", cur));

            this.slider = new CustomSlider(x, y, w - textWidth - 2, h, label, cur, min, max, (val) -> {
                if (!isUpdating) {
                    isUpdating = true;
                    textField.setValue(String.format("%.2f", val));
                    action.accept(val);
                    isUpdating = false;
                }
            });

            this.textField.setResponder(text -> {
                if (isUpdating) return;
                try {
                    float val = Float.parseFloat(text);
                    isUpdating = true;
                    double sliderPos = (val - min) / (max - min);
                    slider.forceValue(Math.max(0.0, Math.min(1.0, sliderPos)));
                    action.accept(val);
                    isUpdating = false;
                } catch (NumberFormatException ignored) {}
            });
        }
    }

    private class CustomSlider extends AbstractSliderButton {
        private final String label;
        private final float min, max;
        private final Consumer<Float> callback;

        public CustomSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> callback) {
            super(x, y, w, h, Component.empty(), (double) (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.callback = callback;
            this.updateMessage();
        }

        public void forceValue(double val) {
            this.value = val;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            float val = min + (float) (this.value * (max - min));
            setMessage(Component.literal(label + ": §e" + String.format("%.2f", val)));
        }

        @Override
        protected void applyValue() {
            float val = min + (float) (this.value * (max - min));
            callback.accept(val);
        }
    }
}