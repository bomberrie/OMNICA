package com.omnica.common.screen;

import com.omnica.Omnica;
import com.omnica.common.blockentity.RequestTerminalBlockEntity;
import com.omnica.common.packet.TerminalSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RequestTerminalScreenHandler extends AbstractContainerMenu {
    private final BlockPos terminalPos;
    private final Inventory playerInventory;
    private final List<ItemStack> clientNetworkItems = new ArrayList<>();

    public RequestTerminalScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, BlockPos.ZERO);
    }

    public RequestTerminalScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
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
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            syncNetworkStock(serverPlayer);
        }
    }

    public void syncNetworkStock(ServerPlayer player) {
        if (player.level().getBlockEntity(terminalPos) instanceof RequestTerminalBlockEntity terminal) {
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
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            itemStack = originalStack.copy();

            if (slotIndex < 27) {
                if (!this.moveItemStackTo(originalStack, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(terminalPos.getX() + 0.5, terminalPos.getY() + 0.5, terminalPos.getZ() + 0.5) <= 64.0;
    }
}
