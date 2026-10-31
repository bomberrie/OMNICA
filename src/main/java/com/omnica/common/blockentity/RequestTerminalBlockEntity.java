package com.omnica.common.blockentity;

import com.omnica.Omnica;
import com.omnica.common.crafter.CrafterCoupler;
import com.omnica.common.network.OmnicaNetworkManager;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RequestTerminalBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    private final Set<BlockPos> connectedNodes = new HashSet<>();

    public RequestTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Omnica.REQUEST_TERMINAL_BLOCK_ENTITY, pos, state);
    }

    public Set<BlockPos> getConnectedNodes() {
        return Collections.unmodifiableSet(connectedNodes);
    }

    public boolean addConnection(BlockPos target) {
        if (target.equals(pos) || !pos.isWithinDistance(target, 32.5)) {
            return false;
        }
        boolean added = connectedNodes.add(target);
        if (added) markDirty();
        return added;
    }

    public boolean removeConnection(BlockPos target) {
        boolean removed = connectedNodes.remove(target);
        if (removed) markDirty();
        return removed;
    }

    /**
     * Aggregates total stock available across all reachable nodes in the network.
     */
    public List<ItemStack> getAggregatedNetworkItems() {
        if (!(world instanceof ServerWorld serverWorld)) {
            return Collections.emptyList();
        }

        Map<ItemVariant, Long> stockMap = new HashMap<>();
        OmnicaNetworkManager manager = OmnicaNetworkManager.get(serverWorld);

        for (BlockPos nodePos : connectedNodes) {
            for (BlockPos reachablePos : manager.getGraph().getNeighbors(nodePos)) {
                if (!serverWorld.isChunkLoaded(reachablePos)) continue;
                if (serverWorld.getBlockEntity(reachablePos) instanceof NexusNodeBlockEntity nodeEntity) {
                    Storage<ItemVariant> storage = nodeEntity.getAttachedStorage();
                    if (storage != null && storage.supportsExtraction()) {
                        for (StorageView<ItemVariant> view : storage) {
                            if (!view.isResourceBlank() && view.getAmount() > 0) {
                                stockMap.merge(view.getResource(), view.getAmount(), Long::sum);
                            }
                        }
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
        if (!(world instanceof ServerWorld serverWorld)) {
            return 0;
        }

        ItemVariant targetVariant = ItemVariant.of(requested);
        int remaining = desiredAmount;
        OmnicaNetworkManager manager = OmnicaNetworkManager.get(serverWorld);

        // 1. Direct extraction from storage chests
        for (BlockPos nodePos : connectedNodes) {
            for (BlockPos reachablePos : manager.getGraph().getNeighbors(nodePos)) {
                if (remaining <= 0) break;
                if (!serverWorld.isChunkLoaded(reachablePos)) continue;

                if (serverWorld.getBlockEntity(reachablePos) instanceof NexusNodeBlockEntity nodeEntity) {
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
            }
        }

        // 2. Automated Crafter fulfillment if still needed
        if (remaining > 0) {
            for (BlockPos nodePos : connectedNodes) {
                if (remaining <= 0) break;
                if (serverWorld.getBlockEntity(nodePos) instanceof NexusNodeBlockEntity nodeEntity) {
                    if (CrafterCoupler.isAttachedToCrafter(serverWorld, nodeEntity)) {
                        Optional<ItemStack> crafted = CrafterCoupler.tryCraft(serverWorld, nodeEntity);
                        if (crafted.isPresent() && ItemStack.areItemsEqual(crafted.get(), requested)) {
                            remaining -= Math.min(remaining, crafted.get().getCount());
                        }
                    }
                }
            }
        }

        return desiredAmount - remaining;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.omnica.request_terminal");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RequestTerminalScreenHandler(syncId, playerInventory, pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        NbtList list = new NbtList();
        for (BlockPos connection : connectedNodes) {
            list.add(NbtHelper.fromBlockPos(connection));
        }
        nbt.put("Connections", list);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        connectedNodes.clear();
        NbtList list = nbt.getList("Connections", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtHelper.toBlockPos(list.getCompound(i), "Connections").ifPresent(connectedNodes::add);
        }
    }
}
