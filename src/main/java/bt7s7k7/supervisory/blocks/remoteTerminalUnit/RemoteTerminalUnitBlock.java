package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.blocks.directControlDevice.DirectControlDeviceBlock;
import bt7s7k7.supervisory.configuration.ConfigurationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RemoteTerminalUnitBlock extends DirectControlDeviceBlock<RemoteTerminalUnitBlockEntity> {

	public RemoteTerminalUnitBlock(Properties properties) {
		super(AllBlockEntities.REMOTE_TERMINAL_UNIT, properties);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			level.getBlockEntity(pos, this.blockEntityType.get()).ifPresent(be -> {
				ConfigurationManager.requestConfiguration(serverPlayer, pos, be);
			});
		}

		return ItemInteractionResult.SUCCESS;
	}
}
