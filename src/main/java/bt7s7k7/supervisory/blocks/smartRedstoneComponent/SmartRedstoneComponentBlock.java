package bt7s7k7.supervisory.blocks.smartRedstoneComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SmartRedstoneComponentBlock extends Block {
	public SmartRedstoneComponentBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (level.getBlockEntity(pos) instanceof SmartRedstoneComponentBlockEntity blockEntity) {
			return blockEntity.getOutput(direction.getOpposite());
		}

		return super.getDirectSignal(state, level, pos, direction);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.getDirectSignal(state, level, pos, direction);
	}

	@Override
	public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
		return false;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof SmartRedstoneComponentBlockEntity blockEntity) {
			blockEntity.handleNeighbourChange(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		}
	}

}
