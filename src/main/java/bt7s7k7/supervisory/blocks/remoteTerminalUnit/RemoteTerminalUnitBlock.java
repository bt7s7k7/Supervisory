package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.configuration.ConfigurationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;

public class RemoteTerminalUnitBlock extends Block implements EntityBlock {
	public RemoteTerminalUnitBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return AllBlockEntities.REMOTE_TERMINAL_UNIT.create(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != AllBlockEntities.REMOTE_TERMINAL_UNIT.get()) return null;

		return (BlockEntityTicker<T>) (level_1, pos, state_1, blockEntity) -> {
			((RemoteTerminalUnitBlockEntity) blockEntity).tick(level_1, pos, state_1);
		};
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			level.getBlockEntity(pos, AllBlockEntities.REMOTE_TERMINAL_UNIT.get()).ifPresent(be -> {
				ConfigurationManager.requestConfiguration(serverPlayer, pos, be);
			});
		}

		return ItemInteractionResult.SUCCESS;
	}
}
