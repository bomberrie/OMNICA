package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record TetherTransferPayload(
        BlockPos from,
        BlockPos to,
        ItemStack item,
        int durationTicks
) implements CustomPayload {
    public static final Id<TetherTransferPayload> ID = new Id<>(Omnica.id("tether_transfer"));

    public static final PacketCodec<RegistryByteBuf, TetherTransferPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, TetherTransferPayload::from,
            BlockPos.PACKET_CODEC, TetherTransferPayload::to,
            ItemStack.OPTIONAL_PACKET_CODEC, TetherTransferPayload::item,
            net.minecraft.network.codec.PacketCodecs.VAR_INT, TetherTransferPayload::durationTicks,
            TetherTransferPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
