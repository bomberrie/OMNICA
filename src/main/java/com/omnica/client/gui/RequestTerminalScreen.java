package com.omnica.client.gui;

import com.omnica.common.packet.TerminalRequestPayload;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class RequestTerminalScreen extends HandledScreen<RequestTerminalScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    private TextFieldWidget searchField;
    private String filterText = "";

    public RequestTerminalScreen(RequestTerminalScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.searchField = new TextFieldWidget(this.textRenderer, this.x + 80, this.y + 6, 88, 12, Text.literal("Search..."));
        this.searchField.setChangedListener(text -> this.filterText = text.toLowerCase());
        this.addSelectableChild(this.searchField);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = this.x;
        int top = this.y;

        // Draw GUI background panel
        context.fill(left, top, left + backgroundWidth, top + backgroundHeight, 0xEE1E1826);
        context.fill(left + 7, top + 18, left + 169, top + 124, 0xFF120E1A);

        // Header border
        context.fill(left, top + 18, left + backgroundWidth, top + 19, 0xFF7A459E);
        context.fill(left, top + 124, left + backgroundWidth, top + 125, 0xFF7A459E);

        // Search field
        this.searchField.render(context, mouseX, mouseY, delta);

        // Render network items in 6x9 grid
        List<ItemStack> items = handler.getClientNetworkItems().stream()
                .filter(stack -> filterText.isEmpty() || stack.getName().getString().toLowerCase().contains(filterText))
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
            context.drawItem(stack, itemX, itemY);

            // Draw stack count in compact format
            String countText = stack.getCount() > 999 ? (stack.getCount() / 1000) + "k" : String.valueOf(stack.getCount());
            context.drawItemInSlot(this.textRenderer, stack, itemX, itemY, countText);

            // Hover highlight
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                context.fill(itemX, itemY, itemX + 16, itemY + 16, 0x80FFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = this.x;
        int top = this.y;
        int startX = left + 8;
        int startY = top + 20;
        int slotSize = 18;

        List<ItemStack> items = handler.getClientNetworkItems().stream()
                .filter(stack -> filterText.isEmpty() || stack.getName().getString().toLowerCase().contains(filterText))
                .toList();

        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            int col = i % 9;
            int row = i / 9;
            int itemX = startX + col * slotSize;
            int itemY = startY + row * slotSize;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                ItemStack clicked = items.get(i);
                int requestCount = hasShiftDown() ? clicked.getMaxCount() : 1;
                ClientPlayNetworking.send(new TerminalRequestPayload(handler.getTerminalPos(), clicked, requestCount));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
