package com.omnica.common.network;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.packet.NodeSyncPayload;
import com.omnica.common.packet.TetherTransferPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World-level logistics engine for OMNICA.
 * Executes item transfers mathematically via cached arrays, replacing thousands of hopper entity ticks.
 */
public class OmnicaNetworkManager {
    private static final Map<ServerWorld, OmnicaNetworkManager> MANAGERS = new ConcurrentHashMap<>();
    private static final int TRANSFER_INTERVAL = 8; // Process transfers every 8 ticks (2.5 times per second)

    private final ServerWorld world;
    private final OmnicaGraph graph = new OmnicaGraph();
    private long tickCounter = 0;

    private OmnicaNetworkManager(ServerWorld world) {
        this.world = world;
    }

    public static OmnicaNetworkManager get(ServerWorld world) {
        return MANAGERS.computeIfAbsent(world, OmnicaNetworkManager::new);
    }

    public OmnicaGraph getGraph() {
        return graph;
    }

    public void registerNode(BlockPos pos) {
        graph.addNode(pos);
        if (world.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            for (BlockPos connected : nodeEntity.getConnections()) {
                graph.link(pos, connected);
            }
        }
        syncNodeToClients(pos);
    }

    public void unregisterNode(BlockPos pos) {
        graph.removeNode(pos);
        syncNodeToClients(pos);
    }

    public void syncNodeToClients(BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            List<BlockPos> connectionsList = new ArrayList<>(nodeEntity.getConnections());
            NodeSyncPayload payload = new NodeSyncPayload(pos, connectionsList);
            for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /**
     * Executes the centralized, zero-lag batch transfer pipeline.
     */
    public void tick(ServerWorld world) {
        tickCounter++;
        if (tickCounter % TRANSFER_INTERVAL != 0) {
            return;
        }

        for (BlockPos sourcePos : graph.getAllNodes()) {
            if (!world.isChunkLoaded(sourcePos)) continue;

            if (!(world.getBlockEntity(sourcePos) instanceof NexusNodeBlockEntity sourceNode)) {
                continue;
            }

            // Inserter-only nodes do not extract items
            if (sourceNode.getMode() == NexusNodeBlockEntity.NodeMode.INSERTER) {
                continue;
            }

            Storage<ItemVariant> sourceStorage = sourceNode.getAttachedStorage();
            if (sourceStorage == null || !sourceStorage.supportsExtraction()) {
                continue;
            }

            // Find an eligible item to extract
            for (StorageView<ItemVariant> view : sourceStorage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;

                ItemVariant variant = view.getResource();
                ItemStack candidateStack = variant.toStack((int) Math.min(view.getAmount(), 64));

                if (!sourceNode.allowsItem(candidateStack)) {
                    continue;
                }

                // Attempt to route to connected target nodes
                for (BlockPos targetPos : sourceNode.getConnections()) {
                    if (!world.isChunkLoaded(targetPos)) continue;

                    if (!(world.getBlockEntity(targetPos) instanceof NexusNodeBlockEntity targetNode)) {
                        continue;
                    }

                    // Extractor-only nodes do not receive items
                    if (targetNode.getMode() == NexusNodeBlockEntity.NodeMode.EXTRACTOR) {
                        continue;
                    }

                    if (!targetNode.allowsItem(candidateStack)) {
                        continue;
                    }

                    Storage<ItemVariant> targetStorage = targetNode.getAttachedStorage();
                    if (targetStorage == null || !targetStorage.supportsInsertion()) {
                        continue;
                    }

                    // Atomic transaction transfer
                    try (Transaction transaction = Transaction.openOuter()) {
                        long maxTransfer = Math.min(view.getAmount(), variant.getItem().getMaxCount());
                        long extracted = sourceStorage.extract(variant, maxTransfer, transaction);

                        if (extracted > 0) {
                            long inserted = targetStorage.insert(variant, extracted, transaction);
                            if (inserted > 0) {
                                transaction.commit();

                                // Notify nearby clients to render the laser tether animation
                                ItemStack transferredStack = variant.toStack((int) inserted);
                                int travelTicks = (int) Math.max(5, Math.sqrt(sourcePos.getSquaredDistance(targetPos)) * 1.5);
                                TetherTransferPayload payload = new TetherTransferPayload(sourcePos, targetPos, transferredStack, travelTicks);

                                for (ServerPlayerEntity player : PlayerLookup.tracking(world, sourcePos)) {
                                    ServerPlayNetworking.send(player, payload);
                                }

                                world.playSound(null, sourcePos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.4f, 2.0f);
                                break; // Only one transfer per node per cycle
                            }
                        }
                    }
                }
            }
        }
    }
}
