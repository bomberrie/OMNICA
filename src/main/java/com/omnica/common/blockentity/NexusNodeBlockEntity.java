package com.omnica.common.blockentity;

import com.omnica.Omnica;
import com.omnica.common.block.NexusNodeBlock;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NexusNodeBlockEntity extends BlockEntity {
    private final Set<BlockPos> connections = new HashSet<>();
    private ItemStack filterStack = ItemStack.EMPTY;
    private NodeMode mode = NodeMode.BALANCED;

    public enum NodeMode {
        EXTRACTOR,
        INSERTER,
        BALANCED
    }

    public NexusNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Omnica.NEXUS_NODE_BLOCK_ENTITY, pos, state);
    }

    public Set<BlockPos> getConnections() {
        return Collections.unmodifiableSet(connections);
    }

    public boolean addConnection(BlockPos target) {
        if (target.equals(pos) || !pos.isWithinDistance(target, 32.5)) {
            return false;
        }
        boolean added = connections.add(target);
        if (added) {
            markDirty();
            if (world != null && !world.isClient()) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        }
        return added;
    }

    public boolean removeConnection(BlockPos target) {
        boolean removed = connections.remove(target);
        if (removed) {
            markDirty();
            if (world != null && !world.isClient()) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        }
        return removed;
    }

    public void clearConnections() {
        connections.clear();
        markDirty();
    }

    public ItemStack getFilterStack() {
        return filterStack;
    }

    public void setFilterStack(ItemStack stack) {
        this.filterStack = stack.copy();
        markDirty();
    }

    public NodeMode getMode() {
        return mode;
    }

    public void setMode(NodeMode mode) {
        this.mode = mode;
        markDirty();
    }

    /**
     * Determines whether an item passes the node's configured filter.
     * Deeply supports vanilla 1.21 Bundles as multi-item whitelists!
     */
    public boolean allowsItem(ItemStack candidate) {
        if (filterStack.isEmpty()) {
            return true;
        }

        BundleContentsComponent bundleContents = filterStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            for (ItemStack filterItem : bundleContents.iterate()) {
                if (ItemStack.areItemsEqual(filterItem, candidate)) {
                    return true;
                }
            }
            return false;
        }

        return ItemStack.areItemsEqual(filterStack, candidate);
    }

    /**
     * Resolves the attached inventory storage (Chest, Barrel, Crafter) via Fabric Transfer API.
     */
    @Nullable
    public Storage<ItemVariant> getAttachedStorage() {
        if (world == null) return null;
        Direction facing = getCachedState().get(NexusNodeBlock.FACING);
        BlockPos attachedPos = pos.offset(facing.getOpposite());
        return ItemStorage.SIDED.find(world, attachedPos, facing);
    }

    public BlockPos getAttachedBlockPos() {
        Direction facing = getCachedState().get(NexusNodeBlock.FACING);
        return pos.offset(facing.getOpposite());
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        NbtList list = new NbtList();
        for (BlockPos connection : connections) {
            list.add(NbtHelper.fromBlockPos(connection));
        }
        nbt.put("Connections", list);

        if (!filterStack.isEmpty()) {
            nbt.put("FilterStack", filterStack.encode(registries));
        }

        nbt.putString("Mode", mode.name());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        connections.clear();
        NbtList list = nbt.getList("Connections", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtHelper.toBlockPos(list.getCompound(i), "Connections").ifPresent(connections::add);
        }

        if (nbt.contains("FilterStack")) {
            filterStack = ItemStack.fromNbt(registries, nbt.getCompound("FilterStack")).orElse(ItemStack.EMPTY);
        } else {
            filterStack = ItemStack.EMPTY;
        }

        if (nbt.contains("Mode")) {
            try {
                mode = NodeMode.valueOf(nbt.getString("Mode"));
            } catch (IllegalArgumentException e) {
                mode = NodeMode.BALANCED;
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registries);
        return nbt;
    }
}
