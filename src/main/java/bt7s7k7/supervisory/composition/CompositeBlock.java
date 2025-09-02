package bt7s7k7.supervisory.composition;

import java.util.Optional;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.blocks.programmableLogicController.support.LogEventRouter;
import bt7s7k7.supervisory.configuration.ConfigurationManager;
import bt7s7k7.supervisory.redstone.RedstoneStateComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public abstract class CompositeBlock extends Block implements EntityBlock {
	public final BlockEntityEntry<CompositeBlockEntity> blockEntityType;

	public CompositeBlock(BlockEntityEntry<CompositeBlockEntity> blockEntityType, Properties properties) {
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

	@Override
	public <TEntity extends BlockEntity> BlockEntityTicker<TEntity> getTicker(Level level, BlockState state, BlockEntityType<TEntity> type) {
		if (level.isClientSide() || type != this.blockEntityType.get()) return null;

		return (BlockEntityTicker<TEntity>) (level_1, pos, state_1, blockEntity) -> {
			((CompositeBlockEntity) blockEntity).tick();
		};
	}

	private Optional<RedstoneStateComponent> getRedstoneState(BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof CompositeBlockEntity blockEntity) {
			return blockEntity.getComponent(RedstoneStateComponent.class);
		}

		return Optional.empty();
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		var redstone = getRedstoneState(level, pos);
		if (redstone.isPresent()) return redstone.get().getOutput(direction.getOpposite());

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
		if (level.getBlockEntity(pos) instanceof CompositeBlockEntity blockEntity) {
			blockEntity.handleNeighbourChange(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			level.getBlockEntity(pos, this.blockEntityType.get()).ifPresent(be -> {
				if (be.failed) {
					serverPlayer.sendSystemMessage(I18n.PROGRAMMABLE_LOGIC_CONTROLLER_FAILED.toComponent(), true);
					return;
				}

				LogEventRouter.getInstance().subscribePlayer(serverPlayer, level, pos);
				ConfigurationManager.requestConfiguration(serverPlayer, pos, be);
			});
		}

		return ItemInteractionResult.SUCCESS;
	}
}
