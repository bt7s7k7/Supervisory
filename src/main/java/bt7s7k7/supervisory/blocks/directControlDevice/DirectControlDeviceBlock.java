package bt7s7k7.supervisory.blocks.directControlDevice;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import bt7s7k7.supervisory.blocks.smartRedstoneComponent.SmartRedstoneComponentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class DirectControlDeviceBlock<T extends DirectControlDeviceBlockEntity> extends SmartRedstoneComponentBlock implements EntityBlock {
	public final BlockEntityEntry<T> blockEntityType;

	public DirectControlDeviceBlock(BlockEntityEntry<T> blockEntityType, Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		this.blockEntityType = blockEntityType;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return this.blockEntityType.create(pos, state);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <TEntity extends BlockEntity> BlockEntityTicker<TEntity> getTicker(Level level, BlockState state, BlockEntityType<TEntity> type) {
		if (level.isClientSide() || type != this.blockEntityType.get()) return null;

		return (BlockEntityTicker<TEntity>) (level_1, pos, state_1, blockEntity) -> {
			((T) blockEntity).tick(level_1, pos, state_1);
		};
	}
}
