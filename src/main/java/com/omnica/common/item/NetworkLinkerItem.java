package com.omnica.common.item;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class NetworkLinkerItem extends Item {
    public NetworkLinkerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (world.getBlockEntity(clickedPos) instanceof NexusNodeBlockEntity targetNode) {
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = customData.copyNbt();

            if (nbt.contains("SelectedPos", NbtElement.INT_ARRAY_TYPE)) {
                Optional<BlockPos> selectedPosOpt = NbtHelper.toBlockPos(nbt, "SelectedPos");
                if (selectedPosOpt.isPresent()) {
                    BlockPos selectedPos = selectedPosOpt.get();

                    if (selectedPos.equals(clickedPos)) {
                        nbt.remove("SelectedPos");
                        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                        if (player != null) {
                            player.sendMessage(Text.translatable("message.omnica.link_cleared").formatted(Formatting.YELLOW), true);
                            world.playSound(null, clickedPos, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 1.0f);
                        }
                        return ActionResult.SUCCESS;
                    }

                    if (!selectedPos.isWithinDistance(clickedPos, 32.5)) {
                        if (player != null) {
                            player.sendMessage(Text.translatable("message.omnica.out_of_range").formatted(Formatting.RED), true);
                        }
                        return ActionResult.FAIL;
                    }

                    if (world.getBlockEntity(selectedPos) instanceof NexusNodeBlockEntity sourceNode) {
                        boolean linked = sourceNode.addConnection(clickedPos);
                        targetNode.addConnection(selectedPos);

                        if (player != null) {
                            player.sendMessage(Text.translatable("message.omnica.node_linked",
                                    selectedPos.getX(), selectedPos.getY(), selectedPos.getZ(),
                                    clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()).formatted(Formatting.GREEN), true);
                            world.playSound(null, clickedPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 1.6f);
                        }

                        nbt.remove("SelectedPos");
                        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                        return ActionResult.SUCCESS;
                    }
                }
            }

            // Save the first selected node
            nbt.put("SelectedPos", NbtHelper.fromBlockPos(clickedPos));
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            if (player != null) {
                player.sendMessage(Text.literal(String.format("Selected Nexus Node at [%d, %d, %d]. Right-click target node to link.",
                        clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())).formatted(Formatting.AQUA), true);
                world.playSound(null, clickedPos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking()) {
            NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("SelectedPos")) {
                nbt.remove("SelectedPos");
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                if (!world.isClient()) {
                    user.sendMessage(Text.translatable("message.omnica.link_cleared").formatted(Formatting.YELLOW), true);
                }
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }
}
