package com.omnica.client.gui;

import com.omnica.common.packet.TerminalRequestPayload;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client-side screen for the Omnica Request Terminal.
 * Built with an authentic Vanilla container design based on the 54-slot container layout.
 */
public class RequestTerminalScreen extends AbstractContainerScreen<RequestTerminalScreenHandler> {
    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private EditBox searchField;
    private String filterText = "";
    private int scrollRow = 0;

    public RequestTerminalScreen(RequestTerminalScreenHandler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();

        // Position search box in the top-right header area with ample clearance from the title
        this.searchField = new EditBox(this.font, this.leftPos + 80, this.topPos + 4, 88, 12, Component.literal("Search"));
        this.searchField.setMaxLength(32);
        this.searchField.setBordered(true);
        this.searchField.setCanLoseFocus(true);
        this.searchField.setTextColor(0xFFFFFFFF);
        this.searchField.setHint(Component.literal("Search...").withStyle(ChatFormatting.GRAY));
        this.searchField.setResponder(text -> {
            this.filterText = text.toLowerCase().trim();
            this.scrollRow = 0;
        });
        this.addRenderableWidget(this.searchField);

        // Request initial network inventory from server
        ClientPlayNetworking.send(new TerminalRequestPayload(menu.getTerminalPos(), ItemStack.EMPTY, 0));
    }

    private List<ItemStack> getFilteredItems() {
        return menu.getClientNetworkItems().stream()
                .filter(stack -> filterText.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(filterText))
                .toList();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        // Render authentic vanilla 54-slot container background
        // Top 6 rows of slots + header
        extractor.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0.0f, 0.0f, this.imageWidth, 125, 256, 256);
        // Bottom player inventory + hotbar
        extractor.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + 125, 0.0f, 126.0f, this.imageWidth, 96, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // Draw clean title "Terminal" ensuring it never overlaps the search box
        extractor.text(this.font, Component.literal("Terminal"), 8, 6, 0x404040, false);
        extractor.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Render vanilla container slots, player items, and labels
        super.extractContents(extractor, mouseX, mouseY, partialTick);

        // Render network items on the top 6x9 grid of slots
        List<ItemStack> items = getFilteredItems();
        int startIndex = this.scrollRow * 9;
        int startX = this.leftPos + 8;
        int startY = this.topPos + 18;
        int slotSize = 18;

        for (int i = 0; i < 54; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) break;

            int col = i % 9;
            int row = i / 9;
            int itemX = startX + col * slotSize;
            int itemY = startY + row * slotSize;

            ItemStack stack = items.get(itemIndex);
            extractor.item(stack, itemX, itemY);

            // Draw stack count in compact format
            String countText = stack.getCount() > 999
                    ? (stack.getCount() / 1000) + "k"
                    : (stack.getCount() > 1 ? String.valueOf(stack.getCount()) : null);

            if (countText != null) {
                extractor.itemDecorations(this.font, stack, itemX, itemY, countText);
            } else {
                extractor.itemDecorations(this.font, stack, itemX, itemY);
            }

            // Hover highlight matching vanilla slot hover
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                extractor.fill(itemX, itemY, itemX + 16, itemY + 16, 0x80FFFFFF);
                extractor.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        double mouseX = event.x();
        double mouseY = event.y();

        // Direct search box interaction
        if (this.searchField != null) {
            if (mouseX >= this.searchField.getX() && mouseX < this.searchField.getX() + this.searchField.getWidth()
                    && mouseY >= this.searchField.getY() && mouseY < this.searchField.getY() + this.searchField.getHeight()) {
                if (event.button() == 1) { // Right click clears search
                    this.searchField.setValue("");
                }
                this.searchField.setFocused(true);
                return this.searchField.mouseClicked(event, isDouble);
            } else if (mouseX < this.leftPos || mouseX > this.leftPos + this.imageWidth || mouseY < this.topPos || mouseY > this.topPos + this.imageHeight) {
                this.searchField.setFocused(false);
            }
        }

        // Check if a network item in the top 54 slots was clicked
        int startX = this.leftPos + 8;
        int startY = this.topPos + 18;
        int slotSize = 18;

        List<ItemStack> items = getFilteredItems();
        int startIndex = this.scrollRow * 9;

        for (int i = 0; i < 54; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) break;

            int col = i % 9;
            int row = i / 9;
            int itemX = startX + col * slotSize;
            int itemY = startY + row * slotSize;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                ItemStack clicked = items.get(itemIndex);
                int requestCount = event.hasShiftDown() ? clicked.getMaxStackSize() : 1;
                ClientPlayNetworking.send(new TerminalRequestPayload(menu.getTerminalPos(), clicked, requestCount));
                return true;
            }
        }

        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<ItemStack> items = getFilteredItems();
        int totalRows = (items.size() + 8) / 9;
        int maxScrollRow = Math.max(0, totalRows - 6);
        if (maxScrollRow > 0) {
            if (scrollY > 0) {
                this.scrollRow = Math.max(0, this.scrollRow - 1);
                return true;
            } else if (scrollY < 0) {
                this.scrollRow = Math.min(maxScrollRow, this.scrollRow + 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (event.key() == 256) { // Escape key clears focus
                this.searchField.setFocused(false);
                return true;
            }
            if (this.searchField.keyPressed(event)) {
                return true;
            }
            // Consume key while typing so inventory key (e.g. 'E') does not close the screen
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchField != null && this.searchField.isFocused()) {
            return this.searchField.charTyped(event);
        }
        return super.charTyped(event);
    }
}
