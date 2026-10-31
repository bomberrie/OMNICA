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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.BlockPos;

public class OmnicaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Omnica.LOGGER.info("[OMNICA] Initializing Client Tether & Instanced Rendering Engine...");

        // Register Block Entity Renderer for Nexus Nodes
        BlockEntityRendererFactories.register(Omnica.NEXUS_NODE_BLOCK_ENTITY, NexusNodeRenderer::new);

        // Register Handled Screens
        HandledScreens.register(Omnica.REQUEST_TERMINAL_SCREEN_HANDLER, RequestTerminalScreen::new);

        // Register In-Flight Item World Rendering
        WorldRenderEvents.AFTER_ENTITIES.register(TetherRenderer::renderWorld);

        // Handle In-Flight Item Packets
        ClientPlayNetworking.registerGlobalReceiver(TetherTransferPayload.ID, (payload, context) -> {
            context.client().execute(() -> TetherRenderer.addTransfer(payload));
        });

        // Handle Terminal Stock Updates
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null && context.client().player.currentScreenHandler instanceof RequestTerminalScreenHandler handler) {
                    handler.setClientNetworkItems(payload.availableItems());
                }
            });
        });

        // Handle Node Connection Updates
        ClientPlayNetworking.registerGlobalReceiver(NodeSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().world != null) {
                    if (context.client().world.getBlockEntity(payload.pos()) instanceof NexusNodeBlockEntity nodeEntity) {
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
