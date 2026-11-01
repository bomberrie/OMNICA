package com.omnica.common.block;

import com.omnica.Omnica;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.network.OmnicaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NexusNodeBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    protected static final VoxelShape UP_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 8.0, 14.0);
    protected static final VoxelShape DOWN_SHAPE = Block.box(2.0, 8.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape NORTH_SHAPE = Block.box(2.0, 2.0, 8.0, 14.0, 14.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 8.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(8.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 2.0, 2.0, 8.0, 14.0, 14.0);

    public NexusNodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusNodeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            OmnicaNetworkManager.get(serverLevel).registerNode(pos);
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            OmnicaNetworkManager.get(serverLevel).unregisterNode(pos);
        }
        super.destroy(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        OmnicaNetworkManager.get(level).unregisterNode(pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());
        BlockState attachedState = level.getBlockState(attachedPos);
        return !attachedState.isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Omnica.NETWORK_LINKER)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (level.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    NexusNodeBlockEntity.NodeMode nextMode = switch (nodeEntity.getMode()) {
                        case BALANCED -> NexusNodeBlockEntity.NodeMode.EXTRACTOR;
                        case EXTRACTOR -> NexusNodeBlockEntity.NodeMode.INSERTER;
                        case INSERTER -> NexusNodeBlockEntity.NodeMode.BALANCED;
                    };
                    nodeEntity.setMode(nextMode);
                    player.sendOverlayMessage(Component.literal("Mode set to: " + nextMode.name()));
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.4f);
                }
                return InteractionResult.SUCCESS;
            } else if (!stack.isEmpty()) {
                if (!level.isClientSide()) {
                    nodeEntity.setFilterStack(stack.copyWithCount(1));
                    player.sendOverlayMessage(Component.literal("Filter set to: ").append(stack.getHoverName()));
                    level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0f, 1.2f);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof NexusNodeBlockEntity nodeEntity) {
            if (!level.isClientSide()) {
                ItemStack currentFilter = nodeEntity.getFilterStack();
                String filterName = currentFilter.isEmpty() ? "None" : currentFilter.getHoverName().getString();
                player.sendOverlayMessage(Component.literal(String.format("Node [%s] | Connections: %d | Filter: %s",
                        nodeEntity.getMode().name(),
                        nodeEntity.getConnections().size(),
                        filterName)));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
