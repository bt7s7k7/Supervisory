package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import javax.annotation.Nonnull;

import bt7s7k7.supervisory.Supervisory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RemoteTerminalUnitBlock extends Block {
	public RemoteTerminalUnitBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		Supervisory.LOGGER.info("RemoteTerminalUnitBlock");
	}

	@Override
	protected void createBlockStateDefinition(@Nonnull Builder<Block, BlockState> builder) {
		Supervisory.LOGGER.info("createBlockStateDefinition");
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
		return this.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}
}
