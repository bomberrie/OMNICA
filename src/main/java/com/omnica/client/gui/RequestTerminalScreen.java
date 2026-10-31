package com.omnica.client.gui;

import com.omnica.common.packet.TerminalRequestPayload;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RequestTerminalScreen extends AbstractContainerScreen<RequestTerminalScreenHandler> {
    private EditBox searchField;
    private String filterText = "";

    public RequestTerminalScreen(RequestTerminalScreenHandler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.searchField = new EditBox(this.font, this.leftPos + 80, this.topPos + 6, 88, 12, Component.literal("Search..."));
        this.searchField.setResponder(text -> this.filterText = text.toLowerCase());
        this.addRenderableWidget(this.searchField);

        // Request initial network inventory from server
        ClientPlayNetworking.send(new TerminalRequestPayload(menu.getTerminalPos(), ItemStack.EMPTY, 0));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = this.leftPos;
        int top = this.topPos;

        // Draw GUI background panel
        extractor.fill(left, top, left + imageWidth, top + imageHeight, 0xEE1E1826);
        extractor.fill(left + 7, top + 18, left + 169, top + 124, 0xFF120E1A);

        // Header border
        extractor.fill(left, top + 18, left + imageWidth, top + 19, 0xFF7A459E);
        extractor.fill(left, top + 124, left + imageWidth, top + 125, 0xFF7A459E);

        // Render network items in 6x9 grid
        List<ItemStack> items = menu.getClientNetworkItems().stream()
                .filter(stack -> filterText.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(filterText))
                .toList();

        int startX = left + 8;
        int startY = top + 20;
        int slotSize = 18;

        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            int col = i % 9;
            int row = i / 9;
            int itemX = startX + col * slotSize;
            int itemY = startY + row * slotSize;

            ItemStack stack = items.get(i);
            extractor.item(stack, itemX, itemY);

            // Draw stack count in compact format
            String countText = stack.getCount() > 999 ? (stack.getCount() / 1000) + "k" : String.valueOf(stack.getCount());
            extractor.itemDecorations(this.font, stack, itemX, itemY, countText);

            // Hover highlight
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                extractor.fill(itemX, itemY, itemX + 16, itemY + 16, 0x80FFFFFF);
                extractor.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        double mouseX = event.x();
        double mouseY = event.y();
        int left = this.leftPos;
        int top = this.topPos;
        int startX = left + 8;
        int startY = top + 20;
        int slotSize = 18;

        List<ItemStack> items = menu.getClientNetworkItems().stream()
                .filter(stack -> filterText.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(filterText))
                .toList();

        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            int col = i % 9;
            int row = i / 9;
            int itemX = startX + col * slotSize;
            int itemY = startY + row * slotSize;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                ItemStack clicked = items.get(i);
                int requestCount = event.hasShiftDown() ? clicked.getMaxStackSize() : 1;
                ClientPlayNetworking.send(new TerminalRequestPayload(menu.getTerminalPos(), clicked, requestCount));
                return true;
            }
        }

        return super.mouseClicked(event, isDouble);
    }
}
