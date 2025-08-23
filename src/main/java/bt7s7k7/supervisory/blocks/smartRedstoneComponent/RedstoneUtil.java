package bt7s7k7.supervisory.blocks.smartRedstoneComponent;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

// This code is based on the implementation of DiodeBlock
public class RedstoneUtil {
	public static int getInputSignal(BlockPos position, Level level, Direction direction) {
		var targetPosition = position.relative(direction);
		var i = level.getSignal(targetPosition, direction);

		if (i >= 15) {
			return i;
		}

		var state = level.getBlockState(targetPosition);
		if (state.is(Blocks.REDSTONE_WIRE)) {
			return Math.max(i, state.getValue(RedStoneWireBlock.POWER));
		}

		return i;
	}

	public static void sendOutputSignal(BlockPos pos, BlockState state, Level level, Direction direction, int strength) {
		BlockPos targetPosition = pos.relative(direction);

		if (EventHooks.onNeighborNotify(level, pos, state, EnumSet.of(direction), false).isCanceled()) {
			return;
		}

		level.neighborChanged(targetPosition, state.getBlock(), pos);
		level.updateNeighborsAt(targetPosition, state.getBlock());
	}
}
