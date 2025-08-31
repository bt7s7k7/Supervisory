package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.blocks.directControlDevice.DirectControlDeviceBlock;
import bt7s7k7.supervisory.blocks.programmableLogicController.support.LogEventRouter;
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

public class ProgrammableLogicControllerBlock extends DirectControlDeviceBlock<ProgrammableLogicControllerBlockEntity> {

	public ProgrammableLogicControllerBlock(Properties properties) {
		super(AllBlockEntities.PROGRAMMABLE_LOGIC_CONTROLLER, properties);
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
