package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record TerminalRequestPayload(
        BlockPos terminalPos,
        ItemStack item,
        int count
) implements CustomPayload {
    public static final Id<TerminalRequestPayload> ID = new Id<>(Omnica.id("terminal_request"));

    public static final PacketCodec<RegistryByteBuf, TerminalRequestPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, TerminalRequestPayload::terminalPos,
            ItemStack.PACKET_CODEC, TerminalRequestPayload::item,
            PacketCodecs.VAR_INT, TerminalRequestPayload::count,
            TerminalRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
