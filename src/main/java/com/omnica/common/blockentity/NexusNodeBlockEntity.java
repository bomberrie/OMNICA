package com.omnica.common.blockentity;

import com.omnica.Omnica;
import com.omnica.common.block.NexusNodeBlock;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        double maxDist = com.omnica.common.config.OmnicaConfig.getInstance().maxLinkDistance;
        if (target.equals(worldPosition) || !worldPosition.closerThan(target, maxDist + 0.5)) {
            return false;
        }
        boolean added = connections.add(target);
        if (added) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        return added;
    }

    public boolean removeConnection(BlockPos target) {
        boolean removed = connections.remove(target);
        if (removed) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        return removed;
    }

    public void clearConnections() {
        connections.clear();
        setChanged();
    }

    public ItemStack getFilterStack() {
        return filterStack;
    }

    public void setFilterStack(ItemStack stack) {
        this.filterStack = stack.copy();
        setChanged();
    }

    public NodeMode getMode() {
        return mode;
    }

    public void setMode(NodeMode mode) {
        this.mode = mode;
        setChanged();
    }

    public boolean allowsItem(ItemStack candidate) {
        if (filterStack.isEmpty()) {
            return true;
        }

        BundleContents bundleContents = filterStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            return bundleContents.itemCopies().anyMatch(filterItem -> ItemStack.isSameItem(filterItem, candidate));
        }

        return ItemStack.isSameItem(filterStack, candidate);
    }

    @Nullable
    public Storage<ItemVariant> getAttachedStorage() {
        if (level == null) return null;
        Direction facing = getBlockState().getValue(NexusNodeBlock.FACING);
        BlockPos attachedPos = worldPosition.relative(facing.getOpposite());
        return ItemStorage.SIDED.find(level, attachedPos, facing);
    }

    public BlockPos getAttachedBlockPos() {
        Direction facing = getBlockState().getValue(NexusNodeBlock.FACING);
        return worldPosition.relative(facing.getOpposite());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var list = output.list("Connections", BlockPos.CODEC);
        for (BlockPos connection : connections) {
            list.add(connection);
        }

        if (!filterStack.isEmpty()) {
            output.store("FilterStack", ItemStack.OPTIONAL_CODEC, filterStack);
        }

        output.putString("Mode", mode.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        connections.clear();
        for (BlockPos pos : input.listOrEmpty("Connections", BlockPos.CODEC)) {
            connections.add(pos);
        }

        filterStack = input.read("FilterStack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);

        try {
            mode = NodeMode.valueOf(input.getStringOr("Mode", "BALANCED"));
        } catch (IllegalArgumentException e) {
            mode = NodeMode.BALANCED;
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
