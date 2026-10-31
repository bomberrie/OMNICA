package com.omnica;

import com.omnica.common.block.NexusNodeBlock;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.item.NetworkLinkerItem;
import com.omnica.common.network.OmnicaNetworkManager;
import com.omnica.common.packet.NodeSyncPayload;
import com.omnica.common.packet.TetherTransferPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Omnica implements ModInitializer {
    public static final String MOD_ID = "omnica";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    // Blocks
    public static final Block NEXUS_NODE = Registry.register(
            Registries.BLOCK,
            id("nexus_node"),
            new NexusNodeBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)
                    .strength(2.0f, 6.0f)
                    .sounds(BlockSoundGroup.COPPER)
                    .nonOpaque())
    );

    // Items
    public static final Item NEXUS_NODE_ITEM = Registry.register(
            Registries.ITEM,
            id("nexus_node"),
            new BlockItem(NEXUS_NODE, new Item.Settings())
    );

    public static final Item NETWORK_LINKER = Registry.register(
            Registries.ITEM,
            id("network_linker"),
            new NetworkLinkerItem(new Item.Settings().maxCount(1))
    );

    // Block Entities
    public static final BlockEntityType<NexusNodeBlockEntity> NEXUS_NODE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("nexus_node"),
            FabricBlockEntityTypeBuilder.create(NexusNodeBlockEntity::new, NEXUS_NODE).build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[OMNICA] Initializing Vanilla-First Logistics Network...");

        // Register Creative Tab Entries
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(NEXUS_NODE_ITEM);
            entries.add(NETWORK_LINKER);
        });

        // Register Networking Payloads
        PayloadTypeRegistry.playS2C().register(TetherTransferPayload.ID, TetherTransferPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NodeSyncPayload.ID, NodeSyncPayload.CODEC);

        // Register Server Tick Event for Zero-Lag Centralized Transfers
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            OmnicaNetworkManager.get(world).tick(world);
        });

        LOGGER.info("[OMNICA] Core subsystems ready.");
    }
}
