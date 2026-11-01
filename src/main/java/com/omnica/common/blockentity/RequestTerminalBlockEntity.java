package com.omnica.common.blockentity;

import com.omnica.Omnica;
import com.omnica.common.crafter.CrafterCoupler;
import com.omnica.common.network.OmnicaNetworkManager;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RequestTerminalBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    private final Set<BlockPos> connectedNodes = new HashSet<>();

    public RequestTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Omnica.REQUEST_TERMINAL_BLOCK_ENTITY, pos, state);
    }

    public Set<BlockPos> getConnectedNodes() {
        return Collections.unmodifiableSet(connectedNodes);
    }

    public boolean addConnection(BlockPos target) {
        double maxDist = com.omnica.common.config.OmnicaConfig.getInstance().maxLinkDistance;
        if (target.equals(worldPosition) || !worldPosition.closerThan(target, maxDist + 0.5)) {
            return false;
        }
        boolean added = connectedNodes.add(target);
        if (added) setChanged();
        return added;
    }

    public boolean removeConnection(BlockPos target) {
        boolean removed = connectedNodes.remove(target);
        if (removed) setChanged();
        return removed;
    }

    private List<NexusNodeBlockEntity> collectReachableNodes(ServerLevel serverLevel) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>(connectedNodes);
        visited.addAll(connectedNodes);
        List<NexusNodeBlockEntity> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            if (!serverLevel.isLoaded(currentPos)) continue;

            BlockEntity be = serverLevel.getBlockEntity(currentPos);
            if (be instanceof NexusNodeBlockEntity nodeEntity) {
                result.add(nodeEntity);
                for (BlockPos neighbor : nodeEntity.getConnections()) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            } else if (be instanceof RequestTerminalBlockEntity terminalEntity) {
                for (BlockPos neighbor : terminalEntity.getConnectedNodes()) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Aggregates total stock available across all reachable nodes in the network.
     */
    public List<ItemStack> getAggregatedNetworkItems() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }

        Map<ItemVariant, Long> stockMap = new HashMap<>();
        List<NexusNodeBlockEntity> reachable = collectReachableNodes(serverLevel);

        for (NexusNodeBlockEntity nodeEntity : reachable) {
            Storage<ItemVariant> storage = nodeEntity.getAttachedStorage();
            if (storage != null && storage.supportsExtraction()) {
                for (StorageView<ItemVariant> view : storage) {
                    if (!view.isResourceBlank() && view.getAmount() > 0) {
                        stockMap.merge(view.getResource(), view.getAmount(), Long::sum);
                    }
                }
            }
        }

        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemVariant, Long> entry : stockMap.entrySet()) {
            ItemStack stack = entry.getKey().toStack((int) Math.min(entry.getValue(), Integer.MAX_VALUE));
            result.add(stack);
        }
        return result;
    }

    /**
     * Extracts items from the connected network or triggers Crafters if needed.
     */
    public int fulfillRequest(ItemStack requested, int desiredAmount) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        ItemVariant targetVariant = ItemVariant.of(requested);
        int remaining = desiredAmount;
        List<NexusNodeBlockEntity> reachable = collectReachableNodes(serverLevel);

        // 1. Direct extraction from storage chests
        for (NexusNodeBlockEntity nodeEntity : reachable) {
            if (remaining <= 0) break;
            Storage<ItemVariant> storage = nodeEntity.getAttachedStorage();
            if (storage != null && storage.supportsExtraction()) {
                try (Transaction tx = Transaction.openOuter()) {
                    long extracted = storage.extract(targetVariant, remaining, tx);
                    if (extracted > 0) {
                        tx.commit();
                        remaining -= (int) extracted;
                    }
                }
            }
        }

        // 2. Automated Crafter fulfillment if still needed
        if (remaining > 0) {
            for (NexusNodeBlockEntity nodeEntity : reachable) {
                if (remaining <= 0) break;
                if (CrafterCoupler.isAttachedToCrafter(serverLevel, nodeEntity)) {
                    Optional<ItemStack> crafted = CrafterCoupler.tryCraft(serverLevel, nodeEntity);
                    if (crafted.isPresent() && ItemStack.isSameItem(crafted.get(), requested)) {
                        remaining -= Math.min(remaining, crafted.get().getCount());
                    }
                }
            }
        }

        return desiredAmount - remaining;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.omnica.terminal.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RequestTerminalScreenHandler(containerId, playerInventory, worldPosition);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var list = output.list("Connections", BlockPos.CODEC);
        for (BlockPos connection : connectedNodes) {
            list.add(connection);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        connectedNodes.clear();
        for (BlockPos pos : input.listOrEmpty("Connections", BlockPos.CODEC)) {
            connectedNodes.add(pos);
        }
    }
}
