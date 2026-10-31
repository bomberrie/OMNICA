package com.omnica.common.item;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.blockentity.RequestTerminalBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NetworkLinkerItem extends Item {
    public NetworkLinkerItem(Properties properties) {
        super(properties);
    }

    private boolean isLinkable(BlockEntity be) {
        return be instanceof NexusNodeBlockEntity || be instanceof RequestTerminalBlockEntity;
    }

    private void linkNodes(BlockEntity source, BlockEntity target, BlockPos sourcePos, BlockPos targetPos) {
        if (source instanceof NexusNodeBlockEntity sNode) {
            sNode.addConnection(targetPos);
        } else if (source instanceof RequestTerminalBlockEntity sTerm) {
            sTerm.addConnection(targetPos);
        }

        if (target instanceof NexusNodeBlockEntity tNode) {
            tNode.addConnection(sourcePos);
        } else if (target instanceof RequestTerminalBlockEntity tTerm) {
            tTerm.addConnection(sourcePos);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockEntity clickedBe = level.getBlockEntity(clickedPos);

        if (isLinkable(clickedBe)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = customData.copyTag();

            if (nbt.contains("SelectedX")) {
                int x = nbt.getInt("SelectedX").orElse(0);
                int y = nbt.getInt("SelectedY").orElse(0);
                int z = nbt.getInt("SelectedZ").orElse(0);
                BlockPos selectedPos = new BlockPos(x, y, z);

                if (selectedPos.equals(clickedPos)) {
                    nbt.remove("SelectedX");
                    nbt.remove("SelectedY");
                    nbt.remove("SelectedZ");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    if (player != null) {
                        player.sendOverlayMessage(Component.translatable("message.omnica.link_cleared").withStyle(ChatFormatting.YELLOW));
                        level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (!selectedPos.closerThan(clickedPos, 32.5)) {
                    if (player != null) {
                        player.sendOverlayMessage(Component.translatable("message.omnica.out_of_range").withStyle(ChatFormatting.RED));
                    }
                    return InteractionResult.FAIL;
                }

                BlockEntity selectedBe = level.getBlockEntity(selectedPos);
                if (isLinkable(selectedBe)) {
                    linkNodes(selectedBe, clickedBe, selectedPos, clickedPos);

                    if (player != null) {
                        player.sendOverlayMessage(Component.translatable("message.omnica.node_linked",
                                selectedPos.getX(), selectedPos.getY(), selectedPos.getZ(),
                                clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()).withStyle(ChatFormatting.GREEN));
                        level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.6f);
                    }

                    nbt.remove("SelectedX");
                    nbt.remove("SelectedY");
                    nbt.remove("SelectedZ");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    return InteractionResult.SUCCESS;
                }
            }

            // Save first selected node
            nbt.putInt("SelectedX", clickedPos.getX());
            nbt.putInt("SelectedY", clickedPos.getY());
            nbt.putInt("SelectedZ", clickedPos.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

            if (player != null) {
                player.sendOverlayMessage(Component.literal(String.format("Selected Node at [%d, %d, %d]. Right-click target node to link.",
                        clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())).withStyle(ChatFormatting.AQUA));
                level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
