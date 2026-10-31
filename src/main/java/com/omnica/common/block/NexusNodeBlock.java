package com.omnica.common.block;

import com.mojang.serialization.MapCodec;
import com.omnica.Omnica;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.network.OmnicaNetworkManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NexusNodeBlock extends BlockWithEntity {
    public static final MapCodec<NexusNodeBlock> CODEC = createCodec(NexusNodeBlock::new);
    public static final DirectionProperty FACING = FacingBlock.FACING;

    protected static final VoxelShape UP_SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 8.0, 14.0);
    protected static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(2.0, 8.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(2.0, 2.0, 8.0, 14.0, 14.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(2.0, 2.0, 0.0, 14.0, 14.0, 8.0);
    protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(8.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0.0, 2.0, 2.0, 8.0, 14.0, 14.0);

    public NexusNodeBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NexusNodeBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            OmnicaNetworkManager.get(serverWorld).registerNode(pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                OmnicaNetworkManager.get(serverWorld).unregisterNode(pos);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isOf(Omnica.NETWORK_LINKER)) {
            // Let the linker item handle node connections
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (world.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            if (player.isSneaking()) {
                if (!world.isClient()) {
                    // Toggle mode between EXTRACTOR, INSERTER, BALANCED
                    NexusNodeBlockEntity.NodeMode nextMode = switch (nodeEntity.getMode()) {
                        case BALANCED -> NexusNodeBlockEntity.NodeMode.EXTRACTOR;
                        case EXTRACTOR -> NexusNodeBlockEntity.NodeMode.INSERTER;
                        case INSERTER -> NexusNodeBlockEntity.NodeMode.BALANCED;
                    };
                    nodeEntity.setMode(nextMode);
                    player.sendMessage(Text.literal("Mode set to: " + nextMode.name()), true);
                    world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.4f);
                }
                return ItemActionResult.SUCCESS;
            } else if (!stack.isEmpty()) {
                if (!world.isClient()) {
                    nodeEntity.setFilterStack(stack.copyWithCount(1));
                    player.sendMessage(Text.literal("Filter set to: " + stack.getName().getString()), true);
                    world.playSound(null, pos, SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.BLOCKS, 1.0f, 1.2f);
                }
                return ItemActionResult.SUCCESS;
            }
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            if (!world.isClient()) {
                ItemStack currentFilter = nodeEntity.getFilterStack();
                String filterName = currentFilter.isEmpty() ? "None" : currentFilter.getName().getString();
                player.sendMessage(Text.literal(String.format("Node [%s] | Connections: %d | Filter: %s",
                        nodeEntity.getMode().name(),
                        nodeEntity.getConnections().size(),
                        filterName)), false);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
