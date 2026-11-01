package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record TerminalRequestPayload(
        BlockPos terminalPos,
        ItemStack item,
        int count
) implements CustomPacketPayload {
    public static final Type<TerminalRequestPayload> TYPE = new Type<>(Omnica.id("terminal_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalRequestPayload::terminalPos,
            ItemStack.OPTIONAL_STREAM_CODEC, TerminalRequestPayload::item,
            ByteBufCodecs.VAR_INT, TerminalRequestPayload::count,
            TerminalRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
