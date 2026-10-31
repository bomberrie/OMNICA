package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record NodeSyncPayload(
        BlockPos pos,
        List<BlockPos> connections
) implements CustomPayload {
    public static final Id<NodeSyncPayload> ID = new Id<>(Omnica.id("node_sync"));

    public static final PacketCodec<RegistryByteBuf, NodeSyncPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, NodeSyncPayload::pos,
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()), NodeSyncPayload::connections,
            NodeSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
