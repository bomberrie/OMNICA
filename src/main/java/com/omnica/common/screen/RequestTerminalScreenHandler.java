package com.omnica.common.screen;

import com.omnica.Omnica;
import com.omnica.common.blockentity.RequestTerminalBlockEntity;
import com.omnica.common.packet.TerminalSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RequestTerminalScreenHandler extends ScreenHandler {
    private final BlockPos terminalPos;
    private final PlayerInventory playerInventory;
    private final List<ItemStack> clientNetworkItems = new ArrayList<>();

    public RequestTerminalScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(Omnica.REQUEST_TERMINAL_SCREEN_HANDLER, syncId);
        this.terminalPos = pos;
        this.playerInventory = playerInventory;

        // Player Inventory Slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Hotbar Slots (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }

        // Initial sync of network items to opening player
        if (playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
            syncNetworkStock(serverPlayer);
        }
    }

    public void syncNetworkStock(ServerPlayerEntity player) {
        if (player.getServerWorld().getBlockEntity(terminalPos) instanceof RequestTerminalBlockEntity terminal) {
            List<ItemStack> stock = terminal.getAggregatedNetworkItems();
            ServerPlayNetworking.send(player, new TerminalSyncPayload(stock));
        }
    }

    public void setClientNetworkItems(List<ItemStack> items) {
        clientNetworkItems.clear();
        clientNetworkItems.addAll(items);
    }

    public List<ItemStack> getClientNetworkItems() {
        return clientNetworkItems;
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            itemStack = originalStack.copy();

            if (slotIndex < 27) {
                if (!this.insertItem(originalStack, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return itemStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(terminalPos.getX() + 0.5, terminalPos.getY() + 0.5, terminalPos.getZ() + 0.5) <= 64.0;
    }
}
