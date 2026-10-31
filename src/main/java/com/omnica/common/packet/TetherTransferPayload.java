package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record TetherTransferPayload(
        BlockPos from,
        BlockPos to,
        ItemStack item,
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<TetherTransferPayload> TYPE = new Type<>(Omnica.id("tether_transfer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TetherTransferPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TetherTransferPayload::from,
            BlockPos.STREAM_CODEC, TetherTransferPayload::to,
            ItemStack.OPTIONAL_STREAM_CODEC, TetherTransferPayload::item,
            ByteBufCodecs.VAR_INT, TetherTransferPayload::durationTicks,
            TetherTransferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
