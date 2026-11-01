package com.omnica;

import com.omnica.common.block.NexusNodeBlock;
import com.omnica.common.block.RequestTerminalBlock;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.blockentity.RequestTerminalBlockEntity;
import com.omnica.common.item.NetworkLinkerItem;
import com.omnica.common.network.OmnicaNetworkManager;
import com.omnica.common.packet.NodeSyncPayload;
import com.omnica.common.packet.TerminalRequestPayload;
import com.omnica.common.packet.TerminalSyncPayload;
import com.omnica.common.packet.TetherTransferPayload;
import com.omnica.common.screen.RequestTerminalScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class Omnica implements ModInitializer {
    public static final String MOD_ID = "omnica";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Resource Keys
    public static final ResourceKey<Block> NEXUS_NODE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("nexus_node"));
    public static final ResourceKey<Item> NEXUS_NODE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("nexus_node"));

    public static final ResourceKey<Block> REQUEST_TERMINAL_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("request_terminal"));
    public static final ResourceKey<Item> REQUEST_TERMINAL_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("request_terminal"));

    public static final ResourceKey<Item> NETWORK_LINKER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("network_linker"));

    // Blocks
    public static final Block NEXUS_NODE = Registry.register(
            BuiltInRegistries.BLOCK,
            NEXUS_NODE_BLOCK_KEY,
            new NexusNodeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .setId(NEXUS_NODE_BLOCK_KEY)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion())
    );

    public static final Block REQUEST_TERMINAL = Registry.register(
            BuiltInRegistries.BLOCK,
            REQUEST_TERMINAL_BLOCK_KEY,
            new RequestTerminalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .setId(REQUEST_TERMINAL_BLOCK_KEY)
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.COPPER))
    );

    // Items
    public static final Item NEXUS_NODE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NEXUS_NODE_ITEM_KEY,
            new BlockItem(NEXUS_NODE, new Item.Properties()
                    .setId(NEXUS_NODE_ITEM_KEY)
                    .useBlockDescriptionPrefix())
    );

    public static final Item REQUEST_TERMINAL_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            REQUEST_TERMINAL_ITEM_KEY,
            new BlockItem(REQUEST_TERMINAL, new Item.Properties()
                    .setId(REQUEST_TERMINAL_ITEM_KEY)
                    .useBlockDescriptionPrefix())
    );

    public static final Item NETWORK_LINKER = Registry.register(
            BuiltInRegistries.ITEM,
            NETWORK_LINKER_ITEM_KEY,
            new NetworkLinkerItem(new Item.Properties()
                    .setId(NETWORK_LINKER_ITEM_KEY)
                    .stacksTo(1))
    );

    // Block Entities
    public static final BlockEntityType<NexusNodeBlockEntity> NEXUS_NODE_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("nexus_node"),
            new BlockEntityType<>(NexusNodeBlockEntity::new, Set.of(NEXUS_NODE))
    );

    public static final BlockEntityType<RequestTerminalBlockEntity> REQUEST_TERMINAL_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("request_terminal"),
            new BlockEntityType<>(RequestTerminalBlockEntity::new, Set.of(REQUEST_TERMINAL))
    );

    // Menu Types
    public static final MenuType<RequestTerminalScreenHandler> REQUEST_TERMINAL_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            id("request_terminal"),
            new ExtendedMenuType<>(RequestTerminalScreenHandler::new, BlockPos.STREAM_CODEC)
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[OMNICA] Initializing Vanilla-First Logistics Network for Minecraft 26.3...");

        // Load configuration
        com.omnica.common.config.OmnicaConfig.load();

        // Register Creative Tab Entries
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.parse("redstone_blocks")))
                .register(entries -> {
                    entries.accept(NEXUS_NODE_ITEM);
                    entries.accept(REQUEST_TERMINAL_ITEM);
                    entries.accept(NETWORK_LINKER);
                });

        // Register Networking Payloads
        PayloadTypeRegistry.clientboundPlay().register(TetherTransferPayload.TYPE, TetherTransferPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NodeSyncPayload.TYPE, NodeSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TerminalRequestPayload.TYPE, TerminalRequestPayload.STREAM_CODEC);

        // Handle Terminal Requests from Clients
        ServerPlayNetworking.registerGlobalReceiver(TerminalRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().level().getBlockEntity(payload.terminalPos()) instanceof RequestTerminalBlockEntity terminal) {
                    if (payload.count() > 0 && !payload.item().isEmpty()) {
                        int fulfilled = terminal.fulfillRequest(payload.item(), payload.count());
                        if (fulfilled > 0) {
                            ItemStack delivered = payload.item().copyWithCount(fulfilled);
                            context.player().addItem(delivered);
                            context.player().level().playSound(null, payload.terminalPos(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                        }
                    }
                    // Sync refreshed network stock back to player
                    List<ItemStack> updatedStock = terminal.getAggregatedNetworkItems();
                    ServerPlayNetworking.send(context.player(), new TerminalSyncPayload(updatedStock));
                }
            });
        });

        // Register Server Tick Event for Zero-Lag Centralized Transfers
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            OmnicaNetworkManager.get(level).tick(level);
        });

        LOGGER.info("[OMNICA] Core subsystems ready.");
    }
}
