package com.omnica.common.network;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.blockentity.RequestTerminalBlockEntity;
import com.omnica.common.config.OmnicaConfig;
import com.omnica.common.packet.NodeSyncPayload;
import com.omnica.common.packet.TetherTransferPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World-level logistics engine for OMNICA.
 * Executes item transfers mathematically via cached arrays, replacing thousands of hopper entity ticks.
 */
public class OmnicaNetworkManager {
    private static final Map<ServerLevel, OmnicaNetworkManager> MANAGERS = new ConcurrentHashMap<>();
    private static final int TRANSFER_INTERVAL = 8; // Process transfers every 8 ticks (2.5 times per second)

    private final ServerLevel level;
    private final OmnicaGraph graph = new OmnicaGraph();
    private long tickCounter = 0;

    private OmnicaNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static OmnicaNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, OmnicaNetworkManager::new);
    }

    public OmnicaGraph getGraph() {
        return graph;
    }

    public void registerNode(BlockPos pos) {
        graph.addNode(pos);
        if (level.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            for (BlockPos connected : nodeEntity.getConnections()) {
                graph.link(pos, connected);
            }
        }
        syncNodeToClients(pos);
    }

    public void unregisterNode(BlockPos pos) {
        Set<BlockPos> neighbors = new HashSet<>(graph.getNeighbors(pos));
        if (level.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            neighbors.addAll(nodeEntity.getConnections());
            nodeEntity.clearConnections();
        }
        graph.removeNode(pos);

        for (BlockPos neighbor : neighbors) {
            if (level.getBlockEntity(neighbor) instanceof NexusNodeBlockEntity neighborNode) {
                neighborNode.removeConnection(pos);
                syncNodeToClients(neighbor);
            } else if (level.getBlockEntity(neighbor) instanceof RequestTerminalBlockEntity neighborTerm) {
                neighborTerm.removeConnection(pos);
            }
        }

        NodeSyncPayload emptyPayload = new NodeSyncPayload(pos, Collections.emptyList());
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            ServerPlayNetworking.send(player, emptyPayload);
        }
    }

    public void syncNodeToClients(BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            List<BlockPos> connectionsList = new ArrayList<>(nodeEntity.getConnections());
            NodeSyncPayload payload = new NodeSyncPayload(pos, connectionsList);
            for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /**
     * Executes the centralized, zero-lag batch transfer pipeline.
     */
    public void tick(ServerLevel level) {
        tickCounter++;
        int interval = Math.max(1, OmnicaConfig.getInstance().transferIntervalTicks);
        if (tickCounter % interval != 0) {
            return;
        }

        List<BlockPos> deadNodes = new ArrayList<>();
        for (BlockPos sourcePos : graph.getAllNodes()) {
            if (!level.isLoaded(sourcePos)) continue;

            if (!(level.getBlockEntity(sourcePos) instanceof NexusNodeBlockEntity sourceNode)) {
                deadNodes.add(sourcePos);
                continue;
            }

            // Prune any stale connections pointing to destroyed blocks
            List<BlockPos> staleTargets = new ArrayList<>();
            for (BlockPos targetPos : sourceNode.getConnections()) {
                if (level.isLoaded(targetPos)) {
                    var targetBe = level.getBlockEntity(targetPos);
                    if (!(targetBe instanceof NexusNodeBlockEntity || targetBe instanceof RequestTerminalBlockEntity)) {
                        staleTargets.add(targetPos);
                    }
                }
            }
            if (!staleTargets.isEmpty()) {
                for (BlockPos stale : staleTargets) {
                    sourceNode.removeConnection(stale);
                }
                syncNodeToClients(sourcePos);
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
                    if (!level.isLoaded(targetPos)) continue;

                    if (!(level.getBlockEntity(targetPos) instanceof NexusNodeBlockEntity targetNode)) {
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
                        long maxTransfer = Math.min(view.getAmount(), Math.min(variant.getItem().getDefaultMaxStackSize(), OmnicaConfig.getInstance().itemsPerTransfer));
                        long extracted = sourceStorage.extract(variant, maxTransfer, transaction);

                        if (extracted > 0) {
                            long inserted = targetStorage.insert(variant, extracted, transaction);
                            if (inserted > 0) {
                                transaction.commit();

                                // Notify nearby clients to render the laser tether animation
                                ItemStack transferredStack = variant.toStack((int) inserted);
                                int travelTicks = (int) Math.max(5, Math.sqrt(sourcePos.distSqr(targetPos)) * 1.5);
                                TetherTransferPayload payload = new TetherTransferPayload(sourcePos, targetPos, transferredStack, travelTicks);

                                Set<ServerPlayer> nearbyPlayers = new HashSet<>(PlayerLookup.tracking(level, sourcePos));
                                nearbyPlayers.addAll(PlayerLookup.tracking(level, targetPos));
                                for (ServerPlayer player : nearbyPlayers) {
                                    ServerPlayNetworking.send(player, payload);
                                }

                                if (OmnicaConfig.getInstance().enableSoundEffects) {
                                    level.playSound(null, sourcePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 2.0f);
                                }
                                break; // Only one transfer per node per cycle
                            }
                        }
                    }
                }
            }
        }

        for (BlockPos dead : deadNodes) {
            unregisterNode(dead);
        }
    }
}
