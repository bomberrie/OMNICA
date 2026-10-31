package com.omnica.common.packet;

import com.omnica.Omnica;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record TerminalSyncPayload(
        List<ItemStack> availableItems
) implements CustomPayload {
    public static final Id<TerminalSyncPayload> ID = new Id<>(Omnica.id("terminal_sync"));

    public static final PacketCodec<RegistryByteBuf, TerminalSyncPayload> CODEC = PacketCodec.tuple(
            ItemStack.PACKET_CODEC.collect(PacketCodecs.toList()), TerminalSyncPayload::availableItems,
            TerminalSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
