package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record NodeSyncPayload(
        BlockPos pos,
        List<BlockPos> connections
) implements CustomPacketPayload {
    public static final Type<NodeSyncPayload> TYPE = new Type<>(Omnica.id("node_sync"));

    public static final StreamCodec<FriendlyByteBuf, NodeSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeSyncPayload::pos,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), NodeSyncPayload::connections,
            NodeSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
