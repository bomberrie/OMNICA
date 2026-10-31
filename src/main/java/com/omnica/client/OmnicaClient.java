package com.omnica.client;

import com.omnica.Omnica;
import com.omnica.client.gui.RequestTerminalScreen;
import com.omnica.client.render.NexusNodeRenderer;
import com.omnica.client.render.TetherRenderer;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.packet.NodeSyncPayload;
import com.omnica.common.packet.TerminalSyncPayload;
import com.omnica.common.packet.TetherTransferPayload;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import com.omnica.mixin.MenuScreensAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.core.BlockPos;

public class OmnicaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Omnica.LOGGER.info("[OMNICA] Initializing Client Tether & Instanced Rendering Engine for Minecraft 26.3...");

        // Register Block Entity Renderer for Nexus Nodes
        BlockEntityRendererRegistry.register(Omnica.NEXUS_NODE_BLOCK_ENTITY, NexusNodeRenderer::new);

        // Register Handled Screens using Accessor Mixin
        MenuScreensAccessor.register(Omnica.REQUEST_TERMINAL_SCREEN_HANDLER, RequestTerminalScreen::new);

        // Register In-Flight Item World Rendering
        LevelRenderEvents.COLLECT_SUBMITS.register(TetherRenderer::renderWorld);

        // Handle In-Flight Item Packets
        ClientPlayNetworking.registerGlobalReceiver(TetherTransferPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> TetherRenderer.addTransfer(payload));
        });

        // Handle Terminal Stock Updates
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null && context.client().player.containerMenu instanceof RequestTerminalScreenHandler handler) {
                    handler.setClientNetworkItems(payload.availableItems());
                }
            });
        });

        // Handle Node Connection Updates
        ClientPlayNetworking.registerGlobalReceiver(NodeSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level != null) {
                    if (context.client().level.getBlockEntity(payload.pos()) instanceof NexusNodeBlockEntity nodeEntity) {
                        nodeEntity.clearConnections();
                        for (BlockPos target : payload.connections()) {
                            nodeEntity.addConnection(target);
                        }
                    }
                }
            });
        });

        Omnica.LOGGER.info("[OMNICA] Client rendering engine initialized.");
    }
}
