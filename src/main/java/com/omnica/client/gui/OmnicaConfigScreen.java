package com.omnica.client.gui;

import com.omnica.common.config.OmnicaConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * In-game configuration screen for OMNICA, accessible via Mod Menu.
 */
public class OmnicaConfigScreen extends Screen {
    private final Screen parent;

    public OmnicaConfigScreen(Screen parent) {
        super(Component.translatable("gui.omnica.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        OmnicaConfig config = OmnicaConfig.getInstance();

        int centerX = this.width / 2;
        int startY = 40;
        int rowHeight = 26;
        int widgetWidth = 310;
        int col1X = centerX - widgetWidth / 2;

        // 1. Max Link Distance Slider (8 to 128 blocks)
        this.addRenderableWidget(new AbstractSliderButton(
                col1X, startY, widgetWidth, 20,
                Component.empty(),
                (config.maxLinkDistance - 8.0) / (128.0 - 8.0)
        ) {
            {
                this.updateMessage();
                this.setTooltip(Tooltip.create(Component.translatable("gui.omnica.config.max_distance.tooltip")));
            }

            @Override
            protected void updateMessage() {
                int val = (int) Math.round(8.0 + this.value * (128.0 - 8.0));
                this.setMessage(Component.translatable("gui.omnica.config.max_distance", val));
            }

            @Override
            protected void applyValue() {
                config.maxLinkDistance = (int) Math.round(8.0 + this.value * (128.0 - 8.0));
            }
        });

        // 2. Transfer Interval Slider (1 to 40 ticks)
        this.addRenderableWidget(new AbstractSliderButton(
                col1X, startY + rowHeight, widgetWidth, 20,
                Component.empty(),
                (config.transferIntervalTicks - 1.0) / 39.0
        ) {
            {
                this.updateMessage();
                this.setTooltip(Tooltip.create(Component.translatable("gui.omnica.config.transfer_interval.tooltip")));
            }

            @Override
            protected void updateMessage() {
                int ticks = (int) Math.round(1.0 + this.value * 39.0);
                float seconds = ticks * 0.05f;
                this.setMessage(Component.translatable("gui.omnica.config.transfer_interval", ticks, String.format("%.2f", seconds)));
            }

            @Override
            protected void applyValue() {
                config.transferIntervalTicks = (int) Math.round(1.0 + this.value * 39.0);
            }
        });

        // 3. Batch Items Per Pulse CycleButton (1, 4, 8, 16, 32, 64)
        int halfWidth = (widgetWidth - 6) / 2;
        this.addRenderableWidget(CycleButton.<Integer>builder(
                val -> Component.translatable("gui.omnica.config.items_per_transfer", val),
                () -> config.itemsPerTransfer
        )
                .withValues(1, 4, 8, 16, 32, 64)
                .withTooltip(val -> Tooltip.create(Component.translatable("gui.omnica.config.items_per_transfer.tooltip")))
                .create(col1X, startY + rowHeight * 2, widgetWidth, 20, Component.translatable("gui.omnica.config.items_per_transfer.label"),
                        (button, val) -> config.itemsPerTransfer = val)
        );

        // 4. Render Laser Beams Toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(config.renderLaserBeams)
                .withTooltip(val -> Tooltip.create(Component.translatable("gui.omnica.config.render_beams.tooltip")))
                .create(col1X, startY + rowHeight * 3, halfWidth, 20, Component.translatable("gui.omnica.config.render_beams"),
                        (button, val) -> config.renderLaserBeams = val)
        );

        // 5. Render Floating Items Toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(config.renderFloatingItems)
                .withTooltip(val -> Tooltip.create(Component.translatable("gui.omnica.config.render_items.tooltip")))
                .create(col1X + halfWidth + 6, startY + rowHeight * 3, halfWidth, 20, Component.translatable("gui.omnica.config.render_items"),
                        (button, val) -> config.renderFloatingItems = val)
        );

        // 6. Sound Effects Toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(config.enableSoundEffects)
                .withTooltip(val -> Tooltip.create(Component.translatable("gui.omnica.config.sounds.tooltip")))
                .create(col1X, startY + rowHeight * 4, widgetWidth, 20, Component.translatable("gui.omnica.config.sounds"),
                        (button, val) -> config.enableSoundEffects = val)
        );

        // 7. Done / Save Button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 100, this.height - 30, 200, 20)
                .build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        OmnicaConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }
}
