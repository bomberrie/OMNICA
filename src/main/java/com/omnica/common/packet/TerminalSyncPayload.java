package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record TerminalSyncPayload(
        List<ItemStack> availableItems
) implements CustomPacketPayload {
    public static final Type<TerminalSyncPayload> TYPE = new Type<>(Omnica.id("terminal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::availableItems,
            TerminalSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
